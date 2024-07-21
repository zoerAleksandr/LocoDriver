package com.z_company.route.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.component.TimePickerDialog
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.DateAndTimeFormat
import com.z_company.route.viewmodel.StationFormState
import de.charlex.compose.RevealDirection
import de.charlex.compose.RevealSwipe
import de.charlex.compose.RevealValue
import de.charlex.compose.rememberRevealState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StationItem(
    index: Int,
    stationFormState: StationFormState,
    onStationNameChanged: (Int, String) -> Unit,
    onArrivalTimeChanged: (Int, Long?) -> Unit,
    onDepartureTimeChanged: (Int, Long?) -> Unit,
    onDelete: (StationFormState) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val revealState = rememberRevealState()
    val scope = rememberCoroutineScope()
    val isFirst = index == 0

    var showArrivalTimePicker by remember {
        mutableStateOf(false)
    }

    var showArrivalDatePicker by remember {
        mutableStateOf(false)
    }

    var showDepartureTimePicker by remember {
        mutableStateOf(false)
    }

    var showDepartureDatePicker by remember {
        mutableStateOf(false)
    }

    val arrivalTime = Calendar.getInstance().also { calendar ->
        stationFormState.arrival.data?.let {
            calendar.timeInMillis = it
        }
    }

    val departureTime = Calendar.getInstance().also { calendar ->
        stationFormState.departure.data?.let {
            calendar.timeInMillis = it
        }
    }

    val arrivalCalendar by remember {
        mutableStateOf(arrivalTime)
    }

    val departureCalendar by remember {
        mutableStateOf(departureTime)
    }

    val arrivalTimePickerState = rememberTimePickerState(
        initialHour = arrivalCalendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = arrivalCalendar.get(Calendar.MINUTE),
        is24Hour = true
    )

    val arrivalDatePickerState =
        rememberDatePickerStateInLocale(initialSelectedDateMillis = arrivalCalendar.timeInMillis)

    val departureTimePickerState = rememberTimePickerState(
        initialHour = departureCalendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = departureCalendar.get(Calendar.MINUTE),
        is24Hour = true
    )

    val departureDatePickerState =
        rememberDatePickerStateInLocale(initialSelectedDateMillis = departureCalendar.timeInMillis)

    val dataTextStyle = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)

    RevealSwipe(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        state = revealState,
        directions = setOf(
            RevealDirection.EndToStart
        ),
        hiddenContentEnd = {
            IconButton(onClick = {
                onDelete(stationFormState)
                scope.launch {
                    revealState.animateTo(RevealValue.Default)
                }
            }) {
                Icon(
                    modifier = Modifier.padding(end = 15.dp),
                    imageVector = Icons.Outlined.Delete,
                    tint = Color.White,
                    contentDescription = null
                )
            }
        },
        backgroundCardEndColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
        shape = Shapes.medium
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        ) {
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .weight(0.5f),
                    value = stationFormState.station.data ?: "",
                    onValueChange = {
                        onStationNameChanged(index, it)
                    },
                    placeholder = {
                        Text(
                            text = "Станция",
                            style = dataTextStyle
                        )
                    },
                    textStyle = dataTextStyle,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            scope.launch {
                                focusManager.clearFocus()
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

                Box(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight()
                        .background(color = MaterialTheme.colorScheme.surface, shape = Shapes.medium)
                        .clickable(!isFirst) {
                            showArrivalDatePicker = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!isFirst) {
                        val textTimeArrival = stationFormState.arrival.data?.let { millis ->
                            SimpleDateFormat(
                                DateAndTimeFormat.TIME_FORMAT,
                                Locale.getDefault()
                            ).format(
                                millis
                            )
                        } ?: DateAndTimeFormat.DEFAULT_TIME_TEXT

                        Text(
                            text = textTimeArrival,
                            maxLines = 1,
                            style = dataTextStyle
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight()
                        .background(color = MaterialTheme.colorScheme.surface, shape = Shapes.medium)
                        .clickable {
                            showDepartureDatePicker = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val textTimeDeparture = stationFormState.departure.data?.let { millis ->
                        SimpleDateFormat(
                            DateAndTimeFormat.TIME_FORMAT,
                            Locale.getDefault()
                        ).format(
                            millis
                        )
                    } ?: DateAndTimeFormat.DEFAULT_TIME_TEXT

                    Text(
                        text = textTimeDeparture,
                        maxLines = 1,
                        style = dataTextStyle
                    )

                }
            }
        }
    }

    if (showArrivalDatePicker) {
        CustomDatePickerDialog(
            datePickerState = arrivalDatePickerState,
            onDismissRequest = {
                showArrivalDatePicker = false
            },
            onConfirmRequest = {
                showArrivalDatePicker = false
                showArrivalTimePicker = true
                arrivalCalendar.timeInMillis = arrivalDatePickerState.selectedDateMillis!!
            })
    }

    if (showArrivalTimePicker) {
        TimePickerDialog(
            timePickerState = arrivalTimePickerState,
            onDismissRequest = { showArrivalTimePicker = false },
            onConfirmRequest = {
                showArrivalTimePicker = false
                arrivalCalendar.set(Calendar.HOUR_OF_DAY, arrivalTimePickerState.hour)
                arrivalCalendar.set(Calendar.MINUTE, arrivalTimePickerState.minute)
                onArrivalTimeChanged(index, arrivalCalendar.timeInMillis)
            }
        )
    }

    if (showDepartureDatePicker) {
        CustomDatePickerDialog(
            datePickerState = departureDatePickerState,
            onDismissRequest = { showDepartureDatePicker = false },
            onConfirmRequest = {
                showDepartureDatePicker = false
                showDepartureTimePicker = true
                departureCalendar.timeInMillis = departureDatePickerState.selectedDateMillis!!
            }
        )
    }

    if (showDepartureTimePicker) {
        TimePickerDialog(
            timePickerState = departureTimePickerState,
            onDismissRequest = { showDepartureTimePicker = false },
            onConfirmRequest = {
                showDepartureTimePicker = false
                departureCalendar.set(Calendar.HOUR_OF_DAY, departureTimePickerState.hour)
                departureCalendar.set(Calendar.MINUTE, departureTimePickerState.minute)
                onDepartureTimeChanged(index, departureCalendar.timeInMillis)
            }
        )
    }
}