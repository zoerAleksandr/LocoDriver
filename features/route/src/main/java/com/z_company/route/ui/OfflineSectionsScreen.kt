package com.z_company.route.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.route.component.ChipApp
import com.z_company.route.component.AppAlertDialog
import com.z_company.route.component.HeaderBackTile
import com.z_company.route.component.ScreenHeader
import com.z_company.route.viewmodel.OfflineSectionItem
import com.z_company.route.viewmodel.OfflineSectionsViewModel
import com.z_company.route.viewmodel.OfflineStatus
import java.util.Locale

/**
 * «Скачанные участки» (SCREEN_SPECS §6.10): список опубликованных участков с поиском. Пользователь скачивает нужные -
 * профиль пути, станции и ось сохраняются на телефоне (зашифрованно), «Профиль пути» и определение плеча работают без сети.
 * Скачанное, изменившееся на сервере, помечается «Есть обновление». Открывается из Профиля → «ДАННЫЕ».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineSectionsScreen(viewModel: OfflineSectionsViewModel, onBack: () -> Unit) {
    val ui by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var confirmRemove by remember { mutableStateOf<OfflineSectionItem?>(null) }
    LaunchedEffect(ui.message) {
        ui.message?.let {
            viewModel.messageShown()
            snackbar.showSnackbar(it)
        }
    }
    BackHandler(onBack = onBack)

    confirmRemove?.let { item ->
        AppAlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = "Удалить участок с телефона?",
            text = "«${item.title}» будет удалён с этого устройства. Его можно скачать снова, пока есть интернет.",
            confirmText = "Удалить",
            isDestructive = true,
            onConfirm = { viewModel.remove(item.id); confirmRemove = null },
            dismissText = "Отмена",
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { ScreenHeader(title = "Скачанные участки", navigation = { HeaderBackTile(onBack) }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                ui.needLogin -> Note("Участки доступны после входа в аккаунт. Войдите в приложение (Профиль).")
                ui.loading && ui.items.isEmpty() -> Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                }
                else -> {
                    Summary(ui.downloadedCount, ui.totalBytes, ui.outdatedCount, ui.busy, viewModel::updateOutdated)
                    OutlinedTextField(
                        value = ui.query,
                        onValueChange = viewModel::setQuery,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Поиск по названию участка") },
                        keyboardOptions = KeyboardOptions.Default,
                        shape = Shapes.small,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChipApp(label = "Все", selected = !ui.onlyDownloaded, onClick = { viewModel.setOnlyDownloaded(false) })
                        ChipApp(label = "Скачано", selected = ui.onlyDownloaded, onClick = { viewModel.setOnlyDownloaded(true) })
                    }
                    if (ui.items.isEmpty()) {
                        Note(
                            when {
                                ui.onlyDownloaded -> "Скачанных участков пока нет."
                                ui.query.isBlank() -> "Участки не загружены. Проверьте интернет."
                                else -> "Ничего не найдено."
                            },
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            items(ui.items, key = { it.id }) { item ->
                                SectionRow(item, onDownload = { viewModel.download(item.id) }, onRemove = { confirmRemove = item })
                            }
                            item {
                                Text(
                                    "У сервера есть суточный предел числа разных загружаемых участков. Данные участка (профиль, станции) хранятся " +
                                        "на телефоне и обновляются, когда меняются на сервере.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun Summary(count: Int, bytes: Long, outdated: Int, busy: Boolean, onUpdate: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().shadow(1.dp, Shapes.medium).background(MaterialTheme.colorScheme.secondary, Shapes.medium).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("На телефоне", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "$count ${plural(count)} · ${sizeText(bytes)}",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFont, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (outdated > 0) {
            if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else TextButton(onClick = onUpdate) { Text("Обновить ($outdated)") }
        }
    }
}

@Composable
private fun SectionRow(item: OfflineSectionItem, onDownload: () -> Unit, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().shadow(1.dp, Shapes.medium).background(MaterialTheme.colorScheme.secondary, Shapes.medium).padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val meta = buildString {
                if (item.lengthKm > 0) append(String.format(Locale("ru"), "%.0f км", item.lengthKm))
                when (item.status) {
                    OfflineStatus.READY -> append(" · ${sizeText(item.bytes)} · на телефоне")
                    OfflineStatus.OUTDATED -> append(" · ${sizeText(item.bytes)} · есть обновление")
                    OfflineStatus.DOWNLOADING -> append(" · загрузка…")
                    OfflineStatus.NOT_DOWNLOADED -> Unit
                }
            }
            if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when (item.status) {
            OfflineStatus.DOWNLOADING -> Box(Modifier.padding(horizontal = 14.dp)) { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) }
            OfflineStatus.NOT_DOWNLOADED -> TextButton(onClick = onDownload) { Text("Скачать") }
            OfflineStatus.OUTDATED -> Row {
                TextButton(onClick = onDownload) { Text("Обновить") }
                TextButton(onClick = onRemove) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            }
            OfflineStatus.READY -> TextButton(onClick = onRemove) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
        }
    }
}

private fun plural(n: Int): String {
    val m10 = n % 10; val m100 = n % 100
    return when { m10 == 1 && m100 != 11 -> "участок"; m10 in 2..4 && m100 !in 12..14 -> "участка"; else -> "участков" }
}

private fun sizeText(bytes: Long): String = when {
    bytes <= 0 -> "0 КБ"
    bytes < 1024 * 1024 -> String.format(Locale("ru"), "%.0f КБ", bytes / 1024.0)
    else -> String.format(Locale("ru"), "%.1f МБ", bytes / 1048576.0)
}
