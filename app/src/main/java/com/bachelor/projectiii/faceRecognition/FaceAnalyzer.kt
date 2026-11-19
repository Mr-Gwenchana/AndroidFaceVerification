package com.bachelor.projectiii.faceRecognition

import androidx.camera.core.ImageProxy
import com.bachelor.projectiii.faceRecognition.detectors.GgMlKitDetector
import com.bachelor.projectiii.interfaces.IFaceAnalyzer
import com.bachelor.projectiii.interfaces.faceProcessing.IFaceDetector
import com.bachelor.projectiii.models.FaceDetectionResModel
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ImageAnalysis.Analyzer for real-time face processing.
 * Delegates face detection to [IFaceDetector].
 */
class FaceAnalyzer(
    private val detector: IFaceDetector = GgMlKitDetector(),
    private val onResult: (List<FaceDetectionResModel>) -> Unit
) : IFaceAnalyzer {

    constructor(onResult: (List<FaceDetectionResModel>) -> Unit) : this(
        detector = GgMlKitDetector(),
        onResult = onResult
    )

    private val isProcessing = AtomicBoolean(false)

    override fun analyze(imageProxy: ImageProxy) {
        // Drop frames if previous frame is still processing
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        try {
            detector.detect(
                imageProxy = imageProxy,
                onSuccess = { faces ->
                    onResult(faces)
                    imageProxy.close()
                    isProcessing.set(false)
                },
                onError = {
                    imageProxy.close()
                    isProcessing.set(false)
                }
            )
        } catch (e: Exception) {
            imageProxy.close()
            isProcessing.set(false)
        }
    }

    override fun release() {
        detector.release()
    }
}
