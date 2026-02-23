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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.z_company.domain.entities.route.Route
import com.z_company.domain.navigation.Router
import com.z_company.iosapp.viewmodel.HomeIosViewModel
import org.koin.compose.koinInject

/**
 * Главный экран: список маршрутов текущего месяца.
 *
 * Подключён к HomeIosViewModel (KMP) через koinInject().
 *
 * Шаг 17: добавить отображение итоговой зарплаты, пагинацию, жесты (swipe-to-delete).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(router: Router) {
    val viewModel: HomeIosViewModel = koinInject()
    val routes by viewModel.routes.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val monthLabel = settings?.selectMonthOfYear?.let { moy ->
        val monthNames = listOf(
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        )
        "${monthNames.getOrElse(moy.month) { "?" }} ${moy.year}"
    } ?: "Загрузка…"

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("LocoDriver · $monthLabel") })
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            routes.isEmpty() -> {
                EmptyRoutesContent(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    router = router,
                )
            }

            else -> {
                RouteListContent(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    routes = routes,
                    router = router,
                )
            }
        }
    }
}

@Composable
private fun EmptyRoutesContent(modifier: Modifier, router: Router) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = "Маршрутов нет",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = { router.showRouteForm() }) {
                Text("+ Добавить маршрут")
            }
        }
    }
}

@Composable
private fun RouteListContent(modifier: Modifier, routes: List<Route>, router: Router) {
    LazyColumn(modifier = modifier) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { router.showRouteForm() },
                    modifier = Modifier.weight(1f),
                ) { Text("+ Маршрут") }
                Button(
                    onClick = { router.showSalaryCalculation() },
                    modifier = Modifier.weight(1f),
                ) { Text("Зарплата") }
                Button(
                    onClick = { router.showSettings() },
                    modifier = Modifier.weight(1f),
                ) { Text("Настройки") }
            }
        }

        items(routes) { route ->
            RouteCard(route = route, onClick = { router.showRouteDetails(route.basicData) })
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun RouteCard(route: Route, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val number = route.basicData.number?.takeIf { it.isNotBlank() } ?: "Без номера"
            Text(
                text = number,
                style = MaterialTheme.typography.titleSmall,
            )
            route.basicData.timeStartWork?.let { start ->
                Text(
                    text = "Начало: ${formatEpochMs(start)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            route.basicData.timeEndWork?.let { end ->
                Text(
                    text = "Конец: ${formatEpochMs(end)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatEpochMs(millis: Long): String {
    val totalSec = millis / 1000L
    val totalMin = totalSec / 60
    val totalHour = totalMin / 60
    val days = totalHour / 24
    val hours = totalHour % 24
    val minutes = totalMin % 60
    // Простое форматирование без java.util.Date — kotlinx-datetime не нужен для отображения
    return "$days д. $hours:${minutes.toString().padStart(2, '0')}"
}
