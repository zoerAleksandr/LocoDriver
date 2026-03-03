package com.z_company.iosapp.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z_company.domain.navigation.Router
import com.z_company.iosapp.viewmodel.SettingsIosViewModel
import org.koin.compose.koinInject

/**
 * Settings screen with grouped Card sections and toggle switches for boolean settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(router: Router) {
    val viewModel: SettingsIosViewModel = koinInject()
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = { router.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val s = settings
        if (s == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Настройки не найдены",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // --- Section: General ---
            SettingsSectionCard(title = "Общие") {
                SettingsToggleRow(
                    label = "Учитывать будущие маршруты",
                    checked = s.isConsiderFutureRoute,
                    onCheckedChange = { viewModel.setConsiderFutureRoute(it) },
                )

                SettingsToggleRow(
                    label = "Показывать перерыв",
                    checked = s.isShowBreak,
                    onCheckedChange = { viewModel.setShowBreak(it) },
                )

                SettingsValueRow(
                    label = "Десятичное время",
                    value = if (s.isDecimalTime) "Да" else "Нет",
                )
            }

            // --- Section: Time ---
            SettingsSectionCard(title = "Время") {
                SettingsValueRow(
                    label = "Ночное время",
                    value = s.nightTime.toString(),
                )

                SettingsValueRow(
                    label = "Временная зона",
                    value = formatTimeZone(s.timeZone),
                )

                SettingsValueRow(
                    label = "Минимальный отдых в ПО",
                    value = formatHoursFromMs(s.minTimeRestPointOfTurnover),
                )

                SettingsValueRow(
                    label = "Минимальный домашний отдых",
                    value = formatHoursFromMs(s.minTimeHomeRest),
                )

                if (s.usingDefaultWorkTime) {
                    SettingsValueRow(
                        label = "Время работы по умолчанию",
                        value = formatHoursFromMs(s.defaultWorkTime),
                    )
                }
            }

            // --- Section: Locomotive ---
            SettingsSectionCard(title = "Локомотив") {
                SettingsValueRow(
                    label = "Тип по умолчанию",
                    value = s.defaultLocoType.text,
                )

                SettingsValueRow(
                    label = "Коэффициент дизтоплива",
                    value = s.lastEnteredDieselCoefficient.toString(),
                )

                if (s.locomotiveSeriesList.isNotEmpty()) {
                    SettingsValueRow(
                        label = "Серии локомотивов",
                        value = s.locomotiveSeriesList.joinToString(", "),
                    )
                }

                if (s.stationList.isNotEmpty()) {
                    SettingsValueRow(
                        label = "Станции",
                        value = s.stationList.joinToString(", "),
                    )
                }
            }

            // --- Navigation buttons ---
            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { router.showSettingSalary() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Настройка зарплаты")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Card-based section with a title header for grouping settings.
 */
@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

/**
 * A row with a label and a value text.
 */
@Composable
private fun SettingsValueRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * A row with a label and a Switch toggle.
 */
@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

/** Formats milliseconds as "X ч" (hours). */
private fun formatHoursFromMs(ms: Long): String {
    val hours = ms / 3_600_000L
    val minutes = (ms % 3_600_000L) / 60_000L
    return if (minutes > 0) "${hours} ч ${minutes} мин" else "${hours} ч"
}

/** Formats timezone offset to readable string. */
private fun formatTimeZone(timeZoneMs: Long): String {
    val totalHoursFromMoscow = timeZoneMs / 3_600_000L
    val moscowOffset = 3L + totalHoursFromMoscow
    return if (totalHoursFromMoscow == 0L) {
        "МСК (UTC+3)"
    } else {
        "UTC+$moscowOffset (МСК${if (totalHoursFromMoscow > 0) "+" else ""}${totalHoursFromMoscow})"
    }
}
