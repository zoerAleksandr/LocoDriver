package com.z_company.iosapp.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DirectionsRailway
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.domain.entities.route.Route
import com.z_company.domain.navigation.Router
import com.z_company.iosapp.util.TimeFormatter
import com.z_company.iosapp.viewmodel.HomeIosViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(router: Router) {
    val viewModel: HomeIosViewModel = koinInject()
    val routes by viewModel.routes.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val monthLabel = settings?.selectMonthOfYear?.let { moy ->
        "${TimeFormatter.monthNames.getOrElse(moy.month) { "?" }} ${moy.year}"
    } ?: "Загрузка\u2026"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LocoDriver") },
                actions = {
                    IconButton(onClick = { router.showSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { router.showRouteForm() }) {
                Icon(Icons.Default.Add, contentDescription = "Новый маршрут")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Month selector row
            MonthSelector(
                monthLabel = monthLabel,
                onPrevious = { viewModel.setPreviousMonth() },
                onNext = { viewModel.setNextMonth() },
            )

            // Quick action buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = { router.showSalaryCalculation() },
                    label = { Text("Зарплата") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.AttachMoney,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                AssistChip(
                    onClick = { router.showSettings() },
                    label = { Text("Настройки") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }

                routes.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Icon(
                                Icons.Default.DirectionsRailway,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                            Text(
                                text = "Нет маршрутов за $monthLabel",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FilledTonalButton(onClick = { router.showRouteForm() }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Добавить маршрут")
                            }
                        }
                    }
                }

                else -> {
                    // Route count header
                    Text(
                        text = "Маршрутов: ${routes.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 88.dp),
                    ) {
                        items(routes, key = { it.basicData.id }) { route ->
                            RouteCard(
                                route = route,
                                onClick = { router.showRouteDetails(route.basicData) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(
    monthLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Предыдущий месяц")
        }
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Следующий месяц")
        }
    }
}

@Composable
private fun RouteCard(route: Route, onClick: () -> Unit) {
    val basicData = route.basicData
    val workDuration = run {
        val start = basicData.timeStartWork ?: return@run null
        val end = basicData.timeEndWork ?: return@run null
        if (end > start) end - start else null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row: number + favorite icon + work duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = basicData.number?.takeIf { it.isNotBlank() } ?: "Без номера",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    if (basicData.isFavorite) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Избранное",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                workDuration?.let {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = TimeFormatter.formatDuration(it),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Time row: start -> end
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                basicData.timeStartWork?.let { start ->
                    Text(
                        text = TimeFormatter.formatDateTimeShort(start),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                basicData.timeEndWork?.let {
                    Text(
                        text = "\u2192",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = TimeFormatter.formatDateTimeShort(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Break info
            if (basicData.timeStartBreak != null && basicData.timeEndBreak != null) {
                val breakDuration = basicData.timeEndBreak!! - basicData.timeStartBreak!!
                if (breakDuration > 0) {
                    Text(
                        text = "Перерыв: ${TimeFormatter.formatDuration(breakDuration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            // Counters: locomotives, trains, passengers
            val locoCount = route.locomotives.size
            val trainCount = route.trains.size
            val passCount = route.passengers.size
            if (locoCount > 0 || trainCount > 0 || passCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (locoCount > 0) {
                        CountBadge(icon = Icons.Default.Train, count = locoCount, label = "лок.")
                    }
                    if (trainCount > 0) {
                        CountBadge(
                            icon = Icons.Default.DirectionsRailway,
                            count = trainCount,
                            label = "поезд.",
                        )
                    }
                    if (passCount > 0) {
                        CountBadge(icon = Icons.Default.Person, count = passCount, label = "пасс.")
                    }
                }
            }

            // Notes preview
            basicData.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CountBadge(icon: ImageVector, count: Int, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "$count $label",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
