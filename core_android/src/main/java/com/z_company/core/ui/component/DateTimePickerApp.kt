package com.z_company.core.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.component.customDatePicker.Hour
import com.z_company.core.ui.component.customDatePicker.MAX
import com.z_company.core.ui.component.customDatePicker.MIN
import com.z_company.core.ui.component.customDatePicker.Minute
import com.z_company.core.ui.component.customDatePicker.MyWheelTextPicker
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.component.customDatePicker.now
import com.z_company.core.ui.component.customDatePicker.truncateTo
import com.z_company.core.ui.component.customDatePicker.withHour
import com.z_company.core.ui.component.customDatePicker.withMinute
import com.z_company.core.ui.theme.Shapes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerBottomSheet(
    onDateTimeSelected: (Long) -> Unit, onDismiss: () -> Unit,
    startDateTime: LocalDateTime = LocalDateTime.now(),
    initialTimestamp: Long?,
    title: String = "",
//    minDateTime: LocalDateTime = LocalDateTime.MIN(),
//    maxDateTime: LocalDateTime = LocalDateTime.MAX(),
//    onSnappedDateTime: (snappedDateTime: MySnappedDateTime) -> Int? = { _ -> null }
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val viewModel = remember { DateTimePickerViewModel(initialTimestamp = initialTimestamp) }
    val uiState by viewModel.uiState.collectAsState()

    var snappedDateTime by remember { mutableStateOf(startDateTime.truncateTo(DateTimeUnit.MINUTE)) }

    ModalBottomSheet(
        onDismissRequest =
            onDismiss, sheetState = sheetState, dragHandle = {
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
        }, containerColor = MaterialTheme.colorScheme.secondary
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(16.dp))
            // Заголовок с переключателем календаря
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            )
            {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (uiState.isCompactCalendar) "Неделя" else "Месяц",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.clickable { viewModel.toggleCalendarView() })
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Календарь
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
                        onDateSelected = { viewModel.selectDate(it) })
                } else {
                    FullCalendar(
                        selectedDate = uiState.selectedDate,
                        currentMonth = uiState.currentMonth,
                        onDateSelected = { viewModel.selectDate(it) })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Время
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Всегда отображаем ScrollPicker
                TimeScrollPicker(
                    startTime = uiState.selectedDate.toLocalDateTime().time,
                    onHourChange = { viewModel.setHour(it) },
                    onMinuteChange = { viewModel.setMinute(it) },
                )
                // Overlay для ввода времени
                if (uiState.isEditingTime) {
                    TimeInputOverlay(
                        hour = uiState.hour,
                        minute = uiState.minute,
                        onHourChange = { viewModel.setHour(it) },
                        onMinuteChange = { viewModel.setMinute(it) },
                        onDone = { viewModel.toggleEditMode() })
                }
            }
            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )
            // Дата и время внизу
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
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
            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка выбрать
            Button(
                onClick = {
                    val calendar = Calendar.getInstance().apply {
                        time = uiState.selectedDate
                        set(Calendar.HOUR_OF_DAY, uiState.hour)
                        set(Calendar.MINUTE, uiState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onDateTimeSelected(calendar.timeInMillis)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.secondary
                ),
                shape = Shapes.medium
            ) {
                Text(text = "Выбрать", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CompactCalendar(selectedDate: Date, onDateSelected: (Date) -> Unit) {
    val calendar = Calendar.getInstance().apply { time = selectedDate }
    val selectedDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val selectedDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

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
            val dayNumber =
                calendar.get(Calendar.DAY_OF_MONTH) - ((selectedDayOfWeek - 2 + 7) % 7) + index
            val isSelectedDay =
                selectedDayOfWeek == adjustedIndex
            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(if (isSelectedDay) MaterialTheme.colorScheme.tertiary else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        fontSize = 14.sp,
                        color = if (isSelectedDay) MaterialTheme.colorScheme.secondary else Color.Gray
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
                        .background(if (isSelectedDay) MaterialTheme.colorScheme.tertiary else Color.Transparent)
                        .noRippleEffect {
                            val newCalendar = Calendar.getInstance().apply {
                                time = selectedDate
                                add(
                                    Calendar.DAY_OF_MONTH,
                                    index - ((selectedDayOfWeek - 2 + 7) % 7)
                                )
                            }
                            onDateSelected(newCalendar.time)
                        }, contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayNumber.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelectedDay) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun FullCalendar(
    selectedDate: Date,
    currentMonth: Date,
    onDateSelected: (Date) -> Unit
) {
    val calendar = Calendar.getInstance().apply { time = currentMonth }
    val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    Column(modifier = Modifier.padding(horizontal = 16.dp)) { // Дни недели
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(
            modifier =
                Modifier.height(16.dp)
        )
        // Дни месяца
        val firstDayOfMonth = Calendar.getInstance().apply {
            time = currentMonth
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOfWeek = (firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7
        val daysInMonth = firstDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        val weeks = (firstDayOfWeek + daysInMonth + 6) / 7
        for (week in 0 until weeks) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (dayOfWeek in 0 until 7) {
                    val dayNumber =
                        week * 7 + dayOfWeek - firstDayOfWeek + 1
                    if (dayNumber in 1..daysInMonth) {
                        val dayCalendar = Calendar.getInstance().apply {
                            time = currentMonth
                            set(Calendar.DAY_OF_MONTH, dayNumber)
                        }
                        val isSelected =
                            Calendar.getInstance().apply { time = selectedDate }
                                .get(Calendar.DAY_OF_MONTH) == dayNumber && Calendar.getInstance()
                                .apply { time = selectedDate }
                                .get(Calendar.MONTH) == calendar.get(
                                Calendar.MONTH
                            )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.tertiary
                                    else Color.Transparent
                                )
                                .clickable { onDateSelected(dayCalendar.time) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNumber.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
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

@Composable
fun TimeScrollPicker(
    startTime: LocalTime = LocalTime.now(),
    minTime: LocalTime = LocalTime.MIN(),
    maxTime: LocalTime = LocalTime.MAX(),
    rowCount: Int = 5,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    textColor: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 128.dp,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
) {
    var snappedTime by remember { mutableStateOf(startTime.truncateTo(DateTimeUnit.MINUTE)) }

    val hoursRange = (0..23)
    val longHoursRange =
        List(500) { (it + hoursRange.first + hoursRange.count()) % hoursRange.count() + hoursRange.first }
    val hours = longHoursRange.mapIndexed { index, value ->
        Hour(
            text = value.toString().padStart(2, '0'),
            value = value,
            index = index
        )
    }

    val hoursMinutes = (0..59)
    val longMinutesRange =
        List(500) { (it + hoursMinutes.first + hoursMinutes.count()) % hoursMinutes.count() + hoursMinutes.first }
    val minutes = longMinutesRange.mapIndexed { index, value ->
        Minute(
            text = value.toString().padStart(2, '0'),
            value = value,
            index = index
        )
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height / rowCount)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = Shapes.small
                )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            MyWheelTextPicker(
                startIndex = hours.find { it.value == startTime.hour }?.index ?: 0,
                height = height,
                texts = hours.map { it.text },
                rowCount = rowCount,
                style = textStyle,
                color = textColor
            ) { snappedIndex ->

                val newHour = hours.getOrNull(snappedIndex)?.value

                newHour?.let {
                    onHourChange(newHour)
                    val newTime = snappedTime.withHour(newHour)

                    if (newTime.compareTo(minTime) >= 0 && newTime.compareTo(maxTime) <= 0) {
                        snappedTime = newTime
                    }
                }

                return@MyWheelTextPicker snappedIndex
            }
            Box(
                modifier = Modifier
                    .height(height),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ":",
                    style = textStyle,
                    color = textColor
                )
            }
            MyWheelTextPicker(
                startIndex = minutes.find { it.value == startTime.minute }?.index ?: 0,
                height = height,
                texts = minutes.map { it.text },
                rowCount = rowCount,
                style = textStyle,
                color = textColor
            ) { snappedIndex ->

                val newMinute = minutes.getOrNull(snappedIndex)?.value

                newMinute?.let {
                    val newTime = snappedTime.withMinute(newMinute)
                    if (newTime.compareTo(minTime) >= 0 && newTime.compareTo(maxTime) <= 0) {
                        snappedTime = newTime
                    }
                    onMinuteChange(newMinute)

                }

                return@MyWheelTextPicker snappedIndex
            }
        }
    }

}


@Composable
fun TimeInputOverlay(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onDone: () -> Unit
) {
    var timeText by remember {
        mutableStateOf(String.format("%02d%02d", hour, minute))
    }
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(timeText) {
        if (timeText.length >= 2) {
            val h = timeText.substring(0, 2).toIntOrNull()
            if (h != null && h in 0..23) {
                onHourChange(h)
            }
        }
        if (timeText.length == 4) {
            val m = timeText.substring(2, 4).toIntOrNull()
            if (m != null && m in 0..59) {
                onMinuteChange(m)
            }
        }
    }

    Box(
        modifier = Modifier
            .wrapContentSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = timeText,
                    onValueChange = { newValue ->
                        if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                            val isValid = when (newValue.length) {
                                0, 1 -> true
                                2 -> {
                                    val h = newValue.toIntOrNull()
                                    h != null && h in 0..23
                                }

                                3 -> {
                                    val h =
                                        newValue.substring(0, 2).toIntOrNull()
                                    h != null && h in 0..23
                                }

                                4 -> {
                                    val h =
                                        newValue.substring(0, 2).toIntOrNull()
                                    val m =
                                        newValue.substring(2, 4).toIntOrNull()
                                    h != null && h in 0..23 && m != null && m in 0..59
                                }

                                else -> false
                            }
                            if (isValid) {
                                timeText = newValue
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    },
                    modifier = Modifier
                        .width(180.dp)
                        .focusRequester(focusRequester)
                        .background(Color.Yellow),
                    textStyle = TextStyle(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    keyboardActions = KeyboardActions(
                        onDone = { onDone }
                    ),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier.width(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (timeText.isEmpty()) {
                                    Text(
                                        text = "00",
                                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.3f
                                        )
                                    )
                                } else if (timeText.length == 1) {
                                    Text(
                                        text = "0${timeText[0]}",
                                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Text(
                                        text = timeText.substring(0, 2),
                                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text =
                                    ":",
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Box(
                                modifier = Modifier.width(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (timeText.length <= 2) {
                                    Text(
                                        text = "00",
                                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.3f
                                        )
                                    )
                                } else if (timeText.length == 3) {
                                    Text(
                                        text = "0${timeText[2]}",
                                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Text(
                                        text = timeText.substring(2, 4),
                                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Cyan)
                        ) {
                            innerTextField()
                        }
                    }
                )
            }
//
//            Spacer(modifier = Modifier.height(24.dp))
//
//            TextButton(onClick = onDone) {
//                Text(
//                    "Готово",
//                    fontSize = 18.sp,
//                    color = MaterialTheme.colorScheme.primary
//                )
//            }
        }
    }
}


class DateTimePickerViewModel(initialTimestamp: Long? = null) {
    private val initialCalendar =
        Calendar.getInstance().apply { initialTimestamp?.let { timeInMillis = it } }

    private val _uiState = MutableStateFlow(
        DateTimePickerState(
            selectedDate = initialCalendar.time,
            currentMonth = initialCalendar.time,
            hour = initialCalendar.get(Calendar.HOUR_OF_DAY),
            minute = initialCalendar.get(Calendar.MINUTE)
        )
    )
    val uiState: StateFlow<DateTimePickerState> = _uiState
    fun selectDate(date: Date) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }

    fun setHour(hour: Int) {
        _uiState.value = _uiState.value.copy(hour = hour)
    }

    fun setMinute(minute: Int) {
        _uiState.value = _uiState.value.copy(minute = minute)
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
    val selectedDate: Date = Date(),
    val currentMonth: Date = Date(),
    val hour: Int = 3,
    val minute: Int = 50,
    val isEditingTime: Boolean = false,
    val isCompactCalendar: Boolean = true
)

fun Date.toLocalDateTime(
    fixedOffsetTimeZone: FixedOffsetTimeZone = FixedOffsetTimeZone(
        UtcOffset.ZERO
    )
): LocalDateTime =
    Instant.fromEpochMilliseconds(this.time).toLocalDateTime(fixedOffsetTimeZone)
