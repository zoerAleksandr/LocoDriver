package com.z_company.route.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.component.CustomDivider
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.AppTimePicker
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.ConverterLongToTime
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.route.viewmodel.Passenger12hOption
import org.koin.compose.koinInject
import com.z_company.route.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRouteContent(
    currentSettings: UserSettings,
    viewModel: SettingsViewModel,
    changeUsingDefaultWorkTime: (Boolean) -> Unit,
    workTimeChanged: (Long) -> Unit,
    changeShowBreak: (Boolean) -> Unit,
) {
    val styleData = MaterialTheme.typography.bodyLarge
    val styleValueMono = styleData.copy(fontFamily = MonoFont)
    val styleHint = MaterialTheme.typography.bodyMedium
    val styleTitle = MaterialTheme.typography.titleSmall
    val primaryColor = MaterialTheme.colorScheme.primary

    val sharedPrefs: SharedPreferencesRepositories = koinInject()
    var showWorkTimeDialog by remember { mutableStateOf(false) }

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
            .padding(horizontal = 12.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Данные по умолчанию
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsGroupHeader("ДАННЫЕ ПО УМОЛЧАНИЮ")
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .noRippleEffect { viewModel.changeDefaultLocoType() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Вид тяги",
                        style = styleData,
                        color = primaryColor
                    )
                    Text(
                        text = currentSettings.defaultLocoType.text,
                        style = styleData,
                        color = primaryColor
                    )
                }
                SettingsRowDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .noRippleEffect { viewModel.changeTimeFormat() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .weight(1f),
                        text = "Формат времени",
                        style = styleData,
                        color = primaryColor,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2
                    )
                    val text = if (currentSettings.isDecimalTime) "12,5" else "12:30"
                    Text(
                        modifier = Modifier.padding(end = 12.dp),
                        text = text,
                        style = styleValueMono,
                        color = primaryColor,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2
                    )
                }

                SettingsRowDivider()

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
                        text = "Использовать cтандартное время работы",
                        style = styleData,
                        color = primaryColor,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2
                    )
                    Switch(
                        checked = currentSettings.usingDefaultWorkTime,
                        onCheckedChange = { changeUsingDefaultWorkTime(it) }
                    )
                }

                AnimatedVisibility(visible = currentSettings.usingDefaultWorkTime) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsRowDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .noRippleEffect { showWorkTimeDialog = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = "Время работы",
                                style = styleData,
                                color = primaryColor,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2
                            )
                            Text(
                                text = ConverterLongToTime.getTimeInStringFormat(
                                    currentSettings.defaultWorkTime
                                ),
                                style = styleValueMono,
                                color = primaryColor,
                            )
                        }
                    }
                }

                SettingsCardHint("Эти значения будут установлены по умолчанию при создании нового маршрута.")
            }
        }

        // «Показывать перерыв» перенесён в подраздел «Маршрут».

        // Стиль выбора времени (вынесен из «Данные по умолчанию» — это не значение
        // по умолчанию для нового маршрута, а UI-настройка диалога выбора времени)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                text = "СТИЛЬ ВЫБОРА ВРЕМЕНИ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .noRippleEffect { viewModel.changeTimePickerStyle() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .weight(1f),
                        text = "Стиль выбора времени",
                        style = styleData,
                        color = primaryColor,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2
                    )
                    val pickerText = if (currentSettings.useStandardTimePicker) "Системный" else "Кастомный"
                    Text(
                        text = pickerText,
                        style = styleData,
                        color = primaryColor,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2
                    )
                }

                SettingsCardHint("Системный — стандартный диалог Android, Кастомный — встроенный пикер с быстрым набором.")
            }
        }

        // Пассажир при >12 часах
        Passenger12hSettingsSection(viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Passenger12hSettingsSection(
    viewModel: SettingsViewModel,
) {
    val styleData = MaterialTheme.typography.bodyLarge
    val styleHint = MaterialTheme.typography.bodyMedium
    val styleTitle = MaterialTheme.typography.titleSmall
    val primaryColor = MaterialTheme.colorScheme.primary

    val currentOption by viewModel.passenger12hOption.collectAsState()
    var showOptionSheet by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsGroupHeader("СВЫШЕ 12 ЧАСОВ")
        SettingsCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .noRippleEffect { showOptionSheet = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Свыше 12-ти часов относить в следование пассажиром",
                    style = styleData,
                    color = primaryColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = currentOption.label,
                    style = styleData.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            SettingsCardHint(currentOption.hint)
        }
    }

    if (showOptionSheet) {
        val optionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        AppBottomSheet(
            onDismissRequest = { showOptionSheet = false },
            sheetState = optionSheetState,
            title = "Свыше 12-ти часов относить в следование пассажиром",
            contentAfterHeader = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Passenger12hOption.entries.forEachIndexed { index, option ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                        val isSelected = option == currentOption
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setPassenger12hOption(option)
                                    showOptionSheet = false
                                }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = option.label,
                                style = styleData.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.tertiary
                                else
                                    primaryColor
                            )
                        }
                    }
                }
            }
        )
    }
}
