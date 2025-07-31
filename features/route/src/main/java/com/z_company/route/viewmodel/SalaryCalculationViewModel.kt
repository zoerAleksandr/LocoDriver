package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.util.MonthFullText.getMonthFullText
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.SalarySetting
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.UtilForMonthOfYear.getStandardNormaHours
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.str
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SalaryCalculationViewModel : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val salarySettingUseCase: SalarySettingUseCase by inject()

    private var userSettings: UserSettings? = null
    private var salarySetting: SalarySetting? = null
    private lateinit var salaryCalculationHelper: SalaryCalculationHelper

    private val _uiState = MutableStateFlow(SalaryCalculationUIState())
    val uiState = _uiState.asStateFlow()

    private fun loadSalarySetting() {
        viewModelScope.launch {
            salarySettingUseCase.getFlowSalarySetting().collect { result ->
                if (result is ResultState.Success) {
                    salarySetting = result.data
                    loadUserSetting()
                }
            }
        }
    }

    private fun loadUserSetting() {
        viewModelScope.launch {
            settingsUseCase.getFlowCurrentSettingsState().collect { result ->
                if (result is ResultState.Success) {
                    userSettings = result.data
                    calculationSalary()
                }
            }
        }
    }

    private fun calculationSalary() {
        val currentTimeInMillis = Calendar.getInstance().timeInMillis
        userSettings?.let { userSettings ->
            salarySetting?.let { salarySetting ->
                viewModelScope.launch {
                    val currentMonthOfYear = userSettings.selectMonthOfYear
                    routeUseCase.listRoutesByMonth(currentMonthOfYear, userSettings.timeZone)
                        .collect { loadRouteState ->
                            if (loadRouteState is ResultState.Success) {
                                val routeList = if (userSettings.isConsiderFutureRoute) {
                                    loadRouteState.data
                                } else {
                                    loadRouteState.data.filter { it.basicData.timeStartWork!! < currentTimeInMillis }
                                }
                                salaryCalculationHelper = SalaryCalculationHelper(
                                    userSettings = userSettings,
                                    salarySetting = salarySetting,
                                    routeList = routeList
                                )
                                salaryCalculationHelper?.let {
                                    setHeaderData(currentMonthOfYear)
                                    setToTariffTimeData()
                                    setNightTimeData(salarySetting)
                                    setSingleLocomotiveData()
                                    setPassengerData()
                                    setHolidayData()
                                    setQualificationClassSurchargeData(salarySetting)
                                    setSurchargeExtendedServicePhase()
                                    setSurchargeOnePersonOperationData()
                                    setSurchargeOnePersonOperationPassengerTrainData()
                                    setSurchargeHarmfulnessData()
                                    setSurchargeLongDistanceData()
                                    setSurchargeHeavyTransData()
                                    setZonalSurchargeData()
                                    setOvertimeData()
                                    setSurchargeOvertimeData()
                                    setDistrictSurchargeData()
                                    setNordicSurchargeData()
                                    setAveragePaymentData()
                                    setOtherSurchargeData()
                                    setTotalCharged()
                                    setRetentionData()
                                    setToBeCredited()
                                }
                            }
                        }
                }
            }
        }
    }

    private fun setHeaderData(currentMonthOfYear: MonthOfYear) {
        viewModelScope.launch(Dispatchers.IO) {
            val normaHours = currentMonthOfYear.getStandardNormaHours()
            val totalWorkTime = salaryCalculationHelper.getTotalWorkTime().first()
            val tariffText = if (currentMonthOfYear.dateSetTariffRate == null) {
                "${currentMonthOfYear.tariffRate.str()} ₽"
            } else {
                "${currentMonthOfYear.dateSetTariffRate!!.oldRate.str()} / ${currentMonthOfYear.tariffRate.str()} ₽"
            }
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        month = getMonthFullText(currentMonthOfYear.month),
                        normaHours = normaHours,
                        totalWorkTime = totalWorkTime,
                        tariffRate = tariffText
                    )
                }
            }
        }
    }

    private fun setToTariffTimeData() {
        viewModelScope.launch {
            combine(
                salaryCalculationHelper.getWorkTimeAtTariffFlow(),
                salaryCalculationHelper.getMoneyAtWorkTimeAtTariff()
            ) { workTime, money ->
                _uiState.update {
                    it.copy(
                        paymentAtTariffHours = workTime,
                        paymentAtTariffMoney = money
                    )
                }
            }.collect()
        }
    }

    private fun setNightTimeData(salarySetting: SalarySetting) {
        val paymentNightTimePercent = salarySetting.nightTimePercent
        viewModelScope.launch {
            combine(
                salaryCalculationHelper.getNightTimeFlow(),
                salaryCalculationHelper.getMoneyAtNightTimeFlow()
            ) { nightTime, money ->
                _uiState.update {
                    it.copy(
                        paymentNightTimeHours = nightTime,
                        paymentNightTimePercent = paymentNightTimePercent,
                        paymentNightTimeMoney = money,
                    )
                }
            }.collect()
        }
    }

    private fun setSingleLocomotiveData() {
        viewModelScope.launch {
            combine(
                salaryCalculationHelper.getSingleLocomotiveTimeFlow(),
                salaryCalculationHelper.getMoneyAtSingleLocomotiveFlow()
            ) { time, money ->
                _uiState.update {
                    it.copy(
                        paymentAtSingleLocomotiveHours = time,
                        paymentAtSingleLocomotiveMoney = money
                    )
                }
            }.collect()
        }
    }

    private fun setPassengerData() {
        viewModelScope.launch {
            combine(
                salaryCalculationHelper.getPassengerTimeFlow(),
                salaryCalculationHelper.getMoneyAtPassengerFlow()
            ) { passengerTime, money ->
                _uiState.update {
                    it.copy(
                        paymentAtPassengerHours = passengerTime,
                        paymentAtPassengerMoney = money,
                    )
                }
            }.collect()
        }
    }

    private fun setHolidayData() {
        viewModelScope.launch {
            combine(
                salaryCalculationHelper.getHolidayTimeFlow(),
                salaryCalculationHelper.getMoneyAtHolidayFlow()
            ) { holidayTime, money ->
                _uiState.update {
                    it.copy(
                        paymentHolidayHours = holidayTime,
                        surchargeHolidayHours = holidayTime,
                        paymentHolidayMoney = money,
                        surchargeHolidayMoney = money,
                    )
                }
            }.collect()
        }
    }

    private fun setQualificationClassSurchargeData(salarySetting: SalarySetting) {
        val percent = salarySetting.surchargeQualificationClass
        viewModelScope.launch {
            salaryCalculationHelper.getMoneyAtQualificationClassFlow().collect { money ->
                _uiState.update {
                    it.copy(
                        surchargeQualificationClassPercent = percent,
                        surchargeQualificationClassMoney = money
                    )
                }
            }
        }
    }

    private fun setSurchargeExtendedServicePhase() {
        viewModelScope.launch {
            combine(
                salaryCalculationHelper.getTimeListSurchargeServicePhaseFlow(),
                salaryCalculationHelper.getPercentListSurchargeExtendedServicePhaseFlow(),
                salaryCalculationHelper.getMoneyListSurchargeExtendedServicePhaseFlow()
            )
            { timeList, percentList, moneyList ->
                _uiState.update {
                    it.copy(
                        surchargeExtendedServicePhaseHour = timeList,
                        surchargeExtendedServicePhasePercent = percentList,
                        surchargeExtendedServicePhaseMoney = moneyList
                    )
                }
            }.collect()

        }
    }

    private fun setSurchargeOnePersonOperationData() {
        viewModelScope.launch {
            combine(
                salaryCalculationHelper.getPercentOnePersonOperationFlow(),
                salaryCalculationHelper.getMoneyOnePersonOperationFlow()
            ) { percent, money ->
                _uiState.update {
                    it.copy(
                        onePersonOperationPercent = percent,
                        onePersonOperationMoney = money
                    )
                }
            }.collect()
        }
    }

    private fun setSurchargeOnePersonOperationPassengerTrainData() {
        viewModelScope.launch {
            combine(
                salaryCalculationHelper.getPercentOnePersonOperationPassengerTrainFlow(),
                salaryCalculationHelper.getMoneyOnePersonOperationPassengerTrainFlow()
            ) { percent, money ->
                _uiState.update {
                    it.copy(
                        onePersonOperationPassengerTrainPercent = percent,
                        onePersonOperationPassengerTrainMoney = money
                    )
                }
            }.collect()
        }
    }

    private fun setSurchargeHarmfulnessData() {
        viewModelScope.launch {
            combine(
                salaryCalculationHelper.getPercentHarmfulnessFlow(),
                salaryCalculationHelper.getMoneyHarmfulnessFlow()
            ) { percent, money ->
                _uiState.update {
                    it.copy(
                        harmfulnessSurchargePercent = percent,
                        harmfulnessSurchargeMoney = money
                    )
                }
            }.collect()
        }
    }

    private fun setSurchargeLongDistanceData() {
        viewModelScope.launch {
            combine(
                salaryCalculationHelper.getPercentLongDistanceTrainFlow(),
                salaryCalculationHelper.getTimeLongDistanceTrainFlow(),
                salaryCalculationHelper.getMoneyLongDistanceTrainFlow()
            ) { percent, time, money ->
                _uiState.update {
                    it.copy(
                        surchargeLongDistanceTrainsPercent = percent,
                        surchargeLongDistanceTrainsHours = time,
                        surchargeLongDistanceTrainsMoney = money
                    )
                }
            }.collect()
        }
    }

    private fun setSurchargeHeavyTransData() {
        viewModelScope.launch {
            combine(
                salaryCalculationHelper.getTimeListSurchargeHeavyTrainsFlow(),
                salaryCalculationHelper.getPercentListSurchargeExtendedHeavyTrainsFlow(),
                salaryCalculationHelper.getMoneyListSurchargeExtendedHeavyTrainsFlow()
            ) { timeList, percentList, moneyList ->
                _uiState.update {
                    it.copy(
                        surchargeHeavyTransHour = timeList,
                        surchargeHeavyTransPercent = percentList,
                        surchargeHeavyTransMoney = moneyList
                    )
                }
            }.collect()
        }
    }

    private fun setZonalSurchargeData() {
        viewModelScope.launch {
            combine(
                salaryCalculationHelper.getPercentZonalSurchargeFlow(),
                salaryCalculationHelper.getMoneyZonalSurchargeFlow()
            ) { percent, money ->
                _uiState.update {
                    it.copy(
                        zonalSurchargePercent = percent,
                        zonalSurchargeMoney = money,
                    )
                }
            }.collect()
        }
    }

    private fun setOvertimeData() {
        viewModelScope.launch {
            combine(
                salaryCalculationHelper.getTimeOvertimeFlow(),
                salaryCalculationHelper.getMoneyOvertimeFlow()
            ) { overtimeHours, overtimeMoney ->
                _uiState.update {
                    it.copy(
                        paymentAtOvertimeHours = overtimeHours,
                        paymentAtOvertimeMoney = overtimeMoney,
                    )
                }
            }.collect()
        }
    }

    private fun setSurchargeOvertimeData() {
        viewModelScope.launch {
            combine(
                salaryCalculationHelper.getTimeSurchargeAtOvertime05Flow(),
                salaryCalculationHelper.getMoneySurchargeOvertime05Flow(),
                salaryCalculationHelper.getTimeSurchargeAtOvertimeFlow(),
                salaryCalculationHelper.getMoneySurchargeOvertimeFlow()
            ) { surchargeAtOvertime05Hour, surchargeAtOvertime05Money,
                surchargeAtOvertimeHour, surchargeAtOvertimeMoney ->
                _uiState.update {
                    it.copy(
                        surchargeAtOvertime05Hours = surchargeAtOvertime05Hour,
                        surchargeAtOvertime05Money = surchargeAtOvertime05Money,
                        surchargeAtOvertimeHours = surchargeAtOvertimeHour,
                        surchargeAtOvertimeMoney = surchargeAtOvertimeMoney
                    )
                }
            }.collect()
        }
    }

    private fun setDistrictSurchargeData() {
        viewModelScope.launch {
            combine(
                salaryCalculationHelper.getPercentDistrictSurcharge(),
                salaryCalculationHelper.getMoneyDistrictSurcharge()
            ) { percent, money ->
                _uiState.update {
                    it.copy(
                        districtSurchargeCoefficient = percent,
                        districtSurchargeMoney = money
                    )
                }
            }.collect()
        }
    }

    private fun setNordicSurchargeData() {
        viewModelScope.launch {
            combine(
                salaryCalculationHelper.getPercentNordicSurcharge(),
                salaryCalculationHelper.getMoneyNordicSurcharge()
            ) { percent, money ->
                _uiState.update {
                    it.copy(
                        nordicSurchargePercent = percent,
                        nordicSurchargeMoney = money
                    )
                }
            }.collect()
        }
    }

    private fun setAveragePaymentData() {
        viewModelScope.launch {
            combine(
                salaryCalculationHelper.getDayOffHoursFlow(),
                salaryCalculationHelper.getMoneyAverageFlow()
            ) { hours, money ->
                _uiState.update {
                    it.copy(
                        averagePaymentHours = hours,
                        averagePaymentMoney = money
                    )
                }
            }.collect()
        }
    }

    private fun setOtherSurchargeData() {
        viewModelScope.launch {
            combine(
                salaryCalculationHelper.getPercentOtherSurchargeFlow(),
                salaryCalculationHelper.getMoneyOtherSurchargeFlow()
            ) { percent, money ->
                _uiState.update {
                    it.copy(
                        otherSurchargePercent = percent,
                        otherSurchargeMoney = money
                    )
                }
            }.collect()
        }
    }

    private fun setTotalCharged() {
        viewModelScope.launch {
            salaryCalculationHelper.getMoneyTotalChargedFlow().collect { money ->
                _uiState.update {
                    it.copy(
                        totalChargedMoney = money
                    )
                }
            }
        }
    }

    private fun setRetentionData() {
        viewModelScope.launch {
            combine(
                salaryCalculationHelper.getMoneyNDFLRetentionFlow(),
                salaryCalculationHelper.getMoneyUnionistsRetentionFlow(),
                salaryCalculationHelper.getMoneyOtherRetentionFlow(),
                salaryCalculationHelper.getMoneyTotalRetentionFlow()
            ) { moneyNDFL, moneyUnionists, moneyOther, moneyTotal ->
                _uiState.update {
                    it.copy(
                        retentionNdfl = moneyNDFL,
                        unionistsRetention = moneyUnionists,
                        otherRetention = moneyOther,
                        totalRetention = moneyTotal,
                    )
                }
            }.collect()
        }
    }

    private fun setToBeCredited() {
        viewModelScope.launch {
            val toBeCredited = salaryCalculationHelper.getMoneyToBeCredited().first()
            _uiState.update {
                it.copy(
                    toBeCredited = toBeCredited,
                    screenState = ResultState.Success(Unit)
                )
            }
        }
    }

    fun loadData() {
        _uiState.update {
            it.copy(
                screenState = ResultState.Loading("Выполняем расчет...")
            )
        }
        loadUserSetting()
        loadSalarySetting()
    }
}