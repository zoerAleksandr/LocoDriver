package com.z_company.route.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z_company.route.component.AppTimePicker
import com.z_company.core.util.ConverterLongToTime
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.route.viewmodel.Passenger12hOption
import com.z_company.route.viewmodel.SettingsViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRouteContent(
    currentSettings: UserSettings,
    viewModel: SettingsViewModel,
    changeUsingDefaultWorkTime: (Boolean) -> Unit,
    workTimeChanged: (Long) -> Unit,
    changeShowBreak: (Boolean) -> Unit,
) {
    val sharedPrefs: SharedPreferencesRepositories = koinInject()
    var showWorkTimeDialog by remember { mutableStateOf(false) }
    val passenger12hOption by viewModel.passenger12hOption.collectAsState()

    if (showWorkTimeDialog) {
        AppTimePicker(
            initialTimeMillis = currentSettings.defaultWorkTime,
            onTimeSelected = { millis ->
                workTimeChanged(millis)
                showWorkTimeDialog = false
            },
            onDismiss = { showWorkTimeDialog = false },
            title = "Время работы по умолчанию",
            recentTimes = sharedPrefs.getRecentTimes("default_work_time"),
            onRecentTimeSaved = { sharedPrefs.addRecentTime("default_work_time", it) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 28.dp),
    ) {
        // ── Вид тяги (по умолчанию) ──
        SettingsGroupHeader("ВИД ТЯГИ", top = 8.dp, startPad = 4.dp)
        SettingsCard {
            LocoType.entries.forEachIndexed { index, type ->
                if (index > 0) SettingsCardSep()
                SettingsRadioRow(
                    label = type.text,
                    selected = currentSettings.defaultLocoType == type,
                    onClick = { viewModel.setDefaultLocoType(type) },
                )
            }
        }

        // ── Формат времени ──
        SettingsGroupHeader("ФОРМАТ ВРЕМЕНИ", top = 20.dp, startPad = 4.dp)
        SettingsCard {
            SettingsRadioRow(
                label = "Часы и минуты",
                sub = "Например, 12:30",
                selected = !currentSettings.isDecimalTime,
                onClick = { if (currentSettings.isDecimalTime) viewModel.changeTimeFormat() },
            )
            SettingsCardSep()
            SettingsRadioRow(
                label = "Десятичный",
                sub = "Например, 12,5",
                selected = currentSettings.isDecimalTime,
                onClick = { if (!currentSettings.isDecimalTime) viewModel.changeTimeFormat() },
            )
        }

        // ── Время работы по умолчанию ──
        SettingsGroupHeader("ВРЕМЯ РАБОТЫ", top = 20.dp, startPad = 4.dp)
        SettingsCard {
            SettingsSwitchRow(
                label = "Стандартное время работы",
                checked = currentSettings.usingDefaultWorkTime,
                onCheckedChange = changeUsingDefaultWorkTime,
            )
            if (currentSettings.usingDefaultWorkTime) {
                SettingsCardSep()
                SettingsFieldRow(
                    label = "Время работы",
                    value = ConverterLongToTime.getTimeInStringFormat(currentSettings.defaultWorkTime),
                    mono = true,
                    onClick = { showWorkTimeDialog = true },
                )
            }
        }
        SettingsSectionNote("Подставляется по умолчанию при создании нового маршрута.")

        // ── Стиль выбора времени ──
        SettingsGroupHeader("СТИЛЬ ВЫБОРА ВРЕМЕНИ", top = 20.dp, startPad = 4.dp)
        SettingsCard {
            SettingsRadioRow(
                label = "Системный",
                sub = "Стандартный диалог Android",
                selected = currentSettings.useStandardTimePicker,
                onClick = { if (!currentSettings.useStandardTimePicker) viewModel.changeTimePickerStyle() },
            )
            SettingsCardSep()
            SettingsRadioRow(
                label = "Кастомный",
                sub = "Быстрый ввод цифрами",
                selected = !currentSettings.useStandardTimePicker,
                onClick = { if (currentSettings.useStandardTimePicker) viewModel.changeTimePickerStyle() },
            )
        }

        // ── Свыше 12 часов ──
        SettingsGroupHeader("СВЫШЕ 12 ЧАСОВ", top = 20.dp, startPad = 4.dp)
        SettingsCard {
            Passenger12hOption.entries.forEachIndexed { index, option ->
                if (index > 0) SettingsCardSep()
                SettingsRadioRow(
                    label = option.label,
                    selected = passenger12hOption == option,
                    onClick = { viewModel.setPassenger12hOption(option) },
                )
            }
        }
        SettingsSectionNote("Если поездка длится более 12 часов, часть времени можно оформить как следование пассажиром.")
    }
}
