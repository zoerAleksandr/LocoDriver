package com.z_company.iosapp.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.domain.navigation.Router
import com.z_company.iosapp.util.TimeFormatter
import com.z_company.iosapp.viewmodel.DayCell
import com.z_company.iosapp.viewmodel.WorkScheduleIosViewModel
import org.koin.compose.koinInject

private val dayOfWeekLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkScheduleScreen(router: Router) {
    val viewModel: WorkScheduleIosViewModel = koinInject()
    val settings by viewModel.settings.collectAsState()
    val dayCells by viewModel.dayCells.collectAsState()
    val totalRouteCount by viewModel.totalRouteCount.collectAsState()
    val totalWorkMs by viewModel.totalWorkMs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val monthLabel = settings?.selectMonthOfYear?.let { moy ->
        "${WorkScheduleIosViewModel.monthNames.getOrElse(moy.month) { "?" }} ${moy.year}"
    } ?: "Загрузка..."

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("График работы") },
                navigationIcon = {
                    IconButton(onClick = { router.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            // Month selector
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { viewModel.setPreviousMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Предыдущий месяц")
                }
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = { viewModel.setNextMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Следующий месяц")
                }
            }

            // Day of week header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                dayOfWeekLabels.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Calendar grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(dayCells) { cell ->
                        CalendarDayCell(cell = cell)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Итого за месяц",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Маршрутов",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = "$totalRouteCount",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Общее рабочее время",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = TimeFormatter.formatDuration(totalWorkMs),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(cell: DayCell) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .then(
                if (cell.hasRoute)
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                else
                    Modifier
            )
            .then(
                if (cell.dayOfMonth > 0)
                    Modifier.border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(4.dp),
                    )
                else
                    Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (cell.dayOfMonth > 0) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = cell.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = if (cell.hasRoute) FontWeight.Bold else FontWeight.Normal,
                    color = if (cell.hasRoute)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface,
                )
                if (cell.hasRoute) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
    }
}
