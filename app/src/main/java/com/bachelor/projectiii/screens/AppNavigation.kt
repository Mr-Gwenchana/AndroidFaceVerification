package com.bachelor.projectiii.screens

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bachelor.projectiii.`const`.MainNavRoutes

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MainNavRoutes.HOME
    ) {
        composable(MainNavRoutes.HOME) {
            HomeScreen(
                onNavigate = navController::navigate
            )
        }
        composable(MainNavRoutes.FACE_RECOGNITION) {
            FaceRecognitionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(MainNavRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
