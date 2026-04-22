package com.z_company.iosapp.repository

import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.iosapp.platform.PlatformKeyValueStorage

/**
 * iOS-реализация SharedPreferencesRepositories на NSUserDefaults
 * (через expect/actual [PlatformKeyValueStorage]).
 *
 * Значения сохраняются между запусками приложения, что критично для:
 *  - [tokenIsFirstAppEntry] — флаг первого запуска (иначе инициализация
 *    будет выполняться каждый раз и сбрасывать пользовательские настройки);
 *  - флагов миграций, синк-таймштампа, опций UI.
 */
class IosSharedPreferencesRepository(
    private val storage: PlatformKeyValueStorage,
) : SharedPreferencesRepositories {

    private companion object Keys {
        const val LAST_SYNC = "last_sync_timestamp"
        const val OP_KEY = "op_key_robokassa"
        const val IS_MIGRATED = "is_migrated"
        const val TIMEZONE_MIGRATION_DONE = "timezone_migration_done"
        const val RELEASE_DAY_MIGRATION_DONE = "release_day_migration_done"
        const val PRODUCTION_CAL_MIGRATION_DONE = "production_calendar_migration_done"
        const val SHOW_UPDATE_PRESENTATION = "show_update_presentation"
        const val SUBSCRIPTION_EXPIRATION = "subscription_expiration"
        const val TOKEN_CHANGES_HAVE = "token_changes_have"
        // На iOS — новое приложение, поэтому первый запуск считается единожды.
        const val TOKEN_FIRST_ENTRY = "token_first_app_entry"
        const val TOKEN_LOAD_STATION_LOCO = "token_load_station_locomotive"
        const val STATIONS_REVERSED = "stations_reversed"
        const val INPUT_DIESEL_KILO = "input_diesel_in_kilo"
        const val SORT_OPTION = "sort_option"
        const val SELECTED_FILTERS = "selected_filters"
        const val EXPANDED_VIEW = "expanded_view"
        const val SHOW_TRAVEL_TIME = "show_travel_time"
        const val SHOW_LOCO_FORM_HINT = "show_loco_form_update_hint"
        const val LOCO_SECTION_TIME = "loco_section_time_expanded"
        const val LOCO_SECTION_HEATING = "loco_section_heating_expanded"
        const val LOCO_SECTION_AUXILIARY = "loco_section_auxiliary_expanded"
        const val LOCO_SECTION_STATISTICS = "loco_section_statistics_expanded"
        const val LOCO_SECTION_NORMA = "loco_section_norma_expanded"
        const val SHOW_LOCO_HEATING = "show_loco_heating"
        const val SHOW_LOCO_AUXILIARY = "show_loco_auxiliary"
        const val SHOW_LOCO_STATISTICS = "show_loco_statistics"
        const val SHOW_LOCO_NORMA = "show_loco_norma"
        const val SHOW_OTHER_CURRENT = "show_other_current"
        const val PASSENGER_12H_DONT_ASK = "passenger_12h_dont_ask"
        const val PASSENGER_12H_AUTO_ACCEPTED = "passenger_12h_auto_accepted"

        const val RECENT_TIMES_PREFIX = "recent_times_"
        const val RECENT_TIMES_LIMIT = 10
    }

    override fun setLastSyncTimestamp(time: Long) = storage.setLong(LAST_SYNC, time)
    override fun getLastSyncTimestamp(): Long = storage.getLong(LAST_SYNC, 0L)

    override fun getOPKeyRobokassa(): String? = storage.getString(OP_KEY)
    override fun setOPKeyRobokassa(opKey: String?) = storage.setString(OP_KEY, opKey)

    override fun isMigrated(): Boolean = storage.getBoolean(IS_MIGRATED, false)
    override fun setIsMigrated(value: Boolean) = storage.setBoolean(IS_MIGRATED, value)

    override fun isTimezoneMigrationDone(): Boolean =
        storage.getBoolean(TIMEZONE_MIGRATION_DONE, false)
    override fun setTimezoneMigrationDone() = storage.setBoolean(TIMEZONE_MIGRATION_DONE, true)

    override fun isReleaseDayMigrationDone(): Boolean =
        storage.getBoolean(RELEASE_DAY_MIGRATION_DONE, false)
    override fun setReleaseDayMigrationDone() =
        storage.setBoolean(RELEASE_DAY_MIGRATION_DONE, true)

    override fun isProductionCalendarMigrationDone(): Boolean =
        storage.getBoolean(PRODUCTION_CAL_MIGRATION_DONE, false)
    override fun setProductionCalendarMigrationDone() =
        storage.setBoolean(PRODUCTION_CAL_MIGRATION_DONE, true)

    override fun isShowUpdatePresentation(): Boolean =
        storage.getBoolean(SHOW_UPDATE_PRESENTATION, true)
    override fun enableShowingUpdatePresentation() =
        storage.setBoolean(SHOW_UPDATE_PRESENTATION, true)

    override fun getSubscriptionExpiration(): Long = storage.getLong(SUBSCRIPTION_EXPIRATION, 0L)
    override fun setSubscriptionExpiration(value: Long) =
        storage.setLong(SUBSCRIPTION_EXPIRATION, value)

    override fun tokenIsChangesHave(): Boolean = storage.getBoolean(TOKEN_CHANGES_HAVE, false)
    override fun setTokenIsChangeHave(value: Boolean) =
        storage.setBoolean(TOKEN_CHANGES_HAVE, value)

    override fun tokenIsFirstAppEntry(): Boolean = storage.getBoolean(TOKEN_FIRST_ENTRY, true)
    override fun setTokenIsFirstAppEntry(value: Boolean) =
        storage.setBoolean(TOKEN_FIRST_ENTRY, value)

    override fun tokenIsLoadStationAndLocomotiveSeries(): Boolean =
        storage.getBoolean(TOKEN_LOAD_STATION_LOCO, false)
    override fun setTokenIsLoadStationAndLocomotiveSeries(value: Boolean) =
        storage.setBoolean(TOKEN_LOAD_STATION_LOCO, value)

    override fun toggleStationsSortOrder(value: Boolean) =
        storage.setBoolean(STATIONS_REVERSED, value)
    override fun isReversedSortStationList(): Boolean =
        storage.getBoolean(STATIONS_REVERSED, false)

    override fun toggleInputDieselInKilo(value: Boolean) =
        storage.setBoolean(INPUT_DIESEL_KILO, value)
    override fun isInputDieselInKilo(): Boolean = storage.getBoolean(INPUT_DIESEL_KILO, false)

    override fun getSortOption(): String? = storage.getString(SORT_OPTION)
    override fun setSortOption(value: String) = storage.setString(SORT_OPTION, value)

    override fun getSelectedFilters(): Set<String>? = storage.getStringSet(SELECTED_FILTERS)
    override fun setSelectedFilters(values: Set<String>) =
        storage.setStringSet(SELECTED_FILTERS, values)

    override fun isExpandedView(): Boolean = storage.getBoolean(EXPANDED_VIEW, false)
    override fun setIsExpandedView(value: Boolean) = storage.setBoolean(EXPANDED_VIEW, value)

    override fun toggleShowTravelTime(value: Boolean) =
        storage.setBoolean(SHOW_TRAVEL_TIME, value)
    override fun isShowTravelTime(): Boolean = storage.getBoolean(SHOW_TRAVEL_TIME, false)

    override fun isShowLocoFormUpdateHint(): Boolean =
        storage.getBoolean(SHOW_LOCO_FORM_HINT, true)
    override fun setLocoFormUpdateHintShown() = storage.setBoolean(SHOW_LOCO_FORM_HINT, false)

    override fun isLocoSectionTimeExpanded(): Boolean =
        storage.getBoolean(LOCO_SECTION_TIME, false)
    override fun setLocoSectionTimeExpanded(value: Boolean) =
        storage.setBoolean(LOCO_SECTION_TIME, value)

    override fun isLocoSectionHeatingExpanded(): Boolean =
        storage.getBoolean(LOCO_SECTION_HEATING, false)
    override fun setLocoSectionHeatingExpanded(value: Boolean) =
        storage.setBoolean(LOCO_SECTION_HEATING, value)

    override fun isLocoSectionAuxiliaryExpanded(): Boolean =
        storage.getBoolean(LOCO_SECTION_AUXILIARY, false)
    override fun setLocoSectionAuxiliaryExpanded(value: Boolean) =
        storage.setBoolean(LOCO_SECTION_AUXILIARY, value)

    override fun isLocoSectionStatisticsExpanded(): Boolean =
        storage.getBoolean(LOCO_SECTION_STATISTICS, false)
    override fun setLocoSectionStatisticsExpanded(value: Boolean) =
        storage.setBoolean(LOCO_SECTION_STATISTICS, value)

    override fun isShowLocoHeating(): Boolean = storage.getBoolean(SHOW_LOCO_HEATING, false)
    override fun setShowLocoHeating(value: Boolean) =
        storage.setBoolean(SHOW_LOCO_HEATING, value)

    override fun isShowLocoAuxiliary(): Boolean = storage.getBoolean(SHOW_LOCO_AUXILIARY, false)
    override fun setShowLocoAuxiliary(value: Boolean) =
        storage.setBoolean(SHOW_LOCO_AUXILIARY, value)

    override fun isShowLocoStatistics(): Boolean =
        storage.getBoolean(SHOW_LOCO_STATISTICS, false)
    override fun setShowLocoStatistics(value: Boolean) =
        storage.setBoolean(SHOW_LOCO_STATISTICS, value)

    override fun isShowLocoNorma(): Boolean = storage.getBoolean(SHOW_LOCO_NORMA, false)
    override fun setShowLocoNorma(value: Boolean) = storage.setBoolean(SHOW_LOCO_NORMA, value)

    override fun isShowOtherCurrent(): Boolean = storage.getBoolean(SHOW_OTHER_CURRENT, false)
    override fun setShowOtherCurrent(value: Boolean) =
        storage.setBoolean(SHOW_OTHER_CURRENT, value)

    override fun isLocoSectionNormaExpanded(): Boolean =
        storage.getBoolean(LOCO_SECTION_NORMA, false)
    override fun setLocoSectionNormaExpanded(value: Boolean) =
        storage.setBoolean(LOCO_SECTION_NORMA, value)

    override fun isPassenger12hDontAskAgain(): Boolean =
        storage.getBoolean(PASSENGER_12H_DONT_ASK, false)
    override fun setPassenger12hDontAskAgain(value: Boolean) =
        storage.setBoolean(PASSENGER_12H_DONT_ASK, value)

    override fun isPassenger12hAutoAccepted(): Boolean =
        storage.getBoolean(PASSENGER_12H_AUTO_ACCEPTED, false)
    override fun setPassenger12hAutoAccepted(value: Boolean) =
        storage.setBoolean(PASSENGER_12H_AUTO_ACCEPTED, value)

    override fun getRecentTimes(key: String): List<Long> =
        storage.getLongList(RECENT_TIMES_PREFIX + key)

    override fun addRecentTime(key: String, timeMillis: Long) {
        val storageKey = RECENT_TIMES_PREFIX + key
        val current = storage.getLongList(storageKey).toMutableList()
        current.add(0, timeMillis)
        while (current.size > RECENT_TIMES_LIMIT) current.removeAt(current.lastIndex)
        storage.setLongList(storageKey, current)
    }
}
