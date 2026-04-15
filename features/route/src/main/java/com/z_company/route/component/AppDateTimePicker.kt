package com.z_company.route.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.z_company.core.ui.component.DateTimePickerBottomSheet
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.SettingsUseCase
import org.koin.compose.koinInject
import java.util.Calendar
import java.util.TimeZone

/**
 * Центральный менеджер выбора даты и времени.
 * Читает настройку useStandardTimePicker из UserSettings и показывает:
 * - false (по умолчанию): кастомный DateTimePickerBottomSheet (календарь + барабаны)
 * - true: стандартный Material3 DatePickerDialog (шаг 1) → TimePickerDialog (шаг 2)
 *
 * Используется везде, где нужен выбор даты+времени (явка, сдача, приёмка и т.д.).
 * Параметры идентичны [DateTimePickerBottomSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDateTimePicker(
    onDateTimeSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    startDateTime: Long?,
    title: String = "",
    timeZoneStr: String = "GMT+3",
    recentTimes: List<Long> = emptyList(),
    onRecentTimeSaved: ((Long) -> Unit)? = null,
) {
    val settingsUseCase: SettingsUseCase = koinInject()
    val sharedPrefs: SharedPreferencesRepositories = koinInject()
    val settings by settingsUseCase.getUserSettingFlow()
        .collectAsStateWithLifecycle(initialValue = UserSettings())

    if (settings.useStandardTimePicker) {
        // Шаг 0 — выбор даты, Шаг 1 — выбор времени
        var step by remember { mutableIntStateOf(0) }
        var pickedDateMillis by remember {
            mutableLongStateOf(startDateTime ?: System.currentTimeMillis())
        }

        val containerColor = MaterialTheme.colorScheme.secondary
        val primaryColor = MaterialTheme.colorScheme.primary
        val surfaceColor = MaterialTheme.colorScheme.surface
        val selectedColor = MaterialTheme.colorScheme.surfaceContainerLow

        if (step == 0) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = startDateTime ?: System.currentTimeMillis()
            )
            DatePickerDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(onClick = {
                        pickedDateMillis = datePickerState.selectedDateMillis
                            ?: System.currentTimeMillis()
                        step = 1
                    }) { Text("Далее") }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                },
                colors = DatePickerDefaults.colors(containerColor = containerColor)
            ) {
                DatePicker(
                    state = datePickerState,
                    title = {
                        if (title.isNotEmpty()) {
                            Text(
                                text = title,
                                modifier = Modifier.padding(
                                    start = 24.dp,
                                    end = 12.dp,
                                    top = 16.dp,
                                    bottom = 4.dp
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = primaryColor
                            )
                        }
                    },
                    colors = DatePickerDefaults.colors(
                        containerColor = containerColor,
                        titleContentColor = primaryColor,
                        headlineContentColor = primaryColor,
                        weekdayContentColor = primaryColor,
                        subheadContentColor = primaryColor,
                        navigationContentColor = primaryColor,
                        yearContentColor = primaryColor,
                        currentYearContentColor = primaryColor,
                        selectedYearContainerColor = selectedColor,
                        selectedYearContentColor = containerColor,
                        dayContentColor = primaryColor,
                        selectedDayContainerColor = selectedColor,
                        selectedDayContentColor = containerColor,
                        todayContentColor = primaryColor,
                        todayDateBorderColor = selectedColor,
                        dividerColor = surfaceColor,
                        dateTextFieldColors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = surfaceColor,
                            unfocusedContainerColor = surfaceColor,
                            focusedTextColor = primaryColor,
                            unfocusedTextColor = primaryColor,
                            cursorColor = selectedColor,
                            focusedBorderColor = selectedColor,
                            unfocusedBorderColor = primaryColor,
                            focusedLabelColor = selectedColor,
                            unfocusedLabelColor = primaryColor,
                        )
                    )
                )
            }
        } else {
            val zone = TimeZone.getTimeZone(timeZoneStr)
            val cal = Calendar.getInstance(zone).apply {
                timeInMillis = startDateTime ?: System.currentTimeMillis()
            }
            val timePickerState = rememberTimePickerState(
                initialHour = cal.get(Calendar.HOUR_OF_DAY),
                initialMinute = cal.get(Calendar.MINUTE),
                is24Hour = true
            )
            var useKeyboardInput by remember { mutableStateOf(sharedPrefs.isTimePickerKeyboardInput()) }

            val timePickerColors = TimePickerDefaults.colors(
                clockDialColor = surfaceColor,
                selectorColor = selectedColor,
                clockDialSelectedContentColor = containerColor,
                clockDialUnselectedContentColor = primaryColor,
                containerColor = containerColor,
                timeSelectorSelectedContainerColor = selectedColor,
                timeSelectorUnselectedContainerColor = surfaceColor,
                timeSelectorSelectedContentColor = containerColor,
                timeSelectorUnselectedContentColor = primaryColor,
            )

            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = containerColor,
                confirmButton = {
                    TextButton(onClick = {
                        val result = Calendar.getInstance(zone).apply {
                            timeInMillis = pickedDateMillis
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        onRecentTimeSaved?.invoke(result)
                        onDateTimeSelected(result)
                        onDismiss()
                    }) { Text("ОК") }
                },
                dismissButton = {
                    TextButton(onClick = { step = 0 }) { Text("Назад") }
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = title, color = primaryColor)
                        IconButton(onClick = {
                            val newValue = !useKeyboardInput
                            sharedPrefs.setTimePickerKeyboardInput(newValue)
                            useKeyboardInput = newValue
                        }) {
                            Icon(
                                painter = painterResource(
                                    if (useKeyboardInput)
                                        com.z_company.core.R.drawable.outline_access_time_24
                                    else
                                        com.z_company.core.R.drawable.outline_keyboard_24
                                ),
                                contentDescription = if (useKeyboardInput) "Циферблат" else "Клавиатура",
                                tint = primaryColor
                            )
                        }
                    }
                },
                text = {
                    if (useKeyboardInput) {
                        TimeInput(state = timePickerState, colors = timePickerColors)
                    } else {
                        TimePicker(state = timePickerState, colors = timePickerColors)
                    }
                }
            )
        }
    } else {
        DateTimePickerBottomSheet(
            onDateTimeSelected = onDateTimeSelected,
            onDismiss = onDismiss,
            startDateTime = startDateTime,
            title = title,
            timeZoneStr = timeZoneStr,
            recentTimes = recentTimes,
            onRecentTimeSaved = onRecentTimeSaved,
        )
    }
}
