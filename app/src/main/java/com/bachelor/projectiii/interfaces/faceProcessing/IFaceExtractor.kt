package com.bachelor.projectiii.interfaces.faceProcessing

import android.graphics.Bitmap

interface IFaceExtractor {
    fun extractEmbedding(faceBitmap: Bitmap): FloatArray?
    fun release()
}
