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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.z_company.domain.entities.route.LocoType
import com.z_company.shared.util.TimeFormatter
import com.z_company.shared.viewmodel.FormLocoSharedViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormLocoScreen(
    locoId: String?,
    basicId: String,
    onBackClick: () -> Unit,
) {
    val viewModel: FormLocoSharedViewModel = koinInject()
    val loco by viewModel.locomotive.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val seriesList by viewModel.seriesList.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(locoId, basicId) {
        viewModel.loadLoco(locoId, basicId)
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
                title = { Text("Локомотив") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveLoco() },
                        enabled = !isLoading && loco != null,
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Сохранить")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (isLoading || loco == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val l = loco!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // --- Type toggle ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = { viewModel.setType(LocoType.ELECTRIC) },
                    modifier = Modifier.weight(1f),
                    enabled = l.type != LocoType.ELECTRIC,
                ) { Text("Электровоз") }
                FilledTonalButton(
                    onClick = { viewModel.setType(LocoType.DIESEL) },
                    modifier = Modifier.weight(1f),
                    enabled = l.type != LocoType.DIESEL,
                ) { Text("Тепловоз") }
            }

            // --- Series + Number ---
            FormSectionCard(title = "Основные данные") {
                OutlinedTextField(
                    value = l.series ?: "",
                    onValueChange = { viewModel.updateSeries(it) },
                    label = { Text("Серия") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = l.number ?: "",
                    onValueChange = { viewModel.updateNumber(it) },
                    label = { Text("Номер") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // --- Acceptance times ---
            FormSectionCard(title = "Приёмка") {
                FormReadOnlyTimeRow(
                    label = "Начало приёмки",
                    millis = l.timeStartOfAcceptance,
                )
                FormReadOnlyTimeRow(
                    label = "Окончание приёмки",
                    millis = l.timeEndOfAcceptance,
                )
                val acceptDuration = computeDurationMs(l.timeStartOfAcceptance, l.timeEndOfAcceptance)
                if (acceptDuration > 0) {
                    FormReadOnlyRow("Длительность", TimeFormatter.formatDuration(acceptDuration))
                }
            }

            // --- Delivery times ---
            FormSectionCard(title = "Сдача") {
                FormReadOnlyTimeRow(
                    label = "Начало сдачи",
                    millis = l.timeStartOfDelivery,
                )
                FormReadOnlyTimeRow(
                    label = "Окончание сдачи",
                    millis = l.timeEndOfDelivery,
                )
                val delivDuration = computeDurationMs(l.timeStartOfDelivery, l.timeEndOfDelivery)
                if (delivDuration > 0) {
                    FormReadOnlyRow("Длительность", TimeFormatter.formatDuration(delivDuration))
                }
            }

            // --- Electric sections ---
            if (l.type == LocoType.ELECTRIC) {
                FormSectionCard(title = "Секции (электро)") {
                    l.electricSectionList.forEachIndexed { index, section ->
                        Text(
                            text = "Секция ${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DoubleField(
                                label = "Принято",
                                value = section.acceptedEnergy,
                                onValueChange = { viewModel.updateElectricAccepted(index, it) },
                                modifier = Modifier.weight(1f),
                            )
                            DoubleField(
                                label = "Сдано",
                                value = section.deliveryEnergy,
                                onValueChange = { viewModel.updateElectricDelivery(index, it) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        val consumption = computeConsumption(section.acceptedEnergy, section.deliveryEnergy)
                        if (consumption != null) {
                            FormReadOnlyRow("Расход", formatConsumption(consumption))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DoubleField(
                                label = "Рекуп. принято",
                                value = section.acceptedRecovery,
                                onValueChange = { viewModel.updateElectricAcceptedRecovery(index, it) },
                                modifier = Modifier.weight(1f),
                            )
                            DoubleField(
                                label = "Рекуп. сдано",
                                value = section.deliveryRecovery,
                                onValueChange = { viewModel.updateElectricDeliveryRecovery(index, it) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = { viewModel.removeElectricSection(index) }) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Удалить")
                            }
                        }
                        if (index < l.electricSectionList.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                    TextButton(onClick = { viewModel.addElectricSection() }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Добавить секцию")
                    }
                }
            }

            // --- Diesel sections ---
            if (l.type == LocoType.DIESEL) {
                FormSectionCard(title = "Секции (дизель)") {
                    l.dieselSectionList.forEachIndexed { index, section ->
                        Text(
                            text = "Секция ${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DoubleField(
                                label = "Принято",
                                value = section.acceptedFuel,
                                onValueChange = { viewModel.updateDieselAccepted(index, it) },
                                modifier = Modifier.weight(1f),
                            )
                            DoubleField(
                                label = "Сдано",
                                value = section.deliveryFuel,
                                onValueChange = { viewModel.updateDieselDelivery(index, it) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        val consumption = computeConsumption(section.acceptedFuel, section.deliveryFuel)
                        if (consumption != null) {
                            FormReadOnlyRow("Расход", formatConsumption(consumption))
                        }
                        DoubleField(
                            label = "Коэффициент",
                            value = section.coefficient,
                            onValueChange = { viewModel.updateDieselCoefficient(index, it) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        DoubleField(
                            label = "Экипировка",
                            value = section.fuelSupply,
                            onValueChange = { viewModel.updateDieselFuelSupply(index, it) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = { viewModel.removeDieselSection(index) }) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Удалить")
                            }
                        }
                        if (index < l.dieselSectionList.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                    TextButton(onClick = { viewModel.addDieselSection() }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Добавить секцию")
                    }
                }
            }

            // --- Heating counters ---
            FormSectionCard(title = "Отопление") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DoubleField(
                        label = "Принято",
                        value = l.heatingCounterAccepted,
                        onValueChange = { viewModel.updateHeatingAccepted(it) },
                        modifier = Modifier.weight(1f),
                    )
                    DoubleField(
                        label = "Сдано",
                        value = l.heatingCounterDelivery,
                        onValueChange = { viewModel.updateHeatingDelivery(it) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// --- Reusable private composables ---

@Composable
internal fun FormSectionCard(
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
                fontWeight = FontWeight.Bold,
            )
            content()
        }
    }
}

@Composable
internal fun FormReadOnlyRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
internal fun FormReadOnlyTimeRow(label: String, millis: Long?) {
    FormReadOnlyRow(
        label = label,
        value = if (millis != null) TimeFormatter.formatDateTime(millis) else "—",
    )
}

@Composable
internal fun DoubleField(
    label: String,
    value: Double?,
    onValueChange: (Double?) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value?.let { formatDoubleClean(it) } ?: "",
        onValueChange = { raw ->
            if (raw.isBlank()) {
                onValueChange(null)
            } else {
                raw.replace(",", ".").toDoubleOrNull()?.let { onValueChange(it) }
            }
        },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

private fun formatDoubleClean(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        value.toString()
    }
}

internal fun computeDurationMs(start: Long?, end: Long?): Long {
    if (start == null || end == null) return 0
    return (end - start).coerceAtLeast(0)
}

private fun formatConsumption(value: Double): String {
    val intPart = value.toLong()
    val fracPart = ((value - intPart) * 10).toLong()
    return "$intPart.$fracPart"
}

private fun computeConsumption(accepted: Double?, delivered: Double?): Double? {
    if (accepted == null || delivered == null) return null
    val result = delivered - accepted
    return if (result != 0.0) result else null
}
