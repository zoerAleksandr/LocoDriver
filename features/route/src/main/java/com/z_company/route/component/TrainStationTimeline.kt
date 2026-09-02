package com.z_company.route.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.util.TimeManager
import com.z_company.domain.entities.route.UtilsForEntities
import com.z_company.route.viewmodel.StationFormState
import java.util.concurrent.TimeUnit

// ─── Константы ──────────────────────────────────────────────────────────────

private val TimelineColumnWidth = 24.dp
private val ContentPadding = 4.dp
private val DotHeight = 40.dp
private val StopBadgeWidth = 56.dp
private val DangerColor = Color(0xFFEF5350)

// ─── Модель данных для отображения ───────────────────────────────────────────

data class StationTimelineItem(
    val id: String = "",
    val stationName: String,
    val timeArrival: Long?,
    val timeDeparture: Long?,
    val trackNumber: String? = null,
    val isFinalStation: Boolean = false,
    // Проходная станция — без остановки; показывается только timeArrival
    // («время проследования»), по центру ширины прибытия+отправления, а
    // название станции — приглушённым цветом.
    val isPassingStation: Boolean = false,
    // Путь/примечание перегона ПЕРЕД этой станцией (между предыдущей и этой).
    val segmentTrackNumber: String? = null,
    val segmentNotes: String? = null,
)

private data class SegmentInfo(val durationMillis: Long)
private data class StopInfo(val durationMillis: Long)

// ─── Утилиты форматирования ──────────────────────────────────────────────────

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

private val timeManager = TimeManager()

private fun formatTime(millis: Long): String = timeManager.formatTime(millis)

private fun calculateStop(item: StationTimelineItem): StopInfo? {
    val arrival = item.timeArrival ?: return null
    val departure = item.timeDeparture ?: return null
    val diff = (departure - departure % 60_000L) - (arrival - arrival % 60_000L)
    return if (diff > 0) StopInfo(diff) else null
}

private fun calculateSegment(
    from: StationTimelineItem,
    to: StationTimelineItem,
): SegmentInfo? {
    // Перегон = время между отправлением более ранней станции и прибытием более поздней.
    // Считаем независимо от порядка отображения (при развороте списка from — поздняя),
    // иначе разница отрицательная и перегон пропадает.
    val fromDeparture = from.timeDeparture ?: from.timeArrival ?: return null
    val fromArrival = from.timeArrival ?: from.timeDeparture ?: return null
    val toDeparture = to.timeDeparture ?: to.timeArrival ?: return null
    val toArrival = to.timeArrival ?: to.timeDeparture ?: return null

    val diff = if (fromDeparture <= toArrival) {
        // Обычный порядок: from раньше to
        toArrival - fromDeparture
    } else {
        // Развёрнутый порядок: from позже to
        fromArrival - toDeparture
    }
    return if (diff > 0) SegmentInfo(diff) else null
}

// ─── Цвета таймлайна ────────────────────────────────────────────────────────

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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TrainStationTimeline(
    stations: List<StationTimelineItem>,
    modifier: Modifier = Modifier,
    colors: TimelineColors = defaultTimelineColors(),
    trainNumber: String? = null,
    distance: Double? = null,
    reorderingStationId: String? = null,
    onStationClick: ((Int) -> Unit)? = null,
    onStationLongPress: ((Int) -> Unit)? = null,
    // Клик по перегону МЕЖДУ station[index] и station[index+1] — index здесь
    // индекс станции ПЕРЕД перегоном (т.е. данные перегона хранятся на station[index+1]).
    onSegmentClick: ((Int) -> Unit)? = null,
    onMoveUp: ((Int) -> Unit)? = null,
    onMoveDown: ((Int) -> Unit)? = null,
    onDismissReorder: (() -> Unit)? = null,
    onStationSwipeDelete: ((Int) -> Unit)? = null,
    closeSwipeSignal: Int = 0,
    rowBackgroundColor: Color = Color.Unspecified,
    // Показывать бейджи «Время в пути / Уч / Техн» внутри списка. Когда false —
    // они выносятся наружу отдельными пилюлями (см. RouteSummaryRow).
    showSummary: Boolean = true,
    // Индекс строки в ОТОБРАЖАЕМОМ списке, ПОСЛЕ которой рисуется «будущий
    // перегон» — тот, у которого вторая граница ещё неизвестна. -1 = над всем
    // списком (перевёрнутый порядок), null = не рисуем. Считается на экране,
    // т.к. зависит от выбранного плеча и порядка сортировки.
    futureSegmentAfterIndex: Int? = null,
    // Показывать карточки перегонов между станциями (переключается кнопкой
    // над блоком «Маршрут»).
    showSegments: Boolean = true,
) {
    if (stations.isEmpty()) return

    val isAnyReordering = reorderingStationId != null
    val rowBg = if (rowBackgroundColor == Color.Unspecified)
        MaterialTheme.colorScheme.background else rowBackgroundColor

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isAnyReordering && onDismissReorder != null) {
                    Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onDismissReorder() }
                } else Modifier
            )
            .animateContentSize()
    ) {
        // ── Шапка колонок ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ContentPadding)
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = isAnyReordering,
                enter = expandHorizontally(),
                exit = shrinkHorizontally()
            ) {
                Spacer(modifier = Modifier.width(48.dp))
            }
            Spacer(modifier = Modifier.width(TimelineColumnWidth + 8.dp))
            Text(
                text = "СТАНЦИЯ",
                style = ColumnHeaderStyle(),
                color = colors.stationNameColor.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "ПРИБ",
                style = ColumnHeaderStyle(),
                color = colors.stationNameColor.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(48.dp),
            )
            Spacer(modifier = Modifier.width(StopBadgeWidth))
            Text(
                text = "ОТПР",
                style = ColumnHeaderStyle(),
                color = colors.stationNameColor.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(48.dp),
            )
            if (onStationClick != null) {
                Spacer(modifier = Modifier.width(18.dp))
            }
        }

        if (showSegments && futureSegmentAfterIndex == -1) {
            SegmentCard(
                segment = null,
                segmentTrackNumber = null,
                segmentNotes = null,
                colors = colors,
                isAnyReordering = isAnyReordering,
                isFuture = true,
                onClick = null,
            )
        }

        stations.forEachIndexed { index, station ->
            val isFirst = index == 0
            val isLast = index == stations.lastIndex
            val isReordering = reorderingStationId == station.id

            key(station.id) {
                // ── Строка станции (со свайпом или без) ──
                // swipeOwnsClicks=true — клик/долгое нажатие обрабатывает SwipeToRevealDelete
                // (в одном жесте со свайпом), поэтому у самой строки clickable отключён.
                val stationRowContent = @Composable { swipeOwnsClicks: Boolean, revealed: Boolean ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        StationRow(
                            item = station,
                            index = index,
                            isFirst = isFirst,
                            isLast = isLast,
                            colors = colors,
                            trainNumber = trainNumber,
                            isAnyReordering = isAnyReordering,
                            isReordering = isReordering,
                            onStationClick = if (swipeOwnsClicks) null else onStationClick,
                            onStationLongPress = if (swipeOwnsClicks) null else onStationLongPress,
                            onMoveUp = if (!isFirst) onMoveUp else null,
                            onMoveDown = if (!isLast) onMoveDown else null,
                            rowBackgroundColor = rowBg,
                            revealed = revealed,
                        )
                    }
                }

                if (onStationSwipeDelete != null && !isAnyReordering) {
                    val currentIndex by rememberUpdatedState(index)
                    // Свайп влево раскрывает кнопку «Удалить» (как у секций локомотива) —
                    // строка не удаляется сразу, подтверждение показывается через UI state.
                    SwipeToRevealDelete(
                        modifier = Modifier.fillMaxWidth(),
                        onDeleteClick = { onStationSwipeDelete.invoke(currentIndex) },
                        closeSignal = closeSwipeSignal,
                        compact = true,
                        onContentClick = { onStationClick?.invoke(currentIndex) },
                        onContentLongClick = { onStationLongPress?.invoke(currentIndex) },
                    ) { revealed ->
                        stationRowContent(true, revealed)
                    }
                } else {
                    stationRowContent(false, false)
                }

                // ── Перегон между станциями ──
                // Если промежуток помечен «будущим» — вторая граница неизвестна,
                // рисуем пунктирный блок вместо обычного и не даём его открыть.
                if (showSegments) {
                    val isFuture = index == futureSegmentAfterIndex
                    if (isFuture) {
                        SegmentCard(
                            segment = null,
                            segmentTrackNumber = null,
                            segmentNotes = null,
                            colors = colors,
                            isAnyReordering = isAnyReordering,
                            isFuture = true,
                            onClick = null,
                        )
                    } else if (!isLast) {
                        val nextStation = stations[index + 1]
                        SegmentCard(
                            segment = calculateSegment(station, nextStation),
                            segmentTrackNumber = nextStation.segmentTrackNumber,
                            segmentNotes = nextStation.segmentNotes,
                            colors = colors,
                            isAnyReordering = isAnyReordering,
                            onClick = if (onSegmentClick != null) {
                                { onSegmentClick(index) }
                            } else null,
                        )
                    }
                }
            }
        }

        // ── Бейдж суммарного времени в пути ──
        if (showSummary && stations.size >= 2) {
            Spacer(modifier = Modifier.height(12.dp))
            val startTime = stations.first().timeDeparture ?: stations.first().timeArrival
            val endTime = stations.last().timeArrival ?: stations.last().timeDeparture
            if (startTime != null && endTime != null && endTime > startTime) {
                val totalMillis = endTime - startTime
                TotalTimeBadge(
                    totalMillis = totalMillis,
                    colors = colors,
                    isAnyReordering = isAnyReordering,
                )

                // ── Бейдж скоростей ──
                if (distance != null && distance > 0) {
                    val sectionSpeed = distance / (totalMillis / 3_600_000.0)
                    var stopTime = 0L
                    for (i in 1 until stations.size - 1) {
                        val arr = stations[i].timeArrival
                        val dep = stations[i].timeDeparture
                        if (arr != null && dep != null && dep > arr) {
                            stopTime += dep - arr
                        }
                    }
                    val runningTime = totalMillis - stopTime
                    val technicalSpeed =
                        if (runningTime > 0) distance / (runningTime / 3_600_000.0) else null

                    SpeedBadge(
                        sectionSpeed = sectionSpeed,
                        technicalSpeed = technicalSpeed,
                        colors = colors,
                        isAnyReordering = isAnyReordering,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnHeaderStyle() = MaterialTheme.typography.labelSmall.copy(
    fontFamily = MonoFont,
    fontWeight = FontWeight.Medium,
    letterSpacing = 1.sp,
)

// ─── Вычисление цвета бейджа стоянки ─────────────────────────────────────────

private fun computeStopBadgeColors(
    stopDurationMillis: Long,
    isPassengerTrain: Boolean,
    primaryColor: Color,
): Pair<Color, Color> {
    val stopMinutes = TimeUnit.MILLISECONDS.toMinutes(stopDurationMillis)
    return when {
        stopMinutes > 20 && isPassengerTrain -> DangerColor to Color.White
        stopMinutes > 30 -> DangerColor to Color.White
        stopMinutes > 5 -> Color(0xFFFFC107) to Color(0xFF3E2723)
        else -> Color.Transparent to primaryColor.copy(alpha = 0.8f)
    }
}

// ─── Строка станции ──────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StationRow(
    item: StationTimelineItem,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    colors: TimelineColors,
    trainNumber: String? = null,
    isAnyReordering: Boolean,
    isReordering: Boolean,
    onStationClick: ((Int) -> Unit)?,
    onStationLongPress: ((Int) -> Unit)?,
    onMoveUp: ((Int) -> Unit)?,
    onMoveDown: ((Int) -> Unit)?,
    rowBackgroundColor: Color = Color.Unspecified,
    revealed: Boolean = false,
) {
    // У конечной станции стоянка не имеет смысла: маршрут закончился.
    val stopInfo = if (item.isFinalStation) null else calculateStop(item)
    val dotColor = if (isFirst || isLast) colors.firstLastDotColor else colors.dotColor
    val primaryColor = MaterialTheme.colorScheme.primary

    val dotSize = 12.dp
    val lineWidth = 2.dp

    // Фон строки: обычный, а в свайпнутом (revealed) состоянии — красноватый,
    // чтобы вся карточка станции читалась как «готова к удалению».
    val baseRowColor = if (rowBackgroundColor == Color.Unspecified)
        MaterialTheme.colorScheme.background else rowBackgroundColor
    val targetRowColor = if (revealed)
        MaterialTheme.colorScheme.error.copy(alpha = 0.10f).compositeOver(baseRowColor)
    else baseRowColor
    val rowColor by animateColorAsState(targetValue = targetRowColor, label = "rowBg")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowColor),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── Область стрелок (видна когда любая станция в режиме reorder) ──
        AnimatedVisibility(
            visible = isAnyReordering,
            enter = expandHorizontally(),
            exit = shrinkHorizontally()
        ) {
            Box(
                modifier = Modifier.width(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isReordering) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (onMoveUp != null) {
                            IconButton(onClick = { onMoveUp(index) }) {
                                Icon(
                                    painter = painterResource(com.z_company.route.R.drawable.keyboard_arrow_up_24px),
                                    contentDescription = "Переместить вверх",
                                    tint = primaryColor
                                )
                            }
                        }
                        if (onMoveDown != null) {
                            IconButton(onClick = { onMoveDown(index) }) {
                                Icon(
                                    painter = painterResource(com.z_company.route.R.drawable.keyboard_arrow_down_24px),
                                    contentDescription = "Переместить вниз",
                                    tint = primaryColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Основной контент (кликабельный) ──
        Row(
            modifier = Modifier
                .weight(1f)
                // IntrinsicSize.Min — чтобы линия таймлайна (fillMaxHeight у точки)
                // растягивалась на всю высоту строки, когда название переносится
                // на 2 строки и строка становится выше DotHeight.
                .height(IntrinsicSize.Min)
                .then(
                    if (onStationClick != null || onStationLongPress != null) {
                        Modifier.combinedClickable(
                            onClick = { onStationClick?.invoke(index) },
                            onLongClick = { onStationLongPress?.invoke(index) }
                        )
                    } else Modifier
                )
                .padding(horizontal = ContentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── Колонка таймлайна (линия + точка) ──
            TimelineDot(
                dotSize = dotSize,
                lineWidth = lineWidth,
                columnWidth = TimelineColumnWidth,
                dotColor = dotColor,
                dotBorderColor = colors.dotBorderColor,
                lineColor = colors.lineColor,
                showTopLine = !isFirst,
                showBottomLine = !isLast,
            )

            Spacer(modifier = Modifier.width(8.dp))

            // ── Название станции + подзаголовок (путь / «проходная») ──
            val nameColor = if (item.isPassingStation) {
                colors.stationNameColor.copy(alpha = 0.55f)
            } else {
                colors.stationNameColor
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    text = item.stationName.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = nameColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = when {
                    item.isPassingStation -> "проходная"
                    !item.trackNumber.isNullOrBlank() -> "путь ${item.trackNumber}"
                    else -> null
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.stationNameColor.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            if (item.isPassingStation) {
                // ── Проходная: одно время (проследование) с меткой «просл.» ──
                Row(
                    modifier = Modifier.width(48.dp * 2 + StopBadgeWidth),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "просл.",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont),
                        color = colors.timeColor.copy(alpha = 0.5f),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    TimeText(millis = item.timeArrival, color = colors.timeColor)
                }
            } else {
                // ── Время прибытия ──
                TimeText(
                    millis = item.timeArrival,
                    color = colors.timeColor,
                )

                // ── Бейдж стоянки (фиксированная ширина, цвет по порогу) ──
                Box(
                    modifier = Modifier.width(StopBadgeWidth),
                    contentAlignment = Alignment.Center
                ) {
                    if (stopInfo != null && stopInfo.durationMillis > 0) {
                        val isPassengerTrain = trainNumber?.toIntOrNull()?.let { num ->
                            UtilsForEntities.passengerTrainNumberList.any { range -> num in range }
                        } ?: false
                        val (badgeColor, badgeTextColor) = computeStopBadgeColors(
                            stopDurationMillis = stopInfo.durationMillis,
                            isPassengerTrain = isPassengerTrain,
                            primaryColor = primaryColor,
                        )
                        StopBadge(
                            stopInfo = stopInfo,
                            badgeColor = badgeColor,
                            textColor = badgeTextColor,
                        )
                    }
                }

                // ── Время отправления ──
                TimeText(
                    millis = item.timeDeparture,
                    color = colors.timeColor,
                )
            }

            // ── Шеврон: строка станции кликабельна (открывает шторку) ──
            if (onStationClick != null) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    painter = painterResource(com.z_company.route.R.drawable.qv_arrow_forward),
                    contentDescription = null,
                    tint = primaryColor.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ─── Карточка перегона ───────────────────────────────────────────────────────
//
// Между станциями — самостоятельная кликабельная карточка (а не пилюля рядом с
// временем): так видно, что перегон можно открыть и заполнить. Заполненный —
// со сплошным фоном и иконкой локомотива, пустой — пунктирной рамкой и «+».
// Километры и смежные пути не показываем (решение пользователя).

@Composable
private fun SegmentCard(
    segment: SegmentInfo?,
    segmentTrackNumber: String?,
    segmentNotes: String?,
    colors: TimelineColors,
    isAnyReordering: Boolean = false,
    // «Будущий перегон» — вторая граница ещё неизвестна (маршрут может быть
    // продолжен). Такой блок не заливаем цветом и обводим пунктиром.
    isFuture: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val hasData = !segmentTrackNumber.isNullOrBlank() || !segmentNotes.isNullOrBlank()
    val accent = MaterialTheme.colorScheme.tertiary
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(12.dp)
    val dashColor = colors.lineColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Совпадение ширины с областью стрелок режима перестановки.
        AnimatedVisibility(
            visible = isAnyReordering,
            enter = expandHorizontally(),
            exit = shrinkHorizontally()
        ) {
            Spacer(modifier = Modifier.width(48.dp))
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = ContentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Пунктирная линия таймлайна слева — перегон это «между».
            TimelineDashedLine(
                columnWidth = TimelineColumnWidth,
                lineWidth = 2.dp,
                lineColor = colors.lineColor,
            )

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp)
                    .clip(shape)
                    .then(
                        if (!isFuture) {
                            // Как в референсе: блок «утоплен» относительно карточки
                            // станций (она лежит на secondary), поэтому background —
                            // он темнее в тёмной теме и светло-серый в светлой.
                            Modifier.background(MaterialTheme.colorScheme.background)
                        } else {
                            Modifier.drawBehind {
                                drawRoundRect(
                                    color = dashColor,
                                    style = Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(
                                            floatArrayOf(10f, 8f), 0f
                                        )
                                    ),
                                    cornerRadius = CornerRadius(12.dp.toPx()),
                                )
                            }
                        }
                    )
                    .then(
                        if (onClick != null && !isAnyReordering) Modifier.clickable { onClick() }
                        else Modifier
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Иконка: локомотив у заполненного, «+» у пустого.
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isFuture) accent.copy(alpha = 0.12f)
                            else primary.copy(alpha = 0.08f)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(
                            if (isFuture) com.z_company.route.R.drawable.ic_card_train_ref
                            else com.z_company.core.R.drawable.ic_edit
                        ),
                        contentDescription = null,
                        tint = if (isFuture) accent else primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(if (isFuture) 20.dp else 16.dp),
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Данные разнесены по смыслу: сверху «что это и сколько ехали»,
                // снизу — введённое пользователем (путь отдельной пилюлей,
                // примечание — текстом). Раньше всё шло одной строкой через «·».
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ПЕРЕГОН",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = MonoFont,
                                letterSpacing = 1.sp,
                            ),
                            fontWeight = FontWeight.SemiBold,
                            color = primary.copy(alpha = 0.45f),
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (segment != null && segment.durationMillis > 0) {
                            Text(
                                text = formatDuration(segment.durationMillis),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = MonoFont
                                ),
                                color = primary,
                                maxLines = 1,
                            )
                        }
                    }

                    // Нижняя строка только при реальных данных: подписи-подсказки
                    // не нужны, о редактировании говорит иконка-карандаш.
                    if (hasData) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (!segmentTrackNumber.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(primary.copy(alpha = 0.10f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$segmentTrackNumber путь",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = MonoFont
                                        ),
                                        color = primary.copy(alpha = 0.8f),
                                        maxLines = 1,
                                    )
                                }
                            }
                            segmentNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                                Text(
                                    text = notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = primary.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                if (!isFuture) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(com.z_company.route.R.drawable.qv_arrow_forward),
                        contentDescription = null,
                        tint = primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

// ─── Бейдж суммарного времени в пути ─────────────────────────────────────────

@Composable
private fun TotalTimeBadge(
    totalMillis: Long,
    colors: TimelineColors,
    isAnyReordering: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ContentPadding)
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── Совпадение ширины с областью стрелок ──
        AnimatedVisibility(
            visible = isAnyReordering,
            enter = expandHorizontally(),
            exit = shrinkHorizontally()
        ) {
            Spacer(modifier = Modifier.width(48.dp))
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(colors.segmentBackgroundColor)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = "Время в пути: ${formatDuration(totalMillis)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colors.segmentTextColor,
            )
        }
    }
}

// ─── Бейдж скоростей ────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeedBadge(
    sectionSpeed: Double,
    technicalSpeed: Double?,
    colors: TimelineColors,
    isAnyReordering: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ContentPadding)
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = isAnyReordering,
            enter = expandHorizontally(),
            exit = shrinkHorizontally()
        ) {
            Spacer(modifier = Modifier.width(48.dp))
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.segmentBackgroundColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "Уч: ${"%.1f".format(sectionSpeed)} км/ч",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.segmentTextColor,
                )
            }
            if (technicalSpeed != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.segmentBackgroundColor)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "Техн: ${"%.1f".format(technicalSpeed)} км/ч",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.segmentTextColor,
                    )
                }
            }
        }
    }
}

// ─── Вспомогательные компоненты ──────────────────────────────────────────────

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
            // Заполняем высоту строки (задаётся IntrinsicSize.Min у родителя),
            // но не меньше DotHeight — линия остаётся неразрывной и при переносе
            // названия станции на 2 строки.
            .fillMaxHeight()
            .defaultMinSize(minHeight = DotHeight),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2

            if (showTopLine) {
                drawLine(
                    color = lineColor,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, centerY - dotSizePx / 2),
                    strokeWidth = lineWidthPx,
                    cap = StrokeCap.Round,
                )
            }

            if (showBottomLine) {
                drawLine(
                    color = lineColor,
                    start = Offset(centerX, centerY + dotSizePx / 2),
                    end = Offset(centerX, size.height),
                    strokeWidth = lineWidthPx,
                    cap = StrokeCap.Round,
                )
            }

            drawCircle(
                color = dotBorderColor,
                radius = dotSizePx / 2 + borderWidth / 2,
                center = Offset(centerX, centerY),
            )

            drawCircle(
                color = dotColor,
                radius = dotSizePx / 2,
                center = Offset(centerX, centerY),
            )
        }
    }
}

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

@Composable
private fun TimeText(
    millis: Long?,
    color: Color,
) {
    Text(
        text = if (millis != null) formatTime(millis) else "—",
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFont),
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(min = 48.dp),
    )
}

@Composable
private fun StopBadge(
    stopInfo: StopInfo,
    badgeColor: Color,
    textColor: Color,
) {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(stopInfo.durationMillis)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    val text = when {
        hours > 0 && minutes > 0 -> "${hours}ч${minutes}'"
        hours > 0 -> "${hours}ч"
        else -> "${minutes}'"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont),
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
        segmentTextColor = scheme.primary,
        segmentBackgroundColor = scheme.surface.copy(alpha = 0.6f),
        stopBadgeColor = Color(0xFFFFC107),
        stopBadgeTextColor = Color(0xFF3E2723),
        stationNameColor = scheme.primary,
        timeColor = scheme.primary,
        firstLastDotColor = scheme.primary,
    )
}

// ─── Адаптеры ────────────────────────────────────────────────────────────────

fun List<StationFormState>.toTimelineItems(): List<StationTimelineItem> {
    return map { state ->
        StationTimelineItem(
            id = state.id,
            stationName = state.station.data ?: "",
            timeArrival = state.arrival.data,
            timeDeparture = state.departure.data,
            trackNumber = state.trackNumber,
            isFinalStation = state.isFinalStation,
            isPassingStation = state.isPassingStation,
            segmentTrackNumber = state.segmentTrackNumber,
            segmentNotes = state.segmentNotes,
        )
    }
}

fun List<com.z_company.domain.entities.route.Station>.toStationTimelineItems(): List<StationTimelineItem> {
    return map { station ->
        StationTimelineItem(
            id = station.stationId,
            stationName = station.stationName ?: "",
            timeArrival = station.timeArrival,
            timeDeparture = station.timeDeparture,
            trackNumber = station.trackNumber,
            isFinalStation = station.isFinalStation,
            isPassingStation = station.isPassingStation,
            segmentTrackNumber = station.segmentTrackNumber,
            segmentNotes = station.segmentNotes,
        )
    }
}

// ─── Сводка маршрута ─────────────────────────────────────────────────────────
// Значения для внешних пилюль «Время в пути / Уч / Техн» (когда showSummary=false).

data class RouteSummaryData(
    val totalTimeText: String,
    val sectionSpeed: Double?,
    val technicalSpeed: Double?,
)

fun computeRouteSummary(
    stations: List<StationTimelineItem>,
    distance: Double?,
): RouteSummaryData? {
    if (stations.size < 2) return null
    val startTime = stations.first().timeDeparture ?: stations.first().timeArrival
    val endTime = stations.last().timeArrival ?: stations.last().timeDeparture
    if (startTime == null || endTime == null || endTime <= startTime) return null
    val totalMillis = endTime - startTime

    var sectionSpeed: Double? = null
    var technicalSpeed: Double? = null
    if (distance != null && distance > 0) {
        sectionSpeed = distance / (totalMillis / 3_600_000.0)
        var stopTime = 0L
        for (i in 1 until stations.size - 1) {
            val arr = stations[i].timeArrival
            val dep = stations[i].timeDeparture
            if (arr != null && dep != null && dep > arr) stopTime += dep - arr
        }
        val runningTime = totalMillis - stopTime
        technicalSpeed = if (runningTime > 0) distance / (runningTime / 3_600_000.0) else null
    }
    return RouteSummaryData(
        totalTimeText = formatDuration(totalMillis),
        sectionSpeed = sectionSpeed,
        technicalSpeed = technicalSpeed,
    )
}
