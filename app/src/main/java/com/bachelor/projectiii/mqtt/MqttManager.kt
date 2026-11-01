package com.bachelor.projectiii.mqtt

import android.util.Log
import com.bachelor.projectiii.core.NetworkMonitor
import com.bachelor.projectiii.data.local.prefs.DevicePrefs
import com.bachelor.projectiii.data.remote.TokenApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Orchestrator managing network monitoring, JWT token acquisition, and MQTT lifecycle.
 */
class MqttManager(
    private val tokenApi: TokenApi = TokenApi(),
    private val networkMonitor: NetworkMonitor,
    private val devicePrefs: DevicePrefs,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    companion object {
        private const val TAG = "JWT-Test"
    }

    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    private var lifecycleJob: Job? = null
    private var isRunning = false

    /**
     * Starts the automatic lifecycle loop.
     */
    fun start() {
        if (isRunning) return
        isRunning = true

        lifecycleJob = scope.launch {
            combine(
                networkMonitor.isOnline,
                devicePrefs.configFlow
            ) { isOnline, config ->
                isOnline to config
            }.collectLatest { (isOnline, config) ->
                if (!isOnline) {
                    Log.i(TAG, "Device is offline. Waiting for network...")
                    _connectionState.value = MqttConnectionState.Disconnected
                    return@collectLatest
                }

                if (config.apiBaseUrl.isBlank() || config.macAddress.isBlank() || config.apiSaltKey.isBlank()) {
                    Log.i(TAG, "Missing configuration (apiBaseUrl, macAddress, or apiSaltKey). Waiting for settings...")
                    _connectionState.value = MqttConnectionState.Disconnected
                    return@collectLatest
                }

                while (isActive && isRunning) {
                    _connectionState.value = MqttConnectionState.Connecting
                    Log.i(TAG, "Requesting JWT token for MAC: ${config.macAddress}...")

                    val result = tokenApi.getToken(
                        baseUrl = config.apiBaseUrl,
                        mac = config.macAddress,
                        salt = config.apiSaltKey
                    )

                    result.onSuccess { token ->
                        _connectionState.value = MqttConnectionState.Connected
                        Log.i(TAG, "JWT Token: $token")
                        return@collectLatest
                    }.onFailure { error ->
                        val errorMsg = error.message ?: "Unknown error"
                        _connectionState.value = MqttConnectionState.Error(errorMsg, error)
                        Log.e(TAG, "Failed to get JWT token: $errorMsg")

                        val delaySeconds = (config.reconnectDelay ?: 5).coerceAtLeast(3)
                        Log.i(TAG, "Retrying in ${delaySeconds}s...")
                        delay(delaySeconds * 1000L)
                    }
                }
            }
        }
    }

    /**
     * Stops the lifecycle loop and releases resources.
     */
    fun stop() {
        isRunning = false
        lifecycleJob?.cancel()
        lifecycleJob = null
        _connectionState.value = MqttConnectionState.Disconnected
        Log.i(TAG, "MqttManager stopped.")
    }
}
