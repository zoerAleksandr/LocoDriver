package com.z_company.route.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.z_company.domain.entities.setting.UserSettings

@Composable
fun SettingsTrainContent(
    currentSettings: UserSettings,
    setPassengerWagonLengthMeters: (Double) -> Unit,
) {
    var showEditor by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 28.dp),
    ) {
        SettingsGroupHeader("РАСЧЁТ ДЛИНЫ", top = 8.dp, startPad = 4.dp)
        SettingsCard {
            SettingsFieldRow(
                label = "Пассажирский вагон",
                value = "${currentSettings.passengerWagonLengthMeters.formatMeters()} м",
                mono = true,
                onClick = { showEditor = true },
            )
        }
        SettingsSectionNote(
            "Для пассажирского поезда значение У.Д. считается количеством вагонов и " +
                "умножается на эту длину. Пассажирский поезд определяется по номеру. " +
                "Для грузового поезда сохраняется расчёт 1 У.Д. = 14 м."
        )
    }

    if (showEditor) {
        var value by remember(currentSettings.passengerWagonLengthMeters) {
            mutableStateOf(currentSettings.passengerWagonLengthMeters.formatMeters())
        }
        val parsed = value.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }
        AlertDialog(
            onDismissRequest = { showEditor = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Длина пассажирского вагона") },
            text = {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = value,
                    onValueChange = { input ->
                        value = input.filter { it.isDigit() || it == ',' || it == '.' }
                    },
                    label = { Text("Метры") },
                    supportingText = { Text("По умолчанию 24,5 м") },
                    isError = value.isNotBlank() && parsed == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = parsed != null,
                    onClick = {
                        parsed?.let(setPassengerWagonLengthMeters)
                        showEditor = false
                    },
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) { Text("Отмена") }
            },
        )
    }
}

private fun Double.formatMeters(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString().replace('.', ',')
