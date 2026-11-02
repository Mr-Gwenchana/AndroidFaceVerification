package com.bachelor.projectiii.mqtt

import com.bachelor.projectiii.data.local.AppDatabase
import com.bachelor.projectiii.data.local.prefs.DevicePrefs
import com.bachelor.projectiii.interfaces.mqtt.MqttTopicHandler
import com.bachelor.projectiii.interfaces.mqtt.MqttTopicPublisher
import com.bachelor.projectiii.mqtt.publisher.HealthcheckPublisher
import kotlinx.serialization.json.Json

/**
 * Stateless object factory assembling MQTT topic handlers and periodic publishers.
 * Keeps memory overhead at 0 bytes on Heap, adhering to the Open-Closed Principle (OCP).
 */
internal object MqttTopicRegistry {

    /**
     * Assembles all inbound topic handlers (Server -> Device).
     */
    fun createInboundHandlers(
        devicePrefs: DevicePrefs,
        appDatabase: AppDatabase,
        json: Json
    ): List<MqttTopicHandler> = listOf(
        // Handlers will be added here as implemented:
        // PushConfigHandler(devicePrefs, json),
        // PushBiometricHandler(appDatabase.biometricDao(), ...),
        // CommandHandler(appDatabase.processedCommandDao(), ...)
    )

    /**
     * Assembles all periodic outbound publishers (Device -> Server) where intervalMs > 0.
     */
    fun createPeriodicPublishers(
        mqttClient: MqttClient,
        devicePrefs: DevicePrefs,
        json: Json
    ): List<MqttTopicPublisher<Unit>> = listOf(
        HealthcheckPublisher(mqttClient, devicePrefs, json)
        // BioSyncPublisher(mqttClient, appDatabase.biometricDao(), devicePrefs, json),
        // DoorSyncPublisher(mqttClient, devicePrefs, appDatabase.doorDependencyDao(), json)
    )
}
