package com.z_company.core.ui.component


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.R
import com.z_company.core.ui.component.customDateTimePicker.noRippleEffect
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import kotlinx.datetime.LocalDateTime
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputDateTimePicker(
    isShowPicker: Boolean,
    titleText: String,
    initDateTime: Long,
    onDismiss: () -> Unit,
    onDoneClick: (LocalDateTime) -> Unit,
    onSettingClick: () -> Unit
) {
    val calendar by remember {
        mutableStateOf(
            Calendar.getInstance().also {
                it.timeInMillis = initDateTime
            }
        )
    }

    val titleStyle: TextStyle = AppTypography.getType().titleLarge.copy(
        color = MaterialTheme.colorScheme.primary
    )

    val datePickerState = rememberDatePickerStateInLocale(calendar.timeInMillis)

    var isShowTimePicker by remember {
        mutableStateOf(false)
    }

    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = true
    )
    if (isShowPicker) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    modifier = Modifier.padding(end = 24.dp),
                    onClick = {
                        calendar.timeInMillis = datePickerState.selectedDateMillis!!
                        isShowTimePicker = true
                    },
                    shape = Shapes.medium,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = stringResource(id = R.string.text_btn_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier.padding(end = 12.dp),
                    onClick = onDismiss
                ) {
                    Text(
                        text = stringResource(id = R.string.text_btn_dismiss),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            shape = Shapes.medium
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = titleText,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            style = titleStyle
                        )
                        Text(
                            modifier = Modifier
                                .padding(16.dp)
                                .noRippleEffect { onSettingClick() },
                            text = "Изменить интерфейс",
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            )
        }
    }

    if (isShowTimePicker) {
        TimePickerDialog(timePickerState = timePickerState,
            isPicker = false,
            onDismissRequest = { isShowTimePicker = false },
            onConfirmRequest = {
                calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                calendar.set(Calendar.MINUTE, timePickerState.minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                onDoneClick(
                    LocalDateTime(
                        year = calendar.get(Calendar.YEAR),
                        monthNumber = calendar.get(Calendar.MONTH) + 1,
                        dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
                        hour = calendar.get(Calendar.HOUR_OF_DAY),
                        minute = calendar.get(Calendar.MINUTE)
                    )
                )
                isShowTimePicker = false
            })
    }
}