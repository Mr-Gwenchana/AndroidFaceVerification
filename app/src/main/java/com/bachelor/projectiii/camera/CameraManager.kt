package com.bachelor.projectiii.camera

import android.content.Context
import android.hardware.camera2.CameraManager as SystemCameraManager
import android.util.Log
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ConcurrentCamera
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.concurrent.futures.await

class CameraManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private var cameraProvider: ProcessCameraProvider? = null

    suspend fun initialize(): Boolean {
        if (cameraProvider != null) return true
        return try {
            cameraProvider = ProcessCameraProvider.getInstance(appContext).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khởi tạo ProcessCameraProvider", e)
            false
        }
    }

    fun startCameras(
        lifecycleOwner: LifecycleOwner,
        primaryPreviewView: PreviewView,
        secondaryPreviewView: PreviewView? = null,
        enablePreview: Boolean = true,
        enableDualCamera: Boolean = true,
        enableImageAnalysis: Boolean = false,
        analyzer: ImageAnalysis.Analyzer? = null
    ) {
        val provider = cameraProvider ?: return
        try {
            provider.unbindAll()

            val systemCameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as SystemCameraManager
            val cameraIds = systemCameraManager.cameraIdList

            val primaryUseCases = UseCaseGroup.Builder()

            if (enablePreview) {
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(primaryPreviewView.surfaceProvider)
                }
                primaryUseCases.addUseCase(preview)
            }

            if (enableImageAnalysis && analyzer != null) {
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(ContextCompat.getMainExecutor(appContext), analyzer)
                primaryUseCases.addUseCase(analysis)
            }

            val primaryGroup = primaryUseCases.build()

            if (enableDualCamera && secondaryPreviewView != null && cameraIds.size >= 2) {
                val id1 = cameraIds[0]
                val id2 = cameraIds[1]

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

                val secondaryUseCases = UseCaseGroup.Builder()
                if (enablePreview) {
                    val preview2 = Preview.Builder().build().also {
                        it.setSurfaceProvider(secondaryPreviewView.surfaceProvider)
                    }
                    secondaryUseCases.addUseCase(preview2)
                }

                try {
                    val config1 = ConcurrentCamera.SingleCameraConfig(
                        selector1,
                        primaryGroup,
                        lifecycleOwner
                    )
                    val config2 = ConcurrentCamera.SingleCameraConfig(
                        selector2,
                        secondaryUseCases.build(),
                        lifecycleOwner
                    )
                    provider.bindToLifecycle(listOf(config1, config2))
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi bind Concurrent Camera: ${e.message}", e)
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, selector1, primaryGroup)
                }
            } else if (cameraIds.isNotEmpty()) {
                val id1 = cameraIds[0]
                val selector1 = CameraSelector.Builder()
                    .addCameraFilter { cameras ->
                        cameras.filter { Camera2CameraInfo.from(it).cameraId == id1 }
                    }
                    .build()
                provider.bindToLifecycle(lifecycleOwner, selector1, primaryGroup)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi startCameras: ${e.message}", e)
        }
    }

    fun unbindAll() {
        cameraProvider?.unbindAll()
    }

    companion object {
        private const val TAG = "CameraManager"

        @Volatile
        private var instance: CameraManager? = null

        fun getInstance(context: Context): CameraManager {
            return instance ?: synchronized(this) {
                instance ?: CameraManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
