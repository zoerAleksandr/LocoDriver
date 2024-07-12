package com.z_company.data_local

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import org.koin.core.component.KoinComponent


private const val TOKEN_IS_FIRST_ENTRY_TAG = "tokenIsFirstEntryTag"
private const val TOKEN_IS_SYNC_TAG = "TOKEN_IS_SYNC_TAG"

class SharedPreferenceStorage(application: Application) : KoinComponent {

    private val sharedpref: SharedPreferences =
        application.getSharedPreferences(
            application.packageName,
            Context.MODE_PRIVATE
        )
    private val editor = sharedpref.edit()

    val tokenIsFirstEntry: Boolean = sharedpref.getBoolean(TOKEN_IS_FIRST_ENTRY_TAG, true)
    val tokesIsSyncDBEnable: Boolean = sharedpref.getBoolean(TOKEN_IS_SYNC_TAG, false)

    fun setTokenIsFirstEntry(value: Boolean) {
        editor.putBoolean(TOKEN_IS_FIRST_ENTRY_TAG, value).apply()
    }

    fun setTokenIsSyncEnable(value: Boolean) {
        editor.putBoolean(TOKEN_IS_SYNC_TAG, value)
    }
}