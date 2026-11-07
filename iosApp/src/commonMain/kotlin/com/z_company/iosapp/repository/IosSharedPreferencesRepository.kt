package com.z_company.iosapp.repository

import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.entities.WorkScheduleProfile
import com.z_company.domain.entities.norma_time.SectionNumberingType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

/**
 * iOS-реализация SharedPreferencesRepositories на NSUserDefaults (аналог
 * Android SharedPreferences, см. SharedPreferenceStorage).
 *
 * Ключи, участвующие в синхронизации с сервером (С8), переживают выгрузку
 * процесса: `lastSyncTimestamp`, `routeSyncCursor`, `settingsSyncPending`,
 * `subscriptionExpiration`, `workScheduleProfile` (LWW-поле SyncManager).
 * Иначе каждый перезапуск сбрасывал курсор дельта-синхронизации и cooldown
 * фоновой синхронизации.
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
    private fun udInt(key: String, default: Int): Int {
        val d = NSUserDefaults.standardUserDefaults
        return if (d.objectForKey("ld.$key") == null) default else d.integerForKey("ld.$key").toInt()
    }
    private fun udSetInt(key: String, value: Int) {
        NSUserDefaults.standardUserDefaults.setInteger(value.toLong(), forKey = "ld.$key")
    }
    private fun udLong(key: String, default: Long): Long {
        val d = NSUserDefaults.standardUserDefaults
        return if (d.objectForKey("ld.$key") == null) default else d.integerForKey("ld.$key")
    }
    private fun udSetLong(key: String, value: Long) {
        NSUserDefaults.standardUserDefaults.setInteger(value, forKey = "ld.$key")
    }
    private var lastScheduleMonth: String? = null
    override fun getLastScheduleMonth(): String? = lastScheduleMonth
    override fun setLastScheduleMonth(value: String) { lastScheduleMonth = value }
    // Профиль рабочей недели — LWW-поле синхронизации (updatedAt); хранится JSON-ом,
    // как в Android SharedPreferenceStorage (см. adoptRemoteWorkSchedule в SyncManager).
    private val workScheduleJson = Json { ignoreUnknownKeys = true }
    private val workScheduleProfileState = MutableStateFlow(readWorkScheduleProfile())
    private fun readWorkScheduleProfile(): WorkScheduleProfile {
        val raw = udString("workScheduleProfile") ?: return WorkScheduleProfile.standard()
        return runCatching {
            workScheduleJson.decodeFromString(WorkScheduleProfile.serializer(), raw)
        }.getOrElse { WorkScheduleProfile.standard() }
    }
    override fun getWorkScheduleProfile(): WorkScheduleProfile = workScheduleProfileState.value
    override fun getWorkScheduleProfileFlow(): StateFlow<WorkScheduleProfile> = workScheduleProfileState
    override fun setWorkScheduleProfile(profile: WorkScheduleProfile) {
        udSetString("workScheduleProfile", workScheduleJson.encodeToString(WorkScheduleProfile.serializer(), profile))
        workScheduleProfileState.value = profile
    }
    // Ключи синхронизации (С8) — в NSUserDefaults, как в Android (TOKEN_LAST_SYNC_TIME и т.п.).
    private var lastSyncTimestamp: Long
        get() = udLong("lastSyncTimestamp", 0L)
        set(v) = udSetLong("lastSyncTimestamp", v)
    private var subscriptionExpiration: Long
        get() = udLong("subscriptionExpiration", 0L)
        set(v) = udSetLong("subscriptionExpiration", v)
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
    // Флаг «Что нового» (§22): ключ и семантика как на Android
    // (TOKEN_IS_SHOW_UPDATE_PRESENTATION_VER_1_2_16, по умолчанию true).
    private var showUpdatePresentation: Boolean
        get() = udBool("TOKEN_IS_SHOW_UPDATE_PRESENTATION_VER_1_2_16", true)
        set(v) = udSetBool("TOKEN_IS_SHOW_UPDATE_PRESENTATION_VER_1_2_16", v)
    private var tokenIsChangesHave: Boolean
        get() = udBool("tokenIsChangesHave", false)
        set(v) = udSetBool("tokenIsChangesHave", v)
    // default true — только самая первая установка; после setTokenIsFirstAppEntry(false)
    // значение остаётся в NSUserDefaults и между запусками.
    // Ключ — TOKEN_IS_FIRST_APP_ENTRY_TAG (как в Android SharedPreferences).
    private var tokenIsFirstAppEntry: Boolean
        get() = udBool("TOKEN_IS_FIRST_APP_ENTRY_TAG", true)
        set(v) = udSetBool("TOKEN_IS_FIRST_APP_ENTRY_TAG", v)
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
    // «Видел/не видел» новость при запуске (§23.3) — хранение на устройстве,
    // по ключу типа сообщения (news/update), дефолт -1 = ничего не видел.
    override fun getLastSeenAnnouncementNumber(type: String): Int =
        udInt("lastSeenAnnouncement.$type", -1)
    override fun setLastSeenAnnouncementNumber(type: String, value: Int) {
        udSetInt("lastSeenAnnouncement.$type", value)
    }
    private var underworkInfoDismissed: Boolean = false
    private var themeMode: String?
        get() = NSUserDefaults.standardUserDefaults.stringForKey("ld.themeMode")
        set(v) = NSUserDefaults.standardUserDefaults.setObject(v, forKey = "ld.themeMode")
    private var currentRouteBlockOrder: List<String>? = null

    override fun setLastSyncTimestamp(time: Long) { lastSyncTimestamp = time }
    override fun getLastSyncTimestamp(): Long = lastSyncTimestamp

    // Курсор дельта-синхронизации маршрутов: переживает перезапуск (С8),
    // иначе каждая синхронизация — полная вместо дельты.
    override fun getRouteSyncCursor(): String? = udString("routeSyncCursor")
    override fun setRouteSyncCursor(value: String?) { udSetString("routeSyncCursor", value) }

    // Флаг «настройки ждут выгрузки» (дефолт true — как в Android).
    override fun getSettingsSyncPending(): Boolean = udBool("settingsSyncPending", true)
    override fun setSettingsSyncPending(value: Boolean) { udSetBool("settingsSyncPending", value) }

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
    // Название обманчиво (как на Android): метод пишет false — «больше не показывать».
    override fun enableShowingUpdatePresentation() { showUpdatePresentation = false }
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
