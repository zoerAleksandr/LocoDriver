package com.z_company.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmEmailDialog(
    onDismissRequest: () -> Unit,
    onConfirmButton: () -> Unit,
    emailForConfirm: String,
    onChangeEmail: (String) -> Unit,
    enableButtonConfirmVerification: Boolean
) {
    BasicAlertDialog(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = Shapes.medium
            ),
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            OutlinedTextField(
                value = emailForConfirm,
                onValueChange = onChangeEmail,
                textStyle = AppTypography.getType().bodyMedium
            )
            Text(
                text = "На данный email будет отправлено письмо со ссылкой для верификации аккаунта.",
                style = AppTypography.getType().bodySmall
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = Shapes.medium
                    ),
                onClick = { onConfirmButton() },
                shape = Shapes.medium,
                enabled = enableButtonConfirmVerification
            ) {
                Text(
                    text = "Отправить письмо",
                    style = AppTypography.getType().titleSmall
                )
            }
        }
    }
}