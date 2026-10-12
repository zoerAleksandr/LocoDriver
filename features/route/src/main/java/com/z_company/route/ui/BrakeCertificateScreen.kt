package com.z_company.route.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.brake.BrakeCategory
import com.z_company.domain.brake.BrakeCertificateCalculator
import com.z_company.domain.brake.BrakeCertificateResult
import com.z_company.domain.brake.BrakeDensity
import com.z_company.domain.brake.BrakeNorms
import com.z_company.domain.brake.CompositeShare
import com.z_company.domain.brake.DensityLocoGroup
import com.z_company.domain.brake.GradeClass
import com.z_company.domain.brake.SpeedLimit
import com.z_company.domain.brake.TrainGroup
import com.z_company.domain.util.toFiniteDoubleOrNull
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.component.separateSheetFromScroll
import com.z_company.route.ui.settings.SettingsCard
import com.z_company.route.ui.settings.SettingsCardSep
import com.z_company.route.ui.settings.SettingsPickerOption
import com.z_company.route.ui.settings.SettingsPickerSheet
import com.z_company.route.ui.settings.SettingsSectionNote
import com.z_company.route.viewmodel.BrakeCertificateUiState
import com.z_company.route.viewmodel.BrakeCertificateViewModel
import com.z_company.route.viewmodel.DensityTimerState
import com.z_company.route.viewmodel.TimerKind
import com.z_company.route.viewmodel.toInput

private sealed interface BrakeSheet {
    data object Category : BrakeSheet
    data object ManualNorm : BrakeSheet
    data object DensityGroup : BrakeSheet
    data class RowPressure(val rowId: Int) : BrakeSheet
}

private val TABS = listOf("Расчёт", "Бланк", "Замеры", "Справочник")

private fun fmt(v: Double, digits: Int = 1): String {
    val factor = Math.pow(10.0, digits.toDouble())
    val r = Math.round(v * factor) / factor
    val s = if (r % 1.0 == 0.0 && digits > 0) r.toLong().toString() else r.toString()
    return s.replace('.', ',')
}

private fun formatTimer(ms: Long): String {
    val tenths = ms / 100
    val min = tenths / 600
    val sec = (tenths % 600) / 10
    val d = tenths % 10
    return "%02d:%02d.%d".format(min, sec, d)
}

private fun axlesForNorm(state: BrakeCertificateUiState): Int =
    state.totalAxlesText.trim().toIntOrNull()
        ?: state.rows.sumOf { r ->
            if ((r.pressureText.toFiniteDoubleOrNull() ?: 0.0) > 0) (r.axlesText.trim().toIntOrNull() ?: 0) else 0
        }

/** Экран-инструмент «Справка ВУ-45» (SCREEN_SPECS §26): вкладки «Расчёт», «Бланк», «Замеры», «Справочник». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrakeCertificateScreen(
    state: BrakeCertificateUiState,
    onBack: () -> Unit,
    viewModel: BrakeCertificateViewModel,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var measureKind by rememberSaveable { mutableStateOf(TimerKind.II.name) }
    var sheet by remember { mutableStateOf<BrakeSheet?>(null) }
    var helpKey by remember { mutableStateOf<String?>(null) }
    val now by viewModel.nowMs.collectAsState()

    val category = BrakeNorms.category(state.categoryId) ?: BrakeNorms.categories.first()
    val input = remember(state) { state.toInput() }
    val result = remember(input) { input?.let(BrakeCertificateCalculator::calculate) }
    val primary = MaterialTheme.colorScheme.primary

    // Экран не гаснет, пока идёт замер плотности.
    val anyRunning = state.timerII.running || state.timerIV.running || state.timerRoute.running
    val view = LocalView.current
    DisposableEffect(anyRunning) {
        view.keepScreenOn = anyRunning
        onDispose { view.keepScreenOn = false }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Справка ВУ-45", style = MaterialTheme.typography.titleMedium, color = primary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(com.z_company.core.R.drawable.ic_arrow_back),
                            contentDescription = "Назад",
                            tint = primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabsRow(tab) { tab = it }
            when (tab) {
                0 -> CalcTab(state, category, result, viewModel, onSheet = { sheet = it }, onHelp = { helpKey = it }, onOpenBlank = { tab = 1 })
                1 -> BlankTab(state, category, result, viewModel, onSheet = { sheet = it }, onHelp = { helpKey = it }, onOpenMeasure = { tab = 2 })
                2 -> MeasureTab(
                    state, category, viewModel, now,
                    kind = TimerKind.valueOf(measureKind), onKind = { measureKind = it.name },
                    onGroupClick = { sheet = BrakeSheet.DensityGroup }, onHelp = { helpKey = it },
                )
                else -> ReferenceTab()
            }
        }
    }

    when (val s = sheet) {
        BrakeSheet.Category -> SettingsPickerSheet(
            title = "Категория поезда",
            hint = "Табл. 1 Норм: единое наименьшее нажатие на 100 тс веса и максимальная скорость",
            onDismiss = { sheet = null },
            options = BrakeNorms.categories.map { c ->
                SettingsPickerOption(
                    label = c.title,
                    sub = "п. ${c.tableRow}: ${c.norm} тс/100 тс, до ${c.vFlat} км/ч",
                    selected = c.id == state.categoryId,
                    onSelect = { viewModel.setCategory(c.id) },
                )
            },
        )
        BrakeSheet.ManualNorm -> SettingsPickerSheet(
            title = "Норма ручных тормозных осей",
            hint = "Единое наименьшее - 0,6 оси на 100 тс. Для одной дороги и уклонов круче 0,012 норму устанавливает начальник дороги (табл. 6)",
            onDismiss = { sheet = null },
            options = BrakeNorms.ManualBrakeNorm.values().map { n ->
                SettingsPickerOption(label = n.title, selected = n == state.manualNorm, onSelect = { viewModel.setManualNorm(n) })
            },
        )
        BrakeSheet.DensityGroup -> SettingsPickerSheet(
            title = "Группа локомотива (табл. 9.1)",
            hint = "По серии или по объёму главных резервуаров (прим. 1 к таблице)",
            onDismiss = { sheet = null },
            options = DensityLocoGroup.values().map { g ->
                SettingsPickerOption(label = g.title, selected = g == state.densityGroup, onSelect = { viewModel.setDensityGroup(g) })
            },
        )
        is BrakeSheet.RowPressure -> SettingsPickerSheet(
            title = "Нажатие на ось, тс",
            hint = "Значения первой графы бланка. Тип вагона - по табл. 3: груз, режим ВР, колодки",
            onDismiss = { sheet = null },
            options = BrakeNorms.blankAxlePressures.map { p ->
                val types = BrakeNorms.presetsFor(p)
                SettingsPickerOption(
                    label = "${BrakeCertificateViewModel.formatPressure(p)} тс",
                    sub = types.take(3).joinToString("; ") { it.title }.ifBlank { "нет в табл. 3" },
                    selected = state.rows.firstOrNull { it.id == s.rowId }?.pressureText?.toFiniteDoubleOrNull() == p,
                    onSelect = { viewModel.setRowPressure(s.rowId, BrakeCertificateViewModel.formatPressure(p)) },
                )
            },
        )
        null -> Unit
    }

    helpKey?.let { key ->
        brakeHints[key]?.let { hint -> HintSheet(hint, onDismiss = { helpKey = null }) }
    }
}

// ───────────────────────────── Вкладки ─────────────────────────────

@Composable
private fun TabsRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        TABS.forEachIndexed { i, title ->
            val on = i == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .background(if (on) MaterialTheme.colorScheme.tertiary else Color.Transparent, RoundedCornerShape(10.dp))
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (on) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

// ───────────────────────────── Вкладка «Расчёт» ─────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalcTab(
    state: BrakeCertificateUiState,
    category: BrakeCategory,
    result: BrakeCertificateResult?,
    vm: BrakeCertificateViewModel,
    onSheet: (BrakeSheet) -> Unit,
    onHelp: (String) -> Unit,
    onOpenBlank: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PickerField(
            label = "Категория поезда",
            value = "${category.title.substringBefore(" / ").substringBefore(" (")} · ${category.norm} тс/100",
            onClick = { onSheet(BrakeSheet.Category) },
            onHelp = { onHelp("category") },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompactField("Вес состава, тс", state.weightText, vm::setWeight, Modifier.weight(1f), unit = "тс", helpKey = "weight", onHelp = onHelp)
            CompactField("Всего осей", state.totalAxlesText, vm::setTotalAxles, Modifier.weight(1f), unit = "осей", integer = true, helpKey = "axles", onHelp = onHelp)
        }
        if (!state.rowsMode) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                CompactField("Нажатие, тс на ось", state.avgPressureText, vm::setAvgPressure, Modifier.weight(1f), unit = "тс", helpKey = "row", onHelp = onHelp)
                if (category.manualBrakesRequired) {
                    PickerField(
                        label = "Ручные оси на 100 тс",
                        value = state.manualNorm.title.substringBefore(" - "),
                        onClick = { onSheet(BrakeSheet.ManualNorm) },
                        modifier = Modifier.weight(1f),
                    )
                } else Spacer(Modifier.weight(1f))
            }
            Text(
                modifier = Modifier.clickable { vm.setRowsMode(true) }.padding(vertical = 2.dp),
                text = "Оси по типам вагонов ▸",
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            if (category.manualBrakesRequired) {
                PickerField(
                    label = "Ручные оси на 100 тс",
                    value = state.manualNorm.title,
                    onClick = { onSheet(BrakeSheet.ManualNorm) },
                )
            }
            AxleRowsEditor(state, vm, onSheet, onHelp)
            Text(
                modifier = Modifier.clickable { vm.setRowsMode(false) }.padding(vertical = 2.dp),
                text = "◂ Одно среднее нажатие",
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        GradeChips(state, category, vm)
        ResultBlock(result, state, category, onOpenBlank)
    }
}

@Composable
private fun GradeChips(state: BrakeCertificateUiState, category: BrakeCategory, vm: BrakeCertificateViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Руководящий спуск", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            GradeClass.values().forEach { g ->
                ChoiceChip(when (g) { GradeClass.UP_TO_006 -> "≤0,006"; GradeClass.OVER_006_TO_010 -> "0,006-0,010"; GradeClass.OVER_010_TO_015 -> "0,010-0,015" }, state.grade == g) { vm.setGrade(g) }
            }
            if (category.vFlatAutoblock != null && state.grade != GradeClass.OVER_010_TO_015) {
                ChoiceChip("АБ ${category.vFlatAutoblock}", state.autoblock3) { vm.setAutoblock(!state.autoblock3) }
            }
        }
    }
}

@Composable
private fun ResultBlock(
    result: BrakeCertificateResult?,
    state: BrakeCertificateUiState,
    category: BrakeCategory,
    onOpenBlank: () -> Unit,
) {
    val okColor = MaterialTheme.colorScheme.tertiary
    val badColor = MaterialTheme.colorScheme.error
    val has = (result?.actualPressure ?: 0.0) > 0.0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("ДОПУСКАЕМАЯ СКОРОСТЬ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val sp = result?.speed
        when {
            !has || sp == null -> Text("—", fontSize = 44.sp, fontFamily = MonoFont, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            sp is SpeedLimit.Allowed -> Text(
                "${sp.kmh} км/ч", fontSize = 44.sp, fontFamily = MonoFont, fontWeight = FontWeight.Bold,
                color = if (sp.reducedFrom != null) badColor else okColor,
            )
            else -> Text("не определена", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = badColor)
        }
        if (result == null) {
            Text(
                "Введите вес состава, число осей и нажатие на ось.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Потребное, тс", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(fmt(result.requiredPressure), fontFamily = MonoFont, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Фактическое, тс", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (has) fmt(result.actualPressure) else "—", fontFamily = MonoFont, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                    color = if (!has) MaterialTheme.colorScheme.primary else if (result.enough) okColor else badColor,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Ручных осей", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (category.manualBrakesRequired) (result.requiredManualAxles?.toString() ?: "—") else "все",
                    fontFamily = MonoFont, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (has) {
            Text(
                if (result.enough) "Нажатие достаточно: запас ${fmt(result.actualPressure - result.requiredPressure)} тс (${fmt(result.actualPer100, 2)} на 100 тс)"
                else "Нажатие недостаточно (${fmt(result.actualPer100, 2)} на 100 тс, норма ${category.norm})",
                color = if (result.enough) okColor else badColor,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
            val note = when (sp) {
                is SpeedLimit.Allowed -> sp.note + (sp.reducedFrom?.let { " Было бы по табл. 1: $it км/ч." } ?: "")
                is SpeedLimit.NotDetermined -> sp.reason
                null -> ""
            }
            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        result.warnings.forEach {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.timerII.recordedSec?.let { t ->
            Text("Плотность II: ${fmt(t)} с" + (state.timerIV.recordedSec?.let { ", IV: ${fmt(it)} с" } ?: ""),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (has && !result.enough) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Не хватает ${fmt(result.requiredPressure - result.actualPressure)} тс (${fmt(result.shortagePer100, 2)} на 100 тс)",
                    color = badColor, fontWeight = FontWeight.Bold,
                )
                Text(
                    "Что делать (п. 4.4): скорость снижается на 2 км/ч за каждую недостающую тонну (пассажирские - 1-2 км/ч), округление вниз до кратного 5. " +
                        "Допустимо только в случаях п. 4.2-4.3 (выключены неисправные тормоза в пути, особые составы). " +
                        "Ниже минимума табл. 2 - скорость устанавливает начальник дороги. См. «Справочник».",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        TextButton(onClick = onOpenBlank, contentPadding = PaddingValues(0.dp)) { Text("Открыть бланк справки ▸") }
    }
}

// ───────────────────────────── Вкладка «Бланк» ─────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlankTab(
    state: BrakeCertificateUiState,
    category: BrakeCategory,
    result: BrakeCertificateResult?,
    vm: BrakeCertificateViewModel,
    onSheet: (BrakeSheet) -> Unit,
    onHelp: (String) -> Unit,
    onOpenMeasure: () -> Unit,
) {
    var reqOpen by rememberSaveable { mutableStateOf(false) }
    var otherOpen by rememberSaveable { mutableStateOf(false) }
    var tailOpen by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        SummaryLine(result)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ФОРМА ВУ-45", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "СПРАВКА об обеспечении поезда тормозами и исправном их действии",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center,
                )
            }

            ExpandCard(
                title = "Реквизиты",
                subtitle = listOf(
                    state.trainNumberText.takeIf { it.isNotBlank() }?.let { "поезд № $it" },
                    (state.locoSeriesText + " " + state.locoNumberText).trim().takeIf { it.isNotBlank() },
                    state.stationText.takeIf { it.isNotBlank() },
                ).filterNotNull().joinToString(" · ").ifBlank { "необязательно: станция, время, дата, локомотив, поезд №" },
                open = reqOpen, onToggle = { reqOpen = !reqOpen },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompactField("Штемпель станции", state.stationText, vm::setStation, Modifier.weight(1f), text = true, helpKey = "station", onHelp = onHelp)
                    CompactField("Дата", state.dateText, vm::setDate, Modifier.weight(1f), text = true, helpKey = "date", onHelp = onHelp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                    CompactField("Время выдачи, ч:мин", state.issueTimeText, vm::setIssueTime, Modifier.weight(1f), text = true, helpKey = "time", onHelp = onHelp)
                    TextButton(onClick = vm::setIssueTimeNow) { Text("Сейчас") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompactField("Локомотив, серия", state.locoSeriesText, vm::setLocoSeries, Modifier.weight(1f), text = true, helpKey = "loco", onHelp = onHelp)
                    CompactField("Локомотив, №", state.locoNumberText, vm::setLocoNumber, Modifier.weight(1f), text = true)
                }
                CompactField("Поезд №", state.trainNumberText, vm::setTrainNumber, Modifier.fillMaxWidth(), text = true, helpKey = "trainNo", onHelp = onHelp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactField("Весом, тс", state.weightText, vm::setWeight, Modifier.weight(1f), unit = "тс", helpKey = "weight", onHelp = onHelp)
                CompactField("Всего осей", state.totalAxlesText, vm::setTotalAxles, Modifier.weight(1f), unit = "осей", integer = true, helpKey = "axles", onHelp = onHelp)
            }
            SettingsCard {
                CalcLine("Требуемое нажатие колодок, тс", result?.let { fmt(it.requiredPressure) } ?: "-", "required", onHelp)
                SettingsCardSep()
                CalcLine(
                    "Ручных тормозов в осях (потребно)",
                    when {
                        !category.manualBrakesRequired -> "все вагоны"
                        result?.requiredManualAxles != null -> result.requiredManualAxles.toString()
                        else -> "-"
                    },
                    "manual_req", onHelp,
                )
            }

            // Таблица нажатий
            Text("ТОРМОЗНОЕ НАЖАТИЕ НА ОСЬ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.rowsMode) {
                AxleRowsEditor(state, vm, onSheet, onHelp)
            } else {
                SettingsCard {
                    val p = state.avgPressureText.toFiniteDoubleOrNull()
                    val n = state.totalAxlesText.trim().toIntOrNull()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = if (p != null && n != null) "${fmt(p)} тс × $n осей = ${fmt(p * n)} тс" else "Среднее нажатие не введено (вкладка «Расчёт»)",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
                        )
                        HelpDot { onHelp("row") }
                    }
                    SettingsCardSep()
                    Text(
                        modifier = Modifier.fillMaxWidth().clickable { vm.setRowsMode(true) }.padding(horizontal = 14.dp, vertical = 12.dp),
                        text = "Разбить оси по типам вагонов ▸",
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            LocoCard(state, category, vm, onHelp)

            ExpandCard(
                title = "Другие данные",
                subtitle = (listOf(
                    when (state.compositeShare) { CompositeShare.NONE -> null; CompositeShare.P50 -> "К-50"; CompositeShare.P75 -> "К-75"; CompositeShare.P100 -> "К-100" },
                ).filterNotNull() + state.flags.toList()).joinToString(", ").ifBlank { "К-100, ЭПТ, ТЦПВ, ДПВ, ВО2ХВ..." },
                open = otherOpen, onToggle = { otherOpen = !otherOpen },
            ) {
                LabelWithHelp("Доля композиционных колодок", "k_share", onHelp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(CompositeShare.NONE to "нет", CompositeShare.P50 to "К-50", CompositeShare.P75 to "К-75", CompositeShare.P100 to "К-100").forEach { (v, t) ->
                        ChoiceChip(t, state.compositeShare == v) { vm.setCompositeShare(v) }
                    }
                }
                LabelWithHelp("Тормоза в составе", "flags", onHelp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ЭПТ", "ЭПП", "ДТ", "МРТ", "П", "В10", "РИЦ").forEach { f -> ChoiceChip(f, f in state.flags) { vm.toggleFlag(f) } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompactField("ТЦПВ", state.tcpvText, vm::setTcpv, Modifier.weight(1f), unit = "мм", helpKey = "tcpv", onHelp = onHelp)
                    CompactField("Встр.", state.vstrText, vm::setVstr, Modifier.weight(1f), text = true, helpKey = "vstr", onHelp = onHelp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompactField("ДПВ", state.dpvText, vm::setDpv, Modifier.weight(1f), unit = "кгс/см²", helpKey = "dpv", onHelp = onHelp)
                    if (category.group == TrainGroup.FREIGHT || category.group == TrainGroup.FREIGHT_PASSENGER) {
                        CompactField("ВО2ХВ", state.vo2hvText, vm::setVo2hv, Modifier.weight(1f), unit = "с", helpKey = "vo2hv", onHelp = onHelp)
                    } else Spacer(Modifier.weight(1f))
                }
                DpvAssessment(state, category)
            }

            ExpandCard(
                title = "Нижняя часть бланка",
                subtitle = "ручные оси, питательная сеть, напряжение, хвостовой вагон",
                open = tailOpen, onToggle = { tailOpen = !tailOpen },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompactField("Наличие ручных осей", state.manualHaveText, vm::setManualHave, Modifier.weight(1f), unit = "осей", integer = true, helpKey = "manual_have", onHelp = onHelp)
                    CompactField("Хвостовой вагон №", state.tailCarText, vm::setTailCar, Modifier.weight(1f), text = true, helpKey = "tail_car", onHelp = onHelp)
                }
                result?.manualAxlesOk?.let { ok ->
                    Text(
                        if (ok) "Ручных осей достаточно (потребно ${result.requiredManualAxles})" else "Ручных осей не хватает: потребно ${result.requiredManualAxles}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (ok) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompactField("Питательная сеть", state.feedDensityText, vm::setFeedDensity, Modifier.weight(1f), text = true, helpKey = "feed", onHelp = onHelp)
                    CompactField("Напряжение в хвосте", state.tailVoltageText, vm::setTailVoltage, Modifier.weight(1f), unit = "В", helpKey = "tail_voltage", onHelp = onHelp)
                }
            }

            // Плотность — результаты замеров с вкладки «Замеры»
            SettingsCard {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Плотность тормозной сети при II/IV положениях ручки крана машиниста",
                            modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
                        )
                        HelpDot { onHelp("density") }
                    }
                    DensityLine("II положение", state.timerII)
                    if (category.group != TrainGroup.PASSENGER) DensityLine("IV положение", state.timerIV)
                    if (state.timerRoute.recordedSec != null) DensityLine("В пути", state.timerRoute)
                    TextButton(onClick = onOpenMeasure, contentPadding = PaddingValues(0.dp)) { Text("Выполнить замеры ▸") }
                }
            }
            SettingsSectionNote(
                "Расчёт по Инструкции ЦТ-ЦВ-ЦЛ-ВНИИЖТ/277 (прил. 2, ред. 1994); форма - по прил. К Инструкции 808-2017 ПКБ ЦВ. " +
                    "Официальной справкой остаётся документ осмотрщика вагонов. Данные нигде не сохраняются.",
            )
        }
    }
}

@Composable
private fun DensityLine(title: String, t: DensityTimerState) {
    Text(
        "$title: " + (t.recordedSec?.let { "${fmt(it)} с" } ?: "не замерено"),
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SummaryLine(result: BrakeCertificateResult?) {
    val has = (result?.actualPressure ?: 0.0) > 0.0
    val okColor = MaterialTheme.colorScheme.tertiary
    val badColor = MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp))
            .height(48.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val sp = result?.speed
        val speedText = when {
            !has || sp == null -> "—"
            sp is SpeedLimit.Allowed -> "${sp.kmh} км/ч"
            else -> "нет"
        }
        Text(speedText, fontFamily = MonoFont, fontWeight = FontWeight.Bold, fontSize = 22.sp,
            color = when {
                !has || sp == null -> MaterialTheme.colorScheme.primary
                sp is SpeedLimit.Allowed -> if (sp.reducedFrom != null) badColor else okColor
                else -> badColor
            })
        Spacer(Modifier.width(12.dp))
        Text(
            when {
                !has || result == null -> "введите вес и нажатие"
                result.enough -> "нажатие достаточно"
                else -> "не хватает ${fmt(result.requiredPressure - result.actualPressure)} тс"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (has && result != null && !result.enough) badColor else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CalcLine(label: String, value: String, helpKey: String, onHelp: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(modifier = Modifier.weight(1f), text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFont, fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
        HelpDot { onHelp(helpKey) }
    }
}

@Composable
private fun LocoCard(state: BrakeCertificateUiState, category: BrakeCategory, vm: BrakeCertificateViewModel, onHelp: (String) -> Unit) {
    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Учитывать локомотив", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    if (category.group == TrainGroup.FREIGHT) "В грузовых поездах не учитывается (п. 6.3)" else "Вес по табл. 5, нажатие по табл. 4",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.material3.Switch(
                checked = state.locoEnabled,
                onCheckedChange = vm::setLocoEnabled,
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
                    checkedTrackColor = MaterialTheme.colorScheme.tertiary,
                    checkedBorderColor = MaterialTheme.colorScheme.tertiary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
        if (state.locoEnabled) {
            SettingsCardSep()
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompactField("Вес, тс", state.locoWeightText, vm::setLocoWeight, Modifier.weight(1f), unit = "тс", helpKey = "loco_brake", onHelp = onHelp)
                    CompactField("Осей", state.locoAxlesText, vm::setLocoAxles, Modifier.weight(0.8f), integer = true)
                    CompactField("тс/ось", state.locoPressureText, vm::setLocoPressure, Modifier.weight(0.9f))
                }
                Text(
                    if (state.locoAuto) "По табл. 4-5 для серии «${state.locoSeriesText}»" + (state.locoNote?.let { ": $it" } ?: "") + ". Можно править."
                    else if (state.locoSeriesText.isNotBlank()) "Серии «${state.locoSeriesText}» нет в справочнике - введите вручную."
                    else "Введите серию локомотива в «Реквизитах» или значения вручную.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DpvAssessment(state: BrakeCertificateUiState, category: BrakeCategory) {
    if (category.group != TrainGroup.FREIGHT && category.group != TrainGroup.FREIGHT_PASSENGER) return
    val axles = axlesForNorm(state)
    val charge = state.chargePressureText.toFiniteDoubleOrNull()
    val min = BrakeDensity.minTailPressure(charge, axles)
    val dpv = state.dpvText.toFiniteDoubleOrNull()
    val (text, ok) = when {
        min == null -> "Норма в хвосте: укажите зарядное давление локомотива 4,8-5,8 (вкладка «Замеры») и число осей." to null
        dpv == null -> "Норма в хвосте: не менее ${fmt(min)} кгс/см² (п. 9.2.6)" to null
        dpv + 1e-9 >= min -> "ДПВ в норме (не менее ${fmt(min)})" to true
        else -> "ДПВ ниже нормы: нужно не менее ${fmt(min)} кгс/см²" to false
    }
    Text(
        text, style = MaterialTheme.typography.bodySmall,
        color = when (ok) { true -> MaterialTheme.colorScheme.tertiary; false -> MaterialTheme.colorScheme.error; null -> MaterialTheme.colorScheme.onSurfaceVariant },
    )
}

// ───────────────────────────── Таблица осей ─────────────────────────────

private val FREQUENT_PRESSURES = listOf(8.5, 7.0, 3.5, 5.0, 6.0, 10.0, 6.5, 9.0)

@Composable
private fun AxleRowsEditor(
    state: BrakeCertificateUiState,
    vm: BrakeCertificateViewModel,
    onSheet: (BrakeSheet) -> Unit,
    onHelp: (String) -> Unit,
) {
    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Нажатие на ось, тс  ×  число осей", Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HelpDot { onHelp("row") }
        }
        state.rows.forEach { row ->
            SettingsCardSep()
            val p = row.pressureText.toFiniteDoubleOrNull()
            val n = row.axlesText.trim().toIntOrNull()
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FREQUENT_PRESSURES.forEach { v ->
                        ChoiceChip(BrakeCertificateViewModel.formatPressure(v), p == v) {
                            vm.setRowPressure(row.id, BrakeCertificateViewModel.formatPressure(v))
                        }
                    }
                    ChoiceChip("ещё…", false) { onSheet(BrakeSheet.RowPressure(row.id)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { SmallField(row.pressureText, "тс на ось", { vm.setRowPressure(row.id, it) }) }
                    Box(Modifier.weight(0.9f)) { SmallField(row.axlesText, "осей", { vm.setRowAxles(row.id, it) }, integer = true) }
                    Text(
                        modifier = Modifier.weight(0.9f),
                        text = if (p != null && n != null) "= ${fmt(p * n)}" else "=",
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFont),
                        color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.End,
                    )
                    Text(
                        modifier = Modifier.clickable { vm.removeRow(row.id) }.padding(horizontal = 8.dp, vertical = 4.dp),
                        text = "×", color = MaterialTheme.colorScheme.error, fontSize = 22.sp,
                    )
                }
            }
        }
        SettingsCardSep()
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f).clickable { vm.addRow() },
                text = "+ Строка", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodyLarge,
            )
            val ax = state.rows.sumOf { r -> if ((r.pressureText.toFiniteDoubleOrNull() ?: 0.0) > 0) (r.axlesText.trim().toIntOrNull() ?: 0) else 0 }
            val pr = state.rows.sumOf { r ->
                val pp = r.pressureText.toFiniteDoubleOrNull() ?: 0.0
                val nn = r.axlesText.trim().toIntOrNull() ?: 0
                if (pp > 0 && nn > 0) pp * nn else 0.0
            }
            Text("Всего: $ax осей, ${fmt(pr)} тс", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFont, fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
        }
    }
}

// ───────────────────────────── Вкладка «Замеры» ─────────────────────────────

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
) {
    val passenger = category.group == TrainGroup.PASSENGER
    val kinds = if (passenger) listOf(TimerKind.II, TimerKind.ROUTE) else TimerKind.values().toList()
    val active = if (kind in kinds) kind else TimerKind.II
    val group = state.densityGroup ?: BrakeDensity.groupBySeries(state.locoSeriesText)
    val charge = state.chargePressureText.toFiniteDoubleOrNull()
    val axles = axlesForNorm(state)
    val locoCount = state.locoCountText.trim().toIntOrNull() ?: 1
    val minTime = group?.let { BrakeDensity.minTimeSeconds(it, axles, charge, locoCount) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            kinds.forEach { k ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(
                            if (k == active) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable { onKind(k) },
                    contentAlignment = Alignment.Center,
                ) {
                    val t = state.timerOf(k)
                    Text(
                        (when (k) { TimerKind.II -> "II"; TimerKind.IV -> "IV"; TimerKind.ROUTE -> "В пути" }) +
                            (t.recordedSec?.let { " · ${fmt(it)} с" } ?: ""),
                        color = if (k == active) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    )
                }
            }
        }

        TimerCard(
            title = when (active) {
                TimerKind.II -> "II положение - поездное"
                TimerKind.IV -> "IV положение - перекрыша с питанием"
                TimerKind.ROUTE -> "Контроль в пути"
            },
            subtitle = when (active) {
                TimerKind.II -> if (passenger) "Перекрыть комбинированный кран, подождать 20 с, «Старт» и замер падения давления в ТМ"
                else "После снижения давления в ГР на 0,4-0,5 кгс/см² от предельного - «Старт», на снижении ещё на 0,5 - «Стоп»"
                TimerKind.IV -> "Через 2 мин и более после ступени торможения (УР -0,5...0,7 кгс/см²), ручка в IV. Тот же замер"
                TimerKind.ROUTE -> "После стоянки, смены бригады и т.п.: отличие от замера II не более 20 %, иначе сокращённое опробование"
            },
            kind = active, timer = state.timerOf(active), now = now, passenger = passenger && active == TimerKind.II, vm = vm,
            assess = { t ->
                when (active) {
                    TimerKind.II -> if (passenger) BrakeDensity.assessPassenger(t, state.timerII.dropText.toFiniteDoubleOrNull())
                    else BrakeDensity.assessFreightII(t, minTime)
                    TimerKind.IV -> BrakeDensity.assessFreightIV(t, state.timerII.recordedSec)
                    TimerKind.ROUTE -> BrakeDensity.assessRoute(t, state.timerII.recordedSec)
                }
            },
            hideGr = active == TimerKind.ROUTE && passenger,
        )

        if (!passenger) {
            SettingsCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                        CompactField("Зарядное давление ТМ", state.chargePressureText, vm::setChargePressure, Modifier.weight(1f), unit = "кгс/см²", helpKey = "charge", onHelp = onHelp)
                        CompactField("Локомотивов (СМЕ)", state.locoCountText, vm::setLocoCount, Modifier.weight(0.8f), integer = true, helpKey = "group", onHelp = onHelp)
                    }
                    PickerField(
                        label = "Группа локомотива (табл. 9.1)",
                        value = group?.let { "№ ${it.name.removePrefix("G")}: ${it.title}" } ?: "выбрать",
                        onClick = onGroupClick,
                    )
                    Text(
                        when {
                            group == null -> "Норма II положения: выберите группу локомотива."
                            minTime == null -> "Норма II положения: длина состава должна быть 1-480 осей (сейчас: $axles)."
                            else -> "Норма II положения: не менее ${fmt(minTime)} с (группа ${group.name.removePrefix("G")}, $axles осей" +
                                (if (BrakeDensity.chargeFactor(charge) < 1.0) ", с поправкой на зарядное давление" else "") + ")."
                        },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            "Результаты замеров автоматически попадают во вкладку «Бланк». Хранятся только на экране. Экран не гаснет, пока идёт замер.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun BrakeCertificateUiState.timerOf(k: TimerKind): DensityTimerState = when (k) {
    TimerKind.II -> timerII
    TimerKind.IV -> timerIV
    TimerKind.ROUTE -> timerRoute
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
    hideGr: Boolean = false,
) {
    val elapsed = timer.elapsedMs(now)
    SettingsCard {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = formatTimer(elapsed),
                textAlign = TextAlign.Center,
                fontSize = 56.sp,
                fontFamily = MonoFont,
                fontWeight = FontWeight.Bold,
                color = if (timer.running) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (timer.running) {
                    Button(
                        modifier = Modifier.weight(1f).height(60.dp),
                        onClick = { vm.stopTimer(kind) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) { Text("Стоп", fontSize = 20.sp) }
                } else {
                    Button(
                        modifier = Modifier.weight(1f).height(60.dp),
                        onClick = { vm.startTimer(kind) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        ),
                    ) { Text(if (timer.recordedSec != null) "Заново" else "Старт", fontSize = 20.sp) }
                }
                if (!timer.running && (timer.recordedSec != null || elapsed > 0)) {
                    TextButton(onClick = { vm.resetTimer(kind) }) { Text("Сбросить") }
                }
            }
            timer.recordedSec?.let { rec ->
                val a = assess(rec)
                val color = when (a.verdict) {
                    BrakeDensity.Verdict.OK -> MaterialTheme.colorScheme.tertiary
                    BrakeDensity.Verdict.FAIL -> MaterialTheme.colorScheme.error
                    BrakeDensity.Verdict.NO_NORM -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val head = when (a.verdict) {
                    BrakeDensity.Verdict.OK -> "Норма"
                    BrakeDensity.Verdict.FAIL -> "Превышена"
                    BrakeDensity.Verdict.NO_NORM -> "Оценка недоступна"
                }
                Text("Записано: ${fmt(rec)} с - $head", style = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFont, fontWeight = FontWeight.SemiBold), color = color)
                Text(a.text, style = MaterialTheme.typography.bodySmall, color = color)
            }
            if (passenger) {
                SmallLabeled("Падение давления в ТМ, кгс/см²") { SmallField(timer.dropText, "0,2", { vm.setDrop(kind, it) }) }
            } else if (!hideGr) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { SmallLabeled("Давление ГР в начале") { SmallField(timer.grStartText, "кгс/см²", { vm.setGrStart(kind, it) }) } }
                    Box(Modifier.weight(1f)) { SmallLabeled("Давление ГР в конце") { SmallField(timer.grEndText, "кгс/см²", { vm.setGrEnd(kind, it) }) } }
                }
                val a = timer.grStartText.toFiniteDoubleOrNull()
                val b = timer.grEndText.toFiniteDoubleOrNull()
                if (a != null && b != null) {
                    val drop = a - b
                    val okDrop = kotlin.math.abs(drop - 0.5) <= 0.05
                    Text(
                        "Снижение: ${fmt(drop, 2)} кгс/см²" + if (okDrop) "" else " - по нормативу замер на снижении 0,5",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (okDrop) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

// ───────────────────────────── Вкладка «Справочник» ─────────────────────────────

@Composable
private fun ReferenceTab() {
    var expanded by rememberSaveable { mutableStateOf(setOf<Int>()) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingsCard {
            brakeReference.forEachIndexed { i, card ->
                if (i > 0) SettingsCardSep()
                val open = i in expanded
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = if (open) expanded - i else expanded + i }
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(modifier = Modifier.weight(1f), text = card.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                        Text(if (open) "−" else "+", fontSize = 22.sp, color = MaterialTheme.colorScheme.tertiary)
                    }
                    if (open) {
                        Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            card.lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }
        SettingsSectionNote(
            "Расчёт по Инструкции ЦТ-ЦВ-ЦЛ-ВНИИЖТ/277 (прил. 2, ред. 1994 г.); форма бланка - прил. К Инструкции 808-2017 ПКБ ЦВ. " +
                "Справочный инструмент: официальной справкой ВУ-45 является документ, выданный осмотрщиком вагонов. Нормы могли измениться - сверяйте с действующими документами.",
        )
    }
}

// ───────────────────────────── Общие элементы ─────────────────────────────

@Composable
private fun ExpandCard(
    title: String,
    subtitle: String,
    open: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(if (open) "−" else "+", fontSize = 22.sp, color = MaterialTheme.colorScheme.tertiary)
        }
        if (open) {
            SettingsCardSep()
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
        }
    }
}

@Composable
private fun PickerField(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier, onHelp: (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (onHelp != null) HelpDot(onHelp)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(MaterialTheme.colorScheme.surfaceBright, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(value, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            Text("▾", color = MaterialTheme.colorScheme.tertiary, fontSize = 16.sp)
        }
    }
}

@Composable
private fun CompactField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    unit: String? = null,
    text: Boolean = false,
    integer: Boolean = false,
    helpKey: String? = null,
    onHelp: (String) -> Unit = {},
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (helpKey != null) HelpDot { onHelp(helpKey) }
        }
        OutlinedTextFieldApp(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = { raw ->
                val allowed = when {
                    text -> raw
                    integer -> raw.filter { it.isDigit() }
                    else -> raw.filter { it.isDigit() || it == ',' || it == '.' }
                }
                onChange(allowed.take(if (text) 40 else 10))
            },
            singleLine = true,
            suffix = unit?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = when {
                    text -> KeyboardType.Text
                    integer -> KeyboardType.Number
                    else -> KeyboardType.Decimal
                },
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFont),
            shape = RoundedCornerShape(12.dp),
            fieldElevation = 0.dp,
            contentPadding = PaddingValues(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
            colorBackgroundEmptyField = MaterialTheme.colorScheme.surfaceBright,
            colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surfaceBright,
        )
    }
}

@Composable
private fun SmallField(value: String, placeholder: String, onChange: (String) -> Unit, integer: Boolean = false) {
    OutlinedTextFieldApp(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = { raw ->
            val allowed = if (integer) raw.filter { it.isDigit() } else raw.filter { it.isDigit() || it == ',' || it == '.' }
            onChange(allowed.take(8))
        },
        singleLine = true,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFont),
        shape = RoundedCornerShape(12.dp),
        fieldElevation = 0.dp,
        contentPadding = PaddingValues(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
        colorBackgroundEmptyField = MaterialTheme.colorScheme.surfaceBright,
        colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surfaceBright,
    )
}

@Composable
private fun SmallLabeled(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun LabelWithHelp(label: String, helpKey: String, onHelp: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HelpDot { onHelp(helpKey) }
    }
}

@Composable
private fun HelpDot(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(start = 4.dp)
            .size(22.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("?", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceBright
    val fg = if (selected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text, color = fg, maxLines = 1, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFont))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HintSheet(hint: BrakeHint, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.secondary,
        shape = Shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .separateSheetFromScroll()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(hint.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            hint.who?.let { Text("Кто заполняет: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary) }
            Text(hint.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            hint.clause?.let {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Источник: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
