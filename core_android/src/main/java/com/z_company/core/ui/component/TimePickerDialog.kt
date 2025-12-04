package com.z_company.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.R
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.domain.entities.TypeDateTimePicker
import com.z_company.domain.repositories.SharedPreferencesRepositories
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimePickerDialog(
    cancelText: String = stringResource(id = R.string.text_btn_dismiss),
    confirmText: String = stringResource(id = R.string.text_btn_confirm),
    timePickerState: TimePickerState,
    onDismissRequest: () -> Unit,
    onCancelRequest: () -> Unit = onDismissRequest,
    onConfirmRequest: () -> Unit,
    isPicker: Boolean = true,
    header: String? = null,
    showSelectTypeDateTimePicker: Boolean = true
) {
    val preferences = koinInject<SharedPreferencesRepositories>()

    val configuration = LocalConfiguration.current
    val showingPicker = remember { mutableStateOf(isPicker) }

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.secondary,
                shape = Shapes.medium
            )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val styleTitle = AppTypography.getType().titleLarge
                .copy(
                    fontWeight = FontWeight.Light
                )
            header?.let {
                Text(
                    modifier = Modifier
                        .padding(bottom = 16.dp),
                    text = header,
                    style = styleTitle,
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            if (showingPicker.value && configuration.screenHeightDp > 400) {
                TimePicker(state = timePickerState)
            } else {
                TimeInput(state = timePickerState)
            }

            FlowRow(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (configuration.screenHeightDp > 400 && showSelectTypeDateTimePicker) {
                    IconButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showingPicker.value = !showingPicker.value
                            if (showingPicker.value) {
                                preferences.setTokenDateTimePickerType(TypeDateTimePicker.ROUND.text)
                            } else {
                                preferences.setTokenDateTimePickerType(TypeDateTimePicker.INPUT.text)
                            }
                        }
                    ) {
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


                TextButton(
                    modifier = Modifier.padding(start = 12.dp),
                    onClick = onCancelRequest
                ) {
                    Text(
                        text = cancelText,
                        color = MaterialTheme.colorScheme.error
                    )
                }


                TextButton(
                    modifier = Modifier.padding(start = 12.dp),
                    onClick = onConfirmRequest,
                    shape = Shapes.medium,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = confirmText,
                    )
                }
            }
        }
    }
}