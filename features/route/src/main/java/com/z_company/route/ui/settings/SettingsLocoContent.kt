package com.z_company.route.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.setting.UserSettings

@Composable
fun SettingsLocoContent(
    currentSettings: UserSettings,
    changeShowLocoHeating: (Boolean) -> Unit,
    changeShowLocoAuxiliary: (Boolean) -> Unit,
    changeShowLocoStatistics: (Boolean) -> Unit,
) {
    val styleData = MaterialTheme.typography.bodyLarge
    val styleHint = MaterialTheme.typography.bodyMedium
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = Shapes.medium)
                .background(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = Shapes.medium
                )
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Отопление",
                    style = styleData,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                )
                Switch(
                    checked = currentSettings.isShowLocoHeating,
                    onCheckedChange = { changeShowLocoHeating(it) }
                )
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Собственные нужды",
                    style = styleData,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                )
                Switch(
                    checked = currentSettings.isShowLocoAuxiliary,
                    onCheckedChange = { changeShowLocoAuxiliary(it) }
                )
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Статистика",
                    style = styleData,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                )
                Switch(
                    checked = currentSettings.isShowLocoStatistics,
                    onCheckedChange = { changeShowLocoStatistics(it) }
                )
            }
        }

        Text(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            text = "Показывать эти поля в разделе Локомотив",
            style = styleHint,
            color = primaryColor
        )
    }
}
