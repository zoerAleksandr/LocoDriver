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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import com.z_company.shared.ui.component.AppBottomSheet
import com.z_company.shared.ui.component.BottomSheetAction
import com.z_company.shared.ui.component.picker.DateTimePickerBottomSheet
import com.z_company.shared.util.TimeFormatter
import com.z_company.shared.viewmodel.FormPassengerSharedViewModel
import kotlin.time.Clock
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormPassengerScreen(
    passengerId: String?,
    basicId: String,
    onBackClick: () -> Unit,
) {
    val viewModel: FormPassengerSharedViewModel = koinInject()
    val passenger by viewModel.passenger.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(passengerId, basicId) { viewModel.loadPassenger(passengerId, basicId) }
    LaunchedEffect(isSaved) { if (isSaved) onBackClick() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    // Time picker states
    var showDeparturePicker by remember { mutableStateOf(false) }
    var showArrivalPicker by remember { mutableStateOf(false) }

    // Delete bottom sheet states
    var showDeleteDeparture by remember { mutableStateOf(false) }
    var showDeleteArrival by remember { mutableStateOf(false) }

    // Delete bottom sheets
    if (showDeleteDeparture) {
        AppBottomSheet(
            onDismissRequest = { showDeleteDeparture = false },
            title = "Время отправления",
            actions = listOf(BottomSheetAction("Удалить значение") { viewModel.setTimeDeparture(null) }),
        )
    }
    if (showDeleteArrival) {
        AppBottomSheet(
            onDismissRequest = { showDeleteArrival = false },
            title = "Время прибытия",
            actions = listOf(BottomSheetAction("Удалить значение") { viewModel.setTimeArrival(null) }),
        )
    }

    // Time pickers
    if (showDeparturePicker) {
        DateTimePickerBottomSheet(
            title = "Время отправления",
            onDateTimeSelected = { viewModel.setTimeDeparture(it) },
            onDismiss = { showDeparturePicker = false },
            startDateTime = passenger?.timeDeparture ?: Clock.System.now().toEpochMilliseconds(),
        )
    }
    if (showArrivalPicker) {
        DateTimePickerBottomSheet(
            title = "Время прибытия",
            onDateTimeSelected = { viewModel.setTimeArrival(it) },
            onDismiss = { showArrivalPicker = false },
            startDateTime = passenger?.timeArrival ?: passenger?.timeDeparture ?: Clock.System.now().toEpochMilliseconds(),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Пассажиром") },
                navigationIcon = {
                    TextButton(
                        onClick = { viewModel.savePassenger() },
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
        if (isLoading || passenger == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val p = passenger!!

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

            // --- Train number ---
            item {
                OutlinedTextField(
                    value = p.trainNumber ?: "",
                    onValueChange = { viewModel.updateTrainNumber(it) },
                    label = { Text("Номер поезда") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // --- Stations ---
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Маршрут",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedTextField(
                        value = p.stationDeparture ?: "",
                        onValueChange = { viewModel.updateStationDeparture(it) },
                        label = { Text("Станция отправления") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = p.stationArrival ?: "",
                        onValueChange = { viewModel.updateStationArrival(it) },
                        label = { Text("Станция прибытия") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }

            // --- Times ---
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Время",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    PassengerTimeRowAnimated(
                        label = "Отправление",
                        millis = p.timeDeparture,
                        onClick = { showDeparturePicker = true },
                        onLongClick = { showDeleteDeparture = true },
                    )
                    PassengerTimeRowAnimated(
                        label = "Прибытие",
                        millis = p.timeArrival,
                        onClick = { showArrivalPicker = true },
                        onLongClick = { showDeleteArrival = true },
                    )
                    val travelDuration = computeDurationMs(p.timeDeparture, p.timeArrival)
                    if (travelDuration > 0) {
                        FormReadOnlyRow("В пути", TimeFormatter.formatDuration(travelDuration))
                    }
                }
            }

            // --- Notes ---
            item {
                OutlinedTextField(
                    value = p.notes ?: "",
                    onValueChange = { viewModel.updateNotes(it) },
                    label = { Text("Примечание") },
                    modifier = Modifier.fillMaxWidth().heightIn(max = 105.dp),
                    minLines = 2,
                    maxLines = 4,
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun PassengerTimeRowAnimated(
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
