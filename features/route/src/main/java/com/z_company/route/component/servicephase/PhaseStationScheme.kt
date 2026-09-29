package com.z_company.route.component.servicephase

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class StopType { TERMINAL, STATION, HALT }
internal enum class SchemeMode { VIEW, EDIT, PICK }

private val LineColumn = 32.dp

/**
 * Строка схемы участка (PstStopRow). Крайние станции — [StopType.TERMINAL], для них
 * [isStart] выбирает подпись «отправление» / «прибытие», а [terminalKm] — целые км прибытия.
 */
@Composable
internal fun PhaseStopRow(
    name: String,
    type: StopType,
    isFirst: Boolean,
    isLast: Boolean,
    mode: SchemeMode,
    modifier: Modifier = Modifier,
    isStart: Boolean = false,
    km: Double? = null,
    terminalKm: Int? = null,
    esr: String? = null,
    noCoordinates: Boolean = false,
    already: Boolean = false,
    checked: Boolean = false,
    lifted: Boolean = false,
    fresh: Boolean = false,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    dragHandle: (@Composable () -> Unit)? = null,
) {
    val terminal = type == StopType.TERMINAL
    val halt = type == StopType.HALT
    val lineColor = Pst.borderStrong
    val rowShape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .graphicsLayer {
                if (lifted) {
                    translationX = 4.dp.toPx()
                    scaleX = 1.015f
                    scaleY = 1.015f
                }
            }
            .then(if (lifted) Modifier.shadow(8.dp, rowShape) else Modifier)
            .background(
                when {
                    lifted -> Pst.surface
                    fresh -> Pst.accentSoft
                    else -> Color.Transparent
                },
                if (lifted || fresh) rowShape else RoundedCornerShape(0.dp),
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        // Колонка линии: пунктир + точка по центру строки
        Box(modifier = Modifier.width(LineColumn).fillMaxHeight(), contentAlignment = Alignment.Center) {
            if (!lifted) {
                Canvas(modifier = Modifier.fillMaxHeight().width(LineColumn)) {
                    val x = size.width / 2
                    val top = if (isFirst) size.height / 2 else 0f
                    val bottom = if (isLast) size.height / 2 else size.height
                    if (bottom > top) {
                        drawLine(
                            color = lineColor,
                            start = Offset(x, top),
                            end = Offset(x, bottom),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                        )
                    }
                }
            }
            StopDot(type)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = if (halt) 48.dp else 56.dp)
                .then(
                    if (!isLast && !lifted) Modifier.drawBehind {
                        drawLine(
                            color = lineColor.copy(alpha = 0.35f),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 0.5.dp.toPx(),
                        )
                    } else Modifier
                )
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (mode == SchemeMode.PICK && !terminal) {
                PstCheckbox(checked = already || checked, disabled = already)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = name,
                    style = Pst.sans.copy(
                        fontSize = if (halt) 14.sp else 15.sp,
                        fontWeight = if (halt) FontWeight.Medium else FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp,
                        lineHeight = 19.sp,
                    ),
                    color = if (halt) Pst.textMuted else Pst.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                StopSubline(
                    terminal = terminal,
                    isStart = isStart,
                    halt = halt,
                    esr = esr,
                    noCoordinates = noCoordinates && !terminal,
                    already = already,
                )
            }
            Text(
                text = when {
                    terminal && isStart -> "0 км"
                    terminal && terminalKm != null -> "$terminalKm км"
                    else -> pstKm(km)
                },
                style = Pst.mono.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                color = if (!terminal && km == null) Pst.textFaint else Pst.textMuted,
            )
            when (mode) {
                SchemeMode.VIEW -> if (terminal || onClick == null) {
                    Spacer(Modifier.width(14.dp))
                } else {
                    PstIcon(PstIcons.ChevronRight, 14.dp, Pst.textFaint)
                }
                SchemeMode.EDIT -> if (terminal) {
                    Spacer(Modifier.width(68.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 0.dp)) {
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 40.dp)
                                .clickable(enabled = onRemove != null) { onRemove?.invoke() },
                            contentAlignment = Alignment.Center,
                        ) { PstIcon(PstIcons.Close, 18.dp, Pst.textMuted, description = "Убрать") }
                        Box(modifier = Modifier.size(width = 36.dp, height = 40.dp), contentAlignment = Alignment.Center) {
                            dragHandle?.invoke()
                        }
                    }
                }
                SchemeMode.PICK -> Unit
            }
        }
    }
}

@Composable
private fun StopDot(type: StopType) {
    when (type) {
        StopType.TERMINAL -> Box(Modifier.size(14.dp).background(Pst.text, CircleShape))
        StopType.STATION -> Box(
            Modifier.size(12.dp).background(Pst.surface, CircleShape).border(2.dp, Pst.borderStrong, CircleShape)
        )
        StopType.HALT -> Box(
            Modifier.size(8.dp).background(Pst.surface, CircleShape).border(1.5.dp, Pst.textFaint, CircleShape)
        )
    }
}

@Composable
private fun StopSubline(
    terminal: Boolean,
    isStart: Boolean,
    halt: Boolean,
    esr: String?,
    noCoordinates: Boolean,
    already: Boolean,
) {
    val main = when {
        terminal -> if (isStart) "отправление" else "прибытие"
        halt -> "О.П."
        esr.isNullOrBlank() -> "станция"
        else -> "станция · ЕСР $esr"
    }
    val style = Pst.mono.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = main, style = style, color = Pst.textFaint, maxLines = 1)
        if (noCoordinates) Text(text = "нет координат", style = style, color = Pst.warning, maxLines = 1)
        if (already) Text(text = "уже в списке", style = style, color = Pst.accent, maxLines = 1)
    }
}

/** Кнопка «+ вставить» на линии между соседними станциями (режим «Изменить»). */
@Composable
internal fun PhaseInsertRow(active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val lineColor = Pst.borderStrong
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(LineColumn).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxHeight().width(LineColumn)) {
                drawLine(
                    color = lineColor,
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                )
            }
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(if (active) Pst.accent else Pst.surface, CircleShape)
                    .then(if (active) Modifier else Modifier.border(1.5.dp, Pst.accent.copy(alpha = 0.5f), CircleShape)),
                contentAlignment = Alignment.Center,
            ) { PstIcon(PstIcons.Plus, 12.dp, if (active) Pst.onAccent else Pst.accent) }
        }
        Text(
            text = if (active) "Вставить станцию сюда" else "вставить",
            style = Pst.sans.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            color = if (active) Pst.accent else Pst.textFaint,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
