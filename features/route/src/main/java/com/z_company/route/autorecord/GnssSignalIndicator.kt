package com.z_company.route.autorecord

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.z_company.core.gps.GpsRecorder
import com.z_company.domain.gnss.GnssLevel
import com.z_company.domain.gnss.GnssQuality
import com.z_company.domain.gnss.GnssQualityText
import kotlinx.coroutines.delay

/**
 * Цвета и число «штрихов» индикатора связи со спутниками (SCREEN_SPECS §6.9). Цвета подобраны под контраст ≥ 3:1
 * (графический элемент) к фону экрана/карточки в обеих темах: зелёный - хороший, жёлтый - слабый, красный - нет
 * сигнала, серый - ещё не известно.
 */
object GnssSignalStyle {
    fun color(level: GnssLevel, dark: Boolean): Color = when (level) {
        GnssLevel.GOOD -> if (dark) Color(0xFF4ADE80) else Color(0xFF15803D)
        GnssLevel.WEAK -> if (dark) Color(0xFFFBBF24) else Color(0xFFB45309)
        GnssLevel.LOST -> if (dark) Color(0xFFF87171) else Color(0xFFB91C1C)
        GnssLevel.UNKNOWN -> if (dark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
        // Помеха: оранжевый (отличим от жёлтого WEAK и красного LOST).
        GnssLevel.INTERFERENCE -> if (dark) Color(0xFFFB923C) else Color(0xFFC2410C)
    }

    /** Сколько из трёх штрихов залито: 3 - хороший, 2 - слабый, 0 - нет сигнала / неизвестно. */
    fun bars(level: GnssLevel): Int = when (level) {
        GnssLevel.GOOD -> 3
        GnssLevel.WEAK -> 2
        else -> 0
    }
}

/**
 * Иконка уровня сигнала: три штриха растущей высоты. При потере сигнала - перечёркнута. Тап показывает подсказку
 * ([GnssQualityText.tooltip]) на 4 секунды.
 */
@Composable
fun GnssSignalIndicator(
    quality: GnssQuality,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val color = GnssSignalStyle.color(quality.level, dark)
    val filled = GnssSignalStyle.bars(quality.level)
    val lost = quality.level == GnssLevel.LOST
    val interference = quality.level == GnssLevel.INTERFERENCE
    var showTip by remember { mutableStateOf(false) }
    LaunchedEffect(showTip) {
        if (showTip) {
            delay(4_000)
            showTip = false
        }
    }
    val description = GnssQualityText.description(quality)
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 44.dp, minHeight = 52.dp)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClickLabel = "Пояснить уровень сигнала GPS") { showTip = true },
        contentAlignment = Alignment.Center,
    ) {
        // Горизонтальные отступы: подпись «Помеха GPS» шире иконки и не должна упираться в края плашки.
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val barW = size.width * 0.22f
            val gap = (size.width - 3 * barW) / 2f
            for (i in 0 until 3) {
                val h = size.height * (0.4f + 0.3f * i)
                val on = i < filled || lost || interference
                drawRoundRect(
                    color = if (on) color else color.copy(alpha = 0.3f),
                    topLeft = Offset(i * (barW + gap), size.height - h),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(barW / 3f),
                )
            }
            if (lost) {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, 0f),
                    strokeWidth = size.width * 0.12f,
                    cap = StrokeCap.Round,
                )
            }
        }
        Text(
            text = if (interference) "Помеха GPS" else "GPS",
            fontSize = 10.sp, lineHeight = 12.sp,
            color = if (interference) color else labelColor,
            maxLines = 1, softWrap = false,
            modifier = Modifier.padding(top = 2.dp),
        )
        }
        if (showTip) {
            val px = with(androidx.compose.ui.platform.LocalDensity.current) { 44.dp.roundToPx() }
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, px),
                onDismissRequest = { showTip = false },
                properties = PopupProperties(focusable = false, dismissOnClickOutside = true),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shadowElevation = 4.dp,
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text(
                        text = GnssQualityText.tooltip(quality),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
