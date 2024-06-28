package com.z_company.data_local.setting

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.domain.entities.route.LocoType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "locomotive_driver_pref")
private const val NOT_AUTH = "NOT_AUTH"

class DataStoreRepository(context: Context) {
    // FOR EXAMPLE
    private val oneHourInMillis = 3_600_000L
    private val defaultTimeRest = oneHourInMillis * 3
    private val defaultDieselCoefficient = 0.82
    private val defaultTypeLoco = LocoType.ELECTRIC
    private val defaultStandardDurationOfWork = 0L
    private val defaultStartNightHour = 22
    private val defaultStartNightMinute = 0
    private val defaultEndNightHour = 6
    private val defaultEndNightMinute = 0

    private object PreferencesKey {
        val uid = stringPreferencesKey(name = "uid")
        val minTimeRest = longPreferencesKey(name = "minTimeRest")
        val dieselCoefficient = doublePreferencesKey(name = "dieselCoefficient")
        val typeLoco = stringPreferencesKey(name = "typeLoco")
        val standardDurationWork = longPreferencesKey(name = "standardDurationWork")
        val startNightHour = intPreferencesKey(name = "startNightHour")
        val startNightMinute = intPreferencesKey(name = "startNightMinute")
        val endNightHour = intPreferencesKey(name = "endNightHour")
        val endNightMinute = intPreferencesKey(name = "endNightMinute")
        val nightTime = stringPreferencesKey("nightTime")

    }

    private val dataStore = context.dataStore

    suspend fun isRegistered(): Boolean {
        val userId = dataStore.data.first()[PreferencesKey.uid] ?: NOT_AUTH
        return userId != NOT_AUTH
    }

    suspend fun signIn(userId: String) {
        dataStore.edit { pref ->
            pref[PreferencesKey.uid] = userId
        }
    }

    suspend fun logOut() {
        dataStore.edit { pref ->
            pref[PreferencesKey.uid] = NOT_AUTH
        }
    }


    fun getMinTimeRest(): Flow<Long?> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { pref ->
                pref[PreferencesKey.minTimeRest] ?: defaultTimeRest
            }
    }


    fun setDieselCoefficient(value: Double?): Flow<ResultState<Unit>> {
        return flowRequest {
            value?.let {
                dataStore.edit { pref ->
                    pref[PreferencesKey.dieselCoefficient] = it
                }
            }
        }
    }
}