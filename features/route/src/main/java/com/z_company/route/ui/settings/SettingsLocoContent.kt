package com.z_company.route.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.setting.UserSettings

@Composable
fun SettingsLocoContent(
    currentSettings: UserSettings,
    changeShowLocoHeating: (Boolean) -> Unit,
    changeShowLocoAuxiliary: (Boolean) -> Unit,
    changeShowLocoStatistics: (Boolean) -> Unit,
    changeShowLocoNorma: (Boolean) -> Unit,
    changeShowOtherCurrent: (Boolean) -> Unit,
    setDefaultLocoType: (LocoType) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 28.dp)
    ) {
        var showTractionSheet by remember { mutableStateOf(false) }

        // ── Поля ввода показаний ──
        SettingsGroupHeader("ПОЛЯ ВВОДА ПОКАЗАНИЙ", top = 8.dp, startPad = 4.dp)
        SettingsCard {
            SettingsSwitchRow(
                label = "Отопление",
                checked = currentSettings.isShowLocoHeating,
                onCheckedChange = changeShowLocoHeating,
            )
            SettingsCardSep()
            SettingsSwitchRow(
                label = "Собственные нужды",
                checked = currentSettings.isShowLocoAuxiliary,
                onCheckedChange = changeShowLocoAuxiliary,
            )
            SettingsCardSep()
            SettingsSwitchRow(
                label = "Статистика",
                checked = currentSettings.isShowLocoStatistics,
                onCheckedChange = changeShowLocoStatistics,
            )
            SettingsCardSep()
            SettingsSwitchRow(
                label = "Норма",
                checked = currentSettings.isShowLocoNorma,
                onCheckedChange = changeShowLocoNorma,
            )
        }
        SettingsSectionNote("Показывать эти поля в форме локомотива для ввода показаний.")

        // ── Род тока ──
        SettingsGroupHeader("РОД ТОКА", top = 20.dp, startPad = 4.dp)
        SettingsCard {
            SettingsSwitchRow(
                label = "Смена рода тока",
                checked = currentSettings.isShowOtherCurrent,
                onCheckedChange = changeShowOtherCurrent,
            )
        }
        SettingsSectionNote("Для переменно-постоянных электровозов.")

        // ── По умолчанию ──
        SettingsGroupHeader("ПО УМОЛЧАНИЮ", top = 20.dp, startPad = 4.dp)
        SettingsCard {
            SettingsSelectRow(
                label = "Вид тяги",
                value = currentSettings.defaultLocoType.text,
                onClick = { showTractionSheet = true },
            )
        }
        SettingsSectionNote("Будет выбран при добавлении локомотива.")

        if (showTractionSheet) {
            SettingsPickerSheet(
                title = "Вид тяги",
                onDismiss = { showTractionSheet = false },
                options = LocoType.entries.map { type ->
                    SettingsPickerOption(
                        label = type.text,
                        selected = currentSettings.defaultLocoType == type,
                        onSelect = { setDefaultLocoType(type) },
                    )
                },
            )
        }
    }
}
