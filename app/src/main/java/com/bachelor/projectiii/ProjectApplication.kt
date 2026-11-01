package com.bachelor.projectiii

import android.app.Application
import com.bachelor.projectiii.core.ServiceLocator

/**
 * Base Application class initializing global singletons before any Activity or Service runs.
 */
class ProjectApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
