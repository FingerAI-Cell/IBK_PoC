// repository/MeetingRepository.kt
package com.ibkpoc.amn.repository

import com.ibkpoc.amn.model.*
import com.ibkpoc.amn.network.NetworkResult
import kotlinx.coroutines.flow.Flow
import java.io.File

interface MeetingRepository {
    suspend fun startMeeting(): Flow<NetworkResult<MeetingSession>>
    suspend fun endMeeting(meetingId: Long): Flow<NetworkResult<Unit>>
    suspend fun uploadMeetingRecord(meetingId: Long, audioFile: File): Flow<NetworkResult<Unit>>
}