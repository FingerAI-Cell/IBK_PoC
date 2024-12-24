package com.ibkpoc.amn.event

import com.ibkpoc.amn.model.RecordServiceState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.Flow

object EventBus {
    private val _events = MutableSharedFlow<Any>()
    val events = _events.asSharedFlow()

    suspend fun post(event: Any) {
        _events.emit(event)
    }

    suspend inline fun <reified T> subscribe(): Flow<T> =
        events.filterIsInstance<T>()
}

data class RecordingStateEvent(val state: RecordServiceState)
