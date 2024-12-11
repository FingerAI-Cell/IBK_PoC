// model/Meeting.kt 수정 (기존 모델 유지하면서 확장)
package com.ibkpoc.amn.model

data class Meeting(
    val id: Long,
    val title: String,           // "회의 ${logId}"와 같은 형식으로 생성
    val start: String,
    val end: String,
    val participantCount: Int,
    val topic: String
)