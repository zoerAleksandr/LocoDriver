package com.z_company.data_local

import com.z_company.domain.repositories.SharedPreferencesRepositories
import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of SharedPreferencesRepositories using NSUserDefaults.
 */
class IosSharedPreferenceStorage : SharedPreferencesRepositories {

    private val defaults = NSUserDefaults.standardUserDefaults

    override fun setLastSyncTimestamp(time: Long) {
        defaults.setDouble(time.toDouble(), forKey = KEY_LAST_SYNC)
    }

    override fun getLastSyncTimestamp(): Long {
        return defaults.doubleForKey(KEY_LAST_SYNC).toLong()
    }

    override fun getOPKeyRobokassa(): String? {
        return defaults.stringForKey(KEY_OP_ROBOKASSA)
    }

    override fun setOPKeyRobokassa(opKey: String?) {
        if (opKey != null) defaults.setObject(opKey, forKey = KEY_OP_ROBOKASSA)
        else defaults.removeObjectForKey(KEY_OP_ROBOKASSA)
    }

    override fun isMigrated(): Boolean {
        return defaults.boolForKey(KEY_MIGRATED)
    }

    override fun setIsMigrated(value: Boolean) {
        defaults.setBool(value, forKey = KEY_MIGRATED)
    }

    override fun isShowUpdatePresentation(): Boolean {
        return defaults.boolForKey(KEY_SHOW_UPDATE)
    }

    override fun enableShowingUpdatePresentation() {
        defaults.setBool(true, forKey = KEY_SHOW_UPDATE)
    }

    override fun getSubscriptionExpiration(): Long {
        return defaults.doubleForKey(KEY_SUB_EXPIRATION).toLong()
    }

    override fun tokenIsChangesHave(): Boolean {
        return defaults.boolForKey(KEY_CHANGES_HAVE)
    }

    override fun tokenIsFirstAppEntry(): Boolean {
        return defaults.boolForKey(KEY_FIRST_ENTRY)
    }

    override fun setSubscriptionExpiration(value: Long) {
        defaults.setDouble(value.toDouble(), forKey = KEY_SUB_EXPIRATION)
    }

    override fun setTokenIsChangeHave(value: Boolean) {
        defaults.setBool(value, forKey = KEY_CHANGES_HAVE)
    }

    override fun setTokenIsFirstAppEntry(value: Boolean) {
        defaults.setBool(value, forKey = KEY_FIRST_ENTRY)
    }

    override fun tokenIsLoadStationAndLocomotiveSeries(): Boolean {
        return defaults.boolForKey(KEY_LOAD_STATIONS)
    }

    override fun setTokenIsLoadStationAndLocomotiveSeries(value: Boolean) {
        defaults.setBool(value, forKey = KEY_LOAD_STATIONS)
    }

    override fun toggleStationsSortOrder(value: Boolean) {
        defaults.setBool(value, forKey = KEY_SORT_ORDER)
    }

    override fun isReversedSortStationList(): Boolean {
        return defaults.boolForKey(KEY_SORT_ORDER)
    }

    override fun toggleInputDieselInKilo(value: Boolean) {
        defaults.setBool(value, forKey = KEY_DIESEL_KILO)
    }

    override fun isInputDieselInKilo(): Boolean {
        return defaults.boolForKey(KEY_DIESEL_KILO)
    }

    override fun getSortOption(): String? {
        return defaults.stringForKey(KEY_SORT_OPTION)
    }

    override fun setSortOption(value: String) {
        defaults.setObject(value, forKey = KEY_SORT_OPTION)
    }

    override fun getSelectedFilters(): Set<String>? {
        val array = defaults.stringArrayForKey(KEY_SELECTED_FILTERS) ?: return null
        return array.mapNotNull { it as? String }.toSet()
    }

    override fun setSelectedFilters(values: Set<String>) {
        defaults.setObject(values.toList(), forKey = KEY_SELECTED_FILTERS)
    }

    override fun isExpandedView(): Boolean {
        return defaults.boolForKey(KEY_EXPANDED_VIEW)
    }

    override fun setIsExpandedView(value: Boolean) {
        defaults.setBool(value, forKey = KEY_EXPANDED_VIEW)
    }

    override fun toggleShowTravelTime(value: Boolean) {
        defaults.setBool(value, forKey = KEY_SHOW_TRAVEL)
    }

    override fun isShowTravelTime(): Boolean {
        return defaults.boolForKey(KEY_SHOW_TRAVEL)
    }

    companion object {
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
        private const val KEY_OP_ROBOKASSA = "op_key_robokassa"
        private const val KEY_MIGRATED = "is_migrated"
        private const val KEY_SHOW_UPDATE = "show_update"
        private const val KEY_SUB_EXPIRATION = "subscription_expiration"
        private const val KEY_CHANGES_HAVE = "changes_have"
        private const val KEY_FIRST_ENTRY = "first_entry"
        private const val KEY_LOAD_STATIONS = "load_stations"
        private const val KEY_SORT_ORDER = "sort_order"
        private const val KEY_DIESEL_KILO = "diesel_kilo"
        private const val KEY_SORT_OPTION = "sort_option"
        private const val KEY_SELECTED_FILTERS = "selected_filters"
        private const val KEY_EXPANDED_VIEW = "expanded_view"
        private const val KEY_SHOW_TRAVEL = "show_travel"
    }
}
