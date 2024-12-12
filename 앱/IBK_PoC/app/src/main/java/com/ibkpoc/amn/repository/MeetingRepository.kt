// repository/MeetingRepository.kt
package com.ibkpoc.amn.repository

import com.ibkpoc.amn.model.*
import com.ibkpoc.amn.network.NetworkResult
import kotlinx.coroutines.flow.Flow
import java.io.File

interface MeetingRepository {
    suspend fun validateUser(userId: Int): Flow<NetworkResult<UserValidationResponse>>
    suspend fun startMeeting(request: MeetingStartRequest): Flow<NetworkResult<MeetingSession>>
    suspend fun endMeeting(request: MeetingEndRequest): Flow<NetworkResult<Unit>>
    suspend fun joinMeeting(request: MeetingJoinRequest): Flow<NetworkResult<MeetingSession>>
    suspend fun getMeetingList(): Flow<NetworkResult<List<MeetingListResponse>>>
    suspend fun getMeetingDetail(id: Long): Flow<NetworkResult<MeetingDetailResponse>>
    suspend fun uploadMeetingRecord(meetingId: Long, audioFile: File): Flow<NetworkResult<Unit>>

}