package com.ibkpoc.amn.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EventBus {
    private val _recordingEvent = MutableSharedFlow<RecordingEvent>()
    val recordingEvent = _recordingEvent.asSharedFlow()

    suspend fun emitEvent(event: RecordingEvent) {
        _recordingEvent.emit(event)
    }

    sealed class RecordingEvent {
        object ForceStop : RecordingEvent()
    }
} 