package com.bachelor.projectiii.mqtt

/**
 * Sealed class representing the current state of the MQTT connection.
 */
sealed class MqttConnectionState {
    data object Disconnected : MqttConnectionState()
    data object Connecting : MqttConnectionState()
    data object Connected : MqttConnectionState()
    data class Error(val message: String, val cause: Throwable? = null) : MqttConnectionState()
}
