package com.z_company.data_local

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import org.koin.core.component.KoinComponent


private const val TOKEN_IS_FIRST_APP_ENTRY_TAG = "TOKEN_IS_FIRST_APP_ENTRY_TAG"
private const val TOKEN_IS_SYNC_TAG = "TOKEN_IS_SYNC_TAG"
private const val TOKEN_IS_CHANGES_HAVE_TAG = "TOKEN_IS_CHANGES_HAVE_TAG"

class SharedPreferenceStorage(application: Application) : KoinComponent {
    private val sharedpref: SharedPreferences =
        application.getSharedPreferences(
            application.packageName,
            Context.MODE_PRIVATE
        )
    private val editor = sharedpref.edit()

    fun tokenIsChangesHave(): Boolean =
        sharedpref.getBoolean(TOKEN_IS_CHANGES_HAVE_TAG, false)

    fun tokenIsFirstAppEntry(): Boolean =
        sharedpref.getBoolean(TOKEN_IS_FIRST_APP_ENTRY_TAG, true)


    fun tokesIsSyncDBEnable(): Boolean =
        sharedpref.getBoolean(TOKEN_IS_SYNC_TAG, false)

    fun setTokenIsChangeHave(value: Boolean) {
        editor.putBoolean(TOKEN_IS_CHANGES_HAVE_TAG, value).apply()
    }
    fun setTokenIsFirstAppEntry(value: Boolean) {
        editor.putBoolean(TOKEN_IS_FIRST_APP_ENTRY_TAG, value).apply()
    }

    fun setTokenIsSyncEnable(value: Boolean) {
        editor.putBoolean(TOKEN_IS_SYNC_TAG, value)
    }
}