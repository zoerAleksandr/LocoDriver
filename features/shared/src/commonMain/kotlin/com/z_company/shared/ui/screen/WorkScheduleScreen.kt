package com.z_company.shared.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.shared.util.ConverterLongToTime
import com.z_company.shared.viewmodel.WorkScheduleSharedViewModel
import org.koin.compose.koinInject

private val dayOfWeekLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkScheduleScreen(
    onBackClick: () -> Unit,
) {
    val viewModel: WorkScheduleSharedViewModel = koinInject()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val routesByDay by viewModel.routesByDay.collectAsState()
    val totalTimeWork by viewModel.totalTimeWork.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val timeButtons by viewModel.timeButtons.collectAsState()
    val activeTime by viewModel.activeTime.collectAsState()
    val selectedDays by viewModel.selectedDays.collectAsState()
    val isDeleteMode by viewModel.isDeleteMode.collectAsState()
    val selectedRoutesToDelete by viewModel.selectedRoutesToDelete.collectAsState()
    val totalReleaseHours by viewModel.totalReleaseHours.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.prepareScreen()
    }

    val month = currentMonth
    val monthLabel = if (month != null) {
        "${WorkScheduleSharedViewModel.monthNames.getOrElse(month.month) { "?" }} ${month.year}"
    } else {
        "Загрузка..."
    }

    val year = month?.year ?: 2024
    val monthNum = (month?.month ?: 0) + 1
    val daysInMonth = WorkScheduleSharedViewModel.getDaysInMonth(year, monthNum)
    val firstDow = WorkScheduleSharedViewModel.getFirstDayOfWeekInMonth(year, monthNum)
    val leadingEmpty = (firstDow - 1).coerceAtLeast(0)
    val dayCells = buildList {
        repeat(leadingEmpty) { add(0) }
        for (d in 1..daysInMonth) add(d)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("График работы") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleDeleteMode() }) {
                        Icon(
                            imageVector = if (isDeleteMode) Icons.Default.Done else Icons.Default.Delete,
                            contentDescription = if (isDeleteMode) "Готово" else "Удалить",
                            tint = if (isDeleteMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        )
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
            // Month label
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
            )

            // Day of week headers
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(dayCells) { day ->
                        val routes = routesByDay[day] ?: emptyList()
                        val hasRoute = routes.isNotEmpty()
                        val isSelected = selectedDays.containsKey(day)
                        val isDeleteSelected = selectedRoutesToDelete.containsKey(day)

                        CalendarDayCell(
                            day = day,
                            hasRoute = hasRoute,
                            routeCount = routes.size,
                            isSelected = isSelected,
                            isDeleteSelected = isDeleteSelected,
                            isDeleteMode = isDeleteMode,
                            onClick = {
                                if (day > 0) {
                                    if (isDeleteMode) {
                                        viewModel.onDayDeleteClicked(day)
                                    } else if (activeTime != null) {
                                        viewModel.togglePlannedTimeForDay(day, activeTime!!)
                                    }
                                }
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Time buttons
            if (!isDeleteMode) {
                Text(
                    text = "Время явки",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    timeButtons.forEach { time ->
                        FilterChip(
                            selected = activeTime == time,
                            onClick = {
                                viewModel.setActiveTime(if (activeTime == time) null else time)
                            },
                            label = {
                                Text(ConverterLongToTime.getTimeInStringFormat(time))
                            },
                        )
                    }
                }

                if (selectedDays.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val workDuration = userSettings?.let {
                        if (it.usingDefaultWorkTime) it.defaultWorkTime else null
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.saveSelectedSchedules(workDuration) },
                            label = { Text("Сохранить (${selectedDays.size} дн.)") },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                        )
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.resetSelectedDays() },
                            label = { Text("Сбросить") },
                        )
                    }
                }
            }

            if (isDeleteMode && selectedRoutesToDelete.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FilterChip(
                    selected = true,
                    onClick = { viewModel.deleteSelectedRoutes() },
                    label = { Text("Удалить выбранные (${selectedRoutesToDelete.values.sumOf { it.size }})") },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

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
                            text = "${routesByDay.values.sumOf { it.size }}",
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
                            text = "Рабочее время",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = ConverterLongToTime.getTimeInStringFormat(totalTimeWork),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    if (totalReleaseHours > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Часы отдыха",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                text = ConverterLongToTime.getTimeInStringFormat(totalReleaseHours),
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
}

@Composable
private fun CalendarDayCell(
    day: Int,
    hasRoute: Boolean,
    routeCount: Int,
    isSelected: Boolean,
    isDeleteSelected: Boolean,
    isDeleteMode: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = when {
        isDeleteSelected -> MaterialTheme.colorScheme.errorContainer
        isSelected -> MaterialTheme.colorScheme.tertiaryContainer
        hasRoute -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .then(
                if (day > 0)
                    Modifier
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(4.dp),
                        )
                        .clickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (day > 0) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = if (hasRoute) FontWeight.Bold else FontWeight.Normal,
                    color = if (hasRoute)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface,
                )
                if (hasRoute && routeCount > 0) {
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
