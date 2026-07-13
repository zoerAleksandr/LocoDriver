package com.z_company.route.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.route.component.AppTimePicker
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.ConverterLongToTime
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.SharedPreferencesRepositories
import org.koin.compose.koinInject

@Composable
fun SettingsAccountingContent(
    currentSettings: UserSettings,
    changeStartNightTime: (Int, Int) -> Unit,
    changeEndNightTime: (Int, Int) -> Unit,
    changeConsiderFutureRoute: (Boolean) -> Unit,
) {
    val styleData = MaterialTheme.typography.bodyLarge
    val styleHint = MaterialTheme.typography.bodyMedium
    val primaryColor = MaterialTheme.colorScheme.primary

    val sharedPrefs: SharedPreferencesRepositories = koinInject()
    var showNightTimeStartDialog by remember { mutableStateOf(false) }
    var showNightTimeEndDialog by remember { mutableStateOf(false) }

    if (showNightTimeStartDialog) {
        val initHour = currentSettings.nightTime.startNightHour
        val initMinute = currentSettings.nightTime.startNightMinute
        val initMillis = (initHour.times(3600000) + initMinute.times(60000)).toLong()
        AppTimePicker(
            initialTimeMillis = initMillis,
            onTimeSelected = { millis ->
                val hour = ConverterLongToTime.getHour(millis)
                val minute = ConverterLongToTime.getRemainingMinuteFromHour(millis)
                changeStartNightTime(hour, minute)
                showNightTimeStartDialog = false
                showNightTimeEndDialog = true
            },
            onDismiss = { showNightTimeStartDialog = false },
            title = "Начало ночи",
            recentTimes = sharedPrefs.getRecentTimes("night_start"),
            onRecentTimeSaved = { sharedPrefs.addRecentTime("night_start", it) }
        )
    }

    if (showNightTimeEndDialog) {
        val initHour = currentSettings.nightTime.endNightHour
        val initMinute = currentSettings.nightTime.endNightMinute
        val initMillis = (initHour.times(3600000) + initMinute.times(60000)).toLong()
        AppTimePicker(
            initialTimeMillis = initMillis,
            onTimeSelected = { millis ->
                val hour = ConverterLongToTime.getHour(millis)
                val minute = ConverterLongToTime.getRemainingMinuteFromHour(millis)
                changeEndNightTime(hour, minute)
            },
            onDismiss = { showNightTimeEndDialog = false },
            title = "Окончание ночи",
            recentTimes = sharedPrefs.getRecentTimes("night_end"),
            onRecentTimeSaved = { sharedPrefs.addRecentTime("night_end", it) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Ночь
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsGroupHeader("НОЧНЫЕ ЧАСЫ")
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .noRippleEffect { showNightTimeStartDialog = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ночь",
                        style = styleData,
                        color = primaryColor,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = currentSettings.nightTime.toString(),
                        style = styleData.copy(fontFamily = MonoFont),
                        color = primaryColor
                    )
                }
            }

            Text(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                text = "Установите время ночных часов для расчета ночных.",
                style = styleHint,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Будущие маршруты
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsGroupHeader("БУДУЩИЕ МАРШРУТЫ")
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Text(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .weight(1f),
                    text = "Учитывать будущие маршруты",
                    style = styleData,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2
                )
                Switch(
                    checked = currentSettings.isConsiderFutureRoute,
                    onCheckedChange = { changeConsiderFutureRoute(it) }
                )
                }
            }

            Text(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                text = "Маршруты, время явки которых не наступило, будут учитываться при подсчете отработаного времени.",
                style = styleHint,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
