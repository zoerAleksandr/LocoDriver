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
    changeDefaultLocoType: () -> Unit,
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
        // --- Показывать поля ---
        SettingsGroupHeader("ПОКАЗАТЕЛИ")
        SettingsCard {
            SettingSwitchRow(
                text = "Отопление",
                checked = currentSettings.isShowLocoHeating,
                onCheckedChange = changeShowLocoHeating,
                style = styleData
            )
            SettingsRowDivider()
            SettingSwitchRow(
                text = "Собственные нужды",
                checked = currentSettings.isShowLocoAuxiliary,
                onCheckedChange = changeShowLocoAuxiliary,
                style = styleData
            )
        }
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            text = "Показывать эти поля для ввода показаний",
            style = styleHint,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- Смена рода тока ---
        SettingsGroupHeader("РОД ТОКА")
        SettingsCard {
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- Вид тяги ---
        SettingsGroupHeader("ПО УМОЛЧАНИЮ")
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .noRippleEffect { changeDefaultLocoType() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
        }
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            text = "Будет выбран при добавлении локомотива",
            style = styleHint,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
