package com.example.route.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ConfirmExitDialog(
    showExitConfirmDialog: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    exitWithoutSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { showExitConfirmDialog(false) },
        title = {
            Text(text = "Предупреждение")
        },
        text = {
            Text(text = "Данные не сохранены.\nВсе равно выйти?")
        },
        confirmButton = {
            Button(
                onClick = {
                    showExitConfirmDialog(false)
                    onSaveClick()
                }
            ) {
                Text(text = "Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = { exitWithoutSave() }) {
                Text(text = "Выйти", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}