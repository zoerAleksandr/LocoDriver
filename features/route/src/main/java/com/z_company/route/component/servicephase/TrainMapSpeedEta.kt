package com.z_company.route.component.servicephase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.z_company.domain.track.TrackAnalysis
import com.z_company.domain.util.ArrivalPlan
import com.z_company.domain.util.TrainMapRoute

internal object TrainMapSpeedEtaTexts {
    const val TITLE = "Время прибытия"
    const val EMPTY = "Впереди нет станций плеча. Расчёт доступен, когда положение определено и вы на пути маршрута."
    const val SPEED_BLOCK = "Если ехать со средней скоростью:"
    const val NEED_SPEED = "Укажите скорость для расчёта времени"
    const val UNIT = "км/ч"
}

/**
 * Экран «Время прибытия» (SCREEN_SPECS §6.9): все станции плеча впереди с расстоянием и временем прибытия при средней
 * скорости, введённой внизу. Скорость по умолчанию - фактическая на момент открытия ([initialSpeedKmh]; стоим - 0).
 */
@Composable
internal fun TrainMapSpeedEtaScreen(
    ahead: List<ArrivalPlan.StopAhead>,
    initialSpeedKmh: Double?,
    nowMs: Long,
    zone: String,
    onDismiss: () -> Unit,
) {
    var speedText by remember {
        mutableStateOf(TextFieldValue(Math.round(initialSpeedKmh ?: 0.0).coerceIn(0L, 250L).toString()))
    }
    val speed = speedText.text.toIntOrNull()?.takeIf { it > 0 }?.toDouble()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Pst.bg)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
                PstSheetHeader(title = TrainMapSpeedEtaTexts.TITLE, onClose = onDismiss)
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    if (ahead.isEmpty()) {
                        PstNote(TrainMapSpeedEtaTexts.EMPTY)
                    } else {
                        Column(Modifier.fillMaxWidth().background(Pst.surface, RoundedCornerShape(16.dp))) {
                            ahead.forEachIndexed { i, a ->
                                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            a.stop.name.ifBlank { TrainMapPlanTexts.DEFAULT_TITLE },
                                            style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                                            color = Pst.text, maxLines = 2, modifier = Modifier.weight(1f),
                                        )
                                        Text(TrainMapEta.distance(a.distanceM), style = Pst.mono.copy(fontSize = 13.sp), color = Pst.textMuted, maxLines = 1)
                                    }
                                    val eta = TrainMapRoute.Eta.arrivalMs(nowMs, a.distanceM, speed)
                                    Text(
                                        text = if (eta == null) TrainMapSpeedEtaTexts.NEED_SPEED
                                        else "Прибуду в " + TrackAnalysis.clock(eta, zone) +
                                            if (ArrivalPlan.dayOffset(nowMs, eta, zone) > 0) " (следующие сутки)" else "",
                                        style = Pst.sans.copy(fontSize = 13.sp, fontWeight = if (eta == null) FontWeight.Normal else FontWeight.SemiBold),
                                        color = if (eta == null) Pst.textMuted else Pst.accent,
                                    )
                                }
                                if (i < ahead.lastIndex) HorizontalDivider(color = Pst.border)
                            }
                        }
                    }
                }
                PstSheetFooter {
                    Text(
                        TrainMapSpeedEtaTexts.SPEED_BLOCK,
                        style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        color = Pst.text,
                    )
                    PstTextField(
                        value = speedText,
                        onValueChange = { v ->
                            // Без ведущих нулей: «0» + «5» даёт «5», а не «05».
                            val digits = v.text.filter { it.isDigit() }.take(3)
                            val clean = if (digits.isEmpty()) "" else digits.trimStart('0').ifEmpty { "0" }
                            speedText = TextFieldValue(clean, androidx.compose.ui.text.TextRange(clean.length))
                        },
                        label = "Средняя скорость",
                        suffix = TrainMapSpeedEtaTexts.UNIT,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        mono = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
