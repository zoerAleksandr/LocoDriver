package com.z_company.domain.repositories

interface SharedPreferencesRepositories {
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
    fun tokenDateTimePickerType(): String
    fun setTokenDateTimePickerType(type: String)

}