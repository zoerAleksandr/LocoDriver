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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.z_company.domain.entities.route.Station
import com.z_company.domain.navigation.Router
import com.z_company.iosapp.util.TimeFormatter
import com.z_company.iosapp.viewmodel.TrainFormIosViewModel
import org.koin.compose.koinInject

/**
 * Train create/edit screen.
 *
 * [trainId] == null -> new train; non-null -> editing existing.
 * [basicId] is the parent route's basicData.id.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormTrainScreen(
    router: Router,
    basicId: String,
    trainId: String? = null,
) {
    val viewModel: TrainFormIosViewModel = koinInject()
    val train by viewModel.train.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(trainId, basicId) {
        viewModel.loadTrain(trainId, basicId)
    }

    LaunchedEffect(isSaved) {
        if (isSaved) {
            router.back()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (trainId == null) "Новый поезд" else "Поезд")
                },
                navigationIcon = {
                    IconButton(onClick = { router.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.saveTrain() }) {
                        Text("Сохранить")
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
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val currentTrain = train

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // --- Section: Номер поезда ---
            TrainSectionCard(title = "Основные данные") {
                OutlinedTextField(
                    value = currentTrain?.number ?: "",
                    onValueChange = { viewModel.updateNumber(it) },
                    label = { Text("Номер поезда") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // --- Section: Характеристики ---
            TrainSectionCard(title = "Характеристики") {
                OutlinedTextField(
                    value = currentTrain?.weight ?: "",
                    onValueChange = { viewModel.updateWeight(it) },
                    label = { Text("Вес (т)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = currentTrain?.axle ?: "",
                    onValueChange = { viewModel.updateAxle(it) },
                    label = { Text("Осей") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = currentTrain?.conditionalLength ?: "",
                    onValueChange = { viewModel.updateConditionalLength(it) },
                    label = { Text("Условная длина") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = currentTrain?.distance ?: "",
                    onValueChange = { viewModel.updateDistance(it) },
                    label = { Text("Расстояние (км)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // --- Section: Станции ---
            val stations = currentTrain?.stations ?: emptyList()
            if (stations.isNotEmpty()) {
                TrainSectionCard(title = "Станции") {
                    stations.forEachIndexed { index, station ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                        StationItem(
                            index = index,
                            station = station,
                            onNameChange = { viewModel.updateStationName(index, it) },
                            onRemove = { viewModel.removeStation(index) },
                        )
                    }
                }
            }

            // Add station button
            Button(
                onClick = { viewModel.addStation() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("  Добавить станцию")
            }

            // --- Save Button ---
            Button(
                onClick = { viewModel.saveTrain() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StationItem(
    index: Int,
    station: Station,
    onNameChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Станция ${index + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить станцию",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        OutlinedTextField(
            value = station.stationName ?: "",
            onValueChange = onNameChange,
            label = { Text("Название станции") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        TrainReadOnlyRow(
            label = "Прибытие",
            value = station.timeArrival?.let { TimeFormatter.formatDateTime(it) },
        )
        TrainReadOnlyRow(
            label = "Отправление",
            value = station.timeDeparture?.let { TimeFormatter.formatDateTime(it) },
        )
        // Duration at station
        val arrival = station.timeArrival
        val departure = station.timeDeparture
        if (arrival != null && departure != null && departure > arrival) {
            TrainReadOnlyRow(
                label = "Стоянка",
                value = TimeFormatter.formatDuration(departure - arrival),
            )
        }
    }
}

@Composable
private fun TrainSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

@Composable
private fun TrainReadOnlyRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value ?: "\u2014",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
