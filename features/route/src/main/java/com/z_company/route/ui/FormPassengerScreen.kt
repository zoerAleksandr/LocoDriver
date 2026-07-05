package com.z_company.route.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.z_company.core.R
import com.z_company.core.ResultState
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.Passenger
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.AppDateTimePicker
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.BottomShadow
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.component.StationDropdownMenu
import com.z_company.route.extention.isScrollInInitialState
import com.z_company.route.viewmodel.PassengerFormUiState
import com.z_company.route.viewmodel.PassengerFormViewModel
import com.z_company.core.util.TimeManager
import kotlinx.coroutines.launch

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
    isWorkStartByArrival: Boolean,
    onWorkStartByArrivalChanged: (Boolean) -> Unit,
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val hintStyle = MaterialTheme.typography.bodyMedium
    val dataTextStyle = MaterialTheme.typography.bodyLarge

    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Общий пикер даты/времени: какой из двух (Откуда/Куда) сейчас открыт.
    var pickerTarget by remember { mutableStateOf(PickerTarget.NONE) }

    Scaffold(
        modifier = Modifier.fillMaxWidth(),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackBarData ->
                com.z_company.core.ui.component.CustomSnackBar(snackBarData = snackBarData)
            }
        },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text(
                        text = "Пассажиром",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onPassengerSaved()
                        },
                    ) {
                        Text(
                            text = "‹",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        if (formUiState.savePassengerState is ResultState.Error) {
            val errorState = formUiState.savePassengerState as ResultState.Error
            LaunchedEffect(Unit) {
                scope.launch {
                    snackbarHostState.showSnackbar("Ошибка: ${errorState.entity.message}")
                }
                resetSaveState()
            }
        }

        var showBottomSheetRemoveTimeDeparture by remember { mutableStateOf(false) }
        if (showBottomSheetRemoveTimeDeparture) {
            AppBottomSheet(
                onDismissRequest = { showBottomSheetRemoveTimeDeparture = false },
                sheetState = sheetState,
                title = "Время отправления",
                actions = listOf(
                    BottomSheetAction(text = "Удалить значение") {
                        onTimeDepartureChanged(null)
                    }
                )
            )
        }

        var showBottomSheetRemoveTimeArrival by remember { mutableStateOf(false) }
        if (showBottomSheetRemoveTimeArrival) {
            AppBottomSheet(
                onDismissRequest = { showBottomSheetRemoveTimeArrival = false },
                sheetState = sheetState,
                title = "Время прибытия",
                actions = listOf(
                    BottomSheetAction(text = "Удалить значение") {
                        onTimeArrivalChanged(null)
                    }
                )
            )
        }

        AnimatedVisibility(
            modifier = Modifier.zIndex(1f),
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
                    modifier = Modifier.testTag("form_passenger_lazy_column"),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp, top = 8.dp, end = 16.dp, bottom = 32.dp
                    )
                ) {
                    // ── Ошибка (в едином амбер-стиле) ──
                    item {
                        formUiState.errorMessage?.let { PassInfoBanner(it) }
                    }

                    // ── Предупреждение: следование (или его часть) вне окна смены не оплачивается ──
                    item {
                        val dep = passenger.timeDeparture
                        val arr = passenger.timeArrival
                        val ws = formUiState.routeWorkStart
                        val we = formUiState.routeWorkEnd
                        // Предупреждаем, как только ЛЮБОЕ заданное значение (отправление или
                        // прибытие) выходит за окно смены [явка, сдача] — даже если вторая точка
                        // ещё не введена. Исключение: если обе точки заданы и прибытие не позже
                        // отправления — это невалидные данные, показывается красная ошибка, и
                        // второй баннер не нужен.
                        val warningText: String? = if (
                            !isWorkStartByArrival && ws != null && we != null &&
                            !(dep != null && arr != null && arr <= dep)
                        ) {
                            // Время, затем дата.
                            val startTxt = dateAndTimeConverter?.let {
                                "${it.getTime(ws)} ${it.getDate(ws)}"
                            } ?: ""
                            val endTxt = dateAndTimeConverter?.let {
                                "${it.getTime(we)} ${it.getDate(we)}"
                            } ?: ""
                            when {
                                dep != null && dep < ws ->
                                    "Отправление пассажиром раньше явки в $startTxt — не входит в оплату."
                                arr != null && arr > we ->
                                    "Прибытие пассажиром позже сдачи в $endTxt — не входит в оплату."
                                dep != null && dep > we ->
                                    "Отправление пассажиром позже сдачи в $endTxt — не входит в оплату."
                                arr != null && arr < ws ->
                                    "Прибытие пассажиром раньше явки в $startTxt — не входит в оплату."
                                else -> null
                            }
                        } else null

                        if (warningText != null) {
                            PassInfoBanner(warningText)
                        }
                    }

                    // ── ПОЕЗД ──
                    item {
                        PassGroup(label = "ПОЕЗД") {
                            PassFlatCard {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceBright,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            modifier = Modifier.size(18.dp),
                                            painter = painterResource(com.z_company.route.R.drawable.ic_card_train_ref),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "№",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedTextFieldApp(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = passenger.trainNumber ?: "",
                                        onValueChange = { onNumberChanged(it) },
                                        placeholder = {
                                            Text(
                                                text = "поезда",
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                                                style = LocalTextStyle.current.copy(
                                                    fontFamily = MonoFont,
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Light
                                                )
                                            )
                                        },
                                        textStyle = dataTextStyle.copy(
                                            fontFamily = MonoFont,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal,
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = {
                                                scope.launch { focusManager.moveFocus(FocusDirection.Down) }
                                            }
                                        ),
                                        singleLine = true,
                                        fieldElevation = 0.dp,
                                        colorBackgroundEmptyField = Color.Transparent,
                                        colorBackgroundNotEmptyField = Color.Transparent,
                                    )
                                }
                            }
                        }
                    }

                    // ── ОТКУДА ──
                    item {
                        StationLegCard(
                            label = "ОТКУДА",
                            stationPlaceholder = "От станции",
                            initialStation = passenger.stationDeparture ?: "",
                            time = passenger.timeDeparture,
                            dropDownMenuList = dropDownMenuList,
                            onStationChanged = onStationDepartureChanged,
                            onChangedDropDownContent = onChangedDropDownContentDepartureStation,
                            onDeleteStationName = onDeleteStationName,
                            onOpenPicker = {
                                pickerTarget = PickerTarget.DEPARTURE
                            },
                            onRemoveTime = { showBottomSheetRemoveTimeDeparture = true },
                            dateAndTimeConverter = dateAndTimeConverter,
                            dataTextStyle = dataTextStyle,
                            focusManager = focusManager,
                            scope = scope
                        )
                    }

                    // ── В пути ──
                    item {
                        AnimatedVisibility(
                            visible = errorMessage == null && resultTime != null && resultTime > 0,
                            enter = fadeIn(tween(250)),
                            exit = fadeOut(tween(150))
                        ) {
                            DurationBadge(label = "В пути · ${formatTravelDuration(resultTime)}")
                        }
                    }

                    // ── КУДА ──
                    item {
                        StationLegCard(
                            label = "КУДА",
                            stationPlaceholder = "До станции",
                            initialStation = passenger.stationArrival ?: "",
                            time = passenger.timeArrival,
                            dropDownMenuList = dropDownMenuList,
                            onStationChanged = onStationArrivalChanged,
                            onChangedDropDownContent = onChangedDropDownContentArrivalStation,
                            onDeleteStationName = onDeleteStationName,
                            onOpenPicker = {
                                pickerTarget = PickerTarget.ARRIVAL
                            },
                            onRemoveTime = { showBottomSheetRemoveTimeArrival = true },
                            dateAndTimeConverter = dateAndTimeConverter,
                            dataTextStyle = dataTextStyle,
                            focusManager = focusManager,
                            scope = scope
                        )
                    }

                    // ── УЧЁТ РАБОЧЕГО ВРЕМЕНИ ──
                    item {
                        WorkStartCard(
                            checked = isWorkStartByArrival,
                            onCheckedChange = onWorkStartByArrivalChanged,
                            station = passenger.stationArrival,
                            time = passenger.timeArrival,
                            dateAndTimeConverter = dateAndTimeConverter
                        )
                    }

                    // ── ПРИМЕЧАНИЯ ──
                    item {
                        PassGroup(label = "ПРИМЕЧАНИЯ") {
                            PassFlatCard {
                                OutlinedTextFieldApp(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 64.dp)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    value = passenger.notes ?: "",
                                    onValueChange = { onNotesChanged(it) },
                                    textStyle = dataTextStyle,
                                    placeholder = {
                                        Text(
                                            text = "Например: бригада №2, вагон 7, место 32",
                                            style = LocalTextStyle.current.copy(
                                                fontWeight = FontWeight.Normal
                                            ),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                                        )
                                    },
                                    minLines = 2,
                                    fieldElevation = 0.dp,
                                    colorBackgroundEmptyField = Color.Transparent,
                                    colorBackgroundNotEmptyField = Color.Transparent,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Пикер даты/времени (общий для «Откуда» и «Куда») ──
        when (pickerTarget) {
            PickerTarget.DEPARTURE -> {
                AppDateTimePicker(
                    title = "Отправление",
                    onDateTimeSelected = { timestamp -> onTimeDepartureChanged(timestamp) },
                    onDismiss = { pickerTarget = PickerTarget.NONE },
                    startDateTime = currentPassenger?.timeDeparture ?: TimeManager().now(),
                    timeZoneStr = displayTz,
                )
            }

            PickerTarget.ARRIVAL -> {
                AppDateTimePicker(
                    title = "Прибытие",
                    onDateTimeSelected = { timestamp -> onTimeArrivalChanged(timestamp) },
                    onDismiss = { pickerTarget = PickerTarget.NONE },
                    startDateTime = currentPassenger?.timeArrival ?: TimeManager().now(),
                    timeZoneStr = displayTz,
                )
            }

            PickerTarget.NONE -> Unit
        }
    }
}

/** Какой пикер даты/времени сейчас открыт (общий для двух карточек). */
private enum class PickerTarget { NONE, DEPARTURE, ARRIVAL }

/** Форматирует длительность в пути (мс) в «7 ч 40 мин». */
private fun formatTravelDuration(millis: Long?): String {
    val total = ((millis ?: 0L) / 60_000L).toInt()
    val h = total / 60
    val m = total % 60
    return when {
        h > 0 && m > 0 -> "$h ч $m мин"
        h > 0 -> "$h ч"
        else -> "$m мин"
    }
}

/** Группа полей: верхний caps-лейбл + содержимое. */
@Composable
private fun PassGroup(
    label: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = MonoFont,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/** Белая карточка с мягкой тенью (как в редизайне остальных форм). */
@Composable
private fun PassFlatCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = Shapes.medium)
            .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
    ) {
        content()
    }
}

/** Единый амбер-баннер для всех сообщений на экране (и ошибок, и предупреждений). */
@Composable
private fun PassInfoBanner(text: String) {
    val warning = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.medium)
            .background(warning.copy(alpha = 0.10f))
            .border(
                width = 1.dp,
                color = warning.copy(alpha = 0.55f),
                shape = Shapes.medium
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(warning.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(15.dp),
                painter = painterResource(com.z_company.route.R.drawable.info_24px),
                contentDescription = null,
                tint = warning
            )
        }
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** Тапабельная пилюля «иконка + значение + ›» — для даты и времени. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PickerChip(
    modifier: Modifier = Modifier,
    iconRes: Int,
    value: String,
    isPlaceholder: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceBright, RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            modifier = Modifier.weight(1f),
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = MonoFont,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = if (isPlaceholder)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
            else MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            modifier = Modifier.size(14.dp),
            painter = painterResource(R.drawable.keyboard_arrow_right_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
        )
    }
}

/** Карточка «Откуда / Куда» — станция + дата + время. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun StationLegCard(
    label: String,
    stationPlaceholder: String,
    initialStation: String,
    time: Long?,
    dropDownMenuList: List<String>,
    onStationChanged: (String) -> Unit,
    onChangedDropDownContent: (String) -> Unit,
    onDeleteStationName: (String) -> Unit,
    onOpenPicker: () -> Unit,
    onRemoveTime: () -> Unit,
    dateAndTimeConverter: DateAndTimeConverter?,
    dataTextStyle: androidx.compose.ui.text.TextStyle,
    focusManager: androidx.compose.ui.focus.FocusManager,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val focusRequester = remember { FocusRequester() }
    var stationName by remember {
        mutableStateOf(
            TextFieldValue(text = initialStation, selection = TextRange(initialStation.length))
        )
    }
    // Раскрытие списка станций управляется локально по введённому тексту —
    // как в поле станции в шторке FormTrainScreen (StationEditBottomSheet).
    var isDropdownExpanded by remember { mutableStateOf(false) }

    PassGroup(label = label) {
        PassFlatCard {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Станция
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            painter = painterResource(com.z_company.route.R.drawable.location_on_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    ExposedDropdownMenuBox(
                        modifier = Modifier.weight(1f),
                        expanded = isDropdownExpanded,
                        onExpandedChange = { isDropdownExpanded = it }
                    ) {
                        OutlinedTextFieldApp(
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                                .focusRequester(focusRequester),
                            value = stationName,
                            onValueChange = {
                                stationName = it
                                onChangedDropDownContent(it.text)
                                onStationChanged(it.text)
                                isDropdownExpanded = it.text.isNotEmpty()
                            },
                            textStyle = dataTextStyle.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            placeholder = {
                                Text(
                                    text = stationPlaceholder,
                                    style = LocalTextStyle.current.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    isDropdownExpanded = false
                                    scope.launch { focusManager.moveFocus(FocusDirection.Down) }
                                }
                            ),
                            singleLine = true,
                            fieldElevation = 0.dp,
                            colorBackgroundEmptyField = Color.Transparent,
                            colorBackgroundNotEmptyField = Color.Transparent,
                        )

                        StationDropdownMenu(
                            expanded = isDropdownExpanded,
                            stations = dropDownMenuList,
                            onSelect = { selectionStation ->
                                onStationChanged(selectionStation)
                                isDropdownExpanded = false
                                stationName = stationName.copy(
                                    text = selectionStation,
                                    selection = TextRange(selectionStation.length)
                                )
                            },
                            onDelete = onDeleteStationName,
                            onDismiss = { isDropdownExpanded = false },
                            textStyle = dataTextStyle
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // Дата + Время
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val hasValue = time != null
                    PickerChip(
                        modifier = Modifier.weight(1.4f),
                        iconRes = com.z_company.route.R.drawable.calendar_today_24px,
                        value = if (hasValue) dateAndTimeConverter?.getDate(time) ?: "" else "Дата",
                        isPlaceholder = !hasValue,
                        onClick = onOpenPicker,
                        onLongClick = { if (hasValue) onRemoveTime() }
                    )
                    PickerChip(
                        modifier = Modifier.weight(1f),
                        iconRes = com.z_company.route.R.drawable.schedule_24px,
                        value = if (hasValue) dateAndTimeConverter?.getTime(time) ?: "" else "Время",
                        isPlaceholder = !hasValue,
                        onClick = onOpenPicker,
                        onLongClick = { if (hasValue) onRemoveTime() }
                    )
                }
            }
        }
    }
}

/** Бейдж длительности «В пути · 7 ч 40 мин» — между двумя StationLegCard. */
@Composable
private fun DurationBadge(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                modifier = Modifier.size(13.dp),
                painter = painterResource(com.z_company.route.R.drawable.schedule_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = MonoFont,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp
                ),
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

/**
 * Карточка «Учёт рабочего времени» → «Явка по прибытию».
 * Когда включено — прибытие пассажиром (станция + время «Куда») становится
 * началом рабочего времени маршрута. Переключатель доступен только если задано
 * время прибытия — иначе точку отсчёта вычислить нельзя.
 */
@Composable
private fun WorkStartCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    station: String?,
    time: Long?,
    dateAndTimeConverter: DateAndTimeConverter?
) {
    val canEnable = time != null

    PassGroup(label = "УЧЁТ РАБОЧЕГО ВРЕМЕНИ") {
        PassFlatCard {
            Column {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Явка по прибытию",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            modifier = Modifier.padding(top = 3.dp),
                            text = if (canEnable)
                                "Явка на работу по прибытию пассажиром."
                            else
                                "Укажите время прибытия («Куда»), чтобы включить.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = checked,
                        onCheckedChange = onCheckedChange,
                        enabled = canEnable,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.tertiary,
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                AnimatedVisibility(visible = checked && canEnable) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp)
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    modifier = Modifier.size(17.dp),
                                    painter = painterResource(com.z_company.route.R.drawable.schedule_24px),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "НАЧАЛО РАБОТЫ",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = MonoFont,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                val timeText = time?.let { dateAndTimeConverter?.getDateAndTime(it) }
                                Text(
                                    modifier = Modifier.padding(top = 3.dp),
                                    text = when {
                                        timeText == null -> "—"
                                        !station.isNullOrBlank() -> "$station · $timeText"
                                        else -> timeText
                                    },
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = MonoFont,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
