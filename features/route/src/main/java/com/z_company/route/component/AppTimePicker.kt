package com.z_company.route.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.z_company.core.ui.component.TimePickerApp
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.SettingsUseCase
import org.koin.compose.koinInject

/**
 * Центральный менеджер выбора времени.
 * Читает настройку useStandardTimePicker из UserSettings и показывает:
 * - false (по умолчанию): кастомный барабанный пикер [TimePickerApp]
 * - true: стандартный Material3 TimePicker с переключателем часы/клавиатура
 *
 * Используется везде, где нужен выбор времени (HH:mm).
 * Параметры идентичны [TimePickerApp].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePicker(
    onTimeSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    initialTimeMillis: Long = 0L,
    title: String = "Выберите время",
    cancelButtonText: String = "Пропустить",
    onCancelButton: () -> Unit = onDismiss,
    recentTimes: List<Long> = emptyList(),
    onRecentTimeSaved: ((Long) -> Unit)? = null,
    showTimeLabel: Boolean = true,
) {
    val settingsUseCase: SettingsUseCase = koinInject()
    val sharedPrefs: SharedPreferencesRepositories = koinInject()
    val settings by settingsUseCase.getUserSettingFlow()
        .collectAsStateWithLifecycle(initialValue = UserSettings())

    if (settings.useStandardTimePicker) {
        val initialHour = (initialTimeMillis / 3_600_000L).toInt().coerceIn(0, 23)
        val initialMinute = ((initialTimeMillis % 3_600_000L) / 60_000L).toInt().coerceIn(0, 59)
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )
        var useKeyboardInput by remember { mutableStateOf(sharedPrefs.isTimePickerKeyboardInput()) }

        val containerColor = MaterialTheme.colorScheme.secondary
        val primaryColor = MaterialTheme.colorScheme.primary
        val surfaceColor = MaterialTheme.colorScheme.surface
        val selectedColor = MaterialTheme.colorScheme.surfaceContainerLow

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
                    val ms = timePickerState.hour * 3_600_000L + timePickerState.minute * 60_000L
                    onRecentTimeSaved?.invoke(ms)
                    onTimeSelected(ms)
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = onCancelButton) { Text(cancelButtonText) }
            },
            title = {
                Row(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
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
    } else {
        TimePickerApp(
            onTimeSelected = onTimeSelected,
            onDismiss = onDismiss,
            initialTimeMillis = initialTimeMillis,
            title = title,
            cancelButtonText = cancelButtonText,
            onCancelButton = onCancelButton,
            recentTimes = recentTimes,
            onRecentTimeSaved = onRecentTimeSaved,
            showTimeLabel = showTimeLabel,
        )
    }
}
