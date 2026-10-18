package com.z_company.route.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.z_company.core.ui.theme.MonoFont
import com.z_company.route.R
import com.z_company.domain.brake.StatusLevel
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.viewmodel.CertificateItem
import com.z_company.route.viewmodel.MyCertificatesViewModel

/** «Мои справки ВУ-45» (SCREEN_SPECS §26.10): список сохранённых справок, удаление по одной и пачкой. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCertificatesScreen(
    viewModel: MyCertificatesViewModel,
    onBack: () -> Unit,
    onOpen: (CertificateItem) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.reload() }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (state.selectionMode) "Выбрано: ${state.selected.size}" else "Мои справки ВУ-45", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(com.z_company.core.R.drawable.ic_arrow_back), contentDescription = "Назад", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    if (state.items.isNotEmpty()) {
                        if (state.selectionMode) TextButton(onClick = viewModel::selectAll) { Text("Все", color = MaterialTheme.colorScheme.tertiary) }
                        TextButton(onClick = viewModel::toggleSelectionMode) {
                            Text(if (state.selectionMode) "Готово" else "Выбрать", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = {
            if (state.selectionMode) {
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondary).padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { viewModel.askDelete(state.selected) }, enabled = state.selected.isNotEmpty(),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Удалить (${state.selected.size})", color = if (state.selected.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    }
                }
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
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.items, key = { it.id }) { item ->
                    CertificateCard(
                        item, selectionMode = state.selectionMode, selected = item.id in state.selected,
                        onClick = { if (state.selectionMode) viewModel.toggleSelected(item.id) else onOpen(item) },
                        onDelete = { viewModel.askDelete(setOf(item.id)) },
                    )
                }
            }
        }
    }
    state.confirmDelete?.let { ids ->
        AppBottomSheet(
            onDismissRequest = viewModel::dismissDelete,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            headerContent = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (ids.size == 1) "Удалить справку?" else "Удалить справки (${ids.size})?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                    Text("Справки будут удалены с этого устройства и с сервера", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp))
                }
            },
            actions = listOf(BottomSheetAction(text = "Да, удалить") { viewModel.confirmDelete(ids) }),
        )
    }
}

@Composable
private fun CertificateCard(item: CertificateItem, selectionMode: Boolean, selected: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val verdictColor = when (item.level) { StatusLevel.DANGER -> c.error; StatusLevel.OK -> c.tertiary; else -> c.onSurfaceVariant }
    Row(
        Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(14.dp)).background(c.secondary, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (selectionMode) Checkbox(checked = selected, onCheckedChange = { onClick() })
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(item.savedAt, fontFamily = MonoFont, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.primary)
            Text(item.routeLabel, fontSize = 13.sp, color = c.onSurfaceVariant)
            Text("Поезд ${item.trainNumber} · локомотив ${item.loco}", fontSize = 13.sp, color = c.primary)
            Text(item.verdict, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = verdictColor)
        }
        if (!selectionMode) {
            IconButton(onClick = onDelete) {
                Icon(painter = painterResource(R.drawable.delete_24px), contentDescription = "Удалить справку", tint = c.onSurfaceVariant)
            }
        }
    }
}
