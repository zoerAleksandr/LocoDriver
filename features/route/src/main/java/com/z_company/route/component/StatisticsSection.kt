package com.z_company.route.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import kotlin.math.abs
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.util.CalculationEnergy.rounding
import com.z_company.domain.util.CalculationEnergy.getTotalFuelConsumption
import com.z_company.domain.util.minus
import com.z_company.domain.util.plus
import com.z_company.domain.util.str
import com.z_company.domain.util.times
import com.z_company.domain.util.toDoubleOrZero
import com.z_company.route.viewmodel.DieselSectionFormState
import com.z_company.route.viewmodel.ElectricSectionFormState

private val NormaColor = Color(0xFFE29960)

@Composable
fun ElectricStatisticsSection(
    electricSectionListState: SnapshotStateList<ElectricSectionFormState>?,
    locomotive: Locomotive,
    isShowOtherCurrent: Boolean,
    onSettingsClick: () -> Unit,
    onNorma1Change: (String) -> Unit = {},
    onNorma2Change: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var overResult: Double? = null
    var overRecovery: Double? = null
    var overResult2: Double? = null
    var overRecovery2: Double? = null

    electricSectionListState?.forEach {
        val accepted = it.accepted.data?.toDoubleOrNull()
        val delivery = it.delivery.data?.toDoubleOrNull()
        val acceptedRecovery = it.recoveryAccepted.data?.toDoubleOrNull()
        val deliveryRecovery = it.recoveryDelivery.data?.toDoubleOrNull()
        val accepted2 = it.accepted2.data?.toDoubleOrNull()
        val delivery2 = it.delivery2.data?.toDoubleOrNull()
        val acceptedRecovery2 = it.recoveryAccepted2.data?.toDoubleOrNull()
        val deliveryRecovery2 = it.recoveryDelivery2.data?.toDoubleOrNull()

        overResult += (delivery - accepted)
        overRecovery += (deliveryRecovery - acceptedRecovery)
        overResult2 += (delivery2 - accepted2)
        overRecovery2 += (deliveryRecovery2 - acceptedRecovery2)
    }

    val monoLabel = MaterialTheme.typography.labelSmall.copy(
        fontFamily = MonoFont,
        fontSize = 11.sp,
        fontWeight = FontWeight.W600,
        letterSpacing = TextUnit(1.4f, TextUnitType.Sp)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        EnergyTotals(
            currentLabel = if (isShowOtherCurrent) "ТОК 1" else null,
            consumed = overResult,
            recovery = overRecovery,
            norma = locomotive.normaElectricCurrent1?.str() ?: "",
            onNormaChange = onNorma1Change,
            monoLabel = monoLabel
        )
        if (isShowOtherCurrent) {
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            EnergyTotals(
                currentLabel = "ТОК 2",
                consumed = overResult2,
                recovery = overRecovery2,
                norma = locomotive.normaElectricCurrent2?.str() ?: "",
                onNormaChange = onNorma2Change,
                monoLabel = monoLabel
            )
        }
    }
}

/** Блок ИТОГО для одного рода тока: чистый расход + раскладка + норма + результат. */
@Composable
private fun EnergyTotals(
    currentLabel: String?,
    consumed: Double?,
    recovery: Double?,
    norma: String,
    onNormaChange: (String) -> Unit,
    monoLabel: androidx.compose.ui.text.TextStyle,
) {
    val net = (consumed ?: 0.0) - (recovery ?: 0.0)
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 14.dp)) {
        if (currentLabel != null) {
            Text(
                text = currentLabel,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
        // РАСХОД — основной показатель, крупно
        EnergyAmountLine(
            label = "РАСХОД",
            value = groupThousands(rounding(consumed, 2)?.str()),
            valueStyle = MaterialTheme.typography.headlineMedium,
            valueColor = MaterialTheme.colorScheme.primary,
            monoLabel = monoLabel,
            topPadding = 0.dp
        )
        // РЕКУПЕРАЦИЯ и ЧИСТЫЙ РАСХОД — только при наличии рекуперации, меньшим шрифтом
        if (recovery != null) {
            EnergyAmountLine(
                label = "РЕКУПЕРАЦИЯ",
                value = groupThousands(rounding(recovery, 2)?.str()),
                valueStyle = MaterialTheme.typography.titleMedium,
                valueColor = Color(0xFF00B341),
                monoLabel = monoLabel,
                topPadding = 8.dp
            )
            EnergyAmountLine(
                label = "ЧИСТЫЙ РАСХОД",
                value = groupThousands(rounding(net, 2).str()),
                valueStyle = MaterialTheme.typography.titleMedium,
                valueColor = MaterialTheme.colorScheme.primary,
                monoLabel = monoLabel,
                topPadding = 8.dp
            )
        }
    }

    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "НОРМА", style = monoLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            NormaPill(value = norma, unit = "", onValueChange = onNormaChange)
        }
        if (norma.isNotBlank()) {
            val n = norma.toDoubleOrZero() ?: 0.0
            // Результат считается от расхода (а не от чистого расхода)
            val result = n - (consumed ?: 0.0)
            val pct = if (n != 0.0) abs(result / n * 100).toInt() else 0
            ResultCard(result = result, magnitude = groupThousands(rounding(abs(result), 2).str()), unit = "", percent = pct)
        }
    }
}

/** Подпись капслоком + значение (крупное для расхода, поменьше для рекуперации/чистого). */
@Composable
private fun EnergyAmountLine(
    label: String,
    value: String,
    valueStyle: androidx.compose.ui.text.TextStyle,
    valueColor: Color,
    monoLabel: androidx.compose.ui.text.TextStyle,
    topPadding: androidx.compose.ui.unit.Dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding)
    ) {
        Text(
            text = label,
            style = monoLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alignByBaseline()
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = valueStyle.copy(fontFamily = MonoFont, fontWeight = FontWeight.Bold),
            color = valueColor,
            modifier = Modifier.alignByBaseline()
        )
    }
}

/** Группировка целой части числа по разрядам через неразрывный пробел: 2710 → «2 710». */
private fun groupThousands(s: String?): String {
    if (s.isNullOrBlank()) return "0"
    val neg = s.startsWith("-") || s.startsWith("−")
    val body = s.trimStart('-', '−')
    val dot = body.indexOf('.')
    val intPart = if (dot < 0) body else body.substring(0, dot)
    val frac = if (dot < 0) "" else body.substring(dot)
    val grouped = intPart.reversed().chunked(3).joinToString(" ").reversed()
    // Единый формат по приложению: дробная часть через запятую (12 345.6 → 12 345,6).
    return (if (neg) "−" else "") + grouped + frac.replace('.', ',')
}

@Composable
private fun BreakdownRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFont, fontWeight = FontWeight.W600),
            color = valueColor
        )
    }
}

@Composable
fun DieselStatisticsSection(
    dieselSectionListState: SnapshotStateList<DieselSectionFormState>?,
    locomotive: Locomotive,
    onSettingsClick: () -> Unit,
    onNormaChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var overResultInLiter: Double? = null
    var overResultInKilo: Double? = null
    var totalRefuelLiter: Double? = null
    var totalRefuelKilo: Double? = null

    dieselSectionListState?.forEach {
        val accepted = it.accepted.data?.toDoubleOrNull()
        val delivery = it.delivery.data?.toDoubleOrNull()
        val refuel = it.refuel.data?.toDoubleOrNull()
        val result = getTotalFuelConsumption(accepted, delivery, refuel)
        val resultInKilo = result.times(it.coefficient.data?.toDoubleOrZero())
        overResultInLiter += result
        overResultInKilo += resultInKilo
        totalRefuelLiter += it.refuel.data?.toDoubleOrNull()
        totalRefuelKilo += it.refuelInKilo.data?.toDoubleOrNull()
    }

    val litersText = rounding(overResultInLiter, 2)?.str() ?: "—"
    val kiloText = rounding(overResultInKilo, 2)?.str()
    val refuelText = rounding(totalRefuelLiter, 2)?.str() ?: "0"
    val refuelKiloText = rounding(totalRefuelKilo, 2)?.str()
    val monoLabel = MaterialTheme.typography.labelSmall.copy(
        fontFamily = MonoFont,
        fontSize = 11.sp,
        fontWeight = FontWeight.W600,
        letterSpacing = TextUnit(1.4f, TextUnitType.Sp)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Расход + Заправка — одинаковым стилем
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 14.dp)) {
            AmountBlock(label = "РАСХОД", liters = litersText, kilo = kiloText, monoLabel = monoLabel)
            if (totalRefuelLiter != null) {
                Box(modifier = Modifier.height(14.dp))
                AmountBlock(
                    label = "ЭКИПИРОВКА",
                    liters = refuelText,
                    kilo = refuelKiloText,
                    monoLabel = monoLabel
                )
            }
        }

        androidx.compose.material3.HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Норма + результат
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "НОРМА",
                    style = monoLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NormaPill(
                    value = locomotive.normaDiesel ?: "",
                    unit = "кг",
                    onValueChange = onNormaChange
                )
            }

            if (!locomotive.normaDiesel.isNullOrBlank()) {
                val result = (locomotive.normaDiesel?.toDoubleOrZero() - overResultInKilo) ?: 0.0
                val norma = locomotive.normaDiesel?.toDoubleOrZero() ?: 0.0
                val pct = if (norma != 0.0) abs(result / norma * 100).toInt() else 0
                ResultCard(result = result, magnitude = rounding(abs(result), 2).str(), unit = "кг", percent = pct)
            }
        }
    }
}

/** Блок суммарной величины: подпись + крупное число (л) + пилюля (кг). */
@Composable
private fun AmountBlock(
    label: String,
    liters: String,
    kilo: String?,
    monoLabel: androidx.compose.ui.text.TextStyle,
) {
    Text(
        text = label,
        style = monoLabel,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    // Крупно — килограммы, в пилюле — литры (если коэффициент задан)
    val hasKilo = kilo != null
    val bigText = if (hasKilo) kilo!! else liters
    val bigUnit = if (hasKilo) "кг" else "л"
    val pillText = if (hasKilo) "$liters л" else null
    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = bigText,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = MonoFont, fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = bigUnit,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFont),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        pillText?.let {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surfaceBright)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = MonoFont, fontWeight = FontWeight.W600
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** Поле нормы — пилюля с единицей справа (как NormPill в референсе). */
@Composable
private fun NormaPill(value: String, unit: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .clip(Shapes.small)
            .background(MaterialTheme.colorScheme.secondary),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextFieldApp(
            modifier = Modifier.width(150.dp),
            value = value,
            onValueChange = { onValueChange(it.take(7)) },
            textStyle = MaterialTheme.typography.titleMedium.copy(
                fontFamily = MonoFont, fontWeight = FontWeight.Bold
            ),
            fieldElevation = 0.dp,
            borderColor = Color.Transparent,
            colorBackgroundEmptyField = MaterialTheme.colorScheme.surfaceBright,
            colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surfaceBright,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            suffix = {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFont),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done
            ),
            singleLine = true,
            shape = Shapes.small,
        )
    }
}

/** Карточка результата: ПЕРЕРАСХОД (red) при result<0, ЭКОНОМИЯ (green) иначе. */
@Composable
private fun ResultCard(result: Double, magnitude: String, unit: String, percent: Int, modifier: Modifier = Modifier) {
    val isOver = result < 0
    val green = Color(0xFF00B341)
    val accentColor = if (isOver) MaterialTheme.colorScheme.error else green
    val label = if (isOver) "ПЕРЕРАСХОД" else "ЭКОНОМИЯ"
    val sign = if (isOver) "−" else "+"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accentColor.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isOver) "↓" else "↑",
                style = MaterialTheme.typography.titleMedium,
                color = accentColor
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = MonoFont, letterSpacing = TextUnit(1f, TextUnitType.Sp)
                ),
                color = accentColor
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$sign$magnitude",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = MonoFont, fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (unit.isBlank()) " ($sign$percent%)" else " $unit  ($sign$percent%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp, bottom = 1.dp)
                )
            }
        }
    }
}

/** Баннер итога расхода (электровоз): ПЕРЕРАСХОД/ЭКОНОМИЯ. */
@Composable
private fun ResultBanner(result: Double, value: String, modifier: Modifier = Modifier) {
    val isOver = result < 0
    val green = Color(0xFF00B341)
    val accentColor = if (isOver) MaterialTheme.colorScheme.error else green
    val label = if (isOver) "ПЕРЕРАСХОД" else "ЭКОНОМИЯ"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = accentColor)
        Text(text = value, style = MaterialTheme.typography.titleSmall, color = accentColor)
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onBackground,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor
        )
    }
}
