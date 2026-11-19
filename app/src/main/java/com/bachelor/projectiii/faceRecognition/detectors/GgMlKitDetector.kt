package com.bachelor.projectiii.faceRecognition.detectors

import androidx.camera.core.ImageProxy
import com.bachelor.projectiii.interfaces.faceProcessing.IFaceDetector
import com.bachelor.projectiii.models.FaceDetectionResModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

class GgMlKitDetector : IFaceDetector {

    private val detector: FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .enableTracking()
            .build()
    )

    override fun detect(
        imageProxy: ImageProxy,
        onSuccess: (List<FaceDetectionResModel>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            onError(IllegalArgumentException("ImageProxy does not contain a valid media image"))
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
                    onSuccess(listOf(result))
                } else {
                    onSuccess(emptyList())
                }
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }

    override fun release() {
        detector.close()
    }
}
