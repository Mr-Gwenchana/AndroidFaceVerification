package com.bachelor.projectiii.interfaces.faceProcessing

import androidx.camera.core.ImageProxy
import com.bachelor.projectiii.models.FaceDetectionResModel

interface IFaceDetector {
    fun detect(
        imageProxy: ImageProxy,
        onSuccess: (List<FaceDetectionResModel>) -> Unit,
        onError: (Exception) -> Unit = {}
    )
    fun release()
}
