package com.spendshot.android.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.spendshot.android.ui.MainApp
import com.spendshot.android.ui.screens.SettingsScreen
import com.spendshot.android.ui.screens.OnboardingScreen
import com.spendshot.android.viewmodels.MainViewModel
import com.spendshot.android.viewmodels.SettingsViewModel
import com.spendshot.android.viewmodels.BudgetViewModel
import com.spendshot.android.viewmodels.GoalsViewModel
import com.spendshot.android.data.repository.SettingsRepository
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import com.spendshot.android.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp

@Composable
fun AppNavigation(
    isShareFlow: Boolean = false,
    isAuthenticated: Boolean = false,
    onFinish: () -> Unit,
    onRequestAuthentication: (onSuccess: () -> Unit) -> Unit
) {
    val navController = rememberNavController()

    // If we assume auth is handled in Activity before calling content.
    
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val mainViewModel: MainViewModel = hiltViewModel()
    val budgetViewModel: BudgetViewModel = hiltViewModel()
    val goalsViewModel: GoalsViewModel = hiltViewModel()

    val isOnboardingComplete = settingsViewModel.isOnboardingComplete()
    val startDestination = if (!isOnboardingComplete) "onboarding" else "main_app_flow"

    NavHost(navController = navController, startDestination = startDestination) {
        
        composable("onboarding") {
             OnboardingScreen(settingsViewModel = settingsViewModel) {
                 settingsViewModel.setOnboardingComplete(true)
                 navController.navigate("main_app_flow") {
                     popUpTo("onboarding") { inclusive = true }
                 }
             }
        }

        composable("main_app_flow") {
            // MainApp handles its own bottom nav paging.
            // But we pass navigation callback for Settings.
            MainApp(
                viewModel = mainViewModel,
                settingsViewModel = settingsViewModel,
                budgetViewModel = budgetViewModel,
                goalsViewModel = goalsViewModel,
                isShareFlow = isShareFlow,
                onNavigateToSettings = {
                    navController.navigate("settings_screen")
                },
                onShareFlowComplete = onFinish,
                onRequestAuthentication = onRequestAuthentication
            )
        }

        composable("settings_screen") {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
