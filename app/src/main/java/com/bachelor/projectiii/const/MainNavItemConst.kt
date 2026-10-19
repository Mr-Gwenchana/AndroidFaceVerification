package com.bachelor.projectiii.`const`

import com.bachelor.projectiii.R
import com.bachelor.projectiii.models.NavItemModel

object MainNavRoutes {
    const val HOME = "home"
    const val FACE_RECOGNITION = "face_recognition"
    const val SETTINGS = "settings"
}

val mainNavItems = listOf(
    NavItemModel(
        route = MainNavRoutes.FACE_RECOGNITION,
        label = "Face ID",
        iconRes = R.drawable.face_id_icon
    )
)
