package com.z_company.route.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.maxkeppeker.sheets.core.views.Grid
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AutoSizeText
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.ConverterUrlBase64
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.DateAndTimeConverter.getDateMiniAndTime
import com.z_company.core.util.DateAndTimeConverter.getDateFromDateLong
import com.z_company.core.util.DateAndTimeConverter.getMonthFullText
import com.z_company.core.util.DateAndTimeConverter.getTimeFromDateLong
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.UtilForMonthOfYear.getNormaHours
import com.z_company.domain.entities.route.BasicData
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
import com.z_company.route.component.AnimationDialog
import com.z_company.route.component.ButtonLocoDriver
import com.z_company.route.component.DialogSelectMonthOfYear
import com.z_company.route.component.HomeBottomSheetContent
import com.z_company.core.ui.component.TopSnackbar
import kotlinx.coroutines.launch
import com.z_company.core.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    routeListState: ResultState<List<Route>>,
    removeRouteState: ResultState<Unit>?,
    onRouteClick: (BasicData) -> Unit,
    onChangeRoute: (String) -> Unit,
    onNewRouteClick: () -> Unit,
    onDeleteRoute: (Route) -> Unit,
    onDeleteRouteConfirmed: () -> Unit,
    reloadRoute: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    totalTime: Long,
    currentMonthOfYear: MonthOfYear?,
    monthList: List<Int>,
    yearList: List<Int>,
    selectYearAndMonth: (Pair<Int, Int>) -> Unit,
    minTimeRest: Long?,
    nightTime: Long?,
    passengerTime: Long?,
    calculationHomeRest: (Route) -> Long?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            confirmValueChange = {
                it != SheetValue.Hidden
            }
        )
    )
    val heightScreen = LocalConfiguration.current.screenHeightDp
    val sheetPeekHeight = remember {
        heightScreen.times(0.3)
    }

    val isExpand = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded

    if (removeRouteState is ResultState.Success) {
        LaunchedEffect(removeRouteState) {
            scope.launch {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = context.getString(R.string.msg_route_deleted)
                )
                onDeleteRouteConfirmed()
            }
        }
    }
    var routeForPreview by remember {
        mutableStateOf<Route?>(null)
    }

    var showContextDialog by remember {
        mutableStateOf(false)
    }

    AnimationDialog(
        showDialog = showContextDialog,
        onDismissRequest = { showContextDialog = false }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            showContextDialog = false
                        }
                    )
                },
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightScreen.times(0.7f).dp)
                    .padding(start = 12.dp, end = 12.dp, top = 30.dp, bottom = 12.dp)
                    .background(color = MaterialTheme.colorScheme.surface, shape = Shapes.medium)
                    .clickable {}
            ) {
                PreviewRoute(routeForPreview, minTimeRest, calculationHomeRest)
            }

            Column(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .fillMaxWidth(0.6f)
                    .background(color = MaterialTheme.colorScheme.surface, shape = Shapes.medium)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            showContextDialog = false
                            routeForPreview?.basicData?.let { basicData ->
                                onRouteClick(basicData)
                            }
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
                            showContextDialog = false
                            routeForPreview?.basicData?.let { basicData ->
                                onChangeRoute(basicData.id)
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Редактировать",
                        style = AppTypography.getType().bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                    )
                    Icon(
                        imageVector = Icons.Default.Create,
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
                            routeForPreview?.let { route ->
                                onDeleteRoute(route)
                            }
                            showContextDialog = false
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

    val showMonthSelectorDialog = remember {
        mutableStateOf(false)
    }

    if (showMonthSelectorDialog.value) {
        currentMonthOfYear?.let {
            DialogSelectMonthOfYear(
                showMonthSelectorDialog,
                currentMonthOfYear,
                monthList = monthList,
                yearList = yearList,
                selectMonthOfYear = selectYearAndMonth
            )
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        snackbarHost = {
            SnackbarHost(hostState = scaffoldState.snackbarHostState) { snackBarData ->
                TopSnackbar(snackBarData = snackBarData)
            }
        },
        sheetPeekHeight = sheetPeekHeight.dp,
        sheetContainerColor = MaterialTheme.colorScheme.background,
        sheetDragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        sheetShadowElevation = 0.dp,
        sheetContent = {
            HomeBottomSheetContent(
                routeListState = routeListState,
                reloadRoute = reloadRoute,
                onDeleteRoute = onDeleteRoute,
                onRouteClick = onRouteClick,
                onRouteLongClick = { route ->
                    showContextDialog = true
                    routeForPreview = route
                },
                isExpand = isExpand
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightScreen.times(0.02f).dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightScreen.times(0.07f).dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = Shapes.medium
                        ),
                    onClick = { onSettingsClick() }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(
                    modifier = Modifier,
                    shape = Shapes.medium,
                    onClick = {
                        showMonthSelectorDialog.value = true
                    }) {
                    AutoSizeText(
                        text = "${
                            currentMonthOfYear?.month?.getMonthFullText()
                        } ${currentMonthOfYear?.year}",
                        style = AppTypography.getType().headlineSmall,
                        maxTextSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = Shapes.medium
                        ),
                    onClick = { onSearchClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightScreen.times(0.14f).dp)
            )
            currentMonthOfYear?.let { monthOfYear ->
                TotalTime(
                    modifier = Modifier
                        .height(heightScreen.times(0.13f).dp),
                    valueTime = totalTime,
                    normaHours = monthOfYear.getNormaHours(),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightScreen.times(0.05f).dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.padding(end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier.padding(end = 4.dp),
                        painter = painterResource(id = CoreR.drawable.ic_star_border),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    AutoSizeText(
                        text = ConverterLongToTime.getTimeInStringFormat(nightTime),
                        style = AppTypography.getType().headlineSmall,
                        maxTextSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        modifier = Modifier.padding(end = 4.dp),
                        painter = painterResource(id = CoreR.drawable.ic_star_border),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    AutoSizeText(
                        text = ConverterLongToTime.getTimeInStringFormat(passengerTime),
                        style = AppTypography.getType().headlineSmall,
                        maxTextSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightScreen.times(0.19f).dp)
            )
            ButtonLocoDriver(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightScreen.times(0.08f).dp),
                onClick = { onNewRouteClick() }
            ) {
                AutoSizeText(
                    text = stringResource(id = CoreR.string.adding),
                    style = AppTypography.getType().headlineSmall.copy(color = MaterialTheme.colorScheme.onPrimary),
                    maxTextSize = 24.sp,
                )
            }
        }
    }
}

@Composable
fun TotalTime(
    modifier: Modifier,
    valueTime: Long,
    normaHours: Int
) {
    AutoSizeText(
        modifier = modifier.fillMaxWidth(),
        alignment = Alignment.BottomStart,
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            ) {
                append(DateAndTimeConverter.getTimeInStringFormat(valueTime))
            }
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Light,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            ) {
                append(" / $normaHours")
            }
        },
        maxTextSize = 70.sp,
        fontFamily = AppTypography.Companion.AppFontFamilies.RobotoConsed
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreviewRoute(route: Route?, minTimeRest: Long?, calculationHomeRest: (Route) -> Long?) {
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
    val widthScreen = LocalConfiguration.current.screenWidthDp

    val locomotiveExpandItemState = remember {
        mutableStateMapOf<Int, Boolean>()
    }
    val trainExpandItemState = remember {
        mutableStateMapOf<Int, Boolean>()
    }
    route?.let {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
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
                            text = getDateFromDateLong(route.basicData.timeStartWork),
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
                                )
                        )
                        Column(modifier = Modifier.padding(start = paddingIcon)) {
                            Box {
                                Text(
                                    text = DateAndTimeConverter.getTimeInStringFormat(route.getWorkTime()),
                                    style = styleData,
                                    maxLines = 1
                                )
                            }
                            Row {
                                Text(
                                    text = getTimeFromDateLong(route.basicData.timeStartWork),
                                    style = styleHint,
                                    maxLines = 1
                                )

                                Text(
                                    text = " - ${getTimeFromDateLong(route.basicData.timeEndWork)}",
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
                                )
                        )

                        Column(modifier = Modifier.padding(start = paddingIcon)) {
                            Text(
                                text = restText,
                                style = styleData,
                                maxLines = 1,
                            )
                            if (route.basicData.restPointOfTurnover) {
                                minTimeRest?.let {
                                    val shortRestText = getTimeFromDateLong(
                                        route.shortRest(minTimeRest)
                                    )
                                    val fullRestText = getTimeFromDateLong(
                                        route.fullRest(minTimeRest)
                                    )
                                    Text(
                                        text = "$shortRestText - $fullRestText",
                                        style = styleHint,
                                        maxLines = 1,
                                    )
                                }
                            } else {
                                val homeRestInLong = calculationHomeRest(route)
                                homeRestInLong?.let {
                                    val homeRestInLongText = getDateMiniAndTime(homeRestInLong)
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
                    getTimeFromDateLong(locomotive.timeStartOfAcceptance)
                val timeEndAcceptedText =
                    getTimeFromDateLong(locomotive.timeEndOfAcceptance)
                val timeStartDeliveryText =
                    getTimeFromDateLong(locomotive.timeStartOfDelivery)
                val timeEndDeliveryText =
                    getTimeFromDateLong(locomotive.timeEndOfDelivery)

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
                                    )
                            )
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
                                            sectionElectric.acceptedEnergy.str()
                                        val deliveryEnergyText =
                                            sectionElectric.deliveryEnergy.str()
                                        val acceptedRecoveryText =
                                            sectionElectric.acceptedRecovery.str()
                                        val deliveryRecoveryText =
                                            sectionElectric.deliveryRecovery.str()
                                        val consumptionEnergy =
                                            CalculationEnergy.getTotalEnergyConsumption(
                                                accepted = sectionElectric.acceptedEnergy,
                                                delivery = sectionElectric.deliveryEnergy
                                            )
                                        val consumptionRecovery =
                                            CalculationEnergy.getTotalEnergyConsumption(
                                                accepted = sectionElectric.acceptedRecovery,
                                                delivery = sectionElectric.deliveryRecovery
                                            )

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
                                                    )
                                            )
                                            Column(modifier = Modifier.padding(start = paddingIcon)) {
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
                                                                .size(iconMiniSize)
                                                                .background(
                                                                    color = MaterialTheme.colorScheme.secondary,
                                                                    shape = Shapes.medium
                                                                )
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
                                                            paddingIcon / 2
                                                        )
                                                    ) {
                                                        // Icon recovery symbol
                                                        Box(
                                                            modifier = Modifier
                                                                .size(iconMiniSize)
                                                                .background(
                                                                    color = MaterialTheme.colorScheme.secondary,
                                                                    shape = Shapes.medium
                                                                )
                                                        )
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
                                                    consumptionEnergy?.let {
                                                        Text(
                                                            text = "Расход: $consumptionEnergy",
                                                            style = styleHint,
                                                        )
                                                    }
                                                    consumptionRecovery?.let {
                                                        Text(
                                                            text = "Рекуперация: $consumptionRecovery",
                                                            style = styleHint,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        LocoType.DIESEL -> {
                            AnimatedVisibility(visible = locomotiveExpandItemState[index]!!) {
                                locomotive.dieselSectionList.forEachIndexed { index, sectionDiesel ->
                                    val consumption = CalculationEnergy.getTotalFuelConsumption(
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
                                    val acceptedInKiloText = rounding(acceptedInKilo, 2).str()
                                    val deliveryInKilo =
                                        sectionDiesel.deliveryFuel.times(sectionDiesel.coefficient)
                                    val deliveryInKiloText = rounding(deliveryInKilo, 2).str()
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
                                                )
                                        )

                                        Column(modifier = Modifier.padding(start = paddingIcon)) {
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
                                    )
                            )
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
                            train.stations.forEachIndexed { index, station ->
                                val stationNameText = station.stationName.ifNullOrBlank { "" }
                                val timeArrival = getTimeFromDateLong(station.timeArrival)
                                val timeDeparture = getTimeFromDateLong(station.timeDeparture)
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
                                            )
                                    )
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
                val timeDeparture = getTimeFromDateLong(passenger.timeDeparture)
                val timeArrival = getTimeFromDateLong(passenger.timeArrival)
                val timeFollowing =
                    DateAndTimeConverter.getTimeInStringFormat(passenger.getFollowingTime())
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
                                )
                        )
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
                            .padding(top = paddingInsideBlock)
                    ) {
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(iconSize)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = Shapes.medium
                                )
                        )
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
            if (!route.basicData.notes.isNullOrBlank() && route.photos.isNotEmpty()) {
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
                val imageSize = (widthScreen - 24 - 24) / 3
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
                                    )
                            )
                            Text(
                                text = notesText,
                                style = styleData,
                                modifier = Modifier.padding(start = paddingIcon)
                            )
                        }
                    }
                    Grid(
                        modifier = Modifier.padding(top = paddingInsideBlock),
                        items = route.photos,
                        columns = 3,
                        rowSpacing = paddingInsideBlock,
                        columnSpacing = paddingInsideBlock
                    ) { photo ->
                        Card(
                            modifier = Modifier
                                .size(imageSize.dp),
                            shape = Shapes.extraSmall,
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                photo.base64.let { base64String ->
                                    val decodedImage =
                                        ConverterUrlBase64.base64toBitmap(base64String)
                                    Image(
                                        modifier = Modifier.fillMaxSize(),
                                        painter = rememberAsyncImagePainter(model = decodedImage),
                                        contentScale = ContentScale.Crop,
                                        contentDescription = null
                                    )
                                }
                            }
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



