package com.z_company.route.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ErrorEntity
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

class SalaryCalculationViewModel : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val salarySettingUseCase: SalarySettingUseCase by inject()

    private val _uiState = MutableStateFlow(SalaryCalculationUIState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsUseCase.getFlowCurrentSettingsState(),
                salarySettingUseCase.getFlowSalarySetting()
            ) { userRes, salaryRes ->
                Pair(userRes, salaryRes)
            }.collect { (userRes, salaryRes) ->

                if (userRes is ResultState.Success && salaryRes is ResultState.Success) {
                    _uiState.update { it.copy(screenState = ResultState.Loading("Пересчет...")) }
                    calculationSalary(userRes.data, salaryRes.data)
                } else {
                    val errorMsg = when {
                        userRes is ResultState.Error -> userRes.entity.message
                        salaryRes is ResultState.Error -> salaryRes.entity.message
                        else -> "Не удалось загрузить настройки"
                    }
                    _uiState.update {
                        it.copy(
                            screenState = ResultState.Error(
                                ErrorEntity(
                                    message = errorMsg ?: "Неизвестная ошибка"
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun calculationSalary(
        userSettings: UserSettings,
        salarySetting: SalarySetting
    ) {
        val currentTimeInMillis = Calendar.getInstance().timeInMillis
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val loadRouteState =
            routeUseCase.listRoutesByMonth(currentMonthOfYear, userSettings.timeZone)
                .first { it is ResultState.Success }

        if (loadRouteState is ResultState.Success) {
            val routeList = if (userSettings.isConsiderFutureRoute) {
                loadRouteState.data
            } else {
                loadRouteState.data.filter { it.basicData.timeStartWork!! < currentTimeInMillis }
            }
            val salaryCalculationHelper = SalaryCalculationHelper(
                userSettings = userSettings,
                salarySetting = salarySetting,
                routeList = routeList
            )

            // Последовательный вызов методов установки данных
            setHeaderData(currentMonthOfYear, salaryCalculationHelper)
            setToTariffTimeData(salaryCalculationHelper)
            setNightTimeData(salarySetting, salaryCalculationHelper)
            setSingleLocomotiveData(salaryCalculationHelper)
            setPassengerData(salaryCalculationHelper)
            setHolidayData(salaryCalculationHelper)
            setQualificationClassSurchargeData(salarySetting, salaryCalculationHelper)
            setSurchargeExtendedServicePhase(salaryCalculationHelper)
            setSurchargeOnePersonOperationData(salaryCalculationHelper)
            setSurchargeOnePersonOperationPassengerTrainData(salaryCalculationHelper)
            setSurchargeHarmfulnessData(salaryCalculationHelper)
            setSurchargeLongDistanceData(salaryCalculationHelper)
            setSurchargeHeavyTransData(salaryCalculationHelper)
            setZonalSurchargeData(salaryCalculationHelper)
            setOvertimeData(salaryCalculationHelper)
            setSurchargeOvertimeData(salaryCalculationHelper)
            setDistrictSurchargeData(salaryCalculationHelper)
            setNordicSurchargeData(salaryCalculationHelper)
            setAveragePaymentData(salaryCalculationHelper)
            setOtherSurchargeData(salaryCalculationHelper)
            setTotalCharged(salaryCalculationHelper)
            setRetentionData(salaryCalculationHelper)
            setToBeCredited(salaryCalculationHelper)
        } else if (loadRouteState is ResultState.Error) {
            _uiState.update { it.copy(screenState = ResultState.Error(ErrorEntity(message = loadRouteState.entity.message))) }
        } else {
            _uiState.update { it.copy(screenState = ResultState.Error(ErrorEntity(message = "Ошибка загрузки маршрутов"))) }
        }
    }

    // Метод для установки заголовочных данных (месяц, норма часов, общее время работы, тариф).
    private suspend fun setHeaderData(
        currentMonthOfYear: MonthOfYear,
        helper: SalaryCalculationHelper
    ) {
        val normaHours = currentMonthOfYear.getStandardNormaHours()
        val totalWorkTime = helper.getTotalWorkTime().first()
        val tariffText = if (currentMonthOfYear.dateSetTariffRate == null) {
            "${currentMonthOfYear.tariffRate.str()} ₽"
        } else {
            "${currentMonthOfYear.dateSetTariffRate!!.oldRate.str()} / ${currentMonthOfYear.tariffRate.str()} ₽"
        }
        _uiState.update {
            it.copy(
                month = getMonthFullText(currentMonthOfYear.month),
                normaHours = normaHours,
                totalWorkTime = totalWorkTime,
                tariffRate = tariffText
            )
        }
    }

    // Метод для установки данных по тарифному времени (часы и сумма).
    private suspend fun setToTariffTimeData(helper: SalaryCalculationHelper) {
        val workTime = helper.getWorkTimeAtTariffFlow().first()
        val money = helper.getMoneyAtWorkTimeAtTariff().first()
        _uiState.update {
            it.copy(
                paymentAtTariffHours = workTime,
                paymentAtTariffMoney = money
            )
        }
    }

    // Метод для установки данных по ночному времени (часы, процент, сумма).
    private suspend fun setNightTimeData(
        salarySetting: SalarySetting,
        helper: SalaryCalculationHelper
    ) {
        val paymentNightTimePercent = salarySetting.nightTimePercent
        val nightTime = helper.getNightTimeFlow().first()
        val money = helper.getMoneyAtNightTimeFlow().first()
        _uiState.update {
            it.copy(
                paymentNightTimeHours = nightTime,
                paymentNightTimePercent = paymentNightTimePercent,
                paymentNightTimeMoney = money
            )
        }
    }

    // Метод для установки данных по одиночному локомотиву (часы, сумма).
    private suspend fun setSingleLocomotiveData(helper: SalaryCalculationHelper) {
        val time = helper.getSingleLocomotiveTimeFlow().first()
        val money = helper.getMoneyAtSingleLocomotiveFlow().first()
        _uiState.update {
            it.copy(
                paymentAtSingleLocomotiveHours = time,
                paymentAtSingleLocomotiveMoney = money
            )
        }
    }

    // Метод для установки данных по пассажирским поездам (часы, сумма).
    private suspend fun setPassengerData(helper: SalaryCalculationHelper) {
        val passengerTime = helper.getPassengerTimeFlow().first()
        val money = helper.getMoneyAtPassengerFlow().first()
        _uiState.update {
            it.copy(
                paymentAtPassengerHours = passengerTime,
                paymentAtPassengerMoney = money
            )
        }
    }

    // Метод для установки данных по праздничным дням (часы, сумма, надбавка).
    private suspend fun setHolidayData(helper: SalaryCalculationHelper) {
        val holidayTime = helper.getHolidayTimeFlow().first()
        val money = helper.getMoneyAtHolidayFlow().first()
        _uiState.update {
            it.copy(
                paymentHolidayHours = holidayTime,
                surchargeHolidayHours = holidayTime,
                paymentHolidayMoney = money,
                surchargeHolidayMoney = money
            )
        }
    }

    // Метод для установки данных по надбавке за класс квалификации (процент, сумма).
    private suspend fun setQualificationClassSurchargeData(
        salarySetting: SalarySetting,
        helper: SalaryCalculationHelper
    ) {
        val percent = salarySetting.surchargeQualificationClass
        val money = helper.getMoneyAtQualificationClassFlow().first()
        _uiState.update {
            it.copy(
                surchargeQualificationClassPercent = percent,
                surchargeQualificationClassMoney = money
            )
        }
    }

    // Метод для установки данных по надбавке за расширенную зону обслуживания (списки часов, процентов, сумм).
    private suspend fun setSurchargeExtendedServicePhase(helper: SalaryCalculationHelper) {
        val timeList = helper.getTimeListSurchargeServicePhaseFlow().first()
        val percentList = helper.getPercentListSurchargeExtendedServicePhaseFlow().first()
        val moneyList = helper.getMoneyListSurchargeExtendedServicePhaseFlow().first()
        _uiState.update {
            it.copy(
                surchargeExtendedServicePhaseHour = timeList,
                surchargeExtendedServicePhasePercent = percentList,
                surchargeExtendedServicePhaseMoney = moneyList
            )
        }
    }

    // Метод для установки данных по надбавке за управление одним лицом (процент, сумма).
    private suspend fun setSurchargeOnePersonOperationData(helper: SalaryCalculationHelper) {
        val percent = helper.getPercentOnePersonOperationFlow().first()
        val money = helper.getMoneyOnePersonOperationFlow().first()
        _uiState.update {
            it.copy(
                onePersonOperationPercent = percent,
                onePersonOperationMoney = money
            )
        }
    }

    // Метод для установки данных по надбавке за управление одним лицом в пассажирском поезде (процент, сумма).
    private suspend fun setSurchargeOnePersonOperationPassengerTrainData(helper: SalaryCalculationHelper) {
        val percent = helper.getPercentOnePersonOperationPassengerTrainFlow().first()
        val money = helper.getMoneyOnePersonOperationPassengerTrainFlow().first()
        _uiState.update {
            it.copy(
                onePersonOperationPassengerTrainPercent = percent,
                onePersonOperationPassengerTrainMoney = money
            )
        }
    }

    // Метод для установки данных по надбавке за вредность (процент, сумма).
    private suspend fun setSurchargeHarmfulnessData(helper: SalaryCalculationHelper) {
        val percent = helper.getPercentHarmfulnessFlow().first()
        val money = helper.getMoneyHarmfulnessFlow().first()
        _uiState.update {
            it.copy(
                harmfulnessSurchargePercent = percent,
                harmfulnessSurchargeMoney = money
            )
        }
    }

    // Метод для установки данных по надбавке за дальние поезда (часы, процент, сумма).
    private suspend fun setSurchargeLongDistanceData(helper: SalaryCalculationHelper) {
        val percent = helper.getPercentLongDistanceTrainFlow().first()
        val time = helper.getTimeLongDistanceTrainFlow().first()
        val money = helper.getMoneyLongDistanceTrainFlow().first()
        _uiState.update {
            it.copy(
                surchargeLongDistanceTrainsPercent = percent,
                surchargeLongDistanceTrainsHours = time,
                surchargeLongDistanceTrainsMoney = money
            )
        }
    }

    // Метод для установки данных по надбавке за тяжелые поезда (списки часов, процентов, сумм).
    private suspend fun setSurchargeHeavyTransData(helper: SalaryCalculationHelper) {
        val timeList = helper.getTimeListSurchargeHeavyTrainsFlow().first()
        val percentList = helper.getPercentListSurchargeExtendedHeavyTrainsFlow().first()
        val moneyList = helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first()
        _uiState.update {
            it.copy(
                surchargeHeavyTransHour = timeList,
                surchargeHeavyTransPercent = percentList,
                surchargeHeavyTransMoney = moneyList
            )
        }
    }

    // Метод для установки данных по зональной надбавке (процент, сумма).
    private suspend fun setZonalSurchargeData(helper: SalaryCalculationHelper) {
        val percent = helper.getPercentZonalSurchargeFlow().first()
        val money = helper.getMoneyZonalSurchargeFlow().first()
        _uiState.update {
            it.copy(
                zonalSurchargePercent = percent,
                zonalSurchargeMoney = money
            )
        }
    }

    // Метод для установки данных по сверхурочным часам (часы, сумма).
    private suspend fun setOvertimeData(helper: SalaryCalculationHelper) {
        val overtimeHours = helper.getTimeOvertimeFlow().first()
        val overtimeMoney = helper.getMoneyOvertimeFlow().first()
        _uiState.update {
            it.copy(
                paymentAtOvertimeHours = overtimeHours,
                paymentAtOvertimeMoney = overtimeMoney
            )
        }
    }

    // Метод для установки данных по надбавке за сверхурочные (часы и суммы для 0.5 и полной ставки).
    private suspend fun setSurchargeOvertimeData(helper: SalaryCalculationHelper) {
        val surchargeAtOvertime05Hour = helper.getTimeSurchargeAtOvertime05Flow().first()
        val surchargeAtOvertime05Money = helper.getMoneySurchargeOvertime05Flow().first()
        val surchargeAtOvertimeHour = helper.getTimeSurchargeAtOvertimeFlow().first()
        val surchargeAtOvertimeMoney = helper.getMoneySurchargeOvertimeFlow().first()
        _uiState.update {
            it.copy(
                surchargeAtOvertime05Hours = surchargeAtOvertime05Hour,
                surchargeAtOvertime05Money = surchargeAtOvertime05Money,
                surchargeAtOvertimeHours = surchargeAtOvertimeHour,
                surchargeAtOvertimeMoney = surchargeAtOvertimeMoney
            )
        }
    }

    // Метод для установки данных по районной надбавке (коэффициент, сумма).
    private suspend fun setDistrictSurchargeData(helper: SalaryCalculationHelper) {
        val percent = helper.getPercentDistrictSurcharge().first()
        val money = helper.getMoneyDistrictSurcharge().first()
        _uiState.update {
            it.copy(
                districtSurchargeCoefficient = percent,
                districtSurchargeMoney = money
            )
        }
    }

    // Метод для установки данных по северной надбавке (процент, сумма).
    private suspend fun setNordicSurchargeData(helper: SalaryCalculationHelper) {
        val percent = helper.getPercentNordicSurcharge().first()
        val money = helper.getMoneyNordicSurcharge().first()
        _uiState.update {
            it.copy(
                nordicSurchargePercent = percent,
                nordicSurchargeMoney = money
            )
        }
    }

    // Метод для установки данных по средней оплате (часы, сумма).
    private suspend fun setAveragePaymentData(helper: SalaryCalculationHelper) {
        val hours = helper.getDayOffHoursFlow().first()
        val money = helper.getMoneyAverageFlow().first()
        _uiState.update {
            it.copy(
                averagePaymentHours = hours,
                averagePaymentMoney = money
            )
        }
    }

    // Метод для установки данных по прочим надбавкам (процент, сумма).
    private suspend fun setOtherSurchargeData(helper: SalaryCalculationHelper) {
        val percent = helper.getPercentOtherSurchargeFlow().first()
        val money = helper.getMoneyOtherSurchargeFlow().first()
        _uiState.update {
            it.copy(
                otherSurchargePercent = percent,
                otherSurchargeMoney = money
            )
        }
    }

    // Метод для установки общей начисленной суммы.
    private suspend fun setTotalCharged(helper: SalaryCalculationHelper) {
        val money = helper.getMoneyTotalChargedFlow().first()
        _uiState.update {
            it.copy(
                totalChargedMoney = money
            )
        }
    }

    // Метод для установки данных по удержаниям (НДФЛ, профсоюз, прочие, итого).
    private suspend fun setRetentionData(helper: SalaryCalculationHelper) {
        val moneyNDFL = helper.getMoneyNDFLRetentionFlow().first()
        val moneyUnionists = helper.getMoneyUnionistsRetentionFlow().first()
        val moneyOther = helper.getMoneyOtherRetentionFlow().first()
        val moneyTotal = helper.getMoneyTotalRetentionFlow().first()
        _uiState.update {
            it.copy(
                retentionNdfl = moneyNDFL,
                unionistsRetention = moneyUnionists,
                otherRetention = moneyOther,
                totalRetention = moneyTotal
            )
        }
    }

    // Метод для установки суммы к выдаче и завершения загрузки (установка Success).
    private suspend fun setToBeCredited(helper: SalaryCalculationHelper) {
        val toBeCredited = helper.getMoneyToBeCredited().first()
        _uiState.update {
            it.copy(
                toBeCredited = toBeCredited,
                screenState = ResultState.Success(Unit)
            )
        }
    }
}