package com.z_company.route.component

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import com.z_company.core.ui.theme.custom.AppTypography
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
    datePickerState: DatePickerState,
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit,
) {
    val textStyle = AppTypography.getType().bodyMedium
    DatePickerDialog(
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(onClick = { onConfirmRequest() }) {
                Text(text = "Выбрать", style = textStyle)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismissRequest() }) {
                Text(text = "Отмена", style = textStyle, color = MaterialTheme.colorScheme.error)
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberDatePickerStateInLocale(initialSelectedDateMillis: Long): DatePickerState {
    val calendar = Calendar.getInstance()
    return rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis
                + calendar.timeZone.rawOffset
    )
}
