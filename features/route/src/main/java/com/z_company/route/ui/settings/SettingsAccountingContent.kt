package com.z_company.route.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.route.component.AppTimePicker
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.ConverterLongToTime
import com.z_company.domain.entities.setting.NightTime
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.SharedPreferencesRepositories
import org.koin.compose.koinInject

@Composable
fun SettingsAccountingContent(
    currentSettings: UserSettings,
    changeStartNightTime: (Int, Int) -> Unit,
    changeEndNightTime: (Int, Int) -> Unit,
    changeConsiderFutureRoute: (Boolean) -> Unit,
) {
    var showNightSheet by remember { mutableStateOf(false) }

    if (showNightSheet) {
        NightRangeSheet(
            nightTime = currentSettings.nightTime,
            onSave = { sh, sm, eh, em ->
                changeStartNightTime(sh, sm)
                changeEndNightTime(eh, em)
                showNightSheet = false
            },
            onDismiss = { showNightSheet = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 28.dp),
    ) {
        SettingsGroupHeader("НОЧНЫЕ ЧАСЫ", top = 8.dp, startPad = 4.dp)
        SettingsCard {
            SettingsSelectRow(
                label = "Ночь",
                value = currentSettings.nightTime.toString(),
                mono = true,
                onClick = { showNightSheet = true },
            )
        }
        SettingsSectionNote("Интервал ночных часов для расчёта доплаты за работу ночью.")

        SettingsGroupHeader("БУДУЩИЕ МАРШРУТЫ", top = 20.dp, startPad = 4.dp)
        SettingsCard {
            SettingsSwitchRow(
                label = "Учитывать будущие маршруты",
                sub = "С ещё не наступившей явкой",
                checked = currentSettings.isConsiderFutureRoute,
                onCheckedChange = changeConsiderFutureRoute,
            )
        }
        SettingsSectionNote("Маршруты, время явки которых не наступило, будут учитываться при подсчёте отработанного времени.")
    }
}

/**
 * Шторка выбора интервала ночного времени: два mono-поля «Начало»/«Конец»,
 * каждое открывает [AppTimePicker] для правки; внизу — тёмная кнопка «Сохранить».
 * (Пресеты-чипы из дизайна намеренно не реализуем.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NightRangeSheet(
    nightTime: NightTime,
    onSave: (startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sharedPrefs: SharedPreferencesRepositories = koinInject()

    var startHour by remember { mutableIntStateOf(nightTime.startNightHour) }
    var startMinute by remember { mutableIntStateOf(nightTime.startNightMinute) }
    var endHour by remember { mutableIntStateOf(nightTime.endNightHour) }
    var endMinute by remember { mutableIntStateOf(nightTime.endNightMinute) }
    // null / "start" / "end" — какое поле сейчас правим во вложенном пикере
    var editing by remember { mutableStateOf<String?>(null) }

    editing?.let { field ->
        val isStart = field == "start"
        val h = if (isStart) startHour else endHour
        val m = if (isStart) startMinute else endMinute
        AppTimePicker(
            initialTimeMillis = (h * 3_600_000L + m * 60_000L),
            onTimeSelected = { millis ->
                val nh = ConverterLongToTime.getHour(millis)
                val nm = ConverterLongToTime.getRemainingMinuteFromHour(millis)
                if (isStart) {
                    startHour = nh; startMinute = nm
                } else {
                    endHour = nh; endMinute = nm
                }
                editing = null
            },
            onDismiss = { editing = null },
            title = if (isStart) "Начало ночи" else "Окончание ночи",
            recentTimes = sharedPrefs.getRecentTimes(if (isStart) "night_start" else "night_end"),
            onRecentTimeSaved = {
                sharedPrefs.addRecentTime(if (isStart) "night_start" else "night_end", it)
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.secondary,
        shape = Shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Ночные часы",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    modifier = Modifier
                        .clickable { onSave(startHour, startMinute, endHour, endMinute) }
                        .padding(4.dp),
                    text = "Готово",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Интервал, который считается ночным временем",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NightTimeColumn(
                    modifier = Modifier.weight(1f),
                    label = "Начало",
                    time = fmtHm(startHour, startMinute),
                    onClick = { editing = "start" },
                )
                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = "→",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
                NightTimeColumn(
                    modifier = Modifier.weight(1f),
                    label = "Конец",
                    time = fmtHm(endHour, endMinute),
                    onClick = { editing = "end" },
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                onClick = { onSave(startHour, startMinute, endHour, endMinute) },
                shape = Shapes.medium,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = "Сохранить",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun NightTimeColumn(
    label: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceBright)
                .clickable { onClick() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = MonoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun fmtHm(hour: Int, minute: Int): String {
    val h = if (hour < 10) "0$hour" else "$hour"
    val m = if (minute < 10) "0$minute" else "$minute"
    return "$h:$m"
}
