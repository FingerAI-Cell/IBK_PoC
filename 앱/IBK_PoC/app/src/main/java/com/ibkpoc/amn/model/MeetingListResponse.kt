// model/MeetingListResponse.kt (수정)
package com.ibkpoc.amn.model

data class MeetingListResponse(
    val id: Long,
    val title: String,
    val start: String,
    val end: String,
    val participant: Int,
    val topic: String?
)