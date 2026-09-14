package com.z_company.route.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.component.CustomDivider
import com.z_company.route.component.AppTimePicker
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.ConverterLongToTime
import com.z_company.domain.entities.setting.UserSettings

@Composable
fun SettingsRestContent(
    currentSettings: UserSettings,
    restTimeChanged: (Long) -> Unit,
    secondRestTimeChanged: (Long) -> Unit,
    homeRestTimeChanged: (Long) -> Unit,
) {
    val styleData = MaterialTheme.typography.bodyLarge
    val styleValueMono = styleData.copy(fontFamily = MonoFont)
    val styleHint = MaterialTheme.typography.bodyMedium
    val primaryColor = MaterialTheme.colorScheme.primary

    var showRestDialog by remember { mutableStateOf(false) }
    var showSecondRestDialog by remember { mutableStateOf(false) }
    var showHomeRestDialog by remember { mutableStateOf(false) }

    if (showRestDialog) {
        AppTimePicker(
            initialTimeMillis = currentSettings.minTimeRestPointOfTurnover,
            onTimeSelected = { millis ->
                restTimeChanged(millis)
                showRestDialog = false
            },
            onDismiss = { showRestDialog = false },
            title = "Минимальный отдых в ПО",
            showTimeLabel = false,
            cancelButtonText = "Отмена",
            swipeToDismiss = false,
        )
    }

    if (showHomeRestDialog) {
        AppTimePicker(
            initialTimeMillis = currentSettings.minTimeHomeRest,
            onTimeSelected = { millis ->
                homeRestTimeChanged(millis)
                showHomeRestDialog = false
            },
            onDismiss = { showHomeRestDialog = false },
            title = "Минимальный домашний отдых",
            showTimeLabel = false,
            cancelButtonText = "Отмена",
            swipeToDismiss = false,
        )
    }

    if (showSecondRestDialog) {
        AppTimePicker(
            initialTimeMillis = currentSettings.minTimeRestPointOfTurnoverSecond,
            onTimeSelected = { millis ->
                secondRestTimeChanged(millis)
                showSecondRestDialog = false
            },
            onDismiss = { showSecondRestDialog = false },
            title = "Отдых в ПО (второй отдых)",
            showTimeLabel = false,
            cancelButtonText = "Отмена",
            swipeToDismiss = false,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 28.dp)
    ) {
        SettingsGroupHeader("МИНИМАЛЬНЫЙ ОТДЫХ", top = 8.dp, startPad = 4.dp)
        SettingsCard {
            SettingsFieldRow(
                label = "Отдых в ПО",
                value = ConverterLongToTime.getTimeInStringFormat(
                    currentSettings.minTimeRestPointOfTurnover
                ),
                mono = true,
                onClick = { showRestDialog = true },
            )
            SettingsCardSep()
            SettingsFieldRow(
                label = "Отдых в ПО (второй отдых)",
                value = ConverterLongToTime.getTimeInStringFormat(currentSettings.minTimeRestPointOfTurnoverSecond),
                mono = true,
                onClick = { showSecondRestDialog = true },
            )
            SettingsCardSep()
            SettingsFieldRow(
                label = "Домашний отдых",
                value = ConverterLongToTime.getTimeInStringFormat(
                    currentSettings.minTimeHomeRest
                ),
                mono = true,
                onClick = { showHomeRestDialog = true },
            )
        }
        SettingsSectionNote("Второй минимум применяется, когда два последовательных маршрута заканчиваются отдыхом в ПО.")
    }
}
