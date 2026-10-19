package com.bachelor.projectiii.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bachelor.projectiii.components.DigitalClock
import com.bachelor.projectiii.components.SideNavBar
import com.bachelor.projectiii.components.SideNavItem
import com.bachelor.projectiii.`const`.mainNavItems
import com.bachelor.projectiii.models.NavItemModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navItems: List<NavItemModel> = mainNavItems,
    selectedRoute: String = "",
    onNavigate: (String) -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        SideNavBar {
            navItems.forEach { item ->
                if (item.iconRes != null) {
                    SideNavItem(
                        iconRes = item.iconRes,
                        label = item.label,
                        isSelected = selectedRoute == item.route,
                        onClick = { onNavigate(item.route) }
                    )
                } else if (item.icon != null) {
                    SideNavItem(
                        icon = item.icon,
                        label = item.label,
                        isSelected = selectedRoute == item.route,
                        onClick = { onNavigate(item.route) }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            DigitalClock()
        }
    }
}
