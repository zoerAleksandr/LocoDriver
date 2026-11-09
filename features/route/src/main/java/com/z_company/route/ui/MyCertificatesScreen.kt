package com.z_company.route.ui

import com.z_company.route.component.HeaderTextTile
import com.z_company.route.component.HeaderBackTile
import com.z_company.route.component.ScreenHeader
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.SelectionBottomBar
import com.z_company.route.component.SelectionMarker
import com.z_company.route.component.SwipeToRevealDelete
import com.z_company.route.viewmodel.CertificateItem
import com.z_company.route.viewmodel.MyCertificatesViewModel

/**
 * «Мои справки ВУ-45» (SCREEN_SPECS §26.10). Режим выбора, карточки, свайп-удаление и панель действий повторяют «Все маршруты»
 * (`AllRouteScreen`): те же `SelectionMarker`, `SwipeToRevealDelete`, `SelectionBottomBar`, шторка подтверждения.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCertificatesScreen(
    viewModel: MyCertificatesViewModel,
    onBack: () -> Unit,
    onOpen: (CertificateItem) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.reload() }
    val isSelectionMode = state.selectionMode
    val selectedIds = state.selected
    val isAllSelected = state.items.isNotEmpty() && selectedIds.containsAll(state.items.map { it.id })
    // Системная «назад» в режиме выбора выходит из него, а не с экрана.
    BackHandler(enabled = isSelectionMode) { viewModel.toggleSelectionMode() }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenHeader(
                title = if (isSelectionMode) "Выбрано: ${selectedIds.size}" else "Мои справки ВУ-45",
                navigation = {
                    if (isSelectionMode) {
                        HeaderTextTile(if (isAllSelected) "Снять" else "Все", { if (isAllSelected) viewModel.clearSelection() else viewModel.selectAll() }, color = MaterialTheme.colorScheme.surfaceContainerLow)
                    } else {
                        HeaderBackTile(onBack)
                    }
                },
                actions = {
                    HeaderTextTile(if (isSelectionMode) "Готово" else "Выбрать", viewModel::toggleSelectionMode, color = MaterialTheme.colorScheme.surfaceContainerLow)
                },
            )
        },
        bottomBar = {
            if (isSelectionMode) {
                SelectionBottomBar(
                    isEnabled = selectedIds.isNotEmpty(),
                    onDeleteClick = { viewModel.askDelete(selectedIds) },
                    deleteContentDescription = "Удалить выбранные справки",
                )
            }
        },
    ) { padding ->
        if (state.items.isEmpty() && !state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("Справок нет", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Справки сохраняются автоматически, когда вы заполняете ВУ-45 для поезда маршрута.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)) {
                items(state.items, key = { it.id }) { item ->
                    CertificateCard(
                        item, selectionMode = isSelectionMode, isSelected = item.id in selectedIds,
                        onClick = { if (isSelectionMode) viewModel.toggleSelected(item.id) else onOpen(item) },
                        onRequestDelete = { viewModel.askDelete(setOf(item.id)) },
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
    state.confirmDelete?.let { ids ->
        // подтверждение удаления - та же шторка, что в «Все маршруты»
        AppBottomSheet(
            onDismissRequest = viewModel::dismissDelete,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            headerContent = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (ids.size == 1) "Удалить справку?" else "Удалить справки?",
                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Выбрано: ${ids.size}", fontFamily = MonoFont, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp),
                    )
                }
            },
            actions = listOf(BottomSheetAction(text = "Да, удалить") { viewModel.confirmDelete(ids) }),
        )
    }
}

/** Карточка справки: оформление и поведение как у карточки маршрута («Все маршруты»): свайп-удаление, в режиме выбора - маркер и акцентная рамка. */
@Composable
private fun CertificateCard(
    item: CertificateItem,
    selectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val cardContent: @Composable () -> Unit = {
        Card(
            modifier = Modifier.fillMaxWidth().wrapContentHeight().defaultMinSize(minHeight = 65.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(containerColor = c.secondary, contentColor = c.primary),
            border = if (selectionMode && isSelected) BorderStroke(2.dp, c.surfaceContainerLow) else null,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectionMode) SelectionMarker(isSelected = isSelected, modifier = Modifier.padding(start = 12.dp))
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    Text(item.savedAt, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFont, fontWeight = FontWeight.Medium), color = c.primary)
                    Text(item.routeLabel, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    Text("Поезд ${item.trainNumber} · локомотив ${item.loco}", style = MaterialTheme.typography.bodyMedium, color = c.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (item.station.isNotBlank()) Text("Станция: ${item.station}", style = MaterialTheme.typography.bodyMedium, color = c.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    else Text("Станция не указана", style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant)
                }
            }
        }
    }
    if (selectionMode) {
        // В режиме выбора свайп-удаление выключено, тап по карточке - выделение.
        Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp).defaultMinSize(minHeight = 65.dp).clickable { onClick() }) { cardContent() }
    } else {
        SwipeToRevealDelete(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).defaultMinSize(minHeight = 65.dp),
            onDeleteClick = onRequestDelete,
            onContentClick = onClick,
            // долгое нажатие: у карточки справки контекстного меню нет (в «Все маршруты» оно открывает быстрый просмотр маршрута)
            onContentLongClick = {},
            backgroundVerticalPadding = 0.dp,
            itemKey = item.id,
        ) { _ -> cardContent() }
    }
}
