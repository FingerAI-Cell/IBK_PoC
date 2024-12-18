// model/MeetingStartRequest.kt (추가)
package com.ibkpoc.amn.model

data class MeetingStartRequest(
    val startTime: String,
    val participantCount: Int
)