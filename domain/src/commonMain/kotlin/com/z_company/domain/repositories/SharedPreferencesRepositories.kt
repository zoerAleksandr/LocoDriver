package com.z_company.domain.repositories

interface SharedPreferencesRepositories {
    fun setLastSyncTimestamp(time: Long)
    fun getLastSyncTimestamp(): Long

    fun getOPKeyRobokassa(): String?
    fun setOPKeyRobokassa(opKey: String?)
    fun isMigrated(): Boolean
    fun setIsMigrated(value: Boolean)
    fun isShowUpdatePresentation(): Boolean
    fun enableShowingUpdatePresentation()
    fun getSubscriptionExpiration(): Long
    fun tokenIsChangesHave(): Boolean
    fun tokenIsFirstAppEntry(): Boolean
    fun setSubscriptionExpiration(value: Long)
    fun setTokenIsChangeHave(value: Boolean)
    fun setTokenIsFirstAppEntry(value: Boolean)
    fun tokenIsLoadStationAndLocomotiveSeries(): Boolean
    fun setTokenIsLoadStationAndLocomotiveSeries(value: Boolean)
    fun toggleStationsSortOrder(value: Boolean)
    fun isReversedSortStationList(): Boolean
    fun toggleInputDieselInKilo(value: Boolean)
    fun isInputDieselInKilo(): Boolean
    fun getSortOption(): String?
    fun setSortOption(value: String)
    fun getSelectedFilters(): Set<String>?
    fun setSelectedFilters(values: Set<String>)
    fun isExpandedView(): Boolean
    fun setIsExpandedView(value: Boolean)
    fun toggleShowTravelTime(value: Boolean)
    fun isShowTravelTime(): Boolean
    fun isShowLocoFormUpdateHint(): Boolean
    fun setLocoFormUpdateHintShown()
    fun isLocoSectionTimeExpanded(): Boolean
    fun setLocoSectionTimeExpanded(value: Boolean)
    fun isLocoSectionHeatingExpanded(): Boolean
    fun setLocoSectionHeatingExpanded(value: Boolean)
    fun isLocoSectionAuxiliaryExpanded(): Boolean
    fun setLocoSectionAuxiliaryExpanded(value: Boolean)
    fun isLocoSectionStatisticsExpanded(): Boolean
    fun setLocoSectionStatisticsExpanded(value: Boolean)
    fun isShowLocoHeating(): Boolean
    fun setShowLocoHeating(value: Boolean)
    fun isShowLocoAuxiliary(): Boolean
    fun setShowLocoAuxiliary(value: Boolean)
    fun isShowLocoStatistics(): Boolean
    fun setShowLocoStatistics(value: Boolean)
    fun isShowLocoNorma(): Boolean
    fun setShowLocoNorma(value: Boolean)
    fun isShowOtherCurrent(): Boolean
    fun setShowOtherCurrent(value: Boolean)
    fun isLocoSectionNormaExpanded(): Boolean
    fun setLocoSectionNormaExpanded(value: Boolean)
    fun isPassenger12hDontAskAgain(): Boolean
    fun setPassenger12hDontAskAgain(value: Boolean)
    fun isPassenger12hAutoAccepted(): Boolean
    fun setPassenger12hAutoAccepted(value: Boolean)

    fun getRecentTimes(key: String): List<Long>
    fun addRecentTime(key: String, timeMillis: Long)

    /** true = пользователь предпочитает ввод с клавиатуры в системном пикере времени */
    fun isTimePickerKeyboardInput(): Boolean
    fun setTimePickerKeyboardInput(value: Boolean)

    fun isTimezoneMigrationDone(): Boolean
    fun setTimezoneMigrationDone()

    /** Флаг одноразовой миграции отвлечений из MonthOfYear.days → таблицу ReleaseDay */
    fun isReleaseDayMigrationDone(): Boolean
    fun setReleaseDayMigrationDone()

    /** Флаг одноразовой миграции тегов дней из MonthOfYear → ProductionCalendarDay */
    fun isProductionCalendarMigrationDone(): Boolean
    fun setProductionCalendarMigrationDone()
}