package com.Lia.assistant.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VoiceLatencyTracker {
    private val _reconnectCount = MutableStateFlow(0)
    val reconnectCount: StateFlow<Int> = _reconnectCount.asStateFlow()

    private var turnStartTime = 0L
    private var firstAudioTime = 0L
    private var turnEndTime = 0L

    var lastResponseLatencyMs: Long = 0L
        private set

    fun recordReconnect() {
        _reconnectCount.value++
    }

    fun resetReconnects() {
        _reconnectCount.value = 0
    }

    fun markUserTurnStarted(time: Long = System.currentTimeMillis()) {
        turnStartTime = time
        firstAudioTime = 0L
        turnEndTime = 0L
    }

    fun markFirstAudioReceived(time: Long = System.currentTimeMillis()) {
        if (firstAudioTime == 0L && turnStartTime > 0L && time >= turnStartTime) {
            firstAudioTime = time
            lastResponseLatencyMs = time - turnStartTime
        }
    }

    fun markTurnCompleted(time: Long = System.currentTimeMillis()) {
        if (turnStartTime > 0L && time >= turnStartTime) {
            turnEndTime = time
        }
    }
}
