package com.z_company.route.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.component.CustomDivider
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.MonthFullText.getMonthFullText
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.setting.CrossMonthTimezone
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.route.component.AnimationDialog
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.viewmodel.CountryLoadingState
import com.z_company.route.viewmodel.TimeZoneRussia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNormaContent(
    currentSettings: UserSettings,
    showReleaseDaySelectScreen: () -> Unit,
    timeZoneRussiaList: List<TimeZoneRussia>,
    setTimeZone: (Long) -> Unit,
    setCountry: (String) -> Unit,
    countryLoadingState: CountryLoadingState? = null,
    onDismissCountryDialog: () -> Unit = {},
    setCrossMonthTimezone: (CrossMonthTimezone) -> Unit = {},
) {
    val styleData = MaterialTheme.typography.bodyLarge
    val styleHint = MaterialTheme.typography.bodyMedium
    val styleTitle = MaterialTheme.typography.titleSmall
    val primaryColor = MaterialTheme.colorScheme.primary

    countryLoadingState?.let { state ->
        AnimationDialog(
            showDialog = true,
            onDismissRequest = {
                if (state !is CountryLoadingState.Loading) onDismissCountryDialog()
            }
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, shape = Shapes.medium)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (state) {
                    is CountryLoadingState.Loading -> {
                        CircularProgressIndicator()
                        Text(
                            text = "Загружаем производственный календарь для ${state.countryName}",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                    CountryLoadingState.Success -> {
                        Text(
                            text = "Календарь успешно загружен",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = onDismissCountryDialog) {
                            Text("OK")
                        }
                    }
                    CountryLoadingState.Error -> {
                        Text(
                            text = "Ошибка загрузки",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(onClick = onDismissCountryDialog) {
                            Text("OK")
                        }
                    }
                    CountryLoadingState.NoInternet -> {
                        Text(
                            text = "Нет интернета",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(onClick = onDismissCountryDialog) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Норма часов
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
                text = "Норма часов",
                style = styleTitle
            )
            Column(
                modifier = Modifier
                    .shadow(elevation = 2.dp, shape = Shapes.medium)
                    .background(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = Shapes.medium
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val currentMonth =
                        getMonthFullText(currentSettings.selectMonthOfYear.month)
                    val personalNormaText =
                        ConverterLongToTime.getTimeInStringFormat(
                            currentSettings.selectMonthOfYear.getPersonalNormaHours()
                                .toLong()
                                .times(3_600_000)
                        )

                    Text(
                        text = currentMonth,
                        color = primaryColor,
                        style = styleData
                    )

                    Text(
                        text = personalNormaText,
                        style = styleData,
                        color = primaryColor,
                    )
                }
                CustomDivider(orientation = Orientation.Horizontal)

                Text(
                    modifier = Modifier.clickable {
                        showReleaseDaySelectScreen()
                    },
                    text = "Изменить норму",
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Страна производственного календаря
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
                text = "Производственный календарь",
                style = styleTitle,
                color = primaryColor,
            )
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = Shapes.medium
                    )
                    .fillMaxWidth()
            ) {
                data class CountryOption(val code: String, val name: String, val flag: String)
                val countries = listOf(
                    CountryOption("RU", "Россия", "🇷🇺"),
                    CountryOption("KZ", "Казахстан", "🇰🇿"),
                    CountryOption("BY", "Беларусь", "🇧🇾"),
                )

                var expanded by remember { mutableStateOf(false) }
                val selected = countries.find { it.code == currentSettings.country } ?: countries[0]

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextFieldApp(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        value = "${selected.flag} ${selected.name}",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        textStyle = styleData.copy(color = MaterialTheme.colorScheme.primary)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        countries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${option.flag} ${option.name}",
                                        color = primaryColor,
                                        style = styleHint
                                    )
                                },
                                onClick = {
                                    setCountry(option.code)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Часовой пояс
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
                text = "Домашний часовой пояс",
                style = styleTitle,
                color = primaryColor,
                overflow = TextOverflow.Ellipsis
            )
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = Shapes.medium
                        )
                        .fillMaxWidth()
                ) {
                    when (currentSettings.country) {
                        "KZ" -> {
                            OutlinedTextFieldApp(
                                modifier = Modifier.fillMaxWidth(),
                                value = "UTC+5 (Kazakhstan Time, KZT)",
                                onValueChange = {},
                                readOnly = true,
                                textStyle = styleData.copy(color = MaterialTheme.colorScheme.primary)
                            )
                        }
                        "BY" -> {
                            OutlinedTextFieldApp(
                                modifier = Modifier.fillMaxWidth(),
                                value = "UTC+3 (Минск)",
                                onValueChange = {},
                                readOnly = true,
                                textStyle = styleData.copy(color = MaterialTheme.colorScheme.primary)
                            )
                        }
                        else -> {
                            val currentTimeZone: TimeZoneRussia =
                                timeZoneRussiaList.find {
                                    it.offsetOfMoscow == currentSettings.timeZone
                                } ?: timeZoneRussiaList[1]

                            var selectedTimeZone by remember(currentSettings.timeZone) {
                                mutableStateOf(currentTimeZone)
                            }
                            var expanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextFieldApp(
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    value = selectedTimeZone.description,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    },
                                    textStyle = styleData.copy(color = MaterialTheme.colorScheme.primary)
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    timeZoneRussiaList.forEach { item ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = item.description,
                                                    color = primaryColor,
                                                    style = styleHint
                                                )
                                            },
                                            onClick = {
                                                selectedTimeZone = item
                                                setTimeZone(item.offsetOfMoscow)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Text(
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    text = "Установите местный часовой пояс. Будет учитываться при расчете ночных, праздничных часов и переходных поездках.",
                    style = styleHint,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Visible
                )
            }
        }

        // Переходные маршруты — только для России с часовым поясом, отличным от московского
        if (currentSettings.country == "RU" && currentSettings.timeZone != 0L) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
                    text = "Переходные маршруты",
                    style = styleTitle,
                    color = primaryColor,
                )
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, shape = Shapes.medium)
                            .fillMaxWidth()
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        val options = listOf(
                            CrossMonthTimezone.LOCAL to "По местному времени",
                            CrossMonthTimezone.MOSCOW to "По московскому времени"
                        )
                        val current = options.find { it.first == currentSettings.crossMonthTimezone } ?: options[0]
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextFieldApp(
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                value = current.second,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                textStyle = styleData.copy(color = MaterialTheme.colorScheme.primary)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                options.forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = label,
                                                color = primaryColor,
                                                style = styleHint
                                            )
                                        },
                                        onClick = {
                                            setCrossMonthTimezone(value)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                        text = "Определяет по какому времени считается к какому месяцу относится переходной маршрут.",
                        style = styleHint,
                    )
                }
            }
        }
    }
}
