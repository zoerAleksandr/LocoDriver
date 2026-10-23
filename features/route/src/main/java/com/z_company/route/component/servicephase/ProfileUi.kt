package com.z_company.route.component.servicephase

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import com.z_company.domain.track.Calibration
import com.z_company.domain.track.KmGrid
import com.z_company.domain.track.ProfileWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.z_company.domain.track.KmMarks
import com.z_company.domain.track.LiveProfile
import com.z_company.domain.track.ProfileElement
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/*
 * Профиль пути на карте поезда: полоса внизу карты и полноэкранный экран (SCREEN_SPECS §6.9, «Профиль пути»).
 * График — продольный профиль выбранного участка в направлении движения, маркер — живое положение поезда.
 */

/** Окно мини-профиля в полосе: позади 1 км, впереди 5 км. */
private const val STRIP_BEHIND_KM = 1.0
private const val STRIP_AHEAD_KM = 5.0

/** Запас позади хвоста поезда: окно профиля всегда включает поезд целиком. */
private const val TRAIN_MARGIN_KM = 0.15

private fun fmt1(v: Double): String = ((v * 10).roundToInt() / 10.0).toString().replace('.', ',')

/** «↑ 4,0 ‰» (подъём в направлении движения), «↓ 2,5 ‰», «площадка». */
internal fun gradeText(live: LiveProfile, e: ProfileElement?): String? {
    e ?: return null
    if (e.noData) return "нет данных"
    val g = live.gradeInView(e)
    return when {
        abs(g) < 0.05 -> "площадка"
        g > 0 -> "↑ ${fmt1(g)} ‰"
        else -> "↓ ${fmt1(-g)} ‰"
    }
}

private fun emptyText(model: ProfileModel): String = when (model.emptyReason) {
    ProfileEmpty.LOADING -> "Загружаем профили участков…"
    ProfileEmpty.NO_LOCATION -> "Определяем местоположение…"
    ProfileEmpty.NO_SECTIONS -> "Профили участков не загружены. Проверьте интернет — после загрузки профиль появится."
    ProfileEmpty.NO_NEARBY -> "Для этого места профиль пока не собран. Данные профиля собираются — скоро он появится."
    ProfileEmpty.NO_DATA -> "Для участка «${model.section?.name.orEmpty()}» данные профиля собираются. Скоро профиль появится."
    null -> ""
}

/**
 * Полоса «Профиль пути»: слева над графиком километр и уклон, мини-профиль впереди по ходу; справа кнопки по вертикали:
 * «Развернуть на весь экран» вверху, «Скрыть» внизу. Нет данных - пояснение вместо графика.
 */
@Composable
internal fun ProfileStrip(
    model: ProfileModel,
    height: Dp,
    onHide: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(Pst.surface)
            .padding(start = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val live = model.live
        val reason = model.emptyReason
        Column(Modifier.weight(1f).fillMaxHeight().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (reason == null && live != null) {
                val pos = model.position
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (pos?.offRoute == true) "вне участка" else pos?.kmText ?: "ПРОФИЛЬ ПУТИ",
                        style = Pst.mono.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = if (pos?.offRoute == true) Pst.danger else Pst.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    gradeText(live, pos?.element)?.let {
                        Text(it, style = Pst.mono.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = Pst.text, maxLines = 1)
                    }
                }
                ProfileChart(model, live.window(max(STRIP_BEHIND_KM, (model.trainLengthKm ?: 0.0) + TRAIN_MARGIN_KM), STRIP_AHEAD_KM), detailed = false, modifier = Modifier.weight(1f).fillMaxWidth().clickable(onClickLabel = "Профиль пути на весь экран", onClick = onExpand))
            } else {
                Text("ПРОФИЛЬ ПУТИ", style = Pst.mono.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = Pst.textMuted)
                Text(emptyText(model), style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 15.sp), color = Pst.textMuted, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        StripButtons("Профиль пути на весь экран", onExpand, "Скрыть профиль пути", onHide, height)
    }
}

/** Минимальный и максимальный размер окна профиля при зуме, км. */
private const val MIN_SPAN_KM = 0.3

/** Жест на графике: [zoom] - множитель за шаг, [panFrac] - сдвиг пальцев в долях ширины, [centerFrac] - положение центра жеста. */
internal typealias ProfileTransform = (zoom: Float, panFrac: Float, centerFrac: Float) -> Unit

/**
 * График профиля в окне [window] (координаты вида, км). При [detailed] под ним — как на режимной карте: ряд уклонов
 * (клетка на элемент: диагональ вверх/вниз, уклон ‰ сверху, длина в метрах снизу, площадка — «0»), ряд кривых (только
 * если в данных есть радиусы) и линейка км и пикетов; пунктиром — столбы км. [onTransform] != null — щипок и сдвиг.
 */
@Composable
internal fun ProfileChart(
    model: ProfileModel,
    window: ProfileWindow,
    detailed: Boolean,
    modifier: Modifier = Modifier,
    onTransform: ProfileTransform? = null,
) {
    val live = model.live
    val line = model.elevation()
    if (live == null || line == null || line.points.size < 2) return
    val pos = model.position
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current.density
    val ink = Pst.text
    val muted = Pst.textMuted
    val grid = Pst.border
    val accent = Pst.accent
    val labelStyle: TextStyle = Pst.mono.copy(fontSize = 10.sp, color = muted)
    val cellStyle: TextStyle = Pst.mono.copy(fontSize = 10.sp, color = ink)
    val kmStyle: TextStyle = Pst.mono.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ink)
    val stationStyle: TextStyle = Pst.sans.copy(fontSize = 10.sp, color = muted)
    val noDataStyle: TextStyle = Pst.sans.copy(fontSize = if (detailed) 12.sp else 10.sp, color = ink.copy(alpha = 0.7f))
    val section = live.section
    val marks = section.kmMarks
    val hasCurves = remember(section) { section.elements.any { it.curveRadiusM > 0 } }
    // Лямбда меняется при каждой перерисовке: ключом жеста берём только «жесты включены», иначе жест рвётся посреди движения.
    val transform by rememberUpdatedState(onTransform)
    val gesture = if (onTransform != null) {
        Modifier.pointerInput(Unit) {
            detectTransformGestures { centroid, pan, zoom, _ ->
                transform?.invoke(zoom, pan.x / size.width, centroid.x / size.width)
            }
        }
    } else Modifier
    Canvas(modifier.then(gesture)) {
        val w = size.width
        val h = size.height
        val dp = density
        val topPad = if (detailed) 14f * dp else 4f * dp
        val gradeH = if (detailed) 44f * dp else 0f
        val curveH = if (detailed && hasCurves) 24f * dp else 0f
        val rulerH = if (detailed && marks.isNotEmpty()) 34f * dp else if (detailed) 16f * dp else 0f
        val plotH = h - topPad - gradeH - curveH - rulerH - 2f * dp
        if (w <= 20f || plotH <= 10f) return@Canvas
        val span = (window.toX - window.fromX).coerceAtLeast(1e-6)
        fun px(x: Double) = ((x - window.fromX) / span * w).toFloat()
        val plotBottom = topPad + plotH
        val n = (w / (3f * dp)).toInt().coerceIn(20, 500)
        val xs = DoubleArray(n + 1) { window.fromX + span * it / n }
        val hs = DoubleArray(n + 1) { line.heightAt(live.toAxis(xs[it])) }
        val lo = hs.min()
        val hi = hs.max()
        val range = max(hi - lo, 20.0)
        val mid = (hi + lo) / 2
        val yMin = mid - range * 0.6
        val yMax = mid + range * 0.6
        fun py(v: Double) = topPad + plotH - ((v - yMin) / (yMax - yMin) * plotH).toFloat()

        // Видимая часть оси (для сетки км в осевых координатах).
        val axA = minOf(live.toAxis(window.fromX), live.toAxis(window.toX))
        val axB = maxOf(live.toAxis(window.fromX), live.toAxis(window.toX))
        val cells = if (detailed && marks.isNotEmpty()) KmGrid.cells(marks, axA, axB) else emptyList()
        val ticks = if (detailed && marks.isNotEmpty()) KmGrid.ticks(marks, axA - 0.1, axB + 0.1) else emptyList()
        val pxPerPicket = w / span.toFloat() * 0.1f

        drawLine(grid, Offset(0f, topPad), Offset(w, topPad), strokeWidth = 1f)
        drawLine(grid, Offset(0f, plotBottom), Offset(w, plotBottom), strokeWidth = 1f)

        val dash = PathEffect.dashPathEffect(floatArrayOf(5f * dp, 5f * dp))
        val bottomOfRows = plotBottom + gradeH + curveH

        val fill = Path().apply {
            moveTo(px(xs[0]), plotBottom)
            for (i in 0..n) lineTo(px(xs[i]), py(hs[i]))
            lineTo(px(xs[n]), plotBottom)
            close()
        }
        drawPath(fill, accent.copy(alpha = 0.16f))
        val stroke = Path().apply {
            moveTo(px(xs[0]), py(hs[0]))
            for (i in 1..n) lineTo(px(xs[i]), py(hs[i]))
        }
        drawPath(stroke, ink, style = Stroke(width = 2f * dp, cap = StrokeCap.Round))

        // Зоны «нет данных»: серая полоса во всю высоту графика и подпись (профиль на этом месте неизвестен, данные собираются).
        val noDataColor = muted.copy(alpha = 0.22f)
        section.elements.filter { it.noData }.forEach { e ->
            val v0 = live.toView(if (live.reversed) e.endKm else e.startKm)
            val v1 = live.toView(if (live.reversed) e.startKm else e.endKm)
            if (v1 < window.fromX || v0 > window.toX) return@forEach
            val x0 = px(v0).coerceIn(0f, w)
            val x1 = px(v1).coerceIn(0f, w)
            if (x1 - x0 < 1f) return@forEach
            drawRect(noDataColor, Offset(x0, topPad), androidx.compose.ui.geometry.Size(x1 - x0, plotH))
            val caption = measurer.measure(
                if (detailed) "Данные профиля собираются, скоро он появится" else "Данные собираются",
                noDataStyle,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = (x1 - x0 - 8f * dp).toInt().coerceAtLeast(0)),
            )
            if (x1 - x0 > 60f * dp) drawText(caption, topLeft = Offset(x0 + 4f * dp, topPad + 4f * dp + (if (detailed) 14f * dp else 0f)))
        }

        // Границы км: от линейки вверх до линии профиля (поверх заливки, чтобы были видны).
        if (detailed) {
            ticks.filter { it.isPost }.forEach { t ->
                val xx = px(live.toView(t.axisKm))
                if (xx in 0f..w) {
                    val yTop = py(line.heightAt(t.axisKm))
                    drawLine(muted.copy(alpha = 0.7f), Offset(xx, yTop), Offset(xx, bottomOfRows), strokeWidth = 1f * dp, pathEffect = dash)
                }
            }
        }

        if (detailed) {
            // Станции: подпись сверху, без наложений.
            var lastEnd = -1f
            section.stations.map { live.toView(it.km) to it.name }
                .filter { it.first in window.fromX..window.toX }
                .sortedBy { it.first }
                .forEach { (x, name) ->
                    val xx = px(x)
                    drawLine(muted.copy(alpha = 0.5f), Offset(xx, topPad), Offset(xx, plotBottom), strokeWidth = 1f, pathEffect = dash)
                    val t = measurer.measure(name, stationStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val tx = (xx + 3f * dp).coerceAtMost(w - t.size.width).coerceAtLeast(0f)
                    if (tx > lastEnd) {
                        drawText(t, topLeft = Offset(tx, 0f))
                        lastEnd = tx + t.size.width + 4f * dp
                    }
                }

            // Ряд уклонов: клетка на элемент; диагональ - подъём/спуск, сверху ‰, снизу длина в метрах.
            val rowTop = plotBottom
            val rowBottom = plotBottom + gradeH
            drawLine(grid, Offset(0f, rowBottom), Offset(w, rowBottom), strokeWidth = 1f)
            val curPos = pos?.takeIf { !it.offRoute }?.kmAxis
            // Линия ряда тоньше и светлее линии профиля; между соседними элементами - вертикальные соединения.
            val rowInk = ink.copy(alpha = 0.55f)
            val lineW = 1f * dp
            val y0 = rowTop + 15f * dp
            val y1 = rowBottom - 13f * dp
            val yMid = (y0 + y1) / 2
            data class Cell(val e: ProfileElement, val v0: Double, val v1: Double, val g: Double, val yStart: Float, val yEnd: Float)
            val visible = section.elements.mapNotNull { e ->
                val v0 = live.toView(if (live.reversed) e.endKm else e.startKm)
                val v1 = live.toView(if (live.reversed) e.startKm else e.endKm)
                if (v1 < window.fromX || v0 > window.toX) return@mapNotNull null
                val g = live.gradeInView(e)
                val (ys, ye) = when {
                    g > 0.05 -> y1 to y0
                    g < -0.05 -> y0 to y1
                    else -> yMid to yMid
                }
                Cell(e, v0, v1, g, ys, ye)
            }.sortedBy { it.v0 }
            var prev: Cell? = null
            visible.forEach { c ->
                val x0 = px(c.v0).coerceIn(-1f, w + 1f)
                val x1 = px(c.v1).coerceIn(-1f, w + 1f)
                val cw = x1 - x0
                if (cw < 1.5f * dp) return@forEach
                if (curPos != null && curPos >= c.e.startKm && curPos < c.e.endKm) {
                    drawRect(accent.copy(alpha = 0.14f), Offset(x0, rowTop), androidx.compose.ui.geometry.Size(cw, gradeH))
                }
                drawLine(grid, Offset(x0, rowTop), Offset(x0, rowBottom), strokeWidth = 1f)
                if (c.e.noData) {
                    drawRect(noDataColor, Offset(x0, rowTop), androidx.compose.ui.geometry.Size(cw, gradeH))
                    prev = c
                    return@forEach
                }
                drawLine(rowInk, Offset(x0, c.yStart), Offset(x1, c.yEnd), strokeWidth = lineW)
                prev?.let { p ->
                    // соединение с предыдущим элементом, если они стыкуются и уровни различаются
                    if (abs(p.v1 - c.v0) < 1e-6 && abs(p.yEnd - c.yStart) > 0.5f && x0 in 0f..w) {
                        drawLine(rowInk, Offset(x0, p.yEnd), Offset(x0, c.yStart), strokeWidth = lineW)
                    }
                }
                prev = c
                val gv = abs(c.g)
                val gText = if (gv < 0.05) "0" else fmt1(gv)
                val tg = measurer.measure(gText, cellStyle)
                if (cw >= tg.size.width + 4f * dp) {
                    drawText(tg, topLeft = Offset(((x0 + x1) / 2 - tg.size.width / 2f).coerceIn(x0 + 1f, max(x0 + 1f, x1 - tg.size.width - 1f)), rowTop + 1f * dp))
                }
                val lenM = ((c.e.endKm - c.e.startKm) * 1000).roundToInt()
                val tl = measurer.measure(lenM.toString(), labelStyle)
                if (cw >= tl.size.width + 4f * dp) {
                    drawText(tl, topLeft = Offset((x0 + x1) / 2 - tl.size.width / 2f, rowBottom - tl.size.height - 1f * dp))
                }
            }

            // Ряд кривых: только если радиусы есть в данных.
            if (hasCurves) {
                val cTop = rowBottom
                val cMid = cTop + curveH * 0.7f
                val cHigh = cTop + curveH * 0.25f
                drawLine(grid, Offset(0f, cMid), Offset(w, cMid), strokeWidth = 1f)
                section.elements.filter { it.curveRadiusM > 0 }.forEach { e ->
                    val v0 = live.toView(if (live.reversed) e.endKm else e.startKm)
                    val v1 = live.toView(if (live.reversed) e.startKm else e.endKm)
                    if (v1 < window.fromX || v0 > window.toX) return@forEach
                    val x0 = px(v0).coerceIn(0f, w)
                    val x1 = px(v1).coerceIn(0f, w)
                    drawLine(ink, Offset(x0, cMid), Offset(x0, cHigh), strokeWidth = 1.4f * dp)
                    drawLine(ink, Offset(x0, cHigh), Offset(x1, cHigh), strokeWidth = 1.4f * dp)
                    drawLine(ink, Offset(x1, cHigh), Offset(x1, cMid), strokeWidth = 1.4f * dp)
                    val tr = measurer.measure("R" + e.curveRadiusM.roundToInt(), labelStyle)
                    if (x1 - x0 >= tr.size.width + 2f * dp) drawText(tr, topLeft = Offset((x0 + x1) / 2 - tr.size.width / 2f, cMid + 1f * dp))
                }
            }

            // Линейка: номер км по ячейкам и пикеты 1..10 с границами.
            val ry = bottomOfRows
            drawLine(grid, Offset(0f, ry), Offset(w, ry), strokeWidth = 1f)
            if (marks.isNotEmpty()) {
                var runStart = 0
                while (runStart < cells.size) {
                    var runEnd = runStart
                    while (runEnd + 1 < cells.size && cells[runEnd + 1].km == cells[runStart].km) runEnd++
                    val vA = live.toView(cells[runStart].fromAxisKm)
                    val vB = live.toView(cells[runEnd].toAxisKm)
                    val xA = px(minOf(vA, vB)).coerceIn(0f, w)
                    val xB = px(maxOf(vA, vB)).coerceIn(0f, w)
                    val tk = measurer.measure(cells[runStart].km.toString(), kmStyle)
                    if (xB - xA >= tk.size.width + 4f * dp) drawText(tk, topLeft = Offset((xA + xB) / 2 - tk.size.width / 2f, ry + 1f * dp))
                    runStart = runEnd + 1
                }
                val pkY = ry + 17f * dp
                ticks.forEach { t ->
                    val xx = px(live.toView(t.axisKm))
                    if (xx in 0f..w) drawLine(muted, Offset(xx, pkY), Offset(xx, if (t.isPost) ry + rulerH else pkY + 6f * dp), strokeWidth = if (t.isPost) 1.5f else 1f)
                }
                if (pxPerPicket >= 15f * dp) {
                    cells.forEach { c ->
                        val vA = live.toView(c.fromAxisKm)
                        val vB = live.toView(c.toAxisKm)
                        val cx = (px(vA) + px(vB)) / 2
                        val t = measurer.measure(c.picket.toString(), labelStyle)
                        if (cx - t.size.width / 2f >= 0f && cx + t.size.width / 2f <= w) drawText(t, topLeft = Offset(cx - t.size.width / 2f, pkY + 5f * dp))
                    }
                }
            }
        }

        // Линия поезда: от головы (положение) назад по ходу на длину поезда, вдоль линии профиля.
        val trainKm = model.trainLengthKm
        if (pos != null && trainKm != null && !pos.offRoute) {
            val head = live.toView(pos.kmAxis)
            val tail = (head - trainKm).coerceAtLeast(0.0)
            val a = max(tail, window.fromX)
            val b = minOf(head, window.toX)
            if (b > a) {
                val steps = max(2, ((b - a) / span * w / (2f * dp)).toInt())
                val trainPath = Path().apply {
                    for (i in 0..steps) {
                        val x = a + (b - a) * i / steps
                        val xx = px(x)
                        val yy = py(line.heightAt(live.toAxis(x)))
                        if (i == 0) moveTo(xx, yy) else lineTo(xx, yy)
                    }
                }
                val trainColor = if (pos.locked) accent else muted
                drawPath(trainPath, trainColor, style = Stroke(width = (if (detailed) 6f else 5f) * dp, cap = StrokeCap.Round))
                // Хвост: вертикаль от линии профиля вниз до километровой разметки (вверх линию не продолжаем).
                if (tail >= window.fromX) {
                    val tx = px(tail)
                    drawLine(trainColor, Offset(tx, py(line.heightAt(live.toAxis(tail)))), Offset(tx, bottomOfRows), strokeWidth = 2f * dp)
                }
            }
        }

        // Живой маркер.
        if (pos != null) {
            val vx = live.toView(pos.kmAxis)
            if (vx in window.fromX..window.toX) {
                val xx = px(vx)
                val yy = py(line.heightAt(pos.kmAxis))
                val c = if (pos.offRoute || !pos.locked) muted else accent
                drawLine(c, Offset(xx, yy), Offset(xx, bottomOfRows), strokeWidth = 2f * dp)
                drawCircle(c, radius = 5f * dp, center = Offset(xx, yy))
            }
        }
    }
}

/** Окно показа полноэкранного профиля в км (пресеты); [ALL] - весь участок. */
internal enum class ProfileScale(val label: String, val spanKm: Double) {
    S3("3 км", 3.0),
    S6("6 км", 6.0),
    S12("12 км", 12.0),
    ALL("Весь участок", 0.0),
}

/**
 * Экран «Профиль пути» на весь экран. Только горизонтально, как график скорости: на время показа активность переводится
 * в landscape, по выходу возвращается прежняя ориентация (пересоздания нет, configChanges в манифесте).
 * Сверху одна строка: назад, участок (нажатие - список), масштаб, «К поезду» (после сдвига), смена направления.
 * График масштабируется щипком и сдвигается пальцем; пока окно двигали вручную, оно не следует за поездом.
 */
@Composable
internal fun ProfileFullScreen(model: ProfileModel, onDismiss: () -> Unit) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var spanKm by rememberSaveable { mutableStateOf(6f) }
    var follow by rememberSaveable { mutableStateOf(true) }
    var manualFrom by rememberSaveable { mutableStateOf(0f) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val activity = LocalContext.current.findActivity()
        DisposableEffect(activity) {
            val previous = activity?.requestedOrientation
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            onDispose { previous?.let { activity.requestedOrientation = it } }
        }
        val live = model.live
        val reason = model.emptyReason
        Column(modifier = Modifier.fillMaxSize().background(Pst.bg).statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapButton(onClick = onDismiss, description = "Назад") { PstIcon(PstIcons.ChevronLeft, 22.dp, Pst.text) }
                SectionHeader(model, Modifier.weight(1f)) { showPicker = true }
                if (reason == null && live != null) {
                    val length = live.lengthKm
                    ProfileScale.values().forEach { sc ->
                        val target = if (sc == ProfileScale.ALL) length.toFloat() else sc.spanKm.toFloat()
                        val on = abs(spanKm - target) < 0.05f * target.coerceAtLeast(1f)
                        Text(
                            text = sc.label,
                            style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                            color = if (on) Pst.onAccent else Pst.text,
                            modifier = Modifier
                                .background(if (on) Pst.accent else Pst.bgSubtle, RoundedCornerShape(12.dp))
                                .clickable { spanKm = target; follow = sc != ProfileScale.ALL }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            maxLines = 1,
                        )
                    }
                    if (!follow) {
                        PstButton(
                            text = "К поезду", onClick = { follow = true },
                            kind = PstButtonKind.TONAL, height = 36.dp, fontSize = 13,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                        )
                    }
                    StripButton("Сменить направление", { model.toggleDirection() }) { PstIcon(PstIcons.Swap, 20.dp, Pst.text) }
                }
            }
            model.chooseError?.let {
                Text(
                    text = it,
                    style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 15.sp),
                    color = Pst.danger,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                )
            }
            if (model.fitsLeg == false) {
                Text(
                    text = "Этот участок не совпадает с плечом поезда. Выберите другой или оставьте, если едете не по плечу.",
                    style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 15.sp),
                    color = Pst.text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp).fillMaxWidth()
                        .background(Pst.warning, RoundedCornerShape(12.dp)).padding(8.dp),
                )
            }
            if (reason == null && live != null) {
                val length = live.lengthKm
                val span = spanKm.toDouble().coerceIn(MIN_SPAN_KM.coerceAtMost(length), length)
                val window = if (follow) {
                    if (span >= length) {
                        ProfileWindow(0.0, length)
                    } else {
                        // Позади головы - не меньше длины поезда с запасом: поезд виден целиком при любом масштабе.
                        val behind = max(span / 6, (model.trainLengthKm ?: 0.0) + TRAIN_MARGIN_KM).coerceAtMost(span * 0.8)
                        live.window(behind, span - behind)
                    }
                } else {
                    val from = manualFrom.toDouble().coerceIn(0.0, (length - span).coerceAtLeast(0.0))
                    ProfileWindow(from, from + span)
                }
                // Жест: масштаб вокруг точки между пальцами, сдвиг пальцем. Чистый зум не отключает слежение за поездом.
                val onTransform: ProfileTransform = { zoom, panFrac, cFrac ->
                    val newSpan = (span / zoom).coerceIn(MIN_SPAN_KM.coerceAtMost(length), length)
                    val movedPan = abs(panFrac) > 0.002f
                    if (movedPan || !follow) {
                        val anchor = window.fromX + cFrac * span
                        val from = anchor - cFrac * newSpan - panFrac * newSpan
                        manualFrom = from.coerceIn(0.0, (length - newSpan).coerceAtLeast(0.0)).toFloat()
                        follow = false
                    }
                    spanKm = newSpan.toFloat()
                }
                ProfileChart(
                    model, window, detailed = true,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    onTransform = onTransform,
                )
                NowRow(model, live)
            } else {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = emptyText(model),
                        style = Pst.sans.copy(fontSize = 15.sp, lineHeight = 21.sp),
                        color = Pst.textMuted,
                        textAlign = TextAlign.Center,
                    )
                    if (model.summaries.isNotEmpty()) {
                        PstButton(text = "Выбрать участок", onClick = { showPicker = true }, kind = PstButtonKind.TONAL)
                    }
                }
            }
        }
    }
    if (showPicker) SectionPickerDialog(model, onDismiss = { showPicker = false })
}

/** Участок и направление одной карточкой (нажатие - список участков): название, «от А к Б», способ выбора. */
@Composable
private fun SectionHeader(model: ProfileModel, modifier: Modifier = Modifier, onPick: () -> Unit) {
    val live = model.live
    val s = live?.section
    Row(
        modifier = modifier
            .background(Pst.surface, RoundedCornerShape(14.dp))
            .clickable(onClickLabel = "Выбрать участок", onClick = onPick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = s?.name ?: "Выбрать участок",
                style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = Pst.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (s != null && live != null) {
                val from = if (live.reversed) s.toStation else s.fromStation
                val to = if (live.reversed) s.fromStation else s.toStation
                Text(
                    text = "$from → $to · " + if (model.manualId != null) "выбран вручную" else "по местоположению",
                    style = Pst.sans.copy(fontSize = 11.sp), color = Pst.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text("Сменить", style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = Pst.accent, maxLines = 1)
    }
}

/**
 * Строка «Сейчас» в одну линию: километр и пикет (нажатие или «Уточнить» - ручная привязка), уклон, ближайшая станция
 * впереди и поправка, если она задана; вне участка - предупреждение.
 */
@Composable
private fun NowRow(model: ProfileModel, live: LiveProfile) {
    val pos = model.position
    var showCalibrate by rememberSaveable { mutableStateOf(false) }
    var showTrains by rememberSaveable { mutableStateOf(false) }
    val ahead = pos?.let { p ->
        val vx = live.toView(p.kmAxis)
        live.section.stations.map { live.toView(it.km) to it.name }.filter { it.first > vx + 0.05 }.minByOrNull { it.first }
            ?.let { (x, name) -> name to (x - vx) }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            .background(Pst.surface, RoundedCornerShape(14.dp)).padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                pos == null -> Text("Положение на участке определяется…", style = Pst.sans.copy(fontSize = 13.sp), color = Pst.textMuted)
                pos.offRoute -> Text(
                    "Вы вне этого участка. Выберите другой или дождитесь автоподбора.",
                    style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = Pst.danger, maxLines = 2,
                )
                else -> {
                    Text(
                        text = pos.kmText ?: "${fmt1(live.toView(pos.kmAxis))} км от начала",
                        style = Pst.mono.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Pst.text, maxLines = 1,
                    )
                    gradeText(live, pos.element)?.let {
                        Text(it, style = Pst.mono.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Pst.text, maxLines = 1)
                    }
                    val tail = buildList {
                        ahead?.let { add("До «${it.first}» — ${pstKm(it.second)}") }
                        if (abs(model.calibrationKm) > 1e-6) add("поправка ${if (model.calibrationKm > 0) "+" else "−"}${(abs(model.calibrationKm) * 1000).roundToInt()} м")
                        if (ahead == null && pos.kmText == null) add("Нет километровой привязки — показано расстояние от начала.")
                    }.joinToString(" · ")
                    Text(tail, style = Pst.sans.copy(fontSize = 13.sp), color = Pst.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }
        }
        model.train?.let { tr ->
            Text(
                text = trainLabel(tr),
                style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = Pst.text,
                maxLines = 1,
                modifier = Modifier
                    .background(Pst.bgSubtle, RoundedCornerShape(12.dp))
                    .clickable(onClickLabel = "Выбрать поезд") { showTrains = true }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
        if (live.section.kmMarks.isNotEmpty()) {
            PstButton(
                text = "Уточнить км", onClick = { showCalibrate = true },
                kind = PstButtonKind.TONAL, height = 34.dp, fontSize = 13,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            )
        }
    }
    if (showCalibrate) CalibrateDialog(model, live, onDismiss = { showCalibrate = false })
    if (showTrains) TrainPickerDialog(model, onDismiss = { showTrains = false })
}

/** «№2552 · 340 м»; без длины - «№2552 · длина не указана». */
private fun trainLabel(t: ProfileTrain): String {
    val num = t.number?.takeIf { it.isNotBlank() }?.let { "№$it" } ?: "Поезд без номера"
    return num + " · " + (t.lengthM?.takeIf { it > 0 }?.let { "$it м" } ?: "длина не указана")
}

/** Выбор поезда маршрута: по его длине (условная длина поезда) рисуется линия поезда на профиле. */
@Composable
private fun TrainPickerDialog(model: ProfileModel, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier.fillMaxWidth(0.6f).background(Pst.bg, RoundedCornerShape(20.dp)).padding(vertical = 16.dp),
        ) {
            Text("Поезд на профиле", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            Text(
                "Длина линии поезда — по условной длине из данных поезда.",
                style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Pst.textMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            LazyColumn(Modifier.heightIn(max = 240.dp)) {
                items(model.trains, key = { it.id }) { t ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { model.chooseTrain(t.id); onDismiss() }.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(trainLabel(t), style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Pst.text, modifier = Modifier.weight(1f))
                        if (t.id == model.train?.id) PstIcon(PstIcons.Check, 20.dp, Pst.accent)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Pst.border))
                }
            }
            PstButton("Закрыть", onDismiss, kind = PstButtonKind.TEXT, height = 40.dp, fontSize = 14, modifier = Modifier.align(Alignment.End).padding(horizontal = 12.dp))
        }
    }
}

/**
 * Ручная привязка: «я на 7046 км, пк 4». GPS остаётся основой, вводом считается поправка по оси для этого участка (карта могла
 * быть нарисована со сдвигом или километраж не совпадает со столбами); новая привязка заменяет прежнюю. Если введённое место
 * далеко (> 2 км) от показываемого - просим подтвердить повторным нажатием.
 */
@Composable
private fun CalibrateDialog(model: ProfileModel, live: LiveProfile, onDismiss: () -> Unit) {
    var km by remember { mutableStateOf(TextFieldValue("")) }
    var pk by remember { mutableStateOf(TextFieldValue("")) }
    var plus by remember { mutableStateOf(TextFieldValue("")) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmFar by remember { mutableStateOf(false) }
    val numeric = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
    fun submit() {
        val kmV = km.text.trim().toIntOrNull()
        val pkV = pk.text.trim().ifEmpty { "1" }.toIntOrNull()
        val plusV = plus.text.trim().ifEmpty { "0" }.toIntOrNull()
        when {
            kmV == null || kmV < 0 -> { error = "Введите номер километра."; return }
            pkV == null || pkV !in 1..10 -> { error = "Пикет — число от 1 до 10."; return }
            plusV == null || plusV !in 0..99 -> { error = "«+ м» — число от 0 до 99."; return }
        }
        val abs = kmV!! + (pkV!! - 1) / 10.0 + plusV!! / 1000.0
        when (val c = model.calibrationFor(abs)) {
            Calibration.NoPosition -> error = "Положение по GPS пока не определено."
            Calibration.NotOnSection -> error = "Такого километра нет на этом участке."
            is Calibration.Ok -> {
                if (c.diffKm > FAR_KM && !confirmFar) {
                    error = "Это в ${pstKm(c.diffKm)} от места, которое показывает GPS. Нажмите «Привязать» ещё раз, если уверены."
                    confirmFar = true
                } else {
                    model.applyCalibration(c.shiftKm)
                    onDismiss()
                }
            }
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier.fillMaxWidth(0.7f).background(Pst.bg, RoundedCornerShape(20.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Уточнить километр", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
            Text(
                text = "Сейчас: ${model.position?.kmText ?: "положение определяется"}. Введите, где вы на самом деле, — приложение сдвинет положение на профиле для этого участка.",
                style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 17.sp), color = Pst.textMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PstTextField(km, { km = it; error = null; confirmFar = false }, "Км", Modifier.weight(1f), keyboardOptions = numeric, mono = true)
                PstTextField(pk, { pk = it; error = null; confirmFar = false }, "Пикет", Modifier.weight(1f), placeholder = "1", keyboardOptions = numeric, mono = true)
                PstTextField(plus, { plus = it; error = null; confirmFar = false }, "+ м", Modifier.weight(1f), placeholder = "0", keyboardOptions = numeric, mono = true)
            }
            error?.let { Text(it, style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 17.sp), color = if (confirmFar) Pst.text else Pst.danger) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
                if (abs(model.calibrationKm) > 1e-6) {
                    PstButton("Сбросить поправку", { model.resetCalibration(); onDismiss() }, kind = PstButtonKind.TEXT, height = 40.dp, fontSize = 14)
                }
                PstButton("Отмена", onDismiss, kind = PstButtonKind.TEXT, height = 40.dp, fontSize = 14)
                PstButton("Привязать", { submit() }, kind = PstButtonKind.FILLED, height = 40.dp, fontSize = 14)
            }
        }
    }
}

/** Дальше этого расстояния от показываемого места привязку просим подтвердить, км. */
private const val FAR_KM = 2.0

/** Список участков: ближайшие по положению сверху (плечо поезда — выше всех), поиск по названию и станциям. */
@Composable
private fun SectionPickerDialog(model: ProfileModel, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val rows = remember(query.text, model.summaries, model.sections, model.manualId) { model.rows(query.text) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(modifier = Modifier.fillMaxSize().background(Pst.bg).statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapButton(onClick = onDismiss, description = "Назад") { PstIcon(PstIcons.ChevronLeft, 22.dp, Pst.text) }
                Text("Участок профиля", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
            }
            PstTextField(
                value = query,
                onValueChange = { query = it },
                label = "Поиск",
                placeholder = "Название или станция",
                leadingIcon = PstIcons.Search,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
            if (model.manualId != null) {
                Text(
                    text = "Подбирать по местоположению",
                    style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    color = Pst.accent,
                    modifier = Modifier.fillMaxWidth().clickable { model.useAuto(); onDismiss() }.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            if (rows.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (model.summaries.isEmpty()) "Участки не загружены. Проверьте интернет." else "Ничего не найдено. Данные профилей собираются — список пополняется.",
                        style = Pst.sans.copy(fontSize = 14.sp),
                        color = Pst.textMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(rows, key = { it.summary.id }) { row ->
                        val s = row.summary
                        val chosen = s.id == model.section?.id
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { model.choose(s.id); onDismiss() }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(s.name, style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Pst.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                val tags = buildList {
                                    add(pstKm(s.lengthKm))
                                    row.distanceM?.let { add(if (it < 300) "рядом с вами" else "в ${pstKm(it / 1000.0)} от вас") }
                                    if (row.fitsLeg == true) add("совпадает с плечом")
                                    if (!s.hasProfile) add("профиль собирается")
                                }
                                Text(tags.joinToString(" · "), style = Pst.sans.copy(fontSize = 12.sp), color = Pst.textMuted, maxLines = 2)
                            }
                            if (chosen) PstIcon(PstIcons.Check, 20.dp, Pst.accent)
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Pst.border))
                    }
                }
            }
        }
    }
}
