package com.bachelor.projectiii.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

class CameraPermission(
    private val context: Context,
    private val launcher: ActivityResultLauncher<String>,
    private val hasPermissionState: MutableState<Boolean>
) {
    val permission: String = Manifest.permission.CAMERA

    val hasCameraPermission: Boolean
        get() = hasPermissionState.value

    fun isGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun grant() {
        if (!isGranted()) {
            launcher.launch(permission)
        }
    }
}

@Composable
fun rememberInstOfCameraPermission(): CameraPermission {
    val context = LocalContext.current
    val hasPermissionState = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermissionState.value = isGranted
    }

    return remember(context, launcher) {
        CameraPermission(context, launcher, hasPermissionState)
    }
}
