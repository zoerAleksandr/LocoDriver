package com.z_company.route.component

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissState
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.domain.entities.route.Route
import com.z_company.core.ui.component.AutoSizeText
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.route.R

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun ItemHomeScreen(
    modifier: Modifier = Modifier,
    dismissState: DismissState,
    route: Route,
    isExpand: Boolean = false,
    onDelete: (Route) -> Unit,
    requiredSizeText: TextUnit,
    changingTextSize: (TextUnit) -> Unit,
    onLongClick: () -> Unit = {},
    containerColor: Color,
    onClick: () -> Unit,
    dateAndTimeConverter: DateAndTimeConverter,
    isHeavyTrains: Boolean = false,
    isExtendedServicePhaseTrains: Boolean = false,
    isHolidayTimeInRoute: Boolean = false
) {
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
        val workTimeValue = route.getWorkTime()
        val workTimeString = ConverterLongToTime.getTimeInStringFormat(workTimeValue)
        timeText to workTimeString
    }
    // Dismiss state with confirmStateChange -> show Snackbar for Undo, and only delete after timeout if not undone

    // Outer box so we can place a local SnackbarHost (Undo) overlayed on the item
    SwipeToDismiss(
        state = dismissState,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 65.dp),
        directions = setOf(DismissDirection.EndToStart),
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.error, shape = CardDefaults.shape)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(onClick = {
                    // Manual delete via icon: show same snackbar with Undo option
                    onDelete(route)
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.delete_24px),
                        tint = MaterialTheme.colorScheme.background,
                        contentDescription = null
                    )
                }
            }
        },
        dismissContent = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .defaultMinSize(minHeight = 65.dp)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 2.dp,
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.secondary
                ),
                colors = CardDefaults.cardColors(
                    containerColor = containerColor,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.8f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            AutoSizeText(
                                text = timeTextMemo,
                                maxTextSize = requiredSizeText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = AppTypography.getType().headlineSmall,
                                fontWeight = FontWeight.Normal,
                                onTextLayout = { textLayoutResult ->
                                    val size = textLayoutResult.layoutInput.style.fontSize
                                    changingTextSize(size)
                                }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(0.2f),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            AutoSizeText(
                                text = workTimeStringMemo,
                                style = AppTypography.getType().headlineSmall,
                                fontWeight = FontWeight.Normal,
                                maxTextSize = requiredSizeText,
                                onTextLayout = { textLayoutResult ->
                                    val size = textLayoutResult.layoutInput.style.fontSize
                                    changingTextSize(size)
                                }
                            )
                        }
                    }

                    // If expanded -> show all locomotives/trains/passengers; else show last ones only
                    if (isExpand) {
                        if (route.trains.isNotEmpty()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                route.trains
                                    .sortedBy { it.stations.firstOrNull()?.timeDeparture }
                                    .forEach { train ->
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
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = AppTypography.getType().headlineSmall,
                                                fontWeight = FontWeight.Light,
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
                                            maxTextSize = requiredSizeText.value.minus(4).sp,
                                            maxLines = 1,
                                            text = "${loco.series ?: ""} ${loco.number ?: ""}",
                                            style = AppTypography.getType().bodyLarge,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
//                    if (route.passengers.isNotEmpty()) {
//                        Column(modifier = Modifier.fillMaxWidth()) {
//                            route.passengers.forEach { passenger ->
//                                val timeFollowing = ConverterLongToTime.getTimeInStringFormat(passenger.getFollowingTime())
//                                Row(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    verticalAlignment = Alignment.CenterVertically
//                                ) {
//                                    Text(
//                                        text = "Пассажиром $timeFollowing",
//                                        style = AppTypography.getType().bodyLarge,
//                                        maxLines = 1,
//                                        overflow = TextOverflow.Ellipsis
//                                    )
//                                }
//                            }
//                        }
//                    }

                    } else {
                        // compact: show only last locomotive/train/passenger (if any)
                        route.trains.firstOrNull()?.let { train ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = AppTypography.getType().headlineSmall,
                                        fontWeight = FontWeight.Light,
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        if (isHolidayTimeInRoute) {
                            Icon(
                                tint = Color(0xFFf1642e),
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(id = R.drawable.icon_holiday_hours),
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
                        if (isHeavyTrains) {
                            Icon(
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(id = R.drawable.weight_24px),
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
                        route.getPassengerTime()?.let { time ->
                            if (time > 0L) {
                                Icon(
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                    painter = painterResource(id = R.drawable.passenger_24px),
                                    contentDescription = null
                                )
                            }
                        }
                        route.getWorkTime()?.let { time ->
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
                        if (route.basicData.isFavorite) {
                            Icon(
                                tint = Color(0xFFf1642e),
                                modifier = Modifier.size(20.dp),
                                imageVector = androidx.compose.material.icons.Icons.Default.Favorite,
                                contentDescription = null,
                            )
                        }

                        if (route.basicData.isSynchronizedRoute) {
                            Image(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(id = R.drawable.sync_on_icon),
                                contentDescription = null,
                            )
                        } else {
                            Icon(
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(id = R.drawable.not_sync_icon),
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }
    )
}