package com.z_company.shared.ui.screen

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRailway
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Train
import com.z_company.shared.util.TimeFormatter
import com.z_company.shared.viewmodel.FormRouteSharedViewModel
import org.koin.compose.koinInject

/**
 * Route create/edit screen.
 *
 * [routeId] == null → new route; non-null → editing existing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormRouteScreen(
    routeId: String?,
    onBackClick: () -> Unit,
    onLocoClick: (locoId: String, basicId: String) -> Unit,
    onNewLocoClick: (basicId: String) -> Unit,
    onTrainClick: (trainId: String, basicId: String) -> Unit,
    onNewTrainClick: (basicId: String) -> Unit,
    onPassengerClick: (passengerId: String, basicId: String) -> Unit,
    onNewPassengerClick: (basicId: String) -> Unit,
) {
    val viewModel: FormRouteSharedViewModel = koinInject()
    val route by viewModel.route.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(routeId) {
        viewModel.loadRoute(routeId)
    }
    LaunchedEffect(isSaved) {
        if (isSaved) onBackClick()
    }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (routeId == null) "Новый маршрут" else "Маршрут") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    val isFavorite = route?.basicData?.isFavorite ?: false
                    IconToggleButton(
                        checked = isFavorite,
                        onCheckedChange = { viewModel.toggleFavorite(it) },
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "Избранное",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val currentRoute = route
        val basicData = currentRoute?.basicData

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // --- Section: Route Number ---
            FormSectionCard(title = "Основные данные") {
                OutlinedTextField(
                    value = basicData?.number ?: "",
                    onValueChange = { viewModel.updateNumber(it) },
                    label = { Text("Номер маршрута") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // --- Section: Work Times ---
            FormSectionCard(title = "Время работы") {
                FormReadOnlyRow(
                    label = "Начало работы",
                    value = basicData?.timeStartWork?.let { TimeFormatter.formatDateTime(it) } ?: "—",
                )
                FormReadOnlyRow(
                    label = "Окончание работы",
                    value = basicData?.timeEndWork?.let { TimeFormatter.formatDateTime(it) } ?: "—",
                )

                val workDurationMs = computeWorkDurationWithBreak(
                    startMs = basicData?.timeStartWork,
                    endMs = basicData?.timeEndWork,
                    breakStartMs = basicData?.timeStartBreak,
                    breakEndMs = basicData?.timeEndBreak,
                )
                if (workDurationMs != null && workDurationMs > 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    FormReadOnlyRow(
                        label = "Продолжительность",
                        value = TimeFormatter.formatDuration(workDurationMs),
                    )
                }
            }

            // --- Section: Break Times ---
            val hasBreak = basicData?.timeStartBreak != null || basicData?.timeEndBreak != null
            if (hasBreak) {
                FormSectionCard(title = "Перерыв") {
                    FormReadOnlyRow(
                        label = "Начало перерыва",
                        value = basicData?.timeStartBreak?.let { TimeFormatter.formatDateTime(it) } ?: "—",
                    )
                    FormReadOnlyRow(
                        label = "Окончание перерыва",
                        value = basicData?.timeEndBreak?.let { TimeFormatter.formatDateTime(it) } ?: "—",
                    )
                    val breakStart = basicData?.timeStartBreak
                    val breakEnd = basicData?.timeEndBreak
                    if (breakStart != null && breakEnd != null && breakEnd > breakStart) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        FormReadOnlyRow(
                            label = "Длительность перерыва",
                            value = TimeFormatter.formatDuration(breakEnd - breakStart),
                        )
                    }
                }
            }

            // --- Section: Locomotives ---
            currentRoute?.let { r ->
                if (r.locomotives.isNotEmpty()) {
                    FormSectionCard(title = "Локомотивы") {
                        r.locomotives.forEachIndexed { index, loco ->
                            LocoRow(
                                loco = loco,
                                onClick = { onLocoClick(loco.locoId, r.basicData.id) },
                                onDelete = { viewModel.deleteLoco(loco) },
                            )
                            if (index < r.locomotives.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }

            // --- Section: Trains ---
            currentRoute?.let { r ->
                if (r.trains.isNotEmpty()) {
                    FormSectionCard(title = "Поезда") {
                        r.trains.forEachIndexed { index, train ->
                            TrainRow(
                                train = train,
                                onClick = { onTrainClick(train.trainId, r.basicData.id) },
                                onDelete = { viewModel.deleteTrain(train) },
                            )
                            if (index < r.trains.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }

            // --- Section: Passengers ---
            currentRoute?.let { r ->
                if (r.passengers.isNotEmpty()) {
                    FormSectionCard(title = "Пассажирские") {
                        r.passengers.forEachIndexed { index, passenger ->
                            PassengerRow(
                                passenger = passenger,
                                onClick = { onPassengerClick(passenger.passengerId, r.basicData.id) },
                                onDelete = { viewModel.deletePassenger(passenger) },
                            )
                            if (index < r.passengers.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }

            // --- Add child entity buttons ---
            currentRoute?.let { r ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = {
                            viewModel.preSaveRoute { basicId -> onNewLocoClick(basicId) }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Train, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" Лок.")
                    }
                    TextButton(
                        onClick = {
                            viewModel.preSaveRoute { basicId -> onNewTrainClick(basicId) }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.DirectionsRailway, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" Поезд")
                    }
                    TextButton(
                        onClick = {
                            viewModel.preSaveRoute { basicId -> onNewPassengerClick(basicId) }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" Пасс.")
                    }
                }
            }

            // --- Section: Notes ---
            FormSectionCard(title = "Заметки") {
                OutlinedTextField(
                    value = basicData?.notes ?: "",
                    onValueChange = { viewModel.updateNotes(it) },
                    label = { Text("Заметки") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // --- Section: Additional ---
            FormSectionCard(title = "Дополнительно") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Избранное", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = basicData?.isFavorite ?: false,
                        onCheckedChange = { viewModel.toggleFavorite(it) },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Работа в одно лицо", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = basicData?.isOnePersonOperation ?: false,
                        onCheckedChange = { viewModel.setOnePersonOperation(it) },
                    )
                }
            }

            // --- Save Button ---
            Button(
                onClick = { viewModel.saveRoute() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LocoRow(
    loco: Locomotive,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildString {
                    loco.series?.let { append(it) }
                    loco.number?.let { if (isNotEmpty()) append(" "); append("№$it") }
                    if (isEmpty()) append("Локомотив")
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = loco.type.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Close, contentDescription = "Удалить", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TrainRow(
    train: Train,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = train.number?.takeIf { it.isNotBlank() }?.let { "Поезд №$it" } ?: "Поезд",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Close, contentDescription = "Удалить", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PassengerRow(
    passenger: Passenger,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = passenger.trainNumber?.takeIf { it.isNotBlank() }?.let { "Пассажирский №$it" }
                ?: "Пассажирский",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Close, contentDescription = "Удалить", modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Computes work duration taking break into account.
 */
private fun computeWorkDurationWithBreak(
    startMs: Long?,
    endMs: Long?,
    breakStartMs: Long?,
    breakEndMs: Long?,
): Long? {
    if (startMs == null || endMs == null || endMs <= startMs) return null
    val raw = endMs - startMs
    val breakDuration = if (breakStartMs != null && breakEndMs != null && breakEndMs > breakStartMs) {
        breakEndMs - breakStartMs
    } else {
        0L
    }
    return raw - breakDuration
}
