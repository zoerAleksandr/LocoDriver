package com.z_company.loco_driver.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun MigrationRecoveryScreen(
    isRetrying: Boolean,
    isCloudRestoring: Boolean,
    cloudRecoveryEnabled: Boolean,
    cloudMessage: String?,
    errorCode: String?,
    onRetry: () -> Unit,
    onCloudRestore: () -> Unit,
    onClose: () -> Unit,
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Данные сохранены",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (errorCode == "RouteMigrationLowStorageException") {
                        "Для безопасного обновления не хватает свободного места. " +
                            "Освободите место на устройстве и повторите восстановление. " +
                            "Исходные маршруты не изменены."
                    } else {
                        "Обновление базы не завершилось. Исходная копия маршрутов сохранена. " +
                            "Можно безопасно повторить восстановление."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                errorCode?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Код ошибки: $it",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRetrying && !isCloudRestoring,
                    onClick = onRetry,
                ) {
                    if (isRetrying) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Восстановить приложение")
                    }
                }
                if (cloudRecoveryEnabled) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isRetrying && !isCloudRestoring,
                        onClick = onCloudRestore,
                    ) {
                        if (isCloudRestoring) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Восстановить резервную копию аккаунта")
                        }
                    }
                    cloudMessage?.let { message ->
                        Spacer(Modifier.height(8.dp))
                        Text(message, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRetrying && !isCloudRestoring,
                    onClick = onClose,
                ) {
                    Text("Закрыть")
                }
            }
        }
    }
}
