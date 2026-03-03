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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.SectionDiesel
import com.z_company.domain.entities.route.SectionElectric
import com.z_company.domain.navigation.Router
import com.z_company.iosapp.util.TimeFormatter
import com.z_company.iosapp.viewmodel.LocoFormIosViewModel
import org.koin.compose.koinInject

/**
 * Locomotive create/edit screen.
 *
 * [locoId] == null -> new locomotive; non-null -> editing existing.
 * [basicId] is the parent route's basicData.id.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormLocoScreen(
    router: Router,
    basicId: String,
    locoId: String? = null,
) {
    val viewModel: LocoFormIosViewModel = koinInject()
    val locomotive by viewModel.locomotive.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(locoId, basicId) {
        viewModel.loadLoco(locoId, basicId)
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
                    Text(if (locoId == null) "Новый локомотив" else "Локомотив")
                },
                navigationIcon = {
                    IconButton(onClick = { router.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.saveLoco() }) {
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

        val loco = locomotive

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // --- Section: Тип тяги ---
            LocoSectionCard(title = "Тип тяги") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val currentType = loco?.type ?: LocoType.ELECTRIC
                    FilledTonalButton(
                        onClick = { viewModel.setType(LocoType.ELECTRIC) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (currentType == LocoType.ELECTRIC)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (currentType == LocoType.ELECTRIC)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Text("Электро")
                    }
                    FilledTonalButton(
                        onClick = { viewModel.setType(LocoType.DIESEL) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (currentType == LocoType.DIESEL)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (currentType == LocoType.DIESEL)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Text("Тепло")
                    }
                }
            }

            // --- Section: Серия и Номер ---
            LocoSectionCard(title = "Идентификация") {
                OutlinedTextField(
                    value = loco?.series ?: "",
                    onValueChange = { viewModel.updateSeries(it) },
                    label = { Text("Серия") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = loco?.number ?: "",
                    onValueChange = { viewModel.updateNumber(it) },
                    label = { Text("Номер") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // --- Section: Приёмка ---
            LocoSectionCard(title = "Приёмка") {
                LocoReadOnlyRow(
                    label = "Начало приёмки",
                    value = loco?.timeStartOfAcceptance?.let { TimeFormatter.formatDateTime(it) },
                )
                LocoReadOnlyRow(
                    label = "Окончание приёмки",
                    value = loco?.timeEndOfAcceptance?.let { TimeFormatter.formatDateTime(it) },
                )
                val acceptanceDuration = computeDuration(
                    loco?.timeStartOfAcceptance,
                    loco?.timeEndOfAcceptance,
                )
                if (acceptanceDuration != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    LocoReadOnlyRow(
                        label = "Продолжительность",
                        value = TimeFormatter.formatDuration(acceptanceDuration),
                    )
                }
            }

            // --- Section: Сдача ---
            LocoSectionCard(title = "Сдача") {
                LocoReadOnlyRow(
                    label = "Начало сдачи",
                    value = loco?.timeStartOfDelivery?.let { TimeFormatter.formatDateTime(it) },
                )
                LocoReadOnlyRow(
                    label = "Окончание сдачи",
                    value = loco?.timeEndOfDelivery?.let { TimeFormatter.formatDateTime(it) },
                )
                val deliveryDuration = computeDuration(
                    loco?.timeStartOfDelivery,
                    loco?.timeEndOfDelivery,
                )
                if (deliveryDuration != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    LocoReadOnlyRow(
                        label = "Продолжительность",
                        value = TimeFormatter.formatDuration(deliveryDuration),
                    )
                }
            }

            // --- Секции (зависят от типа тяги) ---
            val currentType = loco?.type ?: LocoType.ELECTRIC

            if (currentType == LocoType.ELECTRIC) {
                val sections = loco?.electricSectionList ?: emptyList()
                sections.forEachIndexed { index, section ->
                    ElectricSectionCard(
                        index = index,
                        section = section,
                        onAcceptedChange = { viewModel.updateElectricSectionAccepted(index, it) },
                        onDeliveryChange = { viewModel.updateElectricSectionDelivery(index, it) },
                        onAcceptedRecoveryChange = {
                            viewModel.updateElectricSectionAcceptedRecovery(index, it)
                        },
                        onDeliveryRecoveryChange = {
                            viewModel.updateElectricSectionDeliveryRecovery(index, it)
                        },
                        onRemove = { viewModel.removeElectricSection(index) },
                    )
                }
                Button(
                    onClick = { viewModel.addElectricSection() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("  Добавить секцию")
                }
            } else {
                val sections = loco?.dieselSectionList ?: emptyList()
                sections.forEachIndexed { index, section ->
                    DieselSectionCard(
                        index = index,
                        section = section,
                        onAcceptedChange = { viewModel.updateDieselSectionAccepted(index, it) },
                        onDeliveryChange = { viewModel.updateDieselSectionDelivery(index, it) },
                        onCoefficientChange = {
                            viewModel.updateDieselSectionCoefficient(index, it)
                        },
                        onFuelSupplyChange = {
                            viewModel.updateDieselSectionFuelSupply(index, it)
                        },
                        onRemove = { viewModel.removeDieselSection(index) },
                    )
                }
                Button(
                    onClick = { viewModel.addDieselSection() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("  Добавить секцию")
                }
            }

            // --- Save Button ---
            Button(
                onClick = { viewModel.saveLoco() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ElectricSectionCard(
    index: Int,
    section: SectionElectric,
    onAcceptedChange: (String) -> Unit,
    onDeliveryChange: (String) -> Unit,
    onAcceptedRecoveryChange: (String) -> Unit,
    onDeliveryRecoveryChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    LocoSectionCard(
        title = "Секция ${index + 1}",
        trailingAction = {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить секцию",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    ) {
        OutlinedTextField(
            value = section.acceptedEnergy?.toString() ?: "",
            onValueChange = onAcceptedChange,
            label = { Text("Принято (кВт·ч)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = section.deliveryEnergy?.toString() ?: "",
            onValueChange = onDeliveryChange,
            label = { Text("Сдано (кВт·ч)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = section.acceptedRecovery?.toString() ?: "",
            onValueChange = onAcceptedRecoveryChange,
            label = { Text("Рекуперация принята (кВт·ч)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = section.deliveryRecovery?.toString() ?: "",
            onValueChange = onDeliveryRecoveryChange,
            label = { Text("Рекуперация сдана (кВт·ч)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        // Calculated consumption
        val accepted = section.acceptedEnergy
        val delivery = section.deliveryEnergy
        if (accepted != null && delivery != null) {
            val consumed = accepted - delivery
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            LocoReadOnlyRow(
                label = "Расход",
                value = "${kotlin.math.round(consumed * 100) / 100} кВт·ч",
            )
        }
    }
}

@Composable
private fun DieselSectionCard(
    index: Int,
    section: SectionDiesel,
    onAcceptedChange: (String) -> Unit,
    onDeliveryChange: (String) -> Unit,
    onCoefficientChange: (String) -> Unit,
    onFuelSupplyChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    LocoSectionCard(
        title = "Секция ${index + 1}",
        trailingAction = {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить секцию",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    ) {
        OutlinedTextField(
            value = section.acceptedFuel?.toString() ?: "",
            onValueChange = onAcceptedChange,
            label = { Text("Принято топлива (л)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = section.deliveryFuel?.toString() ?: "",
            onValueChange = onDeliveryChange,
            label = { Text("Сдано топлива (л)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = section.coefficient?.toString() ?: "",
            onValueChange = onCoefficientChange,
            label = { Text("Коэффициент") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = section.fuelSupply?.toString() ?: "",
            onValueChange = onFuelSupplyChange,
            label = { Text("Запас топлива (л)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        // Calculated consumption
        val accepted = section.acceptedFuel
        val delivery = section.deliveryFuel
        if (accepted != null && delivery != null) {
            val consumed = accepted - delivery
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            LocoReadOnlyRow(
                label = "Расход",
                value = "${kotlin.math.round(consumed * 100) / 100} л",
            )
        }
    }
}

@Composable
private fun LocoSectionCard(
    title: String,
    trailingAction: @Composable (() -> Unit)? = null,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                trailingAction?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun LocoReadOnlyRow(label: String, value: String?) {
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

private fun computeDuration(startMs: Long?, endMs: Long?): Long? {
    if (startMs == null || endMs == null || endMs <= startMs) return null
    return endMs - startMs
}
