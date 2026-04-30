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
import com.z_company.domain.use_cases.ProductionCalendarUseCase
import com.z_company.domain.use_cases.ReleaseDayUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.iosapp.platform.detectInitialCountryAndOffset
import com.z_company.repository.remote_rest.SettingManager
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * KMP-аналог [com.z_company.loco_driver.viewmodel.MainViewModel] для iOS.
 *
 * Выполняет инициализацию при старте приложения:
 *  - при первом запуске фиксирует флаги миграций, страну и часовой пояс;
 *  - загружает производственный календарь с сервера (если нужно);
 *  - применяет теги дней к MonthOfYear;
 *  - выставляет [appInitialized] = true, когда всё готово.
 *
 * Swift UI показывает splash пока [appInitialized] == false.
 */
class AppInitIosViewModel(
    private val sharedPrefs: SharedPreferencesRepositories,
    private val settingsUseCase: SettingsUseCase,
    private val calendarUseCase: CalendarUseCase,
    private val releaseDayUseCase: ReleaseDayUseCase,
    private val productionCalendarUseCase: ProductionCalendarUseCase,
    private val settingManager: SettingManager,
) : ViewModel() {

    private val _appInitialized = MutableStateFlow(false)
    val appInitialized: StateFlow<Boolean> = _appInitialized.asStateFlow()

    val isFirstEntry: Boolean = sharedPrefs.tokenIsFirstAppEntry()

    init {
        if (isFirstEntry) {
            sharedPrefs.setIsMigrated(true)
            sharedPrefs.setTimezoneMigrationDone()
            sharedPrefs.setReleaseDayMigrationDone()
            sharedPrefs.setProductionCalendarMigrationDone()
            applyInitialCountryAndTimezone()
            sharedPrefs.setTokenIsFirstAppEntry(false)
        }
        viewModelScope.launch {
            runProductionCalendarMigration()
            fetchProductionCalendarIfNeeded()
            loadCalendar()
            _appInitialized.value = true
        }
    }

    /**
     * Определяет страну пользователя (RU/KZ/BY → иначе RU) и часовой пояс
     * по системной локали iOS, сохраняет в UserSettings.
     */
    private fun applyInitialCountryAndTimezone() {
        val detected = detectInitialCountryAndOffset()
        viewModelScope.launch {
            try {
                val current = settingsUseCase.getUserSetting()
                settingsUseCase.saveSetting(
                    current.copy(
                        country = detected.country,
                        timeZone = detected.offsetFromMoscowMs,
                    )
                ).first { it is ResultState.Success || it is ResultState.Error }
            } catch (_: Exception) {
                // не критично — при следующих запусках страна уже выставлена
            }
        }
    }

    /**
     * Единовременная миграция тегов дней MonthOfYear → ProductionCalendarDay.
     * На iOS при первой установке уже выставлено [isProductionCalendarMigrationDone] = true,
     * но обработка оставлена для полноты.
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
                        tag = day.tag,
                    )
                }
            }
            if (productionDays.isNotEmpty()) {
                productionCalendarUseCase.saveCalendar(productionDays).first {
                    it is ResultState.Success || it is ResultState.Error
                }
            }
            sharedPrefs.setProductionCalendarMigrationDone()
        } catch (_: Exception) {
        }
    }

    /**
     * Запрашивает производственный календарь с сервера, если его нет за текущий год.
     * С декабря также пробует загрузить следующий год.
     *
     * Даже если данные уже есть локально — повторно применяем их к MonthOfYear.
     * Это гарантирует, что теги дней (праздник/выходной/рабочий) не «пропадают»
     * между запусками, если таблица MonthOfYear по какой-то причине обновлялась.
     */
    private suspend fun fetchProductionCalendarIfNeeded() {
        try {
            val settings = settingsUseCase.getUserSetting()
            val country = settings.country.ifBlank { "RU" }
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val currentYear = now.year
            val currentMonth = now.monthNumber

            ensureYearApplied(country, currentYear)
            productionCalendarUseCase.nextYearToFetchIfDecember(currentMonth, currentYear)
                ?.let { nextYear -> ensureYearApplied(country, nextYear) }
        } catch (_: Exception) {
        }
    }

    /**
     * Для указанного [year]:
     *  - если данных нет → загружает с сервера и применяет к MonthOfYear;
     *  - если данные уже есть локально → просто применяет их к MonthOfYear
     *    (восстановление тегов после очистки/потери таблицы MonthOfYear).
     */
    private suspend fun ensureYearApplied(country: String, year: Int) {
        try {
            if (productionCalendarUseCase.needsFetch(country, year)) {
                fetchAndApplyCalendar(country, year)
                return
            }
            // Данные уже в локальной БД — повторно применяем к MonthOfYear.
            // В ProductionCalendarDay месяцы 0-based (как в MonthOfYear).
            // БД-запросы — только в фоне, чтобы не блокировать Main.
            val allDays = withContext(Dispatchers.Default) {
                (0..11).flatMap { month ->
                    productionCalendarUseCase.getByYearMonth(country, year, month)
                }
            }
            if (allDays.isEmpty()) return
            calendarUseCase.applyProductionCalendar(allDays).firstOrNull {
                it is ResultState.Success || it is ResultState.Error
            }
        } catch (_: Throwable) {
            // Не критично — пропустим повторное применение, теги подтянет следующий fetch.
        }
    }

    private suspend fun fetchAndApplyCalendar(country: String, year: Int) {
        try {
            settingManager.getProductionCalendarFromRemote(country, year).collect { state ->
                if (state is ResultState.Success) {
                    val days = state.data
                    productionCalendarUseCase.saveCalendar(days).first {
                        it is ResultState.Success || it is ResultState.Error
                    }
                    calendarUseCase.applyProductionCalendar(days).first {
                        it is ResultState.Success || it is ResultState.Error
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Мержит загруженный календарь с локальными ReleaseDay и сохраняет в БД.
     * Гарантирует, что [MonthOfYear] за текущий месяц выставлен в UserSettings.
     */
    private suspend fun loadCalendar() {
        try {
            val monthList = calendarUseCase.loadMonthOfYearList()
            if (monthList.isEmpty()) return

            val releaseDays: List<ReleaseDay> = try {
                releaseDayUseCase.getAll()
            } catch (_: Exception) {
                emptyList()
            }
            val releaseMap = releaseDays.groupBy { it.year to it.month }

            val merged = monthList.map { month ->
                val rDays = releaseMap[month.year to month.month].orEmpty()
                if (rDays.isEmpty()) month
                else {
                    val rByDay = rDays.associateBy { it.dayOfMonth }
                    val updatedDays: List<Day> = month.days.map { day ->
                        val rd = rByDay[day.dayOfMonth]
                        if (rd != null) day.copy(isReleaseDay = true, releaseType = rd.releaseType)
                        else day
                    }
                    month.copy(days = updatedDays)
                }
            }

            // Выставляем текущий MonthOfYear в UserSettings (если его там нет).
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val currentMoy = merged.firstOrNull {
                it.month == now.monthNumber - 1 && it.year == now.year
            } ?: merged.first()

            settingsUseCase.updateMonthOfYearInUserSetting(currentMoy).firstOrNull {
                it is ResultState.Success || it is ResultState.Error
            }
        } catch (_: Exception) {
        }
    }

    // watcher для Swift
    fun watchAppInitialized(callback: (Boolean) -> Unit): WatchHandle =
        WatchHandle(viewModelScope.launch { appInitialized.collect { callback(it) } })
}
