package com.z_company.route.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.DateAndTimeFormat
import com.z_company.domain.entities.route.UtilsForEntities
import com.z_company.route.viewmodel.StationFormState

private val warningColor = Color(0xFFFFC107)
private val warningTextColor = Color(0xFF3E2723)
private val dangerColor = Color(0xFFEF5350)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StationItem(
    modifier: Modifier = Modifier,
    stationFormState: StationFormState,
    dateAndTimeConverter: DateAndTimeConverter?,
    trainNumber: String? = null,
    isReordering: Boolean = false,
    onStationClick: () -> Unit,
    onLongPress: () -> Unit,
    onCancelReorder: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val dataTextStyle = MaterialTheme.typography.bodyLarge
    var timeTextStyle by remember { mutableStateOf(dataTextStyle) }

    // Stop duration calculation
    val stopMinutes =
        if (stationFormState.arrival.data != null && stationFormState.departure.data != null) {
            val diff = stationFormState.departure.data - stationFormState.arrival.data
            if (diff > 0) (diff / 60_000).toString() else null
        } else null

    // Passenger train detection
    val isPassengerTrain = trainNumber?.toIntOrNull()?.let { num ->
        UtilsForEntities.passengerTrainNumberList.any { range -> num in range }
    } ?: false

    // Stop color coding
    val stopLong = stopMinutes?.toLongOrNull() ?: 0L
    val stopBackground = when {
        stopLong > 20 && isPassengerTrain -> dangerColor
        stopLong > 30 -> dangerColor
        stopLong > 5 -> warningColor
        else -> Color.Transparent
    }
    val stopTextColor = when {
        stopLong > 20 && isPassengerTrain -> Color.White
        stopLong > 30 -> Color.White
        stopLong > 5 -> warningTextColor
        else -> primaryColor.copy(alpha = 0.7f)
    }

    // Time text formatting
    val arrivalText = stationFormState.arrival.data?.let {
        dateAndTimeConverter?.getTimeFromDateLong(it)
    } ?: DateAndTimeFormat.DEFAULT_TIME_TEXT

    val departureText = stationFormState.departure.data?.let {
        dateAndTimeConverter?.getTimeFromDateLong(it)
    } ?: DateAndTimeFormat.DEFAULT_TIME_TEXT

    val stationName = stationFormState.station.data?.takeIf { it.isNotBlank() } ?: "Станция"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Arrows on the left - only for the station being reordered
        AnimatedVisibility(
            visible = isReordering,
            enter = expandHorizontally(),
            exit = shrinkHorizontally()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (onMoveUp != null) {
                    IconButton(onClick = onMoveUp) {
                        Icon(
                            painter = painterResource(com.z_company.route.R.drawable.keyboard_arrow_up_24px),
                            contentDescription = "Переместить вверх",
                            tint = primaryColor
                        )
                    }
                }
                if (onMoveDown != null) {
                    IconButton(onClick = onMoveDown) {
                        Icon(
                            painter = painterResource(com.z_company.route.R.drawable.keyboard_arrow_down_24px),
                            contentDescription = "Переместить вниз",
                            tint = primaryColor
                        )
                    }
                }
            }
        }

        // Main content card
        Card(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    onClick = onStationClick,
                    onLongClick = onLongPress
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            shape = Shapes.medium
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Station name: 60% width
                Text(
                    text = stationName,
                    modifier = Modifier.weight(0.6f),
                    style = dataTextStyle,
                    color = if (stationFormState.station.data.isNullOrBlank())
                        primaryColor.copy(alpha = 0.5f) else primaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Times container: 40% width, split into 3 equal parts
                Row(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Arrival
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                color = if (stationFormState.arrival.data != null)
                                    MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.surface,
                                shape = Shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = arrivalText,
                            maxLines = 1,
                            softWrap = false,
                            style = timeTextStyle,
                            color = if (stationFormState.arrival.data != null)
                                primaryColor else primaryColor.copy(alpha = 0.5f),
                            onTextLayout = { result ->
                                if (result.hasVisualOverflow) {
                                    timeTextStyle = timeTextStyle.copy(
                                        fontSize = timeTextStyle.fontSize * 0.9
                                    )
                                }
                            }
                        )
                    }

                    // Stop duration
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(stopBackground, Shapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        if (stopMinutes != null) {
                            Text(
                                text = "${stopMinutes}'",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = stopTextColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Departure
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                color = if (stationFormState.departure.data != null)
                                    MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.surface,
                                shape = Shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = departureText,
                            maxLines = 1,
                            softWrap = false,
                            style = timeTextStyle,
                            color = if (stationFormState.departure.data != null)
                                primaryColor else primaryColor.copy(alpha = 0.5f),
                            onTextLayout = { result ->
                                if (result.hasVisualOverflow) {
                                    timeTextStyle = timeTextStyle.copy(
                                        fontSize = timeTextStyle.fontSize * 0.9
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
