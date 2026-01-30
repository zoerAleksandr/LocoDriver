package com.z_company.route.ui

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.flowWithLifecycle
import com.z_company.core.ui.component.AutoSizeText
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.component.TimePickerApp
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.MonthFullText.getMonthFullText
import com.z_company.domain.entities.route.Route
import com.z_company.route.R
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.ChipApp
import com.z_company.route.viewmodel.WorkScheduleViewModel
import com.z_company.route.viewmodel.home_view_model.AlertBeforePurchasesEvent
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Calendar
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.plus
import com.z_company.domain.entities.ReleaseType

/**
 * WorkScheduleScreen - calendar view for creating schedules fast.
 *
 * Notes:
 * - The screen relies on WorkScheduleViewModel.prepareScreen() being called prior to showing
 *   (or you can call it inside LaunchedEffect(Unit) here).
 * - The ViewModel currently exposes flows for currentMonth, monthList/yearList, routesByDay, time buttons, active time and selectedDays.
 * - When Save is pressed the ViewModel triggers route creation & saving (but you must implement createRouteForStartTime in the ViewModel).
 */
@SuppressLint("StateFlowValueCalledInComposition", "MutableCollectionMutableState")
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun WorkScheduleScreen(
    viewModel: WorkScheduleViewModel,
    modifier: Modifier = Modifier,
    onReleaseDayScreenClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val green = Color(0xFFa3b565)
    val red = Color(0xFFD20119)

    // Ensure viewModel prepared
    LaunchedEffect(Unit) {
        viewModel.prepareScreen()
    }

    val currentMonth by viewModel.currentMonth.collectAsState()
    val dateAndTimeConverter by viewModel.dateAndTimeConverter.collectAsState()
    val monthList by viewModel.monthList.collectAsState()
    val yearList by viewModel.yearList.collectAsState()
    val routesByDay by viewModel.routesByDay.collectAsState()
    val timeButtonsState by viewModel.timeButtons.collectAsState()
    val activeTime by viewModel.activeTime.collectAsState()
    val selectedDaysMap by viewModel.selectedDays.collectAsState()
    val totalTimeWork by viewModel.totalTimeWork.collectAsState()
    val alert = viewModel.alertBeforePurchasesEvent

    val plannedMap by viewModel.selectedDays.collectAsState() // now Map<Int, List<Long>>

    val currentMonthFull by viewModel.currentMonthFull.collectAsState()
    val releasePeriods by viewModel.releasePeriods.collectAsState()
    val releaseByDay by viewModel.releaseByDay.collectAsState()
    val totalReleaseHours by viewModel.totalReleaseHours.collectAsState()

    // bottom sheet state for month picker
    val monthSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showMonthSheet by remember { mutableStateOf(false) }
    var showRemoveSheet by remember { mutableStateOf(false) }
    var showEndTimeSheet by remember { mutableStateOf(false) }
    var showDialogConfirmRemoveRoute by remember { mutableStateOf(false) }

    // bottom sheet for custom time input
    var showCustomTimeSheet by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycle = lifecycleOwner.lifecycle

    val snackbarManager: ISnackbarManager = koinInject()

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

    val isDeleteMode by viewModel.isDeleteMode.collectAsState()
    val selectedRoutesToDeleteMap by viewModel.selectedRoutesToDelete.collectAsState()
    val deleteDialogFlow = viewModel.deleteDialogRequests
    val totalSelectedRoutes = selectedRoutesToDeleteMap.values.sumOf { it.size }

    // local state для отображения диалога
    var showDeleteDialog by remember { mutableStateOf(false) }
    var dialogDay by remember { mutableStateOf(0) }
    var dialogRoutes by remember {
        mutableStateOf<List<Route>>(
            emptyList()
        )
    }
    var dialogSelectedIds by remember { mutableStateOf(setOf<String>()) }

    // слушаем поток запросов из VM
    LaunchedEffect(Unit) {
        deleteDialogFlow.collect { req ->
            dialogDay = req.day
            dialogRoutes = req.routes
            dialogSelectedIds = req.routes.map { it.basicData.id }
                .toSet() // по умолчанию выбрать все или пусто — ваш выбор
            showDeleteDialog = true
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    var isShowNeedSubscribeDialog by remember {
        mutableStateOf(false)
    }

    var isShowAlertSubscribeDialog by remember {
        mutableStateOf(false)
    }

    if (showDialogConfirmRemoveRoute) {
        AppBottomSheet(
            onDismissRequest = { showDialogConfirmRemoveRoute = false },
            sheetState = sheetState,
            title = "Удалить маршрут ($totalSelectedRoutes)",
            actions = listOf(
                BottomSheetAction(text = "Да, удалить") {
                    viewModel.deleteSelectedRoutes()
                }
            )
        )
    }

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
                    showEndTimeSheet = true
                },
                BottomSheetAction(text = "Оформить подписку за 69 руб/мес") {
                    viewModel.checkPurchasesAvailability()
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
                        .padding(horizontal = 12.dp)
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
                    viewModel.checkPurchasesAvailability()
                },
                BottomSheetAction(text = "Восстановить покупки") {
                    viewModel.restorePurchases()
                }
            )
        )
    }

    LaunchedEffect(Unit) {
        alert.collect { event ->
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

    var maxHeightCell by remember {
        mutableStateOf<Dp?>(null)
    }

    var headerHeightDp by remember {
        mutableStateOf<Dp?>(null)
    }

    val density = LocalDensity.current

    // Замер высоты DayCell
    Box(modifier = Modifier.alpha(0f)) {
        currentMonth?.let { month ->
            val cells = remember(month) {
                // формируем список ячеек (null = пустая)
                val cal = Calendar.getInstance().also {
                    it.set(Calendar.YEAR, month.year)
                    it.set(Calendar.MONTH, month.month)
                    it.set(Calendar.DAY_OF_MONTH, 1)
                }
                val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val leadingEmpty =
                    (firstDayOfWeek - Calendar.MONDAY).let { if (it >= 0) it else it + 7 } // 0..6
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val list = mutableListOf<Int?>()
                repeat(leadingEmpty) { list.add(null) }
                for (d in 1..daysInMonth) list.add(d)
                while (list.size % 7 != 0) list.add(null)
                list
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                item {
                    // Замер высоты header
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .alpha(0f)
                            .onGloballyPositioned { coordinates ->
                                headerHeightDp = with(density) { coordinates.size.height.toDp() }
                                Log.d("zzz", "headerHeightDp $headerHeightDp")
                            },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(text = "Пн")
                    }
                }



                items(cells) {
                    Column(
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                maxHeightCell = with(density) { coordinates.size.height.toDp() }
                            }
                            .wrapContentHeight()
                            .fillMaxWidth()
                            .padding(6.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // top row - day number
                        Text(
                            text = "1",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Column(modifier = Modifier.padding(top = 3.dp)) {
                            // First: planned time (показываем первым или выделяем)
                            AutoSizeText(
                                text = "12:00",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary, // выделяем плановый цветом
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                maxTextSize = 18.sp
                            )
                            AutoSizeText(
                                text = "+1",
                                style = MaterialTheme.typography.bodySmall,
                                maxTextSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        // Top: month title with picker
        var chipHeightPx by remember { mutableIntStateOf(44) }
        val chipHeightDp: Dp = with(density) { chipHeightPx.toDp() }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator() // Показ loading
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .padding(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            showMonthSheet = true
                                        }
                                    )
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = currentMonth?.let { getMonthFullText(it.month) } ?: "Месяц",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = currentMonth?.let { (it.year.toString()) } ?: "Год",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = { viewModel.toggleDeleteMode() }) {
                            val tint =
                                if (isDeleteMode) red else MaterialTheme.colorScheme.primary
                            Icon(
                                painter = painterResource(R.drawable.delete_24px),
                                contentDescription = null,
                                tint = tint
                            )
                        }
                    }
                }

                if (totalReleaseHours > 0L) {
                    val textWorkTime = ConverterLongToTime.getTimeInStringFormat(totalTimeWork)
                    val textReleaseTime =
                        ConverterLongToTime.getTimeInStringFormat(totalReleaseHours)

                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                style = MaterialTheme.typography.titleSmall,
                                text = "$textWorkTime - рабочие",
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                modifier = Modifier.noRippleEffect {
                                    onReleaseDayScreenClick()
                                },
                                style = MaterialTheme.typography.titleSmall,
                                text = "$textReleaseTime - отвлечения",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            style = MaterialTheme.typography.titleMedium,
                            text = ConverterLongToTime.getTimeInStringFormat(totalTimeWork),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        enter = fadeIn(animationSpec = tween(durationMillis = 100)) + slideInVertically(
                            animationSpec = tween(durationMillis = 100)
                        ),
                        exit = fadeOut(animationSpec = tween(durationMillis = 100)),
                        visible = isDeleteMode
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .animateItem()
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = Shapes.medium
                                )
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Режим удаления маршрутов",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(8.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(28.dp))
                }

                item {
                    currentMonth?.let { month ->
                        val isDeleteMode by viewModel.isDeleteMode.collectAsState()
                        val activeTime by viewModel.activeTime.collectAsState()
                        val snackbarManager: ISnackbarManager = koinInject()

                        val onDayClick: (Int) -> Unit = { day ->
                            if (isDeleteMode) {
                                // режим удаления: переадресуем в VM (там логика: если нет маршрутов -> snackbar, если 1 -> toggleRouteSelection, если >1 -> show dialog)
                                viewModel.onDayDeleteClicked(day)
                            } else {
                                // обычный режим: добавляем/удаляем плановую явку для дня с текущим активным временем
                                val time = activeTime
                                if (time == null) {
                                    // можно показывать snackbar через snackbarManager.events, но тут - прямой вызов show
                                    snackbarManager.show(message = "Выберите время явки")
                                } else {
                                    // toggle — если уже есть такой план в этот день, удалит одну копию; иначе добавит
                                    viewModel.togglePlannedTimeForDay(day, time)
                                }
                            }
                        }

                        val cells = remember(month) {
                            // формируем список ячеек (null = пустая)
                            val cal = Calendar.getInstance().also {
                                it.set(Calendar.YEAR, month.year)
                                it.set(Calendar.MONTH, month.month)
                                it.set(Calendar.DAY_OF_MONTH, 1)
                            }
                            val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                            val leadingEmpty = (firstDayOfWeek - Calendar.MONDAY).let { if (it >= 0) it else it + 7 } // 0..6
                            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            val list = mutableListOf<Int?>()
                            repeat(leadingEmpty) { list.add(null) }
                            for (d in 1..daysInMonth) list.add(d)
                            while (list.size % 7 != 0) list.add(null)
                            list
                        }

                        val rowsCount = (cells.size + 6) / 7

                        val verticalSpacingDp = 4.dp

                        // вычисляем высоту календаря только если unifiedCellHeightDp известен
                        val calendarHeightDp: Dp =
                            if (maxHeightCell != null && headerHeightDp != null) {
                                // rowsCount ячеек, плюс междустрочные spacing (rowsCount - 1) * verticalSpacing
                                headerHeightDp!! + (maxHeightCell!! * rowsCount.toFloat()) + (verticalSpacingDp * (rowsCount - 1)) + 24.dp
                            } else {
                                // запасной fallback: минимальная высота (одна строка)
                                24.dp + unifiedCellHeightDpOrDefault() // реализуйте fallback функцию
                            }

                        Box(
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .height(calendarHeightDp)
                        ) {
                            CalendarView(
                                cells = cells,
                                routesByDay = routesByDay,
                                plannedTimesByDay = plannedMap,
                                onDayClick = onDayClick,
                                daysToDelete = selectedRoutesToDeleteMap.keys, // set<Int> дней, которые имеют пометки
                                isDeleteMode = isDeleteMode,
                                timeConverter = dateAndTimeConverter,
                                maxHeightPx = maxHeightCell ?: 24.dp,
                                releaseByDay = releaseByDay
                            )
                        }
                    }

                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
                // Row of time buttons
                item {
                    val minChipHeight = 48.dp

                    AnimatedVisibility(
                        visible = !isDeleteMode,
                        enter = fadeIn(
                            animationSpec = tween(
                                delayMillis = 300,
                                durationMillis = 100
                            )
                        ) + slideInVertically(
                            animationSpec = tween(durationMillis = 100)
                        ),
                        exit = fadeOut(animationSpec = tween(durationMillis = 100))
                    ) {
                        FlowRow(
                            modifier = Modifier
                                .animateItem(),
                            horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(space = 6.dp),
                        ) {
                            timeButtonsState.forEach { time ->
                                val selected = time == activeTime
                                ChipApp(
                                    modifier = Modifier
                                        .onGloballyPositioned { coordinates ->
                                            chipHeightPx = coordinates.size.height
                                        },
                                    label = ConverterLongToTime.getTimeInStringFormat(time),
                                    selected = time == activeTime,
                                    onClick = {
                                        if (selected) viewModel.setActiveTime(null)
                                        else viewModel.setActiveTime(time)
                                    },
                                    onLongClick = {
                                        viewModel.setActiveTime(time)
                                        showRemoveSheet = true
                                    },
                                    selectedBackgroundColor = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            val density = LocalDensity.current
                            val chipHeightDp = with(density) { chipHeightPx.toDp() }

                            Box(
                                modifier = Modifier
                                    .height(chipHeightDp.coerceAtLeast(40.dp)) // 40.dp — минимальная высота чипа, подгоните под ваш дизайн
                                    .noRippleEffect { showCustomTimeSheet = true }
                                    .padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    modifier = Modifier.clickable {
                                        showCustomTimeSheet = true
                                    },
                                    text = "Добавить время",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
                // Save button row
                item {
                    AnimatedVisibility(
                        visible = selectedDaysMap.isNotEmpty(),
                        enter = fadeIn(animationSpec = tween(durationMillis = 100)) + slideInVertically(
                            animationSpec = tween(durationMillis = 100)
                        ),
                        exit = fadeOut(animationSpec = tween(durationMillis = 100))
                    ) {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(chipHeightDp),
                            shape = Shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.secondary
                            ),
                            onClick = {
                                showEndTimeSheet = true
                            },
                        ) {
                            Text("Сохранить маршруты", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    AnimatedVisibility(
                        visible = selectedDaysMap.isNotEmpty(),
                        enter = fadeIn(animationSpec = tween(durationMillis = 100)) + slideInVertically(
                            animationSpec = tween(durationMillis = 100)
                        ),
                        exit = fadeOut(animationSpec = tween(durationMillis = 100))
                    ) {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(chipHeightDp),
                            shape = Shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                contentColor = MaterialTheme.colorScheme.secondary,
                                containerColor = red
                            ),
                            onClick = {
                                viewModel.resetSelectedDays()
                            },
                        ) {
                            Text(
                                "Сбросить",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = isDeleteMode && totalSelectedRoutes > 0,
                        enter = fadeIn(
                            animationSpec = tween(
                                delayMillis = 300,
                                durationMillis = 100
                            )
                        ) + slideInVertically(
                            animationSpec = tween(durationMillis = 200)
                        ),
                        exit = fadeOut(animationSpec = tween(durationMillis = 100))
                    ) {
                        Button(
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .height(chipHeightDp),
                            shape = Shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = red,
                                contentColor = MaterialTheme.colorScheme.secondary
                            ),
                            onClick = { showDialogConfirmRemoveRoute = true },
                        ) {
                            Text(
                                text = "Удалить выбранные ($totalSelectedRoutes)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isDeleteMode && totalSelectedRoutes == 0,
                        enter = fadeIn(
                            animationSpec = tween(
                                delayMillis = 300,
                                durationMillis = 100
                            )
                        ) + slideInVertically(
                            animationSpec = tween(durationMillis = 100)
                        ),
                        exit = fadeOut(animationSpec = tween(durationMillis = 100))
                    ) {
                        Box(
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Выбери маршруты для удаления", modifier = Modifier
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        ModalBottomSheet(
            onDismissRequest = { showDeleteDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Выберите маршруты для удаления",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))

                var selectedIds by remember { mutableStateOf(dialogSelectedIds.toSet()) }
                dialogRoutes.forEach { route ->
                    val id = route.basicData.id
                    val label =
                        dateAndTimeConverter?.getDateAndTime(route.basicData.timeStartWork) ?: ""
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // toggling: создаём новое множество при изменении
                                selectedIds = if (selectedIds.contains(id)) {
                                    selectedIds - id
                                } else {
                                    selectedIds + id
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedIds.contains(id),
                            onCheckedChange = { checked ->
                                selectedIds = if (checked) selectedIds + id else selectedIds - id
                            }
                        )
                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = label,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        shape = Shapes.medium,
                        onClick = {
                            viewModel.setSelectedRoutesForDay(dialogDay, selectedIds.toSet())
                            showDeleteDialog = false
                        }) {
                        Text(
                            text = "Удалить",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    OutlinedButton(shape = Shapes.medium, onClick = { showDeleteDialog = false }) {
                        Text(
                            text = "Отмена",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    if (showRemoveSheet && activeTime != null) {
        AppBottomSheet(
            onDismissRequest = { showRemoveSheet = false },
            sheetState = monthSheetState,
            title = "Удалить время ${ConverterLongToTime.getTimeInStringFormat(activeTime)}",
            actions = listOf(
                BottomSheetAction(text = "Да, удалить") {
                    viewModel.removeCustomTime(activeTime!!)
                    showRemoveSheet = false
                }
            )
        )
    }

    // Month selection Sheet
    if (showMonthSheet) {
        currentMonth?.let { current ->
            ModalBottomSheet(
                onDismissRequest = { showMonthSheet = false },
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
                        style = MaterialTheme.typography.titleSmall,
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
                            viewModel.setCurrentMonth(selectedYear to selectedMonth)
                            showMonthSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            contentColor = MaterialTheme.colorScheme.secondary,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium
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

    // Custom time sheet
    if (showCustomTimeSheet) {
        TimePickerApp(
            onTimeSelected = { millis ->
                viewModel.addCustomTime(millis)
                showCustomTimeSheet = false
            },
            onDismiss = { showCustomTimeSheet = false },
            title = "Время явки"
        )
    }

    if (showEndTimeSheet) {
        TimePickerApp(
            onTimeSelected = { millis ->
                scope.launch {
                    viewModel.newRouteClick(millis)
                }
                showEndTimeSheet = false
            },
            onDismiss = { showEndTimeSheet = false },
            initialTimeMillis = 3_600_000 * 12,
            title = "Продолжительность работы",
            onCancelButton = {
                scope.launch {
                    viewModel.newRouteClick()
                }
                showEndTimeSheet = false
            },
        )
    }
}

/**
 * Simple calendar grid showing days of the given month.
 *
 * - routesByDay: map day-of-month -> list of routes starting that day
 * - selectedDays: days chosen in create mode (will show the active time on the cell)
 * - onDayClick: called when user clicks a cell
 */

@Composable
private fun unifiedCellHeightDpOrDefault(): Dp {
    // небольшая функция для возврата запасной высоты до тех пор, пока unifiedCellHeight не измерен.
    // Можно взять 44.dp или вычислить по теме
    return 44.dp
}

@OptIn(FlowPreview::class)
@Composable
private fun CalendarView(
    releaseByDay: Map<Int, Pair<ReleaseType?, Int>>,
    cells: MutableList<Int?>,
    routesByDay: Map<Int, List<Route>>,
    plannedTimesByDay: Map<Int, List<Long>>,
    onDayClick: (Int) -> Unit,
    daysToDelete: Set<Int>,
    isDeleteMode: Boolean,
    timeConverter: DateAndTimeConverter?,
    maxHeightPx: Dp
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val dayOfMonth = listOf<String>(
            "Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"
        )

        items(dayOfMonth) { day ->
            Box(
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
        }
        items(cells) { day ->
            if (day == null) {
                Box(
                    modifier = Modifier
                        .height(maxHeightPx)
                )
            } else {
                val routeTimes =
                    routesByDay[day]?.mapNotNull { it.basicData.timeStartWork }?.sorted()
                        ?: emptyList()
                val planned = plannedTimesByDay[day] ?: emptyList()
                DayCell(
                    day = day,
                    routeTimes = routeTimes,
                    plannedTime = planned,
                    onClick = { onDayClick(day) },
                    timeConverter = timeConverter,
                    isDeleteMode = isDeleteMode,
                    isMarkedForDelete = daysToDelete.contains(day),
                    height = maxHeightPx,
                    releaseByDay = releaseByDay
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    routeTimes: List<Long>,
    plannedTime: List<Long>,
    onClick: () -> Unit,
    timeConverter: DateAndTimeConverter?,
    isDeleteMode: Boolean,
    isMarkedForDelete: Boolean,
    height: Dp,
    releaseByDay: Map<Int, Pair<ReleaseType?, Int>>,
) {
    val redColor = MaterialTheme.colorScheme.error

    val typeColors = mapOf(
        ReleaseType.Vacation to Color.Green,
        ReleaseType.SickLeave to Color.Red,
        ReleaseType.Donor to Color.Blue,
        ReleaseType.Courses to Color.Yellow,
        ReleaseType.ChildCare to Color.Magenta,
        ReleaseType.Other to Color.Gray
    )

    val bgColor = if (isDeleteMode && isMarkedForDelete) redColor
//    else if (releaseByDay.containsKey(day)) typeColors[releaseByDay[day]?.first]
//        ?: MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
    else MaterialTheme.colorScheme.secondary

    val borderColor =
        if (isDeleteMode && isMarkedForDelete) redColor.copy(alpha = 0.4f)
//        else if (releaseByDay.containsKey(day)) typeColors[releaseByDay[day]?.first]
//            ?: MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
        else MaterialTheme.colorScheme.secondary

    val textColor = if (isDeleteMode && isMarkedForDelete)
        MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)

    val releaseInfo = releaseByDay[day]
    val releaseHours = releaseInfo?.second ?: 0
    val releaseTexts = if (releaseHours > 0) listOf("$releaseHours") else emptyList()
    val releaseType = releaseInfo?.first  // Для цвета полоски
    val indicatorColor = typeColors[releaseType] ?: Color.Transparent
    Card(
        modifier = Modifier
            .defaultMinSize(
                minHeight = height
            )
            .clickable { onClick() },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        border = BorderStroke(width = 1.dp, color = borderColor),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(6.dp)
    ) {
        Box(modifier = Modifier.height(height)) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // top row - day number
                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )

                val plannedTexts = plannedTime
                    .map { ConverterLongToTime.getTimeInStringFormat(it) }

                val routeTexts = routeTimes
                    .map {
                        timeConverter?.getTime(it) ?: ConverterLongToTime.getTimeInStringFormat(it)
                    }

                val combined: List<Pair<String, Int>> =
                    plannedTexts.map { it to 1 } + routeTexts.map { it to 0 } + releaseTexts.map { it to 2 }

                val (toShow: List<Pair<String, Int>>, remainingCount: Int) = when {
                    combined.isEmpty() -> Pair(emptyList(), 0)

                    combined.size < 2 -> {
                        Pair(combined, 0)
                    }

                    combined.size == 2 -> {
                        Pair(listOf(combined[0], combined[1]), 0)
                    }

                    else -> {
                        val firstPlannedIndex = combined.indexOfFirst { it.second == 1 }
                        val firstItem =
                            if (firstPlannedIndex >= 0) combined[firstPlannedIndex] else combined[0]
                        Pair(listOf(firstItem), combined.size - 1)
                    }
                }

                Column(modifier = Modifier.padding(top = 3.dp)) {
                    toShow.forEach { (label, type) ->
                        AutoSizeText(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = when (type) {
                                1 -> MaterialTheme.colorScheme.tertiary
                                2 -> redColor
                                else -> textColor
                            },
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            maxTextSize = 18.sp
                        )
                    }

                    // If nothing to show (shouldn't happen) keep spacing; else show remaining if >0
                    if (toShow.isEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (remainingCount > 0) {
                        AutoSizeText(
                            text = "+$remainingCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor,
                            maxTextSize = 12.sp
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .align(Alignment.BottomCenter)
                    .background(indicatorColor)
            )
        }
    }
}

//@Composable
//private fun DayCell(
//    day: Int,
//    routeTimes: List<Long>,
//    plannedTime: List<Long>,
//    onClick: () -> Unit,
//    timeConverter: DateAndTimeConverter?,
//    isDeleteMode: Boolean,
//    isMarkedForDelete: Boolean,
//    height: Dp,
//    releaseByDay: Map<Int, Pair<ReleaseType?, Int>>,
//) {
//    val redColor = Color(0xFFD20119)
//    val bgColor =
//        if (isDeleteMode && isMarkedForDelete) redColor else MaterialTheme.colorScheme.secondary
//    val borderColor =
//        if (isDeleteMode && isMarkedForDelete) redColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.secondary
//
//    val textColor = if (isDeleteMode && isMarkedForDelete)
//        MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
//
//    Card(
//        modifier = Modifier
//            .defaultMinSize(
//                minHeight = height
//            )
//            .clickable { onClick() },
//        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
//        border = BorderStroke(width = 1.dp, color = borderColor),
//        colors = CardDefaults.cardColors(containerColor = bgColor),
//        shape = RoundedCornerShape(6.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .wrapContentHeight()
//                .fillMaxWidth()
//                .padding(6.dp),
//            verticalArrangement = Arrangement.SpaceBetween
//        ) {
//            // top row - day number
//            Text(
//                text = day.toString(),
//                style = MaterialTheme.typography.bodyMedium,
//                color = textColor
//            )
//
//            val plannedTexts = plannedTime
//                .map { ConverterLongToTime.getTimeInStringFormat(it) }
//
//            val routeTexts = routeTimes
//                .map {
//                    timeConverter?.getTime(it) ?: ConverterLongToTime.getTimeInStringFormat(it)
//                }
//
//            val combined: List<Pair<String, Boolean>> =
//                plannedTexts.map { it to true } + routeTexts.map { it to false }
//
//            val (toShow: List<Pair<String, Boolean>>, remainingCount: Int) = when {
//                combined.isEmpty() -> Pair(emptyList(), 0)
//
//                combined.size < 2 -> {
//                    Pair(combined, 0)
//                }
//
//                combined.size == 2 -> {
//                    Pair(listOf(combined[0], combined[1]), 0)
//                }
//
//                else -> {
//                    val firstPlannedIndex = combined.indexOfFirst { it.second }
//                    val firstItem =
//                        if (firstPlannedIndex >= 0) combined[firstPlannedIndex] else combined[0]
//                    Pair(listOf(firstItem), combined.size - 1)
//                }
//            }
//
//            Column(modifier = Modifier.padding(top = 3.dp)) {
//                toShow.forEach { (label, isPlanned) ->
//                    AutoSizeText(
//                        text = label,
//                        style = MaterialTheme.typography.bodySmall,
//                        color = if (isPlanned) MaterialTheme.colorScheme.tertiary else textColor,
//                        fontWeight = FontWeight.Medium,
//                        maxLines = 1,
//                        maxTextSize = 18.sp
//                    )
//                }
//
//                // If nothing to show (shouldn't happen) keep spacing; else show remaining if >0
//                if (toShow.isEmpty()) {
//                    Spacer(modifier = Modifier.height(12.dp))
//                }
//
//                if (remainingCount > 0) {
//                    AutoSizeText(
//                        text = "+$remainingCount",
//                        style = MaterialTheme.typography.bodySmall,
//                        color = textColor,
//                        maxTextSize = 12.sp
//                    )
//                }
//            }
//        }
//    }
//}