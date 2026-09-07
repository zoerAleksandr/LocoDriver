package com.z_company.route.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.TrashPurgePolicy
import com.z_company.route.R
import com.z_company.route.component.AppAlertDialog
import com.z_company.route.viewmodel.TrashViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(viewModel: TrashViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEmptyConfirmation by remember { mutableStateOf(false) }
    val purgeableCount = state.routes.count(TrashPurgePolicy::canPurgeManually)

    if (showEmptyConfirmation) {
        AppAlertDialog(
            onDismissRequest = { if (!state.isPurging) showEmptyConfirmation = false },
            title = "Очистить корзину?",
            text = "Будут безвозвратно удалены $purgeableCount маршрутов.",
            confirmText = "Удалить",
            isDestructive = true,
            onConfirm = {
                showEmptyConfirmation = false
                viewModel.emptyTrash()
            },
            dismissText = "Отмена",
            onDismiss = { showEmptyConfirmation = false },
        )
    }

    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { viewModel.onScreenOpened() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it) }
        if (state.message != null) viewModel.consumeMessage()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(com.z_company.core.R.drawable.ic_arrow_back),
                            contentDescription = "Назад",
                        )
                    }
                },
                title = { Text("Корзина", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    if (purgeableCount > 0) {
                        TextButton(
                            onClick = { showEmptyConfirmation = true },
                            enabled = !state.isRestoringAll && !state.isPurging && state.restoringRouteId == null,
                        ) {
                            Text(
                                if (state.isPurging) "Очистка…" else "Очистить",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { CustomSnackBar(snackBarData = it) }
        },
    ) { padding ->
        TrashContent(
            routes = state.routes,
            restoringRouteId = state.restoringRouteId,
            isRestoringAll = state.isRestoringAll,
            isSyncing = state.isSyncing,
            onRestore = viewModel::restore,
            onRestoreAll = viewModel::restoreAll,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun TrashContent(
    routes: List<Route>,
    restoringRouteId: String?,
    isRestoringAll: Boolean,
    isSyncing: Boolean,
    onRestore: (String) -> Unit,
    onRestoreAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pendingCount = routes.count { it.basicData.remoteDeletionPending }
    val waitingForServerCount = routes.count {
        !it.basicData.remoteDeletionPending &&
            !it.basicData.remoteRouteId.isNullOrBlank() &&
            it.basicData.remoteDeletedAt == null
    }

    if (routes.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painterResource(R.drawable.delete_24px),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.height(12.dp))
                Text("Корзина пуста", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Удалённые маршруты появятся здесь",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isSyncing) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Синхронизация корзины…", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer, Shapes.medium)
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                        Shapes.medium,
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Здесь маршруты хранятся 30 дней, затем удаляются автоматически. До этого их можно восстановить.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (pendingCount > 0) {
                    Text(
                        "Проверяем статус: $pendingCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else if (waitingForServerCount > 0) {
                    Text(
                        "Ожидают синхронизации: $waitingForServerCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        item {
            Button(
                onClick = onRestoreAll,
                enabled = restoringRouteId == null && !isRestoringAll,
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.surfaceTint,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.06f),
                    disabledContentColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.45f),
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceTint),
            ) { Text(if (isRestoringAll) "Восстановление…" else "Восстановить всё") }
        }

        items(routes, key = { it.basicData.id }) { route ->
            TrashRouteCard(
                route = route,
                restoring = restoringRouteId == route.basicData.id,
                enabled = restoringRouteId == null && !isRestoringAll,
                onRestore = { onRestore(route.basicData.id) },
            )
        }
    }
}

@Composable
private fun TrashRouteCard(route: Route, restoring: Boolean, enabled: Boolean, onRestore: () -> Unit) {
    val data = route.basicData
    val waitingForServer = !data.remoteDeletionPending && !data.remoteRouteId.isNullOrBlank() && data.remoteDeletedAt == null
    val useLargeFontLayout = LocalDensity.current.fontScale >= 1.3f
    val routeDateText = data.timeStartWork?.let(::formatRouteDate) ?: "Дата и время не указаны"
    val restoreButton: @Composable () -> Unit = {
        Button(
            onClick = onRestore,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.surfaceTint,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.06f),
                disabledContentColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.45f),
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceTint),
            shape = Shapes.medium,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(if (restoring) "Восстановление…" else "Восстановить")
        }
    }
    val routeDate: @Composable (Modifier) -> Unit = { modifier ->
        Text(
            text = routeDateText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = MonoFont,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    }
    Card(
        modifier = Modifier.fillMaxWidth().shadow(1.dp, Shapes.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        shape = Shapes.medium,
    ) {
        Column(Modifier.padding(16.dp)) {
            if (useLargeFontLayout) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                    restoreButton()
                }
                routeDate(Modifier.fillMaxWidth().padding(top = 8.dp))
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    routeDate(Modifier.weight(1f).padding(top = 10.dp))
                    restoreButton()
                }
            }
            data.deletedAt?.let {
                Text(
                    "Удалён ${formatTrashDate(it)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    trashRetentionText(it),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (waitingForServer) {
                Text(
                    "Ожидает удаления с сервера",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

private fun formatRouteDate(value: Long): String =
    SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale.getDefault()).format(Date(value))

private fun formatTrashDate(value: Long): String =
    SimpleDateFormat("dd.MM.yyyy в HH:mm", Locale.getDefault()).format(Date(value))

private fun trashRetentionText(deletedAt: Long): String {
    val retentionMs = 30L * 24L * 60L * 60L * 1000L
    val remainingMs = (deletedAt + retentionMs - System.currentTimeMillis()).coerceAtLeast(0L)
    val remainingDays = ((remainingMs + 86_399_999L) / 86_400_000L).toInt()
    return if (remainingDays == 0) "Срок хранения истёк"
    else "Осталось $remainingDays ${trashPluralRu(remainingDays, "день", "дня", "дней")}"
}

internal fun trashPluralRu(n: Int, one: String, few: String, many: String): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
}
