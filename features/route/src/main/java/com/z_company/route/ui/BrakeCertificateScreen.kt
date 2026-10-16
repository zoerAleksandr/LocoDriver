package com.z_company.route.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.z_company.domain.brake.AxleSumCheck
import com.z_company.domain.brake.GradeClass
import com.z_company.domain.brake.InstructionRef
import com.z_company.domain.brake.SlopeStep
import com.z_company.domain.brake.SpeedLimit
import com.z_company.domain.brake.StopClass
import com.z_company.domain.brake.TrainGroup
import com.z_company.domain.brake.TurnaroundEntry
import com.z_company.domain.brake.TurnaroundPlanner
import com.z_company.domain.util.toFiniteDoubleOrNull
import com.z_company.route.ui.settings.SettingsPickerOption
import com.z_company.route.ui.settings.SettingsPickerSheet
import com.z_company.route.viewmodel.BrakeCertificateUiState
import com.z_company.route.viewmodel.BrakeCertificateViewModel
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
    data object Other : BrakeSheet
    data object RowPressure : BrakeSheet
    data class Turnaround(val entryId: String) : BrakeSheet
    data class Help(val key: String) : BrakeSheet
}

private val TABS = listOf("Расчёт", "Бланк", "Замеры", "Справочник")
private val PRINTED = listOf(2.5, 3.5, 5.0, 6.0, 6.5, 7.0, 8.0, 8.5, 9.0, 10.0, 12.0, 15.0)
private val OTHER_VALUES = listOf(1.25, 2.5, 3.5, 5.0, 6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 18.0, 21.0)

/** Число для показа: без потери значащих дробных разрядов (до [digits] знаков), без хвостовых нулей, запятая. */
private fun fmt(v: Double, digits: Int = 2): String {
    val factor = Math.pow(10.0, digits.toDouble())
    val r = Math.round(v * factor) / factor
    val s = if (r % 1.0 == 0.0) r.toLong().toString() else r.toString()
    return s.replace('.', ',')
}

private fun fmtP(p: Double): String = BrakeCertificateViewModel.formatPressure(p)

private fun formatTimer(ms: Long): String {
    val tenths = ms / 100
    return "%02d:%02d.%d".format(tenths / 600, (tenths % 600) / 10, tenths % 10)
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
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var side by rememberSaveable { mutableIntStateOf(0) }
    var measureKind by rememberSaveable { mutableStateOf(TimerKind.II.name) }
    var sheet by remember { mutableStateOf<BrakeSheet?>(null) }
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

    Scaffold(
        containerColor = t.bg,
        topBar = {
            TopAppBar(
                title = { Text("Справка ВУ-45", style = MaterialTheme.typography.titleMedium, color = t.text) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(com.z_company.core.R.drawable.ic_arrow_back), contentDescription = "Назад", tint = t.text)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)) { VuTabs(TABS, tab, { tab = it }) }
            when (tab) {
                0 -> CalcTab(
                    state, category, result, viewModel,
                    onSheet = { sheet = it },
                    onOpenBlank = { tab = 1; side = 0 },
                )
                1 -> BlankTab(
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
                    onBackToTurnaround = { tab = 1; side = 1 },
                )
                else -> ReferenceTab(state, category, onOpenMeasure = { tab = 2 })
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
            title = "Группа локомотива по объёму ГР",
            hint = "Строка табл. IV.1 - общий объём главных резервуаров локомотива; при другом объёме - ближайший наименьший (прим. 3 к табл. IV.1 прил. 2 ${InstructionRef.GENITIVE}). Примеры серий ориентировочные, проверьте по паспорту локомотива.",
            onDismiss = { sheet = null },
            options = buildList {
                DensityLocoGroup.values().forEach { g ->
                    add(SettingsPickerOption(label = g.title, sub = g.examplesText, selected = g == state.densityGroup, onSelect = { viewModel.setDensityGroup(g) }))
                }
                add(SettingsPickerOption(label = "Не выбрана", sub = "По серии локомотива из маршрута, если она есть в примерах", selected = state.densityGroup == null, onSelect = { viewModel.setDensityGroup(null) }))
            },
        )
        BrakeSheet.Other -> OtherSheet(state, category, viewModel, onDismiss = { sheet = null })
        BrakeSheet.RowPressure -> RowSheet(state, viewModel, onDismiss = { sheet = null })
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VuInput(
                    "Категория поезда", "${category.title.substringBefore(" / ").substringBefore(" (")} · ${category.norm} тс на 100 тс веса",
                    onClick = { onSheet(BrakeSheet.Category) }, onHelp = { onSheet(BrakeSheet.Help("category")) },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VuInput("Вес состава, т", state.weightText, Modifier.weight(1f), onChange = vm::setWeight, unit = "т", placeholder = "0", onHelp = { onSheet(BrakeSheet.Help("weight")) })
                    VuInput("Всего осей", state.totalAxlesText, Modifier.weight(1f), onChange = vm::setTotalAxles, unit = "осей", placeholder = "0", keyboard = KeyboardType.Number, onHelp = { onSheet(BrakeSheet.Help("axles")) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.rowsMode) {
                        val rows = state.effectiveRows()
                        VuInput(
                            "Нажатие на ось, тс", "по типам · ${rows.size} стр.", Modifier.weight(1f),
                            onClick = onOpenBlank, onHelp = { onSheet(BrakeSheet.Help("row")) },
                        )
                    } else {
                        VuInput("Нажатие на ось, тс", state.avgPressureText, Modifier.weight(1f), onChange = vm::setAvgPressure, unit = "тс", placeholder = "0", error = low, onHelp = { onSheet(BrakeSheet.Help("row")) })
                    }
                    if (category.manualBrakesRequired) {
                        VuInput(
                            "Ручных на 100 тс веса", (if (state.manualOverride == null) "Авто · " else "") + fmt(state.manualInfo.perHundred, 1), Modifier.weight(1f),
                            onClick = { onSheet(BrakeSheet.ManualNorm) }, onHelp = { onSheet(BrakeSheet.Help("manual_req")) },
                        )
                    } else Spacer(Modifier.weight(1f))
                }
                if (state.rowsMode) AxleSumWarning(state, vm)
                Box(Modifier.padding(start = 0.dp)) { VuBtn("Оси по типам вагонов ›", onClick = onOpenBlank, kind = VuBtnKind.Text, h = 40.dp) }
                if (category.manualBrakesRequired) VuHint(state.manualInfo.text.replaceFirstChar { it.uppercase() }, Modifier.padding(horizontal = 4.dp))
                CompositeRow(state, vm, onHelp = { onSheet(BrakeSheet.Help("composite")) })
                // Руководящий спуск
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Руководящий спуск, ‰", Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = t.muted)
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
            if (category.group != TrainGroup.PASSENGER) DensityCard(state, vm, onGroup = { onSheet(BrakeSheet.DensityGroup) }, onHelp = onSheet)
            FillFromRoute(state, vm)
            if (calculated) ResultCard(result, state, category, onOpenBlank)
            if (low && result != null && BrakeStatusResolver.resolve(result, true).level == StatusLevel.DANGER) DeficitBlock(result, category, onHelp = { onSheet(BrakeSheet.Help("deficit")) })
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
    VuCard {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp).alpha(if (applicable) 1f else 0.5f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Автоблокировка", Modifier.weight(1f), fontSize = 16.sp, color = t.text)
            VuHelp(onHelp, touch = 40.dp)
            Switch(
                checked = state.autoblock3, onCheckedChange = onChange, modifier = Modifier.padding(horizontal = 8.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = t.accentInk, checkedTrackColor = t.accent, checkedBorderColor = t.accent,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline, uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant, uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
    }
}

/** Параметры плотности тормозной сети (общее состояние «Расчёта», «Бланка» и «Замеров»): зарядное давление, число локомотивов (СМЕ), группа по объёму ГР. */
@Composable
private fun DensityCard(state: BrakeCertificateUiState, vm: BrakeCertificateViewModel, onGroup: () -> Unit, onHelp: (BrakeSheet) -> Unit) {
    val group = state.densityGroup ?: BrakeDensity.groupBySeries(state.locoSeriesText)
    val charge = state.chargePressureText.toFiniteDoubleOrNull()
    val axles = axlesForNorm(state)
    val locoCount = state.locoCountText.trim().toIntOrNull() ?: 1
    val minTime = group?.let { BrakeDensity.minTimeSeconds(it, axles, charge, locoCount) }
    VuCard {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            VuTag("Плотность тормозной сети")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VuInput("Зарядное давление", state.chargePressureText, Modifier.weight(1f), onChange = vm::setChargePressure, unit = "кгс/см²", sunk = true, onHelp = { onHelp(BrakeSheet.Help("charge")) })
                VuInput("Локомотивов (СМЕ)", state.locoCountText, Modifier.weight(1f), onChange = vm::setLocoCount, sunk = true, keyboard = KeyboardType.Number, onHelp = { onHelp(BrakeSheet.Help("sme")) })
            }
            VuInput(
                "Группа локомотива (объём ГР)",
                group?.let { it.title + (if (state.densityGroup == null) " · по серии" else "") } ?: "Не выбрана",
                onClick = onGroup, sunk = true, onHelp = { onHelp(BrakeSheet.Help("group")) },
            )
            VuHint(
                when {
                    group == null -> "Без группы норма плотности (табл. IV.1) не определяется: выберите группу по объёму главных резервуаров локомотива."
                    minTime == null -> "Норма II положения: длина состава должна быть 1-530 осей (сейчас: $axles)."
                    else -> "Норма II положения по табл. IV.1: не менее ${fmt(minTime)} с (${group.title}, $axles осей" + (if (BrakeDensity.chargeFactor(charge) < 1.0) ", с поправкой на зарядное давление" else "") + ")."
                },
            )
        }
    }
}

@Composable
private fun FillFromRoute(state: BrakeCertificateUiState, vm: BrakeCertificateViewModel) {
    val t = vuColors()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        VuBtn(
            if (state.filling) "Заполняю…" else "Заполнить из маршрута", { vm.fillFromRoute() },
            Modifier.fillMaxWidth(), kind = VuBtnKind.Tonal, enabled = !state.filling,
        )
        val text = state.fillReport?.text
        if (text != null) VuHint(text, Modifier.padding(horizontal = 4.dp), color = t.muted)
        if (state.fillReport?.conflicts?.isNotEmpty() == true) {
            VuBtn("Заменить введённое данными маршрута", { vm.fillFromRoute(force = true) }, Modifier.fillMaxWidth(), kind = VuBtnKind.Text, h = 44.dp)
        }
        else if (state.routeId == null) VuHint("Экран открыт без маршрута - данные поезда и локомотива можно ввести вручную.", Modifier.padding(horizontal = 4.dp))
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
private fun DeficitBlock(result: BrakeCertificateResult, category: BrakeCategory, onHelp: () -> Unit) {
    val t = vuColors()
    val k = if (category.group == TrainGroup.FREIGHT) "2 км/ч" else "1 км/ч (спуск до 0,006) или 2 км/ч (круче)"
    val items = listOf(
        "Если нажатие не ниже допускаемого минимума ${InstructionRef.table("III.2")}, скорость табл. III.1 снижается на $k за каждую недостающую тонну на 100 тс веса, результат округляется вниз до кратного 5 (${InstructionRef.p("35")}).",
        "Снижение допустимо только в случаях, указанных в ${InstructionRef.p("4.2-4.3")} (выключены неисправные автотормоза в пути; особые составы).",
        "Если нажатие ниже допускаемого минимума, в исключительных случаях допускаемую скорость устанавливает начальник железной дороги по номограммам, на 20 % ниже определённой по ним (${InstructionRef.p("5.1")}). Подробнее - «Справочник».",
    )
    var open by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().background(t.danger.copy(alpha = 0.12f), RoundedCornerShape(16.dp)).padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
    ) {
        Row(Modifier.clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
            Text("Что делать при нехватке нажатия", Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = t.danger)
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
    var rekv by rememberSaveable { mutableStateOf(false) }
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
                PaperHeader(state, vm, open = rekv, onToggle = { rekv = !rekv }, side = effSide == 1, onHelp = { onSheet(BrakeSheet.Help(it)) })
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
    val hh = state.issueTimeText.substringBefore(':', state.issueTimeText)
    val mm = if (state.issueTimeText.contains(':')) state.issueTimeText.substringAfter(':') else ""
    Column(Modifier.padding(top = 10.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                VuPrint("Штемпель станции", 12)
                if (open) {
                    DashedBox(Modifier.padding(top = 6.dp).width(112.dp).height(62.dp)) {
                        VuInk(state.stationText, Modifier.padding(horizontal = 4.dp), onChange = vm::setStation, placeholder = "станция", align = TextAlign.Center)
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
                        VuInk(hh, Modifier.width(38.dp), onChange = { vm.setIssueTime(it.take(2) + ":" + mm) }, align = TextAlign.End, keyboard = KeyboardType.Number, maxLen = 2)
                        VuPrint("ч.", 12)
                        VuInk(mm, Modifier.width(38.dp), onChange = { vm.setIssueTime(hh + ":" + it.take(2)) }, align = TextAlign.End, keyboard = KeyboardType.Number, maxLen = 2)
                        VuPrint("м.", 12)
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
                    VuInk(state.dateText, Modifier.width(128.dp), onChange = vm::setDate, maxLen = 10)
                }
                VuFillRow {
                    VuPrint("Локомотив, серия")
                    VuInk(state.locoSeriesText, Modifier.weight(1f), onChange = vm::setLocoSeries, maxLen = 12)
                    VuPrint("№")
                    VuInk(state.locoNumberText, Modifier.width(80.dp), onChange = vm::setLocoNumber, maxLen = 8)
                    VuMiniHelp { onHelp("loco") }
                }
                VuFillRow {
                    VuPrint("Поезд №")
                    VuInk(state.trainNumberText, Modifier.width(100.dp), onChange = vm::setTrainNumber, maxLen = 10)
                    Spacer(Modifier.weight(1f))
                    Text("Свернуть ▴", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = t.accent, modifier = Modifier.clickable(onClick = onToggle).heightIn(min = 40.dp).padding(horizontal = 4.dp))
                }
            } else {
                val summary = listOf(
                    (state.locoSeriesText + (if (state.locoNumberText.isNotBlank()) "-" + state.locoNumberText else "")).trim().takeIf { it.isNotBlank() },
                    state.trainNumberText.takeIf { it.isNotBlank() }?.let { "поезд $it" },
                    state.dateText.takeIf { it.isNotBlank() },
                    state.issueTimeText.takeIf { it.length > 1 },
                ).filterNotNull().joinToString(" · ")
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp)
                        .drawBehind {
                            val w = 1.dp.toPx()
                            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                            drawLine(t.borderStrong, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), w, pathEffect = dash)
                            drawLine(t.borderStrong, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height), w, pathEffect = dash)
                        }
                        .clickable(onClick = onToggle).heightIn(min = 48.dp).padding(start = 14.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(summary.ifBlank { "Штемпель, время, дата, локомотив, поезд — необязательно" }, Modifier.weight(1f), fontSize = 12.sp, color = t.muted, lineHeight = 16.sp)
                    Text(if (summary.isBlank()) "Заполнить ▾" else "Показать ▾", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = t.accent, modifier = Modifier.padding(horizontal = 8.dp))
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
    VuFillRow {
        VuPrint("весом")
        VuInk(state.weightText, Modifier.weight(1f), onChange = vm::setWeight, keyboard = KeyboardType.Decimal, maxLen = 8)
        VuPrint("т.")
        VuPrint("Всего осей", modifier = Modifier.padding(start = 6.dp))
        VuInk(state.totalAxlesText, Modifier.width(56.dp), onChange = vm::setTotalAxles, keyboard = KeyboardType.Number, maxLen = 4)
        VuMiniHelp { help("axles") }
    }
    VuFillRow {
        Text("Требуется нажатие тормозных колодок в тс.", Modifier.weight(1f), fontSize = 14.sp, color = t.text, lineHeight = 18.sp)
        VuInk(result?.let { fmt(it.requiredPressure) } ?: "", Modifier.width(92.dp), calc = true, align = TextAlign.End)
        VuMiniHelp { help("required") }
    }
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
    val dens = listOfNotNull(ii?.let { "II ${fmt(it)} с" }, iv?.let { "IV ${fmt(it)} с" }).joinToString(" · ")
    VuFillRow(Modifier.clickable(onClick = onOpenMeasure)) {
        Text("Плотность тормозной сети поезда", Modifier.weight(1f), fontSize = 14.sp, color = t.text, lineHeight = 18.sp)
        VuInk(dens, Modifier.width(132.dp), calc = false, placeholder = "замерить ›", align = TextAlign.End)
        VuMiniHelp { help("density") }
    }
    if (dens.isNotEmpty()) {
        Text(
            "из «Замеров»" + if (iv == null) " · IV не замерено" else "",
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp).padding(top = 0.dp, bottom = 8.dp), fontSize = 12.sp, color = t.accent, textAlign = TextAlign.End,
        )
    }
    VuFillRow {
        VuPrint("Хвостовой вагон №")
        VuInk(state.tailCarText, Modifier.weight(1f), onChange = vm::setTailCar, maxLen = 14)
        VuMiniHelp { help("tail_car") }
    }
    VuFillRow {
        VuPrint("Подпись", bold = true, modifier = Modifier.padding(start = 24.dp))
        VuInk("", Modifier.weight(1f), placeholder = "осмотрщик")
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun AxleTable(state: BrakeCertificateUiState, vm: BrakeCertificateViewModel, onSheet: (BrakeSheet) -> Unit) {
    val t = vuColors()
    val rows = state.effectiveRows()
    val byPressure = rows.associateBy { it.pressureText.toFiniteDoubleOrNull() }
    val extra = rows.filter { r -> val p = r.pressureText.toFiniteDoubleOrNull(); p != null && p !in PRINTED }
    val totalAxles = rows.sumOf { it.axlesText.trim().toIntOrNull() ?: 0 }
    val totalPressure = rows.sumOf { (it.pressureText.toFiniteDoubleOrNull() ?: 0.0) * (it.axlesText.trim().toIntOrNull() ?: 0) }
    val low = state.toInput()?.let { BrakeCertificateCalculator.calculate(it) }?.let { it.actualPressure > 0 && !it.enough } == true
    val lineColor = t.borderStrong
    val head: @Composable (String, Boolean) -> Unit = { text, right ->
        Box(Modifier.fillMaxHeight().vuCellBorders(lineColor, right = right, bottom = true).padding(horizontal = 2.dp), contentAlignment = Alignment.Center) {
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
                AxleRowCells(label = fmtP(p), axles = r?.axlesText.orEmpty(), product = r?.let { fmt(p * (it.axlesText.trim().toIntOrNull() ?: 0)) }.orEmpty(), filled = r != null, onAxles = { vm.setAxlesForPressure(p, it) })
            }
            extra.forEach { r ->
                val p = r.pressureText.toFiniteDoubleOrNull() ?: 0.0
                AxleRowCells(label = fmtP(p), axles = r.axlesText, product = fmt(p * (r.axlesText.trim().toIntOrNull() ?: 0)), filled = true, onAxles = { vm.setAxlesForPressure(p, it) })
            }
            // Всего
            Row(Modifier.height(IntrinsicSize.Min)) {
                Box(Modifier.weight(1f).heightIn(min = 40.dp).vuCellBorders(lineColor), contentAlignment = Alignment.Center) { VuPrint("Всего", bold = true) }
                Box(Modifier.weight(1f).heightIn(min = 40.dp).vuCellBorders(lineColor), contentAlignment = Alignment.Center) { VuInk(if (totalAxles > 0) "$totalAxles" else "", calc = true, align = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                Box(Modifier.weight(1f).heightIn(min = 40.dp).vuCellBorders(lineColor), contentAlignment = Alignment.Center) {
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
    val check = AxleCheck.compare(state.totalAxlesText.trim().toIntOrNull(), sum)
    if (!state.rowsMode) return
    when (check) {
        is AxleSumCheck.Equal -> Text("✓ осей сходится: ${check.total}", modifier.padding(horizontal = 14.dp, vertical = 6.dp), fontFamily = VuMono, fontSize = 13.sp, color = t.success)
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
private fun AxleRowCells(label: String, axles: String, product: String, filled: Boolean, onAxles: (String) -> Unit) {
    val t = vuColors()
    val line = t.borderStrong
    val bg = if (filled) t.accentSoft else Color.Transparent
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.height(40.dp)) {
        Box(Modifier.weight(1f).height(40.dp).background(bg).vuCellBorders(line), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 14.sp, fontWeight = if (filled) FontWeight.Bold else FontWeight.Normal, color = t.text)
        }
        Box(
            Modifier.weight(1f).height(40.dp).background(bg).vuCellBorders(line)
                .then(if (focused) Modifier.border(2.dp, t.accent) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            BasicTextField(
                value = axles,
                onValueChange = { onAxles(it.filter { c -> c in '0'..'9' }.take(4)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).onFocusChanged { focused = it.isFocused },
                singleLine = true,
                textStyle = TextStyle(fontFamily = VuMono, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = t.accent, textAlign = TextAlign.Center),
                cursorBrush = SolidColor(t.accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        Box(Modifier.weight(1f).height(40.dp).background(bg).vuCellBorders(line), contentAlignment = Alignment.Center) {
            Text(product, fontFamily = VuMono, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = t.text, maxLines = 1)
        }
    }
}

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
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items.forEach { (k, v) ->
            Column(Modifier.padding(bottom = 4.dp)) {
                Text(k, fontSize = 11.sp, color = t.muted, lineHeight = 13.sp)
                Text(v, fontFamily = VuMono, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = t.accent, lineHeight = 17.sp)
            }
        }
        Text(
            (if (items.isEmpty()) "+ Заполнить" else "+ Изменить"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = t.accent,
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
                Text("Учитывать локомотив", fontSize = 16.sp, color = t.text)
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
                    VuInput("Вес, тс", state.locoWeightText, Modifier.weight(1f), onChange = vm::setLocoWeight, unit = "тс", sunk = true, onHelp = { onHelp("loco_brake") })
                    VuInput("Осей", state.locoAxlesText, Modifier.weight(0.8f), onChange = vm::setLocoAxles, sunk = true, keyboard = KeyboardType.Number)
                    VuInput("тс/ось", state.locoPressureText, Modifier.weight(0.9f), onChange = vm::setLocoPressure, sunk = true)
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
    Column(Modifier.fillMaxWidth()) {
        VuHint("Оборотная сторона справки. Записи создаются по стоянкам поезда: более 5 мин - станция и плотность; более 30 мин - ещё время готовности и табельный номер вагонника.", Modifier.padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 10.dp))
        Row(Modifier.fillMaxWidth().vuCellBorders(line, right = false, bottom = true).height(IntrinsicSize.Min)) {
            listOf("Станция" to 1.2f, "Стоянка" to 1.1f, "Плотн., с" to 0.7f).forEachIndexed { i, (x, w) ->
                Box(Modifier.weight(w).fillMaxHeight().vuCellBorders(line, right = i < 2, bottom = false).padding(6.dp), contentAlignment = Alignment.Center) {
                    Text(x, fontSize = 11.sp, color = t.text, textAlign = TextAlign.Center)
                }
            }
        }
        if (state.turnaround.isEmpty()) {
            Text(
                if (state.routeId == null) "Нет данных маршрута: добавьте отметку вручную." else "Стоянок дольше 5 минут пока нет. Отметка появится автоматически, когда на станции раздела «Поезд» стоянка превысит 5 минут.",
                Modifier.padding(14.dp), fontSize = 13.sp, color = t.muted, lineHeight = 18.sp,
            )
        }
        state.turnaround.forEach { e ->
            Column(Modifier.fillMaxWidth().clickable { onSheet(BrakeSheet.Turnaround(e.id)) }.vuCellBorders(line, right = false, bottom = true)) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Box(Modifier.weight(1.2f).fillMaxHeight().vuCellBorders(line, right = true, bottom = false).padding(horizontal = 8.dp, vertical = 10.dp), contentAlignment = Alignment.CenterStart) {
                        Text(e.stationName.ifBlank { "станция" }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (e.stationName.isBlank()) t.faint else t.accent, lineHeight = 18.sp)
                    }
                    Box(Modifier.weight(1.1f).fillMaxHeight().vuCellBorders(line, right = true, bottom = false).padding(horizontal = 8.dp, vertical = 10.dp), contentAlignment = Alignment.CenterStart) {
                        val period = if (e.arrivalMs != null) hhmm(e.arrivalMs!!) + "–" + (e.departureMs?.let { hhmm(it) } ?: "…") else ""
                        Column {
                            Text(if (e.stopClass == StopClass.OVER_30) "> 30 мин" else "> 5 мин", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = t.text)
                            if (period.isNotEmpty()) Text(period, fontFamily = VuMono, fontSize = 12.sp, color = t.muted)
                        }
                    }
                    Box(Modifier.weight(0.7f).fillMaxHeight().padding(horizontal = 8.dp, vertical = 10.dp), contentAlignment = Alignment.CenterStart) {
                        Text(e.densitySec?.let { fmt(it) } ?: "замерить", fontFamily = if (e.densitySec != null) VuMono else null, fontSize = if (e.densitySec != null) 17.sp else 12.sp, fontWeight = if (e.densitySec != null) FontWeight.SemiBold else FontWeight.Normal, color = if (e.densitySec != null) t.accent else t.faint)
                    }
                }
                if (e.stopClass == StopClass.OVER_30) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                        Text("Готовность", fontSize = 11.sp, color = t.muted)
                        Text(e.readinessTime.ifBlank { "—" }, fontFamily = VuMono, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (e.readinessTime.isBlank()) t.faint else t.accent)
                        Text("Таб. № вагонника", fontSize = 11.sp, color = t.muted)
                        Text(e.inspectorTab.ifBlank { "—" }, fontFamily = VuMono, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (e.inspectorTab.isBlank()) t.faint else t.accent)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            VuBtn("+ Добавить опробование", {
                val id = vm.newTurnaroundId()
                vm.addTurnaround(StopClass.OVER_5, state.trainStationNames.lastOrNull().orEmpty(), id)
                onSheet(BrakeSheet.Turnaround(id))
            }, kind = VuBtnKind.Text, h = 48.dp)
            Spacer(Modifier.weight(1f))
            if (state.routeId != null) VuBtn("Обновить по стоянкам", { vm.refreshStops() }, kind = VuBtnKind.Text, h = 48.dp)
        }
        VuHint("Отметки живут, пока открыт экран. «Плотность» можно ввести или замерить секундомером во вкладке «Замеры».", Modifier.padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 14.dp))
    }
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
            VuInput("Станция", e.stationName, onChange = { v -> vm.updateTurnaround(entryId) { it.copy(stationName = v) } }, text = true, sunk = true)
            if (state.trainStationNames.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.trainStationNames.forEach { n -> VuChip(n, e.stationName == n, { vm.updateTurnaround(entryId) { it.copy(stationName = n) } }, small = true) }
                }
            }
            VuInput(
                "Плотность, с", e.densitySec?.let { fmt(it) } ?: "",
                onChange = { v -> vm.updateTurnaround(entryId) { it.copy(densitySec = v.toFiniteDoubleOrNull()) } }, unit = "с", sunk = true,
            )
            VuBtn("Замерить секундомером", onMeasure, Modifier.fillMaxWidth(), kind = VuBtnKind.Tonal)
            if (e.stopClass == StopClass.OVER_30) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VuInput("Время готовности", e.readinessTime, Modifier.weight(1f), onChange = { v -> vm.updateTurnaround(entryId) { it.copy(readinessTime = v) } }, text = true, sunk = true, placeholder = "ЧЧ:ММ")
                    VuInput("Таб. № вагонника", e.inspectorTab, Modifier.weight(1f), onChange = { v -> vm.updateTurnaround(entryId) { it.copy(inspectorTab = v) } }, keyboard = KeyboardType.Number, sunk = true)
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
        VuCircled(listOf("Г", "ЭПТ", "ЭПП", "ДТ", "МРТ", "П", "В10", "РИЦ"), state.flags) { vm.toggleFlag(it) }
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
        "Что делать при недостатке нажатия", "Нажатие ниже нормы табл. III.1 Правил ТО тормозного оборудования. Если оно не ниже допускаемого минимума табл. III.2 и случай относится к п. 33-34, скорость снижают: 2 км/ч за каждую недостающую тонну для грузовых, 1-2 км/ч для пассажирских, результат округляют вниз до кратного 5.", "Правила ТО тормозного оборудования, прил. 2, п. 33-35, 37",
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
    val group = state.densityGroup ?: BrakeDensity.groupBySeries(state.locoSeriesText)
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
                (state.timerOf(k).let { tm -> if (tm.running) " · идёт" else tm.recordedSec?.let { " · ${fmt(it)} с" } ?: "" })
        }
        VuTabs(labels, kinds.indexOf(active), { onKind(kinds[it]) }, mono = true)
        if (active == TimerKind.ROUTE && state.routeTargetEntryId != null) {
            val target = state.turnaround.firstOrNull { it.id == state.routeTargetEntryId }
            Row(
                Modifier.fillMaxWidth().background(t.accentSoft, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Результат запишется в отметку оборота: ${target?.stationName?.ifBlank { "станция" } ?: ""}", Modifier.weight(1f), fontSize = 13.sp, color = t.text)
                Text("Отмена", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = t.accent, modifier = Modifier.clickable { vm.setRouteTarget(null) }.padding(8.dp))
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
                TimerKind.II -> if (!passenger && minTime != null) "Норма: не менее ${fmt(minTime)} с" else null
                TimerKind.IV -> state.timerII.recordedSec?.let { "Допуск: не менее ${fmt(it * 0.9)} с (90 % от II)" }
                TimerKind.ROUTE -> state.timerII.recordedSec?.let { "Допуск ±20 %: ${fmt(it * 0.8)}–${fmt(it * 1.2)} с" }
            },
            assess = { sec ->
                when (active) {
                    TimerKind.II -> if (passenger) BrakeDensity.assessPassenger(sec, state.timerII.dropText.toFiniteDoubleOrNull()) else BrakeDensity.assessFreightII(sec, minTime, groupSelected = group != null)
                    TimerKind.IV -> BrakeDensity.assessFreightIV(sec, state.timerII.recordedSec)
                    TimerKind.ROUTE -> BrakeDensity.assessRoute(sec, state.timerII.recordedSec)
                }
            },
            hideGr = active == TimerKind.ROUTE && passenger,
            onHelp = { onHelp("density") },
        )
        if (active == TimerKind.ROUTE && TurnaroundPlanner.applies(category)) {
            VuBtn("К отметкам оборота ›", onBackToTurnaround, kind = VuBtnKind.Text)
        }
        if (!passenger) VuHint("Зарядное давление, число локомотивов (СМЕ) и группа локомотива (объём ГР) задаются на вкладке «Расчёт» - здесь они используются для нормы.", Modifier.padding(horizontal = 4.dp))
        VuHint("Результаты замеров автоматически попадают во вкладку «Бланк». Хранятся только на экране. Экран не гаснет, пока идёт замер.", Modifier.padding(horizontal = 4.dp))
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
    hideGr: Boolean = false,
    onHelp: () -> Unit,
) {
    val t = vuColors()
    val elapsed = timer.elapsedMs(now)
    val done = timer.recordedSec != null
    VuCard {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = t.text)
                VuHelp(onHelp)
            }
            VuHint(subtitle, Modifier.padding(top = 4.dp))
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
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                when {
                    timer.running -> VuBtn("Стоп", { vm.stopTimer(kind) }, Modifier.fillMaxWidth(), kind = VuBtnKind.Cta, h = 64.dp)
                    done -> {
                        VuBtn("Заново", { vm.startTimer(kind) }, Modifier.weight(1f), kind = VuBtnKind.Tonal, h = 64.dp)
                        VuBtn("Сбросить", { vm.resetTimer(kind) }, kind = VuBtnKind.Text, h = 64.dp)
                    }
                    else -> VuBtn("Старт", { vm.startTimer(kind) }, Modifier.fillMaxWidth(), h = 64.dp)
                }
            }
            timer.recordedSec?.let { rec ->
                val a = assess(rec)
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    when (a.verdict) {
                        BrakeDensity.Verdict.OK -> VuVerdict(true, "Записано: ${fmt(rec)} с — норма", 16)
                        BrakeDensity.Verdict.FAIL -> VuVerdict(false, "Записано: ${fmt(rec)} с — превышена", 16)
                        BrakeDensity.Verdict.NO_NORM -> Text("Записано: ${fmt(rec)} с — оценка недоступна", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = t.muted)
                    }
                    val left = if (a.verdict == BrakeDensity.Verdict.NO_NORM) 0.dp else 28.dp
                    VuHint(
                        when (a.verdict) {
                            BrakeDensity.Verdict.NO_NORM -> a.text.replaceFirstChar { it.uppercase() }.let { if (a.text.startsWith("Выберите")) "$it — появится норма" else it }
                            else -> (normText ?: a.text) + (transferred?.let { " · $it" } ?: "")
                        },
                        Modifier.padding(start = left),
                    )
                }
            }
            if (passenger) {
                Box(Modifier.padding(top = 16.dp)) { VuInput("Падение давления в ТМ, кгс/см²", timer.dropText, onChange = { vm.setDrop(kind, it) }, sunk = true) }
            } else if (!hideGr) {
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VuInput("ГР в начале", timer.grStartText, Modifier.weight(1f), onChange = { vm.setGrStart(kind, it) }, unit = "кгс/см²", sunk = true)
                    VuInput("ГР в конце", timer.grEndText, Modifier.weight(1f), onChange = { vm.setGrEnd(kind, it) }, unit = "кгс/см²", sunk = true)
                }
                val a = timer.grStartText.toFiniteDoubleOrNull()
                val b = timer.grEndText.toFiniteDoubleOrNull()
                if (a != null && b != null) {
                    val drop = a - b
                    val okDrop = kotlin.math.abs(drop - 0.5) <= 0.05
                    VuHint("Снижение: ${fmt(drop, 2)} кгс/см²" + if (okDrop) "" else " — по нормативу замер на снижении 0,5 кгс/см²", Modifier.padding(top = 6.dp), color = if (okDrop) null else t.danger)
                }
            }
        }
    }
}

// ───────────────────────────── Справочник ─────────────────────────────

@Composable
private fun ReferenceTab(state: BrakeCertificateUiState, category: BrakeCategory, onOpenMeasure: () -> Unit) {
    val t = vuColors()
    var open by rememberSaveable { mutableIntStateOf(-1) }
    val group = state.densityGroup ?: BrakeDensity.groupBySeries(state.locoSeriesText)
    val axles = axlesForNorm(state)
    val minTime = group?.let { BrakeDensity.minTimeSeconds(it, axles, state.chargePressureText.toFiniteDoubleOrNull(), state.locoCountText.trim().toIntOrNull() ?: 1) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VuTag("Справочник · " + InstructionRef.SHORT)
        brakeReference.forEachIndexed { i, card ->
            VuCard {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { open = if (open == i) -1 else i }.padding(start = 18.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", Modifier.width(28.dp), fontFamily = VuMono, fontSize = 12.sp, color = t.faint)
                    Text(card.title, Modifier.weight(1f).padding(vertical = 8.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = t.text, lineHeight = 20.sp)
                    Text(if (open == i) "▴" else "▾", Modifier.padding(horizontal = 14.dp), fontSize = 14.sp, color = t.muted)
                }
                if (open == i) {
                    Column(Modifier.padding(start = 46.dp, end = 18.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        card.lines.forEach { Text(InstructionRef.annotate(it), fontSize = 14.sp, color = t.text, lineHeight = 20.sp) }
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
            "Расчёт по Правилам технического обслуживания тормозного оборудования и управления тормозами (СЖТ, протокол № 60 от 6-7.05.2014, с изменениями до 2017 г.), прил. 2. Форма ВУ-45 - Инструкция 808-2017 ПКБ ЦВ, прил. К. Справочный инструмент: официальной справкой ВУ-45 является документ, выданный осмотрщиком вагонов. Нормы могут меняться - сверяйте с действующими документами.",
            Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )
    }
}
