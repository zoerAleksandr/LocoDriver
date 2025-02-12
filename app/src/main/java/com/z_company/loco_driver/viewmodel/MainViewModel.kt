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
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.use_case.RemoteRouteUseCase
import com.z_company.use_case.RuStoreUseCase
import com.z_company.work_manager.UserFieldName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.rustore.sdk.billingclient.RuStoreBillingClient
import ru.rustore.sdk.billingclient.model.purchase.PurchaseState
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
    private val sharedPreferenceStorage: SharedPreferenceStorage by inject()
    private val billingClient: RuStoreBillingClient by inject()
    private val ruStoreUseCase: RuStoreUseCase by inject()

    private var saveCalendarInLocalJob: Job? = null
    private var setDefaultSetting: Job? = null

    val showFirstPresentation = sharedPreferenceStorage.tokenIsFirstAppEntry()
    val showUpdatePresentation =
        sharedPreferenceStorage.isShowUpdatePresentation() && !sharedPreferenceStorage.tokenIsFirstAppEntry()

    private var _inProgress = MutableLiveData(true)
    val inProgress: MutableLiveData<Boolean> get() = _inProgress

    private var _isRegistered = MutableLiveData<Boolean>()
    val isRegistered: MutableLiveData<Boolean> get() = _isRegistered

    init {
        viewModelScope.launch {
            syncRuStoreSubscription()
            loadCalendar()
            delay(1000L)
            getSession()
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
                val salarySetting = this.async { salarySettingUseCase.getSalarySetting() }.await()
                val currentTariffRate = salarySetting.tariffRate

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
                                    month = month.copy(
                                        days = newDays
                                    )
                                    // если тариф не сохранялся, то добавить последний
                                    // если указан, то оставить без изменений
                                    if (month.tariffRate == 0.0) {
                                        month = month.copy(
                                            tariffRate = currentTariffRate
                                        )
                                    }
                                    newMonthOfYearList.add(month)
                                } else {
                                    // добавить последний тариф
                                    val monthOfYearWithTariffRate = monthOfYear.copy(
                                        tariffRate = currentTariffRate
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
    private suspend fun syncRuStoreSubscription() {
        var job: Job? = null
        try {
            billingClient.purchases.getPurchases()
                .addOnSuccessListener { purchases ->
                    viewModelScope.launch {
                        purchases.forEach { purchase ->
                            job?.cancel()
                            job = this.launch(Dispatchers.IO) {
                                if (purchase.purchaseState == PurchaseState.CONFIRMED) {
                                    ruStoreUseCase.getExpiryTimeMillis(
                                        productId = purchase.productId,
                                        subscriptionToken = purchase.subscriptionToken ?: ""
                                    ).collect { resultState ->
                                        if (resultState is ResultState.Success) {
                                            sharedPreferenceStorage.setSubscriptionExpiration(
                                                resultState.data
                                            )
                                            job?.cancel()
                                        }
                                    }
                                }
                            }
                            job?.join()
                        }
                    }

                }
                .addOnFailureListener {
                    Log.w(TAG, "${it.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "${e.message}")
        }
    }

    private suspend fun getSession() {
        val isRegisteredJob = viewModelScope.launch {
            val session = ParseUser.getCurrentUser()
            if (session != null) {
                _isRegistered.postValue(true)
                if (session.getBoolean(UserFieldName.EMAIL_VERIFIED_FIELD_NAME_REMOTE)) {
                    enableSynchronisedRoute()
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