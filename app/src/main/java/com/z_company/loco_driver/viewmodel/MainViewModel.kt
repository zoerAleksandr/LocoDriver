package com.z_company.loco_driver.viewmodel

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parse.ParseUser
import com.z_company.core.ResultState
import com.z_company.data_local.SharedPreferenceStorage
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.use_cases.LoadCalendarFromStorage
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.use_case.RemoteRouteUseCase
import com.z_company.work_manager.UserFieldName
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val TAG = "MainViewModel_TAG"

class MainViewModel : ViewModel(), KoinComponent, DefaultLifecycleObserver {
    private val loadCalendarFromStorage: LoadCalendarFromStorage by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val remoteRouteUseCase: RemoteRouteUseCase by inject()
    private val sharedPreferenceStorage: SharedPreferenceStorage by inject()

    private var loadCalendarJob: Job? = null
    private var saveCalendarInLocalJob: Job? = null
    private var setDefaultSetting: Job? = null


    private var _inProgress = MutableLiveData(true)
    val inProgress: MutableLiveData<Boolean> get() = _inProgress

    private var _isRegistered = MutableLiveData<Boolean>()
    val isRegistered: MutableLiveData<Boolean> get() = _isRegistered

    init {
        if (sharedPreferenceStorage.tokenIsFirstAppEntry()) {
            setDefaultSettings()
        }
        loadCalendar()
        viewModelScope.launch {
            getSession()
        }
    }

    private fun enableSynchronisedRoute() {
        viewModelScope.launch {
            remoteRouteUseCase.syncBasicDataPeriodic()
        }
    }

    private fun setDefaultSettings() {
        setDefaultSetting?.cancel()
        setDefaultSetting = settingsUseCase.setDefaultSettings().launchIn(viewModelScope)
    }

    private fun loadCalendar() {
        loadCalendarJob?.cancel()
        loadCalendarJob = loadCalendarFromStorage.getMonthOfYearList().onEach { resultState ->
            if (resultState is ResultState.Success) {
                saveCalendarInLocal(resultState.data)
            }
        }.launchIn(viewModelScope)
    }

    private fun saveCalendarInLocal(calendar: List<MonthOfYear>) {
        saveCalendarInLocalJob?.cancel()
        saveCalendarInLocalJob = calendarUseCase.saveCalendar(calendar).onEach { resultState ->
            if (resultState is ResultState.Success) {
                Log.i(TAG, "production calendar is loading")
            }
        }.launchIn(viewModelScope)
    }

    private suspend fun getSession() {
        val isRegisteredJob = viewModelScope.launch {
            val session = ParseUser.getCurrentUser()
            if (session != null) {
                _isRegistered.postValue(true)
                if (session.getBoolean(UserFieldName.EMAIL_VERIFIED_FIELD_NAME_REMOTE) && !sharedPreferenceStorage.tokesIsSyncDBEnable()
                ) {
                    enableSynchronisedRoute()
                    sharedPreferenceStorage.setTokenIsSyncEnable(true)
                }

            } else {
                _isRegistered.postValue(false)
            }
        }
        delay(500L)
        isRegisteredJob.join()

        inProgress.postValue(false)
    }
}