// model/WavUploadData.kt (추가)
package com.ibkpoc.amn.model

import java.io.File

data class WavUploadData(
    val meetingId: Long,
    val wavFile: File,
    val startTime: String,
    val duration: Long,
    val totalChunks: Int,
    val currentChunk: Int,
    val chunkData: ByteArray
)