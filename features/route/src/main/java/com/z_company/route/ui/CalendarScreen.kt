package com.z_company.route.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Route
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.ItemHomeScreen
import com.z_company.route.component.RouteQuickViewSheet
import com.z_company.route.component.SwipeToRevealDelete
import com.z_company.domain.entities.ReleaseType
import com.z_company.route.R
import com.z_company.route.viewmodel.AbsenceInfo
import com.z_company.route.viewmodel.CalendarUiState
import com.z_company.route.viewmodel.PreviewRouteUiState
import com.z_company.route.viewmodel.RoutePlanState
import com.z_company.route.viewmodel.TripInfo
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin

// ── Семантические цвета событий (по дизайну, не зависят от палитры) ──
private val VacationColor = Color(0xFF34C759)
private val SickColor = Color(0xFFFF9F0A)
private val CoursesColor = Color(0xFFAF52DE)
private val DonorColor = Color(0xFFFF3B30)
private val ChildCareColor = Color(0xFF00A0F5)
private val OtherColor = Color(0xFF8A8278)
private val DayOffColor = Color(0xFFF4433C)
private val BusinessTripColor = Color(0xFF30B0C7)

private fun releaseColor(type: ReleaseType): Color = when (type) {
    ReleaseType.Vacation -> VacationColor
    ReleaseType.SickLeave -> SickColor
    ReleaseType.Courses -> CoursesColor
    ReleaseType.Donor -> DonorColor
    ReleaseType.ChildCare -> ChildCareColor
    ReleaseType.DayOff -> DayOffColor
    ReleaseType.BusinessTrip -> BusinessTripColor
    ReleaseType.Other -> OtherColor
}

private fun absBadge(type: ReleaseType): String = when (type) {
    ReleaseType.Vacation -> "ОТП"
    ReleaseType.SickLeave -> "Б/Л"
    ReleaseType.Courses -> "КУР"
    ReleaseType.Donor -> "ДОН"
    ReleaseType.ChildCare -> "УХ"
    ReleaseType.DayOff -> "ВЫХ"
    ReleaseType.BusinessTrip -> "КОМ"
    ReleaseType.Other -> "ОТВ"
}

/**
 * Глиф типа отвлечения (по дизайну calendar-merged): солнце — отпуск,
 * календарь — выходной, мед-кейс — остальные. Рисуется в системе координат 24×24.
 */
@Composable
private fun AbsenceGlyph(type: ReleaseType, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val sw = 2f * s
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(color, p(x1, y1), p(x2, y2), strokeWidth = sw, cap = StrokeCap.Round)

        when (type) {
            ReleaseType.Vacation -> { // солнце
                drawCircle(color, radius = 4.5f * s, center = p(12f, 12f), style = Stroke(sw))
                for (k in 0..7) {
                    val a = Math.toRadians(k * 45.0)
                    val cx = cos(a).toFloat(); val cy = sin(a).toFloat()
                    line(12f + cx * 7f, 12f + cy * 7f, 12f + cx * 9f, 12f + cy * 9f)
                }
            }
            ReleaseType.DayOff -> { // календарь
                drawRoundRect(
                    color, topLeft = p(3f, 4f), size = Size(18f * s, 17f * s),
                    cornerRadius = CornerRadius(2.5f * s, 2.5f * s), style = Stroke(sw),
                )
                line(3f, 9f, 21f, 9f)
                line(8f, 2f, 8f, 6f)
                line(16f, 2f, 16f, 6f)
            }
            else -> { // мед-кейс (больничный/курсы/донорские/уход/прочее)
                drawRoundRect(
                    color, topLeft = p(3f, 6f), size = Size(18f * s, 14f * s),
                    cornerRadius = CornerRadius(3f * s, 3f * s), style = Stroke(sw),
                )
                line(9f, 6f, 9f, 4f); line(9f, 4f, 15f, 4f); line(15f, 4f, 15f, 6f)
                line(12f, 10f, 12f, 16f); line(9f, 13f, 15f, 13f)
            }
        }
    }
}

private val WEEK_RU = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

// Высота ячейки дня и радиус скругления. Радиус небольшой относительно
// высоты — иначе пустые ячейки читаются как вытянутые «капсулы». Высота
// вмещает номер + 2 явки.
private val CalendarCellHeight = 60.dp
private val CalendarCellRadius = 8.dp

private fun monthGenitive(monthIndex: Int): String = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря"
).getOrElse(monthIndex) { "" }

/**
 * Экран «Календарь» — поездки + отвлечения в одном месяце.
 * Соответствует дизайну CalendarVariantE (calendar-variants-de.jsx) + карточки дня.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    plan: RoutePlanState,
    onBack: () -> Unit,
    onShiftMonth: (Int) -> Unit,
    onFillMonth: () -> Unit,
    onTripClick: (String) -> Unit,
    onEnterRoutePlan: () -> Unit,
    onExitRoutePlan: () -> Unit,
    onPickTime: (Long) -> Unit,
    onAddCustomTime: (Int, Int) -> Unit,
    onToggleDay: (Int) -> Unit,
    onCreateRoutes: () -> Unit,
    onAddAbsence: (Int) -> Unit,
    onAddDayoff: (Int) -> Unit,
    onDeleteAbsenceDay: (Int) -> Unit,
    onDeleteAbsencePeriod: (Int, Int) -> Unit,
    dateAndTimeConverter: DateAndTimeConverter?,
    timeContext: TimeCalculationContext?,
    convertTimeToString: (Long?) -> String,
    onDeleteRoute: (Route) -> Unit,
    // Быстрый просмотр маршрута (LongClick) — как на Главном и «Все маршруты».
    previewRouteState: PreviewRouteUiState = PreviewRouteUiState(),
    onCalculationHomeRest: (Route?) -> Unit = {},
    onCalculationActualRest: (Route?) -> Unit = {},
    onToggleFavorite: (Route) -> Unit = {},
    onShareRoute: (Route) -> Unit = {},
    onSyncRoute: (Route) -> Unit = {},
    onMakeCopy: (String) -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    var selectedDay by remember { mutableIntStateOf(state.today ?: 1) }
    // Маршрут, открытый в быстром просмотре, и id для подтверждения копии.
    var routeForQuickView by remember { mutableStateOf<Route?>(null) }
    var copyRouteId by remember { mutableStateOf<String?>(null) }
    var routeForRemove by remember { mutableStateOf<Route?>(null) }
    var filter by remember { mutableStateOf(CalendarFilter.ALL) }
    var expanded by remember { mutableStateOf(true) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showCustomTime by remember { mutableStateOf(false) }

    val planning = plan.active
    val monthExpanded = expanded || planning // в режиме планирования — всегда месяц

    // Сброс выбранного дня при смене месяца
    LaunchedEffect(state.month, state.year) {
        selectedDay = state.today ?: 1
    }

    Scaffold(
        containerColor = cs.background,
        bottomBar = { if (planning) CreateRoutesBar(plan = plan, onCreate = onCreateRoutes) },
    ) { padding ->
        if (state.isLoading && state.year == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = cs.tertiary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState()),
        ) {
            // Тонкий индикатор загрузки при смене месяца (данные уже есть на экране,
            // полноэкранный лоадер тут не нужен). Фиксированная высота — без сдвига вёрстки.
            Box(Modifier.fillMaxWidth().height(3.dp)) {
                if (state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = cs.tertiary,
                        trackColor = Color.Transparent,
                    )
                }
            }
            // ── Шапка ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = state.monthLabelUpper,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp
                        ),
                        color = cs.onSurfaceVariant,
                    )
                    Text(
                        text = state.totalWorkedText,
                        fontFamily = MonoFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 34.sp,
                        letterSpacing = (-1).sp,
                        color = cs.primary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (planning) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, cs.outline, RoundedCornerShape(18.dp))
                            .clickable { onExitRoutePlan() }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text("Отмена", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = cs.primary)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PillIconButton(onClick = { onShiftMonth(-1) }) {
                            Text("‹", fontSize = 18.sp, color = cs.primary)
                        }
                        PillIconButton(onClick = { onShiftMonth(1) }) {
                            Text("›", fontSize = 18.sp, color = cs.primary)
                        }
                    }
                }
            }

            // ── Сегмент-фильтр (только в обычном режиме месяца) ──
            if (!planning && expanded) {
                SegmentedFilter(
                    filter = filter,
                    onSelect = { filter = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // ── Подсказка режима планирования ──
            if (planning) {
                PlanHint(
                    plan = plan,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp),
                )
            }

            // ── Дни недели ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                WEEK_RU.forEachIndexed { i, d ->
                    Text(
                        text = d.uppercase(),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = if (i >= 5) cs.tertiary else cs.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }

            // ── Сетка месяца / неделя ──
            MonthGrid(
                state = state,
                expanded = monthExpanded,
                filter = if (planning) CalendarFilter.ALL else filter,
                selectedDay = if (planning) -1 else selectedDay,
                plan = plan,
                onDayClick = { day -> if (planning) onToggleDay(day) else selectedDay = day },
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            if (planning) {
                // ── Чипы времени явки ──
                TimeChipsRow(
                    plan = plan,
                    onPick = onPickTime,
                    onCustom = { showCustomTime = true },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                )
                Spacer(Modifier.height(24.dp))
            } else {
                // ── Свернуть / развернуть ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(cs.surfaceBright)
                            .clickable { expanded = !expanded }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(if (expanded) "⌃" else "⌄", fontSize = 11.sp, color = cs.onSurfaceVariant)
                        Text(
                            if (expanded) "Свернуть в неделю" else "Развернуть в месяц",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }

                // ── Кнопки действий ──
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(cs.tertiary)
                            .clickable { showAddSheet = true }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "+ Добавить событие на $selectedDay ${monthGenitive(state.month)}",
                            color = cs.surface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.5.dp, cs.outline, RoundedCornerShape(14.dp))
                            .clickable { onFillMonth() }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.calendar_today_24px),
                                contentDescription = null,
                                tint = cs.tertiary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                "Заполнить месяц по графику",
                                color = cs.primary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                // ── Детали выбранного дня ──
                DayDetails(
                    state = state,
                    selectedDay = selectedDay,
                    onTripClick = onTripClick,
                    onLongClickRoute = { routeForQuickView = it },
                    onDeleteAbsenceDay = onDeleteAbsenceDay,
                    onDeleteAbsencePeriod = onDeleteAbsencePeriod,
                    dateAndTimeConverter = dateAndTimeConverter,
                    timeContext = timeContext,
                    convertTimeToString = convertTimeToString,
                    onDeleteRoute = onDeleteRoute,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showAddSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState,
            containerColor = cs.surface,
        ) {
            AddEventSheet(
                dayLabel = "$selectedDay ${monthGenitive(state.month)}",
                onRoute = { showAddSheet = false; onEnterRoutePlan() },
                onAbsence = { showAddSheet = false; onAddAbsence(selectedDay) },
                onDayoff = { showAddSheet = false; onAddDayoff(selectedDay) },
            )
        }
    }

    if (showCustomTime) {
        TimePickerDialog(
            title = "Своё время явки",
            confirmLabel = "Готово",
            initialHour = 8,
            initialMinute = 0,
            onConfirm = { h, m -> showCustomTime = false; onAddCustomTime(h, m) },
            onDismiss = { showCustomTime = false },
        )
    }

    // ── Быстрый просмотр маршрута при LongClick (как на Главном/Все маршруты) ──
    // Спецификация: route-quick-view-android.md + SCREEN_SPECS.md.
    routeForQuickView?.let { route ->
        val flags = state.routeFlags[route.basicData.id]
        LaunchedEffect(route.basicData.id) {
            onCalculationHomeRest(route)
            onCalculationActualRest(route)
        }
        RouteQuickViewSheet(
            route = route,
            shiftPaymentText = state.routePayments[route.basicData.id],
            isHeavyTrains = flags?.isHeavyTrains ?: false,
            isLongCompositionTrain = flags?.isLongCompositionTrain ?: false,
            isExtendedServicePhaseTrains = flags?.isExtendedServicePhaseTrains ?: false,
            dateAndTimeConverter = dateAndTimeConverter,
            minTimeRest = state.minTimeRest,
            homeRest = previewRouteState.homeRest,
            actualRestDuration = previewRouteState.actualRestDuration,
            actualRestUntil = previewRouteState.actualRestUntil,
            onDismiss = { routeForQuickView = null },
            onOpen = { id ->
                routeForQuickView = null
                onTripClick(id)
            },
            onToggleFavorite = { onToggleFavorite(it) },
            onShare = {
                // Закрываем шторку, чтобы snackbar с результатом/ошибкой не оказался под ней.
                routeForQuickView = null
                onShareRoute(it)
            },
            onCopy = { id ->
                routeForQuickView = null
                copyRouteId = id
            },
            onSync = { onSyncRoute(it) },
            onRequestDelete = { r ->
                routeForQuickView = null
                routeForRemove = r
            },
        )
    }

    // Подтверждение создания копии маршрута.
    copyRouteId?.let { id ->
        AppBottomSheet(
            onDismissRequest = { copyRouteId = null },
            sheetState = rememberModalBottomSheetState(),
            headerContent = {
                Text(
                    "Создать копию маршрута?",
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            },
            actions = listOf(
                BottomSheetAction(text = "Создать копию") {
                    copyRouteId = null
                    onMakeCopy(id)
                }
            ),
        )
    }

    // Подтверждение удаления маршрута из быстрого просмотра.
    routeForRemove?.let { route ->
        AppBottomSheet(
            onDismissRequest = { routeForRemove = null },
            sheetState = rememberModalBottomSheetState(),
            headerContent = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Удалить маршрут?",
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.primary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        dateAndTimeConverter?.getDateMiniAndTime(route.basicData.timeStartWork) ?: "",
                        fontFamily = MonoFont,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            },
            actions = listOf(
                BottomSheetAction(text = "Да, удалить") {
                    routeForRemove = null
                    onDeleteRoute(route)
                }
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Создать",
) {
    val timeState = rememberTimePickerState(
        initialHour = initialHour, initialMinute = initialMinute, is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = {
            TextButton(onClick = { onConfirm(timeState.hour, timeState.minute) }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        title = { Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = timeState)
            }
        },
    )
}

private enum class CalendarFilter { ALL, TRIPS, ABSENCES }

@Composable
private fun PlanHint(plan: RoutePlanState, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val step = when {
        plan.activeTime == null -> 1
        plan.plannedDays.isEmpty() -> 2
        else -> 3
    }
    val title = if (step == 1) "Время явки" else "Даты"
    val sub = when (step) {
        1 -> "Выберите время явки из списка ниже"
        2 -> "Отметьте дни в календаре"
        else -> "Готово — можно создавать маршруты"
    }
    val done = step == 3
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (done) cs.primaryContainer else Color.Transparent)
            .border(1.5.dp, cs.tertiary, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(cs.tertiary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (done) "✓" else if (step == 1) "1" else "2",
                fontFamily = MonoFont, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = cs.surface,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Шаг ${if (step == 1) 1 else 2} · $title".uppercase(),
                fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = cs.tertiary,
            )
            Text(sub, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = cs.primary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeChipsRow(
    plan: RoutePlanState,
    onPick: (Long) -> Unit,
    onCustom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "ВРЕМЯ ЯВКИ",
            fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
            color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 2.dp, bottom = 10.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            plan.timeOptions.forEach { opt ->
                val active = opt.millis == plan.activeTime
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) cs.tertiary else cs.surface)
                        .border(
                            1.dp,
                            if (active) Color.Transparent else cs.outlineVariant,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable { onPick(opt.millis) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                ) {
                    Text(
                        opt.label,
                        fontFamily = MonoFont,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) cs.surface else cs.primary,
                    )
                }
            }
            // Своё время
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, cs.tertiary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable { onCustom() }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Text("+ Своё время", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = cs.tertiary)
            }
        }
    }
}

@Composable
private fun CreateRoutesBar(plan: RoutePlanState, onCreate: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val count = plan.plannedDays.size
    val enabled = count > 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cs.background)
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 26.dp),
    ) {
        Text(
            if (enabled) "$count ${plural(count, "маршрут", "маршрута", "маршрутов")} к созданию"
            else "Выберите время и отметьте дни",
            fontSize = 12.sp,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (enabled) cs.tertiary else cs.surfaceBright)
                .clickable(enabled = enabled) { onCreate() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Создать маршруты",
                color = if (enabled) cs.surface else cs.onSurfaceVariant,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun plural(n: Int, one: String, few: String, many: String): String {
    val m10 = n % 10
    val m100 = n % 100
    return when {
        m100 in 11..14 -> many
        m10 == 1 -> one
        m10 in 2..4 -> few
        else -> many
    }
}

@Composable
private fun SegmentedFilter(
    filter: CalendarFilter,
    onSelect: (CalendarFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val items = listOf(
        CalendarFilter.ALL to "Всё",
        CalendarFilter.TRIPS to "Поездки",
        CalendarFilter.ABSENCES to "Отвлечения",
    )
    // Три равных сегмента фиксированной ширины — при крупном шрифте «Отвлечения»
    // не влезает и рвётся посреди слова. Ограничиваем масштаб подписей до 1.2×,
    // чтобы каждая помещалась одной строкой.
    val segDensity = LocalDensity.current.let { d ->
        if (d.fontScale > 1.2f) Density(d.density, 1.2f) else d
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(cs.surfaceBright)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items.forEach { (id, label) ->
            val active = filter == id
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) cs.surface else Color.Transparent)
                    .clickable { onSelect(id) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalDensity provides segDensity) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) cs.primary else cs.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    state: CalendarUiState,
    expanded: Boolean,
    filter: CalendarFilter,
    selectedDay: Int,
    plan: RoutePlanState,
    onDayClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val first = LocalDate.of(state.year, state.month + 1, 1)
    val offset = first.dayOfWeek.value - 1 // Пн = 0
    val daysInMonth = first.lengthOfMonth()

    // Дни, которые нужно показать
    val cells: List<Int?> = if (expanded) {
        val list = ArrayList<Int?>()
        repeat(offset) { list.add(null) }
        for (d in 1..daysInMonth) list.add(d)
        while (list.size % 7 != 0) list.add(null)
        list
    } else {
        // Неделя выбранного дня
        val selDow = LocalDate.of(state.year, state.month + 1, selectedDay).dayOfWeek.value - 1
        val weekStart = selectedDay - selDow
        (0..6).map { i -> (weekStart + i).takeIf { it in 1..daysInMonth } }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                week.forEachIndexed { i, d ->
                    Box(modifier = Modifier.weight(1f)) {
                        DayCell(
                            state = state,
                            day = d,
                            weekendIndex = i,
                            filter = filter,
                            isSelected = d == selectedDay,
                            isHoliday = d != null && state.holidayDays.contains(d),
                            planning = plan.active,
                            plannedTime = if (d != null && plan.plannedDays.contains(d)) plan.activeTime else null,
                            onClick = { d?.let(onDayClick) },
                        )
                    }
                }
                // добить строку до 7, если неделя короче
                repeat(7 - week.size) { Box(modifier = Modifier.weight(1f)) {} }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun DayCell(
    state: CalendarUiState,
    day: Int?,
    weekendIndex: Int,
    filter: CalendarFilter,
    isSelected: Boolean,
    isHoliday: Boolean,
    planning: Boolean,
    plannedTime: Long?,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    // Высота ячейки растёт вместе с системным шрифтом, чтобы при крупном шрифте
    // два времени поездок помещались, а не подрезались снизу. Одинаково для всех
    // ячеек — недели остаются ровными.
    val cellHeight = CalendarCellHeight +
        30.dp * (LocalDensity.current.fontScale - 1f).coerceAtLeast(0f)
    // Ширину ячейки (сетка 7 колонок) увеличить нельзя, поэтому подписи времени
    // рендерим в дизайн-размере (fontScale = 1) и запрещаем перенос — иначе «12:35»
    // рвётся на «12:/35». Полный текст доступен в деталях дня ниже.
    val timeDensity = LocalDensity.current.let { d ->
        if (d.fontScale > 1f) Density(d.density, 1f) else d
    }
    if (day == null) {
        Spacer(Modifier.height(cellHeight))
        return
    }
    val trips = if (filter == CalendarFilter.ABSENCES) null else state.tripsByDay[day]
    val absence = if (filter == CalendarFilter.TRIPS) null else state.absenceByDay[day]
    val isToday = day == state.today
    val isWeekend = weekendIndex >= 5
    val ac = absence?.let { releaseColor(it.type) }
    val isPlanned = planning && plannedTime != null
    // В планировании дни с отвлечением недоступны — глушим. Исключение — «Выходной»:
    // это перенос выходного дня, явку в него можно ставить, поэтому не глушим.
    val dimmed = planning && absence != null &&
        absence.type != ReleaseType.DayOff && !isPlanned

    val bg = when {
        isPlanned -> cs.tertiary
        ac != null -> ac.copy(alpha = 0.16f)
        trips != null -> cs.tertiary.copy(alpha = 0.05f)
        else -> cs.surface
    }
    val borderColor = when {
        isPlanned -> cs.tertiary
        isSelected -> cs.tertiary
        ac != null -> ac.copy(alpha = 0.27f)
        trips != null -> cs.tertiary.copy(alpha = 0.13f)
        else -> cs.outlineVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(cellHeight)
            .clip(RoundedCornerShape(CalendarCellRadius))
            .background(bg)
            .border(
                width = if (isSelected || isPlanned) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(CalendarCellRadius),
            )
            .alpha(if (dimmed) 0.45f else 1f)
            .clickable { onClick() }
            .padding(horizontal = 5.dp, vertical = 4.dp),
    ) {
        // Номер дня. Размер не фиксируем жёстко (только минимум) — иначе при крупном
        // шрифте 2-значные числа («31») не влезают в 20dp и обрезаются до первой цифры.
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                .then(
                    if (isToday && !isPlanned) Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(cs.tertiary)
                    else Modifier
                )
                .padding(horizontal = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.toString(),
                fontFamily = MonoFont,
                fontSize = 13.sp,
                fontWeight = if (isToday || isPlanned) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = when {
                    isPlanned -> cs.surface
                    isToday -> cs.surface
                    isHoliday -> cs.error       // праздничный день — красным
                    isWeekend -> cs.onSurfaceVariant
                    else -> cs.primary
                },
            )
        }
        // Запланированное время явки (режим планирования)
        if (isPlanned) {
            Spacer(Modifier.weight(1f))
            CompositionLocalProvider(LocalDensity provides timeDensity) {
                Text(
                    text = fmtFromMidnight(plannedTime!!),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    fontFamily = MonoFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = cs.surface,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        } else {
            // Явки (существующие поездки)
            if (trips != null) {
                Spacer(Modifier.height(2.dp))
                CompositionLocalProvider(LocalDensity provides timeDensity) {
                    trips.take(2).forEach { tr ->
                        Text(
                            text = tr.timeText,
                            fontFamily = MonoFont,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = cs.tertiary,
                            lineHeight = 12.sp,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
                if (trips.size > 2) {
                    CompositionLocalProvider(LocalDensity provides timeDensity) {
                        Text("+${trips.size - 2}", fontSize = 9.sp, color = cs.onSurfaceVariant,
                            maxLines = 1, softWrap = false)
                    }
                }
            }
            // Бейдж отвлечения
            if (absence != null && ac != null) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = absBadge(absence.type),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                    color = ac,
                )
            }
        }
    }
}

private fun fmtFromMidnight(ms: Long): String {
    val total = ms / 60_000
    val h = (total / 60).toInt()
    val m = (total % 60).toInt()
    return "%02d:%02d".format(h, m)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetails(
    state: CalendarUiState,
    selectedDay: Int,
    onTripClick: (String) -> Unit,
    onLongClickRoute: (Route) -> Unit,
    onDeleteAbsenceDay: (Int) -> Unit,
    onDeleteAbsencePeriod: (Int, Int) -> Unit,
    dateAndTimeConverter: DateAndTimeConverter?,
    timeContext: TimeCalculationContext?,
    convertTimeToString: (Long?) -> String,
    onDeleteRoute: (Route) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val routes = state.routesByDay[selectedDay].orEmpty()
    val absence = state.absenceByDay[selectedDay]
    val dow = LocalDate.of(state.year, state.month + 1, selectedDay).dayOfWeek.value - 1
    var showDeleteAbsence by remember(selectedDay) { mutableStateOf(false) }
    var routeToDelete by remember(selectedDay) { mutableStateOf<Route?>(null) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(cs.outlineVariant),
        )
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "$selectedDay ${monthGenitive(state.month)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = cs.primary,
            )
            Text(
                WEEK_RU[dow],
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurfaceVariant,
            )
        }

        // Праздничный день — подпись (название из БД, если есть).
        if (state.holidayDays.contains(selectedDay)) {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.error.copy(alpha = 0.10f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(cs.error))
                Text(
                    state.holidayNames[selectedDay] ?: "Праздничный день",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.error,
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            absence?.let { abs ->
                SwipeToRevealDelete(
                    onDeleteClick = { showDeleteAbsence = true },
                    backgroundVerticalPadding = 0.dp,
                ) {
                    AbsenceCard(abs, monthIndex = state.month)
                }
            }
            // Маршруты — тот же item, что на главном экране (со свайпом на удаление).
            if (dateAndTimeConverter != null) {
                routes.forEach { route ->
                    val flags = state.routeFlags[route.basicData.id]
                    ItemHomeScreen(
                        route = route,
                        convertTimeToString = convertTimeToString,
                        onRequestDelete = { routeToDelete = route },
                        onLongClick = { onLongClickRoute(route) },
                        containerColor = cs.secondary,
                        onClick = { onTripClick(route.basicData.id) },
                        dateAndTimeConverter = dateAndTimeConverter,
                        isHeavyTrains = flags?.isHeavyTrains ?: false,
                        isLongCompositionTrain = flags?.isLongCompositionTrain ?: false,
                        isExtendedServicePhaseTrains = flags?.isExtendedServicePhaseTrains ?: false,
                        isHolidayTimeInRoute = state.holidayDays.contains(selectedDay),
                        monthOfYear = state.monthData,
                        offsetInMoscow = state.offsetInMoscow,
                        timeCalculationContext = timeContext,
                    )
                }
            }
            if (absence == null && routes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Нет событий в этот день.",
                        fontSize = 14.sp,
                        color = cs.onSurfaceVariant,
                    )
                }
            }
        }
    }

    routeToDelete?.let { route ->
        val deleteSheetState = rememberModalBottomSheetState()
        AppBottomSheet(
            onDismissRequest = { routeToDelete = null },
            sheetState = deleteSheetState,
            headerContent = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Обычный текст — Inter (стиль темы).
                    Text(
                        "Удалить маршрут?",
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.primary,
                        textAlign = TextAlign.Center,
                    )
                    // Дата/время — идентификатор → MonoFont (Правило применения шрифтов).
                    Text(
                        dateAndTimeConverter?.getDateMiniAndTime(route.basicData.timeStartWork) ?: "",
                        fontFamily = MonoFont,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            },
            actions = listOf(
                BottomSheetAction(text = "Да, удалить") { onDeleteRoute(route) }
            ),
        )
    }

    if (showDeleteAbsence && absence != null) {
        val absenceSheetState = rememberModalBottomSheetState()
        val multi = absence.periodDays > 1
        AppBottomSheet(
            onDismissRequest = { showDeleteAbsence = false },
            sheetState = absenceSheetState,
            headerContent = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Удалить «${absence.label}»?",
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.primary,
                        textAlign = TextAlign.Center,
                    )
                    if (multi) {
                        Text(
                            "Занимает ${absence.periodStart}–${absence.periodEnd} ${monthGenitive(state.month)} · ${absence.periodDays} ${daysWord(absence.periodDays)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            },
            actions = if (multi) listOf(
                BottomSheetAction(text = "Только этот день") { onDeleteAbsenceDay(selectedDay) },
                BottomSheetAction(text = "Весь период · ${absence.periodDays} ${daysWord(absence.periodDays)}") {
                    onDeleteAbsencePeriod(absence.periodStart, absence.periodEnd)
                },
            ) else listOf(
                BottomSheetAction(text = "Да, удалить") { onDeleteAbsenceDay(selectedDay) },
            ),
        )
    }
}

private fun daysWord(n: Int): String {
    val m10 = n % 10; val m100 = n % 100
    return when {
        m100 in 11..14 -> "дней"
        m10 == 1 -> "день"
        m10 in 2..4 -> "дня"
        else -> "дней"
    }
}

@Composable
private fun AbsenceCard(abs: AbsenceInfo, monthIndex: Int) {
    val cs = MaterialTheme.colorScheme
    val c = releaseColor(abs.type)
    val multi = abs.periodDays > 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            // Непрозрачный фон: базовый цвет экрана + мягкая заливка типа,
            // иначе красная кнопка «Удалить» просвечивает сквозь карточку при свайпе.
            .background(cs.background)
            .background(c.copy(alpha = 0.16f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(c),
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(c),
            contentAlignment = Alignment.Center,
        ) {
            AbsenceGlyph(
                type = abs.type,
                color = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(abs.label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cs.primary)
            Text(
                if (multi) "${abs.periodStart}–${abs.periodEnd} ${monthGenitive(monthIndex)} · ${abs.periodDays} ${daysWord(abs.periodDays)}"
                else "Весь день",
                fontSize = 13.sp, color = cs.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (abs.type == ReleaseType.DayOff) {
                // Выходной: работа в этот день — по двойному тарифу.
                Text(
                    "×2",
                    fontFamily = MonoFont,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = cs.tertiary,
                )
                Text("тариф", fontSize = 11.sp, color = cs.onSurfaceVariant)
            } else {
                // Для периода — всего часов за период, для одного дня — часы дня.
                Text(
                    "${if (multi) abs.periodHours else abs.hoursPerDay}:00",
                    fontFamily = MonoFont,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.primary,
                )
                Text(if (multi) "всего" else "часов", fontSize = 11.sp, color = cs.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AddEventSheet(
    dayLabel: String,
    onRoute: () -> Unit,
    onAbsence: () -> Unit,
    onDayoff: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text("Что добавить?", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = cs.primary)
        Text("На $dayLabel", fontSize = 13.sp, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        AddEventRow("Маршрут", cs.tertiary, onRoute)
        Spacer(Modifier.height(8.dp))
        AddEventRow("Отвлечение", SickColor, onAbsence)
        Spacer(Modifier.height(8.dp))
        AddEventRow("Выходной", DayOffColor, onDayoff)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AddEventRow(label: String, color: Color, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cs.background)
            .border(1.dp, cs.outlineVariant, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Text(label, modifier = Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = cs.primary)
        Text("›", fontSize = 18.sp, color = cs.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun PillIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(cs.surfaceBright)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { content() }
}
