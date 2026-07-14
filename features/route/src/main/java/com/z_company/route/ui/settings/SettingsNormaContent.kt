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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.component.CustomDivider
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.MonthFullText.getMonthFullText
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.setting.CrossMonthTimezone
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.route.component.AnimationDialog
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.viewmodel.CountryLoadingState
import com.z_company.route.viewmodel.RegionLoadingState
import com.z_company.route.viewmodel.TimeZoneRussia

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
            .padding(bottom = 24.dp),
    ) {
        // Норма часов
        SettingsFormCard {
            val currentMonth = getMonthFullText(currentSettings.selectMonthOfYear.month)
            // Используем normaHours из NormaUseCase (всегда актуальный, с учётом
            // регионального календаря). Fallback — getPersonalNormaHours() на случай
            // первого запуска до загрузки данных.
            val effectiveNorma = normaHours
                ?: currentSettings.selectMonthOfYear.getPersonalNormaHours()
            val personalNormaText =
                ConverterLongToTime.getTimeInStringFormat(
                    effectiveNorma.toLong().times(3_600_000)
                )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Норма на $currentMonth",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = styleHint
                )
                Text(
                    text = personalNormaText,
                    style = styleData.copy(fontFamily = MonoFont),
                    color = primaryColor,
                )
            }
            SettingsFormSep()
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAbsenceScreen() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                text = "Изменить норму",
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Страна производственного календаря
        SettingsFormCard {
            data class CountryOption(val code: String, val name: String, val flag: String)
            val countries = listOf(
                CountryOption("RU", "Россия", "🇷🇺"),
                CountryOption("KZ", "Казахстан", "🇰🇿"),
                CountryOption("BY", "Беларусь", "🇧🇾"),
            )
            val selected = countries.find { it.code == currentSettings.country } ?: countries[0]

            SettingsFormFieldSlot(label = "Страна") {
                SettingsFilledDropdown(
                    value = "${selected.flag} ${selected.name}",
                    options = countries.map { option ->
                        SettingsDropdownOption(
                            text = "${option.flag} ${option.name}",
                            isSelected = currentSettings.country == option.code,
                            onSelect = { setCountry(option.code) },
                        )
                    }
                )
            }
        }

        // Регион (только для России — дополнительные праздничные дни
        // субъектов РФ накладываются поверх стандартного календаря).
        // Показываем секцию даже пока грузится список регионов — иначе
        // пользователь не понимает что данные ещё догружаются и думает
        // что регион не настраивается.
        if (currentSettings.country == "RU" && (regionsForCountry.isNotEmpty() || isRegionsLoading)) {
            SettingsFormCard {
                SettingsFormFieldSlot(
                    label = "Регион",
                    hint = "Региональные праздники добавятся к стандартному календарю"
                ) {
                    if (regionsForCountry.isEmpty() && isRegionsLoading) {
                        // Лоадер с подписью — занимает место dropdown, пока грузится список.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SpinnerIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = primaryColor.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Загрузка списка регионов…",
                                style = styleHint,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    } else {
                        val selectedName = currentSettings.region?.let { code ->
                            regionsForCountry.firstOrNull { it.code == code }?.displayName
                        } ?: "Стандартный календарь"

                        SettingsFilledDropdown(
                            value = selectedName,
                            // Первый пункт «Стандартный календарь» (region = null) — сброс региона.
                            options = buildList {
                                add(
                                    SettingsDropdownOption(
                                        text = "Стандартный календарь",
                                        isSelected = currentSettings.region == null,
                                        onSelect = { setRegion(null) },
                                    )
                                )
                                regionsForCountry.forEach { region ->
                                    add(
                                        SettingsDropdownOption(
                                            text = region.displayName,
                                            isSelected = currentSettings.region == region.code,
                                            onSelect = { setRegion(region.code) },
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // Домашний часовой пояс
        SettingsFormCard {
            SettingsFormFieldSlot(
                label = "Домашний часовой пояс",
                hint = "Установите местный часовой пояс. Будет учитываться при расчете ночных, праздничных часов и переходных поездках."
            ) {
                when (currentSettings.country) {
                    "KZ" -> SettingsFilledReadonlyField(value = "UTC+5 (Kazakhstan Time, KZT)")
                    "BY" -> SettingsFilledReadonlyField(value = "UTC+3 (Минск)")
                    else -> {
                        val currentTimeZone: TimeZoneRussia =
                            timeZoneRussiaList.find {
                                it.offsetOfMoscow == currentSettings.timeZone
                            } ?: timeZoneRussiaList[1]

                        var selectedTimeZone by remember(currentSettings.timeZone) {
                            mutableStateOf(currentTimeZone)
                        }

                        SettingsFilledDropdown(
                            value = selectedTimeZone.description,
                            options = timeZoneRussiaList.map { item ->
                                SettingsDropdownOption(
                                    text = item.description,
                                    isSelected = item.offsetOfMoscow == currentSettings.timeZone,
                                    onSelect = {
                                        selectedTimeZone = item
                                        setTimeZone(item.offsetOfMoscow)
                                    },
                                )
                            }
                        )
                    }
                }
            }
        }

        // Переходные маршруты — только для России с часовым поясом, отличным от московского
        if (currentSettings.country == "RU" && currentSettings.timeZone != 0L) {
            SettingsFormCard {
                SettingsFormFieldSlot(
                    label = "Переходные маршруты",
                    hint = "Определяет по какому времени считается к какому месяцу относится переходной маршрут."
                ) {
                    val options = listOf(
                        CrossMonthTimezone.LOCAL to "По местному времени",
                        CrossMonthTimezone.MOSCOW to "По московскому времени"
                    )
                    val current = options.find { it.first == currentSettings.crossMonthTimezone } ?: options[0]
                    SettingsFilledDropdown(
                        value = current.second,
                        options = options.map { (value, label) ->
                            SettingsDropdownOption(
                                text = label,
                                isSelected = value == currentSettings.crossMonthTimezone,
                                onSelect = { setCrossMonthTimezone(value) },
                            )
                        }
                    )
                }
            }
        }
    }
}
