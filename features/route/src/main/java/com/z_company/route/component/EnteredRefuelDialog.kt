package com.z_company.route.component

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import kotlinx.coroutines.launch

@Composable
fun EnteredRefuelDialog(
    refuelValue: String?,
    onSaveClick: (String?) -> Unit,
    onDismissClick: () -> Unit
    ) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var temporaryValue by remember {
        mutableStateOf(refuelValue)
    }
    val dataTextStyle = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
    val subTitleTextStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
    AlertDialog(
        onDismissRequest = onDismissClick,
        title = {
            Text(text = "Экипировка", style = AppTypography.getType().headlineSmall)
        },
        shape = Shapes.medium,
        text = {
            OutlinedTextField(
                modifier = Modifier
                    .padding(end = 4.dp, top = 24.dp),
                value = temporaryValue ?: "",
                onValueChange = {
                    temporaryValue = it.take(6)
                },

                suffix = {
                    Text(text = "л.", style = dataTextStyle)
                },
                textStyle = dataTextStyle,
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
                    onSaveClick(temporaryValue)
                },
                shape = Shapes.medium
            ) {
                Text(text = "Сохранить", style = subTitleTextStyle)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissClick) {
                Text(text = "Отмена", style = subTitleTextStyle, color = MaterialTheme.colorScheme.error)
            }
        }
    )
}