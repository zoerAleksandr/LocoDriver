package com.example.settings.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable
import androidx.navigation.compose.navigation
import com.example.domain.navigation.Router
import com.example.settings.ui.SettingsScreen
import com.example.settings.viewmodel.SettingsViewModel


@ExperimentalAnimationApi
fun NavGraphBuilder.settingsGraph(
    router: Router
) {
    navigation(
        route = SettingsFeature.route,
        startDestination = SettingsScreenRoute.route,
    ) {
        composable(SettingsScreenRoute.route) {
            val settingsViewModel: SettingsViewModel = viewModel()
            val uiState by settingsViewModel.uiState.collectAsState()
            SettingsScreen(
                settingsUiState = uiState,
                currentSettings = settingsViewModel.currentSettings,
                resetSaveState = settingsViewModel::resetSaveState,
                onBackPressed = router::back,
                onSaveClick = settingsViewModel::saveSettings,
                onSettingSaved = router::back
            )
        }
    }
}