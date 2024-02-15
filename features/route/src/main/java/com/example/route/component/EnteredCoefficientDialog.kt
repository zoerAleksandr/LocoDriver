package com.example.route.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.ui.theme.custom.AppTypography
import com.example.route.viewmodel.DieselSectionType
import kotlinx.coroutines.launch

@Composable
fun EnteredCoefficientDialog(
    index: Int,
    coefficientValue: String?,
    showDialog: (Pair<Boolean, Int>) -> Unit,
    onSaveClick: (Int, String?) -> Unit,
    focusChangedDieselSection: (Int, DieselSectionType) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var temporaryValue by remember {
        mutableStateOf(coefficientValue)
    }

    AlertDialog(
        onDismissRequest = { showDialog(Pair(false, index)) },
        title = {
            Text(text = "Коэффициент")
        },
        text = {
            OutlinedTextField(
                modifier = Modifier
                    .padding(end = 4.dp),
                value = temporaryValue ?: "",
                onValueChange = {
                    temporaryValue = it
                },
                textStyle = AppTypography.getType().bodyLarge,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onNext = {
                    scope.launch {
                        focusManager.clearFocus()
                    }
                })
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    showDialog(Pair(false, index))
                    onSaveClick(index, temporaryValue)
                    focusChangedDieselSection(index, DieselSectionType.COEFFICIENT)
                }
            ) {
                Text(text = "Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = { showDialog(Pair(false, index)) }) {
                Text(text = "Отмена", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}