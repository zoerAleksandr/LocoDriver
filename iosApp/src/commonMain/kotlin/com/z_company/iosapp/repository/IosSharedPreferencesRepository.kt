package com.z_company.iosapp.repository

import com.z_company.domain.repositories.SharedPreferencesRepositories

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
    // Хранится в памяти; при перезапуске приложения будет сброшено.
    // Для продакшена следует использовать NSUserDefaults.
    private var lastSyncTimestamp: Long = 0L
    private var subscriptionExpiration: Long = 0L
    private var isMigrated: Boolean = false
    private var timezoneMigrationDone: Boolean = true // iOS не нуждается в миграции временных меток
    private var opKeyRobokassa: String? = null
    private var showUpdatePresentation: Boolean = true
    private var tokenIsChangesHave: Boolean = false
    private var tokenIsFirstAppEntry: Boolean = true
    private var tokenIsLoadStationAndLocomotiveSeries: Boolean = false
    private var stationsReversed: Boolean = false
    private var inputDieselInKilo: Boolean = false
    private var sortOption: String? = null
    private var selectedFilters: Set<String>? = null
    private var expandedView: Boolean = false
    private var showTravelTime: Boolean = false
    private var showLocoFormUpdateHint: Boolean = true
    private var locoSectionTimeExpanded: Boolean = false
    private var locoSectionHeatingExpanded: Boolean = false
    private var locoSectionAuxiliaryExpanded: Boolean = false
    private var locoSectionStatisticsExpanded: Boolean = false
    private var showLocoHeating: Boolean = false
    private var showLocoAuxiliary: Boolean = false
    private var showLocoStatistics: Boolean = false
    private var showLocoNorma: Boolean = false
    private var showOtherCurrent: Boolean = false
    private var locoSectionNormaExpanded: Boolean = false
    private var passenger12hDontAskAgain: Boolean = false
    private var passenger12hAutoAccepted: Boolean = false
    private val recentTimesMap = mutableMapOf<String, MutableList<Long>>()

    override fun setLastSyncTimestamp(time: Long) { lastSyncTimestamp = time }
    override fun getLastSyncTimestamp(): Long = lastSyncTimestamp

    override fun getOPKeyRobokassa(): String? = opKeyRobokassa
    override fun setOPKeyRobokassa(opKey: String?) { opKeyRobokassa = opKey }
    override fun isMigrated(): Boolean = isMigrated
    override fun setIsMigrated(value: Boolean) { isMigrated = value }
    override fun isTimezoneMigrationDone(): Boolean = timezoneMigrationDone
    override fun setTimezoneMigrationDone() { timezoneMigrationDone = true }
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
    override fun toggleStationsSortOrder(value: Boolean) { stationsReversed = value }
    override fun isReversedSortStationList(): Boolean = stationsReversed
    override fun toggleInputDieselInKilo(value: Boolean) { inputDieselInKilo = value }
    override fun isInputDieselInKilo(): Boolean = inputDieselInKilo
    override fun getSortOption(): String? = sortOption
    override fun setSortOption(value: String) { sortOption = value }
    override fun getSelectedFilters(): Set<String>? = selectedFilters
    override fun setSelectedFilters(values: Set<String>) { selectedFilters = values }
    override fun isExpandedView(): Boolean = expandedView
    override fun setIsExpandedView(value: Boolean) { expandedView = value }
    override fun toggleShowTravelTime(value: Boolean) { showTravelTime = value }
    override fun isShowTravelTime(): Boolean = showTravelTime
    override fun isShowLocoFormUpdateHint(): Boolean = showLocoFormUpdateHint
    override fun setLocoFormUpdateHintShown() { showLocoFormUpdateHint = false }
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
    override fun isPassenger12hDontAskAgain(): Boolean = passenger12hDontAskAgain
    override fun setPassenger12hDontAskAgain(value: Boolean) { passenger12hDontAskAgain = value }
    override fun isPassenger12hAutoAccepted(): Boolean = passenger12hAutoAccepted
    override fun setPassenger12hAutoAccepted(value: Boolean) { passenger12hAutoAccepted = value }

    override fun getRecentTimes(key: String): List<Long> = recentTimesMap[key] ?: emptyList()
    override fun addRecentTime(key: String, timeMillis: Long) {
        val list = recentTimesMap.getOrPut(key) { mutableListOf() }
        list.add(0, timeMillis)
        if (list.size > 10) list.removeAt(list.lastIndex)
    }
}
