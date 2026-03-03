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
import com.z_company.domain.navigation.Router
import com.z_company.iosapp.util.TimeFormatter
import com.z_company.iosapp.viewmodel.PassengerFormIosViewModel
import org.koin.compose.koinInject

/**
 * Passenger train create/edit screen.
 *
 * [passengerId] == null -> new entry; non-null -> editing existing.
 * [basicId] is the parent route's basicData.id.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormPassengerScreen(
    router: Router,
    basicId: String,
    passengerId: String? = null,
) {
    val viewModel: PassengerFormIosViewModel = koinInject()
    val passenger by viewModel.passenger.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(passengerId, basicId) {
        viewModel.loadPassenger(passengerId, basicId)
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
                    Text(if (passengerId == null) "Новый пассажирский" else "Пассажирский")
                },
                navigationIcon = {
                    IconButton(onClick = { router.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.savePassenger() }) {
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

        val current = passenger

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // --- Section: Поезд ---
            PassengerSectionCard(title = "Поезд") {
                OutlinedTextField(
                    value = current?.trainNumber ?: "",
                    onValueChange = { viewModel.updateTrainNumber(it) },
                    label = { Text("Номер поезда") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // --- Section: Маршрут ---
            PassengerSectionCard(title = "Маршрут") {
                OutlinedTextField(
                    value = current?.stationDeparture ?: "",
                    onValueChange = { viewModel.updateStationDeparture(it) },
                    label = { Text("Станция отправления") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = current?.stationArrival ?: "",
                    onValueChange = { viewModel.updateStationArrival(it) },
                    label = { Text("Станция прибытия") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // --- Section: Время ---
            PassengerSectionCard(title = "Время") {
                PassengerReadOnlyRow(
                    label = "Время отправления",
                    value = current?.timeDeparture?.let { TimeFormatter.formatDateTime(it) },
                )
                PassengerReadOnlyRow(
                    label = "Время прибытия",
                    value = current?.timeArrival?.let { TimeFormatter.formatDateTime(it) },
                )

                // Calculated travel duration
                val departure = current?.timeDeparture
                val arrival = current?.timeArrival
                if (departure != null && arrival != null && arrival > departure) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    PassengerReadOnlyRow(
                        label = "Время в пути",
                        value = TimeFormatter.formatDuration(arrival - departure),
                    )
                }
            }

            // --- Section: Заметки ---
            PassengerSectionCard(title = "Заметки") {
                OutlinedTextField(
                    value = current?.notes ?: "",
                    onValueChange = { viewModel.updateNotes(it) },
                    label = { Text("Заметки") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // --- Save Button ---
            Button(
                onClick = { viewModel.savePassenger() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PassengerSectionCard(
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
private fun PassengerReadOnlyRow(label: String, value: String?) {
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
