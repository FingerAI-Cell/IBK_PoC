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
import com.ibkpoc.amn.util.EventBus

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

    // 녹음 관련 설정
    private var audioRecord: AudioRecord? = null
    private val recordedData = mutableListOf<ByteArray>()
    private var isRecording = false
    private var recordingDurationJob: Job? = null

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private var tempFile: File? = null
    private var currentBufferSize = 0
    private val MAX_BUFFER_SIZE = 1024 * 1024 * 10  // 10MB

    sealed class RecordingState {
        object Idle : RecordingState()
        data class Recording(
            val meetingId: Long,
            val startTime: String,
            val duration: Long = 0L
        ) : RecordingState()
    }

    init {
        viewModelScope.launch {
            EventBus.recordingEvent.collect { event ->
                when (event) {
                    is EventBus.RecordingEvent.ForceStop -> {
                        (_recordingState.value as? RecordingState.Recording)?.let {
                            endMeeting()
                        }
                    }
                }
            }
        }
    }

    fun startMeeting() {
        viewModelScope.launch {
            _isLoading.value = true
            val startTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            
            repository.startMeeting().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        startRecording(result.data.convId, startTime)
                        _errorMessage.value = null
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
        viewModelScope.launch {
            val currentState = _recordingState.value as? RecordingState.Recording ?: return@launch
            _isLoading.value = true
            
            stopRecording()
            
            repository.endMeeting(currentState.meetingId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _recordingState.value = RecordingState.Idle
                        uploadRecording(currentState.meetingId)
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

    private fun startRecording(meetingId: Long, startTime: String) {
        if (!_hasRecordPermission.value) {
            _errorMessage.value = "녹음 권한이 없습니다"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 임시 파일 생성
                tempFile = File(
                    context.getExternalFilesDir(null),
                    "temp_meeting_${System.currentTimeMillis()}.pcm"
                )
                
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                val buffer = ByteArray(bufferSize)
                audioRecord?.startRecording()
                isRecording = true

                _recordingState.value = RecordingState.Recording(meetingId, startTime)
                startDurationCounter()

                FileOutputStream(tempFile, true).use { output ->
                    while (isRecording) {
                        val readSize = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                        if (readSize > 0) {
                            output.write(buffer, 0, readSize)
                            currentBufferSize += readSize
                            
                            // 버퍼 크기가 임계값을 넘으면 임시 저장
                            if (currentBufferSize >= MAX_BUFFER_SIZE) {
                                output.flush()
                                currentBufferSize = 0
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "녹음을 시작할 수 없습니다: ${e.message}"
                handleRecordingError()
            }
        }
    }

    private fun handleRecordingError() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                stopRecording()
                // 비정상 종료시 임시 파일을 PCM 형태로 보존
                tempFile?.let { file ->
                    val safeName = "recovered_${System.currentTimeMillis()}.pcm"
                    val safeFile = File(context.getExternalFilesDir(null), safeName)
                    file.copyTo(safeFile, true)
                    Logger.i("녹음 파일 복구 저장됨: ${safeFile.absolutePath}")
                }
            } catch (e: Exception) {
                Logger.e("녹음 파일 복구 실패", e)
            }
        }
    }

    private fun stopRecording() {
        isRecording = false
        recordingDurationJob?.cancel()
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Logger.e("녹음 종료 실패", e)
        }
        
        audioRecord = null
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

    private fun uploadRecording(meetingId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(Date())
                val wavFile = File(
                    context.getExternalFilesDir(null),
                    "meeting_$timestamp.wav"
                )
                
                // PCM to WAV 변환
                tempFile?.let { pcmFile ->
                    FileOutputStream(wavFile).use { output ->
                        writeWavHeader(output, pcmFile.length())
                        pcmFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    
                    // 업로드 후 임시 파일 삭제
                    repository.uploadMeetingRecord(meetingId, wavFile).collect { result ->
                        when (result) {
                            is NetworkResult.Success -> {
                                Logger.i("녹음 파일 업로드 성공")
                                wavFile.delete()
                                pcmFile.delete()
                                tempFile = null
                            }
                            is NetworkResult.Error -> {
                                Logger.e("녹음 파일 업로드 실패", null)
                                _errorMessage.value = "녹음 파일 업로드 실패: ${result.message}"
                            }
                            is NetworkResult.Loading -> {
                                _isLoading.value = true
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.e("녹음 파일 업로드 중 오류", e)
                _errorMessage.value = "녹음 파일 업로드 중 오류: ${e.message}"
                handleRecordingError()  // 업로드 실패시에도 파일 보존 시도
            }
        }
    }

    private fun writeWavHeader(output: FileOutputStream, audioLength: Long) {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val totalDataLen = audioLength + 36
        
        val header = ByteArray(44)
        
        // RIFF 헤더
        System.arraycopy("RIFF".toByteArray(), 0, header, 0, 4)
        header.putLittleEndianInt(4, totalDataLen.toInt())
        System.arraycopy("WAVE".toByteArray(), 0, header, 8, 4)
        
        // fmt 청크
        System.arraycopy("fmt ".toByteArray(), 0, header, 12, 4)
        header.putLittleEndianInt(16, 16) // fmt 청크 크기
        header.putLittleEndianShort(20, 1) // PCM 오디오 포맷
        header.putLittleEndianShort(22, channels.toShort())
        header.putLittleEndianInt(24, sampleRate)
        header.putLittleEndianInt(28, byteRate)
        header.putLittleEndianShort(32, (channels * bitsPerSample / 8).toShort())
        header.putLittleEndianShort(34, bitsPerSample.toShort())
        
        // 데이터 청크
        System.arraycopy("data".toByteArray(), 0, header, 36, 4)
        header.putLittleEndianInt(40, audioLength.toInt())
        
        output.write(header)
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

    private fun handleUnexpectedTermination(meetingId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. 녹음 중지
                stopRecording()
                
                // 2. 서버에 회의 종료 API 호출
                repository.endMeeting(meetingId).collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            // 3. 임시 파일이 있다면 WAV 변환 후 업로드
                            tempFile?.let { pcmFile ->
                                val wavFile = convertPcmToWav(pcmFile)
                                uploadRecoveredFile(meetingId, wavFile)
                            }
                        }
                        else -> {
                            Logger.e("비정상 종료 처리 중 회의 종료 실패", null)
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.e("비정상 종료 처리 실패", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        (_recordingState.value as? RecordingState.Recording)?.let { state ->
            handleUnexpectedTermination(state.meetingId)
        }
    }

    private fun convertPcmToWav(pcmFile: File): File {
        val wavFile = File(
            context.getExternalFilesDir(null),
            "recovered_${System.currentTimeMillis()}.wav"
        )
        
        FileOutputStream(wavFile).use { output ->
            writeWavHeader(output, pcmFile.length())
            pcmFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }
        return wavFile
    }

    private fun uploadRecoveredFile(meetingId: Long, wavFile: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.uploadMeetingRecord(meetingId, wavFile).collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            Logger.i("복구된 녹음 파일 업로드 성공")
                            wavFile.delete()
                            tempFile?.delete()
                            tempFile = null
                        }
                        is NetworkResult.Error -> {
                            Logger.e("복구된 녹음 파일 업로드 실패", null)
                        }
                        is NetworkResult.Loading -> {
                            _isLoading.value = true
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.e("복구된 녹음 파일 업로드 중 오류", e)
            }
        }
    }
}