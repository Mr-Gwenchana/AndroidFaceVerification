package com.bachelor.projectiii.interfaces.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos

/**
 * Contract for publishing outbound MQTT messages (Device -> Server).
 */
interface MqttTopicPublisher<in T> {
    /** Destination MQTT topic */
    val topic: String

    /** Target QoS level */
    val qos: MqttQos get() = MqttQos.AT_LEAST_ONCE

    /** Retain flag on broker */
    val retain: Boolean get() = false

    /** Publishing interval in ms. 0 = event-driven, > 0 = periodic */
    val intervalMs: Long get() = 0L

    /** Publishes data payload to broker */
    suspend fun publish(data: T): Boolean
}

/** Extension helper for periodic publishers without input payload */
suspend fun MqttTopicPublisher<Unit>.publish(): Boolean = publish(Unit)
