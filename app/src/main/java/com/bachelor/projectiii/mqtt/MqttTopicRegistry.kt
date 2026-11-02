package com.bachelor.projectiii.mqtt

import com.bachelor.projectiii.data.local.AppDatabase
import com.bachelor.projectiii.data.local.prefs.DevicePrefs
import com.bachelor.projectiii.interfaces.mqtt.MqttTopicHandler
import com.bachelor.projectiii.interfaces.mqtt.MqttTopicPublisher
import com.bachelor.projectiii.mqtt.handler.PushBiometricHandler
import com.bachelor.projectiii.mqtt.publisher.BioReceivedPublisher
import com.bachelor.projectiii.mqtt.publisher.BioSyncPublisher
import com.bachelor.projectiii.mqtt.publisher.HealthcheckPublisher
import com.bachelor.projectiii.mqtt.syncStateManager.BioSyncStateManager
import kotlinx.serialization.json.Json

/**
 * Stateless object factory assembling MQTT topic handlers and periodic publishers.
 * Keeps memory overhead at 0 bytes on Heap, adhering to the Open-Closed Principle (OCP).
 */
internal object MqttTopicRegistry {

    data class TopicComponents(
        val inboundHandlers: List<MqttTopicHandler>,
        val periodicPublishers: List<MqttTopicPublisher<Unit>>
    )

    /**
     * Assembles all inbound topic handlers and periodic publishers, linking shared states
     * (such as BioSyncStateManager) cleanly without persistent heap overhead.
     */
    fun createComponents(
        mqttClient: MqttClient,
        devicePrefs: DevicePrefs,
        appDatabase: AppDatabase,
        json: Json
    ): TopicComponents {
        // All Sync State Managers
        val bioSyncStateManager = BioSyncStateManager()

        val bioReceivedPublisher = BioReceivedPublisher(mqttClient, json)

        // All handler for topic subcribed
        val inboundHandlers = listOf<MqttTopicHandler>(
            PushBiometricHandler(
                biometricDao = appDatabase.biometricDao(),
                devicePrefs = devicePrefs,
                bioReceivedPublisher = bioReceivedPublisher,
                syncStateManager = bioSyncStateManager,
                json = json
            )
        )

        // All publisher for period topic
        val periodicPublishers = listOf<MqttTopicPublisher<Unit>>(
            HealthcheckPublisher(mqttClient, devicePrefs, json),
            BioSyncPublisher(
                mqttClient = mqttClient,
                biometricDao = appDatabase.biometricDao(),
                devicePrefs = devicePrefs,
                syncStateManager = bioSyncStateManager,
                json = json
            )
        )

        return TopicComponents(
            inboundHandlers = inboundHandlers,
            periodicPublishers = periodicPublishers
        )
    }
}
