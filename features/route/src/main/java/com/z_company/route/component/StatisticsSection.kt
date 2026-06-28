package com.z_company.route.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier
) {
    var overResultInLiter: Double? = null
    var overResultInKilo: Double? = null

    dieselSectionListState?.forEach {
        val accepted = it.accepted.data?.toDoubleOrNull()
        val delivery = it.delivery.data?.toDoubleOrNull()
        val refuel = it.refuel.data?.toDoubleOrNull()
        val result = getTotalFuelConsumption(accepted, delivery, refuel)
        val resultInKilo = result.times(it.coefficient.data?.toDoubleOrZero())
        overResultInLiter += result
        overResultInKilo += resultInKilo
    }

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatRow(
            label = "Расход (Л)",
            value = rounding(overResultInLiter, 2)?.toString() ?: "—"
        )
        StatRow(
            label = "Расход (Кг)",
            value = rounding(overResultInKilo, 2)?.str() ?: "—"
        )
        StatRow(
            label = "Норма (Кг)",
            value = locomotive.normaDiesel ?: "—",
            valueColor = NormaColor,
            onClick = onSettingsClick
        )
        if (!locomotive.normaDiesel.isNullOrBlank()) {
            val result = (locomotive.normaDiesel?.toDoubleOrZero() - overResultInKilo) ?: 0.0
            val resultText = if (result > 0) {
                "+${rounding(result, 2).str()}"
            } else {
                rounding(result, 2).str2decimalSign()
            }
            val resultColor = if (result < 0) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
            ResultBanner(result = result, value = resultText)
        }
    }
}

/** Баннер итога расхода: ПЕРЕРАСХОД (red-soft) при result<0, ЭКОНОМИЯ (green-soft) иначе. */
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
            .background(accentColor.copy(alpha = 0.10f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = accentColor,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = accentColor,
        )
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
