package com.z_company.route.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.z_company.core.ResultState
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.ConverterLongToTime
import com.z_company.domain.entities.User
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.route.LocoType
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextOverflow
import com.z_company.core.ui.component.CustomDivider
import com.z_company.core.ui.component.TimePickerApp
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.MonthFullText.getMonthFullText
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.util.toIntOrZero
import com.z_company.route.R
import com.z_company.route.component.ConfirmEmailDialog
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.component.SwitchApp
import com.z_company.route.viewmodel.SettingsViewModel
import com.z_company.route.viewmodel.SettingsUiState
import com.z_company.route.viewmodel.TimeZoneRussia
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    settingsUiState: SettingsUiState,
    currentSettings: UserSettings?,
    onSettingSaved: () -> Unit,
    workTimeChanged: (Long) -> Unit,
    restTimeChanged: (Long) -> Unit,
    homeRestTimeChanged: (Long) -> Unit,
    logOut: () -> Unit,
    showReleaseDaySelectScreen: () -> Unit,
    resetUploadState: () -> Unit,
    resetDownloadState: () -> Unit,
    changeStartNightTime: (Int, Int) -> Unit,
    changeEndNightTime: (Int, Int) -> Unit,
    changeUsingDefaultWorkTime: (Boolean) -> Unit,
    changeConsiderFutureRoute: (Boolean) -> Unit,
    changeShowBreak: (Boolean) -> Unit,
    changeShowLocoHeating: (Boolean) -> Unit,
    changeShowLocoAuxiliary: (Boolean) -> Unit,
    changeShowLocoStatistics: (Boolean) -> Unit,
    setTimeZone: (Long) -> Unit,
    timeZoneRussiaList: List<TimeZoneRussia>,
    servicePhases: SnapshotStateList<ServicePhase>?,
    showDialogAddServicePhase: (ServicePhase) -> Unit,
    hideDialogAddServicePhase: () -> Unit,
    addServicePhase: (ServicePhase, Int) -> Unit,
    deleteServicePhase: (Int) -> Unit,
    updateServicePhase: (ServicePhase, Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val styleData = MaterialTheme.typography.bodyLarge
    val styleHint = MaterialTheme.typography.bodyMedium
    val styleTitle = MaterialTheme.typography.titleSmall

    val primaryColor = MaterialTheme.colorScheme.primary
    val noValueColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
            }
        },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TextButton(
                        onClick = viewModel::saveSettings,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.tertiary,
                            containerColor = Color.Transparent
                        )
                    ) {
                        Text(
                            text = "Сохранить",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                },
                title = {},
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = Color.Transparent,
                )
            )
        }) {
        LaunchedEffect(settingsUiState.uploadState) {
            if (settingsUiState.uploadState is ResultState.Error) {
                scope.launch {
                    snackbarHostState.showSnackbar("Ошибка выгрузки данных. \n${settingsUiState.uploadState.entity.message}.")
                }
                resetUploadState()
            }
            if (settingsUiState.uploadState is ResultState.Success) {
                scope.launch {
                    if (settingsUiState.uploadState.data == 0) {
                        snackbarHostState.showSnackbar("Все маршруты синхронизированы")
                    } else {
                        snackbarHostState.showSnackbar("Маршруты успешно сохранены на сервере. (${settingsUiState.uploadState.data})")
                    }
                }
                resetUploadState()
            }
        }

        LaunchedEffect(settingsUiState.downloadState) {
            if (settingsUiState.downloadState is ResultState.Error) {
                scope.launch {
                    snackbarHostState.showSnackbar("Ошибка загрузки данных. \n${settingsUiState.downloadState.entity.message}.")
                }
                resetDownloadState()
            }
            if (settingsUiState.downloadState is ResultState.Success) {
                scope.launch {
                    snackbarHostState.showSnackbar("Маршруты загружены. (${settingsUiState.downloadState.data})")
                }
                resetUploadState()
            }
        }

        LaunchedEffect(Unit) {
            viewModel.saveEvent.collect {
                onSettingSaved()
            }
        }

        LaunchedEffect(settingsUiState.logOutState) {
            if (settingsUiState.logOutState is ResultState.Success) {
                logOut()
            }
        }

        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )

        Box(Modifier.padding(it)) {
            currentSettings?.let { setting ->
                var showNightTimeStartDialog by remember {
                    mutableStateOf(false)
                }

                var showNightTimeEndDialog by remember {
                    mutableStateOf(false)
                }

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

                var showConfirmEmailDialog by remember {
                    mutableStateOf(false)
                }

                var departureStation by remember(settingsUiState.showDialogAddServicePhase) {
                    mutableStateOf(
                        settingsUiState.selectedServicePhase?.first?.departureStation ?: ""
                    )
                }
                var arrivalStation by remember(settingsUiState.showDialogAddServicePhase) {
                    mutableStateOf(
                        settingsUiState.selectedServicePhase?.first?.arrivalStation ?: ""
                    )
                }
                var distance by remember(settingsUiState.showDialogAddServicePhase) {
                    mutableStateOf(
                        settingsUiState.selectedServicePhase?.first?.distance?.toString() ?: ""
                    )
                }

                if (settingsUiState.showDialogAddServicePhase) {
                    ModalBottomSheet(
                        onDismissRequest = { hideDialogAddServicePhase() },
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.background,
                        tonalElevation = 8.dp
                    ) {
                        val focusManager = LocalFocusManager.current

                        LaunchedEffect(settingsUiState.selectedServicePhase) {
                            if (settingsUiState.selectedServicePhase != null) {
                                departureStation =
                                    settingsUiState.selectedServicePhase.first.departureStation
                                arrivalStation =
                                    settingsUiState.selectedServicePhase.first.arrivalStation
                                distance =
                                    settingsUiState.selectedServicePhase.first.distance.toString()
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(bottom = 16.dp),
                                text = "Новое плечо",
                                textAlign = TextAlign.Center,
                                style = styleTitle,
                                color = primaryColor
                            )
                            OutlinedTextFieldApp(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = departureStation,
                                onValueChange = { departure ->
                                    departureStation = departure
                                },
                                placeholder = {
                                    Text(
                                        text = "От станции",
                                        style = styleData,
                                        color = noValueColor
                                    )
                                },
                                textStyle = styleData,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text, imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = {
                                    scope.launch {
                                        focusManager.moveFocus(FocusDirection.Down)
                                    }
                                }),
                                singleLine = true,
                            )

                            OutlinedTextFieldApp(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = arrivalStation,
                                onValueChange = { arrival ->
                                    arrivalStation = arrival
                                },
                                placeholder = {
                                    Text(
                                        text = "До станции",
                                        style = styleData,
                                        color = noValueColor
                                    )
                                },
                                textStyle = styleData,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text, imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = {
                                    scope.launch {
                                        focusManager.moveFocus(FocusDirection.Down)
                                    }
                                }),
                                singleLine = true,
                            )

                            OutlinedTextFieldApp(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = distance,
                                onValueChange = { dis ->
                                    distance = dis
                                },
                                placeholder = {
                                    Text(
                                        text = "Расстояние",
                                        style = styleData,
                                        color = noValueColor
                                    )
                                },
                                suffix = {
                                    Text(text = "км", style = styleHint, color = noValueColor)
                                },
                                textStyle = styleData,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onNext = {
                                    scope.launch {
                                        focusManager.clearFocus()
                                    }
                                }),
                                singleLine = true,
                            )

                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 32.dp),
                                shape = Shapes.medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                enabled = departureStation.isNotBlank() && arrivalStation.isNotBlank() && distance.toIntOrZero() > 0,
                                onClick = {
                                    val dist = distance.toIntOrZero()
                                    if (dist > 0 && departureStation.isNotBlank() && arrivalStation.isNotBlank()) {
                                        val newPhase = ServicePhase(
                                            departureStation = departureStation.trim(),
                                            arrivalStation = arrivalStation.trim(),
                                            distance = dist
                                        )
                                        val index =
                                            settingsUiState.selectedServicePhase?.second ?: -1
                                        addServicePhase(
                                            newPhase,
                                            index
                                        )
                                    }
                                }
                            ) {
                                Text(
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    text = "Добавить"
                                )
                            }
                        }
                    }
                }

                if (showNightTimeStartDialog) {
                    val initHour = currentSettings.nightTime.startNightHour
                    val initMinute = currentSettings.nightTime.startNightMinute
                    val initMillis =
                        (initHour.times(3600000) + initMinute.times(60000)).toLong()
                    TimePickerApp(
                        initialTimeMillis = initMillis,
                        onTimeSelected = { millis ->
                            val hour = ConverterLongToTime.getHour(millis)
                            val minute =
                                ConverterLongToTime.getRemainingMinuteFromHour(millis)
                            changeStartNightTime(hour, minute)
                            showNightTimeStartDialog = false
                            showNightTimeEndDialog = true
                        },
                        onDismiss = { showNightTimeStartDialog = false },
                        title = "Начало ночи"
                    )
                }

                if (showNightTimeEndDialog) {
                    val initHour = currentSettings.nightTime.endNightHour
                    val initMinute = currentSettings.nightTime.endNightMinute
                    val initMillis =
                        (initHour.times(3600000) + initMinute.times(60000)).toLong()
                    TimePickerApp(
                        initialTimeMillis = initMillis,
                        onTimeSelected = { millis ->
                            val hour = ConverterLongToTime.getHour(millis)
                            val minute =
                                ConverterLongToTime.getRemainingMinuteFromHour(millis)
                            changeEndNightTime(hour, minute)
                        },
                        onDismiss = { showNightTimeEndDialog = false },
                        title = "Окончание ночи"
                    )
                }

                if (showRestDialog) {
                    TimePickerApp(
                        initialTimeMillis = currentSettings.minTimeRestPointOfTurnover,
                        onTimeSelected = { millis ->
                            restTimeChanged(millis)
                            showRestDialog = false
                        },
                        onDismiss = { showRestDialog = false },
                        title = "Минимальный отдых в ПО"
                    )
                }

                if (showHomeRestDialog) {
                    TimePickerApp(
                        initialTimeMillis = currentSettings.minTimeHomeRest,
                        onTimeSelected = { millis ->
                            homeRestTimeChanged(millis)
                            showHomeRestDialog = false
                        },
                        onDismiss = { showHomeRestDialog = false },
                        title = "Минимальный домашний отдых"
                    )
                }

//                if (showLocoTypeSelectedDialog) {
//                    ModalBottomSheet(
//                        onDismissRequest = { showLocoTypeSelectedDialog = false },
//                        sheetState = sheetState,
//                        containerColor = MaterialTheme.colorScheme.background,
//                        tonalElevation = 8.dp
//                    ) {
//                        Column(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(16.dp)
//                        ) {
//                            var currentType by remember {
//                                mutableStateOf(currentSettings.defaultLocoType)
//                            }
//
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxWidth(),
//                                horizontalArrangement = Arrangement.SpaceBetween,
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                AnimatedContent(targetState = currentType, label = "") {
//                                    val text =
//                                        if (it == LocoType.ELECTRIC) LocoType.ELECTRIC.text
//                                        else LocoType.DIESEL.text
//
//                                    Text(
//                                        text = text,
//                                        color = MaterialTheme.colorScheme.primary,
//                                        style = MaterialTheme.typography.bodyLarge
//                                    )
//                                }
//                                SwitchApp(
//                                    modifier = Modifier.wrapContentWidth(),
//                                    checked = currentType == LocoType.ELECTRIC,
//                                    onCheckedChange = {
//                                        currentType =
//                                            if (currentType == LocoType.ELECTRIC) {
//                                                LocoType.DIESEL
//                                            } else {
//                                                LocoType.ELECTRIC
//                                            }
//                                    },
//                                    positiveContent = {
//                                        Box(
//                                            modifier = Modifier.padding(horizontal = 16.dp),
//                                            contentAlignment = Alignment.Center
//                                        ) {
//                                            Icon(
//                                                painter = painterResource(id = R.drawable.electric_bolt_24px),
//
//                                                contentDescription = null,
//                                                tint = MaterialTheme.colorScheme.primary.copy(
//                                                    alpha = 0.8f
//                                                )
//                                            )
//                                        }
//                                    },
//                                    negativeContent = {
//                                        Box(
//                                            modifier = Modifier.padding(horizontal = 16.dp),
//                                            contentAlignment = Alignment.Center
//                                        ) {
//                                            Icon(
//                                                painter = painterResource(id = R.drawable.opacity_24px),
//                                                contentDescription = null,
//                                                tint = MaterialTheme.colorScheme.primary.copy(
//                                                    alpha = 0.8f
//                                                )
//                                            )
//                                        }
//                                    }
//                                )
//                            }
//
//                            Spacer(modifier = Modifier.height(24.dp))
//                            Button(
//                                modifier = Modifier
//                                    .fillMaxWidth(),
//                                colors = ButtonDefaults.buttonColors(
//                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
//                                ),
//                                shape = Shapes.medium,
//                                onClick = {
//                                    locoTypeChanged(currentType)
//                                    showLocoTypeSelectedDialog = false
//                                }
//                            ) {
//                                Text(
//                                    text = "Применить",
//                                    color = MaterialTheme.colorScheme.secondary,
//                                    style = MaterialTheme.typography.bodySmall
//                                )
//                            }
//                        }
//                    }
//                }

                if (showWorkTimeDialog) {
                    TimePickerApp(
                        initialTimeMillis = currentSettings.defaultWorkTime,
                        onTimeSelected = { millis ->
                            workTimeChanged(millis)
                            showWorkTimeDialog = false
                        },
                        onDismiss = { showWorkTimeDialog = false },
                        title = "Время работы по умолчанию"
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                )
                {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(start = 16.dp, bottom = 6.dp),
                                text = "Норма часов",
                                style = styleTitle
                            )
                            Column(
                                modifier = Modifier
                                    .shadow(elevation = 2.dp, shape = Shapes.medium)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = Shapes.medium
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val currentMonth =
                                        getMonthFullText(currentSettings.selectMonthOfYear.month)
                                    val personalNormaText =
                                        ConverterLongToTime.getTimeInStringFormat(
                                            currentSettings.selectMonthOfYear.getPersonalNormaHours()
                                                .toLong()
                                                .times(3_600_000)
                                        )

                                    Text(
                                        text = currentMonth,
                                        color = primaryColor,
                                        style = styleData
                                    )

                                    Text(
                                        text = personalNormaText,
                                        style = styleData,
                                        color = primaryColor,
                                    )
                                }
                                CustomDivider(orientation = Orientation.Horizontal)

                                Text(
                                    modifier = Modifier.clickable {
                                        showReleaseDaySelectScreen()
                                    },
                                    text = "Изменить норму",
                                    color = MaterialTheme.colorScheme.tertiary,
                                    style = MaterialTheme.typography.bodySmall
                                )

                            }
                        }
                    }

                    // часовой пояс
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(start = 16.dp, bottom = 6.dp),
                                text = "Домашний часовой пояс",
                                style = styleTitle,
                                color = primaryColor,
                                overflow = TextOverflow.Ellipsis
                            )
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = Shapes.medium
                                        )
                                        .fillMaxWidth()
                                ) {
                                    val currentTimeZone: TimeZoneRussia =
                                        timeZoneRussiaList.find {
                                            it.offsetOfMoscow == currentSettings.timeZone
                                        } ?: timeZoneRussiaList[1]

                                    var selectedTimeZone by remember {
                                        mutableStateOf(
                                            currentTimeZone
                                        )
                                    }
                                    var expanded by remember { mutableStateOf(false) }

                                    ExposedDropdownMenuBox(
                                        expanded = expanded,
                                        onExpandedChange = {
                                            expanded = !expanded
                                        }
                                    ) {
                                        OutlinedTextFieldApp(
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth(),
                                            value = selectedTimeZone.description,
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(
                                                    expanded = expanded
                                                )
                                            },
                                            textStyle = styleData.copy(color = MaterialTheme.colorScheme.primary)
                                        )

                                        ExposedDropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            timeZoneRussiaList.forEach { item ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = item.description,
                                                            color = primaryColor,
                                                            style = styleHint
                                                        )
                                                    },
                                                    onClick = {
                                                        selectedTimeZone = item
                                                        setTimeZone(item.offsetOfMoscow)
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        top = 8.dp
                                    ),
                                    text = "Установите местный часовой пояс. Будет учитываться при расчете ночных, праздничных часов и переходных поездках.",
                                    style = styleHint,
                                    maxLines = Int.MAX_VALUE,
                                    overflow = TextOverflow.Visible
                                )
                            }
                        }
                    }

                    // время
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .shadow(elevation = 2.dp, shape = Shapes.medium)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = Shapes.medium
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .noRippleEffect {
                                            showNightTimeStartDialog = true
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Ночь",
                                        style = styleData,
                                        color = primaryColor,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 2,
                                        modifier = Modifier
                                            .weight(1f)
                                    )
                                    val text =
                                        currentSettings.nightTime.toString()
                                    Text(
                                        text = text,
                                        style = styleData,
                                        color = primaryColor
                                    )
                                }
                                CustomDivider(orientation = Orientation.Horizontal)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .noRippleEffect {
                                            showRestDialog = true
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Отдых в ПО",
                                        style = styleData,
                                        color = primaryColor,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 2,
                                        modifier = Modifier
                                            .weight(1f)
                                    )
                                    val text =
                                        ConverterLongToTime.getTimeInStringFormat(
                                            currentSettings.minTimeRestPointOfTurnover
                                        )
                                    Text(
                                        text = text,
                                        style = styleData,
                                        color = primaryColor
                                    )
                                }
                                CustomDivider(orientation = Orientation.Horizontal)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .noRippleEffect { showHomeRestDialog = true },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Домашний отдых",
                                        style = styleData,
                                        color = primaryColor,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 2,
                                        modifier = Modifier
                                            .weight(1f)
                                    )

                                    Text(
                                        text = ConverterLongToTime.getTimeInStringFormat(
                                            currentSettings.minTimeHomeRest
                                        ),
                                        style = styleData,
                                        color = primaryColor,
                                    )
                                }
                            }

                            Text(
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    top = 8.dp
                                ),
                                text = "Установите время минимального отдыха. Это значение будет использовано при расчете отдыха после поездки.",
                                style = styleHint,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // плечи
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Плечи",
                                overflow = TextOverflow.Ellipsis,
                                style = styleTitle,
                                color = primaryColor
                            )
                            TextButton(
                                onClick = {
                                    departureStation = ""
                                    arrivalStation = ""
                                    distance = ""
                                    showDialogAddServicePhase(
                                        ServicePhase(
                                            departureStation = departureStation,
                                            arrivalStation = arrivalStation,
                                            distance = distance.toIntOrZero()
                                        )
                                    )
                                }
                            ) {
                                Text(
                                    text = "Добавить",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }

                    itemsIndexed(
                        items = servicePhases?.toList() ?: emptyList(),
                        key = { _, item -> item.hashCode() }
                    ) { index, item ->
                        val dismissState = rememberSwipeToDismissBoxState()
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            deleteServicePhase(index)
                        }
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val color by animateColorAsState(
                                    when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.Settled -> Color.Transparent
                                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                                        else -> Color.Transparent
                                    }, label = ""
                                )
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(
                                            color = color,
                                            shape = Shapes.medium
                                        ),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        modifier = Modifier.padding(end = 16.dp),
                                        painter = painterResource(R.drawable.delete_24px),
                                        tint = MaterialTheme.colorScheme.surface,
                                        contentDescription = null
                                    )
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .shadow(elevation = 2.dp, shape = Shapes.medium)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = Shapes.medium
                                    )
                                    .noRippleEffect {
                                        updateServicePhase(
                                            ServicePhase(
                                                departureStation = item.departureStation,
                                                arrivalStation = item.arrivalStation,
                                                distance = item.distance
                                            ),
                                            index
                                        )
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = "${item.departureStation} - ${item.arrivalStation}",
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 2,
                                    style = styleData
                                )
                                Text(
                                    text = "${item.distance} км",
                                    overflow = TextOverflow.Visible,
                                    style = styleData
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )
                    }

                    // данные по умолчанию
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(start = 16.dp, bottom = 6.dp),
                                text = "Данные по умолчанию",
                                style = styleTitle,
                                color = primaryColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Column(
                                modifier = Modifier
                                    .shadow(elevation = 2.dp, shape = Shapes.medium)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = Shapes.medium
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .noRippleEffect {
                                            viewModel.changeDefaultLocoType()
                                        },
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        modifier = Modifier.weight(1f),
                                        text = "Вид тяги",
                                        style = styleData,
                                        color = primaryColor
                                    )

                                    Text(
                                        text = currentSettings.defaultLocoType.text,
                                        style = styleData,
                                        color = primaryColor
                                    )
                                }
                                CustomDivider(orientation = Orientation.Horizontal)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .noRippleEffect {
                                            viewModel.changeTimeFormat()
                                        },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ){
                                    Text(
                                        modifier = Modifier
                                            .padding(end = 12.dp)
                                            .weight(1f),
                                        text = "Формат времени",
                                        style = styleData,
                                        color = primaryColor,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 2
                                    )
                                    val text = if (setting.isDecimalTime) "12,5" else "12:30"
                                    Text(
                                        modifier = Modifier
                                            .padding(end = 12.dp),
                                        text = text,
                                        style = styleData,
                                        color = primaryColor,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 2
                                    )

                                }

                                CustomDivider(orientation = Orientation.Horizontal)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .padding(end = 12.dp)
                                            .weight(1f),
                                        text = "Использовать cтандартное время работы",
                                        style = styleData,
                                        color = primaryColor,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 2
                                    )
                                    Switch(
                                        checked = currentSettings.usingDefaultWorkTime,
                                        onCheckedChange = {
                                            changeUsingDefaultWorkTime(it)
                                        })

                                }

                                AnimatedVisibility(visible = currentSettings.usingDefaultWorkTime) {
                                    Column(modifier = Modifier.fillMaxWidth()) {

                                        CustomDivider(orientation = Orientation.Horizontal)

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .noRippleEffect {
                                                    showWorkTimeDialog = true
                                                },
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        )
                                        {
                                            Text(
                                                modifier = Modifier.weight(1f),
                                                text = "Время работы",
                                                style = styleData,
                                                color = primaryColor,
                                                overflow = TextOverflow.Ellipsis,
                                                maxLines = 2
                                            )
                                            val text =
                                                ConverterLongToTime.getTimeInStringFormat(
                                                    currentSettings.defaultWorkTime
                                                )
                                            Text(
                                                text = text,
                                                style = styleData,
                                                color = primaryColor,
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    top = 8.dp
                                ),
                                text = "Эти значения будут установлены по умолчанию при создании нового маршрута.",
                                style = styleHint,
                                color = primaryColor
                            )
                        }
                    }

                    // будущие маршруты
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .shadow(elevation = 2.dp, shape = Shapes.medium)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = Shapes.medium
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .weight(1f),
                                    text = "Учитывать будущие маршруты",
                                    style = styleData,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 2
                                )
                                Switch(
                                    checked = currentSettings.isConsiderFutureRoute,
                                    onCheckedChange = {
                                        changeConsiderFutureRoute(it)
                                    })
                            }

                            Text(
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    top = 8.dp
                                ),
                                text = "Маршруты, время явки которых не наступило, будут учитываться при подсчете отработаного времени.",
                                style = styleHint,
                                color = primaryColor
                            )
                        }
                    }

                    // показывать перерыв
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .shadow(elevation = 2.dp, shape = Shapes.medium)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = Shapes.medium
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .weight(1f),
                                    text = "Показывать перерыв",
                                    style = styleData,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 2
                                )
                                Switch(
                                    checked = currentSettings.isShowBreak,
                                    onCheckedChange = {
                                        changeShowBreak(it)
                                    })
                            }

                            Text(
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    top = 8.dp
                                ),
                                text = "Показывать поля перерыва в форме маршрута.",
                                style = styleHint,
                                color = primaryColor
                            )
                        }
                    }

                    // Локомотив
                    item {
                        Text(
                            modifier = Modifier.padding(top = 16.dp, start = 16.dp, bottom = 6.dp),
                            text = "Локомотив",
                            style = styleTitle
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(elevation = 2.dp, shape = Shapes.medium)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = Shapes.medium
                                )
                                .padding(vertical = 8.dp)
                        )
                        {
                            // Отопление
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Отопление",
                                    style = styleData,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                                )
                                Switch(
                                    checked = currentSettings.isShowLocoHeating,
                                    onCheckedChange = { changeShowLocoHeating(it) }
                                )
                            }

                            // Собственные нужды
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Собственные нужды",
                                    style = styleData,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                                )
                                Switch(
                                    checked = currentSettings.isShowLocoAuxiliary,
                                    onCheckedChange = { changeShowLocoAuxiliary(it) }
                                )
                            }

                            // Статистика
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Статистика",
                                    style = styleData,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                                )
                                Switch(
                                    checked = currentSettings.isShowLocoStatistics,
                                    onCheckedChange = { changeShowLocoStatistics(it) }
                                )
                            }
                        }
                        Text(
                            modifier = Modifier.padding(
                                start = 16.dp,
                                top = 8.dp
                            ),
                            text = "Показывать эти поля в разделе Локомотив",
                            style = styleHint,
                            color = primaryColor
                        )
                    }

                    item {
                        Text(
                            modifier = Modifier.padding(top = 16.dp, start = 16.dp, bottom = 6.dp),
                            text = "О приложении",
                            style = styleTitle
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(elevation = 2.dp, shape = Shapes.medium)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = Shapes.medium
                                )
                                .padding(16.dp)
                        ) {
                            val context = LocalContext.current

                            val versionName = remember {
                                try {
                                    val packageInfo =
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            context.packageManager.getPackageInfo(
                                                context.packageName,
                                                PackageManager.PackageInfoFlags.of(0L)
                                            )
                                        } else {
                                            @Suppress("DEPRECATION")
                                            context.packageManager.getPackageInfo(
                                                context.packageName,
                                                0
                                            )
                                        }
                                    packageInfo.versionName ?: "Unknown"
                                } catch (e: Exception) {
                                    "Unknown"
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val context = LocalContext.current
                                val email = "locodriver.app@yandex.ru"
                                Text(
                                    text = "Версия приложения",
                                    style = styleHint,
                                    color = primaryColor.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = versionName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = primaryColor
                                )

                                CustomDivider(orientation = Orientation.Horizontal)

                                Text(
                                    text = "Поддержка",
                                    style = styleHint,
                                    color = primaryColor.copy(alpha = 0.7f)
                                )

                                Text(
                                    modifier = Modifier.noRippleEffect {
                                        val selector = Intent(Intent.ACTION_SENDTO).apply {
                                            data =
                                                "mailto:".toUri() // ← обязательно, чтобы открывались только почтовые приложения
                                        }

                                        val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = "mailto:$email".toUri()
                                        }

                                        try {
                                            context.startActivity(mailIntent)
                                        } catch (e: Exception) {
                                            context.startActivity(selector)
                                        }
                                    },
                                    text = "locodriver.app@yandex.ru",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = primaryColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Visible
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

