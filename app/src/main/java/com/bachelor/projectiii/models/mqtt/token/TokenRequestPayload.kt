package com.bachelor.projectiii.models.mqtt.token

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request payload sent to POST /api/devicecomm/getmqtttoken.
 */
@Serializable
data class TokenRequestPayload(
    @SerialName("MacAddress")
    val macAddress: String,
    @SerialName("Password")
    val password: String
)
