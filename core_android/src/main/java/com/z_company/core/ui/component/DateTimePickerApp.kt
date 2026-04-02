package com.z_company.core.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.R
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.theme.Shapes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.getValue

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
//@OptIn(ExperimentalLayoutApi::class)
fun DateTimePickerBottomSheet(
    onDateTimeSelected: (Long) -> Unit, onDismiss: () -> Unit,
    startDateTime: Long?,
    title: String = "",
    recentTimes: List<Long> = emptyList(),
    onRecentTimeSaved: ((Long) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val viewModel = remember { DateTimePickerViewModel(initialTimestamp = startDateTime) }
    val uiState by viewModel.uiState.collectAsState()
    val timeZoneStr by viewModel.timeZone.collectAsState()

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
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
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
                            modifier = Modifier.clickable { viewModel.toggleCalendarView() })
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
                            CompactCalendar(
                                selectedDate = uiState.selectedDate,
                                onDateSelected = { viewModel.selectDate(it) },
                                timeZoneStr = timeZoneStr
                            )

                        } else {
                            FullCalendar(
                                selectedDate = uiState.selectedDate,
                                onDateSelected = { viewModel.selectDate(it) },
                                timeZoneStr = timeZoneStr
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Время
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Всегда отображаем ScrollPicker
                        TimeScrollPicker(
                            currentHour = uiState.hour,
                            currentMinute = uiState.minute,
                            isEditing = uiState.isEditingTime,
                            onHourChange = { viewModel.setHour(it) },
                            onMinuteChange = { viewModel.setMinute(it) },
                            onChangeEditTime = viewModel::toggleEditMode
                        )
                        // Overlay для ввода времени
                        if (uiState.isEditingTime) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(
                                        onClick = { viewModel.toggleEditMode() },
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    )
                            )
                            TimeInputOverlay(
                                onHourChange = { viewModel.setHour(it) },
                                onMinuteChange = { viewModel.setMinute(it) },
                                onDone = { viewModel.toggleEditMode() },
                            )
                        }
                    }
                }

                // Быстрый выбор из последних значений
                if (recentTimes.isNotEmpty()) {
                    item {
                        val chips = recentTimes.take(10).map { timeMillis ->
                            val cal = Calendar.getInstance(TimeZone.getTimeZone(timeZoneStr)).apply {
                                this.timeInMillis = timeMillis
                            }
                            Triple(timeMillis, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                        }.distinctBy { (_, h, m) -> h * 60 + m }

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(chips.size) { index ->
                                val (_, h, m) = chips[index]
                                SuggestionChip(
                                    onClick = {
                                        viewModel.setTime(h, m)
                                    },
                                    label = {
                                        Text(
                                            text = "%02d:%02d".format(h, m),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            ),
                                            maxLines = 1
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.3f),
                                        labelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(if (recentTimes.isEmpty()) 48.dp else 8.dp))
                }

                // Дата и время внизу
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = SimpleDateFormat("d MMMM yyyy", Locale("ru")).apply {
                                timeZone = TimeZone.getTimeZone(timeZoneStr)
                            }
                                .format(uiState.selectedDate),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = String.format("%02d:%02d", uiState.hour, uiState.minute),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                // Кнопка выбрать
                item {
                    Button(
                        onClick = {
                            val calendar =
                                Calendar.getInstance(TimeZone.getTimeZone(timeZoneStr)).apply {
                                    timeInMillis = uiState.selectedDate
                                    set(Calendar.HOUR_OF_DAY, uiState.hour)
                                    set(Calendar.MINUTE, uiState.minute)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                            val resultMillis = calendar.timeInMillis
                            onRecentTimeSaved?.invoke(resultMillis)
                            onDateTimeSelected(resultMillis)
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
                        Text(text = "Выбрать", style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun CompactCalendar(selectedDate: Long, onDateSelected: (Long) -> Unit, timeZoneStr: String) {
    val calendar = Calendar.getInstance(
        TimeZone.getTimeZone(timeZoneStr)
    ).apply {
        timeInMillis = selectedDate
    }
    val selectedDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

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
            val adjustedIndex = (index + 1) % 7 + 1
//            val dayNumber =
//                calendar.get(Calendar.DAY_OF_MONTH) - ((selectedDayOfWeek - 2 + 7) % 7) + index
            val isSelectedDay =
                selectedDayOfWeek == adjustedIndex

//            var currentMonth by remember {
//                mutableStateOf(
//                    Calendar.getInstance(TimeZone.getTimeZone(timeZoneStr)).apply { timeInMillis = selectedDate })
//            }

//            val firstDayOfMonth = Calendar.getInstance(TimeZone.getTimeZone(timeZoneStr)).apply {
//                time = currentMonth.time
//                set(Calendar.DAY_OF_MONTH, 1)
//            }
//            val firstDayOfWeek = (firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7
            val monthCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
            val month = monthCal.get(Calendar.MONTH)
            val year = monthCal.get(Calendar.YEAR)

//            val daysInMonth = firstDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
            val offset = (selectedDayOfWeek - 2 + 7) % 7
            val currentDateCal = Calendar.getInstance().apply {
                timeInMillis = selectedDate
                add(Calendar.DAY_OF_MONTH, index - offset)
            }

            val isCurrentMonthDay =
                currentDateCal.get(Calendar.MONTH) == month && currentDateCal.get(Calendar.YEAR) == year

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(if (isSelectedDay) MaterialTheme.colorScheme.tertiary else Color.Transparent)
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = if (isSelectedDay) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.6f
                        )
                    )
                }
//                if (dayNumber in 1..daysInMonth) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(
                                RoundedCornerShape(
                                    bottomStart = 12.dp,
                                    bottomEnd = 12.dp
                                )
                            )
                            .background(if (isSelectedDay) MaterialTheme.colorScheme.tertiary else Color.Transparent)
                            .noRippleEffect {
                                val newCalendar = Calendar.getInstance(TimeZone.getTimeZone(timeZoneStr)).apply {
                                    timeInMillis = selectedDate
                                    add(
                                        Calendar.DAY_OF_MONTH,
                                        index - ((selectedDayOfWeek - 2 + 7) % 7)
                                    )
                                }
                                onDateSelected(newCalendar.timeInMillis)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentDateCal.get(Calendar.DAY_OF_MONTH).toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelectedDay) MaterialTheme.colorScheme.secondary
                            else if (isCurrentMonthDay) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    }
//                } else {
//                    Spacer(modifier = Modifier.weight(1f))
//                }
            }
        }
    }
}

@Composable
fun FullCalendar(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    timeZoneStr: String
) {
    val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    // Форматтеры для названий месяцев
    val monthNameFormat = SimpleDateFormat("LLLL", Locale("ru"))
    val yearFormat = SimpleDateFormat("yyyy", Locale("ru"))

    var currentMonth by remember {
        mutableStateOf(
            Calendar.getInstance(TimeZone.getTimeZone(timeZoneStr)).apply { timeInMillis = selectedDate })
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
                    painter = painterResource(R.drawable.keyboard_arrow_left_24px),

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
                    painter = painterResource(R.drawable.keyboard_arrow_right_24px),
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
        val firstDayOfMonth = Calendar.getInstance(TimeZone.getTimeZone(timeZoneStr)).apply {
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
                                val dayCalendar = Calendar.getInstance(TimeZone.getTimeZone(timeZoneStr)).apply {
                                    time = monthCalendar.time
                                    set(Calendar.DAY_OF_MONTH, dayNumber)
                                }
                                val isSelected =
                                    Calendar.getInstance(TimeZone.getTimeZone(timeZoneStr)).apply { timeInMillis = selectedDate }
                                        .let {
                                            it.get(Calendar.DAY_OF_MONTH) == dayNumber &&
                                                    it.get(Calendar.MONTH) == monthCalendar.get(
                                                Calendar.MONTH
                                            ) &&
                                                    it.get(Calendar.YEAR) == monthCalendar.get(
                                                Calendar.YEAR
                                            )
                                        }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.tertiary
                                            else Color.Transparent
                                        )
                                        .clickable { onDateSelected(dayCalendar.timeInMillis) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected)
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

class DateTimePickerViewModel(initialTimestamp: Long? = null) : ViewModel() {

    private val _timeZone = MutableStateFlow<String>("GMT+3")
    val timeZone: StateFlow<String> = _timeZone

    private val initialCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+3"))
        .apply { initialTimestamp?.let { timeInMillis = it } }

    private val _uiState = MutableStateFlow(
        DateTimePickerState(
            selectedDate = initialCalendar.timeInMillis,
            currentMonth = initialCalendar.time,
            hour = initialCalendar.get(Calendar.HOUR_OF_DAY),
            minute = initialCalendar.get(Calendar.MINUTE),
            isLoading = false
        )
    )
    val uiState: StateFlow<DateTimePickerState> = _uiState

    fun selectDate(long: Long) {
        _uiState.value = _uiState.value.copy(selectedDate = long)
    }

    fun setHour(hour: Int) {
        _uiState.value = _uiState.value.copy(hour = hour)
    }

    fun setMinute(minute: Int) {
        _uiState.value = _uiState.value.copy(minute = minute)
    }

    fun setTime(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(hour = hour, minute = minute)
    }

    fun toggleEditMode() {
        _uiState.value =
            _uiState.value.copy(isEditingTime = !_uiState.value.isEditingTime)
    }

    fun toggleCalendarView() {
        _uiState.value =
            _uiState.value.copy(isCompactCalendar = !_uiState.value.isCompactCalendar)
    }
}

data class DateTimePickerState(
    val isLoading: Boolean = true,
    val selectedDate: Long = 0L,
    val currentMonth: Date = Date(),
    val hour: Int = 3,
    val minute: Int = 50,
    val isEditingTime: Boolean = false,
    val isCompactCalendar: Boolean = true
)