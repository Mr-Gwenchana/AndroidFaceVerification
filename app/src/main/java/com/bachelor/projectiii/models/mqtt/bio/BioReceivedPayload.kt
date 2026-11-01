package com.bachelor.projectiii.models.mqtt.bio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Outgoing ACK payload for MQTT topic "iot/devices/device_received_bio" (QoS 2).
 * Sent immediately after processing a push_biometric message.
 */
@Serializable
data class BioReceivedPayload(
    @SerialName("MacAddress")
    val macAddress: String,

    @SerialName("BioId")
    val bioId: Long,

    @SerialName("DoorId")
    val doorId: Int,

    @SerialName("CmdType")
    val cmdType: String,

    @SerialName("DeviceTime")
    val deviceTime: String
)
