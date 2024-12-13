// model/RecordingData.kt (추가)
package com.ibkpoc.amn.model

data class RecordingData(
    val meetingId: Long,
    val chunkStartTime: Long,
    val duration: Long,
    val audioData: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RecordingData

        if (meetingId != other.meetingId) return false
        if (chunkStartTime != other.chunkStartTime) return false
        if (duration != other.duration) return false
        if (!audioData.contentEquals(other.audioData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = meetingId.hashCode()
        result = 31 * result + chunkStartTime.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + audioData.contentHashCode()
        return result
    }
}