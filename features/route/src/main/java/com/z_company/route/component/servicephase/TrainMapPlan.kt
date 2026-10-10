package com.z_company.route.component.servicephase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.domain.track.TrackAnalysis
import com.z_company.domain.util.ArrivalPlan
import com.z_company.domain.util.ArrivalPlanData
import com.z_company.domain.util.PlanTarget
import com.z_company.domain.util.TimeMask

/** Локальное хранилище плана прибытия (SharedPreferences, ключ - поезд); на сервер не уходит (SCREEN_SPECS §6.9, состояние 7). */
internal object TrainMapPlanStore {
    private const val PREFS = "train_map_prefs"
    private fun key(trainId: String) = "arrival_plan_$trainId"

    fun load(context: Context, trainId: String?): ArrivalPlanData? {
        if (trainId == null) return null
        return runCatching {
            ArrivalPlan.decode(context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key(trainId), null))
        }.getOrNull()
    }

    fun save(context: Context, trainId: String?, plan: ArrivalPlanData) {
        if (trainId == null) return
        runCatching {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(key(trainId), ArrivalPlan.encode(plan)).apply()
        }
    }

    fun clear(context: Context, trainId: String?) {
        if (trainId == null) return
        runCatching {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(key(trainId)).apply()
        }
    }
}

/** Флажок точки плана для слоя карты (древко слева, якорь - низ древка). Размер - в пикселях экрана. */
internal fun renderPlanFlag(density: Float): Bitmap {
    val w = (26 * density).toInt()
    val h = (34 * density).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val poleX = 3 * density
    val flag = Path().apply {
        moveTo(poleX, 2 * density)
        lineTo(w - 2 * density, 2 * density)
        lineTo(w - 7 * density, 10 * density)
        lineTo(w - 2 * density, 18 * density)
        lineTo(poleX, 18 * density)
        close()
    }
    val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5 * density
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    c.drawLine(poleX, 2 * density, poleX, h - 2 * density, outline)
    c.drawPath(flag, outline)
    val pole = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0A0E14")
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * density
        strokeCap = Paint.Cap.ROUND
    }
    c.drawLine(poleX, 2 * density, poleX, h - 2 * density, pole)
    c.drawPath(flag, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#D32F2F") })
    return bmp
}

private fun kmText(m: Double): String = TrainMapEta.distance(m)

private fun speedText(v: Double): String = "${Math.round(v).toInt()} км/ч"

/** Тексты состояний карточки планирования (SCREEN_SPECS §6.9, состояние 7). */
internal object TrainMapPlanTexts {
    const val DEFAULT_TITLE = "Точка на маршруте"
    const val FIELD_LABEL = "Плановое время прибытия"
    const val FIELD_PLACEHOLDER = "чч:мм"
    const val CALC = "Рассчитать"
    const val CLOSE = "Закрыть"
    const val COLLAPSE = "Свернуть"
    const val NO_LINE = "Линия пути ещё не загружена."
    const val RESET = "Сбросить план"
    const val INPUT_ERROR = "Введите время в формате чч:мм, например 18:30."
    const val NEXT_DAY_ASK = "Это время раньше текущего. Считать на следующие сутки?"
    const val NEXT_DAY_YES = "Да, на следующие сутки"
    const val NEXT_DAY_EDIT = "Изменить"
    const val OFF_RAIL = "Расчёт прибытия недоступен: положение дальше 50 м от рельсового пути. Вернитесь на путь маршрута."
    const val NO_FIX = "Расчёт прибытия недоступен: положение не определено."
    const val OVERDUE = "Плановое время уже прошло."
    const val SPEED_UNKNOWN = "Текущая скорость неизвестна."
}

/**
 * Карточка планирования прибытия: ввод планового времени ([plan] == null) либо результат расчёта ([plan] задан).
 * [distanceM] - расстояние вдоль линии до точки (null - сейчас не определить, [note] объясняет причину).
 */
@Composable
internal fun ArrivalPlanCard(
    target: PlanTarget,
    plan: ArrivalPlanData?,
    distanceM: Double?,
    note: String?,
    nowMs: Long,
    zone: String,
    speedKmh: Double?,
    onSave: (Long) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            PstIcon(PstIcons.MapPin, 18.dp, Pst.accent)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = target.name ?: TrainMapPlanTexts.DEFAULT_TITLE,
                    style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    color = Pst.text,
                    maxLines = 2,
                )
                if (distanceM != null) {
                    Text(
                        text = (if (target.name != null) "До неё " else "До точки ") + kmText(distanceM),
                        style = Pst.mono.copy(fontSize = 13.sp),
                        color = Pst.textMuted,
                    )
                }
            }
        }
        if (note != null) {
            Text(note, style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 18.sp), color = Pst.textMuted)
        }
        if (plan == null) PlanInput(nowMs, zone, enabled = distanceM != null, onSave = onSave)
        else if (distanceM != null) PlanResult(plan, distanceM, nowMs, zone, speedKmh)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (plan != null) {
                PstButton(TrainMapPlanTexts.RESET, onClick = onReset, kind = PstButtonKind.DANGER, height = 40.dp, fontSize = 14)
            }
            PstButton(if (plan != null) TrainMapPlanTexts.COLLAPSE else TrainMapPlanTexts.CLOSE, onClick = onClose, kind = PstButtonKind.TEXT, height = 40.dp, fontSize = 14)
        }
    }
}

@Composable
private fun PlanInput(nowMs: Long, zone: String, enabled: Boolean, onSave: (Long) -> Unit) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    var error by remember { mutableStateOf(false) }
    var pendingNextDay by remember { mutableStateOf<Long?>(null) }
    PstTextField(
        value = text,
        onValueChange = { v ->
            // Маска чч:мм: только цифры, двоеточие после часов ставится само (TimeMask).
            val clean = TimeMask.format(v.text, previous = text.text)
            text = if (clean == v.text) v else TextFieldValue(clean, TextRange(clean.length))
            error = false
            pendingNextDay = null
        },
        label = TrainMapPlanTexts.FIELD_LABEL,
        placeholder = TrainMapPlanTexts.FIELD_PLACEHOLDER,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        mono = true,
        isError = error,
        modifier = Modifier.fillMaxWidth(),
    )
    if (error) {
        Text(
            TrainMapPlanTexts.INPUT_ERROR,
            style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold),
            color = Pst.danger,
        )
    }
    val pending = pendingNextDay
    if (pending != null) {
        Text(TrainMapPlanTexts.NEXT_DAY_ASK, style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 18.sp), color = Pst.text)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PstButton(TrainMapPlanTexts.NEXT_DAY_YES, onClick = { onSave(pending) }, kind = PstButtonKind.CTA, height = 44.dp, fontSize = 14)
            PstButton(TrainMapPlanTexts.NEXT_DAY_EDIT, onClick = { pendingNextDay = null }, kind = PstButtonKind.TEXT, height = 44.dp, fontSize = 14)
        }
    } else {
        PstButton(
            text = TrainMapPlanTexts.CALC,
            kind = PstButtonKind.CTA,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val hm = ArrivalPlan.parseTime(text.text)
                if (hm == null) {
                    error = true
                } else {
                    val p = ArrivalPlan.planned(System.currentTimeMillis(), hm.first, hm.second, zone)
                    if (p.earlier) pendingNextDay = p.ms else onSave(p.ms)
                }
            },
        )
    }
}

@Composable
private fun PlanResult(plan: ArrivalPlanData, distanceM: Double, nowMs: Long, zone: String, speedKmh: Double?) {
    val ev = ArrivalPlan.evaluate(nowMs, distanceM, plan.plannedMs, speedKmh)
    val dayOff = ArrivalPlan.dayOffset(nowMs, plan.plannedMs, zone)
    val plannedText = TrackAnalysis.clock(plan.plannedMs, zone) + if (dayOff > 0) " (следующие сутки)" else ""
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PlanLine("Плановое прибытие", plannedText, strong = false)
        if (ev.requiredKmh != null) {
            PlanLine("Нужная средняя скорость", speedText(ev.requiredKmh!!), strong = true)
        } else {
            Text(TrainMapPlanTexts.OVERDUE, style = Pst.sans.copy(fontSize = 13.sp), color = Pst.danger)
        }
        if (speedKmh != null) PlanLine("Текущая скорость", speedText(speedKmh), strong = false)
        ev.expectedMs?.let {
            PlanLine("При текущей скорости прибудете", TrackAnalysis.clock(it, zone) + if (ArrivalPlan.dayOffset(nowMs, it, zone) > 0) " (следующие сутки)" else "", strong = false)
        }
        when (ev.status) {
            ArrivalPlan.Status.AHEAD ->
                Text("Опережаете", style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
            ArrivalPlan.Status.ON_TIME ->
                Text("Успеваете", style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
            ArrivalPlan.Status.BEHIND ->
                Text(
                    "Нужно быстрее на ${ev.faster} км/ч",
                    style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    color = Pst.danger,
                )
            ArrivalPlan.Status.UNKNOWN ->
                Text(TrainMapPlanTexts.SPEED_UNKNOWN, style = Pst.sans.copy(fontSize = 13.sp), color = Pst.textMuted)
            ArrivalPlan.Status.OVERDUE -> Unit
        }
    }
}

@Composable
private fun PlanLine(label: String, value: String, strong: Boolean) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = Pst.sans.copy(fontSize = 13.sp, fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal),
            color = if (strong) Pst.text else Pst.textMuted,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = Pst.mono.copy(fontSize = if (strong) 18.sp else 13.sp, fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal),
            color = Pst.text,
            maxLines = 1,
        )
    }
}
