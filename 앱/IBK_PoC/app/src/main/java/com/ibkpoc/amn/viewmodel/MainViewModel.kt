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

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MeetingRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Main())
    val currentScreen = _currentScreen.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _currentMeetingSession = MutableStateFlow<MeetingSession?>(null)
    val currentMeetingSession = _currentMeetingSession.asStateFlow()

    private val _endMeetingSuccess = MutableStateFlow(false)
    val endMeetingSuccess: StateFlow<Boolean> get() = _endMeetingSuccess

    private val _inputNumber = MutableStateFlow("")
    val inputNumber = _inputNumber.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted = _isMuted.asStateFlow()

    private val _hasRecordPermission = MutableStateFlow(false)
    val hasRecordPermission = _hasRecordPermission.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private val recordedData = mutableListOf<ByteArray>()
    private var isRecording = false

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val _isMuteLayoutVisible = MutableStateFlow(false)
    val isMuteLayoutVisible = _isMuteLayoutVisible.asStateFlow()

    private val _muteLayoutVisible = MutableStateFlow(false)
    val muteLayoutVisible = _muteLayoutVisible.asStateFlow()

    private var periodicSaveJob: Job? = null
    private var currentWavFile: String? = null


    init {
        // 초기 상태만 설정
        println("ViewModel initialized with isLoading: ${_isLoading.value}")
    }


    fun validateAndEnterMeeting(userId: Int, onNavigate: (Screen) -> Unit) {
        if (userId <= 0) {
            _errorMessage.value = "유효하지 않은 숫자입니다."
            return
        }
        _isLoading.value = true // 로딩 상태 시작
        viewModelScope.launch {
            repository.validateUser(userId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        if (result.data.isValid) {
                            if (result.data.active) {  // 이미 활성화된 사용자 체크
                                _errorMessage.value = "이미 다른 회의에 참여 중인 사용자입니다."
                                _isLoading.value = false
                            } else {
                                if (result.data.isMeetingActive) {
                                    joinMeeting(userId, onNavigate)
                                } else {
                                    startMeeting(userId, onNavigate)
                                }
                            }
                        } else {
                            _errorMessage.value = "유효하지 않은 사용자입니다."
                            _isLoading.value = false
                        }
                    }
                    is NetworkResult.Error -> {
                        _errorMessage.value = result.message
                        _isLoading.value = false
                    }
                    is NetworkResult.Loading -> {
                        // 로딩 상태를 별도로 처리하거나 미 처리한 상태 유지
                        _isLoading.value = true
                    }
                }
            }
        }
    }

    private fun handleMeetingStartOrJoin(
        userId: Int,
        response: NetworkResult<MeetingSession>,
        onNavigate: (Screen) -> Unit
    ) {
        when (response) {
            is NetworkResult.Success -> {
                val session = response.data
                initializeMeetingSession(session.convId, session.userId)

                // 성공 시 에러 메시지 초기화
                _errorMessage.value = null

                // 화면 전환은 즉시 실행
                _isLoading.value = false
                onNavigate(Screen.Meeting(session.convId, session.userId))
            }
            is NetworkResult.Error -> {
                _errorMessage.value = response.message
                _isLoading.value = false
            }
            is NetworkResult.Loading -> {
                _isLoading.value = true
            }
        }
    }
    private fun startMeeting(userId: Int, onNavigate: (Screen) -> Unit) {
        viewModelScope.launch {
            repository.startMeeting(MeetingStartRequest(userId)).collect { result ->
                handleMeetingStartOrJoin(userId, result, onNavigate)
            }
        }
    }

    private fun joinMeeting(userId: Int, onNavigate: (Screen) -> Unit) {
        viewModelScope.launch {
            repository.joinMeeting(MeetingJoinRequest(userId)).collect { result ->
                handleMeetingStartOrJoin(userId, result, onNavigate)
            }
        }
    }


    fun endMeeting(onNavigate: (Screen) -> Unit) {
        viewModelScope.launch {
            val session = _currentMeetingSession.value ?: run {
                Logger.e("회의 종료 실패: 현재 세션 null", null, context)
                return@launch
            }
            _isLoading.value = true  // 시작할 때 로딩 상태 설정
            val request = MeetingEndRequest(session.userId)
            repository.endMeeting(request).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        stopRecording()
                        _currentMeetingSession.value = null
                        println("회의 종료 성공: ${session.convId}")
                        _isLoading.value = false  // 성공 시 로딩 상태 해제
                        onNavigate(Screen.Main())
                    }
                    is NetworkResult.Error -> {
                        Logger.e("회의 종료 실패: ${result.message}", null, context)
                        _errorMessage.value = result.message
                        _isLoading.value = false  // 에러 시 로딩 상태 해제
                    }
                    is NetworkResult.Loading -> _isLoading.value = true
                }
            }
        }
    }

    fun initializeMeetingSession(meetingId: Long, userId: Int) {
        println("Mee: $meetingId, $userId")
        val session = MeetingSession(convId = meetingId, userId = userId)
        _currentMeetingSession.value = session
        println("Meeting session initialized: $session")
    }

    fun navigateTo(screen: Screen) {
        when (screen) {
            is Screen.List -> _currentScreen.value = Screen.List()
            is Screen.Main -> _currentScreen.value = Screen.Main()
            is Screen.Detail -> _currentScreen.value = Screen.Detail(
                meetingId = screen.meetingId,
                title = screen.title,
                start = screen.start,
                end = screen.end,
                participant = screen.participant,
                topic = screen.topic
            )
            is Screen.Meeting -> _currentScreen.value = Screen.Meeting(screen.meetingId)
        }
    }

    // 입력 처리 - 숫자만 입력되도록 수정
    fun onNumberInput(number: String) {
        if (number.all { it.isDigit() } || number.isEmpty()) {
            _inputNumber.value = number
        }
    }

    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }

    fun setHasRecordPermission(hasPermission: Boolean) {
        _hasRecordPermission.value = hasPermission
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }


    private fun startRecording() {
        if (!_hasRecordPermission.value) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        _errorMessage.value = "녹음 권한이 없습니다"
                        return@withContext
                    }
                }
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                currentWavFile = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/meeting_$timestamp.wav"

                val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
                
                noiseSuppressor = if (NoiseSuppressor.isAvailable()) {
                    NoiseSuppressor.create(audioRecord!!.audioSessionId).apply {
                        enabled = true
                    }
                } else null
                
                audioRecord?.startRecording()
                isRecording = true
                recordedData.clear()

                // 주기적 저장 작업 시작
                startPeriodicSaving()

                val audioData = ByteArray(bufferSize)
                val shortBuffer = ShortArray(bufferSize / 2)

                while (isRecording) {
                    val bytesRead = audioRecord?.read(audioData, 0, audioData.size) ?: -1
                    if (bytesRead > 0) {
                        // ByteArray를 ShortArray로 변환
                        for (i in 0 until bytesRead step 2) {
                            shortBuffer[i / 2] = ((audioData[i + 1].toInt() shl 8) or (audioData[i].toInt() and 0xFF)).toShort()
                        }

                        // DSP 처리 적용
                        val processedShortArray = if (_isMuted.value) {
                            ShortArray(shortBuffer.size) { 0 }
                        } else {
                            applyBandPassFilter(shortBuffer, sampleRate, 300f, 3000f)
                        }

                        // DSP 결과를 ByteArray로 변환
                        val processedByteArray = ByteArray(processedShortArray.size * 2)
                        for (i in processedShortArray.indices) {
                            processedByteArray[i * 2] = (processedShortArray[i].toInt() and 0xFF).toByte()
                            processedByteArray[i * 2 + 1] = (processedShortArray[i].toInt() shr 8 and 0xFF).toByte()
                        }

                        synchronized(recordedData) {
                            recordedData.add(processedByteArray)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Logger.e("녹음 시작 실패", e, context)
                }
            }
        }
    }

    fun startMeetingRecording(meetingId: Long, userId: Int) {
        if (!_hasRecordPermission.value) return

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        currentWavFile = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/${meetingId}_${userId}_$timestamp.wav"

        viewModelScope.launch(Dispatchers.IO) {
            startRecordingInternal()
        }
    }

    private suspend fun startRecordingInternal() {
        try {
            withContext(Dispatchers.Main) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    _errorMessage.value = "녹음 권한이 없습니다"
                    return@withContext
                }
            }

            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            noiseSuppressor = if (NoiseSuppressor.isAvailable()) {
                NoiseSuppressor.create(audioRecord!!.audioSessionId).apply {
                    enabled = true
                }
            } else null

            audioRecord?.startRecording()
            isRecording = true
            recordedData.clear()

            startPeriodicSaving()

            val audioData = ByteArray(bufferSize)
            val shortBuffer = ShortArray(bufferSize / 2)

            while (isRecording) {
                val bytesRead = audioRecord?.read(audioData, 0, audioData.size) ?: -1
                if (bytesRead > 0) {
                    for (i in 0 until bytesRead step 2) {
                        shortBuffer[i / 2] = ((audioData[i + 1].toInt() shl 8) or (audioData[i].toInt() and 0xFF)).toShort()
                    }

                    val processedShortArray = if (_isMuted.value) {
                        ShortArray(shortBuffer.size) { 0 }
                    } else {
                        applyBandPassFilter(shortBuffer, sampleRate, 300f, 3000f)
                    }

                    val processedByteArray = ByteArray(processedShortArray.size * 2)
                    for (i in processedShortArray.indices) {
                        processedByteArray[i * 2] = (processedShortArray[i].toInt() and 0xFF).toByte()
                        processedByteArray[i * 2 + 1] = (processedShortArray[i].toInt() shr 8 and 0xFF).toByte()
                    }

                    synchronized(recordedData) {
                        recordedData.add(processedByteArray)
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Logger.e("녹음 시작 실패", e, context)
            }
        }
    }

    private fun startPeriodicSaving() {
        periodicSaveJob?.cancel()
        periodicSaveJob = viewModelScope.launch(Dispatchers.IO) {
            while (isRecording) {
                delay(Config.RECORD_INTERVAL)
                saveCurrentRecording()
            }
        }
    }

    private fun saveCurrentRecording() {
        synchronized(recordedData) {
            if (recordedData.isEmpty() || currentWavFile == null) return

            try {
                FileOutputStream(currentWavFile!!).use { wavOutputStream ->
                    val totalAudioLen = recordedData.sumOf { it.size }.toLong()
                    writeWavHeader(wavOutputStream, totalAudioLen)

                    for (data in recordedData) {
                        wavOutputStream.write(data)
                    }
                }
                uploadCurrentRecording()
            } catch (e: Exception) {
                Logger.e("주기적 저장 실패", e, context)
            }
        }
    }

    private fun applyBandPassFilter(audioData: ShortArray, sampleRate: Int, lowFreq: Float, highFreq: Float): ShortArray {
        val filteredData = ShortArray(audioData.size)
        val lowPassCoeff = 2 * Math.PI * lowFreq / sampleRate
        val highPassCoeff = 2 * Math.PI * highFreq / sampleRate

        var prevSample = 0.0
        for (i in audioData.indices) {
            val sample = audioData[i] / 32768.0 // Normalize to [-1.0, 1.0]
            val lowPassed = sample - prevSample * lowPassCoeff
            val highPassed = lowPassed * highPassCoeff
            prevSample = sample

            // Convert back to 16-bit PCM
            filteredData[i] = (highPassed * 32768).toInt().toShort()
        }
        return filteredData
    }

    private fun stopRecording() {
        isRecording = false
        periodicSaveJob?.cancel()

        try {
            audioRecord?.stop()
            audioRecord?.release()
            noiseSuppressor?.release()
            
            // 녹음 파일 저장
            synchronized(recordedData) {
                if (recordedData.isNotEmpty() && currentWavFile != null) {
                    saveCurrentRecording()
                    // 저장 완료 후 파일 업로드
                    uploadMeetingRecord(currentWavFile!!)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = "녹음 종료 실패: ${e.message}"
        } finally {
            audioRecord = null
            noiseSuppressor = null
            recordedData.clear()
            currentWavFile = null
        }
    }

    /**
     * 음성 파일을 서버로 전송하는 함수
     * @param audioFilePath 전송할 음성 파일 경로
     * 현재는 회의 종료 시 자동으로 currentWavFile을 전송하며,
     * 추후 사용자가 직접 파일을 선택하여 전송할 수 있도록 확장 가능
     */
    private fun uploadMeetingRecord(audioFilePath: String) {
        viewModelScope.launch {
            try {
                val file = File(audioFilePath)
                repository.uploadMeetingRecord(0, file).collect { result -> // meetingId는 파일명에서 파싱되므로 0으로 전달
                    when (result) {
                        is NetworkResult.Success -> {
                            println("파일 업로드 성공: ${file.name}")
                        }
                        is NetworkResult.Error -> {
                            _errorMessage.value = "파일 업로드 실패: ${result.message}"
                        }
                        is NetworkResult.Loading -> {
                            // 로딩 상태 처리
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "파일 업로드 중 오류 발생: ${e.message}"
            }
        }
    }

    private fun uploadCurrentRecording() {
        currentWavFile?.let { filePath ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val file = File(filePath)
                    repository.uploadMeetingRecord(0, file).collect { result ->
                        when (result) {
                            is NetworkResult.Success -> {
                                println("파일 업로드 성공: ${file.name}")
                            }
                            is NetworkResult.Error -> {
                                Logger.e("파일 업로드 실패: ${result.message}", null, context)
                            }
                            is NetworkResult.Loading -> {
                                // 로딩 처리
                            }
                        }
                    }
                } catch (e: Exception) {
                    Logger.e("파일 업로드 중 오류: ${e.message}", e, context)
                }
            }
        }
    }

    private fun saveRecordingToFile() {
        viewModelScope.launch {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val wavFilePath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/meeting_$timestamp.wav"
                
                FileOutputStream(wavFilePath).use { wavOutputStream ->
                    // WAV 헤더 작성
                    val totalAudioLen = recordedData.sumOf { it.size }.toLong()
                    writeWavHeader(wavOutputStream, totalAudioLen)
                    
                    // 오디오 데이터 작성
                    for (data in recordedData) {
                        wavOutputStream.write(data)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "파일 저장 실패: ${e.message}"
            }
        }
    }

    private fun writeWavHeader(outputStream: FileOutputStream, totalAudioLen: Long) {
        val channels = 1
        val bitRate = 16
        val byteRate = (sampleRate * channels * bitRate / 8).toLong()
        val totalDataLen = totalAudioLen + 36

        val header = ByteArray(44)
        
        // RIFF 헤더
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        
        // WAVE 헤더
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        
        // fmt 청크
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16  // 서브청크 크기
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1   // PCM 오디오 포맷
        header[21] = 0
        header[22] = channels.toByte()  // 채널 수
        header[23] = 0
        
        // 샘플레이트
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        
        // 바이트레이트
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        
        // 블록 얼라인
        header[32] = (channels * bitRate / 8).toByte()
        header[33] = 0
        
        // 비트레이트
        header[34] = bitRate.toByte()
        header[35] = 0
        
        // 데이터 청크
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()
        
        outputStream.write(header)
    }

    fun showMuteLayout() {
        _isMuteLayoutVisible.value = true
    }

    fun hideMuteLayout() {
        _isMuteLayoutVisible.value = false
    }
}