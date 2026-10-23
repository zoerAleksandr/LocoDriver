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

    /** Запомненное время на остановку (мин) и признак «на остановку» для следующих расчётов. */
    fun stopMin(context: Context): Int = runCatching {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("plan_stop_min", ArrivalPlan.DEFAULT_STOP_MIN)
    }.getOrDefault(ArrivalPlan.DEFAULT_STOP_MIN)

    fun onStop(context: Context): Boolean = runCatching {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("plan_on_stop", false)
    }.getOrDefault(false)

    fun saveStop(context: Context, onStop: Boolean, stopMin: Int) {
        runCatching {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean("plan_on_stop", onStop).putInt("plan_stop_min", stopMin).apply()
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

/** Тексты экрана «Цель» и карточки расчёта (SCREEN_SPECS §6.9, состояние 7). */
internal object TrainMapPlanTexts {
    const val DEFAULT_TITLE = "Точка на маршруте"
    const val FIELD_LABEL = "Время прибытия"
    const val FIELD_PLACEHOLDER = "чч:мм"
    const val CALC = "Рассчитать"
    const val CLOSE = "Закрыть"
    const val HIDE = "Скрыть"
    const val NO_LINE = "Линия пути ещё не загружена."
    const val INPUT_ERROR = "Введите время в формате чч:мм, например 18:30."
    const val NEXT_DAY_ASK = "Это время раньше текущего. Считать на следующие сутки?"
    const val NEXT_DAY_YES = "Да, на следующие сутки"
    const val NEXT_DAY_EDIT = "Изменить"
    const val OFF_RAIL = "Расчёт прибытия недоступен: положение дальше 50 м от рельсового пути. Вернитесь на путь маршрута."
    const val NO_FIX = "Расчёт прибытия недоступен: положение не определено."
    const val OVERDUE = "Время прибытия уже прошло."
    const val IMPOSSIBLE = "Не успеть: с учётом разгона от текущей скорости времени не хватает."
    const val SPEED_UNKNOWN = "Текущая скорость неизвестна."
    const val TARGET_TITLE = "Цель"
    const val STEP1 = "Шаг 1. Выберите станцию впереди"
    const val STEP2 = "Шаг 2. Время прибытия"
    const val NO_STOPS = "Впереди нет станций плеча. Цель можно выбрать, когда положение определено и вы на пути маршрута."
    const val ON_STOP = "На остановку"
    const val STOP_MIN_LABEL = "Время торможения до остановки, мин"
    const val SHOW_ON_MAP = "Показывать цель на экране Карты"
    const val DONE = "Готово"
    const val CHANGE = "Сменить цель"
    const val CANCEL_TARGET = "Отменить цель"
}

/**
 * Карточка расчёта цели на карте: результат по [plan] и единственная кнопка «Скрыть» (цель снимается только на экране «Цель»).
 * [distanceM] - расстояние вдоль линии до цели (null - сейчас не определить, [note] объясняет причину).
 */
@Composable
internal fun ArrivalPlanCard(
    plan: ArrivalPlanData,
    distanceM: Double?,
    note: String?,
    nowMs: Long,
    zone: String,
    speedKmh: Double?,
    onHide: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        ArrivalPlanBody(plan, distanceM, note, nowMs, zone, speedKmh)
        PstButton(TrainMapPlanTexts.HIDE, onClick = onHide, kind = PstButtonKind.TEXT, height = 40.dp, fontSize = 14)
    }
}

/** Заголовок цели, расстояние и результат расчёта (общая часть карточки карты и экрана «Цель»). */
@Composable
internal fun ArrivalPlanBody(
    plan: ArrivalPlanData,
    distanceM: Double?,
    note: String?,
    nowMs: Long,
    zone: String,
    speedKmh: Double?,
) {
    val target = plan.target
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
        } else if (distanceM != null) {
            PlanResult(plan, distanceM, nowMs, zone, speedKmh)
        }
    }
}

@Composable
internal fun PlanResult(plan: ArrivalPlanData, distanceM: Double, nowMs: Long, zone: String, speedKmh: Double?) {
    val ev = ArrivalPlan.evaluate(nowMs, distanceM, plan.plannedMs, speedKmh, if (plan.onStop) plan.stopMin else 0)
    val dayOff = ArrivalPlan.dayOffset(nowMs, plan.plannedMs, zone)
    val plannedText = TrackAnalysis.clock(plan.plannedMs, zone) + if (dayOff > 0) " (следующие сутки)" else ""
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PlanLine("Плановое прибытие", plannedText, strong = false)
        if (plan.onStop) PlanLine("Торможение до остановки", "${plan.stopMin} мин", strong = false)
        if (ev.requiredKmh != null) {
            PlanLine("Нужная скорость хода", speedText(ev.requiredKmh!!), strong = true)
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
            ArrivalPlan.Status.IMPOSSIBLE ->
                Text(TrainMapPlanTexts.IMPOSSIBLE, style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.danger)
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
