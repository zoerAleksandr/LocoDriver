package com.z_company.route.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BasicTooltipBox
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberBasicTooltipState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.z_company.core.R
import com.z_company.core.ui.component.DateTimePickerBottomSheet
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.DateAndTimeFormat
import com.z_company.domain.entities.route.UtilsForEntities
import com.z_company.route.viewmodel.StationFormState
import java.util.Calendar

private val warningColor = Color(0xFFFFC107)
private val warningTextColor = Color(0xFF3E2723)
private val dangerColor = Color(0xFFEF5350)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StationItem(
    modifier: Modifier,
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
    selectIndexState: MutableState<Int>,
    dateAndTimeConverter: DateAndTimeConverter?,
    trainNumber: String? = null,
    isReorderMode: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    val dataTextStyle = MaterialTheme.typography.bodyLarge
    var timeTextStyle by remember { mutableStateOf(dataTextStyle) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val noValueColor = primaryColor.copy(alpha = 0.5f)
    val sheetState = rememberModalBottomSheetState()

    val focusRequester = remember(stationFormState.id) { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var showBottomSheetRemoveTimeArrival by remember(stationFormState.id) {
        mutableStateOf(false)
    }

    if (showBottomSheetRemoveTimeArrival) {
        AppBottomSheet(
            onDismissRequest = { showBottomSheetRemoveTimeArrival = false },
            sheetState = sheetState,
            headerContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Время прибытия",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            actions = listOf(
                BottomSheetAction(text = "Удалить значение") {
                    onArrivalTimeChanged(index, null)
                }
            )
        )
    }

    var showBottomSheetRemoveTimeDeparture by remember(stationFormState.id) {
        mutableStateOf(false)
    }

    if (showBottomSheetRemoveTimeDeparture) {
        AppBottomSheet(
            onDismissRequest = { showBottomSheetRemoveTimeDeparture = false },
            sheetState = sheetState,
            headerContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Время отправления",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            actions = listOf(
                BottomSheetAction(text = "Удалить значение") {
                    onDepartureTimeChanged(index, null)
                }
            )
        )
    }

    var showArrivalDatePicker by remember(stationFormState.id) {
        mutableStateOf(false)
    }

    var showDepartureDatePicker by remember(stationFormState.id) {
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

    if (showArrivalDatePicker) {
        DateTimePickerBottomSheet(
            title = "Прибытие",
            onDateTimeSelected = { timestamp ->
                onArrivalTimeChanged(index, timestamp)
            },
            onDismiss = { showArrivalDatePicker = false },
            startDateTime = arrivalDateTime
        )
    }

    if (showDepartureDatePicker) {
        DateTimePickerBottomSheet(
            title = "Отправление",
            onDateTimeSelected = { timestamp ->
                onDepartureTimeChanged(index, timestamp)
            },
            onDismiss = { showDepartureDatePicker = false },
            startDateTime = departureDateTime
        )
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete(stationFormState)
            }
            false
        }
    )

    // Стоянка (минуты между прибытием и отправлением)
    val stopMinutes = if (stationFormState.arrival.data != null && stationFormState.departure.data != null) {
        val diff = stationFormState.departure.data - stationFormState.arrival.data
        if (diff > 0) (diff / 60_000).toString() else null
    } else null

    // Определение пассажирского поезда
    val isPassengerTrain = trainNumber?.toIntOrNull()?.let { num ->
        UtilsForEntities.passengerTrainNumberList.any { range -> num in range }
    } ?: false

    // Цвета фона стоянки
    val stopLong = stopMinutes?.toLongOrNull() ?: 0L
    val stopBackground = when {
        stopLong > 20 && isPassengerTrain -> dangerColor
        stopLong > 30 -> dangerColor
        stopLong > 5 -> warningColor
        else -> Color.Transparent
    }
    val stopTextColor = when {
        stopLong > 20 && isPassengerTrain -> Color.White
        stopLong > 30 -> Color.White
        stopLong > 5 -> warningTextColor
        else -> primaryColor.copy(alpha = 0.7f)
    }

    Column(modifier = modifier.fillMaxWidth().wrapContentHeight()) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = !isReorderMode,
            backgroundContent = {
                val color by animateColorAsState(
                    when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.Settled -> Color.Transparent
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        else -> Color.Transparent
                    }, label = ""
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(color = color, shape = Shapes.medium),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        modifier = Modifier.padding(end = 16.dp),
                        painter = painterResource(com.z_company.route.R.drawable.delete_24px),
                        tint = MaterialTheme.colorScheme.surface,
                        contentDescription = null
                    )
                }
            }
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            ) {
                Row(
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .fillMaxWidth()
                        .padding(bottom = 2.dp, end = 2.dp, start = 1.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    ExposedDropdownMenuBox(
                        modifier = Modifier
                            .weight(0.5f),
                        expanded = isExpandedMenu,
                        onExpandedChange = { onExpandedMenuChange(index, it) }
                    ) {
                        var stationName by remember(key1 = stationFormState.id) {
                            mutableStateOf(
                                TextFieldValue(
                                    text = stationFormState.station.data ?: "",
                                    selection = TextRange(stationFormState.station.data?.length ?: 0)
                                )
                            )
                        }

                        LaunchedEffect(stationFormState.station.data) {
                            if (stationName.text != stationFormState.station.data) {
                                stationName = stationName.copy(text = stationFormState.station.data ?: "")
                            }
                        }

                        OutlinedTextFieldApp(
                            modifier = Modifier
                                .menuAnchor()
                                .focusRequester(focusRequester)
                                .onFocusChanged {
                                    if (!it.isFocused) {
                                        if (stationName.text != stationFormState.station.data) {
                                            onStationNameChanged(index, stationName.text)
                                        }
                                        stationName = stationName.copy(selection = TextRange(0))
                                    }
                                },
                            value = stationName,
                            onValueChange = {
                                stationName = it
                                onChangedContentMenu(index, it.text)
                            },
                            placeholder = {
                                Text(
                                    text = "Станция",
                                    style = LocalTextStyle.current.copy(
                                        fontWeight = FontWeight.Light
                                    ),
                                    color = noValueColor
                                )
                            },
                            textStyle = dataTextStyle,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onStationNameChanged(index, stationName.text)
                                }
                            ),
                            singleLine = true,
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
                                            .fillMaxWidth()
                                            .background(
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = Shapes.medium
                                            ),
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = selectionStation, style = dataTextStyle, color = primaryColor)
                                                Icon(
                                                    modifier = Modifier.clickable {
                                                        onDeleteStationName(selectionStation)
                                                    },
                                                    painter = painterResource(R.drawable.ic_clear),
                                                    contentDescription = null,
                                                    tint = primaryColor
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

                    // Время (arrival + стоянка + departure) — 50%
                    Row(
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val animatedBackgroundColorsArrival by animateColorAsState(
                            targetValue = if (stationFormState.arrival.data == null) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.secondary,
                            animationSpec = tween(
                                durationMillis = 200,
                                easing = FastOutSlowInEasing
                            )
                        )

                        val animatedBackgroundColorsDeparture by animateColorAsState(
                            targetValue = if (stationFormState.departure.data == null) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.secondary,
                            animationSpec = tween(
                                durationMillis = 200,
                                easing = FastOutSlowInEasing
                            )
                        )

                        val animatedTextColorsArrival by animateColorAsState(
                            targetValue = if (stationFormState.arrival.data == null) primaryColor.copy(alpha = 0.5f)
                            else primaryColor,
                            animationSpec = tween(
                                durationMillis = 200,
                                easing = FastOutSlowInEasing
                            )
                        )
                        val animatedTextColorsDeparture by animateColorAsState(
                            targetValue = if (stationFormState.departure.data == null) primaryColor.copy(alpha = 0.5f)
                            else primaryColor,
                            animationSpec = tween(
                                durationMillis = 200,
                                easing = FastOutSlowInEasing
                            )
                        )

                        // Arrival time
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .shadow(elevation = 2.dp, shape = Shapes.medium)
                                .fillMaxHeight()
                                .background(
                                    color = animatedBackgroundColorsArrival,
                                    shape = Shapes.medium
                                )
                                .combinedClickable(
                                    onClick = {
                                        showArrivalDatePicker = true
                                    },
                                    onLongClick = {
                                        selectIndexState.value = index
                                        stationFormState.arrival.data?.let {
                                            showBottomSheetRemoveTimeArrival = true
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val textTimeArrival = stationFormState.arrival.data?.let {
                                dateAndTimeConverter?.getTimeFromDateLong(it)
                            } ?: DateAndTimeFormat.DEFAULT_TIME_TEXT

                            Text(
                                text = textTimeArrival,
                                maxLines = 1,
                                softWrap = false,
                                style = timeTextStyle,
                                color = animatedTextColorsArrival,
                                onTextLayout = { result ->
                                    if (result.hasVisualOverflow) {
                                        timeTextStyle = timeTextStyle.copy(
                                            fontSize = timeTextStyle.fontSize * 0.9
                                        )
                                    }
                                }
                            )
                        }

                        // Стоянка (фиксированная ширина 40dp)
                        BasicTooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = { Text("Время стоянки") },
                            state = rememberBasicTooltipState()
                        ) {
                            Box(
                                modifier = Modifier.width(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (stopMinutes != null) {
                                    Box(
                                        modifier = Modifier
                                            .background(stopBackground, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 2.dp, vertical = 1.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${stopMinutes}'",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = stopTextColor,
                                            maxLines = 1,
                                            softWrap = false,
                                            onTextLayout = { result ->
                                                if (result.hasVisualOverflow) {
                                                    timeTextStyle = timeTextStyle.copy(
                                                        fontSize = timeTextStyle.fontSize * 0.9
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Departure time
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .shadow(elevation = 2.dp, shape = Shapes.medium)
                                .fillMaxHeight()
                                .background(
                                    color = animatedBackgroundColorsDeparture,
                                    shape = Shapes.medium
                                )
                                .combinedClickable(
                                    onClick = {
                                        showDepartureDatePicker = true
                                    },
                                    onLongClick = {
                                        selectIndexState.value = index
                                        stationFormState.departure.data?.let {
                                            showBottomSheetRemoveTimeDeparture = true
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val textTimeDeparture = stationFormState.departure.data?.let {
                                dateAndTimeConverter?.getTimeFromDateLong(it)
                            } ?: DateAndTimeFormat.DEFAULT_TIME_TEXT

                            Text(
                                text = textTimeDeparture,
                                maxLines = 1,
                                softWrap = false,
                                style = timeTextStyle,
                                color = animatedTextColorsDeparture,
                                onTextLayout = { result ->
                                    if (result.hasVisualOverflow) {
                                        timeTextStyle = timeTextStyle.copy(
                                            fontSize = timeTextStyle.fontSize * 0.9
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Кнопки перемещения при reorder mode
        AnimatedVisibility(visible = isReorderMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onMoveUp != null) {
                    IconButton(onClick = onMoveUp) {
                        Icon(
                            painter = painterResource(com.z_company.route.R.drawable.keyboard_arrow_up_24px),
                            contentDescription = "Переместить вверх",
                            tint = primaryColor
                        )
                    }
                }
                if (onMoveDown != null) {
                    IconButton(onClick = onMoveDown) {
                        Icon(
                            painter = painterResource(com.z_company.route.R.drawable.keyboard_arrow_down_24px),
                            contentDescription = "Переместить вниз",
                            tint = primaryColor
                        )
                    }
                }
            }
        }
    }
}
