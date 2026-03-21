package com.z_company.route.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.setting.UserSettings

@Composable
fun SettingsLocoContent(
    currentSettings: UserSettings,
    changeShowLocoHeating: (Boolean) -> Unit,
    changeShowLocoAuxiliary: (Boolean) -> Unit,
    changeShowLocoStatistics: (Boolean) -> Unit,
    changeDefaultLocoType: () -> Unit,
    changeShowLocoNorma: (Boolean) -> Unit,
    changeShowOtherCurrent: (Boolean) -> Unit,
) {
    val styleData = MaterialTheme.typography.bodyLarge
    val styleHint = MaterialTheme.typography.bodyMedium
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(bottom = 24.dp)
    ) {
        // --- Показывать поля (без заголовка) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = Shapes.medium)
                .background(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = Shapes.medium
                )
                .padding(vertical = 4.dp)
        )
        {
            SettingSwitchRow(
                text = "Отопление",
                checked = currentSettings.isShowLocoHeating,
                onCheckedChange = changeShowLocoHeating,
                style = styleData
            )
            HorizontalDivider()
            SettingSwitchRow(
                text = "Собственные нужды",
                checked = currentSettings.isShowLocoAuxiliary,
                onCheckedChange = changeShowLocoAuxiliary,
                style = styleData
            )
            HorizontalDivider()
            SettingSwitchRow(
                text = "Статистика",
                checked = currentSettings.isShowLocoStatistics,
                onCheckedChange = changeShowLocoStatistics,
                style = styleData
            )
            HorizontalDivider()
            SettingSwitchRow(
                text = "Норма",
                checked = currentSettings.isShowLocoNorma,
                onCheckedChange = changeShowLocoNorma,
                style = styleData
            )
        }
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            text = "Показывать эти поля для ввода показаний",
            style = styleHint,
            color = primaryColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- Смена рода тока ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = Shapes.medium)
                .background(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = Shapes.medium
                )
                .padding(vertical = 4.dp)
        ) {
            SettingSwitchRow(
                text = "Смена рода тока",
                checked = currentSettings.isShowOtherCurrent,
                onCheckedChange = changeShowOtherCurrent,
                style = styleData
            )
        }
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            text = "Для переменно-постоянных электровозов",
            style = styleHint,
            color = primaryColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- Вид тяги ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = Shapes.medium)
                .background(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = Shapes.medium
                )
                .noRippleEffect { changeDefaultLocoType() }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "Вид тяги",
                style = styleData,
                color = primaryColor
            )
            Text(
                text = currentSettings.defaultLocoType.text,
                style = styleData,
                color = primaryColor
            )
        }
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            text = "Будет выбран при добавлении локомотива",
            style = styleHint,
            color = primaryColor
        )
    }
}

@Composable
private fun SettingSwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    style: androidx.compose.ui.text.TextStyle,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = style,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
