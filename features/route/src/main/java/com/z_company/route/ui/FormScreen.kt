package com.z_company.route.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.ConverterUrlBase64
import com.z_company.domain.entities.route.Route
import com.z_company.route.viewmodel.RouteFormUiState
import com.z_company.domain.util.minus
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Photo
import com.z_company.domain.entities.route.Train
import com.z_company.route.R
import com.z_company.route.component.BottomShadow
import com.z_company.route.component.rememberDatePickerStateInLocale
import java.util.Calendar
import com.z_company.route.extention.isScrollInInitialState
import com.maxkeppeker.sheets.core.views.Grid
import com.z_company.core.ui.component.TopSnackbar
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.route.component.ConfirmExitDialog
import kotlinx.coroutines.launch
import com.z_company.core.R as CoreR

const val LINK_TO_SETTING = "LINK_TO_SETTING"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    formUiState: RouteFormUiState,
    currentRoute: Route?,
    exitScreen: () -> Unit,
    onSaveClick: () -> Unit,
    onSettingClick: () -> Unit,
    onClearAllField: () -> Unit,
    onBack: () -> Unit,
    resetSaveState: () -> Unit,
    onNumberChanged: (String) -> Unit,
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
    onNewPhotoClick: (basicId: String) -> Unit,
    onDeletePhoto: (photo: Photo) -> Unit,
    onPhotoClick: (photoId: String) -> Unit,
    minTimeRest: Long?,
    nightTime: Long?,
    changeShowConfirmExitDialog: (Boolean) -> Unit,
    exitWithoutSave: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Маршрут",
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
                        ClickableText(
                            modifier = Modifier.padding(end = 16.dp),
                            text = AnnotatedString("Готово"),
                            style = AppTypography.getType().titleMedium.copy(color = MaterialTheme.colorScheme.tertiary),
                        ) {
                            onSaveClick()
                        }
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackBarData ->
                TopSnackbar(snackBarData = snackBarData)
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
                            onNumberChanged = onNumberChanged,
                            onNotesChanged = onNotesChanged,
                            errorMessage = formUiState.errorMessage,
                            onTimeStartWorkChanged = onTimeStartWorkChanged,
                            onTimeEndWorkChanged = onTimeEndWorkChanged,
                            onRestChanged = onRestChanged,
                            onSettingClick = onSettingClick,
                            minUntilTimeRest = formUiState.minTimeRest,
                            fullUntilTimeRest = formUiState.fullTimeRest,
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
                            onNewPhotoClick = onNewPhotoClick,
                            onDeletePhoto = onDeletePhoto,
                            onPhotoClick = onPhotoClick,
                            minTimeRest = minTimeRest,
                            nightTime = nightTime,
                            showConfirmExitDialog = formUiState.confirmExitDialogShow,
                            changeShowConfirmExitDialog = changeShowConfirmExitDialog,
                            onSaveClick = onSaveClick,
                            exitWithoutSave = exitWithoutSave
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
    onNumberChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    errorMessage: String?,
    onTimeStartWorkChanged: (Long?) -> Unit,
    onTimeEndWorkChanged: (Long?) -> Unit,
    onRestChanged: (Boolean) -> Unit,
    onSettingClick: () -> Unit,
    minUntilTimeRest: Long?,
    fullUntilTimeRest: Long?,
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
    onNewPhotoClick: (basicId: String) -> Unit,
    onDeletePhoto: (photo: Photo) -> Unit,
    onPhotoClick: (photoId: String) -> Unit,
    minTimeRest: Long?,
    nightTime: Long?,
    showConfirmExitDialog: Boolean,
    changeShowConfirmExitDialog: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    exitWithoutSave: () -> Unit
) {
    val dataTextStyle = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)

    val scrollState = rememberLazyListState()

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

    val startOfWorkTime = Calendar.getInstance().also { calendar ->
        route.basicData.timeStartWork?.let {
            calendar.timeInMillis = it
        }
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

    val endOfWorkTime = Calendar.getInstance().also { calendar ->
        route.basicData.timeEndWork?.let {
            calendar.timeInMillis = it
        }
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
                                painter = painterResource(id = CoreR.drawable.ic_star_border),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = ConverterLongToTime.getTimeInStringFormat(nightTime),
                                style = AppTypography.getType().titleMedium,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                modifier = Modifier.padding(end = 4.dp),
                                painter = painterResource(id = CoreR.drawable.ic_star_border),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = ConverterLongToTime.getTimeInStringFormat(
                                    route.getPassengerTime() ?: 0L
                                ),
                                style = AppTypography.getType().titleMedium,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
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
                    val dateStartText = startTimeInLong?.let {
                        DateAndTimeConverter.getDateFromDateLong(startTimeInLong)
                    } ?: "Начало"
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
                Row(
                    modifier = Modifier
                        .weight(1f)
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
                    val dateEndText = endTimeInLong?.let {
                        DateAndTimeConverter.getDateFromDateLong(endTimeInLong)
                    } ?: "Окончание"
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
                keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Words)
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val text =
                    if (route.basicData.restPointOfTurnover) "Отдых в ПО" else "Домашний отдых"
                Text(text = text, style = dataTextStyle)
                AnimatedVisibility(
                    visible = (route.basicData.restPointOfTurnover) && (route.getWorkTime() != null)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        tint = MaterialTheme.colorScheme.tertiary,
                        contentDescription = null,
                        modifier = Modifier.clickable {
                            moreInfoRestVisible = !moreInfoRestVisible
                        }
                    )
                }
            }
        }

        item {
            val minUntilTimeRestText =
                ConverterLongToTime.getDateAndTimeStringFormat(minUntilTimeRest)
            val fullUntilTimeRestText =
                ConverterLongToTime.getDateAndTimeStringFormat(fullUntilTimeRest)
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
            AnimatedVisibility(
                visible = moreInfoRestVisible && (route.basicData.restPointOfTurnover) && (route.getWorkTime() != null),
                enter = slideInHorizontally(animationSpec = tween(durationMillis = 300)) + fadeIn(
                    animationSpec = tween(durationMillis = 300)
                ),
                exit = slideOutHorizontally(animationSpec = tween(durationMillis = 300)) + fadeOut(
                    animationSpec = tween(durationMillis = 150)
                ),
                label = ""
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp), text = stringResource(
                            id = R.string.min_time_rest_text, minUntilTimeRestText
                        ), style = AppTypography.getType().titleMedium, textAlign = TextAlign.End
                    )

                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp), text = stringResource(
                            id = R.string.complete_time_rest_text, fullUntilTimeRestText
                        ), style = AppTypography.getType().titleMedium, textAlign = TextAlign.End
                    )

                    ClickableText(
                        modifier = Modifier.padding(
                            start = 16.dp, bottom = 16.dp, end = 16.dp, top = 12.dp
                        ),
                        text = link,
                        style = AppTypography.getType().bodySmall.copy(
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                    ) {
                        link.getStringAnnotations(LINK_TO_SETTING, it, it).firstOrNull()?.let {
                            onSettingClick()
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, top = 16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    notes = route.basicData.notes,
                    onNotesChanged = onNotesChanged,
                    photosList = route.photos,
                    onDeletePhoto = onDeletePhoto,
                    onPhotoClick = onPhotoClick
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    onClick = { onNewPhotoClick(basicId) }
                ) {
                    Icon(
                        modifier = Modifier.padding(end = 6.dp),
                        painter = painterResource(id = R.drawable.add_a_photo_24px),
                        contentDescription = null
                    )
                    Text(
                        "Добавить фото",
                        style = AppTypography.getType().titleLarge
                            .copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Normal
                            )
                    )
                }
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
            ClickableText(
                text = AnnotatedString(
                    text = "Добавить",
                    spanStyle = SpanStyle(
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = AppTypography.getType().titleMedium.fontSize,
                    )
                )
            ) {
                onNewElementClick(basicId)
            }
        }
        contentList?.let { elements ->
            Column(
                modifier = Modifier
                    .padding(top = 12.dp)
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
                        subItem(index, element)
                        Icon(
                            modifier = Modifier.clickable { onDeleteClick(element) },
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
    val numberText = if (locomotive.number != null){"№$number"}else{""}
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
            train.stations.first().stationName
        } else {
            ""
        }

        val stationEnd = if (train.stations.isNotEmpty()) {
            train.stations.last().stationName
        } else {
            ""
        }

        Text(
            text = "№ ${train.number} $stationStart - $stationEnd",
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
    notes: String?,
    onNotesChanged: (String) -> Unit,
    photosList: List<Photo>,
    onDeletePhoto: (photo: Photo) -> Unit,
    onPhotoClick: (photoId: String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {


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

        val widthScreen = LocalConfiguration.current.screenWidthDp
        val imageSize = (widthScreen - 12 - 24) / 3

        Grid(
            modifier = Modifier.padding(top = 4.dp),
            items = photosList,
            columns = 3,
            rowSpacing = 6.dp,
            columnSpacing = 6.dp
        ) { photo ->
            Card(
                modifier = Modifier
                    .size(height = imageSize.dp, width = imageSize.dp)
                    .clickable {
                        onPhotoClick(photo.photoId)
                    },
                shape = Shapes.extraSmall,

                ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    photo.base64.let { base64String ->
                        val decodedImage = ConverterUrlBase64.base64toBitmap(base64String)
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            painter = rememberAsyncImagePainter(model = decodedImage),
                            contentScale = ContentScale.Crop,
                            contentDescription = null
                        )
                    }
                    IconButton(modifier = Modifier
                        .padding(4.dp)
                        .align(Alignment.TopEnd)
                        .background(
                            color = Color.White.copy(alpha = 0.3f), shape = CircleShape
                        )
                        .wrapContentSize(), onClick = { onDeletePhoto(photo) }) {
                        Icon(
                            imageVector = Icons.Default.Clear, contentDescription = null
                        )
                    }
                }
            }
        }
    }
}