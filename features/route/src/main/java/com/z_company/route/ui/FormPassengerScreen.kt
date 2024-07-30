package com.z_company.route.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import com.z_company.core.ui.theme.Shapes
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.domain.entities.route.Passenger
import com.z_company.route.component.BottomShadow
import com.z_company.route.extention.isScrollInInitialState
import kotlinx.coroutines.launch
import com.z_company.route.component.CustomDatePickerDialog
import com.z_company.core.ui.component.TimePickerDialog
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.route.component.ConfirmExitDialog
import com.z_company.route.component.rememberDatePickerStateInLocale
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormPassengerScreen(
    currentPassenger: Passenger?,
    passengerDetailState: ResultState<Passenger?>,
    changeHaveState: Boolean,
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
    formValid: Boolean,
    exitScreen: () -> Unit,
    exitFromScreenState: Boolean,
    changeShowConfirmExitDialog: (Boolean) -> Unit,
    showConfirmExitDialogState: Boolean,
    exitWithoutSave: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )
    val titleStyle = AppTypography.getType().headlineMedium.copy(fontWeight = FontWeight.Light)
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
            SnackbarHost(snackbarHostState) { snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Пассажиром",
                        style = titleStyle
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
                    AsyncData(
                        resultState = savePassengerState,
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
                            enabled = changeHaveState,
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
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        if (savePassengerState is ResultState.Error) {
            LaunchedEffect(Unit) {
                scope.launch {
                    snackbarHostState.showSnackbar("Ошибка: ${savePassengerState.entity.message}")
                }
                resetSaveState()
            }
        }
        if (exitFromScreenState) {
            LaunchedEffect(Unit) {
                exitScreen()
            }
        }
        if (showConfirmExitDialogState) {
            ConfirmExitDialog(
                showExitConfirmDialog = changeShowConfirmExitDialog,
                onSaveClick = onSaveClick,
                exitWithoutSave = exitWithoutSave
            )
        }

        Box(modifier = Modifier.padding(paddingValues)) {
            AsyncData(resultState = passengerDetailState) {
                currentPassenger?.let { passenger ->
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
                        )
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
) {
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val dataTextStyle = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)

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
        horizontalAlignment = Alignment.End,
        contentPadding = PaddingValues(16.dp)
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
                placeholder = {
                    Text(text = "поезда", style = dataTextStyle)
                },
                prefix = {
                    Text(text = "№ ", style = dataTextStyle)
                },
                textStyle = dataTextStyle,
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
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = Shapes.medium,

            )
        }
        item {
            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .weight(0.5f),
                    value = passenger.stationDeparture ?: "",
                    onValueChange = {
                        onStationDepartureChanged(it)
                    },
                    textStyle = dataTextStyle,
                    placeholder = {
                        Text(
                            text = "От станции",
                            style = dataTextStyle
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
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = Shapes.medium,
                )

                OutlinedTextField(
                    modifier = Modifier
                        .weight(0.5f),
                    value = passenger.stationArrival ?: "",
                    onValueChange = {
                        onStationArrivalChanged(it)
                    },
                    placeholder = {
                        Text(
                            text = "До станции",
                            style = dataTextStyle
                        )
                    },
                    textStyle = dataTextStyle,
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
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = Shapes.medium,
                )
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
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(0.5f)
                        .background(color = MaterialTheme.colorScheme.surface, shape = Shapes.medium)
                        .clickable {
                            showArrivalDatePicker = true
                        }.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val textDateDeparture = passenger.timeDeparture?.let {
                        DateAndTimeConverter.getDateFromDateLong(passenger.timeDeparture)
                    } ?: "Отправление"
                    val textTimeDeparture = passenger.timeDeparture?.let {
                        DateAndTimeConverter.getTimeFromDateLong(passenger.timeDeparture)
                    } ?: ""

                    Text(text = textDateDeparture, style = dataTextStyle)
                    Text(text = textTimeDeparture, style = dataTextStyle)
                }

                Row(
                    modifier = Modifier
                        .weight(0.5f)
                        .background(color = MaterialTheme.colorScheme.surface, shape = Shapes.medium)
                        .clickable {
                            showDepartureDatePicker = true
                        }.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val textDateArrival =
                        passenger.timeArrival?.let {
                            DateAndTimeConverter.getDateFromDateLong(passenger.timeArrival)
                        } ?: "Прибытие"

                    val textTimeArrival =
                        passenger.timeArrival?.let {
                            DateAndTimeConverter.getTimeFromDateLong(passenger.timeArrival)
                        } ?: ""

                    Text(text = textDateArrival, style = dataTextStyle)
                    Text(text = textTimeArrival, style = dataTextStyle)
                }
            }
        }

        item {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                value = passenger.notes ?: "",
                onValueChange = { onNotesChanged(it) },
                textStyle = dataTextStyle,
                placeholder = {
                    Text(
                        text = "Примечания",
                        style = dataTextStyle
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Default
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = Shapes.medium,
            )
        }
    }
}
