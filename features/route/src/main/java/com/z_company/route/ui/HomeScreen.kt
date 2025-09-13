package com.z_company.route.ui

import android.annotation.SuppressLint
import org.koin.androidx.compose.get
import android.app.Activity
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BasicTooltipBox
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberBasicTooltipState
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.z_company.core.ui.theme.custom.AppTypography
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
import com.z_company.domain.entities.route.UtilsForEntities.isTransition
import com.z_company.domain.util.minus
import com.z_company.repository.ShareManager
import com.z_company.route.R
import com.z_company.route.component.AnimatedCounter
import com.z_company.route.component.AnimationDialog
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.ItemHomeScreen
import com.z_company.route.component.LinearPagerIndicator
import com.z_company.route.component.PieChart
import com.z_company.route.viewmodel.home_view_model.AlertBeforePurchasesEvent
import com.z_company.route.viewmodel.home_view_model.ItemState
import com.z_company.route.viewmodel.home_view_model.StartPurchasesEvent
import com.z_company.route.viewmodel.home_view_model.UpdateEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import com.z_company.route.component.Chip
import com.z_company.route.component.PreviewRouteDialog

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
    uiState: ResultState<Unit>,
    listRouteState: MutableList<ItemState>,
    onRouteClick: (String) -> Unit,
    onNewRouteClick: () -> Unit,
    onMoreInfoClick: (String) -> Unit,
    makeCopyRoute: (String) -> Unit,
    onDeleteRoute: (Route) -> Unit,
    onSettingsClick: () -> Unit,
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
    firstEntryDialogState: Boolean,
    resetStateFirstEntryDialog: () -> Unit,
    showFormScreen: () -> Unit,
    isLoadingStateAddButton: Boolean,
    alertBeforePurchasesState: SharedFlow<AlertBeforePurchasesEvent>,
    checkPurchasesAvailability: () -> Unit,
    restorePurchases: () -> Unit,
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
) {
    val view = LocalView.current
    val backgroundColor = MaterialTheme.colorScheme.background

    val redOrange = Color(0xFFf1642e)
    val purple = Color(0xFF504e76)
    val green = Color(0xFFa3b565)

    // для изменения color status bar после изменения в PresentationBlock
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = backgroundColor.toArgb()
        }
    }


    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val snackbarHostState = remember { SnackbarHostState() }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val snackbarManager: ISnackbarManager = get()

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

    var isShowNeedSubscribeDialog by remember {
        mutableStateOf(false)
    }

    var isShowAlertSubscribeDialog by remember {
        mutableStateOf(false)
    }

    var isShowDialogConfirmRemoveRoute by remember { mutableStateOf(false) }

    if (isShowAlertSubscribeDialog) {
        AppBottomSheet(
            onDismissRequest = { isShowAlertSubscribeDialog = false },
            sheetState = sheetState,
            headerContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "${stringResource(id = R.string.test_period)}\n",
                        style = AppTypography.getType().titleLarge.copy(color = MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = "${stringResource(id = R.string.available_for_free_route)}\n",
                        style = AppTypography.getType().titleMedium.copy(color = MaterialTheme.colorScheme.primary)
                    )
                }
            },
            actions = listOf(
                BottomSheetAction(text = stringResource(id = R.string.billing_common_ok)) {
                    showFormScreen()
                },
                BottomSheetAction(text = "Оформить подписку за 44 руб/мес") {
                    checkPurchasesAvailability()
                }
            ),
        )
    }

    if (isShowNeedSubscribeDialog) {
        AppBottomSheet(
            onDismissRequest = { isShowNeedSubscribeDialog = false },
            sheetState = sheetState,
            headerContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "${stringResource(id = R.string.dialog_title_need_purchases)}\n",
                        style = AppTypography.getType().titleLarge.copy(color = MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = stringResource(id = R.string.available_for_free_route),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            actions = listOf(
                BottomSheetAction(text = "Оформить подписку за 44 руб/мес") {
                    checkPurchasesAvailability()
                },
                BottomSheetAction(text = "Восстановить покупки") {
                    restorePurchases()
                }
            )
        )
    }

    LaunchedEffect(Unit) {
        scope.launch {
            alertBeforePurchasesState.flowWithLifecycle(lifecycle).collect { event ->
                when (event) {
                    is AlertBeforePurchasesEvent.ShowDialogNeedSubscribe -> {
                        isShowNeedSubscribeDialog = true
                    }

                    is AlertBeforePurchasesEvent.ShowDialogAlertSubscribe -> {
                        isShowAlertSubscribeDialog = true
                    }
                }
            }
        }
    }

    var routeForPreview by remember {
        mutableStateOf<Route?>(null)
    }

    var routeForRemove by remember {
        mutableStateOf<Route?>(null)
    }

    var showContextDialog by remember {
        mutableStateOf(false)
    }

    var currentRouteWorkTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scope.launch {
            currentRouteTimeWork.flowWithLifecycle(lifecycle).collect { time ->
                currentRouteWorkTime =
                    ConverterLongToTime.getTimeInStringFormat(time)
            }
        }
    }

    LaunchedEffect(saveTimeEvent) {
        saveTimeEvent.collectLatest {
            scope.launch {
                snackbarHostState.showSnackbar("$it")
            }
        }
    }

    AnimationDialog(
        showDialog = firstEntryDialogState,
        onDismissRequest = resetStateFirstEntryDialog
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.surface, shape = Shapes.medium)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start, text = "ДОБРО ПОЖАЛОВАТЬ!\n",
                    style = AppTypography.getType().titleLarge.copy(color = MaterialTheme.colorScheme.primary)
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Подтвердите вашу электронную почту.\n",
                    style = AppTypography.getType().titleMedium.copy(color = MaterialTheme.colorScheme.primary)
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "На ваш адрес электронной почты было отправлено письмо со ссылкой для подтверждения. Пожалуйста, проверьте вашу почту и нажмите на ссылку для завершения регистрации.\n\n",
                    style = AppTypography.getType().bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Если вы не получили письмо, проверьте папку \"Спам\" или повторите попытку позже.\n",
                    style = AppTypography.getType().bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                )

                Button(
                    modifier = Modifier.padding(top = 16.dp),
                    shape = Shapes.medium,
                    onClick = resetStateFirstEntryDialog
                ) {
                    Text(
                        text = "Понял",
                        style = AppTypography.getType().titleMedium
                    )
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
                    isShowDialogConfirmRemoveRoute = true
                    routeForRemove = route
                }
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
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Выберите месяц и год",
                        style = AppTypography.getType().titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
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
                            Chip(
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
                            Chip(
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Применить")

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

    val brushMain = Brush.linearGradient(
        0.1f to MaterialTheme.colorScheme.surfaceVariant,
        1500.0f to MaterialTheme.colorScheme.surface,
        start = Offset.Zero,
        end = Offset.Infinite
    )
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
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            shape = Shapes.medium,
                            onClick = {
                                showMonthSheetVisible = true
                            }) {
                            val text = currentMonthOfYear?.month?.let {
                                getMonthFullText(it)
                            } ?: "загрузка"
                            Text(
                                text = "$text ${currentMonthOfYear?.year}",
                                style = AppTypography.getType().headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
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
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                containerColor = green,
                onClick = onNewRouteClick
            ) {
                Icon(
                    tint = MaterialTheme.colorScheme.background,
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null
                )
            }
        },
        bottomBar = {
            val colors: NavigationBarItemColors = NavigationBarItemDefaults.colors(
                unselectedTextColor = MaterialTheme.colorScheme.background,
                selectedTextColor = MaterialTheme.colorScheme.background,
                unselectedIconColor = MaterialTheme.colorScheme.background,
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.background
            )
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                NavigationBarItem(
                    colors = colors,
                    selected = true,
                    icon = {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Outlined.Home, contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = "Главная",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {}
                )

                NavigationBarItem(
                    colors = colors,
                    selected = false,
                    icon = {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(R.drawable.rub),
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = "ЗП",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {}
                )

                NavigationBarItem(
                    colors = colors,
                    selected = false,
                    icon = {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = "Настройки",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = onSettingsClick
                )

                NavigationBarItem(
                    colors = colors,
                    selected = false,
                    icon = {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = "Профиль",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {}
                )
            }
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
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HorizontalPager(
                            modifier = Modifier.animateItemPlacement(),
                            state = pagerState
                        ) { page ->
                            when (page) {
                                0 -> {
                                    MainInfo(
                                        totalTime = totalTime,
                                        totalTimeWithHoliday = totalTimeWithHoliday,
                                        currentMonthOfYear = currentMonthOfYear,
                                        dateAndTimeConverter = dateAndTimeConverter,
                                        brush = brushMain
                                    )
                                }

                                1 -> {
                                    DetailWorkTimeCard(
                                        totalTime = totalTime,
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
                                .animateItemPlacement(),
                            state = pagerState
                        )
                    }
                }

                val brushSecondary = Brush.linearGradient(
                    0.1f to Color(0xFFefede3),
                    1500.0f to Color(0xFFFDFDFC),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )

                item {
                    currentRoute?.let { route ->
                        var maxHeightBox by remember { mutableIntStateOf(widthScreen / 3) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItemPlacement()
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = 24.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                onRouteClick(route.basicData.id)
                                            }
                                        )
                                    },
                                text = "Текущий маршрут",
                                style = AppTypography.getType().titleMedium
                            )
                            LazyRow(
                                modifier = Modifier.padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
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
                                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                AnimatedCounter(
                                                    count = currentRouteWorkTime,
                                                    style = AppTypography.getType().headlineMedium.copy(
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.background
                                                    )
                                                )
                                                Text(
                                                    text = "На работе",
                                                    color = MaterialTheme.colorScheme.background,
                                                    maxLines = 1,
                                                    style = AppTypography.getType().titleMedium,
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
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.secondary
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
                                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                                verticalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                if (route.locomotives.isEmpty()) {
                                                    IconButton(
                                                        modifier = Modifier.align(Alignment.End),
                                                        colors = IconButtonDefaults.iconButtonColors(
                                                            containerColor = green,
                                                            contentColor = MaterialTheme.colorScheme.background
                                                        ),
                                                        onClick = {
                                                            onNewLocoClick(route.basicData.id)
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Add,
                                                            contentDescription = null
                                                        )
                                                    }
                                                } else {
                                                    Column(
                                                        modifier = Modifier
                                                            .padding(bottom = 8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        val loco = route.locomotives.last()
                                                        Box(
                                                            modifier = Modifier
                                                                .pointerInput(Unit) {
                                                                    detectTapGestures(
                                                                        onPress = {
                                                                            onChangedLocoClick(
                                                                                loco
                                                                            )
                                                                        }
                                                                    )
                                                                }
                                                        ) {
                                                            Text(
                                                                text = "${loco.series ?: ""} ${loco.number ?: ""}",
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1,
                                                                style = AppTypography.getType().titleLarge,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        if (route.locomotives.size > 1) {
                                                            Text(
                                                                text = "... и ещё ${route.locomotives.size - 1}",
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1,
                                                                style = AppTypography.getType().bodyLarge,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "Локомотив",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    style = AppTypography.getType().titleMedium,
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
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.secondary
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
                                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                                verticalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                if (route.trains.isEmpty()) {
                                                    IconButton(
                                                        modifier = Modifier.align(Alignment.End),
                                                        colors = IconButtonDefaults.iconButtonColors(
                                                            containerColor = green,
                                                            contentColor = MaterialTheme.colorScheme.background
                                                        ),
                                                        onClick = {
                                                            onNewTrainClick(route.basicData.id)
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Add,
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
                                                            train.number?.let {
                                                                Text(
                                                                    text = "№ $it",
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    maxLines = 1,
                                                                    style = AppTypography.getType().titleLarge,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
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
                                                                    style = AppTypography.getType().bodyLarge,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                        if (route.trains.size > 1) {
                                                            Text(
                                                                text = "... и ещё ${route.trains.size - 1}",
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1,
                                                                style = AppTypography.getType().bodyLarge,
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
                                                        style = AppTypography.getType().titleMedium,
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
                                                                color = if (nextIsDeparture) green else purple
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
                                                                    tint = if (it) green else purple
                                                                )
                                                            }
//                                                        AnimatedContent(targetState = onTheWay) {
//                                                            val text = if (it) {
//                                                                "Остановка"
//                                                            } else {
//                                                                "Отправление"
//                                                            }
//                                                            Text(
//                                                                text = text,
//                                                                maxLines = 1,
//                                                                overflow = TextOverflow.Ellipsis,
//                                                                style = AppTypography.getType().bodyLarge,
//                                                                color = if (it) purple else green
//                                                            )
//                                                        }
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
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.secondary
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
                                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                                verticalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                if (route.passengers.isEmpty()) {
                                                    IconButton(
                                                        modifier = Modifier.align(Alignment.End),
                                                        colors = IconButtonDefaults.iconButtonColors(
                                                            containerColor = green,
                                                            contentColor = MaterialTheme.colorScheme.background
                                                        ),
                                                        onClick = {
                                                            onNewPassengerClick(route.basicData.id)
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Add,
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
                                                                    style = AppTypography.getType().titleLarge,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                            Text(
                                                                text = "${passenger.stationDeparture ?: ""} ${passenger.stationArrival?.let { " - $it" } ?: ""} ",
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1,
                                                                style = AppTypography.getType().bodyLarge,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        if (route.passengers.size > 1) {
                                                            Text(
                                                                text = "... и ещё ${route.passengers.size - 1}",
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1,
                                                                style = AppTypography.getType().bodyLarge,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "Пассажиром",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    style = AppTypography.getType().titleMedium,
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
                            .animateItemPlacement()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Маршруты",
                                style = AppTypography.getType().titleMedium
                            )
                            TextButton(onClick = {
                                onAllRouteClick()
                            }) {
                                Text(
                                    color = MaterialTheme.colorScheme.tertiary,
                                    text = "Все"
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp),
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

                                    if (route.basicData.timeStartWork!! > Calendar.getInstance().timeInMillis) {
                                        background = MaterialTheme.colorScheme.surfaceBright
                                    } else {
                                        if (route.isTransition(offsetInMoscow)) {
                                            background = MaterialTheme.colorScheme.surfaceDim
                                        }
                                    }
                                    val dismissState =
                                        rememberDismissState(confirmStateChange = { newState ->
                                            if (newState == DismissValue.DismissedToStart) {
                                                isShowDialogConfirmRemoveRoute = true
                                                routeForRemove = route
                                                false
                                            } else {
                                                true
                                            }
                                        })

                                    ItemHomeScreen(
                                        modifier = Modifier.animateItemPlacement(),
                                        dismissState = dismissState,
                                        route = route,
                                        onDelete = {
                                            isShowDialogConfirmRemoveRoute = true
                                            routeForRemove = route
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
                                        isHolidayTimeInRoute = listRouteState[0].isHoliday
                                    )
                                }
                                if (listRouteState.size > 1) {
                                    val route = listRouteState[1].route
                                    var background = MaterialTheme.colorScheme.secondary

                                    if (route.basicData.timeStartWork!! > Calendar.getInstance().timeInMillis) {
                                        background = MaterialTheme.colorScheme.surfaceBright
                                    } else {
                                        if (route.isTransition(offsetInMoscow)) {
                                            background = MaterialTheme.colorScheme.surfaceDim
                                        }
                                    }
                                    val dismissState =
                                        rememberDismissState(confirmStateChange = { newState ->
                                            if (newState == DismissValue.DismissedToStart) {
                                                isShowDialogConfirmRemoveRoute = true
                                                routeForRemove = route
                                                false
                                            } else {
                                                true
                                            }
                                        })

                                    ItemHomeScreen(
                                        modifier = Modifier.animateItemPlacement(),
                                        route = route,
                                        dismissState = dismissState,
                                        onDelete = {
                                            isShowDialogConfirmRemoveRoute = true
                                            routeForRemove = route
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
                                        isHolidayTimeInRoute = listRouteState[1].isHoliday
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement()
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 24.dp),
                            text = "Действия"
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .padding(start = 12.dp)
                                        .size((widthScreen / 3).dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.LightGray
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.holiday_icon),
                                            contentDescription = null
                                        )
                                        Text(
                                            modifier = Modifier.weight(1f),
                                            text = "Создать график"
                                        )
                                    }
                                }
                            }
                            item {
                                Card(
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .size((widthScreen / 3).dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.LightGray
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.palma),
                                            contentDescription = null
                                        )
                                        Text(
                                            modifier = Modifier.weight(1f),
                                            text = "Добавить отвлечение"
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
                            .height(40.dp)
                            .animateItemPlacement()
                    )
                }
            }
        }
    }
}

@Composable
fun rememberShareManager(): ShareManager {
    val context = LocalContext.current
    return remember { ShareManager(context) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainInfo(
    totalTime: Long,
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
            defaultElevation = 3.dp,
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
                    .padding(horizontal = 12.dp, vertical = 24.dp)
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
                                text = ConverterLongToTime.getTimeInStringFormat(
                                    time
                                ),
                                style = AppTypography.getType().headlineMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.background
                            )
                            if (totalTime != time) {
                                val differenceTimeInLong = time.minus(totalTime)
                                val totalTime =
                                    ConverterLongToTime.getTimeInStringFormat(
                                        totalTime
                                    )
                                val differenceTime =
                                    ConverterLongToTime.getTimeInStringFormat(
                                        differenceTimeInLong
                                    )
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
                                    style = AppTypography.getType().titleMedium,
                                    fontWeight = FontWeight.Light,
                                    color = MaterialTheme.colorScheme.background
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
                                style = AppTypography.getType().bodyMedium,
                                color = MaterialTheme.colorScheme.background
                            )
                            Text(
                                text = "$normaHoursInMonth ч.",
                                style = AppTypography.getType().bodyLarge,
                                color = MaterialTheme.colorScheme.background
                            )
                        }
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            trackColor = MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.8f
                            ),
                            color = MaterialTheme.colorScheme.background,
                            strokeCap = StrokeCap.Round,
                            progress = { percent.toFloat() },
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
                                style = AppTypography.getType().bodyMedium,
                                color = MaterialTheme.colorScheme.background
                            )
                            Text(
                                text = "$normaHoursToday ч.",
                                style = AppTypography.getType().bodyLarge,
                                color = MaterialTheme.colorScheme.background
                            )
                        }
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            trackColor = MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.8f
                            ),
                            color = MaterialTheme.colorScheme.background,
                            strokeCap = StrokeCap.Round,
                            progress = { percent.toFloat() },
                        )
                    }
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
                                )
                            }
                        },
                        state = state
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 24.dp)
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
                                    text = ConverterLongToTime.getTimeInStringFormat(
                                        totalTimeWithHoliday
                                    ),
                                    style = AppTypography.getType().headlineMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.background
                                )
                                if (totalTime != totalTimeWithHoliday) {
                                    val differenceTimeInLong =
                                        totalTimeWithHoliday.minus(totalTime)
                                    val totalTime =
                                        ConverterLongToTime.getTimeInStringFormat(
                                            totalTime
                                        )
                                    val differenceTime =
                                        ConverterLongToTime.getTimeInStringFormat(
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
                                        style = AppTypography.getType().titleMedium,
                                        fontWeight = FontWeight.Light,
                                        color = MaterialTheme.colorScheme.background
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
                                        val nightTimeText =
                                            ConverterLongToTime.getTimeInStringFormat(nightTime)
                                        val percent =
                                            ((nightTime * 100).toFloat() / (totalTimeWithHoliday).toFloat()) / 100f
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
                                                style = AppTypography.getType().bodyMedium,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                            Text(
                                                text = nightTimeText,
                                                style = AppTypography.getType().bodyLarge,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp),
                                            trackColor = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.8f
                                            ),
                                            color = MaterialTheme.colorScheme.background,
                                            strokeCap = StrokeCap.Round,
                                            progress = { percent.toFloat() },
                                        )
                                    }
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
                                            ConverterLongToTime.getTimeInStringFormat(
                                                passengerTime
                                            )
                                        val percent =
                                            ((passengerTime * 100).toFloat() / (totalTimeWithHoliday).toFloat()) / 100f
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
                                                style = AppTypography.getType().bodyMedium,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                            Text(
                                                text = passengerTimeText,
                                                style = AppTypography.getType().bodyLarge,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp),
                                            trackColor = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.8f
                                            ),
                                            color = MaterialTheme.colorScheme.background,
                                            strokeCap = StrokeCap.Round,
                                            progress = { percent.toFloat() },
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
                                            ConverterLongToTime.getTimeInStringFormat(
                                                singleLocomotiveTime
                                            )
                                        val percent =
                                            ((singleLocomotiveTime * 100).toFloat() / (totalTimeWithHoliday).toFloat()) / 100f
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
                                                style = AppTypography.getType().bodyMedium,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                            Text(
                                                text = passengerTimeText,
                                                style = AppTypography.getType().bodyLarge,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp),
                                            trackColor = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.8f
                                            ),
                                            color = MaterialTheme.colorScheme.background,
                                            strokeCap = StrokeCap.Round,
                                            progress = { percent.toFloat() },
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailTrainCard(
    brush: Brush,
    totalTime: Long,
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
                                )
                            }
                        },
                        state = state
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 24.dp)
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
                                    text = ConverterLongToTime.getTimeInStringFormat(
                                        totalTimeWithHoliday
                                    ),
                                    style = AppTypography.getType().headlineMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.background
                                )
                                if (totalTime != totalTimeWithHoliday) {
                                    val differenceTimeInLong =
                                        totalTimeWithHoliday.minus(totalTime)
                                    val totalTime =
                                        ConverterLongToTime.getTimeInStringFormat(
                                            totalTime
                                        )
                                    val differenceTime =
                                        ConverterLongToTime.getTimeInStringFormat(
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
                                        style = AppTypography.getType().titleMedium,
                                        fontWeight = FontWeight.Light,
                                        color = MaterialTheme.colorScheme.background
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
                                            ConverterLongToTime.getTimeInStringFormat(
                                                extendedServicePhaseTime
                                            )
                                        val percent =
                                            ((extendedServicePhaseTime * 100).toFloat() / (totalTimeWithHoliday).toFloat()) / 100f
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
                                                style = AppTypography.getType().bodyMedium,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                            Text(
                                                text = extendedServicePhaseTimeText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Visible,
                                                style = AppTypography.getType().bodyLarge,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp),
                                            trackColor = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.8f
                                            ),
                                            color = MaterialTheme.colorScheme.background,
                                            strokeCap = StrokeCap.Round,
                                            progress = { percent.toFloat() },
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
                                            ConverterLongToTime.getTimeInStringFormat(
                                                longDistanceTrainsTime
                                            )
                                        val percent =
                                            ((longDistanceTrainsTime * 100).toFloat() / (totalTimeWithHoliday).toFloat()) / 100f
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
                                                style = AppTypography.getType().bodyMedium,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                            Text(
                                                text = longDistanceTrainsTimeText,
                                                style = AppTypography.getType().bodyLarge,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp),
                                            trackColor = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.8f
                                            ),
                                            color = MaterialTheme.colorScheme.background,
                                            strokeCap = StrokeCap.Round,
                                            progress = { percent.toFloat() },
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
                                            ConverterLongToTime.getTimeInStringFormat(
                                                heavyTrainsTime
                                            )
                                        val percent =
                                            ((heavyTrainsTime * 100).toFloat() / (totalTimeWithHoliday).toFloat()) / 100f
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
                                                style = AppTypography.getType().bodyMedium,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                            Text(
                                                text = heavyTrainsTimeText,
                                                style = AppTypography.getType().bodyLarge,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp),
                                            trackColor = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.8f
                                            ),
                                            color = MaterialTheme.colorScheme.background,
                                            strokeCap = StrokeCap.Round,
                                            progress = { percent.toFloat() },
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
                                        val onePersonOperationTimeText =
                                            ConverterLongToTime.getTimeInStringFormat(
                                                onePersonOperationTime
                                            )
                                        val percent =
                                            ((onePersonOperationTime * 100).toFloat() / (totalTimeWithHoliday).toFloat()) / 100f
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
                                                style = AppTypography.getType().bodyMedium,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                            Text(
                                                text = onePersonOperationTimeText,
                                                style = AppTypography.getType().bodyLarge,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp),
                                            trackColor = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.8f
                                            ),
                                            color = MaterialTheme.colorScheme.background,
                                            strokeCap = StrokeCap.Round,
                                            progress = { percent.toFloat() },
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

@Composable
fun DetailInfo() {
    val localDensity = LocalDensity.current
    var cardWidthDp by remember {
        mutableStateOf(0.dp)
    }

    Card(
        modifier = Modifier
            .padding(12.dp)
            .onGloballyPositioned { coordinates ->
                cardWidthDp = with(localDensity) { coordinates.size.width.toDp() }
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            contentAlignment = Alignment.Center
        ) {
            PieChart(
                data = mapOf(
                    Pair("Пассажиром", 19),
                    Pair("Резервом", 8),
                    Pair("Остальные", 220)
                ),
                centerText = "247:00",
                radiusOuter = cardWidthDp * 0.18f,
                nightTime = 80
            )
        }
    }
}