package com.z_company.route.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.z_company.core.ui.component.TimePickerApp
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.SettingsUseCase
import org.koin.compose.koinInject

/**
 * Центральный менеджер выбора времени.
 * Читает настройку useStandardTimePicker из UserSettings и показывает:
 * - false (по умолчанию): кастомный барабанный пикер [TimePickerApp]
 * - true: стандартный Material3 TimePicker
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

        val containerColor = MaterialTheme.colorScheme.secondary
        val primaryColor = MaterialTheme.colorScheme.primary
        val surfaceColor = MaterialTheme.colorScheme.surface
        val selectedColor = MaterialTheme.colorScheme.surfaceContainerLow

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
            title = { Text(text = title, color = primaryColor) },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
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
                )
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
