package com.z_company.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.R
import com.z_company.core.ui.theme.custom.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    timePickerState: TimePickerState,
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit,
    isPicker: Boolean = true
) {
    val configuration = LocalConfiguration.current
    val showingPicker = remember { mutableStateOf(isPicker) }

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = Shapes.medium
            )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (showingPicker.value && configuration.screenHeightDp > 400) {
                TimePicker(state = timePickerState)
            } else {
                TimeInput(state = timePickerState)
            }

            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (configuration.screenHeightDp > 400) {
                    IconButton(onClick = { showingPicker.value = !showingPicker.value }) {
                        val icon = painterResource(
                            id =
                            if (showingPicker.value) {
                                R.drawable.outline_keyboard_24
                            } else {
                                R.drawable.outline_access_time_24
                            }
                        )
                        Icon(
                            icon,
                            contentDescription =
                            if (showingPicker.value) {
                                "Switch to Text Input"
                            } else {
                                "Switch to Touch Input"
                            }
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val textStyle = AppTypography.getType().titleMedium

                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(id = R.string.text_btn_dismiss), style = textStyle, color = MaterialTheme.colorScheme.error)
                    }

                    TextButton(
                        onClick = onConfirmRequest,
                        shape = Shapes.medium,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(text = stringResource(id = R.string.text_btn_confirm), style = textStyle)
                    }
                }
            }
        }
    }
}