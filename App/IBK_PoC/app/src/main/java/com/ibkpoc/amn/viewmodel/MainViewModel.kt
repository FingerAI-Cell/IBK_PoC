package com.ibkpoc.amn.viewmodel

import android.media.AudioFormat
import android.media.AudioRecord
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
import com.ibkpoc.amn.event.EventBus
import com.ibkpoc.amn.event.RecordingStateEvent
import android.media.MediaPlayer
import com.ibkpoc.amn.network.ApiService
import android.app.Application

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MeetingRepository,
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val application: Application
) : ViewModel() {
    private var recordingTimer: Job? = null
    private var lastRecordingState: RecordingStateInfo? = null
    private var lastFileName: String? = null
    private var pendingAudioFile: String? = null

    private val _recordingState = MutableStateFlow<RecordServiceState>(RecordServiceState.Idle)
    val recordingState = _recordingState.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime = _elapsedTime.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _showSuccessMessage = MutableStateFlow(false)
    val showSuccessMessage = _showSuccessMessage.asStateFlow()

    private var recordingJob: Job? = null

    private var fileOutputStream: FileOutputStream? = null

    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    var lastRequestedFile: String? = null

    private val _currentPosition = MutableStateFlow(0f)
    val currentPosition: StateFlow<Float> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0f)
    val duration: StateFlow<Float> = _duration.asStateFlow()

    private val _showPlayer = MutableStateFlow(false)
    val showPlayer: StateFlow<Boolean> = _showPlayer.asStateFlow()

    private val _currentFileName = MutableStateFlow<String?>(null)
    val currentFileName: StateFlow<String?> = _currentFileName.asStateFlow()

    init {
        viewModelScope.launch {
            EventBus.subscribe<RecordingStateEvent>().collect { event ->
                when (event.state) {
                    is RecordServiceState.AllTasksCompleted -> {
                        Logger.i("모든 작업 완료 이벤트 수신: 화자 구분 요청 시작")
                        requestSpeakerSeparation(event.state.meetingId)
                    }
                    else -> { Logger.e("상태 이벤트 수신: ${event.state}")
                        handleRecordServiceState(event.state)}
                }

            }
        }
        viewModelScope.launch {
            while (true) {
                if (_isPlaying.value) {
                    mediaPlayer?.let { player ->
                        _currentPosition.value = player.currentPosition.toFloat()
                    }
                }
                delay(100) // 100ms마다 업데이트
            }
        }
    }

    private fun handleRecordServiceState(state: RecordServiceState) {
        Logger.e("[상태처리] 새로운 상태 수신: $state")
        when (state) {
            is RecordServiceState.Recording -> {
                Logger.e("[상태처리] 녹음 시작 - 회의ID: ${state.meetingId}, 시작시간: ${state.startTime}")
                lastRecordingState = RecordingStateInfo(
                    meetingId = state.meetingId,
                    startTime = state.startTime,
                    duration = state.duration
                )
                startRecordingTimer()
                _recordingState.value = state
            }
            is RecordServiceState.Completed -> {
                Logger.e("[상태처리] 녹음 완료 - 파일경로: ${state.filePath}")
                stopRecordingTimer()
                _recordingState.value = RecordServiceState.Idle
                _isLoading.value = false
                viewModelScope.launch {
                    handleRecordingComplete(File(state.filePath))
                }
            }
            is RecordServiceState.Error -> {
                Logger.e("[상태처리] 에러 발생: ${state.message}")
                stopRecordingTimer()
                _errorMessage.value = state.message
                _recordingState.value = RecordServiceState.Idle
                _isLoading.value = false
            }
            is RecordServiceState.AllTasksCompleted -> {
                Logger.e("[상태처리] 모든 작업 완료 - 회의ID: ${state.meetingId}")
                viewModelScope.launch {
                    requestSpeakerSeparation(state.meetingId)
                }
            }
            RecordServiceState.Idle -> {
                Logger.e("[상태처리] 대기 상태로 전환")
                stopRecordingTimer()
                _recordingState.value = state
                _isLoading.value = false
            }
        }
    }

    private fun startRecordingTimer() {
        recordingJob?.cancel()
        recordingJob = viewModelScope.launch {
            var seconds = 0L
            while (true) {
                _elapsedTime.value = seconds++
                delay(1000)
            }
        }
    }

    private fun stopRecordingTimer() {
        recordingJob?.cancel()
        recordingJob = null
        _elapsedTime.value = 0
    }

    private suspend fun handleRecordingComplete(pcmFile: File) {
        try {
            val recordingInfo = lastRecordingState
            if (recordingInfo == null) {
                Logger.e("[녹음완료처리] 이전 Recording 상태 정보 없음")
                _errorMessage.value = "녹음 정보를 찾을 수 없습니다"
                _recordingState.value = RecordServiceState.Idle
                return
            }

            // 즉시 Idle 상태로 전환하고 성공 메시지 표시
            _recordingState.value = RecordServiceState.Idle
            
            // 2초 후 성공 메시지 숨기기
            viewModelScope.launch {
                delay(2000)
            }
        } catch (e: Exception) {
            Logger.e("[녹음완료처리] 오류 발생: ${e.message}")
            _errorMessage.value = "녹음 처리 실패: ${e.message}"
            _recordingState.value = RecordServiceState.Idle
        }
    }

    private suspend fun createRecordFile(meetingId: Long, startTime: String): File {
        Logger.i("PCM 파일 생성 시작: meetingId=$meetingId, startTime=$startTime")
        return withContext(Dispatchers.IO) {
            File(audioDir, "record_${meetingId}_${startTime}.pcm").apply {
                parentFile?.mkdirs()
                createNewFile()
            }
        }.also { Logger.i("PCM 파일 생성 완료: ${it.absolutePath}") }
    }

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

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _hasRecordPermission = MutableStateFlow(false)
    val hasRecordPermission = _hasRecordPermission.asStateFlow()

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
        recordingJob?.cancel()
        try {
            fileOutputStream?.close()
            fileOutputStream = null
        } catch (e: Exception) {
            Logger.e("파일 스트림 닫기 실패", e)
        }
        currentRecordFile = null
        _recordingState.value = RecordServiceState.Idle
    }

//    private suspend fun convertPcmToWav(pcmFile: File): File {
//        val wavFile = File(audioDir, pcmFile.name.replace(".pcm", ".wav"))
//        withContext(Dispatchers.IO) {
//            FileInputStream(pcmFile).use { input ->
//                FileOutputStream(wavFile).use { output ->
//                    // WAV 헤더 작성
//                    writeWavHeader(output, pcmFile.length())
//
//                    // PCM 데이터 복사
//                    val buffer = ByteArray(8192)
//                    var bytesRead: Int
//                    while (input.read(buffer).also { bytesRead = it } != -1) {
//                        output.write(buffer, 0, bytesRead)
//                    }
//                }
//            }
//        }
//        return wavFile
//    }

    // 백그라운드/포그라운 전환 시에는 아무 작업도 하지 않음
    fun onAppBackgrounded() {}
    fun onAppForegrounded() {}

    fun onMessageShown() {
        _errorMessage.value = null
    }

    fun handleMeetingEndSuccess() {
        viewModelScope.launch {
            viewModelScope.launch {
                delay(2000) // 2초 후
            }
        }
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
            .format(Date())
    }

    fun startMeeting(participantCount: Int) {
        if (participantCount <= 0) {
            _errorMessage.value = "참가자 수는 1명 이상이어야 합니다"
            return
        }
        Logger.i("MainViewModel 전달받은 참가자 수: $participantCount")

        viewModelScope.launch {
            val startTime = getCurrentTime()
            
            repository.startMeeting(participantCount).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        // 녹음 파일 생성
                        currentRecordFile = createRecordFile(result.data.convId, startTime)
                        
                        // 서비스 시작
                        val serviceIntent = Intent(context, AudioRecordService::class.java).apply {
                            action = AudioRecordService.ACTION_START
                            putExtra(AudioRecordService.EXTRA_MEETING_ID, result.data.convId)
                            putExtra(AudioRecordService.EXTRA_START_TIME, startTime)
                            putExtra(AudioRecordService.EXTRA_FILE_PATH, currentRecordFile?.absolutePath)
                        }
                        
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                        
                        _recordingState.value = RecordServiceState.Recording(
                            meetingId = result.data.convId,
                            startTime = startTime
                        )
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
        val currentState = recordingState.value as? RecordServiceState.Recording ?: return
        
        viewModelScope.launch {
            _isLoading.value = true  // 로딩 시작
            repository.endMeeting(currentState.meetingId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        // 회의 종료 API 성공 시에만 성공 메시지 표시
                        _showSuccessMessage.value = true
                        viewModelScope.launch {
                            delay(2000)
                            _showSuccessMessage.value = false
                        }
                        
                        // 녹음 중지 요청
                        val serviceIntent = Intent(context, AudioRecordService::class.java).apply {
                            action = AudioRecordService.ACTION_STOP
                        }
                        context.startService(serviceIntent)

                        requestSpeakerSeparation(currentState.meetingId)
                    }
                    is NetworkResult.Error -> {
                        _errorMessage.value = "회의 종료 실패: ${result.message}"
                        _isLoading.value = false  // 에러 시 로딩 종료
                    }
                    is NetworkResult.Loading -> _isLoading.value = true
                }
            }
        }
    }

    fun requestSpeakerSeparation(meetingId: Long) {
        viewModelScope.launch {
            repository.requestSpeakerSeparation(meetingId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        Logger.i("화자 구분 요청 성공: 회의 ID $meetingId")
                        _showSuccessMessage.value = true
                    }
                    is NetworkResult.Error -> {
                        Logger.e("화자 구분 요청 실패: ${result.message}")
                        _errorMessage.value = "화자 구분 요청 실패: ${result.message}"
                    }
                    is NetworkResult.Loading -> {
                        Logger.i("화자 구분 요청 진행 중...")
                        _isLoading.value = true
                    }
                }
                _isLoading.value = false
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        cleanupRecording()
        mediaPlayer?.release()
        mediaPlayer = null
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

    // Recording 상태 정보를 저장하기 위한 데이터 클래스
    private data class RecordingStateInfo(
        val meetingId: Long,
        val startTime: String,
        val duration: Long
    )

    fun playAudioFile(fileName: String) {
        viewModelScope.launch {
            try {
                // 기존 MediaPlayer 해제
                mediaPlayer?.release()
                
                // 다운로드 디렉토리에서 파일 찾기
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val audioFile = if (fileName.contains("/")) {
                    // 서브디렉토리가 포함된 경우
                    File(downloadDir, fileName)
                } else {
                    // 다운로드 디렉토리 직접 접근
                    File(downloadDir, fileName)
                }
                
                if (!audioFile.exists()) {
                    _errorMessage.value = "파일을 찾을 수 없습니다: $fileName"
                    return@launch
                }

                // 새로운 MediaPlayer 생성 및 재생
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(audioFile.path)
                    setOnPreparedListener {
                        start()
                        _isPlaying.value = true
                        _duration.value = duration.toFloat()
                        _currentFileName.value = fileName
                        _showPlayer.value = true
                    }
                    setOnCompletionListener {
                        _isPlaying.value = false
                    }
                    setOnErrorListener { _, what, extra ->
                        _errorMessage.value = "재생 중 오류 발생: $what, $extra"
                        _isPlaying.value = false
                        true
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                _errorMessage.value = "음성 파일 재생 중 오류가 발생했습니다: ${e.message}"
                _isPlaying.value = false
            }
        }
    }

    fun seekTo(position: Float) {
        mediaPlayer?.seekTo(position.toInt())
    }

    fun forward10Seconds() {
        mediaPlayer?.let { player ->
            val newPosition = minOf(player.currentPosition + 10000, player.duration)
            player.seekTo(newPosition)
        }
    }

    fun rewind10Seconds() {
        mediaPlayer?.let { player ->
            val newPosition = maxOf(player.currentPosition - 10000, 0)
            player.seekTo(newPosition)
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
            } else {
                player.start()
                _isPlaying.value = true
            }
        }
    }

    fun hidePlayer() {
        _showPlayer.value = false
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
    }

    fun setPendingAudioFile(fileName: String) {
        pendingAudioFile = fileName
    }

    fun playPendingAudio() {
        pendingAudioFile?.let { fileName ->
            playAudioFile(fileName)
            pendingAudioFile = null
        }
    }
}