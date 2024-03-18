package com.z_company.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.GenericError
import com.z_company.domain.entities.UserSettings
import com.z_company.settings.viewmodel.SettingsUiState
import com.z_company.core.R as CoreR


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsUiState: SettingsUiState,
    currentSettings: UserSettings?,
    resetSaveState: () -> Unit,
    onBackPressed: () -> Unit,
    onSaveClick: () -> Unit,
    onSettingSaved: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = CoreR.drawable.ic_arrow_back),
                    contentDescription = stringResource(id = CoreR.string.cd_back)
                )
            }
        }, title = {
            Text(text = stringResource(id = CoreR.string.settings))
        }, actions = {
            ClickableText(text = AnnotatedString(text = stringResource(id = CoreR.string.button_save)),
                onClick = { onSaveClick.invoke() }

            )
        })
    }) {
        Box(Modifier.padding(it)) {
            AsyncData(resultState = settingsUiState.settingDetails) {
                currentSettings?.let { userSettings ->
                    AsyncData(
                        resultState = settingsUiState.saveSettings,
                        errorContent = {
                            GenericError(
                                onDismissAction = resetSaveState
                            )
                        }) {
                        if (settingsUiState.saveSettings is ResultState.Success) {
                            LaunchedEffect(settingsUiState.saveSettings) {
                                onSettingSaved()
                            }
                        } else {
                            SettingScreenContent(
                                userSettings = userSettings,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingScreenContent(
    userSettings: UserSettings
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Yellow)
    ) {
        Text(userSettings.minTimeRest.toString())
    }
}