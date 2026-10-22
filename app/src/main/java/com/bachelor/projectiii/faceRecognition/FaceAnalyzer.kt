package com.bachelor.projectiii.faceRecognition

import androidx.camera.core.ImageProxy
import com.bachelor.projectiii.interfaces.IFaceAnalyzer
import com.bachelor.projectiii.interfaces.IFaceStorageManager
import com.bachelor.projectiii.models.FaceDetectionResModel
import com.bachelor.projectiii.utils.FaceCropUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class FaceAnalyzer(
    private val storageManager: IFaceStorageManager? = null,
    private val saveIntervalMs: Long = 2000L,
    private val onResult: (List<FaceDetectionResModel>) -> Unit
) : IFaceAnalyzer {

    private val isProcessing = AtomicBoolean(false)
    private var lastSavedTime = 0L
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val detector: FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .enableTracking()
            .build()
    )

    override fun analyze(imageProxy: ImageProxy) {
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            isProcessing.set(false)
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val imageWidth = imageProxy.width
        val imageHeight = imageProxy.height
        val rotation = imageProxy.imageInfo.rotationDegrees

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                val primaryFace = faces.firstOrNull()
                if (primaryFace != null) {
                    val result = FaceDetectionResModel(
                        boundingBox = primaryFace.boundingBox,
                        trackingId = primaryFace.trackingId,
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        rotationDegrees = rotation
                    )
                    onResult(listOf(result))

                    if (storageManager != null) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastSavedTime >= saveIntervalMs) {
                            lastSavedTime = currentTime
                            val croppedBitmap = FaceCropUtils.cropFace(imageProxy, primaryFace.boundingBox, 112)
                            if (croppedBitmap != null) {
                                ioScope.launch {
                                    storageManager.saveCroppedFace(croppedBitmap, primaryFace.trackingId)
                                    croppedBitmap.recycle()
                                }
                            }
                        }
                    }
                } else {
                    onResult(emptyList())
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
                isProcessing.set(false)
            }
    }

    override fun release() {
        detector.close()
    }
}
