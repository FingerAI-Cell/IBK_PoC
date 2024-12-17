// model/RecordingData.kt (추가)
package com.ibkpoc.amn.model

// 녹음 상태 관리용 sealed class
sealed class RecordingState {
    object Idle : RecordingState()
    data class Recording(
        val meetingId: Long,
        val startTime: String,
        val duration: Long = 0L
    ) : RecordingState()
    data class Error(val message: String) : RecordingState()
}