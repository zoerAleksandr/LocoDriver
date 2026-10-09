package com.z_company.route.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.z_company.route.ui.settings.SettingsPrivacyContent
import com.z_company.route.ui.settings.SettingsTrackHistoryContent
import com.z_company.route.viewmodel.TrackHistoryViewModel

/** «Данные и конфиденциальность» — отдельный экран, открывается из Профиля → «ДАННЫЕ» (SCREEN_SPECS §17.7, §18). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataPrivacyScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(
                            text = "Готово",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                },
                title = {
                    Text(
                        text = "Данные и конфиденциальность",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors().copy(containerColor = Color.Transparent),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { SettingsPrivacyContent() }
    }
}

/** «Мои треки» — отдельный экран, открывается из Профиля → «ДАННЫЕ» (SCREEN_SPECS §17.8, §18). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackHistoryScreen(viewModel: TrackHistoryViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(com.z_company.core.R.drawable.ic_arrow_back),
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                title = {
                    Text(
                        text = "Мои треки",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors().copy(containerColor = Color.Transparent),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { SettingsTrackHistoryContent(viewModel = viewModel) }
    }
}
