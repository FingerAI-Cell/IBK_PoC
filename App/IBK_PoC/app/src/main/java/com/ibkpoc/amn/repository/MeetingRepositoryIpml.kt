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

class MeetingRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : MeetingRepository {
    override suspend fun startMeeting(): Flow<NetworkResult<MeetingSession>> = flow {
        emit(NetworkResult.Loading)
        try {
            val request = MeetingStartRequest(startTime = getCurrentTime())
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
                emit(NetworkResult.Success(Unit))
            } else {
                Logger.e("회의 종료 실패: ${response.message()}", null)
                emit(NetworkResult.Error(response.code(), "회의 종료 실패"))
            }
        } catch (e: Exception) {
            Logger.e("회의 종료 중 오류 발생", e)
            emit(NetworkResult.Error(0, "회의 종료 중 오류 발생: ${e.message}"))
        }
    }

    override suspend fun uploadMeetingRecordChunk(recordingData: RecordingData): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            val meetingIdBody = recordingData.meetingId.toString()
                .toRequestBody("text/plain".toMediaTypeOrNull())
            val chunkStartTimeBody = recordingData.chunkStartTime.toString()
                .toRequestBody("text/plain".toMediaTypeOrNull())
            val durationBody = recordingData.duration.toString()
                .toRequestBody("text/plain".toMediaTypeOrNull())
            
            val fileBody = recordingData.audioData.toRequestBody("audio/pcm".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", "chunk_${recordingData.meetingId}.pcm", fileBody)

            val response = apiService.uploadMeetingRecordChunk(
                meetingId = meetingIdBody,
                chunkStartTime = chunkStartTimeBody,
                duration = durationBody,
                file = filePart
            )
            
            if (response.isSuccessful) {
                emit(NetworkResult.Success(Unit))
            } else {
                Logger.e("청크 업로드 실패: ${response.message()}", null)
                emit(NetworkResult.Error(response.code(), "청크 업로드 실패"))
            }
        } catch (e: Exception) {
            Logger.e("청크 업로드 중 오류 발생", e)
            emit(NetworkResult.Error(0, "청크 업로드 중 오류 발생: ${e.message}"))
        }
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())
    }
}