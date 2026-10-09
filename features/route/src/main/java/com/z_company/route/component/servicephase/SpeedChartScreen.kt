package com.z_company.route.component.servicephase

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.z_company.domain.track.ChartAxis
import com.z_company.domain.track.ChartWindow
import com.z_company.domain.track.SpeedChart
import com.z_company.domain.track.SpeedScale
import com.z_company.domain.track.TrackAnalysis
import com.z_company.domain.track.TrackEmptyReason
import com.z_company.domain.track.TrackEmptyReasons
import com.z_company.domain.track.TrackRecordingState
import com.z_company.domain.track.TrackPoint
import com.z_company.domain.util.LabelPlacer
import com.z_company.domain.util.MapLabelItem
import com.z_company.route.viewmodel.TrainFormViewModel
import kotlinx.coroutines.delay

private const val EMPTY_TEXT_UNKNOWN = "Нет записи скорости. Включите АВТО и согласие на сохранение трека"

/** Текст пустого графика по реальной причине (§6.9). */
private fun emptyText(reason: TrackEmptyReason, otherTrainNumber: String?): String = when (reason) {
    TrackEmptyReason.UNKNOWN -> EMPTY_TEXT_UNKNOWN
    TrackEmptyReason.CONSENT_OFF -> "Согласие на сохранение трека выключено"
    TrackEmptyReason.AUTO_OFF -> "АВТО выключено. Включите АВТО, чтобы записывать путь"
    TrackEmptyReason.OTHER_TRAIN ->
        "АВТО записывает другой поезд" + otherTrainNumber?.takeIf { it.isNotBlank() }?.let { " (№$it)" }.orEmpty()
    TrackEmptyReason.NO_TARGET -> "АВТО включено, но поезд для записи не выбран"
    TrackEmptyReason.NO_POINTS_YET -> "Запись идёт, точек пока нет. Нужно проехать несколько секунд"
    TrackEmptyReason.NO_SPEED -> "Нет данных о скорости от GPS"
}

/** Всё, что нужно обработчикам жестов (актуальное значение на момент жеста). */
private class ChartGeometry(
    val total: Double,
    val window: ChartWindow,
    val axis: ChartAxis,
    val plotLeft: Float,
    val plotWidth: Float,
    val minSpan: Double,
)

/**
 * Экран «График скорости» (SCREEN_SPECS §6.9, этап 4b): скорость (км/ч) по пути или времени из записанного трека,
 * станции/остановки метками, зум (щипок и «+»/«−») и прокрутка по оси X, курсор по касанию. [selectedTimeMs] —
 * общий с картой выбор точки: касание графика вызывает [onSelect] (карта подсветит ту же точку при возврате),
 * выбранная на карте точка показывается курсором. Без трека — пустое состояние с переходом к настройкам согласия.
 */
@Composable
internal fun SpeedChartScreen(
    track: TrainFormViewModel.TrainMapTrack?,
    loadRecordingState: suspend () -> TrackRecordingState? = { null },
    makeTarget: () -> Boolean = { false },
    selectedTimeMs: Long?,
    onSelect: (TrackPoint?) -> Unit,
    onOpenConsent: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val points = track?.points.orEmpty()
        val samples = remember(track) { SpeedChart.buildSeries(points) }
        val zone = track?.zone ?: "GMT+3"
        var axisName by rememberSaveable { mutableStateOf(ChartAxis.PATH.name) }
        val axis = ChartAxis.valueOf(axisName)
        var windowState by remember { mutableStateOf<ChartWindow?>(null) }
        val total = SpeedChart.total(samples, axis)
        val minSpan = SpeedChart.minSpan(axis, total)
        val window = windowState?.let { SpeedChart.clamp(it, total) } ?: SpeedChart.fullWindow(total)
        var plotLeft by remember { mutableStateOf(0f) }
        var plotWidth by remember { mutableStateOf(1f) }
        val geo = rememberUpdatedState(ChartGeometry(total, window, axis, plotLeft, plotWidth, minSpan))
        val samplesState = rememberUpdatedState(samples)
        val pointsState = rememberUpdatedState(points)
        val onSelectState = rememberUpdatedState(onSelect)

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().background(Pst.bg).statusBarsPadding().navigationBarsPadding(),
        ) {
            val wide = maxWidth >= 600.dp
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
                @Composable
                fun backButton() = ChartButton("Назад", onDismiss) { PstIcon(PstIcons.ChevronLeft, 22.dp, Pst.text) }

                @Composable
                fun axisToggle() = AxisToggle(axis) {
                    axisName = it.name
                    windowState = null
                }

                @Composable
                fun zoomButtons() = Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val enabled = samples.size >= 2
                    ChartButton("Отдалить", onClick = {
                        if (enabled) windowState = SpeedChart.zoom(window, total, 0.5, (window.start + window.end) / 2, minSpan)
                    }) { Text("−", style = Pst.sans.copy(fontSize = 22.sp), color = if (enabled) Pst.text else Pst.textFaint) }
                    ChartButton("Приблизить", onClick = {
                        if (enabled) windowState = SpeedChart.zoom(window, total, 2.0, (window.start + window.end) / 2, minSpan)
                    }) { Text("+", style = Pst.sans.copy(fontSize = 22.sp), color = if (enabled) Pst.text else Pst.textFaint) }
                    ChartButton("Показать весь путь", onClick = { windowState = null }) {
                        Text("Всё", style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = if (enabled) Pst.text else Pst.textFaint)
                    }
                }

                val title: @Composable (Modifier) -> Unit = { m ->
                    Text(
                        text = "График скорости",
                        style = Pst.sans.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                        color = Pst.text,
                        maxLines = 1,
                        modifier = m,
                    )
                }
                if (wide) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        backButton()
                        title(Modifier.weight(1f))
                        axisToggle()
                        zoomButtons()
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        backButton()
                        title(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        axisToggle()
                        zoomButtons()
                    }
                }
                Spacer(Modifier.height(8.dp))

                val pointsWithSpeed = points.count { it.speedKmh != null }
                if (samples.size < 2 || pointsWithSpeed == 0) {
                    // Пустое состояние с реальной причиной: состояние записи опрашивается, пока экран пуст.
                    var recording by remember { mutableStateOf<TrackRecordingState?>(null) }
                    var refresh by remember { mutableStateOf(0) }
                    LaunchedEffect(refresh) {
                        while (true) {
                            recording = runCatching { loadRecordingState() }.getOrNull()
                            delay(3_000)
                        }
                    }
                    val reason = TrackEmptyReasons.determine(recording, points.size, pointsWithSpeed)
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = emptyText(reason, recording?.otherTrainNumber),
                            style = Pst.sans.copy(fontSize = 15.sp, lineHeight = 22.sp),
                            color = Pst.textMuted,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                        when (reason) {
                            TrackEmptyReason.CONSENT_OFF ->
                                PstButton(text = "Открыть настройки", onClick = onOpenConsent, kind = PstButtonKind.CTA)
                            TrackEmptyReason.UNKNOWN ->
                                PstButton(text = "Настройки согласия", onClick = onOpenConsent, kind = PstButtonKind.CTA)
                            TrackEmptyReason.OTHER_TRAIN, TrackEmptyReason.NO_TARGET ->
                                PstButton(
                                    text = "Записывать этот поезд",
                                    onClick = { if (makeTarget()) refresh++ },
                                    kind = PstButtonKind.CTA,
                                )
                            else -> Unit
                        }
                        Spacer(Modifier.weight(1f, fill = false).height(24.dp))
                        Text(
                            text = TrackEmptyReasons.debugLine(recording, points.size),
                            style = Pst.mono.copy(fontSize = 10.sp),
                            color = Pst.textFaint,
                        )
                    }
                    return@Column
                }

                val stops = remember(samples, track, axis) { SpeedChart.stopsOnAxis(samples, track?.stops.orEmpty(), axis) }
                val selectedIdx = selectedTimeMs?.let { SpeedChart.indexAtTime(samples, it) }
                    ?.takeIf { it >= 0 && kotlin.math.abs(samples[it].timeMs - selectedTimeMs) <= 60_000L }

                SpeedPlot(
                    samples = samples,
                    stops = stops,
                    axis = axis,
                    window = window,
                    zone = zone,
                    selectedIdx = selectedIdx,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    onLayout = { l, w -> plotLeft = l; plotWidth = w },
                    onTransform = { centroidX, panX, zoom ->
                        val g = geo.value
                        if (g.total > 0 && g.plotWidth > 0f) {
                            val anchor = g.window.start + ((centroidX - g.plotLeft) / g.plotWidth).coerceIn(0f, 1f) * g.window.span
                            val zoomed = SpeedChart.zoom(g.window, g.total, zoom.toDouble(), anchor, g.minSpan)
                            windowState = SpeedChart.pan(zoomed, g.total, -(panX / g.plotWidth) * zoomed.span)
                        }
                    },
                    onTap = { tapX ->
                        val g = geo.value
                        val x = g.window.start + ((tapX - g.plotLeft) / g.plotWidth).coerceIn(0f, 1f) * g.window.span
                        val i = SpeedChart.nearest(samplesState.value, g.axis, x)
                        if (i >= 0) onSelectState.value(pointsState.value.getOrNull(samplesState.value[i].index))
                    },
                )
                Spacer(Modifier.height(8.dp))
                CursorCard(samples, selectedIdx, zone)
            }
        }
    }
}

@Composable
private fun SpeedPlot(
    samples: List<com.z_company.domain.track.SpeedSample>,
    stops: List<com.z_company.domain.track.ChartStop>,
    axis: ChartAxis,
    window: ChartWindow,
    zone: String,
    selectedIdx: Int?,
    modifier: Modifier,
    onLayout: (left: Float, width: Float) -> Unit,
    onTransform: (centroidX: Float, panX: Float, zoom: Float) -> Unit,
    onTap: (x: Float) -> Unit,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val axisStyle = Pst.mono.copy(fontSize = 10.sp, color = Pst.textMuted)
    val stationStyle = Pst.sans.copy(fontSize = 11.sp, color = Pst.text)
    val textColor = Pst.text
    val gridColor = Pst.border
    val surface = Pst.surface
    val accent = Pst.accent
    val onTransformState = rememberUpdatedState(onTransform)
    val onTapState = rememberUpdatedState(onTap)
    val t0 = samples.first().timeMs
    val zoneOffset = remember(zone, t0) { SpeedChart.zoneOffsetMs(zone, t0) }
    val yMax = remember(samples) { SpeedChart.speedCeiling(samples) }
    // Подписи станций (ellipsis до 110 dp) измеряем один раз на набор остановок.
    val labelLayouts: List<TextLayoutResult?> = remember(stops, density) {
        val maxW = with(density) { 110.dp.roundToPx() }
        stops.map { st ->
            st.name?.takeIf { it.isNotBlank() }?.let {
                measurer.measure(it, stationStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, constraints = Constraints(maxWidth = maxW))
            }
        }
    }
    val desc = "График скорости"
    Canvas(
        modifier = modifier
            .clipToBounds()
            .background(surface, RoundedCornerShape(16.dp))
            .semantics { contentDescription = desc }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ -> onTransformState.value(centroid.x, pan.x, zoom) }
            }
            .pointerInput(Unit) { detectTapGestures { onTapState.value(it.x) } },
    ) {
        val d = density.density
        val left = 40f * d
        val right = 14f * d
        val top = 34f * d
        val bottom = 22f * d
        val plotW = size.width - left - right
        val plotH = size.height - top - bottom
        if (plotW <= 10f || plotH <= 10f) return@Canvas
        onLayout(left, plotW)
        val span = window.span.coerceAtLeast(1e-6)
        fun px(x: Double) = left + ((x - window.start) / span * plotW).toFloat()
        fun py(v: Double) = top + plotH - (v / yMax * plotH).toFloat()

        // Сетка и подписи скорости.
        val yStep = if (yMax <= 120) 20.0 else 40.0
        var v = 0.0
        while (v <= yMax + 1e-6) {
            val y = py(v)
            drawLine(gridColor, Offset(left, y), Offset(left + plotW, y), strokeWidth = 1f)
            val tl = measurer.measure(v.toInt().toString(), axisStyle)
            drawText(tl, topLeft = Offset(left - 6f * d - tl.size.width, y - tl.size.height / 2f))
            v += yStep
        }
        val ul = measurer.measure("км/ч", axisStyle)
        drawText(ul, topLeft = Offset(2f * d, top - ul.size.height - 6f * d))

        // Подписи оси X.
        val maxTicks = (plotW / (64f * d)).toInt().coerceAtLeast(2)
        val ticks: List<Pair<Double, String>> = if (axis == ChartAxis.PATH) {
            SpeedChart.niceTicks(window.start / 1000.0, window.end / 1000.0, maxTicks).map { km ->
                (km * 1000.0) to (if (km == km.toLong().toDouble()) km.toLong().toString() else "%.1f".format(java.util.Locale("ru"), km))
            }
        } else {
            SpeedChart.timeTicks(t0, window.start, window.end, maxTicks, zoneOffset).map { ms ->
                ms to TrackAnalysis.clock(t0 + ms.toLong(), zone)
            }
        }
        ticks.forEach { (x, label) ->
            val xx = px(x)
            drawLine(gridColor, Offset(xx, top), Offset(xx, top + plotH), strokeWidth = 1f)
            val tl = measurer.measure(label, axisStyle)
            val tx = (xx - tl.size.width / 2f).coerceIn(left - 8f * d, size.width - tl.size.width.toFloat())
            drawText(tl, topLeft = Offset(tx, top + plotH + 4f * d))
        }
        val unit = measurer.measure(if (axis == ChartAxis.PATH) "км" else "", axisStyle)
        if (axis == ChartAxis.PATH) drawText(unit, topLeft = Offset(2f * d, top + plotH + 4f * d))

        // Остановки: на оси времени — полоса стоянки, на обеих — вертикальная линия.
        val inView = stops.indices.filter { stops[it].xEnd >= window.start && stops[it].x <= window.end }
        inView.forEach { i ->
            val st = stops[i]
            if (st.xEnd > st.x) {
                val a = px(st.x).coerceIn(left, left + plotW)
                val b = px(st.xEnd).coerceIn(left, left + plotW)
                drawRect(accent.copy(alpha = 0.18f), Offset(a, top), androidx.compose.ui.geometry.Size((b - a).coerceAtLeast(2f * d), plotH))
            }
            val xx = px(st.x)
            if (xx in left..(left + plotW)) {
                drawLine(
                    accent.copy(alpha = 0.7f), Offset(xx, top - 6f * d), Offset(xx, top + plotH),
                    strokeWidth = 1.5f * d, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f * d, 4f * d)),
                )
            }
        }

        // Линия скорости: цвет сегмента по шкале карты; светлая/тёмная «обводка» цвета текста даёт контраст в обеих темах.
        val cols = plotW.toInt().coerceAtLeast(2)
        val vis = SpeedChart.visible(samples, axis, window, cols)
        // «Нет данных» (потеря сигнала/записи, разрыв > 90 с, SCREEN_SPECS §6.9): штриховая полоса, линия скорости рвётся.
        val noData = (1 until samples.size).filter { samples[it].timeMs - samples[it - 1].timeMs > TrackAnalysis.DATA_GAP_MS }
        clipRect(left, top - 1f, left + plotW, top + plotH + 1f) {
            for (k in noData) {
                val x1 = px(SpeedChart.xOf(samples[k - 1], axis, t0))
                val x2 = px(SpeedChart.xOf(samples[k], axis, t0))
                val w = (x2 - x1).coerceAtLeast(3f * d)
                drawRect(textColor.copy(alpha = 0.08f), Offset(x1, top), androidx.compose.ui.geometry.Size(w, plotH))
                drawLine(
                    textColor.copy(alpha = 0.45f), Offset(x1, top + plotH / 2f), Offset(x1 + w, top + plotH / 2f),
                    strokeWidth = 1.5f * d, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * d, 4f * d)),
                )
            }
        }
        clipRect(left, top - 1f, left + plotW, top + plotH + 1f) {
            for (pass in 0..1) {
                for (i in 1 until vis.size) {
                    val a = vis[i - 1]
                    val b = vis[i]
                    if (b.timeMs - a.timeMs > TrackAnalysis.GAP_MS) continue
                    if (noData.any { samples[it].timeMs > a.timeMs && samples[it - 1].timeMs < b.timeMs }) continue
                    val sa = a.speedKmh ?: continue
                    val sb = b.speedKmh ?: continue
                    val p1 = Offset(px(SpeedChart.xOf(a, axis, t0)), py(sa))
                    val p2 = Offset(px(SpeedChart.xOf(b, axis, t0)), py(sb))
                    if (pass == 0) {
                        drawLine(textColor.copy(alpha = 0.35f), p1, p2, strokeWidth = 5f * d, cap = StrokeCap.Round)
                    } else {
                        drawLine(Color(0xFF000000 or SpeedScale.rgb((sa + sb) / 2).toLong()), p1, p2, strokeWidth = 3f * d, cap = StrokeCap.Round)
                    }
                }
            }
        }

        // Подписи станций в верхней полосе: показываются только те, что помещаются (LabelPlacer).
        val dotR = 3f * d
        val dotY = top - 24f * d
        val items = inView.map { i ->
            val lay = labelLayouts.getOrNull(i)
            MapLabelItem(
                px(stops[i].x).toDouble(), dotY.toDouble(),
                labelW = lay?.size?.width?.toDouble() ?: 0.0, labelH = lay?.size?.height?.toDouble() ?: 0.0,
                dotRadius = dotR.toDouble(), priority = if (lay != null) 0 else 1,
            )
        }
        val shown = LabelPlacer.visible(items)
        inView.forEachIndexed { k, i ->
            val xx = px(stops[i].x)
            drawCircle(accent, dotR, Offset(xx, dotY))
            if (k in shown) {
                val lay = labelLayouts[i] ?: return@forEachIndexed
                val tx = (xx - lay.size.width / 2f).coerceIn(2f * d, size.width - lay.size.width - 2f * d)
                drawText(lay, topLeft = Offset(tx, dotY + dotR + 3f * d))
            }
        }

        // Курсор выбранной точки.
        if (selectedIdx != null) {
            val s = samples[selectedIdx]
            val xx = px(SpeedChart.xOf(s, axis, t0))
            if (xx in left..(left + plotW)) {
                drawLine(textColor, Offset(xx, top), Offset(xx, top + plotH), strokeWidth = 1.5f * d)
                s.speedKmh?.let { sp ->
                    val c = Offset(xx, py(sp))
                    drawCircle(surface, 6.5f * d, c)
                    drawCircle(Color(0xFF000000 or SpeedScale.rgb(sp).toLong()), 4.5f * d, c)
                    drawCircle(textColor, 6.5f * d, c, style = Stroke(width = 1.5f * d))
                }
            }
        }
    }
}

@Composable
private fun CursorCard(
    samples: List<com.z_company.domain.track.SpeedSample>,
    idx: Int?,
    zone: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Pst.surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (idx != null) {
            val s = samples[idx]
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
                Text(
                    TrackAnalysis.clock(s.timeMs, zone, seconds = true),
                    style = Pst.mono.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = Pst.text,
                )
                Text(
                    s.speedKmh?.let { "${it.toInt()} км/ч" } ?: "скорость неизвестна",
                    style = Pst.mono.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = Pst.text,
                )
                Text(
                    pstKm(s.distM / 1000.0) + " от начала",
                    style = Pst.mono.copy(fontSize = 14.sp), color = Pst.textMuted,
                )
            }
        } else {
            val last = samples.last()
            val maxV = samples.maxOfOrNull { it.speedKmh ?: 0.0 } ?: 0.0
            Text(
                "Коснитесь графика, чтобы увидеть значения",
                style = Pst.sans.copy(fontSize = 13.sp), color = Pst.textMuted,
            )
            Text(
                "Путь ${pstKm(last.distM / 1000.0)} · ${durationText(last.timeMs - samples.first().timeMs)} · макс. ${maxV.toInt()} км/ч",
                style = Pst.mono.copy(fontSize = 13.sp), color = Pst.text,
            )
        }
    }
}

private fun durationText(ms: Long): String {
    val m = (ms / 60_000L).toInt()
    return if (m >= 60) "${m / 60} ч ${m % 60} м" else "$m м"
}

@Composable
private fun ChartButton(description: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = if (description == "Показать весь путь") 52.dp else 44.dp, height = 44.dp)
            .shadow(3.dp, RoundedCornerShape(14.dp))
            .background(Pst.surface, RoundedCornerShape(14.dp))
            .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun AxisToggle(axis: ChartAxis, onChange: (ChartAxis) -> Unit) {
    Row(
        modifier = Modifier
            .height(44.dp)
            .background(Pst.bgSubtle, RoundedCornerShape(14.dp))
            .padding(3.dp),
    ) {
        listOf(ChartAxis.PATH to "Путь", ChartAxis.TIME to "Время").forEach { (a, label) ->
            val sel = a == axis
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(38.dp)
                    .background(if (sel) Pst.accent else Color.Transparent, RoundedCornerShape(11.dp))
                    .clickable(onClickLabel = "Ось X: $label") { onChange(a) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    color = if (sel) Pst.onAccent else Pst.text,
                )
            }
        }
    }
}
