package com.bachelor.projectiii.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class CameraPermission(private val context: Context) {
    val permission: String = Manifest.permission.CAMERA

    fun isGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}
