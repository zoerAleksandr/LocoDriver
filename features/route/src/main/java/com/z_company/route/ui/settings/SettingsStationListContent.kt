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
    onOpenEditor: (String?) -> Unit,
) {
    val stations by viewModel.stationsFlow.collectAsState()
    val withNorms = stations.filter {
        it.appearanceToStartMin != null && it.endToBarrierMin != null &&
            it.barrierToStartMin != null && it.endToWorkEndMin != null
    }
    val withoutNorms = stations.filter {
        it.appearanceToStartMin == null || it.endToBarrierMin == null ||
            it.barrierToStartMin == null || it.endToWorkEndMin == null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (stations.isEmpty()) {
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
                text = "ЧАСТО ИСПОЛЬЗУЕМЫЕ · ${withNorms.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
            )
            StationGroupCard(items = withNorms, showNorms = true, onOpenEditor = onOpenEditor)
        }

        if (withoutNorms.isNotEmpty()) {
            Text(
                text = "БЕЗ НОРМ · ${withoutNorms.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
            )
            StationGroupCard(items = withoutNorms, showNorms = false, onOpenEditor = onOpenEditor)
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
                    if (showNorms && s.appearanceToStartMin != null && s.endToBarrierMin != null &&
                        s.barrierToStartMin != null && s.endToWorkEndMin != null
                    ) {
                        Text(
                            text = "Приём +${s.appearanceToStartMin}/+${s.endToBarrierMin} · Сдача +${s.barrierToStartMin}/+${s.endToWorkEndMin}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    } else if (!showNorms) {
                        Text(
                            text = "Нормы не заданы",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
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
