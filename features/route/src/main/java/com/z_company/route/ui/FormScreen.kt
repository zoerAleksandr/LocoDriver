package com.z_company.route.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import com.z_company.core.ui.component.TimePickerDialog
import com.z_company.route.component.CustomDatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.domain.entities.route.Route
import com.z_company.route.viewmodel.RouteFormUiState
import com.z_company.domain.util.minus
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Train
import com.z_company.route.R
import com.z_company.route.component.BottomShadow
import com.z_company.route.component.rememberDatePickerStateInLocale
import java.util.Calendar
import com.z_company.route.extention.isScrollInInitialState
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.route.component.ConfirmExitDialog
import com.z_company.route.viewmodel.DialogRestUiState
import kotlinx.coroutines.launch

const val LINK_TO_SETTING = "LINK_TO_SETTING"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    formUiState: RouteFormUiState,
    dialogRestUiState: DialogRestUiState,
    currentRoute: Route?,
    isCopy: Boolean,
    exitScreen: () -> Unit,
    onSaveClick: () -> Unit,
    onSettingClick: () -> Unit,
    onBack: () -> Unit,
    resetSaveState: () -> Unit,
    onNumberChanged: (String) -> Unit,
    checkedOnePersonOperation: (Boolean) -> Unit,
    onNotesChanged: (String) -> Unit,
    onTimeStartWorkChanged: (Long?) -> Unit,
    onTimeEndWorkChanged: (Long?) -> Unit,
    onRestChanged: (Boolean) -> Unit,
    onChangedLocoClick: (loco: Locomotive) -> Unit,
    onNewLocoClick: (basicId: String) -> Unit,
    onDeleteLoco: (loco: Locomotive) -> Unit,
    onChangeTrainClick: (train: Train) -> Unit,
    onNewTrainClick: (basicId: String) -> Unit,
    onDeleteTrain: (train: Train) -> Unit,
    onChangePassengerClick: (passenger: Passenger) -> Unit,
    onNewPassengerClick: (basicId: String) -> Unit,
    onDeletePassenger: (passenger: Passenger) -> Unit,
    nightTime: Long?,
    changeShowConfirmExitDialog: (Boolean) -> Unit,
    exitWithoutSave: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )
    val titleStyle = AppTypography.getType().headlineMedium.copy(fontWeight = FontWeight.Light)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Маршрут",
                        style = titleStyle
                    )
                }, navigationIcon = {
                    IconButton(onClick = {
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }, actions = {
                    AsyncData(
                        resultState = formUiState.saveRouteState,
                        loadingContent = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        },
                        errorContent = {}
                    ) {
                        TextButton(
                            modifier = Modifier
                                .padding(end = 16.dp),
                            enabled = formUiState.changesHaveState,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.tertiary
                            ),
                            onClick = { onSaveClick() }
                        ) {
                            Text(text = "Сохранить", style = hintStyle)
                        }
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
            }
        }
    ) {
        if (formUiState.saveRouteState is ResultState.Error) {
            LaunchedEffect(Unit) {
                scope.launch {
                    snackbarHostState.showSnackbar("Ошибка: ${formUiState.saveRouteState.entity.message}")
                }
                resetSaveState()
            }
        }
        if (formUiState.exitFromScreen) {
            LaunchedEffect(Unit) {
                exitScreen()
            }
        }

        Box(Modifier.padding(it)) {
            AsyncData(resultState = formUiState.routeDetailState) {
                currentRoute?.let { route ->
                    if (formUiState.saveRouteState is ResultState.Success) {
                        LaunchedEffect(formUiState.saveRouteState) {
                            exitScreen()
                        }
                    } else {
                        RouteFormScreenContent(
                            route = route,
                            isCopy = isCopy,
                            onNumberChanged = onNumberChanged,
                            checkedOnePersonOperation = checkedOnePersonOperation,
                            onNotesChanged = onNotesChanged,
                            errorMessage = formUiState.errorMessage,
                            onTimeStartWorkChanged = onTimeStartWorkChanged,
                            onTimeEndWorkChanged = onTimeEndWorkChanged,
                            onRestChanged = onRestChanged,
                            onSettingClick = onSettingClick,
                            locoListState = route.locomotives,
                            onChangeLocoClick = onChangedLocoClick,
                            onNewLocoClick = onNewLocoClick,
                            onDeleteLoco = onDeleteLoco,
                            trainListState = route.trains,
                            onChangeTrainClick = onChangeTrainClick,
                            onNewTrainClick = onNewTrainClick,
                            onDeleteTrain = onDeleteTrain,
                            passengerListState = route.passengers,
                            onChangePassengerClick = onChangePassengerClick,
                            onNewPassengerClick = onNewPassengerClick,
                            onDeletePassenger = onDeletePassenger,
                            nightTime = nightTime,
                            showConfirmExitDialog = formUiState.confirmExitDialogShow,
                            changeShowConfirmExitDialog = changeShowConfirmExitDialog,
                            onSaveClick = onSaveClick,
                            exitWithoutSave = exitWithoutSave,
                            dialogRestUiState = dialogRestUiState
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteFormScreenContent(
    route: Route,
    isCopy: Boolean,
    onNumberChanged: (String) -> Unit,
    checkedOnePersonOperation: (Boolean) -> Unit,
    onNotesChanged: (String) -> Unit,
    errorMessage: String?,
    onTimeStartWorkChanged: (Long?) -> Unit,
    onTimeEndWorkChanged: (Long?) -> Unit,
    onRestChanged: (Boolean) -> Unit,
    onSettingClick: () -> Unit,
    locoListState: List<Locomotive>?,
    onChangeLocoClick: (loco: Locomotive) -> Unit,
    onNewLocoClick: (basicId: String) -> Unit,
    onDeleteLoco: (loco: Locomotive) -> Unit,
    trainListState: List<Train>?,
    onChangeTrainClick: (train: Train) -> Unit,
    onNewTrainClick: (basicId: String) -> Unit,
    onDeleteTrain: (train: Train) -> Unit,
    passengerListState: List<Passenger>?,
    onChangePassengerClick: (passenger: Passenger) -> Unit,
    onNewPassengerClick: (basicId: String) -> Unit,
    onDeletePassenger: (passenger: Passenger) -> Unit,
    nightTime: Long?,
    showConfirmExitDialog: Boolean,
    changeShowConfirmExitDialog: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    exitWithoutSave: () -> Unit,
    dialogRestUiState: DialogRestUiState,
) {
    val dataTextStyle = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )
    val scrollState = rememberLazyListState()

    var showStartDatePickerCopyRoute by remember {
        mutableStateOf(false)
    }

    var showStartTimePicker by remember {
        mutableStateOf(false)
    }

    var showStartDatePicker by remember {
        mutableStateOf(false)
    }

    var showEndTimePicker by remember {
        mutableStateOf(false)
    }

    var showEndDatePicker by remember {
        mutableStateOf(false)
    }

    var moreInfoRestVisible by remember {
        mutableStateOf(false)
    }

    if (showConfirmExitDialog) {
        ConfirmExitDialog(
            showExitConfirmDialog = changeShowConfirmExitDialog,
            onSaveClick = onSaveClick,
            exitWithoutSave = exitWithoutSave
        )
    }

    val startOfWorkTime by remember {
        mutableStateOf(
            Calendar.getInstance().also { calendar ->
                route.basicData.timeStartWork?.let {
                    calendar.timeInMillis = it
                }
            })
    }

    val startCalendar by remember {
        mutableStateOf(startOfWorkTime)
    }

    val startTimePickerState = rememberTimePickerState(
        initialHour = startCalendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = startCalendar.get(Calendar.MINUTE),
        is24Hour = true
    )

    val startDatePickerState =
        rememberDatePickerStateInLocale(initialSelectedDateMillis = startCalendar.timeInMillis)

    if (showStartTimePicker) {
        TimePickerDialog(timePickerState = startTimePickerState,
            onDismissRequest = { showStartTimePicker = false },
            onConfirmRequest = {
                showStartTimePicker = false
                startCalendar.set(Calendar.HOUR_OF_DAY, startTimePickerState.hour)
                startCalendar.set(Calendar.MINUTE, startTimePickerState.minute)
                startCalendar.set(Calendar.SECOND, 0)
                startCalendar.set(Calendar.MILLISECOND, 0)
                onTimeStartWorkChanged(startCalendar.timeInMillis)
            })
    }

    if (showStartDatePicker) {
        CustomDatePickerDialog(datePickerState = startDatePickerState, onDismissRequest = {
            showStartDatePicker = false
        }, onConfirmRequest = {
            showStartDatePicker = false
            showStartTimePicker = true
            startCalendar.timeInMillis = startDatePickerState.selectedDateMillis!!
        })
    }

    val endOfWorkTime by remember {
        mutableStateOf(
            Calendar.getInstance().also { calendar ->
                route.basicData.timeEndWork?.let {
                    calendar.timeInMillis = it
                }
            })
    }

    val endCalendar by remember {
        mutableStateOf(endOfWorkTime)
    }

    val endTimePickerState = rememberTimePickerState(
        initialHour = endCalendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = endCalendar.get(Calendar.MINUTE),
        is24Hour = true
    )

    val endDatePickerState =
        rememberDatePickerStateInLocale(initialSelectedDateMillis = endCalendar.timeInMillis)

    if (showEndTimePicker) {
        TimePickerDialog(timePickerState = endTimePickerState,
            onDismissRequest = { showEndTimePicker = false },
            onConfirmRequest = {
                showEndTimePicker = false
                endCalendar.set(Calendar.HOUR_OF_DAY, endTimePickerState.hour)
                endCalendar.set(Calendar.MINUTE, endTimePickerState.minute)
                endCalendar.set(Calendar.SECOND, 0)
                endCalendar.set(Calendar.MILLISECOND, 0)
                onTimeEndWorkChanged(endCalendar.timeInMillis)
            })
    }

    if (showEndDatePicker) {
        CustomDatePickerDialog(datePickerState = endDatePickerState, onDismissRequest = {
            showEndDatePicker = false
        }, onConfirmRequest = {
            showEndDatePicker = false
            showEndTimePicker = true
            endCalendar.timeInMillis = endDatePickerState.selectedDateMillis!!
        })
    }

    LaunchedEffect(isCopy) {
        if (isCopy) {
            showStartDatePickerCopyRoute = true
        }
    }

    val startDateCopyRoutePickerState = rememberDatePickerStateInLocale(
        route.basicData.timeStartWork ?: Calendar.getInstance().timeInMillis
    )

    if (showStartDatePickerCopyRoute) {
        CustomDatePickerDialog(datePickerState = startDateCopyRoutePickerState, onDismissRequest = {
            showStartDatePickerCopyRoute = false
        }, onConfirmRequest = {
            showStartDatePickerCopyRoute = false
            val oldValueStartCalendar = Calendar.getInstance().also {
                it.timeInMillis = startCalendar.timeInMillis
            }
            startCalendar.timeInMillis = startDateCopyRoutePickerState.selectedDateMillis!!
            startCalendar.set(Calendar.HOUR_OF_DAY, oldValueStartCalendar.get(Calendar.HOUR_OF_DAY))
            startCalendar.set(Calendar.MINUTE, oldValueStartCalendar.get(Calendar.MINUTE))
            startCalendar.set(Calendar.SECOND, 0)
            startCalendar.set(Calendar.MILLISECOND, 0)

            onTimeStartWorkChanged(startCalendar.timeInMillis)
            val workTimeInMillis = route.getWorkTime()
            workTimeInMillis?.let { workTime ->
                endCalendar.timeInMillis =
                    startCalendar.timeInMillis + workTime
                onTimeEndWorkChanged(endCalendar.timeInMillis)
            }
        })
    }

    AnimatedVisibility(
        modifier = Modifier.zIndex(1f),
        visible = !scrollState.isScrollInInitialState(),
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        BottomShadow()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        state = scrollState,
    ) {
        val startTimeInLong = route.basicData.timeStartWork
        val endTimeInLong = route.basicData.timeEndWork
        val workTimeInLong = endTimeInLong - startTimeInLong
        val workTimeInFormatted = ConverterLongToTime.getTimeInStringFormat(workTimeInLong)

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                errorMessage?.let { message ->
                    Icon(
                        modifier = Modifier.size(48.dp),
                        imageVector = Icons.Default.Warning,
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = "Ошибка"
                    )
                    Text(
                        text = message,
                        style = dataTextStyle
                    )
                }
                if (workTimeInLong != null && errorMessage == null) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                        text = workTimeInFormatted,
                        style = AppTypography.getType().displaySmall
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            modifier = Modifier.padding(end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.padding(end = 4.dp),
                                painter = painterResource(id = R.drawable.dark_mode_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = ConverterLongToTime.getTimeInStringFormat(nightTime),
                                style = hintStyle,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                modifier = Modifier.padding(end = 4.dp),
                                painter = painterResource(id = R.drawable.passenger_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = ConverterLongToTime.getTimeInStringFormat(
                                    route.getPassengerTime() ?: 0L
                                ),
                                style = hintStyle,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = Shapes.medium
                        )
                        .clickable {
                            showStartDatePicker = true
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Явка",
                        style = dataTextStyle
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val dateStartText = startTimeInLong?.let {
                            DateAndTimeConverter.getDateFromDateLong(startTimeInLong)
                        } ?: "укажите время"
                        val timeStartText = startTimeInLong?.let {
                            DateAndTimeConverter.getTimeFromDateLong(startTimeInLong)
                        } ?: ""

                        Text(
                            text = dateStartText,
                            style = dataTextStyle
                        )

                        Text(
                            text = " $timeStartText",
                            style = dataTextStyle,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = Shapes.medium
                        )
                        .clickable {
                            showEndDatePicker = true
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Сдача",
                        style = dataTextStyle
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val dateEndText = endTimeInLong?.let {
                            DateAndTimeConverter.getDateFromDateLong(endTimeInLong)
                        } ?: "укажите время"
                        val timeEndText = endTimeInLong?.let {
                            DateAndTimeConverter.getTimeFromDateLong(endTimeInLong)
                        } ?: ""
                        Text(
                            text = dateEndText,
                            style = dataTextStyle,
                        )
                        Text(
                            text = " $timeEndText",
                            style = dataTextStyle,
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth(),
                value = route.basicData.number ?: "",
                onValueChange = onNumberChanged,
                placeholder = {
                    Text(text = "маршрута", style = dataTextStyle)
                },
                prefix = {
                    Text(text = "№ ", style = dataTextStyle)
                },
                singleLine = true,
                textStyle = dataTextStyle.copy(
                    color = MaterialTheme.colorScheme.primary,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = Shapes.medium,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number
                )
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onRestChanged(!route.basicData.restPointOfTurnover)
                    }
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = Shapes.medium
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val textRest =
                    if (route.basicData.restPointOfTurnover) "Отдых в ПО" else "Домашний отдых"
                Text(
                    modifier = Modifier.wrapContentHeight(),
                    text = textRest,
                    style = dataTextStyle
                )

                val textHint = if (!moreInfoRestVisible) "Подробнее" else "Скрыть"
                Text(
                    modifier = Modifier
                        .background(color = Color.Transparent, shape = Shapes.medium)
                        .clickable {
                            moreInfoRestVisible = !moreInfoRestVisible
                        },
                    text = textHint,
                    style = hintStyle,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        item {
            AnimatedVisibility(
                visible = moreInfoRestVisible,
                enter = slideInHorizontally(animationSpec = tween(durationMillis = 300)) + fadeIn(
                    animationSpec = tween(durationMillis = 300)
                ),
                exit = slideOutHorizontally(animationSpec = tween(durationMillis = 300)) + fadeOut(
                    animationSpec = tween(durationMillis = 150)
                ),
                label = ""
            ) {
                if (route.basicData.restPointOfTurnover) {
                    InfoRestPointOfTurnoverTime(
                        minUntilTimeRest = dialogRestUiState.minUntilTimeRestPointOfTurnover,
                        fullUntilTimeRest = dialogRestUiState.fullUntilTimeRestPointOfTurnover,
                        onSettingClick = onSettingClick,
                        minTimeRest = dialogRestUiState.minTimeRestPointOfTurnover
                    )
                } else {
                    InfoRestOfHmeOfTime(
                        untilTimeHomeRest = dialogRestUiState.untilTimeHomeRest,
                        minTimeRest = dialogRestUiState.minTimeHomeRest,
                        onSettingClick = onSettingClick,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = Shapes.medium
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Работа в одно лицо",
                    style = dataTextStyle,
                )
                Switch(
                    checked = route.basicData.isOnePersonOperation,
                    onCheckedChange = checkedOnePersonOperation
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, top = 16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val basicId = route.basicData.id
                ItemAddingScreen(
                    title = stringResource(id = R.string.locomotive),
                    contentList = locoListState,
                    onChangeElementClick = onChangeLocoClick,
                    onNewElementClick = onNewLocoClick,
                    basicId = basicId,
                    onDeleteClick = onDeleteLoco
                ) { index, locomotive ->
                    LocomotiveSubItem(locomotive, index)
                }
                ItemAddingScreen(
                    title = stringResource(id = R.string.train),
                    contentList = trainListState,
                    onChangeElementClick = onChangeTrainClick,
                    onNewElementClick = onNewTrainClick,
                    basicId = basicId,
                    onDeleteClick = onDeleteTrain
                ) { index, train ->
                    TrainSubItem(index, train)
                }
                ItemAddingScreen(
                    title = stringResource(id = R.string.passenger),
                    contentList = passengerListState,
                    onChangeElementClick = onChangePassengerClick,
                    onNewElementClick = onNewPassengerClick,
                    basicId = basicId,
                    onDeleteClick = onDeletePassenger
                ) { index, passenger ->
                    PassengerSubItem(index, passenger)
                }
                ItemNotes(
                    modifier = Modifier.padding(top = 8.dp),
                    notes = route.basicData.notes,
                    onNotesChanged = onNotesChanged,
                )
            }
        }
    }
}

@Composable
fun <T> ItemAddingScreen(
    title: String,
    contentList: List<T>?,
    onChangeElementClick: (element: T) -> Unit,
    onNewElementClick: (basicId: String) -> Unit,
    basicId: String,
    onDeleteClick: (element: T) -> Unit,
    subItem: @Composable RowScope.(index: Int, element: T) -> Unit
) {
    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Normal)
            )
            TextButton(onClick = { onNewElementClick(basicId) }) {
                Text(
                    text = "Добавить",
                    style = hintStyle,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        contentList?.let { elements ->
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                elements.forEachIndexed { index, element ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChangeElementClick(element) }
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = Shapes.medium
                            )
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(0.95f)
                                .padding(end = 8.dp)
                        ) {
                            subItem(index, element)
                        }
                        Icon(
                            modifier = Modifier
                                .weight(0.05f)
                                .clickable { onDeleteClick(element) },
                            imageVector = Icons.Outlined.Clear, contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocomotiveSubItem(locomotive: Locomotive, index: Int) {
    val series = locomotive.series ?: locomotive.type.text
    val number = locomotive.number ?: ""
    val numberText = if (locomotive.number != null) {
        "№$number"
    } else {
        ""
    }
    val type = locomotive.type.text
    if (locomotive.series.isNullOrBlank() && locomotive.number.isNullOrBlank()) {
        Text(
            text = "$type № ${index + 1}",
            style = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
        )
    } else {
        Text(
            text = "$series $numberText",
            style = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
        )
    }
}

@Composable
private fun TrainSubItem(index: Int, train: Train) {
    if (train.number.isNullOrBlank()) {
        Text(
            text = "Поезд № ${index + 1}",
            style = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
        )
    } else {
        val stationStart = if (train.stations.isNotEmpty()) {
            train.stations.first().stationName ?: ""
        } else {
            ""
        }

        val stationEnd = if (train.stations.isNotEmpty() && train.stations.size > 1) {
            " - ${train.stations.last().stationName ?: ""}"
        } else {
            ""
        }

        Text(
            text = "№ ${train.number} $stationStart$stationEnd",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
        )
    }
}

@Composable
private fun PassengerSubItem(index: Int, passenger: Passenger) {
    if (passenger.trainNumber.isNullOrBlank()) {
        Text(
            text = "Пассажиром № ${index + 1}",
            style = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
        )
    } else {
        Text(
            text = "№ ${passenger.trainNumber}",
            style = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
        )
    }
}

@Composable
fun ItemNotes(
    modifier: Modifier = Modifier,
    notes: String?,
    onNotesChanged: (String) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            modifier = Modifier
                .heightIn(max = 105.dp)
                .fillMaxWidth(),
            value = notes ?: "",
            onValueChange = {
                onNotesChanged(it)
            },
            placeholder = {
                Text(
                    text = "Примечания",
                    style = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
                )
            },
            textStyle = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            ),
            shape = Shapes.medium
        )
    }
}

@Composable
fun InfoRestOfHmeOfTime(
    minTimeRest: Long?,
    untilTimeHomeRest: ResultState<Long?>,
    onSettingClick: () -> Unit
) {
    val hintStyle = AppTypography.getType().titleLarge.copy(
        fontSize = 18.sp,
        fontWeight = FontWeight.Light
    )

    Column(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                shape = Shapes.medium
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        val minTimeRestText = ConverterLongToTime.getTimeInStringFormat(minTimeRest)
        val link = buildAnnotatedString {
            val text = stringResource(id = R.string.info_text_min_time_rest, minTimeRestText)

            val endIndex = text.length - 1
            val startIndex = startIndexLastWord(text)

            append(text)
            addStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.tertiary,
                    textDecoration = TextDecoration.Underline
                ), start = startIndex, end = endIndex
            )

            addStringAnnotation(
                tag = LINK_TO_SETTING,
                annotation = LINK_TO_SETTING,
                start = startIndex,
                end = endIndex
            )
        }

        AsyncData(
            resultState = untilTimeHomeRest,
            loadingContent = {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            },
            errorContent = {
                Text(text = "Ошибка вычисления", style = hintStyle)
            }
        ) {
            val untilTimeHomeRestText =
                ConverterLongToTime.getDateAndTimeStringFormat(it)
            Text(text = "Отдых до $untilTimeHomeRestText", style = hintStyle)
        }

        ClickableText(
            modifier = Modifier.padding(top = 12.dp),
            text = link,
            style = AppTypography.getType().bodyMedium.copy(
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.primary
            ),
        ) {
            link.getStringAnnotations(LINK_TO_SETTING, it, it).firstOrNull()?.let {
                onSettingClick()
            }
        }
        Text(
            text = "\nформула расчета\n(время рабочее * 2,6) - время отдыха в ПО",
            style = AppTypography.getType().bodyMedium.copy(
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.primary
            )
        )

    }
}

@Composable
fun InfoRestPointOfTurnoverTime(
    minUntilTimeRest: ResultState<Long?>,
    fullUntilTimeRest: ResultState<Long?>,
    onSettingClick: () -> Unit,
    minTimeRest: Long?
) {
    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )

    val minTimeRestText = ConverterLongToTime.getTimeInStringFormat(minTimeRest)

    val link = buildAnnotatedString {
        val text = stringResource(id = R.string.info_text_min_time_rest, minTimeRestText)

        val endIndex = text.length - 1
        val startIndex = startIndexLastWord(text)

        append(text)
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.tertiary,
                textDecoration = TextDecoration.Underline
            ), start = startIndex, end = endIndex
        )

        addStringAnnotation(
            tag = LINK_TO_SETTING,
            annotation = LINK_TO_SETTING,
            start = startIndex,
            end = endIndex
        )
    }

    Column(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                shape = Shapes.medium
            )
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        AsyncData(
            resultState = minUntilTimeRest,
            loadingContent = {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            },
            errorContent = {
                Text(text = "Ошибка вычисления", style = hintStyle)
            }
        ) {
            val minUntilTimeRestText =
                ConverterLongToTime.getDateAndTimeStringFormat(it)
            Text(
                modifier = Modifier.padding(horizontal = 16.dp), text = stringResource(
                    id = R.string.min_time_rest_text, minUntilTimeRestText
                ), style = hintStyle, textAlign = TextAlign.End
            )
        }

        AsyncData(
            resultState = fullUntilTimeRest,
            loadingContent = {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            },
            errorContent = {
                Text(text = "Ошибка вычисления", style = hintStyle)
            }
        ) {
            val fullUntilTimeRestText =
                ConverterLongToTime.getDateAndTimeStringFormat(it)

            Text(
                modifier = Modifier.padding(horizontal = 16.dp), text = stringResource(
                    id = R.string.complete_time_rest_text, fullUntilTimeRestText
                ), style = hintStyle, textAlign = TextAlign.End
            )
        }




        ClickableText(
            modifier = Modifier.padding(
                start = 16.dp, bottom = 16.dp, end = 16.dp, top = 12.dp
            ),
            text = link,
            style = AppTypography.getType().bodyMedium.copy(
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.primary
            ),
        ) {
            link.getStringAnnotations(LINK_TO_SETTING, it, it).firstOrNull()?.let {
                onSettingClick()
            }
        }
    }
}
