// repository/MeetingRepository.kt
package com.ibkpoc.amn.repository

import com.ibkpoc.amn.model.*
import com.ibkpoc.amn.network.ApiService
import com.ibkpoc.amn.network.NetworkResult

import kotlinx.coroutines.flow.Flow
import java.io.File

interface MeetingRepository {
    suspend fun startMeeting(participantCount: Int): Flow<NetworkResult<MeetingSession>>
    suspend fun endMeeting(meetingId: Long): Flow<NetworkResult<Unit>>
    suspend fun uploadMeetingWavFile(uploadData: UploadData): Flow<NetworkResult<Unit>>
    suspend fun convertWavToStt(request: SttRequest): Flow<NetworkResult<Unit>>
    suspend fun getMeetingList(): Flow<NetworkResult<List<MeetingListResponse>>>
    suspend fun requestSpeakerSeparation(meetingId: Long): Flow<NetworkResult<Unit>>
}