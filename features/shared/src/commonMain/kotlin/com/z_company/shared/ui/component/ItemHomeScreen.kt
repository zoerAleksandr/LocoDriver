package com.z_company.shared.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getBreakDuration
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.shared.util.DateAndTimeConverter
import com.z_company.shared.util.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemHomeScreen(
    modifier: Modifier = Modifier,
    route: Route,
    convertTimeToString: (Long?) -> String,
    isExpand: Boolean = false,
    onRequestDelete: (Route) -> Unit,
    onLongClick: () -> Unit = {},
    containerColor: Color,
    onClick: () -> Unit,
    dateAndTimeConverter: DateAndTimeConverter? = null,
    isHeavyTrains: Boolean = false,
    isExtendedServicePhaseTrains: Boolean = false,
    isHolidayTimeInRoute: Boolean = false,
    number: Int? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Memoized texts
    val (timeText, workTimeString) = remember(route, dateAndTimeConverter) {
        val time = if (dateAndTimeConverter != null) {
            val startWork = dateAndTimeConverter.getDateMiniAndTime(route.basicData.timeStartWork)
            val isDifference = dateAndTimeConverter.isDifferenceDate(
                route.basicData.timeStartWork,
                route.basicData.timeEndWork,
            )
            val endWork = if (isDifference) {
                dateAndTimeConverter.getDateMiniAndTime(route.basicData.timeEndWork)
            } else {
                dateAndTimeConverter.getTime(route.basicData.timeEndWork)
            }
            "$startWork - $endWork"
        } else {
            val start = route.basicData.timeStartWork?.let { TimeFormatter.formatDateTime(it) } ?: ""
            val end = route.basicData.timeEndWork?.let { TimeFormatter.formatTime(it) } ?: ""
            if (start.isNotEmpty() && end.isNotEmpty()) "$start - $end" else start
        }
        val workTime = convertTimeToString(route.getWorkTime())
        time to workTime
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRequestDelete(route)
                false // snap back
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 65.dp),
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        shape = CardDefaults.shape,
                    )
                    .padding(end = 8.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                IconButton(onClick = { onRequestDelete(route) }) {
                    Icon(
                        Icons.Default.Delete,
                        tint = MaterialTheme.colorScheme.background,
                        contentDescription = null,
                    )
                }
            }
        },
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .defaultMinSize(minHeight = 65.dp)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = ripple(
                        color = MaterialTheme.colorScheme.background,
                        bounded = true,
                    ),
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                // Row 1: time range + work duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(
                        modifier = Modifier.weight(0.8f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = timeText,
                            maxLines = 1,
                            fontWeight = FontWeight.Medium,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Box(
                        modifier = Modifier.weight(0.2f),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text(
                            text = workTimeString,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                // Expanded: trains, locos, notes
                if (isExpand) {
                    if (route.trains.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            route.trains
                                .sortedBy { it.stations.firstOrNull()?.timeDeparture }
                                .forEach { train ->
                                    val trainNumber = if (!train.number.isNullOrBlank()) "\u2116${train.number} " else ""
                                    val stationStart = train.stations.firstOrNull()?.stationName ?: ""
                                    val stationEnd = if (train.stations.size > 1) " - ${train.stations.last().stationName ?: ""}" else ""
                                    val text = "$trainNumber$stationStart$stationEnd"
                                    if (text.isNotBlank()) {
                                        Text(
                                            text = text,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                    }
                                }
                        }
                    }
                    if (route.locomotives.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            route.locomotives.forEach { loco ->
                                Text(
                                    text = "${loco.series ?: ""} ${loco.number ?: ""}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                    if (!route.basicData.notes.isNullOrBlank()) {
                        Text(
                            text = route.basicData.notes!!,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                } else {
                    // Compact: first train only
                    route.trains.firstOrNull()?.let { train ->
                        val trainNumber = if (!train.number.isNullOrBlank()) "\u2116${train.number} " else ""
                        val stationStart = train.stations.firstOrNull()?.stationName ?: ""
                        val stationEnd = if (train.stations.size > 1) " - ${train.stations.last().stationName ?: ""}" else ""
                        val text = "$trainNumber$stationStart$stationEnd"
                        if (text.isNotBlank()) {
                            Text(
                                text = text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }

                // Row 3: number + status icons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    number?.let {
                        Text(
                            text = "#$it",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }

                    Row(
                        modifier = Modifier.wrapContentWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        // Holiday
                        if (isHolidayTimeInRoute) {
                            Icon(
                                Icons.Default.Celebration,
                                contentDescription = "Праздничный день",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                        // Break
                        if (route.getBreakDuration() > 0L) {
                            Icon(
                                Icons.Default.Pause,
                                contentDescription = "Перерыв",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        // Extended service phase
                        if (isExtendedServicePhaseTrains) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = "Удлинённая фаза",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        // Heavy trains
                        if (isHeavyTrains) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = "Тяжеловесные",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        // Passenger time
                        if ((route.getPassengerTime() ?: 0L) > 0L) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Пассажиром",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        // Over 12 hours
                        val workTime = route.getWorkTime()
                        if (workTime != null && workTime > 12 * 3_600_000L) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Свыше 12ч",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        // Sync status
                        val isSynced = route.basicData.isSynchronized
                        Icon(
                            if (isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = if (isSynced) "Синхронизирован" else "Не синхронизирован",
                            modifier = Modifier.size(20.dp),
                            tint = if (isSynced) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}
