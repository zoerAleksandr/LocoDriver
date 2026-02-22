package com.z_company.iosapp.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
 * Экран настроек — читает UserSettings из SQLDelight через SettingsIosViewModel.
 *
 * Шаг 17: добавить редактирование полей (nightTime, timeZone, defaultLocoType и т.д.).
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
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                settings?.let { s ->
                    SettingRow("Тип локомотива по умолчанию", s.defaultLocoType.name)
                    SettingRow("Минимальный отдых (ч)", "${s.minTimeRestPointOfTurnover / 3_600_000}")
                    SettingRow("Минимальный домашний отдых (ч)", "${s.minTimeHomeRest / 3_600_000}")
                    SettingRow("Ночное время", "${s.nightTime.startNightHour}:00 – ${s.nightTime.endNightHour}:00")
                    SettingRow("Временная зона", "${s.timeZone / 3_600_000L} ч от UTC+3")
                    SettingRow("Дизельный коэффициент", "${s.lastEnteredDieselCoefficient}")
                    SettingRow("Учитывать будущие маршруты", if (s.isConsiderFutureRoute) "Да" else "Нет")
                } ?: Text(
                    text = "Настройки не найдены",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = { router.showSettingSalary() }) {
                    Text("Настройка зарплаты")
                }
                Button(onClick = { router.back() }) {
                    Text("Назад")
                }
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}
