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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.z_company.domain.track.ChartAxis
import com.z_company.domain.track.ChartWindow
import com.z_company.domain.track.SpeedChart
import com.z_company.domain.track.SpeedProfile
import com.z_company.domain.track.SpeedSample
import com.z_company.domain.track.SpeedScale
import com.z_company.domain.track.TrackAnalysis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Доля высоты экрана, занимаемая каждой нижней полосой (скорость, профиль пути). */
internal const val MAP_STRIP_HEIGHT_FRACTION = 0.12f

/** Высота нижней полосы: ~12% экрана, но не меньше 88 dp: в полосе две кнопки по вертикали. */
internal fun mapStripHeight(screenHeight: Dp): Dp = maxOf(88.dp, screenHeight * MAP_STRIP_HEIGHT_FRACTION)

/** Квадратная кнопка внутри полосы: на фоне полосы, без тени. */
@Composable
private fun StripButton(description: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(Pst.bgSubtle, RoundedCornerShape(12.dp))
            .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Кнопки полосы по вертикали у правого края: «Развернуть» - вверху, «Скрыть» - внизу. */
@Composable
private fun StripButtons(expandDescription: String, onExpand: (() -> Unit)?, hideDescription: String, onHide: () -> Unit, height: Dp) {
    Column(
        modifier = Modifier.height(height).padding(vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (onExpand != null) {
            StripButton(expandDescription, onExpand) { PstIcon(PstIcons.Expand, 20.dp, Pst.text) }
        } else {
            Box(Modifier.size(38.dp))
        }
        StripButton(hideDescription, onHide) { PstIcon(PstIcons.ChevronDown, 20.dp, Pst.text) }
    }
}

/**
 * Полоса скорости внизу карты: слева текущая скорость, в центре мини-график скорости (без прокрутки; последние
 * [MINI_CHART_WINDOW_M] пути или весь трек, если он короче), справа кнопки «Развернуть» (полноэкранный график) и «Скрыть».
 * [speedKmh] null - скорости нет (показывается «0»). [caption] - пояснение под числом. [onExpand] null - кнопки нет.
 */
@Composable
internal fun SpeedStrip(
    height: Dp,
    speedKmh: Double?,
    caption: String?,
    samples: List<SpeedSample>,
    profile: SpeedProfile,
    selectedTimeMs: Long?,
    emptyText: String,
    /** Кнопка под пояснением пустого графика (например, «Настройки согласия»); null - без кнопки. */
    emptyActionLabel: String?,
    onEmptyAction: (() -> Unit)?,
    onExpand: (() -> Unit)?,
    onHide: () -> Unit,
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
        Column(verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = (speedKmh?.toInt() ?: 0).toString(),
                    style = Pst.mono.copy(fontSize = (height.value * 0.42f).sp, fontWeight = FontWeight.Bold),
                    color = Pst.text,
                    maxLines = 1,
                )
                Text(
                    text = "км/ч",
                    style = Pst.sans.copy(fontSize = 12.sp),
                    color = Pst.textMuted,
                    modifier = Modifier.padding(bottom = (height.value * 0.06f).dp),
                    maxLines = 1,
                )
            }
            if (caption != null) {
                Text(text = caption, style = Pst.sans.copy(fontSize = 10.sp), color = Pst.textMuted, maxLines = 1)
            }
        }
        MiniSpeedChart(
            samples = samples,
            profile = profile,
            selectedTimeMs = selectedTimeMs,
            emptyText = emptyText,
            emptyActionLabel = emptyActionLabel,
            onEmptyAction = onEmptyAction,
            modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = 8.dp)
                .then(if (onExpand != null) Modifier.clickable(onClickLabel = "График скорости на весь экран", onClick = onExpand) else Modifier),
        )
        StripButtons("График скорости на весь экран", onExpand, "Скрыть скорость", onHide, height)
    }
}

/** Длина участка пути, показываемого мини-графиком, м. */
internal const val MINI_CHART_WINDOW_M = 15_000.0

/** Мини-версия графика скорости: линия цвета шкалы скорости по пути, без осей X и прокрутки; пусто - пояснение. */
@Composable
private fun MiniSpeedChart(samples: List<SpeedSample>, profile: SpeedProfile, selectedTimeMs: Long?, emptyText: String, emptyActionLabel: String?, onEmptyAction: (() -> Unit)?, modifier: Modifier) {
    val dark = Pst.surface.luminance() < 0.5f
    val grid = Pst.border
    val ink = Pst.text
    val measurer = rememberTextMeasurer()
    val labelStyle = Pst.mono.copy(fontSize = 9.sp, color = Pst.textMuted)
    val density = LocalDensity.current.density
    if (samples.size < 2) {
        Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = emptyText,
                style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 15.sp),
                color = Pst.textMuted,
                textAlign = TextAlign.Center,
            )
            if (emptyActionLabel != null && onEmptyAction != null) {
                PstButton(
                    text = emptyActionLabel,
                    onClick = onEmptyAction,
                    kind = PstButtonKind.TONAL,
                    height = 32.dp,
                    fontSize = 13,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        return
    }
    val t0 = samples.first().timeMs
    val total = SpeedChart.total(samples, ChartAxis.PATH)
    val window = ChartWindow((total - MINI_CHART_WINDOW_M).coerceAtLeast(0.0), total.coerceAtLeast(1.0))
    val yMax = remember(samples) { SpeedChart.speedCeiling(samples) }
    val selIdx = selectedTimeMs?.let { SpeedChart.indexAtTime(samples, it) }
    Canvas(modifier) {
        val labelW = 22f * density
        val left = labelW
        val plotW = size.width - left
        val plotH = size.height
        if (plotW <= 10f || plotH <= 10f) return@Canvas
        val span = window.span.coerceAtLeast(1e-6)
        fun px(x: Double) = left + ((x - window.start) / span * plotW).toFloat()
        fun py(v: Double) = plotH - (v / yMax * plotH).toFloat()
        // Две линии сетки: «0» и верх шкалы.
        listOf(0.0, yMax / 2, yMax).forEach { v ->
            drawLine(grid, Offset(left, py(v)), Offset(left + plotW, py(v)), strokeWidth = 1f)
        }
        listOf(0.0, yMax).forEach { v ->
            val tl = measurer.measure(v.toInt().toString(), labelStyle)
            val y = (py(v) - tl.size.height / 2f).coerceIn(0f, plotH - tl.size.height)
            drawText(tl, topLeft = Offset(left - 4f * density - tl.size.width, y))
        }
        val vis = SpeedChart.visible(samples, ChartAxis.PATH, window, plotW.toInt().coerceAtLeast(2))
        clipRect(left, -1f, left + plotW, plotH + 1f) {
            for (pass in 0..1) {
                for (i in 1 until vis.size) {
                    val a = vis[i - 1]
                    val b = vis[i]
                    if (b.timeMs - a.timeMs > TrackAnalysis.DATA_GAP_MS) continue
                    val sa = a.speedKmh ?: continue
                    val sb = b.speedKmh ?: continue
                    val p1 = Offset(px(SpeedChart.xOf(a, ChartAxis.PATH, t0)), py(sa))
                    val p2 = Offset(px(SpeedChart.xOf(b, ChartAxis.PATH, t0)), py(sb))
                    if (pass == 0) {
                        drawLine(ink.copy(alpha = 0.35f), p1, p2, strokeWidth = 4f * density, cap = StrokeCap.Round)
                    } else {
                        drawLine(Color(0xFF000000 or SpeedScale.rgb((sa + sb) / 2, profile, dark).toLong()), p1, p2, strokeWidth = 2.2f * density, cap = StrokeCap.Round)
                    }
                }
            }
        }
        // Выбранная точка (просмотр записанного трека): вертикальный курсор.
        if (selIdx != null) {
            val xx = px(SpeedChart.xOf(samples[selIdx], ChartAxis.PATH, t0))
            if (xx in left..(left + plotW)) drawLine(ink, Offset(xx, 0f), Offset(xx, plotH), strokeWidth = 1.5f * density)
        }
    }
}

/**
 * Полоса «Профиль пути»: заготовка под продольный профиль (уклоны/высоты). Сейчас показывает пояснение;
 * кнопки по вертикали у правого края: «Развернуть на весь экран» вверху, «Скрыть» внизу.
 */
@Composable
internal fun ProfileStrip(
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
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "ПРОФИЛЬ ПУТИ", style = Pst.mono.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = Pst.textMuted)
            Text(
                text = PROFILE_PLACEHOLDER,
                style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 17.sp),
                color = Pst.textMuted,
                maxLines = 2,
            )
        }
        StripButtons("Профиль пути на весь экран", onExpand, "Скрыть профиль пути", onHide, height)
    }
}

internal const val PROFILE_PLACEHOLDER = "Здесь появится профиль участка: подъёмы, спуски и высота по ходу движения."

/** Экран «Профиль пути» на весь экран. Пока заготовка с пояснением. */
@Composable
internal fun ProfileFullScreen(onDismiss: () -> Unit) {
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
                Text(
                    text = "Профиль пути",
                    style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                    color = Pst.text,
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = PROFILE_PLACEHOLDER + "\n\nЭтот раздел в разработке.",
                    style = Pst.sans.copy(fontSize = 15.sp, lineHeight = 21.sp),
                    color = Pst.textMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Кнопка записи внизу карты: значок состояния и подпись «Начать запись» / «Остановить запись». */
@Composable
internal fun RecordButton(recording: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(48.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(Pst.surface, RoundedCornerShape(16.dp))
            .clickable(onClickLabel = if (recording) "Остановить запись" else "Начать запись", onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        RecordIcon(recording)
        Text(
            text = if (recording) "Остановить запись" else "Начать запись",
            style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            color = Pst.text,
            maxLines = 1,
        )
    }
}

/** Кнопка внизу экрана вместо скрытой полосы ([label]: «Скорость» / «Профиль»); нажатие возвращает полосу. */
@Composable
internal fun StripShowChip(label: String, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(48.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(Pst.surface, RoundedCornerShape(16.dp))
            .clickable(onClickLabel = description, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        PstIcon(PstIcons.ChevronUp, 20.dp, Pst.text)
        Text(text = label, style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = Pst.text, maxLines = 1)
    }
}
