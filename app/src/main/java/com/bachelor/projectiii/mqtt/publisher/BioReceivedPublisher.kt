package com.bachelor.projectiii.mqtt.publisher

import android.util.Log
import com.bachelor.projectiii.interfaces.mqtt.MqttTopicPublisher
import com.bachelor.projectiii.models.mqtt.bio.BioReceivedPayload
import com.bachelor.projectiii.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Event-driven publisher sending ACK confirmation to the broker after receiving and processing
 * a push_biometric message.
 * Topic: "iot/devices/device_received_bio" (QoS 2)
 */
class BioReceivedPublisher(
    private val mqttClient: MqttClient,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : MqttTopicPublisher<BioReceivedPayload> {

    companion object {
        private const val TAG = "BioReceivedPublisher"
    }

    override val topic: String = "iot/devices/device_received_bio"
    override val qos: MqttQos = MqttQos.EXACTLY_ONCE
    override val retain: Boolean = false
    override val intervalMs: Long = 0L

    override suspend fun publish(data: BioReceivedPayload): Boolean {
        if (!mqttClient.isConnected) {
            Log.w(TAG, "Cannot publish bio received ACK: MQTT client is disconnected")
            return false
        }

        return try {
            val jsonString = json.encodeToString(data)
            val payloadBytes = jsonString.toByteArray(Charsets.UTF_8)
            val result = mqttClient.publish(topic, payloadBytes, qos, retain)
            if (result.isSuccess) {
                Log.d(TAG, "Sent bio ACK for bioId=${data.bioId}, cmdType=${data.cmdType}")
                true
            } else {
                Log.e(TAG, "Failed to publish bio ACK for bioId=${data.bioId}: ${result.exceptionOrNull()?.message}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error serializing or publishing bio ACK for bioId=${data.bioId}", e)
            false
        }
    }
}
