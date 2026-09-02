package com.z_company.route.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.z_company.route.component.AppDateTimePicker
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.DateAndTimeFormat
import com.z_company.core.util.TimeManager
import com.z_company.route.viewmodel.StationFormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationEditBottomSheet(
    stationFormState: StationFormState?,
    menuList: List<String>,
    onFilterMenu: (String) -> Unit,
    onDeleteStationName: (String) -> Unit,
    trainWeight: String?,
    trainAxle: String?,
    trainConditionalLength: String?,
    onTrainDataChange: ((stationName: String?, weight: String, axle: String, conditionalLength: String) -> Unit)?,
    // Первая станция маршрута: поезд с неё отправляется, поэтому «конечной» и
    // «проходной» она быть не может — флажки не показываем.
    isFirstStation: Boolean = false,
    onSave: (
        name: String?,
        arrival: Long?,
        departure: Long?,
        trackNumber: String?,
        isFinalStation: Boolean,
        isPassingStation: Boolean,
    ) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    dateAndTimeConverter: DateAndTimeConverter?,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current
    val displayTz = dateAndTimeConverter?.timeZoneText ?: "GMT+3"

    val isNewStation = stationFormState == null
    val title = if (isNewStation) "Новая станция" else "Редактировать станцию"

    var localName by remember {
        mutableStateOf(
            TextFieldValue(
                text = stationFormState?.station?.data ?: "",
                selection = TextRange(stationFormState?.station?.data?.length ?: 0)
            )
        )
    }
    var localTrackNumber by remember {
        mutableStateOf(
            TextFieldValue(
                text = stationFormState?.trackNumber ?: "",
                selection = TextRange(stationFormState?.trackNumber?.length ?: 0)
            )
        )
    }
    var localArrival by remember { mutableStateOf(stationFormState?.arrival?.data) }
    var localDeparture by remember { mutableStateOf(stationFormState?.departure?.data) }
    var localIsFinal by remember { mutableStateOf(stationFormState?.isFinalStation ?: false) }
    var localIsPassing by remember { mutableStateOf(stationFormState?.isPassingStation ?: false) }
    // Клавиатура номера пути: по умолчанию числовая, с переключением на буквенную
    // (есть номера путей вида «3Г»).
    var trackKeyboardNumeric by remember { mutableStateOf(true) }

    var showArrivalPicker by remember { mutableStateOf(false) }
    var showDeparturePicker by remember { mutableStateOf(false) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var showTrainDataEditor by remember { mutableStateOf(false) }
    var localWeight by remember(trainWeight) { mutableStateOf(trainWeight ?: "") }
    var localAxle by remember(trainAxle) { mutableStateOf(trainAxle ?: "") }
    var localConditionalLength by remember(trainConditionalLength) {
        mutableStateOf(trainConditionalLength.orEmpty().withoutZeroFraction())
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val hintColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val dataTextStyle = MaterialTheme.typography.bodyLarge
    val hintStyle = MaterialTheme.typography.bodyMedium
    val fieldColor = MaterialTheme.colorScheme.surfaceBright
    val fieldShape = RoundedCornerShape(14.dp)

    val saveAndDismiss: () -> Unit = {
        val allBlank = localName.text.isBlank() &&
                localTrackNumber.text.isBlank() &&
                localArrival == null &&
                localDeparture == null &&
                (isFirstStation || (!localIsFinal && !localIsPassing))
        if (isNewStation && allBlank) {
            onDismiss()
        } else {
            val name = localName.text.ifBlank { null }
            val track = localTrackNumber.text.ifBlank { null }
            // Проходная станция — только время проследования (localArrival);
            // отправление не используется.
            val isFinal = localIsFinal && !isFirstStation
            val isPassing = localIsPassing && !isFirstStation
            val departure = if (isPassing) null else localDeparture
            onSave(name, localArrival, departure, track, isFinal, isPassing)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = saveAndDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.secondary,
        shape = Shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            // ── Заголовок + кнопка закрытия ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { saveAndDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(com.z_company.core.R.drawable.ic_clear),
                        contentDescription = "Закрыть",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Название станции + Путь (горизонтальный ряд) ──
            // Лейбл — ВНУТРИ серого поля (сверху), значение — под ним. По референсу.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // ── Название станции ──
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                            .clip(fieldShape)
                            .background(fieldColor)
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        FieldLabel("Название", primaryColor)
                        BasicTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = localName,
                            onValueChange = { newValue ->
                                localName = newValue
                                onFilterMenu(newValue.text)
                                isDropdownExpanded = newValue.text.isNotEmpty()
                            },
                            textStyle = dataTextStyle.copy(
                                fontWeight = FontWeight.Medium,
                                color = primaryColor
                            ),
                            cursorBrush = SolidColor(primaryColor),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    isDropdownExpanded = false
                                }
                            ),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (localName.text.isEmpty()) {
                                        Text(text = "Станция", style = hintStyle, color = hintColor)
                                    }
                                    inner()
                                }
                            }
                        )
                    }

                    StationDropdownMenu(
                        expanded = isDropdownExpanded,
                        stations = menuList,
                        onSelect = { stationName ->
                            localName = localName.copy(
                                text = stationName,
                                selection = TextRange(stationName.length)
                            )
                            isDropdownExpanded = false
                        },
                        onDelete = onDeleteStationName,
                        onDismiss = { isDropdownExpanded = false },
                        textStyle = dataTextStyle
                    )
                }

                // ── Поле «Путь» (4 символа) ──
                Column(
                    modifier = Modifier
                        .width(92.dp)
                        .clip(fieldShape)
                        .background(fieldColor)
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    FieldLabel("Путь", primaryColor)
                    BasicTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = localTrackNumber,
                        onValueChange = { newValue ->
                            if (newValue.text.length <= 4) {
                                localTrackNumber = newValue
                            }
                        },
                        // Номер пути — идентификатор → Mono.
                        textStyle = dataTextStyle.copy(
                            fontWeight = FontWeight.Medium,
                            fontFamily = com.z_company.core.ui.theme.MonoFont,
                            color = primaryColor
                        ),
                        cursorBrush = SolidColor(primaryColor),
                        singleLine = true,
                        // Текстовая клавиатура: путь бывает буквенным («3Г»).
                        keyboardOptions = KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (localTrackNumber.text.isEmpty()) {
                                    Text(text = "№", style = hintStyle, color = hintColor)
                                }
                                inner()
                            }
                        }
                    )
                }
            }

            // ── Флажки «Конечная станция» / «Проходная станция» ──
            // У первой станции их нет: поезд с неё отправляется.
            if (!isFirstStation) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StationFlagCheckbox(
                        label = "Конечная",
                        checked = localIsFinal,
                        onCheckedChange = { localIsFinal = it },
                        modifier = Modifier.weight(1f)
                    )
                    // Время отправления НЕ стираем при включении: пользователь может
                    // передумать и выключить флаг обратно. Отправление отбрасывается
                    // только при сохранении (см. saveAndDismiss).
                    StationFlagCheckbox(
                        label = "Проходная",
                        checked = localIsPassing,
                        onCheckedChange = { localIsPassing = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            if (localIsPassing) {
                // ── Проходная станция: только время проследования ──
                TimeBlock(
                    label = "Проследование",
                    timeMillis = localArrival,
                    dateAndTimeConverter = dateAndTimeConverter,
                    onFieldClick = { showArrivalPicker = true },
                    onClear = { localArrival = null },
                    onAdjust = { delta ->
                        val base = localArrival ?: nowTruncatedToMinutes()
                        localArrival = base + delta * 60_000L
                    },
                    onNow = { localArrival = nowTruncatedToMinutes() },
                )
            } else {
                // ── Прибытие ──
                TimeBlock(
                    label = "Прибытие",
                    timeMillis = localArrival,
                    dateAndTimeConverter = dateAndTimeConverter,
                    onFieldClick = { showArrivalPicker = true },
                    onClear = { localArrival = null },
                    onAdjust = { delta ->
                        val base = localArrival ?: nowTruncatedToMinutes()
                        localArrival = base + delta * 60_000L
                    },
                    onNow = { localArrival = nowTruncatedToMinutes() },
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Бейдж стоянки (между прибытием и отправлением) ──
                // У конечной станции стоянку не показываем — маршрут закончился.
                val stopMinutes = if (!localIsFinal && localArrival != null && localDeparture != null) {
                    val arr = localArrival!! - localArrival!! % 60_000L
                    val dep = localDeparture!! - localDeparture!! % 60_000L
                    val diff = dep - arr
                    if (diff > 0) (diff / 60_000).toInt() else null
                } else null

                if (stopMinutes != null && stopMinutes > 0) {
                    StopDurationDivider(stopMinutes = stopMinutes)
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── Отправление ──
                TimeBlock(
                    label = "Отправление",
                    timeMillis = localDeparture,
                    dateAndTimeConverter = dateAndTimeConverter,
                    onFieldClick = { showDeparturePicker = true },
                    onClear = { localDeparture = null },
                    onAdjust = { delta ->
                        val base = localDeparture ?: nowTruncatedToMinutes()
                        localDeparture = base + delta * 60_000L
                    },
                    onNow = { localDeparture = nowTruncatedToMinutes() },
                )
            }

            if (onTrainDataChange != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = Shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                        contentColor = MaterialTheme.colorScheme.tertiary
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                    onClick = { showTrainDataEditor = !showTrainDataEditor }
                ) {
                    Text(
                        text = if (showTrainDataEditor) "Скрыть данные поезда" else "Изменить данные поезда",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (showTrainDataEditor) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TrainDataInput("Вес, т", localWeight, Modifier.weight(1f)) { localWeight = it }
                        TrainDataInput("Оси", localAxle, Modifier.weight(1f)) { localAxle = it }
                        TrainDataInput("У.Д.", localConditionalLength, Modifier.weight(1f)) {
                            localConditionalLength = it
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = Shapes.medium,
                        onClick = {
                            onTrainDataChange(
                                localName.text.ifBlank { null },
                                localWeight,
                                localAxle,
                                localConditionalLength
                            )
                            showTrainDataEditor = false
                        }
                    ) {
                        Text("Сохранить новые данные", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── Разделитель ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
                    .height(1.dp)
                    .background(primaryColor.copy(alpha = 0.1f))
            )

            // ── Кнопка «Готово» ──
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = Shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                onClick = saveAndDismiss
            ) {
                Text(
                    text = "Готово",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // ── Кнопка «Удалить станцию» ──
            if (onDelete != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    ),
                    onClick = onDelete
                ) {
                    Text(
                        text = "Удалить станцию",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }

    // Arrival DateTimePicker
    if (showArrivalPicker) {
        AppDateTimePicker(
            title = "Прибытие",
            onDateTimeSelected = { time ->
                localArrival = time
                showArrivalPicker = false
            },
            onDismiss = { showArrivalPicker = false },
            startDateTime = localArrival ?: nowTruncatedToMinutes(),
            timeZoneStr = displayTz
        )
    }

    // Departure DateTimePicker
    if (showDeparturePicker) {
        AppDateTimePicker(
            title = "Отправление",
            onDateTimeSelected = { time ->
                localDeparture = time
                showDeparturePicker = false
            },
            onDismiss = { showDeparturePicker = false },
            startDateTime = localDeparture ?: nowTruncatedToMinutes(),
            timeZoneStr = displayTz
        )
    }
}

// ─── Шторка редактирования перегона (между двумя станциями) ─────────────────
//
// Открывается кликом по «времени в пути» между station[i] и station[i+1].
// Названия станций предзаполнены соседними станциями, но остаются
// редактируемыми (правка здесь меняет сами station[i]/station[i+1]).
// Путь и примечание относятся к перегону (хранятся на station[i+1]).

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentEditBottomSheet(
    fromStationName: String?,
    toStationName: String?,
    trackNumber: String?,
    notes: String?,
    menuList: List<String>,
    onFilterMenu: (String) -> Unit,
    onDeleteStationName: (String) -> Unit,
    onSave: (fromName: String?, toName: String?, trackNumber: String?, notes: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    var localFrom by remember { mutableStateOf(TextFieldValue(fromStationName ?: "")) }
    var localTo by remember { mutableStateOf(TextFieldValue(toStationName ?: "")) }
    var localTrack by remember { mutableStateOf(TextFieldValue(trackNumber ?: "")) }
    var localNotes by remember { mutableStateOf(TextFieldValue(notes ?: "")) }
    var expandedField by remember { mutableStateOf<String?>(null) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val hintColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val dataTextStyle = MaterialTheme.typography.bodyLarge
    val hintStyle = MaterialTheme.typography.bodyMedium
    val fieldColor = MaterialTheme.colorScheme.surfaceBright
    val fieldShape = RoundedCornerShape(14.dp)

    val saveAndDismiss: () -> Unit = {
        onSave(
            localFrom.text.ifBlank { null },
            localTo.text.ifBlank { null },
            localTrack.text.ifBlank { null },
            localNotes.text.ifBlank { null },
        )
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = saveAndDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.secondary,
        shape = Shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Перегон",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { saveAndDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(com.z_company.core.R.drawable.ic_clear),
                        contentDescription = "Закрыть",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── От — До (предзаполнены соседними станциями, редактируемы) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SegmentStationField(
                    label = "От",
                    value = localFrom,
                    onValueChange = { localFrom = it },
                    expanded = expandedField == "from",
                    onExpandedChange = { expandedField = if (it) "from" else null },
                    menuList = menuList,
                    onFilterMenu = onFilterMenu,
                    onDeleteStationName = onDeleteStationName,
                    modifier = Modifier.weight(1f),
                    fieldColor = fieldColor,
                    fieldShape = fieldShape,
                    primaryColor = primaryColor,
                    hintColor = hintColor,
                    dataTextStyle = dataTextStyle,
                    hintStyle = hintStyle,
                )
                SegmentStationField(
                    label = "До",
                    value = localTo,
                    onValueChange = { localTo = it },
                    expanded = expandedField == "to",
                    onExpandedChange = { expandedField = if (it) "to" else null },
                    menuList = menuList,
                    onFilterMenu = onFilterMenu,
                    onDeleteStationName = onDeleteStationName,
                    modifier = Modifier.weight(1f),
                    fieldColor = fieldColor,
                    fieldShape = fieldShape,
                    primaryColor = primaryColor,
                    hintColor = hintColor,
                    dataTextStyle = dataTextStyle,
                    hintStyle = hintStyle,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Путь на перегоне ──
            // Ширина делится поровну между полем ввода и тремя кнопками
            // римских номеров: на перегоне это самые частые значения.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(fieldShape)
                        .background(fieldColor)
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    FieldLabel("Путь", primaryColor)
                    BasicTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = localTrack,
                        onValueChange = { if (it.text.length <= 4) localTrack = it },
                        textStyle = dataTextStyle.copy(
                            fontWeight = FontWeight.Medium,
                            fontFamily = com.z_company.core.ui.theme.MonoFont,
                            color = primaryColor
                        ),
                        cursorBrush = SolidColor(primaryColor),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (localTrack.text.isEmpty()) {
                                    Text(text = "№", style = hintStyle, color = hintColor)
                                }
                                inner()
                            }
                        }
                    )
                }
                listOf("I", "II", "III").forEach { numeral ->
                    RomanTrackButton(
                        numeral = numeral,
                        selected = localTrack.text == numeral,
                        onClick = {
                            localTrack = TextFieldValue(
                                text = numeral,
                                selection = TextRange(numeral.length)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Примечание ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(fieldShape)
                    .background(fieldColor)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                FieldLabel("Примечание", primaryColor)
                BasicTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = localNotes,
                    onValueChange = { localNotes = it },
                    textStyle = dataTextStyle.copy(fontWeight = FontWeight.Medium, color = primaryColor),
                    cursorBrush = SolidColor(primaryColor),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (localNotes.text.isEmpty()) {
                                Text(text = "Например: по неправильному", style = hintStyle, color = hintColor)
                            }
                            inner()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = Shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                onClick = saveAndDismiss
            ) {
                Text(
                    text = "Готово",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentStationField(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    menuList: List<String>,
    onFilterMenu: (String) -> Unit,
    onDeleteStationName: (String) -> Unit,
    modifier: Modifier,
    fieldColor: Color,
    fieldShape: RoundedCornerShape,
    primaryColor: Color,
    hintColor: Color,
    dataTextStyle: androidx.compose.ui.text.TextStyle,
    hintStyle: androidx.compose.ui.text.TextStyle,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                .clip(fieldShape)
                .background(fieldColor)
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            FieldLabel(label, primaryColor)
            BasicTextField(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                onValueChange = { newValue ->
                    onValueChange(newValue)
                    onFilterMenu(newValue.text)
                    onExpandedChange(newValue.text.isNotEmpty())
                },
                textStyle = dataTextStyle.copy(fontWeight = FontWeight.Medium, color = primaryColor),
                cursorBrush = SolidColor(primaryColor),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.text.isEmpty()) {
                            Text(text = "Станция", style = hintStyle, color = hintColor)
                        }
                        inner()
                    }
                }
            )
        }

        StationDropdownMenu(
            expanded = expanded,
            stations = menuList,
            onSelect = { stationName ->
                onValueChange(
                    value.copy(text = stationName, selection = TextRange(stationName.length))
                )
                onExpandedChange(false)
            },
            onDelete = onDeleteStationName,
            onDismiss = { onExpandedChange(false) },
            textStyle = dataTextStyle
        )
    }
}

private fun String.withoutZeroFraction(): String {
    val separatorIndex = indexOfLast { it == '.' || it == ',' }
    if (separatorIndex < 0) return this
    return if (substring(separatorIndex + 1).isNotEmpty() &&
        substring(separatorIndex + 1).all { it == '0' }
    ) substring(0, separatorIndex) else this
}

@Composable
private fun TrainDataInput(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceBright)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BasicTextField(
            value = value,
            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' || char == ',' }) onValueChange(it) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.primary,
                fontFamily = com.z_company.core.ui.theme.MonoFont,
                fontWeight = FontWeight.SemiBold
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
            singleLine = true,
            decorationBox = { inner ->
                Box {
                    if (value.isBlank()) Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                }
            }
        )
    }
}

// ─── Блок времени (лейбл + крупное время + кнопки ±) ─────────────────────────

@Composable
private fun TimeBlock(
    label: String,
    timeMillis: Long?,
    dateAndTimeConverter: DateAndTimeConverter?,
    onFieldClick: () -> Unit,
    onClear: () -> Unit,
    onAdjust: (Int) -> Unit,
    onNow: () -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val fieldColor = MaterialTheme.colorScheme.surfaceBright
    val fieldShape = RoundedCornerShape(14.dp)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Лейбл + дата
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (timeMillis != null) {
                val dateText = formatDateShort(timeMillis)

                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = primaryColor.copy(alpha = 0.4f)
                )
            }
        }

        // Поле с крупным временем
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = fieldColor, shape = fieldShape)
                .clickable { onFieldClick() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,

        ) {
            // Крупное время по центру
            val timeText = if (timeMillis != null) {
                dateAndTimeConverter?.getTimeFromDateLong(timeMillis)
                    ?: DateAndTimeFormat.DEFAULT_TIME_TEXT
            } else {
                DateAndTimeFormat.DEFAULT_TIME_TEXT
            }

            Text(
                text = timeText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = com.z_company.core.ui.theme.MonoFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                ),
                color = if (timeMillis != null) primaryColor else primaryColor.copy(alpha = 0.25f),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )

            // Кнопка очистки (Box всегда 34dp для стабильного layout)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .then(
                        if (timeMillis != null) Modifier
                            .clip(CircleShape)
                            .clickable { onClear() }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (timeMillis != null) {
                    Icon(
                        painter = painterResource(com.z_company.core.R.drawable.ic_clear),
                        contentDescription = "Очистить",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 5 кнопок: -5, -1, Сейчас, +1, +5
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // -5
            StepButton(
                label = "-5",
                modifier = Modifier.weight(1f),
                onClick = { onAdjust(-5) }
            )
            // -1
            StepButton(
                label = "-1",
                modifier = Modifier.weight(1f),
                onClick = { onAdjust(-1) }
            )
            // Сейчас
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .background(
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onNow() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Сейчас",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            // +1
            StepButton(
                label = "+1",
                modifier = Modifier.weight(1f),
                onClick = { onAdjust(1) }
            )
            // +5
            StepButton(
                label = "+5",
                modifier = Modifier.weight(1f),
                onClick = { onAdjust(5) }
            )
        }
    }
}

// ─── Кнопка ±N ───────────────────────────────────────────────────────────────

@Composable
private fun StepButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val fieldColor = MaterialTheme.colorScheme.surfaceBright
    Box(
        modifier = modifier
            .background(
                color = fieldColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = com.z_company.core.ui.theme.MonoFont
            ),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
    }
}

// ─── Флажок станции (Конечная / Проходная) ───────────────────────────────────

@Composable
private fun StationFlagCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ─── Кнопка римского номера пути (шторка перегона) ───────────────────────────

@Composable
private fun RomanTrackButton(
    numeral: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val container = if (selected) {
        primaryColor.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surfaceBright
    }
    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = numeral,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = com.z_company.core.ui.theme.MonoFont
            ),
            fontWeight = FontWeight.SemiBold,
            color = primaryColor,
        )
    }
}

// ─── Лейбл внутри поля (Название / Путь) ─────────────────────────────────────

@Composable
private fun FieldLabel(text: String, primaryColor: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        ),
        color = primaryColor.copy(alpha = 0.5f),
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

// ─── Бейдж стоянки между прибытием и отправлением ────────────────────────────

@Composable
private fun StopDurationDivider(stopMinutes: Int) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(primaryColor.copy(alpha = 0.1f))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .background(
                    color = Color(0xFFFFC107),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Стоянка $stopMinutes мин",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4E342E)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(primaryColor.copy(alpha = 0.1f))
        )
    }
}

// ─── Утилиты ────────────────────────────────────────────────────────────────

private val timeManager = TimeManager()

private fun formatDateShort(millis: Long): String = timeManager.formatDate(millis)

private fun nowTruncatedToMinutes(): Long = timeManager.now()
