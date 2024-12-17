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

@AndroidEntryPoint
class AudioRecordService : Service() {
    companion object {
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
        const val EXTRA_MEETING_ID = "meeting_id"
        const val EXTRA_START_TIME = "start_time"
        
        const val ACTION_RECORDING_DATA = "com.ibkpoc.amn.action.RECORDING_DATA"
        const val EXTRA_AUDIO_DATA = "com.ibkpoc.amn.extra.AUDIO_DATA"
        private const val TAG = "AudioRecordService"
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentRecordFile: File? = null
    
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
                if (meetingId != -1L && startTime != null && !isRecording) {
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

    private suspend fun recordAudioData() {
        val buffer = ByteArray(bufferSize)
        while (isRecording) {
            try {
                val readSize = audioRecord?.read(buffer, 0, bufferSize) ?: -1
                if (readSize > 0) {
                    // 원본 데이터 그대로 전송
                    Intent(ACTION_RECORDING_DATA).also { intent ->
                        intent.putExtra(EXTRA_AUDIO_DATA, buffer.copyOf(readSize))
                        sendBroadcast(intent)
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
        
        currentRecordFile?.let {
            if (it.exists() && it.length() > 0) {
                Log.i(TAG, "녹음 파일 저장 완료: ${it.absolutePath}")
            } else {
                Logger.e("녹음 파일 저장 실패 또는 빈 파일")
            }
        }
        currentRecordFile = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            isRecording = false
            stopRecording()
        }
    }
}
