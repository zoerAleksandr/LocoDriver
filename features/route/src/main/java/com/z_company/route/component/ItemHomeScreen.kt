package com.z_company.route.component

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.DismissDirection
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.DismissValue
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Route
import com.z_company.core.ui.component.AutoSizeText
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.UtilsForEntities.getBreakDuration
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTimeInMonth
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.domain.util.getTimeZone
import com.z_company.route.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(
    ExperimentalMaterialApi::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun ItemHomeScreen(
    modifier: Modifier = Modifier,
    route: Route,
    convertTimeToString: (Long?) -> String,
    isExpand: Boolean = false,
    onRequestDelete: (Route) -> Unit,
    // Дефолтный максимум: 18.sp — крупнее обычного текста (bodyLarge = 16),
    // но не такой большой как titleLarge (22). AutoSizeText сжимает до 10.sp
    // если не помещается. Если нужно крупнее — увеличить до 20-22.sp.
    requiredSizeText: TextUnit = 18.sp,
    changingTextSize: (TextUnit) -> Unit = {},
    onLongClick: () -> Unit = {},
    containerColor: Color,
    onClick: () -> Unit,
    dateAndTimeConverter: DateAndTimeConverter,
    isHeavyTrains: Boolean = false,
    isLongCompositionTrain: Boolean = false,
    isExtendedServicePhaseTrains: Boolean = false,
    isHolidayTimeInRoute: Boolean = false,
    number: Int? = null,
    monthOfYear: MonthOfYear? = null,
    offsetInMoscow: Long = 0L,
    timeCalculationContext: TimeCalculationContext? = null,
) {

    // --- мемоизируем тяжёлые вычисления по route ---
    val sortedTrains = remember(route) {
        route.trains
            .filter { it.stations.firstOrNull()?.timeDeparture != null }
            .sortedByDescending { it.stations.firstOrNull()?.timeDeparture } +
            route.trains.filter { it.stations.firstOrNull()?.timeDeparture == null }
    }
    val breakDuration = remember(route) { route.getBreakDuration() }
    val passengerTime = remember(route) { route.getPassengerTime() }
    val workTime = remember(route) { route.getWorkTime() }

    // --- memoized texts to avoid repeated computation on recomposition ---
    val (timeTextMemo, workTimeStringMemo) = remember(route, dateAndTimeConverter) {
        val startWork =
            dateAndTimeConverter.getDateMiniAndTime(value = route.basicData.timeStartWork)
        val isDifference = dateAndTimeConverter.isDifferenceDate(
            first = route.basicData.timeStartWork,
            second = route.basicData.timeEndWork
        )
        val endWork = if (isDifference) {
            dateAndTimeConverter.getDateMiniAndTime(value = route.basicData.timeEndWork)
        } else {
            dateAndTimeConverter.getTime(route.basicData.timeEndWork)
        }
        val timeText = "$startWork - $endWork"
        val workTimeValue = if (monthOfYear != null) {
            val ctx = timeCalculationContext ?: TimeCalculationContext(
                localTZ = kotlinx.datetime.TimeZone.of(getTimeZone(offsetInMoscow)),
                crossMonthTZ = kotlinx.datetime.TimeZone.of(getTimeZone(offsetInMoscow))
            )
            route.getWorkTimeInMonth(monthOfYear, ctx)
        } else {
            route.getWorkTime()
        }
        val workTimeString = convertTimeToString(workTimeValue)
        timeText to workTimeString
    }

    val interactionSource = remember { MutableInteractionSource() }

    // Bottom-sheet «Обозначения» — открывается по нажатию на значки маршрута.
    val legendSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showLegend by remember { mutableStateOf(false) }
    if (showLegend) {
        RouteLegendSheet(
            onDismissRequest = { showLegend = false },
            sheetState = legendSheetState,
        )
    }

    // Свайп-удаление как у станции/секции локомотива: раскрывает красную кнопку
    // «УДАЛИТЬ», по нажатию → onRequestDelete (подтверждение показывает экран).
    SwipeToRevealDelete(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 65.dp),
        onDeleteClick = { onRequestDelete(route) },
        onContentClick = onClick,
        onContentLongClick = onLongClick,
        backgroundVerticalPadding = 0.dp,
    ) { _ ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .defaultMinSize(minHeight = 65.dp),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 1.dp,
                ),
                colors = CardDefaults.cardColors(
                    containerColor = containerColor,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            // maxLines=1 + minTextSize=10.sp гарантируют что текст
                                // помещается в одну строку даже при больших системных шрифтах
                                // или узких экранах. Если уменьшать ниже 10sp — текст
                                // нечитабельный, тогда срабатывает Ellipsis.
                            AutoSizeText(
                                text = timeTextMemo,
                                maxTextSize = requiredSizeText,
                                minTextSize = 10.sp,
                                maxLines = 1,
                                fontWeight = FontWeight.Medium,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = com.z_company.core.ui.theme.MonoFont,
                                ),
                                onTextLayout = { textLayoutResult ->
                                    val size = textLayoutResult.layoutInput.style.fontSize
                                    changingTextSize(size)
                                }
                            )
                        }

                        AutoSizeText(
                            text = workTimeStringMemo,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = com.z_company.core.ui.theme.MonoFont,
                            ),
                            maxTextSize = requiredSizeText,
                            minTextSize = 10.sp,
                            maxLines = 1,
                            fontWeight = FontWeight.Bold,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { textLayoutResult ->
                                val size = textLayoutResult.layoutInput.style.fontSize
                                changingTextSize(size)
                            }
                        )
                    }

                    // Поезда: основной (первый) всегда одной строкой; в expanded —
                    // остальные поезда списком ниже. #номер маршрута — только в футере.
                    if (sortedTrains.isNotEmpty()) {
                        val trainsToShow = if (isExpand) sortedTrains else sortedTrains.take(1)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            trainsToShow.forEach { train ->
                                val tn = if (!train.number.isNullOrBlank()) "№${train.number} " else ""
                                val s1 = train.stations.firstOrNull()?.stationName ?: ""
                                val s2 = if (train.stations.size > 1) " — ${train.stations.last().stationName ?: ""}" else ""
                                val info = "$tn$s1$s2"
                                if (info.isNotBlank()) {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = info,
                                        // Тот же шрифт что и время работы — JetBrains Mono
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = com.z_company.core.ui.theme.MonoFont,
                                            fontSize = 14.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }

                    // Локомотивы/пассажиры — только в развёрнутом режиме
                    if (isExpand) {
                        if (false) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                sortedTrains.forEach { train ->
                                        val trainNumber = if (!train.number.isNullOrBlank()) {
                                            "\u2116${train.number} "
                                        } else {
                                            ""
                                        }
                                        val stationStart = if (train.stations.isNotEmpty()) {
                                            train.stations.first().stationName ?: ""
                                        } else {
                                            ""
                                        }

                                        val stationEnd =
                                            if (train.stations.isNotEmpty() && train.stations.size > 1) {
                                                " - ${train.stations.last().stationName ?: ""}"
                                            } else {
                                                ""
                                            }
                                        if (!"$trainNumber$stationStart$stationEnd".isBlank()) {
                                            AutoSizeText(
                                                text = "$trainNumber$stationStart$stationEnd",
                                                maxTextSize = requiredSizeText,
                                                minTextSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyLarge,
                                                onTextLayout = { textLayoutResult ->
                                                    val size =
                                                        textLayoutResult.layoutInput.style.fontSize
                                                    changingTextSize(size)
                                                }
                                            )
                                        }
                                    }
                            }
                        }
                        if (route.locomotives.isNotEmpty()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                route.locomotives.forEach { loco ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AutoSizeText(
                                            maxTextSize = requiredSizeText,
                                            minTextSize = 10.sp,
                                            maxLines = 1,
                                            text = "${loco.series ?: ""} ${loco.number ?: ""}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                        if (!route.basicData.notes.isNullOrBlank()) {
                            Box(modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                AutoSizeText(
                                    maxTextSize = requiredSizeText,
                                    minTextSize = 10.sp,
                                    maxLines = 2,
                                    text = "${route.basicData.notes}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        number?.let {
                            Text(
                                text = "#$it",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Row(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = { showLegend = true }
                                        )
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                            ) {
                                if (isHolidayTimeInRoute) {
                                    Image(
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.icon_holiday),
                                        contentDescription = null
                                    )
                                }
                                if (breakDuration > 0L) {
                                    Icon(
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.pause_24px),
                                        contentDescription = null
                                    )
                                }
                                if (isLongCompositionTrain) {
                                    Icon(
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.straighten_24px),
                                        contentDescription = null
                                    )
                                }
                                if (isHeavyTrains) {
                                    Icon(
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.weight_24px),
                                        contentDescription = null
                                    )
                                }
                                if (isExtendedServicePhaseTrains) {
                                    Icon(
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.long_distance_24px),
                                        contentDescription = null
                                    )
                                }
                                if (route.basicData.isOnePersonOperation) {
                                    Icon(
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.person_24px),
                                        contentDescription = null
                                    )
                                }
                                passengerTime?.let { time ->
                                    if (time > 0L) {
                                        Icon(
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp),
                                            painter = painterResource(id = R.drawable.passenger_24px),
                                            contentDescription = null
                                        )
                                    }
                                }
                                workTime?.let { time ->
                                    val oneHourInMillis = 3600000
                                    val normaHours = 12
                                    if (time > normaHours * oneHourInMillis) {
                                        Image(
                                            modifier = Modifier.size(20.dp),
                                            painter = painterResource(id = R.drawable.orden),
                                            contentDescription = null,
                                        )
                                    }
                                }
                                if (route.trains.any { it.pusher != null }) {
                                    Icon(
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.ic_pusher_24px),
                                        contentDescription = null
                                    )
                                }
                                if (route.trains.any { it.doubleTraction != null }) {
                                    Icon(
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.ic_double_traction_24px),
                                        contentDescription = null
                                    )
                                }
                                if (route.trains.any { it.doubledTrain != null }) {
                                    Icon(
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.ic_doubled_train_24px),
                                        contentDescription = null
                                    )
                                }
                                if (route.basicData.isFavorite) {
                                    Icon(
                                        tint = Color(0xFFf1642e),
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(R.drawable.favorite_fill_24px),
                                        contentDescription = null,
                                    )
                                }

                                if (route.basicData.isSynchronized) {
                                    Image(
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.sync_on_icon),
                                        contentDescription = null,
                                    )
                                } else {
                                    Icon(
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                        painter = painterResource(id = R.drawable.sync_disabled_24px),
                                        contentDescription = null,
                                    )
                                }
                            }
                    }
                }
            }
        }
}