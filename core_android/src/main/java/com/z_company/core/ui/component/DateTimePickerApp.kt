package com.z_company.core.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.component.customDatePicker.Hour
import com.z_company.core.ui.component.customDatePicker.MAX
import com.z_company.core.ui.component.customDatePicker.MIN
import com.z_company.core.ui.component.customDatePicker.Minute
import com.z_company.core.ui.component.customDatePicker.MyWheelTextPicker
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.component.customDatePicker.withHour
import com.z_company.core.ui.component.customDatePicker.withMinute
import com.z_company.core.ui.theme.Shapes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerBottomSheet(
    onDateTimeSelected: (Long) -> Unit, onDismiss: () -> Unit,
    startDateTime: Long = Calendar.getInstance().timeInMillis,
    title: String = "",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val viewModel = remember { DateTimePickerViewModel(initialTimestamp = startDateTime) }
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
                            onDateSelected = { viewModel.selectDate(it) })
                    } else {
                        FullCalendar(
                            selectedDate = uiState.selectedDate,
                            onDateSelected = { viewModel.selectDate(it) })
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

            item {
                Spacer(
                    modifier = Modifier.height(48.dp)
                )
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
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Кнопка выбрать
            item {
                Button(
                    onClick = {
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = uiState.selectedDate
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

@Composable
fun CompactCalendar(selectedDate: Long, onDateSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
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
                        text = dayNumber.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelectedDay) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun FullCalendar(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit
) {
    val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    // Форматтеры для названий месяцев
    val monthNameFormat = SimpleDateFormat("LLLL", Locale("ru"))
    val yearFormat = SimpleDateFormat("yyyy", Locale("ru"))
    var currentMonth by remember {
        mutableStateOf(
            Calendar.getInstance().apply { timeInMillis = selectedDate })
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
                                val isSelected =
                                    Calendar.getInstance().apply { timeInMillis = selectedDate }
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
//
//@Composable
//fun TimeScrollPicker(
//    currentHour: Int,
//    currentMinute: Int,
//    isEditing: Boolean,
//    minTime: LocalTime = LocalTime.MIN(),
//    maxTime: LocalTime = LocalTime.MAX(),
//    rowCount: Int = 5,
//    height: Dp = 128.dp,
//    onHourChange: (Int) -> Unit,
//    onMinuteChange: (Int) -> Unit,
//    onChangeEditTime: () -> Unit,
//    dampingRatio: Float = Spring.DampingRatioLowBouncy, // Регулируемый параметр: низкое сопротивление для длительного затухания
//    stiffness: Float = Spring.StiffnessLow, // Регулируемый параметр: меньшая жесткость для плавного замедления
//    frictionMultiplier: Float = 0.5f // Регулируемый параметр: уменьшение трения для большей инерции
//) {
//    val scope = rememberCoroutineScope()
//
//    var snappedTime by remember {
//        mutableStateOf(
//            LocalTime(
//                hour = currentHour,
//                minute = currentMinute
//            )
//        )
//    }
//
//    val hoursRange = (0..23)
//    val hoursCycle = hoursRange.count()
//    val longHoursRange =
//        List(500) { (it % hoursCycle) }
//
//    val hours = longHoursRange.mapIndexed { index, value ->
//        Hour(
//            text = value.toString().padStart(2, '0'),
//            value = value,
//            index = index
//        )
//    }
//
//    val minutesRange = (0..59)
//    val minutesCycle = minutesRange.count()
//    val longMinutesRange =
//        List(500) { (it % minutesCycle) }
//    val minutes = longMinutesRange.mapIndexed { index, value ->
//        Minute(
//            text = value.toString().padStart(2, '0'),
//            value = value,
//            index = index
//        )
//    }
//
//    val hoursMiddleCycle = (longHoursRange.size / 2) / hoursCycle
//    val hoursMiddleIndex = hoursMiddleCycle * hoursCycle + currentHour
//
//    val minutesMiddleCycle = (longMinutesRange.size / 2) / minutesCycle
//    val minutesMiddleIndex = minutesMiddleCycle * minutesCycle + currentMinute
//
//    val hourListState = rememberLazyListState(
//        initialFirstVisibleItemIndex = hoursMiddleIndex.coerceIn(0, hours.size - 1)
//    )
//    val minuteListState = rememberLazyListState(
//        initialFirstVisibleItemIndex = minutesMiddleIndex.coerceIn(0, minutes.size - 1)
//    )
//
//    LaunchedEffect(currentHour) {
//        snappedTime = snappedTime.withHour(currentHour)
//        val offset = rowCount / 2
//        val currentCenter = hourListState.firstVisibleItemIndex + offset
//        val maxK = (longHoursRange.size - 1 - currentHour) / hoursCycle
//        val candidates = (0..maxK).map { currentHour + it * hoursCycle }
//        val closest = candidates.minByOrNull { kotlin.math.abs(it - currentCenter) } ?: 0
//        scope.launch {
//            hourListState.animateScrollToItem(closest.coerceIn(0, hours.size - 1))
//        }
//    }
//
//    LaunchedEffect(currentMinute) {
//        snappedTime = snappedTime.withMinute(currentMinute)
//        val offset = rowCount / 2
//        val currentCenter = minuteListState.firstVisibleItemIndex + offset
//        val maxK = (longMinutesRange.size - 1 - currentMinute) / minutesCycle
//        val candidates = (0..maxK).map { currentMinute + it * minutesCycle }
//        val closest = candidates.minByOrNull { kotlin.math.abs(it - currentCenter) } ?: 0
//        scope.launch {
//            minuteListState.animateScrollToItem(closest.coerceIn(0, minutes.size - 1))
//        }
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxWidth(),
//        contentAlignment = Alignment.Center
//    ) {
//        Box(
//            modifier = Modifier
//                .padding(horizontal = 16.dp)
//                .fillMaxWidth()
//                .height(height / rowCount)
//                .background(
//                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
//                    shape = Shapes.small
//                )
//                .noRippleEffect(onChangeEditTime)
//        )
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
//        ) {
//            val textStyle = MaterialTheme.typography.bodyLarge
//            val textColor = MaterialTheme.colorScheme.primary
//
//            MyWheelTextPicker(
//                modifier = Modifier
//                    .width(60.dp),
//                lazyListState = hourListState,
//                height = height,
//                texts = hours.map { it.text },
//                rowCount = rowCount,
//                style = textStyle,
//                color = textColor,
//                contentArrangement = Arrangement.Center,
//                dampingRatio = dampingRatio,
//                stiffness = stiffness,
//                frictionMultiplier = frictionMultiplier
//            ) { snappedIndex ->
//
//                val newHour =
//                    hours.getOrNull(snappedIndex)?.value ?: return@MyWheelTextPicker snappedIndex
//
//                val newTime = snappedTime.withHour(newHour)
//
//                if (newTime.compareTo(minTime) >= 0 && newTime.compareTo(maxTime) <= 0) {
//                    snappedTime = newTime
//                    if (!isEditing) {
//                        onHourChange(newHour)
//                    }
//                    return@MyWheelTextPicker snappedIndex
//                } else {
//                    val offset = rowCount / 2
//                    val currentCenter = hourListState.firstVisibleItemIndex + offset
//                    val maxK = (longHoursRange.size - 1 - snappedTime.hour) / hoursCycle
//                    val candidates = (0..maxK).map { snappedTime.hour + it * hoursCycle }
//                    val closest =
//                        candidates.minByOrNull { kotlin.math.abs(it - currentCenter) }
//                            ?: snappedIndex
//                    return@MyWheelTextPicker closest
//                }
//            }
//
//            Box(
//                modifier = Modifier
//                    .height(height),
//                contentAlignment = Alignment.Center
//            ) {
//                Text(
//                    text = ":",
//                    style = textStyle,
//                    color = textColor
//                )
//            }
//
//            MyWheelTextPicker(
//                modifier = Modifier
//                    .width(60.dp),
//                lazyListState = minuteListState,
//                height = height,
//                texts = minutes.map { it.text },
//                rowCount = rowCount,
//                style = textStyle,
//                color = textColor,
//                contentArrangement = Arrangement.Center,
//                dampingRatio = Spring.DampingRatioLowBouncy,
//                stiffness = Spring.StiffnessLow,
//                frictionMultiplier = 0.5f
//            ) { snappedIndex ->
//
//                val newMinute =
//                    minutes.getOrNull(snappedIndex)?.value ?: return@MyWheelTextPicker snappedIndex
//
//                val newTime = snappedTime.withMinute(newMinute)
//                if (newTime.compareTo(minTime) >= 0 && newTime.compareTo(maxTime) <= 0) {
//                    snappedTime = newTime
//                    if (!isEditing) {
//                        onMinuteChange(newMinute)
//                    }
//                    return@MyWheelTextPicker snappedIndex
//                } else {
//                    val offset = rowCount / 2
//                    val currentCenter = minuteListState.firstVisibleItemIndex + offset
//                    val maxK = (longMinutesRange.size - 1 - snappedTime.minute) / minutesCycle
//                    val candidates = (0..maxK).map { snappedTime.minute + it * minutesCycle }
//                    val closest = candidates.minByOrNull { kotlin.math.abs(it - currentCenter) }
//                        ?: snappedIndex
//                    return@MyWheelTextPicker closest
//                }
//            }
//        }
//    }
//}

//
//@Composable
//private fun TimeInputOverlay(
//    onHourChange: (Int) -> Unit,
//    onMinuteChange: (Int) -> Unit,
//    onDone: () -> Unit
//) {
//    var timeText by remember {
//        mutableStateOf("")
//    }
//
//    val haptic = LocalHapticFeedback.current
//
//    val focusRequester = remember { FocusRequester() }
//
//    LaunchedEffect(Unit) { focusRequester.requestFocus() }
//
//    val textStyle = MaterialTheme.typography.bodyLarge
//    val textColor = MaterialTheme.colorScheme.primary
//
//    Box(
//        modifier = Modifier
//            .wrapContentWidth(),
//        contentAlignment = Alignment.Center
//    ) {
//        BasicTextField(
//            value = timeText,
//            onValueChange = { newValue ->
//                val processedValue = if (newValue.length > 4) {
//                    newValue.takeLast(4)
//                } else {
//                    newValue
//                }
//
//                if (processedValue.all { it.isDigit() }) {
//                    timeText = processedValue
//                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
//
//                    val len = timeText.length
//                    if (len > 0) {
//                        val d = timeText.map { it.digitToInt() }
//                        when (len) {
//                            1 -> {
//                                var newM = d[0]
//                                if (newM > 59) newM = d[0] // impossible for single digit
//                                onMinuteChange(newM)
//                            }
//
//                            2 -> {
//                                var newM = d[0] * 10 + d[1]
//                                if (newM > 59) newM = d[1]
//                                onMinuteChange(newM)
//                            }
//
//                            3 -> {
//                                var newH = d[0]
//                                if (newH > 23) newH = d[0] // impossible
//                                onHourChange(newH)
//
//                                var newM = d[1] * 10 + d[2]
//                                if (newM > 59) newM = d[2]
//                                onMinuteChange(newM)
//                            }
//
//                            4 -> {
//                                var newH = d[0] * 10 + d[1]
//                                if (newH > 23) newH = d[1]
//                                onHourChange(newH)
//
//                                var newM = d[2] * 10 + d[3]
//                                if (newM > 59) newM = d[3]
//                                onMinuteChange(newM)
//                            }
//                        }
//                    }
//                }
//            },
//            modifier = Modifier
//                .wrapContentWidth()
//                .focusRequester(focusRequester),
//            textStyle = TextStyle(
//                fontFamily = textStyle.fontFamily,
//                fontSize = textStyle.fontSize,
//                textAlign = TextAlign.Center,
//                color = Color.Transparent,
//            ),
//            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//            keyboardActions = KeyboardActions(
//                onDone = { onDone() }
//            ),
//            cursorBrush = SolidColor(Color.Transparent),
//            singleLine = true,
//            decorationBox = { innerTextField ->
//                innerTextField()
//            }
//        )
//    }
//}


class DateTimePickerViewModel(initialTimestamp: Long? = null) {
    private val initialCalendar =
        Calendar.getInstance().apply { initialTimestamp?.let { timeInMillis = it } }

    private val _uiState = MutableStateFlow(
        DateTimePickerState(
            selectedDate = initialCalendar.timeInMillis,
            currentMonth = initialCalendar.time,
            hour = initialCalendar.get(Calendar.HOUR_OF_DAY),
            minute = initialCalendar.get(Calendar.MINUTE)
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
    val selectedDate: Long = 0L,
    val currentMonth: Date = Date(),
    val hour: Int = 3,
    val minute: Int = 50,
    val isEditingTime: Boolean = false,
    val isCompactCalendar: Boolean = true
)