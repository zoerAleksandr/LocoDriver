package com.z_company.route.component.servicephase

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
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
import com.z_company.domain.track.SignalTrack
import com.z_company.domain.track.TravelParity
import com.z_company.route.ui.OfflineSectionDownloadDialogs
import com.z_company.route.viewmodel.OfflineSectionsViewModel
import com.z_company.route.viewmodel.OfflineStatus
import org.koin.androidx.compose.koinViewModel
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

private const val DEVICE_REJECTED_TEXT = "Не удалось зарегистрировать защищённый ключ устройства, поэтому новые профили путей не загружаются. Ранее сохранённые участки доступны."
private const val RATE_LIMITED_TEXT = "Сервер временно ограничил загрузку профилей путей. Повторите через несколько минут."

private fun emptyText(model: ProfileModel): String = emptyBase(model)

private fun emptyBase(model: ProfileModel): String = when (model.emptyReason) {
    ProfileEmpty.LOADING -> "Загружаем профили участков…"
    ProfileEmpty.NO_LOCATION -> "Поиск спутников… Определяем местоположение."
    ProfileEmpty.NO_SECTIONS, ProfileEmpty.NO_NEARBY ->
        if (model.deviceRejected) DEVICE_REJECTED_TEXT
        else if (model.rateLimited) RATE_LIMITED_TEXT
        else if (model.emptyReason == ProfileEmpty.NO_SECTIONS) "Профили участков не загружены. Проверьте интернет — после загрузки профиль появится."
        else "Для этого места профиль пока не собран. Данные профиля собираются — скоро он появится."
    ProfileEmpty.NEED_LOGIN -> "Профиль пути доступен после входа в аккаунт. Войдите в приложение (Профиль) — участки загрузятся."
    ProfileEmpty.OFF_RAILS -> "Вы находитесь вне путей."
    ProfileEmpty.NO_DATA -> "Для участка «${model.section?.name.orEmpty()}» данные профиля собираются. Скоро профиль появится."
    null -> ""
}

/** Чипы веток за ближайшей развилкой («→ Колпино · 12 км») над лентой профиля; нажатие выбирает ветку. */
@Composable
internal fun BranchChipsRow(model: ProfileModel, modifier: Modifier = Modifier) {
    val chips = model.branchChips
    if (chips.isEmpty()) return
    Row(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { c ->
            val on = c.edgeId == model.selectedBranchId
            Text(
                "далее на ${c.title}",
                style = Pst.sans.copy(fontSize = 13.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal),
                color = if (on) Pst.onAccent else Pst.text,
                modifier = Modifier
                    .background(if (on) Pst.accent else Pst.bgSubtle, RoundedCornerShape(12.dp))
                    .clickable { model.selectBranch(c.edgeId) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                maxLines = 1,
            )
        }
    }
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
        Column(
            Modifier.weight(1f).fillMaxHeight().padding(vertical = 6.dp)
                .clickable(enabled = reason == null && live != null, onClickLabel = "Профиль пути на весь экран", onClick = onExpand),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (reason == null && live != null) {
                val pos = model.position
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (pos?.offRoute == true) (if (model.offRails == true) "вне путей" else "вне участка") else pos?.kmText ?: "ПРОФИЛЬ ПУТИ",
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
                ProfileChart(model, model.stripWindow(live, STRIP_BEHIND_KM + STRIP_AHEAD_KM), detailed = false, modifier = Modifier.weight(1f).fillMaxWidth())
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
    val warningStyle: TextStyle = Pst.sans.copy(fontSize = if (detailed) 11.sp else 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B1400))
    val stationStyle: TextStyle = Pst.sans.copy(fontSize = 10.sp, color = muted)
    val noDataStyle: TextStyle = Pst.sans.copy(fontSize = if (detailed) 12.sp else 10.sp, color = ink.copy(alpha = 0.7f))
    val section = live.section
    val marks = section.kmMarks
    // Светофоры нужного литера (по направлению и пути); сторона оси, в которую идёт поезд, - чтобы отличить «спиной».
    val signals = model.shownSignals
    val travelDir = model.travelAxisDir
    val evenColor = accent
    val oddColor = Pst.cta
    val onEven = Pst.onAccent
    val onOdd = Pst.ctaInk
    val crossingSign = androidx.compose.ui.graphics.ImageBitmap.imageResource(com.z_company.route.R.drawable.level_crossing_sign)
    val signalStyle: TextStyle = Pst.mono.copy(fontSize = if (detailed) 10.sp else 8.sp, fontWeight = FontWeight.Bold)
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
    // Нажатие на жёлтую зону предупреждения - редактирование.
    val currentWindow by rememberUpdatedState(window)
    val tapZones = Modifier.pointerInput(Unit) {
        detectTapGestures { o ->
            val w = currentWindow
            val x = w.fromX + o.x / size.width * (w.toX - w.fromX)
            model.activeWarnings.firstOrNull { r ->
                val a = minOf(live.toView(r.fromKm), live.toView(r.toKm))
                val b = max(live.toView(r.fromKm), live.toView(r.toKm))
                x in a..max(b, a + 0.03)
            }?.let { model.editWarning(it.warning.id) }
        }
    }
    Canvas(modifier.then(gesture).then(tapZones)) {
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
        // Предупреждения: жёлтый фон на всю высоту графика с подписью вверху зоны.
        model.activeWarnings.forEach { r ->
            val a = max(minOf(live.toView(r.fromKm), live.toView(r.toKm)), window.fromX)
            val b = minOf(max(live.toView(r.fromKm), live.toView(r.toKm)), window.toX)
            if (b <= a) return@forEach
            val x1 = px(a)
            val x2 = max(px(b), x1 + 3f * dp)
            drawRect(WarningYellow.copy(alpha = 0.38f), Offset(x1, topPad), androidx.compose.ui.geometry.Size(x2 - x1, bottomOfRows - topPad))
            drawLine(WarningYellow, Offset(x1, topPad), Offset(x1, bottomOfRows), strokeWidth = 2f * dp)
            // Подпись - карточка с жёлтой рамкой и указателем вниз, как на карте, но короче: у ограничения скорости только число,
            // у остальных видов - текст. Стоит по центру видимой части зоны и может быть шире самой зоны (внутри зоны подпись не влезала).
            val tl = measurer.measure(
                r.warning.chartLabel, warningStyle, overflow = TextOverflow.Ellipsis, maxLines = 1,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = (140f * dp).toInt()),
            )
            val padX = 5f * dp
            val padY = 2f * dp
            val pointer = 4f * dp
            val cw = tl.size.width + padX * 2
            val ch = tl.size.height + padY * 2
            val plotL = px(window.fromX)
            val plotR = px(window.toX)
            val cl = ((x1 + x2) / 2f - cw / 2f).coerceIn(plotL, max(plotL, plotR - cw))
            val ct = topPad + 2f * dp
            val card = Path().apply {
                addRoundRect(androidx.compose.ui.geometry.RoundRect(cl, ct, cl + cw, ct + ch, androidx.compose.ui.geometry.CornerRadius(5f * dp)))
                val mid = ((x1 + x2) / 2f).coerceIn(cl + 6f * dp, cl + cw - 6f * dp)
                moveTo(mid - pointer, ct + ch - 0.5f)
                lineTo(mid, ct + ch + pointer)
                lineTo(mid + pointer, ct + ch - 0.5f)
                close()
            }
            drawPath(card, Color.White)
            drawPath(card, WarningYellow, style = Stroke(width = 1.5f * dp))
            drawText(tl, topLeft = Offset(cl + padX, ct + padY))
        }
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
            model.stationsOnProfile(section).map { live.toView(it.km) to it.name }
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

        // Значки на ножках вверху графика: светофоры (кружок с табличкой), переезды (дорожный знак «Железнодорожный
        // переезд без шлагбаума», section.crossings) и места торможения («НТ», section.brakeTests) - каждый на
        // тонком стебле вниз до линии профиля, как светофор. Литер Ч - акцентный, Н - тёмный; светофор, стоящий
        // к поезду спиной (неправильный путь), - контуром без заливки. Яруса два, тесные значки пропускаются.
        val chipH = (if (detailed) 15f else 12f) * dp
        val rad = (if (detailed) 8f else 5.5f) * dp
        val signH = (if (detailed) 26f else 18f) * dp
        val signW = signH * crossingSign.width / crossingSign.height
        val boxW = (if (detailed) 26f else 20f) * dp
        val boxH = (if (detailed) 20f else 15f) * dp
        val levelH = maxOf(rad * 2f + 2f * dp + chipH, signH, boxH) + 4f * dp
        // Значки: (x, ширина, рисунок от верха своего яруса).
        val marks = ArrayList<Triple<Float, Float, (Float) -> Unit>>()
        signals.map { it to px(live.toView(it.km)) }
            .filter { it.second in (-20f * dp)..(w + 20f * dp) }
            .forEach { (sg, xx) ->
                val back = sg.facing != 0 && sg.facing != travelDir
                val even = sg.track == SignalTrack.EVEN
                val color = if (even) evenColor else oddColor
                val tl = measurer.measure(sg.title, signalStyle.copy(color = if (back) color else if (even) onEven else onOdd), maxLines = 1)
                val cw = tl.size.width + 6f * dp
                marks += Triple(xx, cw, { top ->
                    val plateTop = top + rad * 2f + 2f * dp
                    val lineY = py(line.heightAt(sg.km))
                    if (lineY > plateTop + chipH) drawLine(color.copy(alpha = 0.8f), Offset(xx, plateTop + chipH), Offset(xx, lineY), strokeWidth = 1f * dp)
                    val radius = androidx.compose.ui.geometry.CornerRadius(3f * dp, 3f * dp)
                    val chipSize = androidx.compose.ui.geometry.Size(cw, chipH)
                    val center = Offset(xx, top + rad)
                    if (back) {
                        drawCircle(color.copy(alpha = 0.12f), rad, center)
                        drawCircle(color, rad, center, style = Stroke(width = 1.2f * dp))
                        drawRoundRect(color.copy(alpha = 0.12f), Offset(xx - cw / 2, plateTop), chipSize, radius)
                        drawRoundRect(color, Offset(xx - cw / 2, plateTop), chipSize, radius, style = Stroke(width = 1.2f * dp))
                    } else {
                        drawCircle(color, rad, center)
                        drawRoundRect(color, Offset(xx - cw / 2, plateTop), chipSize, radius)
                    }
                    drawText(tl, topLeft = Offset(xx - cw / 2 + 3f * dp, plateTop + (chipH - tl.size.height) / 2f))
                })
            }
        section.crossings.forEach { c ->
            val vx = live.toView(c.km)
            if (vx < window.fromX || vx > window.toX) return@forEach
            val xx = px(vx)
            marks += Triple(xx, signW, { top ->
                val lineY = py(line.heightAt(c.km))
                if (lineY > top + signH) drawLine(ink.copy(alpha = 0.6f), Offset(xx, top + signH), Offset(xx, lineY), strokeWidth = 1f * dp)
                drawImage(
                    crossingSign,
                    dstOffset = androidx.compose.ui.unit.IntOffset((xx - signW / 2).roundToInt(), top.roundToInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(signW.roundToInt(), signH.roundToInt()),
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                )
            })
        }
        val brakeRed = Color(0xFFD62828)
        section.brakeTests.forEach { b ->
            val vx = live.toView(b.km)
            if (vx < window.fromX || vx > window.toX) return@forEach
            val xx = px(vx)
            marks += Triple(xx, boxW, { top ->
                val lineY = py(line.heightAt(b.km))
                if (lineY > top + boxH) drawLine(brakeRed.copy(alpha = 0.8f), Offset(xx, top + boxH), Offset(xx, lineY), strokeWidth = 1f * dp)
                drawRect(Color.White, Offset(xx - boxW / 2, top), androidx.compose.ui.geometry.Size(boxW, boxH))
                drawRect(brakeRed, Offset(xx - boxW / 2, top), androidx.compose.ui.geometry.Size(boxW, boxH), style = Stroke(width = 1.6f * dp))
                val tl = measurer.measure("НТ", signalStyle.copy(color = brakeRed), maxLines = 1)
                drawText(tl, topLeft = Offset(xx - tl.size.width / 2f, top + (boxH - tl.size.height) / 2f))
            })
        }
        val lastEnd = floatArrayOf(-1e9f, -1e9f)
        marks.sortedBy { it.first }.forEach { (xx, mw, drawMark) ->
            val left = xx - mw / 2
            val level = (0..1).firstOrNull { left > lastEnd[it] + 2f * dp } ?: return@forEach
            lastEnd[level] = left + mw
            drawMark(topPad + 2f * dp + level * levelH)
        }

        // Линия поезда: от головы (положение) назад по ходу на длину поезда, вдоль линии профиля.
        val trainKm = model.trainLengthKm
        if (pos != null && trainKm != null && !pos.offRoute) {
            val head = live.toView(pos.kmAxis)
            val flip = model.trainFlipped
            val tail = if (flip) (head + trainKm).coerceAtMost(live.lengthKm) else (head - trainKm).coerceAtLeast(0.0)
            val a = max(minOf(tail, head), window.fromX)
            val b = minOf(max(tail, head), window.toX)
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
                if (tail >= window.fromX && tail <= window.toX) {
                    val tx = px(tail)
                    drawLine(trainColor, Offset(tx, py(line.heightAt(live.toAxis(tail)))), Offset(tx, bottomOfRows), strokeWidth = 2f * dp)
                }
            }
        }

        // Начало замера расстояния: пунктирная вертикаль на всю высоту графика.
        model.measureStartAxisKm?.let { ax ->
            val mx = live.toView(ax)
            if (mx in window.fromX..window.toX) {
                val xx = px(mx)
                drawLine(ink, Offset(xx, topPad), Offset(xx, bottomOfRows), strokeWidth = 1.5f * dp, pathEffect = dash)
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
                // Голова поезда: стрелка по ходу (вид всегда повёрнут так, что поезд идёт вправо).
                val arrow = Path().apply {
                    moveTo(xx + 7f * dp, yy - 4f * dp)
                    lineTo(xx + 13f * dp, yy)
                    lineTo(xx + 7f * dp, yy + 4f * dp)
                    close()
                }
                drawPath(arrow, c)
            }
        }
    }
}

/** Жёлтый фон зоны предупреждения (профиль и карта). */
internal val WarningYellow = Color(0xFFFFD60A)

/** Окно показа полноэкранного профиля в км (пресеты); [ALL] - весь участок. */
internal enum class ProfileScale(val label: String, val spanKm: Double) {
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
internal fun ProfileFullScreen(model: ProfileModel, onDismiss: () -> Unit, leg: LegContext? = null, gnss: com.z_company.domain.gnss.GnssQuality? = null, speedKmh: Double? = null) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var spanKm by rememberSaveable { mutableStateOf(6f) }
    var follow by rememberSaveable { mutableStateOf(true) }
    var manualFrom by rememberSaveable { mutableStateOf(0f) }
    var showWarnings by rememberSaveable { mutableStateOf(false) }
    var showVoice by rememberSaveable { mutableStateOf(false) }
    var warnHint by remember { mutableStateOf(false) }
    var showSpeedHint by remember { mutableStateOf(false) }
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
                MapButton(onClick = onDismiss, description = "Назад", size = 38.dp) { PstIcon(PstIcons.ChevronLeft, 22.dp, Pst.text) }
                SectionHeader(model, Modifier.weight(1f)) { showPicker = true }
                if (reason == null && live != null) {
                    val length = live.viewSpanKm
                    ProfileScale.values().forEach { sc ->
                        val target = if (sc == ProfileScale.ALL) length.toFloat() else sc.spanKm.toFloat()
                        val on = abs(spanKm - target) < 0.05f * target.coerceAtLeast(1f)
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .background(if (on) Pst.accent else Pst.bgSubtle, RoundedCornerShape(12.dp))
                                .clickable { spanKm = target; follow = sc != ProfileScale.ALL }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = sc.label,
                                style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                color = if (on) Pst.onAccent else Pst.text,
                                maxLines = 1,
                            )
                        }
                    }
                    if (!follow) {
                        PstButton(
                            text = "К поезду", onClick = { follow = true },
                            kind = PstButtonKind.TONAL, height = 38.dp, fontSize = 13,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                        )
                    }
                    MeasureButton(model)
                    StripButton("Сменить направление", { model.toggleDirection() }) { PstIcon(PstIcons.Swap, 20.dp, Pst.text) }
                    if (model.hasSignals) {
                        SignalTrackSwitch(model)
                        ParitySwitch(model)
                    }                }
                // «ДУ-61» (предупреждения): видна всегда; без выбранного участка - пояснение вместо окна.
                val canWarn = model.canEditWarnings
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(if (model.activeWarnings.isNotEmpty()) WarningYellow else Pst.bgSubtle, RoundedCornerShape(12.dp))
                        .clickable(onClickLabel = "ДУ-61") { if (canWarn) showWarnings = true else warnHint = !warnHint },
                    contentAlignment = Alignment.Center,
                ) { PstIcon(PstIcons.Warning, 20.dp, if (canWarn) Pst.text else Pst.textMuted) }
            }
            // Скорость и качество GPS занимают строку под шапкой; качество GPS показывается только при проблемах сигнала.
            if (speedKmh != null || gnss != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (speedKmh != null) {
                        Box(
                            modifier = Modifier
                                .background(Pst.bgSubtle, RoundedCornerShape(8.dp))
                                .clickable(onClickLabel = "О скорости GPS") { showSpeedHint = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${speedKmh.toInt()} км/ч", style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Pst.text, maxLines = 1)
                        }
                    }
                    if (gnss != null) com.z_company.route.autorecord.GnssBadChip(gnss)
                }
            }
            if (warnHint && !model.canEditWarnings) {
                Text(
                    text = "Чтобы добавить ДУ-61, выберите участок.",
                    style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 15.sp),
                    color = Pst.text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp).fillMaxWidth()
                        .background(Pst.warning, RoundedCornerShape(12.dp)).padding(8.dp),
                )
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
                val length = live.viewSpanKm
                val vFrom = live.viewFrom
                val span = spanKm.toDouble().coerceIn(MIN_SPAN_KM.coerceAtMost(length), length)
                val followKm = model.position?.kmAxis // подписка на положение: окно сдвигается за поездом при каждом новом фиксе
                val window = if (follow) {
                    if (span >= length) {
                        ProfileWindow(vFrom, vFrom + length)
                    } else {
                        // Позади головы - не меньше длины поезда с запасом: поезд виден целиком при любом масштабе.
                        val room = ((model.trainLengthKm ?: 0.0) + TRAIN_MARGIN_KM).coerceAtMost(span * 0.8)
                        if (model.trainFlipped) live.window(span - max(span / 6, room), max(span / 6, room))
                        else live.window(max(span / 6, room), span - max(span / 6, room))
                    }
                } else {
                    val from = manualFrom.toDouble().coerceIn(vFrom, vFrom + (length - span).coerceAtLeast(0.0))
                    ProfileWindow(from, from + span)
                }
                // Жест: масштаб вокруг точки между пальцами, сдвиг пальцем. Чистый зум не отключает слежение за поездом.
                val onTransform: ProfileTransform = { zoom, panFrac, cFrac ->
                    val newSpan = (span / zoom).coerceIn(MIN_SPAN_KM.coerceAtMost(length), length)
                    val movedPan = abs(panFrac) > 0.002f
                    if (movedPan || !follow) {
                        val anchor = window.fromX + cFrac * span
                        val from = anchor - cFrac * newSpan - panFrac * newSpan
                        manualFrom = from.coerceIn(vFrom, vFrom + (length - newSpan).coerceAtLeast(0.0)).toFloat()
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
    if (showSpeedHint) {
        Dialog(onDismissRequest = { showSpeedHint = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().background(Pst.bg, RoundedCornerShape(20.dp)).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Скорость", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = Pst.text)
                Text("Данные скорости получены от спутников и зависят от качества сигнала. Они могут отличаться от показаний на приборах безопасности.", style = Pst.sans.copy(fontSize = 15.sp, lineHeight = 21.sp), color = Pst.text)
                PstButton(text = "Понятно", onClick = { showSpeedHint = false }, kind = PstButtonKind.TONAL, height = 42.dp)
            }
        }
    }
    if (showPicker) SectionPickerDialog(model, onDismiss = { showPicker = false })
    if (showWarnings) WarningsDialog(model, onVoice = { showVoice = true }, onDismiss = { showWarnings = false }, leg = leg)
    if (showVoice) WarningVoiceDialog(model, onDismiss = { showVoice = false })
    WarningEditDialog(model)
}

/**
 * Переключатель пути светофоров одной кнопкой: текст - текущий путь (1П - нечётные, литер Н; 2П - чётные,
 * литер Ч), нажатие меняет его на другой. Положение по умолчанию - по чётности поезда (номер), иначе по
 * направлению; по неправильному пути машинист включает светофоры того пути, по которому едет.
 */
@Composable
private fun SignalTrackSwitch(model: ProfileModel) {
    val current = model.signalTrack
    HeaderToggle(if (current == SignalTrack.ODD) "1П" else "2П") {
        model.chooseSignalTrack(if (current == SignalTrack.ODD) SignalTrack.EVEN else SignalTrack.ODD)
    }
}

/**
 * Чётность направления одной кнопкой (вместо отдельной настройки в диалоге): «Авто» - направление по плечу
 * поезда или по движению, «Чётное»/«Нечётное» - выбор вручную. Нажатие переводит в следующее состояние.
 * При повороте карты кнопка принимает стандартное для нового направления состояние - его чётность.
 */
@Composable
private fun ParitySwitch(model: ProfileModel) {
    val manual = model.directionIsManual
    val parity = model.parity
    val text = if (manual && parity != null) parity.title else "Авто"
    HeaderToggle(text) {
        when {
            !manual -> model.chooseParity(TravelParity.EVEN)
            parity == TravelParity.EVEN -> model.chooseParity(TravelParity.ODD)
            else -> model.useAutoDirection()
        }
    }
}

/** Кнопка-переключатель в шапке профиля: размер и вид как у соседних кнопок строки. */
@Composable
private fun HeaderToggle(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .background(Pst.bgSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = Pst.text,
            maxLines = 1,
        )
    }
}

/** «А — Б — В» в обратном направлении: «В — Б — А» (участок хранит ось в одну сторону, график разворачивается). */
private fun sectionTitle(name: String, reversed: Boolean): String =
    if (!reversed) name else name.split(" — ").asReversed().joinToString(" — ")

/** Участок и направление одной карточкой (нажатие - список участков): название, «от А к Б», способ выбора. */
@Composable
private fun SectionHeader(model: ProfileModel, modifier: Modifier = Modifier, onPick: () -> Unit) {
    val live = model.live
    val s = live?.section
    Row(
        // Высота и форма как у «Назад» и кнопок справа (38 dp, скругление 12 dp).
        modifier = modifier
            .height(38.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(Pst.surface, RoundedCornerShape(12.dp))
            .clickable(onClickLabel = "Выбрать участок", onClick = onPick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = model.profileTitle
                    ?: (s?.let { sectionTitle(it.name, live.reversed) } ?: "Выбрать участок"),
                style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = Pst.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Text("Сменить", style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = Pst.accent, maxLines = 1)
    }
}

/**
 * Строка «Сейчас» в одну линию: километр и пикет (нажатие или «Уточнить» - ручная привязка), уклон, ближайшая станция
 * впереди; вне участка - предупреждение.
 */
@Composable
private fun NowRow(model: ProfileModel, live: LiveProfile) {
    val pos = model.position
    var showCalibrate by rememberSaveable { mutableStateOf(false) }
    var showTrains by rememberSaveable { mutableStateOf(false) }
    var showLength by rememberSaveable { mutableStateOf(false) }
    val ahead = pos?.let { p ->
        val vx = live.toView(p.kmAxis)
        model.stationsOnProfile(live.section).map { live.toView(it.km) to it.name }.filter { it.first > vx + 0.05 }.minByOrNull { it.first }
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
                    if (model.offRails == true) "Вы находитесь вне путей." else "Вы вне этого участка. Выберите другой или дождитесь автоподбора.",
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
                    val junction = model.junctionAhead(live.section, live.reversed)
                    val junctionKm = model.junctionDistanceKm(pos.kmAxis, live.reversed)
                    val tail = buildList {
                        junction?.name?.takeIf { it.isNotEmpty() }?.let {
                            add("до ст. $it — ${pstKm(junctionKm ?: 0.0)}")
                        }
                        ahead?.let { add("до ст. ${it.first} — ${pstKm(it.second)}") }
                        model.signalsAhead(1).firstOrNull()?.let {
                            add("Светофор ${it.signal.title} — ${pstKm(it.distanceKm)}" + if (it.backToTrain) " (спиной)" else "")
                        }
                        if (ahead == null && pos.kmText == null) add("Нет километровой привязки — показано расстояние от начала.")
                    }.joinToString(" · ")
                    Text(tail, style = Pst.sans.copy(fontSize = 13.sp), color = Pst.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }
        }
        if (model.measureShown) MeasureChip(model)
        model.train?.let { tr ->
            val dataLength = tr.lengthM?.takeIf { it > 0 }
            val length = model.trainLengthM(tr)
            Row(
                modifier = Modifier.background(Pst.bgSubtle, RoundedCornerShape(12.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tr.number?.takeIf { it.isNotBlank() }?.let { "№$it" } ?: "Поезд без номера",
                    style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    color = Pst.text,
                    maxLines = 1,
                    modifier = Modifier.clickable(onClickLabel = "Выбрать поезд") { showTrains = true }.padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                )
                // Длина: из данных поезда (приоритетна, не правится здесь); нет в данных - вводится вручную, пока в данных не появится.
                Text(
                    text = "· " + (length?.let { "$it м" } ?: "длина не указана"),
                    style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    color = if (dataLength == null) Pst.accent else Pst.text,
                    maxLines = 1,
                    modifier = Modifier
                        .then(if (dataLength == null) Modifier.clickable(onClickLabel = "Указать длину поезда") { showLength = true } else Modifier)
                        .padding(start = 4.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                )
            }
            if (showLength && dataLength == null) TrainLengthDialog(model, tr, onDismiss = { showLength = false })
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

/** Кнопка-линейка в шапке: показывает/прячет панель замера внизу (замер сама не запускает; при скрытии замер сбрасывается). */
@Composable
private fun MeasureButton(model: ProfileModel) {
    val on = model.measureShown
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(if (on) Pst.accent else Pst.bgSubtle, RoundedCornerShape(12.dp))
            .clickable(onClickLabel = if (on) "Скрыть замер расстояния" else "Показать замер расстояния") { model.toggleMeasurePanel() },
        contentAlignment = Alignment.Center,
    ) { PstIcon(PstIcons.Ruler, 20.dp, if (on) Pst.onAccent else Pst.text) }
}

/** До километра - целые метры, дальше - километры с одним знаком. */
private fun measureText(m: Double): String =
    if (m < 1000) "${m.roundToInt()} м" else "${fmt1(m / 1000)} км"

/**
 * Панель замера в строке «Сейчас»: пройденное расстояние (по оси пути, при потере привязки - «по прямой»; цвет акцента, когда
 * пройдено не меньше длины поезда: хвост вышел) и кнопка по состоянию: «Старт» (замера нет / сброшен) → «Стоп» (идёт) → «Сброс»
 * (остановлен, число заморожено) → «Старт». Без фикса GPS «Старт» ничего не делает.
 */
@Composable
private fun MeasureChip(model: ProfileModel) {
    val m = model.measuredM ?: 0.0
    val trainM = model.trainLengthKm?.times(1000)
    val cleared = trainM != null && m >= trainM
    val running = model.measuring
    val stopped = model.measureStopped && model.hasMeasure
    Row(
        modifier = Modifier.background(Pst.bgSubtle, RoundedCornerShape(12.dp)).padding(start = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column {
            Text(
                text = measureText(m),
                style = Pst.mono.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                color = if (cleared) Pst.accent else Pst.text, maxLines = 1,
            )
            val sub = buildList {
                if (trainM != null) add("поезд ${trainM.roundToInt()} м")
                if (model.measureApprox) add("по прямой")
            }.joinToString(" · ")
            if (sub.isNotEmpty()) Text(sub, style = Pst.sans.copy(fontSize = 10.sp), color = Pst.textMuted, maxLines = 1)
        }
        PstButton(
            text = when { running -> "Стоп"; stopped -> "Сброс"; else -> "Старт" },
            onClick = { when { running -> model.stopMeasure(); stopped -> model.resetMeasure(); else -> model.startMeasure() } },
            kind = if (running) PstButtonKind.FILLED else PstButtonKind.TONAL, height = 34.dp, fontSize = 13,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
        )
    }
}

/** Длина поезда вручную (в данных поезда её нет): число метров; данные поезда приоритетнее - как только длина появится там, эта перестаёт использоваться. */
@Composable
private fun TrainLengthDialog(model: ProfileModel, t: ProfileTrain, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(TextFieldValue(model.trainLengthM(t)?.toString().orEmpty())) }
    var error by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier.fillMaxWidth(0.6f).background(Pst.bg, RoundedCornerShape(20.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Длина поезда", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
            Text(
                "В данных поезда длина не указана. Введите её, чтобы на профиле рисовался весь состав. Если длина появится в данных поезда, будет использоваться она.",
                style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Pst.textMuted,
            )
            PstTextField(
                text, { text = it; error = null }, "Длина, м", Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), mono = true,
            )
            error?.let { Text(it, style = Pst.sans.copy(fontSize = 13.sp), color = Pst.danger) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
                if (model.trainLengthM(t) != null) {
                    PstButton("Сбросить", { model.setManualLength(t.id, null); onDismiss() }, kind = PstButtonKind.TEXT, height = 40.dp, fontSize = 14)
                }
                PstButton("Отмена", onDismiss, kind = PstButtonKind.TEXT, height = 40.dp, fontSize = 14)
                PstButton(
                    "Сохранить",
                    {
                        val m = text.text.trim().toIntOrNull()
                        if (m == null || m !in 10..3000) error = "Длина — число метров от 10 до 3000."
                        else { model.setManualLength(t.id, m); onDismiss() }
                    },
                    kind = PstButtonKind.FILLED, height = 40.dp, fontSize = 14,
                )
            }
        }
    }
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
internal fun SectionPickerDialog(model: ProfileModel, onDismiss: () -> Unit) {
    val downloadViewModel: OfflineSectionsViewModel = koinViewModel()
    val downloadState by downloadViewModel.state.collectAsState()
    OfflineSectionDownloadDialogs(downloadViewModel)
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val rows = remember(query.text, model.summaries, model.sections, model.manualId, model.hasFix) { model.rows(query.text) }
    // Сначала ждём положение (до 1 с) и показываем уже отсортированный список; пока ждём - индикатор загрузки. Положения нет -
    // показываем как есть (по названию), список не «прыгает» после появления.
    var waitedForFix by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(1_000); waitedForFix = true }
    val sorting = !model.hasFix && !waitedForFix && model.summaries.isNotEmpty()
    // Порядок фиксируем в момент первого показа (на запрос поиска): дальше список не переставляется и не дёргается, даже
    // если положение уточнилось; расстояния в строках обновляются, новые строки идут в конец.
    var frozen by remember(query.text) { mutableStateOf<Map<String, Int>?>(null) }
    LaunchedEffect(sorting, rows) {
        if (!sorting && frozen == null && rows.isNotEmpty()) frozen = rows.mapIndexed { i, r -> r.summary.id to i }.toMap()
    }
    val shown = frozen?.let { f -> rows.sortedBy { f[it.summary.id] ?: Int.MAX_VALUE } } ?: rows
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
            if (sorting) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = Pst.accent, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                }
            } else if (rows.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (model.needLogin) "Профиль пути доступен после входа в аккаунт." else if (model.deviceRejected) DEVICE_REJECTED_TEXT else if (model.rateLimited) RATE_LIMITED_TEXT else if (model.summaries.isEmpty()) "Участки не загружены. Проверьте интернет." else "Ничего не найдено. Данные профилей собираются — список пополняется.",
                        style = Pst.sans.copy(fontSize = 14.sp),
                        color = Pst.textMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(shown, key = { it.summary.id }) { row ->
                        val s = row.summary
                        val chosen = s.id == model.section?.id
                        val offlineStatus = downloadState.items.firstOrNull { it.id == s.id }?.status ?: OfflineStatus.NOT_DOWNLOADED
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
                            if (s.hasProfile || offlineStatus == OfflineStatus.READY || offlineStatus == OfflineStatus.OUTDATED) {
                                when (offlineStatus) {
                                    OfflineStatus.DOWNLOADING -> CircularProgressIndicator(Modifier.size(22.dp), color = Pst.accent, strokeWidth = 2.dp)
                                    OfflineStatus.READY -> PstIcon(PstIcons.Check, 20.dp, Pst.accent)
                                    OfflineStatus.NOT_DOWNLOADED, OfflineStatus.OUTDATED -> IconButton(
                                        onClick = { downloadViewModel.download(s.id) },
                                        enabled = !downloadState.busy,
                                    ) {
                                        PstIcon(
                                            PstIcons.Download,
                                            20.dp,
                                            if (offlineStatus == OfflineStatus.OUTDATED) Pst.success else Pst.accent,
                                            description = if (offlineStatus == OfflineStatus.OUTDATED) "Обновить участок" else "Скачать участок",
                                        )
                                    }
                                }
                            }
                            if (chosen && offlineStatus != OfflineStatus.READY) PstIcon(PstIcons.Check, 20.dp, Pst.accent)
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Pst.border))
                    }
                }
            }
        }
    }
}
