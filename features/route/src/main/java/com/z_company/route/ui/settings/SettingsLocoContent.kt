package com.z_company.route.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.setting.UserSettings

@Composable
fun SettingsLocoContent(
    currentSettings: UserSettings,
    changeShowLocoHeating: (Boolean) -> Unit,
    changeShowLocoAuxiliary: (Boolean) -> Unit,
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

        // ── Вид тяги по умолчанию (inline radio, как в «Основных») ──
        SettingsGroupHeader("ВИД ТЯГИ", top = 20.dp, startPad = 4.dp)
        SettingsCard {
            LocoType.entries.forEachIndexed { index, type ->
                if (index > 0) SettingsCardSep()
                SettingsRadioRow(
                    label = type.text,
                    selected = currentSettings.defaultLocoType == type,
                    onClick = { setDefaultLocoType(type) },
                )
            }
        }
        SettingsSectionNote("Будет выбран при добавлении локомотива.")
    }
}
