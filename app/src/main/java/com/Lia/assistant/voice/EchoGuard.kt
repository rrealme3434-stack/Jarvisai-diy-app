package com.Lia.assistant.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object EchoGuard {
    private const val ECHO_TAIL_MS = 450L
    private const val WATCHDOG_FORCE_UNMUTE_MS = 60_000L

    private var activePlaybackCount = 0
    private var isMuted = false
    private var tailJob: Job? = null
    private var watchdogJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Synchronized
    fun notifyPlaybackStarted() {
        activePlaybackCount++
        isMuted = true
        tailJob?.cancel()
        tailJob = null

        // Watchdog to prevent permanent mute if playback crashes
        if (watchdogJob == null) {
            watchdogJob = scope.launch {
                delay(WATCHDOG_FORCE_UNMUTE_MS)
                forceUnmute()
            }
        }
    }

    @Synchronized
    fun notifyPlaybackStopped() {
        if (activePlaybackCount > 0) {
            activePlaybackCount--
        }
        if (activePlaybackCount == 0) {
            watchdogJob?.cancel()
            watchdogJob = null
            tailJob?.cancel()
            tailJob = scope.launch {
                delay(ECHO_TAIL_MS)
                synchronized(this@EchoGuard) {
                    if (activePlaybackCount == 0) {
                        isMuted = false
                    }
                }
            }
        }
    }

    @Synchronized
    fun isMicMuted(): Boolean {
        return isMuted
    }

    @Synchronized
    fun forceUnmute() {
        activePlaybackCount = 0
        isMuted = false
        tailJob?.cancel()
        tailJob = null
        watchdogJob?.cancel()
        watchdogJob = null
    }

    @Synchronized
    fun reset() {
        forceUnmute()
    }
}
