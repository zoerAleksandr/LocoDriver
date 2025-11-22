package com.z_company.core.ui.component

import android.annotation.SuppressLint
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
    startDate: Long = Calendar.getInstance().timeInMillis,
    title: String = "",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val viewModel = remember { DateRangePickerViewModel(initialTimestamp = startDate) }
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
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            // Заголовок
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

            // переключатель календаря
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
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Календарь
            item {
                AnimatedContent(
                    targetState = uiState.isCompactCalendar,
                    label = "calendar_animation",
                    transitionSpec = {
                        fadeIn() + expandVertically() togetherWith
                                fadeOut() + shrinkVertically()
                    }
                ) { isCompact ->
                    if (isCompact) {
                        CompactCalendarForRange(
                            startDate = uiState.startDate,
                            endDate = uiState.endDate,
                            currentMonth = uiState.currentMonth,
                            onDateSelected = { viewModel.selectDate(it) }
                        )
                    } else {
                        FullCalendarForRange(
                            startDate = uiState.startDate,
                            endDate = uiState.endDate,
                            onDateSelected = { viewModel.selectDate(it) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }

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
                    if (uiState.startDate != null && uiState.endDate != null) {
                        Text(
                            text = "${dateFormat.format(uiState.startDate)} - ${dateFormat.format(uiState.endDate)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (uiState.startDate != null) {
                        Text(
                            text = dateFormat.format(uiState.startDate),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "Выберите дату",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Кнопка применить
            item {
                Button(
                    onClick = {
                        val selectedDates = mutableListOf<Calendar>()
                        uiState.startDate?.let { start ->
                            val startCal = Calendar.getInstance().apply { timeInMillis = start }
                            if (uiState.endDate == null) {
                                selectedDates.add(startCal)
                            } else {
                                val endCal = Calendar.getInstance().apply { timeInMillis =
                                    uiState.endDate!!
                                }
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
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 3.dp
                    )
                ) {
                    Text(text = "Применить", style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CompactCalendarForRange(
    startDate: Long?,
    endDate: Long?,
    currentMonth: Date,
    onDateSelected: (Long) -> Unit
) {
    val selectedDate = startDate ?: endDate ?: System.currentTimeMillis()
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
    val selectedDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

    val monthCal = Calendar.getInstance().apply { time = currentMonth }
    val month = monthCal.get(Calendar.MONTH)
    val year = monthCal.get(Calendar.YEAR)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val daysOfWeek = listOf(
            "Пн",
            "Вт",
            "Ср",
            "Чт",
            "Пт",
            "Сб",
            "Вс"
        )

        daysOfWeek.forEachIndexed { index, day ->
            val offset = (selectedDayOfWeek - 2 + 7) % 7
            val currentDateCal = Calendar.getInstance().apply {
                timeInMillis = selectedDate
                add(Calendar.DAY_OF_MONTH, index - offset)
            }
            val currentDate = currentDateCal.timeInMillis
            val dayNumber = currentDateCal.get(Calendar.DAY_OF_MONTH)
            val isInCurrentMonth = currentDateCal.get(Calendar.MONTH) == month && currentDateCal.get(Calendar.YEAR) == year
            val isInRange = isDateInRange(currentDate, startDate, endDate)
            val isStart = currentDate == startDate
            val isEnd = currentDate == endDate
            val isIntermediate = isInRange && !isStart && !isEnd

            val highlightColor = MaterialTheme.colorScheme.tertiary
            val intermediateColor = highlightColor.copy(alpha = 0.5f)

            if (isInCurrentMonth) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(Color.Transparent)
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = if (isInRange) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.6f
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(
                                RoundedCornerShape(
                                    bottomStart = 12.dp,
                                    bottomEnd = 12.dp
                                )
                            )
                            .background(Color.Transparent)
                            .noRippleEffect { onDateSelected(currentDate) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isStart || isEnd) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(CircleShape)
                                    .background(highlightColor)
                            )
                        } else if (isIntermediate) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .height(8.dp)
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
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    }
}

@Composable
fun FullCalendarForRange(
    startDate: Long?,
    endDate: Long?,
    onDateSelected: (Long) -> Unit
) {
    val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    // Форматтеры для названий месяцев
    val monthNameFormat = SimpleDateFormat("LLLL", Locale("ru"))
    val yearFormat = SimpleDateFormat("yyyy", Locale("ru"))
    var currentMonth by remember {
        mutableStateOf(
            Calendar.getInstance().apply { timeInMillis = startDate ?: System.currentTimeMillis() })
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Навигация по месяцам
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка предыдущего месяца
            val prevMonth = Calendar.getInstance().apply {
                time = currentMonth.time
                add(Calendar.MONTH, -1)
            }

            Row(
                modifier = Modifier.clickable {
                    currentMonth = prevMonth
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Предыдущий месяц",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Text(
                    text = "${monthNameFormat.format(prevMonth.time).capitalize(Locale.ROOT)} " +
                            (if (prevMonth.get(Calendar.YEAR) != currentMonth.get(Calendar.YEAR))
                                yearFormat.format(prevMonth.time)
                            else ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }

            // Текущий месяц
            Text(
                text = monthNameFormat.format(currentMonth.time).capitalize(Locale.ROOT),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )

            // Кнопка следующего месяца
            val nextMonth = Calendar.getInstance().apply {
                time = currentMonth.time
                add(Calendar.MONTH, 1)
            }

            Row(
                modifier = Modifier.clickable {
                    currentMonth = nextMonth
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${monthNameFormat.format(nextMonth.time).capitalize()} " +
                            (if (nextMonth.get(Calendar.YEAR) != currentMonth.get(Calendar.YEAR))
                                yearFormat.format(nextMonth.time)
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
        // Дни месяца
        val firstDayOfMonth = Calendar.getInstance().apply {
            time = currentMonth.time
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOfWeek = (firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7
        val daysInMonth = firstDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        val weeks = (firstDayOfWeek + daysInMonth + 6) / 7

        // Анимированная смена месяца
        AnimatedContent(
            targetState = currentMonth,
            label = "month_change",
            transitionSpec = {
                val direction = if (targetState.time > initialState.time) {
                    AnimatedContentTransitionScope.SlideDirection.Left
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Right
                }

                slideIntoContainer(
                    towards = direction,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) togetherWith
                        slideOutOfContainer(
                            towards = direction,
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        )
            }
        ) { monthCalendar ->
            Column {
                for (week in 0 until weeks) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (dayOfWeek in 0 until 7) {
                            val dayNumber = week * 7 + dayOfWeek - firstDayOfWeek + 1
                            if (dayNumber in 1..daysInMonth) {
                                val dayCalendar = Calendar.getInstance().apply {
                                    time = monthCalendar.time
                                    set(Calendar.DAY_OF_MONTH, dayNumber)
                                }
                                val currentDate = dayCalendar.timeInMillis
                                val isInRange = isDateInRange(currentDate, startDate, endDate)
                                val isStart = currentDate == startDate
                                val isEnd = currentDate == endDate
                                val isIntermediate = isInRange && !isStart && !isEnd

                                val highlightColor = MaterialTheme.colorScheme.tertiary
                                val intermediateColor = highlightColor.copy(alpha = 0.5f)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clickable { onDateSelected(currentDate) },
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
                                                .fillMaxHeight(0.5f)
                                                .background(intermediateColor)
                                                .align(Alignment.Center)
                                        )
                                    }
                                    Text(
                                        text = dayNumber.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isInRange)
                                            MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.primary
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

private fun isDateInRange(date: Long, start: Long?, end: Long?): Boolean {
    if (start == null) return false
    val effectiveEnd = end ?: start
    val min = minOf(start, effectiveEnd)
    val max = maxOf(start, effectiveEnd)
    return date >= min && date <= max
}

class DateRangePickerViewModel(initialTimestamp: Long? = null) {
    private val initialCalendar =
        Calendar.getInstance().apply { initialTimestamp?.let { timeInMillis = it } }

    private val _uiState = MutableStateFlow(
        DateRangePickerState(
            startDate = null,
            endDate = null,
            currentMonth = initialCalendar.time,
            isCompactCalendar = true
        )
    )
    val uiState: StateFlow<DateRangePickerState> = _uiState

    fun selectDate(long: Long) {
        val state = _uiState.value
        if (state.startDate == null) {
            _uiState.value = state.copy(startDate = long)
        } else if (state.endDate == null) {
            if (long > state.startDate) {
                _uiState.value = state.copy(endDate = long)
            } else {
                _uiState.value = state.copy(startDate = long, endDate = null)
            }
        } else {
            // Сброс и новый выбор
            _uiState.value = state.copy(startDate = long, endDate = null)
        }
    }

    fun toggleCalendarView() {
        _uiState.value =
            _uiState.value.copy(isCompactCalendar = !_uiState.value.isCompactCalendar)
    }
}

data class DateRangePickerState(
    val startDate: Long? = null,
    val endDate: Long? = null,
    val currentMonth: Date = Date(),
    val isCompactCalendar: Boolean = true
)