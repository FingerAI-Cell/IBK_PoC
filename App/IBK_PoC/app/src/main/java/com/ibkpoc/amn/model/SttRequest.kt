package com.ibkpoc.amn.model

data class SttRequest(
    val meetingId: Long,
    val sectionNumber: Int,
    val audioData: ByteArray // WAV 섹션 데이터를 담는 필드
)
