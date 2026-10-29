package com.bachelor.projectiii.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.net.Inet6Address
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale

// DataStore instance tied to applicationContext to prevent memory leaks
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "device_settings")

/**
 * Data class representing device configuration. Empty strings/null if not configured.
 */
data class DeviceConfig(
    val apiBaseUrl: String = "",
    val apiSaltKey: String = "",
    val macAddress: String = "",
    val brokerHost: String = "",
    val brokerPort: Int? = null,
    val enableTls: Boolean = false,
    val cleanStart: Boolean = false,
    val keepAlive: Int? = null,
    val reconnectDelay: Int? = null,
    val healthcheckInterval: Int? = null,
    val bioSyncInterval: Int? = null,
    val doorSyncInterval: Int? = null,
    val bioSyncTimeout: Int? = null,
    val doorId: Int? = null,
    val direction: String = "IN",
    val isDeviceDisable: Boolean = false,
    val isEmergencyOpen: Boolean = false
)

/**
 * Storage manager for device settings and state flags.
 */
class DevicePrefs(context: Context) {

    // Store applicationContext to completely prevent Activity reference leaks
    private val appContext = context.applicationContext

    companion object {
        // Precompiled regex to prevent repeated allocations
        private val MAC_REGEX = Regex("([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}")

        // API Server keys
        val KEY_API_BASE_URL = stringPreferencesKey("api_base_url")
        val KEY_API_SALT_KEY = stringPreferencesKey("api_salt_key")

        // Device identity key
        val KEY_MAC_ADDRESS = stringPreferencesKey("device_mac_address")

        // MQTT Broker keys
        val KEY_BROKER_HOST = stringPreferencesKey("mqtt_broker_host")
        val KEY_BROKER_PORT = intPreferencesKey("mqtt_broker_port")
        val KEY_ENABLE_TLS = booleanPreferencesKey("mqtt_enable_tls")
        val KEY_CLEAN_START = booleanPreferencesKey("mqtt_clean_start")
        val KEY_KEEP_ALIVE = intPreferencesKey("mqtt_keep_alive")
        val KEY_RECONNECT_DELAY = intPreferencesKey("mqtt_reconnect_delay")

        // Periodic & sync timer keys
        val KEY_HEALTHCHECK_INTERVAL = intPreferencesKey("timer_healthcheck_interval")
        val KEY_BIO_SYNC_INTERVAL = intPreferencesKey("timer_bio_sync_interval")
        val KEY_DOOR_SYNC_INTERVAL = intPreferencesKey("timer_door_sync_interval")
        val KEY_BIO_SYNC_TIMEOUT = intPreferencesKey("timeout_bio_sync_round")

        // Runtime state keys
        val KEY_DOOR_ID = intPreferencesKey("runtime_door_id")
        val KEY_DIRECTION = stringPreferencesKey("runtime_direction")
        val KEY_DEVICE_DISABLE = booleanPreferencesKey("runtime_device_disable")
        val KEY_EMERGENCY_OPEN = booleanPreferencesKey("runtime_emergency_open")

        /**
         * Scans hardware interfaces with Wi-Fi prioritized (e.g. wlan0) using multi-tier fallback.
         * Runs on Dispatchers.IO to prevent main thread stalls.
         */
        suspend fun getWifiMacAddress(): String? = withContext(Dispatchers.IO) {
            getMacFromIpCommand()
                ?: getMacFromIpv6LinkLocal()
                ?: getMacFromHardwareAddress()
                ?: getMacFromFile()
        }

        /**
         * Tier 1: Executes `ip -br link` to read network interface MAC.
         * Ensures process and streams are properly closed to avoid file descriptor/process leaks.
         */
        private fun getMacFromIpCommand(): String? {
            var process: Process? = null
            try {
                process = Runtime.getRuntime().exec(arrayOf("ip", "-br", "link"))
                var wlanMac: String? = null
                var fallbackMac: String? = null

                process.inputStream.bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val l = line!!.trim()
                        val match = MAC_REGEX.find(l)?.value ?: continue
                        if (match.equals("00:00:00:00:00:00", ignoreCase = true) ||
                            match.equals("02:00:00:00:00:00", ignoreCase = true)) continue

                        if (l.startsWith("wlan", ignoreCase = true)) {
                            wlanMac = match.uppercase(Locale.US)
                            break
                        } else if (l.startsWith("eth", ignoreCase = true) && fallbackMac == null) {
                            fallbackMac = match.uppercase(Locale.US)
                        }
                    }
                }
                process.waitFor()
                if (wlanMac != null) return wlanMac
                if (fallbackMac != null) return fallbackMac
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                process?.destroy()
            }
            return null
        }

        /**
         * Tier 2: Derives MAC address from IPv6 Link-Local address (EUI-64 standard).
         */
        private fun getMacFromIpv6LinkLocal(): String? {
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                val wlanIface = interfaces.firstOrNull { it.name.contains("wlan", ignoreCase = true) }
                    ?: interfaces.firstOrNull { it.name.contains("eth", ignoreCase = true) }
                    ?: return null

                for (addr in Collections.list(wlanIface.inetAddresses)) {
                    if (addr is Inet6Address && addr.isLinkLocalAddress) {
                        val bytes = addr.address
                        // Standard EUI-64: bytes[11] == 0xFF && bytes[12] == 0xFE
                        if (bytes.size >= 16 && bytes[11] == 0xFF.toByte() && bytes[12] == 0xFE.toByte()) {
                            val mac = ByteArray(6)
                            mac[0] = (bytes[8].toInt() xor 0x02).toByte()
                            mac[1] = bytes[9]
                            mac[2] = bytes[10]
                            mac[3] = bytes[13]
                            mac[4] = bytes[14]
                            mac[5] = bytes[15]

                            val sb = StringBuilder(17)
                            for (b in mac) {
                                sb.append(String.format(Locale.US, "%02X:", b))
                            }
                            if (sb.isNotEmpty()) sb.deleteCharAt(sb.length - 1)
                            val res = sb.toString()
                            if (res.isNotEmpty() && !res.equals("02:00:00:00:00:00", ignoreCase = true)) {
                                return res
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * Tier 3: Standard NetworkInterface.hardwareAddress.
         */
        private fun getMacFromHardwareAddress(): String? {
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                val sorted = interfaces.sortedBy { iface ->
                    when {
                        iface.name.contains("wlan", ignoreCase = true) -> 0
                        iface.name.contains("eth", ignoreCase = true) -> 1
                        else -> 2
                    }
                }

                for (nif in sorted) {
                    if (nif.isLoopback || nif.isVirtual) continue

                    val macBytes = nif.hardwareAddress ?: continue
                    if (macBytes.isEmpty()) continue

                    val sb = StringBuilder(17)
                    for (b in macBytes) {
                        sb.append(String.format(Locale.US, "%02X:", b))
                    }
                    if (sb.isNotEmpty()) {
                        sb.deleteCharAt(sb.length - 1)
                    }

                    val mac = sb.toString()
                    if (mac.isNotEmpty() &&
                        !mac.equals("00:00:00:00:00:00", ignoreCase = true) &&
                        !mac.equals("02:00:00:00:00:00", ignoreCase = true)) {
                        return mac
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * Tier 4: Reads from common sysfs virtual device files.
         */
        private fun getMacFromFile(): String? {
            val paths = listOf("/sys/class/net/wlan0/address", "/sys/class/net/eth0/address")
            for (path in paths) {
                try {
                    val file = File(path)
                    if (file.exists() && file.canRead()) {
                        val content = file.readText().trim()
                        if (MAC_REGEX.matches(content) &&
                            !content.equals("00:00:00:00:00:00", ignoreCase = true) &&
                            !content.equals("02:00:00:00:00:00", ignoreCase = true)) {
                            return content.uppercase(Locale.US)
                        }
                    }
                } catch (_: Exception) {}
            }
            return null
        }
    }

    /**
     * Flow of device configuration stream.
     */
    val configFlow: Flow<DeviceConfig> = appContext.dataStore.data.map { prefs ->
        DeviceConfig(
            apiBaseUrl = prefs[KEY_API_BASE_URL] ?: "",
            apiSaltKey = prefs[KEY_API_SALT_KEY] ?: "",
            macAddress = prefs[KEY_MAC_ADDRESS] ?: "",
            brokerHost = prefs[KEY_BROKER_HOST] ?: "",
            brokerPort = prefs[KEY_BROKER_PORT],
            enableTls = prefs[KEY_ENABLE_TLS] ?: false,
            cleanStart = prefs[KEY_CLEAN_START] ?: false,
            keepAlive = prefs[KEY_KEEP_ALIVE],
            reconnectDelay = prefs[KEY_RECONNECT_DELAY],
            healthcheckInterval = prefs[KEY_HEALTHCHECK_INTERVAL],
            bioSyncInterval = prefs[KEY_BIO_SYNC_INTERVAL],
            doorSyncInterval = prefs[KEY_DOOR_SYNC_INTERVAL],
            bioSyncTimeout = prefs[KEY_BIO_SYNC_TIMEOUT],
            doorId = prefs[KEY_DOOR_ID],
            direction = prefs[KEY_DIRECTION] ?: "IN",
            isDeviceDisable = prefs[KEY_DEVICE_DISABLE] ?: false,
            isEmergencyOpen = prefs[KEY_EMERGENCY_OPEN] ?: false
        )
    }

    /**
     * Saves user configuration from settings screen.
     */
    suspend fun saveSettings(
        apiBaseUrl: String,
        apiSaltKey: String,
        macAddress: String,
        brokerHost: String,
        brokerPort: Int?,
        enableTls: Boolean,
        cleanStart: Boolean,
        keepAlive: Int?,
        reconnectDelay: Int?,
        healthcheckInterval: Int?,
        bioSyncInterval: Int?,
        doorSyncInterval: Int?,
        bioSyncTimeout: Int?
    ) {
        appContext.dataStore.edit { prefs ->
            prefs[KEY_API_BASE_URL] = apiBaseUrl.trim()
            prefs[KEY_API_SALT_KEY] = apiSaltKey.trim()
            prefs[KEY_MAC_ADDRESS] = macAddress.trim()
            prefs[KEY_BROKER_HOST] = brokerHost.trim()
            prefs[KEY_ENABLE_TLS] = enableTls
            prefs[KEY_CLEAN_START] = cleanStart

            if (brokerPort != null) prefs[KEY_BROKER_PORT] = brokerPort else prefs.remove(KEY_BROKER_PORT)
            if (keepAlive != null) prefs[KEY_KEEP_ALIVE] = keepAlive else prefs.remove(KEY_KEEP_ALIVE)
            if (reconnectDelay != null) prefs[KEY_RECONNECT_DELAY] = reconnectDelay else prefs.remove(KEY_RECONNECT_DELAY)
            if (healthcheckInterval != null) prefs[KEY_HEALTHCHECK_INTERVAL] = healthcheckInterval else prefs.remove(KEY_HEALTHCHECK_INTERVAL)
            if (bioSyncInterval != null) prefs[KEY_BIO_SYNC_INTERVAL] = bioSyncInterval else prefs.remove(KEY_BIO_SYNC_INTERVAL)
            if (doorSyncInterval != null) prefs[KEY_DOOR_SYNC_INTERVAL] = doorSyncInterval else prefs.remove(KEY_DOOR_SYNC_INTERVAL)
            if (bioSyncTimeout != null) prefs[KEY_BIO_SYNC_TIMEOUT] = bioSyncTimeout else prefs.remove(KEY_BIO_SYNC_TIMEOUT)
        }
    }

    /**
     * Updates runtime state received from server.
     */
    suspend fun updateRuntimeState(
        doorId: Int? = null,
        direction: String? = null,
        isDeviceDisable: Boolean? = null,
        isEmergencyOpen: Boolean? = null
    ) {
        appContext.dataStore.edit { prefs ->
            doorId?.let { prefs[KEY_DOOR_ID] = it }
            direction?.let { prefs[KEY_DIRECTION] = it }
            isDeviceDisable?.let { prefs[KEY_DEVICE_DISABLE] = it }
            isEmergencyOpen?.let { prefs[KEY_EMERGENCY_OPEN] = it }
        }
    }
}
