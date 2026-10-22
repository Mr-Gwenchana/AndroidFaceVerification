package com.bachelor.projectiii.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.bachelor.projectiii.models.FaceDetectionResModel

@Composable
fun FaceBoundingBoxOverlay(
    detectedFaces: List<FaceDetectionResModel>,
    modifier: Modifier = Modifier,
    isMirrored: Boolean = false,
    boxColor: Color = Color(0xFF00E676)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (detectedFaces.isEmpty()) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height

        for (face in detectedFaces) {
            val rot = face.rotationDegrees
            val effectiveWidth = if (rot == 90 || rot == 270) face.imageHeight.toFloat() else face.imageWidth.toFloat()
            val effectiveHeight = if (rot == 90 || rot == 270) face.imageWidth.toFloat() else face.imageHeight.toFloat()

            if (effectiveWidth <= 0f || effectiveHeight <= 0f) continue

            val scale = maxOf(canvasWidth / effectiveWidth, canvasHeight / effectiveHeight)
            val offsetX = (canvasWidth - effectiveWidth * scale) / 2f
            val offsetY = (canvasHeight - effectiveHeight * scale) / 2f

            val rawLeft = face.boundingBox.left.toFloat() * scale + offsetX
            val rawTop = face.boundingBox.top.toFloat() * scale + offsetY
            val rawRight = face.boundingBox.right.toFloat() * scale + offsetX
            val rawBottom = face.boundingBox.bottom.toFloat() * scale + offsetY

            val left = if (isMirrored) canvasWidth - rawRight else rawLeft
            val right = if (isMirrored) canvasWidth - rawLeft else rawRight
            val top = rawTop
            val bottom = rawBottom

            val rectWidth = right - left
            val rectHeight = bottom - top

            drawRoundRect(
                color = boxColor,
                topLeft = Offset(left, top),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}
