package com.z_company.route.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.norma_time.StationNorm
import com.z_company.domain.repositories.StationNormRepository
import com.z_company.route.ui.settings.SettingsStationEditorContent
import com.z_company.route.viewmodel.StationNormEditorViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationPickerSheet(
    onSelect: (StationNorm) -> Unit,
    onClose: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onEditStation: ((StationNorm) -> Unit)? = null,
    // Открыть сразу в редакторе этой станции (например из «Настроить станцию»).
    initialEditStation: StationNorm? = null,
) {
    val stationRepo: StationNormRepository = koinInject()
    val stations by stationRepo.getAllFlow().collectAsState(initial = emptyList())

    var query by remember { mutableStateOf("") }

    val filtered = if (query.isBlank()) stations
    else stations.filter { it.name.contains(query, ignoreCase = true) }

    // «С нормами» — задана хотя бы одна норма интервала (остальные — прочерком).
    fun hasAnyNorm(s: StationNorm) = s.appearanceToStartMin != null || s.endToBarrierMin != null ||
        s.barrierToStartMin != null || s.endToWorkEndMin != null

    val withNorms = filtered.filter { hasAnyNorm(it) }
    val withoutNorms = filtered.filter { !hasAnyNorm(it) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Редактирование/создание станции показываем прямо внутри этой же шторки
    // (свап контента), чтобы «Назад» возвращал к списку станций, а не на форму.
    var editingStation by remember { mutableStateOf<StationNorm?>(null) }
    var addingStation by remember { mutableStateOf(false) }
    LaunchedEffect(initialEditStation) {
        if (initialEditStation != null) editingStation = initialEditStation
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        // Fixed height column so LazyColumn can scroll inside the sheet (~85% screen)
        Column(modifier = Modifier.fillMaxHeight(0.85f)) {
            val editing = editingStation
            if (editing != null || addingStation) {
                // null stationId → создание новой станции.
                val editStationId = editing?.stationId
                fun closeEditor() { editingStation = null; addingStation = false }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { closeEditor() }) {
                        Text("Назад", color = MaterialTheme.colorScheme.tertiary)
                    }
                    Text(
                        text = if (editStationId == null) "Новая станция" else "Станция",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.size(72.dp))
                }
                val editorVm = koinViewModel<StationNormEditorViewModel>(
                    key = "picker_station_editor_${editStationId ?: "new"}",
                    parameters = { parametersOf(editStationId, null) }
                )
                Box(modifier = Modifier.weight(1f)) {
                    SettingsStationEditorContent(
                        viewModel = editorVm,
                        onDone = { closeEditor() }
                    )
                }
                return@Column
            }
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onClose) {
                    Text("Отмена", color = MaterialTheme.colorScheme.tertiary)
                }
                Text(
                    text = "Станция",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.size(72.dp))
            }

            // Search
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Поиск станции...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.secondary,
                    focusedContainerColor = MaterialTheme.colorScheme.secondary,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                ),
                singleLine = true
            )

            // Scrollable list fills remaining space
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                item {
                    TextButton(
                        onClick = { addingStation = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "+ Добавить станцию",
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                if (stations.isEmpty()) {
                    item {
                        Text(
                            text = "Нет станций. Добавьте в Настройках → Станции.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                if (withNorms.isNotEmpty()) {
                    item {
                        Text(
                            text = "С НОРМАМИ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, Shapes.medium)
                                .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
                        ) {
                            withNorms.forEachIndexed { idx, s ->
                                StationPickerItem(
                                    station = s,
                                    showNorms = true,
                                    onClick = { onSelect(s) },
                                    onEdit = { editingStation = s }
                                )
                                if (idx < withNorms.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                                }
                            }
                        }
                    }
                }

                if (withoutNorms.isNotEmpty()) {
                    item {
                        Text(
                            text = "БЕЗ НОРМ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
                        )
                    }
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, Shapes.medium)
                                .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
                        ) {
                            withoutNorms.forEachIndexed { idx, s ->
                                StationPickerItem(
                                    station = s,
                                    showNorms = false,
                                    onClick = { onSelect(s) },
                                    onEdit = { editingStation = s }
                                )
                                if (idx < withoutNorms.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StationPickerItem(
    station: StationNorm,
    showNorms: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = if (onEdit != null) 6.dp else 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(2.dp))
            // Показываем, какие нормы заданы. Незаданные интервалы — 0.
            val hasAnyNorm = station.appearanceToStartMin != null || station.endToBarrierMin != null ||
                station.barrierToStartMin != null || station.endToWorkEndMin != null
            val subtitle = if (hasAnyNorm) {
                "Приёмка ${station.appearanceToStartMin ?: 0}/${station.endToBarrierMin ?: 0} · " +
                    "Сдача ${station.barrierToStartMin ?: 0}/${station.endToWorkEndMin ?: 0}"
            } else "Норма не задана"
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        }
        if (onEdit != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(com.z_company.core.R.drawable.ic_edit),
                    contentDescription = "Редактировать",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
