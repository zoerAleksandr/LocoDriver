package com.z_company.route.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.flowWithLifecycle
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.MonthFullText.getMonthFullText
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTimeInMonth
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.route.R
import com.z_company.route.util.TurnaroundRest
import com.z_company.route.viewmodel.home_view_model.ItemState
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.ChipApp
import com.z_company.route.component.ItemHomeScreen
import com.z_company.route.component.PullToSyncContainer
import com.z_company.route.component.RadioButtonWithLabel
import com.z_company.route.component.RouteQuickViewSheet
import com.z_company.route.util.toShareIntent
import com.z_company.route.util.turnaroundRestBetween
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
    showPurchasesScreen: () -> Unit,
    onBack: () -> Unit = {},
    isPullRefreshing: Boolean = false,
    onPullRefresh: () -> Unit = {},
    pullSyncMessage: String? = null,
    onPullSyncMessageShown: () -> Unit = {},
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
    // Подтверждение массового удаления выбранных маршрутов (режим «Выбрать»).
    var isShowDialogConfirmRemoveSelected by remember { mutableStateOf(false) }
    var copyRouteId by remember { mutableStateOf<String?>(null) }

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

    // Строки списка: одиночная карточка или трей-пара «поездка с отдыхом в ПО».
    // Пара — два смежных маршрута туда→обратно, обёрнутые в общий контейнер.
    val allRouteRows = remember(
        displayedRoutes, state.currentMonthOfYear, viewModel.timeCalculationContext,
        state.showTurnaroundRest
    ) {
        buildAllRouteRows(
            routes = displayedRoutes,
            monthOfYear = state.currentMonthOfYear,
            ctx = viewModel.timeCalculationContext,
            groupTurnaroundRest = state.showTurnaroundRest,
        )
    }

    // states for context actions (long click)
    // Храним ItemState (а не только Route), чтобы в быстром просмотре показать
    // доплатные признаки (тяжеловесный / длинносоставный / удлинённое плечо),
    // которые вычисляются на уровне списка.
    var itemForPreview by remember { mutableStateOf<ItemState?>(null) }
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

    // Режим множественного выбора: тап по карточке выделяет маршрут, внизу
    // появляется панель действий (избранное / поделиться / удалить).
    val isSelectionMode = state.isSelectionMode
    val selectedIds = state.selectedRouteIds
    val visibleIds = remember(displayedRoutes) {
        displayedRoutes.map { it.route.basicData.id }.toSet()
    }
    val isAllSelected = visibleIds.isNotEmpty() && selectedIds.containsAll(visibleIds)
    // Системная «назад» в режиме выбора выходит из него, а не с экрана.
    BackHandler(enabled = isSelectionMode) { viewModel.exitSelectionMode() }

    // FAB «Добавить маршрут»: та же анимация появления/скрытия, что у нижней
    // контекстной панели в FormScreen — прячется при скролле списка вниз,
    // появляется при скролле вверх.
    var fabVisible by remember { mutableStateOf(true) }
    val fabNestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -3f) fabVisible = false
                else if (available.y > 3f) fabVisible = true
                return Offset.Zero
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    if (isSelectionMode) {
                        // Выделить все видимые маршруты / снять выделение со всех.
                        TextButton(onClick = { viewModel.toggleSelectAll() }) {
                            Text(
                                text = if (isAllSelected) "Снять" else "Все",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                            )
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Text("‹", style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                title = {
                    Text(
                        text = if (isSelectionMode) "Выбрано: ${selectedIds.size}"
                        else "Маршруты",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                actions = {
                    // Вход/выход из режима множественного выбора.
                    TextButton(
                        onClick = {
                            if (isSelectionMode) viewModel.exitSelectionMode()
                            else viewModel.enterSelectionMode()
                        }
                    ) {
                        Text(
                            text = if (isSelectionMode) "Готово" else "Выбрать",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        )
                    }
                },
                )
                if (state.isBackgroundSyncing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
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
                headerContent = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Удалить маршрут?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "от ${viewModel.dateAndTimeConverter?.getDateAndTime(value = routeForRemove?.basicData?.timeStartWork) ?: ""}",
                            fontFamily = com.z_company.core.ui.theme.MonoFont,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                },
                actions = listOf(
                    BottomSheetAction(text = "Да, удалить") {
                        routeForRemove?.let {
                            viewModel.deleteRoute(it)
                        }
                    }
                )
            )
        }

        // Подтверждение массового удаления выбранных маршрутов
        if (isShowDialogConfirmRemoveSelected) {
            val count = state.selectedRouteIds.size
            AppBottomSheet(
                onDismissRequest = { isShowDialogConfirmRemoveSelected = false },
                sheetState = sheetState,
                headerContent = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (count == 1) "Удалить маршрут?"
                            else "Удалить маршруты?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Выбрано: $count",
                            fontFamily = com.z_company.core.ui.theme.MonoFont,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                },
                actions = listOf(
                    BottomSheetAction(text = "Да, удалить") {
                        viewModel.deleteSelectedRoutes()
                    }
                )
            )
        }

        // Шторка подтверждения создания копии маршрута
        copyRouteId?.let { id ->
            AppBottomSheet(
                onDismissRequest = { copyRouteId = null },
                sheetState = sheetState,
                headerContent = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Создать копию маршрута?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "\nОткроется редактирование копии — исходный маршрут не изменится.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                actions = listOf(
                    BottomSheetAction(text = "Создать копию") { viewModel.newRouteClick(id) }
                )
            )
        }

        // Быстрый просмотр маршрута при LongClick (Android ModalBottomSheet).
        // Спецификация: route-quick-view-android.md + SCREEN_SPECS.md.
        if (showContextDialog) {
            itemForPreview?.let { item ->
                val route = item.route
                LaunchedEffect(route.basicData.id) {
                    viewModel.calculationHomeRest(route)
                    viewModel.calculationActualRest(route)
                }
                RouteQuickViewSheet(
                    route = route,
                    shiftPaymentText = state.routePayments[route.basicData.id],
                    isHeavyTrains = item.isHeavyTrains,
                    isLongCompositionTrain = item.isLongCompositionTrain,
                    isExtendedServicePhaseTrains = item.isExtendedServicePhaseTrains,
                    dateAndTimeConverter = viewModel.dateAndTimeConverter,
                    minTimeRest = viewModel.minTimeRest,
                    homeRest = previewRouteState.homeRest,
                    actualRestDuration = previewRouteState.actualRestDuration,
                    actualRestUntil = previewRouteState.actualRestUntil,
                    onDismiss = { showContextDialog = false },
                    onOpen = { id ->
                        showContextDialog = false
                        onRouteClick(id)
                    },
                    onToggleFavorite = { viewModel.setFavoriteRoute(it) },
                    onShare = {
                        // Закрываем шторку, чтобы snackbar с результатом/ошибкой
                        // не оказался под ней.
                        showContextDialog = false
                        viewModel.shareRoute(it)
                    },
                    onCopy = { id ->
                        showContextDialog = false
                        copyRouteId = id
                    },
                    onSync = {
                        // Закрываем шторку, чтобы snackbar (успех/ошибка/подписка/
                        // авторизация) не оказался под ней.
                        showContextDialog = false
                        viewModel.syncRoute(it)
                    },
                    onRequestDelete = { r ->
                        showContextDialog = false
                        routeForRemove = r
                        isShowDialogConfirmRemoveRoute = true
                    },
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

        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .nestedScroll(fabNestedScroll)
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Переключатель месяца
            val monthText =
                state.currentMonthOfYear?.month?.let { getMonthFullText(it) } ?: ""
            val yearText = state.currentMonthOfYear?.year?.toString() ?: ""

            // Стрелки листают доступные месяцы, тап по названию открывает шторку выбора.
            val monthYearList by viewModel.monthYearList.collectAsState()
            val currentMonthIndex = state.currentMonthOfYear?.let { cur ->
                monthYearList.indexOfFirst { it.first == cur.year && it.second == cur.month }
            } ?: -1
            val hasPrevMonth = currentMonthIndex > 0
            val hasNextMonth = currentMonthIndex in 0 until monthYearList.lastIndex

            // Слева — месяц и год со стрелками переключения, справа — счётчики
            // «маршрутов» и «отработано» за выбранный месяц.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.clickable { isMonthSheetVisible = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = monthText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = yearText,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = {
                            monthYearList.getOrNull(currentMonthIndex - 1)
                                ?.let { viewModel.setCurrentMonth(it) }
                        },
                        enabled = hasPrevMonth,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Text("‹", fontSize = 30.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (hasPrevMonth) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    }
                    IconButton(
                        onClick = {
                            monthYearList.getOrNull(currentMonthIndex + 1)
                                ?.let { viewModel.setCurrentMonth(it) }
                        },
                        enabled = hasNextMonth,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Text("›", fontSize = 30.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (hasNextMonth) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    }
                }

                // Счётчики: количество маршрутов и отработанное время за месяц (моно).
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StatPill(
                        // Без иконки — просто число маршрутов, чтобы не путать с
                        // соседней пилюлей отработанного времени.
                        iconRes = null,
                        text = "${state.routes.size}",
                        contentDescription = "Количество маршрутов",
                    )
                    if (state.monthWorkedTimeText.isNotBlank()) {
                        StatPill(
                            iconRes = R.drawable.schedule_24px,
                            text = state.monthWorkedTimeText,
                            contentDescription = "Отработано часов",
                        )
                    }
                }
            }

            // Один ряд: фильтр и сортировка слева, справа — переключатели
            // «отдых в ПО» и плотности карточек (раньше были в топбаре).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { isFilterSheetVisible = !isFilterSheetVisible },
                        label = { Text("Фильтр", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.filter_alt_24px),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                    )
                    AssistChip(
                        onClick = { isSortSheetVisible = !isSortSheetVisible },
                        label = {
                            Text(
                                when (state.sortOption) {
                                    SortOption.DATE_DESC -> "Дата ↓"
                                    SortOption.DATE_ASC -> "Дата ↑"
                                    SortOption.WORKTIME_DESC -> "Часы ↓"
                                    SortOption.WORKTIME_ASC -> "Часы ↑"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Переключатель показа объединения «отдых в ПО» (трей с коннектором).
                    // Активен (accent) — показываем; выключен (muted) — плоский список.
                    IconButton(
                        onClick = {
                            val shown = viewModel.toggleShowTurnaroundRest()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    if (shown) "Отдых в ПО показан"
                                    else "Отдых в ПО скрыт"
                                )
                            }
                        },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.hotel_24px),
                            contentDescription = if (state.showTurnaroundRest) {
                                "Скрыть отдыхи в ПО"
                            } else {
                                "Показать отдыхи в ПО"
                            },
                            // Та же пара цветов, что у соседнего переключателя вида:
                            // активен — surfaceContainerLow, выключен — primary.
                            tint = if (state.showTurnaroundRest) {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                    // Переключатель плотности карточек: свёрнуто ⇄ развёрнуто (для
                    // всех карточек сразу). Развёрнутая карточка показывает локомотивы,
                    // поезда, следование пассажиром и расчёт за смену.
                    IconButton(
                        onClick = { viewModel.toggleExpandedView() },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                if (state.isExpandedView) R.drawable.collapse_rows_24px
                                else R.drawable.expand_rows_24px
                            ),
                            contentDescription = if (state.isExpandedView) "Свернуть карточки"
                            else "Развернуть карточки",
                            tint = if (state.isExpandedView) MaterialTheme.colorScheme.surfaceContainerLow
                            else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            PullToSyncContainer(
                isRefreshing = isPullRefreshing,
                onRefresh = onPullRefresh,
                message = pullSyncMessage,
                onMessageShown = onPullSyncMessageShown,
                modifier = Modifier.weight(1f),
            ) {
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
                        // Горизонтальный отступ вынесен на сами элементы: одиночные
                        // карточки инсетятся на 8dp, а трей-пара выходит на всю ширину.
                        // Снизу оставляем место под панель действий режима выбора.
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = if (isSelectionMode) 88.dp else 8.dp,
                        )
                    ) {
                        viewModel.dateAndTimeConverter?.let { converter ->
                            items(
                                items = allRouteRows,
                                key = { it.key }
                            ) { row ->
                                val placementSpec = androidx.compose.animation.core.tween<
                                    androidx.compose.ui.unit.IntOffset>(
                                    durationMillis = 220,
                                    easing = androidx.compose.animation.core.FastOutSlowInEasing,
                                )
                                when (row) {
                                    is SingleRouteRow -> {
                                        val route = row.item.route
                                        val routeId = route.basicData.id
                                        RouteCardItem(
                                            modifier = Modifier
                                                .animateItem(placementSpec = placementSpec)
                                                .padding(horizontal = 8.dp),
                                            routeState = row.item,
                                            number = row.number,
                                            isExpand = state.isExpandedView,
                                            monthOfYear = state.currentMonthOfYear,
                                            timeCalculationContext = viewModel.timeCalculationContext,
                                            dateAndTimeConverter = converter,
                                            convertTimeToString = viewModel::convertTimeToStringFormat,
                                            shiftPaymentText = state.routePayments[routeId],
                                            selectionMode = isSelectionMode,
                                            isSelected = selectedIds.contains(routeId),
                                            onClick = {
                                                if (isSelectionMode) {
                                                    viewModel.toggleRouteSelection(routeId)
                                                } else {
                                                    onRouteClick(routeId)
                                                }
                                            },
                                            onRequestDelete = { r ->
                                                isShowDialogConfirmRemoveRoute = true
                                                routeForRemove = r
                                            },
                                            onLongClick = {
                                                itemForPreview = row.item
                                                showContextDialog = true
                                            },
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    is TripGroupRow -> {
                                        // Трей «Поездка с отдыхом»: два плеча в общем фоне,
                                        // шапка с суммой часов, между карточками — коннектор.
                                        TripGroupTray(
                                            modifier = Modifier.animateItem(placementSpec = placementSpec),
                                            totalWorkText = viewModel.convertTimeToStringFormat(row.totalWorkMs),
                                        ) {
                                            val upperRoute = row.upper.route
                                            val lowerRoute = row.lower.route
                                            RouteCardItem(
                                                routeState = row.upper,
                                                number = row.upperNumber,
                                                isExpand = state.isExpandedView,
                                                monthOfYear = state.currentMonthOfYear,
                                                timeCalculationContext = viewModel.timeCalculationContext,
                                                dateAndTimeConverter = converter,
                                                convertTimeToString = viewModel::convertTimeToStringFormat,
                                                shiftPaymentText = state.routePayments[upperRoute.basicData.id],
                                                selectionMode = isSelectionMode,
                                                isSelected = selectedIds.contains(upperRoute.basicData.id),
                                                onClick = {
                                                    if (isSelectionMode) {
                                                        viewModel.toggleRouteSelection(upperRoute.basicData.id)
                                                    } else {
                                                        onRouteClick(upperRoute.basicData.id)
                                                    }
                                                },
                                                onRequestDelete = { r ->
                                                    isShowDialogConfirmRemoveRoute = true
                                                    routeForRemove = r
                                                },
                                                onLongClick = {
                                                    itemForPreview = row.upper
                                                    showContextDialog = true
                                                },
                                            )
                                            TurnaroundRestConnector(
                                                station = row.rest.station,
                                                durationText = viewModel.convertTimeToStringFormat(
                                                    row.rest.durationMs
                                                ),
                                            )
                                            RouteCardItem(
                                                routeState = row.lower,
                                                number = row.lowerNumber,
                                                isExpand = state.isExpandedView,
                                                monthOfYear = state.currentMonthOfYear,
                                                timeCalculationContext = viewModel.timeCalculationContext,
                                                dateAndTimeConverter = converter,
                                                convertTimeToString = viewModel::convertTimeToStringFormat,
                                                shiftPaymentText = state.routePayments[lowerRoute.basicData.id],
                                                selectionMode = isSelectionMode,
                                                isSelected = selectedIds.contains(lowerRoute.basicData.id),
                                                onClick = {
                                                    if (isSelectionMode) {
                                                        viewModel.toggleRouteSelection(lowerRoute.basicData.id)
                                                    } else {
                                                        onRouteClick(lowerRoute.basicData.id)
                                                    }
                                                },
                                                onRequestDelete = { r ->
                                                    isShowDialogConfirmRemoveRoute = true
                                                    routeForRemove = r
                                                },
                                                onLongClick = {
                                                    itemForPreview = row.lower
                                                    showContextDialog = true
                                                },
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }

            // Нижняя панель действий над выбранными маршрутами (как в FormScreen).
            if (isSelectionMode) {
                SelectionBottomAppBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    isEnabled = selectedIds.isNotEmpty(),
                    isAllFavorite = selectedIds.isNotEmpty() && displayedRoutes
                        .filter { selectedIds.contains(it.route.basicData.id) }
                        .all { it.route.basicData.isFavorite },
                    onFavoriteClick = { viewModel.toggleFavoriteSelectedRoutes() },
                    onShareClick = { viewModel.shareSelectedRoutes() },
                    onDeleteClick = { isShowDialogConfirmRemoveSelected = true },
                )
            } else {
                // FAB «Добавить маршрут» — скрыт в режиме выбора (там место занято
                // панелью массовых действий). Оффсет считается по фактической высоте
                // обёртки (margin + сама кнопка), поэтому при скрытии уезжает
                // полностью за пределы экрана — тот же приём, что у FormBottomAppBar.
                var fabWrapperHeightPx by remember { mutableStateOf(0) }
                val fabOffsetY by androidx.compose.animation.core.animateIntAsState(
                    targetValue = if (fabVisible) 0 else fabWrapperHeightPx,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 600,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing,
                    ),
                    label = "fabOffset"
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .onSizeChanged { fabWrapperHeightPx = it.height }
                        .offset { androidx.compose.ui.unit.IntOffset(0, fabOffsetY) }
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    FloatingActionButton(
                        onClick = { viewModel.newRouteClick() },
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                    ) {
                        Icon(
                            painter = painterResource(com.z_company.core.R.drawable.ic_add),
                            contentDescription = "Добавить маршрут",
                        )
                    }
                }
            }
        }

    }
}


/**
 * Компактная «пилюля» со счётчиком в строке месяца: иконка + моно-значение.
 * Используется для количества маршрутов и отработанного времени за месяц.
 */
@Composable
private fun StatPill(
    iconRes: Int?,
    text: String,
    contentDescription: String? = null,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = com.z_company.core.ui.theme.MonoFont,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Нижняя панель действий над выбранными маршрутами — тот же вид, что и панель
 * маршрута в FormScreen: избранное и «поделиться» слева, удаление справа.
 */
@Composable
private fun SelectionBottomAppBar(
    isEnabled: Boolean,
    isAllFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val disabledTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onFavoriteClick, enabled = isEnabled) {
                Icon(
                    painter = painterResource(
                        if (isAllFavorite) R.drawable.favorite_fill_24px
                        else R.drawable.favorite_24px
                    ),
                    contentDescription = if (isAllFavorite) "Убрать из избранного"
                    else "В избранное",
                    tint = when {
                        !isEnabled -> disabledTint
                        isAllFavorite -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
            IconButton(onClick = onShareClick, enabled = isEnabled) {
                Icon(
                    painter = painterResource(R.drawable.share_24px),
                    contentDescription = "Поделиться",
                    tint = if (isEnabled) MaterialTheme.colorScheme.primary else disabledTint
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onDeleteClick, enabled = isEnabled) {
                Icon(
                    painter = painterResource(R.drawable.delete_24px),
                    contentDescription = "Удалить выбранные маршруты",
                    tint = if (isEnabled) MaterialTheme.colorScheme.error else disabledTint
                )
            }
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

// ─────────────────────────────────────────────────────────────
// Коннектор «Отдых в ПО» между двумя плечами внутри трея.
// Пилюля на surface с тенью (чтобы читалась на фоне accentSoft-трея): иконка-кровать
// + «Отдых в ПО · станция» + моно-длительность. Сверху и снизу — короткий пунктир.
// Презентационный, в подсчёты/выборки не входит.
// ─────────────────────────────────────────────────────────────
@Composable
private fun TurnaroundRestConnector(
    station: String,
    durationText: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Отрицательный отступ, чтобы «вжаться» в gap между карточками.
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DashedVerticalLine()
        Row(
            modifier = Modifier
                .padding(vertical = 2.dp)
                .shadow(3.dp, RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.hotel_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = if (station.isNotBlank()) "Отдых в ПО · $station" else "Отдых в ПО",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            if (durationText.isNotBlank()) {
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = com.z_company.core.ui.theme.MonoFont,
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                )
            }
        }
        DashedVerticalLine()
    }
}

// Короткий вертикальный пунктир (цвет — borderStrong = outline).
@Composable
private fun DashedVerticalLine() {
    val color = MaterialTheme.colorScheme.outline
    Canvas(
        modifier = Modifier
            .width(2.dp)
            .height(8.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, size.height),
            strokeWidth = size.width,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f),
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Строки списка «Все маршруты»: одиночная карточка или трей-пара «отдых в ПО».
// ─────────────────────────────────────────────────────────────
private sealed interface AllRouteRow {
    val key: String
}

private data class SingleRouteRow(
    val item: ItemState,
    val number: Int,
) : AllRouteRow {
    override val key: String get() = item.route.basicData.id
}

private data class TripGroupRow(
    val upper: ItemState,
    val upperNumber: Int,
    val lower: ItemState,
    val lowerNumber: Int,
    val rest: TurnaroundRest,
    val totalWorkMs: Long,
) : AllRouteRow {
    override val key: String get() = "trip_${upper.route.basicData.id}"
}

// Отработанное время плеча так же, как показывает карточка (учёт переходных месяцев).
private fun workTimeForCard(
    route: Route,
    monthOfYear: MonthOfYear?,
    ctx: TimeCalculationContext,
): Long = if (monthOfYear != null) {
    route.getWorkTimeInMonth(monthOfYear, ctx) ?: 0L
} else {
    route.getWorkTime() ?: 0L
}

// Собирает плоский список в строки: смежные пары туда→обратно (флаг «отдых в ПО»
// у раннего плеча) сворачиваются в один трей. Три плеча подряд связываются попарно.
private fun buildAllRouteRows(
    routes: List<ItemState>,
    monthOfYear: MonthOfYear?,
    ctx: TimeCalculationContext,
    groupTurnaroundRest: Boolean,
): List<AllRouteRow> {
    val rows = mutableListOf<AllRouteRow>()
    val total = routes.size
    var i = 0
    while (i < total) {
        val cur = routes[i]
        val curNumber = total - i
        val next = routes.getOrNull(i + 1)
        // Если пользователь отключил объединение — плоский список без треев.
        val rest = if (groupTurnaroundRest) {
            next?.let { turnaroundRestBetween(cur.route, it.route) }
        } else null
        if (rest != null && next != null) {
            rows.add(
                TripGroupRow(
                    upper = cur,
                    upperNumber = curNumber,
                    lower = next,
                    lowerNumber = total - (i + 1),
                    rest = rest,
                    totalWorkMs = workTimeForCard(cur.route, monthOfYear, ctx) +
                        workTimeForCard(next.route, monthOfYear, ctx),
                )
            )
            i += 2
        } else {
            rows.add(SingleRouteRow(item = cur, number = curNumber))
            i += 1
        }
    }
    return rows
}

// Одна карточка маршрута со всей обвязкой (фон по типу маршрута + колбэки).
@Composable
private fun RouteCardItem(
    routeState: ItemState,
    number: Int,
    isExpand: Boolean,
    monthOfYear: MonthOfYear?,
    timeCalculationContext: TimeCalculationContext,
    dateAndTimeConverter: DateAndTimeConverter,
    convertTimeToString: (Long?) -> String,
    shiftPaymentText: String?,
    onClick: () -> Unit,
    onRequestDelete: (Route) -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
) {
    val background = when {
        routeState.isFuture -> MaterialTheme.colorScheme.surfaceBright
        routeState.isTransition -> MaterialTheme.colorScheme.surfaceDim
        else -> MaterialTheme.colorScheme.secondary
    }
    ItemHomeScreen(
        modifier = modifier,
        route = routeState.route,
        convertTimeToString = convertTimeToString,
        onClick = onClick,
        onRequestDelete = onRequestDelete,
        isExpand = isExpand,
        onLongClick = onLongClick,
        containerColor = background,
        dateAndTimeConverter = dateAndTimeConverter,
        isHeavyTrains = routeState.isHeavyTrains,
        isHolidayTimeInRoute = routeState.isHoliday,
        isLongCompositionTrain = routeState.isLongCompositionTrain,
        isExtendedServicePhaseTrains = routeState.isExtendedServicePhaseTrains,
        number = number,
        monthOfYear = monthOfYear,
        timeCalculationContext = timeCalculationContext,
        shiftPaymentText = shiftPaymentText,
        selectionMode = selectionMode,
        isSelected = isSelected,
    )
}

// Трей пары «поездка с отдыхом в ПО»: общий фон accentSoft, шапка с итогом часов
// обоих плеч. Выходит на всю ширину (карточки внутри инсетятся на 8dp и сохраняют
// обычную ширину). Итог часов — единственная суммируемая величина, только отображение.
@Composable
private fun TripGroupTray(
    totalWorkText: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            // Нейтральный серый контейнер — не путать с голубым фоном переходного
            // маршрута (surfaceDim). Акцент «отдыха» остаётся в шапке и пилюле.
            .background(MaterialTheme.colorScheme.surfaceBright)
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.hotel_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Поездка с отдыхом · $totalWorkText".uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = com.z_company.core.ui.theme.MonoFont,
                    letterSpacing = 0.8.sp,
                ),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            )
        }
        content()
    }
}
