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
import android.net.Uri
import android.provider.MediaStore
import android.content.ContentValues
import androidx.core.net.toUri

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
    private val audioBuffer = mutableListOf<ByteArray>()
    private var recordingDurationJob: Job? = null

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

    private var currentRecordFile: Uri? = null
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
        val dataCopy: ByteArray
        synchronized(audioBuffer) {
            audioBuffer.add(data)
            dataCopy = data.copyOf()
        }
        viewModelScope.launch {
            savePcmData(dataCopy)
        }
    }

    private suspend fun savePcmData(data: ByteArray) {
        try {
            currentRecordFile?.let { uri ->
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wa")?.use { output ->
                        output.write(data)
                        output.flush()
                    } ?: throw IOException("스트림을 열 수 없습니다")
                }
            } ?: throw IOException("파일 URI가 null입니다")
        } catch (e: Exception) {
            Logger.e("파일 저장 실패: ${e.message}")
            _errorMessage.value = "녹음 파일 저장 실패"
        }
    }

    private fun saveBufferToFile() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val chunksToSave = mutableListOf<ByteArray>()
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                synchronized(bufferLock) {
                    chunksToSave.addAll(audioBuffer)
                    audioBuffer.clear()
                }
                chunksToSave.forEach { chunk ->
                    savePcmData(chunk)
                }
            } catch (e: Exception) {
                _errorMessage.value = "파일 저장 실패: ${e.message}"
            }
        }
    }

    private suspend fun createRecordFile(meetingId: Long, startTime: String): Uri? {
        return withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "record_${meetingId}_${startTime}.pcm")
                    put(MediaStore.Downloads.MIME_TYPE, "audio/x-pcm")
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/IBK_Records")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                
                context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?.also { _ ->
                    Logger.e("파일 생성 시도 성공")
                } ?: run {
                    Logger.e("파일 생성 실패: insert returned null")
                    throw IOException("파일 생성 실패")
                }
            } else {
                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "IBK_Records/record_${meetingId}_${startTime}.pcm"
                ).apply {
                    parentFile?.mkdirs()
                    createNewFile()
                }
                Logger.e("Android 9 이하 - 파일 생성됨: ${file.absolutePath}")
                Uri.fromFile(file)
            }
        }
    }

    private suspend fun createWavFile(meetingId: Long, startTime: String): Uri? {
        return withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "meeting_${meetingId}_${startTime}.wav")
                    put(MediaStore.Downloads.MIME_TYPE, "audio/wav")
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/IBK_Records")
                }
                Logger.e("Android 10 이상 - WAV 파일 생성 시도: Download/IBK_Records/meeting_${meetingId}_${startTime}.wav")
                
                context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?.also { uri ->
                    Logger.e("WAV 파일 URI 생성됨: $uri")
                    Logger.e("WAV 파일 실제 경로: ${uri.path}")
                    uri.path ?: throw IllegalStateException("URI path is null")
                }
            } else {
                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "IBK_Records/meeting_${meetingId}_${startTime}.wav"
                ).apply {
                    parentFile?.mkdirs()
                    createNewFile()
                }
                Logger.e("Android 9 이하 - WAV 파일 생성됨: ${file.absolutePath}")
                Uri.fromFile(file)
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
                            putExtra(AudioRecordService.EXTRA_FILE_URI, currentRecordFile.toString())
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
                val pcmUri = currentRecordFile ?: throw Exception("녹음 파일이 없습니다")
                val wavUri = createWavFile(currentState.meetingId, currentState.startTime) 
                    ?: throw Exception("WAV 파일을 생성할 수 없습니다")
                
                convertPcmToWav(pcmUri, wavUri)

                // Uri에서 실제 File 객체를 얻기
                val wavFile = getFileFromUri(wavUri) ?: throw IOException("WAV 파일을 찾을 수 없습니다")
                
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
                uploadWavAndConvertToStt(
                    currentState.meetingId,
                    wavFile,
                    currentState.startTime,
                    currentState.duration
                )

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
        recordingDurationJob?.cancel()
        currentRecordFile = null  // 녹음 종료시 파일 참조 제거
    }

    private suspend fun convertPcmToWav(pcmUri: Uri, wavUri: Uri) {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(pcmUri)?.use { input ->
                context.contentResolver.openOutputStream(wavUri)?.use { output ->
                    // WAV 헤더 작성
                    val pcmLength = input.available().toLong()
                    writeWavHeader(output, pcmLength)
                    
                    // 청크 단위로 복사
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
        }
    }

    // 백그라운드/포그라운��� 전환 시에는 아무 작업도 하지 않음
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

    // Uri에서 실제 File 객체를 얻는 메서드 추가
    private fun getFileFromUri(uri: Uri): File? {
        return when {
            // Android 10 이상
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                try {
                    // 임시 파일 생성
                    val tempFile = File(context.cacheDir, "temp_wav_file.wav")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile
                } catch (e: Exception) {
                    Logger.e("파일 변환 실패: ${e.message}")
                    null
                }
            }
            // Android 9 이하
            else -> {
                try {
                    uri.path?.let { File(it) }
                } catch (e: Exception) {
                    Logger.e("파일 경로 변환 실패: ${e.message}")
                    null
                }
            }
        }
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
            .format(Date())
    }
}