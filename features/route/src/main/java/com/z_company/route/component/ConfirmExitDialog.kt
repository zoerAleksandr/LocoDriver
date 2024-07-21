package com.z_company.route.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography

@Composable
fun ConfirmExitDialog(
    showExitConfirmDialog: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    exitWithoutSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { showExitConfirmDialog(false) },
        title = {
            Text(text = "Внимание", style = AppTypography.getType().headlineSmall)
        },
        shape = Shapes.medium,
        text = {
            Text(
                text = "При выходе все несохранённые данные будут утеряны.\n\nВы уверены, что хотите выйти?",
                style = AppTypography.getType().bodyLarge
            )
        },
        confirmButton = {
            Button(
                shape = Shapes.medium,
                onClick = {
                    showExitConfirmDialog(false)
                    onSaveClick()
                }
            ) {
                Text(text = "Сохранить и выйти", style = AppTypography.getType().titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = exitWithoutSave) {
                Text(
                    text = "Выйти",
                    style = AppTypography.getType().titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}