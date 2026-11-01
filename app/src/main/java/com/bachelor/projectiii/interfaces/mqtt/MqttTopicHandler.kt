package com.bachelor.projectiii.interfaces.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos

/**
 * Contract for handling inbound MQTT messages (Server -> Device).
 */
interface MqttTopicHandler {
    /** Topic pattern (supports wildcard '+'), e.g. "iot/server/push_config/+" */
    val topicPattern: String

    /** Target QoS subscription level */
    val qos: MqttQos get() = MqttQos.EXACTLY_ONCE

    /** Handles raw incoming payload from the broker */
    suspend fun handle(topic: String, payload: ByteArray)
}
