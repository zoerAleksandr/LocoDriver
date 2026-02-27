package com.z_company.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ─── Модель данных для отображения ───────────────────────────────────────────

/**
 * Элемент таймлайна — станция с временами прибытия/отправления.
 */
data class StationTimelineItem(
    val stationName: String,
    val timeArrival: Long?,     // millis
    val timeDeparture: Long?,   // millis
)

/**
 * Информация о перегоне между станциями.
 */
private data class SegmentInfo(
    val durationMillis: Long,
)

/**
 * Информация о стоянке на станции.
 */
private data class StopInfo(
    val durationMillis: Long,
)

// ─── Утилиты форматирования ──────────────────────────────────────────────────

/**
 * Форматирует длительность в минутах/часах.
 * Примеры: "5 мин", "1 ч 23 мин", "0 мин"
 */
private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0 мин"
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "$hours ч $minutes мин"
        hours > 0 -> "$hours ч"
        else -> "$minutes мин"
    }
}

/**
 * Форматирует timestamp в "HH:mm".
 */
private fun formatTime(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}

/**
 * Вычисляет время стоянки на станции (разница между отправлением и прибытием).
 */
private fun calculateStop(item: StationTimelineItem): StopInfo? {
    val arrival = item.timeArrival ?: return null
    val departure = item.timeDeparture ?: return null
    val diff = departure - arrival
    return if (diff > 0) StopInfo(diff) else null
}

/**
 * Вычисляет перегонное время между двумя станциями.
 * Перегон = прибытие на следующую станцию − отправление с предыдущей.
 */
private fun calculateSegment(
    from: StationTimelineItem,
    to: StationTimelineItem,
): SegmentInfo? {
    val departure = from.timeDeparture ?: from.timeArrival ?: return null
    val arrival = to.timeArrival ?: to.timeDeparture ?: return null
    val diff = arrival - departure
    return if (diff >= 0) SegmentInfo(diff) else null
}

// ─── Цвета таймлайна ────────────────────────────────────────────────────────

/**
 * Цвета для кастомизации таймлайна.
 */
data class TimelineColors(
    val lineColor: Color,
    val dotColor: Color,
    val dotBorderColor: Color,
    val segmentTextColor: Color,
    val segmentBackgroundColor: Color,
    val stopBadgeColor: Color,
    val stopBadgeTextColor: Color,
    val stationNameColor: Color,
    val timeColor: Color,
    val firstLastDotColor: Color,
)

// ─── Основной компонент ──────────────────────────────────────────────────────

/**
 * Вертикальный таймлайн маршрута поезда со станциями, перегонным временем
 * и временем стоянки.
 *
 * @param stations Список станций с временами.
 * @param modifier Модификатор компонента.
 * @param colors Цвета таймлайна (по умолчанию берутся из MaterialTheme).
 */
@Composable
fun TrainStationTimeline(
    stations: List<StationTimelineItem>,
    modifier: Modifier = Modifier,
    colors: TimelineColors = defaultTimelineColors(),
) {
    if (stations.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        stations.forEachIndexed { index, station ->
            val isFirst = index == 0
            val isLast = index == stations.lastIndex

            // ── Строка станции ──
            StationRow(
                item = station,
                isFirst = isFirst,
                isLast = isLast,
                colors = colors,
            )

            // ── Перегон между станциями ──
            if (!isLast) {
                val nextStation = stations[index + 1]
                val segment = calculateSegment(station, nextStation)
                SegmentRow(
                    segment = segment,
                    colors = colors,
                )
            }
        }
    }
}

// ─── Строка станции ──────────────────────────────────────────────────────────

@Composable
private fun StationRow(
    item: StationTimelineItem,
    isFirst: Boolean,
    isLast: Boolean,
    colors: TimelineColors,
) {
    val stopInfo = calculateStop(item)
    val dotColor = if (isFirst || isLast) colors.firstLastDotColor else colors.dotColor

    // Размеры адаптируются через dp — масштабируются с системными настройками
    val dotSize = 12.dp
    val lineWidth = 2.dp
    val timelineColumnWidth = 40.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── Колонка таймлайна (линия + точка) ──
        TimelineDot(
            dotSize = dotSize,
            lineWidth = lineWidth,
            columnWidth = timelineColumnWidth,
            dotColor = dotColor,
            dotBorderColor = colors.dotBorderColor,
            lineColor = colors.lineColor,
            showTopLine = !isFirst,
            showBottomLine = !isLast,
        )

        Spacer(modifier = Modifier.width(8.dp))

        // ── Название станции ──
        Text(
            text = item.stationName.ifBlank { "—" },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isFirst || isLast) FontWeight.SemiBold else FontWeight.Normal,
            color = colors.stationNameColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(4.dp))

        // ── Время прибытия ──
        TimeText(
            millis = item.timeArrival,
            color = colors.timeColor,
        )

        // ── Бейдж стоянки ──
        if (stopInfo != null && stopInfo.durationMillis > 0) {
            StopBadge(
                stopInfo = stopInfo,
                badgeColor = colors.stopBadgeColor,
                textColor = colors.stopBadgeTextColor,
            )
        } else {
            // Пустое место для выравнивания
            Spacer(modifier = Modifier.width(48.dp))
        }

        // ── Время отправления ──
        TimeText(
            millis = item.timeDeparture,
            color = colors.timeColor,
        )
    }
}

// ─── Перегонная строка ───────────────────────────────────────────────────────

@Composable
private fun SegmentRow(
    segment: SegmentInfo?,
    colors: TimelineColors,
) {
    val lineWidth = 2.dp
    val timelineColumnWidth = 40.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── Вертикальная линия (пунктирная) ──
        TimelineDashedLine(
            columnWidth = timelineColumnWidth,
            lineWidth = lineWidth,
            lineColor = colors.lineColor,
        )

        Spacer(modifier = Modifier.width(8.dp))

        // ── Перегонное время ──
        if (segment != null && segment.durationMillis > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.segmentBackgroundColor)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "▸ ${formatDuration(segment.durationMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.segmentTextColor,
                )
            }
        }
    }
}

// ─── Вспомогательные компоненты ──────────────────────────────────────────────

/**
 * Точка таймлайна с линиями вверх/вниз.
 */
@Composable
private fun TimelineDot(
    dotSize: Dp,
    lineWidth: Dp,
    columnWidth: Dp,
    dotColor: Color,
    dotBorderColor: Color,
    lineColor: Color,
    showTopLine: Boolean,
    showBottomLine: Boolean,
) {
    val density = LocalDensity.current
    val dotSizePx = with(density) { dotSize.toPx() }
    val lineWidthPx = with(density) { lineWidth.toPx() }
    val borderWidth = with(density) { 2.dp.toPx() }

    Box(
        modifier = Modifier
            .width(columnWidth)
            .height(40.dp), // Минимальная высота строки станции
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2

            // Линия вверх
            if (showTopLine) {
                drawLine(
                    color = lineColor,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, centerY - dotSizePx / 2),
                    strokeWidth = lineWidthPx,
                    cap = StrokeCap.Round,
                )
            }

            // Линия вниз
            if (showBottomLine) {
                drawLine(
                    color = lineColor,
                    start = Offset(centerX, centerY + dotSizePx / 2),
                    end = Offset(centerX, size.height),
                    strokeWidth = lineWidthPx,
                    cap = StrokeCap.Round,
                )
            }

            // Внешний круг (бордер)
            drawCircle(
                color = dotBorderColor,
                radius = dotSizePx / 2 + borderWidth / 2,
                center = Offset(centerX, centerY),
            )

            // Внутренний круг (заливка)
            drawCircle(
                color = dotColor,
                radius = dotSizePx / 2,
                center = Offset(centerX, centerY),
            )
        }
    }
}

/**
 * Пунктирная вертикальная линия для перегона.
 */
@Composable
private fun TimelineDashedLine(
    columnWidth: Dp,
    lineWidth: Dp,
    lineColor: Color,
) {
    val density = LocalDensity.current
    val lineWidthPx = with(density) { lineWidth.toPx() }
    val dashLengthPx = with(density) { 4.dp.toPx() }
    val gapLengthPx = with(density) { 3.dp.toPx() }

    Box(
        modifier = Modifier
            .width(columnWidth)
            .defaultMinSize(minHeight = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2

            drawLine(
                color = lineColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, size.height),
                strokeWidth = lineWidthPx,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(dashLengthPx, gapLengthPx),
                    0f,
                ),
            )
        }
    }
}

/**
 * Текст времени (HH:mm). Если время null — показывает "—".
 */
@Composable
private fun TimeText(
    millis: Long?,
    color: Color,
) {
    Text(
        text = if (millis != null) formatTime(millis) else "—",
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(min = 48.dp),
    )
}

/**
 * Бейдж стоянки — жёлтый с временем стоянки в минутах.
 * Компактный вид: "5'" для коротких стоянок, "1ч 5'" для длинных.
 */
@Composable
private fun StopBadge(
    stopInfo: StopInfo,
    badgeColor: Color,
    textColor: Color,
) {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(stopInfo.durationMillis)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    // Компактный формат: "5'", "1ч5'"
    val text = when {
        hours > 0 && minutes > 0 -> "${hours}ч${minutes}'"
        hours > 0 -> "${hours}ч"
        else -> "${minutes}'"
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(badgeColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}

// ─── Цвета по умолчанию ─────────────────────────────────────────────────────

@Composable
private fun defaultTimelineColors(): TimelineColors {
    val scheme = MaterialTheme.colorScheme
    return TimelineColors(
        lineColor = scheme.outline.copy(alpha = 0.4f),
        dotColor = scheme.surface,
        dotBorderColor = scheme.primary,
        segmentTextColor = scheme.onSurfaceVariant,
        segmentBackgroundColor = scheme.surfaceVariant.copy(alpha = 0.5f),
        stopBadgeColor = Color(0xFFFFC107),       // Жёлтый как на скриншоте
        stopBadgeTextColor = Color(0xFF3E2723),    // Тёмно-коричневый
        stationNameColor = scheme.primary,
        timeColor = scheme.primary,
        firstLastDotColor = scheme.primary,
    )
}

// ─── Функция-адаптер из доменной модели Station ──────────────────────────────

/**
 * Конвертирует список доменных Station в список StationTimelineItem
 * для отображения в таймлайне.
 */
fun List<com.z_company.domain.entities.route.Station>.toTimelineItems(): List<StationTimelineItem> {
    return map { station ->
        StationTimelineItem(
            stationName = station.stationName ?: "",
            timeArrival = station.timeArrival,
            timeDeparture = station.timeDeparture,
        )
    }
}
