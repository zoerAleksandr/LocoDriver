package com.z_company.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.z_company.core.R
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeInputDialog(
    initValue: Long,
    onDismissRequest: () -> Unit,
    onConfirmRequest: (Long) -> Unit,
) {
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
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var selectHourToLong by remember {
                mutableIntStateOf(ConverterLongToTime.getHour(initValue))
            }
            var selectMinuteToLong by remember {
                mutableIntStateOf(ConverterLongToTime.getRemainingMinuteFromHour(initValue))
            }
            val hourInitValue = ConverterLongToTime.getHour(initValue).toString()
            val minuteInitValue =
                ConverterLongToTime.getRemainingMinuteFromHour(initValue).toString()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InputTimeField(
                    initValue = hourInitValue,
                    maxValue = 99,
                    maxLength = 2,
                    helperText = "Часы"
                ) { value ->
                    selectHourToLong = 0
                    selectHourToLong += value
                }
                InputTimeField(
                    initValue = minuteInitValue,
                    maxValue = 59,
                    maxLength = 2,
                    helperText = "Минуты"
                ) { value ->
                    selectMinuteToLong = 0
                    selectMinuteToLong += value
                }
            }
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(id = R.string.text_btn_dismiss))
                    }

                    TextButton(
                        onClick = {
                            val resultLong = (selectHourToLong * 3_600_000L + selectMinuteToLong * 60_000L)
                            onConfirmRequest(resultLong)
                        },
                        shape = Shapes.medium,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = stringResource(id = R.string.text_btn_confirm),
                            style = AppTypography.getType().bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputTimeField(
    initValue: String,
    maxValue: Int,
    maxLength: Int,
    helperText: String,
    selectValue: (Int) -> Unit
) {
    val valueString = if (initValue.length < maxLength) {
        "0$initValue"
    } else {
        initValue
    }

    var value by remember {
        mutableStateOf(valueString)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val enabled = true
    val singleLine = true
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicTextField(
            value = TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            ),
            onValueChange = { strValue ->
                if (strValue.text.length > maxLength) {
                    if (strValue.text.first() == '0') {
                        val intValue = strValue.text.takeLast(maxLength).toIntOrNull()
                        intValue?.let { int ->
                            if (int <= maxValue) {
                                val text = strValue.text.takeLast(maxLength)
                                value = if (text.length < maxLength) {
                                    "0$text"
                                } else {
                                    text
                                }
                                selectValue(value.toInt())
                            }
                        }
                    } else {
                        val intValue = strValue.text.last().toString().toIntOrNull()
                        intValue?.let { int ->
                            if (int <= maxValue) {
                                val text = strValue.text.last().toString()
                                value = if (text.length < maxLength) {
                                    "0$text"
                                } else {
                                    text
                                }
                                selectValue(value.toInt())
                            }
                        }
                    }
                } else {
                    val intValue = strValue.text.takeLast(maxLength).toIntOrNull()
                    intValue?.let { int ->
                        if (int == 0) {
                            value = "00"
                            selectValue(value.toInt())
                        } else if (int <= maxValue) {
                            val text = strValue.text.takeLast(maxLength)
                            value = if (text.length < maxLength) {
                                "0$text"
                            } else {
                                text
                            }
                            selectValue(value.toInt())
                        }
                    }
                    if (intValue == null) {
                        value = ""
                        selectValue(0)
                    }
                }
            },
            interactionSource = interactionSource,
            singleLine = singleLine,
            enabled = enabled,
            textStyle = AppTypography.getType().displayMedium.copy(
                color = MaterialTheme.colorScheme.primary,
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    scope.launch {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                }
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .background(
                    shape = Shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
        ) {
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                visualTransformation = VisualTransformation.None,
                innerTextField = it,
                singleLine = singleLine,
                enabled = enabled,
                interactionSource = interactionSource,
                contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(
                    start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = Color.Transparent,
                ),
            )
        }

        Text(
            text = helperText,
            style = AppTypography.getType().bodySmall.copy(fontWeight = FontWeight.W300)
        )
    }
}