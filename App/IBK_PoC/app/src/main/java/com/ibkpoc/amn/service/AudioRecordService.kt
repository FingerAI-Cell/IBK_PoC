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
import com.ibkpoc.amn.model.RecordServiceState
import com.ibkpoc.amn.event.RecordingStateEvent
import com.ibkpoc.amn.event.EventBus
import com.ibkpoc.amn.model.WavUploadData
import com.ibkpoc.amn.model.SttRequest
import com.ibkpoc.amn.model.UploadData
import com.ibkpoc.amn.repository.MeetingRepository
import java.io.RandomAccessFile
import java.io.ByteArrayOutputStream

@AndroidEntryPoint
class AudioRecordService : Service() {
    @Inject
    lateinit var repository: MeetingRepository // Hilt를 통해 주입
    companion object {
        const val ACTION_START = "com.ibkpoc.amn.action.START_RECORDING"
        const val ACTION_STOP = "com.ibkpoc.amn.action.STOP_RECORDING"
        const val EXTRA_MEETING_ID = "meeting_id"
        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_FILE_PATH = "file_path"
        private const val TAG = "AudioRecordService"
    }

    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var fileOutputStream: FileOutputStream? = null
    private var isRecording = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentFilePath: String? = null
    
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "audio_record_channel"

    private var recordFile: File? = null
    private var currentMeetingId: Long = -1
    private var currentStartTime: String = ""
    private var wavFile: File? = null
    private var wavOutputStream: FileOutputStream? = null

    private val audioDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        .resolve("IBK_Records")
        .also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }

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

    private fun createWavFile(meetingId: Long, startTime: String) {
        wavFile = File(audioDir, "record_${meetingId}_${startTime}.wav").apply {
            if (!exists()) createNewFile()
        }
        wavOutputStream = FileOutputStream(wavFile, true) // Append mode
        // 초기 빈 헤더 작성
        writeWavHeader(wavOutputStream!!, 0)
    }

    private fun appendToWavFile(sectionData: ByteArray) {
        wavOutputStream?.let {
            it.write(sectionData)
            it.flush()
            updateWavHeader()
        }
    }
    private fun updateWavHeader() {
        wavOutputStream?.let { output ->
            val wavFileLength = wavFile?.length() ?: 0
            RandomAccessFile(wavFile, "rw").use { raf ->
                raf.seek(4)
                raf.write((wavFileLength - 8).toInt().toLittleEndian())
                raf.seek(40)
                raf.write((wavFileLength - 44).toInt().toLittleEndian())
            }
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
                currentMeetingId = intent.getLongExtra(EXTRA_MEETING_ID, -1)
                currentStartTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""
                currentFilePath = intent.getStringExtra(EXTRA_FILE_PATH)
                
                if (currentMeetingId != -1L && currentStartTime.isNotEmpty() && 
                    currentFilePath != null && !isRecording) {
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
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }

        try {
            // 파일 생성
            recordFile = File(currentFilePath ?: throw IllegalStateException("파일 경로가 없습니다"))
            fileOutputStream = FileOutputStream(recordFile)
            // 녹음 초기화
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate, channelConfig, audioFormat, bufferSize
            ).apply {
                startRecording()
            }
            
            setupNoiseSuppressor()
            
            isRecording = true
            serviceScope.launch {
                broadcastState(RecordServiceState.Recording(
                    meetingId = currentMeetingId,
                    startTime = currentStartTime
                ))
                recordAudioData()
            }
        } catch (e: SecurityException) {
            serviceScope.launch {
                broadcastState(RecordServiceState.Error("권한이 없습니다"))
            }
            stopSelf()
        } catch (e: Exception) {
            serviceScope.launch {
                broadcastState(RecordServiceState.Error("녹음을 시작할 수 없습니다: ${e.message}"))
            }
            stopSelf()
        }
    }

    private suspend fun recordAudioData() {
        val buffer = ByteArray(bufferSize)

        var totalPcmSize = 0L
        var sectionNumber = 1
        var lastTriggerSize = 0L // 마지막 섹션 트리거 시점의 누적 크기
        var currentOffset = 0L

        // PCM 기준 4분 30초

        val bitsPerSample = if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) 16 else 8
        val bytesPerSample = bitsPerSample / 8
        Logger.i("샘플 비트 깊이: $bitsPerSample 비트 ($bytesPerSample 바이트)")

        val durationSeconds = 4 * 60 + 30 // 4분 30초
        val pcmThreshold = sampleRate * (if (channelConfig == AudioFormat.CHANNEL_IN_MONO) 1 else 2) * bytesPerSample * durationSeconds

        createWavFile(currentMeetingId,currentStartTime)
        Logger.i("녹음 시작: meetingId=$currentMeetingId, threshold=$pcmThreshold")

        while (isRecording) {
            val readSize = audioRecord?.read(buffer, 0, bufferSize) ?: -1
            if (readSize > 0) {
                // PCM 데이터를 저장
                fileOutputStream?.write(buffer, 0, readSize)
                fileOutputStream?.flush()

                // 누적 크기 증가
                totalPcmSize += readSize

                // 트리거: 4분30초 WAV 기준 도달 시
                if (totalPcmSize >= lastTriggerSize + pcmThreshold) {
                    Logger.i("섹션 트리거 발생: meetingId=$currentMeetingId, sectionNumber=$sectionNumber")
                    processSection(
                        pcmFile = File(currentFilePath),
                        startOffset = lastTriggerSize,
                        endOffset = lastTriggerSize + pcmThreshold,
                        sectionNumber = sectionNumber,
                        meetingId = currentMeetingId,
                        startTime = currentStartTime
                    )

                    // 섹션 번호 증가 및 기준점 업데이트
                    sectionNumber++
                    lastTriggerSize += pcmThreshold
                }
            }
        }

        // 녹음 종료 후 남은 데이터 처리
        if (totalPcmSize > lastTriggerSize) {
            Logger.i("잔여 데이터 처리: meetingId=$currentMeetingId, 마지막 섹션=$sectionNumber")
            processSection(
                pcmFile = File(currentFilePath),
                startOffset = lastTriggerSize,
                endOffset = totalPcmSize,
                sectionNumber = sectionNumber,
                meetingId = currentMeetingId,
                startTime = currentStartTime
            )
        }
        // 모든 작업 완료 이벤트 전송
        notifyAllSectionsCompleted(currentMeetingId)
        Logger.i("녹음 종료: meetingId=$currentMeetingId")
    }

    private fun notifyAllSectionsCompleted(meetingId: Long) {
        serviceScope.launch {
            EventBus.post(RecordingStateEvent(RecordServiceState.AllTasksCompleted(meetingId)))
            Logger.i("모든 섹션 작업 완료 이벤트 전송: 회의 ID $meetingId")
        }
    }

    private fun stopRecording() {
        try {
            isRecording = false
            
            audioRecord?.let { recorder ->
                if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    recorder.stop()
                }
                recorder.release()
            }

            fileOutputStream?.close()
            wavOutputStream?.close()
            wavFile?.let { Logger.i("WAV 파일 저장 완료: ${it.absolutePath}") }

            noiseSuppressor?.release()
            
            currentFilePath?.let { path ->
                serviceScope.launch {
                    broadcastState(RecordServiceState.Completed(path))
                }
            }
        } catch (e: Exception) {
            serviceScope.launch {
                broadcastState(RecordServiceState.Error(e.message ?: "녹음 종료 중 오류 발생"))
            }
        } finally {
            audioRecord = null
            fileOutputStream = null
            noiseSuppressor = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            isRecording = false
            stopRecording()
        }
    }

    private suspend fun convertPcmToWav(pcmFile: File, startOffset: Long, endOffset: Long): ByteArray {
        // 임시 ByteArrayOutputStream 사용
        Logger.i("WAV 변환 시작: 파일=${pcmFile.name}, 시작 오프셋=$startOffset, 종료 오프셋=$endOffset")
        val output = ByteArrayOutputStream()
        
        // WAV 헤더 작성
        val pcmSize = endOffset - startOffset
        writeWavHeader(output, pcmSize)
        
        // PCM 데이터 읽기
        RandomAccessFile(pcmFile, "r").use { input ->
            input.seek(startOffset)
            val buffer = ByteArray(8192)
            var remaining = pcmSize
            while (remaining > 0) {
                val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                val bytesRead = input.read(buffer, 0, toRead)
                if (bytesRead == -1) break
                output.write(buffer, 0, bytesRead)
                remaining -= bytesRead
            }
        }
        Logger.i("WAV 변환 완료: 파일=${pcmFile.name}, 변환된 크기=${output.size()}")
        return output.toByteArray()
    }

    private fun writeWavHeader(output: java.io.OutputStream, audioLength: Long) {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val totalDataLen = audioLength + 36

        output.write(ByteArray(44).apply {
            // RIFF 헤더
            this[0] = 'R'.code.toByte()
            this[1] = 'I'.code.toByte()
            this[2] = 'F'.code.toByte()
            this[3] = 'F'.code.toByte()

            // 파일 크기
            this[4] = (totalDataLen and 0xff).toByte()
            this[5] = ((totalDataLen shr 8) and 0xff).toByte()
            this[6] = ((totalDataLen shr 16) and 0xff).toByte()
            this[7] = ((totalDataLen shr 24) and 0xff).toByte()

            // WAVE 헤더
            this[8] = 'W'.code.toByte()
            this[9] = 'A'.code.toByte()
            this[10] = 'V'.code.toByte()
            this[11] = 'E'.code.toByte()

            // fmt 청크
            this[12] = 'f'.code.toByte()
            this[13] = 'm'.code.toByte()
            this[14] = 't'.code.toByte()
            this[15] = ' '.code.toByte()
            this[16] = 16  // fmt 청크 크기
            this[17] = 0
            this[18] = 0
            this[19] = 0

            // 오디오 포맷 (PCM = 1)
            this[20] = 1
            this[21] = 0

            // 채널 수
            this[22] = channels.toByte()
            this[23] = 0

            // 샘플레이트
            this[24] = (sampleRate and 0xff).toByte()
            this[25] = ((sampleRate shr 8) and 0xff).toByte()
            this[26] = ((sampleRate shr 16) and 0xff).toByte()
            this[27] = ((sampleRate shr 24) and 0xff).toByte()

            // 바이트레이트
            this[28] = (byteRate and 0xff).toByte()
            this[29] = ((byteRate shr 8) and 0xff).toByte()
            this[30] = ((byteRate shr 16) and 0xff).toByte()
            this[31] = ((byteRate shr 24) and 0xff).toByte()
            this[32] = (channels * bitsPerSample / 8).toByte()
            this[33] = 0

            // 비트퍼샘플
            this[34] = bitsPerSample.toByte()
            this[35] = 0

            // 데이터 청크
            this[36] = 'd'.code.toByte()
            this[37] = 'a'.code.toByte()
            this[38] = 't'.code.toByte()
            this[39] = 'a'.code.toByte()

            // 데이터 크기
            this[40] = (audioLength and 0xff).toByte()
            this[41] = ((audioLength shr 8) and 0xff).toByte()
            this[42] = ((audioLength shr 16) and 0xff).toByte()
            this[43] = ((audioLength shr 24) and 0xff).toByte()
        })
    }

    private suspend fun processSection(
        pcmFile: File,
        startOffset: Long,
        endOffset: Long,
        sectionNumber: Int,
        meetingId: Long,
        startTime: String
    ) {
        Logger.i("섹션 처리 시작: meetingId=$meetingId, sectionNumber=$sectionNumber, startOffset=$startOffset, endOffset=$endOffset")
        // WAV 변환 (이제 ByteArray 직접 반환)
        val wavData = convertPcmToWav(pcmFile, startOffset, endOffset)
        
        // WAV 파일 이어붙이기
        appendToWavFile(wavData)
        
        // 서버 전송 (같은 wavData 사용)
        val uploadData = UploadData(
            meetingId = meetingId,
            startTime = startTime,
            sectionNumber = sectionNumber,
            chunkData = wavData
        )
        Logger.i("uploadMeetingWavFile 호출 준비 중: uploadData=$uploadData")
        repository.uploadMeetingWavFile(uploadData).collect { result ->
            when (result) {
                is NetworkResult.Loading -> Logger.i("섹션 업로드 중: sectionNumber=$sectionNumber")
                is NetworkResult.Success -> Logger.i("섹션 업로드 완료: sectionNumber=$sectionNumber")
                is NetworkResult.Error -> Logger.e("섹션 업로드 실패: sectionNumber=$sectionNumber, 에러=${result.message}")
            }
        }
        Logger.i("섹션 처리 완료: meetingId=$meetingId, sectionNumber=$sectionNumber")
    }


    private fun setupNoiseSuppressor() {
        if (NoiseSuppressor.isAvailable()) {
            audioRecord?.let { record ->
                noiseSuppressor = NoiseSuppressor.create(record.audioSessionId)?.apply {
                    enabled = true
                }
            }
        }
    }

    private suspend fun broadcastState(state: RecordServiceState) {
        EventBus.post(RecordingStateEvent(state))
    }

    private fun Int.toLittleEndian(): ByteArray {
        return byteArrayOf(
            (this and 0xFF).toByte(),
            ((this shr 8) and 0xFF).toByte(),
            ((this shr 16) and 0xFF).toByte(),
            ((this shr 24) and 0xFF).toByte()
        )
    }
}
