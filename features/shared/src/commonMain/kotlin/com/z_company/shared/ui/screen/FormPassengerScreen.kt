package com.z_company.shared.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z_company.shared.util.TimeFormatter
import com.z_company.shared.viewmodel.FormPassengerSharedViewModel
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

    LaunchedEffect(passengerId, basicId) {
        viewModel.loadPassenger(passengerId, basicId)
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
                title = { Text("Пассажиром") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.savePassenger() },
                        enabled = !isLoading && passenger != null,
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Сохранить")
                    }
                },
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // --- Train ---
            FormSectionCard(title = "Поезд") {
                OutlinedTextField(
                    value = p.trainNumber ?: "",
                    onValueChange = { viewModel.updateTrainNumber(it) },
                    label = { Text("Номер поезда") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // --- Route ---
            FormSectionCard(title = "Маршрут") {
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

            // --- Time ---
            FormSectionCard(title = "Время") {
                FormReadOnlyTimeRow("Отправление", p.timeDeparture)
                FormReadOnlyTimeRow("Прибытие", p.timeArrival)
                val travelDuration = computeDurationMs(p.timeDeparture, p.timeArrival)
                if (travelDuration > 0) {
                    FormReadOnlyRow("В пути", TimeFormatter.formatDuration(travelDuration))
                }
            }

            // --- Notes ---
            FormSectionCard(title = "Заметки") {
                OutlinedTextField(
                    value = p.notes ?: "",
                    onValueChange = { viewModel.updateNotes(it) },
                    label = { Text("Примечание") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
