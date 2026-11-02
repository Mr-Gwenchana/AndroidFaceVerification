package com.bachelor.projectiii.mqtt.publisher

import android.util.Log
import com.bachelor.projectiii.data.local.dao.BiometricDao
import com.bachelor.projectiii.data.local.prefs.DevicePrefs
import com.bachelor.projectiii.interfaces.mqtt.MqttTopicPublisher
import com.bachelor.projectiii.models.mqtt.bio.BioSyncRequestPayload
import com.bachelor.projectiii.mqtt.MqttClient
import com.bachelor.projectiii.mqtt.syncStateManager.BioSyncStateManager
import com.hivemq.client.mqtt.datatypes.MqttQos
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Periodic publisher requesting differential biometric synchronization from the server.
 * Automatically pauses requests if a sync session is actively receiving data from the server.
 * Topic: "iot/devices/device_sync_bio" (QoS 2)
 */
class BioSyncPublisher(
    private val mqttClient: MqttClient,
    private val biometricDao: BiometricDao,
    private val devicePrefs: DevicePrefs,
    private val syncStateManager: BioSyncStateManager,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : MqttTopicPublisher<Unit> {

    companion object {
        private const val TAG = "BioSyncPublisher"
        const val DEFAULT_INTERVAL_MS = 30_000L
        const val MIN_INTERVAL_SECONDS = 5
        const val DEFAULT_TIMEOUT_SECONDS = 120
    }

    override val topic: String = "iot/devices/device_sync_bio"
    override val qos: MqttQos = MqttQos.EXACTLY_ONCE
    override val retain: Boolean = false

    @Volatile
    private var _intervalMs: Long = DEFAULT_INTERVAL_MS

    override val intervalMs: Long
        get() = _intervalMs

    override suspend fun publish(data: Unit): Boolean {
        if (!mqttClient.isConnected) {
            Log.d(TAG, "Skipping bio sync request: MQTT client is disconnected")
            return false
        }

        return try {
            val config = devicePrefs.configFlow.first()
            val intervalSec = config.bioSyncInterval ?: 30
            _intervalMs = maxOf(MIN_INTERVAL_SECONDS, intervalSec) * 1000L

            val timeoutSec = config.bioSyncTimeout ?: DEFAULT_TIMEOUT_SECONDS
            val timeoutMs = timeoutSec * 1000L

            // If server is actively pushing biometric packets, pause sending new sync requests
            if (syncStateManager.isCurrentlySyncing(timeoutMs)) {
                Log.d(TAG, "Active biometric sync in progress. Skipping periodic sync request.")
                return true
            }

            val mac = config.macAddress
            if (mac.isBlank()) {
                Log.w(TAG, "Cannot publish bio sync request: MAC address is blank")
                return false
            }

            val currentBioIds = biometricDao.getAllBioIds()
            val payload = BioSyncRequestPayload(
                macAddress = mac,
                doorId = config.doorId,
                lstBioId = currentBioIds
            )

            val jsonString = json.encodeToString(payload)
            val payloadBytes = jsonString.toByteArray(Charsets.UTF_8)

            val result = mqttClient.publish(topic, payloadBytes, qos, retain)
            if (result.isSuccess) {
                Log.i(TAG, "Published bio sync request (${currentBioIds.size} local bioIds, doorId=${config.doorId})")
                true
            } else {
                Log.e(TAG, "Failed to publish bio sync request: ${result.exceptionOrNull()?.message}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while preparing or publishing bio sync request", e)
            false
        }
    }
}
