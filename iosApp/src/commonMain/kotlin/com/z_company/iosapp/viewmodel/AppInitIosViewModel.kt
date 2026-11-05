@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ProductionCalendarDay
import com.z_company.domain.entities.ReleaseDay
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.LoadCalendarFromStorage
import com.z_company.domain.use_cases.ProductionCalendarUseCase
import com.z_company.domain.use_cases.ReleaseDayUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.remote_rest.SettingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
// NSLocale-свойства (currentLocale, countryCode) — extension'ы ObjC-категорий
// в пакете platform.Foundation: нужен звёздочный импорт.
import platform.Foundation.*
import kotlin.time.Clock

/**
 * iOS-аналог init-блока Android `MainViewModel` (SCREEN_SPECS §20), сквозная задача С1.
 *
 * Пока календарь не загружен в `CalendarUseCase`, `NormaUseCase` считает
 * «Норма на месяц» от пустого календаря (= 0 ч). Здесь выполняется цепочка
 * старта: первая установка (страна/пояс), одноразовые миграции тегов,
 * загрузка производственного календаря с сервера и склейка календаря
 * (LoadCalendarFromStorage → CalendarUseCase.saveCalendar).
 *
 * Ошибки не фатальны: логируются через println и не рвут запуск.
 */
class AppInitIosViewModel(
    private val sharedPrefs: SharedPreferencesRepositories,
    private val settingsUseCase: SettingsUseCase,
    private val routeUseCase: RouteUseCase,
    private val calendarUseCase: CalendarUseCase,
    private val loadCalendarFromStorage: LoadCalendarFromStorage,
    private val salarySettingUseCase: SalarySettingUseCase,
    private val productionCalendarUseCase: ProductionCalendarUseCase,
    private val releaseDayUseCase: ReleaseDayUseCase,
    private val settingManager: SettingManager,
) : ViewModel() {

    // Флаг первого запуска — считываем ОДИН РАЗ до любых async операций,
    // как в Android MainViewModel.
    private val isFirstEntry = sharedPrefs.tokenIsFirstAppEntry()

    private val _appInitialized = MutableStateFlow(false)

    private var saveCalendarInLocalJob: Job? = null
    private var setDefaultSetting: Job? = null

    init {
        if (isFirstEntry) {
            sharedPrefs.setIsMigrated(true)
            sharedPrefs.setTimezoneMigrationDone() // новый пользователь — мигрировать нечего
            sharedPrefs.setReleaseDayMigrationDone() // новый пользователь — мигрировать нечего
            sharedPrefs.setProductionCalendarMigrationDone() // теги ещё не накоплены
            applyInitialCountryAndTimezone()
            // Сразу сбрасываем флаг, чтобы даже при завершении процесса
            // он не остался true и не сбросил настройки при следующем запуске
            sharedPrefs.setTokenIsFirstAppEntry(false)
        }
        viewModelScope.launch {
            runTimezoneMigration()
            runReleaseDayMigration()
            runProductionCalendarMigration()
            fetchProductionCalendarIfNeeded()
            loadCalendar()
            _appInitialized.value = true
        }
    }

    fun watchAppInitialized(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            _appInitialized.collect { callback(it) }
        }
    }

    fun dispose() {
        viewModelScope.cancel()
    }

    private suspend fun runTimezoneMigration() {
        if (sharedPrefs.isTimezoneMigrationDone()) return
        try {
            val offsetFromMoscow = settingsUseCase.getUserSetting().timeZone
            routeUseCase.migrateTimestamps(offsetFromMoscow)
            sharedPrefs.setTimezoneMigrationDone()
        } catch (e: Exception) {
            println("AppInitIosViewModel.runTimezoneMigration: ${e.message}")
        }
    }

    /**
     * Одноразовая миграция: переносит дни отвлечений из MonthOfYear.days (isReleaseDay=true)
     * в отдельную таблицу ReleaseDay. Порт Android MainViewModel.runReleaseDayMigration.
     */
    private suspend fun runReleaseDayMigration() {
        if (sharedPrefs.isReleaseDayMigrationDone()) return
        try {
            // Собираем старые дни отвлечений из MonthOfYear.days (isReleaseDay=true)
            val months = calendarUseCase.loadMonthOfYearList()
            val oldReleaseDays = months.flatMap { month ->
                month.days
                    .filter { day -> day.isReleaseDay && day.releaseType != null }
                    .map { day ->
                        ReleaseDay(
                            year = month.year,
                            month = month.month,
                            dayOfMonth = day.dayOfMonth,
                            releaseType = day.releaseType!!
                        )
                    }
            }
            // Если в MonthOfYear нет дней с isReleaseDay — мигрировать нечего
            if (oldReleaseDays.isEmpty()) {
                sharedPrefs.setReleaseDayMigrationDone()
                return
            }
            // Сравниваем по year/month/dayOfMonth — добавляем только отсутствующие.
            // Нельзя использовать replaceAll: это удалит дни, добавленные после обновления.
            val existingKeys = releaseDayUseCase.getAll()
                .map { Triple(it.year, it.month, it.dayOfMonth) }
                .toSet()
            val daysToAdd = oldReleaseDays.filter { day ->
                Triple(day.year, day.month, day.dayOfMonth) !in existingKeys
            }
            if (daysToAdd.isNotEmpty()) {
                releaseDayUseCase.saveAll(daysToAdd).collect {}
            }
            sharedPrefs.setReleaseDayMigrationDone()
        } catch (e: Exception) {
            println("AppInitIosViewModel.runReleaseDayMigration: ${e.message}")
        }
    }

    /**
     * Автоопределение страны и часового пояса при первой установке.
     * Порт Android MainViewModel.applyInitialCountryAndTimezone:
     * страна по локали устройства, смещение от Москвы по системному поясу.
     */
    private fun applyInitialCountryAndTimezone() {
        try {
            // Определяем страну по локали устройства (аналог Locale.getDefault().country)
            val deviceCountry = NSLocale.currentLocale.countryCode ?: ""
            val supportedCountries = listOf("RU", "KZ", "BY")
            val country = if (deviceCountry in supportedCountries) deviceCountry else "RU"

            // Определяем смещение от Москвы по системному часовому поясу
            val moscowOffsetMs = 3 * 3_600_000L // UTC+3 в миллисекундах
            val now = Clock.System.now()
            val deviceOffsetMs =
                TimeZone.currentSystemDefault().offsetAt(now).totalSeconds * 1000L
            val offsetFromMoscow = deviceOffsetMs - moscowOffsetMs
            // Округляем до целых часов (усечение к нулю, как Long-деление в Android)
            val oneHour = 3_600_000L
            val rawOffset = (offsetFromMoscow / oneHour) * oneHour
            // Ограничиваем допустимым диапазоном для поддерживаемых стран:
            // RU: от Калининграда (MSK-1) до Камчатки (MSK+9) → [-1h, +9h] от Москвы
            // KZ/BY: фиксированное значение, не зависит от устройства
            // Вне диапазона (VPN, неверный авто-пояс) → Москва (0L)
            val roundedOffset = when (country) {
                "KZ" -> 7_200_000L // UTC+5 = MSK+2, всегда
                "BY" -> 0L // UTC+3 = MSK, всегда
                else -> if (rawOffset in (-1 * oneHour)..(9 * oneHour)) rawOffset else 0L
            }

            viewModelScope.launch {
                try {
                    val current = settingsUseCase.getUserSetting()
                    settingsUseCase.saveSetting(
                        current.copy(
                            country = country,
                            timeZone = roundedOffset
                        )
                    ).collect {}
                } catch (e: Exception) {
                    println("AppInitIosViewModel.applyInitialCountryAndTimezone: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("AppInitIosViewModel.applyInitialCountryAndTimezone: ${e.message}")
        }
    }

    /**
     * Одноразовая миграция: копирует теги дней из MonthOfYear.days → ProductionCalendarDay.
     * Порт Android MainViewModel.runProductionCalendarMigration.
     */
    private suspend fun runProductionCalendarMigration() {
        if (sharedPrefs.isProductionCalendarMigrationDone()) return
        try {
            val months = calendarUseCase.loadMonthOfYearList()
            val productionDays = months.flatMap { month ->
                month.days.map { day ->
                    ProductionCalendarDay(
                        country = "RU",
                        year = month.year,
                        month = month.month,
                        dayOfMonth = day.dayOfMonth,
                        tag = day.tag
                    )
                }
            }
            if (productionDays.isNotEmpty()) {
                productionCalendarUseCase.saveCalendar(productionDays).collect {}
            }
            sharedPrefs.setProductionCalendarMigrationDone()
        } catch (e: Exception) {
            println("AppInitIosViewModel.runProductionCalendarMigration: ${e.message}")
        }
    }

    /**
     * Загружает производственный календарь с сервера если он ещё не загружен.
     * С 1 декабря также пробует загрузить календарь на следующий год.
     * После загрузки стандартного календаря — восстанавливает региональные праздники,
     * если выбран регион. Порт Android MainViewModel.fetchProductionCalendarIfNeeded.
     */
    private suspend fun fetchProductionCalendarIfNeeded() {
        try {
            val settings = settingsUseCase.getUserSetting()
            val country = settings.country.ifBlank { "RU" }
            val region = settings.region
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val currentYear = now.year
            val currentMonth = now.monthNumber
            var calendarUpdated = false

            // Загружаем текущий год если нужно
            if (productionCalendarUseCase.needsFetch(country, currentYear)) {
                fetchAndApplyCalendar(country, currentYear)
                calendarUpdated = true
            }

            // С 1 декабря — пробуем следующий год (не ошибка если нет на сервере)
            productionCalendarUseCase.nextYearToFetchIfDecember(currentMonth, currentYear)?.let { nextYear ->
                if (productionCalendarUseCase.needsFetch(country, nextYear)) {
                    fetchAndApplyCalendar(country, nextYear)
                    calendarUpdated = true
                }
            }

            // После загрузки стандартного календаря — восстанавливаем региональные праздники.
            // applyProductionCalendar сбрасывает теги дней к стандартным, поэтому если
            // выбран регион — нужно снова применить regional HOLIDAY-метки.
            if (calendarUpdated && region != null) {
                try {
                    calendarUseCase.applyRegionalHolidays(region, currentYear).collect {}
                    productionCalendarUseCase.nextYearToFetchIfDecember(currentMonth, currentYear)?.let { nextYear ->
                        calendarUseCase.applyRegionalHolidays(region, nextYear).collect {}
                    }
                } catch (e: Exception) {
                    println("AppInitIosViewModel.fetchProductionCalendarIfNeeded_regional: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("AppInitIosViewModel.fetchProductionCalendarIfNeeded: ${e.message}")
        }
    }

    /**
     * Загружает и применяет производственный календарь для страны и года.
     * Порт Android MainViewModel.fetchAndApplyCalendar.
     */
    private suspend fun fetchAndApplyCalendar(country: String, year: Int) {
        try {
            settingManager.getProductionCalendarFromRemote(country, year).collect { state ->
                if (state is ResultState.Success) {
                    val days = state.data
                    productionCalendarUseCase.saveCalendar(days).collect {}
                    calendarUseCase.applyProductionCalendar(days).collect {}
                }
            }
        } catch (e: Exception) {
            // Не критично — сервер может не иметь данных за этот год
            println("AppInitIosViewModel.fetchAndApplyCalendar: ${e.message}")
        }
    }

    /**
     * Склейка календаря: жёстко заданный базовый календарь (LoadCalendarFromStorage)
     * накладывается поверх локального, сохраняя дни отвлечений и тариф.
     * Порт Android MainViewModel.loadCalendar.
     */
    private fun loadCalendar() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val monthOfYearList = mutableListOf<MonthOfYear>()
                this.launch {
                    calendarUseCase.loadFlowMonthOfYearListState().collect { monthListResult ->
                        monthListResult.forEach { monthOfYear ->
                            monthOfYearList.add(monthOfYear)
                        }
                        this.cancel()
                    }
                }.join()

                val lastMonthWithTariffRate = monthOfYearList.findLast { it.tariffRate != 0.0 }

                val lastTariffRate = lastMonthWithTariffRate?.let { month ->
                    salarySettingUseCase.getTariffRateFromCurrentMonthOfYear(month)
                } ?: 0.0

                this.launch {
                    loadCalendarFromStorage.getMonthOfYearList()
                        .collect { resultState ->
                            if (resultState is ResultState.Success) {
                                val newMonthOfYearList = mutableListOf<MonthOfYear>()
                                resultState.data.forEach { monthOfYear ->
                                    var month =
                                        monthOfYearList.find { it.month == monthOfYear.month && it.year == monthOfYear.year }
                                    val newDays = mutableListOf<Day>()
                                    if (month != null) {
                                        month.days.forEachIndexed { index, day ->
                                            if (!day.isReleaseDay) {
                                                newDays.add(monthOfYear.days[index])
                                            } else {
                                                newDays.add(
                                                    monthOfYear.days[index].copy(
                                                        isReleaseDay = true,
                                                        releaseType = day.releaseType
                                                    )
                                                )
                                            }
                                        }
                                        month = month.copy(days = newDays)
                                        if (month.tariffRate == 0.0) {
                                            month = month.copy(tariffRate = lastTariffRate)
                                        }
                                        newMonthOfYearList.add(month)
                                    } else {
                                        newMonthOfYearList.add(monthOfYear.copy(tariffRate = lastTariffRate))
                                    }
                                }
                                saveCalendarInLocal(newMonthOfYearList)
                                this.cancel()
                            }
                        }
                }
            } catch (e: Exception) {
                println("AppInitIosViewModel.loadCalendar: ${e.message}")
            }
        }
    }

    /**
     * Порт Android MainViewModel.saveCalendarInLocal: очистка + сохранение
     * склеенного календаря, восстановление региональных праздников,
     * обновление месяца в настройках и дефолтных настроек при первом запуске.
     */
    private fun saveCalendarInLocal(calendar: List<MonthOfYear>) {
        saveCalendarInLocalJob?.cancel()
        saveCalendarInLocalJob = viewModelScope.launch {
            try {
                this.launch {
                    calendarUseCase.clearCalendar().collect { clearResult ->
                        if (clearResult is ResultState.Success) {
                            this.cancel()
                        }
                    }
                }.join()

                calendarUseCase.saveCalendar(calendar).collect { resultState ->
                    if (resultState is ResultState.Success) {
                        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        val currentYear = now.year
                        val currentMonth = now.monthNumber - 1 // 0-based, как MonthOfYear
                        val searchMonthOfYear = calendar.find {
                            it.month == currentMonth && it.year == currentYear
                        }

                        // Календарь хранит только стандартные теги дней.
                        // После saveCalendar региональные HOLIDAY-метки утрачены — восстанавливаем их.
                        val region = try {
                            settingsUseCase.getUserSetting().region
                        } catch (e: Exception) {
                            null
                        }
                        val monthForSettings = if (region != null) {
                            try {
                                calendarUseCase.applyRegionalHolidays(region, currentYear).collect {}
                                // После applyRegionalHolidays все дубли MonthOfYear обновлены —
                                // выбираем тот у которого больше дней (та же логика что в SettingsViewModel).
                                calendarUseCase.loadMonthOfYearList()
                                    .filter {
                                        it.year == currentYear && it.month == currentMonth
                                    }
                                    .maxByOrNull { it.days.size }
                            } catch (e: Exception) {
                                println("AppInitIosViewModel.saveCalendarInLocal_regional: ${e.message}")
                                searchMonthOfYear
                            }
                        } else {
                            searchMonthOfYear
                        }

                        settingsUseCase.updateMonthOfYearInUserSetting(
                            monthForSettings ?: searchMonthOfYear ?: calendar.first()
                        ).collect {}
                        if (isFirstEntry) {
                            setDefaultSettings(searchMonthOfYear ?: calendar.first())
                        }
                    }
                }
            } catch (e: Exception) {
                println("AppInitIosViewModel.saveCalendarInLocal: ${e.message}")
            }
        }
    }

    private fun setDefaultSettings(currentMonthOfYear: MonthOfYear) {
        setDefaultSetting?.cancel()
        setDefaultSetting =
            settingsUseCase.setDefaultSettings(currentMonthOfYear).launchIn(viewModelScope)
    }
}
