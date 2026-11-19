package com.bachelor.projectiii.faceRecognition

import android.content.Context
import androidx.camera.core.ImageProxy
import com.bachelor.projectiii.core.ServiceLocator
import com.bachelor.projectiii.faceRecognition.detectors.GgMlKitDetector
import com.bachelor.projectiii.faceRecognition.extractors.MobileFaceNetExtractor
import com.bachelor.projectiii.interfaces.IFaceAnalyzer
import com.bachelor.projectiii.interfaces.faceProcessing.IFaceDetector
import com.bachelor.projectiii.interfaces.faceProcessing.IFaceExtractor
import com.bachelor.projectiii.models.FaceDetectionResModel
import com.bachelor.projectiii.models.FaceMatchResModel
import com.bachelor.projectiii.utils.FaceCropUtils
import com.bachelor.projectiii.utils.FaceRecognitionUtils
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ImageAnalysis.Analyzer orchestrating face detection, face cropping, embedding extraction,
 * and 1:N biometric matching against cached templates.
 */
class FaceAnalyzer(
    private val detector: IFaceDetector,
    private val extractor: IFaceExtractor,
    private val templateCache: FaceTemplateCache = ServiceLocator.faceTemplateCache,
    private val onResult: (List<FaceDetectionResModel>, FaceMatchResModel?) -> Unit
) : IFaceAnalyzer {

    constructor(
        context: Context,
        detector: IFaceDetector = GgMlKitDetector(),
        extractor: IFaceExtractor = MobileFaceNetExtractor(context),
        templateCache: FaceTemplateCache = ServiceLocator.faceTemplateCache,
        onResult: (List<FaceDetectionResModel>, FaceMatchResModel?) -> Unit
    ) : this(
        detector = detector,
        extractor = extractor,
        templateCache = templateCache,
        onResult = onResult
    )

    private val isProcessing = AtomicBoolean(false)
    private val isReleased = AtomicBoolean(false)

    override fun analyze(imageProxy: ImageProxy) {
        // Drop incoming frames if previous frame is still processing or analyzer is released
        if (isReleased.get() || !isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        try {
            detector.detect(
                imageProxy = imageProxy,
                onSuccess = { faces ->
                    if (isReleased.get()) {
                        imageProxy.close()
                        isProcessing.set(false)
                        return@detect
                    }

                    val primaryFace = faces.firstOrNull()
                    if (primaryFace == null) {
                        imageProxy.close()
                        onResult(emptyList(), null)
                        isProcessing.set(false)
                        return@detect
                    }

                    // Crop 112x112 face bitmap from ImageProxy
                    val faceBitmap = FaceCropUtils.cropFace(imageProxy, primaryFace.boundingBox)

                    // Close imageProxy early to release CameraX hardware buffer immediately
                    imageProxy.close()

                    var matchResult: FaceMatchResModel? = null
                    if (faceBitmap != null) {
                        try {
                            val liveVector = extractor.extractEmbedding(faceBitmap)
                            if (liveVector != null && !isReleased.get()) {
                                matchResult = FaceRecognitionUtils.findBestMatch(
                                    liveVector = liveVector,
                                    profiles = templateCache.getProfiles()
                                )
                            }
                        } finally {
                            // Explicitly recycle Bitmap native memory to prevent GC spikes
                            faceBitmap.recycle()
                        }
                    }

                    if (!isReleased.get()) {
                        onResult(listOf(primaryFace), matchResult)
                    }
                    isProcessing.set(false)
                },
                onError = {
                    imageProxy.close()
                    isProcessing.set(false)
                }
            )
        } catch (_: Exception) {
            imageProxy.close()
            isProcessing.set(false)
        }
    }

    override fun release() {
        if (isReleased.compareAndSet(false, true)) {
            detector.release()
            extractor.release()
        }
    }
}
