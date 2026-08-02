package com.z_company.route.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.domain.entities.norma_time.LocomotiveSeries

/** Shared row used in both SeriesPickerSheet and SettingsSeriesListContent. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SeriesRow(
    series: LocomotiveSeries,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    showChevron: Boolean = true,
    // Если задан — вместо шапки-стрелки показываем «карандаш» для перехода в
    // редактирование (тап по строке при этом продолжает выбирать серию).
    onEdit: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 16.dp, end = if (onEdit != null) 6.dp else 16.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = series.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFont),
                color = MaterialTheme.colorScheme.primary
            )
            // Показываем «после отстоя» как основной вариант, иначе «из рук в руки».
            val acc = series.acceptanceDurationMin ?: series.acceptanceHandToHandMin
            val del = series.deliveryDurationMin ?: series.deliveryHandToHandMin
            val normText = when {
                acc != null && del != null -> "Приёмка $acc мин · Сдача $del мин"
                acc != null -> "Приёмка $acc мин"
                del != null -> "Сдача $del мин"
                else -> "Нормы не заданы"
            }
            Text(
                text = normText,
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
        } else if (showChevron) {
            Icon(
                painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_right_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
