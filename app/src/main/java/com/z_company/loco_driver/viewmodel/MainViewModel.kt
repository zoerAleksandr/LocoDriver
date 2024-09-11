package com.z_company.loco_driver.viewmodel

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parse.ParseUser
import com.z_company.core.ResultState
import com.z_company.data_local.SharedPreferenceStorage
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.use_cases.LoadCalendarFromStorage
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.route.extention.getEndTimeSubscription
import com.z_company.use_case.RemoteRouteUseCase
import com.z_company.work_manager.UserFieldName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.rustore.sdk.billingclient.RuStoreBillingClient
import java.util.Calendar

private const val TAG = "MainViewModel_TAG"

class MainViewModel : ViewModel(), KoinComponent, DefaultLifecycleObserver {
    private val loadCalendarFromStorage: LoadCalendarFromStorage by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val remoteRouteUseCase: RemoteRouteUseCase by inject()
    private val sharedPreferenceStorage: SharedPreferenceStorage by inject()
    private val billingClient: RuStoreBillingClient by inject()

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
        syncRuStoreSubscription()
        viewModelScope.launch {
            loadCalendar()
            delay(1000L)
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
        viewModelScope.launch(Dispatchers.IO) {
            val monthOfYearList = mutableListOf<MonthOfYear>()
            this.launch {
                // загрузил старые и сохранил их в список
                calendarUseCase.loadMonthOfYearList().collect { monthListResult ->
                    if (monthListResult is ResultState.Success) {
                        monthListResult.data.forEach { monthOfYear ->
                            monthOfYearList.add(monthOfYear)
                        }
                        this.cancel()
                    }
                }
            }.join()
            // проверил, если этот месяц ранее был сохранен, проверил помечен ли он isRelease
            // оставляем это поле без изменений, остальное обновляем, если месяц ранее не сохранялся,
            // тогда записываем его в room без изменений
            this.launch {
                loadCalendarFromStorage.getMonthOfYearList()
                    .collect { resultState ->
                        if (resultState is ResultState.Success) {
                            val newMonthOfYearList = mutableListOf<MonthOfYear>()
                            resultState.data.forEach { monthOfYear ->
                                var month =
                                    monthOfYearList.find { it.month == monthOfYear.month && it.year == monthOfYear.year }
                                val newDays = mutableListOf<Day>()
                                if (month != null) {
                                    month.days.forEachIndexed { index, day ->
                                        if (!day.isReleaseDay) {
                                            newDays.add(monthOfYear.days[index])
                                        } else {
                                            newDays.add(
                                                monthOfYear.days[index].copy(
                                                    isReleaseDay = true
                                                )
                                            )
                                        }
                                    }
                                    month = month.copy(days = newDays)

                                    newMonthOfYearList.add(month)
                                } else {
                                    newMonthOfYearList.add(monthOfYear)
                                }
                            }
                            saveCalendarInLocal(newMonthOfYearList)
                            this.cancel()
                        }
                    }
            }
        }
    }

    private fun saveCalendarInLocal(calendar: List<MonthOfYear>) {
        saveCalendarInLocalJob?.cancel()
        saveCalendarInLocalJob = viewModelScope.launch {
            this.launch {
                calendarUseCase.clearCalendar().collect { clearResult ->
                    if (clearResult is ResultState.Success) {
                        this.cancel()
                    }
                }
            }.join()

            calendarUseCase.saveCalendar(calendar).collect { resultState ->
                if (resultState is ResultState.Success) {
                    Log.i(TAG, "production calendar is loading")
                }
            }
        }
    }

    // при вызове метода происходит утечка памяти
    private fun syncRuStoreSubscription() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val currentTimeInMillis = Calendar.getInstance().timeInMillis
                    val purchases = billingClient.purchases.getPurchases().await()
                    var maxEndTime = 0L
                    purchases.forEach { purchase ->
                        val purchaseEndTime =
                            purchase.getEndTimeSubscription(billingClient).first()
                        if (purchaseEndTime > maxEndTime) {
                            maxEndTime = purchaseEndTime
                        }
                    }
                    if (maxEndTime > currentTimeInMillis) {
                        sharedPreferenceStorage.setSubscriptionExpiration(maxEndTime)
                    }

                } catch (e: Exception) {
                    Log.w(TAG, "${e.message}")
                }
            }
        }
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