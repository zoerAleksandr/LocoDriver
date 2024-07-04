package com.z_company.route.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.route.R
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
