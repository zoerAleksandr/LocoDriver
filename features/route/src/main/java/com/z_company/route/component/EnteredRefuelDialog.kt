package com.z_company.route.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun EnteredRefuelDialog(
    refuelValue: String?,
    onSaveClick: (String?) -> Unit,
    onDismissClick: () -> Unit
) {
    AppInputBottomSheet(
        onDismissRequest = onDismissClick,
        title = "Экипировка",
        initialValue = refuelValue ?: "",
        onConfirm = { onSaveClick(it.ifBlank { null }) },
        label = "Значение",
        suffix = "л.",
        keyboardType = KeyboardType.Decimal,
        transform = { it.take(6) },
        // Разрешаем сохранить и пустое значение (сброс).
        isValid = { true },
    )
}
