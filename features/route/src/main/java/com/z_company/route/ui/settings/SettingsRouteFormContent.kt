package com.z_company.route.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z_company.domain.entities.setting.UserSettings

/**
 * Подраздел настроек «Маршрут» — управление отображением элементов
 * формы маршрута.
 */
@Composable
fun SettingsRouteFormContent(
    currentSettings: UserSettings,
    changeShowBreak: (Boolean) -> Unit,
    changeShowOnePersonSwitch: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 28.dp),
    ) {
        SettingsGroupHeader("ЭЛЕМЕНТЫ ФОРМЫ", top = 8.dp, startPad = 4.dp)
        SettingsCard {
            SettingsSwitchRow(
                label = "Показывать перерыв",
                sub = "Поля перерыва в форме маршрута",
                checked = currentSettings.isShowBreak,
                onCheckedChange = changeShowBreak,
            )
            SettingsCardSep()
            SettingsSwitchRow(
                label = "Переключатель «Одно лицо»",
                sub = "Отметка работы в одно лицо в маршруте",
                checked = currentSettings.isShowOnePersonSwitch,
                onCheckedChange = changeShowOnePersonSwitch,
            )
        }
        SettingsSectionNote("Управляет отображением элементов в форме маршрута.")
    }
}
