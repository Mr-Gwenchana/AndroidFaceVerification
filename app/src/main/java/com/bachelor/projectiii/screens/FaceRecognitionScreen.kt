package com.bachelor.projectiii.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ConcurrentCamera
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val TAG = "FaceRecognitionScreen"

@Composable
fun FaceRecognitionScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewView1 by remember { mutableStateOf<PreviewView?>(null) }
    var previewView2 by remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                cameraProvider = future.get()
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi ProcessCameraProvider", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(hasCameraPermission, cameraProvider, previewView1, previewView2) {
        val provider = cameraProvider ?: return@LaunchedEffect
        val view1 = previewView1 ?: return@LaunchedEffect
        val view2 = previewView2 ?: return@LaunchedEffect
        if (!hasCameraPermission) return@LaunchedEffect

        try {
            provider.unbindAll()

            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraIds = cameraManager.cameraIdList
            Log.d(TAG, "Danh sách ID camera tìm thấy: ${cameraIds.joinToString()}")

            val availableConcurrent = provider.availableConcurrentCameraInfos
            Log.d(TAG, "Số cặp Concurrent Camera khả dụng: ${availableConcurrent.size}")
            for (combination in availableConcurrent) {
                val ids = combination.map { Camera2CameraInfo.from(it).cameraId }
                Log.d(TAG, "Concurrent pair: $ids")
            }

            if (cameraIds.size >= 2) {
                val id1 = cameraIds[0]
                val id2 = cameraIds[1]
                Log.d(TAG, "Lấy 2 camera đầu tiên: Camera 1 (ID: $id1), Camera 2 (ID: $id2)")

                val preview1 = Preview.Builder().build().also {
                    it.setSurfaceProvider(view1.surfaceProvider)
                }
                val preview2 = Preview.Builder().build().also {
                    it.setSurfaceProvider(view2.surfaceProvider)
                }

                val selector1 = CameraSelector.Builder()
                    .addCameraFilter { cameras ->
                        cameras.filter { Camera2CameraInfo.from(it).cameraId == id1 }
                    }
                    .build()

                val selector2 = CameraSelector.Builder()
                    .addCameraFilter { cameras ->
                        cameras.filter { Camera2CameraInfo.from(it).cameraId == id2 }
                    }
                    .build()

                try {
                    val config1 = ConcurrentCamera.SingleCameraConfig(
                        selector1,
                        UseCaseGroup.Builder().addUseCase(preview1).build(),
                        lifecycleOwner
                    )
                    val config2 = ConcurrentCamera.SingleCameraConfig(
                        selector2,
                        UseCaseGroup.Builder().addUseCase(preview2).build(),
                        lifecycleOwner
                    )

                    Log.d(TAG, "Đang bind Concurrent Camera với ID: $id1 và ID: $id2...")
                    provider.bindToLifecycle(listOf(config1, config2))
                    Log.d(TAG, "Bind Concurrent Camera thành công")
                } catch (e: Exception) {
                    Log.e(TAG, "Không thể bind Concurrent Camera: ${e.message}", e)
                    provider.unbindAll()
                    Log.d(TAG, "Fallback: Bind Camera 1 (ID: $id1)")
                    provider.bindToLifecycle(lifecycleOwner, selector1, preview1)
                }
            } else if (cameraIds.isNotEmpty()) {
                val id1 = cameraIds[0]
                val preview1 = Preview.Builder().build().also {
                    it.setSurfaceProvider(view1.surfaceProvider)
                }
                val selector1 = CameraSelector.Builder()
                    .addCameraFilter { cameras ->
                        cameras.filter { Camera2CameraInfo.from(it).cameraId == id1 }
                    }
                    .build()
                Log.d(TAG, "Chỉ có 1 camera, bind ID: $id1")
                provider.bindToLifecycle(lifecycleOwner, selector1, preview1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khởi tạo camera", e)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEEEEEE))
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            previewView1 = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEEEEEE))
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            previewView2 = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        IconButton(
            onClick = {
                cameraProvider?.unbindAll()
                onNavigateBack()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(44.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.White.copy(alpha = 0.85f)
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Quay lại",
                tint = Color(0xFF1E293B)
            )
        }
    }
}
