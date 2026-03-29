package com.z_company.route.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.z_company.core.ui.component.CustomDivider
import com.z_company.core.ui.theme.Shapes
import com.z_company.route.R
import com.z_company.route.component.AnimationDialog
import com.z_company.route.viewmodel.SyncStepState
import com.z_company.route.viewmodel.SyncType

/**
 * Общий composable диалога синхронизации — используется в HomeScreen и ProfileScreen.
 * Показывает прогресс по шагам, ошибки и кнопку отчёта.
 */
@Composable
fun SyncProgressDialog(
    showDialog: Boolean,
    isSyncSuccess: Boolean,
    isSyncComplete: Boolean,
    syncType: SyncType?,
    progressMap: Map<String, SyncStepState>,
    syncRouteErrors: List<String>,
    syncRoutesTotalAttempted: Int,
    syncRoutesSavedCount: Int,
    userId: String? = null,
    isNetworkError: Boolean = false,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current

    // --- Нет интернета ---
    if (isNetworkError) {
        AnimationDialog(showDialog = showDialog, onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surface, Shapes.medium)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.signal_disconnected_24px),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Нет интернета",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    shape = Shapes.medium,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Text("Понятно", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        return
    }

    // --- Диалог успешной синхронизации ---
    if (isSyncSuccess) {
        androidx.compose.material3.AlertDialog(
            containerColor = MaterialTheme.colorScheme.secondary,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.primary,
            iconContentColor = MaterialTheme.colorScheme.surfaceTint,
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    modifier = Modifier.size(86.dp),
                    painter = painterResource(R.drawable.check_circle_24px),
                    contentDescription = null
                )
            },
            title = {
                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = if (syncType == SyncType.Upload) "Выгрузка завершена!" else "Загрузка завершена!"
                )
            },
            text = { Text("Данные успешно синхронизированы.") },
            confirmButton = {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    onClick = onDismiss
                ) {
                    Text(
                        text = "Отлично!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        )
    }

    // --- Диалог прогресса / ошибок ---
    AnimationDialog(
        showDialog = showDialog,
        onDismissRequest = onDismiss
    ) {
        val styleData = MaterialTheme.typography.bodyLarge
        val primaryColor = MaterialTheme.colorScheme.primary
        var showSyncRouteErrors by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface, Shapes.medium)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (syncType == SyncType.Upload) "Выгрузка данных" else "Загрузка данных",
                style = MaterialTheme.typography.titleMedium,
                color = primaryColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            progressMap.forEach { (step, state) ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (step) {
                                "UserSettings" -> "Настройки пользователя"
                                "SalarySettings" -> "Настройки зарплаты"
                                "Months" -> "Календарь"
                                "Routes" -> "Маршруты"
                                else -> step
                            },
                            style = styleData,
                            modifier = Modifier.weight(1f)
                        )
                        when (state) {
                            is SyncStepState.Loading -> CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = primaryColor,
                                strokeWidth = 2.dp
                            )
                            is SyncStepState.Success -> Icon(
                                painter = painterResource(R.drawable.check_circle_24px),
                                contentDescription = "Успех",
                                tint = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            is SyncStepState.Error -> Icon(
                                painter = painterResource(com.z_company.core.R.drawable.ic_clear),
                                contentDescription = "Ошибка",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (state is SyncStepState.Error) {
                        if (step == "Routes" && syncRoutesTotalAttempted > 0) {
                            // Частичный успех маршрутов
                            Text(
                                text = "Синхронизировано $syncRoutesSavedCount из $syncRoutesTotalAttempted маршрутов",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            TextButton(
                                onClick = { showSyncRouteErrors = !showSyncRouteErrors },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = if (showSyncRouteErrors) "Скрыть ошибки" else "Показать ошибки",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = primaryColor
                                )
                            }
                            if (showSyncRouteErrors) {
                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                    syncRouteErrors.forEach { line ->
                                        Text(
                                            text = line,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                }
                            }
                        } else if (state.message.isNotEmpty()) {
                            // Ошибка другого шага или сети
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    if (state is SyncStepState.Success && state.details.contains("\n")) {
                        Text(
                            text = state.details,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                CustomDivider(orientation = Orientation.Horizontal)
            }

            if (isSyncComplete) {
                Spacer(modifier = Modifier.height(16.dp))

                // Кнопка отчёта — только при наличии НЕ сетевых ошибок
                val hasNonNetworkErrors = progressMap.values.any { it is SyncStepState.Error }
                if (!isSyncSuccess && !isNetworkError && hasNonNetworkErrors) {
                    OutlinedButton(
                        shape = Shapes.medium,
                        onClick = {
                            try {
                                val reportContent = buildString {
                                    val typeLabel = if (syncType == SyncType.Upload)
                                        "Выгрузка на сервер" else "Загрузка с сервера"
                                    appendLine("Отчет об ошибках синхронизации")
                                    appendLine("Тип: $typeLabel")
                                    if (!userId.isNullOrBlank()) appendLine("ID пользователя: $userId")
                                    appendLine("=".repeat(40))
                                    progressMap.forEach { (step, state) ->
                                        if (state is SyncStepState.Error) {
                                            val stepName = when (step) {
                                                "UserSettings" -> "Настройки пользователя"
                                                "SalarySettings" -> "Настройки зарплаты"
                                                "Months" -> "Календарь"
                                                "Routes" -> "Маршруты"
                                                else -> step
                                            }
                                            if (step == "Routes" && syncRoutesTotalAttempted > 0) {
                                                appendLine("$stepName: синхронизировано $syncRoutesSavedCount из $syncRoutesTotalAttempted")
                                                syncRouteErrors.forEach { appendLine("  - $it") }
                                            } else if (state.message.isNotEmpty()) {
                                                appendLine("$stepName: ${state.message}")
                                            }
                                        }
                                    }
                                }
                                val file = java.io.File(ctx.cacheDir, "sync_error_report.txt")
                                file.writeText(reportContent)
                                val uri = FileProvider.getUriForFile(
                                    ctx, "com.z_company.loco_driver.fileprovider", file
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("locodriver.app@yandex.ru"))
                                    putExtra(Intent.EXTRA_SUBJECT, "Отчет об ошибках синхронизации")
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                ctx.startActivity(Intent.createChooser(intent, "Отправить отчет"))
                            } catch (_: Exception) {
                                ctx.startActivity(
                                    Intent(Intent.ACTION_SENDTO).apply {
                                        data = "mailto:locodriver.app@yandex.ru?subject=Отчет об ошибках синхронизации".toUri()
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "Отправить отчет об ошибке",
                            style = styleData,
                            color = primaryColor
                        )
                    }
                }

                Button(
                    shape = Shapes.medium,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Text("Понятно", style = styleData)
                }
            }
        }
    }
}
