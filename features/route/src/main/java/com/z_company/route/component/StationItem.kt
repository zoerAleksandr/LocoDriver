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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.z_company.core.ui.component.SelectableDateTimePicker
import com.z_company.core.ui.component.WheelDateTimePicker
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.DateAndTimeFormat
import com.z_company.route.viewmodel.StationFormState
import de.charlex.compose.RevealDirection
import de.charlex.compose.RevealSwipe
import de.charlex.compose.RevealValue
import de.charlex.compose.rememberRevealState
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StationItem(
    index: Int,
    stationFormState: StationFormState,
    onStationNameChanged: (Int, String) -> Unit,
    menuList: List<String>,
    isExpandedMenu: Boolean,
    onExpandedMenuChange: (Int, Boolean) -> Unit,
    onChangedContentMenu: (Int, String) -> Unit,
    onArrivalTimeChanged: (Int, Long?) -> Unit,
    onDepartureTimeChanged: (Int, Long?) -> Unit,
    onDelete: (StationFormState) -> Unit,
    onDeleteStationName: (String) -> Unit,
    onSettingClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val revealState = rememberRevealState()
    val scope = rememberCoroutineScope()
    val isFirst = index == 0

    var showArrivalDatePicker by remember {
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

    val arrivalDateTime = arrivalTime.timeInMillis

    val departureDateTime = departureTime.timeInMillis

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
                val focusRequester = remember { FocusRequester() }

                ExposedDropdownMenuBox(
                    modifier = Modifier
                        .weight(0.5f),
                    expanded = isExpandedMenu,
                    onExpandedChange = { onExpandedMenuChange(index, it) }
                ) {
                    var stationName by remember {
                        mutableStateOf(
                            TextFieldValue(
                                text = stationFormState.station.data ?: "",
                                selection = TextRange(stationFormState.station.data?.length ?: 0)
                            )
                        )
                    }
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }

                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor()
                            .focusRequester(focusRequester),
                        value = stationName,
                        onValueChange = {
                            stationName = it
                            onStationNameChanged(index, it.text)
                            onChangedContentMenu(index, it.text)
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

                    if (menuList.isNotEmpty()) {
                        DropdownMenu(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = Shapes.medium
                                )
                                .exposedDropdownSize(true),
                            expanded = isExpandedMenu,
                            properties = PopupProperties(focusable = false),
                            onDismissRequest = { onExpandedMenuChange(index, false) }
                        ) {
                            menuList.forEach { selectionStation ->
                                DropdownMenuItem(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = selectionStation, style = dataTextStyle)
                                            Icon(
                                                modifier = Modifier.clickable {
                                                    onDeleteStationName(selectionStation)
                                                },
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = null
                                            )
                                        }

                                    },
                                    onClick = {
                                        onStationNameChanged(index, selectionStation)
                                        onExpandedMenuChange(index, false)
                                        stationName = stationName.copy(
                                            text = selectionStation,
                                            selection = TextRange(selectionStation.length)
                                        )
                                    })
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight()
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = Shapes.medium
                        )
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
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = Shapes.medium
                        )
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

    SelectableDateTimePicker(
        titleText = "Прибытие",
        isShowPicker = showArrivalDatePicker,
        initDateTime = arrivalDateTime,
        onDoneClick = { localDateTime ->
            val instant = localDateTime.toInstant(TimeZone.currentSystemDefault())
            val millis = instant.toEpochMilliseconds()
            onArrivalTimeChanged(index, millis)
            showArrivalDatePicker = false
        },
        onDismiss = {
            showArrivalDatePicker = false
        },
        onSettingClick = onSettingClick
    )

    SelectableDateTimePicker(
        titleText = "Отправление",
        isShowPicker = showDepartureDatePicker,
        initDateTime = departureDateTime,
        onDoneClick = { localDateTime ->
            val instant = localDateTime.toInstant(TimeZone.currentSystemDefault())
            val millis = instant.toEpochMilliseconds()
            onDepartureTimeChanged(index, millis)
            showDepartureDatePicker = false
        },
        onDismiss = {
            showDepartureDatePicker = false
        },
        onSettingClick = onSettingClick
    )
}