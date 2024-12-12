// model/MeetingSession.kt (추가)
package com.ibkpoc.amn.model

data class MeetingSession(
    val convId: Long,
    val userId: Int,
    val startTime: String
)