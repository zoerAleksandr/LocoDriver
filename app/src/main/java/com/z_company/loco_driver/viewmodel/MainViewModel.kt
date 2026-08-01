@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.loco_driver.viewmodel

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robokassa.library.params.PaymentParams
import com.z_company.core.ResultState
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.reidentifyForImport
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.util.SharedRouteHolder
import com.z_company.repository.remote_rest.ShareRouteManager
import com.z_company.domain.use_cases.LoadCalendarFromStorage
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.ProductionCalendarUseCase
import com.z_company.domain.use_cases.ReleaseDayUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.entities.ProductionCalendarDay
import com.z_company.repository.remote_rest.SettingManager
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import com.z_company.route.viewmodel.PurchasesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import com.z_company.core.sendToSentry
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar
import java.util.Calendar.MONTH
import java.util.Calendar.YEAR

private const val TAG = "MainViewModel_TAG"

class MainViewModel : ViewModel(), KoinComponent, DefaultLifecycleObserver {
    private val purchasesViewModel: PurchasesViewModel by inject()
    private val salarySettingUseCase: SalarySettingUseCase by inject()
    private val loadCalendarFromStorage: LoadCalendarFromStorage by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val releaseDayUseCase: ReleaseDayUseCase by inject()
    private val productionCalendarUseCase: ProductionCalendarUseCase by inject()
    private val settingManager: SettingManager by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val sharedPreferenceStorage: SharedPreferencesRepositories by inject()
    private val routeUseCase: RouteUseCase by inject()
    private val snackbarManager: ISnackbarManager by inject()
    private val shareRouteManager: ShareRouteManager by inject()

    private var saveCalendarInLocalJob: Job? = null
    private var setDefaultSetting: Job? = null

    // Флаг первого запуска — считываем ОДИН РАЗ синхронно до любых async операций
    val isFirstEntry = sharedPreferenceStorage.tokenIsFirstAppEntry()

    val showFirstPresentation = isFirstEntry
    val showUpdatePresentation = sharedPreferenceStorage.isShowUpdatePresentation() && !isFirstEntry

    private val _appInitialized = MutableStateFlow(false)
    val appInitialized: StateFlow<Boolean> = _appInitialized.asStateFlow()

    // Импорт маршрута из файла (.zroute) — ожидает подтверждения пользователя
    private val _pendingImportRoute = MutableStateFlow<Route?>(null)
    val pendingImportRoute: StateFlow<Route?> = _pendingImportRoute.asStateFlow()

    // Навигация из виджета → FormScreen (добавить маршрут)
    private val _pendingFormOpen = MutableStateFlow(false)
    val pendingFormOpen: StateFlow<Boolean> = _pendingFormOpen.asStateFlow()

    fun requestOpenForm() { _pendingFormOpen.value = true }
    fun clearOpenForm() { _pendingFormOpen.value = false }

    // Открыть FormRoute с конкретным routeId (shared route через SharedRouteHolder)
    private val _pendingOpenFormWithId = MutableStateFlow<String?>(null)
    val pendingOpenFormWithId: StateFlow<String?> = _pendingOpenFormWithId.asStateFlow()

    fun clearOpenFormWithId() { _pendingOpenFormWithId.value = null }

    // Навигация из виджета → HomeScreen (по тапу на тело виджета)
    private val _pendingNavigateHome = MutableStateFlow(false)
    val pendingNavigateHome: StateFlow<Boolean> = _pendingNavigateHome.asStateFlow()

    fun requestNavigateHome() { _pendingNavigateHome.value = true }
    fun clearNavigateHome() { _pendingNavigateHome.value = false }

    // Deep link locodriver://profile → экран Профиль
    private val _pendingNavigateProfile = MutableStateFlow(false)
    val pendingNavigateProfile: StateFlow<Boolean> = _pendingNavigateProfile.asStateFlow()

    fun requestNavigateProfile() { _pendingNavigateProfile.value = true }
    fun clearNavigateProfile() { _pendingNavigateProfile.value = false }

    fun setPendingImportRoute(route: Route) {
        _pendingImportRoute.value = route
    }

    fun confirmImportRoute() {
        val route = _pendingImportRoute.value ?: return
        _pendingImportRoute.value = null
        viewModelScope.launch {
            try {
                routeUseCase.saveRoute(route).collect { result ->
                    when (result) {
                        is ResultState.Success -> snackbarManager.show("Маршрут импортирован")
                        is ResultState.Error -> snackbarManager.show("Ошибка импорта маршрута")
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                e.sendToSentry("MainViewModel", "confirmImportRoute")
            }
        }
    }

    fun dismissImportRoute() {
        _pendingImportRoute.value = null
    }

    /**
     * Обрабатывает deep-link `locodriver://share/{shareId}`:
     * 1. Загружает Route с сервера
     * 2. Переприсваивает идентификаторы
     * 3. Сохраняет Route в локальную БД с флагом [isDeleted = true] — «tentative»
     *    (не отображается в списках, но доступен по id, child-сущности работают)
     * 4. Помечает id в [SharedRouteHolder] для FormViewModel (покажет шторку)
     * 5. Навигирует на FormScreen через [pendingOpenFormWithId]
     */
    fun handleShareDeepLink(shareId: String) {
        if (shareId.isBlank()) return
        viewModelScope.launch {
            try {
                shareRouteManager.getSharedRoute(shareId).collect { result ->
                    when (result) {
                        is ResultState.Success -> {
                            // Переприсваиваем id + ставим isDeleted = true (tentative)
                            val imported = result.data.reidentifyForImport()
                            val tentative = imported.copy(
                                basicData = imported.basicData.copy(isDeleted = true)
                            )
                            // Сохраняем в БД (tentative)
                            routeUseCase.saveRoute(tentative).collect { saveState ->
                                if (saveState is ResultState.Success) {
                                    SharedRouteHolder.markSharedPreview(tentative.basicData.id)
                                    _pendingOpenFormWithId.value = tentative.basicData.id
                                } else if (saveState is ResultState.Error) {
                                    snackbarManager.show(
                                        saveState.entity.message
                                            ?: "Ошибка импорта маршрута"
                                    )
                                }
                            }
                        }
                        is ResultState.Error -> {
                            snackbarManager.show(
                                result.entity.message ?: "Не удалось загрузить общий маршрут"
                            )
                        }
                        is ResultState.Loading -> Unit
                    }
                }
            } catch (e: Exception) {
                e.sendToSentry("MainViewModel", "handleShareDeepLink")
                snackbarManager.show("Ошибка загрузки общего маршрута")
            }
        }
    }


    init {
        if (isFirstEntry) {
            sharedPreferenceStorage.setIsMigrated(true)
            sharedPreferenceStorage.setTimezoneMigrationDone() // новый пользователь — мигрировать нечего
            sharedPreferenceStorage.setReleaseDayMigrationDone() // новый пользователь — мигрировать нечего
            sharedPreferenceStorage.setProductionCalendarMigrationDone() // теги ещё не накоплены
            applyInitialCountryAndTimezone()
            // Сразу сбрасываем флаг синхронно (commit), чтобы даже при завершении процесса
            // на Android 16 он не остался true и не сбросил настройки при следующем запуске
            sharedPreferenceStorage.setTokenIsFirstAppEntry(false)
        }
        viewModelScope.launch {
            runTimezoneMigration()
            runReleaseDayMigration()
            runProductionCalendarMigration()
            fetchProductionCalendarIfNeeded()
            loadCalendar()
            delay(400L)
            _appInitialized.value = true
        }
    }

    private suspend fun runTimezoneMigration() {
        if (sharedPreferenceStorage.isTimezoneMigrationDone()) return
        try {
            val offsetFromMoscow = settingsUseCase.getUserSetting().timeZone
            routeUseCase.migrateTimestamps(offsetFromMoscow)
            sharedPreferenceStorage.setTimezoneMigrationDone()
        } catch (e: Exception) {
            e.sendToSentry("MainViewModel", "runTimezoneMigration")
        }
    }

    /**
     * Одноразовая миграция: переносит дни отвлечений из MonthOfYear.days (isReleaseDay=true)
     * в отдельную таблицу ReleaseDay. Запускается один раз при обновлении.
     */
    private suspend fun runReleaseDayMigration() {
        if (sharedPreferenceStorage.isReleaseDayMigrationDone()) return
        try {
            // Собираем старые дни отвлечений из MonthOfYear.days (isReleaseDay=true)
            val months = calendarUseCase.loadMonthOfYearList()
            val oldReleaseDays = months.flatMap { month ->
                month.days
                    .filter { day -> day.isReleaseDay && day.releaseType != null }
                    .map { day ->
                        com.z_company.domain.entities.ReleaseDay(
                            year = month.year,
                            month = month.month,
                            dayOfMonth = day.dayOfMonth,
                            releaseType = day.releaseType!!
                        )
                    }
            }
            // Если в MonthOfYear нет дней с isReleaseDay — мигрировать нечего
            if (oldReleaseDays.isEmpty()) {
                sharedPreferenceStorage.setReleaseDayMigrationDone()
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
            sharedPreferenceStorage.setReleaseDayMigrationDone()
        } catch (e: Exception) {
            e.sendToSentry("MainViewModel", "runReleaseDayMigration")
        }
    }

    /**
     * Автоопределение страны и часового пояса при первой установке.
     * Вызывается синхронно в init до launch корутины.
     */
    private fun applyInitialCountryAndTimezone() {
        try {
            // Определяем страну по локали устройства
            val deviceCountry = java.util.Locale.getDefault().country
            val supportedCountries = listOf("RU", "KZ", "BY")
            val country = if (deviceCountry in supportedCountries) deviceCountry else "RU"

            // Определяем смещение от Москвы по системному часовому поясу
            val moscowOffsetMs = 3 * 3_600_000L   // UTC+3 в миллисекундах
            val deviceZone = java.util.TimeZone.getDefault()
            val now = System.currentTimeMillis()
            val deviceOffsetMs = deviceZone.getOffset(now).toLong()
            val offsetFromMoscow = deviceOffsetMs - moscowOffsetMs
            // Округляем до целых часов
            val oneHour = 3_600_000L
            val rawOffset = (offsetFromMoscow / oneHour) * oneHour
            // Ограничиваем допустимым диапазоном для поддерживаемых стран:
            // RU: от Калининграда (MSK-1) до Камчатки (MSK+9) → [-1h, +9h] от Москвы
            // KZ/BY: фиксированное значение, не зависит от устройства
            // Вне диапазона (VPN, неверный авто-пояс) → Москва (0L)
            val roundedOffset = when (country) {
                "KZ" -> 7_200_000L   // UTC+5 = MSK+2, всегда
                "BY" -> 0L           // UTC+3 = MSK, всегда
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
                    e.sendToSentry("MainViewModel", "applyInitialCountryAndTimezone")
                }
            }
        } catch (e: Exception) {
            e.sendToSentry("MainViewModel", "applyInitialCountryAndTimezone")
        }
    }

    /**
     * Одноразовая миграция: копирует теги дней из MonthOfYear.days → ProductionCalendarDay (country=RU).
     */
    private suspend fun runProductionCalendarMigration() {
        if (sharedPreferenceStorage.isProductionCalendarMigrationDone()) return
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
            sharedPreferenceStorage.setProductionCalendarMigrationDone()
        } catch (e: Exception) {
            e.sendToSentry("MainViewModel", "runProductionCalendarMigration")
        }
    }

    /**
     * Загружает производственный календарь с сервера если он ещё не загружен.
     * С 1 декабря также пробует загрузить календарь на следующий год.
     * После загрузки стандартного календаря — восстанавливает региональные праздники,
     * если выбран регион (applyProductionCalendar сбрасывает теги к стандартным).
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
                    e.sendToSentry("MainViewModel", "fetchProductionCalendarIfNeeded_regional")
                }
            }
        } catch (e: Exception) {
            e.sendToSentry("MainViewModel", "fetchProductionCalendarIfNeeded")
        }
    }

    /**
     * Загружает и применяет производственный календарь для страны и года.
     */
    internal suspend fun fetchAndApplyCalendar(country: String, year: Int) {
        try {
            settingManager.getProductionCalendarFromRemote(country, year).collect { state ->
                if (state is com.z_company.core.ResultState.Success) {
                    val days = state.data
                    productionCalendarUseCase.saveCalendar(days).collect {}
                    calendarUseCase.applyProductionCalendar(days).collect {}
                }
            }
        } catch (e: Exception) {
            // Не критично — сервер может не иметь данных за этот год
            e.sendToSentry("MainViewModel", "fetchAndApplyCalendar")
        }
    }

    fun handlePaymentReturn(params: PaymentParams?) {
        viewModelScope.launch {
            if (params != null) {
                purchasesViewModel.emitStartPayment(params, onlyCheck = true)
            } else {
                Log.d("zzz", "Параметры платежа не найдены при возврате")
            }
        }
    }

    private fun setDefaultSettings(currentMonthOfYear: MonthOfYear) {
        setDefaultSetting?.cancel()
        setDefaultSetting =
            settingsUseCase.setDefaultSettings(currentMonthOfYear).launchIn(viewModelScope)
    }

    private fun loadCalendar() {
        viewModelScope.launch(Dispatchers.IO) {
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

                Log.d("zzz", "lastMonthWithTariffRate $lastMonthWithTariffRate")

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
                e.sendToSentry("MainViewModel", "loadCalendar")
            }
        }
    }

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
                        Log.i(TAG, "production calendar is loading")
                        val currentCalendar = Calendar.getInstance()
                        val searchMonthOfYear = calendar.find {
                            it.month == currentCalendar.get(MONTH) && it.year == currentCalendar.get(YEAR)
                        }

                        // CalendarStorageLocalImpl хранит только стандартные теги дней.
                        // После saveCalendar региональные HOLIDAY-метки утрачены — восстанавливаем их.
                        val region = try { settingsUseCase.getUserSetting().region } catch (e: Exception) { null }
                        val monthForSettings = if (region != null) {
                            try {
                                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                                calendarUseCase.applyRegionalHolidays(region, now.year).collect {}
                                // После applyRegionalHolidays все дубли MonthOfYear обновлены —
                                // выбираем тот у которого больше дней (та же логика что в SettingsViewModel).
                                calendarUseCase.loadMonthOfYearList()
                                    .filter {
                                        it.year == currentCalendar.get(YEAR) &&
                                            it.month == currentCalendar.get(MONTH)
                                    }
                                    .maxByOrNull { it.days.size }
                            } catch (e: Exception) {
                                e.sendToSentry("MainViewModel", "saveCalendarInLocal_regional")
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
                e.sendToSentry("MainViewModel", "saveCalendarInLocal")
            }
        }
    }
}
