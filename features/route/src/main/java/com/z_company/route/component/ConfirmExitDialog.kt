package com.z_company.route.component

import androidx.compose.runtime.Composable

@Composable
fun ConfirmExitDialog(
    showExitConfirmDialog: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    exitWithoutSave: () -> Unit
) {
    // «Выйти» без сохранения — деструктивное действие (danger). «Сохранить и
    // выйти» — безопасная кнопка (accent). Порядок по стандарту: деструктивное
    // справа (confirm), безопасное слева (dismiss).
    AppAlertDialog(
        onDismissRequest = { showExitConfirmDialog(false) },
        title = "Внимание",
        text = "При выходе все несохранённые данные будут утеряны.\n\nВы уверены, что хотите выйти?",
        confirmText = "Выйти",
        onConfirm = exitWithoutSave,
        isDestructive = true,
        dismissText = "Сохранить и выйти",
        onDismiss = {
            showExitConfirmDialog(false)
            onSaveClick()
        }
    )
}
