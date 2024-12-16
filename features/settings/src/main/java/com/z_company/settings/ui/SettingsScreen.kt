package com.z_company.settings.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.component.GenericError
import com.z_company.core.ui.component.TimeInputDialog
import com.z_company.core.ui.component.TimePickerDialog
import com.z_company.settings.component.SelectedDialog
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.DateAndTimeConverter.getMonthFullText
import com.z_company.domain.entities.User
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.route.LocoType
import com.z_company.settings.component.ConfirmEmailDialog
import com.z_company.settings.viewmodel.SettingsUiState
import kotlinx.coroutines.launch
import com.z_company.core.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsUiState: SettingsUiState,
    currentSettings: UserSettings?,
    currentUserState: ResultState<User?>,
    resetSaveState: () -> Unit,
    onSaveClick: () -> Unit,
    onBack: () -> Unit,
    onSettingSaved: () -> Unit,
    workTimeChanged: (Long) -> Unit,
    locoTypeChanged: (LocoType) -> Unit,
    restTimeChanged: (Long) -> Unit,
    homeRestTimeChanged: (Long) -> Unit,
    onLogOut: () -> Unit,
    logOut: () -> Unit,
    onSync: () -> Unit,
    showReleaseDaySelectScreen: () -> Unit,
    onResentVerificationEmail: () -> Unit,
    emailForConfirm: String,
    onChangeEmail: (String) -> Unit,
    enableButtonConfirmVerification: Boolean,
    resetRepositoryState: () -> Unit,
    changeStartNightTime: (Int, Int) -> Unit,
    changeEndNightTime: (Int, Int) -> Unit,
    changeUsingDefaultWorkTime: (Boolean) -> Unit,
    changeConsiderFutureRoute: (Boolean) -> Unit,
    purchasesState: ResultState<String>,
    onBillingClick: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSettingHomeScreenClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val titleStyle = AppTypography.getType().headlineMedium.copy(fontWeight = FontWeight.Light)

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
            }
        },
        topBar = {
            TopAppBar(navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = CoreR.drawable.ic_arrow_back),
                        contentDescription = stringResource(id = CoreR.string.cd_back)
                    )
                }
            }, title = {
                Text(text = stringResource(id = CoreR.string.settings), style = titleStyle)
            }, actions = {
                TextButton(onClick = onSaveClick) {
                    Text(
                        text = "Сохранить",
                        style = AppTypography.getType().titleLarge
                            .copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.tertiary
                            ),
                    )
                }
            },
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = Color.Transparent,
                )
            )
        }) {
        LaunchedEffect(settingsUiState.updateRepositoryState) {
            if (settingsUiState.updateRepositoryState is ResultState.Error) {
                scope.launch {
                    snackbarHostState.showSnackbar("Ошибка синхронизации. \n${settingsUiState.updateRepositoryState.entity.message}.")
                }
                resetRepositoryState()
            }
        }

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
                        }
                        if (settingsUiState.logOutState is ResultState.Success) {
                            LaunchedEffect(settingsUiState.logOutState) {
                                logOut()
                            }
                        } else {
                            SettingScreenContent(
                                currentSettings = setting,
                                onLogOut = onLogOut,
                                onSync = onSync,
                                updateRepoState = settingsUiState.updateRepositoryState,
                                currentUserState = currentUserState,
                                updateAtState = settingsUiState.updateAt,
                                workTimeChanged = workTimeChanged,
                                locoTypeChanged = locoTypeChanged,
                                restTimeChanged = restTimeChanged,
                                homeRestTimeChanged = homeRestTimeChanged,
                                showReleaseDaySelectScreen = showReleaseDaySelectScreen,
                                onResentVerificationEmail = onResentVerificationEmail,
                                emailForConfirm = emailForConfirm,
                                onChangeEmail = onChangeEmail,
                                enableButtonConfirmVerification = enableButtonConfirmVerification,
                                changeStartNightTime = changeStartNightTime,
                                changeEndNightTime = changeEndNightTime,
                                changeUsingDefaultWorkTime = changeUsingDefaultWorkTime,
                                changeConsiderFutureRoute = changeConsiderFutureRoute,
                                purchasesState = purchasesState,
                                onBillingClick = onBillingClick,
                                isRefreshing = isRefreshing,
                                onRefresh = onRefresh,
                                onSettingHomeScreenClick = onSettingHomeScreenClick
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
    workTimeChanged: (Long) -> Unit,
    locoTypeChanged: (LocoType) -> Unit,
    restTimeChanged: (Long) -> Unit,
    homeRestTimeChanged: (Long) -> Unit,
    onLogOut: () -> Unit,
    onSync: () -> Unit,
    updateRepoState: ResultState<Long>?,
    currentUserState: ResultState<User?>,
    updateAtState: Long?,
    showReleaseDaySelectScreen: () -> Unit,
    onResentVerificationEmail: () -> Unit,
    emailForConfirm: String,
    onChangeEmail: (String) -> Unit,
    enableButtonConfirmVerification: Boolean,
    changeStartNightTime: (Int, Int) -> Unit,
    changeEndNightTime: (Int, Int) -> Unit,
    changeUsingDefaultWorkTime: (Boolean) -> Unit,
    changeConsiderFutureRoute: (Boolean) -> Unit,
    purchasesState: ResultState<String>,
    onBillingClick: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSettingHomeScreenClick: () -> Unit
) {
    val styleTitle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
    val styleData = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
    val styleHint = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )

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

    if (showNightTimeStartDialog) {
        val startNightPickerDialog = rememberTimePickerState(
            initialHour = currentSettings.nightTime.startNightHour,
            initialMinute = currentSettings.nightTime.startNightMinute,
            is24Hour = true
        )
        TimePickerDialog(
            timePickerState = startNightPickerDialog,
            onDismissRequest = { showNightTimeStartDialog = false },
            onConfirmRequest = {
                changeStartNightTime(startNightPickerDialog.hour, startNightPickerDialog.minute)
                showNightTimeStartDialog = false
                showNightTimeEndDialog = true
            },
            header = "Начало ночи"
        )
    }

    if (showNightTimeEndDialog) {
        val endNightPickerDialog = rememberTimePickerState(
            initialHour = currentSettings.nightTime.endNightHour,
            initialMinute = currentSettings.nightTime.endNightMinute,
            is24Hour = true
        )
        TimePickerDialog(
            timePickerState = endNightPickerDialog,
            onDismissRequest = { showNightTimeEndDialog = false },
            onConfirmRequest = {
                changeEndNightTime(endNightPickerDialog.hour, endNightPickerDialog.minute)
                showNightTimeEndDialog = false
            },
            header = "Окончание ночи"
        )
    }

    if (showConfirmEmailDialog) {
        ConfirmEmailDialog(
            onDismissRequest = { showConfirmEmailDialog = false },
            onConfirmButton = {
                onResentVerificationEmail()
                showConfirmEmailDialog = false
            },
            emailForConfirm = emailForConfirm,
            onChangeEmail = onChangeEmail,
            enableButtonConfirmVerification = enableButtonConfirmVerification
        )
    }

    if (showRestDialog) {
        TimeInputDialog(
            initValue = currentSettings.minTimeRestPointOfTurnover,
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

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = isRefreshing),
        onRefresh = onRefresh,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
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
                    Column(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = Shapes.medium
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val currentMonth =
                                currentSettings.selectMonthOfYear.month.getMonthFullText()
                            Text(text = currentMonth, style = styleData)

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = ConverterLongToTime.getTimeInStringFormat(
                                        currentSettings.selectMonthOfYear.getPersonalNormaHours()
                                            .toLong()
                                            .times(3_600_000)
                                    ),
                                    style = styleData
                                )
                            }
                        }
                        HorizontalDivider()
                        ClickableText(
                            text = AnnotatedString("Изменить норму"),
                            style = styleHint.copy(color = MaterialTheme.colorScheme.tertiary)
                        ) {
                            showReleaseDaySelectScreen()
                        }
                    }
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
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Ночь", style = styleData)
                                val text = currentSettings.nightTime.toString()
                                Text(
                                    modifier = Modifier
                                        .clickable {
                                            showNightTimeStartDialog = true
                                        },
                                    text = text,
                                    style = styleData
                                )
                            }
                            HorizontalDivider()


                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Отдых в ПО", style = styleData)
                                val text =
                                    ConverterLongToTime.getTimeInStringFormat(currentSettings.minTimeRestPointOfTurnover)
                                Text(
                                    modifier = Modifier
                                        .clickable { showRestDialog = true },
                                    text = text,
                                    style = styleData
                                )
                            }
                            HorizontalDivider()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Домашний отдых", style = styleData)
                                val text =
                                    ConverterLongToTime.getTimeInStringFormat(currentSettings.minTimeHomeRest)
                                Text(
                                    modifier = Modifier
                                        .clickable { showHomeRestDialog = true },
                                    text = text,
                                    style = styleData
                                )
                            }
                        }
                    }
                    Text(
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                        text = "Установите время минимального отдыха. Это значение будет использовано при расчете отдыха после поездки.",
                        style = styleHint
                    )
                }
            }
//            item {
//                Box(
//                    modifier = Modifier
//                        .padding(top = 8.dp)
//                        .background(
//                            color = MaterialTheme.colorScheme.surface,
//                            shape = Shapes.medium
//                        )
//                        .clickable {
//                            onSettingHomeScreenClick()
//                        }
//                        .padding(16.dp)
//                ) {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                    ) {
//                        Text(text = "Настоить главный экран", style = styleData)
//                        Icon(
//                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
//                            contentDescription = null
//                        )
//                    }
//                }
//            }
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
                                Text(text = "Локомотив", style = styleData)
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
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    modifier = Modifier
                                        .padding(end = 16.dp)
                                        .weight(0.8f),
                                    text = "Использовать cтандартное время работы",
                                    style = styleData
                                )
                                Switch(
                                    checked = currentSettings.usingDefaultWorkTime,
                                    onCheckedChange = {
                                        changeUsingDefaultWorkTime(it)
                                    })

                            }
                            AnimatedVisibility(visible = currentSettings.usingDefaultWorkTime) {
                                HorizontalDivider()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Время работы",
                                        style = styleData
                                    )
                                    val text =
                                        ConverterLongToTime.getTimeInStringFormat(currentSettings.defaultWorkTime)
                                    Text(
                                        modifier = Modifier
                                            .padding(end = 12.dp)
                                            .clickable { showWorkTimeDialog = true },
                                        text = text,
                                        style = styleData
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                        text = "Эти значения будут установлены по умолчанию при создании нового маршрута.",
                        style = styleHint
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .weight(0.8f),
                                text = "Учитывать будущие маршруты",
                                style = styleData
                            )
                            Switch(
                                checked = currentSettings.isConsiderFutureRoute,
                                onCheckedChange = {
                                    changeConsiderFutureRoute(it)
                                })
                        }
                    }
                    Text(
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                        text = "Маршруты, время явки которых не наступило, будут учитываться при подсчете отработаного времени.",
                        style = styleHint
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

                            Text(text = "E-mail", style = styleData)
                            AsyncData(
                                resultState = currentUserState,
                                loadingContent = {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                },
                                errorContent = {
                                    Text(
                                        text = "Ошибка синхронизации",
                                        style = styleData
                                    )
                                }) { user ->
                                user?.let {
                                    Text(
                                        text = user.email,
                                        style = styleData
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Статус", style = styleData)
                            AsyncData(
                                resultState = currentUserState,
                                loadingContent = {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }, errorContent = {
                                    Text(
                                        text = "Ошибка синхронизации",
                                        style = styleData
                                    )
                                }) { user ->
                                user?.let {
                                    val dataText = if (user.isVerification) {
                                        "Подтвержден"
                                    } else {
                                        "Не подтвержден"
                                    }

                                    Text(
                                        modifier = Modifier.clickable(enabled = !user.isVerification) {
                                            showConfirmEmailDialog = true
                                        },
                                        text = dataText,
                                        style = styleData,
                                        color = if (!user.isVerification) MaterialTheme.colorScheme.tertiary else Color.Unspecified
                                    )

                                }
                            }
                        }
                        AsyncData(
                            resultState = currentUserState,
                            loadingContent = {},
                            errorContent = {}
                        ) { user ->
                            user?.let {
                                if (user.isVerification) {
                                    HorizontalDivider()

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Синхронизация", style = styleData)
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
//                                            AsyncData(resultState = updateAtState) { updateAt ->
                                            updateAtState?.let { timeInMillis ->
                                                val textSyncDate =
                                                    DateAndTimeConverter.getDateAndTime(
                                                        timeInMillis
                                                    )

                                                Text(
                                                    text = textSyncDate,
                                                    style = styleData
                                                )
                                            }
//                                            }
                                            AsyncData(
                                                resultState = updateRepoState,
                                                loadingContent = {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(24.dp),
                                                        strokeWidth = 2.dp
                                                    )
                                                },
                                                errorContent = {}
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

                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBillingClick() },
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Подписка", style = styleData)
                            AsyncData(
                                resultState = purchasesState,
                                loadingContent = {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                },
                                errorContent = {
                                    Text(
                                        text = "Ошибка синхронизации",
                                        style = styleData
                                    )
                                }
                            ) { purchaseInfo ->
                                if (purchaseInfo.isNullOrEmpty()) {
                                    Text(
                                        text = "Отсутствует",
                                        style = styleData
                                    )
                                } else {
                                    Text(
                                        text = "до $purchaseInfo",
                                        style = styleData
                                    )
                                }
                            }
                        }
                    }
                }
                Text(
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    text = "Подтверждение e-mail нужно для синхронизации с облачным хранилищем.",
                    style = styleHint
                )
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
                    Text(
                        text = "Выйти",
                        color = MaterialTheme.colorScheme.error,
                        style = styleTitle
                    )
                }
            }
        }
    }
}

