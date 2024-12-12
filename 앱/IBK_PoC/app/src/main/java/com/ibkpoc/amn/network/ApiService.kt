// api/ApiService.kt
package com.ibkpoc.amn.network

import com.ibkpoc.amn.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/user/validate")
    suspend fun validateUser(
        @Body request: UserValidationRequest
    ): Response<CommonResponse<UserValidationResponse>>

    @POST("api/meeting/start")
    suspend fun startMeeting(
        @Body request: MeetingStartRequest
    ): Response<CommonResponse<MeetingSession>>

    @POST("api/meeting/join")
    suspend fun joinMeeting(
        @Body request: MeetingJoinRequest
    ): Response<CommonResponse<MeetingSession>>

    @POST("api/meeting/end")
    suspend fun endMeeting(
        @Body request: MeetingEndRequest
    ): Response<CommonResponse<Unit>>

    @GET("api/meeting/list")
    suspend fun getMeetingList(): Response<CommonResponse<List<MeetingListResponse>>>

    @POST("api/meeting/details")
    suspend fun getMeetingDetail(
        @Body request: MeetingDetailRequest
    ): Response<CommonResponse<MeetingDetailResponse>>

    @Multipart
    @POST("api/meeting/upload-record")
    suspend fun uploadMeetingRecord(
        @Part file: MultipartBody.Part
    ): Response<CommonResponse<Unit>>
}