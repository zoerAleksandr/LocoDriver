package com.example.route.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import com.example.route.component.TimePickerDialog
import com.example.route.component.DatePickerDialog
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.core.ResultState
import com.example.core.ui.component.AsyncData
import com.example.core.ui.component.GenericError
import com.example.core.ui.theme.Shapes
import com.example.core.ui.theme.custom.AppTypography
import com.example.core.util.ConverterLongToTime
import com.example.domain.entities.route.Route
import com.example.route.viewmodel.RouteFormUiState
import com.example.domain.util.minus
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.core.util.DateAndTimeFormat
import com.example.domain.entities.route.Locomotive
import com.example.domain.entities.route.Notes
import com.example.domain.entities.route.Passenger
import com.example.domain.entities.route.Train
import com.example.route.R
import com.example.route.component.BottomShadow
import com.example.route.component.ConfirmExitDialog
import com.example.route.component.rememberDatePickerStateInLocale
import java.util.Calendar
import com.example.route.extention.isScrollInInitialState

const val LINK_TO_SETTING = "LINK_TO_SETTING"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    formUiState: RouteFormUiState,
    currentRoute: Route?,
    onExit: () -> Unit,
    exitWithoutSave: () -> Unit,
    checkBeforeExit: () -> Unit,
    showExitConfirmDialog: (Boolean) -> Unit,
    onRouteSaved: () -> Unit,
    onSaveClick: () -> Unit,
    onSettingClick: () -> Unit,
    onClearAllField: () -> Unit,
    resetSaveState: () -> Unit,
    onNumberChanged: (String) -> Unit,
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
    onNotesClick: (notes: Notes?) -> Unit,
    onDeleteNotes: (notes: Notes) -> Unit
) {
    if (formUiState.confirmExitDialogShow) {
        ConfirmExitDialog(
            showExitConfirmDialog,
            onSaveClick,
            exitWithoutSave
        )
    }
    if (formUiState.exitFromScreen) {
        LaunchedEffect(Unit) {
            onExit()
        }
    }
    Scaffold(
        topBar = {
            MediumTopAppBar(title = {
                Text(
                    text = "Маршрут",
                )
            }, navigationIcon = {
                IconButton(onClick = checkBeforeExit) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
            }, actions = {
                ClickableText(text = AnnotatedString(text = "Сохранить"),
                    onClick = { onSaveClick() }

                )
                var dropDownExpanded by remember { mutableStateOf(false) }

                IconButton(onClick = {
                    dropDownExpanded = true
                }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert, contentDescription = "Меню"
                    )
                    DropdownMenu(
                        expanded = dropDownExpanded,
                        onDismissRequest = { dropDownExpanded = false },
                        offset = DpOffset(x = 4.dp, y = 8.dp)
                    ) {
                        DropdownMenuItem(modifier = Modifier.padding(horizontal = 16.dp),
                            onClick = {
                                onClearAllField.invoke()
                                dropDownExpanded = false
                            },
                            text = {
                                Text(
                                    text = "Очистить",
                                )
                            })
                    }
                }
            })
        },
    ) {
        Box(Modifier.padding(it)) {
            AsyncData(resultState = formUiState.routeDetailState) {
                currentRoute?.let { route ->
                    AsyncData(resultState = formUiState.saveRouteState, errorContent = {
                        GenericError(
                            onDismissAction = resetSaveState
                        )
                    }) {
                        if (formUiState.saveRouteState is ResultState.Success) {
                            LaunchedEffect(formUiState.saveRouteState) {
                                onRouteSaved()
                            }
                        } else {
                            RouteFormScreenContent(
                                route = route,
                                onNumberChanged = onNumberChanged,
                                errorMessage = formUiState.errorMessage,
                                onTimeStartWorkChanged = onTimeStartWorkChanged,
                                onTimeEndWorkChanged = onTimeEndWorkChanged,
                                onRestChanged = onRestChanged,
                                onSettingClick = onSettingClick,
                                minTimeRest = formUiState.minTimeRest,
                                fullTimeRest = formUiState.fullTimeRest,
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
                                notesState = route.notes,
                                onNotesClick = onNotesClick,
                                onDeleteNotes = onDeleteNotes
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
private fun RouteFormScreenContent(
    route: Route,
    onNumberChanged: (String) -> Unit,
    errorMessage: String?,
    onTimeStartWorkChanged: (Long?) -> Unit,
    onTimeEndWorkChanged: (Long?) -> Unit,
    onRestChanged: (Boolean) -> Unit,
    onSettingClick: () -> Unit,
    minTimeRest: Long?,
    fullTimeRest: Long?,
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
    notesState: Notes?,
    onNotesClick: (notes: Notes?) -> Unit,
    onDeleteNotes: (notes: Notes) -> Unit
) {
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
                onTimeStartWorkChanged(startCalendar.timeInMillis)
            })
    }

    if (showStartDatePicker) {
        DatePickerDialog(datePickerState = startDatePickerState, onDismissRequest = {
            showStartDatePicker = false
        }, onConfirmRequest = {
            showStartDatePicker = false
            showStartTimePicker = true
            startCalendar.timeInMillis = startDatePickerState.selectedDateMillis!!
        }, onClearRequest = {
            showStartDatePicker = false
            onTimeStartWorkChanged(null)
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
        TimePickerDialog(
            timePickerState = endTimePickerState,
            onDismissRequest = { showEndTimePicker = false },
            onConfirmRequest = {
                showEndTimePicker = false
                endCalendar.set(Calendar.HOUR_OF_DAY, endTimePickerState.hour)
                endCalendar.set(Calendar.MINUTE, endTimePickerState.minute)
                onTimeEndWorkChanged(endCalendar.timeInMillis)
            }
        )
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            datePickerState = endDatePickerState,
            onDismissRequest = {
                showEndDatePicker = false
            },
            onConfirmRequest = {
                showEndDatePicker = false
                showEndTimePicker = true
                endCalendar.timeInMillis = endDatePickerState.selectedDateMillis!!
            },
            onClearRequest = {
                showEndDatePicker = false
                onTimeEndWorkChanged(null)
            }
        )
    }

    AnimatedVisibility(
        modifier = Modifier
            .zIndex(1f),
        visible = !scrollState.isScrollInInitialState(),
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        BottomShadow()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        state = scrollState,
    ) {
        val startTimeInLong = route.basicData.timeStartWork
        val endTimeInLong = route.basicData.timeEndWork
        val workTimeInLong = endTimeInLong - startTimeInLong
        val workTimeInFormatted = ConverterLongToTime.getTimeInStringFormat(workTimeInLong)

        item {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                errorMessage?.let { message ->
                    Icon(
                        imageVector = Icons.Default.Warning,
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = "Ошибка"
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = message,
                    )
                } ?: Text(text = workTimeInFormatted, style = AppTypography.getType().headlineLarge)

            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(modifier = Modifier
                    .clickable {
                        showStartDatePicker = true
                    }
                    .border(
                        width = 1.dp,
                        shape = Shapes.small,
                        color = MaterialTheme.colorScheme.outline
                    )
                    .padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val dateStartText = startTimeInLong?.let { millis ->
                        SimpleDateFormat(
                            DateAndTimeFormat.DATE_FORMAT, Locale.getDefault()
                        ).format(
                            millis
                        )
                    } ?: DateAndTimeFormat.DEFAULT_DATE_TEXT

                    Text(
                        text = dateStartText,
                        style = AppTypography.getType().bodyLarge,
                    )
                    startTimeInLong?.let { millis ->
                        val time = SimpleDateFormat(
                            DateAndTimeFormat.TIME_FORMAT, Locale.getDefault()
                        ).format(millis)
                        Text(
                            text = time,
                            style = AppTypography.getType().bodyLarge,
                        )
                    }
                }

                Column(modifier = Modifier
                    .clickable {
                        showEndDatePicker = true
                    }
                    .border(
                        width = 1.dp,
                        shape = Shapes.small,
                        color = MaterialTheme.colorScheme.outline
                    )
                    .padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val dateStartText = endTimeInLong?.let { millis ->
                        SimpleDateFormat(
                            DateAndTimeFormat.DATE_FORMAT, Locale.getDefault()
                        ).format(
                            millis
                        )
                    } ?: DateAndTimeFormat.DEFAULT_DATE_TEXT

                    Text(
                        text = dateStartText,
                        style = AppTypography.getType().bodyLarge,
                    )
                    endTimeInLong?.let { millis ->
                        val time = SimpleDateFormat(
                            DateAndTimeFormat.TIME_FORMAT, Locale.getDefault()
                        ).format(millis)
                        Text(
                            text = time,
                            style = AppTypography.getType().bodyLarge,
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp, top = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    modifier = Modifier
                        .padding(end = 8.dp),
                    text = stringResource(id = R.string.text_switсh_onRest)
                )

                Switch(
                    checked = route.basicData.restPointOfTurnover,
                    onCheckedChange = {
                        onRestChanged(it)
                    },
                )
            }
        }

        val minTimeRestText = ConverterLongToTime.getDateAndTimeStringFormat(minTimeRest)
        val fullTimeRestText = ConverterLongToTime.getDateAndTimeStringFormat(fullTimeRest)

        item {
            val link = buildAnnotatedString {
                val text =
                    stringResource(id = R.string.info_text_min_time_rest, minTimeRestText)

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
                visible = route.basicData.restPointOfTurnover,
                enter = slideInHorizontally(animationSpec = tween(durationMillis = 300))
                        + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = slideOutHorizontally(animationSpec = tween(durationMillis = 300))
                        + fadeOut(animationSpec = tween(durationMillis = 150)),
                label = ""
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)),
                    horizontalAlignment = Alignment.Start
                ) {
                    Icon(
                        modifier = Modifier.padding(
                            top = 16.dp,
                            start = 16.dp,
                            bottom = 8.dp
                        ),
                        imageVector = Icons.Default.Info,
                        tint = MaterialTheme.colorScheme.tertiary,
                        contentDescription = null
                    )

                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(
                            id = R.string.min_time_rest_text,
                            minTimeRestText
                        ),
                        style = AppTypography.getType().bodyMedium,
                        textAlign = TextAlign.End
                    )

                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(
                            id = R.string.complete_time_rest_text,
                            fullTimeRestText
                        ),
                        style = AppTypography.getType().bodyMedium,
                        textAlign = TextAlign.End
                    )

                    ClickableText(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            bottom = 16.dp,
                            end = 16.dp,
                            top = 12.dp
                        ),
                        text = link,
                        style = AppTypography.getType().bodySmall
                            .copy(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                    ) {
                        link.getStringAnnotations(LINK_TO_SETTING, it, it)
                            .firstOrNull()?.let {
                                onSettingClick()
                            }
                    }
                }
            }
        }
        item {
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current

            OutlinedTextField(
                value = route.basicData.number ?: "",
                onValueChange = onNumberChanged,
                singleLine = true,
                placeholder = {
                    Text(text = "маршрута")
                },
                prefix = {
                    Text(text = "№")
                },
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Words),
            )
        }
        item {
            Column(
                modifier = Modifier
                    .padding(bottom = 32.dp)
            ) {
                val basicId = route.basicData.id
                Spacer(modifier = Modifier.height(10.dp))
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
                Spacer(modifier = Modifier.height(16.dp))
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
                Spacer(modifier = Modifier.height(16.dp))
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
                Spacer(modifier = Modifier.height(16.dp))
                ItemNotes(
                    notesState,
                    onNotesClick,
                    onDeleteNotes,
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
    Card(
        modifier = Modifier.padding(horizontal = 16.dp),
        shape = Shapes.extraSmall
    ) {
        Row(
            modifier = Modifier
                .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = AppTypography.getType().titleLarge
            )
            IconButton(onClick = { onNewElementClick(basicId) }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        }
        contentList?.let { elements ->
            Column(
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp)
                    .fillMaxWidth()
            ) {
                elements.forEachIndexed { index, element ->
                    Row(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .fillMaxWidth()
                            .clickable { onChangeElementClick(element) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        subItem(index, element)
                        IconButton(onClick = { onDeleteClick(element) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        }
                    }
                    if (index != elements.lastIndex) {
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun LocomotiveSubItem(locomotive: Locomotive, index: Int) {
    if (locomotive.series.isNullOrBlank() && locomotive.number.isNullOrBlank()) {
        Text(text = "Локомотив №${index + 1}")
    } else {
        Text(
            text = "${locomotive.series} №${locomotive.number}",
        )
    }
}

@Composable
private fun TrainSubItem(index: Int, train: Train) {
    if (train.number.isNullOrBlank()) {
        Text(text = "Поезд №${index + 1}")
    } else {
        Text(
            text = "№${train.number}",
        )
    }
}

@Composable
private fun PassengerSubItem(index: Int, passenger: Passenger) {
    if (passenger.trainNumber.isNullOrBlank()) {
        Text(text = "След. пассажиром №${index + 1}")
    } else {
        Text(
            text = "№${passenger.trainNumber}",
        )
    }
}

@Composable
fun ItemNotes(
    notes: Notes?,
    onNotesClick: (notes: Notes?) -> Unit,
    onDeleteNotes: (notes: Notes) -> Unit,
) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp),
        shape = Shapes.extraSmall
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.notes),
                style = AppTypography.getType().titleLarge
            )
            IconButton(onClick = { onNotesClick(null) }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        }
        notes?.let {
            Row(
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier
                        .padding(bottom = 8.dp),
                    text = it.text ?: "",
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2
                )
                IconButton(onClick = { onDeleteNotes(it) }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                }
            }
        }
    }
}