package com.z_company.loco_driver.viewmodel

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parse.ParseUser
import com.z_company.core.ResultState
import com.z_company.data_remote.AuthUseCase
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.use_cases.LoadCalendarFromStorage
import com.z_company.domain.use_cases.CalendarUseCase
import io.appwrite.models.Session
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
    private val authUseCase: AuthUseCase by inject()

    private var loadCalendarJob: Job? = null
    private var saveCalendarInLocalJob: Job? = null
    private var getSessionJob: Job? = null


    var _inProgress = MutableLiveData(true)
    val inProgress: MutableLiveData<Boolean> get() = _inProgress

    private var _isRegistered = MutableLiveData<Boolean>()
    val isRegistered: MutableLiveData<Boolean> get() = _isRegistered

    init {
        loadCalendar()
        viewModelScope.launch {
            getSession()
        }
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
            Log.d("ZZZ", "session = ${session}")
            if (session != null) {
                _isRegistered.postValue(true)
            } else {
                _isRegistered.postValue(false)
            }
        }
        delay(500L)
        isRegisteredJob.join()

        inProgress.postValue(false)
    }
}