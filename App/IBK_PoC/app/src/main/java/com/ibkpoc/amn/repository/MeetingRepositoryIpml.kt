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

class MeetingRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : MeetingRepository {
    private val uploadMutex = Mutex()
    
    companion object {
        private const val CHUNK_SIZE = 5 * 1024 * 1024 // 5MB
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
            if (response.isSuccessful) {
                response.body()?.data?.let {
                    emit(NetworkResult.Success(it))
                } ?: emit(NetworkResult.Error(response.code(), "응답이 비어있습니다"))
            } else {
                Logger.e("회의 시작 실패: ${response.message()}", null)
                emit(NetworkResult.Error(response.code(), response.message()))
            }
        } catch (e: Exception) {
            Logger.e("회의 시작 중 오류 발생", e)
            emit(NetworkResult.Error(0, "회의 시작 중 오류 발생: ${e.message}"))
        }
    }

    override suspend fun endMeeting(meetingId: Long): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            val request = MeetingEndRequest(meetingId = meetingId)
            val response = apiService.endMeeting(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    if (it.status == "SUCCESS") {
                        emit(NetworkResult.Success(Unit))
                    } else {
                        emit(NetworkResult.Error(response.code(), it.message))
                    }
                } ?: emit(NetworkResult.Error(response.code(), "응답이 비어있습니다"))
            } else {
                Logger.e("회의 종료 실패: ${response.message()}", null)
                emit(NetworkResult.Error(response.code(), "회의 종료 실패"))
            }
        } catch (e: Exception) {
            Logger.e("회의 종료 중 오류 발생", e)
            emit(NetworkResult.Error(0, "회의 종료 중 오류 발생: ${e.message}"))
        }
    }

    override suspend fun uploadMeetingWavFile(wavUploadData: WavUploadData): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
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
                        
                        val chunkUploadData = wavUploadData.copy(
                            totalChunks = totalChunks.toInt(),
                            currentChunk = chunkNumber++,
                            chunkData = chunkData
                        )
                        
                        uploadChunk(chunkUploadData)
                        
                        Logger.i("업로드 진행률: ${(totalBytesUploaded * 100 / file.length())}%")
                    }
                }
                
                Logger.i("파일 업로드 완료")
            }
            emit(NetworkResult.Success(Unit))
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
            if (response.isSuccessful) {
                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error(response.code(), "STT 변환 실패: ${response.message()}"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(0, "STT 변환 중 오류 발생: ${e.message}"))
        }
    }


    private suspend fun uploadChunk(chunkData: WavUploadData) {
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
            
            try {
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
            } catch (e: Exception) {
                Logger.e("청크 업로드 중 오류", e)
                throw e
            }
        }
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())
    }
}