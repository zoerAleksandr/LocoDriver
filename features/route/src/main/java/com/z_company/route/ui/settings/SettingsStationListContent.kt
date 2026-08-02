package com.z_company.route.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.norma_time.StationNorm
import com.z_company.route.viewmodel.StationNormListViewModel

@Composable
fun SettingsStationListContent(
    viewModel: StationNormListViewModel,
    legacyNames: List<String>,
    onOpenEditor: (String?) -> Unit,
    onOpenLegacy: (String) -> Unit,
) {
    val stations by viewModel.stationsFlow.collectAsState()
    // «С нормами» — станция, где задана хотя бы одна норма интервала (остальные
    // показываем прочерком). «Без норм» — где не задано ничего.
    fun hasAnyNorm(s: StationNorm) = s.appearanceToStartMin != null || s.endToBarrierMin != null ||
        s.barrierToStartMin != null || s.endToWorkEndMin != null
    val withNorms = stations.filter { hasAnyNorm(it) }
    val withoutNormsRecords = stations.filter { !hasAnyNorm(it) }

    // Раздел «без норм» = записи с неполными нормами (открываются по id) + «старые»
    // имена из stationList, которым не сопоставлена запись (открываются по имени).
    // Для частично заполненных станций показываем, какие нормы уже заданы.
    val recordNames = stations.map { it.name.trim().lowercase() }.toSet()
    val noNormItems: List<StationNoNorm> =
        withoutNormsRecords.map { s ->
            val hasAny = s.appearanceToStartMin != null || s.endToBarrierMin != null ||
                s.barrierToStartMin != null || s.endToWorkEndMin != null
            StationNoNorm(
                name = s.name,
                id = s.stationId,
                subtitle = if (hasAny) stationNormSummary(s) else null
            )
        } +
            legacyNames
                .map { it.trim() }
                .filter { it.isNotBlank() && it.lowercase() !in recordNames }
                .distinct()
                .map { StationNoNorm(it, null, null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (withNorms.isEmpty() && noNormItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Нет сохранённых станций",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Добавьте станции с нормами интервалов",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (withNorms.isNotEmpty()) {
            Text(
                text = "С НОРМАМИ ДЛЯ ЛОКОМОТИВА · ${withNorms.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
            )
            StationGroupCard(items = withNorms, showNorms = true, onOpenEditor = onOpenEditor)
        }

        if (noNormItems.isNotEmpty()) {
            Text(
                text = "БЕЗ НОРМ · ${noNormItems.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
            )
            StationNoNormCard(
                items = noNormItems,
                onClickItem = { item ->
                    if (item.id != null) onOpenEditor(item.id)
                    else onOpenLegacy(item.name)
                }
            )
            Text(
                text = "Откройте станцию и задайте нормы интервалов.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = Shapes.medium)
                .background(color = MaterialTheme.colorScheme.secondary, shape = Shapes.medium)
                .clickable { onOpenEditor(null) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(com.z_company.core.R.drawable.ic_add),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "  Добавить станцию",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

/**
 * Подпись «какие нормы есть у станции». Незаданные интервалы — 0.
 * Группировка: Приёмка (явка→начало / конец→КП), Сдача (КП→начало / конец→работа).
 */
private fun stationNormSummary(s: StationNorm): String =
    "Приёмка ${s.appearanceToStartMin ?: 0}/${s.endToBarrierMin ?: 0} · " +
        "Сдача ${s.barrierToStartMin ?: 0}/${s.endToWorkEndMin ?: 0}"

/** Станция без полного набора норм. subtitle — какие нормы уже заданы, либо null. */
private data class StationNoNorm(val name: String, val id: String?, val subtitle: String?)

@Composable
private fun StationNoNormCard(
    items: List<StationNoNorm>,
    onClickItem: (StationNoNorm) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = Shapes.medium)
            .background(color = MaterialTheme.colorScheme.secondary, shape = Shapes.medium)
    ) {
        items.forEachIndexed { idx, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClickItem(item) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Название станции — имя собственное → Inter (по правилам шрифтов).
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = item.subtitle ?: "Нормы не заданы",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
                Icon(
                    painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_right_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
            if (idx < items.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}

@Composable
private fun StationGroupCard(
    items: List<StationNorm>,
    showNorms: Boolean,
    onOpenEditor: (String?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = Shapes.medium)
            .background(color = MaterialTheme.colorScheme.secondary, shape = Shapes.medium)
    ) {
        items.forEachIndexed { idx, s ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenEditor(s.stationId) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = s.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (showNorms) {
                        Text(
                            text = stationNormSummary(s),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }
                }
                Icon(
                    painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_right_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
            if (idx < items.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}
