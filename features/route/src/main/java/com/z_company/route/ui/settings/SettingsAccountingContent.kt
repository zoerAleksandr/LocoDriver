package com.z_company.route.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import com.z_company.core.ui.component.TimePickerApp
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.ConverterLongToTime
import com.z_company.domain.entities.setting.UserSettings

@Composable
fun SettingsAccountingContent(
    currentSettings: UserSettings,
    changeStartNightTime: (Int, Int) -> Unit,
    changeEndNightTime: (Int, Int) -> Unit,
    changeConsiderFutureRoute: (Boolean) -> Unit,
) {
    val styleData = MaterialTheme.typography.bodyLarge
    val styleHint = MaterialTheme.typography.bodyMedium
    val primaryColor = MaterialTheme.colorScheme.primary


    var showNightTimeStartDialog by remember { mutableStateOf(false) }
    var showNightTimeEndDialog by remember { mutableStateOf(false) }

    if (showNightTimeStartDialog) {
        val initHour = currentSettings.nightTime.startNightHour
        val initMinute = currentSettings.nightTime.startNightMinute
        val initMillis = (initHour.times(3600000) + initMinute.times(60000)).toLong()
        TimePickerApp(
            initialTimeMillis = initMillis,
            onTimeSelected = { millis ->
                val hour = ConverterLongToTime.getHour(millis)
                val minute = ConverterLongToTime.getRemainingMinuteFromHour(millis)
                changeStartNightTime(hour, minute)
                showNightTimeStartDialog = false
                showNightTimeEndDialog = true
            },
            onDismiss = { showNightTimeStartDialog = false },
            title = "Начало ночи"
        )
    }

    if (showNightTimeEndDialog) {
        val initHour = currentSettings.nightTime.endNightHour
        val initMinute = currentSettings.nightTime.endNightMinute
        val initMillis = (initHour.times(3600000) + initMinute.times(60000)).toLong()
        TimePickerApp(
            initialTimeMillis = initMillis,
            onTimeSelected = { millis ->
                val hour = ConverterLongToTime.getHour(millis)
                val minute = ConverterLongToTime.getRemainingMinuteFromHour(millis)
                changeEndNightTime(hour, minute)
            },
            onDismiss = { showNightTimeEndDialog = false },
            title = "Окончание ночи"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Ночь
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .shadow(elevation = 2.dp, shape = Shapes.medium)
                    .background(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = Shapes.medium
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .noRippleEffect { showNightTimeStartDialog = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ночь",
                        style = styleData,
                        color = primaryColor,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = currentSettings.nightTime.toString(),
                        style = styleData,
                        color = primaryColor
                    )
                }
            }

            Text(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                text = "Установите время ночных часов для расчета ночных.",
                style = styleHint,
                color = primaryColor
            )
        }

        // Будущие маршруты
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .shadow(elevation = 2.dp, shape = Shapes.medium)
                    .background(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = Shapes.medium
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .weight(1f),
                    text = "Учитывать будущие маршруты",
                    style = styleData,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2
                )
                Switch(
                    checked = currentSettings.isConsiderFutureRoute,
                    onCheckedChange = { changeConsiderFutureRoute(it) }
                )
            }

            Text(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                text = "Маршруты, время явки которых не наступило, будут учитываться при подсчете отработаного времени.",
                style = styleHint,
                color = primaryColor
            )
        }
    }
}
