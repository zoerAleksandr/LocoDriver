package com.z_company.route.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
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
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ItemHomeScreen(
    modifier: Modifier = Modifier,
    route: Route,
    isExpand: Boolean,
    onDelete: (Route) -> Unit,
    requiredSizeText: TextUnit,
    changingTextSize: (TextUnit) -> Unit,
    onClick: () -> Unit,
) {
    val containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                .clickable { onClick() },
            shape = Shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = MaterialTheme.colorScheme.contentColorFor(containerColor)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.175f)
                ) {
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
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.175f)
                ) {
                    route.basicData.timeStartWork?.let { timeStartWork ->
                        AutoSizeText(
                            modifier = Modifier.padding(end = 8.dp),
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
                }
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .fillMaxWidth()
                        .weight(0.45f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    route.trains.firstOrNull()?.stations?.firstOrNull()?.stationName?.let { station ->
                        AutoSizeText(
                            text = station,
                            style = AppTypography.getType().headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Light,
                            maxTextSize = requiredSizeText,
                            minTextSize = 22.sp,
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
                        .fillMaxSize()
                        .weight(0.2f),
                    contentAlignment = Alignment.Center
                ) {
                    AutoSizeText(
                        text = DateAndTimeConverter.getTimeInStringFormat(workTimeValue),
                        style = AppTypography.getType().headlineSmall,
                        fontWeight = FontWeight.Light,
                        maxTextSize = requiredSizeText,
                        onTextLayout = { textLayoutResult ->
                            val size = textLayoutResult.layoutInput.style.fontSize
                            changingTextSize(size)
                        }
                    )
                }
            }
        }
    }
}
