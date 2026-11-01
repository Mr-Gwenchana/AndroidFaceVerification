package com.bachelor.projectiii.models.mqtt.bio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Outgoing payload for MQTT topic "iot/devices/device_sync_bio" (QoS 2).
 * Sends current device biometric ID list to server for differential sync.
 */
@Serializable
data class BioSyncRequestPayload(
    @SerialName("MacAddress")
    val macAddress: String,

    @SerialName("DoorId")
    val doorId: Int? = null,

    @SerialName("LstBioId")
    val lstBioId: List<Long>
)
