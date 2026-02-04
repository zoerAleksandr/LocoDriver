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
}