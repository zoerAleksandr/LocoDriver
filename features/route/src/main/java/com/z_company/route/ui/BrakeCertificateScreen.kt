package com.z_company.route.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.brake.BrakeCategory
import com.z_company.domain.brake.BrakeCertificateCalculator
import com.z_company.domain.brake.BrakeCertificateResult
import com.z_company.domain.brake.BrakeNorms
import com.z_company.domain.brake.CompositeShare
import com.z_company.domain.brake.GradeClass
import com.z_company.domain.brake.SpeedLimit
import com.z_company.domain.brake.TrainGroup
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.ui.settings.SettingsCard
import com.z_company.route.ui.settings.SettingsCardSep
import com.z_company.route.ui.settings.SettingsFieldRow
import com.z_company.route.ui.settings.SettingsGroupHeader
import com.z_company.route.ui.settings.SettingsPickerOption
import com.z_company.route.ui.settings.SettingsPickerSheet
import com.z_company.route.ui.settings.SettingsSectionNote
import com.z_company.route.ui.settings.SettingsSelectRow
import com.z_company.route.ui.settings.SettingsSwitchRow
import com.z_company.route.viewmodel.BrakeCertificateUiState
import com.z_company.route.viewmodel.BrakeCertificateViewModel
import com.z_company.route.viewmodel.toInput

private sealed interface BrakeSheet {
    data object Category : BrakeSheet
    data object Grade : BrakeSheet
    data object Composite : BrakeSheet
    data object ManualNorm : BrakeSheet
    data class RowPreset(val rowId: Int) : BrakeSheet
}

private fun fmt(v: Double, digits: Int = 1): String {
    val factor = Math.pow(10.0, digits.toDouble())
    val r = Math.round(v * factor) / factor
    val s = if (r % 1.0 == 0.0 && digits > 0) r.toLong().toString() else r.toString()
    return s.replace('.', ',')
}

/** Экран-инструмент «Справка ВУ-45» (SCREEN_SPECS §26): расчёт потребного/фактического нажатия, скорости и ручных тормозов. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrakeCertificateScreen(
    state: BrakeCertificateUiState,
    onBack: () -> Unit,
    viewModel: BrakeCertificateViewModel,
) {
    var sheet by remember { mutableStateOf<BrakeSheet?>(null) }
    val category = BrakeNorms.category(state.categoryId) ?: BrakeNorms.categories.first()
    val input = remember(state) { state.toInput() }
    val result = remember(input) { input?.let(BrakeCertificateCalculator::calculate) }
    val primary = MaterialTheme.colorScheme.primary

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            // ── Поезд ──
            SettingsGroupHeader("ПОЕЗД", top = 4.dp, startPad = 4.dp)
            SettingsCard {
                SettingsSelectRow(
                    label = "Категория поезда",
                    value = "${category.norm} тс/100 тс",
                    sub = category.title,
                    mono = true,
                    onClick = { sheet = BrakeSheet.Category },
                )
                SettingsCardSep()
                BrakeNumberField("Вес состава (без локомотива)", "тс", state.weightText, viewModel::setWeight)
                SettingsCardSep()
                BrakeNumberField("Осей в составе (для сверки, необязательно)", "оси", state.declaredAxlesText, viewModel::setDeclaredAxles, integer = true)
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
                        sub = "Зелёный огонь АЛСН - значение в скобках табл. 1 (${category.vFlatAutoblock} км/ч)",
                        checked = state.autoblock3,
                        onCheckedChange = viewModel::setAutoblock,
                    )
                }
                if (category.isLoadedFreight) {
                    SettingsCardSep()
                    SettingsSelectRow(
                        label = "Нагрузка на ось > 21 тс, композит",
                        value = when (state.compositeShare) {
                            CompositeShare.NONE -> "нет"
                            CompositeShare.P75 -> "К-75"
                            CompositeShare.P100 -> "К-100"
                        },
                        sub = "П. 3.3 Норм: ВР на среднем режиме, доля композиционных колодок",
                        onClick = { sheet = BrakeSheet.Composite },
                    )
                }
            }

            // ── Таблица нажатий ──
            SettingsGroupHeader("ТОРМОЗНОЕ НАЖАТИЕ НА ОСЬ", top = 20.dp, startPad = 4.dp)
            SettingsCard {
                state.rows.forEachIndexed { index, row ->
                    if (index > 0) SettingsCardSep()
                    val preset = row.presetIndex?.let { BrakeNorms.axlePresets.getOrNull(it) }
                    SettingsSelectRow(
                        label = preset?.title ?: "Тип вагона (табл. 3) или своё значение",
                        value = if (row.pressureText.isNotBlank()) "${row.pressureText} тс" else "выбрать",
                        mono = true,
                        onClick = { sheet = BrakeSheet.RowPreset(row.id) },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            BrakeNumberField("Нажатие на ось", "тс", row.pressureText, { viewModel.setRowPressure(row.id, it) })
                        }
                        Column(Modifier.weight(1f)) {
                            BrakeNumberField("Осей", "оси", row.axlesText, { viewModel.setRowAxles(row.id, it) }, integer = true)
                        }
                        if (state.rows.size > 1) {
                            Text(
                                modifier = Modifier
                                    .clickable { viewModel.removeRow(row.id) }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                text = "Удалить",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                SettingsCardSep()
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.addRow() }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    text = "+ Добавить строку",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            SettingsSectionNote(
                "Значения - расчётные нажатия колодок в пересчёте на чугунные (тс на ось). " +
                    "Для композиционных колодок с трафаретом - величина по трафарету.",
            )

            // ── Локомотив ──
            SettingsGroupHeader("ЛОКОМОТИВ", top = 20.dp, startPad = 4.dp)
            SettingsCard {
                SettingsSwitchRow(
                    label = "Учитывать локомотив",
                    sub = if (category.group == TrainGroup.FREIGHT) {
                        "В грузовых поездах вес и нажатие локомотива не учитываются"
                    } else "Учётный вес (табл. 5) и нажатие колодок на ось (табл. 4)",
                    checked = state.locoEnabled,
                    onCheckedChange = viewModel::setLocoEnabled,
                )
                if (state.locoEnabled) {
                    SettingsCardSep()
                    BrakeNumberField("Учётный вес локомотива", "тс", state.locoWeightText, viewModel::setLocoWeight)
                    SettingsCardSep()
                    BrakeNumberField("Тормозных осей", "оси", state.locoAxlesText, viewModel::setLocoAxles, integer = true)
                    SettingsCardSep()
                    BrakeNumberField("Нажатие на ось", "тс", state.locoPressureText, viewModel::setLocoPressure)
                }
            }

            // ── Ручные тормоза ──
            if (category.manualBrakesRequired) {
                SettingsGroupHeader("РУЧНЫЕ ТОРМОЗА", top = 20.dp, startPad = 4.dp)
                SettingsCard {
                    SettingsSelectRow(
                        label = "Норма осей на 100 тс",
                        value = state.manualNorm.title.substringBefore(" - "),
                        sub = "Выбор запоминается",
                        mono = true,
                        onClick = { sheet = BrakeSheet.ManualNorm },
                    )
                    SettingsCardSep()
                    BrakeNumberField("Фактически ручных осей (необязательно)", "оси", state.actualManualText, viewModel::setActualManual, integer = true)
                }
            }

            // ── Результат ──
            SettingsGroupHeader("РЕЗУЛЬТАТ", top = 20.dp, startPad = 4.dp)
            ResultCard(result = result, category = category)
            SettingsSectionNote(
                "Расчёт по Инструкции ЦТ-ЦВ-ЦЛ-ВНИИЖТ/277, приложение 2 (редакция 1994 г.). " +
                    "Справочный инструмент: официальной справкой ВУ-45 является документ, выданный осмотрщиком вагонов. " +
                    "Данные нигде не сохраняются.",
            )
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
        BrakeSheet.Composite -> SettingsPickerSheet(
            title = "Композиционные колодки (п. 3.3)",
            hint = "Для груженых поездов с вагонами с нагрузкой от колёсной пары более 21 тс, автотормоза включены, ВР на среднем режиме",
            onDismiss = { sheet = null },
            options = listOf(
                CompositeShare.NONE to "Не применять",
                CompositeShare.P75 to "К-75: не менее 75 % вагонов (31 тс на 100 тс)",
                CompositeShare.P100 to "К-100: 100 % вагонов (30 тс на 100 тс)",
            ).map { (v, label) ->
                SettingsPickerOption(label = label, selected = v == state.compositeShare, onSelect = { viewModel.setCompositeShare(v) })
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
        is BrakeSheet.RowPreset -> SettingsPickerSheet(
            title = "Расчётное нажатие на ось (табл. 3)",
            hint = "Выберите тип вагона и режим или введите значение вручную",
            onDismiss = { sheet = null },
            options = BrakeNorms.axlePresets.mapIndexed { index, p ->
                SettingsPickerOption(
                    label = p.title,
                    sub = "${BrakeCertificateViewModel.formatPressure(p.pressure)} тс на ось",
                    selected = state.rows.firstOrNull { it.id == s.rowId }?.presetIndex == index,
                    onSelect = { viewModel.setRowPreset(s.rowId, index) },
                )
            },
        )
        null -> Unit
    }
}

@Composable
private fun BrakeNumberField(
    label: String,
    unit: String,
    value: String,
    onChange: (String) -> Unit,
    integer: Boolean = false,
) {
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        OutlinedTextFieldApp(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = { raw ->
                val allowed = if (integer) raw.filter { it.isDigit() } else raw.filter { it.isDigit() || it == ',' || it == '.' }
                onChange(allowed.take(10))
            },
            singleLine = true,
            suffix = { Text(unit, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFont),
            shape = RoundedCornerShape(12.dp),
            fieldElevation = 0.dp,
            colorBackgroundEmptyField = MaterialTheme.colorScheme.surfaceBright,
            colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surfaceBright,
        )
    }
}

@Composable
private fun ResultCard(result: BrakeCertificateResult?, category: BrakeCategory) {
    if (result == null) {
        SettingsCard {
            Text(
                modifier = Modifier.padding(18.dp),
                text = "Введите вес состава, чтобы увидеть потребное нажатие.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val okColor = MaterialTheme.colorScheme.tertiary
    val badColor = MaterialTheme.colorScheme.error
    SettingsCard {
        SettingsFieldRow("Вес для расчёта", "${fmt(result.totalWeight)} тс", mono = true)
        SettingsCardSep()
        SettingsFieldRow("Потребное нажатие (${category.norm} на 100 тс)", "${fmt(result.requiredPressure)} тс", mono = true)
        SettingsCardSep()
        SettingsFieldRow("Фактическое нажатие", "${fmt(result.actualPressure)} тс", mono = true)
        SettingsCardSep()
        SettingsFieldRow("На 100 тс веса", "${fmt(result.actualPer100, 2)} тс", mono = true)
        SettingsCardSep()
        SettingsFieldRow("Всего осей", "${result.totalAxles}", mono = true)
    }
    Spacer(Modifier.height(12.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val hasRows = result.actualPressure > 0
        if (!hasRows) {
            Text("Заполните нажатие и число осей", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (result.enough) {
            Text(
                "Нажатие достаточно: запас ${fmt(result.actualPressure - result.requiredPressure)} тс",
                color = okColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
            )
        } else {
            Text(
                "Нажатие недостаточно: не хватает ${fmt(result.requiredPressure - result.actualPressure)} тс " +
                    "(${fmt(result.shortagePer100, 2)} тс на 100 тс)",
                color = badColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
            )
        }
        if (hasRows) {
            when (val sp = result.speed) {
                is SpeedLimit.Allowed -> {
                    Text(
                        "Допускаемая скорость: ${sp.kmh} км/ч",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFont),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        sp.note + (sp.reducedFrom?.let { " (по табл. 1 было бы $it км/ч)" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is SpeedLimit.NotDetermined -> Text(
                    sp.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = badColor,
                )
            }
        }
        result.requiredManualAxles?.let { req ->
            Text(
                "Ручных тормозных осей: потребно $req" +
                    (result.manualAxlesOk?.let { if (it) " - достаточно" else " - не хватает" } ?: ""),
                color = when (result.manualAxlesOk) {
                    true -> okColor
                    false -> badColor
                    null -> MaterialTheme.colorScheme.primary
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (category.group == TrainGroup.PASSENGER) {
            Text(
                "Ручные тормоза пассажирского поезда: приводятся в действие у всех вагонов состава.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        result.warnings.forEach {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
