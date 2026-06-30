package com.z_company.route.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.z_company.domain.util.str2decimalSign
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

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Ток 1
        StatRow(
            label = "Расход",
            value = rounding(overResult, 2)?.str() ?: "—"
        )
        StatRow(
            label = "Норма",
            value = locomotive.normaElectricCurrent1?.str() ?: "—",
            valueColor = NormaColor,
            onClick = onSettingsClick
        )
        if (locomotive.normaElectricCurrent1 != null) {
            val result = locomotive.normaElectricCurrent1!! - (overResult ?: 0.0)
            val rounded = rounding(result, 2) ?: result
            val resultText = if (rounded > 0) "+${rounded.str()}" else rounded.str()
            val resultColor = if (rounded < 0) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
            ResultBanner(result = rounded, value = resultText)
        }
        StatRow(
            label = "Рекуперация",
            value = rounding(overRecovery, 2)?.str() ?: "—"
        )

        // Ток 2
        if (isShowOtherCurrent) {
            Text(
                text = "Ток 2",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            StatRow(
                label = "Расход",
                value = rounding(overResult2, 2)?.str() ?: "—"
            )
            StatRow(
                label = "Норма",
                value = locomotive.normaElectricCurrent2?.str() ?: "—",
                valueColor = NormaColor,
                onClick = onSettingsClick
            )
            if (locomotive.normaElectricCurrent2 != null) {
                val result2 = locomotive.normaElectricCurrent2!! - (overResult2 ?: 0.0)
                val rounded2 = rounding(result2, 2) ?: result2
                val resultText2 = if (rounded2 > 0) "+${rounded2.str()}" else rounded2.str()
                val resultColor2 = if (rounded2 < 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
                ResultBanner(result = rounded2, value = resultText2)
            }
            StatRow(
                label = "Рекуперация",
                value = rounding(overRecovery2, 2)?.str() ?: "—"
            )
        }
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
                    label = "ЗАПРАВКА ВСЕХ СЕКЦИЙ",
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
    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = liters,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = MonoFont, fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "л",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFont),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        kilo?.let {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surfaceBright)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$it кг",
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
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            colorBackgroundEmptyField = Color.Transparent,
            colorBackgroundNotEmptyField = Color.Transparent,
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
    val sign = if (isOver) "+" else "−"
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
                text = if (isOver) "↗" else "↘",
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
                    text = " $unit  ($sign$percent%)",
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
