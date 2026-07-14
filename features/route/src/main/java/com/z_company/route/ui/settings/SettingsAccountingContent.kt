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
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 28.dp),
    ) {
        SettingsGroupHeader("НОЧНЫЕ ЧАСЫ", top = 8.dp, startPad = 4.dp)
        SettingsCard {
            SettingsSelectRow(
                label = "Ночь",
                value = currentSettings.nightTime.toString(),
                mono = true,
                onClick = { showNightTimeStartDialog = true },
            )
        }
        SettingsSectionNote("Интервал ночных часов для расчёта доплаты за работу ночью.")

        SettingsGroupHeader("БУДУЩИЕ МАРШРУТЫ", top = 20.dp, startPad = 4.dp)
        SettingsCard {
            SettingsSwitchRow(
                label = "Учитывать будущие маршруты",
                sub = "С ещё не наступившей явкой",
                checked = currentSettings.isConsiderFutureRoute,
                onCheckedChange = changeConsiderFutureRoute,
            )
        }
        SettingsSectionNote("Маршруты, время явки которых не наступило, будут учитываться при подсчёте отработанного времени.")
    }
}
