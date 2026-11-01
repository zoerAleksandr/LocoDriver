package com.z_company.route.ui.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.ConverterLongToTime
import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.entities.WorkScheduleMode
import com.z_company.domain.entities.WorkScheduleProfile
import com.z_company.domain.onboarding.HintId
import com.z_company.domain.onboarding.NormMonthTourStep
import com.z_company.domain.onboarding.OnboardingHints
import com.z_company.route.component.CoachAnchor
import com.z_company.route.component.CoachTour
import com.z_company.route.component.TourStep
import com.z_company.route.component.coachAnchor
import com.z_company.route.component.rememberOnboardingHints
import com.z_company.route.ui.absBadge
import com.z_company.route.ui.releaseColor
import com.z_company.route.viewmodel.NormMonthCell
import com.z_company.route.viewmodel.NormMonthUiState
import com.z_company.route.viewmodel.NormMonthViewModel
import com.z_company.route.viewmodel.WeekPatternDay

private val MonthNames = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
)
private val MonthGenitive = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)
private val WeekdayShort = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
private val WeekdayFull = listOf(
    "понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье",
)

private val CellHeight = 64.dp
private val CellRadius = 8.dp

/**
 * Календарь нормы режима «Своя настройка» (Настройки → Норма/Регион → Рабочая неделя).
 * В ячейке: число, норма на день и норма с начала месяца. Нажатие — шторка правки дня.
 */
@Composable
internal fun SettingsNormMonthContent(
    initialYear: Int,
    initialMonth: Int,
    viewModel: NormMonthViewModel,
    /** Якоря экскурсии: сама экскурсия и кнопка «?» живут в SettingsScreen (шапка + затемнение на весь экран). */
    tourAnchors: Map<NormMonthTourStep, CoachAnchor>,
    onOpenTour: () -> Unit,
) {
    LaunchedEffect(initialYear, initialMonth) { viewModel.open(initialYear, initialMonth) }
    val state by viewModel.uiState.collectAsState()
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    // Экскурсия: один раз при первом открытии (когда есть данные календаря), «?» открывает снова.
    val onboardingHints = rememberOnboardingHints()
    LaunchedEffect(state.hasData) {
        if (state.hasData && OnboardingHints.canShow(
                id = HintId.NORM_MONTH_SCREEN,
                shown = onboardingHints.shown,
                homeReached = onboardingHints.homeReached,
                recording = false,
            )
        ) {
            onOpenTour()
            onboardingHints.markShown(HintId.NORM_MONTH_SCREEN)
        }
    }
    var weekExpanded by remember { mutableStateOf(false) }
    var patternDay by remember { mutableStateOf<Int?>(null) }
    val pattern by viewModel.weekPattern.collectAsState()

    Box(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 28.dp),
    ) {
        MonthHeader(
            state = state,
            onPrev = { selectedDay = null; viewModel.shiftMonth(-1) },
            onNext = { selectedDay = null; viewModel.shiftMonth(1) },
            monthAnchor = tourAnchors.getValue(NormMonthTourStep.MONTH),
        )
        Spacer(Modifier.height(10.dp))

        if (!state.hasData) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                text = "Нет данных производственного календаря за этот месяц",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            WeekdayHeader()
            Spacer(Modifier.height(4.dp))
            MonthNormGrid(
                state = state,
                firstEditableAnchor = tourAnchors.getValue(NormMonthTourStep.CELL),
                sheetOpen = selectedDay != null,
                onDayClick = { selectedDay = it },
            )

            Spacer(Modifier.height(16.dp))
            SettingsCard {
                Box(Modifier.coachAnchor(tourAnchors.getValue(NormMonthTourStep.TOTAL))) {
                    SettingsFieldRow(
                        label = "Норма за месяц",
                        value = ConverterLongToTime.getTimeInStringFormat(state.total.toLong() * 3_600_000),
                        mono = true,
                    )
                }
            }
            SettingsGroupHeader("РАБОЧАЯ НЕДЕЛЯ", top = 20.dp, startPad = 4.dp)
            SettingsCard {
                listOf(
                    WorkScheduleMode.STANDARD to "Стандартная · 8 ч, Пн–Пт",
                    WorkScheduleMode.SIX_DAY_7_5 to "Шестидневная · 7 ч + Сб 5 ч",
                ).forEachIndexed { index, (mode, label) ->
                    if (index > 0) SettingsCardSep()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectPreset(mode) }
                            .padding(horizontal = 18.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val selected = state.mode == mode
                        Text(
                            modifier = Modifier.weight(1f),
                            text = if (selected) "✓  $label" else label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                            color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                SettingsCardSep()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .coachAnchor(tourAnchors.getValue(NormMonthTourStep.WEEKS))
                        .clickable { weekExpanded = !weekExpanded }
                        .padding(horizontal = 18.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Как у «Стандартной» и «Шестидневной»: выделена, только когда выбрана своя неделя.
                    val custom = state.mode == WorkScheduleMode.CUSTOM
                    Text(
                        modifier = Modifier.weight(1f),
                        text = if (custom) "✓  По неделям" else "По неделям",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (custom) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = if (custom) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = if (weekExpanded) "⌃" else "⌄",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (weekExpanded) {
                    SettingsCardSep()
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            WeekdayShort.forEachIndexed { i, name ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(CellHeight)
                                        .clip(RoundedCornerShape(CellRadius))
                                        .background(MaterialTheme.colorScheme.background)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(CellRadius))
                                        .clickable { patternDay = i }
                                        .padding(horizontal = 5.dp, vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = name,
                                        textAlign = TextAlign.Center,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (i >= 5) MaterialTheme.colorScheme.onSurfaceVariant
                                        else MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    if (pattern[i].dayOff) {
                                        CellLabel(absBadge(ReleaseType.DayOff), releaseColor(ReleaseType.DayOff))
                                    } else {
                                        Text(
                                            text = pattern[i].hours.toString(),
                                            fontFamily = MonoFont,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.applyPattern(); weekExpanded = false },
                        ) {
                            Text(
                                text = "Применить к месяцу",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                }
            }
            SettingsSectionNote(
                "В ячейке: число, норма на день и норма с начала месяца. " +
                    "Нажмите на день, чтобы изменить норму или поставить выходной (ВЫХ). " +
                    "Дни без правок заполнены по выбранной ранее рабочей неделе. " +
                    "«По неделям» — задайте норму для каждого дня недели и перенесите её на весь месяц. " +
                    "Выбор стандартной или шестидневной недели сбрасывает правки по дням в этом месяце."
            )
        }
    }
    }

    patternDay?.let { i ->
        PatternDaySheet(
            title = WeekdayFull[i].replaceFirstChar { it.uppercase() },
            day = pattern[i],
            onSetHours = { viewModel.setPatternHours(i, it) },
            onDayOff = { viewModel.setPatternDayOff(i, it) },
            onDismiss = { patternDay = null },
        )
    }

    selectedDay?.let { day ->
        state.cells.firstOrNull { it.day == day }?.let { cell ->
            NormDaySheet(
                state = state,
                cell = cell,
                onSetHours = { viewModel.setHours(day, it) },
                onReset = { viewModel.resetHours(day) },
                onDayOff = { viewModel.setDayOff(day, it) },
                onDismiss = { selectedDay = null },
            )
        }
    }
}

@Composable
private fun MonthHeader(
    state: NormMonthUiState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    monthAnchor: CoachAnchor,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .coachAnchor(monthAnchor),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            modifier = Modifier
                .clickable(enabled = state.canGoPrev, onClick = onPrev)
                .alpha(if (state.canGoPrev) 1f else 0.3f)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            text = "‹",
            style = MaterialTheme.typography.headlineSmall,
            color = cs.tertiary,
        )
        Text(
            text = "${MonthNames.getOrElse(state.month) { "" }} ${state.year}",
            style = MaterialTheme.typography.titleMedium,
            color = cs.primary,
        )
        Text(
            modifier = Modifier
                .clickable(enabled = state.canGoNext, onClick = onNext)
                .alpha(if (state.canGoNext) 1f else 0.3f)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            text = "›",
            style = MaterialTheme.typography.headlineSmall,
            color = cs.tertiary,
        )
    }
}

@Composable
private fun WeekdayHeader() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        WeekdayShort.forEachIndexed { i, name ->
            Text(
                modifier = Modifier.weight(1f),
                text = name,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = if (i >= 5) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
    }
}

@Composable
private fun MonthNormGrid(
    state: NormMonthUiState,
    firstEditableAnchor: CoachAnchor,
    sheetOpen: Boolean,
    onDayClick: (Int) -> Unit,
) {
    val byDay = state.cells.associateBy { it.day }
    // День, у которого изменилась норма (или ВЫХ), коротко мигает рамкой: после закрытия шторки (сравниваем с тем,
    // что было при её открытии — вернули прежнее значение, мигать нечему) или сразу, если правка была без шторки
    // («Применить к месяцу»). При открытии экрана и смене месяца не мигаем.
    val signatures = state.cells.associate { it.day to it.hours * 2 + if (it.isDayOff) 1 else 0 }
    val latest by rememberUpdatedState(signatures)
    val previous = remember(state.year, state.month) { HashMap<Int, Int>() }
    var snapshot by remember(state.year, state.month) { mutableStateOf<Map<Int, Int>?>(null) }
    val blinks = remember(state.year, state.month) { mutableStateMapOf<Int, Int>() }
    fun blinkChanged(from: Map<Int, Int>) {
        latest.forEach { (day, sig) ->
            val old = from[day]
            if (old != null && old != sig) blinks[day] = (blinks[day] ?: 0) + 1
        }
    }
    LaunchedEffect(sheetOpen) {
        if (sheetOpen) {
            snapshot = latest
        } else {
            snapshot?.let { blinkChanged(it) }
            snapshot = null
        }
    }
    LaunchedEffect(signatures) {
        if (!sheetOpen && snapshot == null && previous.isNotEmpty()) blinkChanged(previous)
        previous.clear()
        previous.putAll(signatures)
    }
    val anchorDay = state.cells.firstOrNull { !it.isHoliday && !it.isDayOff && it.absence == null && it.hours > 0 }?.day
    val first = state.cells.firstOrNull() ?: return
    // Первое число месяца — смещение по дню недели его ячейки.
    val offset = (first.weekdayIndex - (first.day - 1) % 7 + 7) % 7
    val daysInMonth = state.cells.maxOf { it.day }
    val slots: List<Int?> = buildList {
        repeat(offset) { add(null) }
        for (d in 1..daysInMonth) add(d)
        while (size % 7 != 0) add(null)
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        slots.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { d ->
                    Box(Modifier.weight(1f)) {
                        val cell = d?.let { byDay[it] }
                        if (cell == null) Spacer(Modifier.height(CellHeight))
                        else NormDayCell(
                            cell = cell,
                            blink = blinks[cell.day] ?: 0,
                            modifier = if (cell.day == anchorDay) Modifier.coachAnchor(firstEditableAnchor) else Modifier,
                            onClick = { onDayClick(cell.day) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NormDayCell(cell: NormMonthCell, blink: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    // Три коротких пульса рамки при изменении нормы дня.
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(blink) {
        if (blink > 0) repeat(3) {
            pulse.animateTo(1f, tween(220))
            pulse.animateTo(0f, tween(220))
        }
    }
    val dayOffColor = releaseColor(ReleaseType.DayOff)
    val accent = when {
        cell.isDayOff -> dayOffColor
        cell.absence != null -> releaseColor(cell.absence)
        else -> null
    }
    val shape = RoundedCornerShape(CellRadius)
    val baseBorder = when {
        accent != null -> accent.copy(alpha = 0.27f)
        else -> cs.outlineVariant
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(CellHeight)
            .clip(shape)
            .background(accent?.copy(alpha = 0.16f) ?: cs.surface)
            .border(
                width = (1f + 1.5f * pulse.value).dp,
                color = androidx.compose.ui.graphics.lerp(baseBorder, cs.tertiary, pulse.value),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = cell.day.toString(),
            fontFamily = MonoFont,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = when {
                cell.isHoliday -> cs.error
                cell.weekdayIndex >= 5 -> cs.onSurfaceVariant
                else -> cs.primary
            },
        )
        Spacer(Modifier.weight(1f))
        // Центр: норма дня или метка (ВЫХ / ПР / отвлечение).
        when {
            cell.isHoliday -> CellLabel("ПР", cs.error)
            cell.isDayOff -> CellLabel(absBadge(ReleaseType.DayOff), dayOffColor)
            cell.absence != null -> CellLabel(absBadge(cell.absence), accent ?: cs.primary)
            else -> Text(
                text = cell.hours.toString(),
                fontFamily = MonoFont,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = cs.primary,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = cell.cumulative?.toString() ?: " ",
            fontFamily = MonoFont,
            fontSize = 10.sp,
            color = cs.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun CellLabel(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.3.sp,
        color = color,
        maxLines = 1,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NormDaySheet(
    state: NormMonthUiState,
    cell: NormMonthCell,
    onSetHours: (Int) -> Unit,
    onReset: () -> Unit,
    onDayOff: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val max = WorkScheduleProfile.MAX_CUSTOM_DAY_HOURS
    val editable = !cell.isHoliday && !cell.isDayOff && cell.absence == null
    // Пояснение: что за праздник / какое отвлечение. У обычного дня подписи нет.
    val info: String? = when {
        cell.isHoliday -> (cell.holidayName ?: "Праздничный день") +
            " — нерабочий праздничный день, нормы нет."
        cell.absence != null -> {
            val a = cell.absencePeriodStart
            val b = cell.absencePeriodEnd
            val month = MonthGenitive.getOrElse(state.month) { "" }
            val period = if (a != null && b != null && b > a) "$a–$b $month (${b - a + 1} дн.)" else null
            listOfNotNull(cell.absenceLabel, period).joinToString(", ") +
                ". Норму и отвлечение меняйте в Календаре."
        }
        else -> null
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = cs.secondary,
        shape = Shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            SheetHeader(
                title = "${cell.day} ${MonthGenitive.getOrElse(state.month) { "" }}, " +
                    WeekdayFull.getOrElse(cell.weekdayIndex) { "" },
                onDone = onDismiss,
            )
            if (info != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                )
            }

            if (!cell.isHoliday && cell.absence == null) {
                Spacer(Modifier.height(16.dp))
                HoursStepper(
                    hours = cell.hours,
                    enabled = editable,
                    max = max,
                    onSetHours = onSetHours,
                )
                Spacer(Modifier.height(6.dp))
                // Всегда на месте (даже когда нормы нет), чтобы шторка не меняла высоту.
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "С начала месяца: " + (cell.cumulative?.let { "$it ч" } ?: "—"),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cs.background, Shapes.medium),
                ) {
                    SettingsSwitchRow(
                        label = "Выходной (ВЫХ)",
                        checked = cell.isDayOff,
                        onCheckedChange = onDayOff,
                    )
                }
                // Место под кнопку занято всегда; когда сбрасывать нечего — невидима и неактивна.
                val canReset = editable && cell.isCustom
                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .alpha(if (canReset) 1f else 0f),
                    enabled = canReset,
                    onClick = onReset,
                ) {
                    Text(
                        text = "Вернуть по умолчанию (${cell.defaultHours} ч)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.tertiary,
                    )
                }
            }
        }
    }
}

/** Шторка правки дня недели в разделе «По неделям»: норма и «Выходной». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatternDaySheet(
    title: String,
    day: WeekPatternDay,
    onSetHours: (Int) -> Unit,
    onDayOff: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = cs.secondary,
        shape = Shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            SheetHeader(title = title, onDone = onDismiss)
            Spacer(Modifier.height(16.dp))
            HoursStepper(
                hours = day.hours,
                enabled = !day.dayOff,
                max = WorkScheduleProfile.MAX_CUSTOM_DAY_HOURS,
                onSetHours = onSetHours,
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cs.background, Shapes.medium),
            ) {
                SettingsSwitchRow(
                    label = "Выходной (ВЫХ)",
                    checked = day.dayOff,
                    onCheckedChange = onDayOff,
                )
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, onDone: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        TextButton(onClick = onDone) {
            Text(
                text = "Готово",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun HoursStepper(
    hours: Int,
    enabled: Boolean,
    max: Int,
    onSetHours: (Int) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var field by remember { mutableStateOf(TextFieldValue(hours.toString(), TextRange(hours.toString().length))) }
    // Первый символ после захода в поле заменяет прежнее число (было 7, ввёл 8 → 8, а не 12).
    var fresh by remember { mutableStateOf(false) }
    // Значение изменилось не вводом (кнопки ±, сброс, ВЫХ) — подтягиваем в поле.
    LaunchedEffect(hours) {
        if (field.text.toIntOrNull() != hours) {
            field = TextFieldValue(hours.toString(), TextRange(hours.toString().length))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        StepButton(label = "−", enabled = enabled && hours > 0) { onSetHours(hours - 1) }
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(width = 96.dp, height = 60.dp)
                .clip(Shapes.small)
                .background(cs.background)
                .border(1.dp, cs.outlineVariant, Shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            BasicTextField(
                value = field,
                onValueChange = { raw ->
                    val digits = raw.text.filter { it.isDigit() }
                    val grew = digits.length > field.text.length
                    when {
                        digits.isEmpty() -> {
                            fresh = false
                            field = TextFieldValue("", TextRange(0))
                        }
                        else -> {
                            // Дописанное в конец: при «свежем» поле — заменяет всё число.
                            val typed = digits.takeLast(if (grew && fresh) 1 else 2)
                            var value = typed.toInt()
                            // «81» → пользователь хотел 1: берём последнюю цифру, если вышло больше максимума.
                            if (value > max) value = typed.last().digitToInt()
                            fresh = false
                            field = TextFieldValue(value.toString(), TextRange(value.toString().length))
                            onSetHours(value)
                        }
                    }
                },
                modifier = Modifier.onFocusChanged { f ->
                    if (f.isFocused) {
                        fresh = true
                        // Курсор — в конец, а не туда, куда пришёлся тап.
                        field = field.copy(selection = TextRange(field.text.length))
                    }
                },
                enabled = enabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = MonoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center,
                    color = cs.primary,
                ),
                cursorBrush = SolidColor(cs.tertiary),
            )
        }
        StepButton(label = "+", enabled = enabled && hours < max) { onSetHours(hours + 1) }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(Shapes.small)
            .background(cs.background)
            .border(1.dp, cs.outlineVariant, Shapes.small)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            color = cs.tertiary,
        )
    }
}
