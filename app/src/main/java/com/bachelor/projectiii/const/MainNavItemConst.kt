package com.bachelor.projectiii.`const`

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import com.bachelor.projectiii.models.NavItemModel

object MainNavRoutes {
    const val HOME = "home"
    const val FACE_RECOGNITION = "face_recognition"
    const val SETTINGS = "settings"
}

val mainNavItems = listOf(
    NavItemModel(
        route = MainNavRoutes.FACE_RECOGNITION,
        icon = Icons.Default.Face,
        label = "Face ID"
    )
)
