// repository/MeetingRepositoryImpl.kt
package com.ibkpoc.amn.repository

import com.ibkpoc.amn.model.*
import com.ibkpoc.amn.network.ApiService
import com.ibkpoc.amn.network.NetworkResult
import com.ibkpoc.amn.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

class MeetingRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : MeetingRepository {
    private val uploadMutex = Mutex()

    companion object {
        private const val CHUNK_SIZE = 1 * 1024 * 1024  // 1MB
    }

    override suspend fun startMeeting(participantCount: Int): Flow<NetworkResult<MeetingSession>> = flow {
        emit(NetworkResult.Loading)
        try {
            val request = MeetingStartRequest(
                startTime = getCurrentTime(),
                participantCount = participantCount
            )
            Logger.i("서버로 전달되는 참가자 수: ${request.participantCount}")
            val response = apiService.startMeeting(request)
            emit(when {
                response.isSuccessful -> {
                    response.body()?.data?.let { data ->
                        NetworkResult.Success(data)
                    } ?: NetworkResult.Error(response.code(), "응답이 비어있습니다")
                }
                else -> NetworkResult.Error(response.code(), response.message())
            })
        } catch (e: Exception) {
            Logger.e("회의 시작 중 오류 발생", e)
            emit(NetworkResult.Error(0, "회의 시작 중 오류 발생: ${e.message}"))
        }
    }.catch { e ->
        emit(NetworkResult.Error(0, e.message ?: "알 수 없는 오류가 발생했습니다"))
    }

    override suspend fun endMeeting(meetingId: Long): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            val request = MeetingEndRequest(meetingId = meetingId)
            val response = apiService.endMeeting(request)
            emit(when {
                response.isSuccessful -> {
                    response.body()?.let {
                        if (it.status == "SUCCESS") NetworkResult.Success(Unit)
                        else NetworkResult.Error(response.code(), it.message)
                    } ?: NetworkResult.Error(response.code(), "응답이 비어있습니다")
                }
                else -> NetworkResult.Error(response.code(), response.message())
            })
        } catch (e: Exception) {
            Logger.e("회의 종료 중 오류 발생", e)
            emit(NetworkResult.Error(0, "회의 종료 중 오류 발생: ${e.message}"))
        }
    }

    override suspend fun uploadMeetingWavFile(wavUploadData: WavUploadData): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        val failedChunks = mutableListOf<Int>()
        try {
            coroutineScope {
                val file = wavUploadData.wavFile
                val totalChunks = (file.length() + CHUNK_SIZE - 1) / CHUNK_SIZE

                Logger.i("파일 업로드 시작: 총 ${totalChunks}개 청크")

                file.inputStream().use { input ->
                    val buffer = ByteArray(CHUNK_SIZE)
                    var chunkNumber = 0
                    var totalBytesUploaded = 0L

                    while (true) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead <= 0) break

                        val chunkData = if (bytesRead == buffer.size) buffer else buffer.copyOf(bytesRead)
                        totalBytesUploaded += bytesRead

                        // 진행률 계산 및 방출
                        val progress = totalBytesUploaded.toFloat() / file.length()
                        emit(NetworkResult.FileUploadProgress(progress))

                        val chunkUploadData = wavUploadData.copy(
                            totalChunks = totalChunks.toInt(),
                            currentChunk = chunkNumber,
                            chunkData = chunkData
                        )
                        // 청크 업로드 시도
                        val isSuccess = uploadChunk(chunkUploadData)
                        if (!isSuccess) {
                            Logger.e("청크 업로드 실패: 청크 번호 $chunkNumber")
                            failedChunks.add(chunkNumber) // 실패한 청크 기록
                        } else {
                            Logger.i("청크 업로드 성공: $chunkNumber")
                        }

                        chunkNumber++
                        Logger.i("업로드 진행률: ${(totalBytesUploaded * 100 / file.length())}%")
                    }
                }

                if (failedChunks.isEmpty()) {
                    Logger.i("파일 업로드 완료: 모든 청크 성공적으로 업로드됨")
                    emit(NetworkResult.Success(Unit))
                } else {
                    Logger.e("파일 업로드 완료: 일부 청크 실패 (${failedChunks.size}개 실패)")
                    emit(
                        NetworkResult.Error(
                            0,
                            "파일 업로드 중 일부 청크 실패: 실패한 청크 번호 -> $failedChunks"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Logger.e("파일 업로드 실패", e)
            emit(NetworkResult.Error(0, "파일 업로드 중 오류: ${e.message}"))
        }
    }

    override suspend fun convertWavToStt(meetingId: Long): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            val request = SttRequest(meetingId)
            val response = apiService.convertWavToStt(request)
            emit(when {
                response.isSuccessful -> {
                    response.body()?.let {
                        NetworkResult.Success(Unit)
                    } ?: NetworkResult.Error(response.code(), "응답이 비어있습니다")
                }
                else -> NetworkResult.Error(response.code(), response.message())
            })
        } catch (e: Exception) {
            emit(NetworkResult.Error(0, "STT 변환 중 오류 발생: ${e.message}"))
        }
    }

    override suspend fun getMeetingList(): Flow<NetworkResult<List<MeetingListResponse>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.getMeetingList()
            emit(when {
                response.isSuccessful -> {
                    response.body()?.data?.let {
                        NetworkResult.Success(it)
                    } ?: NetworkResult.Error(response.code(), "응답이 비어있습니다")
                }
                else -> NetworkResult.Error(response.code(), response.message())
            })
        } catch (e: Exception) {
            Logger.e("회의 목록 조회 중 오류 발생", e)
            emit(NetworkResult.Error(0, "회의 목록 조회 중 오류 발생: ${e.message}"))
        }
    }

    private suspend fun uploadChunk(chunkData: WavUploadData, maxRetries: Int = 3): Boolean {
        var attempt = 0
        while (attempt < maxRetries) {
            try{
                uploadMutex.withLock {
                    val meetingIdBody = chunkData.meetingId.toString()
                        .toRequestBody("text/plain".toMediaTypeOrNull())
                    val startTimeBody = chunkData.startTime
                        .toRequestBody("text/plain".toMediaTypeOrNull())
                    val durationBody = chunkData.duration.toString()
                        .toRequestBody("text/plain".toMediaTypeOrNull())
                    val currentChunkBody = chunkData.currentChunk.toString()
                        .toRequestBody("text/plain".toMediaTypeOrNull())
                    val totalChunksBody = chunkData.totalChunks.toString()
                        .toRequestBody("text/plain".toMediaTypeOrNull())

                    val chunkPart = MultipartBody.Part.createFormData(
                        "file",
                        "chunk_${chunkData.currentChunk}.wav",
                        chunkData.chunkData.toRequestBody("audio/wav".toMediaTypeOrNull())
                    )

                    val response = apiService.uploadWavChunk(
                        meetingId = meetingIdBody,
                        startTime = startTimeBody,
                        duration = durationBody,
                        currentChunk = currentChunkBody,
                        totalChunks = totalChunksBody,
                        file = chunkPart
                    )
                    if (!response.isSuccessful) {
                        throw Exception("청크 업로드 실패: ${response.message()}")
                    }

                    Logger.i("청크 업로드 성공: ${chunkData.currentChunk}/${chunkData.totalChunks}")
                    return true
                }
            } catch (e: Exception) {
                Logger.e("청크 업로드 중 오류", e)
            }
            attempt++
            delay(2000L) // 재시도 대기
        }
        return false
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())
    }
}