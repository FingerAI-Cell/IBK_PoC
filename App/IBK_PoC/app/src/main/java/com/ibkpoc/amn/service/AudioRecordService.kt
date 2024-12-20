// service/AudioRecordService.kt
package com.ibkpoc.amn.service

import android.Manifest
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
import com.ibkpoc.amn.network.NetworkResult
import com.ibkpoc.amn.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import android.content.pm.ServiceInfo
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.util.Log
import android.net.Uri

@AndroidEntryPoint
class AudioRecordService : Service() {
    companion object {
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
        const val EXTRA_MEETING_ID = "meeting_id"
        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_FILE_PATH = "file_path"
        
        const val ACTION_RECORDING_DATA = "com.ibkpoc.amn.action.RECORDING_DATA"
        const val EXTRA_AUDIO_DATA = "com.ibkpoc.amn.extra.AUDIO_DATA"
        private const val TAG = "AudioRecordService"
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentFilePath: String? = null
    
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    
    private var noiseSuppressor: NoiseSuppressor? = null

    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "audio_record_channel"

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
        when(intent?.action) {
            ACTION_START -> {
                val meetingId = intent.getLongExtra(EXTRA_MEETING_ID, -1)
                val startTime = intent.getStringExtra(EXTRA_START_TIME)
                val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
                
                if (meetingId != -1L && startTime != null && filePath != null && !isRecording) {
                    currentFilePath = filePath
                    startForeground(NOTIFICATION_ID, createNotification())
                    startRecording()
                }
            }
            ACTION_STOP -> {
                stopRecording()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun checkRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startRecording() {
        if (!checkRecordPermission()) {
            Logger.e("권한 없음")
            return
        }
        
        try {
            Logger.e("AudioRecord 초기화 시작")
            isRecording = true
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            ).apply {
                startRecording()
            }

            if (NoiseSuppressor.isAvailable()) {
                try {
                    noiseSuppressor = NoiseSuppressor.create(audioRecord?.audioSessionId ?: 0)
                    noiseSuppressor?.enabled = true
                    Logger.i("노이즈 서프레서 활성화됨")
                } catch (e: Exception) {
                    Logger.e("노이즈 서프레서 초기화 실패: ${e.message}")
                }
            }

            serviceScope.launch {
                recordAudioData()
            }
        } catch (e: Exception) {
            Logger.e("녹음 시작 실패: ${e.message}")
            stopSelf()
        }
    }

    private fun recordAudioData() {
        val buffer = ByteArray(bufferSize)
        var accumulatedData = ByteArray(0)
        var lastBroadcastTime = System.currentTimeMillis()
        val BROADCAST_INTERVAL = 200L  // 200ms 간격
        
        while (isRecording) {
            try {
                val readSize = audioRecord?.read(buffer, 0, bufferSize) ?: -1
                if (readSize > 0) {
                    // 데이터 누적
                    accumulatedData = accumulatedData.plus(buffer.copyOf(readSize))
                    
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastBroadcastTime >= BROADCAST_INTERVAL) {
                        Logger.e("오디오 데이터 전송: ${accumulatedData.size} bytes")
                        Intent(ACTION_RECORDING_DATA).apply {
                            setPackage(applicationContext.packageName)
                            putExtra(EXTRA_AUDIO_DATA, accumulatedData)
                            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                            sendBroadcast(this)
                        }
                        
                        // 초기화
                        accumulatedData = ByteArray(0)
                        lastBroadcastTime = currentTime
                    }
                }
            } catch (e: Exception) {
                Logger.e("녹음 중 오류: ${e.message}")
                isRecording = false
                break
            }
        }
    }

    private fun stopRecording() {
        isRecording = false
        
        noiseSuppressor?.apply {
            enabled = false
            release()
        }
        noiseSuppressor = null
        
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            isRecording = false
            stopRecording()
        }
    }
}
