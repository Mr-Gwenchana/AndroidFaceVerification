package com.bachelor.projectiii.models.mqtt.token

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response payload received from POST /api/devicecomm/getmqtttoken.
 */
@Serializable
data class TokenResponsePayload(
    @SerialName("code")
    val code: String,
    @SerialName("message")
    val message: String? = null,
    @SerialName("data")
    val data: TokenData? = null
)

/**
 * Inner data payload containing the JWT token.
 */
@Serializable
data class TokenData(
    @SerialName("token")
    val token: String,
    @SerialName("username")
    val username: String? = null
)
