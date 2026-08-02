package com.z_company.route.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.z_company.core.R
import com.z_company.core.ResultState
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.TimeManager
import com.z_company.domain.entities.route.OtherWork
import com.z_company.route.component.AppAlertDialog
import com.z_company.route.component.AppInputBottomSheet
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.AppDateTimePicker
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.BottomShadow
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.component.StationDropdownMenu
import com.z_company.route.extention.isScrollInInitialState
import com.z_company.route.viewmodel.OtherWorkFormUiState
import com.z_company.route.viewmodel.OtherWorkFormViewModel
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun FormOtherWorkScreen(
    viewModel: OtherWorkFormViewModel,
    formUiState: OtherWorkFormUiState,
    currentOtherWork: OtherWork?,
    onExit: () -> Unit,
    resetSaveState: () -> Unit,
    onWorkTypeChanged: (String) -> Unit,
    onAddCustomType: (String) -> Unit,
    onDeleteCustomType: (String) -> Unit,
    onStationChanged: (String) -> Unit,
    onTimeStartChanged: (Long?) -> Unit,
    onTimeEndChanged: (Long?) -> Unit,
    onApplyTimeFromWork: () -> Unit,
    onApplyTimeFromLoco: () -> Unit,
    locoPicker: List<com.z_company.domain.entities.route.Locomotive>?,
    onPickLoco: (com.z_company.domain.entities.route.Locomotive) -> Unit,
    onDismissLocoPicker: () -> Unit,
    messageFlow: kotlinx.coroutines.flow.Flow<String>,
    onNotesChanged: (String) -> Unit,
    workTypeList: List<String>,
    stationDropDownList: List<String>,
    onChangedStationContent: (String) -> Unit,
    onDeleteStationName: (String) -> Unit,
    resultTime: Long?,
    errorMessage: String?,
    dateAndTimeConverter: DateAndTimeConverter?
) {
    val displayTz = dateAndTimeConverter?.timeZoneText ?: "GMT+3"
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dataTextStyle = MaterialTheme.typography.bodyLarge

    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    var pickerTarget by remember { mutableStateOf(OwPickerTarget.NONE) }

    // Тосты из ViewModel (например, авто-подстановка времени без данных).
    LaunchedEffect(Unit) {
        messageFlow.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

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
                        text = "Прочая работа",
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
                            onExit()
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
        if (formUiState.saveOtherWorkState is ResultState.Error) {
            val errorState = formUiState.saveOtherWorkState as ResultState.Error
            LaunchedEffect(Unit) {
                scope.launch {
                    snackbarHostState.showSnackbar("Ошибка: ${errorState.entity.message}")
                }
                resetSaveState()
            }
        }

        var showRemoveTimeStart by remember { mutableStateOf(false) }
        if (showRemoveTimeStart) {
            AppBottomSheet(
                onDismissRequest = { showRemoveTimeStart = false },
                sheetState = sheetState,
                title = "Время начала",
                actions = listOf(
                    BottomSheetAction(text = "Удалить значение") { onTimeStartChanged(null) }
                )
            )
        }

        var showRemoveTimeEnd by remember { mutableStateOf(false) }
        if (showRemoveTimeEnd) {
            AppBottomSheet(
                onDismissRequest = { showRemoveTimeEnd = false },
                sheetState = sheetState,
                title = "Время окончания",
                actions = listOf(
                    BottomSheetAction(text = "Удалить значение") { onTimeEndChanged(null) }
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
            currentOtherWork?.let { otherWork ->
                LazyColumn(
                    state = scrollState,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp, top = 8.dp, end = 16.dp, bottom = 32.dp
                    )
                ) {
                    item { errorMessage?.let { OwBanner(it) } }

                    // ── ТИП РАБОТЫ ──
                    item {
                        WorkTypeCard(
                            currentType = otherWork.workType,
                            typeList = workTypeList,
                            onSelectType = onWorkTypeChanged,
                            onAddCustomType = onAddCustomType,
                            onDeleteCustomType = onDeleteCustomType,
                            dataTextStyle = dataTextStyle
                        )
                    }

                    // ── СТАНЦИЯ ──
                    item {
                        StationCard(
                            initialStation = otherWork.station ?: "",
                            dropDownMenuList = stationDropDownList,
                            onStationChanged = onStationChanged,
                            onChangedDropDownContent = onChangedStationContent,
                            onDeleteStationName = onDeleteStationName,
                            dataTextStyle = dataTextStyle,
                            focusManager = focusManager,
                            scope = scope
                        )
                    }

                    // ── ВРЕМЯ ──
                    item {
                        OwGroup(label = "ВРЕМЯ") {
                            OwFlatCard {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    TimeRow(
                                        label = "Начало",
                                        time = otherWork.timeStart,
                                        dateAndTimeConverter = dateAndTimeConverter,
                                        onOpenPicker = { pickerTarget = OwPickerTarget.START },
                                        onRemoveTime = { showRemoveTimeStart = true }
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    )
                                    TimeRow(
                                        label = "Окончание",
                                        time = otherWork.timeEnd,
                                        dateAndTimeConverter = dateAndTimeConverter,
                                        onOpenPicker = { pickerTarget = OwPickerTarget.END },
                                        onRemoveTime = { showRemoveTimeEnd = true }
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            // Кнопки авто-подстановки времени.
                            AutoTimeButton(
                                label = "От явки до окончания работы",
                                onClick = onApplyTimeFromWork
                            )
                            Spacer(Modifier.height(8.dp))
                            AutoTimeButton(
                                label = "От приёмки до сдачи локомотива",
                                onClick = onApplyTimeFromLoco
                            )
                        }
                    }

                    // ── Длительность ──
                    item {
                        AnimatedVisibility(
                            visible = errorMessage == null && resultTime != null && resultTime > 0,
                            enter = fadeIn(tween(250)),
                            exit = fadeOut(tween(150))
                        ) {
                            OwDurationBadge(label = "Длительность · ${formatOwDuration(resultTime)}")
                        }
                    }

                    // ── ПРИМЕЧАНИЯ ──
                    item {
                        OwGroup(label = "ПРИМЕЧАНИЯ") {
                            OwFlatCard {
                                OutlinedTextFieldApp(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 64.dp)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    value = otherWork.notes ?: "",
                                    onValueChange = { onNotesChanged(it) },
                                    textStyle = dataTextStyle,
                                    placeholder = {
                                        Text(
                                            text = "Например: фамилия составителя",
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

        when (pickerTarget) {
            OwPickerTarget.START -> {
                AppDateTimePicker(
                    title = "Начало",
                    onDateTimeSelected = { timestamp -> onTimeStartChanged(timestamp) },
                    onDismiss = { pickerTarget = OwPickerTarget.NONE },
                    startDateTime = currentOtherWork?.timeStart ?: TimeManager().now(),
                    timeZoneStr = displayTz,
                )
            }

            OwPickerTarget.END -> {
                AppDateTimePicker(
                    title = "Окончание",
                    onDateTimeSelected = { timestamp -> onTimeEndChanged(timestamp) },
                    onDismiss = { pickerTarget = OwPickerTarget.NONE },
                    startDateTime = currentOtherWork?.timeEnd ?: TimeManager().now(),
                    timeZoneStr = displayTz,
                )
            }

            OwPickerTarget.NONE -> Unit
        }

        // Шторка выбора локомотива (при нескольких локо) — время задаётся по выбранному.
        if (!locoPicker.isNullOrEmpty()) {
            LocoPickerSheet(
                locomotives = locoPicker,
                onPick = onPickLoco,
                onDismiss = onDismissLocoPicker
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocoPickerSheet(
    locomotives: List<com.z_company.domain.entities.route.Locomotive>,
    onPick: (com.z_company.domain.entities.route.Locomotive) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
        ) {
            Text(
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                text = "ЛОКОМОТИВ · ${locomotives.size}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = MonoFont,
                    letterSpacing = 1.4.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            locomotives.forEachIndexed { index, loco ->
                LocoSheetRow(
                    name = locoDisplayName(loco, index + 1),
                    onClick = { onPick(loco) }
                )
            }
        }
    }
}

@Composable
private fun LocoSheetRow(name: String, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(com.z_company.route.R.drawable.ic_card_locomotive_ref),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = c.tertiary
            )
        }
        Text(
            modifier = Modifier.weight(1f),
            text = name,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = c.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            painter = painterResource(R.drawable.keyboard_arrow_right_24px),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = c.onSurfaceVariant
        )
    }
}

private fun locoDisplayName(loco: com.z_company.domain.entities.route.Locomotive, ordinal: Int): String {
    val s = loco.series?.takeIf { it.isNotBlank() }
    val n = loco.number?.takeIf { it.isNotBlank() }
    return when {
        s != null && n != null -> "$s-$n"
        s != null -> "$s б/н"
        n != null -> "${loco.type.text} $n"
        else -> "Локомотив $ordinal"
    }
}

private enum class OwPickerTarget { NONE, START, END }

private fun formatOwDuration(millis: Long?): String {
    val total = ((millis ?: 0L) / 60_000L).toInt()
    val h = total / 60
    val m = total % 60
    return when {
        h > 0 && m > 0 -> "$h ч $m мин"
        h > 0 -> "$h ч"
        else -> "$m мин"
    }
}

@Composable
private fun OwGroup(
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

@Composable
private fun OwFlatCard(
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

@Composable
private fun OwBanner(text: String) {
    val warning = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(warning.copy(alpha = 0.10f), Shapes.medium)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(warning.copy(alpha = 0.18f), CircleShape),
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

/**
 * Выбор типа работы: компактное поле-селектор в ритме блока «Станция».
 * Тап раскрывает модальную шторку снизу ([ModalBottomSheet]) со списком типов
 * (предопределённые [OtherWork.PREDEFINED_TYPES] + пользовательские). Выбор —
 * единичный. Кастомные типы можно удалить (корзина, только у них); новый тип
 * добавляется явно кнопкой «Добавить свой тип» → диалог. Тип хранится как
 * строка-title (удаление типа не меняет уже сохранённые записи).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkTypeCard(
    currentType: String?,
    typeList: List<String>,
    onSelectType: (String) -> Unit,
    onAddCustomType: (String) -> Unit,
    onDeleteCustomType: (String) -> Unit,
    dataTextStyle: androidx.compose.ui.text.TextStyle
) {
    var showSheet by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDeleteType by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    OwGroup(label = "ТИП РАБОТЫ") {
        OwFlatCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clickable { showSheet = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(com.z_company.route.R.drawable.ic_card_other_work_ref),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
                Text(
                    modifier = Modifier.weight(1f),
                    text = currentType?.takeIf { it.isNotBlank() } ?: "Выберите тип",
                    style = dataTextStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = if (currentType.isNullOrBlank())
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                    else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(com.z_company.route.R.drawable.keyboard_arrow_down_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            WorkTypeSheetContent(
                currentType = currentType,
                typeList = typeList,
                onSelect = { type ->
                    onSelectType(type)
                    scope.launch { sheetState.hide() }
                        .invokeOnCompletion { if (!sheetState.isVisible) showSheet = false }
                },
                onDelete = { pendingDeleteType = it },
                onAdd = {
                    scope.launch { sheetState.hide() }
                        .invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showSheet = false
                                showAddDialog = true
                            }
                        }
                }
            )
        }
    }

    if (showAddDialog) {
        AddTypeDialog(
            dataTextStyle = dataTextStyle,
            onConfirm = { name ->
                onAddCustomType(name)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    pendingDeleteType?.let { type ->
        AppAlertDialog(
            onDismissRequest = { pendingDeleteType = null },
            title = "Удалить тип «$type»?",
            text = "Тип работы будет удалён из списка. Уже сохранённые записи не изменятся.",
            confirmText = "Удалить",
            onConfirm = {
                onDeleteCustomType(type)
                pendingDeleteType = null
            },
            isDestructive = true,
            dismissText = "Отмена",
            onDismiss = { pendingDeleteType = null }
        )
    }
}

/** Содержимое шторки выбора типа работы: список + удаление кастомных + «Добавить свой тип». */
@Composable
private fun WorkTypeSheetContent(
    currentType: String?,
    typeList: List<String>,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        Text(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
            text = "ТИП РАБОТЫ",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = MonoFont,
                letterSpacing = 1.4.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        typeList.forEachIndexed { index, type ->
            val selected = type == currentType
            val isCustom = !OtherWork.PREDEFINED_TYPES.contains(type)
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .background(
                        if (selected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)
                        else Color.Transparent
                    )
                    .clickable { onSelect(type) }
                    .padding(start = 20.dp, end = if (isCustom) 4.dp else 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = type,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = if (selected) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (selected) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        painter = painterResource(com.z_company.route.R.drawable.check_circle_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
                if (isCustom) {
                    IconButton(onClick = { onDelete(type) }) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(com.z_company.route.R.drawable.delete_24px),
                            contentDescription = "Удалить тип",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(onClick = onAdd)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(com.z_company.route.R.drawable.add_circle_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = "Добавить свой тип",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun AddTypeDialog(
    dataTextStyle: androidx.compose.ui.text.TextStyle,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AppInputBottomSheet(
        onDismissRequest = onDismiss,
        title = "Новый тип работы",
        hint = "Как называется этот вид работы?",
        initialValue = "",
        onConfirm = { onConfirm(it.trim()) },
        confirmText = "Добавить",
        label = "Название",
        keyboardType = KeyboardType.Text,
    )
}

/** Поле станции с автодополнением. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun StationCard(
    initialStation: String,
    dropDownMenuList: List<String>,
    onStationChanged: (String) -> Unit,
    onChangedDropDownContent: (String) -> Unit,
    onDeleteStationName: (String) -> Unit,
    dataTextStyle: androidx.compose.ui.text.TextStyle,
    focusManager: androidx.compose.ui.focus.FocusManager,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var stationName by remember {
        mutableStateOf(TextFieldValue(text = initialStation, selection = TextRange(initialStation.length)))
    }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    OwGroup(label = "СТАНЦИЯ") {
        OwFlatCard {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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
                            .onFocusChanged { focusState ->
                                // При фокусе показываем полный список станций (как на других экранах).
                                if (focusState.isFocused) {
                                    onChangedDropDownContent(stationName.text)
                                    isDropdownExpanded = true
                                }
                            },
                        value = stationName,
                        onValueChange = {
                            stationName = it
                            onChangedDropDownContent(it.text)
                            onStationChanged(it.text)
                            isDropdownExpanded = true
                        },
                        textStyle = dataTextStyle.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        placeholder = {
                            Text(
                                text = "Станция",
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
        }
    }
}

/**
 * Кнопка-шорткат авто-подстановки времени (Secondary / soft accent):
 * accentSoft-заливка + accent-текст, радиус 12, без рамки. Вспомогательное действие —
 * не спорит с главным CTA экрана.
 */
@Composable
private fun AutoTimeButton(
    label: String,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.tertiary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(com.z_company.route.R.drawable.schedule_24px),
            contentDescription = null,
            tint = accent
        )
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = accent,
            textAlign = TextAlign.Start
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimeRow(
    label: String,
    time: Long?,
    dateAndTimeConverter: DateAndTimeConverter?,
    onOpenPicker: () -> Unit,
    onRemoveTime: () -> Unit
) {
    val hasValue = time != null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            modifier = Modifier.width(96.dp),
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OwPickerChip(
            modifier = Modifier.weight(1f),
            iconRes = com.z_company.route.R.drawable.calendar_today_24px,
            value = if (hasValue) dateAndTimeConverter?.getDate(time) ?: "" else "Дата",
            isPlaceholder = !hasValue,
            onClick = onOpenPicker,
            onLongClick = { if (hasValue) onRemoveTime() }
        )
        OwPickerChip(
            modifier = Modifier.weight(1f),
            iconRes = com.z_company.route.R.drawable.schedule_24px,
            value = if (hasValue) dateAndTimeConverter?.getTime(time) ?: "" else "Время",
            isPlaceholder = !hasValue,
            onClick = onOpenPicker,
            onLongClick = { if (hasValue) onRemoveTime() }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OwPickerChip(
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

@Composable
private fun OwDurationBadge(label: String) {
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
        // Нейтральная read-only сводка — без accent (это не действие, а информация).
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                modifier = Modifier.size(13.dp),
                painter = painterResource(com.z_company.route.R.drawable.schedule_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = MonoFont,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
