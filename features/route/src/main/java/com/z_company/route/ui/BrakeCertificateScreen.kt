package com.z_company.route.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.domain.brake.BrakeCategory
import com.z_company.domain.brake.BrakeCertificateCalculator
import com.z_company.domain.brake.BrakeCertificateResult
import com.z_company.domain.brake.BrakeStatusResolver
import com.z_company.domain.brake.StatusLevel
import com.z_company.domain.brake.BrakeDensity
import com.z_company.domain.brake.BrakeNorms
import com.z_company.domain.brake.CompositeShare
import com.z_company.domain.brake.DensityLocoGroup
import com.z_company.domain.brake.AxleCheck
import com.z_company.domain.brake.AxleRow
import com.z_company.domain.brake.BrakeTable
import com.z_company.domain.brake.AxleSumCheck
import com.z_company.domain.brake.GradeClass
import com.z_company.domain.brake.InstructionRef
import com.z_company.domain.brake.LocoShare
import com.z_company.domain.brake.SlopeStep
import com.z_company.domain.brake.SpeedLimit
import com.z_company.domain.brake.StopClass
import com.z_company.domain.brake.TrainGroup
import com.z_company.domain.brake.BrakeStations
import com.z_company.domain.brake.TimeOfDayInput
import com.z_company.domain.brake.TurnaroundComposition
import com.z_company.domain.brake.WagonChange
import com.z_company.domain.brake.TurnaroundDensityCheck
import com.z_company.domain.brake.TurnaroundReference
import com.z_company.domain.brake.TurnaroundEntry
import com.z_company.domain.brake.TurnaroundPick
import com.z_company.domain.brake.TurnaroundPlanner
import com.z_company.domain.brake.TurnaroundTarget
import com.z_company.domain.util.toFiniteDoubleOrNull
import com.z_company.route.ui.settings.SettingsPickerOption
import com.z_company.route.ui.settings.SettingsPickerSheet
import com.z_company.route.viewmodel.BrakeCertificateUiState
import com.z_company.route.viewmodel.BrakeCertificateViewModel
import com.z_company.route.viewmodel.FillPick
import com.z_company.route.viewmodel.PressureField
import com.z_company.route.viewmodel.effectiveDensityGroup
import com.z_company.route.viewmodel.pressureField
import com.z_company.route.viewmodel.DensityTimerState
import com.z_company.route.viewmodel.TimerKind
import com.z_company.route.viewmodel.effectiveRows
import com.z_company.route.viewmodel.grade
import com.z_company.route.viewmodel.manualInfo
import com.z_company.route.viewmodel.toInput
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface BrakeSheet {
    data object Category : BrakeSheet
    data object ManualNorm : BrakeSheet
    data object DensityGroup : BrakeSheet
    data object CustomVolume : BrakeSheet
    data object Other : BrakeSheet
    data object RowPressure : BrakeSheet
    data class Turnaround(val entryId: String) : BrakeSheet
    data class ManualDensity(val kind: TimerKind) : BrakeSheet
    data class Help(val key: String) : BrakeSheet
}

private val TABS = listOf("Бланк", "Расчёт", "Замеры", "Справочник")
/** Строки «Тормозное нажатие на ось, тс» по образцу формы ВУ-45 (808-2022 ПКБ ЦВ, прил. М): 27 значений. */
private val PRINTED = BrakeNorms.blankAxlePressures
private val OTHER_VALUES = listOf(1.25, 2.5, 3.5, 5.0, 6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 18.0, 21.0)

/** Число для показа: без потери значащих дробных разрядов (до [digits] знаков), без хвостовых нулей, запятая. */
private fun fmt(v: Double, digits: Int = 2): String {
    val factor = Math.pow(10.0, digits.toDouble())
    val r = Math.round(v * factor) / factor
    val s = if (r % 1.0 == 0.0) r.toLong().toString() else r.toString()
    return s.replace('.', ',')
}

private fun fmtP(p: Double): String = BrakeCertificateViewModel.formatPressure(p)

/** Секундомер плотности - только секунды с десятыми («75,3»), без минут. */
private fun formatTimer(ms: Long): String {
    val tenths = ms / 100
    return "%d,%d".format(tenths / 10, tenths % 10)
}

private fun hhmm(ms: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

private fun axlesForNorm(state: BrakeCertificateUiState): Int =
    state.totalAxlesText.trim().toIntOrNull() ?: state.effectiveRows().sumOf { it.axlesText.trim().toIntOrNull() ?: 0 }

private fun BrakeCertificateUiState.timerOf(k: TimerKind): DensityTimerState = when (k) {
    TimerKind.II -> timerII
    TimerKind.IV -> timerIV
    TimerKind.ROUTE -> timerRoute
}

/** Экран «Справка ВУ-45» (SCREEN_SPECS §26) по дизайну: вкладки «Расчёт», «Бланк», «Замеры», «Справочник». */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrakeCertificateScreen(
    state: BrakeCertificateUiState,
    onBack: () -> Unit,
    viewModel: BrakeCertificateViewModel,
) {
    // по умолчанию открывается «Бланк» (индекс 0); запомненная вкладка не используется - состояние живёт только на время экрана
    var tab by remember { mutableIntStateOf(0) }
    // раскрытая карточка «Справочника» (индекс; -1 - все свёрнуты); «Недостаток нажатия» открывается ссылкой из красного блока
    var refOpen by remember { mutableIntStateOf(-1) }
    var side by rememberSaveable { mutableIntStateOf(0) }
    var measureKind by rememberSaveable { mutableStateOf(TimerKind.II.name) }
    var sheet by remember { mutableStateOf<BrakeSheet?>(null) }
    var openCustomVolume by remember { mutableStateOf(false) }
    val now by viewModel.nowMs.collectAsState()
    val t = vuColors()

    val category = BrakeNorms.category(state.categoryId) ?: BrakeNorms.categories.first()
    val input = remember(state) { state.toInput() }
    val result = remember(input) { input?.let(BrakeCertificateCalculator::calculate) }
    val turnaroundApplies = TurnaroundPlanner.applies(category)

    // Экран не гаснет, пока идёт замер плотности.
    val anyRunning = state.timerII.running || state.timerIV.running || state.timerRoute.running
    val view = LocalView.current
    DisposableEffect(anyRunning) {
        view.keepScreenOn = anyRunning
        onDispose { view.keepScreenOn = false }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        com.z_company.route.component.AppBottomSheet(
            onDismissRequest = { confirmDelete = false },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
            headerContent = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Удалить справку?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                    Text("Справка будет удалена с этого устройства и с сервера", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp))
                }
            },
            actions = listOf(com.z_company.route.component.BottomSheetAction(text = "Да, удалить") { confirmDelete = false; viewModel.deleteCertificate(onBack) }),
        )
    }
    // «Записано в оборот: …» с действием «К отметкам оборота»
    LaunchedEffect(state.writtenNotice) {
        val n = state.writtenNotice ?: return@LaunchedEffect
        val r = snackbarHostState.showSnackbar(message = "Записано в оборот: ${n.label}", actionLabel = "К отметкам оборота", duration = androidx.compose.material3.SnackbarDuration.Long)
        viewModel.consumeWrittenNotice()
        if (r == SnackbarResult.ActionPerformed) { tab = 0; side = 1 }
    }
    Scaffold(
        containerColor = t.bg,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Справка ВУ-45", style = MaterialTheme.typography.titleMedium, color = t.text) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(com.z_company.core.R.drawable.ic_arrow_back), contentDescription = "Назад", tint = t.text)
                    }
                },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(painter = painterResource(com.z_company.route.R.drawable.delete_24px), contentDescription = "Удалить справку", tint = t.text)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)) { VuTabs(TABS, tab, { tab = it }) }
            when (tab) {
                1 -> CalcTab(
                    state, category, result, viewModel,
                    onSheet = { sheet = it },
                    onOpenBlank = { tab = 0; side = 0 },
                    onOpenReference = { refOpen = brakeReference.indexOfFirst { it.title == REF_DEFICIT_TITLE }; tab = 3 },
                )
                0 -> BlankTab(
                    state, category, result, viewModel, now, side, turnaroundApplies,
                    onSide = { side = it },
                    onSheet = { sheet = it },
                    onOpenMeasure = { tab = 2 },
                )
                2 -> MeasureTab(
                    state, category, viewModel, now,
                    kind = TimerKind.valueOf(measureKind), onKind = { measureKind = it.name },
                    onGroupClick = { sheet = BrakeSheet.DensityGroup },
                    onHelp = { sheet = BrakeSheet.Help(it) },
                    onBackToTurnaround = { tab = 0; side = 1 },
                )
                else -> ReferenceTab(state, category, open = refOpen, onOpen = { refOpen = it }, onOpenMeasure = { tab = 2 })
            }
        }
    }

    when (val s = sheet) {
        BrakeSheet.Category -> SettingsPickerSheet(
            title = "Категория поезда",
            hint = InstructionRef.table("1") + ": единое наименьшее тормозное нажатие на каждые 100 тс веса поезда (состава) и максимальная допускаемая скорость",
            onDismiss = { sheet = null },
            options = BrakeNorms.categories.map { c ->
                SettingsPickerOption(
                    label = c.title,
                    sub = "табл. III.1 прил. 2 Правил ТО тормозного оборудования, строка ${c.tableRow}: ${c.norm} тс на 100 тс веса, до ${c.vFlat} км/ч",
                    selected = c.id == state.categoryId,
                    onSelect = { viewModel.setCategory(c.id) },
                )
            },
        )
        BrakeSheet.ManualNorm -> SettingsPickerSheet(
            title = "Ручные тормозные оси на 100 тс веса",
            hint = InstructionRef.annotate("«Авто» берёт значение из табл. III.4 по руководящему уклону, но не менее единого наименьшего 0,6 (п. 45). Ручное значение запоминается и от уклона не зависит."),
            onDismiss = { sheet = null },
            options = buildList {
                add(SettingsPickerOption(label = "Авто (по уклону)", sub = InstructionRef.annotate(BrakeNorms.manualInfo(state.slope, null).text), selected = state.manualOverride == null, onSelect = { viewModel.setManualOverride(null) }))
                BrakeNorms.ManualBrakeNorm.values().forEach { n ->
                    add(SettingsPickerOption(label = n.title, selected = n == state.manualOverride, onSelect = { viewModel.setManualOverride(n) }))
                }
            },
        )
        BrakeSheet.DensityGroup -> SettingsPickerSheet(
            title = "Объём ГР",
            hint = "Строка табл. IV.1 по общему объёму главных резервуаров локомотива",
            onDismiss = { sheet = if (openCustomVolume) BrakeSheet.CustomVolume else null; openCustomVolume = false },
            options = buildList {
                DensityLocoGroup.values().forEach { g ->
                    add(SettingsPickerOption(label = g.title, sub = g.examplesText.ifBlank { null }, selected = state.customVolumeText.isBlank() && g == state.densityGroup, onSelect = { viewModel.setDensityGroup(g) }))
                }
                add(SettingsPickerOption(label = "Свой объём, л", sub = if (state.customVolumeText.isNotBlank()) BrakeDensity.volumeCaption(state.customVolumeText) else null, selected = state.customVolumeText.isNotBlank(), onSelect = { openCustomVolume = true }))
                add(SettingsPickerOption(label = "Не выбрана", selected = state.densityGroup == null && state.customVolumeText.isBlank(), onSelect = { viewModel.setDensityGroup(null) }))
            },
        )
        BrakeSheet.CustomVolume -> CustomVolumeSheet(state, viewModel, onDismiss = { sheet = null })
        BrakeSheet.Other -> OtherSheet(state, category, viewModel, onDismiss = { sheet = null })
        BrakeSheet.RowPressure -> RowSheet(state, viewModel, onDismiss = { sheet = null })
        is BrakeSheet.ManualDensity -> ManualDensitySheet(
            state, s.kind, viewModel, onDismiss = { sheet = null },
            onOpenMeasure = { measureKind = it.name; tab = 2; sheet = null },
        )
        is BrakeSheet.Turnaround -> TurnaroundSheet(
            state, s.entryId, viewModel,
            onDismiss = { sheet = null },
            onMeasure = {
                viewModel.setRouteTarget(s.entryId)
                measureKind = TimerKind.ROUTE.name
                sheet = null
                tab = 2
            },
        )
        is BrakeSheet.Help -> HelpSheet(s.key, state, category, result, onDismiss = { sheet = null })
        null -> Unit
    }
}

// ───────────────────────────── Расчёт ─────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalcTab(
    state: BrakeCertificateUiState,
    category: BrakeCategory,
    result: BrakeCertificateResult?,
    vm: BrakeCertificateViewModel,
    onSheet: (BrakeSheet) -> Unit,
    onOpenBlank: () -> Unit,
    onOpenReference: () -> Unit,
) {
    val t = vuColors()
    val calculated = BrakeStatusResolver.isCalculated(result)
    val low = calculated && result != null && !result.enough &&
        BrakeStatusResolver.resolve(result, true).level == StatusLevel.DANGER
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { VuSpeedBar(result) }
        Column(
            modifier = Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FillFromRoute(state, vm)
            val density = category.group != TrainGroup.PASSENGER
            val rt = category.manualBrakesRequired
            // 1. ДАННЫЕ
            VuBlock("Данные") {
                VuInput(
                    "Категория поезда", category.title.substringBefore(" / ").substringBefore(" ("), Modifier.fillMaxWidth(), sunk = true,
                    onClick = { onSheet(BrakeSheet.Category) }, onHelp = { onSheet(BrakeSheet.Help("category")) },
                )
                state.loadNote?.let { VuHint(it, Modifier.padding(horizontal = 4.dp)) }
                VuNormLine("Требуемое нажатие: ${category.norm} тс на 100 т веса")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VuInput("Вес состава, т", state.weightText, Modifier.weight(1f), onChange = vm::setWeight, unit = "т", placeholder = "0", sunk = true, onHelp = { onSheet(BrakeSheet.Help("weight")) })
                    VuInput("Всего осей", state.totalAxlesText, Modifier.weight(1f), onChange = vm::setTotalAxles, unit = "осей", placeholder = "0", sunk = true, keyboard = KeyboardType.Number, onHelp = { onSheet(BrakeSheet.Help("axles")) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (val pf = state.pressureField()) {
                        PressureField.Multiple -> VuInput(
                            "Нажатие на ось, тс", "см. в справке", Modifier.weight(1f),
                            onClick = onOpenBlank, sunk = true, onHelp = { onSheet(BrakeSheet.Help("row")) },
                        )
                        is PressureField.Single -> VuInput("Нажатие на ось, тс", pf.text, Modifier.weight(1f), onChange = vm::setPressureField, unit = "тс", placeholder = "0", error = low, sunk = true, onHelp = { onSheet(BrakeSheet.Help("row")) })
                        PressureField.Empty -> VuInput("Нажатие на ось, тс", state.avgPressureText, Modifier.weight(1f), onChange = vm::setPressureField, unit = "тс", placeholder = "0", error = low, sunk = true, onHelp = { onSheet(BrakeSheet.Help("row")) })
                    }
                    if (density) VuInput("Зарядное давление", state.chargePressureText, Modifier.weight(1f), onChange = vm::setChargePressure, unit = "кгс/см²", placeholder = "0", sunk = true, onHelp = { onSheet(BrakeSheet.Help("charge")) })
                }
                if (state.rowsMode) AxleSumWarning(state, vm)
                Box(Modifier.padding(start = 0.dp)) { VuBtn("Разное нажатие на вагонах ›", onClick = onOpenBlank, kind = VuBtnKind.Text, h = 40.dp) }
            }
            // 2. РАСЧЁТ ПЛОТНОСТИ
            if (density) VuBlock("Расчёт плотности") {
                DensityFields(state, vm, onGroup = { onSheet(BrakeSheet.DensityGroup) }, onHelp = onSheet)
            }
            // 3. РАСЧЁТ РТ
            if (rt) VuBlock("Расчёт РТ") {
                VuInput(
                    "Ручных на 100 т веса", BrakeNorms.manualFieldText(state.manualInfo), Modifier.fillMaxWidth(), sunk = true,
                    onClick = { onSheet(BrakeSheet.ManualNorm) }, onHelp = { onSheet(BrakeSheet.Help("manual_req")) },
                )
                // потребное число ручных осей - сразу, как требуемое нажатие и плотность; нет данных (вес) - ничего не пишем
                result?.requiredManualAxles?.let { n ->
                    VuNormLine("Требуется ручных тормозов: $n осей (${BrakeNorms.manualFieldText(state.manualInfo).substringBefore(" ·")} на 100 т веса)")
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Руководящий уклон, ‰", Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = t.muted)
                        VuHelp({ onSheet(BrakeSheet.Help("grade")) }, touch = 32.dp)
                    }
                    val ab = category.vFlatAutoblock != null && (state.grade == GradeClass.UP_TO_006 || state.grade == GradeClass.OVER_006_TO_010)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SlopeStep.values().forEach { step ->
                            VuChip(step.slopeText.removePrefix("0,0").removePrefix("до ").let { if (step == SlopeStep.S006) "≤ 6" else it.trimStart('0') }, state.slope == step, { vm.setSlope(step) }, Modifier.widthIn(min = 56.dp))
                        }
                    }
                }
            }
            // 4. ДОПОЛНИТЕЛЬНО
            VuBlock("Дополнительно") {
                CompositeRow(state, vm, onHelp = { onSheet(BrakeSheet.Help("composite")) })
                if (!rt) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Руководящий уклон, ‰", Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = t.muted)
                            VuHelp({ onSheet(BrakeSheet.Help("grade")) }, touch = 32.dp)
                        }
                        val ab = category.vFlatAutoblock != null && (state.grade == GradeClass.UP_TO_006 || state.grade == GradeClass.OVER_006_TO_010)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SlopeStep.values().forEach { step ->
                                VuChip(step.slopeText.removePrefix("0,0").removePrefix("до ").let { if (step == SlopeStep.S006) "≤ 6" else it.trimStart('0') }, state.slope == step, { vm.setSlope(step) }, Modifier.widthIn(min = 56.dp))
                            }
                        }
                    }
                }
                AutoblockRow(
                    state, applicable = category.vFlatAutoblock != null && (state.grade == GradeClass.UP_TO_006 || state.grade == GradeClass.OVER_006_TO_010),
                    onChange = vm::setAutoblock, onHelp = { onSheet(BrakeSheet.Help("autoblock")) },
                )
            }
            if (calculated) ResultCard(result, state, category, onOpenBlank)
            if (low && result != null && BrakeStatusResolver.resolve(result, true).level == StatusLevel.DANGER) DeficitBlock(result, category, onHelp = { onSheet(BrakeSheet.Help("deficit")) }, onOpenReference = onOpenReference)
        }
    }
}

/** Блок «Расчёта»: заголовок моно-капслоком над карточкой (стиль групп «Настроек»), поля внутри карточки. */
@Composable
private fun VuBlock(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VuTag(title, Modifier.padding(horizontal = 4.dp))
        VuCard {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

/** Доля вагонов с композиционными колодками (п. 31 прил. 2 Правил): то же состояние, что и в «Бланк» - «Другие данные». */
@Composable
private fun CompositeRow(state: BrakeCertificateUiState, vm: BrakeCertificateViewModel, onHelp: () -> Unit) {
    val t = vuColors()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Композиционные колодки", Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = t.muted)
            VuHelp(onHelp, touch = 32.dp)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VuChip("Нет", state.compositeShare == CompositeShare.NONE, { vm.setCompositeShare(CompositeShare.NONE) }, Modifier.widthIn(min = 56.dp))
            listOf("К-50" to CompositeShare.P50, "К-75" to CompositeShare.P75, "К-100" to CompositeShare.P100).forEach { (label, v) ->
                VuChip(label, state.compositeShare == v, { vm.setCompositeShare(v) }, Modifier.widthIn(min = 56.dp))
            }
        }
    }
}

/** Автоблокировка с 3-значной сигнализацией: отдельный переключатель (вне ряда уклонов); приглушён там, где значения в скобках табл. III.1 нет. */
@Composable
private fun AutoblockRow(state: BrakeCertificateUiState, applicable: Boolean, onChange: (Boolean) -> Unit, onHelp: () -> Unit) {
    val t = vuColors()
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 2.dp, end = 0.dp).alpha(if (applicable) 1f else 0.5f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // доступный переключатель - обычные цвета (текст text, дорожка accent); приглушён только когда для категории нет значения в скобках
            Text("Автоблокировка", Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = t.text)
            VuHelp(onHelp, touch = 32.dp)
            Switch(
                checked = state.autoblock3, onCheckedChange = onChange, modifier = Modifier.padding(horizontal = 8.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = t.accentInk, checkedTrackColor = t.accent, checkedBorderColor = t.accent,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline, uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant, uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
        if (!applicable) VuHint("Для этой категории и уклона значения в скобках табл. III.1 нет - переключатель не влияет на скорость.", Modifier.padding(start = 2.dp, bottom = 4.dp))
    }
}

/** Шторка «Свой объём ГР, л»: группа определяется по прим. 3 к табл. IV.1 (ближайший наименьший объём). */
@Composable
private fun CustomVolumeSheet(state: BrakeCertificateUiState, vm: BrakeCertificateViewModel, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(state.customVolumeText) }
    val result = BrakeDensity.groupByVolume(text)
    VuSheet(
        title = "Свой объём главных резервуаров", sub = "Общий объём ГР локомотива, литры (дробные допустимы)", onDismiss = onDismiss,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                VuBtn("Сбросить", { vm.setCustomVolume(""); onDismiss() }, kind = VuBtnKind.Text, h = 56.dp)
                VuBtn("Применить", { vm.setCustomVolume(text.trim()); onDismiss() }, Modifier.weight(1f), h = 56.dp, enabled = result !is BrakeDensity.VolumeGroup.Invalid)
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            VuInput("Объём ГР, л", text, onChange = { text = it }, unit = "л", sunk = true, placeholder = "например 1700")
            VuHint(BrakeDensity.volumeCaption(text))
            val g = (result as? BrakeDensity.VolumeGroup.Found)?.group
            if (g != null) BrakeDensity.normLines(
                g, axlesForNorm(state), state.chargePressureText.toFiniteDoubleOrNull(), state.locoCountText.trim().toIntOrNull() ?: 1,
            ).forEach { VuNormLine(it) }
        }
    }
}

/** Блок «Расчёт плотности»: [Кол-во локомотивов | Объём ГР] в одну строку и строки норм II и IV (зарядное давление - в блоке «Данные»). */
@Composable
private fun DensityFields(state: BrakeCertificateUiState, vm: BrakeCertificateViewModel, onGroup: () -> Unit, onHelp: (BrakeSheet) -> Unit) {
    val group = state.effectiveDensityGroup()
    val charge = state.chargePressureText.toFiniteDoubleOrNull()
    val axles = axlesForNorm(state)
    val locoCount = state.locoCountText.trim().toIntOrNull() ?: 1
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VuInput("Кол-во локомотивов", state.locoCountText, Modifier.weight(1f), onChange = vm::setLocoCount, keyboard = KeyboardType.Number, placeholder = "1", sunk = true, onHelp = { onHelp(BrakeSheet.Help("sme")) })
        VuInput(
            "Объём ГР",
            BrakeDensity.volumeFieldText(state.customVolumeText, group, manuallySelected = state.densityGroup != null),
            Modifier.weight(1f), sunk = true, onClick = onGroup, onHelp = { onHelp(BrakeSheet.Help("group")) },
        )
    }
    if (state.customVolumeText.isNotBlank() && group == null) VuNormLine(BrakeDensity.volumeCaption(state.customVolumeText))
    else BrakeDensity.normLines(group, axles, charge, locoCount).forEach { VuNormLine(it) }
}

/** «Заполнить из маршрута»: нажатие сразу заменяет значения актуальными данными маршрута; итог не показывается, только короткая ошибка (нет маршрута, маршрут не найден). */
@Composable
private fun FillFromRoute(state: BrakeCertificateUiState, vm: BrakeCertificateViewModel) {
    val t = vuColors()
    state.fillPick?.let { FillPickSheet(it, vm) }
    state.fillDialog?.let { msg ->
        com.z_company.route.component.AppAlertDialog(
            onDismissRequest = vm::dismissFillDialog, title = "Заполнить из маршрута", text = msg, confirmText = "Понятно", onConfirm = vm::dismissFillDialog,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        VuBtn(
            if (state.filling) "Заполняю…" else "Заполнить из маршрута", { vm.onFillClicked() },
            Modifier.fillMaxWidth(), kind = VuBtnKind.Tonal, enabled = !state.filling,
        )
        val error = state.fillReport?.note
        if (error != null) VuHint(error, Modifier.padding(horizontal = 4.dp), color = t.danger)
        else if (state.categoryNote != null) VuHint(state.categoryNote, Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
private fun MetricCell(label: String, value: String, color: Color? = null, align: TextAlign = TextAlign.Start, modifier: Modifier = Modifier) {
    val t = vuColors()
    Column(modifier) {
        Text(label, fontSize = 12.sp, color = t.muted, maxLines = 1, textAlign = align, modifier = Modifier.fillMaxWidth())
        Text(value, fontFamily = VuMono, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = color ?: t.text, modifier = Modifier.fillMaxWidth().padding(top = 2.dp), textAlign = align, maxLines = 1)
    }
}

@Composable
private fun ResultCard(result: BrakeCertificateResult?, state: BrakeCertificateUiState, category: BrakeCategory, onOpenBlank: () -> Unit) {
    val t = vuColors()
    val has = (result?.actualPressure ?: 0.0) > 0.0
    val sp = result?.speed
    val status = BrakeStatusResolver.resolve(result, has)
    val low = has && result != null && !result.enough
    val warn = status.level == StatusLevel.WARNING
    val numColor = when (status.level) { StatusLevel.DANGER -> t.danger; StatusLevel.WARNING -> t.text; else -> t.accent }
    VuCard {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                VuTag("Допускаемая скорость")
                if (has) Text("Бланк ›", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = t.accent, modifier = Modifier.clickable(onClick = onOpenBlank).padding(vertical = 8.dp))
            }
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                when {
                    !has || sp == null -> Text("—", fontFamily = VuMono, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = t.faint)
                    sp is SpeedLimit.Allowed -> {
                        Text("${sp.kmh}", fontFamily = VuMono, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = numColor)
                        Text("км/ч", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = numColor, modifier = Modifier.padding(start = 8.dp, bottom = 6.dp))
                    }
                    else -> Text("не определена", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = t.danger)
                }
            }
            if (!has || result == null) {
                VuHint("Введите вес состава, число осей и нажатие на ось - скорость и вердикт появятся здесь.", Modifier.padding(top = 12.dp))
            } else {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCell("Потребное, тс", fmt(result.requiredPressure), modifier = Modifier.weight(1f))
                    MetricCell("Фактическое, тс", fmt(result.actualPressure), color = if (low) (if (warn) t.warning else t.danger) else null, modifier = Modifier.weight(1f))
                    MetricCell(
                        "Ручных осей", if (category.manualBrakesRequired) (result.requiredManualAxles?.toString() ?: "—") else "все",
                        align = TextAlign.End, modifier = Modifier.weight(0.8f),
                    )
                }
                result.loco?.let { l ->
                    VuHint("Локомотив учтён: ${l.axles} осей, ${fmt(l.pressureTc)} тс, вес ${fmt(l.weight)} т - входят в потребное, фактическое и число осей", Modifier.padding(top = 6.dp))
                }
                Box(Modifier.padding(vertical = 10.dp).fillMaxWidth().height(1.dp).background(t.border))
                if (warn) VuWarnVerdict("Нажатие недостаточно: не хватает ${fmt(result.requiredPressure - result.actualPressure)} тс (${fmt(result.shortagePer100, 2)} тс на 100 тс веса при норме ${category.norm}), но скорость не снижена")
                else if (low) VuVerdict(false, "Нажатие недостаточно: не хватает ${fmt(result.requiredPressure - result.actualPressure)} тс (${fmt(result.shortagePer100, 2)} тс на 100 тс веса при норме ${category.norm})")
                else VuVerdict(true, "Нажатие достаточно: запас ${fmt(result.actualPressure - result.requiredPressure)} тс (${fmt(result.actualPer100, 2)} тс на 100 тс веса)")
                SpeedExplanation(sp, warn)
                result.manualAxlesOk?.let { ok ->
                    VuHint(if (ok) "Ручных осей достаточно (потребно ${result.requiredManualAxles})" else "Ручных осей не хватает: потребно ${result.requiredManualAxles}", Modifier.padding(top = 4.dp), color = if (ok) t.success else t.danger)
                }
                result.warnings.forEach { VuHint(it, Modifier.padding(top = 4.dp)) }
                state.timerII.recordedSec?.let { ii ->
                    VuHint("Плотность II: ${fmt(ii)} с" + (state.timerIV.recordedSec?.let { ", IV: ${fmt(it)} с" } ?: ""), Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

/** Пояснение скорости: одна короткая красная строка + «Подробнее» (шаги расчёта по п. 35 / условия / причина). */
@Composable
private fun SpeedExplanation(sp: SpeedLimit?, warn: Boolean = false) {
    val t = vuColors()
    var open by rememberSaveable { mutableStateOf(false) }
    when (sp) {
        is SpeedLimit.Allowed -> {
            val steps = sp.steps
            if (steps != null) {
                Column(Modifier.padding(top = 8.dp).fillMaxWidth().background(t.danger.copy(alpha = 0.12f), RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Нажатие ниже нормы: скорость снижена до ${sp.kmh} км/ч, ${InstructionRef.p("35")}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = t.danger, lineHeight = 20.sp)
                    Text(if (open) "Свернуть ▴" else "Подробнее ▾", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = t.accent, modifier = Modifier.clickable { open = !open }.padding(vertical = 4.dp))
                    if (open) {
                        VuTag("Расчёт скорости")
                        Text(steps, fontFamily = VuMono, fontSize = 13.sp, lineHeight = 19.sp, color = t.text)
                        sp.condition?.let { VuHint(it, color = t.danger) }
                    }
                }
            } else if (warn) {
                Column(Modifier.padding(top = 8.dp).fillMaxWidth().background(t.warning.copy(alpha = 0.18f), RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Скорость табл. III.1 сохранена: нажатия не хватает, но условие ${InstructionRef.p("31")} выполнено", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = t.text, lineHeight = 20.sp)
                    Text(if (open) "Свернуть ▴" else "Подробнее ▾", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = t.accent, modifier = Modifier.clickable { open = !open }.padding(vertical = 4.dp))
                    if (open) Text(InstructionRef.annotate(sp.note) + (sp.reducedFrom?.let { " Было бы по табл. III.1: $it км/ч." } ?: ""), fontSize = 14.sp, lineHeight = 20.sp, color = t.text)
                }
            } else VuHint(sp.note + (sp.reducedFrom?.let { " Было бы по табл. III.1: $it км/ч." } ?: ""), Modifier.padding(top = 8.dp))
        }
        is SpeedLimit.NotDetermined -> {
            Column(Modifier.padding(top = 8.dp).fillMaxWidth().background(t.danger.copy(alpha = 0.14f), RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (sp.kind == SpeedLimit.Kind.BELOW_MIN) {
                    Text("Нажатие ниже минимального", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = t.danger)
                    Text("Нажатие ниже минимального (${InstructionRef.table("III.2")}). Скорость по таблицам не определена.", fontSize = 14.sp, lineHeight = 20.sp, color = t.text)
                } else {
                    Text("Скорость по таблицам не определена", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = t.danger)
                }
                Text(if (open) "Свернуть ▴" else "Подробнее ▾", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = t.accent, modifier = Modifier.clickable { open = !open }.padding(vertical = 4.dp))
                if (open) Text(InstructionRef.annotate(sp.reason), fontSize = 14.sp, lineHeight = 20.sp, color = t.text)
            }
        }
        null -> Unit
    }
}

@Composable
private fun DeficitBlock(result: BrakeCertificateResult, category: BrakeCategory, onHelp: () -> Unit, onOpenReference: () -> Unit) {
    val t = vuColors()
    val items = deficitFacts(result)
    var open by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().background(t.danger.copy(alpha = 0.12f), RoundedCornerShape(16.dp)).padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
    ) {
        Row(Modifier.clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
            Text("Нехватка нажатия", Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = t.danger)
            Text(if (open) "Свернуть ▴" else "Подробнее ▾", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = t.accent, modifier = Modifier.padding(horizontal = 8.dp))
            VuHelp(onHelp)
        }
        if (open) Column(Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEachIndexed { i, x ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${i + 1}", fontFamily = VuMono, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = t.danger, modifier = Modifier.width(14.dp))
                    Text(x, fontSize = 14.sp, color = t.text, lineHeight = 20.sp)
                }
            }
            VuBtn("Нормы и снижение скорости в «Справочнике» ›", onOpenReference, kind = VuBtnKind.Text)
        }
    }
}

/** Закреплённая компактная строка результата (Бланк; Расчёт с клавиатурой): h56, r16. */
@Composable
private fun VuSpeedBar(result: BrakeCertificateResult?) {
    val t = vuColors()
    val has = BrakeStatusResolver.isCalculated(result)
    val status = BrakeStatusResolver.resolve(result, has)
    val sp = result?.speed
    val shape = RoundedCornerShape(16.dp)
    if (!has) {
        // Расчёт ещё не выполнен: без прочерка и «— км/ч», на всю плашку - пояснение нейтральным цветом
        Box(Modifier.fillMaxWidth().heightIn(min = 56.dp).background(t.surface, shape).padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.CenterStart) {
            Text("Расчёт скорости будет выполнен после ввода веса, осей и нажатия на ось", fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp, color = t.muted)
        }
        return
    }
    val belowMin = (sp as? SpeedLimit.NotDetermined)?.kind == SpeedLimit.Kind.BELOW_MIN
    val warn = status.level == StatusLevel.WARNING
    val danger = status.level == StatusLevel.DANGER
    if (BrakeStatusResolver.speedKmh(result) == null) {
        // Скорость не определена (ниже минимума / нет норм): без «— км/ч» и разделителя, на всю плашку только текст (цвет по уровню)
        Box(Modifier.fillMaxWidth().heightIn(min = 56.dp).background(if (danger) t.danger.copy(alpha = 0.14f) else t.surface, shape).padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                InstructionRef.annotate(status.headline), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp,
                color = when (status.level) { StatusLevel.NONE -> t.muted; StatusLevel.OK -> t.success; StatusLevel.WARNING -> t.text; StatusLevel.DANGER -> t.danger },
            )
        }
        return
    }
    val tone = when (status.level) { StatusLevel.NONE -> t.faint; StatusLevel.OK -> t.accent; StatusLevel.WARNING -> t.warning; StatusLevel.DANGER -> t.danger }
    val bg = when { danger -> t.danger.copy(alpha = 0.14f); warn -> t.warning.copy(alpha = 0.18f); else -> t.surface }
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 56.dp).background(bg, shape).padding(horizontal = 16.dp, vertical = if (belowMin || warn) 8.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val speedText = when {
            !has || sp == null -> "—"
            sp is SpeedLimit.Allowed -> "${sp.kmh}"
            else -> "—"
        }
        Row(verticalAlignment = Alignment.Bottom) {
            val speedColor = if (warn) t.text else tone
            Text(speedText, fontFamily = VuMono, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = speedColor)
            Text("км/ч", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = speedColor, modifier = Modifier.padding(start = 4.dp, bottom = 3.dp))
        }
        Box(Modifier.fillMaxHeight().padding(vertical = 12.dp).width(1.dp).background(t.border))
        Text(
            InstructionRef.annotate(status.headline),
            Modifier.weight(1f), fontSize = if (belowMin || warn) 13.sp else 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = if (belowMin || warn) 15.sp else 17.sp,
            color = when (status.level) { StatusLevel.NONE -> t.muted; StatusLevel.OK -> t.success; StatusLevel.WARNING -> t.text; StatusLevel.DANGER -> t.danger },
            maxLines = if (belowMin || warn) 3 else 2,
        )
    }
}

// ───────────────────────────── Бланк ─────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlankTab(
    state: BrakeCertificateUiState,
    category: BrakeCategory,
    result: BrakeCertificateResult?,
    vm: BrakeCertificateViewModel,
    now: Long,
    side: Int,
    turnaroundApplies: Boolean,
    onSide: (Int) -> Unit,
    onSheet: (BrakeSheet) -> Unit,
    onOpenMeasure: () -> Unit,
) {
    val effSide = if (turnaroundApplies) side else 0
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) { VuSpeedBar(result) }
        Column(
            modifier = Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (turnaroundApplies) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(240.dp)) { VuTabs(listOf("Лицевая", "Оборот"), effSide, onSide, h = 40.dp) }
                }
            }
            FillFromRoute(state, vm)
            VuPaper {
                PaperHeader(state, vm, open = true, onToggle = {}, side = effSide == 1, onHelp = { onSheet(BrakeSheet.Help(it)) })
                if (effSide == 0) {
                    FrontBody(state, category, result, vm, onSheet, onOpenMeasure)
                } else {
                    BackBody(state, vm, onSheet)
                }
            }
            if (effSide == 0) LocoCard(state, category, vm, onHelp = { onSheet(BrakeSheet.Help(it)) })
        }
    }
}

@Composable
private fun DashedBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val t = vuColors()
    Box(
        modifier.drawBehind {
            drawRoundRect(
                color = t.borderStrong, cornerRadius = CornerRadius(4.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
            )
        },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun PaperHeader(
    state: BrakeCertificateUiState,
    vm: BrakeCertificateViewModel,
    open: Boolean,
    onToggle: () -> Unit,
    side: Boolean,
    onHelp: (String) -> Unit,
) {
    val t = vuColors()
    Column(Modifier.padding(top = 10.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // штемпель станции - реквизит лицевой стороны; на обороте не показывается
            if (side) Spacer(Modifier.weight(1f)) else Column(Modifier.weight(1f)) {
                VuPrint("Штемпель станции", 12)
                if (open) {
                    DashedBox(Modifier.padding(top = 6.dp).width(112.dp).height(62.dp)) {
                        VuStationSuggest(state.stationText, remember(state.trainStationNames, state.knownStationNames) { BrakeStations.merge(state.trainStationNames, state.knownStationNames) }, onPick = vm::setStation) {
                            VuInk(state.stationText, Modifier.padding(horizontal = 4.dp), onChange = vm::setStation, placeholder = "станция", align = TextAlign.Center)
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VuPrint("Форма ВУ-45", 12)
                    Box(Modifier.widthIn(min = 70.dp).height(22.dp).border(1.dp, t.borderStrong).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                        Text("№ —", fontFamily = VuMono, fontSize = 12.sp, color = t.faint)
                    }
                }
                if (open && !side) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        VuPrint("Время выдачи", 12)
                        VuTimeField(state.issueTimeText, vm::setIssueTime, Modifier.width(72.dp), ink = true)
                    }
                }
            }
        }
        Text(
            if (side) "ОТМЕТКИ ОБ ОПРОБОВАНИИ В ПУТИ" else "СПРАВКА О ТОРМОЗАХ",
            Modifier.fillMaxWidth().padding(top = if (open) 10.dp else 8.dp),
            textAlign = TextAlign.Center, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp, color = t.text,
        )
        if (!side) {
            if (open) {
                VuFillRow(end = true) {
                    VuPrint("Дата")
                    VuInk(state.dateText, Modifier.width(128.dp), onChange = vm::setDate, keyboard = KeyboardType.Number, maxLen = 10)
                }
                VuFillRow {
                    VuPrint("Локомотив, серия")
                    VuStationSuggest(state.locoSeriesText, state.knownSeriesNames, onPick = vm::setLocoSeries, modifier = Modifier.weight(1f)) {
                        VuInk(state.locoSeriesText, Modifier.fillMaxWidth(), onChange = vm::setLocoSeries, maxLen = 12)
                    }
                    VuPrint("№")
                    VuInk(state.locoNumberText, Modifier.width(80.dp), onChange = vm::setLocoNumber, maxLen = 8)
                }
                VuFillRow {
                    VuPrint("Поезд №")
                    VuInk(state.trainNumberText, Modifier.width(100.dp), onChange = vm::setTrainNumber, maxLen = 10)
                }
            }
        }
    }
}

@Composable
private fun FrontBody(
    state: BrakeCertificateUiState,
    category: BrakeCategory,
    result: BrakeCertificateResult?,
    vm: BrakeCertificateViewModel,
    onSheet: (BrakeSheet) -> Unit,
    onOpenMeasure: () -> Unit,
) {
    val t = vuColors()
    val help = { k: String -> onSheet(BrakeSheet.Help(k)) }
    // локомотив учтён: в шапке - значения с локомотивом (вес состава + вес локомотива, оси вагонов + оси локомотива, как в калькуляторе), подпись снизу
    val locoShare = result?.loco
    VuFillRow {
        VuPrint("весом")
        if (locoShare != null) VuInk(fmt((state.weightText.toFiniteDoubleOrNull() ?: 0.0) + locoShare.weight), Modifier.weight(1f), calc = true)
        else VuInk(state.weightText, Modifier.weight(1f), onChange = vm::setWeight, keyboard = KeyboardType.Decimal, maxLen = 8)
        VuPrint("т.")
        VuPrint("Всего осей", modifier = Modifier.padding(start = 6.dp))
        if (locoShare != null) VuInk("${result.totalAxles}", Modifier.width(56.dp), calc = true)
        else VuInk(state.totalAxlesText, Modifier.width(56.dp), onChange = vm::setTotalAxles, keyboard = KeyboardType.Number, maxLen = 4)
    }
    if (locoShare != null) LocoCaption(locoShare)
    VuFillRow {
        Text("Требуется нажатие тормозных колодок в тс.", Modifier.weight(1f), fontSize = 14.sp, color = t.text, lineHeight = 18.sp)
        VuInk(result?.let { fmt(it.requiredPressure) } ?: "", Modifier.width(92.dp), calc = true, align = TextAlign.End)
        VuMiniHelp { help("required") }
    }
    result?.loco?.let { LocoCaption(it) }
    VuFillRow(end = true) {
        VuPrint("ручных тормозов в осях")
        VuInk(if (!category.manualBrakesRequired) "все" else result?.requiredManualAxles?.toString() ?: "", Modifier.width(56.dp), calc = true, align = TextAlign.End)
        VuMiniHelp { help("manual_req") }
    }
    Spacer(Modifier.height(8.dp))
    AxleTable(state, vm, onSheet)
    AxleSumWarning(state, vm, Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    Spacer(Modifier.height(6.dp))
    VuFillRow {
        VuPrint("Наличие ручных тормозных осей")
        VuInk(state.manualHaveText, Modifier.weight(1f), onChange = vm::setManualHave, keyboard = KeyboardType.Number, maxLen = 3)
        VuMiniHelp { help("manual_have") }
    }
    result?.manualAxlesOk?.let { ok ->
        Text(
            if (ok) "ручных осей достаточно (потребно ${result.requiredManualAxles})" else "ручных осей не хватает: потребно ${result.requiredManualAxles}",
            Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (ok) t.success else t.danger,
        )
    }
    // Плотность из «Замеров»
    val ii = state.timerII.recordedSec
    val iv = state.timerIV.recordedSec
    // II и IV - каждое значение в своей строке (без обрезки при крупном шрифте)
    VuFillRow(Modifier.clickable(onClick = { onSheet(BrakeSheet.ManualDensity(if (ii == null) TimerKind.II else TimerKind.IV)) })) {
        Text("Плотность тормозной сети поезда", Modifier.weight(1f), fontSize = 14.sp, color = t.text, lineHeight = 18.sp)
        if (ii == null && iv == null) VuInk("", Modifier.width(132.dp), calc = false, placeholder = "ввести ›", align = TextAlign.End)
        VuMiniHelp { help("density") }
    }
    if (ii != null || iv != null) {
        Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp).padding(bottom = 8.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ii?.let { Text("II  ${fmt(it)} с" + if (state.timerII.manual) " · вручную" else "", Modifier.clickable { onSheet(BrakeSheet.ManualDensity(TimerKind.II)) }.heightIn(min = 32.dp), fontFamily = VuMono, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = t.accent, softWrap = true, textAlign = TextAlign.End) }
            iv?.let { Text("IV  ${fmt(it)} с" + if (state.timerIV.manual) " · вручную" else "", Modifier.clickable { onSheet(BrakeSheet.ManualDensity(TimerKind.IV)) }.heightIn(min = 32.dp), fontFamily = VuMono, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = t.accent, softWrap = true, textAlign = TextAlign.End) }
            Text("из «Замеров» · нажмите, чтобы ввести вручную" + if (iv == null) " · IV не замерено" else "", fontSize = 12.sp, color = t.accent, textAlign = TextAlign.End)
        }
    }
    VuFillRow {
        VuPrint("Хвостовой вагон №")
        VuInk(state.tailCarText, Modifier.weight(1f), onChange = vm::setTailCar, maxLen = 14)
        VuMiniHelp { help("tail_car") }
    }
    VuFillRow {
        VuPrint("Подпись", bold = true, modifier = Modifier.padding(start = 24.dp))
        VuInk(state.inspectorNameText, Modifier.weight(1f), onChange = vm::setInspectorName, placeholder = "фамилия осмотрщика", maxLen = 30)
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun AxleTable(state: BrakeCertificateUiState, vm: BrakeCertificateViewModel, onSheet: (BrakeSheet) -> Unit) {
    val t = vuColors()
    val rows = state.effectiveRows()
    val byPressure = rows.associateBy { it.pressureText.toFiniteDoubleOrNull() }
    val extra = rows.filter { r -> val p = r.pressureText.toFiniteDoubleOrNull(); p != null && p !in PRINTED }
    val locoIn = state.toInput()?.loco
    val axleRowsForTable = rows.mapNotNull { r -> val p = r.pressureText.toFiniteDoubleOrNull(); val n = r.axlesText.trim().toIntOrNull(); if (p != null && n != null && n > 0) AxleRow(p, n) else null }
    val locoRowInfo = BrakeTable.locoRow(axleRowsForTable, locoIn, PRINTED)
    val totalAxles = locoRowInfo.totalAxles
    val totalPressure = locoRowInfo.totalPressure
    val low = state.toInput()?.let { BrakeCertificateCalculator.calculate(it) }?.let { it.actualPressure > 0 && !it.enough } == true
    val lineColor = t.borderStrong
    val head: @Composable (String, Boolean) -> Unit = { text, right ->
        Box(Modifier.fillMaxSize().vuCellBorders(lineColor, right = right, bottom = true).padding(horizontal = 2.dp), contentAlignment = Alignment.Center) {
            Text(text, fontSize = 10.sp, lineHeight = 12.sp, color = t.text, textAlign = TextAlign.Center)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(lineColor))
    Row(Modifier.height(IntrinsicSize.Min).fillMaxWidth()) {
        Column(Modifier.weight(0.66f)) {
            Row(Modifier.height(44.dp)) {
                Box(Modifier.weight(1f).fillMaxHeight()) { head("Тормозное нажатие на ось, тс", true) }
                Box(Modifier.weight(1f).fillMaxHeight()) { head("Количество осей", true) }
                Box(Modifier.weight(1f).fillMaxHeight()) { head("Нажатие колодок, тс", true) }
            }
            PRINTED.forEach { p ->
                val r = byPressure[p]
                val locoHere = locoIn?.takeIf { locoRowInfo.mergedPressure?.let { m -> BrakeTable.samePressure(m, p) } == true }
                AxleRowCells(label = fmtP(p), axles = r?.axlesText.orEmpty(), product = if (r != null || locoHere != null) fmt(p * ((r?.axlesText?.trim()?.toIntOrNull() ?: 0) + (locoHere?.axles ?: 0))) else "", filled = r != null || locoHere != null, locoAxles = locoHere?.axles, onAxles = { vm.setAxlesForPressure(p, it) })
            }
            extra.forEach { r ->
                val p = r.pressureText.toFiniteDoubleOrNull() ?: 0.0
                val locoHere = locoIn?.takeIf { locoRowInfo.mergedPressure?.let { m -> BrakeTable.samePressure(m, p) } == true }
                AxleRowCells(label = fmtP(p), axles = r.axlesText, product = fmt(p * ((r.axlesText.trim().toIntOrNull() ?: 0) + (locoHere?.axles ?: 0))), filled = true, locoAxles = locoHere?.axles, onAxles = { vm.setAxlesForPressure(p, it) })
            }
            // Локомотив (если учитывается): не редактируется здесь - правка в блоке «Учитывать локомотив»
            if (locoIn != null && locoRowInfo.separateRow) {
                val lbg = t.accentSoft
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(Modifier.weight(1f).heightIn(min = 32.dp).background(lbg).vuCellBorders(lineColor), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("лок. ${state.locoSeriesText}".trim(), fontSize = 9.sp, color = t.muted, maxLines = 1)
                            Text(fmtP(locoIn.pressurePerAxle), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = t.text)
                        }
                    }
                    Box(Modifier.weight(1f).heightIn(min = 32.dp).background(lbg).vuCellBorders(lineColor), contentAlignment = Alignment.Center) {
                        Text("${locoIn.axles}", fontFamily = VuMono, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = t.accent)
                    }
                    Box(Modifier.weight(1f).heightIn(min = 32.dp).background(lbg).vuCellBorders(lineColor), contentAlignment = Alignment.Center) {
                        Text(fmt(locoIn.pressurePerAxle * locoIn.axles), fontFamily = VuMono, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = t.text, maxLines = 1)
                    }
                }
            }
            // Всего
            Row(Modifier.height(IntrinsicSize.Min)) {
                Box(Modifier.weight(1f).heightIn(min = 32.dp).vuCellBorders(lineColor), contentAlignment = Alignment.Center) { VuPrint("Всего", bold = true) }
                Box(Modifier.weight(1f).heightIn(min = 32.dp).vuCellBorders(lineColor), contentAlignment = Alignment.Center) { VuInk(if (totalAxles > 0) "$totalAxles" else "", calc = true, align = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                Box(Modifier.weight(1f).heightIn(min = 32.dp).vuCellBorders(lineColor), contentAlignment = Alignment.Center) {
                    Text(if (totalPressure > 0) fmt(totalPressure) else "", fontFamily = VuMono, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (low) t.danger else t.text, maxLines = 1)
                }
            }
        }
        // «Другие данные» - одна высокая графа
        Column(Modifier.weight(0.34f).fillMaxHeight().vuCellBorders(lineColor, right = false, bottom = true)) {
            Box(Modifier.height(44.dp).fillMaxWidth().vuCellBorders(lineColor, right = false, bottom = true), contentAlignment = Alignment.Center) {
                Text("Другие данные", fontSize = 10.sp, color = t.text)
            }
            OtherColumn(state, onEdit = { onSheet(BrakeSheet.Other) })
        }
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onSheet(BrakeSheet.RowPressure) }.vuCellBorders(lineColor, right = false, bottom = true).padding(start = 14.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("+ Другое нажатие", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = t.accent)
        Spacer(Modifier.weight(1f))
        Text("1,25…21 тс", fontSize = 12.sp, color = t.muted)
    }
}

/** Предупреждение о расхождении числа осей таблицы и «Всего осей» (warning-цвет, не красный «нехватка нажатия»). */
@Composable
private fun AxleSumWarning(state: BrakeCertificateUiState, vm: BrakeCertificateViewModel, modifier: Modifier = Modifier) {
    val t = vuColors()
    val sum = state.effectiveRows().sumOf { it.axlesText.trim().toIntOrNull() ?: 0 }
    val check = AxleCheck.compareWithLoco(state.totalAxlesText.trim().toIntOrNull(), sum, state.toInput()?.loco?.axles ?: 0)
    if (!state.rowsMode) return
    when (check) {
        is AxleSumCheck.Equal -> Unit
        AxleSumCheck.NoTable -> Unit
        else -> {
            val msg = AxleCheck.message(check) ?: return
            Column(modifier.fillMaxWidth().background(t.warning.copy(alpha = 0.16f), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(msg, fontFamily = VuMono, fontSize = 13.sp, lineHeight = 18.sp, color = t.warning)
                if (check is AxleSumCheck.DeclaredEmpty) {
                    VuBtn("Взять из таблицы: ${check.tableSum}", { vm.setTotalAxles(check.tableSum.toString()) }, kind = VuBtnKind.Tonal, h = 40.dp)
                }
            }
        }
    }
}

@Composable
private fun AxleRowCells(label: String, axles: String, product: String, filled: Boolean, onAxles: (String) -> Unit, locoAxles: Int? = null) {
    val t = vuColors()
    val line = t.borderStrong
    val bg = if (filled) t.accentSoft else Color.Transparent
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.height(32.dp)) {
        Box(Modifier.weight(1f).height(32.dp).background(bg).vuCellBorders(line), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 14.sp, fontWeight = if (filled) FontWeight.Bold else FontWeight.Normal, color = t.text)
        }
        Box(
            Modifier.weight(1f).height(32.dp).background(bg).vuCellBorders(line)
                .then(if (focused) Modifier.border(2.dp, t.accent) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            // локомотив влит в строку: крупно число осей строки (вагоны + локомотив), мелко «в т.ч. лок. N»; тап - правка числа осей вагонов
            var editing by remember(label) { mutableStateOf(false) }
            val fr = remember { androidx.compose.ui.focus.FocusRequester() }
            if (locoAxles != null && !editing) {
                val wagons = axles.trim().toIntOrNull() ?: 0
                Column(Modifier.fillMaxSize().clickable { editing = true }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("${wagons + locoAxles}", fontFamily = VuMono, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = t.accent, lineHeight = 17.sp)
                    Text("в т.ч. лок. $locoAxles", fontSize = 8.sp, color = t.muted, maxLines = 1, lineHeight = 9.sp)
                }
            } else {
                if (locoAxles != null) LaunchedEffect(Unit) { fr.requestFocus() }
                BasicTextField(
                    value = axles,
                    onValueChange = { onAxles(it.filter { c -> c in '0'..'9' }.take(4)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).focusRequester(fr)
                        .onFocusChanged { focused = it.isFocused; if (locoAxles != null && !it.isFocused) editing = false },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = VuMono, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = t.accent, textAlign = TextAlign.Center),
                    cursorBrush = SolidColor(t.accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        }
        Box(Modifier.weight(1f).height(32.dp).background(bg).vuCellBorders(line), contentAlignment = Alignment.Center) {
            Text(product, fontFamily = VuMono, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = t.text, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OtherColumn(state: BrakeCertificateUiState, onEdit: () -> Unit) {
    val t = vuColors()
    val items = buildList {
        when (state.compositeShare) {
            CompositeShare.NONE -> {}
            CompositeShare.P50 -> add("К" to "50")
            CompositeShare.P75 -> add("К" to "75")
            CompositeShare.P100 -> add("К" to "100")
        }
        if (state.flags.isNotEmpty()) add("Торм." to state.flags.joinToString("/"))
        if (state.tcpvText.isNotBlank()) add("ТЦПВ" to state.tcpvText + " мм")
        if (state.vstrText.isNotBlank()) add("Встр." to state.vstrText)
        if (state.dpvText.isNotBlank()) add("ДПВ" to state.dpvText)
        if (state.vo2hvText.isNotBlank()) add("ВО2ХВ" to state.vo2hvText + " с")
        if (state.tailVoltageText.isNotBlank()) add("Напр." to state.tailVoltageText + " В")
        if (state.feedDensityText.isNotBlank()) add("Пит." to state.feedDensityText)
        // зарядное давление - тот же источник, что поле на «Расчёте» (`chargePressureText`)
        if (state.chargePressureText.isNotBlank()) add("Заряд." to state.chargePressureText + " кгс/см²")
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        // Каждый параметр - строка «подпись значение» (как в бланке), параметры между собой - столбцом
        items.forEach { (k, v) ->
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(k, Modifier.alignByBaseline(), fontSize = 12.sp, color = t.muted)
                // единица измерения «кгс/см²» мельче значения, чтобы строка помещалась в узкую графу
                val parts = v.split('\u00a0', limit = 2)
                Text(parts[0], Modifier.alignByBaseline(), fontFamily = VuMono, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = t.accent, lineHeight = 17.sp)
                if (parts.size > 1) Text(parts[1], Modifier.alignByBaseline(), fontSize = 9.sp, color = t.muted, maxLines = 1, softWrap = false)
            }
        }
        Text(
            "Редактировать", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = t.accent,
            modifier = Modifier.clickable(onClick = onEdit).heightIn(min = 44.dp).padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun LocoCard(state: BrakeCertificateUiState, category: BrakeCategory, vm: BrakeCertificateViewModel, onHelp: (String) -> Unit) {
    val t = vuColors()
    VuCard {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Учитывать локомотив", fontSize = 16.sp, color = t.text)
                    VuHelp({ onHelp("loco_brake") }, touch = 32.dp)
                }
                Text(
                    if (category.group == TrainGroup.FREIGHT) InstructionRef.annotate("В грузовых поездах не учитывается (п. 41)") else "Вес и нажатие - по данным владельца инфраструктуры (п. 40 прил. 2 Правил)",
                    fontSize = 13.sp, color = t.muted, modifier = Modifier.padding(top = 2.dp),
                )
            }
            Switch(
                checked = state.locoEnabled, onCheckedChange = vm::setLocoEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = t.accentInk, checkedTrackColor = t.accent, checkedBorderColor = t.accent,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline, uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant, uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
        if (state.locoEnabled) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VuInput("Вес, т", state.locoWeightText, Modifier.weight(1f), onChange = vm::setLocoWeight, unit = "т", sunk = true)
                    VuInput("Осей", state.locoAxlesText, Modifier.weight(1f), onChange = vm::setLocoAxles, sunk = true, keyboard = KeyboardType.Number)
                    VuInput("Нажатие, тс/ось", state.locoPressureText, Modifier.weight(1f), onChange = vm::setLocoPressure, sunk = true)
                }
                VuHint(
                    if (state.locoAuto) "Справочник серий (прежняя Инструкция 277; по Правилам данные локомотивов утверждает владелец инфраструктуры) для серии «${state.locoSeriesText}»" + (state.locoNote?.let { ": $it" } ?: "") + ". Можно править."
                    else if (state.locoSeriesText.isNotBlank()) "Серии «${state.locoSeriesText}» нет в справочнике - введите вручную."
                    else "Серия локомотива - в «Реквизитах» бланка; значения можно ввести вручную.",
                )
            }
        }
    }
}

// ───────────────────────────── Оборот ─────────────────────────────

@Composable
private fun BackBody(state: BrakeCertificateUiState, vm: BrakeCertificateViewModel, onSheet: (BrakeSheet) -> Unit) {
    val t = vuColors()
    val line = t.borderStrong
    // станции раздела «Поезд» перечитываются при открытии «Оборота» (ручные записи и введённые данные сохраняются)
    LaunchedEffect(state.routeId) { if (state.routeId != null) vm.refreshStops() }
    val reference = state.timerII.recordedSec
    val refs = remember(state.turnaround, reference) { TurnaroundReference.resolve(state.turnaround, reference, ::minutesOfDay) }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().vuCellBorders(line, right = false, bottom = true).height(IntrinsicSize.Min)) {
            listOf("Станция / место" to 1.25f, "Вид" to 0.55f, "Плотность" to 1.0f, "Время" to 0.7f).forEachIndexed { i, (x, w) ->
                Box(Modifier.weight(w).fillMaxHeight().vuCellBorders(line, right = i < 3, bottom = false).padding(6.dp), contentAlignment = Alignment.Center) {
                    Text(x, fontSize = 11.sp, color = t.text, textAlign = TextAlign.Center)
                }
            }
        }
        if (state.turnaround.isEmpty()) {
            Text("Записей нет", Modifier.padding(14.dp), fontSize = 14.sp, color = t.muted)
        }
        state.turnaround.forEach { e ->
            Column(Modifier.fillMaxWidth().clickable { onSheet(BrakeSheet.Turnaround(e.id)) }.vuCellBorders(line, right = false, bottom = true)) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Box(Modifier.weight(1.25f).fillMaxHeight().vuCellBorders(line, right = true, bottom = false).padding(horizontal = 8.dp, vertical = 10.dp), contentAlignment = Alignment.CenterStart) {
                        Column {
                            Text(e.stationName.ifBlank { "станция" }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (e.stationName.isBlank()) t.faint else t.accent, lineHeight = 18.sp)
                            val period = if (e.arrivalMs != null) hhmm(e.arrivalMs!!) + "–" + (e.departureMs?.let { hhmm(it) } ?: "…") else ""
                            if (period.isNotEmpty()) Text((if (e.stopClass == StopClass.OVER_30) "> 30 мин · " else "> 5 мин · ") + period, fontFamily = VuMono, fontSize = 11.sp, color = t.muted)
                        }
                    }
                    Box(Modifier.weight(0.55f).fillMaxHeight().vuCellBorders(line, right = true, bottom = false).padding(horizontal = 4.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(e.testKindShort, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = t.accent)
                    }
                    Box(Modifier.weight(1.0f).fillMaxHeight().vuCellBorders(line, right = true, bottom = false).padding(horizontal = 8.dp, vertical = 8.dp), contentAlignment = Alignment.CenterStart) {
                        val d = e.densitySec
                        if (d == null) Text("замерить", fontSize = 12.sp, color = t.faint)
                        else {
                            val info = refs[e.id]
                            val r = TurnaroundDensityCheck.check(d, info?.ref?.seconds)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(fmt(d), fontFamily = VuMono, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = t.accent)
                                    // запись СП/ТП не оценивается: только значение и пометка «эталон»
                                    if (info?.becomesReference != true) r.deviationPct?.let { Text(formatDeviation(it), fontFamily = VuMono, fontSize = 12.sp, color = t.muted) }
                                }
                                if (info?.becomesReference == true) Text("эталон", fontSize = 10.sp, color = t.muted)
                                else {
                                    when (r.verdict) {
                                        TurnaroundDensityCheck.Verdict.OK -> Text("✓ в норме", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = t.success)
                                        TurnaroundDensityCheck.Verdict.EXCEEDED -> Text("! более 20 %", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = t.danger)
                                        TurnaroundDensityCheck.Verdict.NO_REFERENCE -> Text("нет эталона", fontSize = 11.sp, color = t.muted)
                                    }
                                    info?.ref?.let { Text(it.label, fontSize = 10.sp, color = t.muted) }
                                }
                            }
                        }
                    }
                    Box(Modifier.weight(0.7f).fillMaxHeight().padding(horizontal = 6.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(TimeOfDayInput.display(e.readinessTime), fontFamily = VuMono, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = t.accent)
                    }
                }
                e.compositionSummary?.let { Text(it, Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp), fontSize = 12.sp, color = t.muted, lineHeight = 16.sp) }
            }
        }
        if (state.turnaround.any { it.densitySec != null && refs[it.id]?.ref == null && refs[it.id]?.becomesReference != true }) {
            VuHint("Нет эталона: запишите замер II на лицевой стороне справки.", Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            VuBtn("+ Добавить отметку", {
                val id = vm.newTurnaroundId()
                vm.addTurnaround(StopClass.OVER_5, state.trainStationNames.lastOrNull().orEmpty(), id)
                onSheet(BrakeSheet.Turnaround(id))
            }, kind = VuBtnKind.Text, h = 48.dp)
        }
    }
}

private fun formatDeviation(pct: Double): String {
    val r = Math.round(pct).toInt()
    return (if (r < 0) "\u2212" else if (r > 0) "+" else "") + kotlin.math.abs(r) + " %"
}

@Composable
private fun TurnaroundSheet(
    state: BrakeCertificateUiState,
    entryId: String,
    vm: BrakeCertificateViewModel,
    onDismiss: () -> Unit,
    onMeasure: () -> Unit,
) {
    val e = state.turnaround.firstOrNull { it.id == entryId }
    if (e == null) { onDismiss(); return }
    val t = vuColors()
    VuSheet(
        title = "Отметка об опробовании", sub = if (e.auto) "Создана автоматически по стоянке" else "Добавлена вручную", onDismiss = onDismiss,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                VuBtn("Удалить", { vm.removeTurnaround(entryId); onDismiss() }, kind = VuBtnKind.Text, h = 56.dp)
                VuBtn("Готово", onDismiss, Modifier.weight(1f), h = 56.dp)
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            VuTag("Стоянка")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VuChip("более 5 мин", e.stopClass == StopClass.OVER_5, { vm.updateTurnaround(entryId) { it.copy(stopClass = StopClass.OVER_5) } }, Modifier.weight(1f))
                VuChip("более 30 мин", e.stopClass == StopClass.OVER_30, { vm.updateTurnaround(entryId) { it.copy(stopClass = StopClass.OVER_30) } }, Modifier.weight(1f))
            }
            if (e.arrivalMs != null) VuHint("Стоянка: ${hhmm(e.arrivalMs!!)} – ${e.departureMs?.let { hhmm(it) } ?: "идёт"}")
            val suggestions = remember(state.trainStationNames, state.knownStationNames) { BrakeStations.merge(state.trainStationNames, state.knownStationNames) }
            VuStationSuggest(e.stationName, suggestions, onPick = { n -> vm.updateTurnaround(entryId) { it.copy(stationName = n) } }) {
                VuInput("Станция", e.stationName, onChange = { v -> vm.updateTurnaround(entryId) { it.copy(stationName = v) } }, text = true, sunk = true)
            }
            VuInput(
                "Плотность, с", e.densitySec?.let { fmt(it) } ?: "",
                onChange = { v -> vm.updateTurnaround(entryId) { it.copy(densitySec = v.toFiniteDoubleOrNull()) } }, unit = "с", sunk = true,
            )
            VuBtn("Замерить секундомером", onMeasure, Modifier.fillMaxWidth(), kind = VuBtnKind.Tonal)
            run {
                val refs = TurnaroundReference.resolve(state.turnaround, state.timerII.recordedSec, ::minutesOfDay)
                val info = refs[e.id]
                val ref = info?.ref
                val isSp = e.testKind == TurnaroundEntry.KIND_SHORT || e.testKind == TurnaroundEntry.KIND_FULL
                val r = e.densitySec?.let { TurnaroundDensityCheck.check(it, ref?.seconds) }
                val range = ref?.let { "допуск ${fmt(it.seconds * 0.8)}–${fmt(it.seconds * 1.2)} с" }
                when {
                    isSp -> VuHint("Станет эталоном для следующих замеров (сама не оценивается).")
                    ref == null -> VuHint("Нет эталона: запишите замер II на лицевой стороне справки или плотность с видом «СП»/«ТП» раньше по времени.")
                    r == null -> VuHint("Эталон ${ref.label.removePrefix("от ")}: ${fmt(ref.seconds)} с; $range (±20 %).")
                    r.verdict == TurnaroundDensityCheck.Verdict.OK -> VuVerdict(true, "В норме: ${formatDeviation(r.deviationPct ?: 0.0)} ${ref.label}; $range")
                    else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        VuVerdict(false, "Отклонение более 20 % (${formatDeviation(r.deviationPct ?: 0.0)}) ${ref.label}; $range")
                        VuHint("По прил. 3, п. 1 ${InstructionRef.GENITIVE}: при изменении плотности более чем на 20 % от указанной в справке проводится сокращённое опробование тормозов - отметьте «СП».")
                    }
                }
            }
            VuTag("Вид опробования")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VuChip("—", e.testKind.isEmpty(), { vm.updateTurnaround(entryId) { it.copy(testKind = "") } }, Modifier.weight(1f))
                VuChip("ТП", e.testKind == TurnaroundEntry.KIND_FULL, { vm.updateTurnaround(entryId) { it.copy(testKind = TurnaroundEntry.KIND_FULL) } }, Modifier.weight(1f))
                VuChip("СП", e.testKind == TurnaroundEntry.KIND_SHORT, { vm.updateTurnaround(entryId) { it.copy(testKind = TurnaroundEntry.KIND_SHORT) } }, Modifier.weight(1f))
            }
            VuHint("ТП - полное опробование, СП - сокращённое. «—» - опробования не было, только замер плотности.")
            VuTag("Подпись и время")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
VuTimeField(e.readinessTime, { v -> vm.updateTurnaround(entryId) { it.copy(readinessTime = v) } }, Modifier.weight(1f), label = "Время опробования")
                VuInput("Подпись (таб. №)", e.inspectorTab, Modifier.weight(1f), onChange = { v -> vm.updateTurnaround(entryId) { it.copy(inspectorTab = v) } }, keyboard = KeyboardType.Number, sunk = true)
            }
            var compHelp by remember { mutableStateOf(false) }
            var compOpen by remember(entryId) { mutableStateOf(e.changes.isNotEmpty() || e.newWeight.isNotBlank() || e.newAxles.isNotBlank() || e.newRequired.isNotBlank() || e.newActual.isNotBlank()) }
            if (compHelp) CompositionHelpSheet(onDismiss = { compHelp = false })
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable {
                compOpen = !compOpen
                // первая строка изменений открыта сразу
                if (compOpen && e.changes.isEmpty()) vm.updateTurnaroundComposition(entryId) { en -> en.copy(changes = en.changes + WagonChange(id = vm.newTurnaroundId())) }
            }, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { VuTag("При изменении состава и веса поезда") }
                VuHelp({ compHelp = true }, touch = 32.dp)
                Text(if (compOpen) "▴" else "▾", Modifier.padding(horizontal = 12.dp), fontSize = 14.sp, color = t.muted)
            }
            if (compOpen) {
            VuHint("Прицепка или отцепка вагонов: введите новый вес и добавьте строки изменений (сколько осей с каким нажатием отцеплено или прицеплено) - число осей, потребное и фактическое нажатие рассчитаются сами (можно поправить). Если состав не менялся - оставьте пустым.")
            val calc = remember(state, e.newWeight, e.newAxles, e.changes) {
                TurnaroundComposition.compute(state.toInput(requireWeight = false), e.newWeight.toFiniteDoubleOrNull(), e.newAxles.trim().toIntOrNull(), e.changes)
            }
            VuInput("Вес поезда, т", e.newWeight, onChange = { v -> vm.updateTurnaroundComposition(entryId) { it.copy(newWeight = v) } }, unit = "т", sunk = true)
            VuTag("Что изменилось в составе")
            e.changes.forEach { c ->
                WagonChangeRow(
                    c,
                    available = calc?.available.orEmpty(),
                    onChange = { n -> vm.updateTurnaroundComposition(entryId) { en -> en.copy(changes = en.changes.map { if (it.id == c.id) n else it }) } },
                    onRemove = { vm.updateTurnaroundComposition(entryId) { en -> en.copy(changes = en.changes.filterNot { it.id == c.id }) } },
                )
            }
            VuBtn("Добавить изменения состава", { vm.updateTurnaroundComposition(entryId) { en -> en.copy(changes = en.changes + WagonChange(id = vm.newTurnaroundId())) } }, kind = VuBtnKind.Text, h = 48.dp)
            calc?.warnings?.forEach { w -> VuHint(w) }
            VuInput("Всего осей", e.newAxles, onChange = { v -> vm.updateTurnaroundComposition(entryId) { it.copy(newAxles = v) } }, keyboard = KeyboardType.Number, sunk = true)
            val reqManual = TurnaroundComposition.isManual(e.newRequired, calc?.requiredText)
            val actManual = TurnaroundComposition.isManual(e.newActual, calc?.actualText)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VuInput(if (reqManual) "Потребное, тс · правка" else if (calc?.requiredText != null && e.newRequired.isNotBlank()) "Потребное, тс · расчёт" else "Потребное, тс", e.newRequired, Modifier.weight(1f), onChange = { v -> vm.updateTurnaround(entryId) { it.copy(newRequired = v) } }, sunk = true)
                VuInput(if (actManual) "Фактическое, тс · правка" else if (calc?.actualText != null && e.newActual.isNotBlank()) "Фактическое, тс · расчёт" else "Фактическое, тс", e.newActual, Modifier.weight(1f), onChange = { v -> vm.updateTurnaround(entryId) { it.copy(newActual = v) } }, sunk = true)
            }
            if (calc != null && (reqManual || actManual)) {
                VuBtn("Пересчитать", { vm.updateTurnaround(entryId) { it.copy(newRequired = calc.requiredText ?: it.newRequired, newActual = calc.actualText ?: it.newActual) } }, Modifier.fillMaxWidth(), kind = VuBtnKind.Tonal, h = 48.dp)
            }
            if (calc != null) {
                if (calc.normChanged) VuHint("Норма изменилась: было ${calc.normBefore}, стало ${calc.normAfter} тс на 100 тс веса (${calc.category.title}).")
                if (calc.actual != null && calc.frontActual != null) {
                    VuHint("Фактическое = лицевое ${fmt(calc.frontActual!!, 1)} тс" + (if (calc.attachedTc > 0) " + прицеплено ${fmt(calc.attachedTc, 1)}" else "") + (if (calc.detachedTc > 0) " − отцеплено ${fmt(calc.detachedTc, 1)}" else "") + ".")
                } else if ((e.newWeight.isNotBlank() || e.newAxles.isNotBlank()) && e.newActual.isBlank()) {
                    VuHint("Введите фактическое нажатие или добавьте строки прицепленных/отцепленных вагонов.")
                }
                calc.requiredManualAxles?.let { m -> VuHint("Ручных тормозных осей потребно: $m" + (calc.manualAxlesBefore?.takeIf { it != m }?.let { " (было $it)" } ?: "") + " - по табл. III.4 на новый вес.") }
                val reqE = e.newRequired.toFiniteDoubleOrNull()
                val actE = e.newActual.toFiniteDoubleOrNull()
                val shownAct = actE ?: calc.actual
                val shownReq = reqE ?: calc.required
                if (shownAct != null && shownReq != null) {
                    if (calc.enough(reqE, actE) == true) VuVerdict(true, "Нажатия достаточно: ${fmt(shownAct, 1)} из ${fmt(shownReq, 1)} тс")
                    else VuVerdict(false, "Нажатия не хватает: ${fmt(shownAct, 1)} из ${fmt(shownReq, 1)} тс")
                }
            } else if (e.newWeight.isNotBlank() || e.newAxles.isNotBlank() || e.changes.isNotEmpty()) {
                VuHint("Заполните вес и нажатия на лицевой стороне - тогда нажатие рассчитается автоматически; пока можно ввести значения вручную.")
            }
            }
            Spacer(Modifier.height(2.dp))
            Text("", color = t.muted)
        }
    }
}

// ───────────────────────────── Шторки бланка ─────────────────────────────

@Composable
private fun SheetLabel(text: String, onHelp: (() -> Unit)? = null) {
    val t = vuColors()
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), Modifier.weight(1f), fontFamily = VuMono, fontSize = 11.sp, letterSpacing = 1.sp, color = t.muted)
        if (onHelp != null) VuHelp(onHelp, touch = 32.dp)
    }
}

@Composable
private fun OtherSheet(state: BrakeCertificateUiState, category: BrakeCategory, vm: BrakeCertificateViewModel, onDismiss: () -> Unit) {
    val t = vuColors()
    var help by remember { mutableStateOf<String?>(null) }
    help?.let { key -> HelpSheet(key, state, category, null, onDismiss = { help = null }) }
    VuSheet(title = "Другие данные", sub = "Графа справа от таблицы", onDismiss = onDismiss, footer = { VuBtn("Готово", onDismiss, Modifier.fillMaxWidth(), h = 56.dp) }) {
        SheetLabel("Композиционные колодки", onHelp = { help = "composite" })
        val share = when (state.compositeShare) { CompositeShare.P50 -> "К-50"; CompositeShare.P75 -> "К-75"; CompositeShare.P100 -> "К-100"; CompositeShare.NONE -> "" }
        VuCircled(listOf("К-50", "К-75", "К-100"), setOf(share)) { x ->
            val v = when (x) { "К-50" -> CompositeShare.P50; "К-75" -> CompositeShare.P75; else -> CompositeShare.P100 }
            vm.setCompositeShare(if (state.compositeShare == v) CompositeShare.NONE else v)
        }
        SheetLabel("Тормоза в составе", onHelp = { help = "flags" })
        VuCircled(listOf("Г", "ЭПТ", "ЭПП", "ДТ", "МРТ", "П", "В10", "РИЦ", "5 ХВ", "СТРП"), state.flags) { vm.toggleFlag(it) }
        Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VuInput("ТЦПВ, мм", state.tcpvText, Modifier.weight(1f), onChange = vm::setTcpv, sunk = true, onHelp = { help = "tcpv" })
                VuInput("Встр. (вагон)", state.vstrText, Modifier.weight(1f), onChange = vm::setVstr, sunk = true, text = true, onHelp = { help = "vstr" })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VuInput("ДПВ, кгс/см²", state.dpvText, Modifier.weight(1f), onChange = vm::setDpv, sunk = true, onHelp = { help = "dpv" })
                if (category.group == TrainGroup.FREIGHT || category.group == TrainGroup.FREIGHT_PASSENGER) {
                    VuInput("ВО2ХВ, с", state.vo2hvText, Modifier.weight(1f), onChange = vm::setVo2hv, sunk = true, onHelp = { help = "vo2hv" })
                } else Spacer(Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VuInput("Напряжение в хвосте, В", state.tailVoltageText, Modifier.weight(1f), onChange = vm::setTailVoltage, sunk = true, onHelp = { help = "tail_voltage" })
                VuInput("Пит. сеть", state.feedDensityText, Modifier.weight(1f), onChange = vm::setFeedDensity, sunk = true, text = true, onHelp = { help = "feed" })
            }
            VuInput("Зарядное давление, кгс/см²", state.chargePressureText, onChange = vm::setChargePressure, unit = "кгс/см²", sunk = true, onHelp = { help = "charge" })
            if (category.group == TrainGroup.FREIGHT || category.group == TrainGroup.FREIGHT_PASSENGER) {
                val axles = axlesForNorm(state)
                val charge = state.chargePressureText.toFiniteDoubleOrNull()
                val min = BrakeDensity.minTailPressure(charge, axles)
                val dpv = state.dpvText.toFiniteDoubleOrNull()
                when {
                    min == null -> VuHint("Норма ДПВ: укажите зарядное давление локомотива (поле выше или вкладка «Расчёт», 4,8-5,8) и число осей.")
                    dpv == null -> VuHint("Норма в хвосте: не менее ${fmt(min)} кгс/см² (п. 48 прил. 2 Правил ТО тормозного оборудования)")
                    else -> VuVerdict(dpv + 1e-9 >= min, if (dpv + 1e-9 >= min) "ДПВ в норме (не менее ${fmt(min)})" else "ДПВ ниже нормы: нужно не менее ${fmt(min)} кгс/см²", 14)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("", color = t.muted)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RowSheet(state: BrakeCertificateUiState, vm: BrakeCertificateViewModel, onDismiss: () -> Unit) {
    // Нажатие - строка ввода (источник истины), любое дробное значение; чипы - быстрый выбор типичных.
    var pressureText by remember { mutableStateOf("") }
    var axles by remember { mutableStateOf("") }
    val pressure = pressureText.toFiniteDoubleOrNull()?.takeIf { it > 0 }
    val n = axles.toIntOrNull()
    val ok = pressure != null && n != null && n > 0
    VuSheet(
        title = "Нажатие на ось", sub = "Строка таблицы бланка: любое значение, тс на ось", onDismiss = onDismiss,
        footer = {
            VuBtn(
                if (ok) "Добавить · $n × ${fmtP(pressure!!)} = ${fmt(pressure * n!!)} тс" else "Добавить", {
                    vm.setAxlesForPressure(pressure!!, axles); onDismiss()
                }, Modifier.fillMaxWidth(), h = 56.dp, enabled = ok,
            )
        },
    ) {
        VuInput("Нажатие на ось, тс", pressureText, onChange = { pressureText = it }, unit = "тс", placeholder = "например 8,25")
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OTHER_VALUES.forEach { v ->
                VuChip(fmtP(v), pressure == v, {
                    pressureText = fmtP(v)
                    axles = state.effectiveRows().firstOrNull { it.pressureText.toFiniteDoubleOrNull() == v }?.axlesText.orEmpty()
                }, Modifier.widthIn(min = 56.dp), small = true)
            }
        }
        Spacer(Modifier.height(14.dp))
        VuInput("Количество осей", axles, onChange = { axles = it }, unit = "осей", keyboard = KeyboardType.Number, maxLen = 4)
    }
}

@Composable
private fun HelpSheet(key: String, state: BrakeCertificateUiState, category: BrakeCategory, result: BrakeCertificateResult?, onDismiss: () -> Unit) {
    val t = vuColors()
    val hint = brakeHints[key] ?: if (key == "deficit") BrakeHint(
        "Недостаток нажатия", "Если нажатие ниже нормы (табл. III.1), но не ниже минимума (табл. III.2), скорость снижается за каждую недостающую тонну нажатия на 100 т веса поезда: на 2 км/ч - в грузовых, рефрижераторных и хозяйственных; на 1 км/ч - в пассажирских, грузопассажирских и рефрижераторных (свыше 90 км/ч) на спусках до 0,006, на более крутых - на 2 км/ч; результат округляется в меньшую сторону до кратного 5 (п. 35). Ниже минимума - скорость по таблицам не определяется (п. 37). Нормы и минимумы - «Справочник → Недостаток нажатия».", "Правила ТО тормозного оборудования, прил. 2, п. 33-35, 37",
    ) else null
    if (hint == null) { onDismiss(); return }
    val calc = when (key) {
        "required" -> result?.let { "${state.weightText} × ${category.norm} / 100 = ${fmt(it.requiredPressure)} тс (норма ${category.norm} тс на каждые 100 тс веса)" }
        "manual_req" -> result?.requiredManualAxles?.let { "${state.weightText} × ${fmt(state.manualInfo.perHundred, 1)} / 100 → $it осей" }
        else -> null
    }
    fun row(k: String, v: String, mono: Boolean = false) = @Composable {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            VuTag(k)
            Text(v, fontFamily = if (mono) VuMono else null, fontSize = 15.sp, fontWeight = if (mono) FontWeight.SemiBold else FontWeight.Normal, color = t.text, lineHeight = 22.sp)
            Box(Modifier.padding(top = 9.dp).fillMaxWidth().height(1.dp).background(t.border))
        }
    }
    VuSheet(title = hint.title, sub = "Графа бланка ВУ-45", onDismiss = onDismiss) {
        row("Что это", InstructionRef.annotate(hint.text))()
        if (calc != null) row("Ваш расчёт", calc, mono = true)()
        row("Источник", (hint.clause ?: InstructionRef.SHORT) + "\n" + InstructionRef.FULL)()
    }
}

// ───────────────────────────── Замеры ─────────────────────────────

@Composable
private fun MeasureTab(
    state: BrakeCertificateUiState,
    category: BrakeCategory,
    vm: BrakeCertificateViewModel,
    now: Long,
    kind: TimerKind,
    onKind: (TimerKind) -> Unit,
    onGroupClick: () -> Unit,
    onHelp: (String) -> Unit,
    onBackToTurnaround: () -> Unit,
) {
    val t = vuColors()
    val passenger = category.group == TrainGroup.PASSENGER
    val kinds = if (passenger) listOf(TimerKind.II, TimerKind.ROUTE) else TimerKind.values().toList()
    val active = if (kind in kinds) kind else TimerKind.II
    val group = state.effectiveDensityGroup()
    val charge = state.chargePressureText.toFiniteDoubleOrNull()
    val axles = axlesForNorm(state)
    val locoCount = state.locoCountText.trim().toIntOrNull() ?: 1
    val minTime = group?.let { BrakeDensity.minTimeSeconds(it, axles, charge, locoCount) }

    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val labels = kinds.map { k ->
            (when (k) { TimerKind.II -> "II"; TimerKind.IV -> "IV"; TimerKind.ROUTE -> "В пути" }) +
                (state.timerOf(k).let { tm -> if (tm.running) " · идёт" else tm.recordedSec?.let { " · ${fmt(it)} с" + if (tm.manual) "*" else "" } ?: "" })
        }
        VuTabs(labels, kinds.indexOf(active), { onKind(kinds[it]) }, mono = true)
        val turnApplies = TurnaroundPlanner.applies(category)
        var pickSheet by remember { mutableStateOf(false) }
        state.replaceConfirm?.let { rc ->
            com.z_company.route.component.AppAlertDialog(
                onDismissRequest = vm::dismissReplaceConfirm,
                title = "Заменить плотность?",
                text = "В записи «${rc.label}» уже есть плотность. Заменить ${fmt(rc.oldSec)} на ${fmt(rc.newSec)} с?",
                confirmText = "Заменить", onConfirm = { vm.writeRouteMeasurement(confirmed = true) },
                dismissText = "Отмена", onDismiss = vm::dismissReplaceConfirm,
            )
        }
        val pick = TurnaroundTarget.pick(state.turnaround, state.routeTargetEntryId, System.currentTimeMillis(), true)
        val pickedId = (pick as? TurnaroundPick.Existing)?.id
        if (active == TimerKind.ROUTE && turnApplies) {
            val label = pickedId?.let { id -> state.turnaround.firstOrNull { it.id == id } }?.let { turnaroundLabel(it) }
            Row(
                Modifier.fillMaxWidth().background(t.accentSoft, RoundedCornerShape(12.dp)).padding(start = 14.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (label != null) "Запишется в оборот: $label" else "Запишется в новую запись", Modifier.weight(1f), fontSize = 13.sp, color = t.text)
                Text("Выбрать", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = t.accent, modifier = Modifier.heightIn(min = 48.dp).clickable { pickSheet = true }.padding(horizontal = 12.dp, vertical = 14.dp))
            }
        }
        if (pickSheet) {
            VuSheet(title = "Куда записать замер", sub = "Запись оборота для плотности «В пути»", onDismiss = { pickSheet = false }) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    state.turnaround.forEach { e ->
                        TargetRow(
                            title = turnaroundLabel(e),
                            sub = e.densitySec?.let { "плотность уже записана: ${fmt(it)} с" },
                            selected = e.id == pickedId, enabled = true,
                        ) { vm.setRouteTarget(e.id); pickSheet = false }
                    }
                    TargetRow(title = "Новая запись", sub = null, selected = pick == TurnaroundPick.CreateNew, enabled = true) {
                        vm.setRouteTarget(TurnaroundTarget.NEW_ENTRY); pickSheet = false
                    }
                    VuBtn("К отметкам оборота ›", { pickSheet = false; onBackToTurnaround() }, Modifier.fillMaxWidth(), kind = VuBtnKind.Text, h = 52.dp)
                }
            }
        }
        TimerCard(
            title = when (active) {
                TimerKind.II -> "II положение — поездное"
                TimerKind.IV -> "IV положение — перекрыша с питанием"
                TimerKind.ROUTE -> "Контроль в пути"
            },
            subtitle = when (active) {
                TimerKind.II -> if (passenger) "Перекрыть комбинированный кран, подождать 20 с, затем «Старт» и замер падения давления в ТМ."
                else "После снижения давления в ГР на 0,4–0,5 кгс/см² от предельного — «Старт», на снижении ещё на 0,5 — «Стоп»."
                TimerKind.IV -> "Через 2 мин и более после ступени торможения (УР −0,5…0,7 кгс/см²), ручка в IV. Тот же замер."
                TimerKind.ROUTE -> "После стоянки, смены бригады и т.п.: отличие от замера II не более 20 %, иначе сокращённое опробование."
            },
            kind = active, timer = state.timerOf(active), now = now, passenger = passenger && active == TimerKind.II, vm = vm,
            transferred = when {
                active == TimerKind.II || active == TimerKind.IV -> "перенесено в бланк"
                else -> null
            },
            normText = when (active) {
                TimerKind.II -> if (!passenger && minTime != null) "Норма плотности во II-м: не менее ${BrakeDensity.formatNormSeconds(minTime)} сек"
                    else if (passenger) "Норма: падение давления в ТМ не более 0,2 кгс/см² за 1 мин (п. 50 прил. 2)" else null
                TimerKind.IV -> state.timerII.recordedSec?.let { "Норма плотности в IV-м: не менее ${BrakeDensity.formatNormSeconds(BrakeDensity.normIvSeconds(it))} сек (90 % от замера II)" }
                    ?: minTime?.let { "Норма плотности в IV-м: не менее ${BrakeDensity.formatNormSeconds(BrakeDensity.normIvSeconds(it))} сек" }
                TimerKind.ROUTE -> state.timerII.recordedSec?.let { "Допуск ±20 % от замера II: ${fmt(it * 0.8)}–${fmt(it * 1.2)} с" }
            },
            normMissing = when (active) {
                TimerKind.II -> if (passenger) null else when {
                    group == null -> "Нормы нет: выберите группу локомотива (или объём ГР) на «Расчёте»"
                    axles <= 0 -> "Нормы нет: укажите число осей на «Расчёте»"
                    else -> null
                }
                TimerKind.IV -> if (state.timerII.recordedSec != null || minTime != null) null else
                    if (group == null) "Нормы нет: нужен замер II или группа локомотива" else "Нормы нет: нужен замер II или число осей"
                TimerKind.ROUTE -> if (state.timerII.recordedSec != null) null else "Допуска нет: нужен замер II"
            },
            assess = { sec ->
                when (active) {
                    TimerKind.II -> if (passenger) BrakeDensity.assessPassenger(sec, state.timerII.dropText.toFiniteDoubleOrNull()) else BrakeDensity.assessFreightII(sec, minTime, groupSelected = group != null)
                    TimerKind.IV -> BrakeDensity.assessFreightIV(sec, state.timerII.recordedSec)
                    TimerKind.ROUTE -> BrakeDensity.assessRoute(sec, state.timerII.recordedSec)
                }
            },
            onHelp = { onHelp("density") },
            onWrite = if (active == TimerKind.ROUTE && turnApplies) ({ vm.writeRouteMeasurement() }) else null,
            onDropHelp = { onHelp("pass_drop") },
        )
        VuHint("Результаты замеров автоматически попадают во вкладку «Бланк». Экран не гаснет, пока идёт замер.", Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
private fun TimerCard(
    title: String,
    subtitle: String,
    kind: TimerKind,
    timer: DensityTimerState,
    now: Long,
    passenger: Boolean,
    vm: BrakeCertificateViewModel,
    assess: (Double) -> BrakeDensity.Assessment,
    transferred: String?,
    normText: String?,
    normMissing: String? = null,
    onHelp: () -> Unit,
    onWrite: (() -> Unit)? = null,
    onDropHelp: (() -> Unit)? = null,
) {
    val t = vuColors()
    val elapsed = timer.elapsedMs(now)
    val hasValue = timer.recordedSec != null
    // «done» - есть результат замера секундомером; ручной ввод секундомер не трогает (0,0 и «Старт»)
    val done = hasValue && !timer.manual
    VuCard {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = t.text)
                VuHelp(onHelp)
            }
            VuHint(subtitle, Modifier.padding(top = 4.dp))
            // норма показывается всегда: до старта, во время замера и после
            Box(Modifier.padding(top = 8.dp)) {
                if (normText != null) VuNormLine(normText) else if (normMissing != null) VuHint(normMissing)
            }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp).height(20.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                if (timer.running) {
                    Box(Modifier.width(8.dp).height(8.dp).background(t.accent, RoundedCornerShape(4.dp)))
                    Text("  ЗАМЕР ИДЁТ · ЭКРАН НЕ ГАСНЕТ", fontFamily = VuMono, fontSize = 11.sp, letterSpacing = 1.sp, color = t.accent)
                }
            }
            Text(
                formatTimer(elapsed), Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center,
                fontFamily = VuMono, fontSize = 72.sp, fontWeight = FontWeight.Bold, letterSpacing = (-3).sp,
                color = if (timer.running) t.accent else if (!done && elapsed == 0L) t.faint else t.text, maxLines = 1,
            )
            if (hasValue && !timer.running && onWrite != null) {
                VuBtn("Записать", onWrite, Modifier.fillMaxWidth().padding(top = 18.dp), h = 64.dp)
            }
            Row(Modifier.fillMaxWidth().padding(top = if (hasValue && onWrite != null) 8.dp else 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                when {
                    timer.running -> VuBtn("Стоп", { vm.stopTimer(kind) }, Modifier.fillMaxWidth(), kind = VuBtnKind.Cta, h = 64.dp)
                    done -> {
                        VuBtn("Заново", { vm.startTimer(kind) }, Modifier.weight(1f), kind = VuBtnKind.Tonal, h = 64.dp)
                        VuBtn("Сбросить", { vm.resetTimer(kind) }, kind = VuBtnKind.Text, h = 64.dp)
                    }
                    else -> VuBtn("Старт", { vm.startTimer(kind) }, Modifier.fillMaxWidth(), h = 64.dp)
                }
            }
            if (!timer.running) {
                var manualText by remember(kind) { mutableStateOf("") }
                val parsed = BrakeDensity.parseManualSeconds(manualText)
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    VuInput("или введите вручную, с", manualText, Modifier.weight(1f), onChange = { manualText = it }, unit = "с", sunk = true, error = manualText.isNotBlank() && parsed == null)
                    VuBtn("Принять", { if (vm.setManualDensity(kind, manualText)) manualText = "" }, kind = VuBtnKind.Tonal, h = 56.dp, enabled = parsed != null)
                }
            }
            timer.recordedSec?.let { rec ->
                val a = assess(rec)
                val what = if (timer.manual) "введено вручную" else "Замер"
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (timer.manual) Text("Убрать значение", Modifier.clickable { vm.resetTimer(kind) }.heightIn(min = 40.dp).padding(vertical = 10.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = t.accent)
                    when (a.verdict) {
                        BrakeDensity.Verdict.OK -> VuVerdict(true, "$what: ${fmt(rec)} с — норма", 16)
                        BrakeDensity.Verdict.FAIL -> VuVerdict(false, "$what: ${fmt(rec)} с — " + (if (kind == TimerKind.ROUTE && !passenger) "отличается от записанного более чем на 20 %" else "недостаточно"), 16)
                        BrakeDensity.Verdict.NO_NORM -> Text("$what: ${fmt(rec)} с — оценка недоступна", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = t.muted)
                    }
                    val left = if (a.verdict == BrakeDensity.Verdict.NO_NORM) 0.dp else 28.dp
                    val noteText = when (a.verdict) {
                        BrakeDensity.Verdict.NO_NORM -> a.text.replaceFirstChar { it.uppercase() }.let { if (a.text.startsWith("Выберите")) "$it — появится норма" else it }
                        else -> a.text + (transferred?.let { " · $it" } ?: "")
                    }
                    if (a.verdict == BrakeDensity.Verdict.NO_NORM) VuHint(noteText, Modifier.padding(start = left))
                    else if (a.verdict == BrakeDensity.Verdict.FAIL || normText == null) VuNormLine(noteText, Modifier.padding(start = left))
                    else transferred?.let { VuHint(it, Modifier.padding(start = left)) }
                }
            }
            if (passenger) {
                Box(Modifier.padding(top = 16.dp)) { VuInput("Падение давления в ТМ, кгс/см²", timer.dropText, onChange = { vm.setDrop(kind, it) }, sunk = true, onHelp = onDropHelp) }
            }
        }
    }
}

// ───────────────────────────── Справочник ─────────────────────────────

@Composable
private fun ReferenceTab(state: BrakeCertificateUiState, category: BrakeCategory, open: Int, onOpen: (Int) -> Unit, onOpenMeasure: () -> Unit) {
    val t = vuColors()
    val group = state.effectiveDensityGroup()
    val axles = axlesForNorm(state)
    val minTime = group?.let { BrakeDensity.minTimeSeconds(it, axles, state.chargePressureText.toFiniteDoubleOrNull(), state.locoCountText.trim().toIntOrNull() ?: 1) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VuTag("Справочник · " + InstructionRef.SHORT)
        brakeReference.forEachIndexed { i, card ->
            VuCard {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { onOpen(if (open == i) -1 else i) }.padding(start = 18.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", Modifier.width(28.dp), fontFamily = VuMono, fontSize = 12.sp, color = t.faint)
                    Text(card.title, Modifier.weight(1f).padding(vertical = 8.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = t.text, lineHeight = 20.sp)
                    Text(if (open == i) "▴" else "▾", Modifier.padding(horizontal = 14.dp), fontSize = 14.sp, color = t.muted)
                }
                if (open == i) {
                    Column(Modifier.padding(start = 46.dp, end = 18.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        card.lines.forEach { line -> RefLine(line) }
                        Text("Источник: " + InstructionRef.FULL, fontSize = 12.sp, color = t.muted, lineHeight = 16.sp)
                        if (card.title.startsWith("Плотность")) {
                            Row(Modifier.fillMaxWidth().background(t.accentSoft, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    if (group != null) "Ваш поезд: группа ${group.name.removePrefix("G")}, $axles осей" else "Группа локомотива не выбрана",
                                    Modifier.weight(1f), fontSize = 13.sp, color = t.text,
                                )
                                if (minTime != null) Text("≥ ${fmt(minTime)} с", fontFamily = VuMono, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = t.accent)
                            }
                            VuBtn("Открыть замер ›", onOpenMeasure, kind = VuBtnKind.Text)
                        }
                    }
                }
            }
        }
        VuHint(
            "Расчёт по Правилам технического обслуживания тормозного оборудования и управления тормозами (СЖТ, протокол № 60 от 6-7.05.2014, с изменениями до 2017 г.), прил. 2. Форма ВУ-45 - Инструкция 808-2022 ПКБ ЦВ, прил. М. Справочный инструмент: официальной справкой ВУ-45 является документ, выданный осмотрщиком вагонов. Нормы могут меняться - сверяйте с действующими документами.",
            Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )
    }
}

/** Подпись записи оборота: станция (или «станция не указана») и время. */
private fun turnaroundLabel(e: TurnaroundEntry): String {
    val name = e.stationName.ifBlank { "станция не указана" }
    return if (e.readinessTime.isNotBlank()) "$name, ${e.readinessTime}" else name
}

/** Строка изменения состава: отцеплено/прицеплено, нажатие на ось (значения как на лицевой таблице или своё) и число осей. */
@Composable
private fun WagonChangeRow(c: WagonChange, available: List<AxleRow>, onChange: (WagonChange) -> Unit, onRemove: () -> Unit) {
    val t = vuColors()
    Column(Modifier.fillMaxWidth().border(1.dp, t.border, RoundedCornerShape(12.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            VuChip("Отцеплено", !c.attached, { onChange(c.copy(attached = false)) }, Modifier.weight(1f), small = true)
            VuChip("Прицеплено", c.attached, { onChange(c.copy(attached = true)) }, Modifier.weight(1f), small = true)
            Text("×", Modifier.clickable(onClick = onRemove).heightIn(min = 40.dp).widthIn(min = 40.dp).padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 24.sp, color = t.muted, textAlign = TextAlign.Center)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VuInput("Осей", c.axlesText, Modifier.weight(1f), onChange = { v -> onChange(c.copy(axlesText = v)) }, keyboard = KeyboardType.Number, sunk = true)
            VuInput("Нажатие на ось, тс", c.pressureText, Modifier.weight(1f), onChange = { v -> onChange(c.copy(pressureText = v)) }, sunk = true)
        }
        // Отцепить можно только то, что есть на лицевой (нажатия с осями); прицепить - любое из печатных значений.
        if (!c.attached && available.isEmpty()) VuHint("На лицевой нет нажатий с осями - заполните таблицу нажатий, тогда здесь можно будет выбрать, что отцепить.")
        else if (!c.attached) available.firstOrNull { it.pressurePerAxle == c.pressure }?.let { VuHint("На лицевой по ${fmt(it.pressurePerAxle)} тс/ось: ${it.axles} осей") }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (if (c.attached) PRINTED else available.map { it.pressurePerAxle }).forEach { p ->
                val txt = fmt(p)
                VuChip(txt, c.pressure == p, { onChange(c.copy(pressureText = txt)) }, small = true)
            }
        }
    }
}

@Composable
private fun CompositionHelpSheet(onDismiss: () -> Unit) {
    val t = vuColors()
    VuSheet(title = "Как считается", sub = "Графы 3-6 оборота при изменении состава", onDismiss = onDismiss, footer = { VuBtn("Понятно", onDismiss, Modifier.fillMaxWidth(), h = 56.dp) }) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Потребное нажатие = N × Q / 100, дробное значение округляется до ближайшего большего целого (п. 8 прил. М Инструкции 808-2022). Q - новый вес поезда, который вводите вы (с учётом локомотива, если он учитывается), N - норма на 100 тс веса.", fontSize = 14.sp, color = t.text, lineHeight = 20.sp)
            Text("N берётся заново по категории поезда (табл. III.1 прил. 2 Правил ТО тормозного оборудования). У порожних поездов категория зависит от числа осей: до 350 осей - 55, от 350 до 400 включительно - 44, более 400 до 520 - 33 тс на 100 тс веса. Поэтому после прицепки или отцепки вагонов норма может измениться - тогда это показано отдельной строкой.", fontSize = 14.sp, color = t.text, lineHeight = 20.sp)
            Text("Фактическое нажатие считается по осям (табл. III.3 прил. 2: нажатие на ось × число осей): фактическое лицевой стороны + прицеплено (оси × нажатие) − отцеплено (оси × нажатие). Число осей: оси лицевой ± оси строк. Для каждой группы вагонов добавьте строку: «Отцеплено»/«Прицеплено», нажатие на ось и число осей. Если отцепленного нажатия нет на лицевой, расчёт всё равно идёт по введённому, но показывается предупреждение.", fontSize = 14.sp, color = t.text, lineHeight = 20.sp)
            Text("Если строк нет, фактическое вводится вручную. Ручная правка помечается «правка», «Пересчитать» возвращает расчёт по строкам.", fontSize = 14.sp, color = t.text, lineHeight = 20.sp)
            Text("Ручные тормозные оси: вес × норма по табл. III.4 (не менее 0,6 оси на 100 тс веса, п. 45), округление вверх - пересчитывается по новому весу.", fontSize = 14.sp, color = t.text, lineHeight = 20.sp)
            Text("Пример. На лицевой 220 осей, фактическое 1500 тс. Отцепили 20 осей по 7 тс/ось: −140 тс, осталось 200 осей. Прицепили 10 осей по 5 тс/ось: +50 тс, стало 210 осей. Итого фактическое = 1500 − 140 + 50 = 1410 тс. Для порожнего поезда на 360 осей (N = 44) отцепка 20 осей даёт 340 осей и N = 55 - норма выросла. Расчёт справочный; окончательно - по Правилам и Инструкции.", fontSize = 14.sp, color = t.muted, lineHeight = 20.sp)
        }
    }
}

/** Выбор поезда или локомотива текущего маршрута: шторка в стиле «Текущего маршрута» главного экрана. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun FillPickSheet(pick: FillPick, vm: BrakeCertificateViewModel) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = vm::dismissFillPick,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (pick.kind == FillPick.Kind.TRAIN) "Поезда · ${pick.trains.size}" else "Локомотивы · ${pick.locos.size}",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
            )
            if (pick.kind == FillPick.Kind.TRAIN) {
                pick.trains.forEach { train ->
                    val first = train.stations.firstOrNull()?.stationName
                    val last = if (train.stations.size > 1) train.stations.last().stationName else null
                    UnitSheetRow(com.z_company.route.R.drawable.ic_card_train_ref, routeUnitFullName(train.number, train.servicePhase, first, last)) { vm.pickTrain(train) }
                }
            } else {
                pick.locos.forEachIndexed { index, loco ->
                    UnitSheetRow(com.z_company.route.R.drawable.ic_card_locomotive_ref, locomotiveName(loco, index + 1)) { vm.pickLoco(loco) }
                }
            }
        }
    }
}

/** Ручной ввод плотности II/IV из бланка: значение участвует в оценке как замер; «Открыть замер» - перейти к секундомеру. */
@Composable
private fun ManualDensitySheet(state: BrakeCertificateUiState, kind0: TimerKind, vm: BrakeCertificateViewModel, onDismiss: () -> Unit, onOpenMeasure: (TimerKind) -> Unit) {
    var kind by remember { mutableStateOf(kind0) }
    val passenger = BrakeNorms.category(state.categoryId)?.group == TrainGroup.PASSENGER
    // оба положения хранятся в шторке одновременно: переключение чипа не теряет введённое, «Готово» сохраняет оба
    fun cur(k: TimerKind) = if (k == TimerKind.II) state.timerII else state.timerIV
    val initial = remember { TimerKind.values().filter { it != TimerKind.ROUTE }.associateWith { k -> cur(k).recordedSec?.let { fmt(it) } ?: "" } }
    val texts = remember { mutableStateMapOf(TimerKind.II to initial.getValue(TimerKind.II), TimerKind.IV to initial.getValue(TimerKind.IV)) }
    val cleared = remember { mutableStateMapOf(TimerKind.II to false, TimerKind.IV to false) }
    val pStarts = remember { mutableStateMapOf(TimerKind.II to cur(TimerKind.II).grStartText, TimerKind.IV to cur(TimerKind.IV).grStartText) }
    val pEnds = remember { mutableStateMapOf(TimerKind.II to cur(TimerKind.II).grEndText, TimerKind.IV to cur(TimerKind.IV).grEndText) }
    val text = texts.getValue(kind)
    val parsed = BrakeDensity.parseManualSeconds(text)
    VuSheet(
        title = "Плотность, с", sub = "Введите вручную или замерьте секундомером", onDismiss = onDismiss,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                VuBtn("Замерить ›", { onOpenMeasure(kind) }, kind = VuBtnKind.Text, h = 56.dp)
                VuBtn("Готово", {
                    listOf(TimerKind.II, TimerKind.IV).forEach { k ->
                        vm.applyDensityCommit(k, BrakeDensity.commitDensity(texts.getValue(k), initial.getValue(k), cleared.getValue(k)))
                        vm.setPressures(k, pStarts.getValue(k), pEnds.getValue(k))
                    }
                    onDismiss()
                }, Modifier.weight(1f), h = 56.dp)
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VuChip("II положение" + if (texts.getValue(TimerKind.II).isNotBlank()) " ✓" else "", kind == TimerKind.II, { kind = TimerKind.II }, Modifier.weight(1f))
                VuChip("IV положение" + if (texts.getValue(TimerKind.IV).isNotBlank()) " ✓" else "", kind == TimerKind.IV, { kind = TimerKind.IV }, Modifier.weight(1f))
            }
            VuInput("Время падения давления, с", text, onChange = { texts[kind] = it; cleared[kind] = false }, unit = "с", sunk = true, error = text.isNotBlank() && parsed == null)
            if (initial.getValue(kind).isNotBlank() || text.isNotBlank()) {
                Text("Очистить", Modifier.clickable { texts[kind] = ""; cleared[kind] = true }.heightIn(min = 40.dp).padding(vertical = 10.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = vuColors().accent)
            }
            if (cur(kind).manual) VuHint("Сейчас значение введено вручную.")
            VuTag("Давление в ГР, кгс/см²")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VuInput("В начале замера", pStarts.getValue(kind), Modifier.weight(1f), onChange = { pStarts[kind] = it }, sunk = true)
                VuInput("В конце замера", pEnds.getValue(kind), Modifier.weight(1f), onChange = { pEnds[kind] = it }, sunk = true)
            }
            BrakeDensity.checkPressure(kind.name, pStarts.getValue(kind), pEnds.getValue(kind), passenger)?.let { c ->
                when (c.ok) {
                    true -> VuVerdict(true, c.text, 14)
                    false -> VuVerdict(false, c.text, 14)
                    null -> VuHint(c.text)
                }
            }
        }
    }
}

/** Строка выбора записи оборота: радио-метка слева, подпись, пометка; недоступна, если плотность уже записана. */
@Composable
private fun TargetRow(title: String, sub: String?, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val t = vuColors()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(24.dp).border(2.dp, if (selected) t.accent else t.borderStrong, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(12.dp).background(t.accent, CircleShape))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) t.text else t.faint)
            if (sub != null) Text(sub, fontSize = 12.sp, color = t.muted)
        }
    }
}

/** Единая подпись под значением на лицевой при учёте локомотива (вес и оси локомотива). */
@Composable
private fun LocoCaption(l: LocoShare) {
    val t = vuColors()
    Text(
        "с учётом локомотива: ${fmt(l.weight)} т, ${l.axles} осей",
        Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 4.dp),
        fontSize = 10.sp, color = t.muted, lineHeight = 13.sp, textAlign = TextAlign.End,
    )
}

/** Минуты суток для момента [ms] в часовом поясе устройства (для порядка записей оборота). */
private fun minutesOfDay(ms: Long): Int {
    val c = java.util.Calendar.getInstance().apply { timeInMillis = ms }
    return c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE)
}

/**
 * Строка карточки «Справочника». Формат: «#G|#Y|#R|» в начале - цветная плашка-маркер (зелёный/жёлтый/красный); « ‖ тег» в конце - ссылка на пункт
 * нормативки мелким шрифтом справа; иначе обычная строка.
 */
@Composable
private fun RefLine(raw: String) {
    val t = vuColors()
    val marker = if (raw.length > 2 && raw[0] == '#' && raw[2] == '|') raw[1] else null
    val body = if (marker != null) raw.substring(3) else raw
    val parts = body.split(" ‖ ", limit = 2)
    val text = parts[0]
    val tag = parts.getOrNull(1)
    val color = when (marker) { 'G' -> t.success; 'Y' -> t.warning; 'R' -> t.danger; else -> null }
    Row(
        Modifier.fillMaxWidth().then(if (color != null) Modifier.background(color.copy(alpha = 0.14f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 8.dp) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top,
    ) {
        if (color != null) Box(Modifier.padding(top = 4.dp).width(6.dp).height(18.dp).background(color, RoundedCornerShape(3.dp)))
        Column(Modifier.weight(1f)) {
            // ссылки на пункты - тегом в конце строки, поэтому текст не раскрывается «аннотацией» источника
            Text(if (tag != null || marker != null) text else InstructionRef.annotate(text), fontSize = 14.sp, color = t.text, lineHeight = 20.sp)
            if (tag != null) Text(tag, fontSize = 10.sp, color = t.muted, lineHeight = 13.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
        }
    }
}
