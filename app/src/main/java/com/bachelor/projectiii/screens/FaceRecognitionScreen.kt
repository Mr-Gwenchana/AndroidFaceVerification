package com.bachelor.projectiii.screens

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bachelor.projectiii.camera.rememberInstOfCameraPermission
import com.bachelor.projectiii.components.FaceBoundingBoxOverlay
import com.bachelor.projectiii.core.ServiceLocator
import com.bachelor.projectiii.faceRecognition.FaceAnalyzer
import com.bachelor.projectiii.interfaces.IFaceAnalyzer
import com.bachelor.projectiii.models.FaceDetectionResModel

@Composable
fun FaceRecognitionScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermission = rememberInstOfCameraPermission()
    val hasCameraPermission = cameraPermission.hasCameraPermission

    val cameraManager = remember { ServiceLocator.cameraManager }
    var isCameraInitialized by remember { mutableStateOf(false) }

    var previewView1 by remember { mutableStateOf<PreviewView?>(null) }
    var previewView2 by remember { mutableStateOf<PreviewView?>(null) }

    var detectedFaces by remember { mutableStateOf<List<FaceDetectionResModel>>(emptyList()) }
    val faceAnalyzer: IFaceAnalyzer = remember {
        FaceAnalyzer { faces -> detectedFaces = faces }
    }

    LaunchedEffect(Unit) {
        cameraPermission.grant()
        isCameraInitialized = cameraManager.initialize()
    }

    LaunchedEffect(hasCameraPermission, isCameraInitialized, previewView1, previewView2) {
        val view1 = previewView1 ?: return@LaunchedEffect
        val view2 = previewView2 ?: return@LaunchedEffect
        if (!hasCameraPermission || !isCameraInitialized) return@LaunchedEffect

        cameraManager.startCameras(
            lifecycleOwner = lifecycleOwner,
            primaryPreviewView = view1,
            secondaryPreviewView = view2,
            enablePreview = true,
            enableDualCamera = true,
            enableImageAnalysis = true,
            analyzer = faceAnalyzer
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraManager.unbindAll()
            faceAnalyzer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEEEEEE))
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            previewView1 = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                FaceBoundingBoxOverlay(
                    detectedFaces = detectedFaces,
                    isMirrored = true,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEEEEEE))
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            previewView2 = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        IconButton(
            onClick = {
                cameraManager.unbindAll()
                onNavigateBack()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(44.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.White.copy(alpha = 0.85f)
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Quay lại",
                tint = Color(0xFF1E293B)
            )
        }
    }
}
