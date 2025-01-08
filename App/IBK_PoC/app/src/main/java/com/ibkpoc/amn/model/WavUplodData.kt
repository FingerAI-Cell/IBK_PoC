// model/WavUploadData.kt (추가)
package com.ibkpoc.amn.model

import java.io.File

data class WavUploadData(
    val meetingId: Long,
    val sectionNumber: Int, // 섹션 번호 추가
    val startTime: String,
    val chunkData: ByteArray,
    val totalChunks: Int, // 총 청크 수 추가
    val currentChunks: Int
)