package com.bachelor.projectiii.mqtt.syncStateManager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thread-safe in-memory state manager tracking biometric sync sessions.
 * Decouples PushBiometricHandler and BioSyncPublisher with zero coroutine leak overhead.
 */
class BioSyncStateManager {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    @Volatile
    private var lastPacketTimeMs: Long = 0L

    /**
     * Marks that a sync packet has been received from the server.
     * Activates syncing state and updates last received packet timestamp.
     */
    fun onPacketReceived() {
        _isSyncing.value = true
        lastPacketTimeMs = System.currentTimeMillis()
    }

    /**
     * Marks that server has signaled sync completion (SyncEnded = true).
     */
    fun onSyncEnded() {
        _isSyncing.value = false
        lastPacketTimeMs = 0L
    }

    /**
     * Checks if a sync session is actively running.
     * If more than [timeoutMs] has elapsed without receiving a new packet,
     * it automatically self-heals by resetting syncing state to false and returns false.
     */
    fun isCurrentlySyncing(timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean {
        if (!_isSyncing.value) return false

        val elapsed = System.currentTimeMillis() - lastPacketTimeMs
        if (lastPacketTimeMs > 0L && elapsed > timeoutMs) {
            _isSyncing.value = false
            lastPacketTimeMs = 0L
            return false
        }
        return true
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 120_000L // 2 minutes watchdog fallback
    }
}
