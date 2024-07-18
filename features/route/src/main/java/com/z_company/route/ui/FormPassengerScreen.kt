package com.z_company.route.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import com.z_company.core.ui.theme.Shapes
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.GenericError
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeFormat
import com.z_company.domain.entities.route.Passenger
import com.z_company.route.component.BottomShadow
import com.z_company.route.extention.isScrollInInitialState
import kotlinx.coroutines.launch
import com.z_company.route.component.CustomDatePickerDialog
import com.z_company.core.ui.component.TimePickerDialog
import com.z_company.route.component.rememberDatePickerStateInLocale
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormPassengerScreen(
    currentPassenger: Passenger?,
    passengerDetailState: ResultState<Passenger?>,
    savePassengerState: ResultState<Unit>?,
    onBackPressed: () -> Unit,
    onSaveClick: () -> Unit,
    onPassengerSaved: () -> Unit,
    onClearAllField: () -> Unit,
    resetSaveState: () -> Unit,
    onNumberChanged: (String) -> Unit,
    onStationDepartureChanged: (String) -> Unit,
    onStationArrivalChanged: (String) -> Unit,
    onTimeDepartureChanged: (Long?) -> Unit,
    onTimeArrivalChanged: (Long?) -> Unit,
    onNotesChanged: (String) -> Unit,
    resultTime: Long?,
    errorState: ResultState<Unit>?,
    resetError: () -> Unit,
    formValid: Boolean
) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            confirmValueChange = {
                it != SheetValue.Hidden
            }
        )
    )

    if (errorState is ResultState) {
        LaunchedEffect(errorState) {
            scope.launch {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = "Нарушена последовательность времени"
                )
            }
            resetError()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxWidth(),
        snackbarHost = {
            SnackbarHost(hostState = scaffoldState.snackbarHostState) { snackBarData ->
                Snackbar(snackBarData)
            }
        },
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = "Пассажиром",
                        style = AppTypography.getType().headlineSmall
                            .copy(color = MaterialTheme.colorScheme.primary)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBackPressed() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    ClickableText(
                        text = AnnotatedString(text = "Сохранить"),
                        style = AppTypography.getType().titleMedium,
                        onClick = { onSaveClick() }
                    )
                    var dropDownExpanded by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = {
                            dropDownExpanded = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Меню"
                        )
                        DropdownMenu(
                            expanded = dropDownExpanded,
                            onDismissRequest = { dropDownExpanded = false },
                            offset = DpOffset(x = 4.dp, y = 8.dp)
                        ) {
                            DropdownMenuItem(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                onClick = {
                                    onClearAllField()
                                    dropDownExpanded = false
                                },
                                text = {
                                    Text(
                                        text = "Очистить",
                                        style = AppTypography.getType().bodyLarge
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AsyncData(resultState = passengerDetailState) {
                currentPassenger?.let { passenger ->
                    AsyncData(resultState = savePassengerState, errorContent = {
                        GenericError(
                            onDismissAction = resetSaveState
                        )
                    }) {
                        if (savePassengerState is ResultState.Success) {
                            LaunchedEffect(savePassengerState) {
                                onPassengerSaved()
                            }
                        } else {
                            PassengerFormScreenContent(
                                passenger = passenger,
                                onNumberChanged = onNumberChanged,
                                onStationDepartureChanged = onStationDepartureChanged,
                                onStationArrivalChanged = onStationArrivalChanged,
                                onTimeDepartureChanged = onTimeDepartureChanged,
                                onTimeArrivalChanged = onTimeArrivalChanged,
                                onNotesChanged = onNotesChanged,
                                resultTime = resultTime,
                                formValid = formValid
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
fun PassengerFormScreenContent(
    passenger: Passenger,
    onNumberChanged: (String) -> Unit,
    onStationDepartureChanged: (String) -> Unit,
    onStationArrivalChanged: (String) -> Unit,
    onTimeDepartureChanged: (Long?) -> Unit,
    onTimeArrivalChanged: (Long?) -> Unit,
    onNotesChanged: (String) -> Unit,
    resultTime: Long?,
    formValid: Boolean,
) {
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val backgroundColor = if (!formValid) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        Color.Transparent
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
        state = scrollState,
        horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        item {
            val timeResultInFormatted = ConverterLongToTime.getTimeInStringFormat(resultTime)

            AnimatedVisibility(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                visible = resultTime != null && resultTime > 0,
                enter = slideInVertically(animationSpec = tween(durationMillis = 500))
                        + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = slideOutVertically(animationSpec = tween(durationMillis = 500))
                        + fadeOut(animationSpec = tween(durationMillis = 150))
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = timeResultInFormatted,
                        style = AppTypography.getType().headlineLarge
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                modifier = Modifier
                    .padding(top = 28.dp)
                    .fillMaxWidth(0.5f),
                value = passenger.trainNumber ?: "",
                onValueChange = {
                    onNumberChanged(it)
                },
                label = {
                    Text(text = "Номер поезда", color = MaterialTheme.colorScheme.secondary)
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
        }
        item {
            val departureTime = Calendar.getInstance().also { calendar ->
                passenger.timeDeparture?.let { millis ->
                    calendar.timeInMillis = millis
                }
            }
            val departureCalendar by remember {
                mutableStateOf(departureTime)
            }

            val departureTimePickerState = rememberTimePickerState(
                initialHour = departureCalendar.get(Calendar.HOUR_OF_DAY),
                initialMinute = departureCalendar.get(Calendar.MINUTE),
                is24Hour = true
            )

            val departureDatePickerState = rememberDatePickerStateInLocale(
                initialSelectedDateMillis = departureCalendar.timeInMillis
            )

            var showDepartureTimePicker by remember {
                mutableStateOf(false)
            }

            var showDepartureDatePicker by remember {
                mutableStateOf(false)
            }

            if (showDepartureDatePicker) {
                CustomDatePickerDialog(
                    datePickerState = departureDatePickerState,
                    onDismissRequest = { showDepartureDatePicker = false },
                    onConfirmRequest = {
                        showDepartureDatePicker = false
                        showDepartureTimePicker = true
                        departureCalendar.timeInMillis =
                            departureDatePickerState.selectedDateMillis!!
                    })
            }

            if (showDepartureTimePicker) {
                TimePickerDialog(
                    timePickerState = departureTimePickerState,
                    onDismissRequest = { showDepartureTimePicker = false },
                    onConfirmRequest = {
                        showDepartureTimePicker = false
                        departureCalendar.set(
                            Calendar.HOUR_OF_DAY,
                            departureTimePickerState.hour
                        )
                        departureCalendar.set(
                            Calendar.MINUTE,
                            departureTimePickerState.minute
                        )
                        onTimeDepartureChanged(departureCalendar.timeInMillis)
                    }
                )
            }

            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .padding(top = 1.dp, bottom = 9.dp)
                        .weight(0.7f),
                    value = passenger.stationDeparture ?: "",
                    onValueChange = {
                        onStationDepartureChanged(it)
                    },
                    label = {
                        Text(
                            text = "Ст. отправления",
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            scope.launch {
                                focusManager.moveFocus(FocusDirection.Down)
                            }
                        }
                    ),
                    singleLine = true,
                )

                Column(
                    modifier = Modifier
                        .padding(vertical = 9.dp)
                        .fillMaxHeight()
                        .weight(0.3f)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = Shapes.extraSmall
                        )
                        .background(backgroundColor)
                        .clickable {
                            showDepartureDatePicker = true
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val textDateDeparture =
                        passenger.timeDeparture?.let { millis ->
                            SimpleDateFormat(
                                DateAndTimeFormat.DATE_FORMAT,
                                Locale.getDefault()
                            ).format(
                                millis
                            )
                        } ?: DateAndTimeFormat.DEFAULT_DATE_TEXT

                    val textTimeDeparture =
                        passenger.timeDeparture?.let { millis ->
                            SimpleDateFormat(
                                DateAndTimeFormat.TIME_FORMAT,
                                Locale.getDefault()
                            ).format(
                                millis
                            )
                        } ?: DateAndTimeFormat.DEFAULT_TIME_TEXT

                    Text(text = textDateDeparture)
                    Text(text = textTimeDeparture)
                }
            }
        }

        item {
            val arrivalTime = Calendar.getInstance().also { calendar ->
                passenger.timeArrival?.let { millis ->
                    calendar.timeInMillis = millis
                }
            }

            val arrivalCalendar by remember {
                mutableStateOf(arrivalTime)
            }

            val arrivalTimePickerState = rememberTimePickerState(
                initialHour = arrivalCalendar.get(Calendar.HOUR_OF_DAY),
                initialMinute = arrivalCalendar.get(Calendar.MINUTE),
                is24Hour = true
            )

            val arrivalDatePickerState =
                rememberDatePickerStateInLocale(initialSelectedDateMillis = arrivalCalendar.timeInMillis)

            var showArrivalTimePicker by remember {
                mutableStateOf(false)
            }

            var showArrivalDatePicker by remember {
                mutableStateOf(false)
            }

            if (showArrivalDatePicker) {
                CustomDatePickerDialog(
                    datePickerState = arrivalDatePickerState,
                    onDismissRequest = { showArrivalDatePicker = false },
                    onConfirmRequest = {
                        showArrivalDatePicker = false
                        showArrivalTimePicker = true
                        arrivalCalendar.timeInMillis =
                            arrivalDatePickerState.selectedDateMillis!!
                    }
                )
            }

            if (showArrivalTimePicker) {
                TimePickerDialog(
                    timePickerState = arrivalTimePickerState,
                    onDismissRequest = { showArrivalTimePicker = false },
                    onConfirmRequest = {
                        showArrivalTimePicker = false
                        arrivalCalendar.set(
                            Calendar.HOUR_OF_DAY,
                            arrivalTimePickerState.hour
                        )
                        arrivalCalendar.set(
                            Calendar.MINUTE,
                            arrivalTimePickerState.minute
                        )
                        onTimeArrivalChanged(arrivalCalendar.timeInMillis)
                    }
                )
            }

            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .padding(top = 1.dp, bottom = 9.dp)
                        .weight(0.7f),
                    value = passenger.stationArrival ?: "",
                    onValueChange = {
                        onStationArrivalChanged(it)
                    },
                    label = {
                        Text(
                            text = "Ст. прибытия",
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            scope.launch {
                                focusManager.moveFocus(FocusDirection.Down)
                            }
                        }
                    ),
                    singleLine = true,
                )

                Column(
                    modifier = Modifier
                        .padding(vertical = 9.dp)
                        .fillMaxHeight()
                        .weight(0.3f)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = Shapes.extraSmall
                        )
                        .background(backgroundColor)
                        .clickable {
                            showArrivalDatePicker = true
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val textDateArrival =
                        passenger.timeArrival?.let { millis ->
                            SimpleDateFormat(
                                DateAndTimeFormat.DATE_FORMAT,
                                Locale.getDefault()
                            ).format(
                                millis
                            )
                        } ?: DateAndTimeFormat.DEFAULT_DATE_TEXT

                    val textTimeArrival =
                        passenger.timeArrival?.let { millis ->
                            SimpleDateFormat(
                                DateAndTimeFormat.TIME_FORMAT,
                                Locale.getDefault()
                            ).format(
                                millis
                            )
                        } ?: DateAndTimeFormat.DEFAULT_TIME_TEXT

                    Text(text = textDateArrival)
                    Text(text = textTimeArrival)
                }
            }
        }

        item {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp, top = 8.dp),
                value = passenger.notes ?: "",
                onValueChange = { onNotesChanged(it) },
                label = {
                    Text(
                        text = "Примечания",
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        scope.launch {
                            focusManager.clearFocus()
                        }
                    }
                ),
            )
        }
    }
}
