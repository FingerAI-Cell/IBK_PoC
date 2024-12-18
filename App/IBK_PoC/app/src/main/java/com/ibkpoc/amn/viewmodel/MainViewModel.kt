package com.ibkpoc.amn.viewmodel

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.NoiseSuppressor
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibkpoc.amn.model.*
import com.ibkpoc.amn.network.NetworkResult
import com.ibkpoc.amn.repository.MeetingRepository
import com.ibkpoc.amn.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.content.Context
import com.ibkpoc.amn.config.Config
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import com.ibkpoc.amn.util.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.io.File
import android.content.Intent
import android.os.Build
import com.ibkpoc.amn.service.AudioRecordService
import java.io.FileInputStream
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.annotation.SuppressLint

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MeetingRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState = _recordingState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _hasRecordPermission = MutableStateFlow(false)
    val hasRecordPermission = _hasRecordPermission.asStateFlow()

    private val _showSuccessMessage = MutableStateFlow(false)
    val showSuccessMessage = _showSuccessMessage.asStateFlow()

    // 녹음 관련 설정
    private var audioRecord: AudioRecord? = null
    private val audioBuffer = mutableListOf<ByteArray>()
    private var isRecording = false
    private var recordingDurationJob: Job? = null
    private var noiseSuppressor: NoiseSuppressor? = null

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private val audioDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        .resolve("IBK_Records")
        .also { 
            if (!it.exists()) {
                it.mkdirs()
            }
        }

    private var currentRecordFile: File? = null
    private val bufferLock = Any()
    private var recordingReceiver: BroadcastReceiver? = null

    private var recordingJob: Job? = null

    init {
        registerRecordingReceiver()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerRecordingReceiver() {
        recordingReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioRecordService.ACTION_RECORDING_DATA) {
                    intent.getByteArrayExtra(AudioRecordService.EXTRA_AUDIO_DATA)?.let { data ->
                        processAudioData(data)
                    }
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Context.RECEIVER_NOT_EXPORTED
            } else {
                0
            }
            context.registerReceiver(
                recordingReceiver,
                IntentFilter(AudioRecordService.ACTION_RECORDING_DATA),
                flags
            )
        } else {
            context.registerReceiver(
                recordingReceiver,
                IntentFilter(AudioRecordService.ACTION_RECORDING_DATA)
            )
        }
    }

    private fun processAudioData(data: ByteArray) {
        synchronized(bufferLock) {
            audioBuffer.add(data)
            if (audioBuffer.size >= 10) { // 약 1초치 데이터
                saveBufferToFile()
            }
        }
    }

    private fun saveBufferToFile() {
        currentRecordFile?.let { file ->
            try {
                FileOutputStream(file, true).use { fos ->
                    synchronized(bufferLock) {
                        audioBuffer.forEach { chunk ->
                            fos.write(chunk)
                        }
                        fos.flush()
                        audioBuffer.clear()
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "파일 저장 실패: ${e.message}"
            }
        }
    }

    fun startMeeting(participantCount: Int) {
        if (participantCount <= 0) {
            _errorMessage.value = "참가자 수는 1명 이상이어야 합니다"
            return
        }
        Logger.i("MainViewModel 전달받은 참가자 수: $participantCount")

        viewModelScope.launch {
            val startTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            
            repository.startMeeting(participantCount).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        // 녹음 파일 생성
                        val recordDir = context.getExternalFilesDir(null)
                            ?.resolve("IBK_Records")
                            ?.apply { mkdirs() }
                        
                        currentRecordFile = File(
                            recordDir,
                            "record_${result.data.convId}_${startTime.replace(":", "-")}.pcm"
                        ).apply { createNewFile() }

                        // 서비스 시작
                        val serviceIntent = Intent(context, AudioRecordService::class.java).apply {
                            action = AudioRecordService.ACTION_START
                            putExtra(AudioRecordService.EXTRA_MEETING_ID, result.data.convId)
                            putExtra(AudioRecordService.EXTRA_START_TIME, startTime)
                        }
                        
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                        
                        _recordingState.value = RecordingState.Recording(result.data.convId, startTime)
                        
                        // 녹음 시간 업데이트 로직 추가
                        recordingJob = viewModelScope.launch {
                            var seconds = 0L
                            while (true) {
                                delay(1000)
                                seconds++
                                (_recordingState.value as? RecordingState.Recording)?.let {
                                    _recordingState.value = it.copy(duration = seconds)
                                }
                            }
                        }
                    }
                    is NetworkResult.Error -> {
                        _errorMessage.value = result.message
                    }
                    is NetworkResult.Loading -> {
                        _isLoading.value = true
                    }
                }
                _isLoading.value = false
            }
        }
    }

    fun endMeeting() {
        recordingJob?.cancel()  // 타이머 취소
        viewModelScope.launch {
            val currentState = _recordingState.value as? RecordingState.Recording ?: return@launch
            
            try {
                // 1. 녹음 서비스 중지
                context.stopService(Intent(context, AudioRecordService::class.java))
                
                // 2. 마지막 버퍼 저장
                saveBufferToFile()
                
                // 3. PCM -> WAV 변환
                val wavFile = currentRecordFile?.let { pcmFile ->
                    File(
                        pcmFile.parentFile,
                        "meeting_${currentState.meetingId}_${currentState.startTime.replace(":", "-")}.wav"
                    ).also { wavFile ->
                        convertPcmToWav(pcmFile, wavFile)
                    }
                } ?: throw Exception("녹음 파일이 없습니다")

                // 4. 회의 종료 API 호출 (독립적)
                repository.endMeeting(currentState.meetingId).collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            Logger.i("회의 종료 완료: meetingId=${currentState.meetingId}")
                            _errorMessage.value = null
                        }
                        is NetworkResult.Error -> {
                            _errorMessage.value = "회의 종료 실패: ${result.message}"
                        }
                        is NetworkResult.Loading -> _isLoading.value = true
                    }
                }

                // 5. WAV 파일 업로드 (별도 진행)
                uploadWavAndConvertToStt(currentState.meetingId, wavFile, currentState.startTime, currentState.duration)

            } catch (e: Exception) {
                _errorMessage.value = "녹음 종료 실패: ${e.message}"
                cleanupRecording()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun uploadWavAndConvertToStt(meetingId: Long, wavFile: File, startTime: String, duration: Long) {
        val wavUploadData = WavUploadData(
            meetingId = meetingId,
            wavFile = wavFile,
            startTime = startTime,
            duration = duration,
            totalChunks = 0,
            currentChunk = 0,
            chunkData = ByteArray(0)
        )

        repository.uploadMeetingWavFile(wavUploadData).collect { uploadResult ->
            when (uploadResult) {
                is NetworkResult.Success -> {
                    Logger.i("WAV 파일 업로드 완료: meetingId=$meetingId")

                    // STT 변환 호출
                    repository.convertWavToStt(meetingId).collect { sttResult ->
                        when (sttResult) {
                            is NetworkResult.Success -> {
                                Logger.i("STT 변환 완료: meetingId=$meetingId")
                                handleMeetingEndSuccess() // UI 업데이트
                                cleanupRecording()
                            }
                            is NetworkResult.Error -> {
                                _errorMessage.value = "STT 변환 실패: ${sttResult.message}"
                            }
                            is NetworkResult.Loading -> {
                                _isLoading.value = true
                            }
                        }
                    }
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = "WAV 파일 업로드 실패: ${uploadResult.message}"
                }
                is NetworkResult.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordingReceiver?.let {
            context.unregisterReceiver(it)
        }
        cleanupRecording()
    }

    private fun startRecording(meetingId: Long, startTime: String) {
        if (!_hasRecordPermission.value) {
            _errorMessage.value = "녹음 권한이 없습니다"
            return
        }

        // 새 회의 시작시 새 파일 생성
        currentRecordFile = File(audioDir, "record_${meetingId}_${startTime.replace(":", "-")}.pcm")
        Logger.i("새 녹음 파일 생성: ${currentRecordFile?.absolutePath}")

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            _errorMessage.value = "녹음 권한이 없습니다"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // AudioRecord 초기화 시도
                val audioRecord = try {
                    AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        bufferSize
                    )
                } catch (e: SecurityException) {
                    _errorMessage.value = "녹음 권한이 거부되었습니다"
                    return@launch
                }

                this@MainViewModel.audioRecord = audioRecord
                
                if (NoiseSuppressor.isAvailable()) {
                    try {
                        noiseSuppressor = NoiseSuppressor.create(audioRecord.audioSessionId)
                    } catch (e: Exception) {
                        Logger.e("노이즈 서프레서 초기화 실패", e)
                        // 노이즈 서프레서는 실패해도 계속 진행
                    }
                }

                audioRecord.startRecording()
                isRecording = true
                _recordingState.value = RecordingState.Recording(meetingId, startTime)

                // 녹음 시간 카운트 시작
                startDurationCounter()

                val buffer = ByteArray(bufferSize)
                while (isRecording) {
                    val readSize = audioRecord.read(buffer, 0, bufferSize)
                    if (readSize > 0) {
                        synchronized(audioBuffer) {
                            audioBuffer.add(buffer.copyOf(readSize))
                            currentRecordFile?.let { file ->
                                FileOutputStream(file, true).use { fos ->
                                    fos.write(buffer, 0, readSize)
                                    fos.flush()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "녹음을 시작할 수 없습니다: ${e.message}"
                cleanupRecording()
            }
        }
    }

    private fun startDurationCounter() {
        recordingDurationJob = viewModelScope.launch {
            var duration = 0L
            while (isRecording) {
                delay(1000)
                duration++
                _recordingState.value = (_recordingState.value as? RecordingState.Recording)?.copy(
                    duration = duration
                ) ?: return@launch
            }
        }
    }

    private fun writeWavHeader(output: FileOutputStream, audioLength: Long) {
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

    private fun ByteArray.putLittleEndianInt(offset: Int, value: Int) {
        this[offset] = value.toByte()
        this[offset + 1] = (value shr 8).toByte()
        this[offset + 2] = (value shr 16).toByte()
        this[offset + 3] = (value shr 24).toByte()
    }

    private fun ByteArray.putLittleEndianShort(offset: Int, value: Short) {
        this[offset] = value.toByte()
        this[offset + 1] = (value.toInt() shr 8).toByte()
    }

    fun setHasRecordPermission(hasPermission: Boolean) {
        _hasRecordPermission.value = hasPermission
    }

    fun updateRecordPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        setHasRecordPermission(hasPermission)
    }

    private fun cleanupRecording() {
        audioRecord?.stop()
        audioRecord?.release()
        noiseSuppressor?.release()
        audioRecord = null
        noiseSuppressor = null
        synchronized(audioBuffer) {
            audioBuffer.clear()
        }
        recordingDurationJob?.cancel()
        currentRecordFile = null  // 녹음 종료시 파일 참조 제거
    }

    private fun convertPcmToWav(pcmFile: File, wavFile: File) {
        FileInputStream(pcmFile).use { input ->
            FileOutputStream(wavFile).use { output ->
                // WAV 헤더 작성
                writeWavHeader(output, pcmFile.length())
                
                // 청크 단위로 복사
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
        }
    }

    // 백그라운드/포그라운드 전환 시에는 아무 작업도 하지 않음
    fun onAppBackgrounded() {}
    fun onAppForegrounded() {}

    fun onMessageShown() {
        _errorMessage.value = null
    }


    fun handleMeetingEndSuccess() {
        viewModelScope.launch {
            _showSuccessMessage.value = true
            delay(2000) // 2초 후
            _showSuccessMessage.value = false
        }
    }
}