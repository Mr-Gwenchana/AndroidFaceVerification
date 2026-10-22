package com.bachelor.projectiii.interfaces

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

interface IFaceAnalyzer : ImageAnalysis.Analyzer {
    override fun analyze(imageProxy: ImageProxy)
    fun release()
}
