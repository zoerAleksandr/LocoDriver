package com.z_company.shared.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.z_company.shared.ui.component.AppBottomSheet
import com.z_company.shared.ui.component.BottomSheetAction
import com.z_company.shared.ui.component.picker.DateTimePickerBottomSheet
import com.z_company.shared.util.TimeFormatter
import com.z_company.shared.viewmodel.FormTrainSharedViewModel
import kotlin.time.Clock
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormTrainScreen(
    trainId: String?,
    basicId: String,
    onBackClick: () -> Unit,
) {
    val viewModel: FormTrainSharedViewModel = koinInject()
    val train by viewModel.train.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(trainId, basicId) { viewModel.loadTrain(trainId, basicId) }
    LaunchedEffect(isSaved) { if (isSaved) onBackClick() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    // Station time picker states — track which station index and which field (arrival/departure)
    var pickerStationIndex by remember { mutableStateOf(-1) }
    var pickerIsArrival by remember { mutableStateOf(true) }
    var showStationTimePicker by remember { mutableStateOf(false) }

    // Long-press delete for station time
    var deleteStationIndex by remember { mutableStateOf(-1) }
    var deleteIsArrival by remember { mutableStateOf(true) }
    var showDeleteStationTime by remember { mutableStateOf(false) }

    if (showDeleteStationTime && deleteStationIndex >= 0) {
        val timeLabel = if (deleteIsArrival) "Прибытие" else "Отправление"
        AppBottomSheet(
            onDismissRequest = { showDeleteStationTime = false },
            title = "$timeLabel (станция ${deleteStationIndex + 1})",
            actions = listOf(BottomSheetAction("Удалить значение") {
                if (deleteIsArrival) viewModel.updateStationArrival(deleteStationIndex, null)
                else viewModel.updateStationDeparture(deleteStationIndex, null)
            }),
        )
    }

    if (showStationTimePicker && pickerStationIndex >= 0) {
        val station = train?.stations?.getOrNull(pickerStationIndex)
        val timeLabel = if (pickerIsArrival) "Прибытие" else "Отправление"
        val currentMillis = if (pickerIsArrival) station?.timeArrival else station?.timeDeparture
        DateTimePickerBottomSheet(
            title = "$timeLabel (станция ${pickerStationIndex + 1})",
            onDateTimeSelected = { millis ->
                if (pickerIsArrival) viewModel.updateStationArrival(pickerStationIndex, millis)
                else viewModel.updateStationDeparture(pickerStationIndex, millis)
            },
            onDismiss = { showStationTimePicker = false },
            startDateTime = currentMillis ?: Clock.System.now().toEpochMilliseconds(),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Поезд") },
                navigationIcon = {
                    TextButton(
                        onClick = { viewModel.saveTrain() },
                        colors = ButtonDefaults.buttonColors(
                            contentColor = MaterialTheme.colorScheme.tertiary,
                            containerColor = Color.Transparent,
                        ),
                    ) {
                        Text(text = "Сохранить", style = MaterialTheme.typography.bodySmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (isLoading || train == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val t = train!!

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            state = rememberLazyListState(),
        ) {
            // --- Error card ---
            item {
                errorMessage?.let { msg ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                        ),
                    ) {
                        Text(
                            text = msg,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }

            // --- Basic data ---
            item {
                OutlinedTextField(
                    value = t.number ?: "",
                    onValueChange = { viewModel.updateNumber(it) },
                    label = { Text("Номер поезда") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // --- Characteristics ---
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Характеристики",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = t.weight ?: "",
                            onValueChange = { viewModel.updateWeight(it) },
                            label = { Text("Масса (т)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        OutlinedTextField(
                            value = t.axle ?: "",
                            onValueChange = { viewModel.updateAxle(it) },
                            label = { Text("Оси") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = t.conditionalLength ?: "",
                            onValueChange = { viewModel.updateConditionalLength(it) },
                            label = { Text("Усл. длина") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        OutlinedTextField(
                            value = t.distance ?: "",
                            onValueChange = { viewModel.updateDistance(it) },
                            label = { Text("Расстояние") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                }
            }

            // --- Stations ---
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Станции",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        TextButton(onClick = { viewModel.addStation() }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Добавить", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }

                    t.stations.forEachIndexed { index, station ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(elevation = 2.dp, shape = MaterialTheme.shapes.medium),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    Arrangement.SpaceBetween,
                                    Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Станция ${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Icon(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .combinedClickable(onClick = { viewModel.removeStation(index) }),
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                    )
                                }

                                OutlinedTextField(
                                    value = station.stationName ?: "",
                                    onValueChange = { viewModel.updateStationName(index, it) },
                                    label = { Text("Название") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )

                                // Arrival time with animated background
                                StationTimeRowAnimated(
                                    label = "Прибытие",
                                    millis = station.timeArrival,
                                    onClick = {
                                        pickerStationIndex = index
                                        pickerIsArrival = true
                                        showStationTimePicker = true
                                    },
                                    onLongClick = {
                                        deleteStationIndex = index
                                        deleteIsArrival = true
                                        showDeleteStationTime = true
                                    },
                                )

                                // Departure time with animated background
                                StationTimeRowAnimated(
                                    label = "Отправление",
                                    millis = station.timeDeparture,
                                    onClick = {
                                        pickerStationIndex = index
                                        pickerIsArrival = false
                                        showStationTimePicker = true
                                    },
                                    onLongClick = {
                                        deleteStationIndex = index
                                        deleteIsArrival = false
                                        showDeleteStationTime = true
                                    },
                                )

                                val stationDuration = computeDurationMs(station.timeArrival, station.timeDeparture)
                                if (stationDuration > 0) {
                                    FormReadOnlyRow("Стоянка", TimeFormatter.formatDuration(stationDuration))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun StationTimeRowAnimated(
    label: String,
    millis: Long?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val animatedBgColor by animateColorAsState(
        targetValue = if (millis != null) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = MaterialTheme.shapes.medium)
            .background(color = animatedBgColor, shape = MaterialTheme.shapes.medium)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Light),
        )
        Text(
            text = if (millis != null) TimeFormatter.formatDateTime(millis) else "",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
