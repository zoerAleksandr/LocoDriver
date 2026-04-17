@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.ui

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.z_company.core.ResultState
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.MonthFullText.getMonthFullText
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.route.component.ChipApp
import java.util.Calendar
import java.util.Date
import androidx.compose.foundation.pager.HorizontalPager
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import com.z_company.domain.entities.ReleasePeriod
import com.z_company.domain.entities.ReleaseType
import kotlinx.coroutines.launch
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import com.z_company.route.component.OutlinedTextFieldApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectReleaseDaysScreen(
    onSaveClick: () -> Unit,
    saveReleaseDaysState: ResultState<Unit>?,
    onReleaseDaysSaved: () -> Unit,
    monthOfYear: MonthOfYear?,
    releasePeriodListState: SnapshotStateList<ReleasePeriod>?,
    addingReleasePeriod: (ReleasePeriod) -> Unit,
    removingReleasePeriod: (ReleasePeriod) -> Unit,
    yearList: List<Int>,
    monthList: List<Int>,
    selectMonthOfYear: (Pair<Int, Int>) -> Unit,
    dateAndTimeConverter: DateAndTimeConverter?,
    allMonthOfYear: List<MonthOfYear>
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TextButton(
                        onClick = onSaveClick,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.tertiary,
                            containerColor = Color.Transparent
                        )
                    ) {
                        Text(
                            text = "Сохранить",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                title = {},
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = Color.Transparent,
                )
            )
        }) { paddingValues ->
        if (saveReleaseDaysState is ResultState.Success) {
            LaunchedEffect(saveReleaseDaysState) {
                onReleaseDaysSaved()
            }
        } else {
            Box(modifier = Modifier.padding(paddingValues)) {
                SelectReleaseDaysContent(
                    monthOfYear = monthOfYear,
                    releasePeriodListState = releasePeriodListState,
                    addingReleasePeriod = addingReleasePeriod,
                    removingReleasePeriod = removingReleasePeriod,
                    yearList = yearList,
                    monthList = monthList,
                    selectMonthOfYear = selectMonthOfYear,
                    dateAndTimeConverter = dateAndTimeConverter,
                    allMonthOfYear = allMonthOfYear
                )
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun SelectReleaseDaysContent(
    monthOfYear: MonthOfYear?,
    allMonthOfYear: List<MonthOfYear>,
    releasePeriodListState: SnapshotStateList<ReleasePeriod>?,
    addingReleasePeriod: (ReleasePeriod) -> Unit,
    removingReleasePeriod: (ReleasePeriod) -> Unit,
    yearList: List<Int>,
    monthList: List<Int>,
    selectMonthOfYear: (Pair<Int, Int>) -> Unit,
    dateAndTimeConverter: DateAndTimeConverter?
) {
    val styleData = MaterialTheme.typography.bodyLarge

    var showMonthSelectorDialog by remember {
        mutableStateOf(false)
    }

    val monthSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    if (showMonthSelectorDialog) {
        monthOfYear?.let { currentMonthOfYear ->
            ModalBottomSheet(
                onDismissRequest = { showMonthSelectorDialog = false },
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
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    var selectedMonth by remember { mutableIntStateOf(currentMonthOfYear.month) }

                    var selectedYear by remember { mutableIntStateOf(currentMonthOfYear.year) }

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
                            selectMonthOfYear(selectedYear to selectedMonth)
                            showMonthSelectorDialog = false
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

    val initialCal = Calendar.getInstance().apply {
        monthOfYear?.let {
            set(it.year, it.month, 1)
        }
    }

    var currentMonth by remember { mutableStateOf(initialCal.time) }

    var selectedStart by remember { mutableStateOf<Long?>(null) }
    var selectedEnd by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(monthOfYear) {
        monthOfYear?.let {
            val cal = Calendar.getInstance().apply {
                set(it.year, it.month, 1)
            }
            currentMonth = cal.time
        }
    }

    fun selectDate(millis: Long) {
        val newStart: Long?
        val newEnd: Long?

        newStart = when {
            selectedStart == null -> millis
            selectedEnd == null -> if (millis > selectedStart!!) selectedStart!! else millis
            else -> millis
        }
        newEnd = when {
            selectedStart == null -> null
            selectedEnd == null -> if (millis > selectedStart!!) millis else null
            else -> null
        }


        selectedStart = newStart?.let {
            Calendar.getInstance().apply {
                timeInMillis = it; set(Calendar.HOUR_OF_DAY, 0); set(
                Calendar.MINUTE,
                0
            ); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        selectedEnd = newEnd?.let {
            Calendar.getInstance().apply {
                timeInMillis = it; set(Calendar.HOUR_OF_DAY, 0); set(
                Calendar.MINUTE,
                0
            ); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }


        val lastSelected = newEnd ?: newStart ?: millis
        val selectedCal = Calendar.getInstance().apply { timeInMillis = lastSelected }
        val currentCal = Calendar.getInstance().apply { time = currentMonth }

        if (selectedCal.get(Calendar.YEAR) != currentCal.get(Calendar.YEAR) ||
            selectedCal.get(Calendar.MONTH) != currentCal.get(Calendar.MONTH)
        ) {
            currentMonth = selectedCal.time
            selectMonthOfYear(selectedCal.get(Calendar.YEAR) to (selectedCal.get(Calendar.MONTH)))
        }
    }

    var showTypeSheet by remember { mutableStateOf(false) }
    var pendingDays by remember { mutableStateOf<List<LocalDate>>(emptyList()) }
    val typeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showTypeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTypeSheet = false },
            sheetState = typeSheetState,
            containerColor = MaterialTheme.colorScheme.secondary,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Выберите тип отвлечения",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val _tz = TimeZone.currentSystemDefault()
                val startDateStr =
                    dateAndTimeConverter?.getDateFromDateLong(
                        pendingDays.first().atStartOfDayIn(_tz).toEpochMilliseconds()
                    ) ?: ""
                val endDateStr = if (pendingDays.size > 1) " - ${
                    dateAndTimeConverter?.getDateFromDateLong(
                        pendingDays.last().atStartOfDayIn(_tz).toEpochMilliseconds()
                    ) ?: ""
                }" else ""
                Text(
                    text = "Период: $startDateStr$endDateStr",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                var selectedType by remember { mutableStateOf<ReleaseType?>(null) }

                val types = listOf(
                    ReleaseType.Vacation,
                    ReleaseType.SickLeave,
                    ReleaseType.Courses,
                    ReleaseType.Donor,
                    ReleaseType.ChildCare,
                    ReleaseType.Other
                )

                val typeColors = mapOf(
                    ReleaseType.Vacation to Color.Green,
                    ReleaseType.SickLeave to Color.Red,
                    ReleaseType.Donor to Color.Blue,
                    ReleaseType.Courses to Color.Yellow,
                    ReleaseType.ChildCare to Color.Magenta,
                    ReleaseType.Other to Color.Gray
                )

                var expanded by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.background,
                            shape = Shapes.medium
                        )
                        .fillMaxWidth()
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextFieldApp(
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            readOnly = true,
                            value = selectedType?.text ?: "Выберите тип",
                            onValueChange = { },
                            textStyle = styleData.copy(color = MaterialTheme.colorScheme.primary),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            types.forEach { t ->
                                val periodColor: Color =
                                    t.let { typeColors[it] } ?: Color.Cyan
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(periodColor)
                                            )
                                            Text(
                                                text = t.text,
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedType = t
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (selectedType != null) {
                            addingReleasePeriod(
                                ReleasePeriod(
                                    days = pendingDays,
                                    type = selectedType
                                )
                            )
                            showTypeSheet = false
                            selectedStart = null
                            selectedEnd = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                ) {
                    Text(
                        text = "Добавить",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .testTag("release_days_lazy_column"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .noRippleEffect {
                        showMonthSelectorDialog = true
                    }
                    .shadow(elevation = 2.dp, shape = Shapes.medium)
                    .background(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = Shapes.medium
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = getMonthFullText(monthOfYear?.month),
                        style = styleData,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = ConverterLongToTime.getTimeInStringFormat(
                            monthOfYear?.getPersonalNormaHours()?.toLong()?.times(3_600_000)
                        ),
                        style = styleData,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {

            Box(
                modifier = Modifier
                    .shadow(elevation = 2.dp, shape = Shapes.medium)
                    .background(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = Shapes.medium
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Периоды отвлечения: ",
                        style = styleData,
                        color = MaterialTheme.colorScheme.primary
                    )

                    releasePeriodListState?.let { releaseDayList ->
                        if (releaseDayList.isNotEmpty()) {

                            val _sortTz = TimeZone.currentSystemDefault()
                            val sortedReleasePeriods = releaseDayList
                                .filter { it.days.isNotEmpty() }
                                .sortedBy { it.days.minOf { day -> day.atStartOfDayIn(_sortTz).toEpochMilliseconds() } }

                            sortedReleasePeriods.forEach { period ->
                                val typeColors = mapOf(
                                    ReleaseType.Vacation to Color.Green,
                                    ReleaseType.SickLeave to Color.Red,
                                    ReleaseType.Donor to Color.Blue,
                                    ReleaseType.Courses to Color.Yellow,
                                    ReleaseType.ChildCare to Color.Magenta,
                                    ReleaseType.Other to Color.Gray
                                )

                                val periodColor: Color =
                                    period.type?.let { typeColors[it] } ?: MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)

                                if (period.days.isNotEmpty()) {
                                    HorizontalDivider()
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItem()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(periodColor)
                                            )
                                            Text(
                                                text = period.type?.text ?: "",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp, start = 16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val _displayTz = TimeZone.currentSystemDefault()
                                                Text(
                                                    text = dateAndTimeConverter?.getDateFromDateLong(
                                                        period.days.first().atStartOfDayIn(_displayTz).toEpochMilliseconds()
                                                    ) ?: "",
                                                    style = styleData,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                if (period.days.size > 1) {
                                                    period.days.last().let {
                                                        Text(
                                                            text = " - ",
                                                            style = styleData,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            text = dateAndTimeConverter?.getDateFromDateLong(
                                                                it.atStartOfDayIn(_displayTz).toEpochMilliseconds()
                                                            ) ?: "",
                                                            style = styleData,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                            Icon(
                                                modifier = Modifier.clickable {
                                                    removingReleasePeriod(period)
                                                },
                                                painter = painterResource(com.z_company.core.R.drawable.ic_clear),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
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

        item {
            FullCalendarForRelease(
                allMonthOfYear = allMonthOfYear,
                monthOfYear = monthOfYear,
                selectedStart = selectedStart,
                selectedEnd = selectedEnd,
                currentMonth = currentMonth,
                periods = releasePeriodListState ?: emptyList(),
                onDateSelected = { selectDate(it) },
                selectMonthOfYear = selectMonthOfYear
            )
        }

        item {
            if (selectedStart != null) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    shape = Shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    onClick = {
                        val list = mutableListOf<LocalDate>()
                        selectedStart?.let { startMillis ->
                            val tz = TimeZone.currentSystemDefault()
                            val startDate = Instant.fromEpochMilliseconds(startMillis).toLocalDateTime(tz).date
                            val endDate = selectedEnd?.let {
                                Instant.fromEpochMilliseconds(it).toLocalDateTime(tz).date
                            } ?: startDate
                            var current = startDate
                            while (current <= endDate) {
                                list.add(current)
                                current = current.plus(1, DateTimeUnit.DAY)
                            }
                        }
                        if (list.isNotEmpty()) {
                            pendingDays = list
                            showTypeSheet = true
                        }
                    }) {
                    Text(
                        text = "Добавить",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullCalendarForRelease(
    monthOfYear: MonthOfYear?,
    allMonthOfYear: List<MonthOfYear>,
    selectedStart: Long?,
    selectedEnd: Long?,
    currentMonth: Date,
    periods: List<ReleasePeriod>,
    onDateSelected: (Long) -> Unit,
    selectMonthOfYear: (Pair<Int, Int>) -> Unit,
) {

    val sortedMonths = remember(allMonthOfYear) {
        allMonthOfYear?.sortedBy { it.year * 100 + it.month } ?: emptyList()
    }

    if (sortedMonths.isEmpty()) {
        Text("Нет доступных месяцев")
        return
    }

    val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    // Pager setup
    @OptIn(ExperimentalFoundationApi::class)
    val initialPage = remember(monthOfYear) {
        sortedMonths.indexOfFirst { it.year == monthOfYear?.year && it.month == monthOfYear?.month }
            .takeIf { it >= 0 } ?: 0
    }

    @OptIn(ExperimentalFoundationApi::class)
    val pagerState = rememberPagerState(initialPage = initialPage) { sortedMonths.size }

    // При смене страницы обновляем месяц
    LaunchedEffect(pagerState.currentPage) {
        val newMonth = sortedMonths[pagerState.currentPage]
        selectMonthOfYear(newMonth.year to newMonth.month)
    }

    // Синхронизация с внешним monthOfYear (из диалога)
    LaunchedEffect(monthOfYear) {
        val newPage =
            sortedMonths.indexOfFirst { it.year == monthOfYear?.year && it.month == monthOfYear?.month }
        if (newPage >= 0 && newPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(newPage)
        }
    }

    val scope = rememberCoroutineScope()
    val prevMonth =
        if (pagerState.currentPage > 0) sortedMonths[pagerState.currentPage - 1] else null

    val currentPageMonth = sortedMonths[pagerState.currentPage]

    val nextMonth =
        if (pagerState.currentPage < sortedMonths.size - 1) sortedMonths[pagerState.currentPage + 1] else null


    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Навигация по месяцам
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        )
        {
            // Предыдущий
            Row(
                modifier = Modifier.clickable(enabled = pagerState.currentPage > 0) {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_left_24px),
                    contentDescription = "Предыдущий месяц",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )

                Text(
                    text = prevMonth?.let {
                        getMonthFullText(it.month) + " " + if (it.year != currentPageMonth.year) it.year else ""
                    } ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }

            // Текущий месяц
            Text(
                text = getMonthFullText(currentPageMonth.month),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )

            // Следующий
            Row(
                modifier = Modifier.clickable(enabled = pagerState.currentPage < sortedMonths.size - 1) {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = nextMonth?.let {
                        getMonthFullText(it.month) + " " + if (it.year != currentPageMonth.year) it.year else ""
                    } ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                Icon(
                    painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_right_24px),
                    contentDescription = "Следующий месяц",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Дни недели
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        val firstDayOfMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentPageMonth.year)
            set(Calendar.MONTH, currentPageMonth.month)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        @OptIn(ExperimentalFoundationApi::class)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val thisMonth = sortedMonths[page]

            val monthCal = Calendar.getInstance().also {
                it.set(Calendar.YEAR, thisMonth.year)
                it.set(Calendar.MONTH, thisMonth.month)
                it.set(Calendar.DAY_OF_MONTH, 1)
            }

            val firstDayOfWeek = (firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7
            val daysInMonth = firstDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
            val weeks = (firstDayOfWeek + daysInMonth + 6) / 7

            val todayDayOfMonth = remember(thisMonth) {
                val now = Calendar.getInstance()
                if (now.get(Calendar.YEAR) == thisMonth.year && now.get(Calendar.MONTH) == thisMonth.month)
                    now.get(Calendar.DAY_OF_MONTH) else -1
            }
            val todayCircleColor = MaterialTheme.colorScheme.surfaceContainerLow

            Column {
                for (week in 0 until weeks) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (dayOfWeek in 0 until 7) {
                            val dayNumber = week * 7 + dayOfWeek - firstDayOfWeek + 1
                            if (dayNumber in 1..daysInMonth) {
                                val dayCal = Calendar.getInstance().apply {
                                    time = monthCal.time
                                    set(Calendar.DAY_OF_MONTH, dayNumber)
                                }

                                val dateMillis = dayCal.timeInMillis

                                val isInSelectedRange =
                                    isDateInRange(dateMillis, selectedStart, selectedEnd, false)

                                val isStart =
                                    selectedStart?.let { isSameDay(dateMillis, it) } == true
                                val isEnd = (selectedEnd ?: selectedStart)?.let {
                                    isSameDay(
                                        dateMillis,
                                        it
                                    )
                                } == true

                                val isIntermediate =
                                    isInSelectedRange && !isStart && !isEnd

                                val highlightColor = MaterialTheme.colorScheme.tertiary
                                val intermediateColor = highlightColor.copy(alpha = 0.5f)

                                // Find if day is in any period

                                val typeColors = mapOf(
                                    ReleaseType.Vacation to Color.Green,
                                    ReleaseType.SickLeave to Color.Red,
                                    ReleaseType.Donor to Color.Blue,
                                    ReleaseType.Courses to Color.Yellow,
                                    ReleaseType.ChildCare to Color.Magenta,
                                    ReleaseType.Other to Color.Gray
                                )

                                val _calTz = TimeZone.currentSystemDefault()
                                val period = periods.firstOrNull { period ->
                                    period.days.any { isSameDay(it.atStartOfDayIn(_calTz).toEpochMilliseconds(), dateMillis) }
                                }

                                val periodColor: Color? = period?.type?.let { typeColors[it] }
                                    ?: if (period != null) Color.Cyan else null

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clickable { onDateSelected(dateMillis) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isStart || isEnd) {
                                        Box(
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .matchParentSize()
                                                .clip(CircleShape)
                                                .background(highlightColor)
                                        )
                                    } else if (isIntermediate) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(24.dp)
                                                .background(intermediateColor)
                                                .align(Alignment.Center)
                                        )
                                    }

                                    if (dayNumber == todayDayOfMonth) {
                                        Box(
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .matchParentSize()
                                                .border(
                                                    width = 2.dp,
                                                    color = todayCircleColor,
                                                    shape = CircleShape
                                                )
                                        )
                                    }

                                    Text(
                                        text = dayNumber.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isInSelectedRange) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                    )

                                    periodColor?.let { color ->
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .padding(horizontal = 2.dp)
                                                .background(color)
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

fun isSameDay(time1: Long, time2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply {
        timeInMillis = time1
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val cal2 = Calendar.getInstance().apply {
        timeInMillis = time2
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal1.timeInMillis == cal2.timeInMillis
}

fun isDateInRange(date: Long, start: Long?, end: Long?, isSingleMode: Boolean): Boolean {
    if (start == null) return false
    if (isSingleMode) return isSameDay(date, start)
    val effectiveEnd = end ?: start
    val min = minOf(start, effectiveEnd)
    val max = maxOf(start, effectiveEnd)
    val dateDay = Calendar.getInstance().apply {
        timeInMillis = date; set(Calendar.HOUR_OF_DAY, 0); set(
        Calendar.MINUTE,
        0
    ); set(
        Calendar.SECOND,
        0
    ); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val minDay = Calendar.getInstance().apply {
        timeInMillis = min; set(Calendar.HOUR_OF_DAY, 0); set(
        Calendar.MINUTE,
        0
    ); set(
        Calendar.SECOND,
        0
    ); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val maxDay = Calendar.getInstance().apply {
        timeInMillis = max; set(Calendar.HOUR_OF_DAY, 0); set(
        Calendar.MINUTE,
        0
    ); set(
        Calendar.SECOND,
        0
    ); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return dateDay >= minDay && dateDay <= maxDay
}