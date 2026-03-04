package com.z_company.shared.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsRailway
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.route.Route
import com.z_company.shared.ui.component.AsyncData
import com.z_company.shared.ui.component.AsyncDataValue
import com.z_company.shared.util.ConverterLongToTime
import com.z_company.shared.util.MonthFullText
import com.z_company.shared.util.TimeFormatter
import com.z_company.shared.viewmodel.HomeSharedViewModel
import com.z_company.shared.viewmodel.HomeUiState
import com.z_company.shared.viewmodel.ItemState
import org.koin.compose.koinInject

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

    // Month label
    val currentMonthOfYear = (uiState.monthSelected as? ResultState.Success)?.data
    val monthLabel = currentMonthOfYear?.let { moy ->
        "${MonthFullText.getMonthFullText(moy.month)} ${moy.year}"
    } ?: "Загрузка\u2026"

    // Month picker bottom sheet
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

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                title = {
                    TextButton(onClick = { showMonthSheet = true }) {
                        Text(
                            text = monthLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Поиск",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewRouteClick) {
                Icon(Icons.Default.Add, contentDescription = "Новый маршрут")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        AsyncData(uiState.routeListState) { routes ->
            HomeContent(
                padding = padding,
                uiState = uiState,
                routes = routes ?: emptyList(),
                currentRoute = currentRoute,
                nextFutureRoute = nextFutureRoute,
                timeWithoutHoliday = timeWithoutHoliday,
                currentMonthOfYear = currentMonthOfYear,
                viewModel = viewModel,
                onRouteClick = onRouteClick,
                onNewRouteClick = onNewRouteClick,
                onSalaryClick = onSalaryClick,
                onAllRouteClick = onAllRouteClick,
                onWorkScheduleClick = onWorkScheduleClick,
                onMoreInfoClick = onMoreInfoClick,
            )
        }
    }
}

@Composable
private fun HomeContent(
    padding: PaddingValues,
    uiState: HomeUiState,
    routes: List<Route>,
    currentRoute: Route?,
    nextFutureRoute: Route?,
    timeWithoutHoliday: Long,
    currentMonthOfYear: MonthOfYear?,
    viewModel: HomeSharedViewModel,
    onRouteClick: (String) -> Unit,
    onNewRouteClick: () -> Unit,
    onSalaryClick: () -> Unit,
    onAllRouteClick: () -> Unit,
    onWorkScheduleClick: () -> Unit,
    onMoreInfoClick: (String) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { 3 })

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Info pager (3 pages)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                ) { page ->
                    when (page) {
                        0 -> MainInfoPage(
                            timeWithoutHoliday = timeWithoutHoliday,
                            totalTimeWithHoliday = uiState.totalTimeWithHoliday,
                            currentMonthOfYear = currentMonthOfYear,
                            convertTime = viewModel::convertTimeToStringFormat,
                        )

                        1 -> DetailWorkTimePage(
                            timeWithoutHoliday = timeWithoutHoliday,
                            totalTimeWithHoliday = uiState.totalTimeWithHoliday,
                            nightTimeState = uiState.nightTimeInRouteList,
                            passengerTimeState = uiState.passengerTimeInRouteList,
                            singleLocomotiveTimeState = uiState.singleLocomotiveTimeState,
                            convertTime = viewModel::convertTimeToStringFormat,
                        )

                        2 -> DetailTrainPage(
                            timeWithoutHoliday = timeWithoutHoliday,
                            totalTimeWithHoliday = uiState.totalTimeWithHoliday,
                            extendedServicePhaseTime = uiState.extendedServicePhaseTime,
                            heavyTrainsTime = uiState.heavyTrainsTime,
                            onePersonOperationTime = uiState.onePersonOperationTime,
                            convertTime = viewModel::convertTimeToStringFormat,
                        )
                    }
                }

                // Pager indicator
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .size(
                                    width = if (pagerState.currentPage == index) 24.dp else 8.dp,
                                    height = 8.dp,
                                )
                                .background(
                                    color = if (pagerState.currentPage == index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp),
                                ),
                        )
                    }
                }
            }
        }

        // Current route section
        currentRoute?.let { route ->
            item {
                CurrentRouteSection(
                    route = route,
                    viewModel = viewModel,
                    onRouteClick = onRouteClick,
                )
            }
        }

        // Next future route section
        if (currentRoute == null && nextFutureRoute != null) {
            item {
                NextRouteSection(
                    route = nextFutureRoute!!,
                    viewModel = viewModel,
                    onRouteClick = onRouteClick,
                )
            }
        }

        // Quick action row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = "Зарплата",
                    onClick = onSalaryClick,
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = "Все маршруты",
                    onClick = onAllRouteClick,
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = "График",
                    onClick = onWorkScheduleClick,
                )
            }
        }

        // Route list header
        if (routes.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Маршрутов: ${routes.size}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    currentMonthOfYear?.let { moy ->
                        TextButton(onClick = { onMoreInfoClick(moy.id) }) {
                            Text("Подробнее")
                        }
                    }
                }
            }
        }

        // Route list
        if (routes.isEmpty()) {
            item {
                EmptyRouteContent(onNewRouteClick = onNewRouteClick, monthLabel = monthLabel(currentMonthOfYear))
            }
        } else {
            items(uiState.listItemState, key = { it.route.basicData.id }) { item ->
                HomeRouteCard(
                    itemState = item,
                    dateAndTimeConverter = uiState.dateAndTimeConverter,
                    onRouteClick = { onRouteClick(item.route.basicData.id) },
                    onFavoriteClick = { viewModel.setFavoriteRoute(item.route) },
                )
            }
        }

        // Bottom spacer for FAB
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

// region Page 0 — MainInfo

@Composable
private fun MainInfoPage(
    timeWithoutHoliday: Long,
    totalTimeWithHoliday: ResultState<Long>?,
    currentMonthOfYear: MonthOfYear?,
    convertTime: (Long?) -> String,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Total work time (without holiday)
            Text(
                text = "Рабочее время",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = convertTime(timeWithoutHoliday),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            // Norm per month progress
            currentMonthOfYear?.let { month ->
                val personalNormaHours = month.getPersonalNormaHours()
                val personalNormaMs = personalNormaHours.toLong() * 3_600_000L

                if (personalNormaMs > 0) {
                    val progress = (timeWithoutHoliday.toFloat() / personalNormaMs).coerceIn(0f, 1.5f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Норма за месяц",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "${personalNormaHours}ч",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress.coerceAtMost(1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (progress > 1f) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }

            // Total with holiday
            AsyncDataValue(totalTimeWithHoliday) { totalWithHoliday ->
                totalWithHoliday?.let { total ->
                    val holidayPart = total - timeWithoutHoliday
                    if (holidayPart > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Праздничные",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                            Text(
                                text = convertTime(holidayPart),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                }
            }
        }
    }
}

// endregion

// region Page 1 — Detail Work Time

@Composable
private fun DetailWorkTimePage(
    timeWithoutHoliday: Long,
    totalTimeWithHoliday: ResultState<Long>?,
    nightTimeState: ResultState<Long>?,
    passengerTimeState: ResultState<Long>?,
    singleLocomotiveTimeState: ResultState<Long>?,
    convertTime: (Long?) -> String,
) {
    val totalTime = timeWithoutHoliday.takeIf { it > 0 } ?: 1L

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Время работы",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Night time
            TimeStatRow(
                label = "Ночные часы",
                resultState = nightTimeState,
                totalMs = totalTime,
                convertTime = convertTime,
            )

            // Passenger time
            TimeStatRow(
                label = "Пассажирские",
                resultState = passengerTimeState,
                totalMs = totalTime,
                convertTime = convertTime,
            )

            // Single locomotive (reserve)
            TimeStatRow(
                label = "Одиночный локомотив",
                resultState = singleLocomotiveTimeState,
                totalMs = totalTime,
                convertTime = convertTime,
            )
        }
    }
}

// endregion

// region Page 2 — Detail Train Types

@Composable
private fun DetailTrainPage(
    timeWithoutHoliday: Long,
    totalTimeWithHoliday: ResultState<Long>?,
    extendedServicePhaseTime: ResultState<Long>?,
    heavyTrainsTime: ResultState<Long>?,
    onePersonOperationTime: ResultState<Long>?,
    convertTime: (Long?) -> String,
) {
    val totalTime = timeWithoutHoliday.takeIf { it > 0 } ?: 1L

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Типы поездов",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Extended service phase trains
            TimeStatRow(
                label = "Продлённая фаза",
                resultState = extendedServicePhaseTime,
                totalMs = totalTime,
                convertTime = convertTime,
            )

            // Heavy trains
            TimeStatRow(
                label = "Тяжеловесные",
                resultState = heavyTrainsTime,
                totalMs = totalTime,
                convertTime = convertTime,
            )

            // One-person operation
            TimeStatRow(
                label = "Работа в одно лицо",
                resultState = onePersonOperationTime,
                totalMs = totalTime,
                convertTime = convertTime,
            )
        }
    }
}

// endregion

// region Current/Next route

@Composable
private fun CurrentRouteSection(
    route: Route,
    viewModel: HomeSharedViewModel,
    onRouteClick: (String) -> Unit,
) {
    val workTime by remember { viewModel.workTimeInCurrentRoute }.collectAsState(initial = 0L)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Текущий маршрут",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { onRouteClick(route.basicData.id) }
                .padding(bottom = 8.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                InfoCard(
                    title = ConverterLongToTime.getTimeInStringFormat(workTime),
                    subtitle = "На работе",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.secondary,
                    onClick = { onRouteClick(route.basicData.id) },
                )
            }
            item {
                val locoText = if (route.locomotives.isEmpty()) {
                    "Не указан"
                } else {
                    val loco = route.locomotives.last()
                    buildString {
                        if (!loco.series.isNullOrBlank()) append(loco.series)
                        if (!loco.number.isNullOrBlank()) {
                            if (isNotEmpty()) append(" ")
                            append("№${loco.number}")
                        }
                        if (isEmpty()) append("Локо №1")
                    }
                }
                InfoCard(
                    title = locoText,
                    subtitle = "Локомотив",
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.primary,
                )
            }
            item {
                val trainText = if (route.trains.isEmpty()) {
                    "Не указан"
                } else {
                    val train = route.trains.last()
                    "№ ${train.number ?: "---"}"
                }
                InfoCard(
                    title = trainText,
                    subtitle = "Поезд",
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun NextRouteSection(
    route: Route,
    viewModel: HomeSharedViewModel,
    onRouteClick: (String) -> Unit,
) {
    val countdown by remember { viewModel.countdownToNextRoute }.collectAsState(initial = 0L)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Следующий маршрут",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                InfoCard(
                    title = ConverterLongToTime.getTimeInStringFormat(countdown),
                    subtitle = "До явки",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = { onRouteClick(route.basicData.id) },
                )
            }
            item {
                val number = route.basicData.number?.takeIf { it.isNotBlank() } ?: "Без номера"
                InfoCard(
                    title = number,
                    subtitle = "Маршрут",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = { onRouteClick(route.basicData.id) },
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    BoxWithConstraints {
        val cardSize = (maxWidth / 3).coerceAtLeast(100.dp)
        Card(
            modifier = modifier
                .defaultMinSize(minWidth = cardSize, minHeight = cardSize),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            onClick = onClick ?: {},
        ) {
            Column(
                modifier = Modifier
                    .defaultMinSize(minWidth = cardSize, minHeight = cardSize)
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// endregion

// region Route cards

@Composable
private fun HomeRouteCard(
    itemState: ItemState,
    dateAndTimeConverter: com.z_company.shared.util.DateAndTimeConverter?,
    onRouteClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    val route = itemState.route
    val basicData = route.basicData

    val backgroundColor = when {
        itemState.isHoliday -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        itemState.isFuture -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        itemState.isTransition -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    val workDuration = run {
        val start = basicData.timeStartWork ?: return@run null
        val end = basicData.timeEndWork ?: return@run null
        if (end > start) end - start else null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        onClick = onRouteClick,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row: number + favorite + duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = basicData.number?.takeIf { it.isNotBlank() } ?: "Без номера",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )

                    // Badges for special types
                    if (itemState.isHoliday) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                        ) {
                            Text(
                                text = "ПР",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    }
                    if (itemState.isHeavyTrains) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                        ) {
                            Text(
                                text = "ТЖ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    workDuration?.let {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = TimeFormatter.formatDuration(it),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = if (basicData.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Избранное",
                            modifier = Modifier.size(18.dp),
                            tint = if (basicData.isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Time row
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dateAndTimeConverter?.let { converter ->
                    basicData.timeStartWork?.let { start ->
                        Text(
                            text = converter.getDateMiniAndTime(start),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    basicData.timeEndWork?.let { end ->
                        Text(
                            text = " \u2192 ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = converter.getDateMiniAndTime(end),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } ?: run {
                    basicData.timeStartWork?.let { start ->
                        Text(
                            text = TimeFormatter.formatDateTimeShort(start),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Counters: locomotives, trains, passengers
            val locoCount = route.locomotives.size
            val trainCount = route.trains.size
            val passCount = route.passengers.size
            if (locoCount > 0 || trainCount > 0 || passCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (locoCount > 0) {
                        CountBadge(icon = Icons.Default.Train, count = locoCount, label = "лок.")
                    }
                    if (trainCount > 0) {
                        CountBadge(
                            icon = Icons.Default.DirectionsRailway,
                            count = trainCount,
                            label = "поезд.",
                        )
                    }
                    if (passCount > 0) {
                        CountBadge(icon = Icons.Default.Person, count = passCount, label = "пасс.")
                    }
                }
            }

            // Notes preview
            basicData.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// endregion

// region Helper composables

@Composable
private fun TimeStatRow(
    label: String,
    resultState: ResultState<Long>?,
    totalMs: Long,
    convertTime: (Long?) -> String,
) {
    AsyncDataValue(resultState) { value ->
        val ms = value ?: 0L
        val progress = if (totalMs > 0) (ms.toFloat() / totalMs).coerceIn(0f, 1f) else 0f
        val pct = (progress * 100).toInt()

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${convertTime(ms)} ($pct%)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CountBadge(icon: ImageVector, count: Int, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "$count $label",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyRouteContent(onNewRouteClick: () -> Unit, monthLabel: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            Icons.Default.DirectionsRailway,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Text(
            text = "Нет маршрутов за $monthLabel",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onNewRouteClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Добавить маршрут")
        }
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Выберите месяц и год",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                monthList.forEach { m ->
                    FilterChip(
                        selected = selectedMonth == m,
                        onClick = { selectedMonth = m },
                        label = { Text(MonthFullText.getMonthFullText(m)) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                yearList.forEach { y ->
                    FilterChip(
                        selected = selectedYear == y,
                        onClick = { selectedYear = y },
                        label = { Text("$y") },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onSelect(selectedYear to selectedMonth) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Text(
                    text = "Применить",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// endregion

// region Utilities

private fun monthLabel(moy: MonthOfYear?): String {
    return moy?.let {
        "${MonthFullText.getMonthFullText(it.month)} ${it.year}"
    } ?: "Загрузка\u2026"
}

// endregion
