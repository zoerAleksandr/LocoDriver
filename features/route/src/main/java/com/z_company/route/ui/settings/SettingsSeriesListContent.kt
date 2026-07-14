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
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.route.LocoType
import com.z_company.route.viewmodel.SeriesListViewModel

@Composable
fun SettingsSeriesListContent(
    viewModel: SeriesListViewModel,
    legacyNames: List<String>,
    onOpenEditor: (String?) -> Unit,
    onOpenLegacy: (String) -> Unit,
) {
    val series by viewModel.seriesFlow.collectAsState()

    // Серия «с нормой» = задана приёмка и/или сдача. Только такие идут в группы
    // по типу тяги. Записи без норм считаем такими же «старыми», как имена из
    // locomotiveSeriesList — они собираются в нижнем разделе «без норм».
    fun LocomotiveSeries.hasNorm() =
        acceptanceDurationMin != null || deliveryDurationMin != null ||
            acceptanceHandToHandMin != null || deliveryHandToHandMin != null

    val withNorms = series.filter { it.hasNorm() }
    val electric = withNorms.filter { it.type == LocoType.ELECTRIC }
    val diesel = withNorms.filter { it.type == LocoType.DIESEL }

    val recordNames = series.map { it.name.trim().lowercase() }.toSet()
    // Нижний раздел: записи без норм (открываются по id) + «старые» имена из
    // locomotiveSeriesList, которым не сопоставлена запись (открываются по имени).
    val noNormItems: List<NoNormItem> =
        series.filter { !it.hasNorm() }.map { NoNormItem(it.name, it.seriesId) } +
            legacyNames
                .map { it.trim() }
                .filter { it.isNotBlank() && it.lowercase() !in recordNames }
                .distinct()
                .map { NoNormItem(it, null) }

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
                        text = "Нет сохранённых серий",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Добавьте серии с нормами приёмки и сдачи",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (electric.isNotEmpty()) {
            Text(
                text = "ЭЛЕКТРОВОЗЫ · ${electric.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
            )
            SeriesGroupCard(items = electric, onOpenEditor = onOpenEditor)
        }

        if (diesel.isNotEmpty()) {
            Text(
                text = "ТЕПЛОВОЗЫ · ${diesel.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
            )
            SeriesGroupCard(items = diesel, onOpenEditor = onOpenEditor)
        }

        // Серии без норм приёмки/сдачи — ниже, с пометкой
        if (noNormItems.isNotEmpty()) {
            Text(
                text = "БЕЗ НОРМ ПРИЁМКИ И СДАЧИ · ${noNormItems.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp)
            )
            LegacySeriesCard(
                items = noNormItems,
                onClickItem = { item ->
                    if (item.id != null) onOpenEditor(item.id)
                    else onOpenLegacy(item.name)
                }
            )
            Text(
                text = "Откройте серию и задайте нормы — они будут подставляться " +
                    "автоматически в шторке локомотива.",
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
                text = "  Добавить серию",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

/** Строка без норм: существующая запись (id != null) или «старое» имя (id == null). */
data class NoNormItem(val name: String, val id: String?)

@Composable
private fun LegacySeriesCard(
    items: List<NoNormItem>,
    onClickItem: (NoNormItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = Shapes.medium)
            .background(color = MaterialTheme.colorScheme.secondary, shape = Shapes.medium)
    ) {
        items.forEachIndexed { idx, item ->
            LegacySeriesRow(name = item.name, onClick = { onClickItem(item) })
            if (idx < items.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}

@Composable
private fun LegacySeriesRow(
    name: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFont),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Норма приёмки и сдачи не задана",
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
}

@Composable
private fun SeriesGroupCard(
    items: List<LocomotiveSeries>,
    onOpenEditor: (String?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = Shapes.medium)
            .background(color = MaterialTheme.colorScheme.secondary, shape = Shapes.medium)
    ) {
        items.forEachIndexed { idx, s ->
            com.z_company.route.ui.SeriesRow(
                series = s,
                onClick = { onOpenEditor(s.seriesId) },
                showChevron = true
            )
            if (idx < items.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}
