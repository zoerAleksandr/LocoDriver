package com.z_company.route.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.setting.UserSettings

/**
 * Подраздел настроек «Маршрут» — управление отображением элементов
 * формы маршрута (по аналогии с подразделом «Локомотив»).
 */
@Composable
fun SettingsRouteFormContent(
    currentSettings: UserSettings,
    changeShowBreak: (Boolean) -> Unit,
    changeShowOnePersonSwitch: (Boolean) -> Unit,
) {
    val styleData = MaterialTheme.typography.bodyLarge
    val styleHint = MaterialTheme.typography.bodyMedium

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
            text = "ЭЛЕМЕНТЫ ФОРМЫ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 1.dp, shape = Shapes.medium)
                .background(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = Shapes.medium
                )
                .padding(vertical = 4.dp)
        ) {
            RouteFormSwitchRow(
                text = "Показывать перерыв",
                checked = currentSettings.isShowBreak,
                onCheckedChange = changeShowBreak,
                style = styleData
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            RouteFormSwitchRow(
                text = "Переключатель «Одно лицо»",
                checked = currentSettings.isShowOnePersonSwitch,
                onCheckedChange = changeShowOnePersonSwitch,
                style = styleData
            )
        }
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            text = "Управляет отображением элементов в форме маршрута.",
            style = styleHint,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RouteFormSwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    style: TextStyle,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .padding(end = 12.dp)
                .weight(1f),
            text = text,
            style = style,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
