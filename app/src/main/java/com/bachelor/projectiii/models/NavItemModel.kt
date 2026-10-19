package com.bachelor.projectiii.models

import androidx.compose.ui.graphics.vector.ImageVector

data class NavItemModel(
    val route: String,
    val label: String,
    val iconRes: Int? = null,
    val icon: ImageVector? = null
)
