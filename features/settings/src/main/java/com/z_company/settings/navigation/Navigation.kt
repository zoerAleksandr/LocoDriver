package com.z_company.settings.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable
import androidx.navigation.compose.navigation
import com.z_company.domain.navigation.Router
import com.z_company.settings.ui.SettingsScreen
import com.z_company.settings.viewmodel.SettingsViewModel


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
                currentUser = settingsViewModel.currentUser,
                resetSaveState = settingsViewModel::resetSaveState,
                onSaveClick = settingsViewModel::saveSettings,
                onSettingSaved = router::back,
                minTimeRestChanged = settingsViewModel::changedMinTimeRest,
                onLogOut = settingsViewModel::logOut,
                onSync = settingsViewModel::onSync
            )
        }
    }
}