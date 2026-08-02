package com.z_company.route.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun ConfirmEmailDialog(
    onDismissRequest: () -> Unit,
    onConfirmButton: () -> Unit,
    emailForConfirm: String,
    onChangeEmail: (String) -> Unit,
    enableButtonConfirmVerification: Boolean
) {
    AppInputBottomSheet(
        onDismissRequest = onDismissRequest,
        title = "Подтверждение email",
        initialValue = emailForConfirm,
        onConfirm = { onConfirmButton() },
        confirmText = "Отправить письмо",
        hint = "На данный email будет отправлено письмо со ссылкой для верификации аккаунта.",
        keyboardType = KeyboardType.Email,
        onValueChange = onChangeEmail,
        // Валидность считает родитель.
        isValid = { enableButtonConfirmVerification },
    )
}
