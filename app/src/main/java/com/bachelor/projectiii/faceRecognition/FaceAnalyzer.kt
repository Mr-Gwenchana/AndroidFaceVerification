package com.bachelor.projectiii.faceRecognition

import androidx.camera.core.ImageProxy
import com.bachelor.projectiii.interfaces.IFaceAnalyzer
import com.bachelor.projectiii.models.FaceDetectionResModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ImageAnalysis.Analyzer for real-time face detection using Google ML Kit.
 */
class FaceAnalyzer(
    private val onResult: (List<FaceDetectionResModel>) -> Unit
) : IFaceAnalyzer {

    private val isProcessing = AtomicBoolean(false)

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
        // Drop frames if previous frame is still processing
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
