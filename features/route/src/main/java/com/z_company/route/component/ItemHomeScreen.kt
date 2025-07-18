package com.z_company.route.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.route.Route
import de.charlex.compose.RevealDirection
import de.charlex.compose.RevealSwipe
import de.charlex.compose.RevealValue
import de.charlex.compose.rememberRevealState
import kotlinx.coroutines.launch
import com.z_company.core.ui.component.AutoSizeText
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.route.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun ItemHomeScreen(
    modifier: Modifier = Modifier,
    route: Route,
    isExpand: Boolean,
    onDelete: (Route) -> Unit,
    requiredSizeText: TextUnit,
    changingTextSize: (TextUnit) -> Unit,
    onLongClick: () -> Unit,
    containerColor: Color,
    onClick: () -> Unit,
    getTextWorkTime: (Route) -> String
) {
    val revealState = rememberRevealState()
    val scope = rememberCoroutineScope()

    val interactionSource = remember { MutableInteractionSource() }

    val viewConfiguration = LocalViewConfiguration.current

    LaunchedEffect(interactionSource) {
        var isLongClick = false

        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isLongClick = false
                    delay(viewConfiguration.longPressTimeoutMillis)
                    isLongClick = true
                }

                is PressInteraction.Release -> {
                    if (isLongClick.not()) {
                    }
                }
            }
        }
    }

    LaunchedEffect(isExpand) {
        if (!isExpand) {
            scope.launch {
                revealState.animateTo(RevealValue.Default)
            }
        }
    }

    var itemHeightDp by remember {
        mutableStateOf(0.dp)
    }

    RevealSwipe(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 65.dp),
        enableSwipe = isExpand,
        state = revealState,
        directions = setOf(
            RevealDirection.EndToStart
        ),
        hiddenContentEnd = {
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = itemHeightDp, minWidth = 75.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = {
                    onDelete(route)
                    scope.launch {
                        revealState.animateTo(RevealValue.Default)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        tint = MaterialTheme.colorScheme.onError,
                        contentDescription = null
                    )
                }
            }
        },
        backgroundCardEndColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
        shape = Shapes.medium
    ) {
        val localDensity = LocalDensity.current
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .defaultMinSize(minHeight = 65.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .onGloballyPositioned { coordinates ->
                    itemHeightDp = with(localDensity) { coordinates.size.height.toDp() }
            },
            shape = Shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = MaterialTheme.colorScheme.primary
            )
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
//                    val startWork = getDateMiniAndTime(route.basicData.timeStartWork)
//
//                    val isDifference = DateAndTimeConverter.isDifferenceDate(
//                        first = route.basicData.timeStartWork,
//                        second = route.basicData.timeEndWork
//                    )
//
//                    val endWork = if (isDifference) {
//                        getDateMiniAndTime(route.basicData.timeEndWork)
//                    } else {
//                        getTime(route.basicData.timeEndWork)
//                    }

                    val timeText = getTextWorkTime(route)
//                        "$startWork - $endWork"

                    Box(
                        modifier = Modifier
                            .weight(0.8f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        AutoSizeText(
                            text = timeText,
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

                    val workTimeValue = route.getWorkTime()
                    Box(
                        modifier = Modifier
                            .weight(0.2f),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        AutoSizeText(
                            text = DateAndTimeConverter.getTimeInStringFormat(workTimeValue),
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

                route.trains.firstOrNull()?.let { train ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val trainNumber = if (!train.number.isNullOrBlank()) {
                            "№${train.number} "
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
                                    val size = textLayoutResult.layoutInput.style.fontSize
                                    changingTextSize(size)
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    route.getPassengerTime()?.let { time ->
                        if (time > 0L) {
                            Icon(
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
                    if (route.basicData.isSynchronizedRoute){
                        Image(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(id = R.drawable.sync_on_icon),
                            contentDescription = null,
                        )
                    } else {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(id = R.drawable.not_sync_icon),
                            contentDescription = null,
                        )
                    }
                    if (route.basicData.isFavorite) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}
