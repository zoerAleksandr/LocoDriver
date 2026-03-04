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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import com.z_company.domain.entities.route.LocoType
import com.z_company.shared.ui.component.AppBottomSheet
import com.z_company.shared.ui.component.BottomSheetAction
import com.z_company.shared.ui.component.picker.DateTimePickerBottomSheet
import com.z_company.shared.util.TimeFormatter
import com.z_company.shared.viewmodel.FormLocoSharedViewModel
import kotlin.time.Clock
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

    LaunchedEffect(locoId, basicId) { viewModel.loadLoco(locoId, basicId) }
    LaunchedEffect(isSaved) { if (isSaved) onBackClick() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    // Time picker states
    var showStartAcceptPicker by remember { mutableStateOf(false) }
    var showEndAcceptPicker by remember { mutableStateOf(false) }
    var showStartDeliveryPicker by remember { mutableStateOf(false) }
    var showEndDeliveryPicker by remember { mutableStateOf(false) }

    // Delete bottom sheet states
    var showDeleteStartAccept by remember { mutableStateOf(false) }
    var showDeleteEndAccept by remember { mutableStateOf(false) }
    var showDeleteStartDelivery by remember { mutableStateOf(false) }
    var showDeleteEndDelivery by remember { mutableStateOf(false) }

    // Delete bottom sheets
    if (showDeleteStartAccept) {
        AppBottomSheet(
            onDismissRequest = { showDeleteStartAccept = false },
            title = "Начало приёмки",
            actions = listOf(BottomSheetAction("Удалить значение") { viewModel.setTimeStartAcceptance(null) }),
        )
    }
    if (showDeleteEndAccept) {
        AppBottomSheet(
            onDismissRequest = { showDeleteEndAccept = false },
            title = "Окончание приёмки",
            actions = listOf(BottomSheetAction("Удалить значение") { viewModel.setTimeEndAcceptance(null) }),
        )
    }
    if (showDeleteStartDelivery) {
        AppBottomSheet(
            onDismissRequest = { showDeleteStartDelivery = false },
            title = "Начало сдачи",
            actions = listOf(BottomSheetAction("Удалить значение") { viewModel.setTimeStartDelivery(null) }),
        )
    }
    if (showDeleteEndDelivery) {
        AppBottomSheet(
            onDismissRequest = { showDeleteEndDelivery = false },
            title = "Окончание сдачи",
            actions = listOf(BottomSheetAction("Удалить значение") { viewModel.setTimeEndDelivery(null) }),
        )
    }

    // Time pickers
    if (showStartAcceptPicker) {
        DateTimePickerBottomSheet(
            title = "Начало приёмки",
            onDateTimeSelected = { viewModel.setTimeStartAcceptance(it) },
            onDismiss = { showStartAcceptPicker = false },
            startDateTime = loco?.timeStartOfAcceptance ?: Clock.System.now().toEpochMilliseconds(),
        )
    }
    if (showEndAcceptPicker) {
        DateTimePickerBottomSheet(
            title = "Окончание приёмки",
            onDateTimeSelected = { viewModel.setTimeEndAcceptance(it) },
            onDismiss = { showEndAcceptPicker = false },
            startDateTime = loco?.timeEndOfAcceptance ?: loco?.timeStartOfAcceptance ?: Clock.System.now().toEpochMilliseconds(),
        )
    }
    if (showStartDeliveryPicker) {
        DateTimePickerBottomSheet(
            title = "Начало сдачи",
            onDateTimeSelected = { viewModel.setTimeStartDelivery(it) },
            onDismiss = { showStartDeliveryPicker = false },
            startDateTime = loco?.timeStartOfDelivery ?: Clock.System.now().toEpochMilliseconds(),
        )
    }
    if (showEndDeliveryPicker) {
        DateTimePickerBottomSheet(
            title = "Окончание сдачи",
            onDateTimeSelected = { viewModel.setTimeEndDelivery(it) },
            onDismiss = { showEndDeliveryPicker = false },
            startDateTime = loco?.timeEndOfDelivery ?: loco?.timeStartOfDelivery ?: Clock.System.now().toEpochMilliseconds(),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Локомотив") },
                navigationIcon = {
                    TextButton(
                        onClick = { viewModel.saveLoco() },
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
        if (isLoading || loco == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val l = loco!!

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

            // --- Type toggle ---
            item {
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
            }

            // --- Series + Number ---
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
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
            }

            // --- Acceptance times ---
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Приёмка",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    LocoTimeRowAnimated(
                        label = "Начало приёмки",
                        millis = l.timeStartOfAcceptance,
                        onClick = { showStartAcceptPicker = true },
                        onLongClick = { showDeleteStartAccept = true },
                    )
                    LocoTimeRowAnimated(
                        label = "Окончание приёмки",
                        millis = l.timeEndOfAcceptance,
                        onClick = { showEndAcceptPicker = true },
                        onLongClick = { showDeleteEndAccept = true },
                    )
                    val acceptDuration = computeDurationMs(l.timeStartOfAcceptance, l.timeEndOfAcceptance)
                    if (acceptDuration > 0) {
                        FormReadOnlyRow("Длительность", TimeFormatter.formatDuration(acceptDuration))
                    }
                }
            }

            // --- Delivery times ---
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Сдача",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    LocoTimeRowAnimated(
                        label = "Начало сдачи",
                        millis = l.timeStartOfDelivery,
                        onClick = { showStartDeliveryPicker = true },
                        onLongClick = { showDeleteStartDelivery = true },
                    )
                    LocoTimeRowAnimated(
                        label = "Окончание сдачи",
                        millis = l.timeEndOfDelivery,
                        onClick = { showEndDeliveryPicker = true },
                        onLongClick = { showDeleteEndDelivery = true },
                    )
                    val delivDuration = computeDurationMs(l.timeStartOfDelivery, l.timeEndOfDelivery)
                    if (delivDuration > 0) {
                        FormReadOnlyRow("Длительность", TimeFormatter.formatDuration(delivDuration))
                    }
                }
            }

            // --- Electric sections ---
            if (l.type == LocoType.ELECTRIC) {
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
                                text = "Секции (электро)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            TextButton(onClick = { viewModel.addElectricSection() }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Добавить", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }

                        l.electricSectionList.forEachIndexed { index, section ->
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
                                            text = "Секция ${index + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Icon(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .combinedClickable(onClick = { viewModel.removeElectricSection(index) }),
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                        )
                                    }
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
                                }
                            }
                        }
                    }
                }
            }

            // --- Diesel sections ---
            if (l.type == LocoType.DIESEL) {
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
                                text = "Секции (дизель)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            TextButton(onClick = { viewModel.addDieselSection() }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Добавить", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }

                        l.dieselSectionList.forEachIndexed { index, section ->
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
                                            text = "Секция ${index + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Icon(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .combinedClickable(onClick = { viewModel.removeDieselSection(index) }),
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                        )
                                    }
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
                                }
                            }
                        }
                    }
                }
            }

            // --- Heating counters ---
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Отопление",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
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
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun LocoTimeRowAnimated(
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

// --- Reusable private composables (kept internal for Train/Passenger reuse) ---

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
