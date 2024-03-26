package com.z_company.data_local.setting

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.repositories.UserSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import com.z_company.domain.util.times

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "locomotive_driver_pref")
private const val NOT_AUTH = "NOT_AUTH"

class DataStoreRepository(context: Context) : UserSettingsRepository {

    private val oneHourInMillis = 3_600_000L
    private val defaultTimeRest = oneHourInMillis * 3
    private val defaultDieselCoefficient = 0.82
    private val defaultTypeLoco = LocoType.ELECTRIC
    private val defaultStandardDurationOfWork = 0L

    private object PreferencesKey {
        val uid = stringPreferencesKey(name = "uid")
        val minTimeRest = longPreferencesKey(name = "minTimeRest")
        val dieselCoefficient = doublePreferencesKey(name = "dieselCoefficient")
        val typeLoco = stringPreferencesKey(name = "typeLoco")
        val standardDurationWork = longPreferencesKey(name = "standardDurationWork")
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

    override fun getMinTimeRest(): Flow<Long?> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { pref ->
                pref[PreferencesKey.minTimeRest] ?: defaultTimeRest.div(oneHourInMillis)

            }
    }

    override fun getDieselCoefficient(): Flow<Double> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { pref ->
                pref[PreferencesKey.dieselCoefficient] ?: defaultDieselCoefficient
            }
    }

    override fun setDieselCoefficient(value: Double?): Flow<ResultState<Unit>> {
        return flowRequest {
            value?.let {
                dataStore.edit { pref ->
                    pref[PreferencesKey.dieselCoefficient] = it
                }
            }
        }
    }

    override fun setMinTimeRest(value: Long?): Flow<ResultState<Unit>> {
        return flowRequest {
            dataStore.edit { pref ->
                pref[PreferencesKey.minTimeRest] = oneHourInMillis.times(value ?: 0)
            }
        }
    }

    override fun getStandardDurationOfWork(): Flow<Long> {
        setStandardDurationOfWork(43200000L)
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { pref ->
                pref[PreferencesKey.standardDurationWork] ?: defaultStandardDurationOfWork
            }
    }

    override fun setStandardDurationOfWork(value: Long): Flow<ResultState<Unit>> {
        return callbackFlow {
            trySend(ResultState.Loading)
            try {
                dataStore.edit { pref ->
                    pref[PreferencesKey.standardDurationWork] = value
                }
            } catch (e: Exception) {
                trySend(ResultState.Error(ErrorEntity(e)))
            }
        }
    }

    override fun setTypeLoco(type: LocoType): Flow<ResultState<Unit>> {
        return flowRequest {
            dataStore.edit { pref ->
                pref[PreferencesKey.typeLoco] =
                    when (type) {
                        LocoType.ELECTRIC -> LocoType.ELECTRIC.name
                        LocoType.DIESEL -> LocoType.DIESEL.name
                    }
            }
        }
    }

    override fun getTypeLoco(): Flow<LocoType> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }

            }
            .map { pref ->
                when (pref[PreferencesKey.typeLoco]) {
                    LocoType.ELECTRIC.name -> LocoType.ELECTRIC
                    LocoType.DIESEL.name -> LocoType.DIESEL
                    else -> defaultTypeLoco
                }
            }
    }
}