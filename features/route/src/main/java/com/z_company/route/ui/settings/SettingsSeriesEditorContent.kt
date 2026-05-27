package com.z_company.route.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.route.LocoType
import com.z_company.route.R
import com.z_company.route.viewmodel.SeriesEditorViewModel
import kotlinx.coroutines.delay

@Composable
fun SettingsSeriesEditorContent(
    viewModel: SeriesEditorViewModel,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    // Navigate only when deleted
    LaunchedEffect(state.deleted) {
        if (state.deleted) onDone()
    }

    // Autosave with 500ms debounce on any field change
    LaunchedEffect(
        state.name,
        state.type,
        state.acceptanceDurationMin,
        state.deliveryDurationMin,
    ) {
        if (state.name.isNotBlank()) {
            delay(500)
            viewModel.save()
        }
    }

    // Type picker dialog
    var showTypePicker by remember { mutableStateOf(false) }
    if (showTypePicker) {
        AlertDialog(
            onDismissRequest = { showTypePicker = false },
            containerColor = MaterialTheme.colorScheme.secondary,
            titleContentColor = MaterialTheme.colorScheme.primary,
            title = { Text("Тип тяги") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TypeOption(
                        label = "⚡ Электровоз",
                        selected = state.type == LocoType.ELECTRIC,
                        onClick = { viewModel.setType(LocoType.ELECTRIC); showTypePicker = false }
                    )
                    TypeOption(
                        label = "💧 Тепловоз",
                        selected = state.type == LocoType.DIESEL,
                        onClick = { viewModel.setType(LocoType.DIESEL); showTypePicker = false }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTypePicker = false }) {
                    Text("Отмена", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                }
            }
        )
    }

    // Acceptance value dialog
    var showAcceptanceDialog by remember { mutableStateOf(false) }
    var acceptanceText by remember { mutableStateOf("") }
    if (showAcceptanceDialog) {
        AlertDialog(
            onDismissRequest = { showAcceptanceDialog = false },
            containerColor = MaterialTheme.colorScheme.secondary,
            titleContentColor = MaterialTheme.colorScheme.primary,
            title = { Text("Длительность приёмки") },
            text = {
                OutlinedTextField(
                    value = acceptanceText,
                    onValueChange = { acceptanceText = it.filter { c -> c.isDigit() }.take(3) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    suffix = { Text("мин") },
                    label = { Text("Значение") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = acceptanceText.toIntOrNull()
                    if (v != null) viewModel.setAcceptanceDurationMin(v)
                    showAcceptanceDialog = false
                }) { Text("OK", color = MaterialTheme.colorScheme.tertiary) }
            },
            dismissButton = {
                TextButton(onClick = { showAcceptanceDialog = false }) {
                    Text("Отмена", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                }
            }
        )
    }

    // Delivery value dialog
    var showDeliveryDialog by remember { mutableStateOf(false) }
    var deliveryText by remember { mutableStateOf("") }
    if (showDeliveryDialog) {
        AlertDialog(
            onDismissRequest = { showDeliveryDialog = false },
            containerColor = MaterialTheme.colorScheme.secondary,
            titleContentColor = MaterialTheme.colorScheme.primary,
            title = { Text("Длительность сдачи") },
            text = {
                OutlinedTextField(
                    value = deliveryText,
                    onValueChange = { deliveryText = it.filter { c -> c.isDigit() }.take(3) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    suffix = { Text("мин") },
                    label = { Text("Значение") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = deliveryText.toIntOrNull()
                    if (v != null) viewModel.setDeliveryDurationMin(v)
                    showDeliveryDialog = false
                }) { Text("OK", color = MaterialTheme.colorScheme.tertiary) }
            },
            dismissButton = {
                TextButton(onClick = { showDeliveryDialog = false }) {
                    Text("Отмена", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Hint
        Text(
            text = "Для каждой серии задаются нормы длительности приёмки и сдачи. " +
                "Нормы применяются автоматически при заполнении времени в шторке локомотива.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
        )

        // ОСНОВНОЕ
        Text(
            text = "ОСНОВНОЕ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shapes.medium)
                .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
        ) {
            // Name — BasicTextField без рамки
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Серия",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
                BasicTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        if (state.name.isEmpty()) {
                            Text(
                                "Серия",
                                style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        inner()
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))

            // Type — tappable row with chevron, opens dialog
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTypePicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Тип",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (state.type == LocoType.ELECTRIC) "⚡ Электровоз" else "💧 Тепловоз",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_right_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // ПРИЁМКА
        Text(
            text = "ПРИЁМКА",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, Shapes.medium)
                .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
        ) {
            StepperRow(
                label = "Длительность",
                value = state.acceptanceDurationMin,
                onIncrement = viewModel::incrementAcceptance,
                onDecrement = viewModel::decrementAcceptance,
                onValueClick = {
                    acceptanceText = (state.acceptanceDurationMin ?: 0).toString()
                    showAcceptanceDialog = true
                }
            )
        }

        // СДАЧА
        Text(
            text = "СДАЧА",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, Shapes.medium)
                .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
        ) {
            StepperRow(
                label = "Длительность",
                value = state.deliveryDurationMin,
                onIncrement = viewModel::incrementDelivery,
                onDecrement = viewModel::decrementDelivery,
                onValueClick = {
                    deliveryText = (state.deliveryDurationMin ?: 0).toString()
                    showDeliveryDialog = true
                }
            )
        }

        // Delete — always visible
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shapes.medium)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), Shapes.medium)
                .clickable {
                    if (state.seriesId == null) {
                        onDone()
                    } else {
                        viewModel.delete()
                    }
                }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.delete_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Удалить серию",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun TypeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.primary
        )
        if (selected) {
            Icon(
                painter = painterResource(com.z_company.route.R.drawable.check_circle_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
