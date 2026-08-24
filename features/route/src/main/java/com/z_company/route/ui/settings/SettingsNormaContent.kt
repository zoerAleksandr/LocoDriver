package com.z_company.route.ui.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.component.CustomDivider
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.MonthFullText.getMonthFullText
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.WorkScheduleMode
import com.z_company.domain.entities.WorkScheduleProfile
import com.z_company.domain.entities.setting.CrossMonthTimezone
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.route.component.AnimationDialog
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.viewmodel.CountryLoadingState
import com.z_company.route.viewmodel.RegionLoadingState
import com.z_company.route.viewmodel.TimeZoneRussia
import kotlinx.datetime.DayOfWeek

/**
 * Спиннер с гарантированной анимацией — на чистом Canvas + infiniteRepeatable.
 * Material3 [androidx.compose.material3.CircularProgressIndicator] в Dialog'ах
 * иногда "замирает" из-за разделения композиций между окнами; этот вариант
 * управляет вращением напрямую через Compose-аниматор и работает везде.
 */
@Composable
private fun SpinnerIndicator(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.tertiary,
    strokeWidth: androidx.compose.ui.unit.Dp = 4.dp,
) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinner-angle"
    )
    Canvas(modifier = modifier.rotate(angle)) {
        val sw = strokeWidth.toPx()
        val inset = sw / 2f
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = Size(size.width - sw, size.height - sw),
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNormaContent(
    currentSettings: UserSettings,
    showAbsenceScreen: () -> Unit,
    timeZoneRussiaList: List<TimeZoneRussia>,
    setTimeZone: (Long) -> Unit,
    setCountry: (String) -> Unit,
    countryLoadingState: CountryLoadingState? = null,
    onDismissCountryDialog: () -> Unit = {},
    setCrossMonthTimezone: (CrossMonthTimezone) -> Unit = {},
    regionsForCountry: List<com.z_company.domain.entities.calendar.Region> = emptyList(),
    isRegionsLoading: Boolean = false,
    setRegion: (String?) -> Unit = {},
    regionLoadingState: RegionLoadingState? = null,
    onDismissRegionDialog: () -> Unit = {},
    /**
     * Норма часов за текущий выбранный месяц, рассчитанная через NormaUseCase.
     * Если null — отображаем значение из selectMonthOfYear как fallback.
     */
    normaHours: Int? = null,
    workScheduleProfile: WorkScheduleProfile = WorkScheduleProfile.standard(),
    setWorkScheduleProfile: (WorkScheduleProfile) -> Unit = {},
) {
    val styleData = MaterialTheme.typography.bodyLarge
    val styleHint = MaterialTheme.typography.bodyMedium
    val styleTitle = MaterialTheme.typography.titleSmall
    val primaryColor = MaterialTheme.colorScheme.primary

    countryLoadingState?.let { state ->
        AnimationDialog(
            showDialog = true,
            onDismissRequest = {
                if (state !is CountryLoadingState.Loading) onDismissCountryDialog()
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = Shapes.medium
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (state) {
                    is CountryLoadingState.Loading -> {
                        SpinnerIndicator(
                            modifier = Modifier.size(40.dp),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Загружаем производственный календарь для ${state.countryName}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                    CountryLoadingState.Success -> {
                        Text(
                            text = "Календарь успешно загружен",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            shape = Shapes.medium,
                            onClick = onDismissCountryDialog) {
                            Text(
                                text = "ОК",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    CountryLoadingState.Error -> {
                        Text(
                            text = "Ошибка загрузки",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = onDismissCountryDialog) {
                            Text(
                                text = "OK",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    CountryLoadingState.NoInternet -> {
                        Text(
                            text = "Нет интернета",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = onDismissCountryDialog) {
                            Text(
                                text = "OK",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }
    }

    // Диалог загрузки региональных праздников — аналог countryLoadingState.
    // Показывается при changeRegion: пока идёт запрос на сервер за списком
    // праздников выбранного субъекта РФ (и пересчитывается норма).
    regionLoadingState?.let { state ->
        AnimationDialog(
            showDialog = true,
            onDismissRequest = {
                if (state !is RegionLoadingState.Loading) onDismissRegionDialog()
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = Shapes.medium
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (state) {
                    is RegionLoadingState.Loading -> {
                        SpinnerIndicator(
                            modifier = Modifier.size(40.dp),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Загружаем региональные праздники: ${state.regionName}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                    RegionLoadingState.Success -> {
                        Text(
                            text = "Региональные праздники применены, норма пересчитана",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            shape = Shapes.medium,
                            onClick = onDismissRegionDialog
                        ) {
                            Text(
                                text = "ОК",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    RegionLoadingState.Error -> {
                        Text(
                            text = "Ошибка сервера",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Не удалось загрузить региональные праздники.\n" +
                                "Регион не изменён — попробуйте ещё раз позже.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            shape = Shapes.medium,
                            onClick = onDismissRegionDialog
                        ) {
                            Text(
                                text = "ОК",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    RegionLoadingState.NoInternet -> {
                        Text(
                            text = "Нет соединения с интернетом",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Проверьте Wi-Fi или мобильную сеть.\n" +
                                "Регион не изменён — для загрузки праздников нужен интернет.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            shape = Shapes.medium,
                            onClick = onDismissRegionDialog
                        ) {
                            Text(
                                text = "ОК",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 28.dp),
    ) {
        var activeSheet by remember { mutableStateOf<NormaSheet?>(null) }

        // ── Норма часов ──
        SettingsGroupHeader("НОРМА ЧАСОВ", top = 8.dp, startPad = 4.dp)
        SettingsCard {
            val currentMonth = getMonthFullText(currentSettings.selectMonthOfYear.month)
            // normaHours из NormaUseCase (актуальный, с региональным календарём);
            // fallback — getPersonalNormaHours() до загрузки данных.
            val effectiveNorma = normaHours
                ?: currentSettings.selectMonthOfYear.getPersonalNormaHours()
            val personalNormaText = ConverterLongToTime.getTimeInStringFormat(
                effectiveNorma.toLong().times(3_600_000)
            )
            SettingsFieldRow(
                label = "$currentMonth ${currentSettings.selectMonthOfYear.year}",
                value = personalNormaText,
                mono = true,
            )
            SettingsCardSep()
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAbsenceScreen() }
                    .padding(horizontal = 18.dp, vertical = 13.dp),
                text = "Изменить норму",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        SettingsSectionNote("Месячная норма рабочего времени. По ней считается переработка и недоработка.")

        // ── Индивидуальная рабочая неделя ──
        SettingsGroupHeader("РАБОЧАЯ НЕДЕЛЯ", top = 20.dp, startPad = 4.dp)
        SettingsCard {
            listOf(
                WorkScheduleMode.STANDARD to "Стандартная · 8 ч, Пн–Пт",
                WorkScheduleMode.SIX_DAY_7_5 to "Шестидневная · 7 ч + Сб 5 ч",
                WorkScheduleMode.CUSTOM to "Своя настройка",
            ).forEachIndexed { index, (mode, label) ->
                if (index > 0) SettingsCardSep()
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val profile = when (mode) {
                                WorkScheduleMode.STANDARD -> WorkScheduleProfile.standard()
                                WorkScheduleMode.SIX_DAY_7_5 -> WorkScheduleProfile.sixDaySevenFive()
                                WorkScheduleMode.CUSTOM -> workScheduleProfile.copy(mode = WorkScheduleMode.CUSTOM)
                            }
                            setWorkScheduleProfile(profile)
                        }
                        .padding(horizontal = 18.dp, vertical = 13.dp),
                    text = if (workScheduleProfile.mode == mode) "✓  $label" else label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (workScheduleProfile.mode == mode) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (workScheduleProfile.mode == mode) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }

            if (workScheduleProfile.mode == WorkScheduleMode.CUSTOM) {
                DayOfWeek.entries.forEach { day ->
                    SettingsCardSep()
                    WorkDayHoursRow(
                        day = day,
                        hours = workScheduleProfile.hoursFor(day),
                        onChange = { hours ->
                            setWorkScheduleProfile(workScheduleProfile.withHours(day, hours))
                        },
                    )
                }
            }
        }
        SettingsSectionNote(
            "Используется для нормы месяца и часов отпуска, больничного и других отвлечений. Праздники остаются нерабочими."
        )

        // ── Страна ──
        SettingsGroupHeader("СТРАНА", top = 20.dp, startPad = 4.dp)
        val countryEmoji = when (currentSettings.country) {
            "KZ" -> "🇰🇿"
            "BY" -> "🇧🇾"
            else -> "🇷🇺"
        }
        val countryName = when (currentSettings.country) {
            "KZ" -> "Казахстан"
            "BY" -> "Беларусь"
            else -> "Россия"
        }
        SettingsSelectTile(
            value = countryName,
            leadingEmoji = countryEmoji,
            onClick = { activeSheet = NormaSheet.COUNTRY },
        )

        // ── Регион (только РФ) ──
        if (currentSettings.country == "RU" && (regionsForCountry.isNotEmpty() || isRegionsLoading)) {
            SettingsGroupHeader("РЕГИОН", top = 20.dp, startPad = 4.dp)
            val selectedRegionName = currentSettings.region?.let { code ->
                regionsForCountry.firstOrNull { it.code == code }?.displayName
            } ?: "Стандартный календарь"
            if (regionsForCountry.isEmpty() && isRegionsLoading) {
                SettingsSelectTile(value = "Загрузка регионов…", onClick = {})
            } else {
                SettingsSelectTile(
                    value = selectedRegionName,
                    onClick = { activeSheet = NormaSheet.REGION },
                )
            }
            SettingsSectionNote("Региональные праздники добавятся к стандартному календарю.")
        }

        // ── Домашний часовой пояс ──
        SettingsGroupHeader("ДОМАШНИЙ ЧАСОВОЙ ПОЯС", top = 20.dp, startPad = 4.dp)
        when (currentSettings.country) {
            "KZ" -> SettingsSelectTile(value = "UTC+5 (Kazakhstan Time, KZT)", onClick = {})
            "BY" -> SettingsSelectTile(value = "UTC+3 (Минск)", onClick = {})
            else -> {
                val currentTimeZone = timeZoneRussiaList.find {
                    it.offsetOfMoscow == currentSettings.timeZone
                } ?: timeZoneRussiaList.getOrNull(1)
                SettingsSelectTile(
                    value = currentTimeZone?.description ?: "",
                    onClick = { activeSheet = NormaSheet.TIMEZONE },
                )
            }
        }
        SettingsSectionNote("Установите местный часовой пояс. Учитывается при расчёте ночных, праздничных часов и переходных поездках.")

        // ── Переходные маршруты (РФ, немосковский пояс) ──
        if (currentSettings.country == "RU" && currentSettings.timeZone != 0L) {
            SettingsGroupHeader("ПЕРЕХОДНЫЕ МАРШРУТЫ", top = 20.dp, startPad = 4.dp)
            val transitionLabel =
                if (currentSettings.crossMonthTimezone == CrossMonthTimezone.MOSCOW)
                    "По московскому времени" else "По местному времени"
            SettingsSelectTile(
                value = transitionLabel,
                onClick = { activeSheet = NormaSheet.TRANSITION },
            )
            SettingsSectionNote("Определяет, по какому времени переходной маршрут относится к месяцу.")
        }

        // ── Пикеры ──
        when (activeSheet) {
            NormaSheet.COUNTRY -> SettingsPickerSheet(
                title = "Страна",
                onDismiss = { activeSheet = null },
                options = listOf(
                    Triple("RU", "Россия", "🇷🇺"),
                    Triple("KZ", "Казахстан", "🇰🇿"),
                    Triple("BY", "Беларусь", "🇧🇾"),
                ).map { (code, name, flag) ->
                    SettingsPickerOption(
                        label = name,
                        leadingEmoji = flag,
                        selected = currentSettings.country == code,
                        onSelect = { setCountry(code) },
                    )
                },
            )
            NormaSheet.REGION -> SettingsPickerSheet(
                title = "Регион",
                hint = "Региональные праздники добавятся к стандартному календарю.",
                onDismiss = { activeSheet = null },
                options = buildList {
                    add(
                        SettingsPickerOption(
                            label = "Стандартный календарь",
                            selected = currentSettings.region == null,
                            onSelect = { setRegion(null) },
                        )
                    )
                    regionsForCountry.forEach { region ->
                        add(
                            SettingsPickerOption(
                                label = region.displayName,
                                selected = currentSettings.region == region.code,
                                onSelect = { setRegion(region.code) },
                            )
                        )
                    }
                },
            )
            NormaSheet.TIMEZONE -> SettingsPickerSheet(
                title = "Часовой пояс",
                onDismiss = { activeSheet = null },
                options = timeZoneRussiaList.map { item ->
                    SettingsPickerOption(
                        label = item.description,
                        selected = item.offsetOfMoscow == currentSettings.timeZone,
                        onSelect = { setTimeZone(item.offsetOfMoscow) },
                    )
                },
            )
            NormaSheet.TRANSITION -> SettingsPickerSheet(
                title = "Переходные маршруты",
                onDismiss = { activeSheet = null },
                options = listOf(
                    CrossMonthTimezone.LOCAL to "По местному времени",
                    CrossMonthTimezone.MOSCOW to "По московскому времени",
                ).map { (value, label) ->
                    SettingsPickerOption(
                        label = label,
                        selected = value == currentSettings.crossMonthTimezone,
                        onSelect = { setCrossMonthTimezone(value) },
                    )
                },
            )
            null -> {}
        }
    }
}

@Composable
private fun WorkDayHoursRow(
    day: DayOfWeek,
    hours: Int,
    onChange: (Int) -> Unit,
) {
    val label = when (day) {
        DayOfWeek.MONDAY -> "Понедельник"
        DayOfWeek.TUESDAY -> "Вторник"
        DayOfWeek.WEDNESDAY -> "Среда"
        DayOfWeek.THURSDAY -> "Четверг"
        DayOfWeek.FRIDAY -> "Пятница"
        DayOfWeek.SATURDAY -> "Суббота"
        DayOfWeek.SUNDAY -> "Воскресенье"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier
                    .clickable(enabled = hours > 0) { onChange(hours - 1) }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                text = "−",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                modifier = Modifier.width(44.dp),
                text = "$hours ч",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = MonoFont,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                modifier = Modifier
                    .clickable(enabled = hours < 24) { onChange(hours + 1) }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                text = "+",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

private enum class NormaSheet { COUNTRY, REGION, TIMEZONE, TRANSITION }
