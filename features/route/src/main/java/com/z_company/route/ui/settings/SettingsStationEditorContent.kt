package com.z_company.route.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.Shapes
import com.z_company.route.R
import com.z_company.route.viewmodel.StationNormEditorViewModel
import com.z_company.route.viewmodel.StationNormField
import kotlinx.coroutines.delay

@Composable
fun SettingsStationEditorContent(
    viewModel: StationNormEditorViewModel,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    // Navigate only when deleted
    LaunchedEffect(state.deleted) {
        if (state.deleted) onDone()
    }

    // Autosave with 500ms debounce — triggers whenever any editable field changes
    LaunchedEffect(
        state.name,
        state.appearanceToStartMin,
        state.endToBarrierMin,
        state.barrierToStartMin,
        state.endToWorkEndMin,
    ) {
        if (state.name.isNotBlank()) {
            delay(500)
            viewModel.save()
        }
    }

    // Inline numeric value editor
    var dialogField by remember { mutableStateOf<StationNormField?>(null) }
    var dialogText by remember { mutableStateOf("") }

    dialogField?.let { field ->
        AlertDialog(
            onDismissRequest = { dialogField = null },
            containerColor = MaterialTheme.colorScheme.secondary,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.primary,
            title = {
                Text(
                    when (field) {
                        StationNormField.APPEARANCE_TO_START -> "Явка → Начало"
                        StationNormField.END_TO_BARRIER -> "Конец → КП"
                        StationNormField.BARRIER_TO_START -> "КП → Начало"
                        StationNormField.END_TO_WORK_END -> "Конец → Окончание работы"
                    }
                )
            },
            text = {
                OutlinedTextField(
                    value = dialogText,
                    onValueChange = { dialogText = it.filter { c -> c.isDigit() }.take(3) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    suffix = { Text("мин") },
                    label = { Text("Значение") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = dialogText.toIntOrNull()
                    if (v != null) viewModel.setField(field, v)
                    dialogField = null
                }) { Text("OK", color = MaterialTheme.colorScheme.tertiary) }
            },
            dismissButton = {
                TextButton(onClick = { dialogField = null }) {
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
        // Section hint
        Text(
            text = "4 интервала, привязанных к этой станции. Используются автоматически при заполнении времени.",
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
                .shadow(2.dp, Shapes.medium)
                .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Название",
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
                                "Лянгасово",
                                style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        inner()
                    }
                )
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
                label = "Явка → Начало",
                value = state.appearanceToStartMin,
                onIncrement = { viewModel.increment(StationNormField.APPEARANCE_TO_START) },
                onDecrement = { viewModel.decrement(StationNormField.APPEARANCE_TO_START) },
                onValueClick = {
                    dialogText = (state.appearanceToStartMin ?: 0).toString()
                    dialogField = StationNormField.APPEARANCE_TO_START
                }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            StepperRow(
                label = "Конец → КП",
                value = state.endToBarrierMin,
                onIncrement = { viewModel.increment(StationNormField.END_TO_BARRIER) },
                onDecrement = { viewModel.decrement(StationNormField.END_TO_BARRIER) },
                onValueClick = {
                    dialogText = (state.endToBarrierMin ?: 0).toString()
                    dialogField = StationNormField.END_TO_BARRIER
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
                label = "КП → Начало",
                value = state.barrierToStartMin,
                onIncrement = { viewModel.increment(StationNormField.BARRIER_TO_START) },
                onDecrement = { viewModel.decrement(StationNormField.BARRIER_TO_START) },
                onValueClick = {
                    dialogText = (state.barrierToStartMin ?: 0).toString()
                    dialogField = StationNormField.BARRIER_TO_START
                }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            StepperRow(
                label = "Конец → Окончание работы",
                value = state.endToWorkEndMin,
                onIncrement = { viewModel.increment(StationNormField.END_TO_WORK_END) },
                onDecrement = { viewModel.decrement(StationNormField.END_TO_WORK_END) },
                onValueClick = {
                    dialogText = (state.endToWorkEndMin ?: 0).toString()
                    dialogField = StationNormField.END_TO_WORK_END
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
                    if (state.stationId == null) {
                        // New station — just exit without saving
                        onDone()
                    } else {
                        viewModel.delete()
                    }
                }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                painter = painterResource(R.drawable.delete_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Удалить станцию",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: Int?,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onValueClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label — takes all remaining space, wraps if long
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            softWrap = true
        )

        // Stepper control — always on the right
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // − button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = (value ?: 0) > 0, onClick = onDecrement),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "−",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if ((value ?: 0) > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    lineHeight = 18.sp
                )
            }

            // Value box — tappable for direct numeric input
            Box(
                modifier = Modifier
                    .widthIn(min = 78.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        RoundedCornerShape(10.dp)
                    )
                    .background(MaterialTheme.colorScheme.background)
                    .clickable(onClick = onValueClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (value != null) "$value мин" else "— мин",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // + button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = (value ?: 0) < 120, onClick = onIncrement),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if ((value ?: 0) < 120) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
