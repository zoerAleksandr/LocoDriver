package com.z_company.route.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.repositories.LocomotiveSeriesRepository
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SeriesPickerSheet(
    onSelect: (LocomotiveSeries) -> Unit,
    onClose: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onEditSeries: ((LocomotiveSeries) -> Unit)? = null,
) {
    val seriesRepo: LocomotiveSeriesRepository = koinInject()
    val allSeries by seriesRepo.getAllFlow().collectAsState(initial = emptyList())

    var query by remember { mutableStateOf("") }

    val filtered = if (query.isBlank()) allSeries
    else allSeries.filter { it.name.contains(query, ignoreCase = true) }

    val electric = filtered.filter { it.type == LocoType.ELECTRIC }
    val diesel = filtered.filter { it.type == LocoType.DIESEL }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.85f)) {
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
                    text = "Серия",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.size(72.dp))
            }

            // Search
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Поиск серии...") },
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

            // List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                item {
                    TextButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "+ Добавить серию",
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                if (allSeries.isEmpty()) {
                    item {
                        Text(
                            text = "Нет серий. Добавьте в Настройках → Серии.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                if (electric.isNotEmpty()) {
                    item {
                        Text(
                            text = "ЭЛЕКТРОВОЗЫ",
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
                            electric.forEachIndexed { idx, s ->
                                SeriesRow(
                                    series = s,
                                    onClick = { onSelect(s) },
                                    onLongClick = onEditSeries?.let { cb -> { cb(s) } },
                                    showChevron = false
                                )
                                if (idx < electric.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                                }
                            }
                        }
                    }
                }

                if (diesel.isNotEmpty()) {
                    item {
                        Text(
                            text = "ТЕПЛОВОЗЫ",
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
                            diesel.forEachIndexed { idx, s ->
                                SeriesRow(
                                    series = s,
                                    onClick = { onSelect(s) },
                                    onLongClick = onEditSeries?.let { cb -> { cb(s) } },
                                    showChevron = false
                                )
                                if (idx < diesel.lastIndex) {
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

