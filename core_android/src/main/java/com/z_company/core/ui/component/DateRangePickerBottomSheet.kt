package com.z_company.core.ui.component

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.theme.Shapes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerBottomSheet(
    onDateRangeSelected: (MutableList<Calendar>) -> Unit,
    onDismiss: () -> Unit,
    startDate: Long = System.currentTimeMillis(),
    title: String = "",
    singleMode: Boolean = true
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val viewModel =
        remember { DateRangePickerViewModel(initialTimestamp = startDate, singleMode = singleMode) }
    val uiState by viewModel.uiState.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.secondary
    ) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Заголовок
            if (title.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Переключатели вида и режима
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = if (uiState.isCompactCalendar) "Неделя" else "Месяц",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.clickable { viewModel.toggleCalendarView() }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Календарь
            item {
                AnimatedContent(
                    targetState = uiState.isCompactCalendar,
                    label = "calendar_animation",
                    transitionSpec = {
                        fadeIn() + expandVertically() togetherWith fadeOut() + shrinkVertically()
                    }
                ) { isCompact ->
                    if (isCompact) {
                        CompactCalendarForRange(
                            startDate = uiState.startDate,
                            endDate = uiState.endDate,
                            currentMonth = uiState.currentMonth,
                            isSingleMode = uiState.isSingleMode,
                            onDateSelected = { viewModel.selectDate(it) }
                        )
                    } else {
                        FullCalendarForRange(
                            startDate = uiState.startDate,
                            endDate = uiState.endDate,
                            currentMonth = uiState.currentMonth,
                            isSingleMode = uiState.isSingleMode,
                            onDateSelected = { viewModel.selectDate(it) },
                            onMonthChange = { viewModel.setCurrentMonth(it) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            // Выбранный период внизу
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))

                    val displayedText = when {
                        uiState.startDate == null -> {
                            "Выберите ${if (uiState.isSingleMode) "дату" else "период"}"
                        }

                        uiState.endDate == null -> {
                            dateFormat.format(uiState.startDate!!)
                        }

                        else -> {
                            "${dateFormat.format(uiState.startDate!!)} - ${dateFormat.format(uiState.endDate!!)}"
                        }
                    }

                    Text(
                        text = displayedText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Кнопка применить
            item {
                Button(
                    onClick = {
                        val selectedDates = mutableListOf<Calendar>()
                        uiState.startDate?.let { startMillis ->
                            val startCal =
                                Calendar.getInstance().apply { timeInMillis = startMillis }
                            if (uiState.isSingleMode || uiState.endDate == null) {
                                selectedDates.add(startCal)
                            } else {
                                val endCal = Calendar.getInstance()
                                    .apply { timeInMillis = uiState.endDate!! }
                                var current = startCal.clone() as Calendar
                                while (!current.after(endCal)) {
                                    selectedDates.add(current.clone() as Calendar)
                                    current.add(Calendar.DAY_OF_MONTH, 1)
                                }
                            }
                        }
                        onDateRangeSelected(selectedDates)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = Shapes.medium,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    Text(text = "Применить", style = MaterialTheme.typography.bodySmall)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun CompactCalendarForRange(
    startDate: Long?,
    endDate: Long?,
    currentMonth: Date,
    isSingleMode: Boolean,
    onDateSelected: (Long) -> Unit
) {
    val monthCal = Calendar.getInstance().apply { time = currentMonth }
    val month = monthCal.get(Calendar.MONTH)
    val year = monthCal.get(Calendar.YEAR)

    val selectedDateMillis = startDate ?: endDate ?: System.currentTimeMillis()
    val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    val selectedDayOfWeek = selectedCal.get(Calendar.DAY_OF_WEEK)

    // Смещение, чтобы понедельник был первым
    val offset = (selectedDayOfWeek - 2 + 7) % 7

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

        daysOfWeek.forEachIndexed { index, dayName ->
            val currentDateCal = Calendar.getInstance().apply {
                timeInMillis = selectedDateMillis
                add(Calendar.DAY_OF_MONTH, index - offset)
            }
            val currentDate = currentDateCal.timeInMillis
            val isCurrentMonthDay =
                currentDateCal.get(Calendar.MONTH) == month && currentDateCal.get(Calendar.YEAR) == year

            val isInRange = isDateInRange(currentDate, startDate, endDate, isSingleMode)
            val isStart = currentDate == startDate
            val isEnd = currentDate == (endDate ?: startDate)
            val isIntermediate = isInRange && !isStart && !isEnd && !isSingleMode

            val highlightColor = MaterialTheme.colorScheme.tertiary
            val intermediateColor = highlightColor.copy(alpha = 0.5f)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Название дня недели
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(Color.Transparent)
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isInRange) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }

                // Число
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .background(Color.Transparent)
                        .noRippleEffect { onDateSelected(currentDate) },
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

                    Text(
                        text = currentDateCal.get(Calendar.DAY_OF_MONTH).toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isInRange) MaterialTheme.colorScheme.secondary
                        else if (isCurrentMonthDay) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun FullCalendarForRange(
    startDate: Long?,
    endDate: Long?,
    currentMonth: Date,
    isSingleMode: Boolean,
    onDateSelected: (Long) -> Unit,
    onMonthChange: (Date) -> Unit
) {
    val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val monthNameFormat = SimpleDateFormat("LLLL", Locale("ru"))
    val yearFormat = SimpleDateFormat("yyyy", Locale("ru"))

    var displayedMonth by remember {
        mutableStateOf(
            Calendar.getInstance().apply { time = currentMonth })
    }

    // Синхронизация с внешним currentMonth
    LaunchedEffect(currentMonth) {
        displayedMonth = Calendar.getInstance().apply { time = currentMonth }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Навигация по месяцам
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Предыдущий
            val prev = Calendar.getInstance().apply {
                time = displayedMonth.time
                add(Calendar.MONTH, -1)
            }
            Row(
                modifier = Modifier.clickable {
                    displayedMonth = prev
                    onMonthChange(prev.time)
                },
                verticalAlignment = Alignment.Bottom
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Предыдущий месяц",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Text(
                    text = "${monthNameFormat.format(prev.time).capitalize(Locale.ROOT)} " +
                            (if (prev.get(Calendar.YEAR) != (currentMonth.year + 1900))
                                yearFormat.format(prev.time)
                            else ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }

            // Текущий месяц
            Text(
                text = monthNameFormat.format(displayedMonth.time)
                    .replaceFirstChar { it.lowercase() },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )

            // Следующий
            val next = Calendar.getInstance().apply {
                time = displayedMonth.time
                add(Calendar.MONTH, 1)
            }
            Row(
                modifier = Modifier.clickable {
                    displayedMonth = next
                    onMonthChange(next.time)
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${monthNameFormat.format(next.time).capitalize()} " +
                            (if (next.get(Calendar.YEAR) != (currentMonth.year + 1900))
                                yearFormat.format(next.time)
                            else ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
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
            time = displayedMonth.time
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOfWeek = (firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7
        val daysInMonth = firstDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        val weeks = (firstDayOfWeek + daysInMonth + 6) / 7

        AnimatedContent(
            targetState = displayedMonth,
            transitionSpec = {
                val direction =
                    if (targetState.time > initialState.time) AnimatedContentTransitionScope.SlideDirection.Left
                    else AnimatedContentTransitionScope.SlideDirection.Right

                slideIntoContainer(
                    towards = direction,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) togetherWith
                        slideOutOfContainer(
                            towards = direction,
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        )
            },
            label = "month_change"
        ) { monthCal ->
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

                                val isInRange =
                                    isDateInRange(dateMillis, startDate, endDate, isSingleMode)
                                val isStart = dateMillis == startDate
                                val isEnd = dateMillis == (endDate ?: startDate)
                                val isIntermediate =
                                    isInRange && !isStart && !isEnd && !isSingleMode

                                val highlightColor = MaterialTheme.colorScheme.tertiary
                                val intermediateColor = highlightColor.copy(alpha = 0.5f)

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

                                    Text(
                                        text = dayNumber.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isInRange) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                    )
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

private fun isDateInRange(date: Long, start: Long?, end: Long?, isSingleMode: Boolean): Boolean {
    if (start == null) return false
    if (isSingleMode) return date == start
    val effectiveEnd = end ?: start
    val min = minOf(start, effectiveEnd)
    val max = maxOf(start, effectiveEnd)
    return date >= min && date <= max
}

class DateRangePickerViewModel(initialTimestamp: Long? = null, singleMode: Boolean) {
    private val initialCal = Calendar.getInstance().apply {
        timeInMillis = initialTimestamp ?: System.currentTimeMillis()
//        initialTimestamp?.let {
//        } ?: timeInMillis = System.currentTimeMillis()
    }

    private val _uiState = MutableStateFlow(
        DateRangePickerState(
            startDate = null,
            endDate = null,
            currentMonth = initialCal.time,
            isCompactCalendar = false,
            isSingleMode = singleMode
        )
    )
    val uiState: StateFlow<DateRangePickerState> = _uiState

    fun selectDate(millis: Long) {
        val state = _uiState.value

        val newStart: Long?
        val newEnd: Long?

        if (state.isSingleMode) {
            newStart = millis
            newEnd = null
        } else {
            newStart = when {
                state.startDate == null -> millis
                state.endDate == null -> if (millis > state.startDate!!) state.startDate!! else millis
                else -> millis
            }
            newEnd = when {
                state.startDate == null -> null
                state.endDate == null -> if (millis > state.startDate!!) millis else null
                else -> null
            }
        }

        val tempState = state.copy(startDate = newStart, endDate = newEnd)
        _uiState.value = tempState

        // Автоматически переключаем месяц на выбранную дату
        val lastSelected = newEnd ?: newStart ?: millis
        val selectedCal = Calendar.getInstance().apply { timeInMillis = lastSelected }
        val currentCal = Calendar.getInstance().apply { time = tempState.currentMonth }

        if (selectedCal.get(Calendar.YEAR) != currentCal.get(Calendar.YEAR) ||
            selectedCal.get(Calendar.MONTH) != currentCal.get(Calendar.MONTH)
        ) {
            _uiState.value = tempState.copy(currentMonth = selectedCal.time)
        }
    }

//    fun toggleSingleMode() {
//        val state = _uiState.value
//        _uiState.value = state.copy(
//            isSingleMode = !state.isSingleMode,
//            endDate = if (!state.isSingleMode) null else state.endDate // переходим в single → сбрасываем end
//        )
//    }

    fun toggleCalendarView() {
        _uiState.value = _uiState.value.copy(isCompactCalendar = !_uiState.value.isCompactCalendar)
    }

    fun setCurrentMonth(month: Date) {
        _uiState.value = _uiState.value.copy(currentMonth = month)
    }
}

data class DateRangePickerState(
    val startDate: Long? = null,
    val endDate: Long? = null,
    val currentMonth: Date = Date(),
    val isCompactCalendar: Boolean = false,
    val isSingleMode: Boolean = false
)