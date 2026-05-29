package com.z_company.route.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.z_company.domain.entities.norma_time.LocomotiveSeries

/** Shared row used in both SeriesPickerSheet and SettingsSeriesListContent. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SeriesRow(
    series: LocomotiveSeries,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    showChevron: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = series.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            val normText = when {
                series.acceptanceDurationMin != null && series.deliveryDurationMin != null ->
                    "Приёмка ${series.acceptanceDurationMin} мин · Сдача ${series.deliveryDurationMin} мин"
                series.acceptanceDurationMin != null ->
                    "Приёмка ${series.acceptanceDurationMin} мин"
                series.deliveryDurationMin != null ->
                    "Сдача ${series.deliveryDurationMin} мин"
                else -> "Нормы не заданы"
            }
            Text(
                text = normText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        }
        if (showChevron) {
            Icon(
                painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_right_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
