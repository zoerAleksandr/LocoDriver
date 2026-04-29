package com.z_company.route.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.lifecycle.flowWithLifecycle
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.MonthFullText.getMonthFullText
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.route.R
import com.z_company.route.component.AnimationDialog
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.ChipApp
import com.z_company.route.component.ItemHomeScreen
import com.z_company.route.component.PdfActionSheet
import com.z_company.route.component.PdfContentDialog
import com.z_company.route.component.PreviewRouteDialog
import com.z_company.route.component.RadioButtonWithLabel
import com.z_company.route.util.toShareIntent
import com.z_company.route.viewmodel.PdfViewModel
import com.z_company.route.viewmodel.all_route_view_model.AllRouteViewModel
import com.z_company.route.viewmodel.all_route_view_model.RouteFilter
import com.z_company.route.viewmodel.all_route_view_model.SortOption
import com.z_company.route.viewmodel.home_view_model.AlertBeforePurchasesEvent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class, ExperimentalMaterialApi::class
)
@Composable
fun AllRouteScreen(
    viewModel: AllRouteViewModel,
    modifier: Modifier = Modifier,
    onRouteClick: (String) -> Unit = {},
    setSortOption: (SortOption) -> Unit,
    showFormScreen: () -> Unit,
    showPurchasesScreen: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {  // Новый: Подписка на shareRouteEvent.
        // Для чего: Когда ViewModel эмитирует ShareLinkData, мы строим Intent
        // через ShareLinkData.toShareIntent() и запускаем из Activity-контекста.
        viewModel.shareRouteEvent.collect { data ->
            context.startActivity(data.toShareIntent(fromActivity = true))
        }
    }

    val state by viewModel.uiState.collectAsState()
    val previewRouteState by viewModel.previewRouteUiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val redOrange = Color(0xFFf1642e)

    var isFilterSheetVisible by remember { mutableStateOf(false) }

    var isMonthSheetVisible by remember { mutableStateOf(false) }
    var isSortSheetVisible by remember { mutableStateOf(false) }
    var isShowAlertSubscribeDialog by remember { mutableStateOf(false) }
    var isShowNeedSubscribeDialog by remember { mutableStateOf(false) }
    var isShowDialogConfirmRemoveRoute by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // displayedRoutes sorted by selected sortOption
    val displayedRoutes = remember(state.filteredRoutes, state.sortOption) {
        val base = state.filteredRoutes
        when (state.sortOption) {
            SortOption.DATE_ASC -> base.sortedWith(compareBy {
                it.route.basicData.timeStartWork ?: Long.MAX_VALUE
            })

            SortOption.DATE_DESC -> base.sortedWith(compareByDescending {
                it.route.basicData.timeStartWork ?: Long.MIN_VALUE
            })

            SortOption.WORKTIME_ASC -> base.sortedWith(compareBy { it.route.getWorkTime() ?: 0L })
            SortOption.WORKTIME_DESC -> base.sortedWith(compareByDescending {
                it.route.getWorkTime() ?: 0L
            })
        }
    }

    // states for context actions (long click)
    var routeForPreview by remember { mutableStateOf<Route?>(null) }
    var routeForRemove by remember { mutableStateOf<Route?>(null) }
    var showContextDialog by remember { mutableStateOf(false) }

    val snackbarManager: ISnackbarManager = koinInject()
    val pdfViewModel: PdfViewModel = koinInject()

    // Cache routes in shared PdfViewModel so SalaryCalculationScreen can use them
    LaunchedEffect(displayedRoutes, state.currentMonthOfYear) {
        val monthLabel = state.currentMonthOfYear?.let {
            "${getMonthFullText(it.month)} ${it.year}"
        } ?: ""
        pdfViewModel.updateRoutes(displayedRoutes.map { it.route }, monthLabel)
    }
    var showPdfDialog by remember { mutableStateOf(false) }
    var pdfUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val isPdfGenerating by pdfViewModel.isGenerating.collectAsState()
    val pdfError by pdfViewModel.errorMessage.collectAsState()

    // PDF ready event
    LaunchedEffect(Unit) {
        pdfViewModel.pdfReady.collect { uri ->
            pdfUri = uri
        }
    }

    // Show PDF error in snackbar
    LaunchedEffect(pdfError) {
        pdfError?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(Unit) {
        snackbarManager.events
            .flowWithLifecycle(lifecycle)
            .collect { event ->
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

    // выбор диалога о рекомендации или необходимости подписки
    LaunchedEffect(Unit) {
        viewModel.alertBeforePurchasesEvent.flowWithLifecycle(lifecycle).collect { event ->
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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        // Month selection bottom sheet
        if (isMonthSheetVisible) {
            state.currentMonthOfYear?.let { monthOfYear ->
                ModalBottomSheet(
                    onDismissRequest = { isMonthSheetVisible = false },
                    sheetState = sheetState,
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
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        // month chips
                        val months by viewModel.monthList.collectAsState()
                        val years by viewModel.yearList.collectAsState()

                        var selectedMonth by remember { mutableIntStateOf(monthOfYear.month) }

                        var selectedYear by remember { mutableIntStateOf(monthOfYear.year) }
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // разобраться !!!
                            months.forEachIndexed { index, m ->
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
                            years.forEach { y ->
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
                                viewModel.setCurrentMonth(selectedYear to selectedMonth)
                                isMonthSheetVisible = false
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

        // Sort options bottom sheet
        if (isSortSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = { isSortSheetVisible = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Сортировка",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    RadioButtonWithLabel(
                        label = "Старые",
                        selected = state.sortOption == SortOption.DATE_ASC,
                        onSelect = {
                            setSortOption(SortOption.DATE_ASC); isSortSheetVisible = false
                        }
                    )
                    RadioButtonWithLabel(
                        label = "Новые",
                        selected = state.sortOption == SortOption.DATE_DESC,
                        onSelect = {
                            setSortOption(SortOption.DATE_DESC); isSortSheetVisible = false
                        }
                    )
                    RadioButtonWithLabel(
                        label = "Мало часов на работе",
                        selected = state.sortOption == SortOption.WORKTIME_ASC,
                        onSelect = {
                            setSortOption(SortOption.WORKTIME_ASC); isSortSheetVisible = false
                        }
                    )
                    RadioButtonWithLabel(
                        label = "Много часов на работе",
                        selected = state.sortOption == SortOption.WORKTIME_DESC,
                        onSelect = {
                            setSortOption(SortOption.WORKTIME_DESC); isSortSheetVisible = false
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // Subscribtion Alert
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
                    BottomSheetAction(text = "Оформить подписку за 69 руб/мес") {
                            showPurchasesScreen()
                    }
                ),
            )
        }

        // Subscription Dialog
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
                    BottomSheetAction(text = "Оформить подписку за 69 руб/мес") {
                        showPurchasesScreen()
                    },
                    BottomSheetAction(text = "Восстановить покупки") {
                        viewModel.restorePurchases()
                    }
                )
            )
        }

        if (isShowDialogConfirmRemoveRoute) {
            AppBottomSheet(
                onDismissRequest = { isShowDialogConfirmRemoveRoute = false },
                sheetState = sheetState,
                title = "Удалить маршрут?\n" +
                        "от ${viewModel.dateAndTimeConverter?.getDateAndTime(value = routeForRemove?.basicData?.timeStartWork) ?: ""} ",
                actions = listOf(
                    BottomSheetAction(text = "Да, удалить") {
                        routeForRemove?.let {
                            viewModel.deleteRoute(it)
                        }
                    }
                )
            )
        }

        // Контекстное меню при LongClick
        AnimationDialog(
            showDialog = showContextDialog,
            onDismissRequest = { showContextDialog = false }
        ) {
            routeForPreview?.let { route ->
                viewModel.calculationHomeRest(route)
                PreviewRouteDialog(
                    showContextDialog = {
                        showContextDialog = it
                    },
                    routeForPreview = route,
                    minTimeRest = viewModel.minTimeRest,
                    homeRest = previewRouteState.homeRest,
                    dateAndTimeConverter = viewModel.dateAndTimeConverter,
                    syncRoute = viewModel::syncRoute,
                    setFavoriteState = viewModel::setFavoriteRoute,
                    onRouteClick = onRouteClick,
                    makeCopyRoute = { viewModel.newRouteClick(route.basicData.id) },
                    showDialogConfirmRemove = { showDialog, route ->
                        isShowDialogConfirmRemoveRoute = showDialog
                        routeForRemove = route
                    },
                    shareRoute = { viewModel.shareRoute(it) },
                )
            }
        }

        // фильтры маршрутов
        if (isFilterSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = { isFilterSheetVisible = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Фильтры",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    FiltersRow(
                        selected = state.selectedFilters,
                        onToggle = { viewModel.toggleFilter(it) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: filter button
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = if (state.selectedFilters.contains(RouteFilter.ALL)) Color.Transparent else redOrange,
                            contentColor = if (state.selectedFilters.contains(RouteFilter.ALL)) Color.Transparent else Color.White
                        ) {
                            Text("${state.selectedFilters.size}")
                        }
                    }
                ) {
                    IconButton(
                        onClick = {
                            isFilterSheetVisible = !isFilterSheetVisible
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.filter_alt_24px),
                            contentDescription = null
                        )
                    }
                }

                // Middle: current month display & click to open month sheet
                val monthText =
                    state.currentMonthOfYear?.month?.let { getMonthFullText(it) } ?: ""

                TextButton(onClick = { isMonthSheetVisible = true }) {
                    Text(
                        text = monthText,
                        style = AppTypography.getType().headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right: pdf + expand + sort buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (!isPdfGenerating) showPdfDialog = true },
                        enabled = !isPdfGenerating
                    ) {
                        if (isPdfGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(8.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.picture_as_pdf_24px),
                                contentDescription = "PDF"
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            viewModel.toggleExpandedView()
                        }
                    ) {
                        AnimatedContent(targetState = state.isExpandedView) { isExpand ->
                            val icon =
                                if (isExpand) painterResource(R.drawable.collapse_content_24px) else painterResource(
                                    R.drawable.expand_content_24px
                                )
                            Icon(
                                painter = icon,
                                contentDescription = if (state.isExpandedView) "Свернуть" else "Развернуть"
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            isSortSheetVisible = true
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.sort_24px),
                            contentDescription = null
                        )
                    }
                }
            }

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Ошибка: ${state.errorMessage}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.reload() }) { Text("Повторить") }
                    }
                }

                state.filteredRoutes.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Список пуст")
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("all_route_lazy_column"),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        viewModel.dateAndTimeConverter?.let { converter ->
                            itemsIndexed(
                                items = displayedRoutes,
                                key = { _, it -> it.route.basicData.id }
                            ) { index, routeState ->
                                val route = routeState.route
                                val background = if (routeState.isFuture) {
                                    MaterialTheme.colorScheme.surfaceBright
                                } else if (routeState.isTransition) {
                                    MaterialTheme.colorScheme.surfaceDim
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                }
                                val routeId = route.basicData.id
                                val onClickMemo = remember(routeId) { { onRouteClick(routeId) } }
                                val onRequestDeleteMemo: (Route) -> Unit = remember(route) {
                                    { _ ->
                                        isShowDialogConfirmRemoveRoute = true
                                        routeForRemove = route
                                    }
                                }
                                val onLongClickMemo = remember(route) {
                                    {
                                        routeForPreview = route
                                        showContextDialog = true
                                    }
                                }

                                ItemHomeScreen(
                                    modifier = Modifier.animateItem(),
                                    route = route,
                                    convertTimeToString = viewModel::convertTimeToStringFormat,
                                    onClick = onClickMemo,
                                    onRequestDelete = onRequestDeleteMemo,
                                    isExpand = state.isExpandedView,
                                    onLongClick = onLongClickMemo,
                                    containerColor = background,
                                    dateAndTimeConverter = converter,
                                    isHeavyTrains = routeState.isHeavyTrains,
                                    isHolidayTimeInRoute = routeState.isHoliday,
                                    isLongCompositionTrain = routeState.isLongCompositionTrain,
                                    isExtendedServicePhaseTrains = routeState.isExtendedServicePhaseTrains,
                                    number = displayedRoutes.size - index
                                )

                                val showRestLine = if (index + 1 < displayedRoutes.size) {
                                    val r1 = route
                                    val r2 = displayedRoutes[index + 1].route
                                    val t1 = r1.basicData.timeStartWork ?: Long.MAX_VALUE
                                    val t2 = r2.basicData.timeStartWork ?: Long.MAX_VALUE
                                    if (t1 < t2) r1.basicData.restPointOfTurnover
                                    else r2.basicData.restPointOfTurnover
                                } else false

                                if (showRestLine) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(12.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .fillMaxHeight()
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .fillMaxHeight()
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // PDF dialog
        if (showPdfDialog) {
            PdfContentDialog(
                onDismiss = { showPdfDialog = false },
                onGenerate = { sections ->
                    showPdfDialog = false
                    val routes = displayedRoutes.map { it.route }
                    val monthLabel = state.currentMonthOfYear?.let {
                        "${getMonthFullText(it.month)} ${it.year}"
                    } ?: ""
                    pdfViewModel.generateAndShare(sections, routes, monthLabel, emptyList())
                }
            )
        }

        pdfUri?.let { uri ->
            PdfActionSheet(
                uri = uri,
                onDismiss = { pdfUri = null }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FiltersRow(selected: Set<RouteFilter>, onToggle: (RouteFilter) -> Unit) {
    val redOrange = Color(0xFFf1642e)

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChipApp(
            selected = selected.contains(RouteFilter.ALL),
            onClick = { onToggle(RouteFilter.ALL) },
            label = "Все"
        )

        ChipApp(
            selected = selected.contains(RouteFilter.FAVORITES),
            onClick = { onToggle(RouteFilter.FAVORITES) },
            leading = {
                Icon(
                    modifier = Modifier.size(20.dp),
                    tint = redOrange,
                    painter = painterResource(R.drawable.favorite_24px),
                    contentDescription = null
                )
            },
            label = "Избранные"
        )

        ChipApp(
            selected = selected.contains(RouteFilter.HEAVY),
            onClick = { onToggle(RouteFilter.HEAVY) },
            leading = {
                Icon(
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                    painter = painterResource(id = R.drawable.weight_24px),
                    contentDescription = null
                )
            },
            label = "Тяжелые"
        )

        ChipApp(
            selected = selected.contains(RouteFilter.EXTENDED_SERVICE),
            onClick = { onToggle(RouteFilter.EXTENDED_SERVICE) },
            leading = {
                Icon(
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                    painter = painterResource(id = R.drawable.long_distance_24px),
                    contentDescription = null
                )
            },
            label = "Удлинённые плечи"
        )

        ChipApp(
            selected = selected.contains(RouteFilter.FOLLOWING_RESERVE),
            onClick = { onToggle(RouteFilter.FOLLOWING_RESERVE) },
            leading = {
                Icon(
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                    painter = painterResource(R.drawable.icon_single_loco),
                    contentDescription = null
                )
            },
            label = "Резервом"
        )

        ChipApp(
            selected = selected.contains(RouteFilter.ONE_PERSON),
            onClick = { onToggle(RouteFilter.ONE_PERSON) },
            leading = {
                Icon(
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                    painter = painterResource(id = R.drawable.person_24px),
                    contentDescription = null
                )
            },
            label = "Одно лицо"
        )

        ChipApp(
            selected = selected.contains(RouteFilter.OVER_12_HOURS),
            onClick = { onToggle(RouteFilter.OVER_12_HOURS) },
            leading = {
                Image(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(id = R.drawable.orden),
                    contentDescription = null,
                )
            },
            label = "Свыше 12ч"
        )

        ChipApp(
            selected = selected.contains(RouteFilter.LONG_TRAINS),
            onClick = { onToggle(RouteFilter.LONG_TRAINS) },
            leading = {
                Image(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(id = R.drawable.icon_long),
                    contentDescription = null
                )
            },
            label = "Длинные поезда"
        )

        ChipApp(
            selected = selected.contains(RouteFilter.HAS_BREAK),
            onClick = { onToggle(RouteFilter.HAS_BREAK) },
            leading = {
                Icon(
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                    painter = painterResource(id = R.drawable.pause_24px),
                    contentDescription = null
                )
            },
            label = "С перерывами"
        )

        ChipApp(
            selected = selected.contains(RouteFilter.PUSHER),
            onClick = { onToggle(RouteFilter.PUSHER) },
            leading = {
                Icon(
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                    painter = painterResource(id = R.drawable.ic_pusher_24px),
                    contentDescription = null
                )
            },
            label = "Толкач"
        )

        ChipApp(
            selected = selected.contains(RouteFilter.DOUBLE_TRACTION),
            onClick = { onToggle(RouteFilter.DOUBLE_TRACTION) },
            leading = {
                Icon(
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                    painter = painterResource(id = R.drawable.ic_double_traction_24px),
                    contentDescription = null
                )
            },
            label = "Двойная тяга"
        )

        ChipApp(
            selected = selected.contains(RouteFilter.DOUBLED_TRAIN),
            onClick = { onToggle(RouteFilter.DOUBLED_TRAIN) },
            leading = {
                Icon(
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                    painter = painterResource(id = R.drawable.ic_doubled_train_24px),
                    contentDescription = null
                )
            },
            label = "Сдвоенный"
        )
    }
}
