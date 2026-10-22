package com.bachelor.projectiii.camera

import android.content.Context
import android.util.Log
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ConcurrentCamera
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.concurrent.futures.await
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

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

            val availableConcurrent = provider.availableConcurrentCameraInfos
            val concurrentPair = availableConcurrent.firstOrNull { it.size >= 2 }

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
                analysis.setAnalyzer(Dispatchers.Default.asExecutor(), analyzer)
                primaryUseCases.addUseCase(analysis)
            }

            val primaryGroup = primaryUseCases.build()

            if (enableDualCamera && secondaryPreviewView != null && concurrentPair != null) {
                val selector1 = concurrentPair[1].cameraSelector
                val selector2 = concurrentPair[0].cameraSelector

                val id1 = Camera2CameraInfo.from(concurrentPair[1]).cameraId
                val id2 = Camera2CameraInfo.from(concurrentPair[0]).cameraId
                Log.d(TAG, "Khởi chạy Concurrent Camera với ID: $id1 và ID: $id2")

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
            } else {
                val fallbackSelector = concurrentPair?.getOrNull(1)?.cameraSelector
                    ?: concurrentPair?.getOrNull(0)?.cameraSelector
                    ?: provider.availableCameraInfos.firstOrNull()?.cameraSelector
                    ?: CameraSelector.DEFAULT_BACK_CAMERA
                provider.bindToLifecycle(lifecycleOwner, fallbackSelector, primaryGroup)
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
