package com.bachelor.projectiii.mqtt

import android.util.Log
import com.bachelor.projectiii.core.NetworkMonitor
import com.bachelor.projectiii.data.local.AppDatabase
import com.bachelor.projectiii.data.local.prefs.DeviceConfig
import com.bachelor.projectiii.data.local.prefs.DevicePrefs
import com.bachelor.projectiii.data.remote.TokenApi
import com.bachelor.projectiii.interfaces.mqtt.publish
import com.hivemq.client.mqtt.datatypes.MqttQos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Orchestrator managing network monitoring, JWT token acquisition, MQTT connection,
 * subscription routing, periodic publishers, and resilient reconnection loops.
 */
class MqttManager(
    private val networkMonitor: NetworkMonitor,
    private val devicePrefs: DevicePrefs,
    private val appDatabase: AppDatabase,
    private val tokenApi: TokenApi = TokenApi(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    companion object {
        private const val TAG = "MqttManager"
    }

    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val disconnectSignal = Channel<Unit>(Channel.CONFLATED)

    private val mqttClient = MqttClient(
        onDisconnectedListener = { cause ->
            Log.w(TAG, "MQTT connection lost: ${cause?.message}")
            disconnectSignal.trySend(Unit)
        }
    )

    private val inboundHandlers = MqttTopicRegistry.createInboundHandlers(devicePrefs, appDatabase, json)
    private val periodicPublishers = MqttTopicRegistry.createPeriodicPublishers(mqttClient, devicePrefs, json)
    private val router = MqttMessageRouter(inboundHandlers)

    private val periodicJobs = mutableListOf<Job>()
    private var lifecycleJob: Job? = null
    private var isRunning = false

    /**
     * Starts the automatic MQTT lifecycle loop.
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
                    handleDisconnection("Device is offline")
                    return@collectLatest
                }

                if (config.apiBaseUrl.isBlank() || config.macAddress.isBlank() ||
                    config.apiSaltKey.isBlank() || config.brokerHost.isBlank()
                ) {
                    Log.i(TAG, "Missing configuration (apiBaseUrl, macAddress, apiSaltKey, or brokerHost). Waiting for settings...")
                    handleDisconnection("Missing configuration")
                    return@collectLatest
                }

                val reconnectDelaySec = (config.reconnectDelay ?: 5).coerceAtLeast(3)
                delay(reconnectDelaySec * 1000L)

                runConnectionLoop(config)
            }
        }
    }

    /**
     * Connects, subscribes, and maintains session with automatic token renewal upon disconnect.
     */
    private suspend fun runConnectionLoop(config: DeviceConfig) {
        val reconnectDelaySec = (config.reconnectDelay ?: 5).coerceAtLeast(3)

        while (scope.isActive && isRunning) {
            _connectionState.value = MqttConnectionState.Connecting
            Log.i(TAG, "Requesting fresh JWT token for MAC: ${config.macAddress}...")

            val tokenResult = tokenApi.getToken(
                baseUrl = config.apiBaseUrl,
                mac = config.macAddress,
                salt = config.apiSaltKey
            )

            val token = tokenResult.getOrElse { error ->
                val errorMsg = error.message ?: "Failed to get token"
                Log.e(TAG, "Token acquisition failed: $errorMsg. Retrying in ${reconnectDelaySec}s...")
                _connectionState.value = MqttConnectionState.Error(errorMsg, error)
                delay(reconnectDelaySec * 1000L)
                return@runConnectionLoop
            }

            val port = config.brokerPort ?: if (config.enableTls) 8883 else 1883
            Log.i(TAG, "Connecting to MQTT Broker at ${config.brokerHost}:$port (TLS: ${config.enableTls})...")

            val connectResult = mqttClient.connect(
                host = config.brokerHost,
                port = port,
                mac = config.macAddress,
                token = token,
                enableTls = config.enableTls,
                cleanStart = config.cleanStart,
                keepAliveSeconds = config.keepAlive ?: 60
            )

            if (connectResult.isFailure) {
                val error = connectResult.exceptionOrNull()
                val errorMsg = error?.message ?: "Failed to connect to broker"
                Log.e(TAG, "MQTT connect failed: $errorMsg. Retrying in ${reconnectDelaySec}s...")
                _connectionState.value = MqttConnectionState.Error(errorMsg, error)
                delay(reconnectDelaySec * 1000L)
                continue
            }

            // Successfully connected to EMQX broker
            _connectionState.value = MqttConnectionState.Connected
            Log.i(TAG, "Connected to MQTT Broker successfully!")

            // Subscribe to server topic filter: iot/server/+/<MAC>
            val topicFilter = "iot/server/+/${config.macAddress}"
            val subResult = mqttClient.subscribe(topicFilter, MqttQos.EXACTLY_ONCE) { topic, payload ->
                scope.launch {
                    router.route(topic, payload)
                }
            }

            if (subResult.isFailure) {
                Log.w(TAG, "Subscribe failed on $topicFilter: ${subResult.exceptionOrNull()?.message}")
            }

            // Start periodic publishers (e.g. HealthcheckPublisher every 30s)
            startPeriodicPublishers()

            // Wait until disconnected
            disconnectSignal.receive()

            // Disconnected: stop timers and delay before requesting NEW token
            stopPeriodicPublishers()
            _connectionState.value = MqttConnectionState.Disconnected
            Log.i(TAG, "Disconnected from broker. Waiting ${reconnectDelaySec}s before reconnecting with a fresh token...")
            delay(reconnectDelaySec * 1000L)
        }
    }

    private fun startPeriodicPublishers() {
        stopPeriodicPublishers()
        periodicPublishers.filter { it.intervalMs > 0 }.forEach { publisher ->
            periodicJobs += scope.launch {
                Log.i(TAG, "Starting periodic publisher for topic '${publisher.topic}' (interval: ${publisher.intervalMs}ms)")
                while (isActive && mqttClient.isConnected) {
                    try {
                        publisher.publish()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in periodic publisher '${publisher.topic}': ${e.message}", e)
                    }
                    delay(publisher.intervalMs)
                }
            }
        }
    }

    private fun stopPeriodicPublishers() {
        if (periodicJobs.isNotEmpty()) {
            Log.i(TAG, "Stopping ${periodicJobs.size} periodic publishers...")
            periodicJobs.forEach { it.cancel() }
            periodicJobs.clear()
        }
    }

    private suspend fun handleDisconnection(reason: String) {
        stopPeriodicPublishers()
        if (mqttClient.isConnected) {
            mqttClient.disconnect()
        }
        _connectionState.value = MqttConnectionState.Disconnected
        Log.i(TAG, "MqttManager disconnected: $reason")
    }

    /**
     * Stops the lifecycle loop and releases resources.
     */
    fun stop() {
        isRunning = false
        lifecycleJob?.cancel()
        lifecycleJob = null
        stopPeriodicPublishers()
        scope.launch {
            mqttClient.disconnect()
        }
        _connectionState.value = MqttConnectionState.Disconnected
        Log.i(TAG, "MqttManager stopped.")
    }
}
