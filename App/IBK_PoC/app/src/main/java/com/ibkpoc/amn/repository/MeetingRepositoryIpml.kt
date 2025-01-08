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
        private const val CHUNK_SIZE = 4 * 1024 * 1024  // 4MB
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

    override suspend fun uploadMeetingWavFile(uploadData: UploadData): Flow<NetworkResult<Unit>> = flow {
        Logger.i("파일 업로드 에밋")
        emit(NetworkResult.Loading)
        Logger.i("파일 업로드 에밋 종료")
        try {
            coroutineScope {
                val totalChunks = (uploadData.chunkData.size + CHUNK_SIZE - 1) / CHUNK_SIZE
                val chunksPerSection = 7
                val startChunk = (uploadData.sectionNumber - 1) * chunksPerSection
                val endChunk = (startChunk + chunksPerSection).coerceAtMost(totalChunks)

                Logger.i("파일 업로드 시작: 총 ${totalChunks}개 청크")

                var totalBytesUploaded = 0L

                for (chunkNumber in startChunk until endChunk) {
                    val chunkStart = chunkNumber * CHUNK_SIZE
                    val chunkEnd = (chunkStart + CHUNK_SIZE).coerceAtMost(uploadData.chunkData.size)
                    val chunkData = uploadData.chunkData.copyOfRange(chunkStart, chunkEnd)

                    totalBytesUploaded += chunkData.size

                    val chunkUploadData = WavUploadData(
                        meetingId = uploadData.meetingId,
                        sectionNumber = uploadData.sectionNumber,
                        startTime = uploadData.startTime,
                        chunkData = chunkData,
                        totalChunks = totalChunks, // 포함
                        currentChunks = chunkNumber
                    )

                    uploadChunk(chunkUploadData)

                    Logger.i("섹션 진행률: ${(chunkNumber - startChunk + 1) * 100 / (endChunk - startChunk)}%")
                }

                Logger.i("섹션 업로드 완료: 섹션 ${uploadData.sectionNumber}")
                emit(NetworkResult.Success(Unit))
            }
        } catch (e: Exception) {
            Logger.e("섹션 업로드 실패", e)
            emit(NetworkResult.Error(0, "섹션 업로드 중 오류: ${e.message}"))
        }
    }

    override suspend fun convertWavToStt(request: SttRequest): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
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

    override suspend fun getMeetingList(): Flow<NetworkResult<List<MeetingListResponse>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.getMeetingList()
            if (response.isSuccessful) {
                response.body()?.data?.let {
                    emit(NetworkResult.Success(it))
                } ?: emit(NetworkResult.Error(response.code(), "응답이 비어있습니다"))
            } else {
                Logger.e("회의 목록 조회 실패: ${response.message()}", null)
                emit(NetworkResult.Error(response.code(), response.message()))
            }
        } catch (e: Exception) {
            Logger.e("회의 목록 조회 중 오류 발생", e)
            emit(NetworkResult.Error(0, "회의 목록 조회 중 오류 발생: ${e.message}"))
        }
    }

    private suspend fun uploadChunk(chunkData: WavUploadData) {
        uploadMutex.withLock {
            val meetingIdBody = chunkData.meetingId.toString()
                .toRequestBody("text/plain".toMediaTypeOrNull())
            val sectionNumberBody = chunkData.sectionNumber.toString()
                .toRequestBody("text/plain".toMediaTypeOrNull())
            val startTimeBody = chunkData.startTime
                .toRequestBody("text/plain".toMediaTypeOrNull())
            val currentChunkBody = chunkData.currentChunks.toString()
                .toRequestBody("text/plain".toMediaTypeOrNull())
            val totalChunksBody = chunkData.totalChunks.toString()
                .toRequestBody("text/plain".toMediaTypeOrNull())

            // 청크 데이터 생성
            val chunkPart = MultipartBody.Part.createFormData(
                "chunkData",
                "chunk_${chunkData.sectionNumber}.bin", // 필요 시 파일 이름
                chunkData.chunkData.toRequestBody("application/octet-stream".toMediaTypeOrNull())
            )

            try {
                Logger.i("청크 업로드 시작: sectionNumber=${chunkData.sectionNumber}")
                val response = apiService.uploadWavChunk(
                    meetingId = meetingIdBody,
                    sectionNumber = sectionNumberBody,
                    startTime = startTimeBody,
                    currentChunk = currentChunkBody, // currentChunk 추가
                    totalChunks = totalChunksBody,   // totalChunks 추가
                    chunkData = chunkPart
                )

                if (!response.isSuccessful) {
                    throw Exception("청크 업로드 실패: ${response.message()}")
                }

                Logger.i("청크 업로드 성공: 섹션 ${chunkData.sectionNumber}")
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

    override suspend fun requestSpeakerSeparation(meetingId: Long): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            val request = SpeakerSeparationRequest(meetingId)
            val response = apiService.requestSpeakerSeparation(request)
            if (response.isSuccessful) {
                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error(response.code(), "화자 구분 요청 실패: ${response.message()}"))
            }
        } catch (e: Exception) {
            Logger.e("화자 구분 요청 중 오류 발생", e)
            emit(NetworkResult.Error(0, "화자 구분 요청 중 오류: ${e.message}"))
        }
    }

}