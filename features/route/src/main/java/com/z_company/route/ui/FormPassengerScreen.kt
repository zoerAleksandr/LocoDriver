package com.z_company.route.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import com.z_company.route.component.StationDropdownMenu
import com.z_company.core.ui.theme.Shapes
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.z_company.core.R
import com.z_company.core.ResultState
import com.z_company.core.util.ConverterLongToTime
import com.z_company.domain.entities.route.Passenger
import com.z_company.route.component.BottomShadow
import com.z_company.route.extention.isScrollInInitialState
import kotlinx.coroutines.launch
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.component.DateTimePickerBottomSheet
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.viewmodel.PassengerFormUiState
import com.z_company.route.viewmodel.PassengerFormViewModel
import com.z_company.core.util.TimeManager

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun FormPassengerScreen(
    viewModel: PassengerFormViewModel,
    formUiState: PassengerFormUiState,
    currentPassenger: Passenger?,
    onPassengerSaved: () -> Unit,
    resetSaveState: () -> Unit,
    onNumberChanged: (String) -> Unit,
    onStationDepartureChanged: (String) -> Unit,
    onStationArrivalChanged: (String) -> Unit,
    onTimeDepartureChanged: (Long?) -> Unit,
    onTimeArrivalChanged: (Long?) -> Unit,
    onNotesChanged: (String) -> Unit,
    resultTime: Long?,
    errorMessage: String?,
    dropDownMenuList: List<String>,
    isExpandedMenuDepartureStation: Boolean,
    isExpandedMenuArrivalStation: Boolean,
    changeExpandMenuDepartureStation: (Boolean) -> Unit,
    changeExpandMenuArrivalStation: (Boolean) -> Unit,
    onDeleteStationName: (String) -> Unit,
    onChangedDropDownContentDepartureStation: (String) -> Unit,
    onChangedDropDownContentArrivalStation: (String) -> Unit,
    dateAndTimeConverter: DateAndTimeConverter?
) {
    val displayTz = dateAndTimeConverter?.timeZoneText ?: "GMT+3"
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val hintStyle = MaterialTheme.typography.bodyMedium
    val dataTextStyle = MaterialTheme.typography.bodyLarge

    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val noValueColor = primaryColor.copy(alpha = 0.5f)

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
                title = {},
                navigationIcon = {
                    TextButton(
                        onClick = viewModel::savePassenger,
                        enabled = formUiState.errorMessage == null,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.tertiary,
                            containerColor = Color.Transparent
                        )
                    ) {
                        Text(
                            text = "Готово",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                actions = {},
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
            )
        }
    ) { paddingValues ->
        when (val state = formUiState.savePassengerState) {
            is ResultState.Success -> {
                LaunchedEffect(formUiState.savePassengerState) {
                    onPassengerSaved()
                }
            }

            is ResultState.Error -> {
                LaunchedEffect(Unit) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Ошибка: ${state.entity.message}")
                    }
                    resetSaveState()
                }
            }

            else -> {}
        }

        var showBottomSheetRemoveTimeDeparture by remember {
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
                        onTimeDepartureChanged(null)
                    }
                )
            )
        }

        var showBottomSheetRemoveTimeArrival by remember() {
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
                        onTimeArrivalChanged(null)
                    }
                )
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

        Box(modifier = Modifier.padding(paddingValues)) {
            currentPassenger?.let { passenger ->
                LazyColumn(
                    state = scrollState,
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    // итоговое время
                    item {
                        if (errorMessage == null) {
                            val timeResultInFormatted = viewModel.convertTimeToStringFormat(resultTime)
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
                                        style = MaterialTheme.typography.titleMedium,
                                        color = primaryColor
                                    )
                                }
                            }
                        }
                    }

                    // ошибка
                    item {
                        formUiState.errorMessage?.let {
                            val widthScreen = LocalConfiguration.current.screenWidthDp.toFloat()
                            val gradient = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                ),
                                center = Offset(Float.POSITIVE_INFINITY, 0f),
                                radius = widthScreen * 2
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                elevation = CardDefaults.elevatedCardElevation(
                                    defaultElevation = 3.dp,
                                    pressedElevation = 0.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            brush = gradient,
                                            shape = MaterialTheme.shapes.medium
                                        )
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = it,
                                        style = hintStyle,
                                        color = MaterialTheme.colorScheme.onError
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // номер поезда
                    item {
                        OutlinedTextFieldApp(
                            modifier = Modifier
                                .fillMaxWidth(),
                            value = passenger.trainNumber ?: "",
                            onValueChange = {
                                onNumberChanged(it)
                            },
                            placeholder = {
                                Text(
                                    text = "поезда",
                                    color = noValueColor,
                                    style = LocalTextStyle.current.copy(
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            },
                            prefix = {
                                Text(
                                    text = "№ ",
                                    style = LocalTextStyle.current.copy(
                                        fontWeight = FontWeight.Light
                                    ),
                                    color = noValueColor
                                )
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
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // станция отправления
                    item {
                        val focusRequester = remember { FocusRequester() }

                        var stationName by remember {
                            mutableStateOf(
                                TextFieldValue(
                                    text = passenger.stationDeparture ?: "",
                                    selection = TextRange(
                                        passenger.stationDeparture?.length ?: 0
                                    )
                                )
                            )
                        }

                        ExposedDropdownMenuBox(
                            modifier = Modifier.fillMaxWidth(),
                            expanded = isExpandedMenuDepartureStation,
                            onExpandedChange = { changeExpandMenuDepartureStation(it) }
                        ) {
                            OutlinedTextFieldApp(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                                    .focusRequester(focusRequester),
                                value = stationName,
                                onValueChange = {
                                    stationName = it
                                    onChangedDropDownContentDepartureStation(it.text)
                                    onStationDepartureChanged(it.text)
                                },
                                textStyle = dataTextStyle,
                                placeholder = {
                                    Text(
                                        text = "От станции",
                                        style = LocalTextStyle.current.copy(
                                            fontWeight = FontWeight.Light
                                        ),
                                        color = noValueColor
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

                            StationDropdownMenu(
                                expanded = isExpandedMenuDepartureStation,
                                stations = dropDownMenuList,
                                onSelect = { selectionStation ->
                                    onStationDepartureChanged(selectionStation)
                                    changeExpandMenuDepartureStation(false)
                                    stationName = stationName.copy(
                                        text = selectionStation,
                                        selection = TextRange(selectionStation.length)
                                    )
                                },
                                onDelete = onDeleteStationName,
                                onDismiss = { changeExpandMenuDepartureStation(false) },
                                textStyle = dataTextStyle
                            )
                        }
                    }

                    // время отправления
                    item {
                        val animatedBackgroundColorsDeparture by animateColorAsState(
                            targetValue = if (passenger.timeDeparture == null) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.secondary,
                            animationSpec = tween(
                                durationMillis = 200,
                                easing = FastOutSlowInEasing
                            )
                        )

                        val animatedTextColorsDeparture by animateColorAsState(
                            targetValue = if (passenger.timeDeparture == null) noValueColor
                            else primaryColor,
                            animationSpec = tween(
                                durationMillis = 200,
                                easing = FastOutSlowInEasing
                            )
                        )

                        var showDepartureDatePicker by remember {
                            mutableStateOf(false)
                        }

                        if (showDepartureDatePicker) {
                            DateTimePickerBottomSheet(
                                title = "Отправление",
                                onDateTimeSelected = { timestamp ->
                                    onTimeDepartureChanged(timestamp)
                                },
                                onDismiss = { showDepartureDatePicker = false },
                                startDateTime = passenger.timeDeparture ?: TimeManager().now(),
                                timeZoneStr = displayTz,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(elevation = 2.dp, shape = Shapes.medium)
                                .background(
                                    color = animatedBackgroundColorsDeparture,
                                    shape = Shapes.medium
                                )
                                .combinedClickable(
                                    onClick = {
                                        showDepartureDatePicker = true
                                    },
                                    onLongClick = {
                                        passenger.timeDeparture?.let {
                                            showBottomSheetRemoveTimeDeparture = true
                                        }
                                    }
                                )
                                .padding(12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val textDateAndTimeDeparture = passenger.timeDeparture?.let {
                                dateAndTimeConverter?.getDateAndTime(it)
                            } ?: "Время отправления"

                            val style = passenger.timeDeparture?.let {
                                dataTextStyle
                            } ?: LocalTextStyle.current

                            Text(
                                text = textDateAndTimeDeparture,
                                style = style,
                                color = animatedTextColorsDeparture
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // станция прибытия
                    item {
                        val focusRequester = remember { FocusRequester() }

                        var stationName by remember {
                            mutableStateOf(
                                TextFieldValue(
                                    text = passenger.stationArrival ?: "",
                                    selection = TextRange(passenger.stationArrival?.length ?: 0)
                                )
                            )
                        }

                        ExposedDropdownMenuBox(
                            modifier = Modifier.fillMaxWidth(),
                            expanded = isExpandedMenuArrivalStation,
                            onExpandedChange = { changeExpandMenuArrivalStation(it) }
                        ) {
                            OutlinedTextFieldApp(
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                                    .focusRequester(focusRequester)
                                    .fillMaxWidth(),
                                value = stationName,
                                onValueChange = {
                                    stationName = it
                                    onChangedDropDownContentArrivalStation(it.text)
                                    onStationArrivalChanged(it.text)
                                },
                                placeholder = {
                                    Text(
                                        text = "До станции",
                                        style = LocalTextStyle.current.copy(
                                            fontWeight = FontWeight.Light
                                        ),
                                        color = noValueColor
                                    )
                                },
                                textStyle = dataTextStyle,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true,
                            )

                            StationDropdownMenu(
                                expanded = isExpandedMenuArrivalStation,
                                stations = dropDownMenuList,
                                onSelect = { selectionStation ->
                                    onStationArrivalChanged(selectionStation)
                                    changeExpandMenuArrivalStation(false)
                                    stationName = stationName.copy(
                                        text = selectionStation,
                                        selection = TextRange(selectionStation.length)
                                    )
                                },
                                onDelete = onDeleteStationName,
                                onDismiss = { changeExpandMenuArrivalStation(false) },
                                textStyle = dataTextStyle
                            )
                        }
                    }

                    // время прибытия
                    item {
                        val animatedBackgroundColorsArrival by animateColorAsState(
                            targetValue = if (passenger.timeArrival == null) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.secondary,
                            animationSpec = tween(
                                durationMillis = 200,
                                easing = FastOutSlowInEasing
                            )
                        )

                        val animatedTextColorsTimeArrival by animateColorAsState(
                            targetValue = if (passenger.timeArrival == null) noValueColor
                            else primaryColor,
                            animationSpec = tween(
                                durationMillis = 200,
                                easing = FastOutSlowInEasing
                            )
                        )

                        var showArrivalDatePicker by remember {
                            mutableStateOf(false)
                        }

                        if (showArrivalDatePicker) {
                            DateTimePickerBottomSheet(
                                title = "Отправление",
                                onDateTimeSelected = { timestamp ->
                                    onTimeArrivalChanged(timestamp)
                                },
                                onDismiss = { showArrivalDatePicker = false },
                                startDateTime = passenger.timeArrival ?: TimeManager().now(),
                                timeZoneStr = displayTz,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .shadow(elevation = 2.dp, shape = Shapes.medium)
                                .fillMaxWidth()
                                .background(
                                    color = animatedBackgroundColorsArrival,
                                    shape = Shapes.medium
                                )

                                .combinedClickable(
                                    onClick = {
                                        showArrivalDatePicker = true
                                    },
                                    onLongClick = {
                                        passenger.timeArrival?.let {
                                            showBottomSheetRemoveTimeArrival = true
                                        }
                                    }
                                )
                                .padding(12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val textDateAndTimeArrival =
                                passenger.timeArrival?.let {
                                    dateAndTimeConverter?.getDateAndTime(it)
                                } ?: "Время прибытия"

                            val style = passenger.timeArrival?.let {
                                dataTextStyle
                            } ?: LocalTextStyle.current
                            Text(
                                text = textDateAndTimeArrival,
                                style = style,
                                color = animatedTextColorsTimeArrival
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    // примечания
                    item {
                        OutlinedTextFieldApp(
                            modifier = Modifier
                                .fillMaxWidth(),
                            value = passenger.notes ?: "",
                            onValueChange = { onNotesChanged(it) },
                            textStyle = dataTextStyle,
                            placeholder = {
                                Text(
                                    text = "Примечания",
                                    style = LocalTextStyle.current.copy(
                                        fontWeight = FontWeight.Light
                                    ),
                                    color = noValueColor
                                )
                            },
                            shape = Shapes.medium,
                        )
                    }
                }
            }
        }
    }
}