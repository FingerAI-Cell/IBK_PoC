// api/ApiService.kt
package com.ibkpoc.amn.network

import com.ibkpoc.amn.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import retrofit2.http.GET
import retrofit2.http.Query
import okhttp3.ResponseBody

interface ApiService {
    @POST("/meeting/start")
    suspend fun startMeeting(
        @Body request: MeetingStartRequest
    ): Response<CommonResponse<MeetingSession>>

    @POST("/meeting/end")
    suspend fun endMeeting(
        @Body request: MeetingEndRequest
    ): Response<CommonResponse<Unit>>

    @Multipart
    @POST("/meeting/upload/chunk")
    suspend fun uploadWavChunk(
        @Part("meetingId") meetingId: RequestBody,
        @Part("startTime") startTime: RequestBody,
        @Part("duration") duration: RequestBody,
        @Part("currentChunk") currentChunk: RequestBody,
        @Part("totalChunks") totalChunks: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<CommonResponse<Unit>>

    @POST("/meeting/stt-request")
    suspend fun convertWavToStt(@Body request: SttRequest): Response<CommonResponse<Unit>>

    @GET("/meeting/list")
    suspend fun getMeetingList(): Response<CommonResponse<List<MeetingListResponse>>>

    @GET("stt/")
    suspend fun getAudioFile(
        @Query("audio_file_name") fileName: String
    ): ResponseBody
}