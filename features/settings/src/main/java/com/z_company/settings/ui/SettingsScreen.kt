package com.z_company.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.GenericError
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.DateAndTimeConverter.getMonthFullText
import com.z_company.domain.entities.User
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.route.LocoType
import com.z_company.settings.viewmodel.SettingsUiState
import java.util.Calendar
import java.util.Calendar.MONTH
import com.z_company.core.R as CoreR


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsUiState: SettingsUiState,
    currentSettings: UserSettings?,
    currentUser: User?,
    resetSaveState: () -> Unit,
    onSaveClick: () -> Unit,
    onBack: () -> Unit,
    onSettingSaved: () -> Unit,
    minTimeRestChanged: (String) -> Unit,
    onLogOut: () -> Unit,
    onSync: () -> Unit,
    onLoading: () -> Unit,
) {
    Scaffold(topBar = {
        TopAppBar(navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = CoreR.drawable.ic_arrow_back),
                    contentDescription = stringResource(id = CoreR.string.cd_back)
                )
            }
        }, title = {
            Text(text = stringResource(id = CoreR.string.settings))
        }, actions = {
            TextButton(onClick = onSaveClick) {
                Text(text = "Готово", style = AppTypography.getType().bodyMedium)
            }
        },
            colors = TopAppBarDefaults.topAppBarColors().copy(
                containerColor = Color.Transparent,
            )
        )
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
                        onLogOut = onLogOut,
                        onSync = onSync,
                        onLoading = onLoading,
                        updateRepoState = settingsUiState.updateRepositoryState,
                        updateAtState = settingsUiState.updateAt
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
    onLogOut: () -> Unit,
    onSync: () -> Unit,
    onLoading: () -> Unit,
    updateRepoState: ResultState<Unit>,
    updateAtState: ResultState<Long>
) {
    Column(modifier = modifier.padding(start = 12.dp, end = 12.dp, top = 16.dp)) {
        SettingScreenContent(
            settingDetail = settingDetail,
            currentSettings = currentSettings,
            minTimeRestChanged = minTimeRestChanged,
            onLogOut = onLogOut,
            onSync = onSync,
            onLoading = onLoading,
            updateRepoState = updateRepoState,
            userDetail = userDetail,
            currentUser = currentUser,
            updateAtState = updateAtState
        )
//        UserScreenContent(
//            userDetail = userDetail,
//            currentUser = currentUser,
//            onLogOut = onLogOut,
//            onSync = onSync,
//            onLoading = onLoading,
//            updateRepoState = updateRepoState
//        )
    }
}

@Composable
fun SettingScreenContent(
    modifier: Modifier = Modifier,
    settingDetail: ResultState<UserSettings?>,
    currentSettings: UserSettings?,
    minTimeRestChanged: (String) -> Unit,
    onLogOut: () -> Unit,
    onSync: () -> Unit,
    onLoading: () -> Unit,
    updateRepoState: ResultState<Unit>,
    userDetail: ResultState<User?>,
    currentUser: User?,
    updateAtState: ResultState<Long>
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val styleTitle = AppTypography.getType().bodySmall
    val styleSybTitle = AppTypography.getType().bodyMedium
    val styleData = AppTypography.getType().bodyMedium

    Box(modifier = modifier.fillMaxWidth()) {
        AsyncData(resultState = settingDetail) {
            currentSettings?.let { userSettings ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(start = 16.dp, bottom = 6.dp),
                            text = "НОРМА ЧАСОВ",
                            style = styleTitle
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = Shapes.medium
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val currentMonth =
                                    Calendar.getInstance().get(MONTH).getMonthFullText()
                                Text(text = currentMonth, style = styleSybTitle)
                                Text(
                                    text = ConverterLongToTime.getTimeInStringFormat(userSettings.minTimeRest),
                                    style = styleData
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = Shapes.medium
                                )
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Отдых в ПО", style = styleSybTitle)
                                    Text(
                                        text = ConverterLongToTime.getTimeInStringFormat(
                                            userSettings.minTimeRest
                                        ),
                                        style = styleData
                                    )
                                }
                                HorizontalDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Домашний отдых", style = styleSybTitle)
                                    Text(
                                        text = ConverterLongToTime.getTimeInStringFormat(
                                            userSettings.minTimeHomeRest
                                        ),
                                        style = styleData
                                    )
                                }
                            }
                        }
                        Text(
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                            text = "Установите время минимального отдыха. Это значение будет использовано при расчете отдыха после поездки.",
                            style = AppTypography.getType().labelMedium
                        )
                    }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = Shapes.medium
                                )
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val textLocoType = when (userSettings.defaultLocoType) {
                                        LocoType.ELECTRIC -> "Электровоз"
                                        LocoType.DIESEL -> "Тепловоз"
                                    }
                                    Text(text = "Локомотив", style = styleSybTitle)
                                    Text(
                                        text = textLocoType,
                                        style = styleData
                                    )
                                }
                                HorizontalDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Время работы", style = styleSybTitle)
                                    Text(
                                        text = ConverterLongToTime.getTimeInStringFormat(
                                            userSettings.defaultWorkTime
                                        ),
                                        style = styleData
                                    )
                                }
                            }
                        }
                        Text(
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                            text = "Эти значения будут установлены по умолчанию при создании нового маршрута.",
                            style = AppTypography.getType().labelMedium
                        )
                    }
                    Text(
                        modifier = Modifier
                            .padding(start = 16.dp, bottom = 6.dp),
                        text = "АККАУНТ",
                        style = styleTitle
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = Shapes.medium
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {

                                Text(text = "E-mail", style = styleSybTitle)
                                Text(
                                    text = currentUser?.email ?: "",
                                    style = styleData
                                )
                            }
                        }


                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = Shapes.medium
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Синхронизация", style = styleSybTitle)
                                AsyncData(resultState = updateAtState) { updateAt ->
                                    updateAt?.let { timeInMillis ->
                                        val textSyncDate =
                                            DateAndTimeConverter.getDateAndTime(timeInMillis)

                                        Text(
                                            text = textSyncDate,
                                            style = styleData
                                        )
                                    }
                                }
                                AsyncData(resultState = updateRepoState) {
                                    // !!!
                                    Icon(
                                        modifier = Modifier.clickable { onSync() },
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null
                                    )
                                }
                            }
                        }

                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = Shapes.medium
                                ),
                            onClick = { onLogOut() }) {
                            Text(text = "Выйти", color = MaterialTheme.colorScheme.error)
                        }
//                    Button(onClick = { onLoading() }) {
//                        Text("loading")
//                    }
                    }
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
    onLogOut: () -> Unit,
    onSync: () -> Unit,
    onLoading: () -> Unit,
    updateRepoState: ResultState<Unit>
) {
    val styleTitle = AppTypography.getType().bodySmall
    val styleSybTitle = AppTypography.getType().bodyMedium
    val styleData = AppTypography.getType().bodyMedium

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        AsyncData(resultState = userDetail) {
            currentUser?.let { user ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {

                }
            }
        }
    }
}