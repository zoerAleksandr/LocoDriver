package com.z_company.route.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.z_company.route.ui.settings.SettingsGroupHeader
import com.z_company.route.ui.settings.SettingsPickerOption
import com.z_company.route.ui.settings.SettingsPickerSheet
import com.z_company.route.ui.settings.SettingsSectionNote
import com.z_company.route.ui.settings.SettingsSelectRow
import com.z_company.route.ui.settings.SettingsSwitchRow
import com.z_company.route.viewmodel.BrakeCertificateUiState
import com.z_company.route.viewmodel.BrakeCertificateViewModel
import com.z_company.route.viewmodel.DensityTimerState
import com.z_company.route.viewmodel.TimerKind
import com.z_company.route.viewmodel.toInput

private sealed interface BrakeSheet {
    data object Category : BrakeSheet
    data object Grade : BrakeSheet
    data object ManualNorm : BrakeSheet
    data object DensityGroup : BrakeSheet
    data class RowPressure(val rowId: Int) : BrakeSheet
}

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

/** Экран-инструмент «Справка ВУ-45» (SCREEN_SPECS §26): бланк справки с расчётом, замерами плотности и справочником. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrakeCertificateScreen(
    state: BrakeCertificateUiState,
    onBack: () -> Unit,
    viewModel: BrakeCertificateViewModel,
) {
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
            SummaryStrip(result = result)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
            ) {
                // ── Параметры расчёта (не графы бланка) ──
                SettingsGroupHeader("ПАРАМЕТРЫ РАСЧЁТА", top = 8.dp, startPad = 4.dp)
                SettingsCard {
                    SettingsSelectRow(
                        label = "Категория поезда",
                        value = "${category.norm} тс/100 тс",
                        sub = category.title,
                        mono = true,
                        onClick = { sheet = BrakeSheet.Category },
                    )
                    SettingsCardSep()
                    SettingsSelectRow(
                        label = "Руководящий спуск",
                        value = state.grade.title,
                        onClick = { sheet = BrakeSheet.Grade },
                    )
                    if (category.vFlatAutoblock != null && state.grade != GradeClass.OVER_010_TO_015) {
                        SettingsCardSep()
                        SettingsSwitchRow(
                            label = "Автоблокировка с 3-значной сигнализацией",
                            sub = "Зелёный огонь АЛСН: ${category.vFlatAutoblock} км/ч (значение в скобках табл. 1)",
                            checked = state.autoblock3,
                            onCheckedChange = viewModel::setAutoblock,
                        )
                    }
                    if (category.manualBrakesRequired) {
                        SettingsCardSep()
                        SettingsSelectRow(
                            label = "Норма ручных осей на 100 тс",
                            value = state.manualNorm.title.substringBefore(" - "),
                            sub = "Выбор запоминается",
                            mono = true,
                            onClick = { sheet = BrakeSheet.ManualNorm },
                        )
                    }
                }

                // ── Шапка бланка ──
                BlankTitle()
                SettingsCard {
                    BlankField("Штемпель станции", "station", state.stationText, viewModel::setStation, text = true, onHelp = { helpKey = it })
                    SettingsCardSep()
                    BlankField(
                        "Время выдачи, ч:мин", "time", state.issueTimeText, viewModel::setIssueTime, text = true,
                        action = { TextButton(onClick = viewModel::setIssueTimeNow) { Text("Сейчас") } },
                        onHelp = { helpKey = it },
                    )
                    SettingsCardSep()
                    BlankField("Дата", "date", state.dateText, viewModel::setDate, text = true, onHelp = { helpKey = it })
                    SettingsCardSep()
                    BlankField("Локомотив, серия", "loco", state.locoSeriesText, viewModel::setLocoSeries, text = true, onHelp = { helpKey = it })
                    SettingsCardSep()
                    BlankField("Локомотив, №", null, state.locoNumberText, viewModel::setLocoNumber, text = true)
                    SettingsCardSep()
                    BlankField("Поезд №", "trainNo", state.trainNumberText, viewModel::setTrainNumber, text = true, onHelp = { helpKey = it })
                    SettingsCardSep()
                    BlankField("Весом, тс", "weight", state.weightText, viewModel::setWeight, unit = "тс", onHelp = { helpKey = it })
                    SettingsCardSep()
                    BlankField("Всего осей", "axles", state.totalAxlesText, viewModel::setTotalAxles, unit = "оси", integer = true, onHelp = { helpKey = it })
                    SettingsCardSep()
                    CalcRow(
                        label = "Требуемое нажатие колодок, тс",
                        value = result?.let { fmt(it.requiredPressure) } ?: "-",
                        helpKey = "required",
                        onHelp = { helpKey = it },
                    )
                    SettingsCardSep()
                    CalcRow(
                        label = "Ручных тормозов в осях (потребно)",
                        value = when {
                            !category.manualBrakesRequired -> "все вагоны"
                            result?.requiredManualAxles != null -> result.requiredManualAxles.toString()
                            else -> "-"
                        },
                        helpKey = "manual_req",
                        onHelp = { helpKey = it },
                    )
                }

                // ── Таблица нажатий ──
                SettingsGroupHeader("ТОРМОЗНОЕ НАЖАТИЕ НА ОСЬ", top = 20.dp, startPad = 4.dp)
                SettingsCard {
                    TableHeader(onHelp = { helpKey = "row" })
                    state.rows.forEach { row ->
                        SettingsCardSep()
                        val p = row.pressureText.toFiniteDoubleOrNull()
                        val n = row.axlesText.trim().toIntOrNull()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(Modifier.weight(1f)) {
                                SmallField(
                                    value = row.pressureText,
                                    placeholder = "тс на ось",
                                    onChange = { viewModel.setRowPressure(row.id, it) },
                                    trailing = {
                                        Text(
                                            modifier = Modifier.clickable { sheet = BrakeSheet.RowPressure(row.id) }.padding(8.dp),
                                            text = "▾", color = MaterialTheme.colorScheme.tertiary, fontSize = 18.sp,
                                        )
                                    },
                                )
                            }
                            Box(Modifier.weight(0.8f)) {
                                SmallField(
                                    value = row.axlesText,
                                    placeholder = "осей",
                                    integer = true,
                                    onChange = { viewModel.setRowAxles(row.id, it) },
                                )
                            }
                            Text(
                                modifier = Modifier.weight(0.9f),
                                text = if (p != null && n != null) fmt(p * n) else "-",
                                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFont),
                                color = primary,
                                textAlign = TextAlign.End,
                            )
                            Text(
                                modifier = Modifier.clickable { viewModel.removeRow(row.id) }.padding(horizontal = 6.dp, vertical = 4.dp),
                                text = "×", color = MaterialTheme.colorScheme.error, fontSize = 22.sp,
                            )
                        }
                    }
                    SettingsCardSep()
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.addRow() }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        text = "+ Добавить строку",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    SettingsCardSep()
                    val rowsAxles = state.rows.sumOf { r ->
                        if ((r.pressureText.toFiniteDoubleOrNull() ?: 0.0) > 0) (r.axlesText.trim().toIntOrNull() ?: 0) else 0
                    }
                    val rowsPressure = state.rows.sumOf { r ->
                        val pp = r.pressureText.toFiniteDoubleOrNull() ?: 0.0
                        val nn = r.axlesText.trim().toIntOrNull() ?: 0
                        if (pp > 0 && nn > 0) pp * nn else 0.0
                    }
                    TotalRow("Всего", rowsAxles.toString(), fmt(rowsPressure))
                }

                // Локомотив в подсчёте
                Spacer(Modifier.height(12.dp))
                SettingsCard {
                    SettingsSwitchRow(
                        label = "Учитывать локомотив",
                        sub = if (category.group == TrainGroup.FREIGHT) "В грузовых поездах не учитывается (п. 6.3)"
                        else "Учётный вес (табл. 5) и нажатие колодок (табл. 4)",
                        checked = state.locoEnabled,
                        onCheckedChange = viewModel::setLocoEnabled,
                    )
                    if (state.locoEnabled) {
                        SettingsCardSep()
                        BlankField("Учётный вес локомотива", "loco_brake", state.locoWeightText, viewModel::setLocoWeight, unit = "тс", onHelp = { helpKey = it })
                        SettingsCardSep()
                        BlankField("Тормозных осей локомотива", null, state.locoAxlesText, viewModel::setLocoAxles, unit = "оси", integer = true)
                        SettingsCardSep()
                        BlankField("Нажатие на ось локомотива", null, state.locoPressureText, viewModel::setLocoPressure, unit = "тс")
                        if (state.locoAuto) {
                            Text(
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                                text = "Значения по табл. 4-5 для серии «${state.locoSeriesText}»" +
                                    (state.locoNote?.let { ": $it" } ?: "") + ". Можно поправить вручную.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else if (state.locoSeriesText.isNotBlank()) {
                            Text(
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                                text = "Серии «${state.locoSeriesText}» нет в справочнике табл. 4-5 (или значения изменены) - введите вес, оси и нажатие вручную.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // ── Другие данные (4-я графа) ──
                SettingsGroupHeader("ДРУГИЕ ДАННЫЕ", top = 20.dp, startPad = 4.dp)
                SettingsCard {
                    LabelWithHelp("Доля композиционных колодок", "k_share", onHelp = { helpKey = it })
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 6.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            CompositeShare.NONE to "нет",
                            CompositeShare.P50 to "К-50",
                            CompositeShare.P75 to "К-75",
                            CompositeShare.P100 to "К-100",
                        ).forEach { (v, t) ->
                            ChoiceChip(t, state.compositeShare == v) { viewModel.setCompositeShare(v) }
                        }
                    }
                    SettingsCardSep()
                    LabelWithHelp("Тормоза в составе", "flags", onHelp = { helpKey = it })
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 6.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("ЭПТ", "ЭПП", "ДТ", "МРТ", "П", "В10", "РИЦ").forEach { f ->
                            ChoiceChip(f, f in state.flags) { viewModel.toggleFlag(f) }
                        }
                    }
                    SettingsCardSep()
                    BlankField("ТЦПВ - выход штока хвостового вагона", "tcpv", state.tcpvText, viewModel::setTcpv, unit = "мм", onHelp = { helpKey = it })
                    SettingsCardSep()
                    BlankField("Встр. - вагон встречи осмотрщиков", "vstr", state.vstrText, viewModel::setVstr, text = true, onHelp = { helpKey = it })
                    SettingsCardSep()
                    BlankField(
                        "ДПВ - давление в ТМ последнего вагона", "dpv", state.dpvText, viewModel::setDpv, unit = "кгс/см²", onHelp = { helpKey = it },
                    )
                    DpvAssessment(state, category)
                    if (category.group == TrainGroup.FREIGHT || category.group == TrainGroup.FREIGHT_PASSENGER) {
                        SettingsCardSep()
                        BlankField("ВО2ХВ - время отпуска двух хвостовых вагонов", "vo2hv", state.vo2hvText, viewModel::setVo2hv, unit = "с", onHelp = { helpKey = it })
                    }
                }

                // ── Подвал бланка ──
                SettingsGroupHeader("НИЖНЯЯ ЧАСТЬ БЛАНКА", top = 20.dp, startPad = 4.dp)
                SettingsCard {
                    BlankField("Наличие ручных тормозных осей", "manual_have", state.manualHaveText, viewModel::setManualHave, unit = "оси", integer = true, onHelp = { helpKey = it })
                    result?.manualAxlesOk?.let { ok ->
                        Verdict(
                            ok = ok,
                            text = if (ok) "Достаточно (потребно ${result.requiredManualAxles})"
                            else "Не хватает: потребно ${result.requiredManualAxles}",
                        )
                    }
                    SettingsCardSep()
                    BlankField("Плотность питательной сети", "feed", state.feedDensityText, viewModel::setFeedDensity, text = true, onHelp = { helpKey = it })
                    SettingsCardSep()
                    BlankField("Напряжение в хвосте поезда", "tail_voltage", state.tailVoltageText, viewModel::setTailVoltage, unit = "В", onHelp = { helpKey = it })
                    SettingsCardSep()
                    BlankField("Хвостовой вагон №", "tail_car", state.tailCarText, viewModel::setTailCar, text = true, onHelp = { helpKey = it })
                }

                // ── Плотность II/IV ──
                SettingsGroupHeader("ПЛОТНОСТЬ ТОРМОЗНОЙ СЕТИ, II / IV ПОЛОЖЕНИЯ КРАНА", top = 20.dp, startPad = 4.dp)
                DensityBlock(state, category, viewModel, now, onGroupClick = { sheet = BrakeSheet.DensityGroup }, onHelp = { helpKey = it })

                // ── Справочник ──
                SettingsGroupHeader("СПРАВОЧНИК ТОРМОЗОВ", top = 20.dp, startPad = 4.dp)
                ReferenceBlock()

                SettingsSectionNote(
                    "Расчёт по Инструкции ЦТ-ЦВ-ЦЛ-ВНИИЖТ/277 (приложение 2, редакция 1994 г.); форма бланка - по приложению К " +
                        "Инструкции 808-2017 ПКБ ЦВ. Справочный инструмент: официальной справкой ВУ-45 является документ, выданный " +
                        "осмотрщиком вагонов. Данные нигде не сохраняются. Нормы могли измениться - сверяйте с действующими документами.",
                )
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
        BrakeSheet.Grade -> SettingsPickerSheet(
            title = "Руководящий спуск",
            onDismiss = { sheet = null },
            options = GradeClass.values().map { g ->
                SettingsPickerOption(label = g.title, selected = g == state.grade, onSelect = { viewModel.setGrade(g) })
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

// ───────────────────────────── Верхняя сводка ─────────────────────────────

@Composable
private fun SummaryStrip(result: BrakeCertificateResult?) {
    val okColor = MaterialTheme.colorScheme.tertiary
    val badColor = MaterialTheme.colorScheme.error
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "ДОПУСКАЕМАЯ СКОРОСТЬ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val sp = result?.speed
                val has = (result?.actualPressure ?: 0.0) > 0.0
                when {
                    !has || sp == null -> Text("—", fontSize = 34.sp, fontFamily = MonoFont, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    sp is SpeedLimit.Allowed -> Text(
                        "${sp.kmh} км/ч", fontSize = 34.sp, fontFamily = MonoFont, fontWeight = FontWeight.Bold,
                        color = if (sp.reducedFrom != null) badColor else okColor,
                    )
                    else -> Text("не определена", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = badColor)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (result != null && result.actualPressure > 0) {
                    Text(
                        if (result.enough) "Нажатие достаточно" else "Нажатие недостаточно",
                        color = if (result.enough) okColor else badColor,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "${fmt(result.actualPressure)} / ${fmt(result.requiredPressure)} тс",
                        fontFamily = MonoFont,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "Введите вес и нажатие",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        val sp = result?.speed
        if (result != null && result.actualPressure > 0 && sp != null) {
            val note = when (sp) {
                is SpeedLimit.Allowed -> sp.note + (sp.reducedFrom?.let { " Было бы по табл. 1: $it км/ч." } ?: "")
                is SpeedLimit.NotDetermined -> sp.reason
            }
            Text(
                note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!result.enough) {
                Text(
                    "Не хватает ${fmt(result.requiredPressure - result.actualPressure)} тс (${fmt(result.shortagePer100, 2)} на 100 тс). " +
                        "Снижение скорости допустимо только в случаях п. 4.2-4.3 (см. справочник).",
                    style = MaterialTheme.typography.bodySmall,
                    color = badColor,
                )
            }
        }
        result?.warnings?.forEach {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ───────────────────────────── Бланк ─────────────────────────────

@Composable
private fun BlankTitle() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "ФОРМА ВУ-45",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "СПРАВКА",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "об обеспечении поезда тормозами и исправном их действии",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LabelWithHelp(label: String, helpKey: String?, onHelp: (String) -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        trailing?.invoke()
        if (helpKey != null) HelpDot { onHelp(helpKey) }
    }
}

@Composable
private fun HelpDot(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(start = 4.dp)
            .size(26.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("?", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BlankField(
    label: String,
    helpKey: String?,
    value: String,
    onChange: (String) -> Unit,
    unit: String? = null,
    text: Boolean = false,
    integer: Boolean = false,
    action: (@Composable () -> Unit)? = null,
    onHelp: (String) -> Unit = {},
) {
    Column(Modifier.fillMaxWidth()) {
        LabelWithHelp(label, helpKey, onHelp, action)
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 10.dp)) {
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
                suffix = unit?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
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
                colorBackgroundEmptyField = MaterialTheme.colorScheme.surfaceBright,
                colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surfaceBright,
            )
        }
    }
}

@Composable
private fun SmallField(
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
    integer: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    OutlinedTextFieldApp(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = { raw ->
            val allowed = if (integer) raw.filter { it.isDigit() } else raw.filter { it.isDigit() || it == ',' || it == '.' }
            onChange(allowed.take(8))
        },
        singleLine = true,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
        trailingIcon = trailing,
        keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFont),
        shape = RoundedCornerShape(12.dp),
        fieldElevation = 0.dp,
        contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
        colorBackgroundEmptyField = MaterialTheme.colorScheme.surfaceBright,
        colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surfaceBright,
    )
}

@Composable
private fun CalcRow(label: String, value: String, helpKey: String, onHelp: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFont, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
        )
        HelpDot { onHelp(helpKey) }
    }
}

@Composable
private fun TableHeader(onHelp: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val st = MaterialTheme.typography.labelSmall
        val c = MaterialTheme.colorScheme.onSurfaceVariant
        Text("Нажатие на ось, тс", Modifier.weight(1f), style = st, color = c)
        Text("Кол-во осей", Modifier.weight(0.8f), style = st, color = c)
        Text("Нажатие колодок, тс", Modifier.weight(0.9f), style = st, color = c, textAlign = TextAlign.End)
        HelpDot(onHelp)
    }
}

@Composable
private fun TotalRow(label: String, axles: String, pressure: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
        Text("$axles осей", style = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFont), color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text("$pressure тс", style = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFont, fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
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
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, color = fg, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFont))
    }
}

@Composable
private fun Verdict(ok: Boolean, text: String) {
    Text(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        color = if (ok) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun DpvAssessment(state: BrakeCertificateUiState, category: BrakeCategory) {
    if (category.group != TrainGroup.FREIGHT && category.group != TrainGroup.FREIGHT_PASSENGER) return
    val axles = axlesForNorm(state)
    val charge = state.chargePressureText.toFiniteDoubleOrNull()
    val min = BrakeDensity.minTailPressure(charge, axles)
    val dpv = state.dpvText.toFiniteDoubleOrNull()
    if (min == null) {
        Text(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
            text = "Норма в хвосте: укажите зарядное давление локомотива 4,8-5,8 кгс/см² (блок плотности ниже) и число осей.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else if (dpv == null) {
        Text(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
            text = "Норма в хвосте: не менее ${fmt(min)} кгс/см² (п. 9.2.6)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Verdict(
            ok = dpv + 1e-9 >= min,
            text = if (dpv + 1e-9 >= min) "Норма: не менее ${fmt(min)} - в норме" else "Ниже нормы: нужно не менее ${fmt(min)} кгс/см²",
        )
    }
}

private fun axlesForNorm(state: BrakeCertificateUiState): Int =
    state.totalAxlesText.trim().toIntOrNull()
        ?: state.rows.sumOf { r ->
            if ((r.pressureText.toFiniteDoubleOrNull() ?: 0.0) > 0) (r.axlesText.trim().toIntOrNull() ?: 0) else 0
        }

// ───────────────────────────── Плотность ─────────────────────────────

@Composable
private fun DensityBlock(
    state: BrakeCertificateUiState,
    category: BrakeCategory,
    vm: BrakeCertificateViewModel,
    now: Long,
    onGroupClick: () -> Unit,
    onHelp: (String) -> Unit,
) {
    val passenger = category.group == TrainGroup.PASSENGER
    val group = state.densityGroup ?: BrakeDensity.groupBySeries(state.locoSeriesText)
    val charge = state.chargePressureText.toFiniteDoubleOrNull()
    val axles = axlesForNorm(state)
    val locoCount = state.locoCountText.trim().toIntOrNull() ?: 1
    val minTime = group?.let { BrakeDensity.minTimeSeconds(it, axles, charge, locoCount) }

    if (!passenger) {
        SettingsCard {
            BlankField("Зарядное давление ТМ локомотива", "charge", state.chargePressureText, vm::setChargePressure, unit = "кгс/см²", onHelp = onHelp)
            SettingsCardSep()
            SettingsSelectRow(
                label = "Группа локомотива (табл. 9.1)",
                value = group?.name?.removePrefix("G")?.let { "№ $it" } ?: "выбрать",
                sub = group?.title
                    ?: if (state.locoSeriesText.isBlank()) "Выберите по серии или объёму ГР" else "Серии «${state.locoSeriesText}» нет в таблице - выберите группу по объёму ГР",
                onClick = onGroupClick,
            )
            SettingsCardSep()
            BlankField("Локомотивов с объединёнными ГР (СМЕ)", "group", state.locoCountText, vm::setLocoCount, unit = "шт", integer = true, onHelp = onHelp)
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                text = when {
                    group == null -> "Норма II положения: выберите группу локомотива."
                    minTime == null -> "Норма II положения: длина состава должна быть 1-480 осей (сейчас: $axles)."
                    else -> "Норма II положения: не менее ${fmt(minTime)} с (группа ${group.name.removePrefix("G")}, $axles осей" +
                        (if (BrakeDensity.chargeFactor(charge) < 1.0) ", с поправкой на зарядное давление" else "") + ")."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
    }

    TimerCard(
        title = "II положение - поездное",
        subtitle = if (passenger) "Перекрыть комбинированный кран, подождать 20 с, затем нажать «Старт» и замерить падение давления в ТМ"
        else "После снижения давления в ГР на 0,4-0,5 кгс/см² от предельного нажмите «Старт», на снижении ещё на 0,5 - «Стоп»",
        kind = TimerKind.II, timer = state.timerII, now = now, passenger = passenger, vm = vm,
        assess = { t ->
            if (passenger) BrakeDensity.assessPassenger(t, state.timerII.dropText.toFiniteDoubleOrNull())
            else BrakeDensity.assessFreightII(t, minTime)
        },
    )
    if (!passenger) {
        Spacer(Modifier.height(12.dp))
        TimerCard(
            title = "IV положение - перекрыша с питанием",
            subtitle = "Через 2 мин и более после ступени торможения (УР -0,5...0,7 кгс/см²), ручка в IV. Тот же замер",
            kind = TimerKind.IV, timer = state.timerIV, now = now, passenger = false, vm = vm,
            assess = { t -> BrakeDensity.assessFreightIV(t, state.timerII.recordedSec) },
        )
    }
    Spacer(Modifier.height(12.dp))
    TimerCard(
        title = "Контроль в пути",
        subtitle = "После стоянки, смены бригады и т.п.: отличие от замера II в справке не более 20 %, иначе сокращённое опробование",
        kind = TimerKind.ROUTE, timer = state.timerRoute, now = now, passenger = false, vm = vm,
        assess = { t -> BrakeDensity.assessRoute(t, state.timerII.recordedSec) },
        hideGr = passenger,
    )
    SettingsSectionNote("Замеры хранятся только на экране и сбрасываются при выходе. Экран не гаснет, пока идёт замер.")
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
                fontSize = 52.sp,
                fontFamily = MonoFont,
                fontWeight = FontWeight.Bold,
                color = if (timer.running) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (timer.running) {
                    Button(
                        modifier = Modifier.weight(1f).height(52.dp),
                        onClick = { vm.stopTimer(kind) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) { Text("Стоп", fontSize = 18.sp) }
                } else {
                    Button(
                        modifier = Modifier.weight(1f).height(52.dp),
                        onClick = { vm.startTimer(kind) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        ),
                    ) { Text(if (timer.recordedSec != null) "Заново" else "Старт", fontSize = 18.sp) }
                }
                if (!timer.running && (timer.recordedSec != null || elapsed > 0)) {
                    TextButton(onClick = { vm.resetTimer(kind) }) { Text("Сбросить") }
                }
            }
            if (passenger) {
                SmallLabeled("Падение давления в ТМ, кгс/см²") {
                    SmallField(timer.dropText, "0,2", { vm.setDrop(kind, it) })
                }
            } else if (!hideGr) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        SmallLabeled("Давление ГР в начале") { SmallField(timer.grStartText, "кгс/см²", { vm.setGrStart(kind, it) }) }
                    }
                    Box(Modifier.weight(1f)) {
                        SmallLabeled("Давление ГР в конце") { SmallField(timer.grEndText, "кгс/см²", { vm.setGrEnd(kind, it) }) }
                    }
                }
                val a = timer.grStartText.toFiniteDoubleOrNull()
                val b = timer.grEndText.toFiniteDoubleOrNull()
                if (a != null && b != null) {
                    val drop = a - b
                    val okDrop = kotlin.math.abs(drop - 0.5) <= 0.05
                    Text(
                        "Снижение: ${fmt(drop, 2)} кгс/см²" + if (okDrop) "" else " - по нормативу замер на снижении 0,5 кгс/см²",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (okDrop) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    )
                }
            }
            timer.recordedSec?.let { rec ->
                val a = assess(rec)
                Text(
                    "Записано: ${fmt(rec)} с",
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFont, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
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
                Text("$head. ${a.text}", style = MaterialTheme.typography.bodyMedium, color = color)
            }
        }
    }
}

@Composable
private fun SmallLabeled(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

// ───────────────────────────── Справочник и подсказки ─────────────────────────────

@Composable
private fun ReferenceBlock() {
    var expanded by remember { mutableStateOf(setOf<Int>()) }
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
                    Text(
                        modifier = Modifier.weight(1f),
                        text = card.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(if (open) "−" else "+", fontSize = 22.sp, color = MaterialTheme.colorScheme.tertiary)
                }
                if (open) {
                    Column(
                        modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        card.lines.forEach {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
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
            hint.who?.let {
                Text("Кто заполняет: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
            }
            Text(hint.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            hint.clause?.let {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Источник: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
