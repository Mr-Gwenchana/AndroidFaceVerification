package com.bachelor.projectiii.mqtt.handler

import android.util.Log
import com.bachelor.projectiii.data.local.dao.BiometricDao
import com.bachelor.projectiii.data.local.prefs.DevicePrefs
import com.bachelor.projectiii.interfaces.mqtt.MqttTopicHandler
import com.bachelor.projectiii.models.mqtt.bio.BioReceivedPayload
import com.bachelor.projectiii.models.mqtt.bio.PushBiometricPayload
import com.bachelor.projectiii.mqtt.publisher.BioReceivedPublisher
import com.bachelor.projectiii.mqtt.syncStateManager.BioSyncStateManager
import com.hivemq.client.mqtt.datatypes.MqttQos
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Inbound handler for server-pushed biometric template updates (NEW_BIO or DELETE_BIO).
 * Sets sync state flag, updates Room DB, dispatches ACK, and finishes session on SyncEnded.
 * Topic pattern: "iot/server/push_biometric/+" (QoS 2)
 */
class PushBiometricHandler(
    private val biometricDao: BiometricDao,
    private val devicePrefs: DevicePrefs,
    private val bioReceivedPublisher: BioReceivedPublisher,
    private val syncStateManager: BioSyncStateManager,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : MqttTopicHandler {

    companion object {
        private const val TAG = "PushBiometricHandler"
        private const val CMD_NEW_BIO = "NEW_BIO"
        private const val CMD_DELETE_BIO = "DELETE_BIO"
    }

    override val topicPattern: String = "iot/server/push_biometric/+"
    override val qos: MqttQos = MqttQos.EXACTLY_ONCE

    override suspend fun handle(topic: String, payload: ByteArray) {
        // Immediately notify state manager that a sync packet arrived
        syncStateManager.onPacketReceived()

        try {
            val jsonString = payload.toString(Charsets.UTF_8)
            val pushPayload = json.decodeFromString<PushBiometricPayload>(jsonString)

            Log.d(TAG, "Received push_biometric: bioId=${pushPayload.bioId}, cmdType=${pushPayload.cmdType}, syncEnded=${pushPayload.syncEnded}")

            // 1. Process Database action
            when (pushPayload.cmdType) {
                CMD_NEW_BIO -> {
                    val entity = pushPayload.toEntity(json)
                    biometricDao.insertOrReplace(entity)
                    Log.i(TAG, "Saved biometric profile bioId=${pushPayload.bioId} (${pushPayload.fullName})")
                }
                CMD_DELETE_BIO -> {
                    biometricDao.deleteByBioId(pushPayload.bioId)
                    Log.i(TAG, "Deleted biometric profile bioId=${pushPayload.bioId}")
                }
                else -> {
                    Log.w(TAG, "Unrecognized cmdType='${pushPayload.cmdType}' for bioId=${pushPayload.bioId}")
                }
            }

            // 2. Automatically update local doorId if received a valid doorId
            val config = devicePrefs.configFlow.first()
            if (pushPayload.doorId > 0 && config.doorId != pushPayload.doorId) {
                devicePrefs.updateRuntimeState(doorId = pushPayload.doorId)
                Log.i(TAG, "Updated local doorId to ${pushPayload.doorId} from push_biometric")
            }

            // 3. Dispatch immediate ACK to server
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val ackPayload = BioReceivedPayload(
                macAddress = config.macAddress,
                bioId = pushPayload.bioId,
                doorId = pushPayload.doorId,
                cmdType = pushPayload.cmdType,
                deviceTime = dateFormat.format(Date())
            )
            bioReceivedPublisher.publish(ackPayload)

            // 4. Check if server finished sending all differential updates
            if (pushPayload.syncEnded) {
                syncStateManager.onSyncEnded()
                Log.i(TAG, "Biometric synchronization finished by server (SyncEnded=true)")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse or process push_biometric payload", e)
        }
    }
}
