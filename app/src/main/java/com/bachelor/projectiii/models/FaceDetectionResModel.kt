package com.bachelor.projectiii.models

import android.graphics.Rect

data class FaceDetectionResModel(
    val boundingBox: Rect,
    val trackingId: Int? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val rotationDegrees: Int = 0
)
