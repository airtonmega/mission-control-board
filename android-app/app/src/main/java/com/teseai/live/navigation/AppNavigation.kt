package com.teseai.live.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.teseai.live.ui.screens.AudioAnalysisScreen
import com.teseai.live.ui.screens.CameraAnalysisScreen
import com.teseai.live.ui.screens.CockpitLiveScreen
import com.teseai.live.ui.screens.EthicsConsentScreen
import com.teseai.live.ui.screens.InterviewSetupScreen
import com.teseai.live.ui.screens.ReportsScreen
import com.teseai.live.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    data object EthicsConsent : Screen("ethics_consent")
    data object CockpitLive : Screen("cockpit_live")
    data object CameraAnalysis : Screen("camera_analysis")
    data object AudioAnalysis : Screen("audio_analysis")
    data object InterviewSetup : Screen("interview_setup")
    data object Reports : Screen("reports")
    data object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Screen.EthicsConsent.route,
    ) {
        composable(Screen.EthicsConsent.route) {
            EthicsConsentScreen(
                onConsentAccepted = {
                    navController.navigate(Screen.CockpitLive.route) {
                        popUpTo(Screen.EthicsConsent.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Screen.CockpitLive.route) {
            CockpitLiveScreen(
                onNavigateToCamera = { navController.navigate(Screen.CameraAnalysis.route) },
                onNavigateToAudio = { navController.navigate(Screen.AudioAnalysis.route) },
                onNavigateToInterview = { navController.navigate(Screen.InterviewSetup.route) },
                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
            )
        }
        composable(Screen.CameraAnalysis.route) {
            CameraAnalysisScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.AudioAnalysis.route) {
            AudioAnalysisScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.InterviewSetup.route) {
            InterviewSetupScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Reports.route) {
            ReportsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
