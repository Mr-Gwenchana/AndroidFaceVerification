package com.bachelor.projectiii.core

import android.content.Context
import com.bachelor.projectiii.camera.CameraManager
import com.bachelor.projectiii.data.local.prefs.DevicePrefs

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

    private fun checkInitialized() {
        check(::applicationContext.isInitialized) {
            "ServiceLocator is not initialized. Please call ServiceLocator.init(context) first."
        }
    }
}
