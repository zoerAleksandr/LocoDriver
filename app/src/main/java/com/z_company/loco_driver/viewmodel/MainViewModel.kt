package com.z_company.loco_driver.viewmodel

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.SessionManager
import com.z_company.core.ResultState
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.LoadCalendarFromStorage
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.use_case.RemoteRouteUseCase
import com.z_company.use_case.RuStoreUseCase
import com.z_company.use_case.SubscriptionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar
import java.util.Calendar.MONTH
import java.util.Calendar.YEAR

private const val TAG = "MainViewModel_TAG"

class MainViewModel : ViewModel(), KoinComponent, DefaultLifecycleObserver {
    private val salarySettingUseCase: SalarySettingUseCase by inject()
    private val loadCalendarFromStorage: LoadCalendarFromStorage by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val remoteRouteUseCase: RemoteRouteUseCase by inject()
    private val sharedPreferenceStorage: SharedPreferencesRepositories by inject()
    private val ruStoreUseCase: RuStoreUseCase by inject()
    private val subscriptionHelper: SubscriptionHelper by inject()

    private var saveCalendarInLocalJob: Job? = null
    private var setDefaultSetting: Job? = null

    val showFirstPresentation = sharedPreferenceStorage.tokenIsFirstAppEntry()
    val showUpdatePresentation =
        sharedPreferenceStorage.isShowUpdatePresentation() && !sharedPreferenceStorage.tokenIsFirstAppEntry()

    private val _appInitialized = MutableStateFlow(false)
    val appInitialized: StateFlow<Boolean> = _appInitialized.asStateFlow()

    private val sessionManager: SessionManager by inject()

    init {
        viewModelScope.launch {
            sessionManager.updateLoggedIn() // ← мгновенно + запускает sync если нужно
        }

        viewModelScope.launch {
            subscriptionHelper.restorePurchases()
        }
        viewModelScope.launch {
            loadCalendar()
            delay(400L) // минимальное время сплеша
            _appInitialized.value = true
        }
    }

    private fun enableSynchronisedRoute() {
        viewModelScope.launch {
            remoteRouteUseCase.syncBasicDataPeriodic().collect {}
        }
    }

    private fun setDefaultSettings(currentMonthOfYear: MonthOfYear) {
        setDefaultSetting?.cancel()
        setDefaultSetting =
            settingsUseCase.setDefaultSettings(currentMonthOfYear).launchIn(viewModelScope)
    }

    private fun loadCalendar() {
        viewModelScope.launch(Dispatchers.IO) {
            val monthOfYearList = mutableListOf<MonthOfYear>()
            this.launch {
                // загрузил старые и сохранил их в список
                calendarUseCase.loadFlowMonthOfYearListState().collect { monthListResult ->
                    monthListResult.forEach { monthOfYear ->
                        monthOfYearList.add(monthOfYear)
                    }
                    this.cancel()
                }
            }.join()
            // проверил, если этот месяц ранее был сохранен, проверил помечен ли он isRelease
            // оставляем это поле без изменений, остальное обновляем, если месяц ранее не сохранялся,
            // тогда записываем его в room без изменений
            this.launch {
                if (monthOfYearList.isEmpty()) {
                    Log.d("zzz", "monthOfYearList.isEmpty()")
                } else {
                    Log.d("zzz", "monthOfYearList.isNotEmpty()")
                }
                val lastMonthWithTariffRate = monthOfYearList.findLast { it.tariffRate != 0.0 }

                val lastTariffRate = lastMonthWithTariffRate?.let { month ->
                    salarySettingUseCase.getTariffRateFromCurrentMonthOfYear(month)
                } ?: 0.0

                Log.d("zzz", "lastMonthWithTariffRate $lastMonthWithTariffRate")

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
                                                    isReleaseDay = true,
                                                    releaseType = day.releaseType
                                                )
                                            )
                                        }
                                    }
                                    month = month.copy(
                                        days = newDays
                                    )
                                    // если тариф не сохранялся, то добавить последний
                                    // если указан, то оставить без изменений
                                    if (month.tariffRate == 0.0) {
                                        month = month.copy(
                                            tariffRate = lastTariffRate
                                        )
                                    }
                                    newMonthOfYearList.add(month)
                                } else {
                                    // добавить последний тариф
                                    val monthOfYearWithTariffRate = monthOfYear.copy(
                                        tariffRate = lastTariffRate
                                    )
                                    newMonthOfYearList.add(monthOfYearWithTariffRate)
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
                    val currentCalendar = Calendar.getInstance()
                    val searchMonthOfYear = calendar.find {
                        it.month == currentCalendar.get(MONTH) && it.year == currentCalendar.get(
                            YEAR
                        )
                    }
                    settingsUseCase.updateMonthOfYearInUserSetting(
                        searchMonthOfYear ?: calendar.first()
                    )
                        .collect {}
                    if (sharedPreferenceStorage.tokenIsFirstAppEntry()) {
                        setDefaultSettings(searchMonthOfYear ?: calendar.first())
                    }
                }
            }
        }
    }

    // при вызове метода происходит утечка памяти на Pixel API 34 Android 14
//    private fun syncRuStoreSubscription() {
//        var job: Job? = null
//        try {
//            billingClient.purchases.getPurchases()
//                .addOnSuccessListener { purchases ->
//                    viewModelScope.launch {
//                        purchases.forEach { purchase ->
//                            job?.cancel()
//                            job = this.launch(Dispatchers.IO) {
//                                if (purchase.purchaseState == PurchaseState.CONFIRMED) {
//                                    ruStoreUseCase.getExpiryTimeMillis(
//                                        productId = purchase.productId,
//                                        subscriptionToken = purchase.subscriptionToken ?: ""
//                                    ).collect { resultState ->
//                                        if (resultState is ResultState.Success) {
//                                            sharedPreferenceStorage.setSubscriptionExpiration(
//                                                resultState.data
//                                            )
//                                            job?.cancel()
//                                        }
//                                    }
//                                }
//                            }
//                            job.join()
//                        }
//                    }
//
//                }
//                .addOnFailureListener {
//                    Log.w(TAG, "${it.message}")
//                }
//        } catch (e: Exception) {
//            Log.w(TAG, "${e.message}")
//        }
//    }

//    private suspend fun getSession() {
//        val isRegisteredJob = viewModelScope.launch {
//            val session = ParseUser.getCurrentUser()
//            if (session != null) {
//                _isRegistered.postValue(true)
//                if (session.getBoolean(UserFieldName.EMAIL_VERIFIED_FIELD_NAME_REMOTE)) {
//                    enableSynchronisedRoute()
//                }
//
//            } else {
//                _isRegistered.postValue(false)
//            }
//        }
//        delay(500L)
//        isRegisteredJob.join()
//
//        inProgress.postValue(false)
//    }
}