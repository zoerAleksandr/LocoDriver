package com.z_company.loco_driver.viewmodel

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.data_remote.AuthUseCase
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.use_cases.LoadCalendarFromStorage
import com.z_company.domain.use_cases.CalendarUseCase
import io.appwrite.models.Session
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    private var _session = MutableLiveData<Session?>()
    val session : MutableLiveData<Session?> get() = _session

    init {
        loadCalendar()
        getSession()
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
    private fun getSession(){
        getSessionJob?.cancel()
        getSessionJob = authUseCase.getSession().onEach { resultState ->
            if (resultState is ResultState.Success) {
                _session.postValue(resultState.data)
                inProgress.postValue(false)
            } else if (resultState is ResultState.Error) {
                if (resultState.entity.throwable is java.net.UnknownHostException){
                    // TODO нет интернета
                }
                _session.postValue(null)
                inProgress.postValue(false)
            }
        }.launchIn(viewModelScope)
    }
}