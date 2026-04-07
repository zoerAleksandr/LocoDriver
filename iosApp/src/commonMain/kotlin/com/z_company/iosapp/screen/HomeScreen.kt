@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getBreakDuration
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.isFuture
import com.z_company.domain.entities.route.UtilsForEntities.isTransition
import com.z_company.domain.navigation.Router
import com.z_company.iosapp.viewmodel.HomeIosViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

/**
 * Главный экран: список маршрутов текущего месяца.
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
    val borderColor = when {
        route.isFuture(0L) -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        route.isTransition(0L) -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val (timeText, workTimeText) = remember(route) {
        val start = route.basicData.timeStartWork
        val end = route.basicData.timeEndWork
        val timeStr = when {
            start != null && end != null -> {
                val startStr = formatDateTime(start)
                val endStr = if (isSameDateMs(start, end)) formatTime(end) else formatDateTime(end)
                "$startStr — $endStr"
            }
            start != null -> formatDateTime(start)
            else -> "—"
        }
        val workTime = route.getWorkTime()
        val workStr = if (workTime != null && workTime > 0L) durationToString(workTime) else ""
        timeStr to workStr
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onClick,
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Время начало — конец | продолжительность работы
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                if (workTimeText.isNotBlank()) {
                    Text(
                        text = workTimeText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Поезд: номер + маршрут (первый)
            route.trains.firstOrNull()?.let { train ->
                val trainNumber = if (!train.number.isNullOrBlank()) "№${train.number} " else ""
                val stationStart = train.stations.firstOrNull()?.stationName ?: ""
                val stationEnd = if (train.stations.size > 1) " — ${train.stations.last().stationName ?: ""}" else ""
                val trainText = "$trainNumber$stationStart$stationEnd"
                if (trainText.isNotBlank()) {
                    Text(
                        text = trainText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
            }

            // Заметки
            route.basicData.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                )
            }

            // Номер маршрута + иконки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = route.basicData.number ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (route.basicData.isOnePersonOperation) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (route.getBreakDuration() > 0L) {
                        Icon(
                            Icons.Filled.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    route.getPassengerTime()?.let { time ->
                        if (time > 0L) {
                            Icon(
                                Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    route.getWorkTime()?.let { time ->
                        if (time > 12 * 3_600_000L) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFf1642e),
                            )
                        }
                    }
                    if (route.basicData.isFavorite) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFf1642e),
                        )
                    }
                    if (route.basicData.isSynchronized) {
                        Icon(
                            Icons.Filled.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            Icons.Filled.SyncDisabled,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

/** "DD.MM HH:MM" */
private fun formatDateTime(millis: Long): String {
    val ldt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    val d = ldt.dayOfMonth.toString().padStart(2, '0')
    val mo = ldt.monthNumber.toString().padStart(2, '0')
    val h = ldt.hour.toString().padStart(2, '0')
    val min = ldt.minute.toString().padStart(2, '0')
    return "$d.$mo $h:$min"
}

/** "HH:MM" (только время) */
private fun formatTime(millis: Long): String {
    val ldt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    val h = ldt.hour.toString().padStart(2, '0')
    val min = ldt.minute.toString().padStart(2, '0')
    return "$h:$min"
}

/** True если оба timestamp в один календарный день */
private fun isSameDateMs(startMs: Long, endMs: Long): Boolean {
    val tz = TimeZone.currentSystemDefault()
    val start = Instant.fromEpochMilliseconds(startMs).toLocalDateTime(tz)
    val end = Instant.fromEpochMilliseconds(endMs).toLocalDateTime(tz)
    return start.date == end.date
}

/** Duration в "HH:MM" */
private fun durationToString(millis: Long): String {
    val totalMinutes = millis / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}
