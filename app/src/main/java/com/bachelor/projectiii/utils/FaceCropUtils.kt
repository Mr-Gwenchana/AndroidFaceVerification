package com.bachelor.projectiii.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import androidx.camera.core.ImageProxy

object FaceCropUtils {
    fun cropFace(
        imageProxy: ImageProxy,
        boundingBox: Rect,
        targetSize: Int = 112
    ): Bitmap? {
        val rawBitmap = try {
            imageProxy.toBitmap()
        } catch (e: Exception) {
            return null
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val rotatedBitmap = if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            if (rotated != rawBitmap) rawBitmap.recycle()
            rotated
        } else {
            rawBitmap
        }

        val left = maxOf(0, boundingBox.left)
        val top = maxOf(0, boundingBox.top)
        val width = minOf(boundingBox.width(), rotatedBitmap.width - left)
        val height = minOf(boundingBox.height(), rotatedBitmap.height - top)

        if (width <= 10 || height <= 10) {
            rotatedBitmap.recycle()
            return null
        }

        val cropped = Bitmap.createBitmap(rotatedBitmap, left, top, width, height)
        if (cropped != rotatedBitmap) rotatedBitmap.recycle()

        val scaled = Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
        if (scaled != cropped) cropped.recycle()

        return scaled
    }
}
