package com.z_company.settings.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.GenericError
import com.z_company.core.ui.component.TimeInputDialog
import com.z_company.settings.component.SelectedDialog
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.DateAndTimeConverter.getMonthFullText
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.User
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.UtilForMonthOfYear.getNormaHours
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
    currentMonthOfYear: MonthOfYear?,
    resetSaveState: () -> Unit,
    onSaveClick: () -> Unit,
    onBack: () -> Unit,
    onSettingSaved: () -> Unit,
    workTimeChanged: (Long) -> Unit,
    locoTypeChanged: (LocoType) -> Unit,
    restTimeChanged: (Long) -> Unit,
    homeRestTimeChanged: (Long) -> Unit,
    onLogOut: () -> Unit,
    onSync: () -> Unit,
    showReleaseDaySelectScreen: () -> Unit
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
                resultState = settingsUiState.settingDetails,
                errorContent = {
                    GenericError(
                        onDismissAction = resetSaveState
                    )
                }) {
                currentSettings?.let { setting ->
                    AsyncData(resultState = settingsUiState.saveSettingsState) {
                        if (settingsUiState.saveSettingsState is ResultState.Success) {
                            LaunchedEffect(settingsUiState.saveSettingsState) {
                                onSettingSaved()
                            }
                        } else {
                            SettingScreenContent(
                                currentSettings = setting,
                                currentMonthOfYear = currentMonthOfYear,
                                onLogOut = onLogOut,
                                onSync = onSync,
                                updateRepoState = settingsUiState.updateRepositoryState,
                                currentUser = currentUser,
                                updateAtState = settingsUiState.updateAt,
                                workTimeChanged = workTimeChanged,
                                locoTypeChanged = locoTypeChanged,
                                restTimeChanged = restTimeChanged,
                                homeRestTimeChanged = homeRestTimeChanged,
                                showReleaseDaySelectScreen = showReleaseDaySelectScreen
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreenContent(
    currentSettings: UserSettings,
    currentMonthOfYear: MonthOfYear?,
    workTimeChanged: (Long) -> Unit,
    locoTypeChanged: (LocoType) -> Unit,
    restTimeChanged: (Long) -> Unit,
    homeRestTimeChanged: (Long) -> Unit,
    onLogOut: () -> Unit,
    onSync: () -> Unit,
    updateRepoState: ResultState<Unit>,
    currentUser: User?,
    updateAtState: ResultState<Long>,
    showReleaseDaySelectScreen: () -> Unit
) {
    val styleTitle = AppTypography.getType().bodySmall
    val styleSybTitle = AppTypography.getType().bodyMedium
    val styleData = AppTypography.getType().bodyMedium

    var showRestDialog by remember {
        mutableStateOf(false)
    }

    var showHomeRestDialog by remember {
        mutableStateOf(false)
    }

    var showLocoTypeSelectedDialog by remember {
        mutableStateOf(false)
    }

    var showWorkTimeDialog by remember {
        mutableStateOf(false)
    }

    var visibleCalendar by remember {
        mutableStateOf(false)
    }

    val dateRangePickerState = rememberDateRangePickerState()

    if (showRestDialog) {
        TimeInputDialog(
            initValue = currentSettings.minTimeRest,
            onDismissRequest = { showRestDialog = false }
        ) { timeToLong ->
            restTimeChanged(timeToLong)
            showRestDialog = false
        }
    }

    if (showHomeRestDialog) {
        TimeInputDialog(
            initValue = currentSettings.minTimeHomeRest,
            onDismissRequest = { showHomeRestDialog = false }
        ) { timeToLong ->
            homeRestTimeChanged(timeToLong)
            showHomeRestDialog = false
        }
    }

    if (showLocoTypeSelectedDialog) {
        val locoTypeList = LocoType.values().toList()
        val indexSelected = locoTypeList.indexOf(currentSettings.defaultLocoType)

        SelectedDialog(
            onDismissRequest = { showLocoTypeSelectedDialog = false },
            onConfirmRequest = {
                showLocoTypeSelectedDialog = false
                locoTypeChanged(it)
            },
            peekList = locoTypeList,
            selectedItem = indexSelected
        )
    }

    if (showWorkTimeDialog) {
        TimeInputDialog(
            initValue = currentSettings.defaultWorkTime,
            onDismissRequest = { showWorkTimeDialog = false }
        ) { timeInLong ->
            showWorkTimeDialog = false
            workTimeChanged(timeInLong)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                visibleCalendar = !visibleCalendar
                            },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val currentMonth =
                            Calendar.getInstance().get(MONTH).getMonthFullText()
                        Text(text = currentMonth, style = styleSybTitle)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = ConverterLongToTime.getTimeInStringFormat(
                                    currentMonthOfYear?.getNormaHours()?.toLong()?.times(3_600_000)
                                ),
                                style = styleData
                            )
                            Icon(
                                modifier = Modifier.clickable {
                                    showReleaseDaySelectScreen()
                                },
                                imageVector = Icons.Outlined.KeyboardArrowRight,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
        item {
            AnimatedVisibility(visible = visibleCalendar) {
                DateRangePicker(state = dateRangePickerState, modifier = Modifier.height(500.dp))
            }
        }

        item {
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showRestDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Отдых в ПО", style = styleSybTitle)
                            val text =
                                ConverterLongToTime.getTimeInStringFormat(currentSettings.minTimeRest)
                            Text(
                                text = text,
                                style = styleData
                            )
                        }
                        HorizontalDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showHomeRestDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Домашний отдых", style = styleSybTitle)
                            val text =
                                ConverterLongToTime.getTimeInStringFormat(currentSettings.minTimeHomeRest)
                            Text(
                                text = text,
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
        }

        item {
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
                            Text(text = "Локомотив", style = styleSybTitle)
                            val textLocoType = when (currentSettings.defaultLocoType) {
                                LocoType.ELECTRIC -> "Электровоз"
                                LocoType.DIESEL -> "Тепловоз"
                            }
                            Text(
                                modifier = Modifier.clickable {
                                    showLocoTypeSelectedDialog = true
                                },
                                text = textLocoType,
                                style = styleData
                            )
                        }
                        HorizontalDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showWorkTimeDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Время работы", style = styleSybTitle)

                            val text =
                                ConverterLongToTime.getTimeInStringFormat(currentSettings.defaultWorkTime)
                            Text(
                                text = text,
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
        }

        item {
            Text(
                modifier = Modifier
                    .padding(start = 16.dp, bottom = 6.dp),
                text = "АККАУНТ",
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Синхронизация", style = styleSybTitle)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
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
                            AsyncData(
                                resultState = updateRepoState,
                                loadingContent = {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            ) {
                                Icon(
                                    modifier = Modifier.clickable { onSync() },
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }

            }
        }

        item {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = Shapes.medium
                    ),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                onClick = { onLogOut() }) {
                Text(text = "Выйти", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

