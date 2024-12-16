// service/AudioRecordService.kt
package com.ibkpoc.amn.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ibkpoc.amn.model.RecordingData
import com.ibkpoc.amn.network.NetworkResult
import com.ibkpoc.amn.repository.MeetingRepository
import com.ibkpoc.amn.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import android.content.pm.ServiceInfo

@AndroidEntryPoint
class AudioRecordService : Service() {
    private var meetingId: Long = -1
    private var audioRecord: AudioRecord? = null
    private val audioBuffer = mutableListOf<ByteArray>()
    private var isRecording = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastChunkStartTime: Long = 0
    private var currentRecordFile: File? = null
    
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val BUFFER_BLOCK_LIMIT = 5

    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "audio_record_channel"

    @Inject
    lateinit var repository: MeetingRepository

    private var noiseSuppressor: NoiseSuppressor? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "recording_channel",
                "Recording Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "recording_channel")
            .setContentTitle("녹음 진행 중")
            .setContentText("백그라운드에서도 녹음이 계속됩니다.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val meetingId = intent?.getLongExtra("meetingId", -1) ?: -1
        val startTime = intent?.getStringExtra("startTime")
        
        if (meetingId != -1L && startTime != null) {
            startRecording(meetingId, startTime)
        }
        
        return START_STICKY
    }

    private fun startRecording(meetingId: Long, startTime: String) {
        this.meetingId = meetingId
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        currentRecordFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .resolve("IBK_Records")
                .also { if (!it.exists()) it.mkdirs() },
            "record_${meetingId}_${startTime.replace(":", "-")}.pcm"
        )

        isRecording = true
        lastChunkStartTime = System.currentTimeMillis()
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = NoiseSuppressor.create(audioRecord!!.audioSessionId).apply {
                enabled = true
            }
        }

        audioRecord?.startRecording()

        serviceScope.launch {
            val buffer = ByteArray(bufferSize)
            while (isRecording) {
                val readSize = audioRecord?.read(buffer, 0, bufferSize) ?: -1
                if (readSize > 0) {
                    val bufferCopy = buffer.copyOf(readSize)
                    val dataToUpload: ByteArray? = synchronized(audioBuffer) {
                        audioBuffer.add(bufferCopy)
                        if (audioBuffer.size >= BUFFER_BLOCK_LIMIT) {
                            val data = audioBuffer.reduce { acc, bytes -> acc + bytes }
                            audioBuffer.clear()
                            data
                        } else {
                            null
                        }
                    }
                    dataToUpload?.let { saveAndUploadBuffer(it) }
                }
            }
        }
    }

    private suspend fun saveAndUploadBuffer(pcmData: ByteArray) {
        try {
            val currentTime = System.currentTimeMillis()
            val duration = currentTime - lastChunkStartTime
            
            currentRecordFile?.let { file ->
                withContext(Dispatchers.IO) {
                    FileOutputStream(file, true).use { fos ->
                        fos.write(pcmData)
                        fos.flush()
                    }
                }
            }
            
            repository.uploadMeetingRecordChunk(
                RecordingData(
                    meetingId = meetingId,
                    chunkStartTime = lastChunkStartTime,
                    duration = duration,
                    audioData = pcmData
                )
            ).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        lastChunkStartTime = currentTime
                        Logger.i("청크 업로드 성공")
                    }
                    is NetworkResult.Error -> {
                        Logger.e("청크 업로드 실패: ${result.message}")
                    }
                    is NetworkResult.Loading -> { /* 로딩 처리 */ }
                }
            }
        } catch (e: Exception) {
            Logger.e("버퍼 저장/업로드 실패", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.launch(NonCancellable) {
            try {
                isRecording = false
                audioRecord?.stop()
                
                val finalData: ByteArray? = synchronized(audioBuffer) {
                    if (audioBuffer.isNotEmpty()) {
                        val data = audioBuffer.reduce { acc, bytes -> acc + bytes }
                        audioBuffer.clear()
                        data
                    } else null
                }
                
                finalData?.let { saveAndUploadBuffer(it) }
                
            } catch (e: Exception) {
                Logger.e("서비스 종료 중 오류", e)
            } finally {
                audioRecord?.release()
                noiseSuppressor?.release()
                audioRecord = null
                noiseSuppressor = null
                serviceScope.cancel()
            }
        }
    }
}
