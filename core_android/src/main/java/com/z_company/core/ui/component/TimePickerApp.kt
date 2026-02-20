package com.z_company.core.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.component.customDatePicker.Hour
import com.z_company.core.ui.component.customDatePicker.Minute
import com.z_company.core.ui.component.customDatePicker.MyWheelTextPicker
import com.z_company.core.ui.component.customDatePicker.withHour
import com.z_company.core.ui.component.customDatePicker.withMinute
import com.z_company.core.ui.theme.Shapes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerApp(
    onTimeSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    initialTimeMillis: Long = 0L,
    title: String = "Выберите время",
    cancelButtonText: String = "Пропустить",
    onCancelButton: () -> Unit = onDismiss
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val viewModel = remember { TimePickerViewModel(initialTimeMillis) }
    val uiState by viewModel.uiState.collectAsState()
    val heightScreen = LocalConfiguration.current.screenHeightDp

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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((heightScreen / 2).dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Заголовок
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

                    Spacer(modifier = Modifier.height(32.dp))

                    // Сам пикер времени
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        TimeScrollPicker(
                            currentHour = uiState.hour,
                            currentMinute = uiState.minute,
                            isEditing = uiState.isEditingTime,
                            onHourChange = { viewModel.setHour(it) },
                            onMinuteChange = { viewModel.setMinute(it) },
                            onChangeEditTime = viewModel::toggleEditMode
                        )

                        // Оверлей для ручного ввода
                        if (uiState.isEditingTime) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
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

                    Spacer(modifier = Modifier.height(48.dp))

                }
                // Кнопки внизу

                Column(modifier = Modifier) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onCancelButton) {
                            Text(
                                text = cancelButtonText,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            shape = Shapes.medium,
                            onClick = {
                                val selectedMillis =
                                    (uiState.hour * 3_600_000L) + (uiState.minute * 60_000L)
                                onTimeSelected(selectedMillis)
                                onDismiss()
                            }
                        ) {
                            Text(
                                text = "Применить",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

            }
        }
    }
}

@Composable
fun TimeScrollPicker(
    currentHour: Int,
    currentMinute: Int,
    isEditing: Boolean,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onChangeEditTime: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var snappedTime by remember {
        mutableStateOf(
            LocalTime(
                hour = currentHour,
                minute = currentMinute
            )
        )
    }

    var height = 145.dp
    val rowCount = 5

    val itemHeight = height / rowCount

    val textStyle = MaterialTheme.typography.bodyLarge
    val textColor = MaterialTheme.colorScheme.primary

    val hoursRange = (0..23)
    val hoursCycle = hoursRange.count()
    val longHoursRange =
        List(500) { (it % hoursCycle) }

    val hours = longHoursRange.mapIndexed { index, value ->
        Hour(
            text = value.toString().padStart(2, '0'),
            value = value,
            index = index
        )
    }

    val minutesRange = (0..59)
    val minutesCycle = minutesRange.count()
    val longMinutesRange =
        List(500) { (it % minutesCycle) }

    val minutes = longMinutesRange.mapIndexed { index, value ->
        Minute(
            text = value.toString().padStart(2, '0'),
            value = value,
            index = index
        )
    }

    val hoursMiddleCycle = (longHoursRange.size / 2) / hoursCycle
    val hoursMiddleIndex = hoursMiddleCycle * hoursCycle + currentHour

    val minutesMiddleCycle = (longMinutesRange.size / 2) / minutesCycle
    val minutesMiddleIndex = minutesMiddleCycle * minutesCycle + currentMinute

    val hourListState = rememberLazyListState(
        initialFirstVisibleItemIndex = hoursMiddleIndex.coerceIn(0, hours.size - 1)
    )
    val minuteListState = rememberLazyListState(
        initialFirstVisibleItemIndex = minutesMiddleIndex.coerceIn(0, minutes.size - 1)
    )

    LaunchedEffect(currentHour) {
        snappedTime = snappedTime.withHour(currentHour)
        val offset = rowCount / 2
        val currentCenter = hourListState.firstVisibleItemIndex + offset
        val maxK = (longHoursRange.size - 1 - currentHour) / hoursCycle
        val candidates = (0..maxK).map { currentHour + it * hoursCycle }
        val closest = candidates.minByOrNull { kotlin.math.abs(it - currentCenter) } ?: 0
        scope.launch {
            hourListState.animateScrollToItem(closest.coerceIn(0, hours.size - 1))
        }
    }

    LaunchedEffect(currentMinute) {
        snappedTime = snappedTime.withMinute(currentMinute)
        val offset = rowCount / 2
        val currentCenter = minuteListState.firstVisibleItemIndex + offset
        val maxK = (longMinutesRange.size - 1 - currentMinute) / minutesCycle
        val candidates = (0..maxK).map { currentMinute + it * minutesCycle }
        val closest = candidates.minByOrNull { kotlin.math.abs(it - currentCenter) } ?: 0
        scope.launch {
            minuteListState.animateScrollToItem(closest.coerceIn(0, minutes.size - 1))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onChangeEditTime() }
        ) {
            MyWheelTextPicker(
                modifier = Modifier.width(80.dp),
                lazyListState = hourListState,
                height = height,
                texts = hours.map { "%02d".format(it.value) },
                rowCount = rowCount,
                style = textStyle,
                color = textColor,
                contentArrangement = Arrangement.Center,
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow,
                frictionMultiplier = 0.5f
            ) { snappedIndex ->
                val newHour =
                    hours.getOrNull(snappedIndex)?.value ?: return@MyWheelTextPicker snappedIndex
                snappedTime = snappedTime.withHour(newHour)
                if (!isEditing) {
                    onHourChange(newHour)
                }
                snappedIndex
            }

            Box(modifier = Modifier.height(height), contentAlignment = Alignment.Center) {
                AutoSizeText(
                    maxTextSize = 26.sp,
                    minTextSize = 22.sp,
                    text = ":",
                    style = textStyle.copy(
                        textAlign = TextAlign.Center
                    ),
                    color = textColor
                )
            }

            MyWheelTextPicker(
                modifier = Modifier.width(80.dp),
                lazyListState = minuteListState,
                height = height,
                texts = minutes.map { "%02d".format(it.value) },
                rowCount = rowCount,
                style = textStyle,
                color = textColor,
                contentArrangement = Arrangement.Center,
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow,
                frictionMultiplier = 0.5f
            ) { snappedIndex ->
                val newMinute =
                    minutes.getOrNull(snappedIndex)?.value ?: return@MyWheelTextPicker snappedIndex
                snappedTime = snappedTime.withMinute(newMinute)
                if (!isEditing) {
                    onMinuteChange(newMinute)
//                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                snappedIndex
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 24.dp)
        ) {
            Divider(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = -(itemHeight / 2)),
                color = Color.LightGray.copy(alpha = 0.8f),
                thickness = 1.dp
            )
            Divider(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (itemHeight / 2)),
                color = Color.LightGray.copy(alpha = 0.8f),
                thickness = 1.dp
            )
        }
    }
}

@Composable
fun TimeInputOverlay(
    modifier: Modifier = Modifier,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onDone: () -> Unit
) {
    var timeText by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val textStyle = MaterialTheme.typography.headlineMedium.copy(fontSize = 42.sp)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        BasicTextField(
            value = timeText,
            onValueChange = { newValue ->
                val processed = if (newValue.length > 4) newValue.takeLast(4) else newValue
                if (processed.all { it.isDigit() }) {
                    timeText = processed
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                    val d = processed.map { it.digitToInt() }
                    when (processed.length) {
                        1 -> onMinuteChange(d[0].coerceAtMost(5))
                        2 -> onMinuteChange((d[0] * 10 + d[1]).coerceAtMost(59))
                        3 -> {
                            onHourChange(d[0].coerceAtMost(2))
                            onMinuteChange((d[1] * 10 + d[2]).coerceAtMost(59))
                        }

                        4 -> {
                            val h = d[0] * 10 + d[1]
                            onHourChange(if (h > 23) d[1] else h)
                            val m = d[2] * 10 + d[3]
                            onMinuteChange(m.coerceAtMost(59))
                        }
                    }
                }
            },
            modifier = Modifier
                .focusRequester(focusRequester),
            textStyle = TextStyle(
                fontSize = textStyle.fontSize,
                textAlign = TextAlign.Center,
                color = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            keyboardActions = KeyboardActions(onDone = {
                onDone()
            }),
            cursorBrush = SolidColor(Color.Transparent),
            singleLine = true
        )
    }
}

// ViewModel и State
class TimePickerViewModel(initialTimestamp: Long) {
    private val calendar =
        java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+0"))
            .apply { timeInMillis = initialTimestamp }

    private val _uiState = MutableStateFlow(
        TimePickerState(
            hour = calendar.get(java.util.Calendar.HOUR_OF_DAY),
            minute = calendar.get(java.util.Calendar.MINUTE)
        )
    )
    val uiState: StateFlow<TimePickerState> = _uiState

    fun setHour(hour: Int) {
        _uiState.value = _uiState.value.copy(hour = hour)
    }

    fun setMinute(minute: Int) {
        _uiState.value = _uiState.value.copy(minute = minute)
    }

    fun toggleEditMode() {
        _uiState.value = _uiState.value.copy(isEditingTime = !_uiState.value.isEditingTime)
    }
}

data class TimePickerState(
    val hour: Int = 0,
    val minute: Int = 0,
    val isEditingTime: Boolean = false
)