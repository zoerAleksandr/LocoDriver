package com.z_company.route.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.R
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
    datePickerState: DatePickerState,
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit,
) {
    val subTitleTextStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
    DatePickerDialog(
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(
                modifier = Modifier.padding(end = 24.dp),
                onClick = onConfirmRequest,
                shape = Shapes.medium,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(text = stringResource(id = R.string.text_btn_confirm), style = subTitleTextStyle)
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.padding(end = 12.dp),
                onClick =  onDismissRequest
            ) {
                Text(text = stringResource(id = R.string.text_btn_dismiss), style = subTitleTextStyle, color = MaterialTheme.colorScheme.error)
            }
        },
        colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        shape = Shapes.medium
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
