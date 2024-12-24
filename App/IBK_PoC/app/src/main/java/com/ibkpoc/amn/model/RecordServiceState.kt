package com.ibkpoc.amn.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class RecordServiceState : Parcelable {
    @Parcelize
    object Idle : RecordServiceState()
    
    @Parcelize
    data class Recording(
        val meetingId: Long,
        val startTime: String,
        val duration: Long = 0L
    ) : RecordServiceState()
    
    @Parcelize
    data class Error(val message: String) : RecordServiceState()
    
    @Parcelize
    data class Completed(val filePath: String) : RecordServiceState()
}
