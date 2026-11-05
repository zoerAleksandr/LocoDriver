package com.z_company.iosapp.repository

import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.entities.WorkScheduleProfile
import com.z_company.domain.entities.norma_time.SectionNumberingType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.Foundation.NSUserDefaults

/**
 * iOS stub-реализация SharedPreferencesRepositories.
 *
 * На iOS нет SharedPreferences. Данные, которые Android хранит в SharedPreferences,
 * на iOS можно хранить в NSUserDefaults (платформенный аналог) или UserDefaults.
 * Для текущего использования (SyncManager) нужен только timestamp последней синхронизации
 * и подписка — остальное возвращает значения по умолчанию.
 *
 * TODO: заменить хранение на NSUserDefaults через expect/actual, если потребуется.
 */
class IosSharedPreferencesRepository : SharedPreferencesRepositories {
    // Локальные (не синхронизируемые) настройки устройства живут в NSUserDefaults (§17).
    private fun udBool(key: String, default: Boolean): Boolean {
        val d = NSUserDefaults.standardUserDefaults
        return if (d.objectForKey("ld.$key") == null) default else d.boolForKey("ld.$key")
    }
    private fun udSetBool(key: String, value: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(value, forKey = "ld.$key")
    }
    private fun udString(key: String): String? =
        NSUserDefaults.standardUserDefaults.stringForKey("ld.$key")
    private fun udSetString(key: String, value: String?) {
        val d = NSUserDefaults.standardUserDefaults
        if (value == null) d.removeObjectForKey("ld.$key") else d.setObject(value, forKey = "ld.$key")
    }
    private var lastScheduleMonth: String? = null
    override fun getLastScheduleMonth(): String? = lastScheduleMonth
    override fun setLastScheduleMonth(value: String) { lastScheduleMonth = value }
    private val workScheduleProfileState = MutableStateFlow(WorkScheduleProfile.standard())
    override fun getWorkScheduleProfile(): WorkScheduleProfile = workScheduleProfileState.value
    override fun getWorkScheduleProfileFlow(): StateFlow<WorkScheduleProfile> = workScheduleProfileState
    override fun setWorkScheduleProfile(profile: WorkScheduleProfile) {
        workScheduleProfileState.value = profile
    }
    // Хранится в памяти; при перезапуске приложения будет сброшено.
    // Для продакшена следует использовать NSUserDefaults.
    private var lastSyncTimestamp: Long = 0L
    private var subscriptionExpiration: Long = 0L
    // Сквозная задача С1: флаги первого запуска живут в NSUserDefaults (как в
    // Android SharedPreferences). Иначе каждый запуск iOS выглядит как первая
    // установка и applyInitialCountryAndTimezone затирает выбор пользователя.
    private var isMigrated: Boolean
        get() = udBool("isMigrated", false)
        set(v) = udSetBool("isMigrated", v)
    private var timezoneMigrationDone: Boolean = true // iOS не нуждается в миграции временных меток
    private var releaseDayMigrationDone: Boolean = true // iOS не нуждается в миграции отвлечений
    private var productionCalendarMigrationDone: Boolean = true // iOS — нет старых данных для миграции
    private var opKeyRobokassa: String? = null
    private var showUpdatePresentation: Boolean = true
    private var tokenIsChangesHave: Boolean = false
    // default true — только самая первая установка; после setTokenIsFirstAppEntry(false)
    // значение остаётся в NSUserDefaults и между запусками.
    private var tokenIsFirstAppEntry: Boolean
        get() = udBool("tokenIsFirstAppEntry", true)
        set(v) = udSetBool("tokenIsFirstAppEntry", v)
    private var tokenIsLoadStationAndLocomotiveSeries: Boolean = false
    private var stationsReversed: Boolean = false
    private var showSegments: Boolean = true
    private var inputDieselInKilo: Boolean = false
    private var sortOption: String? = null
    private var selectedFilters: Set<String>? = null
    private var expandedView: Boolean = false
    private var showTurnaroundRest: Boolean = true
    private var showTravelTime: Boolean = false
    private var locoSectionTimeExpanded: Boolean = false
    private var locoSectionHeatingExpanded: Boolean = false
    private var locoSectionAuxiliaryExpanded: Boolean = false
    private var locoSectionStatisticsExpanded: Boolean = false
    private var showLocoHeating: Boolean
        get() = udBool("showLocoHeating", false)
        set(v) = udSetBool("showLocoHeating", v)
    private var showLocoAuxiliary: Boolean
        get() = udBool("showLocoAuxiliary", false)
        set(v) = udSetBool("showLocoAuxiliary", v)
    private var showLocoStatistics: Boolean
        get() = udBool("showLocoStatistics", false)
        set(v) = udSetBool("showLocoStatistics", v)
    private var showLocoNorma: Boolean
        get() = udBool("showLocoNorma", false)
        set(v) = udSetBool("showLocoNorma", v)
    private var showOtherCurrent: Boolean
        get() = udBool("showOtherCurrent", false)
        set(v) = udSetBool("showOtherCurrent", v)
    private var locoSectionNormaExpanded: Boolean = false
    private var defaultLocoSectionNumberingType = SectionNumberingType.NUMERIC
    private var passenger12hDontAskAgain: Boolean
        get() = udBool("passenger12hDontAsk", false)
        set(v) = udSetBool("passenger12hDontAsk", v)
    private var passenger12hAutoAccepted: Boolean
        get() = udBool("passenger12hAutoAccepted", false)
        set(v) = udSetBool("passenger12hAutoAccepted", v)
    private var timePickerKeyboardInput: Boolean = false
    private val recentTimesMap = mutableMapOf<String, MutableList<Long>>()
    private val lastSeenAnnouncementNumbers = mutableMapOf<String, Int>()
    private var underworkInfoDismissed: Boolean = false
    private var themeMode: String?
        get() = NSUserDefaults.standardUserDefaults.stringForKey("ld.themeMode")
        set(v) = NSUserDefaults.standardUserDefaults.setObject(v, forKey = "ld.themeMode")
    private var currentRouteBlockOrder: List<String>? = null

    override fun setLastSyncTimestamp(time: Long) { lastSyncTimestamp = time }
    override fun getLastSyncTimestamp(): Long = lastSyncTimestamp

    private var routeSyncCursor: String? = null
    override fun getRouteSyncCursor(): String? = routeSyncCursor
    override fun setRouteSyncCursor(value: String?) { routeSyncCursor = value }

    private var settingsSyncPending: Boolean = true
    override fun getSettingsSyncPending(): Boolean = settingsSyncPending
    override fun setSettingsSyncPending(value: Boolean) { settingsSyncPending = value }

    override fun getOPKeyRobokassa(): String? = opKeyRobokassa
    override fun setOPKeyRobokassa(opKey: String?) { opKeyRobokassa = opKey }
    override fun isMigrated(): Boolean = isMigrated
    override fun setIsMigrated(value: Boolean) { isMigrated = value }
    override fun isTimezoneMigrationDone(): Boolean = timezoneMigrationDone
    override fun setTimezoneMigrationDone() { timezoneMigrationDone = true }
    override fun isReleaseDayMigrationDone(): Boolean = releaseDayMigrationDone
    override fun setReleaseDayMigrationDone() { releaseDayMigrationDone = true }
    override fun isProductionCalendarMigrationDone(): Boolean = productionCalendarMigrationDone
    override fun setProductionCalendarMigrationDone() { productionCalendarMigrationDone = true }
    override fun isShowUpdatePresentation(): Boolean = showUpdatePresentation
    override fun enableShowingUpdatePresentation() { showUpdatePresentation = true }
    override fun getSubscriptionExpiration(): Long = subscriptionExpiration
    override fun tokenIsChangesHave(): Boolean = tokenIsChangesHave
    override fun tokenIsFirstAppEntry(): Boolean = tokenIsFirstAppEntry
    override fun setSubscriptionExpiration(value: Long) { subscriptionExpiration = value }
    override fun setTokenIsChangeHave(value: Boolean) { tokenIsChangesHave = value }
    override fun setTokenIsFirstAppEntry(value: Boolean) { tokenIsFirstAppEntry = value }
    override fun tokenIsLoadStationAndLocomotiveSeries(): Boolean = tokenIsLoadStationAndLocomotiveSeries
    override fun setTokenIsLoadStationAndLocomotiveSeries(value: Boolean) { tokenIsLoadStationAndLocomotiveSeries = value }
    private var stationFragmentsCleaned: Boolean = false
    override fun isStationFragmentsCleaned(): Boolean = stationFragmentsCleaned
    override fun setStationFragmentsCleaned(value: Boolean) { stationFragmentsCleaned = value }
    override fun toggleStationsSortOrder(value: Boolean) { stationsReversed = value }
    override fun isReversedSortStationList(): Boolean = stationsReversed
    override fun setShowSegments(value: Boolean) { showSegments = value }
    override fun isShowSegments(): Boolean = showSegments
    override fun toggleInputDieselInKilo(value: Boolean) { inputDieselInKilo = value }
    override fun isInputDieselInKilo(): Boolean = inputDieselInKilo
    override fun getSortOption(): String? = sortOption
    override fun setSortOption(value: String) { sortOption = value }

    private var lastOtherWorkType: String? = null
    override fun getLastOtherWorkType(): String? = lastOtherWorkType
    override fun setLastOtherWorkType(value: String?) { lastOtherWorkType = value }
    override fun getLastSeenAnnouncementNumber(type: String): Int = lastSeenAnnouncementNumbers[type] ?: -1
    override fun setLastSeenAnnouncementNumber(type: String, value: Int) { lastSeenAnnouncementNumbers[type] = value }
    // §19.5–19.6: «увиденное» по аккаунтам и запись ожидаемой оплаты переживают
    // выгрузку процесса (как SharedPreferences на Android).
    override fun getLastSeenSubscriptionPeriod(userId: String): Long =
        udString("seenPeriod.$userId")?.toLongOrNull() ?: -1L
    override fun setLastSeenSubscriptionPeriod(userId: String, value: Long) {
        udSetString("seenPeriod.$userId", value.toString())
    }
    override fun getSubscriptionPeriodReferralDays(userId: String): Int =
        udString("seenRefDays.$userId")?.toIntOrNull() ?: -1
    override fun setSubscriptionPeriodReferralDays(userId: String, value: Int) {
        udSetString("seenRefDays.$userId", value.toString())
    }
    override fun getPendingPayment(): String? = udString("pendingPayment")
    override fun setPendingPayment(value: String?) { udSetString("pendingPayment", value) }
    override fun getLastSeenReferralAwardedDays(userId: String): Int =
        udString("seenAwarded.$userId")?.toIntOrNull() ?: -1
    override fun setLastSeenReferralAwardedDays(userId: String, value: Int) {
        udSetString("seenAwarded.$userId", value.toString())
    }
    override fun isUnderworkInfoDismissed(): Boolean = underworkInfoDismissed
    override fun setUnderworkInfoDismissed() { underworkInfoDismissed = true }
    override fun getThemeMode(): String? = themeMode
    override fun setThemeMode(value: String) { themeMode = value }
    override fun getCurrentRouteBlockOrder(): List<String>? = currentRouteBlockOrder
    override fun setCurrentRouteBlockOrder(order: List<String>) { currentRouteBlockOrder = order }
    override fun getSelectedFilters(): Set<String>? = selectedFilters
    override fun setSelectedFilters(values: Set<String>) { selectedFilters = values }
    override fun isExpandedView(): Boolean = expandedView
    override fun setIsExpandedView(value: Boolean) { expandedView = value }
    override fun isShowTurnaroundRest(): Boolean = showTurnaroundRest
    override fun setShowTurnaroundRest(value: Boolean) { showTurnaroundRest = value }
    override fun toggleShowTravelTime(value: Boolean) { showTravelTime = value }
    override fun isShowTravelTime(): Boolean = showTravelTime
    override fun isLocoSectionTimeExpanded(): Boolean = locoSectionTimeExpanded
    override fun setLocoSectionTimeExpanded(value: Boolean) { locoSectionTimeExpanded = value }
    override fun isLocoSectionHeatingExpanded(): Boolean = locoSectionHeatingExpanded
    override fun setLocoSectionHeatingExpanded(value: Boolean) { locoSectionHeatingExpanded = value }
    override fun isLocoSectionAuxiliaryExpanded(): Boolean = locoSectionAuxiliaryExpanded
    override fun setLocoSectionAuxiliaryExpanded(value: Boolean) { locoSectionAuxiliaryExpanded = value }
    override fun isLocoSectionStatisticsExpanded(): Boolean = locoSectionStatisticsExpanded
    override fun setLocoSectionStatisticsExpanded(value: Boolean) { locoSectionStatisticsExpanded = value }
    override fun isShowLocoHeating(): Boolean = showLocoHeating
    override fun setShowLocoHeating(value: Boolean) { showLocoHeating = value }
    override fun isShowLocoAuxiliary(): Boolean = showLocoAuxiliary
    override fun setShowLocoAuxiliary(value: Boolean) { showLocoAuxiliary = value }
    override fun isShowLocoStatistics(): Boolean = showLocoStatistics
    override fun setShowLocoStatistics(value: Boolean) { showLocoStatistics = value }
    override fun isShowLocoNorma(): Boolean = showLocoNorma
    override fun setShowLocoNorma(value: Boolean) { showLocoNorma = value }
    override fun isShowOtherCurrent(): Boolean = showOtherCurrent
    override fun setShowOtherCurrent(value: Boolean) { showOtherCurrent = value }
    override fun isLocoSectionNormaExpanded(): Boolean = locoSectionNormaExpanded
    override fun setLocoSectionNormaExpanded(value: Boolean) { locoSectionNormaExpanded = value }
    override fun getDefaultLocoSectionNumberingType(): SectionNumberingType = defaultLocoSectionNumberingType
    override fun setDefaultLocoSectionNumberingType(value: SectionNumberingType) {
        defaultLocoSectionNumberingType = value
    }
    override fun isPassenger12hDontAskAgain(): Boolean = passenger12hDontAskAgain
    override fun setPassenger12hDontAskAgain(value: Boolean) { passenger12hDontAskAgain = value }

    private var locoNormHandToHand: Boolean = false
    override fun isLocoNormHandToHand(): Boolean = locoNormHandToHand
    override fun setLocoNormHandToHand(value: Boolean) { locoNormHandToHand = value }
    override fun isPassenger12hAutoAccepted(): Boolean = passenger12hAutoAccepted
    override fun setPassenger12hAutoAccepted(value: Boolean) { passenger12hAutoAccepted = value }

    override fun isTimePickerKeyboardInput(): Boolean = timePickerKeyboardInput
    override fun setTimePickerKeyboardInput(value: Boolean) { timePickerKeyboardInput = value }

    override fun getRecentTimes(key: String): List<Long> = recentTimesMap[key] ?: emptyList()
    override fun addRecentTime(key: String, timeMillis: Long) {
        val list = recentTimesMap.getOrPut(key) { mutableListOf() }
        list.add(0, timeMillis)
        if (list.size > 10) list.removeAt(list.lastIndex)
    }

    private val recentCoefficients = mutableListOf("0.84", "0.85", "0.86", "0.87", "0.88")
    override fun getRecentCoefficients(): List<String> = recentCoefficients.toList()
    override fun addRecentCoefficient(value: String) {
        if (value.toDoubleOrNull() == null) return
        recentCoefficients.remove(value)
        recentCoefficients.add(0, value)
        while (recentCoefficients.size > 5) recentCoefficients.removeAt(recentCoefficients.lastIndex)
    }

    private var lastRefuelCoefficient: String = "0.83"
    override fun getLastRefuelCoefficient(): String = lastRefuelCoefficient
    override fun setLastRefuelCoefficient(value: String) {
        if (value.toDoubleOrNull() == null) return
        lastRefuelCoefficient = value
    }

    private var schedulePatterns: List<com.z_company.domain.entities.SchedulePattern>? = null
    override fun getSchedulePatterns(): List<com.z_company.domain.entities.SchedulePattern>? = schedulePatterns
    override fun setSchedulePatterns(patterns: List<com.z_company.domain.entities.SchedulePattern>) {
        schedulePatterns = patterns
    }
}
