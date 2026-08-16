package com.z_company.route.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.custom.AppTheme
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.route.component.AnimatedCounter
import com.z_company.route.viewmodel.home_view_model.RestBlockState
import kotlinx.coroutines.delay

/**
 * Блоки состояния «Отдых» на главном экране (в ПО и домашний).
 *
 * Оформлены единой карточкой по аналогии с блоком «Следующий маршрут»:
 *  - «ОТДЫХАЕТЕ» + живой счётчик прошедшего времени,
 *  - начало отдыха,
 *  - шкала прогресса (для ПО — с точками короткого и полного отдыха),
 *  - строки «до …» с остатком времени.
 *
 * Живые счётчики считаются от текущего времени, обновляемого раз в секунду.
 * Приоритет показа блоков решается в [HomeScreen].
 */

/** Тикающее текущее время (обновляется ~раз в секунду), пока блок на экране. */
@Composable
private fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    return now
}

@Composable
private fun RestGroupHeader(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RestElapsedHeader(restStart: Long, now: Long, dateAndTimeConverter: DateAndTimeConverter?) {
    Text(
        text = "ОТДЫХАЕТЕ",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(4.dp))
    AnimatedCounter(
        count = ConverterLongToTime.getTimeInStringFormat((now - restStart).coerceAtLeast(0L)),
        style = MaterialTheme.typography.headlineLarge.copy(
            fontFamily = MonoFont,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp,
        ),
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "начало отдыха ${dateAndTimeConverter?.getDateMiniAndTime(restStart) ?: ""}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Строка «Короткий/Полный отдых · до … · осталось …». */
@Composable
private fun RestRow(
    title: String,
    until: Long,
    now: Long,
    dateAndTimeConverter: DateAndTimeConverter?,
) {
    val titleText: @Composable () -> Unit = {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.W700),
            color = MaterialTheme.colorScheme.primary,
        )
    }
    val untilText: @Composable () -> Unit = {
        Text(
            text = "до ${dateAndTimeConverter?.getDateMiniAndTime(until) ?: ""}",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    val remainingRow: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "осталось ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = ConverterLongToTime.getTimeInStringFormat((until - now).coerceAtLeast(0L)),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = MonoFont,
                    fontWeight = FontWeight.W700,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    // При крупном шрифте название, остаток и срок окончания не помещаются в ряд —
    // раскладываем всё в столбец на всю ширину. Стандартный шрифт — прежний ряд.
    if (LocalDensity.current.fontScale > 1.15f) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            titleText()
            remainingRow()
            untilText()
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                titleText()
                untilText()
            }
            remainingRow()
        }
    }
}

/** Блок «Отдых в ПО»: шкала с точками короткого и полного отдыха + две строки. */
@Composable
fun RestPointOfTurnoverBlock(
    state: RestBlockState,
    dateAndTimeConverter: DateAndTimeConverter?,
    modifier: Modifier = Modifier,
) {
    val now = rememberNow()
    val successColor = AppTheme.colors.accentGreen
    val warningColor = AppTheme.colors.warnOrange

    val total = (state.fullRestEnd - state.restStart).toFloat().coerceAtLeast(1f)
    val progress = ((now - state.restStart) / total).coerceIn(0f, 1f)
    val shortFraction = state.shortRestEnd
        ?.let { ((it - state.restStart) / total).coerceIn(0f, 1f) }

    Column(modifier = modifier.fillMaxWidth()) {
        RestGroupHeader("ОТДЫХ В ПУНКТЕ ОБОРОТА")
        Card(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                RestElapsedHeader(state.restStart, now, dateAndTimeConverter)

                Spacer(modifier = Modifier.height(20.dp))
                RestProgressBar(
                    progress = progress,
                    dotFraction = shortFraction,
                    dotColor = successColor,
                    endDotColor = warningColor,
                )
                Spacer(modifier = Modifier.height(8.dp))
                RestTimelineLabels(
                    startLabel = dateAndTimeConverter?.getTime(state.restStart) ?: "",
                    midLabel = state.shortRestEnd?.let { dateAndTimeConverter?.getTime(it) } ?: "",
                    midFraction = shortFraction,
                    endLabel = dateAndTimeConverter?.getTime(state.fullRestEnd) ?: "",
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                state.shortRestEnd?.let { shortEnd ->
                    RestRow("Короткий отдых", shortEnd, now, dateAndTimeConverter)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                RestRow("Полный отдых", state.fullRestEnd, now, dateAndTimeConverter)
            }
        }
    }
}

/** Блок «Домашний отдых»: одна граница (полный отдых). */
@Composable
fun HomeRestBlock(
    state: RestBlockState,
    dateAndTimeConverter: DateAndTimeConverter?,
    modifier: Modifier = Modifier,
) {
    val now = rememberNow()
    val warningColor = AppTheme.colors.warnOrange

    val total = (state.fullRestEnd - state.restStart).toFloat().coerceAtLeast(1f)
    val progress = ((now - state.restStart) / total).coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        RestGroupHeader("ДОМАШНИЙ ОТДЫХ")
        Card(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                RestElapsedHeader(state.restStart, now, dateAndTimeConverter)

                Spacer(modifier = Modifier.height(20.dp))
                RestProgressBar(
                    progress = progress,
                    dotFraction = null,
                    dotColor = warningColor,
                    endDotColor = warningColor,
                )
                Spacer(modifier = Modifier.height(8.dp))
                RestTimelineLabels(
                    startLabel = dateAndTimeConverter?.getTime(state.restStart) ?: "",
                    midLabel = "",
                    midFraction = null,
                    endLabel = dateAndTimeConverter?.getTime(state.fullRestEnd) ?: "",
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                RestRow("Полный отдых", state.fullRestEnd, now, dateAndTimeConverter)
            }
        }
    }
}

/** Шкала прогресса высотой 3dp с опциональной точкой (короткий отдых) и конечной точкой. */
@Composable
private fun RestProgressBar(
    progress: Float,
    dotFraction: Float?,
    dotColor: androidx.compose.ui.graphics.Color,
    endDotColor: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(9.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        // Дорожка
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        // Заполнение
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.tertiary),
        )
        // Точка короткого отдыха
        dotFraction?.let { f ->
            Dot(fraction = f, color = dotColor)
        }
        // Точка полного отдыха (конец)
        Dot(fraction = 1f, color = endDotColor)
    }
}

/** Точка на шкале, позиционируемая по доле ширины [fraction]. */
@Composable
private fun Dot(fraction: Float, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val x = ((constraints.maxWidth - placeable.width) * fraction).toInt()
                layout(placeable.width, placeable.height) {
                    placeable.place(x, 0)
                }
            }
            .height(9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(9.dp)
                .width(9.dp)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

/** Подписи под шкалой: начало / (короткий) / полный. */
@Composable
private fun RestTimelineLabels(
    startLabel: String,
    midLabel: String,
    midFraction: Float?,
    endLabel: String,
) {
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.align(Alignment.CenterStart),
            text = startLabel,
            style = labelStyle,
            color = labelColor,
        )
        if (midLabel.isNotEmpty() && midFraction != null) {
            Text(
                modifier = Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val x = ((constraints.maxWidth - placeable.width) * midFraction).toInt()
                    layout(placeable.width, placeable.height) { placeable.place(x, 0) }
                },
                text = midLabel,
                style = labelStyle.copy(fontWeight = FontWeight.W700),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            modifier = Modifier.align(Alignment.CenterEnd),
            text = endLabel,
            style = labelStyle,
            color = labelColor,
        )
    }
}
