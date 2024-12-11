// repository/MeetingRepositoryImpl.kt
package com.ibkpoc.amn.repository

import com.ibkpoc.amn.model.*
import com.ibkpoc.amn.network.ApiService
import com.ibkpoc.amn.network.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class MeetingRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : MeetingRepository {
    override suspend fun validateUser(userId: Int): Flow<NetworkResult<UserValidationResponse>> = flow {
        emit(NetworkResult.Loading)
        val request = UserValidationRequest(userId)
        val response = apiService.validateUser(request)
        if (response.isSuccessful) {
            val body = response.body()
            if (body?.status == "SUCCESS" && body.data != null) {
                emit(NetworkResult.Success(body.data))
            } else {
                emit(NetworkResult.Error(response.code(), body?.message ?: "유효하지 않은 사용자입니다"))
            }
        } else {
            emit(NetworkResult.Error(response.code(), response.message()))
        }
    }.catch { e ->
        println("Error in validateUser: ${e.message}")
    }

    override suspend fun startMeeting(request: MeetingStartRequest): Flow<NetworkResult<MeetingSession>> = flow {
        emit(NetworkResult.Loading)
        val response = apiService.startMeeting(request)
        if (response.isSuccessful) {
            response.body()?.data?.let {
                emit(NetworkResult.Success(it))
            } ?: emit(NetworkResult.Error(response.code(), "응답이 비어있습니다"))
        } else {
            emit(NetworkResult.Error(response.code(), response.message()))
        }
    }.catch { e ->
        println("Error in startMeeting: ${e.message}")
    }


    override suspend fun endMeeting(request: MeetingEndRequest): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        val response = apiService.endMeeting(request)
        if (response.isSuccessful) {
            response.body()?.let {
                emit(NetworkResult.Success(Unit))
            } ?: emit(NetworkResult.Error(response.code(), "응답이 비어있습니다"))
        } else {
            emit(NetworkResult.Error(response.code(), response.message()))
        }
    }.catch { e ->
        println("Error in endMeeting: ${e.message}")
    }

    override suspend fun joinMeeting(request: MeetingJoinRequest): Flow<NetworkResult<MeetingSession>> = flow {
        emit(NetworkResult.Loading)
        val response = apiService.joinMeeting(request)
        if (response.isSuccessful) {
            response.body()?.data?.let {
                emit(NetworkResult.Success(it))
            } ?: emit(NetworkResult.Error(response.code(), "응답이 비어있습니다"))
        } else {
            emit(NetworkResult.Error(response.code(), response.message()))
        }
    }.catch { e ->
        println("Error in joinMeeting: ${e.message}")
    }

    override suspend fun getMeetingList(): Flow<NetworkResult<List<MeetingListResponse>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.getMeetingList()
            val result = response.body()
            if (response.isSuccessful && result?.status == "SUCCESS") {
                result.data?.let { data ->
                    emit(NetworkResult.Success(data))
                } ?: emit(NetworkResult.Success(emptyList()))
            } else {
                emit(NetworkResult.Error(response.code(), result?.message ?: "오류가 발생했습니다"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(NetworkResult.Error(0, "네트워크 오류가 발생했습니다: ${e.message}"))
        }
    }.catch { e ->
        e.printStackTrace()
        emit(NetworkResult.Error(0, "데이터 처리 중 오류가 발생했습니다: ${e.message}"))
    }

    override suspend fun getMeetingDetail(id: Long): Flow<NetworkResult<MeetingDetailResponse>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.getMeetingDetail(MeetingDetailRequest(id))
            val result = response.body()
            if (response.isSuccessful && result?.status == "SUCCESS") {
                result.data?.let {
                    emit(NetworkResult.Success(it))
                } ?: emit(NetworkResult.Error(response.code(), "데이터가 없습니다"))
            } else {
                emit(NetworkResult.Error(response.code(), result?.message ?: "오류가 발생했습니다"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(0, "네트워크 오류가 발생했습니다"))
        }
    }

    // MeetingRepositoryImpl.kt
    override suspend fun uploadMeetingRecord(meetingId: Long, audioFile: File): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            val requestFile = audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)

            val response = apiService.uploadMeetingRecord(body)
            if (response.isSuccessful) {
                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error(response.code(), "파일 업로드 실패"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(0, "파일 업로드 중 오류 발생: ${e.message}"))
        }
    }
}