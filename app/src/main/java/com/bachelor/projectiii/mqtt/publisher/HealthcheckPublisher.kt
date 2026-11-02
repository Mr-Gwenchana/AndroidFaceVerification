package com.bachelor.projectiii.mqtt.publisher

import android.util.Log
import com.bachelor.projectiii.data.local.prefs.DevicePrefs
import com.bachelor.projectiii.interfaces.mqtt.MqttTopicPublisher
import com.bachelor.projectiii.models.mqtt.BioAuthTypePayload
import com.bachelor.projectiii.models.mqtt.HealthcheckPayload
import com.bachelor.projectiii.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.net.Inet4Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Periodic publisher sending healthcheck heartbeats to the broker.
 * Interval is read dynamically from DevicePrefs (fallback to 30s).
 * Topic: "iot/devices/healthcheck" (QoS 0)
 */
class HealthcheckPublisher(
    private val mqttClient: MqttClient,
    private val devicePrefs: DevicePrefs,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : MqttTopicPublisher<Unit> {

    companion object {
        private const val TAG = "HealthcheckPublisher"
        const val DEFAULT_INTERVAL_MS = 30_000L
        const val MIN_INTERVAL_SECONDS = 5
    }

    override val topic: String = "iot/devices/healthcheck"
    override val qos: MqttQos = MqttQos.AT_MOST_ONCE
    override val retain: Boolean = false

    @Volatile
    private var _intervalMs: Long = DEFAULT_INTERVAL_MS

    override val intervalMs: Long
        get() = _intervalMs

    override suspend fun publish(data: Unit): Boolean {
        if (!mqttClient.isConnected) {
            Log.d(TAG, "Skipping healthcheck: MQTT client is not connected")
            return false
        }

        return try {
            val config = devicePrefs.configFlow.first()
            val mac = config.macAddress
            if (mac.isBlank()) {
                Log.w(TAG, "Cannot publish healthcheck: MAC address is blank")
                return false
            }

            // Dynamically sync intervalMs from config; fallback to DEFAULT_INTERVAL_MS if unset
            _intervalMs = if (config.healthcheckInterval != null && config.healthcheckInterval > 0) {
                config.healthcheckInterval.coerceAtLeast(MIN_INTERVAL_SECONDS) * 1000L
            } else {
                DEFAULT_INTERVAL_MS
            }

            val payload = HealthcheckPayload(
                macAddress = mac,
                ipAddress = getIpAddress(),
                deviceTime = getUtcIsoTime(),
                isDeviceDisable = config.isDeviceDisable,
                isEmergencyOpen = config.isEmergencyOpen,
                doorStatus = if (config.isEmergencyOpen) "EMERGENCYOPEN" else "CLOSE",
                version = 1L,
                bioAuthType = BioAuthTypePayload(
                    isFace = true,
                    isFinger = false,
                    isIris = false,
                    isIdCard = false,
                    direction = config.direction,
                    oneIsOpen = true,
                    oneIsOpenDoorState = false
                )
            )

            val jsonString = json.encodeToString(HealthcheckPayload.serializer(), payload)
            val bytes = jsonString.toByteArray(Charsets.UTF_8)

            val result = mqttClient.publish(topic, bytes, qos, retain)
            if (result.isSuccess) {
                Log.i(TAG, "Healthcheck published successfully -> $topic")
                true
            } else {
                Log.w(TAG, "Failed to publish healthcheck: ${result.exceptionOrNull()?.message}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while publishing healthcheck: ${e.message}", e)
            false
        }
    }

    /**
     * Formats UTC ISO-8601 timestamp (e.g. 2026-11-02T11:25:00Z) compatible with all Android API levels.
     */
    private fun getUtcIsoTime(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date())
    }

    /**
     * Resolves the primary IPv4 address of the device, prioritizing wlan/eth.
     */
    private fun getIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            // Prioritize Wi-Fi (wlan) and Ethernet (eth) interfaces
            for (iface in interfaces) {
                if (iface.isLoopback || !iface.isUp) continue
                if (iface.name.contains("wlan", ignoreCase = true) || iface.name.contains("eth", ignoreCase = true)) {
                    for (addr in Collections.list(iface.inetAddresses)) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress
                            if (!host.isNullOrBlank()) return host
                        }
                    }
                }
            }
            // Fallback to any valid non-loopback IPv4 interface
            for (iface in interfaces) {
                if (iface.isLoopback || !iface.isUp) continue
                for (addr in Collections.list(iface.inetAddresses)) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress
                        if (!host.isNullOrBlank()) return host
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }
}
