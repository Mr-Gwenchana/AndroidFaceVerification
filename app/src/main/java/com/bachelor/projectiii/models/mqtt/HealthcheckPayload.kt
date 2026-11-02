package com.bachelor.projectiii.models.mqtt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Payload sent periodically by the device to notify server of its health and state.
 * Topic: "iot/devices/healthcheck" (QoS 0)
 */
@Serializable
data class HealthcheckPayload(
    @SerialName("MacAddress")
    val macAddress: String,

    @SerialName("IpAddress")
    val ipAddress: String,

    @SerialName("DeviceTime")
    val deviceTime: String,

    @SerialName("IsDeviceDisable")
    val isDeviceDisable: Boolean = false,

    @SerialName("IsEmergencyOpen")
    val isEmergencyOpen: Boolean = false,

    @SerialName("DoorStatus")
    val doorStatus: String = "CLOSE",

    @SerialName("Version")
    val version: Long = 1L,

    @SerialName("BioAuthType")
    val bioAuthType: BioAuthTypePayload = BioAuthTypePayload()
)

/**
 * Biometric authentication configuration reported by the device in healthcheck.
 */
@Serializable
data class BioAuthTypePayload(
    @SerialName("IsFace")
    val isFace: Boolean = true,

    @SerialName("IsFinger")
    val isFinger: Boolean = false,

    @SerialName("IsIris")
    val isIris: Boolean = false,

    @SerialName("IsIdCard")
    val isIdCard: Boolean = false,

    @SerialName("Direction")
    val direction: String? = "IN",

    @SerialName("OneIsOpen")
    val oneIsOpen: Boolean = true,

    @SerialName("OneIsOpenDoorState")
    val oneIsOpenDoorState: Boolean = false
)
