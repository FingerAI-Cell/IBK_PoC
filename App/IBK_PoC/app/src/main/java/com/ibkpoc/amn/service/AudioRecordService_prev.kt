package com.ibkpoc.amn.service//// service/AudioRecordService.kt
//
//import android.Manifest
//import android.app.*
//import android.content.Context
//import android.content.Intent
//import android.media.AudioFormat
//import android.media.AudioRecord
//import android.media.MediaRecorder
//import android.media.audiofx.NoiseSuppressor
//import android.os.Build
//import android.os.Environment
//import android.os.IBinder
//import androidx.core.app.NotificationCompat
//import com.ibkpoc.amn.network.NetworkResult
//import com.ibkpoc.amn.util.Logger
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.*
//import java.io.File
//import java.io.FileOutputStream
//import javax.inject.Inject
//import android.content.pm.ServiceInfo
//import android.annotation.SuppressLint
//import android.content.pm.PackageManager
//import androidx.core.content.ContextCompat
//import android.util.Log
//import android.net.Uri
//import com.ibkpoc.amn.model.RecordServiceState
//import com.ibkpoc.amn.event.RecordingStateEvent
//import com.ibkpoc.amn.event.EventBus
//import org.webrtc.audio.AudioProcessing
//import org.webrtc.audio.NoiseSuppression
//import org.webrtc.audio.VoiceEngine
//import kotlin.math.abs
//import kotlin.math.max
//import android.media.audiofx.AutomaticGainControl
//
//@AndroidEntryPoint
//class AudioRecordService : Service() {
//    companion object {
//        const val ACTION_START = "com.ibkpoc.amn.action.START_RECORDING"
//        const val ACTION_STOP = "com.ibkpoc.amn.action.STOP_RECORDING"
//        const val EXTRA_MEETING_ID = "meeting_id"
//        const val EXTRA_START_TIME = "start_time"
//        const val EXTRA_FILE_PATH = "file_path"
//        private const val TAG = "AudioRecordService"
//    }
//
//    private var audioRecord: AudioRecord? = null
//    private var noiseSuppressor: NoiseSuppressor? = null
//    private var fileOutputStream: FileOutputStream? = null
//    private var isRecording = false
//    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
//    private var currentFilePath: String? = null
//
//    private val sampleRate = 44100
//    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
//    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
//    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
//
//    private val NOTIFICATION_ID = 1
//    private val CHANNEL_ID = "audio_record_channel"
//
//    private var recordFile: File? = null
//    private var currentMeetingId: Long = -1
//    private var currentStartTime: String = ""
//
//    private var webRtcProcessor: AudioProcessing? = null
//    private val normalizeBuffer = FloatArray(bufferSize / 2) // 16bit PCM을 float로 변환할 때 사용
//    private var maxAmplitude = 0f
//    private val targetAmplitude = 0.8f // 목표 볼륨 레벨 (0.0 ~ 1.0)
//
//    private var automaticGainControl: AutomaticGainControl? = null
//
//    override fun onBind(intent: Intent?): IBinder? = null
//
//    override fun onCreate() {
//        super.onCreate()
//        createNotificationChannel()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
//        } else {
//            startForeground(NOTIFICATION_ID, createNotification())
//        }
//    }
//
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                "recording_channel",
//                "Recording Service",
//                NotificationManager.IMPORTANCE_LOW
//            )
//            val manager = getSystemService(NotificationManager::class.java)
//            manager.createNotificationChannel(channel)
//        }
//    }
//
//    private fun createNotification(): Notification {
//        return NotificationCompat.Builder(this, "recording_channel")
//            .setContentTitle("녹음 진행 중")
//            .setContentText("백그라운드에서도 녹음이 계속됩니다.")
//            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
//            .setPriority(NotificationCompat.PRIORITY_LOW)
//            .build()
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        when(intent?.action) {
//            ACTION_START -> {
//                currentMeetingId = intent.getLongExtra(EXTRA_MEETING_ID, -1)
//                currentStartTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""
//                currentFilePath = intent.getStringExtra(EXTRA_FILE_PATH)
//
//                if (currentMeetingId != -1L && currentStartTime.isNotEmpty() &&
//                    currentFilePath != null && !isRecording) {
//                    startForeground(NOTIFICATION_ID, createNotification())
//                    startRecording()
//                }
//            }
//            ACTION_STOP -> {
//                stopRecording()
//                stopSelf()
//            }
//        }
//        return START_NOT_STICKY
//    }
//
//    private fun checkRecordPermission(): Boolean {
//        return ContextCompat.checkSelfPermission(
//            this,
//            Manifest.permission.RECORD_AUDIO
//        ) == PackageManager.PERMISSION_GRANTED
//    }
//
//    private fun startRecording() {
//        if (ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.RECORD_AUDIO
//            ) != PackageManager.PERMISSION_GRANTED) {
//            stopSelf()
//            return
//        }
//
//        try {
//            initWebRtc() // WebRTC 초기화
//            // 파일 생성
//            recordFile = File(currentFilePath ?: throw IllegalStateException("파일 경로가 없습니다"))
//            fileOutputStream = FileOutputStream(recordFile)
//
//            // 녹음 초기화
//            audioRecord = AudioRecord(
//                MediaRecorder.AudioSource.MIC,
//                sampleRate, channelConfig, audioFormat, bufferSize
//            ).apply {
//                startRecording()
//            }
//
//            setupAudioEnhancements()
//
//            isRecording = true
//            serviceScope.launch {
//                broadcastState(RecordServiceState.Recording(
//                    meetingId = currentMeetingId,
//                    startTime = currentStartTime
//                ))
//                recordAudioData()
//            }
//        } catch (e: SecurityException) {
//            serviceScope.launch {
//                broadcastState(RecordServiceState.Error("권한이 없습니다"))
//            }
//            stopSelf()
//        } catch (e: Exception) {
//            serviceScope.launch {
//                broadcastState(RecordServiceState.Error("녹음을 시작할 수 없습니다: ${e.message}"))
//            }
//            stopSelf()
//        }
//    }
//
//    private fun recordAudioData() {
//        val buffer = ByteArray(bufferSize)
//
//        while (isRecording) {
//            try {
//                val readSize = audioRecord?.read(buffer, 0, bufferSize) ?: -1
//                if (readSize > 0) {
//                    // WebRTC 처리
//                    webRtcProcessor?.let { processor ->
//                        processor.processStream(buffer, 0, readSize)
//                    }
//
//                    // 볼륨 정규화 적용
//                    val normalizedBuffer = normalizeAudio(buffer, readSize)
//
//                    fileOutputStream?.write(normalizedBuffer, 0, readSize)
//                    fileOutputStream?.flush()
//                }
//            } catch (e: Exception) {
//                Logger.e("녹음 중 오류: ${e.message}")
//                serviceScope.launch {
//                    broadcastState(RecordServiceState.Error(e.message ?: "녹음 중 오류 발생"))
//                }
//                isRecording = false
//                break
//            }
//        }
//    }
//
//    private fun stopRecording() {
//        try {
//            isRecording = false
//
//            audioRecord?.let { recorder ->
//                if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
//                    recorder.stop()
//                }
//                recorder.release()
//            }
//
//            fileOutputStream?.close()
//            noiseSuppressor?.release()
//            automaticGainControl?.release()
//
//            currentFilePath?.let { path ->
//                serviceScope.launch {
//                    broadcastState(RecordServiceState.Completed(path))
//                }
//            }
//        } catch (e: Exception) {
//            serviceScope.launch {
//                broadcastState(RecordServiceState.Error(e.message ?: "녹음 종료 중 오류 발생"))
//            }
//        } finally {
//            audioRecord = null
//            fileOutputStream = null
//            noiseSuppressor = null
//            webRtcProcessor?.release()
//            automaticGainControl = null
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        if (isRecording) {
//            isRecording = false
//            stopRecording()
//        }
//    }
//
//    private fun setupAudioEnhancements() {
//        audioRecord?.let { record ->
//            // 기존 노이즈 제거
//            if (NoiseSuppressor.isAvailable()) {
//                noiseSuppressor = NoiseSuppressor.create(record.audioSessionId)?.apply {
//                    enabled = true
//                }
//            }
//
//            // AGC 추가
//            if (AutomaticGainControl.isAvailable()) {
//                automaticGainControl = AutomaticGainControl.create(record.audioSessionId)?.apply {
//                    enabled = true
//                }
//            }
//        }
//    }
//
//    private suspend fun broadcastState(state: RecordServiceState) {
//        EventBus.post(RecordingStateEvent(state))
//    }
//
//    private fun initWebRtc() {
//        try {
//            webRtcProcessor = VoiceEngine.create()?.apply {
//                // WebRTC 설정
//                setNoiseSuppression(NoiseSuppression.HIGH)
//                setHighPassFilter(true)
//                setVoiceDetection(true)
//                setAutomaticGainControl(true)
//            }
//        } catch (e: Exception) {
//            Logger.e("WebRTC 초기화 실패: ${e.message}")
//        }
//    }
//
//    private fun normalizeAudio(buffer: ByteArray, size: Int): ByteArray {
//        // 바이트 배열을 float로 변환
//        for (i in 0 until size / 2) {
//            val sample = (buffer[i * 2].toInt() and 0xFF) or
//                    (buffer[i * 2 + 1].toInt() shl 8)
//            normalizeBuffer[i] = sample / 32768f
//        }
//
//        // 최대 진폭 찾기
//        maxAmplitude = normalizeBuffer.take(size / 2)
//            .map { abs(it) }
//            .maxOrNull() ?: 0f
//
//        // 볼륨 정규화
//        if (maxAmplitude > 0) {
//            val gainFactor = targetAmplitude / maxAmplitude
//            for (i in 0 until size / 2) {
//                normalizeBuffer[i] *= gainFactor
//
//                // float를 다시 16비트 PCM으로 변환
//                val normalizedSample = (normalizeBuffer[i] * 32768).toInt()
//                    .coerceIn(-32768, 32767)
//
//                buffer[i * 2] = normalizedSample.toByte()
//                buffer[i * 2 + 1] = (normalizedSample shr 8).toByte()
//            }
//        }
//
//        return buffer
//    }
//}
