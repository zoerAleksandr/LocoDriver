package com.z_company.route.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePicker
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
import com.z_company.route.R
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    datePickerState: DatePickerState,
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit,
    onClearRequest: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .requiredWidth(360.0.dp)
                .heightIn(max = 568.0.dp),
            shape = Shapes.medium,
            tonalElevation = 6.dp,
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DatePicker(
                    state = datePickerState
                )
                Row(
                    modifier = Modifier
                        .padding(vertical = 12.dp, horizontal = 24.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onClearRequest
                    ) {
                        Text(
                            text = stringResource(id = R.string.text_btn_clear),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Row {
                        TextButton(onClick = onDismissRequest) {
                            Text(text = stringResource(id = R.string.text_btn_dismiss))
                        }

                        TextButton(
                            onClick = onConfirmRequest
                        ) {
                            Text(text = stringResource(id = R.string.text_btn_confirm))
                        }
                    }
                }
            }
        }
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