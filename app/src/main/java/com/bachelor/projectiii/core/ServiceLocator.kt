package com.bachelor.projectiii.core

import android.content.Context
import androidx.room.Room
import com.bachelor.projectiii.camera.CameraManager
import com.bachelor.projectiii.data.local.AppDatabase
import com.bachelor.projectiii.data.local.prefs.DevicePrefs
import com.bachelor.projectiii.mqtt.MqttManager

/**
 * Manual Dependency Injection (Service Locator) with lazy singleton evaluation.
 * Avoids reflection and code generation overhead for 1 GB RAM devices.
 */
object ServiceLocator {

    private lateinit var applicationContext: Context

    /**
     * Initializes ServiceLocator with application context.
     * Should be invoked in Application or MainActivity onCreate.
     */
    fun init(context: Context) {
        if (!::applicationContext.isInitialized) {
            applicationContext = context.applicationContext
        }
    }

    /**
     * Singleton instance of Room AppDatabase.
     */
    val appDatabase: AppDatabase by lazy {
        checkInitialized()
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "project_iii.db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    /**
     * Singleton instance of DevicePrefs (DataStore Preferences).
     */
    val devicePrefs: DevicePrefs by lazy {
        checkInitialized()
        DevicePrefs(applicationContext)
    }

    /**
     * Singleton instance of CameraManager (Dual CameraX controller).
     */
    val cameraManager: CameraManager by lazy {
        checkInitialized()
        CameraManager.getInstance(applicationContext)
    }

    /**
     * Singleton instance of NetworkMonitor.
     */
    val networkMonitor: NetworkMonitor by lazy {
        checkInitialized()
        NetworkMonitor(applicationContext)
    }

    /**
     * Singleton instance of MqttManager.
     */
    val mqttManager: MqttManager by lazy {
        checkInitialized()
        MqttManager(
            networkMonitor = networkMonitor,
            devicePrefs = devicePrefs
        )
    }

    private fun checkInitialized() {
        check(::applicationContext.isInitialized) {
            "ServiceLocator is not initialized. Please call ServiceLocator.init(context) first."
        }
    }
}
