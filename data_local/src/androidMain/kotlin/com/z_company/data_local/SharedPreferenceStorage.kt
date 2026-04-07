package com.z_company.data_local

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.z_company.domain.repositories.SharedPreferencesRepositories
import org.koin.core.component.KoinComponent

private const val TOKEN_IS_LOAD_STATION_NAME_AND_LOCOMOTIVE_SERIES =
    "TOKEN_IS_LOAD_STATION_NAME_AND_LOCOMOTIVE_SERIES"
private const val TOKEN_IS_FIRST_APP_ENTRY_TAG = "TOKEN_IS_FIRST_APP_ENTRY_TAG"
private const val TOKEN_IS_SYNC_TAG = "TOKEN_IS_SYNC_TAG"
private const val TOKEN_IS_CHANGES_HAVE_TAG = "TOKEN_IS_CHANGES_HAVE_TAG"
private const val TOKEN_SUBSCRIPTION_EXPIRATION_TAG = "TOKEN_SUBSCRIPTION_EXPIRATION_TAG"
private const val TOKEN_IS_SHOW_UPDATE_PRESENTATION_VER_1_2_16 =
    "TOKEN_IS_SHOW_UPDATE_PRESENTATION_VER_1_2_16"
private const val STATION_SORT_REVERSED = "STATION_SORT_REVERSED"
private const val TOKEN_INPUT_DIESEL_IN_KILO = "TOKEN_INPUT_DIESEL_IN_KILO"
private const val TOKEN_SHOW_LOCO_FORM_UPDATE_V2_1_7 = "TOKEN_SHOW_LOCO_FORM_UPDATE_V2_1_7"
private const val TOKEN_LOCO_SECTION_TIME = "TOKEN_LOCO_SECTION_TIME"
private const val TOKEN_LOCO_SECTION_HEATING = "TOKEN_LOCO_SECTION_HEATING"
private const val TOKEN_LOCO_SECTION_AUXILIARY = "TOKEN_LOCO_SECTION_AUXILIARY"
private const val TOKEN_LOCO_SECTION_STATISTICS = "TOKEN_LOCO_SECTION_STATISTICS"

private const val SORT_OPTION_TAG = "SORT_OPTION"
private const val SELECTED_FILTERS_TAG = "SELECTED_FILTERS"
private const val IS_EXPANDED_VIEW_TAG = "IS_EXPANDED_VIEW"
private const val SHOW_TRAVEL_TIME_TAG = "SHOW_TRAVEL_TIME"
private const val TOKEN_IS_MIGRATED = "TOKEN_IS_MIGRATED"

private const val OP_KEY_PREF = "OP_KEY_PREF"

private const val TOKEN_SHOW_LOCO_HEATING = "TOKEN_SHOW_LOCO_HEATING"
private const val TOKEN_SHOW_LOCO_AUXILIARY = "TOKEN_SHOW_LOCO_AUXILIARY"
private const val TOKEN_SHOW_LOCO_STATISTICS = "TOKEN_SHOW_LOCO_STATISTICS"
private const val TOKEN_SHOW_LOCO_NORMA = "TOKEN_SHOW_LOCO_NORMA"
private const val TOKEN_SHOW_OTHER_CURRENT = "TOKEN_SHOW_OTHER_CURRENT"
private const val TOKEN_LOCO_SECTION_NORMA = "TOKEN_LOCO_SECTION_NORMA"
private const val TOKEN_PASSENGER_12H_DONT_ASK = "TOKEN_PASSENGER_12H_DONT_ASK"
private const val TOKEN_PASSENGER_12H_ACCEPTED = "TOKEN_PASSENGER_12H_ACCEPTED"

private const val TOKEN_LAST_SYNC_TIME = "TOKEN_LAST_SYNC_TIME"
private const val TOKEN_TIMEZONE_MIGRATION = "TOKEN_TIMEZONE_MIGRATION"
private const val TOKEN_RELEASE_DAY_MIGRATION = "TOKEN_RELEASE_DAY_MIGRATION"
private const val TOKEN_PRODUCTION_CALENDAR_MIGRATION = "TOKEN_PRODUCTION_CALENDAR_MIGRATION"
class SharedPreferenceStorage(application: Application) : SharedPreferencesRepositories,
    KoinComponent {
    private val sharedpref: SharedPreferences =
        application.getSharedPreferences(
            application.packageName,
            Context.MODE_PRIVATE
        )
    private val editor = sharedpref.edit()

    override fun setLastSyncTimestamp(time: Long) {
        editor.putLong(TOKEN_LAST_SYNC_TIME, time).apply()
    }

    override fun getLastSyncTimestamp(): Long =
        sharedpref.getLong(TOKEN_LAST_SYNC_TIME, 0L)


    override fun getOPKeyRobokassa(): String? =
        sharedpref.getString(OP_KEY_PREF, null)


    override fun setOPKeyRobokassa(opKey: String?) {
        editor.putString(OP_KEY_PREF, opKey).apply()
    }



    // Добавлено: Метод для проверки флага миграции
    // Для чего: Чтобы определять, выполнена ли миграция (default false)
    override fun isMigrated(): Boolean =
        sharedpref.getBoolean(TOKEN_IS_MIGRATED, false)

    // Добавлено: Метод для установки флага миграции
    // Для чего: Чтобы отметить миграцию как выполненную после успеха
    override fun setIsMigrated(value: Boolean) {
        editor.putBoolean(TOKEN_IS_MIGRATED, value).apply()
    }

    override fun isShowUpdatePresentation(): Boolean =
        sharedpref.getBoolean(TOKEN_IS_SHOW_UPDATE_PRESENTATION_VER_1_2_16, true)

    override fun enableShowingUpdatePresentation() {
        editor.putBoolean(TOKEN_IS_SHOW_UPDATE_PRESENTATION_VER_1_2_16, false).apply()
    }

    override fun getSubscriptionExpiration(): Long =
        sharedpref.getLong(TOKEN_SUBSCRIPTION_EXPIRATION_TAG, 0L)

    override fun tokenIsChangesHave(): Boolean =
        sharedpref.getBoolean(TOKEN_IS_CHANGES_HAVE_TAG, false)

    override fun tokenIsFirstAppEntry(): Boolean =
        sharedpref.getBoolean(TOKEN_IS_FIRST_APP_ENTRY_TAG, true)

    fun tokesIsSyncDBEnable(): Boolean =
        sharedpref.getBoolean(TOKEN_IS_SYNC_TAG, false)

    override fun setSubscriptionExpiration(value: Long) {
        editor.putLong(TOKEN_SUBSCRIPTION_EXPIRATION_TAG, value).apply()
    }

    override fun setTokenIsChangeHave(value: Boolean) {
        editor.putBoolean(TOKEN_IS_CHANGES_HAVE_TAG, value).apply()
    }

    override fun setTokenIsFirstAppEntry(value: Boolean) {
        // commit() вместо apply() — синхронная запись, защита от потери данных
        // при завершении процесса на Android 16 (process death)
        editor.putBoolean(TOKEN_IS_FIRST_APP_ENTRY_TAG, value).commit()
    }

    fun setTokenIsSyncEnable(value: Boolean) {
        editor.putBoolean(TOKEN_IS_SYNC_TAG, value).apply()
    }

    override fun tokenIsLoadStationAndLocomotiveSeries(): Boolean =
        sharedpref.getBoolean(TOKEN_IS_LOAD_STATION_NAME_AND_LOCOMOTIVE_SERIES, false)

    override fun setTokenIsLoadStationAndLocomotiveSeries(value: Boolean) {
        editor.putBoolean(TOKEN_IS_LOAD_STATION_NAME_AND_LOCOMOTIVE_SERIES, value).apply()
    }

    override fun toggleStationsSortOrder(value: Boolean) {
        editor.putBoolean(STATION_SORT_REVERSED, value).apply()
    }

    override fun isReversedSortStationList(): Boolean =
        sharedpref.getBoolean(STATION_SORT_REVERSED, false)

    override fun toggleInputDieselInKilo(value: Boolean) {
        editor.putBoolean(TOKEN_INPUT_DIESEL_IN_KILO, value).apply()
    }

    override fun isInputDieselInKilo(): Boolean =
        sharedpref.getBoolean(TOKEN_INPUT_DIESEL_IN_KILO, false)

    override fun getSortOption(): String? =
        sharedpref.getString(SORT_OPTION_TAG, null)

    override fun setSortOption(value: String) {
        editor.putString(SORT_OPTION_TAG, value).apply()
    }

    override fun getSelectedFilters(): Set<String>? =
        sharedpref.getStringSet(SELECTED_FILTERS_TAG, null)

    override fun setSelectedFilters(values: Set<String>) {
        editor.putStringSet(SELECTED_FILTERS_TAG, values).apply()
    }

    override fun isExpandedView(): Boolean =
        sharedpref.getBoolean(IS_EXPANDED_VIEW_TAG, false)

    override fun setIsExpandedView(value: Boolean) {
        editor.putBoolean(IS_EXPANDED_VIEW_TAG, value).apply()
    }

    override fun toggleShowTravelTime(value: Boolean) {
        editor.putBoolean(SHOW_TRAVEL_TIME_TAG, value).apply()
    }

    override fun isShowTravelTime(): Boolean =
        sharedpref.getBoolean(SHOW_TRAVEL_TIME_TAG, false)

    override fun isShowLocoFormUpdateHint(): Boolean =
        sharedpref.getBoolean(TOKEN_SHOW_LOCO_FORM_UPDATE_V2_1_7, true)

    override fun setLocoFormUpdateHintShown() {
        editor.putBoolean(TOKEN_SHOW_LOCO_FORM_UPDATE_V2_1_7, false).apply()
    }

    override fun isLocoSectionTimeExpanded(): Boolean =
        sharedpref.getBoolean(TOKEN_LOCO_SECTION_TIME, false)

    override fun setLocoSectionTimeExpanded(value: Boolean) {
        editor.putBoolean(TOKEN_LOCO_SECTION_TIME, value).apply()
    }

    override fun isLocoSectionHeatingExpanded(): Boolean =
        sharedpref.getBoolean(TOKEN_LOCO_SECTION_HEATING, false)

    override fun setLocoSectionHeatingExpanded(value: Boolean) {
        editor.putBoolean(TOKEN_LOCO_SECTION_HEATING, value).apply()
    }

    override fun isLocoSectionAuxiliaryExpanded(): Boolean =
        sharedpref.getBoolean(TOKEN_LOCO_SECTION_AUXILIARY, false)

    override fun setLocoSectionAuxiliaryExpanded(value: Boolean) {
        editor.putBoolean(TOKEN_LOCO_SECTION_AUXILIARY, value).apply()
    }

    override fun isLocoSectionStatisticsExpanded(): Boolean =
        sharedpref.getBoolean(TOKEN_LOCO_SECTION_STATISTICS, false)

    override fun setLocoSectionStatisticsExpanded(value: Boolean) {
        editor.putBoolean(TOKEN_LOCO_SECTION_STATISTICS, value).apply()
    }

    override fun isShowLocoHeating(): Boolean =
        sharedpref.getBoolean(TOKEN_SHOW_LOCO_HEATING, false)

    override fun setShowLocoHeating(value: Boolean) {
        editor.putBoolean(TOKEN_SHOW_LOCO_HEATING, value).apply()
    }

    override fun isShowLocoAuxiliary(): Boolean =
        sharedpref.getBoolean(TOKEN_SHOW_LOCO_AUXILIARY, false)

    override fun setShowLocoAuxiliary(value: Boolean) {
        editor.putBoolean(TOKEN_SHOW_LOCO_AUXILIARY, value).apply()
    }

    override fun isShowLocoStatistics(): Boolean =
        sharedpref.getBoolean(TOKEN_SHOW_LOCO_STATISTICS, false)

    override fun setShowLocoStatistics(value: Boolean) {
        editor.putBoolean(TOKEN_SHOW_LOCO_STATISTICS, value).apply()
    }

    override fun isShowLocoNorma(): Boolean =
        sharedpref.getBoolean(TOKEN_SHOW_LOCO_NORMA, false)

    override fun setShowLocoNorma(value: Boolean) {
        editor.putBoolean(TOKEN_SHOW_LOCO_NORMA, value).apply()
    }

    override fun isShowOtherCurrent(): Boolean =
        sharedpref.getBoolean(TOKEN_SHOW_OTHER_CURRENT, false)

    override fun setShowOtherCurrent(value: Boolean) {
        editor.putBoolean(TOKEN_SHOW_OTHER_CURRENT, value).apply()
    }

    override fun isLocoSectionNormaExpanded(): Boolean =
        sharedpref.getBoolean(TOKEN_LOCO_SECTION_NORMA, false)

    override fun setLocoSectionNormaExpanded(value: Boolean) {
        editor.putBoolean(TOKEN_LOCO_SECTION_NORMA, value).apply()
    }

    override fun isPassenger12hDontAskAgain(): Boolean =
        sharedpref.getBoolean(TOKEN_PASSENGER_12H_DONT_ASK, false)

    override fun setPassenger12hDontAskAgain(value: Boolean) {
        editor.putBoolean(TOKEN_PASSENGER_12H_DONT_ASK, value).apply()
    }

    override fun isPassenger12hAutoAccepted(): Boolean =
        sharedpref.getBoolean(TOKEN_PASSENGER_12H_ACCEPTED, false)

    override fun setPassenger12hAutoAccepted(value: Boolean) {
        editor.putBoolean(TOKEN_PASSENGER_12H_ACCEPTED, value).apply()
    }

    override fun isTimezoneMigrationDone(): Boolean =
        sharedpref.getBoolean(TOKEN_TIMEZONE_MIGRATION, false)

    override fun setTimezoneMigrationDone() {
        editor.putBoolean(TOKEN_TIMEZONE_MIGRATION, true).apply()
    }

    override fun getRecentTimes(key: String): List<Long> {
        val json = sharedpref.getString("recent_times_$key", null) ?: return emptyList()
        return try {
            json.removeSurrounding("[", "]")
                .split(",")
                .filter { it.isNotBlank() }
                .map { it.trim().toLong() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun isReleaseDayMigrationDone(): Boolean =
        sharedpref.getBoolean(TOKEN_RELEASE_DAY_MIGRATION, false)

    override fun setReleaseDayMigrationDone() {
        editor.putBoolean(TOKEN_RELEASE_DAY_MIGRATION, true).apply()
    }

    override fun isProductionCalendarMigrationDone(): Boolean =
        sharedpref.getBoolean(TOKEN_PRODUCTION_CALENDAR_MIGRATION, false)

    override fun setProductionCalendarMigrationDone() {
        editor.putBoolean(TOKEN_PRODUCTION_CALENDAR_MIGRATION, true).apply()
    }

    override fun addRecentTime(key: String, timeMillis: Long) {
        val cal = java.util.Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        val newH = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val newM = cal.get(java.util.Calendar.MINUTE)

        val current = getRecentTimes(key).toMutableList()
        // Убираем дубликаты по часам и минутам (игнорируя дату)
        current.removeAll { existing ->
            val c = java.util.Calendar.getInstance().apply { this.timeInMillis = existing }
            c.get(java.util.Calendar.HOUR_OF_DAY) == newH && c.get(java.util.Calendar.MINUTE) == newM
        }
        current.add(0, timeMillis)
        val trimmed = current.take(5)
        editor.putString("recent_times_$key", trimmed.joinToString(",", "[", "]")).apply()
    }
}