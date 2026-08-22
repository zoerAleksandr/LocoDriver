package com.z_company.route.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.custom.AppTheme
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.TrainAssist
import com.z_company.domain.entities.route.UtilsForEntities.fullRest
import com.z_company.domain.entities.route.UtilsForEntities.getBreakDuration
import com.z_company.domain.entities.route.UtilsForEntities.getFollowingTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.shortRest
import com.z_company.domain.util.CalculationEnergy
import com.z_company.domain.util.CalculationEnergy.rounding
import com.z_company.domain.util.ifNullOrBlank
import com.z_company.domain.util.times
import com.z_company.route.R
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Быстрый просмотр маршрута (Android-паттерн) — раскрытая [ModalBottomSheet] по
 * долгому нажатию на строку маршрута. Показывает всю информацию по маршруту в режиме
 * «только чтение» + нижнюю панель быстрых действий. Спецификация: route-quick-view-android.md
 * и SCREEN_SPECS.md (раздел «Быстрый просмотр маршрута»).
 *
 * Токены дизайна → тема:
 *   surface(карточка)=secondary, accent=tertiary, accentSoft=tertiary@10%,
 *   text=primary, textMuted=primary@60%, success=accentGreen, warning=warnOrange,
 *   danger=error, border=outline@30%, bgSubtle=background.
 * Числовые значения — моноширинным [MonoFont].
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RouteQuickViewSheet(
    route: Route,
    shiftPaymentText: String?,
    isHeavyTrains: Boolean,
    isLongCompositionTrain: Boolean,
    isExtendedServicePhaseTrains: Boolean,
    dateAndTimeConverter: DateAndTimeConverter?,
    minTimeRest: Long?,
    homeRest: Long?,
    actualRestDuration: Long?,
    actualRestUntil: Long?,
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit,
    onToggleFavorite: (Route) -> Unit,
    onShare: (Route) -> Unit,
    onCopy: (String) -> Unit,
    onSync: (Route) -> Unit,
    onRequestDelete: (Route) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Реактивное избранное: route — неизменяемый снимок, поэтому храним локальный
    // оптимистичный флаг, чтобы иконка в панели переключалась сразу по тапу.
    var isFavorite by remember(route.basicData.id) {
        mutableStateOf(route.basicData.isFavorite)
    }

    // Кап высоты области контента ~ 92% экрана минус запас под шапку и панель
    // действий. Высота контента задаётся фиксированным dp (не зависит от позиции
    // драга), поэтому шторка не «прыгает» при поднятии вверх; при этом короткий
    // контент шторку не разворачивает — Column обнимает содержимое.
    val contentMaxHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.92f) - 300.dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.background,
        // dragHandle = null — штатная ручка кликабельна (ripple по тапу). Свою рисуем
        // внутри тональной шапки, без indication, чтобы верх шторки был одного цвета
        // с зоной времени и без эффекта нажатия.
        dragHandle = null,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            QuickViewHeader(
                route = route,
                shiftPaymentText = shiftPaymentText,
            )

            // heightIn(max) с фиксированным dp: при коротком контенте Box обнимает
            // список, при длинном — ограничивает и LazyColumn скроллится.
            Box(modifier = Modifier.heightIn(max = contentMaxHeight)) {
                QuickViewContent(
                    route = route,
                    isHeavyTrains = isHeavyTrains,
                    isLongCompositionTrain = isLongCompositionTrain,
                    isExtendedServicePhaseTrains = isExtendedServicePhaseTrains,
                    dateAndTimeConverter = dateAndTimeConverter,
                    minTimeRest = minTimeRest,
                    homeRest = homeRest,
                    actualRestDuration = actualRestDuration,
                    actualRestUntil = actualRestUntil,
                )
                // Подсказка прокрутки — короткий градиент у нижней кромки списка.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                                )
                            )
                        )
                )
            }

            QuickViewActionBar(
                route = route,
                isFavorite = isFavorite,
                onOpen = onOpen,
                onToggleFavorite = {
                    isFavorite = !isFavorite
                    onToggleFavorite(route)
                },
                onShare = onShare,
                onCopy = onCopy,
                onSync = onSync,
                onRequestDelete = onRequestDelete,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────── Шапка ──

@Composable
private fun QuickViewHeader(
    route: Route,
    shiftPaymentText: String?,
) {
    val accent = MaterialTheme.colorScheme.tertiary
    val accentSoft = accent.copy(alpha = 0.10f)
    val text = MaterialTheme.colorScheme.primary
    val textMuted = text.copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(accentSoft)
    ) {
        // Своя ручка (drag handle) в тональной зоне — без ripple.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 14.dp)
        ) {
            // «Маршрут № …»
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Маршрут",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = textMuted,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "№${route.basicData.number.ifNullOrBlank { "б/н" }}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = MonoFont,
                        fontWeight = FontWeight.W700,
                        fontSize = 19.sp,
                    ),
                    color = text,
                )
            }

            // Направление «Станция → Станция»
            routeDirection(route)?.let { (from, to) ->
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = from,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.W600, fontSize = 17.sp,
                        ),
                        color = text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(18.dp),
                        painter = painterResource(R.drawable.qv_arrow_forward),
                        contentDescription = null,
                        tint = accent,
                    )
                    Text(
                        text = to,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.W600, fontSize = 17.sp,
                        ),
                        color = text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Плашки «Время работы» + «Заработано» — обе однострочные, компактные.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val workTime = route.getWorkTime()
                HeaderTile(
                    modifier = Modifier.weight(1f),
                    label = "Время работы",
                ) {
                    Text(
                        text = if (workTime != null && workTime > 0)
                            ConverterLongToTime.getTimeInStringFormat(workTime) else "—",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = MonoFont,
                            fontWeight = FontWeight.W800,
                            fontSize = 20.sp,
                        ),
                        color = text,
                    )
                }

                HeaderTile(
                    modifier = Modifier.weight(1f),
                    label = "Заработано",
                ) {
                    Text(
                        text = shiftPaymentText?.takeIf { it.isNotBlank() } ?: "—",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = MonoFont,
                            fontWeight = FontWeight.W800,
                            fontSize = 20.sp,
                        ),
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Статус синхронизации.
            val synced = route.basicData.isSynchronized
            HeaderChip {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (synced) {
                        Image(
                            modifier = Modifier.size(14.dp),
                            painter = painterResource(R.drawable.sync_on_icon),
                            contentDescription = null,
                        )
                    } else {
                        Icon(
                            modifier = Modifier.size(14.dp),
                            painter = painterResource(R.drawable.sync_disabled_24px),
                            contentDescription = null,
                            tint = textMuted,
                        )
                    }
                    Text(
                        text = if (synced) "Синхронизирован" else "Не синхронизирован",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (synced) AppTheme.colors.accentGreen else textMuted,
                    )
                }
            }
        }
    }
    HorizontalDivider(color = QvDividerColor())
}

@Composable
private fun HeaderTile(
    modifier: Modifier = Modifier,
    label: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondary)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = MonoFont, letterSpacing = 1.0.sp, fontSize = 9.sp,
            ),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(2.dp))
        content()
    }
}

@Composable
private fun HeaderChip(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.secondary)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

// ────────────────────────────────────────────────────────────── Контент ──

@Composable
private fun QuickViewContent(
    route: Route,
    isHeavyTrains: Boolean,
    isLongCompositionTrain: Boolean,
    isExtendedServicePhaseTrains: Boolean,
    dateAndTimeConverter: DateAndTimeConverter?,
    minTimeRest: Long?,
    homeRest: Long?,
    actualRestDuration: Long?,
    actualRestUntil: Long?,
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        // 1. Время работы
        if (route.basicData.timeStartWork != null || route.basicData.timeEndWork != null) {
            item {
                SectionHeader("Время работы")
                QvCard {
                    WorkTimeRow(
                        label = "Явка",
                        time = route.basicData.timeStartWork,
                        dateAndTimeConverter = dateAndTimeConverter,
                    )
                    QvDivider()
                    WorkTimeRow(
                        label = "Сдача",
                        time = route.basicData.timeEndWork,
                        dateAndTimeConverter = dateAndTimeConverter,
                    )
                    if (route.basicData.timeStartBreak != null && route.basicData.timeEndBreak != null &&
                        route.getBreakDuration() > 0
                    ) {
                        QvDivider()
                        BreakTimeRow(
                            label = "Начало перерыва",
                            time = route.basicData.timeStartBreak,
                            dateAndTimeConverter = dateAndTimeConverter,
                        )
                        BreakDurationDivider(
                            duration = ConverterLongToTime.getTimeInStringFormat(route.getBreakDuration()),
                        )
                        BreakTimeRow(
                            label = "Конец перерыва",
                            time = route.basicData.timeEndBreak,
                            dateAndTimeConverter = dateAndTimeConverter,
                        )
                    }
                }
            }
        }

        // 2. Отдых — в пункте оборота (короткий/полный) либо домашний.
        if (route.basicData.timeStartWork != null && route.basicData.timeEndWork != null) {
            val endWork = route.basicData.timeEndWork
            if (route.basicData.restPointOfTurnover) {
                item {
                    val station = route.trains.lastOrNull()?.stations?.lastOrNull()?.stationName
                    SectionHeader("Отдых в пункте оборота" + (station?.let { " · $it" } ?: ""))
                    val shortAbs = route.shortRest(minTimeRest)
                    val fullAbs = route.fullRest(minTimeRest)
                    QvCard {
                        RestContentRow(
                            title = "Короткий отдых",
                            until = dateAndTimeConverter?.getDateMiniAndTime(shortAbs),
                            duration = durationText(shortAbs, endWork),
                            durationColor = MaterialTheme.colorScheme.primary,
                        )
                        QvDivider()
                        RestContentRow(
                            title = "Полный отдых",
                            until = dateAndTimeConverter?.getDateMiniAndTime(fullAbs),
                            duration = durationText(fullAbs, endWork),
                            durationColor = MaterialTheme.colorScheme.primary,
                        )
                        ActualRestRow(actualRestDuration, actualRestUntil, dateAndTimeConverter)
                    }
                }
            } else if (homeRest != null || actualRestDuration != null) {
                item {
                    SectionHeader("Домашний отдых")
                    QvCard {
                        if (homeRest != null) {
                            RestContentRow(
                                title = "Полный отдых",
                                until = dateAndTimeConverter?.getDateMiniAndTime(homeRest),
                                duration = durationText(homeRest, endWork),
                                durationColor = MaterialTheme.colorScheme.primary,
                            )
                        }
                        ActualRestRow(actualRestDuration, actualRestUntil, dateAndTimeConverter)
                    }
                }
            }
        }

        // 3. Локомотивы
        if (route.locomotives.isNotEmpty()) {
            item { SectionHeader("Локомотивы · ${route.locomotives.size}") }
            items(route.locomotives, key = { it.locoId }) { loco ->
                QvCardBlock { LocomotiveBlock(loco, dateAndTimeConverter) }
            }
        }

        // 4. Поезда
        val sortedTrains = route.trains
            .filter { it.stations.firstOrNull()?.timeDeparture != null }
            .sortedByDescending { it.stations.firstOrNull()?.timeDeparture } +
            route.trains.filter { it.stations.firstOrNull()?.timeDeparture == null }
        if (sortedTrains.isNotEmpty()) {
            item { SectionHeader("Поезда · ${sortedTrains.size}") }
            items(sortedTrains, key = { it.trainId }) { train ->
                QvCardBlock { TrainBlock(train, dateAndTimeConverter, isPassenger = false) }
            }
        }

        // 5. Следование пассажиром
        if (route.passengers.isNotEmpty()) {
            item { SectionHeader("Следование пассажиром") }
            items(route.passengers, key = { it.passengerId }) { passenger ->
                QvCardBlock { PassengerBlock(passenger, dateAndTimeConverter) }
            }
        }

        // 6. Доплаты
        val workTime = route.getWorkTime() ?: 0L
        val over12 = workTime > 12L * 3_600_000L
        val hasPriznaki = isLongCompositionTrain || isHeavyTrains || over12 || isExtendedServicePhaseTrains
        if (hasPriznaki) {
            item {
                SectionHeader("Доплаты")
                PriznakiFlow(
                    isLongCompositionTrain = isLongCompositionTrain,
                    isHeavyTrains = isHeavyTrains,
                    over12 = over12,
                    isExtendedServicePhaseTrains = isExtendedServicePhaseTrains,
                )
            }
        }

        // 7. Заметки
        if (!route.basicData.notes.isNullOrBlank()) {
            item {
                SectionHeader("Заметки")
                QvCard {
                    Text(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        text = route.basicData.notes!!,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp, lineHeight = 21.sp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 20.dp, top = 16.dp, bottom = 7.dp),
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = MonoFont,
            fontSize = 10.5.sp,
            letterSpacing = 1.3.sp,
            fontWeight = FontWeight.W600,
        ),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
    )
}

/** Карточка-контейнер секции (surface, радиус 14, внутренние разделители border). */
@Composable
private fun QvCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.secondary),
        content = content,
    )
}

/** Карточка под отдельный блок в списке (loco/train/passenger) с вертикальным отступом. */
@Composable
private fun QvCardBlock(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) { QvCard(content = content) }
}

@Composable
private fun QvDivider() {
    HorizontalDivider(color = QvDividerColor())
}

/** Единый бледный цвет разделителей между блоками. */
@Composable
private fun QvDividerColor(): Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

@Composable
private fun WorkTimeRow(
    label: String,
    time: Long?,
    dateAndTimeConverter: DateAndTimeConverter?,
) {
    val text = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = text)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SubtlePill(dateAndTimeConverter?.getDate(time).orEmpty())
            SubtlePill(dateAndTimeConverter?.getTime(time).orEmpty())
        }
    }
}

/**
 * Строка перерыва: подпись сверху, дата/время под ней справа — чтобы при
 * крупном системном шрифте длинная подпись и плашки не конкурировали за ширину.
 */
@Composable
private fun BreakTimeRow(
    label: String,
    time: Long?,
    dateAndTimeConverter: DateAndTimeConverter?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            SubtlePill(dateAndTimeConverter?.getDate(time).orEmpty())
            SubtlePill(dateAndTimeConverter?.getTime(time).orEmpty())
        }
    }
}

/** Длительность перерыва — чипом по центру, между началом и концом. */
@Composable
private fun BreakDurationDivider(duration: String) {
    val accent = MaterialTheme.colorScheme.tertiary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = QvDividerColor())
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(accent.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = duration,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont),
                color = accent,
            )
        }
        HorizontalDivider(modifier = Modifier.weight(1f), color = QvDividerColor())
    }
}

@Composable
private fun SubtlePill(value: String) {
    if (value.isBlank()) return
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFont),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun RestContentRow(title: String, until: String?, duration: String, durationColor: Color) {
    val text = MaterialTheme.colorScheme.primary
    val textMuted = text.copy(alpha = 0.6f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = text)
            if (!until.isNullOrBlank()) {
                Text(text = "до $until", style = MaterialTheme.typography.labelSmall, color = textMuted)
            }
        }
        Text(
            text = duration,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = MonoFont, fontWeight = FontWeight.W800, fontSize = 18.sp,
            ),
            color = durationColor,
        )
    }
}

/** Строка «Фактический отдых» (до следующей явки) — рендерится, если явка есть. */
@Composable
private fun ActualRestRow(duration: Long?, until: Long?, converter: DateAndTimeConverter?) {
    if (duration == null || duration <= 0 || until == null) return
    QvDivider()
    RestContentRow(
        title = "Фактический отдых",
        until = converter?.getDateMiniAndTime(until),
        duration = ConverterLongToTime.getTimeInStringFormat(duration),
        durationColor = MaterialTheme.colorScheme.primary,
    )
}

// ──────────────────────────────────────────────────────────── Локомотив ──

@Composable
private fun LocomotiveBlock(loco: Locomotive, dateAndTimeConverter: DateAndTimeConverter?) {
    val text = MaterialTheme.colorScheme.primary
    val textMuted = text.copy(alpha = 0.6f)
    val accent = MaterialTheme.colorScheme.tertiary
    val success = AppTheme.colors.accentGreen

    Column(modifier = Modifier.padding(14.dp)) {
        // Шапка: иконка в тональном боксе + название
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(22.dp),
                    tint = accent,
                    painter = painterResource(
                        when (loco.type) {
                            LocoType.ELECTRIC -> R.drawable.electric_loco
                            LocoType.DIESEL -> R.drawable.diesel_loco
                        }
                    ),
                    contentDescription = null,
                )
            }
            val series = loco.series.ifNullOrBlank { "" }
            val number = loco.number.ifNullOrBlank { "" }
            val name = when {
                series.isNotBlank() && number.isNotBlank() -> "$series-$number"
                series.isNotBlank() -> series
                else -> when (loco.type) {
                    LocoType.ELECTRIC -> "Электровоз"
                    LocoType.DIESEL -> "Тепловоз"
                }
            }
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = MonoFont, fontWeight = FontWeight.W700,
                ),
                color = text,
            )
        }

        // Время: приёмка (начало → конец → выход на КП) и сдача (заход на КП →
        // начало → конец) — те же поля, что в шторке времени FormLocoScreen.
        val acceptanceStops = buildList {
            loco.timeStartOfAcceptance?.let { add("начало" to it) }
            loco.timeEndOfAcceptance?.let { add("конец" to it) }
            loco.timeBarrierOut?.let { add("КП" to it) }
        }
        val deliveryStops = buildList {
            loco.timeBarrierIn?.let { add("КП" to it) }
            loco.timeStartOfDelivery?.let { add("начало" to it) }
            loco.timeEndOfDelivery?.let { add("конец" to it) }
        }
        if (acceptanceStops.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            LocoTimeStopsRow(title = "Приёмка", stops = acceptanceStops, dateAndTimeConverter = dateAndTimeConverter)
        }
        if (deliveryStops.isNotEmpty()) {
            Spacer(Modifier.height(if (acceptanceStops.isNotEmpty()) 10.dp else 12.dp))
            LocoTimeStopsRow(title = "Сдача", stops = deliveryStops, dateAndTimeConverter = dateAndTimeConverter)
        }
        // Доп. отступ перед секциями — время и секции визуально разные группы,
        // одного 12dp (общего для всех Spacer'ов внутри блока) между ними мало.
        if (acceptanceStops.isNotEmpty() || deliveryStops.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
        }

        when (loco.type) {
            LocoType.ELECTRIC -> {
                loco.electricSectionList.forEachIndexed { index, section ->
                    val consEnergy = CalculationEnergy.getTotalEnergyConsumption(
                        section.acceptedEnergy,
                        section.deliveryEnergy,
                    )
                    val consRec = CalculationEnergy.getTotalEnergyConsumption(
                        section.acceptedRecovery,
                        section.deliveryRecovery,
                    )
                    val hasEnergy = consEnergy != null ||
                        section.acceptedEnergy != null || section.deliveryEnergy != null
                    val hasRecovery = consRec != null ||
                        section.acceptedRecovery != null || section.deliveryRecovery != null
                    if (!hasEnergy && !hasRecovery) return@forEachIndexed

                    Spacer(Modifier.height(12.dp))
                    SectionLabel(index)
                    if (hasEnergy) {
                        Spacer(Modifier.height(6.dp))
                        ConsumptionBlock(
                            title = "Расход",
                            total = consEnergy?.let { grouped(it) + " кВт·ч" },
                            totalColor = text,
                            accepted = section.acceptedEnergy?.let { grouped(it) },
                            delivery = section.deliveryEnergy?.let { grouped(it) },
                        )
                    }
                    if (hasRecovery) {
                        Spacer(Modifier.height(10.dp))
                        ConsumptionBlock(
                            title = "Рекуперация",
                            total = consRec?.let { "−" + grouped(abs(it)) + " кВт·ч" },
                            totalColor = success,
                            accepted = section.acceptedRecovery?.let { grouped(it) },
                            delivery = section.deliveryRecovery?.let { grouped(it) },
                        )
                    }
                }
            }

            LocoType.DIESEL -> {
                loco.dieselSectionList.forEachIndexed { index, section ->
                    val accFuelKilo = section.acceptedFuel.times(section.coefficient)
                    val delFuelKilo = section.deliveryFuel.times(section.coefficient)
                    val consFuel = CalculationEnergy.getTotalFuelConsumption(
                        section.acceptedFuel,
                        section.deliveryFuel,
                        section.fuelSupply,
                    )
                    val consFuelKilo = CalculationEnergy.getTotalFuelInKiloConsumption(
                        acceptedInKilo = accFuelKilo,
                        deliveryInKilo = delFuelKilo,
                        refuelInKilo = section.fuelSupplyInKilo,
                    )
                    val hasFuel = consFuel != null ||
                        section.acceptedFuel != null || section.deliveryFuel != null
                    val hasRefuel = section.fuelSupply != null || section.fuelSupplyInKilo != null
                    if (!hasFuel && !hasRefuel) return@forEachIndexed

                    Spacer(Modifier.height(12.dp))
                    SectionLabel(index)
                    if (hasFuel) {
                        Spacer(Modifier.height(6.dp))
                        ConsumptionBlock(
                            title = "Расход",
                            total = consFuel?.let {
                                grouped(it) + " л" + (consFuelKilo?.let { kg -> " · ${grouped(kg)} кг" } ?: "")
                            },
                            totalColor = text,
                            accepted = section.acceptedFuel?.let { grouped(it) },
                            delivery = section.deliveryFuel?.let { grouped(it) },
                            acceptedSupporting = accFuelKilo?.let { grouped(it) + " кг" },
                            deliverySupporting = delFuelKilo?.let { grouped(it) + " кг" },
                        )
                    }
                    if (hasRefuel) {
                        Spacer(Modifier.height(10.dp))
                        SummaryLine(
                            title = "Экипировка",
                            value = buildList {
                                section.fuelSupply?.let { add(grouped(it) + " л") }
                                section.fuelSupplyInKilo?.let { add(grouped(it) + " кг") }
                            }.joinToString(" · "),
                        )
                    }
                }
            }
        }
    }
}

/** Строка «Приёмка»/«Сдача»: заголовок + ряд плашек (начало/конец/КП — что сохранено). */
@Composable
private fun LocoTimeStopsRow(
    title: String,
    stops: List<Pair<String, Long>>,
    dateAndTimeConverter: DateAndTimeConverter?,
) {
    LocoSubLabel(title)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        stops.forEach { (caption, time) ->
            CounterPill(
                modifier = Modifier.weight(1f),
                label = caption,
                value = dateAndTimeConverter?.getTime(time),
            )
        }
    }
}

@Composable
private fun ConsumptionBlock(
    title: String,
    total: String?,
    totalColor: Color,
    accepted: String?,
    delivery: String?,
    acceptedSupporting: String? = null,
    deliverySupporting: String? = null,
) {
    val text = MaterialTheme.colorScheme.primary
    val textMuted = text.copy(alpha = 0.6f)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = MaterialTheme.typography.labelMedium, color = textMuted)
        Spacer(Modifier.weight(1f))
        if (total != null) {
            Text(
                text = total,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = MonoFont, fontWeight = FontWeight.W700,
                ),
                color = totalColor,
            )
        }
    }
    if (accepted != null || delivery != null) {
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CounterPill(
                modifier = Modifier.weight(1f),
                label = "Принял",
                value = accepted,
                supporting = acceptedSupporting,
            )
            CounterPill(
                modifier = Modifier.weight(1f),
                label = "Сдал",
                value = delivery,
                supporting = deliverySupporting,
            )
        }
    }
}

@Composable
private fun CounterPill(
    modifier: Modifier,
    label: String,
    value: String?,
    supporting: String? = null,
) {
    val text = MaterialTheme.colorScheme.primary
    val textMuted = text.copy(alpha = 0.6f)
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.6.sp),
                color = textMuted,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value ?: "—",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = MonoFont, fontWeight = FontWeight.W700,
                ),
                color = text,
            )
        }
        supporting?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = it,
                modifier = Modifier.padding(horizontal = 10.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont),
                color = textMuted,
            )
        }
    }
}

@Composable
private fun SectionLabel(index: Int) {
    LocoSubLabel("Секция ${index + 1}")
}

/**
 * Подзаголовок группы внутри карточки локомотива («Приёмка», «Сдача», «Секция N») —
 * единый стиль для всех подзаголовков этого уровня иерархии: капс, разрядка,
 * приглушённый цвет. Отличается от заголовков полей («Расход», «Экипировка»),
 * которые стоят в одной строке со значением и остаются `bodyMedium`.
 */
@Composable
private fun LocoSubLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
    )
}

@Composable
private fun SummaryLine(title: String, value: String) {
    val text = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = text.copy(alpha = 0.6f),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = MonoFont,
                fontWeight = FontWeight.W700,
            ),
            color = text,
        )
    }
}

// ─────────────────────────────────────────────────────────────── Поезд ──

@Composable
private fun TrainBlock(train: Train, dateAndTimeConverter: DateAndTimeConverter?, isPassenger: Boolean) {
    val text = MaterialTheme.colorScheme.primary
    val textMuted = text.copy(alpha = 0.6f)
    val accent = MaterialTheme.colorScheme.tertiary

    Column(modifier = Modifier.padding(14.dp)) {
        // Шапка: иконка + № + плечо
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    tint = accent,
                    painter = painterResource(R.drawable.ic_card_train_ref),
                    contentDescription = null,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = train.number?.takeIf { it.isNotBlank() }?.let { "№$it" } ?: "Поезд",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = MonoFont, fontWeight = FontWeight.W700,
                    ),
                    color = text,
                )
                trainShoulder(train)?.let {
                    Text(text = it, style = MaterialTheme.typography.labelMedium, color = textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // Характеристика (вес / оси / у.д.)
        val meta = buildList {
            train.weight?.takeIf { it.isNotBlank() }?.let { add("$it т") }
            train.axle?.takeIf { it.isNotBlank() }?.let { add("$it оси") }
            train.conditionalLength?.takeIf { it.isNotBlank() }?.let { raw ->
                val fmt = raw.toDoubleOrNull()
                    ?.let { d -> if (d == kotlin.math.floor(d)) d.toLong().toString() else raw }
                    ?: raw
                add("$fmt у.д.")
            }
        }
        if (meta.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = meta.joinToString("  ·  "),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFont),
                color = textMuted,
            )
        }

        // График станций
        if (train.stations.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            train.stations.forEachIndexed { index, station ->
                StationRow(
                    station = station,
                    isFirst = index == 0,
                    isLast = index == train.stations.lastIndex,
                    dateAndTimeConverter = dateAndTimeConverter,
                )
            }
        }

        // Помощь в движении
        train.pusher?.let { AssistLine("Толкач", it) }
        train.doubleTraction?.let { AssistLine("Двойная тяга", it) }
        train.doubledTrain?.let { AssistLine("Сдвоенный", it) }
    }
}

@Composable
private fun StationRow(
    station: Station,
    isFirst: Boolean,
    isLast: Boolean,
    dateAndTimeConverter: DateAndTimeConverter?,
) {
    val text = MaterialTheme.colorScheme.primary
    val textMuted = text.copy(alpha = 0.6f)
    val accent = MaterialTheme.colorScheme.tertiary
    val danger = MaterialTheme.colorScheme.error
    val warning = AppTheme.colors.warnOrange
    val railColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val endpoint = isFirst || isLast

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
        verticalAlignment = Alignment.Top,
    ) {
        // Рельс с точкой
        Column(
            modifier = Modifier.width(20.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp).weight(1f)
                    .background(if (isFirst) Color.Transparent else railColor)
            )
            Box(
                modifier = Modifier
                    .size(if (endpoint) 11.dp else 9.dp)
                    .clip(CircleShape)
                    .background(if (endpoint) accent else MaterialTheme.colorScheme.background)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(if (endpoint) MaterialTheme.colorScheme.secondary else railColor)
            )
            Box(
                modifier = Modifier
                    .width(2.dp).weight(1f)
                    .background(if (isLast) Color.Transparent else railColor)
            )
        }
        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(
                text = buildString {
                    append(station.stationName.ifNullOrBlank { "—" })
                    if (!station.trackNumber.isNullOrBlank()) append("  ${station.trackNumber} п.")
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600),
                color = text,
            )
            // Бейдж стоянки
            stopDuration(station)?.let { stopMs ->
                if (stopMs > 0) {
                    val color = when {
                        stopMs > 30 * 60_000L -> danger
                        stopMs > 5 * 60_000L -> warning
                        else -> textMuted
                    }
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(color.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "стоянка · ${ConverterLongToTime.getTimeInStringFormat(stopMs)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont),
                            color = color,
                        )
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(vertical = 6.dp)) {
            station.timeArrival?.let {
                Text(
                    text = "приб ${dateAndTimeConverter?.getTime(it) ?: ""}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFont),
                    color = text,
                )
            }
            station.timeDeparture?.let {
                Text(
                    text = "отпр ${dateAndTimeConverter?.getTime(it) ?: ""}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFont),
                    color = text,
                )
            }
        }
    }
}

@Composable
private fun AssistLine(label: String, assist: TrainAssist) {
    val text = MaterialTheme.colorScheme.primary
    val textMuted = text.copy(alpha = 0.6f)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "$label:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.W600), color = textMuted)
        val info = buildString {
            assist.locomotiveSeries?.takeIf { it.isNotBlank() }?.let { append(it) }
            assist.locomotiveNumber?.takeIf { it.isNotBlank() }?.let { append(" - $it") }
        }.trim(' ', '-')
        if (info.isNotBlank()) {
            Text(text = info, style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFont), color = text)
        }
    }
}

// ─────────────────────────────────────────────────────────── Пассажир ──

@Composable
private fun PassengerBlock(passenger: Passenger, dateAndTimeConverter: DateAndTimeConverter?) {
    val text = MaterialTheme.colorScheme.primary
    val textMuted = text.copy(alpha = 0.6f)
    val accent = MaterialTheme.colorScheme.tertiary

    Column(modifier = Modifier.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    tint = accent,
                    painter = painterResource(R.drawable.passenger_24px),
                    contentDescription = null,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = passenger.trainNumber?.takeIf { it.isNotBlank() }?.let { "№$it" } ?: "Пассажиром",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = MonoFont, fontWeight = FontWeight.W700,
                    ),
                    color = text,
                )
                val route = listOfNotNull(
                    passenger.stationDeparture?.takeIf { it.isNotBlank() },
                    passenger.stationArrival?.takeIf { it.isNotBlank() },
                ).joinToString(" — ")
                if (route.isNotBlank()) {
                    Text(text = route, style = MaterialTheme.typography.labelMedium, color = textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            passenger.getFollowingTime()?.let {
                Text(
                    text = ConverterLongToTime.getTimeInStringFormat(it),
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFont, fontWeight = FontWeight.W700),
                    color = text,
                )
            }
        }
        if (passenger.timeDeparture != null || passenger.timeArrival != null) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SubtlePill("отпр ${dateAndTimeConverter?.getTime(passenger.timeDeparture) ?: ""}")
                SubtlePill("приб ${dateAndTimeConverter?.getTime(passenger.timeArrival) ?: ""}")
            }
        }
    }
}

// ───────────────────────────────────────────────── Доплатные признаки ──

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PriznakiFlow(
    isLongCompositionTrain: Boolean,
    isHeavyTrains: Boolean,
    over12: Boolean,
    isExtendedServicePhaseTrains: Boolean,
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isLongCompositionTrain) PriznakChip(R.drawable.straighten_24px, "Повышенная длина", tinted = true)
        if (isHeavyTrains) PriznakChip(R.drawable.weight_24px, "Повышенная масса", tinted = true)
        if (over12) PriznakChip(R.drawable.orden, "Свыше 12 часов", tinted = false)
        if (isExtendedServicePhaseTrains) PriznakChip(R.drawable.long_distance_24px, "Удлинённое плечо", tinted = true)
    }
}

@Composable
private fun PriznakChip(iconRes: Int, label: String, tinted: Boolean) {
    val accent = MaterialTheme.colorScheme.tertiary
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.secondary)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (tinted) {
            Icon(
                modifier = Modifier.size(16.dp),
                tint = accent,
                painter = painterResource(iconRes),
                contentDescription = null,
            )
        } else {
            Image(
                modifier = Modifier.size(16.dp),
                painter = painterResource(iconRes),
                contentDescription = null,
            )
        }
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

// ────────────────────────────────────────────────── Панель действий ──

@Composable
private fun QuickViewActionBar(
    route: Route,
    isFavorite: Boolean,
    onOpen: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: (Route) -> Unit,
    onCopy: (String) -> Unit,
    onSync: (Route) -> Unit,
    onRequestDelete: (Route) -> Unit,
) {
    val actionColor = MaterialTheme.colorScheme.primary
    Column {
        HorizontalDivider(color = QvDividerColor())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondary)
                .height(72.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionIcon(
                iconRes = com.z_company.core.R.drawable.ic_edit,
                description = "Открыть",
                tint = actionColor,
            ) { onOpen(route.basicData.id) }
            ActionIcon(
                iconRes = if (isFavorite) R.drawable.favorite_fill_24px else R.drawable.favorite_24px,
                description = "В избранное",
                tint = if (isFavorite) AppTheme.colors.favoriteColor else actionColor,
            ) { onToggleFavorite() }
            ActionIcon(iconRes = R.drawable.share_24px, description = "Поделиться", tint = actionColor) { onShare(route) }
            ActionIcon(iconRes = R.drawable.outline_content_copy_24, description = "Копировать", tint = actionColor) { onCopy(route.basicData.id) }
            if (!route.basicData.isSynchronized) {
                ActionIcon(iconRes = R.drawable.sync_24px, description = "Синхронизировать", tint = actionColor) { onSync(route) }
            }
            Spacer(Modifier.weight(1f))
            ActionIcon(iconRes = R.drawable.delete_24px, description = "Удалить маршрут", tint = MaterialTheme.colorScheme.error) { onRequestDelete(route) }
        }
    }
}

@Composable
private fun ActionIcon(iconRes: Int, description: String, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = tint,
        )
    }
}

// ──────────────────────────────────────────────────────────── helpers ──

/** Длительность отдыха от [end] (сдача) до абсолютного момента [until]. */
private fun durationText(until: Long?, end: Long?): String {
    if (until == null || end == null) return "—"
    val d = until - end
    return if (d > 0) ConverterLongToTime.getTimeInStringFormat(d) else "—"
}

/**
 * Направление маршрута «перваяСтанция → последняяСтанция» из графика поездов.
 * Сортируем по времени отправления первой станции — порядок route.trains из БД/сети
 * не гарантирован (нет ORDER BY, insertOrReplace может физически переставить строки
 * местами при повторном сохранении/синхронизации), поэтому просто «первый/последний
 * в списке» ненадёжен и может показать маршрут в обратном направлении.
 */
private fun routeDirection(route: Route): Pair<String, String>? {
    val trains = route.trains
        .filter { it.stations.isNotEmpty() }
        .sortedBy { it.stations.firstOrNull()?.timeDeparture ?: Long.MAX_VALUE }
    val from = trains.firstOrNull()?.stations?.firstOrNull()?.stationName
    val to = trains.lastOrNull()?.stations?.lastOrNull()?.stationName
    return if (!from.isNullOrBlank() && !to.isNullOrBlank()) from to to else null
}

private fun trainShoulder(train: Train): String? {
    val from = train.stations.firstOrNull()?.stationName
    val to = train.stations.lastOrNull()?.stationName
    return if (!from.isNullOrBlank() && !to.isNullOrBlank()) "$from — $to" else null
}

private fun stopDuration(station: Station): Long? {
    val a = station.timeArrival
    val d = station.timeDeparture
    return if (a != null && d != null && d > a) d - a else null
}

/** Форматирует число с группировкой разрядов тонким пробелом, без дробной части. */
private fun grouped(value: Double): String {
    val rounded = rounding(value, 2) ?: 0.0
    val whole = abs(rounded).roundToLong()
    val sb = StringBuilder()
    val s = whole.toString()
    for ((i, c) in s.withIndex()) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append(' ')
        sb.append(c)
    }
    return (if (rounded < 0) "−" else "") + sb.toString()
}
