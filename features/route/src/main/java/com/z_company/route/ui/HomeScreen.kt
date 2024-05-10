package com.z_company.route.ui

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.AutoSizeText
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime.getTimeInStringFormat
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.DateAndTimeConverter.getDateFromDateLong
import com.z_company.core.util.DateAndTimeConverter.getMonthFullText
import com.z_company.core.util.DateAndTimeConverter.getTimeFromDateLong
import com.z_company.core.util.DateAndTimeFormat
import com.z_company.domain.entities.MonthOfYear
import com.z_company.route.component.HomeBottomSheetContent
import com.z_company.route.component.DialogSelectMonthOfYear
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Route
import com.z_company.domain.util.CalculationEnergy
import com.z_company.domain.util.str
import com.z_company.route.R
import com.z_company.route.component.AnimationDialog
import com.z_company.route.component.ButtonLocoDriver
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import com.z_company.core.R as CoreR
import com.z_company.domain.util.minus
@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    routeListState: ResultState<List<Route>>,
    removeRouteState: ResultState<Unit>?,
    onRouteClick: (BasicData) -> Unit,
    onNewRouteClick: () -> Unit,
    onDeleteRoute: (Route) -> Unit,
    onDeleteRouteConfirmed: () -> Unit,
    reloadRoute: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    totalTime: Long,
    currentMonthOfYear: MonthOfYear,
    monthList: List<Int>,
    yearList: List<Int>,
    selectYearAndMonth: (Pair<Int, Int>) -> Unit,
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
                PreviewRoute(routeForPreview)
            }

            Column(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .fillMaxWidth(0.5f)
                    .background(color = Color.Red, shape = Shapes.medium)
            ) {
                Text(text = "item 1")
                HorizontalDivider()
                Text(text = "item 2")
                HorizontalDivider()
                Text(text = "item 3")
            }
        }
    }

    val showMonthSelectorDialog = remember {
        mutableStateOf(false)
    }

    if (showMonthSelectorDialog.value) {
        DialogSelectMonthOfYear(
            showMonthSelectorDialog,
            currentMonthOfYear,
            monthList = monthList,
            yearList = yearList,
            selectMonthOfYear = selectYearAndMonth
        )
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        snackbarHost = {
            SnackbarHost(hostState = scaffoldState.snackbarHostState) { snackBarData ->
                Snackbar(snackBarData)
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
        containerColor = MaterialTheme.colorScheme.background
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
                        contentDescription = null
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
                            currentMonthOfYear.month.getMonthFullText()
                        } ${currentMonthOfYear.year}",
                        style = AppTypography.getType().headlineSmall,
                        maxTextSize = 24.sp,
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
                        contentDescription = null
                    )
                }
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightScreen.times(0.14f).dp)
            )
            TotalTime(
                modifier = Modifier
                    .height(heightScreen.times(0.13f).dp),
                valueTime = totalTime,
                normaHours = currentMonthOfYear.normaHours,
            )
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
                        contentDescription = null
                    )
                    AutoSizeText(
                        text = DateAndTimeConverter.getTimeInStringFormat(12L),
                        style = AppTypography.getType().headlineSmall,
                        maxTextSize = 24.sp,
                        fontWeight = FontWeight.Light
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        modifier = Modifier.padding(end = 4.dp),
                        painter = painterResource(id = CoreR.drawable.ic_star_border),
                        contentDescription = null
                    )
                    AutoSizeText(
                        text = DateAndTimeConverter.getTimeInStringFormat(8L),
                        style = AppTypography.getType().headlineSmall,
                        maxTextSize = 24.sp,
                        fontWeight = FontWeight.Light
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
                    style = AppTypography.getType().headlineSmall,
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
                )
            ) {
                append(DateAndTimeConverter.getTimeInStringFormat(valueTime))
            }
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Light,
                    fontSize = 24.sp
                )
            ) {
                append(" / $normaHours")
            }
        },
        maxTextSize = 70.sp,
        fontFamily = AppTypography.Companion.AppFontFamilies.RobotoConsed
    )
}

@Composable
fun PreviewRoute(route: Route?) {
    val styleTitle = AppTypography.getType().titleMedium.copy(fontWeight = FontWeight.W500)
    val styleData = AppTypography.getType().bodyLarge.copy(fontWeight = FontWeight.W400)
    val styleHint = AppTypography.getType().labelMedium.copy(fontWeight = FontWeight.W300)
    route?.let {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    route.basicData.number?.let { number ->
                        Text(
                            text = "Маршрут №${number}  ",
                            style = styleTitle,
                        )
                    }
                    Text(
                        text = getDateFromDateLong(route.basicData.timeStartWork),
                        style = styleTitle,
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row {
                        Text(
                            text = getTimeFromDateLong(route.basicData.timeStartWork),
                            style = styleData,
                            maxLines = 1
                        )

                        Text(
                            text = " - ${getTimeFromDateLong(route.basicData.timeEndWork)}",
                            style = styleData,
                            maxLines = 1
                        )
                    }
                    val restText = if (route.basicData.restPointOfTurnover) {
                        "Отдых в ПО"
                    } else {
                        "Домашний отдых"
                    }

                    Text(
                        text = restText,
                        style = styleData,
                        maxLines = 1,
                    )
                }
            }
            items(route.locomotives, key = { loco -> loco.locoId }) { locomotive ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val typeLocoText = when (locomotive.type) {
                            LocoType.ELECTRIC -> "Электровоз"
                            LocoType.DIESEL -> "Тепловоз"
                        }
                        Text(
                            text = "$typeLocoText ",
                            style = styleTitle
                        )
                        locomotive.series?.let { series ->
                            Text(
                                text = "$series - ",
                                style = styleTitle
                            )
                        }
                        locomotive.number?.let { number ->
                            Text(
                                text = number,
                                style = styleTitle
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val timeStartAcceptedText =
                                getTimeFromDateLong(locomotive.timeStartOfAcceptance)
                            val timeEndAcceptedText =
                                getTimeFromDateLong(locomotive.timeEndOfAcceptance)
                            Text(text = "Приемка", style = styleHint)
                            Text(
                                text = "$timeStartAcceptedText - $timeEndAcceptedText",
                                style = styleData
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val timeStartDeliveryText =
                                getTimeFromDateLong(locomotive.timeStartOfDelivery)
                            val timeEndDeliveryText =
                                getTimeFromDateLong(locomotive.timeEndOfDelivery)
                            Text(text = "Сдача", style = styleHint)
                            Text(
                                text = "$timeStartDeliveryText - $timeEndDeliveryText",
                                style = styleData
                            )
                        }
                    }
                    locomotive.electricSectionList.forEachIndexed { index, sectionElectric ->
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    modifier = Modifier.weight(.25f),
                                    text = "Cекция ${index + 1}",
                                    style = styleHint
                                )
                                Text(
                                    modifier = Modifier.weight(.25f),
                                    text = "Принял",
                                    style = styleHint
                                )
                                Text(
                                    modifier = Modifier.weight(.25f),
                                    text = "Сдал",
                                    style = styleHint
                                )
                                Text(
                                    modifier = Modifier.weight(.25f),
                                    text = "Итого",
                                    style = styleHint
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                val resultText = sectionElectric.deliveryEnergy - sectionElectric.acceptedEnergy
                                Text(
                                    modifier = Modifier.padding(end = 2.dp).weight(.25f),
                                    text = "Расход",
                                    style = styleHint
                                )
                                Text(
                                    modifier = Modifier.padding(end = 2.dp).weight(.25f),
                                    text = sectionElectric.acceptedEnergy.str(),
                                    style = styleData
                                )
                                Text(
                                    modifier = Modifier.padding(end = 2.dp).weight(.25f),
                                    text = sectionElectric.deliveryEnergy.str(),
                                    style = styleData
                                )
                                Text(
                                    modifier = Modifier.weight(.25f),
                                    text = resultText.str(),
                                    style = styleData
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                val resultText = sectionElectric.deliveryRecovery - sectionElectric.acceptedRecovery
                                Text(
                                    modifier = Modifier.weight(.25f),
                                    text = "Рекуперация",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = styleHint
                                )
                                Text(
                                    modifier = Modifier.weight(.25f),
                                    text = sectionElectric.acceptedRecovery.str(),
                                    style = styleData
                                )
                                Text(
                                    modifier = Modifier.weight(.25f),
                                    text = sectionElectric.deliveryRecovery.str(),
                                    style = styleData
                                )
                                Text(
                                    modifier = Modifier.weight(.25f),
                                    text = resultText.str(),
                                    style = styleData
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Table(
    rowCount: Int,
    columnCount: Int,
    rowContent: @Composable () -> Unit
) {
    (0 until columnCount).forEach {
        Column {

        }
    }
}



