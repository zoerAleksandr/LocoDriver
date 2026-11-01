package com.z_company.route.ui

import com.z_company.route.component.HeaderTextTile
import com.z_company.route.component.HeaderBackTile
import com.z_company.route.component.ScreenHeader
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.z_company.route.R
import com.z_company.route.component.AppAlertDialog
import com.z_company.route.component.rememberNetworkGate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.z_company.route.ui.settings.SettingsPrivacyContent
import com.z_company.route.ui.settings.SettingsTrackHistoryContent
import com.z_company.route.viewmodel.TrackHistoryItem
import com.z_company.route.viewmodel.TrackHistoryViewModel

/** «Данные и конфиденциальность» — отдельный экран, открывается из Профиля → «ДАННЫЕ» (SCREEN_SPECS §17.7, §18). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataPrivacyScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            ScreenHeader(
                title = "Данные и конфиденциальность",
                navigation = { HeaderBackTile(onBack) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { SettingsPrivacyContent() }
    }
}

/**
 * «Мои треки» — отдельный экран, открывается из Профиля → «ДАННЫЕ» (SCREEN_SPECS §17.8, §18).
 * Множественный выбор — тот же паттерн, что на «Все маршруты» (§9.2): «Выбрать» в топбаре, «Все/Снять», «Выбрано: N»,
 * «Готово», нижняя панель массовых действий.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackHistoryScreen(
    viewModel: TrackHistoryViewModel,
    onBack: () -> Unit,
    onOpenDataPrivacy: () -> Unit = {},
    onOpenPurchases: () -> Unit = {},
) {
    val ui by viewModel.state.collectAsState()
    val isSelectionMode = ui.isSelectionMode
    val selectedIds = ui.selectedIds
    val isAllSelected = ui.items.isNotEmpty() && selectedIds.containsAll(ui.items.map { it.id })
    var confirmDelete by remember { mutableStateOf(false) }
    var noConsent by remember { mutableStateOf(false) }
    var noSubscription by remember { mutableStateOf(false) }
    val networkGate = rememberNetworkGate()

    val snackbarHostState = remember { SnackbarHostState() }
    // Гейт синхронизации одного трека: нет согласия — диалог, нет сети — «Нет интернета» (как в массовом действии).
    fun requestSync(item: TrackHistoryItem) {
        when {
            !ui.subscribed -> noSubscription = true
            !ui.consent -> noConsent = true
            else -> networkGate.run { viewModel.syncOne(item) }
        }
    }
    LaunchedEffect(ui.message) {
        ui.message?.let {
            viewModel.messageShown()
            snackbarHostState.showSnackbar(it)
        }
    }

    BackHandler { if (isSelectionMode) viewModel.exitSelectionMode() else onBack() }

    if (confirmDelete) {
        val n = selectedIds.size
        AppAlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = if (n == 1) "Удалить трек?" else "Удалить треки?",
            text = (if (n == 1) "Выбран 1 трек. " else "Выбрано треков: $n. ") +
                "Они будут удалены с этого устройства и с сервера. Это действие нельзя отменить.",
            confirmText = "Удалить",
            isDestructive = true,
            onConfirm = {
                confirmDelete = false
                viewModel.deleteSelected()
            },
            dismissText = "Отмена",
        )
    }
    if (noSubscription) {
        AppAlertDialog(
            onDismissRequest = { noSubscription = false },
            title = "Синхронизация треков доступна по подписке",
            text = "Синхронизация треков доступна по подписке «Машинист Pro». " +
                "Треки сохраняются на устройстве и отправятся на сервер после оформления подписки.",
            confirmText = "Оформить подписку",
            onConfirm = {
                noSubscription = false
                onOpenPurchases()
            },
            dismissText = "Закрыть",
        )
    }
    if (noConsent) {
        AppAlertDialog(
            onDismissRequest = { noConsent = false },
            title = "Нет согласия на сохранение треков",
            text = "Чтобы отправлять треки на сервер, включите «Сохранять треки» " +
                "в разделе «Данные и конфиденциальность».",
            confirmText = "Открыть настройки",
            onConfirm = {
                noConsent = false
                onOpenDataPrivacy()
            },
            dismissText = "Отмена",
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ScreenHeader(
                title = if (isSelectionMode) "Выбрано: ${selectedIds.size}" else "Мои треки",
                navigation = {
                    if (isSelectionMode) {
                        HeaderTextTile(if (isAllSelected) "Снять" else "Все", viewModel::toggleSelectAll, color = MaterialTheme.colorScheme.surfaceContainerLow)
                    } else {
                        HeaderBackTile(onBack)
                    }
                },
                actions = {
                    if (isSelectionMode || ui.items.isNotEmpty()) {
                        HeaderTextTile(
                            text = if (isSelectionMode) "Готово" else "Выбрать",
                            onClick = { if (isSelectionMode) viewModel.exitSelectionMode() else viewModel.enterSelectionMode() },
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        )
                    }
                },
            )
        },
        bottomBar = {
            // Нижнее меню приложения здесь не показывается (отдельный маршрут): панель действий занимает его место.
            if (isSelectionMode) {
                val chosen = ui.items.filter { it.id in selectedIds }
                TrackSelectionBar(
                    isEnabled = selectedIds.isNotEmpty() && !ui.bulkBusy,
                    isAllFavorite = chosen.isNotEmpty() && chosen.all { it.isFavorite },
                    onFavoriteClick = viewModel::toggleFavoriteSelected,
                    onSyncClick = {
                        when {
                            !ui.subscribed -> noSubscription = true
                            !ui.consent -> noConsent = true
                            else -> networkGate.run { viewModel.syncSelected() }
                        }
                    },
                    onDeleteClick = { confirmDelete = true },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { SettingsTrackHistoryContent(viewModel = viewModel, onRequestSync = ::requestSync) }
    }
}

/** Нижняя панель массовых действий над выбранными треками: тот же вид, что у «Все маршруты» (§9.2), но с подписями. */
@Composable
private fun TrackSelectionBar(
    isEnabled: Boolean,
    isAllFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onSyncClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val disabledTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackBarAction(
                label = if (isAllFavorite) "Убрать из избранного" else "В избранное",
                iconRes = com.z_company.core.R.drawable.ic_star,
                tint = when {
                    !isEnabled -> disabledTint
                    isAllFavorite -> Color(0xFFF5B301)
                    else -> MaterialTheme.colorScheme.primary
                },
                enabled = isEnabled,
                onClick = onFavoriteClick,
                modifier = Modifier.weight(1f),
            )
            TrackBarAction(
                label = "Синхронизировать",
                iconRes = R.drawable.sync_24px,
                tint = if (isEnabled) MaterialTheme.colorScheme.primary else disabledTint,
                enabled = isEnabled,
                onClick = onSyncClick,
                modifier = Modifier.weight(1f),
            )
            TrackBarAction(
                label = "Удалить",
                iconRes = R.drawable.delete_24px,
                tint = if (isEnabled) MaterialTheme.colorScheme.error else disabledTint,
                enabled = isEnabled,
                onClick = onDeleteClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TrackBarAction(
    label: String,
    iconRes: Int,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
    }
}
