package com.z_company.route.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import com.z_company.route.viewmodel.StationFormState
import de.charlex.compose.RevealDirection
import de.charlex.compose.RevealSwipe
import de.charlex.compose.RevealValue
import de.charlex.compose.rememberRevealState
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun StationItem(
    modifier: Modifier,
    isFirst: Boolean,
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
    dateAndTimeConverter: DateAndTimeConverter?
) {
    val revealState = rememberRevealState()
    val scope = rememberCoroutineScope()
    val dataTextStyle = MaterialTheme.typography.bodyLarge
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
                    modifier = Modifier
                        .fillMaxWidth(),
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
                    modifier = Modifier
                        .fillMaxWidth(),
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
                onArrivalTimeChanged(index,timestamp)
            },
            onDismiss = { showArrivalDatePicker = false },
            startDateTime = arrivalDateTime
        )
    }

    if (showDepartureDatePicker) {
        DateTimePickerBottomSheet(
            title = "Отправление",
            onDateTimeSelected = { timestamp ->
                onDepartureTimeChanged(index,timestamp)
            },
            onDismiss = { showDepartureDatePicker = false },
            startDateTime = departureDateTime
        )
    }

    RevealSwipe(
        modifier = modifier
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
                    painter = painterResource(com.z_company.route.R.drawable.delete_24px),
                    tint = MaterialTheme.colorScheme.secondary,
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
                    .fillMaxWidth()
                    .padding(bottom = 2.dp, end = 2.dp, start = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                if (!it.isFocused && stationName.text != stationFormState.station.data) {
                                    onStationNameChanged(index, stationName.text)
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

                Box(
                    modifier = Modifier
                        .weight(0.25f)
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
                    if (!isFirst) {
                        val textTimeArrival = stationFormState.arrival.data?.let {
                            dateAndTimeConverter?.getTimeFromDateLong(it)
                        } ?: DateAndTimeFormat.DEFAULT_TIME_TEXT

                        Text(
                            text = textTimeArrival,
                            maxLines = 1,
                            style = dataTextStyle,
                            color = animatedTextColorsArrival
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(0.25f)
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
                        style = dataTextStyle,
                        color = animatedTextColorsDeparture
                    )
                }
            }
        }
    }
}