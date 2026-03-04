package com.z_company.shared.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
// BoxWithConstraints removed — crashes on iOS CMP with unbounded LazyRow/LazyColumn constraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.route.Route
import com.z_company.shared.ui.component.AnimatedCounter
import com.z_company.shared.ui.component.AppBottomSheet
import com.z_company.shared.ui.component.AsyncData
import com.z_company.shared.ui.component.AsyncDataValue
import com.z_company.shared.ui.component.BottomSheetAction
import com.z_company.shared.ui.component.ItemHomeScreen
import com.z_company.shared.ui.component.LinearPagerIndicator
import com.z_company.shared.util.ConverterLongToTime
import com.z_company.shared.util.MonthFullText
import com.z_company.shared.viewmodel.HomeSharedViewModel
import com.z_company.shared.viewmodel.HomeUiState
import com.z_company.shared.viewmodel.ItemState
import org.koin.compose.koinInject

private val CARD_SIZE = 120.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onRouteClick: (String) -> Unit,
    onNewRouteClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSalaryClick: () -> Unit,
    onAllRouteClick: () -> Unit,
    onWorkScheduleClick: () -> Unit,
    onSearchClick: () -> Unit,
    onMoreInfoClick: (String) -> Unit,
    onLocoClick: ((String) -> Unit)? = null,
    onTrainClick: ((String) -> Unit)? = null,
    onPassengerClick: ((String) -> Unit)? = null,
    onCopyRoute: ((String) -> Unit)? = null,
    onSyncRoute: ((Route) -> Unit)? = null,
) {
    val viewModel: HomeSharedViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    val previewUiState by viewModel.previewRouteUiState.collectAsState()
    val monthList by viewModel.monthList.collectAsState()
    val yearList by viewModel.yearList.collectAsState()
    val currentRoute by viewModel.currentRoute.collectAsState()
    val nextFutureRoute by viewModel.nextFutureRoute.collectAsState()
    val timeWithoutHoliday by viewModel.timeWithoutHoliday.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showMonthSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var routeForRemove by remember { mutableStateOf<Route?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var routeForPreview by remember { mutableStateOf<Route?>(null) }

    val currentMonthOfYear = (uiState.monthSelected as? ResultState.Success)?.data
    val monthLabel = currentMonthOfYear?.let { moy ->
        "${MonthFullText.getMonthFullText(moy.month)} ${moy.year}"
    } ?: "Загрузка\u2026"

    val brushMain = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.surfaceContainerLow,
            MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    )

    // Month picker
    if (showMonthSheet && currentMonthOfYear != null) {
        MonthPickerSheet(
            currentMonth = currentMonthOfYear,
            monthList = monthList,
            yearList = yearList,
            onSelect = { yearAndMonth ->
                viewModel.setCurrentMonth(yearAndMonth)
                showMonthSheet = false
            },
            onDismiss = { showMonthSheet = false },
        )
    }

    // Delete confirmation
    if (showDeleteDialog && routeForRemove != null) {
        AppBottomSheet(
            onDismissRequest = { showDeleteDialog = false },
            title = "Удалить маршрут?\nот ${uiState.dateAndTimeConverter?.getDateMiniAndTime(routeForRemove?.basicData?.timeStartWork) ?: ""}",
            actions = listOf(
                BottomSheetAction(text = "Да, удалить") {
                    routeForRemove?.let { viewModel.removeRoute(it) }
                    showDeleteDialog = false
                },
            ),
        )
    }

    // Context menu
    if (showContextMenu && routeForPreview != null) {
        ContextMenuDialog(
            route = routeForPreview!!,
            homeRest = previewUiState.homeRest,
            minTimeRest = uiState.minTimeRest,
            dateAndTimeConverter = uiState.dateAndTimeConverter,
            onDismiss = { showContextMenu = false },
            onRouteClick = { id -> showContextMenu = false; onRouteClick(id) },
            onSync = { route -> onSyncRoute?.invoke(route); showContextMenu = false },
            onFavorite = { route -> viewModel.setFavoriteRoute(route) },
            onCopy = { id -> onCopyRoute?.invoke(id); showContextMenu = false },
            onDelete = { route -> routeForRemove = route; showContextMenu = false; showDeleteDialog = true },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {},
                actions = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = { showMonthSheet = true },
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                            ) {
                                val textMonth = currentMonthOfYear?.month?.let { MonthFullText.getMonthFullText(it) } ?: "загрузка"
                                Text(
                                    text = "$textMonth ",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    overflow = TextOverflow.Visible,
                                    maxLines = 2,
                                )
                                Text(
                                    text = "${currentMonthOfYear?.year ?: ""}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        IconButton(onClick = onSearchClick) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val pagerState = rememberPagerState(pageCount = { 3 })
        AsyncData(uiState.routeListState) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                // Statistics pager
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                            when (page) {
                                0 -> MainInfoCard(timeWithoutHoliday, viewModel::convertTimeToStringFormat, uiState.totalTimeWithHoliday, currentMonthOfYear, brushMain)
                                1 -> DetailWorkTimeCard(timeWithoutHoliday, viewModel::convertTimeToStringFormat, brushMain, uiState.nightTimeInRouteList, uiState.passengerTimeInRouteList, uiState.singleLocomotiveTimeState)
                                2 -> DetailTrainCard(timeWithoutHoliday, viewModel::convertTimeToStringFormat, brushMain, uiState.extendedServicePhaseTime, uiState.heavyTrainsTime, uiState.onePersonOperationTime)
                            }
                        }
                        LinearPagerIndicator(state = pagerState)
                    }
                }

                // Current route
                currentRoute?.let { route ->
                    item {
                        CurrentRouteSection(route, viewModel, brushMain, onRouteClick, onLocoClick, onTrainClick, onPassengerClick)
                    }
                }

                // Next future route
                if (currentRoute == null && nextFutureRoute != null) {
                    item {
                        NextRouteSection(nextFutureRoute!!, viewModel, brushMain, uiState.dateAndTimeConverter, onRouteClick)
                    }
                }

                // Route list
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Маршруты", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            TextButton(onClick = onAllRouteClick) {
                                Text("Все", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val items = uiState.listItemState
                            uiState.dateAndTimeConverter?.let { converter ->
                                if (items.isNotEmpty()) {
                                    val first = items.first()
                                    ItemHomeScreen(
                                        route = first.route,
                                        convertTimeToString = viewModel::convertTimeToStringFormat,
                                        onRequestDelete = { routeForRemove = first.route; showDeleteDialog = true },
                                        onLongClick = { routeForPreview = first.route; showContextMenu = true },
                                        containerColor = routeBackground(first),
                                        onClick = { onRouteClick(first.route.basicData.id) },
                                        dateAndTimeConverter = converter,
                                        isHeavyTrains = first.isHeavyTrains,
                                        isExtendedServicePhaseTrains = first.isExtendedServicePhaseTrains,
                                        isHolidayTimeInRoute = first.isHoliday,
                                        number = items.size,
                                    )
                                    if (items.size > 1) {
                                        val second = items[1]
                                        ItemHomeScreen(
                                            route = second.route,
                                            convertTimeToString = viewModel::convertTimeToStringFormat,
                                            onRequestDelete = { routeForRemove = second.route; showDeleteDialog = true },
                                            onLongClick = { routeForPreview = second.route; showContextMenu = true },
                                            containerColor = routeBackground(second),
                                            onClick = { onRouteClick(second.route.basicData.id) },
                                            dateAndTimeConverter = converter,
                                            isHeavyTrains = second.isHeavyTrains,
                                            isExtendedServicePhaseTrains = second.isExtendedServicePhaseTrains,
                                            isHolidayTimeInRoute = second.isHoliday,
                                            number = items.size - 1,
                                        )
                                    }
                                } else {
                                    Text(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        text = "Список пуст\n\nНажмите  +  чтобы добавить маршрут\nили создайте график работы",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                }

                // Actions
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = "Действия",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        LazyRow(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item {
                                ActionCard(Modifier.padding(start = 12.dp), "График", Icons.Default.CalendarMonth, onWorkScheduleClick)
                            }
                            item {
                                ActionCard(Modifier.padding(end = 12.dp), "Отвлечения", Icons.Default.FlightTakeoff, onWorkScheduleClick)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // FAB
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.BottomEnd) {
            FloatingActionButton(modifier = Modifier.padding(16.dp), onClick = onNewRouteClick) {
                Icon(Icons.Default.Add, contentDescription = "Новый маршрут")
            }
        }
    }
}

// region Statistics Cards

@Composable
private fun MainInfoCard(
    totalTime: Long,
    convertTime: (Long?) -> String,
    totalTimeWithHoliday: ResultState<Long>?,
    currentMonthOfYear: MonthOfYear?,
    brush: Brush,
) {
    Card(
        modifier = Modifier.padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).background(brush)) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                AsyncDataValue(totalTimeWithHoliday) { time ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = convertTime(time), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                        if (totalTime != time && time != null) {
                            val diff = time - totalTime
                            Text(text = " (${convertTime(totalTime)} + ${convertTime(diff)})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                currentMonthOfYear?.let { month ->
                    val normaHours = month.getPersonalNormaHours()
                    val normaMs = (normaHours * 3_600_000L).coerceAtLeast(1)
                    val percent = (totalTime.toFloat() / normaMs.toFloat()).coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Норма на месяц", maxLines = 1, modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                            Text("$normaHours ч.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp), trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), color = MaterialTheme.colorScheme.secondary, progress = { percent })
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailWorkTimeCard(
    totalTime: Long,
    convertTime: (Long?) -> String,
    brush: Brush,
    nightTimeState: ResultState<Long>?,
    passengerTimeState: ResultState<Long>?,
    singleLocomotiveTimeState: ResultState<Long>?,
) {
    Card(
        modifier = Modifier.padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).background(brush)) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Время работы", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                StatProgressRow("Ночные часы", nightTimeState, totalTime, convertTime)
                StatProgressRow("Пассажирские", passengerTimeState, totalTime, convertTime)
                StatProgressRow("Одиночный локомотив", singleLocomotiveTimeState, totalTime, convertTime)
            }
        }
    }
}

@Composable
private fun DetailTrainCard(
    totalTime: Long,
    convertTime: (Long?) -> String,
    brush: Brush,
    extendedServicePhaseTime: ResultState<Long>?,
    heavyTrainsTime: ResultState<Long>?,
    onePersonOperationTime: ResultState<Long>?,
) {
    Card(
        modifier = Modifier.padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).background(brush)) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Типы поездов", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                StatProgressRow("Продлённая фаза", extendedServicePhaseTime, totalTime, convertTime)
                StatProgressRow("Тяжеловесные", heavyTrainsTime, totalTime, convertTime)
                StatProgressRow("Работа в одно лицо", onePersonOperationTime, totalTime, convertTime)
            }
        }
    }
}

@Composable
private fun StatProgressRow(label: String, resultState: ResultState<Long>?, totalMs: Long, convertTime: (Long?) -> String) {
    AsyncDataValue(resultState) { value ->
        val ms = value ?: 0L
        val total = totalMs.coerceAtLeast(1L)
        val progress = (ms.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        val pct = (progress * 100).toInt()
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Text("${convertTime(ms)} ($pct%)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.secondary)
            }
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp), trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), color = MaterialTheme.colorScheme.secondary, progress = { progress })
        }
    }
}

// endregion

// region Current/Next Route

@Composable
private fun CurrentRouteSection(
    route: Route,
    viewModel: HomeSharedViewModel,
    brush: Brush,
    onRouteClick: (String) -> Unit,
    onLocoClick: ((String) -> Unit)?,
    onTrainClick: ((String) -> Unit)?,
    onPassengerClick: ((String) -> Unit)?,
) {
    val workTime by remember { viewModel.workTimeInCurrentRoute }.collectAsState(initial = 0L)
    val workTimeText = ConverterLongToTime.getTimeInStringFormat(workTime)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp).clickable { onRouteClick(route.basicData.id) },
            text = "Текущий маршрут",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        LazyRow(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Work time card
            item {
                run {
                    val s = CARD_SIZE
                    Card(
                        modifier = Modifier.padding(start = 12.dp).defaultMinSize(minWidth = s, minHeight = s).clickable { onRouteClick(route.basicData.id) },
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    ) {
                        Box(Modifier.defaultMinSize(minWidth = s, minHeight = s).background(brush)) {
                            Column(Modifier.defaultMinSize(minWidth = s, minHeight = s).padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                AnimatedCounter(count = workTimeText, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                                Text("На работе", color = MaterialTheme.colorScheme.secondary, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            // Loco card
            item {
                run {
                    val s = CARD_SIZE
                    Card(modifier = Modifier.defaultMinSize(minWidth = s, minHeight = s), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)) {
                        Box(Modifier.defaultMinSize(minWidth = s, minHeight = s).background(MaterialTheme.colorScheme.secondary)) {
                            Column(Modifier.defaultMinSize(minWidth = s, minHeight = s).padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                if (route.locomotives.isEmpty()) {
                                    IconButton(modifier = Modifier.align(Alignment.End), colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, contentColor = MaterialTheme.colorScheme.secondary), onClick = { onLocoClick?.invoke(route.basicData.id) }) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                    }
                                } else {
                                    val loco = route.locomotives.last()
                                    Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        val text = buildString {
                                            if (!loco.series.isNullOrBlank()) append(loco.series)
                                            if (!loco.number.isNullOrBlank()) { if (isNotEmpty()) append(" "); append("№${loco.number}") }
                                            if (isEmpty()) append("Локо №1")
                                        }
                                        Text(text, color = MaterialTheme.colorScheme.primary, maxLines = 1, style = MaterialTheme.typography.bodyMedium, overflow = TextOverflow.Ellipsis)
                                        if (route.locomotives.size > 1) Text("... и ещё ${route.locomotives.size - 1}", color = MaterialTheme.colorScheme.primary, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                Text("Локомотив", color = MaterialTheme.colorScheme.primary, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            // Train card
            item {
                run {
                    val s = CARD_SIZE
                    Card(modifier = Modifier.defaultMinSize(minWidth = s, minHeight = s), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)) {
                        Box(Modifier.defaultMinSize(minWidth = s, minHeight = s).background(MaterialTheme.colorScheme.secondary)) {
                            Column(Modifier.defaultMinSize(minWidth = s, minHeight = s).padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                if (route.trains.isEmpty()) {
                                    IconButton(modifier = Modifier.align(Alignment.End), colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, contentColor = MaterialTheme.colorScheme.secondary), onClick = { onTrainClick?.invoke(route.basicData.id) }) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                    }
                                } else {
                                    val train = route.trains.last()
                                    Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("№ ${train.number ?: "---"}", color = MaterialTheme.colorScheme.primary, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                                        val first = train.stations.firstOrNull()?.stationName ?: ""
                                        val last = if (train.stations.size > 1) " - ${train.stations.last().stationName ?: ""}" else ""
                                        if ("$first$last".isNotBlank()) Text("$first$last", color = MaterialTheme.colorScheme.primary, maxLines = 1, style = MaterialTheme.typography.bodyMedium, overflow = TextOverflow.Ellipsis)
                                        if (route.trains.size > 1) Text("... и ещё ${route.trains.size - 1}", color = MaterialTheme.colorScheme.primary, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                Text("Поезд", color = MaterialTheme.colorScheme.primary, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            // Passenger card
            item {
                run {
                    val s = CARD_SIZE
                    Card(modifier = Modifier.defaultMinSize(minWidth = s, minHeight = s).padding(end = 12.dp), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)) {
                        Box(Modifier.defaultMinSize(minWidth = s, minHeight = s).background(MaterialTheme.colorScheme.secondary)) {
                            Column(Modifier.defaultMinSize(minWidth = s, minHeight = s).padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                if (route.passengers.isEmpty()) {
                                    IconButton(modifier = Modifier.align(Alignment.End), colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, contentColor = MaterialTheme.colorScheme.secondary), onClick = { onPassengerClick?.invoke(route.basicData.id) }) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                    }
                                } else {
                                    val p = route.passengers.last()
                                    Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        p.trainNumber?.let { Text("№ $it", color = MaterialTheme.colorScheme.primary, maxLines = 1, style = MaterialTheme.typography.bodyMedium) }
                                        Text("${p.stationDeparture ?: ""} ${p.stationArrival?.let { " - $it" } ?: ""}", color = MaterialTheme.colorScheme.primary, maxLines = 1, style = MaterialTheme.typography.bodyMedium, overflow = TextOverflow.Ellipsis)
                                        if (route.passengers.size > 1) Text("... и ещё ${route.passengers.size - 1}", color = MaterialTheme.colorScheme.primary, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                Text("Пассажиром", color = MaterialTheme.colorScheme.primary, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextRouteSection(
    route: Route,
    viewModel: HomeSharedViewModel,
    brush: Brush,
    dateAndTimeConverter: com.z_company.shared.util.DateAndTimeConverter?,
    onRouteClick: (String) -> Unit,
) {
    val countdown by remember { viewModel.countdownToNextRoute }.collectAsState(initial = 0L)
    val countdownText = ConverterLongToTime.getTimeInStringFormat(countdown)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(modifier = Modifier.padding(horizontal = 16.dp).clickable { onRouteClick(route.basicData.id) }, text = "Следующий маршрут", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        LazyRow(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                run {
                    val s = CARD_SIZE
                    Card(modifier = Modifier.padding(start = 12.dp).defaultMinSize(minWidth = s, minHeight = s).clickable { onRouteClick(route.basicData.id) }, elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)) {
                        Box(Modifier.defaultMinSize(minWidth = s, minHeight = s).background(brush)) {
                            Column(Modifier.defaultMinSize(minWidth = s, minHeight = s).padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                AnimatedCounter(count = countdownText, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                                Text("До явки", color = MaterialTheme.colorScheme.secondary, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            item {
                run {
                    val s = CARD_SIZE
                    Card(modifier = Modifier.defaultMinSize(minWidth = s, minHeight = s).padding(end = 12.dp), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)) {
                        Box(Modifier.defaultMinSize(minWidth = s, minHeight = s).background(MaterialTheme.colorScheme.secondary)) {
                            Column(Modifier.defaultMinSize(minWidth = s, minHeight = s).padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Text(dateAndTimeConverter?.getDateMiniAndTime(route.basicData.timeStartWork) ?: "", color = MaterialTheme.colorScheme.primary, maxLines = 1, style = MaterialTheme.typography.bodyMedium, overflow = TextOverflow.Ellipsis)
                                Text("Явка", color = MaterialTheme.colorScheme.primary, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

// endregion

// region Action Card

@Composable
private fun ActionCard(modifier: Modifier = Modifier, title: String, icon: ImageVector, onClick: () -> Unit) {
    run {
        val s = CARD_SIZE
        Card(
            modifier = modifier.defaultMinSize(minWidth = s, minHeight = s).clickable { onClick() },
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.primary),
        ) {
            Column(
                modifier = Modifier.defaultMinSize(minWidth = s, minHeight = s).padding(vertical = 8.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.weight(1f).size(48.dp).align(Alignment.CenterHorizontally))
                Text(title, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// endregion

// region Context Menu

@Composable
private fun ContextMenuDialog(
    route: Route,
    homeRest: Long?,
    minTimeRest: Long?,
    dateAndTimeConverter: com.z_company.shared.util.DateAndTimeConverter?,
    onDismiss: () -> Unit,
    onRouteClick: (String) -> Unit,
    onSync: (Route) -> Unit,
    onFavorite: (Route) -> Unit,
    onCopy: (String) -> Unit,
    onDelete: (Route) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Маршрут ${route.basicData.number ?: ""}", style = MaterialTheme.typography.titleMedium)
                dateAndTimeConverter?.let {
                    Text("${it.getDateMiniAndTime(route.basicData.timeStartWork)} - ${it.getDateMiniAndTime(route.basicData.timeEndWork)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                minTimeRest?.let { Text("Отдых в ПО: ${ConverterLongToTime.getTimeInStringFormat(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                homeRest?.let { Text("Домашний отдых: ${ConverterLongToTime.getTimeInStringFormat(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ContextMenuItem(Icons.Default.Sync, "Сохранить в облако") { onSync(route) }
                ContextMenuItem(if (route.basicData.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (route.basicData.isFavorite) "Убрать из избранного" else "В избранное") { onFavorite(route) }
                ContextMenuItem(Icons.Default.RemoveRedEye, "Открыть") { onRouteClick(route.basicData.id) }
                ContextMenuItem(Icons.Default.ContentCopy, "Копировать") { onCopy(route.basicData.id) }
                ContextMenuItem(Icons.Default.Delete, "Удалить") { onDelete(route) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

@Composable
private fun ContextMenuItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

// endregion

// region Helpers

@Composable
private fun routeBackground(item: ItemState): Color {
    return when {
        item.isHoliday -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        item.isFuture -> MaterialTheme.colorScheme.surfaceBright
        item.isTransition -> MaterialTheme.colorScheme.surfaceDim
        else -> MaterialTheme.colorScheme.secondary
    }
}

// endregion

// region Month Picker

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MonthPickerSheet(
    currentMonth: MonthOfYear,
    monthList: List<Int>,
    yearList: List<Int>,
    onSelect: (Pair<Int, Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedMonth by remember { mutableIntStateOf(currentMonth.month) }
    var selectedYear by remember { mutableIntStateOf(currentMonth.year) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.secondary,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Выберите месяц и год", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp))
            FlowRow(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                monthList.forEach { m ->
                    FilterChip(selected = selectedMonth == m, onClick = { selectedMonth = m }, label = { Text(MonthFullText.getMonthFullText(m)) })
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                yearList.forEach { y ->
                    FilterChip(selected = selectedYear == y, onClick = { selectedYear = y }, label = { Text("$y") })
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onSelect(selectedYear to selectedMonth) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Text("Применить", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// endregion
