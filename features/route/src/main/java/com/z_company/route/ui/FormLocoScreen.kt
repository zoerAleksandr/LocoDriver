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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
    onSettingsClick: () -> Unit = {}
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
    formUiState.confirmDeleteDieselSectionId?.let { sectionId ->
        // sectionId захватываем в локальную переменную — иначе AppBottomSheet
        // вызывает onDismissRequest() ПЕРЕД action.onClick(), что сбрасывает
        // confirmDeleteDieselSectionId в null, и confirmDeleteDieselSection()
        // читает null → ничего не удаляется.
        val confirmSheetState = androidx.compose.material3.rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
        com.z_company.route.component.AppBottomSheet(
            onDismissRequest = { viewModel.cancelDeleteDieselSection() },
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
            onDismissRequest = { viewModel.cancelDeleteElectricSection() },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text(
                        text = "Локомотив",
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
                            topLevelFocusManager.clearFocus()
                            onLocoSaved()
                        },
                    ) {
                        Text(
                            text = "‹",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
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
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Группа-заголовок + название тяги
                            Column {
                                Text(
                                    text = "ОСНОВНЫЕ ДАННЫЕ",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                AnimatedContent(targetState = currentType, label = "") {
                                    val text = if (it == LocoType.ELECTRIC) LocoType.ELECTRIC.text
                                    else LocoType.DIESEL.text
                                    Text(
                                        text = text,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
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

                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            ExposedDropdownMenuBox(
                                modifier = Modifier.weight(1f),
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
                                    placeholder = {
                                        Text(
                                            text = "Серия",
                                            style = LocalTextStyle.current.copy(
                                                fontWeight = FontWeight.Light
                                            ),
                                            color = noValueColor
                                        )
                                    },
                                    textStyle = dataTextStyle,
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

                            OutlinedTextFieldApp(
                                modifier = Modifier
                                    .weight(1f),
                                value = locomotive.number ?: "",
                                textStyle = dataTextStyle,
                                placeholder = {
                                    Text(
                                        text = "Номер",
                                        style = LocalTextStyle.current.copy(
                                            fontWeight = FontWeight.Light
                                        ),
                                        color = noValueColor
                                    )
                                },
                                onValueChange = { onNumberChanged(it) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                    }
                                ),
                                singleLine = true,
                            )
                        }

                    }

                    // время
                    item {
                        CollapsibleSection(
                            modifier = Modifier.padding(top = 12.dp),
                            title = "Время",
                            expanded = formUiState.isShowTime,
                            onToggle = viewModel::toggleTime,
                            icon = R.drawable.schedule_24px
                        ) {
                                var showStartAcceptedDatePicker by remember {
                                    mutableStateOf(false)
                                }

                                if (showStartAcceptedDatePicker) {
                                    AppDateTimePicker(
                                        title = "Начало приемки",
                                        onDateTimeSelected = { timestamp ->
                                            onStartAcceptedTimeChanged(timestamp)
                                        },
                                        onDismiss = { showStartAcceptedDatePicker = false },
                                        startDateTime = locomotive.timeStartOfAcceptance
                                            ?: Calendar.getInstance().timeInMillis,
                                        timeZoneStr = displayTz,
                                        recentTimes = sharedPrefs.getRecentTimes("time_start_acceptance"),
                                        onRecentTimeSaved = { sharedPrefs.addRecentTime("time_start_acceptance", it) }
                                    )
                                }

                                var showEndAcceptedDatePicker by remember {
                                    mutableStateOf(false)
                                }

                                if (showEndAcceptedDatePicker) {
                                    AppDateTimePicker(
                                        title = "Окончание приемки",
                                        onDateTimeSelected = { timestamp ->
                                            onEndAcceptedTimeChanged(timestamp)
                                        },
                                        onDismiss = { showEndAcceptedDatePicker = false },
                                        startDateTime = locomotive.timeEndOfAcceptance
                                            ?: Calendar.getInstance().timeInMillis,
                                        timeZoneStr = displayTz,
                                        recentTimes = sharedPrefs.getRecentTimes("time_end_acceptance"),
                                        onRecentTimeSaved = { sharedPrefs.addRecentTime("time_end_acceptance", it) }
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Приёмка",
                                        style = subTitleTextStyle
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val animatedBackgroundColorsStartAcceptance by animateColorAsState(
                                            targetValue = if (locomotive.timeStartOfAcceptance == null) MaterialTheme.colorScheme.surface
                                            else MaterialTheme.colorScheme.secondary,
                                            animationSpec = tween(
                                                durationMillis = 200,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                        val animatedBackgroundColorsEndAcceptance by animateColorAsState(
                                            targetValue = if (locomotive.timeEndOfAcceptance == null) MaterialTheme.colorScheme.surface
                                            else MaterialTheme.colorScheme.secondary,
                                            animationSpec = tween(
                                                durationMillis = 200,
                                                easing = FastOutSlowInEasing
                                            )
                                        )

                                        Box(
                                            modifier = Modifier
                                                .shadow(elevation = 1.dp, shape = Shapes.medium)
                                                .background(
                                                    color = animatedBackgroundColorsStartAcceptance,
                                                    shape = Shapes.medium
                                                )
                                                .weight(1f)
                                                .combinedClickable(
                                                    onClick = {
                                                        showStartAcceptedDatePicker = true
                                                    },
                                                    onLongClick = {
                                                        locomotive.timeStartOfAcceptance?.let {
                                                            showBottomSheetRemoveTimeStartAccepted =
                                                                true
                                                        }
                                                    }
                                                )
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                        ) {
                                            val dateStartText =
                                                locomotive.timeStartOfAcceptance?.let {
                                                    dateAndTimeConverter?.getDateMiniAndTime(it)
                                                } ?: "Начало"

                                            val color = locomotive.timeStartOfAcceptance?.let {
                                                MaterialTheme.colorScheme.primary
                                            } ?: noValueColor

                                            val style = locomotive.timeStartOfAcceptance?.let {
                                                dataTextStyle
                                            } ?: LocalTextStyle.current.copy(
                                                fontWeight = FontWeight.Light
                                            )

                                            Text(
                                                text = dateStartText,
                                                style = style,
                                                color = color,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .shadow(elevation = 1.dp, shape = Shapes.medium)
                                                .background(
                                                    color = animatedBackgroundColorsEndAcceptance,
                                                    shape = Shapes.medium
                                                )
                                                .weight(1f)
                                                .combinedClickable(
                                                    onClick = {
                                                        showEndAcceptedDatePicker = true
                                                    },
                                                    onLongClick = {
                                                        locomotive.timeEndOfAcceptance?.let {
                                                            showBottomSheetRemoveTimeEndAccepted =
                                                                true
                                                        }
                                                    }
                                                )
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                        ) {
                                            val dateEndText = locomotive.timeEndOfAcceptance?.let {
                                                dateAndTimeConverter?.getDateMiniAndTime(it)
                                            } ?: "Окончание"

                                            val color = locomotive.timeStartOfAcceptance?.let {
                                                MaterialTheme.colorScheme.primary
                                            } ?: noValueColor

                                            val style = locomotive.timeStartOfAcceptance?.let {
                                                dataTextStyle
                                            } ?: LocalTextStyle.current.copy(
                                                fontWeight = FontWeight.Light
                                            )

                                            Text(
                                                text = dateEndText,
                                                style = style,
                                                color = color,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                var showStartDeliveryDatePicker by remember {
                                    mutableStateOf(false)
                                }

                                if (showStartDeliveryDatePicker) {
                                    AppDateTimePicker(
                                        title = "Начало сдачи",
                                        onDateTimeSelected = { timestamp ->
                                            onStartDeliveryTimeChanged(timestamp)
                                        },
                                        onDismiss = { showStartDeliveryDatePicker = false },
                                        startDateTime = locomotive.timeStartOfDelivery
                                            ?: Calendar.getInstance().timeInMillis,
                                        timeZoneStr = displayTz,
                                        recentTimes = sharedPrefs.getRecentTimes("time_start_delivery"),
                                        onRecentTimeSaved = { sharedPrefs.addRecentTime("time_start_delivery", it) }
                                    )
                                }

                                var showEndDeliveryDatePicker by remember {
                                    mutableStateOf(false)
                                }

                                if (showEndDeliveryDatePicker) {
                                    AppDateTimePicker(
                                        title = "Окончание сдачи",
                                        onDateTimeSelected = { timestamp ->
                                            onEndDeliveryTimeChanged(timestamp)
                                        },
                                        onDismiss = { showEndDeliveryDatePicker = false },
                                        startDateTime = locomotive.timeEndOfDelivery
                                            ?: Calendar.getInstance().timeInMillis,
                                        timeZoneStr = displayTz,
                                        recentTimes = sharedPrefs.getRecentTimes("time_end_delivery"),
                                        onRecentTimeSaved = { sharedPrefs.addRecentTime("time_end_delivery", it) }
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .padding(top = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Сдача",
                                        style = subTitleTextStyle
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val animatedBackgroundColorsStartDelivery by animateColorAsState(
                                            targetValue = if (locomotive.timeStartOfDelivery == null) MaterialTheme.colorScheme.surface
                                            else MaterialTheme.colorScheme.secondary,
                                            animationSpec = tween(
                                                durationMillis = 200,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                        val animatedBackgroundColorsEndDelivery by animateColorAsState(
                                            targetValue = if (locomotive.timeEndOfDelivery == null) MaterialTheme.colorScheme.surface
                                            else MaterialTheme.colorScheme.secondary,
                                            animationSpec = tween(
                                                durationMillis = 200,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                        Box(
                                            modifier = Modifier
                                                .shadow(elevation = 1.dp, shape = Shapes.medium)
                                                .background(
                                                    color = animatedBackgroundColorsStartDelivery,
                                                    shape = Shapes.medium
                                                )
                                                .weight(1f)
                                                .combinedClickable(
                                                    onClick = {
                                                        showStartDeliveryDatePicker = true
                                                    },
                                                    onLongClick = {
                                                        locomotive.timeStartOfDelivery?.let {
                                                            showBottomSheetRemoveTimeStartDelivery =
                                                                true
                                                        }
                                                    }
                                                )
                                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                        ) {
                                            val dateStartText =
                                                locomotive.timeStartOfDelivery?.let {
                                                    dateAndTimeConverter?.getDateMiniAndTime(it)
                                                } ?: "Начало"

                                            val color = locomotive.timeStartOfDelivery?.let {
                                                MaterialTheme.colorScheme.primary
                                            } ?: noValueColor

                                            val style = locomotive.timeStartOfDelivery?.let {
                                                dataTextStyle
                                            } ?: LocalTextStyle.current.copy(
                                                fontWeight = FontWeight.Light
                                            )

                                            Text(
                                                text = dateStartText,
                                                style = style,
                                                color = color,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .shadow(elevation = 1.dp, shape = Shapes.medium)
                                                .background(
                                                    color = animatedBackgroundColorsEndDelivery,
                                                    shape = Shapes.medium
                                                )
                                                .weight(1f)
                                                .combinedClickable(
                                                    onClick = {
                                                        showEndDeliveryDatePicker = true
                                                    },
                                                    onLongClick = {
                                                        locomotive.timeEndOfDelivery?.let {
                                                            showBottomSheetRemoveTimeEndDelivery =
                                                                true
                                                        }
                                                    }
                                                )
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                        ) {
                                            val dateEndText = locomotive.timeEndOfDelivery?.let {
                                                dateAndTimeConverter?.getDateMiniAndTime(it)
                                            } ?: "Окончание"

                                            val color = locomotive.timeEndOfDelivery?.let {
                                                MaterialTheme.colorScheme.primary
                                            } ?: noValueColor

                                            val style = locomotive.timeEndOfDelivery?.let {
                                                dataTextStyle
                                            } ?: LocalTextStyle.current.copy(
                                                fontWeight = FontWeight.Light
                                            )

                                            Text(
                                                text = dateEndText,
                                                style = style,
                                                color = color,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    // отопление
                    if (userSettings?.isShowLocoHeating != false) {
                    item {
                        val accepted = formUiState.heatingAcceptedText
                        val delivered = formUiState.heatingDeliveryText
                        val heatingResult: Double? = delivered.toDoubleOrNull() - accepted.toDoubleOrNull()
                        CollapsibleSection(
                            title = "Отопление",
                            expanded = formUiState.isShowHeatingCounter,
                            onToggle = viewModel::toggleHeatingCounter,
                            icon = R.drawable.nest_farsight_heat_24px,
                            summaryText = heatingResult?.let { "Расход: ${rounding(it, 2)?.str() ?: it.str()}" }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextFieldApp(
                                    modifier = Modifier.weight(1f),
                                    value = accepted,
                                    textStyle = dataTextStyle,
                                    placeholder = {
                                        Text(
                                            text = "Принял",
                                            style = LocalTextStyle.current.copy(fontWeight = FontWeight.Light),
                                            color = noValueColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    onValueChange = { viewModel.setHeatingCounterAccepted(it) },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Decimal),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                                    singleLine = true,
                                    shape = Shapes.medium,
                                )
                                OutlinedTextFieldApp(
                                    modifier = Modifier.weight(1f),
                                    value = delivered,
                                    textStyle = dataTextStyle,
                                    placeholder = {
                                        Text(
                                            text = "Сдал",
                                            style = LocalTextStyle.current.copy(fontWeight = FontWeight.Light),
                                            color = noValueColor
                                        )
                                    },
                                    onValueChange = { viewModel.setHeatingCounterDelivery(it) },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Decimal),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    singleLine = true,
                                    shape = Shapes.medium,
                                )
                            }
                            AnimatedVisibility(heatingResult != null) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                    Text(
                                        modifier = Modifier.padding(end = 16.dp),
                                        text = rounding(heatingResult, 2)?.str() ?: heatingResult.str(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    }

                    // собственные нужды
                    if (userSettings?.isShowLocoAuxiliary != false) {
                    item {
                        val auxAccepted = formUiState.auxiliaryAcceptedText
                        val auxDelivered = formUiState.auxiliaryDeliveryText
                        val auxiliaryResult: Double? = auxDelivered.toDoubleOrNull() - auxAccepted.toDoubleOrNull()
                        CollapsibleSection(
                            title = "Собственные нужды",
                            expanded = formUiState.isShowAuxiliaryCounter,
                            onToggle = viewModel::toggleAuxiliaryCounter,
                            icon = R.drawable.electric_bolt_24px,
                            summaryText = auxiliaryResult?.let { "Расход: ${rounding(it, 2)?.str() ?: it.str()}" }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextFieldApp(
                                    modifier = Modifier.weight(1f),
                                    value = auxAccepted,
                                    textStyle = dataTextStyle,
                                    placeholder = {
                                        Text(
                                            text = "Принял",
                                            style = LocalTextStyle.current.copy(fontWeight = FontWeight.Light),
                                            color = noValueColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    onValueChange = { viewModel.setAuxiliaryCounterAccepted(it) },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Decimal),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                                    singleLine = true,
                                    shape = Shapes.medium,
                                )
                                OutlinedTextFieldApp(
                                    modifier = Modifier.weight(1f),
                                    value = auxDelivered,
                                    textStyle = dataTextStyle,
                                    placeholder = {
                                        Text(
                                            text = "Сдал",
                                            style = LocalTextStyle.current.copy(fontWeight = FontWeight.Light),
                                            color = noValueColor
                                        )
                                    },
                                    onValueChange = { viewModel.setAuxiliaryCounterDelivery(it) },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Decimal),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    singleLine = true,
                                    shape = Shapes.medium,
                                )
                            }
                            AnimatedVisibility(auxiliaryResult != null) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                    Text(
                                        modifier = Modifier.padding(end = 16.dp),
                                        text = rounding(auxiliaryResult, 2)?.str() ?: auxiliaryResult.str(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    }

                    // Итоги
                    if (userSettings?.isShowLocoStatistics != false) {
                    item {
                        CollapsibleSection(
                            title = "Статистика",
                            expanded = formUiState.isShowResults,
                            onToggle = viewModel::toggleResults,
                            icon = R.drawable.finance_24px
                        ) {
                            when (currentLoco.type) {
                                LocoType.ELECTRIC -> {
                                    ElectricStatisticsSection(
                                        electricSectionListState = electricSectionListState,
                                        locomotive = locomotive,
                                        isShowOtherCurrent = formUiState.isShowOtherCurrent,
                                        onSettingsClick = onSettingsClick
                                    )
                                }
                                LocoType.DIESEL -> {
                                    DieselStatisticsSection(
                                        dieselSectionListState = dieselSectionListState,
                                        locomotive = locomotive,
                                        onSettingsClick = onSettingsClick
                                    )
                                }
                            }
                        }
                    }
                    }

                    // Норма
                    if (userSettings?.isShowLocoNorma != false) {
                    item {
                        CollapsibleSection(
                            title = "Норма",
                            expanded = formUiState.isShowNorma,
                            onToggle = viewModel::toggleNorma,
                            icon = R.drawable.weight_24px
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                when (locomotive.type) {
                                    LocoType.ELECTRIC -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            OutlinedTextFieldApp(
                                                modifier = Modifier.weight(1f),
                                                value = formUiState.norma1Text,
                                                placeholder = {
                                                    Text(
                                                        text = "Ток 1",
                                                        style = LocalTextStyle.current.copy(
                                                            fontWeight = FontWeight.Light
                                                        ),
                                                        color = noValueColor
                                                    )
                                                },
                                                textStyle = dataTextStyle,
                                                onValueChange = { viewModel.setNormaElectricCurrent1(it) },
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Decimal,
                                                    imeAction = ImeAction.Done
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
                                                            style = LocalTextStyle.current.copy(
                                                                fontWeight = FontWeight.Light
                                                            ),
                                                            color = noValueColor
                                                        )
                                                    },
                                                    textStyle = dataTextStyle,
                                                    onValueChange = { viewModel.setNormaElectricCurrent2(it) },
                                                    keyboardOptions = KeyboardOptions(
                                                        keyboardType = KeyboardType.Decimal,
                                                        imeAction = ImeAction.Done
                                                    ),
                                                    singleLine = true
                                                )
                                            }
                                        }
                                    }
                                    LocoType.DIESEL -> {
                                        val norma = locomotive.normaDiesel ?: ""
                                        OutlinedTextFieldApp(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = norma,
                                            textStyle = dataTextStyle,
                                            suffix = {
                                                Text(
                                                    text = "Кг.",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            onValueChange = { viewModel.setNormaDiesel(it) },
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Decimal,
                                                imeAction = ImeAction.Done
                                            ),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }

                    when (locomotive.type.name) {
                        LocoType.DIESEL.name -> {
                            dieselSectionListState?.let {
                                itemsIndexed(
                                    items = dieselSectionListState,
                                    key = { _, item -> item.sectionId }
                                ) { index, item ->
                                    Column(horizontalAlignment = Alignment.End) {
                                        CustomDivider(orientation = Orientation.Horizontal)

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
                                            changeIsKiloMode = viewModel::toggleIsKiloMode
                                        )
                                        if (index == dieselSectionListState.lastIndex) {
                                            CustomDivider(orientation = Orientation.Horizontal)
                                        }
                                    }
                                }
                                if (dieselSectionListState.size > 1) {
                                    item(key = "diesel_total") {
                                        val totalFuelLiters = dieselSectionListState.mapNotNull { sec ->
                                            CalculationEnergy.getTotalFuelConsumption(
                                                accepted = sec.accepted.data?.toDoubleOrNull(),
                                                delivery = sec.delivery.data?.toDoubleOrNull(),
                                                refuel = sec.refuel.data?.toDoubleOrNull()
                                            )
                                        }.takeIf { it.isNotEmpty() }?.sum()

                                        val totalFuelKilo = dieselSectionListState.mapNotNull { sec ->
                                            val accKilo = sec.accepted.data?.toDoubleOrNull()
                                                .times(sec.coefficient.data?.toDoubleOrNull())
                                            val delKilo = sec.delivery.data?.toDoubleOrNull()
                                                .times(sec.coefficient.data?.toDoubleOrNull())
                                            CalculationEnergy.getTotalFuelInKiloConsumption(
                                                acceptedInKilo = accKilo,
                                                deliveryInKilo = delKilo,
                                                refuelInKilo = sec.refuelInKilo.data?.toDoubleOrNull()
                                            )
                                        }.takeIf { it.isNotEmpty() }?.sum()

                                        if (totalFuelLiters != null || totalFuelKilo != null) {
                                            FlowRow(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(
                                                    text = "Итого:",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.W600
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                totalFuelLiters?.let {
                                                    Text(
                                                        text = "${rounding(it, 2).str()} л.",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                totalFuelKilo?.let {
                                                    Text(
                                                        text = "${rounding(it, 2).str()} кг.",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        LocoType.ELECTRIC.name -> {
                            electricSectionListState?.let {
                                itemsIndexed(
                                    items = electricSectionListState,
                                    key = { _, item -> item.sectionId }
                                ) { index, item ->
                                    Column(horizontalAlignment = Alignment.End) {
                                        CustomDivider(orientation = Orientation.Horizontal)

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
                                            showOtherCurrent = formUiState.isShowOtherCurrent
                                        )

                                        if (index == electricSectionListState.lastIndex) {
                                            CustomDivider(orientation = Orientation.Horizontal)
                                        }
                                    }
                                }
                                if (electricSectionListState.size > 1) {
                                    item(key = "electric_total") {
                                        fun maxPrecision(vararg texts: String): Int {
                                            return texts.maxOf { s ->
                                                val dot = s.indexOf('.')
                                                if (dot < 0) 0 else s.length - dot - 1
                                            }
                                        }

                                        val energyPrecision = electricSectionListState.maxOf { sec ->
                                            maxPrecision(sec.accepted.data ?: "", sec.delivery.data ?: "")
                                        }
                                        val recoveryPrecision = electricSectionListState.maxOf { sec ->
                                            maxPrecision(sec.recoveryAccepted.data ?: "", sec.recoveryDelivery.data ?: "")
                                        }

                                        val totalEnergy = electricSectionListState.mapNotNull { sec ->
                                            CalculationEnergy.getTotalEnergyConsumption(
                                                accepted = sec.accepted.data?.toDoubleOrNull(),
                                                delivery = sec.delivery.data?.toDoubleOrNull()
                                            )
                                        }.takeIf { it.isNotEmpty() }?.sum()?.let {
                                            CalculationEnergy.rounding(it, energyPrecision)
                                        }

                                        val totalRecovery = electricSectionListState.mapNotNull { sec ->
                                            CalculationEnergy.getTotalEnergyConsumption(
                                                accepted = sec.recoveryAccepted.data?.toDoubleOrNull(),
                                                delivery = sec.recoveryDelivery.data?.toDoubleOrNull()
                                            )
                                        }.takeIf { it.isNotEmpty() }?.sum()?.let {
                                            CalculationEnergy.rounding(it, recoveryPrecision)
                                        }

                                        if (totalEnergy != null || totalRecovery != null) {
                                            FlowRow(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(
                                                    text = "Итого:",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.W600
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                totalEnergy?.let {
                                                    Text(
                                                        text = "расход ${it.str()}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                totalRecovery?.let {
                                                    Text(
                                                        text = "рекуперация ${it.str()}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            shape = Shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.secondary
                            ),
                            elevation = ButtonDefaults.elevatedButtonElevation(
                                defaultElevation = 3.dp,
                                pressedElevation = 0.dp
                            ),
                            onClick = {
                                when (locomotive.type.name) {
                                    LocoType.DIESEL.name -> addingSectionDiesel()
                                    LocoType.ELECTRIC.name -> addingSectionElectric()
                                }
                                scope.launch {
                                    val countItems = scrollState.layoutInfo.totalItemsCount
                                    scrollState.animateScrollToItem(countItems)
                                }
                            }
                        ) {
                            Text(
                                text = "Добавить секцию",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )

                        }
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}