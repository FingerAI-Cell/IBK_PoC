// api/ApiService.kt
package com.ibkpoc.amn.network

import com.ibkpoc.amn.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("meeting/start")
    suspend fun startMeeting(
        @Body request: MeetingStartRequest
    ): Response<CommonResponse<MeetingSession>>

    @POST("meeting/end")
    suspend fun endMeeting(
        @Body request: MeetingEndRequest
    ): Response<CommonResponse<Unit>>

    @Multipart
    @POST("meeting/upload")
    suspend fun uploadMeetingRecord(
        @Part file: MultipartBody.Part
    ): Response<CommonResponse<Unit>>
}