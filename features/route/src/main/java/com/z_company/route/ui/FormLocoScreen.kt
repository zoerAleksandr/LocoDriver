package com.z_company.route.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.z_company.route.component.StationDropdownMenu
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.focus.FocusDirection
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
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.z_company.core.ResultState
import com.z_company.core.ui.component.CustomDivider
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.util.CalculationEnergy
import com.z_company.route.component.BottomShadow
import com.z_company.route.component.DieselSectionItem
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.route.component.AppDateTimePicker
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.domain.repositories.SharedPreferencesRepositories
import org.koin.compose.koinInject
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.route.extention.isScrollInInitialState
import com.z_company.route.viewmodel.LocoFormUiState
import java.util.Calendar
import com.z_company.domain.util.*
import com.z_company.domain.util.CalculationEnergy.rounding
import com.z_company.route.R
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.ElectricSectionItem
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.component.SwitchApp
import com.z_company.route.component.CollapsibleSection
import com.z_company.route.component.DieselStatisticsSection
import com.z_company.route.component.ElectricStatisticsSection
import com.z_company.route.viewmodel.DieselSectionFormState
import com.z_company.route.viewmodel.DieselSectionType
import com.z_company.route.viewmodel.ElectricSectionFormState
import com.z_company.route.viewmodel.ElectricSectionType
import com.z_company.route.viewmodel.LocoFormViewModel
import com.z_company.domain.entities.setting.UserSettings
import androidx.compose.material3.AlertDialog
import com.z_company.route.ui.TimeBottomSheet
import com.z_company.route.ui.TimeSheetResult
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun FormLocoScreen(
    viewModel: LocoFormViewModel,
    currentLoco: Locomotive?,
    dieselSectionListState: SnapshotStateList<DieselSectionFormState>?,
    electricSectionListState: SnapshotStateList<ElectricSectionFormState>?,
    onLocoSaved: () -> Unit,
    formUiState: LocoFormUiState,
    resetSaveState: () -> Unit,
    onNumberChanged: (String) -> Unit,
    onSeriesChanged: (String) -> Unit,
    onChangedTypeLoco: (LocoType) -> Unit,
    onStartAcceptedTimeChanged: (Long?) -> Unit,
    onEndAcceptedTimeChanged: (Long?) -> Unit,
    onStartDeliveryTimeChanged: (Long?) -> Unit,
    onEndDeliveryTimeChanged: (Long?) -> Unit,
    onFuelAcceptedChanged: (Int, String?) -> Unit,
    onFuelDeliveredChanged: (Int, String?) -> Unit,
    onDeleteSectionDiesel: (DieselSectionFormState) -> Unit,
    addingSectionDiesel: () -> Unit,
    focusChangedDieselSection: (Int, DieselSectionType) -> Unit,
    onDeleteSectionElectric: (ElectricSectionFormState) -> Unit,
    addingSectionElectric: () -> Unit,
    focusChangedElectricSection: (Int, ElectricSectionType) -> Unit,
    onExpandStateElectricSection: (Boolean) -> Unit,
    onRefuelValueChanged: (Int, String?) -> Unit,
    onRefuelInKiloValueChanged: (Int, String?) -> Unit,
    onRefuelCoefficientValueChanged: (Int, String?) -> Unit,
    onCoefficientValueChanged: (Int, String?) -> Unit,
    exitScreen: () -> Unit,
    dropDownSeriesMenuList: List<String>,
    isExpandedMenu: Boolean,
    onExpandedMenuChange: (Boolean) -> Unit,
    onChangedContentMenu: (String) -> Unit,
    onDeleteSeries: (String) -> Unit,
    dateAndTimeConverter: DateAndTimeConverter?,
    userSettings: UserSettings? = null,
    onSettingsClick: () -> Unit = {},
    onNavigateToSeriesSettings: () -> Unit = {},
    onNavigateToStationSettings: () -> Unit = {},
    onEditStation: ((String) -> Unit)? = null,
    onEditSeries: ((String) -> Unit)? = null,
) {
    val displayTz = dateAndTimeConverter?.timeZoneText ?: "GMT+3"
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val sharedPrefs: SharedPreferencesRepositories = koinInject()
    val keyboardController = LocalSoftwareKeyboardController.current
    val topLevelFocusManager = LocalFocusManager.current

    val noValueColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    if (formUiState.isShowUpdateHint) {
        AlertDialog(
            onDismissRequest = viewModel::dismissUpdateHint,
            containerColor = MaterialTheme.colorScheme.secondary,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.onBackground,
            title = { Text("Обновление") },
            text = {
                Text(
                    "В форму локомотива добавлены новые поля: счётчики отопления и собственных нужд.\n\n" +
                            "Вы можете отключить их отображение в Настройках \u2192 Локомотив."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissUpdateHint) {
                    Text(
                        text = "Понятно",
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )
    }

    // Подтверждение удаления секции (Diesel/Electric).
    // Каждый sheet — отдельный sheetState чтобы избежать конфликта shared state.
    var closeSwipeSignal by remember { mutableStateOf(0) }
    formUiState.confirmDeleteDieselSectionId?.let { sectionId ->
        // sectionId захватываем в локальную переменную — иначе AppBottomSheet
        // вызывает onDismissRequest() ПЕРЕД action.onClick(), что сбрасывает
        // confirmDeleteDieselSectionId в null, и confirmDeleteDieselSection()
        // читает null → ничего не удаляется.
        val confirmSheetState = androidx.compose.material3.rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
        com.z_company.route.component.AppBottomSheet(
            onDismissRequest = {
                viewModel.cancelDeleteDieselSection()
                closeSwipeSignal++
            },
            sheetState = confirmSheetState,
            title = "Удалить секцию?",
            actions = listOf(
                com.z_company.route.component.BottomSheetAction(text = "Да, удалить") {
                    viewModel.deleteDieselSectionById(sectionId)
                }
            )
        )
    }
    formUiState.confirmDeleteElectricSectionId?.let { sectionId ->
        val confirmSheetState = androidx.compose.material3.rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
        com.z_company.route.component.AppBottomSheet(
            onDismissRequest = {
                viewModel.cancelDeleteElectricSection()
                closeSwipeSignal++
            },
            sheetState = confirmSheetState,
            title = "Удалить секцию?",
            actions = listOf(
                com.z_company.route.component.BottomSheetAction(text = "Да, удалить") {
                    viewModel.deleteElectricSectionById(sectionId)
                }
            )
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxWidth(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Локомотив",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            topLevelFocusManager.clearFocus()
                            onLocoSaved()
                        }
                    ) {
                        Icon(
                            painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_left_24px),
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSettingsClick
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.settings_24px),
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
            }
        }
    ) { paddingValues ->
        if (formUiState.saveLocoState is ResultState.Error) {
            LaunchedEffect(Unit) {
                scope.launch {
                    snackbarHostState.showSnackbar("Ошибка: ${formUiState.saveLocoState.entity.message}")
                }
                resetSaveState()
            }
        }
        if (formUiState.exitFromScreen) {
            LaunchedEffect(Unit) {
                exitScreen()
            }
        }

        Box(modifier = Modifier.padding(paddingValues)) {
            currentLoco?.let { locomotive ->
                val sheetState = rememberModalBottomSheetState(
                    skipPartiallyExpanded = true
                )

                val scrollState = rememberLazyListState()
                val focusManager = LocalFocusManager.current
                val scope = rememberCoroutineScope()
                val dataTextStyle = MaterialTheme.typography.bodyLarge

                val subTitleTextStyle = MaterialTheme.typography.bodyMedium

                AnimatedVisibility(
                    modifier = Modifier
                        .zIndex(1f),
                    visible = !scrollState.isScrollInInitialState(),
                    enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 300))
                ) {
                    BottomShadow()
                }

                var showBottomSheetRemoveTimeStartAccepted by remember {
                    mutableStateOf(false)
                }

                if (showBottomSheetRemoveTimeStartAccepted) {
                    AppBottomSheet(
                        onDismissRequest = { showBottomSheetRemoveTimeStartAccepted = false },
                        sheetState = sheetState,
                        headerContent = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "Начало приемки",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        actions = listOf(
                            BottomSheetAction(text = "Удалить значение") {
                                onStartAcceptedTimeChanged(null)
                            }
                        )
                    )
                }

                var showBottomSheetRemoveTimeStartDelivery by remember { mutableStateOf(false) }

                if (showBottomSheetRemoveTimeStartDelivery) {
                    AppBottomSheet(
                        onDismissRequest = { showBottomSheetRemoveTimeStartDelivery = false },
                        sheetState = sheetState,
                        headerContent = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "Начало сдачи",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        actions = listOf(
                            BottomSheetAction(text = "Удалить значение") {
                                onStartDeliveryTimeChanged(null)
                            }
                        )
                    )
                }

                var showBottomSheetRemoveTimeEndAccepted by remember {
                    mutableStateOf(false)
                }

                if (showBottomSheetRemoveTimeEndAccepted) {
                    AppBottomSheet(
                        onDismissRequest = { showBottomSheetRemoveTimeEndAccepted = false },
                        sheetState = sheetState,
                        headerContent = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "Окончание приемки",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        actions = listOf(
                            BottomSheetAction(text = "Удалить значение") {
                                onEndAcceptedTimeChanged(null)
                            }
                        )
                    )
                }

                var showBottomSheetRemoveTimeEndDelivery by remember {
                    mutableStateOf(false)
                }

                if (showBottomSheetRemoveTimeEndDelivery) {
                    AppBottomSheet(
                        onDismissRequest = { showBottomSheetRemoveTimeEndDelivery = false },
                        sheetState = sheetState,
                        headerContent = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "Окончание сдачи",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        actions = listOf(
                            BottomSheetAction(text = "Удалить значение") {
                                onEndDeliveryTimeChanged(null)
                            }
                        )
                    )
                }

                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.testTag("form_loco_lazy_column"),
                    horizontalAlignment = Alignment.End,
                ) {
                    item {
                        val currentType = locomotive.type
                        GroupHead(
                            text = "Основные данные",
                            topPadding = 4.dp,
                            trailing = {
                            SwitchApp(
                                modifier = Modifier.wrapContentWidth(),
                                checked = currentType == LocoType.ELECTRIC,
                                onCheckedChange = {
                                    onChangedTypeLoco(
                                        if (currentType == LocoType.ELECTRIC) {
                                            LocoType.DIESEL
                                        } else {
                                            LocoType.ELECTRIC
                                        }
                                    )
                                },
                                positiveContent = {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.electric_bolt_24px),

                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.8f
                                            )
                                        )
                                    }
                                },
                                negativeContent = {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.opacity_24px),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.8f
                                            )
                                        )
                                    }
                                }
                            )
                            }
                        )
                    }

                    item {
                        var seriesName by remember {
                            mutableStateOf(
                                TextFieldValue(
                                    text = locomotive.series ?: "",
                                    selection = TextRange(locomotive.series?.length ?: 0)
                                )
                            )
                        }

                        val cellLabelStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                        MCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Ячейка «Серия»
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Серия",
                                        style = cellLabelStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                    ExposedDropdownMenuBox(
                                        modifier = Modifier.fillMaxWidth(),
                                        expanded = isExpandedMenu,
                                        onExpandedChange = { onExpandedMenuChange(it) }
                                    ) {
                                        OutlinedTextFieldApp(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                                            value = seriesName,
                                            onValueChange = {
                                                seriesName = it
                                                onSeriesChanged(it.text)
                                                onChangedContentMenu(it.text)
                                            },
                                            textStyle = dataTextStyle,
                                            fieldElevation = 0.dp,
                                            colorBackgroundEmptyField = Color.Transparent,
                                            colorBackgroundNotEmptyField = Color.Transparent,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Text,
                                                imeAction = ImeAction.Done
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onDone = { focusManager.clearFocus() }
                                            ),
                                            singleLine = true
                                        )

                                        StationDropdownMenu(
                                            expanded = isExpandedMenu,
                                            stations = dropDownSeriesMenuList,
                                            onSelect = { selectionSeries ->
                                                onSeriesChanged(selectionSeries)
                                                onExpandedMenuChange(false)
                                                seriesName = seriesName.copy(
                                                    text = selectionSeries,
                                                    selection = TextRange(selectionSeries.length)
                                                )
                                            },
                                            onDelete = onDeleteSeries,
                                            onDismiss = { onExpandedMenuChange(false) },
                                            textStyle = dataTextStyle
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(48.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )

                                // Ячейка «Номер»
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Номер",
                                        style = cellLabelStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                    OutlinedTextFieldApp(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = locomotive.number ?: "",
                                        textStyle = dataTextStyle,
                                        fieldElevation = 0.dp,
                                        colorBackgroundEmptyField = Color.Transparent,
                                        colorBackgroundNotEmptyField = Color.Transparent,
                                        onValueChange = { onNumberChanged(it) },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = { focusManager.clearFocus() }
                                        ),
                                        singleLine = true,
                                    )
                                }
                            }
                        }

                    }

                    // Время — GroupHead + карточка с TimeSummaryRow
                    item {
                        val routeStart by viewModel.routeStartWork.collectAsState()
                        val routeEnd by viewModel.routeEndWork.collectAsState()
                        var showAcceptanceSheet by remember { mutableStateOf(false) }
                        var showDeliverySheet by remember { mutableStateOf(false) }

                        if (showAcceptanceSheet) {
                            TimeBottomSheet(
                                kind = "acceptance",
                                seriesName = locomotive.series,
                                locoType = locomotive.type,
                                initialStartTime = locomotive.timeStartOfAcceptance,
                                initialEndTime = locomotive.timeEndOfAcceptance,
                                initialBarrierOut = locomotive.timeBarrierOut,
                                initialBarrierIn = locomotive.timeBarrierIn,
                                routeStartWork = routeStart,
                                routeEndWork = routeEnd,
                                initialStationId = locomotive.acceptanceStationId,
                                timeZoneText = displayTz,
                                onSave = { result ->
                                    viewModel.saveAcceptanceFromSheet(result.startTime, result.endTime, result.barrierOut, result.stationId)
                                    showAcceptanceSheet = false
                                },
                                onClose = { showAcceptanceSheet = false },
                                onNavigateToSeriesSettings = onNavigateToSeriesSettings,
                                onNavigateToStationSettings = onNavigateToStationSettings,
                                onEditStation = onEditStation,
                                onSeriesChanged = viewModel::setSeries,
                                onEditSeries = onEditSeries,
                            )
                        }

                        if (showDeliverySheet) {
                            TimeBottomSheet(
                                kind = "delivery",
                                seriesName = locomotive.series,
                                locoType = locomotive.type,
                                initialStartTime = locomotive.timeStartOfDelivery,
                                initialEndTime = locomotive.timeEndOfDelivery,
                                initialBarrierOut = locomotive.timeBarrierOut,
                                initialBarrierIn = locomotive.timeBarrierIn,
                                routeStartWork = routeStart,
                                routeEndWork = routeEnd,
                                initialStationId = locomotive.deliveryStationId,
                                timeZoneText = displayTz,
                                onSave = { result ->
                                    viewModel.saveDeliveryFromSheet(result.barrierIn, result.startTime, result.endTime, result.stationId)
                                    showDeliverySheet = false
                                },
                                onClose = { showDeliverySheet = false },
                                onNavigateToSeriesSettings = onNavigateToSeriesSettings,
                                onNavigateToStationSettings = onNavigateToStationSettings,
                                onEditStation = onEditStation,
                                onSeriesChanged = viewModel::setSeries,
                                onEditSeries = onEditSeries,
                                onTimeEndWorkChanged = viewModel::setTimeEndWork,
                            )
                        }

                        // GroupHead "ВРЕМЯ"
                        GroupHead(text = "Время")

                        // Карточка: Приёмка / Сдача
                        MCard {
                            TimeSummaryRow(
                                label = "ПРИЁМКА",
                                stops = listOf(
                                    TimeStop(
                                        time = locomotive.timeStartOfAcceptance?.let { dateAndTimeConverter?.getTime(it) },
                                        caption = "начало"
                                    ),
                                    TimeStop(
                                        time = locomotive.timeEndOfAcceptance?.let { dateAndTimeConverter?.getTime(it) },
                                        caption = "конец"
                                    ),
                                    TimeStop(
                                        time = locomotive.timeBarrierOut?.let { dateAndTimeConverter?.getTime(it) },
                                        caption = "КП"
                                    ),
                                ),
                                onClick = { showAcceptanceSheet = true }
                            )
                            CustomDivider(orientation = Orientation.Horizontal)
                            TimeSummaryRow(
                                label = "СДАЧА",
                                stops = listOf(
                                    TimeStop(
                                        time = locomotive.timeBarrierIn?.let { dateAndTimeConverter?.getTime(it) },
                                        caption = "КП"
                                    ),
                                    TimeStop(
                                        time = locomotive.timeStartOfDelivery?.let { dateAndTimeConverter?.getTime(it) },
                                        caption = "начало"
                                    ),
                                    TimeStop(
                                        time = locomotive.timeEndOfDelivery?.let { dateAndTimeConverter?.getTime(it) },
                                        caption = "конец"
                                    ),
                                ),
                                onClick = { showDeliverySheet = true }
                            )
                        }
                    }

                    item {
                        val sectionsCount = when (locomotive.type.name) {
                            LocoType.ELECTRIC.name -> electricSectionListState?.size ?: 0
                            else -> dieselSectionListState?.size ?: 0
                        }
                        GroupHead(
                            text = "Секции",
                            trailing = {
                                SectionStepper(
                                    value = sectionsCount,
                                    onMinus = {
                                        when (locomotive.type.name) {
                                            LocoType.DIESEL.name ->
                                                dieselSectionListState?.lastOrNull()?.let { onDeleteSectionDiesel(it) }
                                            LocoType.ELECTRIC.name ->
                                                electricSectionListState?.lastOrNull()?.let { onDeleteSectionElectric(it) }
                                        }
                                    },
                                    onPlus = {
                                        when (locomotive.type.name) {
                                            LocoType.DIESEL.name -> addingSectionDiesel()
                                            LocoType.ELECTRIC.name -> addingSectionElectric()
                                        }
                                    }
                                )
                            }
                        )
                    }

                    when (locomotive.type.name) {
                        LocoType.DIESEL.name -> {
                            dieselSectionListState?.let {
                                itemsIndexed(
                                    items = dieselSectionListState,
                                    key = { _, item -> item.sectionId }
                                ) { index, item ->
                                    DieselSectionItem(
                                        item = item,
                                        index = index,
                                        onFuelAcceptedChanged = onFuelAcceptedChanged,
                                        onFuelDeliveredChanged = onFuelDeliveredChanged,
                                        onDeleteItem = onDeleteSectionDiesel,
                                        focusChangedDieselSection = focusChangedDieselSection,
                                        onRefuelValueChanged = onRefuelValueChanged,
                                        onRefuelInKiloValueChanged = onRefuelInKiloValueChanged,
                                        onRefuelCoefficientValueChanged = onRefuelCoefficientValueChanged,
                                        onCoefficientValueChanged = onCoefficientValueChanged,
                                        sheetState = sheetState,
                                        isKiloMode = formUiState.isKiloMode,
                                        changeIsKiloMode = viewModel::toggleIsKiloMode,
                                        recentCoefficients = { viewModel.recentCoefficients() },
                                        onSaveCoefficient = viewModel::saveRecentCoefficient,
                                        closeSwipeSignal = closeSwipeSignal
                                    )
                                }
                            }
                        }

                        LocoType.ELECTRIC.name -> {
                            electricSectionListState?.let {
                                itemsIndexed(
                                    items = electricSectionListState,
                                    key = { _, item -> item.sectionId }
                                ) { index, item ->
                                    ElectricSectionItem(
                                        index = index,
                                        item = item,
                                        onDeleteItem = onDeleteSectionElectric,
                                        onEnergyAcceptedChanged = viewModel::setEnergyAccepted,
                                        onEnergyDeliveryChanged = viewModel::setEnergyDelivery,
                                        onRecoveryAcceptedChanged = viewModel::setRecoveryAccepted,
                                        onRecoveryDeliveryChanged = viewModel::setRecoveryDelivery,
                                        onEnergyAcceptedChanged2 = viewModel::setEnergyAccepted2,
                                        onEnergyDeliveryChanged2 = viewModel::setEnergyDelivery2,
                                        onRecoveryAcceptedChanged2 = viewModel::setRecoveryAccepted2,
                                        onRecoveryDeliveryChanged2 = viewModel::setRecoveryDelivery2,
                                        focusChangedElectricSection = focusChangedElectricSection,
                                        onExpandStateChanged = onExpandStateElectricSection,
                                        showOtherCurrent = formUiState.isShowOtherCurrent,
                                        closeSwipeSignal = closeSwipeSignal
                                    )
                                }
                            }
                        }
                    }

                    // Показания — Отопление + Собств. нужды в одной карточке
                    run {
                        val showHeating = userSettings?.isShowLocoHeating != false
                        val showAux = userSettings?.isShowLocoAuxiliary != false
                        if (showHeating || showAux) {
                            item {
                                GroupHead(text = "Показания")
                                MCard {
                                    if (showHeating) {
                                        ReadingRow(
                                            icon = R.drawable.nest_farsight_heat_24px,
                                            label = "Отопление",
                                            unit = "кВт·ч",
                                            accepted = formUiState.heatingAcceptedText,
                                            delivered = formUiState.heatingDeliveryText,
                                            onAccepted = { viewModel.setHeatingCounterAccepted(it) },
                                            onDelivered = { viewModel.setHeatingCounterDelivery(it) },
                                            onNext = { focusManager.moveFocus(FocusDirection.Right) },
                                            onDone = { focusManager.clearFocus() },
                                            dataTextStyle = dataTextStyle,
                                            noValueColor = noValueColor
                                        )
                                    }
                                    if (showHeating && showAux) {
                                        CustomDivider(orientation = Orientation.Horizontal)
                                    }
                                    if (showAux) {
                                        ReadingRow(
                                            icon = R.drawable.electric_bolt_24px,
                                            label = "Собств. нужды",
                                            unit = "кВт·ч",
                                            accepted = formUiState.auxiliaryAcceptedText,
                                            delivered = formUiState.auxiliaryDeliveryText,
                                            onAccepted = { viewModel.setAuxiliaryCounterAccepted(it) },
                                            onDelivered = { viewModel.setAuxiliaryCounterDelivery(it) },
                                            onNext = { focusManager.moveFocus(FocusDirection.Right) },
                                            onDone = { focusManager.clearFocus() },
                                            dataTextStyle = dataTextStyle,
                                            noValueColor = noValueColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Итого — TotalsBlock в одной карточке
                    if (userSettings?.isShowLocoStatistics != false) {
                        item { GroupHead(text = "Итого") }
                        item {
                            MCard {
                                when (currentLoco.type) {
                                    LocoType.ELECTRIC -> {
                                        ElectricStatisticsSection(
                                            electricSectionListState = electricSectionListState,
                                            locomotive = locomotive,
                                            isShowOtherCurrent = formUiState.isShowOtherCurrent,
                                            onSettingsClick = onSettingsClick,
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    }
                                    LocoType.DIESEL -> {
                                        DieselStatisticsSection(
                                            dieselSectionListState = dieselSectionListState,
                                            locomotive = locomotive,
                                            onSettingsClick = onSettingsClick,
                                            onNormaChange = { viewModel.setNormaDiesel(it) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Норма — отдельным блоком только для электровоза
                    if (userSettings?.isShowLocoNorma != false && currentLoco.type == LocoType.ELECTRIC) {
                    item {
                        GroupHead(text = "Норма")
                        MCard {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextFieldApp(
                                    modifier = Modifier.weight(1f),
                                    value = formUiState.norma1Text,
                                    placeholder = {
                                        Text(
                                            text = "Ток 1",
                                            style = LocalTextStyle.current.copy(fontWeight = FontWeight.Light),
                                            color = noValueColor
                                        )
                                    },
                                    textStyle = dataTextStyle,
                                    fieldElevation = 0.dp,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                                    colorBackgroundEmptyField = Color.Transparent,
                                    colorBackgroundNotEmptyField = Color.Transparent,
                                    onValueChange = { viewModel.setNormaElectricCurrent1(it) },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done
                                    ),
                                    singleLine = true
                                )
                                if (userSettings?.isShowOtherCurrent == true) {
                                    OutlinedTextFieldApp(
                                        modifier = Modifier.weight(1f),
                                        value = formUiState.norma2Text,
                                        placeholder = {
                                            Text(
                                                text = "Ток 2",
                                                style = LocalTextStyle.current.copy(fontWeight = FontWeight.Light),
                                                color = noValueColor
                                            )
                                        },
                                        textStyle = dataTextStyle,
                                        fieldElevation = 0.dp,
                                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                                        colorBackgroundEmptyField = Color.Transparent,
                                        colorBackgroundNotEmptyField = Color.Transparent,
                                        onValueChange = { viewModel.setNormaElectricCurrent2(it) },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done
                                        ),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

private fun buildTimeSummary(times: List<Long?>, converter: DateAndTimeConverter?): String {
    val parts = times.mapNotNull { ms ->
        if (ms == null || converter == null) null else converter.getTime(ms).takeIf { it.isNotBlank() }
    }
    return parts.joinToString(" → ")
}

data class TimeStop(val time: String?, val caption: String)

@Composable
private fun TimeSummaryRow(
    label: String,
    stops: List<TimeStop>,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Upper: label + chevron
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp)
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Icon(
                painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_right_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                modifier = Modifier.size(14.dp)
            )
        }

        // Lower: 3 time cells in 1fr → 1fr → 1fr grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            stops.forEachIndexed { idx, stop ->
                if (idx > 0) {
                    Text(
                        text = "→",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .padding(top = 4.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = stop.time ?: "—:—",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            fontSize = 18.sp,
                            lineHeight = 18.sp
                        ),
                        color = if (stop.time != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Text(
                        text = stop.caption.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = androidx.compose.ui.unit.TextUnit(0.8f, androidx.compose.ui.unit.TextUnitType.Sp),
                            fontSize = 9.sp
                        ),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
/** Подзаголовок группы (mono, uppercase, letter-spacing) + опц. trailing справа. */
@Composable
private fun GroupHead(
    text: String,
    modifier: Modifier = Modifier,
    topPadding: androidx.compose.ui.unit.Dp = 22.dp,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = topPadding, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = androidx.compose.ui.unit.TextUnit(1.4f, androidx.compose.ui.unit.TextUnitType.Sp)
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        trailing?.invoke()
    }
}

/** Белая карточка-контейнер (как MCard в референсе). */
@Composable
private fun MCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(elevation = 2.dp, shape = Shapes.medium)
            .clip(Shapes.medium)
            .background(color = MaterialTheme.colorScheme.secondary, shape = Shapes.medium),
        content = content
    )
}

/** Степпер количества секций: [−] N [+] (как Stepper в референсе). */
@Composable
private fun SectionStepper(
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepperButton(symbol = "−", accent = false, enabled = value > 1, onClick = onMinus)
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = com.z_company.core.ui.theme.MonoFont,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        StepperButton(symbol = "+", accent = true, enabled = true, onClick = onPlus)
    }
}

@Composable
private fun StepperButton(
    symbol: String,
    accent: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondary)
            .then(
                if (enabled) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (!enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.primary
        )
    }
}

/** Строка показаний: иконка-чип + название + единица справа + Принял → Сдал. */
@Composable
private fun ReadingRow(
    icon: Int,
    label: String,
    unit: String,
    accepted: String,
    delivered: String,
    onAccepted: (String) -> Unit,
    onDelivered: (String) -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit,
    dataTextStyle: androidx.compose.ui.text.TextStyle,
    noValueColor: Color,
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceBright),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W500),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = unit,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = com.z_company.core.ui.theme.MonoFont,
                    fontWeight = FontWeight.W600
                ),
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextFieldApp(
                modifier = Modifier.weight(1f),
                value = accepted,
                textStyle = dataTextStyle,
                fieldElevation = 0.dp,
                borderColor = borderColor,
                colorBackgroundEmptyField = Color.Transparent,
                colorBackgroundNotEmptyField = Color.Transparent,
                placeholder = {
                    Text(
                        text = "Принял",
                        style = LocalTextStyle.current.copy(fontWeight = FontWeight.Light),
                        color = noValueColor, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                },
                onValueChange = onAccepted,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Decimal),
                keyboardActions = KeyboardActions(onNext = { onNext() }),
                singleLine = true,
                shape = Shapes.medium,
            )
            Text(
                text = "→",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            )
            OutlinedTextFieldApp(
                modifier = Modifier.weight(1f),
                value = delivered,
                textStyle = dataTextStyle,
                fieldElevation = 0.dp,
                borderColor = borderColor,
                colorBackgroundEmptyField = Color.Transparent,
                colorBackgroundNotEmptyField = Color.Transparent,
                placeholder = {
                    Text(
                        text = "Сдал",
                        style = LocalTextStyle.current.copy(fontWeight = FontWeight.Light),
                        color = noValueColor
                    )
                },
                onValueChange = onDelivered,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Decimal),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                singleLine = true,
                shape = Shapes.medium,
            )
        }
    }
}
