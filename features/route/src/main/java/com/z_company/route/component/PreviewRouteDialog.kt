package com.z_company.route.component

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.TrainAssist
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.fullRest
import com.z_company.domain.entities.route.UtilsForEntities.getFollowingTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getBreakDuration
import com.z_company.domain.entities.route.UtilsForEntities.shortRest
import com.z_company.domain.util.CalculationEnergy
import com.z_company.domain.util.CalculationEnergy.rounding
import com.z_company.domain.util.ifNullOrBlank
import com.z_company.domain.util.str
import com.z_company.domain.util.times
import com.z_company.route.R
import kotlin.collections.set

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun PreviewRouteDialog(
    showContextDialog: (Boolean) -> Unit,
    routeForPreview: Route,
    dateAndTimeConverter: DateAndTimeConverter?,
    syncRoute: (Route) -> Unit,
    minTimeRest: Long?,
    homeRest: Long?,
    setFavoriteState: (Route) -> Unit,
    onRouteClick: (String) -> Unit,
    makeCopyRoute: (String) -> Unit,
    showDialogConfirmRemove: (Boolean, Route) -> Unit
) {
    val accentBlue = MaterialTheme.colorScheme.tertiary
    val primaryColor = MaterialTheme.colorScheme.primary
    val dangerRed = MaterialTheme.colorScheme.error
    val largeShape = Shapes.large

    val heightScreen = LocalConfiguration.current.screenHeightDp

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 32.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        showContextDialog(false)
                    }
                )
            },
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(
                        min = heightScreen.times(0.3f).dp,
                        max = heightScreen.times(0.7f).dp
                    )
                    .padding(start = 12.dp, end = 12.dp, top = 30.dp, bottom = 12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(
                            topStart = 24.dp,
                            topEnd = 24.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .clip(
                        RoundedCornerShape(
                            topStart = 24.dp,
                            topEnd = 24.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .clickable {}
            ) {
                PreviewRoute(
                    routeForPreview,
                    minTimeRest,
                    homeRest,
                    dateAndTimeConverter
                )
            }
        }

        // Action buttons
        item {
            Column(
                modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp)
                    .fillMaxWidth()
            ) {
                // Primary button row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Просмотр — primary accent button (full width)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(accentBlue, largeShape)
                            .clickable {
                                showContextDialog(false)
                                onRouteClick(routeForPreview.basicData.id)
                            }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_visibility_24),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Просмотр",
                                style = AppTypography.getType().bodyMedium.copy(
                                    fontWeight = FontWeight.W600,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Сохранить в облаке
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, largeShape)
                            .clickable {
                                showContextDialog(false)
                                syncRoute(routeForPreview)
                            }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Image(
                                modifier = Modifier.size(18.dp),
                                painter = painterResource(id = R.drawable.sync_on_icon),
                                contentDescription = null,
                            )
                            Text(
                                text = "Синхронизировать",
                                style = AppTypography.getType().bodyMedium.copy(
                                    fontWeight = FontWeight.W600,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    // В избранное
                    val isFavorite = routeForPreview.basicData.isFavorite
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, largeShape)
                            .clickable {
                                showContextDialog(false)
                                setFavoriteState(routeForPreview)
                            }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Icon(
                                modifier = Modifier.size(18.dp),
                                painter = if (isFavorite) painterResource(R.drawable.favorite_fill_24px)
                                else painterResource(R.drawable.favorite_24px),
                                tint = if (isFavorite) Color(0xFFf1642e) else MaterialTheme.colorScheme.onSurface,
                                contentDescription = null,
                            )
                            Text(
                                text = if (isFavorite) "Избранное" else "Избранное",
                                style = AppTypography.getType().bodyMedium.copy(
                                    fontWeight = FontWeight.W600,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Icon buttons row (3 columns)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Дублировать
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, largeShape)
                            .clickable {
                                showContextDialog(false)
                                makeCopyRoute(routeForPreview.basicData.id)
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.outline_content_copy_24),
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Дублировать",
                                style = AppTypography.getType().bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = primaryColor
                                )
                            )
                        }
                    }

                    // Удалить
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                dangerRed.copy(alpha = 0.3f),
                                largeShape
                            )
                            .clickable {
                                showContextDialog(false)
                                showDialogConfirmRemove(true, routeForPreview)
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.delete_24px),
                                contentDescription = null,
                                tint = dangerRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Удалить",
                                style = AppTypography.getType().bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = dangerRed
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreviewRoute(
    route: Route?,
    minTimeRest: Long?,
    homeRest: Long?,
    dateAndTimeConverter: DateAndTimeConverter?
) {
    val AccentBlue = MaterialTheme.colorScheme.tertiary
    val primaryColor = MaterialTheme.colorScheme.primary
    val hintColor = primaryColor.copy(alpha = 0.6f)
    val AccentGreen = Color(0xFF34C98A)
    val WarnOrange = Color(0xFFF0A84F)

    val CardShape = Shapes.large
    val SmallCardShape = RoundedCornerShape(12.dp)
    val ChipShape = RoundedCornerShape(6.dp)

    val styleTitle = AppTypography.getType().titleSmall.copy(
        fontWeight = FontWeight.W700,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp,
        color = primaryColor
    )
    val styleHint = AppTypography.getType().bodySmall.copy(
        fontWeight = FontWeight.W400,
        fontSize = 11.sp,
        color = primaryColor
    )

    val locomotiveExpandItemState = remember {
        mutableStateMapOf<Int, Boolean>()
    }
    val trainExpandItemState = remember {
        mutableStateMapOf<Int, Boolean>()
    }
    route?.let {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Handle bar
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(99.dp)
                            )
                    )
                }
            }

            // Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 16.dp)
                        .animateItem()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "МАРШРУТ",
                                style = styleTitle,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = route.basicData.number ?: "б/н",
                                style = AppTypography.getType().headlineSmall.copy(
                                    fontWeight = FontWeight.W800,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 26.sp
                                ),
                            )
                            route.basicData.timeStartWork?.let {
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            AccentBlue.copy(alpha = 0.12f),
                                            ChipShape
                                        )
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = dateAndTimeConverter?.getDateFromDateLong(route.basicData.timeStartWork)
                                            ?: "...",
                                        style = AppTypography.getType().bodySmall.copy(
                                            fontWeight = FontWeight.W500,
                                            color = AccentBlue,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }
                        val workTime = ConverterLongToTime.getTimeInStringFormat(route.getWorkTime())
                        Text(
                            modifier = Modifier.weight(1f),
                            text = workTime,
                            textAlign = TextAlign.End,
                            style = AppTypography.getType().headlineSmall.copy(
                                fontWeight = FontWeight.W800,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 26.sp
                            ),
                        )
                    }
                }
            }

            // Divider
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            // Work time section
            item {
                if (route.basicData.timeStartWork != null || route.basicData.timeEndWork != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .animateItem()
                    ) {
                        Text(
                            text = "РАБОЧЕЕ ВРЕМЯ",
                            style = styleTitle,
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Time cards grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Start time card
                            route.basicData.timeStartWork?.let { startWork ->
                                TimeCard(
                                    modifier = Modifier.weight(1f),
                                    label = "НАЧАЛО",
                                    value = dateAndTimeConverter?.getTimeFromDateLong(startWork)
                                        ?: "...",
                                    subtitle = dateAndTimeConverter?.getDate(startWork) ?: "",
                                    accentColor = AccentGreen
                                )
                            }

                            // End time card
                            route.basicData.timeEndWork?.let { endWork ->
                                TimeCard(
                                    modifier = Modifier.weight(1f),
                                    label = "ОКОНЧАНИЕ",
                                    value = dateAndTimeConverter?.getTimeFromDateLong(endWork)
                                        ?: "...",
                                    subtitle = dateAndTimeConverter?.getDate(endWork) ?: "",
                                    accentColor = AccentBlue
                                )
                            }
                        }
                    }
                }
            }

            // Break row
            item {
                if (route.basicData.timeStartBreak != null && route.basicData.timeEndBreak != null) {
                    val breakDuration = route.getBreakDuration()
                    if (breakDuration > 0) {
                        val breakDurationText =
                            ConverterLongToTime.getTimeInStringFormat(breakDuration)
                        val breakStartText =
                            dateAndTimeConverter?.getTimeFromDateLong(route.basicData.timeStartBreak)
                                ?: "..."
                        val breakEndText =
                            dateAndTimeConverter?.getTimeFromDateLong(route.basicData.timeEndBreak)
                                ?: "..."

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 8.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    SmallCardShape
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .animateItem(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        WarnOrange.copy(alpha = 0.12f),
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    tint = WarnOrange,
                                    painter = painterResource(id = R.drawable.pause_24px),
                                    contentDescription = null
                                )
                            }

                            Text(
                                modifier = Modifier.weight(1f),
                                text = "Перерыв  $breakDurationText",
                                style = AppTypography.getType().bodyMedium.copy(
                                    fontWeight = FontWeight.W600,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                ),
                            )

                            Text(
                                text = "$breakStartText - $breakEndText",
                                style = AppTypography.getType().bodySmall.copy(
                                    color = primaryColor,
                                    fontSize = 12.sp
                                ),
                            )
                        }
                    }
                }
            }

            // Rest row
            item {
                if (route.basicData.timeStartWork != null && route.basicData.timeEndWork != null) {
                    val restText = if (route.basicData.restPointOfTurnover) {
                        "Отдых в ПО"
                    } else {
                        "Домашний"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 16.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                SmallCardShape
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .animateItem(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Rest icon
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    primaryColor.copy(alpha = 0.12f),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface,
                                painter = if (route.basicData.restPointOfTurnover)
                                    painterResource(id = R.drawable.hotel_24px)
                                else
                                    painterResource(id = R.drawable.gite_24px),
                                contentDescription = null
                            )
                        }

//                        Column(modifier = Modifier.weight(1f)) {
//                            Text(
//                                text = "Отдых",
//                                style = AppTypography.getType().bodySmall.copy(
//                                    color = hintColor,
//                                    fontSize = 13.sp
//                                ),
//                            )
                            Text(
                                modifier = Modifier.weight(1f),
                                text = restText,
                                style = AppTypography.getType().bodyMedium.copy(
                                    fontWeight = FontWeight.W600,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                ),
                            )
//                        }

                        // Rest time
                        if (route.basicData.restPointOfTurnover) {
                            minTimeRest?.let {
                                val shortRestText =
                                    dateAndTimeConverter?.getDateMiniAndTime(
                                        route.shortRest(minTimeRest)
                                    ) ?: "..."
                                val fullRestText = dateAndTimeConverter?.getDateMiniAndTime(
                                    route.fullRest(minTimeRest)
                                ) ?: "..."
                                Text(
                                    text = "$shortRestText\n$fullRestText",
                                    style = AppTypography.getType().bodySmall.copy(
                                        color = primaryColor,
                                        fontSize = 12.sp
                                    ),
                                )
                            }
                        } else {
                            homeRest?.let {
                                val homeRestText =
                                    dateAndTimeConverter?.getDateMiniAndTime(homeRest) ?: "..."
                                Text(
                                    text = "до $homeRestText",
                                    style = AppTypography.getType().bodySmall.copy(
                                        color = primaryColor,
                                        fontSize = 12.sp
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            // Locomotives section
            if (route.locomotives.isNotEmpty()) {
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 16.dp)
                            .animateItem()
                    ) {
                        Text(
                            text = "ЛОКОМОТИВ",
                            style = styleTitle,
                        )
                    }
                }
            }

            itemsIndexed(
                items = route.locomotives,
                key = { _, item -> item.locoId }
            ) { index, locomotive ->
                if (locomotiveExpandItemState[index] == null) {
                    locomotiveExpandItemState[index] = true
                }
                val typeLocoText = when (locomotive.type) {
                    LocoType.ELECTRIC -> "Электровоз"
                    LocoType.DIESEL -> "Тепловоз"
                }
                val seriesText = locomotive.series.ifNullOrBlank { "" }
                val numberText = locomotive.number.ifNullOrBlank { "" }

                val rotationSectionButton =
                    animateFloatAsState(
                        targetValue = if (locomotiveExpandItemState[index]!!) 180f else 0f,
                        label = ""
                    )

                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .animateItem()
                ) {
                    // Loco card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, CardShape)
                    ) {
                        // Loco card header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Loco icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        AccentBlue.copy(alpha = 0.12f),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    modifier = Modifier.size(24.dp),
                                    tint = AccentBlue,
                                    painter = when (locomotive.type) {
                                        LocoType.ELECTRIC -> painterResource(id = R.drawable.electric_loco)
                                        LocoType.DIESEL -> painterResource(id = R.drawable.diesel_loco)
                                    },
                                    contentDescription = null
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                val displayName = if (locomotive.series != null) {
                                    "$seriesText${if (numberText.isNotEmpty()) " - $numberText" else ""}"
                                } else {
                                    typeLocoText
                                }
                                Text(
                                    text = displayName,
                                    style = AppTypography.getType().bodyLarge.copy(
                                        fontWeight = FontWeight.W700,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 18.sp
                                    ),
                                )
//                                Text(
//                                    text = typeLocoText,
//                                    style = AppTypography.getType().bodySmall.copy(
//                                        color = hintColor,
//                                        fontSize = 12.sp
//                                    ),
//                                )
                            }

                            // Tag
//                            if (locomotive.series != null) {
//                                Box(
//                                    modifier = Modifier
//                                        .background(
//                                            AccentBlue.copy(alpha = 0.12f),
//                                            ChipShape
//                                        )
//                                        .padding(horizontal = 8.dp, vertical = 3.dp)
//                                ) {
//                                    Text(
//                                        text = seriesText,
//                                        style = AppTypography.getType().bodySmall.copy(
//                                            color = AccentBlue,
//                                            fontSize = 11.sp
//                                        ),
//                                    )
//                                }
//                            }

                            // Expand button
                            IconButton(
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer(
                                        rotationZ = rotationSectionButton.value
                                    ),
                                onClick = {
                                    locomotiveExpandItemState[index] =
                                        !locomotiveExpandItemState[index]!!
                                }) {
                                Icon(
                                    painter = painterResource(R.drawable.keyboard_arrow_down_24px),
                                    contentDescription = null,
                                    tint = hintColor
                                )
                            }
                        }

                        // Acceptance/Delivery info
                        val timeStartAcceptedText =
                            dateAndTimeConverter?.getTimeFromDateLong(locomotive.timeStartOfAcceptance)
                                ?: "..."
                        val timeEndAcceptedText =
                            dateAndTimeConverter?.getTimeFromDateLong(locomotive.timeEndOfAcceptance)
                                ?: "..."
                        val timeStartDeliveryText =
                            dateAndTimeConverter?.getTimeFromDateLong(locomotive.timeStartOfDelivery)
                                ?: "..."
                        val timeEndDeliveryText =
                            dateAndTimeConverter?.getTimeFromDateLong(locomotive.timeEndOfDelivery)
                                ?: "..."

                        if (locomotive.timeStartOfAcceptance != null || locomotive.timeEndOfAcceptance != null ||
                            locomotive.timeStartOfDelivery != null || locomotive.timeEndOfDelivery != null
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                if (locomotive.timeStartOfAcceptance != null || locomotive.timeEndOfAcceptance != null) {
                                    Text(
                                        text = "Приемка: $timeStartAcceptedText - $timeEndAcceptedText",
                                        style = styleHint,
                                    )
                                }
                                if (locomotive.timeStartOfDelivery != null || locomotive.timeEndOfDelivery != null) {
                                    Text(
                                        text = "Сдача: $timeStartDeliveryText - $timeEndDeliveryText",
                                        style = styleHint,
                                    )
                                }
                            }
                        }

                        // Total consumption summary
                        when (locomotive.type) {
                            LocoType.ELECTRIC -> {
                                val totalEnergy = locomotive.electricSectionList.mapNotNull { sec ->
                                    CalculationEnergy.getTotalEnergyConsumption(
                                        accepted = sec.acceptedEnergy,
                                        delivery = sec.deliveryEnergy
                                    )
                                }.takeIf { it.isNotEmpty() }?.sum()

                                val totalRecovery = locomotive.electricSectionList.mapNotNull { sec ->
                                    CalculationEnergy.getTotalEnergyConsumption(
                                        accepted = sec.acceptedRecovery,
                                        delivery = sec.deliveryRecovery
                                    )
                                }.takeIf { it.isNotEmpty() }?.sum()

                                if (totalEnergy != null || totalRecovery != null) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 10.dp
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Итого:",
                                            style = styleHint.copy(fontWeight = FontWeight.W600),
                                        )
                                        totalEnergy?.let {
                                            Text(
                                                text = "расход ${rounding(it, 2).str()}",
                                                style = styleHint,
                                            )
                                        }
                                        totalRecovery?.let {
                                            Text(
                                                text = "рекуперация ${rounding(it, 2).str()}",
                                                style = styleHint,
                                            )
                                        }
                                    }
                                }
                            }

                            LocoType.DIESEL -> {
                                val totalFuel = locomotive.dieselSectionList.mapNotNull { sec ->
                                    CalculationEnergy.getTotalFuelConsumption(
                                        accepted = sec.acceptedFuel,
                                        delivery = sec.deliveryFuel,
                                        refuel = sec.fuelSupply
                                    )
                                }.takeIf { it.isNotEmpty() }?.sum()

                                val totalFuelKilo = locomotive.dieselSectionList.mapNotNull { sec ->
                                    val accKilo = sec.acceptedFuel.times(sec.coefficient)
                                    val delKilo = sec.deliveryFuel.times(sec.coefficient)
                                    CalculationEnergy.getTotalFuelInKiloConsumption(
                                        acceptedInKilo = accKilo,
                                        deliveryInKilo = delKilo,
                                        refuelInKilo = sec.fuelSupplyInKilo
                                    )
                                }.takeIf { it.isNotEmpty() }?.sum()

                                if (totalFuel != null || totalFuelKilo != null) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 10.dp
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Итого:",
                                            style = styleHint.copy(fontWeight = FontWeight.W600),
                                        )
                                        totalFuel?.let {
                                            Text(
                                                text = "${rounding(it, 2).str()} л.",
                                                style = styleHint,
                                            )
                                        }
                                        totalFuelKilo?.let {
                                            Text(
                                                text = "${rounding(it, 2).str()} кг.",
                                                style = styleHint,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Sections (expanded)
                        AnimatedVisibility(visible = locomotiveExpandItemState[index]!!) {
                            when (locomotive.type) {
                                LocoType.ELECTRIC -> {
                                    Column {
                                        locomotive.electricSectionList.forEachIndexed { sIndex, sectionElectric ->
                                            val acceptedEnergyText =
                                                sectionElectric.acceptedEnergy?.str() ?: ""
                                            val deliveryEnergyText =
                                                sectionElectric.deliveryEnergy?.str() ?: ""
                                            val acceptedRecoveryText =
                                                sectionElectric.acceptedRecovery?.str() ?: ""
                                            val deliveryRecoveryText =
                                                sectionElectric.deliveryRecovery?.str() ?: ""
                                            val consumptionEnergy =
                                                CalculationEnergy.getTotalEnergyConsumption(
                                                    accepted = sectionElectric.acceptedEnergy,
                                                    delivery = sectionElectric.deliveryEnergy
                                                )?.str() ?: ""
                                            val consumptionRecovery =
                                                CalculationEnergy.getTotalEnergyConsumption(
                                                    accepted = sectionElectric.acceptedRecovery,
                                                    delivery = sectionElectric.deliveryRecovery
                                                )?.str() ?: ""

                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outline.copy(
                                                    alpha = 0.3f
                                                )
                                            )
                                            Row(
                                                modifier = Modifier.padding(
                                                    horizontal = 16.dp,
                                                    vertical = 10.dp
                                                ),
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                // Section number badge
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.outline.copy(
                                                                alpha = 0.3f
                                                            ),
                                                            RoundedCornerShape(6.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${sIndex + 1}",
                                                        style = AppTypography.getType().bodySmall.copy(
                                                            color = hintColor,
                                                            fontSize = 11.sp
                                                        ),
                                                    )
                                                }
                                                Column {
                                                    if (sectionElectric.acceptedEnergy != null ||
                                                        sectionElectric.deliveryEnergy != null ||
                                                        sectionElectric.acceptedRecovery != null ||
                                                        sectionElectric.deliveryRecovery != null
                                                    ) {
                                                        if (sectionElectric.acceptedEnergy != null ||
                                                            sectionElectric.deliveryEnergy != null
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(
                                                                    4.dp
                                                                )
                                                            ) {
                                                                Icon(
                                                                    modifier = Modifier.size(14.dp),
                                                                    tint = hintColor,
                                                                    painter = painterResource(id = R.drawable.electric_bolt_24px),
                                                                    contentDescription = null
                                                                )
                                                                Text(
                                                                    text = "$acceptedEnergyText - $deliveryEnergyText",
                                                                    style = styleHint,
                                                                )
                                                            }
                                                        }
                                                        if (sectionElectric.acceptedRecovery != null ||
                                                            sectionElectric.deliveryRecovery != null
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(
                                                                    4.dp
                                                                )
                                                            ) {
                                                                Icon(
                                                                    modifier = Modifier.size(14.dp),
                                                                    tint = hintColor,
                                                                    painter = painterResource(id = R.drawable.cycle_24px),
                                                                    contentDescription = null
                                                                )
                                                                Text(
                                                                    text = "$acceptedRecoveryText - $deliveryRecoveryText",
                                                                    style = styleHint,
                                                                )
                                                            }
                                                        }
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                8.dp
                                                            )
                                                        ) {
                                                            if (sectionElectric.acceptedEnergy != null &&
                                                                sectionElectric.deliveryEnergy != null
                                                            ) {
                                                                Text(
                                                                    text = "Расход: $consumptionEnergy",
                                                                    style = styleHint,
                                                                )
                                                            }
                                                            if (sectionElectric.acceptedRecovery != null &&
                                                                sectionElectric.deliveryRecovery != null
                                                            ) {
                                                                Text(
                                                                    text = "Рекуперация: $consumptionRecovery",
                                                                    style = styleHint,
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        Text(
                                                            text = "Нет данных",
                                                            style = styleHint.copy(fontStyle = FontStyle.Italic)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                LocoType.DIESEL -> {
                                    Column {
                                        locomotive.dieselSectionList.forEachIndexed { sIndex, sectionDiesel ->
                                            val consumption =
                                                CalculationEnergy.getTotalFuelConsumption(
                                                    accepted = sectionDiesel.acceptedFuel,
                                                    delivery = sectionDiesel.deliveryFuel,
                                                    refuel = sectionDiesel.fuelSupply
                                                )
                                            val consumptionText =
                                                rounding(consumption, 2).str()
                                            val acceptedText = rounding(
                                                sectionDiesel.acceptedFuel, 2
                                            ).str()
                                            val deliveryText = rounding(
                                                sectionDiesel.deliveryFuel, 2
                                            ).str()
                                            val acceptedInKilo =
                                                sectionDiesel.acceptedFuel.times(sectionDiesel.coefficient)
                                            val acceptedInKiloText =
                                                rounding(acceptedInKilo, 2).str()
                                            val deliveryInKilo =
                                                sectionDiesel.deliveryFuel.times(sectionDiesel.coefficient)
                                            val deliveryInKiloText =
                                                rounding(deliveryInKilo, 2).str()
                                            val fuelSupplyText =
                                                sectionDiesel.fuelSupply.str()
                                            val coefficientText =
                                                sectionDiesel.coefficient.str()
                                            val consumptionInKilo =
                                                CalculationEnergy.getTotalFuelInKiloConsumption(
                                                    acceptedInKilo = acceptedInKilo,
                                                    deliveryInKilo = deliveryInKilo,
                                                    refuelInKilo = sectionDiesel.fuelSupplyInKilo
                                                )
                                            val consumptionInKiloText =
                                                consumptionInKilo.str()

                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outline.copy(
                                                    alpha = 0.3f
                                                )
                                            )
                                            Row(
                                                modifier = Modifier.padding(
                                                    horizontal = 16.dp,
                                                    vertical = 10.dp
                                                ),
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                // Section number badge
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.outline.copy(
                                                                alpha = 0.3f
                                                            ),
                                                            RoundedCornerShape(6.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${sIndex + 1}",
                                                        style = AppTypography.getType().bodySmall.copy(
                                                            color = primaryColor,
                                                            fontSize = 11.sp
                                                        ),
                                                    )
                                                }
                                                Column {
                                                    if (acceptedText.isNotEmpty() || deliveryText.isNotEmpty()) {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                4.dp
                                                            )
                                                        ) {
                                                            Text(
                                                                text = "$acceptedText - $deliveryText",
                                                                style = styleHint
                                                            )
                                                            Text(
                                                                text = "$consumptionText л.",
                                                                style = styleHint
                                                            )
                                                        }
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                4.dp
                                                            )
                                                        ) {
                                                            Text(
                                                                text = "$acceptedInKiloText - $deliveryInKiloText",
                                                                style = styleHint
                                                            )
                                                            Text(
                                                                text = "$consumptionInKiloText кг.",
                                                                style = styleHint
                                                            )
                                                        }
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                4.dp
                                                            )
                                                        ) {
                                                            Text(
                                                                text = "k: $coefficientText",
                                                                style = styleHint
                                                            )
                                                            sectionDiesel.fuelSupply?.let {
                                                                Text(
                                                                    text = "Снабжение: $fuelSupplyText л.",
                                                                    style = styleHint
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        Text(
                                                            text = "Нет данных",
                                                            style = styleHint.copy(fontStyle = FontStyle.Italic)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Trains section
            if (route.trains.isNotEmpty()) {
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 16.dp)
                            .animateItem()
                    ) {
                        Text(
                            text = "ПОЕЗД",
                            style = styleTitle,
                        )
                    }
                }
            }
            itemsIndexed(
                route.trains,
                key = { _, train -> train.trainId }
            ) { index, train ->
                if (trainExpandItemState[index] == null) {
                    trainExpandItemState[index] = true
                }
                val numberText = train.number.ifNullOrBlank { "" }
                val weightText = train.weight.ifNullOrBlank { "" }
                val axleText = train.axle.ifNullOrBlank { "" }
                val lengthText = train.conditionalLength.ifNullOrBlank { "" }

                val rotationStationButton =
                    animateFloatAsState(
                        targetValue = if (trainExpandItemState[index]!!) 180f else 0f,
                        label = ""
                    )

                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .animateItem()
                ) {
                    // Train card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, CardShape)
                    ) {
                        // Train header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (numberText.isNotBlank()) "№ $numberText" else "Поезд",
                                    style = AppTypography.getType().bodyLarge.copy(
                                        fontWeight = FontWeight.W600,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 18.sp
                                    ),
                                )
                                // Meta chips row
                                Row(
                                    modifier = Modifier.padding(top = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (train.weight != null) {
                                        MetaChip(text = "Вес: $weightText т")
                                    }
                                    if (train.axle != null) {
                                        MetaChip(text = "Оси: $axleText")
                                    }
                                    if (train.conditionalLength != null) {
                                        MetaChip(text = "У.д.: $lengthText")
                                    }
                                }
                            }

                            IconButton(
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer(
                                        rotationZ = rotationStationButton.value
                                    ),
                                onClick = {
                                    trainExpandItemState[index] =
                                        !trainExpandItemState[index]!!
                                }) {
                                Icon(
                                    painter = painterResource(R.drawable.keyboard_arrow_down_24px),
                                    contentDescription = null,
                                    tint = hintColor
                                )
                            }
                        }

                        // Stations (expanded)
                        AnimatedVisibility(visible = trainExpandItemState[index]!!) {
                            Column {
                                train.stations.forEach { station ->
                                    val stationNameText =
                                        station.stationName.ifNullOrBlank { "" }
                                    val timeArrival =
                                        dateAndTimeConverter?.getTimeFromDateLong(station.timeArrival)
                                            ?: "..."
                                    val timeDeparture =
                                        dateAndTimeConverter?.getTimeFromDateLong(station.timeDeparture)
                                            ?: "..."
                                    val displayTime = buildString {
                                        if (station.timeArrival != null) append(timeArrival)
                                        if (station.timeArrival != null && station.timeDeparture != null) append(
                                            " - "
                                        )
                                        if (station.timeDeparture != null) append(timeDeparture)
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Station dot
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(
                                                    Color.Transparent,
                                                    CircleShape
                                                )
                                                .then(
                                                    Modifier.background(
                                                        Color.Transparent,
                                                        CircleShape
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Transparent)
                                            ) {
                                                // Border dot
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            AccentBlue,
                                                            CircleShape
                                                        )
                                                        .padding(2.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.surfaceVariant,
                                                            CircleShape
                                                        )
                                                )
                                            }
                                        }

                                        Text(
                                            text = stationNameText,
                                            style = AppTypography.getType().bodyMedium.copy(
                                                fontWeight = FontWeight.W600,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 15.sp
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (displayTime.isNotBlank()) {
                                            Text(
                                                text = displayTime,
                                                style = AppTypography.getType().bodySmall.copy(
                                                    color = AccentBlue,
                                                    fontSize = 14.sp
                                                ),
                                            )
                                        }
                                    }
                                }

                                // Train assist info
                                train.pusher?.let { assist ->
                                    TrainAssistRow(
                                        label = "Толкач",
                                        iconRes = R.drawable.ic_pusher_24px,
                                        assist = assist,
                                        styleHint = styleHint,
                                        accentColor = AccentBlue
                                    )
                                }
                                train.doubleTraction?.let { assist ->
                                    TrainAssistRow(
                                        label = "Двойная тяга",
                                        iconRes = R.drawable.ic_double_traction_24px,
                                        assist = assist,
                                        styleHint = styleHint,
                                        accentColor = AccentBlue
                                    )
                                }
                                train.doubledTrain?.let { assist ->
                                    TrainAssistRow(
                                        label = "Сдвоенный",
                                        iconRes = R.drawable.ic_doubled_train_24px,
                                        assist = assist,
                                        styleHint = styleHint,
                                        accentColor = AccentBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Passengers section
            if (route.passengers.isNotEmpty()) {
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 16.dp)
                            .animateItem()
                    ) {
                        Text(
                            text = "ПАССАЖИР",
                            style = styleTitle,
                        )
                    }
                }
            }
            items(
                route.passengers,
                key = { passenger -> passenger.passengerId }
            ) { passenger ->
                val numberText = passenger.trainNumber.ifNullOrBlank { "" }
                val stationDeparture = passenger.stationDeparture.ifNullOrBlank { "" }
                val stationArrival = passenger.stationArrival.ifNullOrBlank { "" }
                val timeDeparture =
                    dateAndTimeConverter?.getTimeFromDateLong(passenger.timeDeparture)
                        ?: "..."
                val timeArrival =
                    dateAndTimeConverter?.getTimeFromDateLong(passenger.timeArrival)
                        ?: "..."
                val timeFollowing =
                    ConverterLongToTime.getTimeInStringFormat(passenger.getFollowingTime())
                        .ifNullOrBlank { "" }
                val notesText = passenger.notes.ifNullOrBlank { "" }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .animateItem()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, CardShape)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                if (numberText.isNotBlank()) {
                                    Text(
                                        text = "№ $numberText",
                                        style = AppTypography.getType().bodyLarge.copy(
                                            fontWeight = FontWeight.W600,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 16.sp
                                        ),
                                    )
                                }
                                if (timeFollowing.isNotBlank()) {
                                    Text(
                                        text = timeFollowing,
                                        style = styleHint,
                                    )
                                }
                            }
                            Icon(
                                modifier = Modifier.size(24.dp),
                                tint = hintColor,
                                painter = painterResource(id = R.drawable.passenger_24px),
                                contentDescription = null
                            )
                        }

                        if (passenger.timeDeparture != null || passenger.timeArrival != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "$timeDeparture - $timeArrival", style = styleHint)
                            }
                        }
                        if (stationDeparture.isNotBlank() && stationArrival.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "$stationDeparture - $stationArrival",
                                    style = styleHint
                                )
                            }
                        }
                        if (notesText.isNotBlank()) {
                            Text(
                                text = notesText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = styleHint,
                            )
                        }
                    }
                }
            }

            // Notes section
            if (!route.basicData.notes.isNullOrBlank()) {
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 16.dp)
                            .animateItem()
                    ) {
                        Text(
                            text = "ЗАМЕТКИ",
                            style = styleTitle,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        route.basicData.notes?.let { notes ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        SmallCardShape
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    tint = hintColor,
                                    painter = painterResource(id = R.drawable.notes_24px),
                                    contentDescription = null
                                )
                                Text(
                                    text = notes,
                                    style = styleHint.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                )
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TimeCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    subtitle: String,
    accentColor: Color,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val hintColor = primaryColor.copy(alpha = 0.6f)

    val cardShape = Shapes.large

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, cardShape)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        )
        {
            Text(
                text = label,
                style = AppTypography.getType().bodySmall.copy(
                    fontWeight = FontWeight.W600,
                    letterSpacing = 0.8.sp,
                    color = accentColor,
                    fontSize = 10.sp
                ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = AppTypography.getType().headlineSmall.copy(
                    fontWeight = FontWeight.W600,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    lineHeight = 20.sp
                ),
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = AppTypography.getType().bodySmall.copy(
                        color = hintColor,
                        fontSize = 11.sp
                    ),
                )
            }
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val hintColor = primaryColor.copy(alpha = 0.6f)

    Box(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(5.dp)
            )
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = AppTypography.getType().bodySmall.copy(
                color = hintColor,
                fontSize = 11.sp
            ),
        )
    }
}

@Composable
private fun TrainAssistRow(
    label: String,
    iconRes: Int,
    assist: TrainAssist,
    styleHint: androidx.compose.ui.text.TextStyle,
    accentColor: Color,
) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            tint = accentColor,
            painter = painterResource(id = iconRes),
            contentDescription = null
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = styleHint.copy(fontWeight = FontWeight.W600),
            )
            val info = buildString {
                assist.locomotiveSeries?.let { if (it.isNotBlank()) append(it) }
                assist.locomotiveNumber?.let { if (it.isNotBlank()) append(" - $it") }
            }.trimStart(' ', '-').trim()
            if (info.isNotBlank()) {
                Text(text = info, style = styleHint)
            }
            assist.driverName?.let {
                if (it.isNotBlank()) {
                    Text(text = it, style = styleHint)
                }
            }
        }
    }
}
