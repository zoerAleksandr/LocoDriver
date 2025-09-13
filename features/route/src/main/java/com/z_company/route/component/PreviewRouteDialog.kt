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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.fullRest
import com.z_company.domain.entities.route.UtilsForEntities.getFollowingTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
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
    val redOrange = Color(0xFFf1642e)
    val heightScreen = LocalConfiguration.current.screenHeightDp

    Column(
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeightIn(
                    min = heightScreen.times(0.3f).dp,
                    max = heightScreen.times(0.65f).dp
                )
                .padding(start = 12.dp, end = 12.dp, top = 30.dp, bottom = 12.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = Shapes.medium
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

        Column(
            modifier = Modifier
                .padding(end = 12.dp)
                .fillMaxWidth(0.6f)
                .background(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = Shapes.medium
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable {
                        showContextDialog(false)
                        syncRoute(routeForPreview)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Сохранить в облаке",
                    style = AppTypography.getType().bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                )
                Image(
                    modifier = Modifier.size(25.dp),
                    painter = painterResource(id = R.drawable.sync_on_icon),
                    contentDescription = null,
                )
            }
            HorizontalDivider()
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 16.dp, vertical = 8.dp)
//                        .clickable {
//                            showContextDialog = false
//                            routeForPreview?.let { route ->
//                                val intent = getSharedIntent(route)
//                                val type = intent.type
//                                Log.d("ZZZ", "type = $type")
//                                context.startActivity(
//                                    Intent.createChooser(
//                                        intent,
//                                        "Поделиться маршрутом"
//                                    )
//                                )
//                            }
//                        },
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        text = "Поделиться",
//                        style = AppTypography.getType().bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
//                    )
//                    Image(
//                        modifier = Modifier.size(25.dp),
//                        imageVector = Icons.Outlined.Share,
//                        contentDescription = null,
//                    )
//                }
//                HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable {
                        showContextDialog(false)
                        setFavoriteState(routeForPreview)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val text =
                    if (routeForPreview.basicData.isFavorite) "Убрать из избранного" else "В избранное"
                val icon =
                    if (routeForPreview.basicData.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder
                val tint =
                    if (routeForPreview.basicData.isFavorite) redOrange else MaterialTheme.colorScheme.primary


                Text(
                    text = text,
                    style = AppTypography.getType().bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                )
                Icon(
                    modifier = Modifier.size(25.dp),
                    imageVector = icon,
                    tint = tint,
                    contentDescription = null,
                )
            }
            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable {
                        showContextDialog(false)
                        onRouteClick(routeForPreview.basicData.id)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Просмотр",
                    style = AppTypography.getType().bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                )
                Icon(
                    painter = painterResource(id = R.drawable.rounded_visibility_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable {
                        showContextDialog(false)
                        makeCopyRoute(routeForPreview.basicData.id)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Дублировать",
                    style = AppTypography.getType().bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    painter = painterResource(id = R.drawable.outline_content_copy_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable {
                        showContextDialog(false)
                        showDialogConfirmRemove(true, routeForPreview)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Удалить",
                    style = AppTypography.getType().bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Icon(
                    painter = painterResource(id = R.drawable.delete_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
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
    val styleTitle = AppTypography.getType().titleSmall.copy(
        fontWeight = FontWeight.W600,
        color = MaterialTheme.colorScheme.primary
    )
    val styleData = AppTypography.getType().bodyMedium.copy(
        fontWeight = FontWeight.W400,
        color = MaterialTheme.colorScheme.primary
    )
    val styleHint = AppTypography.getType().bodySmall.copy(
        fontWeight = FontWeight.W300,
        color = MaterialTheme.colorScheme.primary
    )
    val paddingBetweenBlocks = 20.dp
    val paddingInsideBlock = 14.dp
    val paddingIcon = 12.dp
    val horizontalPaddingSecondItem = 32.dp
    val iconSize = 50.dp
    val iconSizeSecond = iconSize * .8f
    val iconMiniSize = 18.dp

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
                .wrapContentHeight()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingBetweenBlocks)
                        .animateItemPlacement(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Маршрут ${route.basicData.number ?: "б/н"}  ",
                        style = styleTitle,
                    )
                }
            }
            item {
                route.basicData.timeStartWork?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingBetweenBlocks)
                            .animateItemPlacement(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = dateAndTimeConverter?.getDateFromDateLong(route.basicData.timeStartWork)
                                ?: "загрузка",
                            style = styleData,
                        )
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingBetweenBlocks)
                        .animateItemPlacement(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Рабочее время",
                        style = styleTitle,
                    )
                }
            }
            item {
                if (route.basicData.timeStartWork != null || route.basicData.timeEndWork != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingInsideBlock)
                            .animateItemPlacement(),
                    ) {
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(iconSize)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = Shapes.medium
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.fillMaxSize(0.7f),
                                tint = MaterialTheme.colorScheme.primary,
                                painter = painterResource(id = R.drawable.schedule_24px),
                                contentDescription = null
                            )
                        }
                        Column(modifier = Modifier.padding(start = paddingIcon)) {
                            Box {
                                Text(
                                    text = ConverterLongToTime.getTimeInStringFormat(route.getWorkTime()),
                                    style = styleData,
                                    maxLines = 1
                                )
                            }
                            Row {
                                Text(
                                    text = dateAndTimeConverter?.getTimeFromDateLong(route.basicData.timeStartWork)
                                        ?: "загрузка",
                                    style = styleHint,
                                    maxLines = 1
                                )

                                Text(
                                    text = " - ${dateAndTimeConverter?.getTimeFromDateLong(route.basicData.timeEndWork) ?: "загрузка"}",
                                    style = styleHint,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            item {
                if (route.basicData.timeStartWork != null && route.basicData.timeEndWork != null) {
                    val restText = if (route.basicData.restPointOfTurnover) {
                        "Отдых в ПО"
                    } else {
                        "Домашний отдых"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingInsideBlock)
                            .animateItemPlacement(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(iconSize)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = Shapes.medium
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (route.basicData.restPointOfTurnover) {
                                Icon(
                                    modifier = Modifier.fillMaxSize(0.7f),
                                    tint = MaterialTheme.colorScheme.primary,
                                    painter = painterResource(id = R.drawable.hotel_24px),
                                    contentDescription = null
                                )
                            } else {
                                Icon(
                                    modifier = Modifier.fillMaxSize(0.7f),
                                    tint = MaterialTheme.colorScheme.primary,
                                    painter = painterResource(id = R.drawable.gite_24px),
                                    contentDescription = null
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(start = paddingIcon)) {
                            Text(
                                text = restText,
                                style = styleData,
                                maxLines = 1,
                            )
                            if (route.basicData.restPointOfTurnover) {
                                minTimeRest?.let {
                                    val shortRestText =
                                        dateAndTimeConverter?.getDateMiniAndTime(
                                            route.shortRest(minTimeRest)
                                        ) ?: "загрузка"
                                    val fullRestText = dateAndTimeConverter?.getDateMiniAndTime(
                                        route.fullRest(minTimeRest)
                                    ) ?: "загрузка"
                                    Text(
                                        text = "$shortRestText - $fullRestText",
                                        style = styleHint,
                                        maxLines = 1,
                                    )
                                }
                            } else {
                                homeRest?.let {
                                    val homeRestInLongText =
                                        dateAndTimeConverter?.getDateMiniAndTime(
                                            homeRest
                                        ) ?: "загрузка"
                                    Text(
                                        text = "до $homeRestInLongText",
                                        style = styleHint,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (route.locomotives.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingBetweenBlocks)
                            .animateItemPlacement(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Локомотив",
                            style = styleTitle,
                        )
                    }
                }
            }
            itemsIndexed(
                items = route.locomotives,
                key = { _, item -> item.locoId }) { index, locomotive ->
                if (locomotiveExpandItemState[index] == null) {
                    locomotiveExpandItemState[index] = true
                }
                val typeLocoText = when (locomotive.type) {
                    LocoType.ELECTRIC -> "Электровоз"
                    LocoType.DIESEL -> "Тепловоз"
                }
                val seriesText = locomotive.series.ifNullOrBlank { "" }
                val numberText = locomotive.number.ifNullOrBlank { "" }
                val timeStartAcceptedText =
                    dateAndTimeConverter?.getTimeFromDateLong(locomotive.timeStartOfAcceptance)
                        ?: "загрузка"
                val timeEndAcceptedText =
                    dateAndTimeConverter?.getTimeFromDateLong(locomotive.timeEndOfAcceptance)
                        ?: "загрузка"
                val timeStartDeliveryText =
                    dateAndTimeConverter?.getTimeFromDateLong(locomotive.timeStartOfDelivery)
                        ?: "загрузка"
                val timeEndDeliveryText =
                    dateAndTimeConverter?.getTimeFromDateLong(locomotive.timeEndOfDelivery)
                        ?: "загрузка"

                val rotationSectionButton =
                    animateFloatAsState(
                        targetValue = if (locomotiveExpandItemState[index]!!) 180f else 0f,
                        label = ""
                    )

                Column(modifier = Modifier.animateItemPlacement()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingInsideBlock),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row {
                            // Icon
                            Box(
                                modifier = Modifier
                                    .size(iconSize)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = Shapes.medium
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                when (locomotive.type) {
                                    LocoType.ELECTRIC -> {
                                        Icon(
                                            modifier = Modifier.fillMaxSize(0.7f),
                                            tint = MaterialTheme.colorScheme.primary,
                                            painter = painterResource(id = R.drawable.electric_loco),
                                            contentDescription = null
                                        )
                                    }

                                    LocoType.DIESEL -> {
                                        Icon(
                                            modifier = Modifier.fillMaxSize(0.7f),
                                            tint = MaterialTheme.colorScheme.primary,
                                            painter = painterResource(id = R.drawable.diesel_loco),
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .padding(start = paddingIcon)
                            ) {
                                Row {
                                    if (locomotive.series == null) {
                                        Text(
                                            text = "$typeLocoText ",
                                            style = styleData,
                                        )
                                    } else {
                                        Text(
                                            text = seriesText,
                                            style = styleData,
                                        )
                                    }
                                    locomotive.number?.let {
                                        Text(
                                            text = " - $numberText",
                                            style = styleData,
                                        )
                                    }
                                }
                                if (locomotive.timeStartOfAcceptance != null || locomotive.timeEndOfAcceptance != null) {
                                    Row {
                                        Text(
                                            text = "Приемка: ",
                                            style = styleHint,
                                        )
                                        Text(
                                            text = "$timeStartAcceptedText - ",
                                            style = styleHint,
                                        )
                                        Text(
                                            text = timeEndAcceptedText,
                                            style = styleHint,
                                        )
                                    }
                                }
                                if (locomotive.timeStartOfDelivery != null || locomotive.timeEndOfDelivery != null) {
                                    Row {
                                        Text(
                                            text = "Сдача: ",
                                            style = styleHint,
                                        )
                                        Text(
                                            text = "$timeStartDeliveryText - ",
                                            style = styleHint,
                                        )
                                        Text(
                                            text = timeEndDeliveryText,
                                            style = styleHint,
                                        )
                                    }
                                }
                            }
                        }
                        IconButton(
                            modifier = Modifier.graphicsLayer(
                                rotationZ = rotationSectionButton.value
                            ),
                            onClick = {
                                locomotiveExpandItemState[index] =
                                    !locomotiveExpandItemState[index]!!
                            }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    when (locomotive.type) {
                        LocoType.ELECTRIC -> {
                            AnimatedVisibility(visible = locomotiveExpandItemState[index]!!) {
                                Column {
                                    locomotive.electricSectionList.forEachIndexed { index, sectionElectric ->
                                        val acceptedEnergyText =
                                            sectionElectric.acceptedEnergy?.toPlainString()
                                                ?: ""
                                        val deliveryEnergyText =
                                            sectionElectric.deliveryEnergy?.toPlainString()
                                                ?: ""
                                        val acceptedRecoveryText =
                                            sectionElectric.acceptedRecovery?.toPlainString()
                                                ?: ""
                                        val deliveryRecoveryText =
                                            sectionElectric.deliveryRecovery?.toPlainString()
                                                ?: ""
                                        val consumptionEnergy =
                                            CalculationEnergy.getTotalEnergyConsumption(
                                                accepted = sectionElectric.acceptedEnergy,
                                                delivery = sectionElectric.deliveryEnergy
                                            )?.toPlainString() ?: ""
                                        val consumptionRecovery =
                                            CalculationEnergy.getTotalEnergyConsumption(
                                                accepted = sectionElectric.acceptedRecovery,
                                                delivery = sectionElectric.deliveryRecovery
                                            )?.toPlainString() ?: ""

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    top = paddingInsideBlock,
                                                    start = horizontalPaddingSecondItem
                                                ),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            // Icon
                                            Box(
                                                modifier = Modifier
                                                    .size(iconSizeSecond)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        shape = Shapes.medium
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val image: Int = when (index) {
                                                    0 -> R.drawable.one
                                                    1 -> R.drawable.two
                                                    2 -> R.drawable.three
                                                    3 -> R.drawable.four
                                                    4 -> R.drawable.five
                                                    5 -> R.drawable.sex
                                                    6 -> R.drawable.seven
                                                    7 -> R.drawable.eight
                                                    8 -> R.drawable.nine
                                                    else -> R.drawable.one
                                                }
                                                Icon(
                                                    modifier = Modifier.fillMaxSize(0.7f),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    painter = painterResource(id = image),
                                                    contentDescription = null
                                                )
                                            }
                                            Column(modifier = Modifier.padding(start = paddingIcon)) {
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
                                                                paddingIcon / 2
                                                            )
                                                        ) {
                                                            // Icon energy symbol
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(iconMiniSize),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    modifier = Modifier.fillMaxSize(
                                                                        0.7f
                                                                    ),
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    painter = painterResource(id = R.drawable.electric_bolt_24px),
                                                                    contentDescription = null
                                                                )
                                                            }
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
                                                                paddingIcon / 2
                                                            )
                                                        ) {
                                                            // Icon recovery symbol
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(iconMiniSize),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    modifier = Modifier.fillMaxSize(
                                                                        0.7f
                                                                    ),
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    painter = painterResource(id = R.drawable.cycle_24px),
                                                                    contentDescription = null
                                                                )
                                                            }
                                                            Text(
                                                                text = "$acceptedRecoveryText - $deliveryRecoveryText",
                                                                style = styleHint,
                                                            )
                                                        }
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            paddingIcon / 2
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
                                                        style = styleHint
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        LocoType.DIESEL -> {
                            AnimatedVisibility(visible = locomotiveExpandItemState[index]!!) {
                                Column {
                                    locomotive.dieselSectionList.forEachIndexed { index, sectionDiesel ->
                                        val consumption =
                                            CalculationEnergy.getTotalFuelConsumption(
                                                accepted = sectionDiesel.acceptedFuel,
                                                delivery = sectionDiesel.deliveryFuel,
                                                refuel = sectionDiesel.fuelSupply
                                            )
                                        val consumptionInKilo =
                                            CalculationEnergy.getTotalFuelInKiloConsumption(
                                                consumption = consumption,
                                                coefficient = sectionDiesel.coefficient
                                            )
                                        val consumptionText = consumption.str()
                                        val consumptionInKiloText = consumptionInKilo.str()
                                        val acceptedText = sectionDiesel.acceptedFuel.str()
                                        val deliveryText = sectionDiesel.deliveryFuel.str()
                                        val acceptedInKilo =
                                            sectionDiesel.acceptedFuel.times(sectionDiesel.coefficient)
                                        val acceptedInKiloText =
                                            rounding(acceptedInKilo, 2).str()
                                        val deliveryInKilo =
                                            sectionDiesel.deliveryFuel.times(sectionDiesel.coefficient)
                                        val deliveryInKiloText =
                                            rounding(deliveryInKilo, 2).str()
                                        val fuelSupplyText = sectionDiesel.fuelSupply.str()
                                        val coefficientText = sectionDiesel.coefficient.str()

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    top = paddingInsideBlock,
                                                    start = horizontalPaddingSecondItem
                                                ),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            // Icon
                                            Box(
                                                modifier = Modifier
                                                    .size(iconSizeSecond)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        shape = Shapes.medium
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val image: Int = when (index) {
                                                    0 -> R.drawable.one
                                                    1 -> R.drawable.two
                                                    2 -> R.drawable.three
                                                    3 -> R.drawable.four
                                                    4 -> R.drawable.five
                                                    5 -> R.drawable.sex
                                                    6 -> R.drawable.seven
                                                    7 -> R.drawable.eight
                                                    8 -> R.drawable.nine
                                                    else -> R.drawable.one
                                                }
                                                Icon(
                                                    modifier = Modifier.fillMaxSize(0.7f),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    painter = painterResource(id = image),
                                                    contentDescription = null
                                                )
                                            }
                                            Column(modifier = Modifier.padding(start = paddingIcon)) {
                                                if (acceptedText.isNotEmpty() || deliveryText.isNotEmpty()) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            paddingIcon / 2
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
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            paddingIcon / 2
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
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            paddingIcon / 2
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
                                                        style = styleHint
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
            if (route.trains.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingBetweenBlocks)
                            .animateItemPlacement(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Поезд",
                            style = styleTitle,
                        )
                    }
                }
            }
            itemsIndexed(route.trains, key = { _, train -> train.trainId }) { index, train ->
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

                Column(modifier = Modifier.animateItemPlacement()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingInsideBlock),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row {
                            // Icon
                            Box(
                                modifier = Modifier
                                    .size(iconSize)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = Shapes.medium
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    modifier = Modifier.fillMaxSize(0.7f),
                                    tint = MaterialTheme.colorScheme.primary,
                                    painter = painterResource(id = R.drawable.description_24px),
                                    contentDescription = null
                                )
                            }
                            Column(modifier = Modifier.padding(start = paddingIcon)) {
                                Box {
                                    Text(
                                        text = numberText,
                                        style = styleData,
                                    )
                                }
                                Row {
                                    train.weight?.let {
                                        Text(
                                            text = "Вес: ",
                                            style = styleHint,
                                        )
                                        Text(
                                            text = weightText,
                                            style = styleHint,
                                        )
                                    }
                                    train.axle?.let {
                                        Text(
                                            text = "  Оси: ",
                                            style = styleHint,
                                        )
                                        Text(
                                            text = axleText,
                                            style = styleHint,
                                        )
                                    }
                                    train.conditionalLength?.let {
                                        Text(
                                            text = "  у.д.: ",
                                            style = styleHint,
                                        )
                                        Text(
                                            text = lengthText,
                                            style = styleHint,
                                        )
                                    }
                                }
                            }
                        }
                        IconButton(
                            modifier = Modifier.graphicsLayer(
                                rotationZ = rotationStationButton.value
                            ),
                            onClick = {
                                trainExpandItemState[index] = !trainExpandItemState[index]!!
                            }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    AnimatedVisibility(visible = trainExpandItemState[index]!!) {
                        Column {
                            train.stations.forEachIndexed { _, station ->
                                val stationNameText = station.stationName.ifNullOrBlank { "" }
                                val timeArrival =
                                    dateAndTimeConverter?.getTimeFromDateLong(station.timeArrival)
                                        ?: "загрузка"
                                val timeDeparture =
                                    dateAndTimeConverter?.getTimeFromDateLong(station.timeDeparture)
                                        ?: "загрузка"
                                // Icon
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            top = paddingInsideBlock,
                                            start = horizontalPaddingSecondItem
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(iconSizeSecond)
                                            .background(
                                                color = MaterialTheme.colorScheme.secondary,
                                                shape = Shapes.medium
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            modifier = Modifier.fillMaxSize(0.7f),
                                            tint = MaterialTheme.colorScheme.primary,
                                            painter = painterResource(id = R.drawable.location_on_24px),
                                            contentDescription = null
                                        )
                                    }
                                    Column(modifier = Modifier.padding(start = paddingIcon)) {
                                        Text(text = stationNameText, style = styleHint)

                                        Row {
                                            Text(text = timeArrival, style = styleHint)
                                            if (timeArrival.isNotBlank() && timeDeparture.isNotBlank()) {
                                                Text(text = " - ", style = styleHint)
                                            }
                                            Text(text = timeDeparture, style = styleHint)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (route.passengers.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingBetweenBlocks)
                            .animateItemPlacement(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Пассажир",
                            style = styleTitle,
                        )
                    }
                }
            }
            items(route.passengers, key = { passenger -> passenger.passengerId }) { passenger ->
                val numberText = passenger.trainNumber.ifNullOrBlank { "" }
                val stationDeparture = passenger.stationDeparture.ifNullOrBlank { "" }
                val stationArrival = passenger.stationArrival.ifNullOrBlank { "" }
                val timeDeparture =
                    dateAndTimeConverter?.getTimeFromDateLong(passenger.timeDeparture)
                        ?: "загрузка"
                val timeArrival =
                    dateAndTimeConverter?.getTimeFromDateLong(passenger.timeArrival)
                        ?: "загрузка"
                val timeFollowing =
                    ConverterLongToTime.getTimeInStringFormat(passenger.getFollowingTime())
                        .ifNullOrBlank { "" }
                val notesText = passenger.notes.ifNullOrBlank { "" }

                Column(modifier = Modifier.animateItemPlacement()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingInsideBlock)
                    ) {
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(iconSize)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = Shapes.medium
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.fillMaxSize(0.7f),
                                tint = MaterialTheme.colorScheme.primary,
                                painter = painterResource(id = R.drawable.passenger_24px),
                                contentDescription = null
                            )
                        }
                        Column(modifier = Modifier.padding(start = paddingIcon)) {
                            Text(text = timeFollowing, style = styleData)
                            if (passenger.timeDeparture != null || passenger.timeArrival != null) {
                                Row {
                                    Text(text = "$timeDeparture - ", style = styleHint)
                                    Text(text = timeArrival, style = styleHint)
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = paddingInsideBlock,
                                start = horizontalPaddingSecondItem
                            )
                    ) {
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(iconSizeSecond)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = Shapes.medium
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.fillMaxSize(0.7f),
                                tint = MaterialTheme.colorScheme.primary,
                                painter = painterResource(id = R.drawable.number_123_24px),
                                contentDescription = null
                            )
                        }
                        Column(modifier = Modifier.padding(start = paddingIcon)) {
                            Text(text = numberText, style = styleData)
                            if (stationDeparture.isNotBlank() && stationArrival.isNotBlank()) {
                                Row {
                                    Text(text = "$stationDeparture - ", style = styleHint)
                                    Text(text = stationArrival, style = styleHint)
                                }
                            }
                            Text(
                                text = notesText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = styleHint
                            )
                        }
                    }
                }
            }
            if (!route.basicData.notes.isNullOrBlank() || route.photos.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingBetweenBlocks)
                            .animateItemPlacement(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Заметки",
                            style = styleTitle,
                        )
                    }
                }
            }
            item {
                val notesText = route.basicData.notes.ifNullOrBlank { "" }
                Column(
                    modifier = Modifier
                        .padding(top = paddingInsideBlock)
                        .fillMaxWidth()
                ) {
                    route.basicData.notes?.let {
                        Row {
                            Box(
                                modifier = Modifier
                                    .size(iconSize)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = Shapes.medium
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    modifier = Modifier.fillMaxSize(0.7f),
                                    tint = MaterialTheme.colorScheme.primary,
                                    painter = painterResource(id = R.drawable.notes_24px),
                                    contentDescription = null
                                )
                            }
                            Text(
                                text = notesText,
                                style = styleData,
                                modifier = Modifier.padding(start = paddingIcon)
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.padding(top = 24.dp))
            }
        }
    }
}