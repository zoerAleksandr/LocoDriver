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
    changeShowLocomotive: (Boolean) -> Unit,
    changeShowTrain: (Boolean) -> Unit,
    changeShowPassenger: (Boolean) -> Unit,
    changeShowOtherWork: (Boolean) -> Unit,
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

        SettingsGroupHeader("РАЗДЕЛЫ МАРШРУТА", top = 20.dp, startPad = 4.dp)
        SettingsCard {
            SettingsSwitchRow(
                label = "Показывать локомотив",
                sub = "Раздел «Локомотив» в форме маршрута",
                checked = currentSettings.isShowLocomotive,
                onCheckedChange = changeShowLocomotive,
            )
            SettingsCardSep()
            SettingsSwitchRow(
                label = "Показывать поездную работу",
                sub = "Раздел «Поезд» в форме маршрута",
                checked = currentSettings.isShowTrain,
                onCheckedChange = changeShowTrain,
            )
            SettingsCardSep()
            SettingsSwitchRow(
                label = "Показывать пассажиром",
                sub = "Раздел «Пассажиром» в форме маршрута",
                checked = currentSettings.isShowPassenger,
                onCheckedChange = changeShowPassenger,
            )
            SettingsCardSep()
            SettingsSwitchRow(
                label = "Показывать прочую работу",
                sub = "Маневровая, вывозная, при депо и др.",
                checked = currentSettings.isShowOtherWork,
                onCheckedChange = changeShowOtherWork,
            )
        }
        SettingsSectionNote("Управляет отображением разделов и элементов в форме маршрута.")
    }
}
