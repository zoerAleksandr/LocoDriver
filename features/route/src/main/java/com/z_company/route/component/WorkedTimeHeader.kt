package com.z_company.route.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BasicTooltipBox
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.rememberBasicTooltipState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Заголовок инфо-блока: главное время + опционально разбивка `(X + Y)` + опциональный чип.
 *
 * Адаптивный layout (одинаковый для всех 3 блоков HomeScreen):
 *  1. Если всё умещается в одну строку → `[time breakdown ........ chip]`
 *  2. Если чип не помещается рядом → чип уезжает в строку выше у правого края,
 *     `[time breakdown]` остаётся на полной ширине ниже
 *  3. Если даже без чипа `time + breakdown` шире экрана → переносим breakdown
 *     на следующую строку под time. Чип (если есть) идёт первой строкой справа.
 *
 * Это решает баг, когда длинная строка `205:05 (188:50 + 16:15)` уходила
 * за правую границу карточки.
 *
 * @param time главное число (например, "205:05") — обязательное
 * @param breakdownText опциональная разбивка в скобках (например, " (188:50 + 16:15)")
 *   передавать с ведущим пробелом для inline-отображения
 * @param chipText опциональный текст чипа справа (например, "сверх 21:50")
 * @param tooltipForTime текст подсказки при тапе на time
 * @param tooltipForBreakdown текст подсказки при тапе на breakdown
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WorkedTimeHeader(
    time: String,
    breakdownText: String? = null,
    chipText: String? = null,
    tooltipForTime: String = "Общее отработанное время",
    tooltipForBreakdown: String = "Рабочие + праздничные часы",
    modifier: Modifier = Modifier,
) {
    val tooltipPosition = TooltipDefaults.rememberPlainTooltipPositionProvider()
    val tooltipState = rememberBasicTooltipState(isPersistent = false)
    val scope = rememberCoroutineScope()
    var tooltipText by remember { mutableStateOf("") }

    BasicTooltipBox(
        modifier = modifier.fillMaxWidth(),
        positionProvider = tooltipPosition,
        tooltip = {
            Box(
                modifier = Modifier
                    .background(
                        shape = Shapes.medium,
                        color = MaterialTheme.colorScheme.surface
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = tooltipText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        state = tooltipState
    ) {
        // Слоты композим всегда с фиксированным порядком, "пустые" не эмитят ничего.
        // measurables.size = (time, breakdown?, chip?), порядок проверяем по флагам.
        val hasBreakdown = !breakdownText.isNullOrEmpty()
        val hasChip = !chipText.isNullOrEmpty()

        Layout(
            modifier = Modifier.fillMaxWidth(),
            content = {
                // 0: time
                Text(
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            scope.launch {
                                tooltipText = tooltipForTime
                                tooltipState.show(MutatePriority.Default)
                            }
                        })
                    },
                    text = time,
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                // 1: breakdown (опционально)
                if (hasBreakdown) {
                    Text(
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(onPress = {
                                scope.launch {
                                    tooltipText = tooltipForBreakdown
                                    tooltipState.show(MutatePriority.Default)
                                }
                            })
                        },
                        text = breakdownText!!,
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // 2: chip (опционально)
                if (hasChip) {
                    Surface(
                        shape = Shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceDim,
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            text = chipText!!,
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        ) { measurables, constraints ->
            val gap = 8.dp.roundToPx()
            val maxW = constraints.maxWidth

            // Измеряем все слоты без ограничения по ширине — нужен натуральный размер
            val infinite = constraints.copy(minWidth = 0, maxWidth = Int.MAX_VALUE)
            val timeP = measurables[0].measure(infinite)
            var idx = 1
            val breakdownP = if (hasBreakdown && idx < measurables.size) measurables[idx++].measure(infinite) else null
            val chipP = if (hasChip && idx < measurables.size) measurables[idx].measure(infinite) else null

            val timeW = timeP.width
            val timeH = timeP.height
            val breakdownW = breakdownP?.width ?: 0
            val breakdownH = breakdownP?.height ?: 0
            val chipW = chipP?.width ?: 0
            val chipH = chipP?.height ?: 0

            // Определяем placement: chip на той же строке или выше
            val totalOneLine = timeW + breakdownW + (if (chipP != null) gap + chipW else 0)
            val chipOnSameRow = chipP == null || totalOneLine <= maxW

            // Доступная ширина для time+breakdown на их строке
            val widthForTimeRow = if (chipOnSameRow && chipP != null) maxW - chipW - gap else maxW
            val breakdownOnSameRow = breakdownP == null || (timeW + breakdownW <= widthForTimeRow)

            when {
                chipOnSameRow && breakdownOnSameRow -> {
                    // Кейс A: всё в одну строку  [time breakdown ........ chip]
                    val rowH = max(timeH, max(breakdownH, chipH))
                    layout(maxW, rowH) {
                        timeP.place(0, (rowH - timeH) / 2)
                        breakdownP?.place(timeW, (rowH - breakdownH) / 2)
                        chipP?.place(maxW - chipW, (rowH - chipH) / 2)
                    }
                }
                !chipOnSameRow && breakdownOnSameRow -> {
                    // Кейс B: чип сверху-справа, [time breakdown] на полной ширине ниже
                    val timeRowH = max(timeH, breakdownH)
                    layout(maxW, chipH + timeRowH) {
                        chipP!!.place(maxW - chipW, 0)
                        timeP.place(0, chipH + (timeRowH - timeH) / 2)
                        breakdownP?.place(timeW, chipH + (timeRowH - breakdownH) / 2)
                    }
                }
                chipOnSameRow && !breakdownOnSameRow -> {
                    // Кейс C: chip == null (или вмещается с time, но без breakdown).
                    // breakdown переносим на свою строку под time.
                    // Ширина top-row: max(time, chip) — чтобы chip не выпал из layout-bounds.
                    val topRowH = max(timeH, chipH)
                    layout(maxW, topRowH + breakdownH) {
                        timeP.place(0, (topRowH - timeH) / 2)
                        chipP?.place(maxW - chipW, (topRowH - chipH) / 2)
                        breakdownP!!.place(0, topRowH)
                    }
                }
                else -> {
                    // Кейс D: чип сверху, time на следующей строке, breakdown на третьей
                    layout(maxW, chipH + timeH + breakdownH) {
                        chipP!!.place(maxW - chipW, 0)
                        timeP.place(0, chipH)
                        breakdownP!!.place(0, chipH + timeH)
                    }
                }
            }
        }
    }
}
