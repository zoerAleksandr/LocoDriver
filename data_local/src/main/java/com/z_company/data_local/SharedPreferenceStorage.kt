package com.z_company.data_local

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.z_company.core.ui.component.TypeDateTimePicker
import com.z_company.domain.repositories.SharedPreferencesRepositories
import org.koin.core.component.KoinComponent

private const val TOKEN_IS_LOAD_STATION_NAME_AND_LOCOMOTIVE_SERIES = "TOKEN_IS_LOAD_STATION_NAME_AND_LOCOMOTIVE_SERIES"
private const val TOKEN_IS_FIRST_APP_ENTRY_TAG = "TOKEN_IS_FIRST_APP_ENTRY_TAG"
private const val TOKEN_IS_SYNC_TAG = "TOKEN_IS_SYNC_TAG"
private const val TOKEN_IS_CHANGES_HAVE_TAG = "TOKEN_IS_CHANGES_HAVE_TAG"
private const val TOKEN_SUBSCRIPTION_EXPIRATION_TAG = "TOKEN_SUBSCRIPTION_EXPIRATION_TAG"
private const val TOKEN_IS_SHOW_UPDATE_PRESENTATION_VER_1_2_16 = "TOKEN_IS_SHOW_UPDATE_PRESENTATION_VER_1_2_16"
private const val TOKEN_DATE_TIME_PICKER_TYPE = "TOKEN_DATE_TIME_PICKER_TYPE"

class SharedPreferenceStorage(application: Application) : SharedPreferencesRepositories, KoinComponent {
    private val sharedpref: SharedPreferences =
        application.getSharedPreferences(
            application.packageName,
            Context.MODE_PRIVATE
        )
    private val editor = sharedpref.edit()



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
        editor.putBoolean(TOKEN_IS_FIRST_APP_ENTRY_TAG, value).apply()
    }

    fun setTokenIsSyncEnable(value: Boolean) {
        editor.putBoolean(TOKEN_IS_SYNC_TAG, value).apply()
    }

    override fun tokenIsLoadStationAndLocomotiveSeries(): Boolean =
        sharedpref.getBoolean(TOKEN_IS_LOAD_STATION_NAME_AND_LOCOMOTIVE_SERIES, false)

    override fun setTokenIsLoadStationAndLocomotiveSeries(value: Boolean) {
        editor.putBoolean(TOKEN_IS_LOAD_STATION_NAME_AND_LOCOMOTIVE_SERIES, value).apply()
    }

    override fun tokenDateTimePickerType(): String =
        sharedpref.getString(TOKEN_DATE_TIME_PICKER_TYPE, TypeDateTimePicker.INPUT.name)!!


    override fun setTokenDateTimePickerType(type: String) {
        editor.putString(TOKEN_DATE_TIME_PICKER_TYPE, type).apply()
    }
}