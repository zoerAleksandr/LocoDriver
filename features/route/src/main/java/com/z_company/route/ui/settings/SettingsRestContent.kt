package com.z_company.route.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
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
import com.z_company.core.ui.component.TimePickerApp
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.ConverterLongToTime
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.SharedPreferencesRepositories
import org.koin.compose.koinInject

@Composable
fun SettingsRestContent(
    currentSettings: UserSettings,
    restTimeChanged: (Long) -> Unit,
    homeRestTimeChanged: (Long) -> Unit,
) {
    val styleData = MaterialTheme.typography.bodyLarge
    val styleHint = MaterialTheme.typography.bodyMedium
    val primaryColor = MaterialTheme.colorScheme.primary

    val sharedPrefs: SharedPreferencesRepositories = koinInject()
    var showRestDialog by remember { mutableStateOf(false) }
    var showHomeRestDialog by remember { mutableStateOf(false) }

    if (showRestDialog) {
        TimePickerApp(
            initialTimeMillis = currentSettings.minTimeRestPointOfTurnover,
            onTimeSelected = { millis ->
                restTimeChanged(millis)
                showRestDialog = false
            },
            onDismiss = { showRestDialog = false },
            title = "Минимальный отдых в ПО",
            recentTimes = sharedPrefs.getRecentTimes("rest_point_of_turnover"),
            onRecentTimeSaved = { sharedPrefs.addRecentTime("rest_point_of_turnover", it) }
        )
    }

    if (showHomeRestDialog) {
        TimePickerApp(
            initialTimeMillis = currentSettings.minTimeHomeRest,
            onTimeSelected = { millis ->
                homeRestTimeChanged(millis)
                showHomeRestDialog = false
            },
            onDismiss = { showHomeRestDialog = false },
            title = "Минимальный домашний отдых",
            recentTimes = sharedPrefs.getRecentTimes("home_rest"),
            onRecentTimeSaved = { sharedPrefs.addRecentTime("home_rest", it) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
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
                    .noRippleEffect { showRestDialog = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Отдых в ПО",
                    style = styleData,
                    color = primaryColor,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = ConverterLongToTime.getTimeInStringFormat(
                        currentSettings.minTimeRestPointOfTurnover
                    ),
                    style = styleData,
                    color = primaryColor
                )
            }
            CustomDivider(orientation = Orientation.Horizontal)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .noRippleEffect { showHomeRestDialog = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Домашний отдых",
                    style = styleData,
                    color = primaryColor,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = ConverterLongToTime.getTimeInStringFormat(
                        currentSettings.minTimeHomeRest
                    ),
                    style = styleData,
                    color = primaryColor,
                )
            }
        }

        Text(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            text = "Установите время минимального отдыха. Это значение будет использовано при расчете отдыха после поездки.",
            style = styleHint,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
