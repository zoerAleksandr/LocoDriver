package com.z_company.route.component

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.route.Route
import de.charlex.compose.RevealDirection
import de.charlex.compose.RevealSwipe
import de.charlex.compose.RevealValue
import de.charlex.compose.rememberRevealState
import kotlinx.coroutines.launch
import com.z_company.core.ui.component.AutoSizeText
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.DateAndTimeFormat
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Locale

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
    onClick: () -> Unit
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

    RevealSwipe(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        enableSwipe = isExpand,
        state = revealState,
        directions = setOf(
            RevealDirection.EndToStart
        ),
        hiddenContentEnd = {
            Box(
                modifier = Modifier
                    .width(75.dp),
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .height(80.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
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
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        route.basicData.timeStartWork?.let { timeStartWork ->
                            AutoSizeText(
                                modifier = Modifier.padding(end = 8.dp),
                                text = SimpleDateFormat(
                                    DateAndTimeFormat.MINI_DATE_FORMAT, Locale.getDefault()
                                ).format(
                                    timeStartWork
                                ),
                                maxTextSize = requiredSizeText,
                                maxLines = 1,
                                style = AppTypography.getType().headlineSmall,
                                fontWeight = FontWeight.Light,
                                onTextLayout = { textLayoutResult ->
                                    val size = textLayoutResult.layoutInput.style.fontSize
                                    changingTextSize(size)
                                }
                            )
                        }
                        route.basicData.timeStartWork?.let { timeStartWork ->
                            AutoSizeText(
                                modifier = Modifier.padding(),
                                text = SimpleDateFormat(
                                    DateAndTimeFormat.TIME_FORMAT, Locale.getDefault()
                                ).format(
                                    timeStartWork
                                ),
                                maxTextSize = requiredSizeText,
                                maxLines = 1,
                                style = AppTypography.getType().headlineSmall,
                                fontWeight = FontWeight.Light,
                                onTextLayout = { textLayoutResult ->
                                    val size = textLayoutResult.layoutInput.style.fontSize
                                    changingTextSize(size)
                                }
                            )
                        }
                        route.basicData.timeEndWork?.let { timeEndWork ->
                            AutoSizeText(
                                modifier = Modifier.padding(),
                                text = " - ${
                                    SimpleDateFormat(
                                        DateAndTimeFormat.TIME_FORMAT, Locale.getDefault()
                                    ).format(
                                        timeEndWork
                                    )
                                }",
                                maxTextSize = requiredSizeText,
                                maxLines = 1,
                                style = AppTypography.getType().headlineSmall,
                                fontWeight = FontWeight.Light,
                                onTextLayout = { textLayoutResult ->
                                    val size = textLayoutResult.layoutInput.style.fontSize
                                    changingTextSize(size)
                                }
                            )
                        }
                    }

                    val workTimeValue = route.getWorkTime()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        AutoSizeText(
                            text = DateAndTimeConverter.getTimeInStringFormat(workTimeValue),
                            style = AppTypography.getType().headlineSmall,
                            fontWeight = FontWeight.Medium,
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
                        Text(
                            text = "$trainNumber$stationStart$stationEnd",
                            style = AppTypography.getType().titleLarge.copy(
                                fontSize = 20.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Light,
                        )
                    }
                }
            }
        }

    }
}
