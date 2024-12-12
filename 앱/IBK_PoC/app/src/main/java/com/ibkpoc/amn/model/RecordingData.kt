// model/RecordingData.kt (추가)
package com.ibkpoc.amn.model

data class RecordingData(
    val meetingId: Long,
    val userId: Int,
    val audioData: ByteArray,  // STT 서비스로 전송할 데이터
    val duration: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RecordingData

        if (meetingId != other.meetingId) return false
        if (userId != other.userId) return false
        if (!audioData.contentEquals(other.audioData)) return false
        if (duration != other.duration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = meetingId.hashCode()
        result = 31 * result + userId
        result = 31 * result + audioData.contentHashCode()
        result = 31 * result + duration.hashCode()
        return result
    }
}