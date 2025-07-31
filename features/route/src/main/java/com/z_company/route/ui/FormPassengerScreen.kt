package com.z_company.route.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import com.z_company.core.ui.theme.Shapes
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.domain.entities.route.Passenger
import com.z_company.route.component.BottomShadow
import com.z_company.route.extention.isScrollInInitialState
import kotlinx.coroutines.launch
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.component.SelectableDateTimePicker
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.route.component.ConfirmExitDialog
import com.z_company.route.component.RemoveTimeContent
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
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
    errorMessage: String?,
    resetError: () -> Unit,
    formValid: Boolean,
    exitScreen: () -> Unit,
    exitFromScreenState: Boolean,
    changeShowConfirmExitDialog: (Boolean) -> Unit,
    showConfirmExitDialogState: Boolean,
    exitWithoutSave: () -> Unit,
    dropDownMenuList: List<String>,
    isExpandedMenuDepartureStation: Boolean,
    isExpandedMenuArrivalStation: Boolean,
    changeExpandMenuDepartureStation: (Boolean) -> Unit,
    changeExpandMenuArrivalStation: (Boolean) -> Unit,
    onDeleteStationName: (String) -> Unit,
    onChangedDropDownContentDepartureStation: (String) -> Unit,
    onChangedDropDownContentArrivalStation: (String) -> Unit,
    onSettingClick: () -> Unit,
    timeZoneText: String,
    dateAndTimeConverter: DateAndTimeConverter?
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


        val bottomSheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
        )
        var bottomSheetContentState =
            remember { mutableStateOf(BottomSheetRemoveTimeFormPassengerScreen.TIME_DEPARTURE) }

        ModalBottomSheetLayout(
            sheetState = bottomSheetState,
            sheetShape = MaterialTheme.shapes.medium,
            sheetContent = {
                RemoveTimeBottomSheetContent(
                    bottomSheetState = bottomSheetState,
                    onTimeDepartureChanged = onTimeDepartureChanged,
                    onTimeArrivalChanged = onTimeArrivalChanged,
                    selectBottomSheetContent = bottomSheetContentState.value
                )
            }
        ) {
            Box(modifier = Modifier.padding(paddingValues)) {
                AsyncData(resultState = passengerDetailState) {
                    currentPassenger?.let { passenger ->
                        if (savePassengerState is ResultState.Success) {
                            LaunchedEffect(savePassengerState) {
                                onPassengerSaved()
                            }
                        } else {
                            PassengerFormScreenContent(
                                errorMessage = errorMessage,
                                passenger = passenger,
                                onNumberChanged = onNumberChanged,
                                onStationDepartureChanged = onStationDepartureChanged,
                                onStationArrivalChanged = onStationArrivalChanged,
                                onTimeDepartureChanged = onTimeDepartureChanged,
                                onTimeArrivalChanged = onTimeArrivalChanged,
                                onNotesChanged = onNotesChanged,
                                resultTime = resultTime,
                                menuList = dropDownMenuList,
                                changeExpandMenuArrivalStation = changeExpandMenuArrivalStation,
                                changeExpandMenuDepartureStation = changeExpandMenuDepartureStation,
                                isExpandedMenuArrivalStation = isExpandedMenuArrivalStation,
                                isExpandedMenuDepartureStation = isExpandedMenuDepartureStation,
                                onDeleteStationName = onDeleteStationName,
                                onChangedDropDownContentDepartureStation = onChangedDropDownContentDepartureStation,
                                onChangedDropDownContentArrivalStation = onChangedDropDownContentArrivalStation,
                                onSettingClick = onSettingClick,
                                timeZoneText = timeZoneText,
                                bottomSheetState = bottomSheetState,
                                bottomSheetContentState = bottomSheetContentState,
                                dateAndTimeConverter = dateAndTimeConverter
                            )
                        }

                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun PassengerFormScreenContent(
    errorMessage: String?,
    passenger: Passenger,
    onNumberChanged: (String) -> Unit,
    onStationDepartureChanged: (String) -> Unit,
    onStationArrivalChanged: (String) -> Unit,
    onTimeDepartureChanged: (Long?) -> Unit,
    onTimeArrivalChanged: (Long?) -> Unit,
    onNotesChanged: (String) -> Unit,
    resultTime: Long?,
    menuList: List<String>,
    isExpandedMenuDepartureStation: Boolean,
    isExpandedMenuArrivalStation: Boolean,
    changeExpandMenuDepartureStation: (Boolean) -> Unit,
    changeExpandMenuArrivalStation: (Boolean) -> Unit,
    onDeleteStationName: (String) -> Unit,
    onChangedDropDownContentDepartureStation: (String) -> Unit,
    onChangedDropDownContentArrivalStation: (String) -> Unit,
    onSettingClick: () -> Unit,
    timeZoneText: String,
    bottomSheetState: ModalBottomSheetState,
    bottomSheetContentState: MutableState<BottomSheetRemoveTimeFormPassengerScreen>,
    dateAndTimeConverter: DateAndTimeConverter?
) {
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val dataTextStyle = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
    val errorTextStyle = AppTypography.getType().titleMedium.copy(
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onError
    )

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
            if (errorMessage == null) {
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
        }

        item {
            errorMessage?.let {
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
                            .background(
                                brush = gradient,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            style = errorTextStyle
                        )
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                modifier = Modifier
                    .padding(top = 32.dp)
                    .fillMaxWidth(),
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
            val focusRequester = remember { FocusRequester() }

            ExposedDropdownMenuBox(
                modifier = Modifier
                    .padding(top = 32.dp)
                    .fillMaxWidth(),
                expanded = isExpandedMenuDepartureStation,
                onExpandedChange = { changeExpandMenuDepartureStation(it) }
            ) {
                var stationName by remember {
                    mutableStateOf(
                        TextFieldValue(
                            text = passenger.stationDeparture ?: "",
                            selection = TextRange(passenger.stationDeparture?.length ?: 0)
                        )
                    )
                }

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
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
                if (menuList.isNotEmpty()) {
                    DropdownMenu(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = Shapes.medium
                            )
                            .exposedDropdownSize(true),
                        expanded = isExpandedMenuDepartureStation,
                        properties = PopupProperties(focusable = false),
                        onDismissRequest = { changeExpandMenuDepartureStation(false) }
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
                                    onStationDepartureChanged(selectionStation)
                                    changeExpandMenuDepartureStation(false)
                                    stationName = stationName.copy(
                                        text = selectionStation,
                                        selection = TextRange(selectionStation.length)
                                    )
                                })
                        }
                    }
                }
            }
        }

        item {
            var showDepartureDatePicker by remember {
                mutableStateOf(false)
            }

            val departureTime = Calendar.getInstance().also { calendar ->
                passenger.timeDeparture?.let { millis ->
                    calendar.timeInMillis = millis
                }
            }

            SelectableDateTimePicker(
                titleText = "Отправление",
                isShowPicker = showDepartureDatePicker,
                initDateTime = departureTime.timeInMillis,
                onDoneClick = { localDateTime ->
                    val instant = localDateTime.toInstant(TimeZone.of(timeZoneText))
                    val millis = instant.toEpochMilliseconds()
                    onTimeDepartureChanged(millis)
                    showDepartureDatePicker = false
                },
                onDismiss = {
                    showDepartureDatePicker = false
                },
                onSettingClick = onSettingClick
            )

            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = Shapes.medium
                    )
                    .combinedClickable(
                        onClick = {
                            showDepartureDatePicker = true
                        },
                        onLongClick = {
                            passenger.timeDeparture?.let {
                                scope.launch {
                                    bottomSheetContentState.value =
                                        BottomSheetRemoveTimeFormPassengerScreen.TIME_DEPARTURE
                                    bottomSheetState.show()
                                }
                            }
                        }
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val textDateAndTimeDeparture = passenger.timeDeparture?.let {
                    dateAndTimeConverter?.getDateAndTime(it)
                } ?: "Время отправления"

                Text(text = textDateAndTimeDeparture, style = dataTextStyle)
            }
        }

        item {
            val focusRequester = remember { FocusRequester() }

            ExposedDropdownMenuBox(
                modifier = Modifier
                    .padding(top = 32.dp)
                    .fillMaxWidth(),
                expanded = isExpandedMenuArrivalStation,
                onExpandedChange = { changeExpandMenuArrivalStation(it) }
            ) {
                var stationName by remember {
                    mutableStateOf(
                        TextFieldValue(
                            text = passenger.stationArrival ?: "",
                            selection = TextRange(passenger.stationArrival?.length ?: 0)
                        )
                    )
                }

                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor()
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
                            style = dataTextStyle
                        )
                    },
                    textStyle = dataTextStyle,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
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
                        expanded = isExpandedMenuArrivalStation,
                        properties = PopupProperties(focusable = false),
                        onDismissRequest = { changeExpandMenuArrivalStation(false) }
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
                                    onStationArrivalChanged(selectionStation)
                                    changeExpandMenuArrivalStation(false)
                                    stationName = stationName.copy(
                                        text = selectionStation,
                                        selection = TextRange(selectionStation.length)
                                    )
                                })
                        }
                    }
                }
            }
        }

        item {
            var showArrivalDatePicker by remember {
                mutableStateOf(false)
            }

            val arrivalTime = Calendar.getInstance().also { calendar ->
                passenger.timeArrival?.let { millis ->
                    calendar.timeInMillis = millis
                }
            }

            SelectableDateTimePicker(
                titleText = "Прибытие",
                isShowPicker = showArrivalDatePicker,
                initDateTime = arrivalTime.timeInMillis,
                onDoneClick = { localDateTime ->
                    val instant = localDateTime.toInstant(TimeZone.of(timeZoneText))
                    val millis = instant.toEpochMilliseconds()
                    onTimeArrivalChanged(millis)
                    showArrivalDatePicker = false
                },
                onDismiss = {
                    showArrivalDatePicker = false
                },
                onSettingClick = onSettingClick
            )

            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = Shapes.medium
                    )

                    .combinedClickable(
                        onClick = {
                            showArrivalDatePicker = true
                        },
                        onLongClick = {
                            passenger.timeArrival?.let {
                                scope.launch {
                                    bottomSheetContentState.value =
                                        BottomSheetRemoveTimeFormPassengerScreen.TIME_ARRIVAL
                                    bottomSheetState.show()
                                }
                            }
                        }
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val textDateAndTimeArrival =
                    passenger.timeArrival?.let {
                        dateAndTimeConverter?.getDateAndTime(it)
                    } ?: "Время прибытия"

                Text(text = textDateAndTimeArrival, style = dataTextStyle)
            }
        }

        item {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
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
                keyboardActions = KeyboardActions(
                    onNext = {
                        scope.launch {
                            focusManager.clearFocus()
                        }
                    }
                ),
                shape = Shapes.medium,
            )
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun RemoveTimeBottomSheetContent(
    bottomSheetState: ModalBottomSheetState,
    onTimeDepartureChanged: (Long?) -> Unit,
    onTimeArrivalChanged: (Long?) -> Unit,
    selectBottomSheetContent: BottomSheetRemoveTimeFormPassengerScreen
) {
    val scope = rememberCoroutineScope()
    when (selectBottomSheetContent) {
        BottomSheetRemoveTimeFormPassengerScreen.TIME_DEPARTURE -> {
            RemoveTimeContent(
                title = "Время отправления",
                onRemoveTimeClick = {
                    scope.launch {
                        bottomSheetState.hide()
                    }
                    onTimeDepartureChanged(null)
                }
            )
        }

        BottomSheetRemoveTimeFormPassengerScreen.TIME_ARRIVAL -> {
            RemoveTimeContent(
                title = "Время прибытия",
                onRemoveTimeClick = {
                    scope.launch {
                        bottomSheetState.hide()
                    }
                    onTimeArrivalChanged(null)
                }
            )
        }
    }
}

enum class BottomSheetRemoveTimeFormPassengerScreen() {
    TIME_ARRIVAL, TIME_DEPARTURE
}
