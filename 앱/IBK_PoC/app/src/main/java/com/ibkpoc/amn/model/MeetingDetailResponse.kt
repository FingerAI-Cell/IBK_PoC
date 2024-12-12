// model/MeetingDetailResponse.kt (수정)
package com.ibkpoc.amn.model

data class MeetingDetailResponse(
    val summary: String?,
    val participant: String?  // nullable로 변경
)