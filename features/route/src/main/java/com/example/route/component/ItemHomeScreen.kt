package com.example.route.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.core.ui.theme.Shapes
import com.example.domain.entities.route.Route
import de.charlex.compose.RevealDirection
import de.charlex.compose.RevealSwipe
import de.charlex.compose.RevealValue
import de.charlex.compose.rememberRevealState
import kotlinx.coroutines.launch
import androidx.constraintlayout.compose.Dimension.Companion.fillToConstraints
import com.example.core.ui.component.CustomDivider
import com.example.core.util.DateAndTimeConverter
import com.example.core.ui.theme.custom.AppTypography
import com.example.domain.entities.route.UtilsForEntities.getWorkTime

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ItemHomeScreen(
    route: Route,
    alpha: Float,
    isExpand: Boolean,
    onDelete: (Route) -> Unit,
    onClick: () -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
    val revealState = rememberRevealState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(isExpand) {
        if (!isExpand) {
            scope.launch {
                revealState.animateTo(RevealValue.Default)
            }
        }
    }

    RevealSwipe(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 8.dp, end = 8.dp, bottom = 6.dp)
            .height(80.dp),
        enableSwipe = isExpand,
        state = revealState,
        maxRevealDp = 75.dp,
        directions = setOf(
            RevealDirection.EndToStart
        ),
        hiddenContentEnd = {
            IconButton(onClick = {
                onDelete(route)
                scope.launch {
                    revealState.animateTo(RevealValue.Default)
                }
            }) {
                Icon(
                    modifier = Modifier.padding(horizontal = 25.dp),
                    imageVector = Icons.Outlined.Delete,
                    tint = Color.White,
                    contentDescription = null
                )
            }
        },
        backgroundCardEndColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
        shape = Shapes.medium
    ) {
        Card(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(80.dp)
                .clickable { onClick() },
            shape = Shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = MaterialTheme.colorScheme.contentColorFor(containerColor)
                    .copy(alpha = alpha)
            )
        ) {
            ConstraintLayout(
                modifier = Modifier.fillMaxWidth()
            ) {
                val (date, verticalDividerFirst, station, verticalDividerSecond, workTime) = createRefs()
                DateElementItem(
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .constrainAs(date) {
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                        },
                    date = route.basicData.timeStartWork
                )
                CustomDivider(modifier = Modifier
                    .constrainAs(verticalDividerFirst) {
                        start.linkTo(date.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
                    .padding(vertical = 10.dp),
                    thickness = 0.5.dp, orientation = Orientation.Vertical)
                DetailsElementItem(
                    modifier = Modifier
                        .constrainAs(station) {
                            top.linkTo(parent.top)
                            start.linkTo(verticalDividerFirst.end)
                            end.linkTo(verticalDividerSecond.start)
                            bottom.linkTo(parent.bottom)
                            width = fillToConstraints
                            height = fillToConstraints
                        }
                        .padding(horizontal = 16.dp),
                    route = route
                )
                CustomDivider(
                    modifier = Modifier
                        .constrainAs(verticalDividerSecond) {
                            end.linkTo(anchor = workTime.start)
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                        }
                        .padding(vertical = 10.dp, horizontal = 2.dp),
                    thickness = 0.5.dp,
                    orientation = Orientation.Vertical)

                val workTimeValue = route.getWorkTime()

                WorkTimeElementItem(modifier = Modifier
                    .constrainAs(workTime) {
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
                    .padding(horizontal = 12.dp),
                    workTime = workTimeValue)
            }
        }
    }
}

@Composable
fun DateElementItem(
    modifier: Modifier = Modifier, date: Long?
) {
    val day = DateAndTimeConverter.getDayOfMonth(date)
    val month = DateAndTimeConverter.getMonthShorthand(date)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .wrapContentWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = day, style = AppTypography.getType().bodyLarge)
        Text(text = month, style = AppTypography.getType().bodyLarge)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailsElementItem(
    modifier: Modifier = Modifier, route: Route
) {
    val textFirstStation = route.trains.firstOrNull()?.stations?.firstOrNull()?.stationName ?: ""
    var textLastStation = route.trains.lastOrNull()?.stations?.lastOrNull()?.stationName ?: ""
    if (textFirstStation == textLastStation) {
        textLastStation = ""
    }
    val divider = if (textLastStation.isBlank()) "" else " - "

    val textStation = "$textFirstStation $divider $textLastStation"

    val textLocoSeries = if (route.locomotives.isNotEmpty()) {
        route.locomotives.first().series ?: ""
    } else {
        ""
    }
    val textLocoNumber = if (route.locomotives.isNotEmpty()) {
        "№${route.locomotives.first().number ?: ""}"
    } else {
        ""
    }
    val dots = if (route.locomotives.size > 1) {
        "..."
    } else {
        ""
    }

    val textLoco = "$textLocoSeries $textLocoNumber $dots"


    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            modifier = Modifier.basicMarquee(animationMode = MarqueeAnimationMode.WhileFocused),
            text = textStation,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.getType().bodyLarge
        )
        Text(
            modifier = Modifier.basicMarquee(animationMode = MarqueeAnimationMode.WhileFocused),
            text = textLoco,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.getType().bodyLarge
        )
    }
}

@Composable
fun WorkTimeElementItem(
    modifier: Modifier = Modifier,
    workTime: Long?
) {
    val textTime = DateAndTimeConverter.getTimeInStringFormat(workTime)
    Text(modifier = modifier, text = textTime, style = AppTypography.getType().bodyLarge)
}