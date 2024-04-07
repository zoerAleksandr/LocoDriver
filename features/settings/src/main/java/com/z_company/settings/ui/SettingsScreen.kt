package com.z_company.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.GenericError
import com.z_company.domain.entities.User
import com.z_company.domain.entities.UserSettings
import com.z_company.settings.viewmodel.SettingsUiState
import kotlinx.coroutines.launch
import com.z_company.core.R as CoreR


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsUiState: SettingsUiState,
    currentSettings: UserSettings?,
    currentUser: User?,
    resetSaveState: () -> Unit,
    onSaveClick: () -> Unit,
    onSettingSaved: () -> Unit,
    minTimeRestChanged: (String) -> Unit,
    onLogOut: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(navigationIcon = {
            IconButton(onClick = onSaveClick) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = CoreR.drawable.ic_arrow_back),
                    contentDescription = stringResource(id = CoreR.string.cd_back)
                )
            }
        }, title = {
            Text(text = stringResource(id = CoreR.string.settings))
        })
    }) {
        Box(Modifier.padding(it)) {
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
                    ScreenContent(
                        modifier = Modifier,
                        settingDetail = settingsUiState.settingDetails,
                        userDetail = settingsUiState.userDetailsState,
                        currentSettings = currentSettings,
                        currentUser = currentUser,
                        minTimeRestChanged = minTimeRestChanged,
                        onLogOut = onLogOut
                    )
                }
            }
        }
    }
}

@Composable
fun ScreenContent(
    modifier: Modifier = Modifier,
    settingDetail: ResultState<UserSettings?>,
    currentSettings: UserSettings?,
    userDetail: ResultState<User?>,
    currentUser: User?,
    minTimeRestChanged: (String) -> Unit,
    onLogOut: () -> Unit
) {
    Column(modifier = modifier) {
        SettingScreenContent(
            settingDetail = settingDetail,
            currentSettings = currentSettings,
            minTimeRestChanged = minTimeRestChanged
        )
        UserScreenContent(
            userDetail = userDetail,
            currentUser = currentUser,
            onLogOut = onLogOut
        )
    }
}

@Composable
fun SettingScreenContent(
    modifier: Modifier = Modifier,
    settingDetail: ResultState<UserSettings?>,
    currentSettings: UserSettings?,
    minTimeRestChanged: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxWidth()) {
        AsyncData(resultState = settingDetail) {
            currentSettings?.let { userSettings ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Yellow)
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .fillMaxWidth(0.5f),
                        value = userSettings.minTimeRest.toString(),
                        onValueChange = {
                            minTimeRestChanged(it)
                        },
                        label = {
                            Text(text = "Номер", color = MaterialTheme.colorScheme.secondary)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                scope.launch {
                                    focusManager.moveFocus(FocusDirection.Down)
                                }
                            }
                        ),
                        singleLine = true
                    )
                    Text(userSettings.minTimeRest.toString())
                }
            }
        }
    }
}

@Composable
fun UserScreenContent(
    modifier: Modifier = Modifier,
    userDetail: ResultState<User?>,
    currentUser: User?,
    onLogOut: () -> Unit
) {
    Box(modifier = modifier.fillMaxWidth()) {
        AsyncData(resultState = userDetail) {
            currentUser?.let { user ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(user.name)
                    Text(user.email)
                    var name by remember {
                        mutableStateOf("")
                    }
                    OutlinedTextField(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .fillMaxWidth(0.5f),
                        value = name,
                        onValueChange = {
                            name = it
                        },
                        label = {
                            Text(text = "name", color = MaterialTheme.colorScheme.secondary)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true
                    )
                    Button(onClick = { onLogOut() }) {
                        Text("выйти")
                    }
                }
            }
        }
    }
}