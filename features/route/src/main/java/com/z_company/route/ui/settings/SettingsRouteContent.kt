package com.z_company.route.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.z_company.core.ui.component.TimePickerApp
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
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
    val styleHint = MaterialTheme.typography.bodyMedium
    val styleTitle = MaterialTheme.typography.titleSmall
    val primaryColor = MaterialTheme.colorScheme.primary

    val sharedPrefs: SharedPreferencesRepositories = koinInject()
    var showWorkTimeDialog by remember { mutableStateOf(false) }

    if (showWorkTimeDialog) {
        TimePickerApp(
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
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Данные по умолчанию
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
                text = "Данные по умолчанию",
                style = styleTitle,
                color = primaryColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Column(
                modifier = Modifier
                    .shadow(elevation = 2.dp, shape = Shapes.medium)
                    .background(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = Shapes.medium
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .noRippleEffect { viewModel.changeDefaultLocoType() },
                    horizontalArrangement = Arrangement.SpaceBetween
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
                CustomDivider(orientation = Orientation.Horizontal)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .noRippleEffect { viewModel.changeTimeFormat() },
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
                        style = styleData,
                        color = primaryColor,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2
                    )
                }

                CustomDivider(orientation = Orientation.Horizontal)

                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        CustomDivider(orientation = Orientation.Horizontal)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .noRippleEffect { showWorkTimeDialog = true },
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
                                style = styleData,
                                color = primaryColor,
                            )
                        }
                    }
                }
            }

            Text(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                text = "Эти значения будут установлены по умолчанию при создании нового маршрута.",
                style = styleHint,
                color = primaryColor
            )
        }

        // Показывать перерыв
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .shadow(elevation = 2.dp, shape = Shapes.medium)
                    .background(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = Shapes.medium
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .weight(1f),
                    text = "Показывать перерыв",
                    style = styleData,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2
                )
                Switch(
                    checked = currentSettings.isShowBreak,
                    onCheckedChange = { changeShowBreak(it) }
                )
            }

            Text(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                text = "Показывать поля перерыва в форме маршрута.",
                style = styleHint,
                color = primaryColor
            )
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
        Text(
            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
            text = "Свыше 12-ти часов относить в следование пассажиром",
            style = styleTitle,
            color = primaryColor
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = Shapes.medium)
                .background(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = Shapes.medium
                )
                .clickable { showOptionSheet = true }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = currentOption.label,
                style = styleData,
                color = primaryColor
            )
        }

        Text(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            text = currentOption.hint,
            style = styleHint,
            color = primaryColor
        )
    }

    if (showOptionSheet) {
        val optionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showOptionSheet = false },
            sheetState = optionSheetState,
            containerColor = MaterialTheme.colorScheme.secondary,
            shape = Shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp)
            ) {
                Text(
                    text = "Свыше 12-ти часов относить в следование пассажиром",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    textAlign = TextAlign.Center
                )

                Passenger12hOption.entries.forEach { option ->
                    val isSelected = option == currentOption
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .shadow(
                                elevation = if (isSelected) 4.dp else 1.dp,
                                shape = Shapes.medium
                            )
                            .background(
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                else
                                    MaterialTheme.colorScheme.surface,
                                shape = Shapes.medium
                            )
                            .clickable {
                                viewModel.setPassenger12hOption(option)
                                showOptionSheet = false
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.secondary
                            else
                                primaryColor
                        )
                    }
                }
            }
        }
    }
}
