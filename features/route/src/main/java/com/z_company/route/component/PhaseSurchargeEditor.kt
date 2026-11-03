package com.z_company.route.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.z_company.domain.entities.setting.SurchargeHeavyTrains
import com.z_company.domain.entities.setting.SurchargeLongTrains
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.z_company.domain.util.roundMoneyToCents
import com.z_company.domain.util.toMoneyString
import com.z_company.route.component.servicephase.Pst
import com.z_company.route.component.servicephase.PstIcons
import com.z_company.route.component.servicephase.PstTextField

/** Убирает полностью пустые строки перед сохранением в плечо. */
fun List<SurchargeHeavyTrains>.withoutBlankHeavyRows() = filter { it.weight.isNotBlank() || it.percentSurcharge.isNotBlank() }

/** Убирает полностью пустые строки перед сохранением в плечо. */
fun List<SurchargeLongTrains>.withoutBlankLongRows() = filter { it.conditionalLength.isNotBlank() || it.percentSurcharge.isNotBlank() }

/**
 * Доплаты плеча за тяжеловесные и длинносоставные поезда. Если список пуст,
 * для плеча действуют доплаты из настроек ЗП.
 */
@Composable
fun PhaseSurchargeEditor(
    rate: TextFieldValue,
    onRate: (TextFieldValue) -> Unit,
    currency: String,
    distanceKm: Int?,
    heavy: List<SurchargeHeavyTrains>,
    long: List<SurchargeLongTrains>,
    onHeavyChange: (List<SurchargeHeavyTrains>) -> Unit,
    onLongChange: (List<SurchargeLongTrains>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SubHead("Доплата за пробег")
        PstTextField(
            value = rate,
            onValueChange = onRate,
            label = "$currency/км",
            placeholder = "—",
            mono = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        // Сколько получит машинист за всё плечо: расстояние × ставка (как в расчёте зарплаты).
        val perKm = rate.text.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 && it.isFinite() }
        if (distanceKm != null && distanceKm > 0 && perKm != null) {
            Text(
                text = "За плечо: ${(distanceKm * perKm).roundMoneyToCents().toMoneyString(currency)}",
                style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                color = Pst.textMuted,
            )
        }
        TierBlock(title = "Тяжеловесные поезда", onAdd = { onHeavyChange(heavy + SurchargeHeavyTrains()) }) {
            heavy.forEachIndexed { index, item ->
                key(item.id) {
                    TierRow(
                        thresholdLabel = "Вес, т",
                        threshold = item.weight,
                        percent = item.percentSurcharge,
                        onThreshold = { v -> onHeavyChange(heavy.toMutableList().also { it[index] = item.copy(weight = v) }) },
                        onPercent = { v -> onHeavyChange(heavy.toMutableList().also { it[index] = item.copy(percentSurcharge = v) }) },
                        onDelete = { onHeavyChange(heavy.toMutableList().also { it.removeAt(index) }) },
                    )
                }
            }
        }
        TierBlock(title = "Длинносоставные поезда", onAdd = { onLongChange(long + SurchargeLongTrains()) }) {
            long.forEachIndexed { index, item ->
                key(item.id) {
                    TierRow(
                        thresholdLabel = "Длина, ваг.",
                        threshold = item.conditionalLength,
                        percent = item.percentSurcharge,
                        onThreshold = { v -> onLongChange(long.toMutableList().also { it[index] = item.copy(conditionalLength = v) }) },
                        onPercent = { v -> onLongChange(long.toMutableList().also { it[index] = item.copy(percentSurcharge = v) }) },
                        onDelete = { onLongChange(long.toMutableList().also { it.removeAt(index) }) },
                    )
                }
            }
        }
        Text(
            text = "Если списки тяжеловесных и длинносоставных пусты, берутся доплаты из настроек ЗП. Доплата начинается, когда значение больше порога.",
            style = Pst.sans.copy(fontSize = 12.sp),
            color = Pst.textMuted,
        )
    }
}

/** Подзаголовок внутри блока: одинаковый для всех подразделов и мельче заголовка блока. */
@Composable
private fun SubHead(text: String) {
    Text(
        text = text,
        style = Pst.sans.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
        color = Pst.textMuted,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun TierBlock(title: String, onAdd: () -> Unit, rows: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { SubHead(title) }
            Text(
                text = "Добавить",
                style = Pst.sans.copy(fontSize = 13.sp),
                color = Pst.accent,
                modifier = Modifier.clickable(onClick = onAdd).padding(8.dp),
            )
        }
        rows()
    }
}

@Composable
private fun TierRow(
    thresholdLabel: String,
    threshold: String,
    percent: String,
    onThreshold: (String) -> Unit,
    onPercent: (String) -> Unit,
    onDelete: () -> Unit,
) {
    // Свои TextFieldValue на строку, чтобы курсор не прыгал в конец при правке.
    var thresholdField by remember { mutableStateOf(TextFieldValue(threshold)) }
    var percentField by remember { mutableStateOf(TextFieldValue(percent)) }
    val numeric = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PstTextField(
            value = thresholdField,
            onValueChange = { v ->
                val clean = v.copy(text = v.text.filter(Char::isDigit))
                thresholdField = clean
                onThreshold(clean.text)
            },
            label = thresholdLabel,
            mono = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        PstTextField(
            value = percentField,
            onValueChange = { v ->
                val clean = v.copy(text = v.text.filter { it.isDigit() || it == ',' || it == '.' })
                percentField = clean
                onPercent(clean.text)
            },
            label = "Доплата, %",
            mono = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = numeric,
        )
        Icon(
            imageVector = PstIcons.Trash,
            contentDescription = "Удалить строку",
            tint = Pst.danger,
            modifier = Modifier.size(40.dp).clickable(onClick = onDelete).padding(8.dp),
        )
    }
}
