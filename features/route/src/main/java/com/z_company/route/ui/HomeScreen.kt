package com.z_company.route.ui

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BasicTooltipBox
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material3.Badge
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
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
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Train
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.domain.util.minus
import com.z_company.domain.util.toMoneyString
import com.z_company.route.R
import android.net.Uri
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import com.z_company.route.component.AnimatedCounter
import com.z_company.route.component.AnimationDialog
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.ChipApp
import com.z_company.route.component.ItemHomeScreen
import com.z_company.route.component.LinearPagerIndicator
import com.z_company.route.component.PdfActionSheet
import com.z_company.route.component.PdfContentDialog
import com.z_company.route.component.PreviewRouteDialog
import com.z_company.route.component.HomeScreenSkeleton
import com.z_company.route.viewmodel.PdfViewModel
import com.z_company.route.viewmodel.home_view_model.HomeViewModel
import com.z_company.route.viewmodel.home_view_model.ItemState
import com.z_company.route.viewmodel.home_view_model.UpdateEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Calendar

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
    listRouteState: List<ItemState>,
    onRouteClick: (String) -> Unit,
    onMoreInfoClick: (String) -> Unit,
    makeCopyRoute: (String) -> Unit,
    onDeleteRoute: (Route) -> Unit,
    onSearchClick: () -> Unit,
    totalTime: Long,
    todayWorkTime: Long = 0L,
    isConsiderFutureRoute: Boolean = false,
    currentMonthOfYear: MonthOfYear?,
    monthList: List<Int>,
    yearList: List<Int>,
    selectYearAndMonth: (Pair<Int, Int>) -> Unit,
    minTimeRest: Long?,
    nightTimeState: ResultState<Long>?,
    singleLocomotiveTimeState: ResultState<Long>?,
    passengerTimeState: ResultState<Long>?,
    totalTimeWithHoliday: ResultState<Long>?,
    toBeCredited: ResultState<Double>? = null,
    onSalaryClick: () -> Unit = {},
    calculationHomeRest: (Route?) -> Unit,
    homeRestValue: Long?,
    offsetInMoscow: Long,
    timeCalculationContext: TimeCalculationContext? = null,
    syncRoute: (Route) -> Unit,
    shareRoute: (Route) -> Unit,
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
    normaHours: Int? = null,
    unsyncedRoutesCount: Int = 0,
    onSyncClick: () -> Unit = {},
    showSyncDialog: Boolean = false,
    isSyncSuccess: Boolean = false,
    isSyncComplete: Boolean = false,
    syncType: com.z_company.route.viewmodel.SyncType? = null,
    syncUploadProgress: Map<String, com.z_company.route.viewmodel.SyncStepState> = emptyMap(),
    syncRouteErrors: List<String> = emptyList(),
    syncRoutesTotalAttempted: Int = 0,
    syncRoutesSavedCount: Int = 0,
    syncReportUserId: String? = null,
    isNetworkError: Boolean = false,
    onResetSyncState: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val interactionSource = remember { MutableInteractionSource() }

    val snackbarHostState = remember { SnackbarHostState() }

    val snackbarManager: ISnackbarManager = koinInject()
    val pdfViewModel: PdfViewModel = koinInject()

    var showPdfDialog by remember { mutableStateOf(false) }
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    val isPdfGenerating by pdfViewModel.isGenerating.collectAsState()
    val pdfError by pdfViewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        pdfViewModel.pdfReady.collect { uri ->
            pdfUri = uri
        }
    }

    LaunchedEffect(pdfError) {
        pdfError?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(Unit) {
        snackbarManager.events
            .flowWithLifecycle(lifecycle)
            .collect { event ->
                // collect вместо collectLatest: события показываются последовательно.
                // collectLatest отменял showSnackbar() первого тоста при появлении второго —
                // «Маршрут сохранен» обрывался при приходе «Маршрут сохранен в облаке».
                val result = snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = event.actionLabel,
                    duration = event.duration
                )
                if (result == SnackbarResult.ActionPerformed) {
                    event.onAction?.let { onAction ->
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

    // Тип шторки со списком единиц текущего маршрута (loco/train/passenger), null — скрыта
    var unitsSheetType by remember { mutableStateOf<String?>(null) }
    val unitsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var routeForPreview by remember {
        mutableStateOf<Route?>(null)
    }

    var routeForRemove by remember {
        mutableStateOf<Route?>(null)
    }

    var showContextDialog by remember {
        mutableStateOf(false)
    }

    val currentRouteWorkTime by remember(lifecycle) {
        currentRouteTimeWork
            .flowWithLifecycle(lifecycle)
            .map { ConverterLongToTime.getTimeInStringFormat(it) }
    }.collectAsState(initial = "")

    val countdownText by remember(lifecycle) {
        countdownToNextRoute
            .flowWithLifecycle(lifecycle)
            .map { ConverterLongToTime.getTimeInStringFormat(it) }
    }.collectAsState(initial = "")

    LaunchedEffect(saveTimeEvent) {
        saveTimeEvent.collectLatest {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
        }
    }

    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshTimer()
        }
    }

    if (showPdfDialog) {
        val routes = listRouteState.map { it.route }
        val monthLabel = currentMonthOfYear?.let { "${getMonthFullText(it.month)} ${it.year}" } ?: ""
        PdfContentDialog(
            onDismiss = { showPdfDialog = false },
            onGenerate = { sections ->
                showPdfDialog = false
                pdfViewModel.generateAndShare(sections, routes, monthLabel, currentMonthOfYear?.days ?: emptyList())
            }
        )
    }

    pdfUri?.let { uri ->
        PdfActionSheet(
            uri = uri,
            onDismiss = { pdfUri = null }
        )
    }

    // Диалог синхронизации
    SyncProgressDialog(
        showDialog = showSyncDialog,
        isSyncSuccess = isSyncSuccess,
        isSyncComplete = isSyncComplete,
        syncType = syncType,
        progressMap = syncUploadProgress,
        syncRouteErrors = syncRouteErrors,
        syncRoutesTotalAttempted = syncRoutesTotalAttempted,
        syncRoutesSavedCount = syncRoutesSavedCount,
        userId = syncReportUserId,
        isNetworkError = isNetworkError,
        onDismiss = onResetSyncState
    )

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
                shareRoute = shareRoute,
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

    // Шторка со списком единиц текущего маршрута (несколько локо/поездов/пассажиров)
    if (unitsSheetType != null) {
        currentRoute?.let { route ->
            ModalBottomSheet(
                onDismissRequest = { unitsSheetType = null },
                sheetState = unitsSheetState,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val titleText = when (unitsSheetType) {
                        "loco" -> "Локомотивы · ${route.locomotives.size}"
                        "train" -> "Поезда · ${route.trains.size}"
                        else -> "Пассажиром · ${route.passengers.size}"
                    }
                    val addText = when (unitsSheetType) {
                        "loco" -> "Добавить локомотив"
                        "train" -> "Добавить поезд"
                        else -> "Добавить пассажиром"
                    }
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
                    )
                    when (unitsSheetType) {
                        "loco" -> route.locomotives.forEachIndexed { index, loco ->
                            UnitSheetRow(R.drawable.ic_card_locomotive_ref, locomotiveName(loco, index + 1)) {
                                unitsSheetType = null; onChangedLocoClick(loco)
                            }
                        }
                        "train" -> route.trains.forEach { train ->
                            val first = train.stations.firstOrNull()?.stationName
                            val last = if (train.stations.size > 1) train.stations.last().stationName else null
                            UnitSheetRow(R.drawable.ic_card_train_ref, routeUnitFullName(train.number, train.servicePhase, first, last)) {
                                unitsSheetType = null; onChangedTrainClick(train)
                            }
                        }
                        else -> route.passengers.forEach { p ->
                            UnitSheetRow(R.drawable.ic_card_passenger_ref, routeUnitFullName(p.trainNumber, null, p.stationDeparture, p.stationArrival)) {
                                unitsSheetType = null; onChangedPassengerClick(p)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    UnitSheetAddButton(addText) {
                        val type = unitsSheetType
                        unitsSheetType = null
                        when (type) {
                            "loco" -> onNewLocoClick(route.basicData.id)
                            "train" -> onNewTrainClick(route.basicData.id)
                            else -> onNewPassengerClick(route.basicData.id)
                        }
                    }
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
        1f to MaterialTheme.colorScheme.surface,
        1f to MaterialTheme.colorScheme.surface,
        start = Offset.Zero,
        end = Offset.Infinite
    )

    val darkBrushMain = Brush.linearGradient(
        1f to MaterialTheme.colorScheme.surface,
        1f to MaterialTheme.colorScheme.surface,
        start = Offset.Zero,
        end = Offset.Infinite
    )

    val brushMain = if (isSystemInDarkTheme()) darkBrushMain else lightBrushMain

    Scaffold(
        topBar = {
            val textMonth = currentMonthOfYear?.month?.let {
                getMonthFullText(it)
            } ?: "загрузка"
            val yearText = currentMonthOfYear?.year?.toString() ?: ""

            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                // Верхняя строка: «М» логотип + «Машинист» + иконки
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "М",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = "Машинист",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                painter = painterResource(R.drawable.search_24px),
                                contentDescription = "Поиск",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { }) {
                            Icon(
                                painter = painterResource(R.drawable.person_24px),
                                contentDescription = "Профиль",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
                // Заголовок месяца — большой, кликабельный
                TextButton(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = { showMonthSheetVisible = true }
                ) {
                    Text(
                        text = textMonth,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (yearText.isNotEmpty()) {
                        Text(
                            modifier = Modifier.padding(start = 6.dp),
                            text = yearText,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val pagerState = rememberPagerState(pageCount = { 3 })
        AsyncData(
            resultState = uiState,
            loadingContent = { HomeScreenSkeleton(contentPadding = padding) }
        ) {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag("home_lazy_column"),
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
                        // Hero «ОТРАБОТАНО» + число + чип — фиксирован НАД свайп-карточкой
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        ) {
                            Text(
                                text = "ОТРАБОТАНО",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                            AsyncDataValue(resultState = totalTimeWithHoliday) { time ->
                                val chipText: String? = currentMonthOfYear?.let { month ->
                                    val normaHoursInMonth = normaHours ?: month.getPersonalNormaHours()
                                    if (normaHoursInMonth > 0) {
                                        val normaMillis = normaHoursInMonth.toLong() * 3_600_000L
                                        val diff = totalTime - normaMillis
                                        val isOvertime = diff >= 0
                                        val remainingMillis = if (isOvertime) diff else -diff
                                        val timeStr = viewModel.convertTimeToStringFormat(remainingMillis)
                                        if (isOvertime) "сверх $timeStr" else "еще $timeStr"
                                    } else null
                                }
                                val breakdown = if (totalTime != time) {
                                    val diffMs = time.minus(totalTime)
                                    " (${viewModel.convertTimeToStringFormat(totalTime)} + ${viewModel.convertTimeToStringFormat(diffMs)})"
                                } else null
                                com.z_company.route.component.WorkedTimeHeader(
                                    time = viewModel.convertTimeToStringFormat(time),
                                    breakdownText = breakdown,
                                    chipText = chipText,
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        HorizontalPager(
                            modifier = Modifier.animateItem(),
                            state = pagerState,
                            // verticalAlignment=Top + Card.wrapContentHeight(Align.Top)
                            // не дают карточке растягиваться на высоту самой
                            // высокой страницы пейджера (иначе MainInfo с 2-мя
                            // строками выглядит с пустым местом снизу из-за того,
                            // что DetailWorkTimeCard выше).
                            verticalAlignment = Alignment.Top
                        ) { page ->
                            when (page) {
                                0 -> {
                                    MainInfo(
                                        totalTime = totalTime,
                                        todayWorkTime = todayWorkTime,
                                        isConsiderFutureRoute = isConsiderFutureRoute,
                                        convertTimeToString = viewModel::convertTimeToStringFormat,
                                        totalTimeWithHoliday = totalTimeWithHoliday,
                                        currentMonthOfYear = currentMonthOfYear,
                                        dateAndTimeConverter = dateAndTimeConverter,
                                        brush = brushMain,
                                        normaHours = normaHours,
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
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
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
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Проверьте подключение к интернету и выполните синхронизацию.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = onSyncClick,
                                        shape = Shapes.medium,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                        )
                                    ) {
                                        Text(
                                            text = "Синхронизировать",
                                            color = MaterialTheme.colorScheme.secondary,
                                            style = MaterialTheme.typography.bodyMedium,
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
                                text = "ТЕКУЩИЙ МАРШРУТ",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(
                                modifier = Modifier.padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 12.dp)
                                            .size(156.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier
                                                .size(150.dp)
                                                .align(Alignment.BottomStart)
                                                .clickable { onRouteClick(route.basicData.id) },
                                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize().background(brushMain)) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(16.dp),
                                                    verticalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "НА РАБОТЕ",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Column {
                                                        AnimatedCounter(
                                                            count = currentRouteWorkTime,
                                                            style = MaterialTheme.typography.headlineLarge.copy(
                                                                fontFamily = com.z_company.core.ui.theme.MonoFont,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                letterSpacing = (-1).sp,
                                                            ),
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        val workMillis = route.basicData.timeStartWork?.let {
                                                            System.currentTimeMillis() - it
                                                        } ?: 0L
                                                        val workHours = workMillis / 3_600_000f
                                                        val maxHours = 12f
                                                        val progress = (workHours / maxHours).coerceIn(0f, 1f)
                                                        val barColor = if (workHours > maxHours) MaterialTheme.colorScheme.error
                                                            else MaterialTheme.colorScheme.tertiary
                                                        LinearProgressIndicator(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(3.dp),
                                                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                                                            color = barColor,
                                                            drawStopIndicator = {},
                                                            progress = { progress },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                // Локомотив/Поезд/Пассажир: заполненные плитки левее,
                                // пустые правее; внутри групп — порядок loco→train→passenger.
                                val unitOrder = listOf("loco", "train", "passenger").sortedBy { type ->
                                    val isEmpty = when (type) {
                                        "loco" -> route.locomotives.isEmpty()
                                        "train" -> route.trains.isEmpty()
                                        else -> route.passengers.isEmpty()
                                    }
                                    if (isEmpty) 1 else 0
                                }
                                unitOrder.forEachIndexed { idx, type ->
                                    item(key = type) {
                                        val endMod = if (idx == unitOrder.lastIndex) Modifier.padding(end = 12.dp) else Modifier
                                        RouteUnitTile(
                                            type = type,
                                            route = route,
                                            modifier = endMod,
                                            onOpenSheet = { unitsSheetType = it },
                                            onChangedLoco = onChangedLocoClick,
                                            onNewLoco = onNewLocoClick,
                                            onChangedTrain = onChangedTrainClick,
                                            onNewTrain = onNewTrainClick,
                                            onChangedPassenger = onChangedPassengerClick,
                                            onNewPassenger = onNewPassengerClick,
                                        )
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
                                text = "СЛЕДУЮЩИЙ МАРШРУТ",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            // Единая карточка по референсу: «ДО ЯВКИ ОСТАЛОСЬ» + счётчик + явка
                            Card(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .fillMaxWidth()
                                    .clickable { onRouteClick(nextFutureRoute.basicData.id) },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = "ДО ЯВКИ ОСТАЛОСЬ",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    AnimatedCounter(
                                        count = countdownText,
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontFamily = com.z_company.core.ui.theme.MonoFont,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                            letterSpacing = (-1).sp,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    androidx.compose.material3.HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(11.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.schedule_24px),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.tertiary,
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Явка",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                text = dateAndTimeConverter?.getDateMiniAndTime(
                                                    nextFutureRoute.basicData.timeStartWork
                                                ) ?: "",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
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
                                text = "ПОСЛЕДНИЕ МАРШРУТЫ · ${listRouteState.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(
                                modifier = Modifier.testTag("home_all_routes_button"),
                                onClick = {
                                    onAllRouteClick()
                                }
                            ) {
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
                                if (listRouteState.isNotEmpty()) {
                                    val firstItem = listRouteState.first()
                                    val route = firstItem.route
                                    val background = when {
                                        firstItem.isFuture -> MaterialTheme.colorScheme.surfaceBright
                                        firstItem.isTransition -> MaterialTheme.colorScheme.surfaceDim
                                        else -> MaterialTheme.colorScheme.secondary
                                    }

//                                    val dismissState = rememberDismissState()

                                    ItemHomeScreen(
                                        modifier = Modifier
                                            .animateItem()
                                            .testTag("home_first_route_card"),
                                        convertTimeToString = viewModel::convertTimeToStringFormat,
                                        route = route,
                                        onRequestDelete = {
                                            routeForRemove = route
                                            isShowDialogConfirmRemoveRoute = true
                                        },
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
                                        isLongCompositionTrain = listRouteState[0].isLongCompositionTrain,
                                        isExtendedServicePhaseTrains = listRouteState[0].isExtendedServicePhaseTrains,
                                        isHolidayTimeInRoute = listRouteState[0].isHoliday,
                                        number = listRouteState.size,
                                        monthOfYear = currentMonthOfYear,
                                        offsetInMoscow = offsetInMoscow,
                                        timeCalculationContext = timeCalculationContext,
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
                                    val secondItem = listRouteState[1]
                                    val route = secondItem.route
                                    val background = when {
                                        secondItem.isFuture -> MaterialTheme.colorScheme.surfaceBright
                                        secondItem.isTransition -> MaterialTheme.colorScheme.surfaceDim
                                        else -> MaterialTheme.colorScheme.secondary
                                    }

                                    ItemHomeScreen(
                                        modifier = Modifier.animateItem(),
                                        route = route,
                                        convertTimeToString = viewModel::convertTimeToStringFormat,
                                        onRequestDelete = {
                                            routeForRemove = route
                                            isShowDialogConfirmRemoveRoute = true
                                        },
                                        onLongClick = {
                                            showContextDialog = true
                                            routeForPreview = route
                                        },
                                        containerColor = background,
                                        onClick = { onRouteClick(route.basicData.id) },
                                        dateAndTimeConverter = dateAndTimeConverter,
                                        isHeavyTrains = listRouteState[1].isHeavyTrains,
                                        isLongCompositionTrain = listRouteState[1].isLongCompositionTrain,
                                        isExtendedServicePhaseTrains = listRouteState[1].isExtendedServicePhaseTrains,
                                        isHolidayTimeInRoute = listRouteState[1].isHoliday,
                                        number = listRouteState.size - 1,
                                        monthOfYear = currentMonthOfYear,
                                        offsetInMoscow = offsetInMoscow,
                                        timeCalculationContext = timeCalculationContext,
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
                            style = MaterialTheme.typography.labelMedium,
                            text = "ИНСТРУМЕНТЫ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyRow(
                            modifier = Modifier
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Все карточки используют единый компонент ActionCard
                            // для консистентного стиля (layout, шрифт, цвета).
                            item {
                                ActionCard(
                                    modifier = Modifier.padding(start = 12.dp),
                                    title = "График",
                                    iconRes = R.drawable.ic_card_calendar,
                                    iconTint = MaterialTheme.colorScheme.surfaceContainerLow,
                                    widthScreen = widthScreen,
                                    interactionSource = interactionSource,
                                    onSizeChanged = { size ->
                                        if (size.height > maxHeightBox) {
                                            maxHeightBox = size.height
                                        }
                                    },
                                    minHeightDp = maxHeightBox.toDp(),
                                    onClick = { onWorkScheduleScreen() }
                                )
                            }
                            item {
                                ActionCard(
                                    title = "Отвлечения",
                                    iconRes = R.drawable.ic_card_vacation,
                                    iconTint = MaterialTheme.colorScheme.surfaceContainerLow,
                                    widthScreen = widthScreen,
                                    interactionSource = interactionSource,
                                    onSizeChanged = { size ->
                                        if (size.height > maxHeightBox) {
                                            maxHeightBox = size.height
                                        }
                                    },
                                    minHeightDp = maxHeightBox.toDp(),
                                    onClick = { onClickVacation() }
                                )
                            }
                            // Карточка "PDF" — открывает диалог формирования PDF
                            // (логика перенесена из иконки в TopAppBar)
                            item {
                                ActionCard(
                                    title = "PDF",
                                    iconRes = R.drawable.ic_card_pdf,
                                    iconTint = MaterialTheme.colorScheme.surfaceContainerLow,
                                    widthScreen = widthScreen,
                                    interactionSource = interactionSource,
                                    onSizeChanged = { size ->
                                        if (size.height > maxHeightBox) {
                                            maxHeightBox = size.height
                                        }
                                    },
                                    minHeightDp = maxHeightBox.toDp(),
                                    enabled = !isPdfGenerating,
                                    onClick = { showPdfDialog = true }
                                )
                            }
                            // Карточка "Поиск" — открывает экран поиска маршрутов
                            // (логика перенесена из иконки в TopAppBar)
                            item {
                                ActionCard(
                                    modifier = Modifier.padding(end = 12.dp),
                                    title = "Поиск",
                                    iconRes = R.drawable.ic_card_search,
                                    iconTint = MaterialTheme.colorScheme.surfaceContainerLow,
                                    widthScreen = widthScreen,
                                    interactionSource = interactionSource,
                                    onSizeChanged = { size ->
                                        if (size.height > maxHeightBox) {
                                            maxHeightBox = size.height
                                        }
                                    },
                                    minHeightDp = maxHeightBox.toDp(),
                                    onClick = { onSearchClick() }
                                )
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

/** Плитка единицы текущего маршрута (локомотив/поезд/пассажир) — единый рендер
 * для всех трёх типов, чтобы рендерить их в произвольном порядке. */
@Composable
private fun RouteUnitTile(
    type: String,
    route: Route,
    modifier: Modifier,
    onOpenSheet: (String) -> Unit,
    onChangedLoco: (Locomotive) -> Unit,
    onNewLoco: (String) -> Unit,
    onChangedTrain: (Train) -> Unit,
    onNewTrain: (String) -> Unit,
    onChangedPassenger: (Passenger) -> Unit,
    onNewPassenger: (String) -> Unit,
) {
    when (type) {
        "loco" -> StackedTile(
            modifier = modifier,
            count = route.locomotives.size,
            iconRes = R.drawable.ic_card_locomotive_ref,
            label = "ЛОКОМОТИВ",
            title = route.locomotives.lastOrNull()?.let { locomotiveName(it, route.locomotives.size) },
            onClick = {
                when {
                    route.locomotives.size > 1 -> onOpenSheet("loco")
                    route.locomotives.size == 1 -> onChangedLoco(route.locomotives.first())
                    else -> onNewLoco(route.basicData.id)
                }
            },
            onAddClick = { onNewLoco(route.basicData.id) },
        )
        "train" -> {
            val train = route.trains.lastOrNull()
            val first = train?.stations?.firstOrNull()?.stationName
            val last = if ((train?.stations?.size ?: 0) > 1) train?.stations?.last()?.stationName else null
            StackedTile(
                modifier = modifier,
                count = route.trains.size,
                iconRes = R.drawable.ic_card_train_ref,
                label = "ПОЕЗД",
                title = train?.let { routeUnitTitle(it.number, it.servicePhase, first, last) },
                subtitle = train?.let { routeUnitSubtitle(it.number, it.servicePhase, first, last) },
                onClick = {
                    when {
                        route.trains.size > 1 -> onOpenSheet("train")
                        route.trains.size == 1 -> onChangedTrain(route.trains.first())
                        else -> onNewTrain(route.basicData.id)
                    }
                },
                onAddClick = { onNewTrain(route.basicData.id) },
            )
        }
        else -> {
            val p = route.passengers.lastOrNull()
            StackedTile(
                modifier = modifier,
                count = route.passengers.size,
                iconRes = R.drawable.ic_card_passenger_ref,
                label = "ПАССАЖИРОМ",
                title = p?.let { routeUnitTitle(it.trainNumber, null, it.stationDeparture, it.stationArrival) },
                subtitle = p?.let { routeUnitSubtitle(it.trainNumber, null, it.stationDeparture, it.stationArrival) },
                onClick = {
                    when {
                        route.passengers.size > 1 -> onOpenSheet("passenger")
                        route.passengers.size == 1 -> onChangedPassenger(route.passengers.first())
                        else -> onNewPassenger(route.basicData.id)
                    }
                },
                onAddClick = { onNewPassenger(route.basicData.id) },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Имена единиц маршрута (с фолбэками для пустых полей)
// ─────────────────────────────────────────────────────────────

/** Локомотив: серия+номер; только серия → «серия б/н»; только номер →
 * «{тип тяги} {номер}»; пусто → «{тип тяги} {порядковый}». */
private fun locomotiveName(loco: Locomotive, ordinal: Int): String {
    val s = loco.series?.takeIf { it.isNotBlank() }
    val n = loco.number?.takeIf { it.isNotBlank() }
    return when {
        s != null && n != null -> "$s-$n"
        s != null -> "$s б/н"
        n != null -> "${loco.type.text} $n"
        else -> "${loco.type.text} $ordinal"
    }
}

/** Строка станций: «A — B» / «A — » / « — B» / null. */
private fun stationsLine(first: String?, last: String?): String? {
    val f = first?.takeIf { it.isNotBlank() }
    val l = last?.takeIf { it.isNotBlank() }
    return when {
        f != null && l != null -> "$f — $l"
        f != null -> "$f — "
        l != null -> " — $l"
        else -> null
    }
}

/** Заголовок поезда/пассажира: номер → «№N»; иначе плечо → станции плеча;
 * иначе станции маршрута; иначе «б/н». */
private fun routeUnitTitle(number: String?, shoulder: ServicePhase?, first: String?, last: String?): String {
    val num = number?.takeIf { it.isNotBlank() }
    return when {
        num != null -> "№$num"
        shoulder != null -> "${shoulder.departureStation} — ${shoulder.arrivalStation}"
        else -> stationsLine(first, last) ?: "б/н"
    }
}

/** Подзаголовок (станции), показывается только когда заголовок — это номер. */
private fun routeUnitSubtitle(number: String?, shoulder: ServicePhase?, first: String?, last: String?): String? {
    if (number.isNullOrBlank()) return null
    return shoulder?.let { "${it.departureStation} — ${it.arrivalStation}" } ?: stationsLine(first, last)
}

/** Полное имя для строки шторки (заголовок + станции). */
private fun routeUnitFullName(number: String?, shoulder: ServicePhase?, first: String?, last: String?): String {
    val title = routeUnitTitle(number, shoulder, first, last)
    val sub = routeUnitSubtitle(number, shoulder, first, last)
    return if (sub != null) "$title $sub" else title
}

/** Строка единицы в шторке «Список»: аватар-иконка + название + шеврон. */
@Composable
private fun UnitSheetRow(iconRes: Int, name: String, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                .background(c.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = c.tertiary,
            )
        }
        Text(
            modifier = Modifier.weight(1f),
            text = name,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = c.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_right_24px),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = c.onSurfaceVariant,
        )
    }
}

/** Кнопка «+ Добавить …» в шторке списка — пунктирная рамка. */
@Composable
private fun UnitSheetAddButton(text: String, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val border = c.outline
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .drawBehind {
                drawRoundRect(
                    color = border,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                )
            }
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                painter = painterResource(com.z_company.core.R.drawable.ic_add),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = c.tertiary,
            )
            Text(text, style = MaterialTheme.typography.bodyMedium, color = c.tertiary)
        }
    }
}

@Composable
private fun StackedTile(
    modifier: Modifier = Modifier,
    count: Int,
    iconRes: Int,
    useImage: Boolean = false,
    label: String,
    title: String?,
    subtitle: String? = null,
    onClick: () -> Unit,
    onAddClick: () -> Unit,
) {
    val tileSize = 150.dp
    val stackOffset = 6.dp
    val hasStack = count > 1
    val isEmpty = title == null
    val c = MaterialTheme.colorScheme
    val borderStrongColor = c.outline

    // Общее содержимое плитки (иконка+бейдж / label+данные / кнопка +)
    val tileContent: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {
        Box(modifier = Modifier.fillMaxWidth()) {
            val iconTint = if (isEmpty) c.onSurfaceVariant.copy(alpha = 0.45f) else c.tertiary
            if (useImage) {
                Image(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(32.dp))
            } else {
                Icon(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(32.dp), tint = iconTint)
            }
            if (hasStack) {
                Badge(
                    modifier = Modifier.align(Alignment.TopEnd),
                    containerColor = c.tertiary,
                    contentColor = c.surface,
                ) { Text("$count") }
            }
        }
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
            if (title != null) {
                // AutoSizeText: ужимает длинные имена («Электротяга 1») под ширину плитки
                com.z_company.core.ui.component.AutoSizeText(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = c.primary,
                    maxLines = 1,
                    maxTextSize = 17.sp,
                    minTextSize = 11.sp,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
        // Кнопка «+» — круг с accentSoft фоном
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(c.primaryContainer)
                    .clickable { onAddClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(com.z_company.core.R.drawable.ic_add),
                    contentDescription = "Добавить",
                    modifier = Modifier.size(18.dp),
                    tint = c.tertiary,
                )
            }
        }
    }

    Box(
        modifier = modifier.size(tileSize + stackOffset),
    ) {
        // Нижняя (фоновая) карточка — выглядывает справа-СВЕРХУ на stackOffset
        if (hasStack) {
            Card(
                modifier = Modifier
                    .size(tileSize)
                    .align(Alignment.TopEnd),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = c.surface),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            ) {}
        }
        if (isEmpty) {
            // Пустая плитка — пунктирная рамка, прозрачный фон
            Box(
                modifier = Modifier
                    .size(tileSize)
                    .align(Alignment.BottomStart)
                    .drawBehind {
                        drawRoundRect(
                            color = borderStrongColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                        )
                    },
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(14.dp).clickable { onClick() },
                    verticalArrangement = Arrangement.SpaceBetween,
                    content = tileContent,
                )
            }
        } else {
            // Заполненная плитка — surface-карточка
            Card(
                modifier = Modifier.size(tileSize).align(Alignment.BottomStart),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = c.surface),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(14.dp).clickable { onClick() },
                    verticalArrangement = Arrangement.SpaceBetween,
                    content = tileContent,
                )
            }
        }
    }
}

/**
 * Карточка действия в LazyRow «Действия» на HomeScreen (PDF, Поиск, и т.д.).
 * Унифицированная по стилю с карточками "График" и "Отвлечения" — тот же размер,
 * фон и тень, но иконка — vector drawable с tint вместо webp Image.
 */
@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    title: String,
    iconRes: Int,
    iconTint: Color,
    widthScreen: Int,
    interactionSource: MutableInteractionSource,
    onSizeChanged: (androidx.compose.ui.unit.IntSize) -> Unit,
    minHeightDp: androidx.compose.ui.unit.Dp,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .onSizeChanged(onSizeChanged)
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
            .clickable(enabled = enabled, onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 1.dp,
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.primary
        ),
    ) {
        // Column: верхняя зона (Box weight=1f) — иконка по центру оставшегося места.
        // Нижняя зона — текст в левом нижнем углу. Иконка визуально по центру
        // верхней части карточки, не пересекается с текстом.
        Column(
            modifier = Modifier
                .defaultMinSize(
                    minWidth = (widthScreen / 3).dp,
                    minHeight = minHeightDp,
                )
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(48.dp),
                    painter = painterResource(iconRes),
                    contentDescription = title,
                    tint = iconTint,
                )
            }
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Ellipsis,
                text = title,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainInfo(
    totalTime: Long,
    todayWorkTime: Long = 0L,
    isConsiderFutureRoute: Boolean = false,
    convertTimeToString: (Long?) -> String,
    totalTimeWithHoliday: ResultState<Long>?,
    currentMonthOfYear: MonthOfYear?,
    dateAndTimeConverter: DateAndTimeConverter?,
    brush: Brush,
    normaHours: Int? = null,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .wrapContentHeight(Alignment.Top)
            .fillMaxWidth(),
    ) {
            // Свайп-карточка с прогресс-барами (внутри Card)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                currentMonthOfYear?.let { month ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val normaHoursInMonth =
                            normaHours ?: month.getPersonalNormaHours()
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$normaHoursInMonth ч.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                            color = MaterialTheme.colorScheme.tertiary,
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$normaHoursToday ч.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                            color = MaterialTheme.colorScheme.tertiary,
                            gapSize = 4.dp,
                            drawStopIndicator = {},
                            progress = { percentNormaInDay },
                        )
                    }
                    if (isConsiderFutureRoute) {
                        Spacer(modifier = Modifier.height(7.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val currentTime = Calendar.getInstance()
                            val normaHoursToday = month.getNormaHoursInDate(currentTime.timeInMillis)
                            val percentTodayWorked = (todayWorkTime.toFloat() / (normaHoursToday * 3_600_000L).coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Отработано на ${dateAndTimeConverter?.getDate(currentTime.timeInMillis) ?: ""}",
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f),
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = convertTimeToString(todayWorkTime),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                trackColor = MaterialTheme.colorScheme.outlineVariant,
                                color = MaterialTheme.colorScheme.tertiary,
                                gapSize = 4.dp,
                                drawStopIndicator = {},
                                progress = { percentTodayWorked },
                            )
                        }
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
//                                color = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                            Text(
//                                text = "$normaHoursToday ч.",
//                                style = MaterialTheme.typography.bodyMedium,
//                                color = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                        }
//                        LinearProgressIndicator(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .height(4.dp),
//                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
//                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            defaultElevation = 1.dp,
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
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                        ) {
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
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = nightTimeText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp),
                                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                                            color = MaterialTheme.colorScheme.tertiary,
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
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = passengerTimeText,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            LinearProgressIndicator(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp),
                                                trackColor = MaterialTheme.colorScheme.outlineVariant,
                                            color = MaterialTheme.colorScheme.tertiary,
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
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = passengerTimeText,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            LinearProgressIndicator(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp),
                                                trackColor = MaterialTheme.colorScheme.outlineVariant,
                                            color = MaterialTheme.colorScheme.tertiary,
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
                                .padding(20.dp)
                                .fillMaxWidth(),
                        ) {
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
                                            (extendedServicePhaseTime.toFloat() / safeTotal.toFloat()).coerceIn(
                                                0f,
                                                1f
                                            )


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
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = extendedServicePhaseTimeText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Visible,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp),
                                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                                            color = MaterialTheme.colorScheme.tertiary,
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
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = longDistanceTrainsTimeText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp),
                                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                                            color = MaterialTheme.colorScheme.tertiary,
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
                                            (heavyTrainsTime.toFloat() / safeTotal.toFloat()).coerceIn(
                                                0f,
                                                1f
                                            )
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
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = heavyTrainsTimeText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp),
                                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                                            color = MaterialTheme.colorScheme.tertiary,
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
                                        val onePersonOperationTimeText =
                                            convertTimeToString(onePersonOperationTime)
                                        val percent =
                                            ((onePersonOperationTime * 100).toFloat() / (totalTimeWithHoliday).toFloat()) / 100f
                                        val percentOnePerson =
                                            (onePersonOperationTime.toFloat() / safeTotal.toFloat()).coerceIn(
                                                0f,
                                                1f
                                            )
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
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = onePersonOperationTimeText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp),
                                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                                            color = MaterialTheme.colorScheme.tertiary,
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
