package com.z_company.route.ui

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BasicTooltipBox
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberBasicTooltipState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Card
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.flowWithLifecycle
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.AsyncDataValue
import com.z_company.core.ui.component.toDp
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.MonthFullText.getMonthFullText
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.UtilForMonthOfYear.getNormaHoursInDate
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.isFuture
import com.z_company.domain.entities.route.UtilsForEntities.isTransition
import com.z_company.domain.util.minus
import com.z_company.route.R
import com.z_company.route.component.AnimatedCounter
import com.z_company.route.component.AnimationDialog
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.ItemHomeScreen
import com.z_company.route.component.LinearPagerIndicator
import com.z_company.route.viewmodel.home_view_model.ItemState
import com.z_company.route.viewmodel.home_view_model.UpdateEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import com.z_company.route.component.ChipApp
import com.z_company.route.component.PreviewRouteDialog
import com.z_company.route.viewmodel.home_view_model.HomeViewModel
import org.koin.compose.koinInject

@SuppressLint(
    "CoroutineCreationDuringComposition",
    "FlowOperatorInvokedInComposition",
    "SuspiciousIndentation", "ConfigurationScreenWidthHeight"
)
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterialApi::class
)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    uiState: ResultState<Unit>,
    listRouteState: MutableList<ItemState>,
    onRouteClick: (String) -> Unit,
    onMoreInfoClick: (String) -> Unit,
    makeCopyRoute: (String) -> Unit,
    onDeleteRoute: (Route) -> Unit,
    onSearchClick: () -> Unit,
    totalTime: Long,
    currentMonthOfYear: MonthOfYear?,
    monthList: List<Int>,
    yearList: List<Int>,
    selectYearAndMonth: (Pair<Int, Int>) -> Unit,
    minTimeRest: Long?,
    nightTimeState: ResultState<Long>?,
    singleLocomotiveTimeState: ResultState<Long>?,
    passengerTimeState: ResultState<Long>?,
    totalTimeWithHoliday: ResultState<Long>?,
    calculationHomeRest: (Route?) -> Unit,
    homeRestValue: Long?,
    offsetInMoscow: Long,
    syncRoute: (Route) -> Unit,
    updateEvent: SharedFlow<UpdateEvent>,
    completeUpdateRequested: () -> Unit,
    setFavoriteState: (Route) -> Unit,
    dateAndTimeConverter: DateAndTimeConverter?,
    extendedServicePhaseTime: ResultState<Long>?,
    longDistanceTrainsTime: ResultState<Long>?,
    heavyTrainsTime: ResultState<Long>?,
    onePersonOperationTime: ResultState<Long>?,
    currentRoute: Route?,
    currentRouteTimeWork: SharedFlow<Long>,
    nextFutureRoute: Route?,
    countdownToNextRoute: SharedFlow<Long>,
    onNewLocoClick: (basicId: String) -> Unit,
    onChangedLocoClick: (loco: Locomotive) -> Unit,
    onNewTrainClick: (basicId: String) -> Unit,
    onChangedTrainClick: (train: Train) -> Unit,
    onNewPassengerClick: (basicId: String) -> Unit,
    onChangedPassengerClick: (passenger: Passenger) -> Unit,
    onGoClicked: () -> Unit,
    onAllRouteClick: () -> Unit,
    isNextDeparture: () -> Boolean,
    saveTimeEvent: SharedFlow<String>,
    onWorkScheduleScreen: () -> Unit,
    onClickVacation: () -> Unit,
    unsyncedRoutesCount: Int = 0,
    onSyncClick: () -> Unit = {},
    syncDialogState: com.z_company.route.viewmodel.home_view_model.SyncDialogState = com.z_company.route.viewmodel.home_view_model.SyncDialogState.Hidden,
    onCloseSyncDialog: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val interactionSource = remember { MutableInteractionSource() }

    val snackbarHostState = remember { SnackbarHostState() }

    val snackbarManager: ISnackbarManager = koinInject()

    LaunchedEffect(Unit) {
        snackbarManager.events
            .flowWithLifecycle(lifecycle)
            .collectLatest { event ->
                val result = snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = event.actionLabel,
                    duration = event.duration
                )
                if (result == SnackbarResult.ActionPerformed) {
                    event.onAction?.let { onAction ->
                        // запускаем suspend-колбек в scope
                        launch {
                            try {
                                onAction()
                            } catch (_: Exception) { /* optional logging */
                            }
                        }
                    }
                }
            }
    }

    val widthScreen = LocalConfiguration.current.screenWidthDp

    LaunchedEffect(Unit) {
        scope.launch {
            updateEvent.flowWithLifecycle(lifecycle).collect { event ->
                when (event) {
                    UpdateEvent.UpdateCompleted -> {
                        val result = snackbarHostState
                            .showSnackbar(
                                message = "Обновление загружено",
                                actionLabel = "Установить"
                            )
                        if (result == SnackbarResult.ActionPerformed) {
                            completeUpdateRequested()
                        }
                    }
                }
            }
        }
    }

    var isShowDialogConfirmRemoveRoute by remember { mutableStateOf(false) }

    var routeForPreview by remember {
        mutableStateOf<Route?>(null)
    }

    var routeForRemove by remember {
        mutableStateOf<Route?>(null)
    }

    var showContextDialog by remember {
        mutableStateOf(false)
    }

    val currentRouteWorkTime by produceState(initialValue = "") {
        currentRouteTimeWork
            .flowWithLifecycle(lifecycle)
            .collect { time ->
                value = ConverterLongToTime.getTimeInStringFormat(time)
            }
    }

    val countdownText by produceState(initialValue = "") {
        countdownToNextRoute
            .flowWithLifecycle(lifecycle)
            .collect { time ->
                value = ConverterLongToTime.getTimeInStringFormat(time)
            }
    }

    LaunchedEffect(saveTimeEvent) {
        saveTimeEvent.collectLatest {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
        }
    }

    // Диалог синхронизации
    if (syncDialogState !is com.z_company.route.viewmodel.home_view_model.SyncDialogState.Hidden) {
        val context = LocalContext.current
        androidx.compose.ui.window.Dialog(
            onDismissRequest = onCloseSyncDialog,
            properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = false)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Синхронизация",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = onCloseSyncDialog) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Закрыть",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    when (syncDialogState) {
                        is com.z_company.route.viewmodel.home_view_model.SyncDialogState.Loading -> {
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = onCloseSyncDialog,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Прервать", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        is com.z_company.route.viewmodel.home_view_model.SyncDialogState.Error -> {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = syncDialogState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                        data = android.net.Uri.parse("mailto:locodriver.app@yandex.ru?subject=Ошибка%20синхронизации")
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Написать в поддержку", color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onSyncClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Повторить", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    AnimationDialog(
        showDialog = showContextDialog,
        onDismissRequest = { showContextDialog = false }
    ) {
        routeForPreview?.let { route ->
            calculationHomeRest(route)
            PreviewRouteDialog(
                showContextDialog = {
                    showContextDialog = it
                },
                routeForPreview = route,
                minTimeRest = minTimeRest,
                homeRest = homeRestValue,
                dateAndTimeConverter = dateAndTimeConverter,
                syncRoute = syncRoute,
                setFavoriteState = setFavoriteState,
                onRouteClick = onRouteClick,
                makeCopyRoute = makeCopyRoute,
                showDialogConfirmRemove = { showDialog, route ->
                    routeForRemove = route
                    isShowDialogConfirmRemoveRoute = true
                },
//                shareRoute = {}
            )
        }
    }

    var showMonthSheetVisible by remember {
        mutableStateOf(false)
    }

    val monthSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    if (showMonthSheetVisible) {
        currentMonthOfYear?.let { current ->
            ModalBottomSheet(
                onDismissRequest = { showMonthSheetVisible = false },
                sheetState = monthSheetState,
                containerColor = MaterialTheme.colorScheme.secondary,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Выберите месяц и год",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    var selectedMonth by remember { mutableIntStateOf(current.month) }

                    var selectedYear by remember { mutableIntStateOf(current.year) }

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        monthList.forEach { m ->
                            val selected = selectedMonth == m
                            ChipApp(
                                selected = selected,
                                onClick = { selectedMonth = m },
                                label = getMonthFullText(m)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        yearList.forEach { y ->
                            val selected = selectedYear == y
                            ChipApp(
                                selected = selected,
                                onClick = { selectedYear = y },
                                label = "$y"
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            selectYearAndMonth(selectedYear to selectedMonth)
                            showMonthSheetVisible = false

                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Text(
                            text = "Применить",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )

                    }
                    Spacer(modifier = Modifier.height(24.dp))

                }

            }
        }
    }

    if (isShowDialogConfirmRemoveRoute) {
        AppBottomSheet(
            onDismissRequest = { isShowDialogConfirmRemoveRoute = false },
            sheetState = sheetState,
            title = "Удалить маршрут?\n" +
                    "от ${dateAndTimeConverter?.getDateMiniAndTime(value = routeForRemove?.basicData?.timeStartWork) ?: ""} ",
            actions = listOf(
                BottomSheetAction(text = "Да, удалить") {
                    routeForRemove?.let {
                        onDeleteRoute(it)
                    }
                }
            )
        )
    }

    val lightBrushMain = Brush.linearGradient(
        1f to MaterialTheme.colorScheme.surfaceContainerLow,
        1f to MaterialTheme.colorScheme.surfaceContainerLow,
        start = Offset.Zero,
        end = Offset.Infinite
    )

    val darkBrushMain = Brush.linearGradient(
        1f to MaterialTheme.colorScheme.surfaceContainerLow,
        1f to MaterialTheme.colorScheme.surfaceContainerLow,
//        1.0f to Color(0xFFE5E2D6),
        start = Offset.Zero,
        end = Offset.Infinite
    )

    val brushMain = if (isSystemInDarkTheme()) darkBrushMain else lightBrushMain

    Scaffold(
        topBar = {
            TopAppBar(
                scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                title = {},
                actions = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        TextButton(
                            modifier = Modifier
                                .weight(1f),
                            onClick = {
                                showMonthSheetVisible = true
                            }) {
                            val textMonth = currentMonthOfYear?.month?.let {
                                getMonthFullText(it)
                            } ?: "загрузка"
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    text = "$textMonth ",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    overflow = TextOverflow.Visible,
                                    maxLines = 2
                                )
                                Text(
                                    text = "${currentMonthOfYear?.year}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(
                            modifier = Modifier
                                .background(
                                    color = Color.Transparent,
                                    shape = Shapes.medium
                                ),
                            onClick = { onSearchClick() }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.search_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val pagerState = rememberPagerState(pageCount = { 3 })
        AsyncData(uiState) {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        HorizontalPager(
                            modifier = Modifier.animateItem(),
                            state = pagerState
                        ) { page ->
                            when (page) {
                                0 -> {
                                    MainInfo(
                                        totalTime = totalTime,
                                        convertTimeToString = viewModel::convertTimeToStringFormat,
                                        totalTimeWithHoliday = totalTimeWithHoliday,
                                        currentMonthOfYear = currentMonthOfYear,
                                        dateAndTimeConverter = dateAndTimeConverter,
                                        brush = brushMain
                                    )
                                }

                                1 -> {
                                    DetailWorkTimeCard(
                                        totalTime = totalTime,
                                        convertTimeToString = viewModel::convertTimeToStringFormat,
                                        brush = brushMain,
                                        totalTimeWithHoliday = totalTimeWithHoliday,
                                        passengerTimeState = passengerTimeState,
                                        singleLocomotiveTimeState = singleLocomotiveTimeState,
                                        nightTimeState = nightTimeState
                                    )
                                }

                                2 -> {
                                    DetailTrainCard(
                                        totalTime = totalTime,
                                        convertTimeToString = viewModel::convertTimeToStringFormat,
                                        brush = brushMain,
                                        totalTimeWithHoliday = totalTimeWithHoliday,
                                        extendedServicePhaseTime = extendedServicePhaseTime,
                                        longDistanceTrainsTime = longDistanceTrainsTime,
                                        heavyTrainsTime = heavyTrainsTime,
                                        onePersonOperationTime = onePersonOperationTime
                                    )
                                }
                            }
                        }
                        LinearPagerIndicator(
                            modifier = Modifier
                                .animateItem(),
                            state = pagerState
                        )
                        AnimatedVisibility(visible = unsyncedRoutesCount > 2) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Внимание!",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Не синхронизировано маршрутов: $unsyncedRoutesCount",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Данные не были отправлены на сервер. Проверьте подключение к интернету и выполните синхронизацию.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(MaterialTheme.shapes.extraSmall)
                                            .background(brushMain)
                                            .clickable(onClick = onSyncClick),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Синхронизировать",
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                currentRoute?.let { route ->
                    item {
                        var maxHeightBox by remember { mutableIntStateOf(widthScreen / 3) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                onRouteClick(route.basicData.id)
                                            }
                                        )
                                    },
                                text = "Текущий маршрут",
                                maxLines = 2,
                                overflow = TextOverflow.Visible,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            LazyRow(
                                modifier = Modifier.padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .onGloballyPositioned { coordinates ->
                                                val currentHeight = coordinates.size.height
                                                if (currentHeight > maxHeightBox) {
                                                    maxHeightBox = currentHeight
                                                }
                                            }
                                            .padding(start = 12.dp)
                                            .defaultMinSize(
                                                minWidth = (widthScreen / 3).dp,
                                                minHeight = (widthScreen / 3).dp,
                                            )
                                            .clickable {
                                                onRouteClick(route.basicData.id)
                                            },
                                        elevation = CardDefaults.elevatedCardElevation(
                                            defaultElevation = 2.dp,
                                        ),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .defaultMinSize(
                                                    minWidth = (widthScreen / 3).dp,
                                                    minHeight = (widthScreen / 3).dp,
                                                )
                                                .background(brushMain)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .defaultMinSize(
                                                        minWidth = (widthScreen / 3).dp,
                                                        minHeight = maxHeightBox.toDp(),
                                                    )
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                AnimatedCounter(
                                                    count = currentRouteWorkTime,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                Text(
                                                    text = "На работе",
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    maxLines = 1,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                                item {
                                    Card(
                                        modifier = Modifier
                                            .onGloballyPositioned { coordinates ->
                                                val currentHeight = coordinates.size.height
                                                if (currentHeight > maxHeightBox) {
                                                    maxHeightBox = currentHeight
                                                }
                                            }
                                            .defaultMinSize(
                                                minWidth = (widthScreen / 3).dp,
                                                minHeight = (widthScreen / 3).dp,
                                            ),
                                        elevation = CardDefaults.elevatedCardElevation(
                                            defaultElevation = 2.dp,
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .defaultMinSize(
                                                    minWidth = (widthScreen / 3).dp,
                                                    minHeight = (widthScreen / 3).dp,
                                                )
                                                .background(MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .defaultMinSize(
                                                        minWidth = (widthScreen / 3).dp,
                                                        minHeight = maxHeightBox.toDp(),
                                                    )
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                if (route.locomotives.isEmpty()) {
                                                    IconButton(
                                                        modifier = Modifier.align(Alignment.End),
                                                        colors = IconButtonDefaults.iconButtonColors(
                                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                                            contentColor = MaterialTheme.colorScheme.secondary
                                                        ),
                                                        onClick = {
                                                            onNewLocoClick(route.basicData.id)
                                                        }
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(com.z_company.core.R.drawable.ic_add),
                                                            contentDescription = null
                                                        )
                                                    }
                                                } else {
                                                    val loco = route.locomotives.last()
                                                    Column(
                                                        modifier = Modifier
                                                            .padding(bottom = 8.dp)
                                                            .pointerInput(Unit) {
                                                                detectTapGestures(
                                                                    onPress = {
                                                                        onChangedLocoClick(
                                                                            loco
                                                                        )
                                                                    }
                                                                )
                                                            },
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Box(modifier = Modifier.fillMaxWidth()) {
                                                            val text =
                                                                if (!loco.series.isNullOrBlank() && !loco.number.isNullOrBlank()) {
                                                                    "${loco.series} №${loco.number}"
                                                                } else if (loco.series.isNullOrBlank() && loco.number.isNullOrBlank()) {
                                                                    "Локо №1"
                                                                } else if (loco.series.isNullOrBlank() && !loco.number.isNullOrBlank()) {
                                                                    "Локо №${loco.number}"
                                                                } else if (!loco.series.isNullOrBlank() && loco.number.isNullOrBlank()) {
                                                                    "${loco.series}"
                                                                } else {
                                                                    ""
                                                                }
                                                            Text(
                                                                text = text,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        if (route.locomotives.size > 1) {
                                                            Text(
                                                                text = "... и ещё ${route.locomotives.size - 1}",
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "Локомотив",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                                item {
                                    Card(
                                        modifier = Modifier
                                            .onGloballyPositioned { coordinates ->
                                                val currentHeight = coordinates.size.height
                                                if (currentHeight > maxHeightBox) {
                                                    maxHeightBox = currentHeight
                                                }
                                            }
                                            .defaultMinSize(
                                                minWidth = (widthScreen / 3).dp,
                                                minHeight = (widthScreen / 3).dp,
                                            ),
                                        elevation = CardDefaults.elevatedCardElevation(
                                            defaultElevation = 2.dp,
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .defaultMinSize(
                                                    minWidth = (widthScreen / 3).dp,
                                                    minHeight = (widthScreen / 3).dp,
                                                )
                                                .background(MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .defaultMinSize(
                                                        minWidth = (widthScreen / 3).dp,
                                                        minHeight = maxHeightBox.toDp(),
                                                    )
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                if (route.trains.isEmpty()) {
                                                    IconButton(
                                                        modifier = Modifier.align(Alignment.End),
                                                        colors = IconButtonDefaults.iconButtonColors(
                                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                                            contentColor = MaterialTheme.colorScheme.secondary
                                                        ),
                                                        onClick = {
                                                            onNewTrainClick(route.basicData.id)
                                                        }
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(com.z_company.core.R.drawable.ic_add),
                                                            contentDescription = null
                                                        )
                                                    }
                                                } else {
                                                    Column(
                                                        modifier = Modifier
                                                            .padding(bottom = 8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        val train = route.trains.last()
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .pointerInput(Unit) {
                                                                    detectTapGestures(
                                                                        onPress = {
                                                                            onChangedTrainClick(
                                                                                train
                                                                            )
                                                                        }
                                                                    )
                                                                }
                                                        ) {
                                                            val numberTrainText =
                                                                train.number ?: "---"
                                                            Text(
                                                                text = "№ $numberTrainText",
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            val firstStation =
                                                                train.stations.firstOrNull()
                                                                    ?.let { it.stationName ?: "" }
                                                                    ?: ""
                                                            val lastStation =
                                                                if (train.stations.size > 1) {
                                                                    train.stations.lastOrNull()
                                                                        ?.let { " - ${it.stationName ?: ""}" }
                                                                        ?: ""
                                                                } else {
                                                                    ""
                                                                }
                                                            val trainInfoText =
                                                                "$firstStation $lastStation"

                                                            if (trainInfoText.isNotBlank()) {
                                                                Text(
                                                                    text = trainInfoText,
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    maxLines = 1,
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                        if (route.trains.size > 1) {
                                                            Text(
                                                                text = "... и ещё ${route.trains.size - 1}",
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                                Row(
                                                    modifier = Modifier.wrapContentWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalAlignment = Alignment.Bottom
                                                ) {
                                                    Text(
                                                        text = "Поезд",
                                                        color = MaterialTheme.colorScheme.primary,
                                                        maxLines = 1,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (route.trains.isNotEmpty()) {
                                                        val nextIsDeparture = isNextDeparture()
                                                        OutlinedButton(
                                                            onClick = {
                                                                onGoClicked()
                                                            },
                                                            border = BorderStroke(
                                                                width = 1.dp,
                                                                color = if (nextIsDeparture) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerHigh
                                                            ),
                                                        ) {
                                                            AnimatedContent(targetState = nextIsDeparture) {
                                                                val icon = if (it) {
                                                                    R.drawable.play_arrow_24px
                                                                } else {
                                                                    R.drawable.pause_24px
                                                                }
                                                                Icon(
                                                                    painter = painterResource(icon),
                                                                    contentDescription = null,
                                                                    tint = if (it) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerHigh
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                item {
                                    Card(
                                        modifier = Modifier
                                            .onGloballyPositioned { coordinates ->
                                                val currentHeight = coordinates.size.height
                                                if (currentHeight > maxHeightBox) {
                                                    maxHeightBox = currentHeight
                                                }
                                            }
                                            .defaultMinSize(
                                                minWidth = (widthScreen / 3).dp,
                                                minHeight = (widthScreen / 3).dp,
                                            )
                                            .padding(end = 12.dp),
                                        elevation = CardDefaults.elevatedCardElevation(
                                            defaultElevation = 2.dp,
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .defaultMinSize(
                                                    minWidth = (widthScreen / 3).dp,
                                                    minHeight = (widthScreen / 3).dp,
                                                )
                                                .background(MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .defaultMinSize(
                                                        minWidth = (widthScreen / 3).dp,
                                                        minHeight = maxHeightBox.toDp(),
                                                    )
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                if (route.passengers.isEmpty()) {
                                                    IconButton(
                                                        modifier = Modifier.align(Alignment.End),
                                                        colors = IconButtonDefaults.iconButtonColors(
                                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                                            contentColor = MaterialTheme.colorScheme.secondary
                                                        ),
                                                        onClick = {
                                                            onNewPassengerClick(route.basicData.id)
                                                        }
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(com.z_company.core.R.drawable.ic_add),
                                                            contentDescription = null
                                                        )
                                                    }
                                                } else {
                                                    Column(
                                                        modifier = Modifier
                                                            .padding(bottom = 8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        val passenger = route.passengers.last()
                                                        Column(
                                                            modifier = Modifier
                                                                .pointerInput(Unit) {
                                                                    detectTapGestures(
                                                                        onPress = {
                                                                            onChangedPassengerClick(
                                                                                passenger
                                                                            )
                                                                        }
                                                                    )
                                                                }
                                                        ) {
                                                            passenger.trainNumber?.let {
                                                                Text(
                                                                    text = "№ $it",
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    maxLines = 1,
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                            Text(
                                                                text = "${passenger.stationDeparture ?: ""} ${passenger.stationArrival?.let { " - $it" } ?: ""} ",
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        if (route.passengers.size > 1) {
                                                            Text(
                                                                text = "... и ещё ${route.passengers.size - 1}",
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "Пассажиром",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (currentRoute == null && nextFutureRoute != null) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                onRouteClick(nextFutureRoute.basicData.id)
                                            }
                                        )
                                    },
                                text = "Следующий маршрут",
                                maxLines = 2,
                                overflow = TextOverflow.Visible,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            LazyRow(
                                modifier = Modifier.padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .padding(start = 12.dp)
                                            .defaultMinSize(
                                                minWidth = (widthScreen / 3).dp,
                                                minHeight = (widthScreen / 3).dp,
                                            )
                                            .clickable {
                                                onRouteClick(nextFutureRoute.basicData.id)
                                            },
                                        elevation = CardDefaults.elevatedCardElevation(
                                            defaultElevation = 2.dp,
                                        ),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .defaultMinSize(
                                                    minWidth = (widthScreen / 3).dp,
                                                    minHeight = (widthScreen / 3).dp,
                                                )
                                                .background(brushMain)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .defaultMinSize(
                                                        minWidth = (widthScreen / 3).dp,
                                                        minHeight = (widthScreen / 3).dp,
                                                    )
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                AnimatedCounter(
                                                    count = countdownText,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                Text(
                                                    text = "До явки",
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    maxLines = 1,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                                item {
                                    Card(
                                        modifier = Modifier
                                            .defaultMinSize(
                                                minWidth = (widthScreen / 3).dp,
                                                minHeight = (widthScreen / 3).dp,
                                            )
                                            .padding(end = 12.dp),
                                        elevation = CardDefaults.elevatedCardElevation(
                                            defaultElevation = 2.dp,
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .defaultMinSize(
                                                    minWidth = (widthScreen / 3).dp,
                                                    minHeight = (widthScreen / 3).dp,
                                                )
                                                .background(MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .defaultMinSize(
                                                        minWidth = (widthScreen / 3).dp,
                                                        minHeight = (widthScreen / 3).dp,
                                                    )
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                Text(
                                                    text = dateAndTimeConverter?.getDateMiniAndTime(
                                                        nextFutureRoute.basicData.timeStartWork
                                                    ) ?: "",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Явка",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Маршруты",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(onClick = {
                                onAllRouteClick()
                            }) {
                                Text(
                                    color = MaterialTheme.colorScheme.tertiary,
                                    style = MaterialTheme.typography.bodySmall,
                                    text = "Все"
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            dateAndTimeConverter?.let {
                                var requiredSize by remember {
                                    mutableStateOf(20.sp)
                                }

                                fun changingTextSize(value: TextUnit) {
                                    if (requiredSize > value) {
                                        requiredSize = value
                                    }
                                }
                                if (listRouteState.isNotEmpty()) {
                                    val route = listRouteState.first().route
                                    var background = MaterialTheme.colorScheme.secondary

                                    if (route.isFuture(offsetInMoscow)) {
                                        background = MaterialTheme.colorScheme.surfaceBright
                                    } else {
                                        if (route.isTransition(offsetInMoscow)) {
                                            background = MaterialTheme.colorScheme.surfaceDim
                                        }
                                    }

//                                    val dismissState = rememberDismissState()

                                    ItemHomeScreen(
                                        modifier = Modifier.animateItem(),
                                        convertTimeToString = viewModel::convertTimeToStringFormat,
//                                        dismissState = dismissState,
                                        route = route,
                                        onRequestDelete = {
                                            routeForRemove = route
                                            isShowDialogConfirmRemoveRoute = true
                                        },
                                        requiredSizeText = requiredSize,
                                        changingTextSize = ::changingTextSize,
                                        onLongClick = {
                                            showContextDialog = true
                                            routeForPreview = route
                                        },
                                        containerColor = background,
                                        onClick = {
                                            onRouteClick(route.basicData.id)
                                        },
                                        dateAndTimeConverter = dateAndTimeConverter,
                                        isHeavyTrains = listRouteState[0].isHeavyTrains,
                                        isExtendedServicePhaseTrains = listRouteState[0].isExtendedServicePhaseTrains,
                                        isHolidayTimeInRoute = listRouteState[0].isHoliday,
                                        number = listRouteState.size
                                    )
                                } else {
                                    Text(
                                        modifier = Modifier
                                            .animateItem()
                                            .fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        text = "Список пуст\n\nНажмите  +  чтобы добавить маршрут\nили создайте график работы",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (listRouteState.size > 1) {
                                    val route = listRouteState[1].route
                                    var background = MaterialTheme.colorScheme.secondary

                                    if (route.isFuture(offsetInMoscow)) {
                                        background = MaterialTheme.colorScheme.surfaceBright
                                    } else {
                                        if (route.isTransition(offsetInMoscow)) {
                                            background = MaterialTheme.colorScheme.surfaceDim
                                        }
                                    }

                                    ItemHomeScreen(
                                        modifier = Modifier.animateItem(),
                                        route = route,
                                        convertTimeToString = viewModel::convertTimeToStringFormat,
                                        onRequestDelete = {
                                            routeForRemove = route
                                            isShowDialogConfirmRemoveRoute = true
                                        },
                                        requiredSizeText = requiredSize,
                                        changingTextSize = ::changingTextSize,
                                        onLongClick = {
                                            showContextDialog = true
                                            routeForPreview = route
                                        },
                                        containerColor = background,
                                        onClick = { onRouteClick(route.basicData.id) },
                                        dateAndTimeConverter = dateAndTimeConverter,
                                        isHeavyTrains = listRouteState[1].isHeavyTrains,
                                        isExtendedServicePhaseTrains = listRouteState[1].isExtendedServicePhaseTrains,
                                        isHolidayTimeInRoute = listRouteState[1].isHoliday,
                                        number = listRouteState.size - 1
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    var maxHeightBox by remember { mutableIntStateOf(widthScreen / 3) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.titleSmall,
                            text = "Действия",
                            color = MaterialTheme.colorScheme.primary
                        )
                        LazyRow(
                            modifier = Modifier
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .onGloballyPositioned { coordinates ->
                                            val currentHeight = coordinates.size.height
                                            if (currentHeight > maxHeightBox) {
                                                maxHeightBox = currentHeight
                                            }
                                        }
                                        .padding(start = 12.dp)
                                        .defaultMinSize(
                                            minWidth = (widthScreen / 3).dp,
                                            minHeight = (widthScreen / 3).dp
                                        )
                                        .indication(
                                            interactionSource = interactionSource,
                                            indication = ripple(
                                                color = MaterialTheme.colorScheme.background,
                                                bounded = true
                                            )
                                        )
                                        .clickable {
                                            onWorkScheduleScreen()
                                        },
                                    elevation = CardDefaults.elevatedCardElevation(
                                        defaultElevation = 2.dp,
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .defaultMinSize(
                                                minWidth = (widthScreen / 3).dp,
                                                minHeight = maxHeightBox.toDp(),
                                            )
                                            .padding(
                                                vertical = 8.dp, horizontal = 16.dp
                                            ),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        horizontalAlignment = Alignment.Start,
                                    ) {
                                        Image(
                                            modifier = Modifier
                                                .weight(1f)
                                                .align(Alignment.CenterHorizontally),
                                            painter = painterResource(R.drawable.calendar_3d_ver3),
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit
                                        )
                                        Text(
                                            maxLines = 1,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            overflow = TextOverflow.Ellipsis,
                                            text = "График"
                                        )
                                    }
                                }
                            }
                            item {
                                Card(
                                    modifier = Modifier
                                        .onGloballyPositioned { coordinates ->
                                            val currentHeight = coordinates.size.height
                                            if (currentHeight > maxHeightBox) {
                                                maxHeightBox = currentHeight
                                            }
                                        }
                                        .padding(end = 12.dp)
                                        .defaultMinSize(
                                            minWidth = (widthScreen / 3).dp,
                                            minHeight = (widthScreen / 3).dp
                                        )
                                        .indication(
                                            interactionSource = interactionSource,
                                            indication = ripple(
                                                color = MaterialTheme.colorScheme.background,
                                                bounded = true
                                            )
                                        )
                                        .clickable {
                                            onClickVacation()
                                        },
                                    elevation = CardDefaults.elevatedCardElevation(
                                        defaultElevation = 2.dp,
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .defaultMinSize(
                                                minWidth = (widthScreen / 3).dp,
                                                minHeight = maxHeightBox.toDp(),
                                            )
                                            .padding(
                                                vertical = 8.dp, horizontal = 16.dp
                                            ),
                                        horizontalAlignment = Alignment.Start,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Image(
                                            modifier = Modifier
                                                .weight(1f)
                                                .align(Alignment.CenterHorizontally),
                                            painter = painterResource(R.drawable.island_3d),
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit
                                        )
                                        Text(
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.bodySmall,
                                            overflow = TextOverflow.Ellipsis,
                                            text = "Отвлечения"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(
                        modifier = Modifier
                            .height(50.dp)
                            .animateItem()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainInfo(
    totalTime: Long,
    convertTimeToString: (Long?) -> String,
    totalTimeWithHoliday: ResultState<Long>?,
    currentMonthOfYear: MonthOfYear?,
    dateAndTimeConverter: DateAndTimeConverter?,
    brush: Brush
) {
    Card(
        modifier = Modifier
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .background(brush)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            ) {
                AsyncDataValue(resultState = totalTimeWithHoliday) { time ->
                    val tooltipPosition = TooltipDefaults.rememberPlainTooltipPositionProvider()
                    val state = rememberBasicTooltipState(isPersistent = false)
                    val scope = rememberCoroutineScope()
                    var tooltipText by remember {
                        mutableStateOf("")
                    }
                    BasicTooltipBox(
                        modifier = Modifier
                            .fillMaxWidth(),
                        positionProvider = tooltipPosition,
                        tooltip = {
                            Box(
                                modifier = Modifier
                                    .background(
                                        shape = Shapes.medium,
                                        color = MaterialTheme.colorScheme.surface
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = tooltipText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        state = state
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            scope.launch {
                                                tooltipText = "Общее отработанное время"
                                                state.show(MutatePriority.Default)
                                            }
                                        }
                                    )
                                },
                                text = convertTimeToString(time),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (totalTime != time) {
                                val differenceTimeInLong = time.minus(totalTime)
                                val totalTime = convertTimeToString(totalTime)
                                val differenceTime = convertTimeToString(differenceTimeInLong)
                                Text(
                                    modifier = Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                scope.launch {
                                                    tooltipText = "Рабочие + праздничные часы"
                                                    state.show(MutatePriority.Default)
                                                }
                                            }
                                        )
                                    },
                                    text = " ($totalTime + $differenceTime)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                currentMonthOfYear?.let { month ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val normaHoursInMonth =
                            month.getPersonalNormaHours()
                        val percent =
                            ((totalTime * 100).toFloat() / (normaHoursInMonth * 3_600_000L).toFloat()) / 100f

                        val percentNormaInMonth =
                            (totalTime.toFloat() / (normaHoursInMonth * 3_600_000L).coerceAtLeast(1)
                                .toFloat()).coerceIn(0f, 1f)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Норма на месяц",
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "$normaHoursInMonth ч.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            color = MaterialTheme.colorScheme.secondary,
                            gapSize = 4.dp,
                            drawStopIndicator = {},
                            progress = { percentNormaInMonth },
                        )
                    }
                    Spacer(modifier = Modifier.height(7.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val currentTime = Calendar.getInstance()
                        val normaHoursToday =
                            month.getNormaHoursInDate(currentTime.timeInMillis)
                        val percent =
                            ((totalTime * 100).toFloat() / (normaHoursToday * 3_600_000L).toFloat()) / 100f

                        val percentNormaInDay =
                            (totalTime.toFloat() / (normaHoursToday * 3_600_000L).coerceAtLeast(1)
                                .toFloat()).coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Норма на ${
                                    dateAndTimeConverter?.getDate(
                                        currentTime.timeInMillis
                                    ) ?: ""
                                }",
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "$normaHoursToday ч.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            color = MaterialTheme.colorScheme.secondary,
                            gapSize = 4.dp,
                            drawStopIndicator = {},
                            progress = { percentNormaInDay },
                        )
                    }
//                    Spacer(modifier = Modifier.height(7.dp))
//                    Column(
//                        modifier = Modifier
//                            .fillMaxWidth(),
//                        verticalArrangement = Arrangement.spacedBy(4.dp),
//                    ) {
//                        val currentTime = Calendar.getInstance()
//                        val normaHoursToday =
//                            month.getNormaHoursInDate(currentTime.timeInMillis)
//                        val percent =
//                            ((totalTime * 100).toFloat() / (normaHoursToday * 3_600_000L).toFloat()) / 100f
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween,
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Text(
//                                text = "Норма на ${
//                                    dateAndTimeConverter?.getDate(
//                                        currentTime.timeInMillis
//                                    ) ?: ""
//                                }",
//                                maxLines = 1,
//                                modifier = Modifier.weight(1f),
//                                overflow = TextOverflow.Ellipsis,
//                                style = MaterialTheme.typography.bodyMedium,
//                                color = MaterialTheme.colorScheme.secondary
//                            )
//                            Text(
//                                text = "$normaHoursToday ч.",
//                                style = MaterialTheme.typography.bodyMedium,
//                                color = MaterialTheme.colorScheme.secondary
//                            )
//                        }
//                        LinearProgressIndicator(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .height(4.dp),
//                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
//                            color = MaterialTheme.colorScheme.secondary,
//                            strokeCap = StrokeCap.Round,
//                            progress = { percent.coerceIn(0f, 1f) },
//                        )
//                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailWorkTimeCard(
    brush: Brush,
    totalTime: Long,
    convertTimeToString: (Long?) -> String,
    totalTimeWithHoliday: ResultState<Long>?,
    passengerTimeState: ResultState<Long>?,
    singleLocomotiveTimeState: ResultState<Long>?,
    nightTimeState: ResultState<Long>?
) {
    Card(
        modifier = Modifier
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .background(brush)
        ) {
            AsyncDataValue(resultState = totalTimeWithHoliday) { totalTimeWithHoliday ->
                totalTimeWithHoliday?.let {
                    val safeTotal = totalTimeWithHoliday.coerceAtLeast(1)  // никогда не будет 0

                    val tooltipPosition = TooltipDefaults.rememberPlainTooltipPositionProvider()
                    val state = rememberBasicTooltipState(isPersistent = false)
                    val scope = rememberCoroutineScope()
                    var tooltipText by remember {
                        mutableStateOf("")
                    }
                    BasicTooltipBox(
                        modifier = Modifier
                            .fillMaxWidth(),
                        positionProvider = tooltipPosition,
                        tooltip = {
                            Box(
                                modifier = Modifier
                                    .background(
                                        shape = Shapes.medium,
                                        color = MaterialTheme.colorScheme.surface
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = tooltipText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        state = state
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    modifier = Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                scope.launch {
                                                    tooltipText = "Общее отработанное время"
                                                    state.show(MutatePriority.Default)
                                                }
                                            }
                                        )
                                    },
                                    text = convertTimeToString(totalTimeWithHoliday),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                if (totalTime != totalTimeWithHoliday) {
                                    val differenceTimeInLong = totalTimeWithHoliday.minus(totalTime)
                                    val totalTime = convertTimeToString(totalTime)
                                    val differenceTime = convertTimeToString(differenceTimeInLong)
                                    Text(
                                        modifier = Modifier.pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    scope.launch {
                                                        tooltipText =
                                                            "Рабочие + праздничные часы"
                                                        state.show(MutatePriority.Default)
                                                    }
                                                }
                                            )
                                        },
                                        text = " ($totalTime + $differenceTime)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            AsyncDataValue(nightTimeState) { nightTime ->
                                nightTime?.let {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        val nightTimeText = convertTimeToString(nightTime)
                                        val percent = if (totalTimeWithHoliday > 0) {
                                            (nightTime.toFloat() / totalTimeWithHoliday.toFloat()).coerceIn(
                                                0f,
                                                1f
                                            )
                                        } else {
                                            0f
                                        }

                                        val percentNight =
                                            (nightTime.toFloat() / safeTotal.toFloat()).coerceIn(
                                                0f,
                                                1f
                                            )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Ночные",
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f),
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = nightTimeText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp),
                                            trackColor = MaterialTheme.colorScheme.surface.copy(
                                                alpha = 0.5f
                                            ),
                                            color = MaterialTheme.colorScheme.secondary,
//                                            strokeCap = StrokeCap.Round,
                                            gapSize = 4.dp,
                                            drawStopIndicator = {},
                                            progress = { percentNight },
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(7.dp))
                                AsyncDataValue(passengerTimeState) { passengerTime ->
                                    passengerTime?.let {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            val passengerTimeText =
                                                convertTimeToString(passengerTime)
                                            val percent =
                                                ((passengerTime * 100).toFloat() / (totalTimeWithHoliday).toFloat()) / 100f

                                            val percentPassenger =
                                                (passengerTime.toFloat() / safeTotal.toFloat()).coerceIn(
                                                    0f,
                                                    1f
                                                )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Пассажиром",
                                                    maxLines = 1,
                                                    modifier = Modifier.weight(1f),
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                Text(
                                                    text = passengerTimeText,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                            LinearProgressIndicator(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp),
                                                trackColor = MaterialTheme.colorScheme.surface.copy(
                                                    alpha = 0.5f
                                                ),
                                                color = MaterialTheme.colorScheme.secondary,
                                                gapSize = 4.dp,
                                                drawStopIndicator = {},
                                                progress = { percentPassenger },
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(7.dp))
                                AsyncDataValue(singleLocomotiveTimeState) { singleLocomotiveTime ->
                                    singleLocomotiveTime?.let {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            val passengerTimeText =
                                                convertTimeToString(singleLocomotiveTime)
                                            val percent =
                                                ((singleLocomotiveTime * 100).toFloat() / (totalTimeWithHoliday).toFloat()) / 100f

                                            val percentSingleLocomotive =
                                                (singleLocomotiveTime.toFloat() / safeTotal.toFloat()).coerceIn(
                                                    0f,
                                                    1f
                                                )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Резервом",
                                                    maxLines = 1,
                                                    modifier = Modifier.weight(1f),
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                Text(
                                                    text = passengerTimeText,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                            LinearProgressIndicator(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp),
                                                trackColor = MaterialTheme.colorScheme.surface.copy(
                                                    alpha = 0.5f
                                                ),
                                                color = MaterialTheme.colorScheme.secondary,
                                                gapSize = 4.dp,
                                                drawStopIndicator = {},
                                                progress = { percentSingleLocomotive },
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailTrainCard(
    brush: Brush,
    totalTime: Long,
    convertTimeToString: (Long?) -> String,
    totalTimeWithHoliday: ResultState<Long>?,
    extendedServicePhaseTime: ResultState<Long>?,
    longDistanceTrainsTime: ResultState<Long>?,
    heavyTrainsTime: ResultState<Long>?,
    onePersonOperationTime: ResultState<Long>?,
) {
    Card(
        modifier = Modifier
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 3.dp,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .background(brush)
        ) {

            AsyncDataValue(resultState = totalTimeWithHoliday) { totalTimeWithHoliday ->
                totalTimeWithHoliday?.let {
                    val safeTotal = totalTimeWithHoliday.coerceAtLeast(1)  // никогда не будет 0

                    val tooltipPosition = TooltipDefaults.rememberPlainTooltipPositionProvider()
                    val state = rememberBasicTooltipState(isPersistent = false)
                    val scope = rememberCoroutineScope()
                    var tooltipText by remember {
                        mutableStateOf("")
                    }
                    BasicTooltipBox(
                        modifier = Modifier
                            .fillMaxWidth(),
                        positionProvider = tooltipPosition,
                        tooltip = {
                            Box(
                                modifier = Modifier
                                    .background(
                                        shape = Shapes.medium,
                                        color = MaterialTheme.colorScheme.surface
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = tooltipText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        state = state
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    modifier = Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                scope.launch {
                                                    tooltipText = "Общее отработанное время"
                                                    state.show(MutatePriority.Default)
                                                }
                                            }
                                        )
                                    },
                                    text = convertTimeToString(totalTimeWithHoliday),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                if (totalTime != totalTimeWithHoliday) {
                                    val differenceTimeInLong =
                                        totalTimeWithHoliday.minus(totalTime)
                                    val totalTime =
                                        convertTimeToString(
                                            totalTime
                                        )
                                    val differenceTime =
                                        convertTimeToString(
                                            differenceTimeInLong
                                        )
                                    Text(
                                        modifier = Modifier.pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    scope.launch {
                                                        tooltipText =
                                                            "Рабочие + праздничные часы"
                                                        state.show(MutatePriority.Default)
                                                    }
                                                }
                                            )
                                        },
                                        text = " ($totalTime + $differenceTime)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            AsyncDataValue(extendedServicePhaseTime) { extendedServicePhaseTime ->
                                extendedServicePhaseTime?.let {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        val extendedServicePhaseTimeText =
                                            convertTimeToString(extendedServicePhaseTime)
                                        val percent =
                                            ((extendedServicePhaseTime * 100).toFloat() / (totalTimeWithHoliday).toFloat()) / 100f

                                        val percentExtendedServicePhase =
                                            (extendedServicePhaseTime.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)


                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Удл. плечи обслуживания",
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = extendedServicePhaseTimeText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Visible,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp),
                                            trackColor = MaterialTheme.colorScheme.surface.copy(
                                                alpha = 0.5f
                                            ),
                                            color = MaterialTheme.colorScheme.secondary,
                                            gapSize = 4.dp,
                                            drawStopIndicator = {},
                                            progress = { percentExtendedServicePhase },
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(7.dp))
                            AsyncDataValue(longDistanceTrainsTime) { longDistanceTrainsTime ->
                                longDistanceTrainsTime?.let {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        val longDistanceTrainsTimeText =
                                            convertTimeToString(longDistanceTrainsTime)

                                        val percentLong =
                                            (longDistanceTrainsTime.toFloat() / safeTotal.toFloat()).coerceIn(
                                                0f,
                                                1f
                                            )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Длинносоставные",
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = longDistanceTrainsTimeText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp),
                                            trackColor = MaterialTheme.colorScheme.surface.copy(
                                                alpha = 0.5f
                                            ),
                                            color = MaterialTheme.colorScheme.secondary,
                                            gapSize = 4.dp,
                                            drawStopIndicator = {},
                                            progress = { percentLong },
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(7.dp))
                            AsyncDataValue(heavyTrainsTime) { heavyTrainsTime ->
                                heavyTrainsTime?.let {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        val heavyTrainsTimeText =
                                            convertTimeToString(heavyTrainsTime)
                                        val percent =
                                            ((heavyTrainsTime * 100).toFloat() / (totalTimeWithHoliday).toFloat()) / 100f
                                        val percentHeavy =
                                            (heavyTrainsTime.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Тяжелые",
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f),
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = heavyTrainsTimeText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp),
                                            trackColor = MaterialTheme.colorScheme.surface.copy(
                                                alpha = 0.5f
                                            ),
                                            color = MaterialTheme.colorScheme.secondary,
                                            gapSize = 4.dp,
                                            drawStopIndicator = {},
                                            progress = { percentHeavy },
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(7.dp))
                            AsyncDataValue(onePersonOperationTime) { onePersonOperationTime ->
                                onePersonOperationTime?.let {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        val onePersonOperationTimeText = convertTimeToString(onePersonOperationTime)
                                        val percent =
                                            ((onePersonOperationTime * 100).toFloat() / (totalTimeWithHoliday).toFloat()) / 100f
                                        val percentOnePerson =
                                            (onePersonOperationTime.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Одно лицо",
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f),
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = onePersonOperationTimeText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp),
                                            trackColor = MaterialTheme.colorScheme.surface.copy(
                                                alpha = 0.5f
                                            ),
                                            color = MaterialTheme.colorScheme.secondary,
                                            gapSize = 4.dp,
                                            drawStopIndicator = {},
                                            progress = { percentOnePerson },
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(7.dp))
                        }
                    }
                }
            }
        }
    }
}