package com.ibkpoc.amn.model

import java.io.File

data class UploadData(
    val meetingId: Long,
    val sectionNumber: Int, // 섹션 번호 추가
    val startTime: String,
    val chunkData: ByteArray
)