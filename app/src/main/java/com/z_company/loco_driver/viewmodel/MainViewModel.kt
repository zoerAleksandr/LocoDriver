package com.z_company.loco_driver.viewmodel

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robokassa.library.params.PaymentParams
import com.z_company.core.ResultState
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.LoadCalendarFromStorage
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.route.viewmodel.PurchasesViewModel
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
    private val purchasesViewModel: PurchasesViewModel by inject()
    private val salarySettingUseCase: SalarySettingUseCase by inject()
    private val loadCalendarFromStorage: LoadCalendarFromStorage by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val sharedPreferenceStorage: SharedPreferencesRepositories by inject()

    private var saveCalendarInLocalJob: Job? = null
    private var setDefaultSetting: Job? = null

    val showFirstPresentation = sharedPreferenceStorage.tokenIsFirstAppEntry()
    val showUpdatePresentation =
        sharedPreferenceStorage.isShowUpdatePresentation() && !sharedPreferenceStorage.tokenIsFirstAppEntry()

    private val _appInitialized = MutableStateFlow(false)
    val appInitialized: StateFlow<Boolean> = _appInitialized.asStateFlow()

    init {
        if (sharedPreferenceStorage.tokenIsFirstAppEntry()) {
            sharedPreferenceStorage.setIsMigrated(true)
        }
        viewModelScope.launch {
            loadCalendar()
            delay(400L)
            _appInitialized.value = true
        }
    }

    fun handlePaymentReturn(params: PaymentParams?) {
        viewModelScope.launch {
            if (params != null) {
                purchasesViewModel.emitStartPayment(params, onlyCheck = true)
            } else {
                Log.d("zzz", "Параметры платежа не найдены при возврате")
            }
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
                calendarUseCase.loadFlowMonthOfYearListState().collect { monthListResult ->
                    monthListResult.forEach { monthOfYear ->
                        monthOfYearList.add(monthOfYear)
                    }
                    this.cancel()
                }
            }.join()

            val lastMonthWithTariffRate = monthOfYearList.findLast { it.tariffRate != 0.0 }

            val lastTariffRate = lastMonthWithTariffRate?.let { month ->
                salarySettingUseCase.getTariffRateFromCurrentMonthOfYear(month)
            } ?: 0.0

            Log.d("zzz", "lastMonthWithTariffRate $lastMonthWithTariffRate")

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
                                                    isReleaseDay = true,
                                                    releaseType = day.releaseType
                                                )
                                            )
                                        }
                                    }
                                    month = month.copy(days = newDays)
                                    if (month.tariffRate == 0.0) {
                                        month = month.copy(tariffRate = lastTariffRate)
                                    }
                                    newMonthOfYearList.add(month)
                                } else {
                                    newMonthOfYearList.add(monthOfYear.copy(tariffRate = lastTariffRate))
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
                        it.month == currentCalendar.get(MONTH) && it.year == currentCalendar.get(YEAR)
                    }
                    settingsUseCase.updateMonthOfYearInUserSetting(
                        searchMonthOfYear ?: calendar.first()
                    ).collect {}
                    if (sharedPreferenceStorage.tokenIsFirstAppEntry()) {
                        setDefaultSettings(searchMonthOfYear ?: calendar.first())
                    }
                }
            }
        }
    }
}
