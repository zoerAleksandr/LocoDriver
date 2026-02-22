package com.z_company.iosapp.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z_company.domain.navigation.Router

/**
 * Главный экран (список маршрутов) — Шаг 16 заменит стаб реальным ViewModel.
 *
 * TODO Step 16: подключить HomeViewModel, отобразить список маршрутов из SQLDelight.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(router: Router) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("LocoDriver") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Маршруты",
                style = MaterialTheme.typography.titleLarge,
            )

            Text(
                text = "iOS · Compose Multiplatform\nЭкран в разработке — Step 16 подключит ViewModel.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Навигация — для проверки routing-графа
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { router.showRouteForm() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("+ Маршрут")
                }
                Button(
                    onClick = { router.showSettings() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Настройки")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { router.showSalaryCalculation() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Зарплата")
                }
                Button(
                    onClick = { router.showSearch() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Поиск")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Статус миграции KMP",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "✅ core, domain, data_local, data_remote\n" +
                            "✅ SQLDelight · Ktor Darwin · Keychain\n" +
                            "✅ Compose MP навигация\n" +
                            "⏳ features/ экраны (Шаг 16+)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
