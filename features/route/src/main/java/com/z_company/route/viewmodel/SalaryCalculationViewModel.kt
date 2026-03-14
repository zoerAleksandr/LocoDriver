package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.MonthFullText.getMonthFullText
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.entities.UtilForMonthOfYear.getStandardNormaHours
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.z_company.core.sendToSentry
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar
import kotlin.String

class SalaryCalculationViewModel : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val salarySettingUseCase: SalarySettingUseCase by inject()
    private val _userSetting = MutableStateFlow(UserSettings())
    val userSetting = _userSetting.asStateFlow()
    private var job: Job? = null

    private val _uiState = MutableStateFlow(SalaryCalculationUIState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                combine(
                    settingsUseCase.getUserSettingFlow(),
                    salarySettingUseCase.salarySettingFlow()
                ) { userRes, salaryRes ->
                    _userSetting.value = userRes
                    Pair(userRes, salaryRes)
                }.collectLatest { (userRes, salaryRes) ->
                    _uiState.update { it.copy(screenState = ResultState.Loading("Пересчет...")) }
                    calculationSalary(userRes, salaryRes)
                }
            } catch (e: Exception) {
                e.sendToSentry("SalaryCalculationViewModel", "init")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
        viewModelScope.coroutineContext.cancelChildren()  // Отмена всех дочерних корутин
    }

    fun convertTimeToStringFormat(timeToLong: Long?): String {
        userSetting.value.let { settings ->
            return if (settings.isDecimalTime) {
                ConverterLongToTime.getTimeInStringDecimalFormat(timeToLong)
            } else {
                ConverterLongToTime.getTimeInStringFormat(timeToLong)
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
            job?.cancel()
            // Параллельный вызов методов с ожиданием завершения
            job = CoroutineScope(Dispatchers.IO).launch {
                // Список асинхронных задач, каждая возвращает PartialState
                val tasks = listOf(
                    async { setHeaderData(currentMonthOfYear, salaryCalculationHelper) },
                    async { setToTariffTimeData(salaryCalculationHelper) },
                    async { setNightTimeData(salarySetting, salaryCalculationHelper) },
                    async { setSingleLocomotiveData(salaryCalculationHelper) },
                    async { setPassengerData(salaryCalculationHelper) },
                    async { setHolidayData(salaryCalculationHelper) },
                    async {
                        setQualificationClassSurchargeData(
                            salarySetting,
                            salaryCalculationHelper
                        )
                    },
                    async { setSurchargeExtendedServicePhase(salaryCalculationHelper) },
                    async { setSurchargeOnePersonOperationData(salaryCalculationHelper) },
                    async { setSurchargeOnePersonOperationPassengerTrainData(salaryCalculationHelper) },
                    async { setSurchargeHarmfulnessData(salaryCalculationHelper) },
                    async { setSurchargeLongDistanceData(salaryCalculationHelper) },
                    async { setSurchargeHeavyTransData(salaryCalculationHelper) },
                    async { setSurchargeLongTrainData(salaryCalculationHelper) },
                    async { setSurchargeDoubledTrainData(salaryCalculationHelper) },
                    async { setZonalSurchargeData(salaryCalculationHelper) },
                    async { setOvertimeData(salaryCalculationHelper) },
                    async { setSurchargeOvertimeData(salaryCalculationHelper) },
                    async { setDistrictSurchargeData(salaryCalculationHelper) },
                    async { setNordicSurchargeData(salaryCalculationHelper) },
                    async { setAveragePaymentData(salaryCalculationHelper) },
                    async { setCaringForDisableChildrenPaymentData(salaryCalculationHelper) },
                    async { setOtherSurchargeData(salaryCalculationHelper) },
                    async { setRestInExcessOfTheNormData(salaryCalculationHelper) },
                    async { setTotalCharged(salaryCalculationHelper) },
                    async { setRetentionData(salaryCalculationHelper) },
                    async { setToBeCredited(salaryCalculationHelper) }
                )

                // Дожидаемся завершения всех задач и собираем PartialState в один объект
                val allPartialStates = tasks.awaitAll()  // List<PartialState>

                // Объединяем все PartialState в один полный (используем fold для слияния)
                val combinedPartial = allPartialStates.fold(PartialState()) { acc, partial ->

                    acc.copy(
                        // Пояснение: copy копирует объект, заменяя только непустые поля из partial
                        month = partial.month,
                        normaHours = partial.normaHours ?: acc.normaHours,
                        totalWorkTime = partial.totalWorkTime ?: acc.totalWorkTime,
                        tariffRate = partial.tariffRate ?: acc.tariffRate,
                        paymentAtTariffHours = partial.paymentAtTariffHours
                            ?: acc.paymentAtTariffHours,
                        paymentAtTariffMoney = partial.paymentAtTariffMoney
                            ?: acc.paymentAtTariffMoney,
                        paymentAtPassengerHours = partial.paymentAtPassengerHours
                            ?: acc.paymentAtPassengerHours,
                        paymentAtPassengerMoney = partial.paymentAtPassengerMoney
                            ?: acc.paymentAtPassengerMoney,
                        paymentAtSingleLocomotiveHours = partial.paymentAtSingleLocomotiveHours
                            ?: acc.paymentAtSingleLocomotiveHours,
                        paymentAtSingleLocomotiveMoney = partial.paymentAtSingleLocomotiveMoney
                            ?: acc.paymentAtSingleLocomotiveMoney,
                        paymentHolidayHours = partial.paymentHolidayHours
                            ?: acc.paymentHolidayHours,
                        paymentHolidayMoney = partial.paymentHolidayMoney
                            ?: acc.paymentHolidayMoney,
                        surchargeHolidayHours = partial.surchargeHolidayHours
                            ?: acc.surchargeHolidayHours,
                        surchargeHolidayMoney = partial.surchargeHolidayMoney
                            ?: acc.surchargeHolidayMoney,
                        paymentAtOvertimeHours = partial.paymentAtOvertimeHours
                            ?: acc.paymentAtOvertimeHours,
                        paymentAtOvertimeMoney = partial.paymentAtOvertimeMoney
                            ?: acc.paymentAtOvertimeMoney,
                        surchargeAtOvertime05Hours = partial.surchargeAtOvertime05Hours
                            ?: acc.surchargeAtOvertime05Hours,
                        surchargeAtOvertime05Money = partial.surchargeAtOvertime05Money
                            ?: acc.surchargeAtOvertime05Money,
                        surchargeAtOvertimeHours = partial.surchargeAtOvertimeHours
                            ?: acc.surchargeAtOvertimeHours,
                        surchargeAtOvertimeMoney = partial.surchargeAtOvertimeMoney
                            ?: acc.surchargeAtOvertimeMoney,
                        zonalSurchargePercent = partial.zonalSurchargePercent
                            ?: acc.zonalSurchargePercent,
                        zonalSurchargeMoney = partial.zonalSurchargeMoney
                            ?: acc.zonalSurchargeMoney,
                        surchargeQualificationClassPercent = partial.surchargeQualificationClassPercent
                            ?: acc.surchargeQualificationClassPercent,
                        surchargeQualificationClassMoney = partial.surchargeQualificationClassMoney
                            ?: acc.surchargeQualificationClassMoney,
                        surchargeExtendedServicePhaseHour = partial.surchargeExtendedServicePhaseHour.ifEmpty { acc.surchargeExtendedServicePhaseHour },
                        surchargeExtendedServicePhasePercent = partial.surchargeExtendedServicePhasePercent.ifEmpty { acc.surchargeExtendedServicePhasePercent },
                        surchargeExtendedServicePhaseMoney = partial.surchargeExtendedServicePhaseMoney.ifEmpty { acc.surchargeExtendedServicePhaseMoney },
                        surchargeHeavyTransHour = partial.surchargeHeavyTransHour.ifEmpty { acc.surchargeHeavyTransHour },
                        surchargeHeavyTransPercent = partial.surchargeHeavyTransPercent.ifEmpty { acc.surchargeHeavyTransPercent },
                        surchargeHeavyTransMoney = partial.surchargeHeavyTransMoney.ifEmpty { acc.surchargeHeavyTransMoney },
                        surchargeLongTrainHour = partial.surchargeLongTrainHour.ifEmpty { acc.surchargeLongTrainHour },
                        surchargeLongTrainPercent = partial.surchargeLongTrainPercent.ifEmpty { acc.surchargeLongTrainPercent },
                        surchargeLongTrainMoney = partial.surchargeLongTrainMoney.ifEmpty { acc.surchargeLongTrainMoney },
                        surchargeLongDistanceTrainsHours = partial.surchargeLongDistanceTrainsHours
                            ?: acc.surchargeLongDistanceTrainsHours,
                        surchargeLongDistanceTrainsPercent = partial.surchargeLongDistanceTrainsPercent
                            ?: acc.surchargeLongDistanceTrainsPercent,
                        surchargeLongDistanceTrainsMoney = partial.surchargeLongDistanceTrainsMoney
                            ?: acc.surchargeLongDistanceTrainsMoney,
                        surchargeDoubledTrainFirstHours = partial.surchargeDoubledTrainFirstHours
                            ?: acc.surchargeDoubledTrainFirstHours,
                        surchargeDoubledTrainFirstMoney = partial.surchargeDoubledTrainFirstMoney
                            ?: acc.surchargeDoubledTrainFirstMoney,
                        surchargeDoubledTrainSecondHours = partial.surchargeDoubledTrainSecondHours
                            ?: acc.surchargeDoubledTrainSecondHours,
                        surchargeDoubledTrainSecondMoney = partial.surchargeDoubledTrainSecondMoney
                            ?: acc.surchargeDoubledTrainSecondMoney,
                        paymentAtTimeOfWorkLong = partial.paymentAtTimeOfWorkLong
                            ?: acc.paymentAtTimeOfWorkLong,
                        paymentAtTimeOfWorkMoney = partial.paymentAtTimeOfWorkMoney
                            ?: acc.paymentAtTimeOfWorkMoney,
                        paymentNightTimeHours = partial.paymentNightTimeHours
                            ?: acc.paymentNightTimeHours,
                        paymentNightTimePercent = partial.paymentNightTimePercent
                            ?: acc.paymentNightTimePercent,
                        paymentNightTimeMoney = partial.paymentNightTimeMoney
                            ?: acc.paymentNightTimeMoney,
                        nordicSurchargePercent = partial.nordicSurchargePercent
                            ?: acc.nordicSurchargePercent,
                        nordicSurchargeMoney = partial.nordicSurchargeMoney
                            ?: acc.nordicSurchargeMoney,
                        districtSurchargeCoefficient = partial.districtSurchargeCoefficient
                            ?: acc.districtSurchargeCoefficient,
                        districtSurchargeMoney = partial.districtSurchargeMoney
                            ?: acc.districtSurchargeMoney,
                        onePersonOperationPercent = partial.onePersonOperationPercent
                            ?: acc.onePersonOperationPercent,
                        onePersonOperationMoney = partial.onePersonOperationMoney
                            ?: acc.onePersonOperationMoney,
                        onePersonOperationPassengerTrainPercent = partial.onePersonOperationPassengerTrainPercent
                            ?: acc.onePersonOperationPassengerTrainPercent,
                        onePersonOperationPassengerTrainMoney = partial.onePersonOperationPassengerTrainMoney
                            ?: acc.onePersonOperationPassengerTrainMoney,
                        restInExcessOfTheNormTime = partial.restInExcessOfTheNormTime
                            ?: acc.restInExcessOfTheNormTime,
                        restInExcessOfTheNormMoney = partial.restInExcessOfTheNormMoney
                            ?: acc.restInExcessOfTheNormMoney,
                        harmfulnessSurchargePercent = partial.harmfulnessSurchargePercent
                            ?: acc.harmfulnessSurchargePercent,
                        harmfulnessSurchargeMoney = partial.harmfulnessSurchargeMoney
                            ?: acc.harmfulnessSurchargeMoney,
                        averagePaymentHours = partial.averagePaymentHours
                            ?: acc.averagePaymentHours,
                        averagePaymentMoney = partial.averagePaymentMoney
                            ?: acc.averagePaymentMoney,
                        caringForDisableChildrenHours = partial.caringForDisableChildrenHours
                            ?: acc.caringForDisableChildrenHours,
                        caringForDisableChildrenMoney = partial.caringForDisableChildrenMoney
                            ?: acc.caringForDisableChildrenMoney,
                        totalChargedMoney = partial.totalChargedMoney ?: acc.totalChargedMoney,
                        retentionNdfl = partial.retentionNdfl ?: acc.retentionNdfl,
                        unionistsRetention = partial.unionistsRetention ?: acc.unionistsRetention,
                        otherSurchargeMoney = partial.otherSurchargeMoney
                            ?: acc.otherSurchargeMoney,
                        otherSurchargePercent = partial.otherSurchargePercent
                            ?: acc.otherSurchargePercent,
                        otherRetention = partial.otherRetention ?: acc.otherRetention,
                        totalRetention = partial.totalRetention ?: acc.totalRetention,
                        toBeCredited = partial.toBeCredited ?: acc.toBeCredited
                    )
                }

                // Теперь один раз обновляем _uiState, используя combinedPartial
                _uiState.update { currentState ->
                    currentState.copy(  // Пояснение: copy на полном UIState, заменяем все поля из combinedPartial
                        screenState = ResultState.Success(Unit),  // Устанавливаем Success в конце
                        month = combinedPartial.month,
                        normaHours = combinedPartial.normaHours,
                        totalWorkTime = combinedPartial.totalWorkTime,
                        tariffRate = combinedPartial.tariffRate,
                        paymentAtTariffHours = combinedPartial.paymentAtTariffHours,
                        paymentAtTariffMoney = combinedPartial.paymentAtTariffMoney,
                        paymentAtPassengerHours = combinedPartial.paymentAtPassengerHours,
                        paymentAtPassengerMoney = combinedPartial.paymentAtPassengerMoney,
                        paymentAtSingleLocomotiveHours = combinedPartial.paymentAtSingleLocomotiveHours,
                        paymentAtSingleLocomotiveMoney = combinedPartial.paymentAtSingleLocomotiveMoney,
                        paymentHolidayHours = combinedPartial.paymentHolidayHours,
                        paymentHolidayMoney = combinedPartial.paymentHolidayMoney,
                        surchargeHolidayHours = combinedPartial.surchargeHolidayHours,
                        surchargeHolidayMoney = combinedPartial.surchargeHolidayMoney,
                        paymentAtOvertimeHours = combinedPartial.paymentAtOvertimeHours,
                        paymentAtOvertimeMoney = combinedPartial.paymentAtOvertimeMoney,
                        surchargeAtOvertime05Hours = combinedPartial.surchargeAtOvertime05Hours,
                        surchargeAtOvertime05Money = combinedPartial.surchargeAtOvertime05Money,
                        surchargeAtOvertimeHours = combinedPartial.surchargeAtOvertimeHours,
                        surchargeAtOvertimeMoney = combinedPartial.surchargeAtOvertimeMoney,
                        zonalSurchargePercent = combinedPartial.zonalSurchargePercent,
                        zonalSurchargeMoney = combinedPartial.zonalSurchargeMoney,
                        surchargeQualificationClassPercent = combinedPartial.surchargeQualificationClassPercent,
                        surchargeQualificationClassMoney = combinedPartial.surchargeQualificationClassMoney,
                        surchargeExtendedServicePhaseHour = combinedPartial.surchargeExtendedServicePhaseHour,
                        surchargeExtendedServicePhasePercent = combinedPartial.surchargeExtendedServicePhasePercent,
                        surchargeExtendedServicePhaseMoney = combinedPartial.surchargeExtendedServicePhaseMoney,
                        surchargeHeavyTransHour = combinedPartial.surchargeHeavyTransHour,
                        surchargeHeavyTransPercent = combinedPartial.surchargeHeavyTransPercent,
                        surchargeHeavyTransMoney = combinedPartial.surchargeHeavyTransMoney,
                        surchargeLongTrainHour = combinedPartial.surchargeLongTrainHour,
                        surchargeLongTrainPercent = combinedPartial.surchargeLongTrainPercent,
                        surchargeLongTrainMoney = combinedPartial.surchargeLongTrainMoney,
                        surchargeLongDistanceTrainsHours = combinedPartial.surchargeLongDistanceTrainsHours,
                        surchargeLongDistanceTrainsPercent = combinedPartial.surchargeLongDistanceTrainsPercent,
                        surchargeLongDistanceTrainsMoney = combinedPartial.surchargeLongDistanceTrainsMoney,
                        surchargeDoubledTrainFirstHours = combinedPartial.surchargeDoubledTrainFirstHours,
                        surchargeDoubledTrainFirstMoney = combinedPartial.surchargeDoubledTrainFirstMoney,
                        surchargeDoubledTrainSecondHours = combinedPartial.surchargeDoubledTrainSecondHours,
                        surchargeDoubledTrainSecondMoney = combinedPartial.surchargeDoubledTrainSecondMoney,
                        paymentAtTimeOfWorkLong = combinedPartial.paymentAtTimeOfWorkLong,
                        paymentAtTimeOfWorkMoney = combinedPartial.paymentAtTimeOfWorkMoney,
                        paymentNightTimeHours = combinedPartial.paymentNightTimeHours,
                        paymentNightTimePercent = combinedPartial.paymentNightTimePercent,
                        paymentNightTimeMoney = combinedPartial.paymentNightTimeMoney,
                        nordicSurchargePercent = combinedPartial.nordicSurchargePercent,
                        nordicSurchargeMoney = combinedPartial.nordicSurchargeMoney,
                        districtSurchargeCoefficient = combinedPartial.districtSurchargeCoefficient,
                        districtSurchargeMoney = combinedPartial.districtSurchargeMoney,
                        onePersonOperationPercent = combinedPartial.onePersonOperationPercent,
                        onePersonOperationMoney = combinedPartial.onePersonOperationMoney,
                        onePersonOperationPassengerTrainPercent = combinedPartial.onePersonOperationPassengerTrainPercent,
                        onePersonOperationPassengerTrainMoney = combinedPartial.onePersonOperationPassengerTrainMoney,
                        restInExcessOfTheNormTime = combinedPartial.restInExcessOfTheNormTime,
                        restInExcessOfTheNormMoney = combinedPartial.restInExcessOfTheNormMoney,
                        harmfulnessSurchargePercent = combinedPartial.harmfulnessSurchargePercent,
                        harmfulnessSurchargeMoney = combinedPartial.harmfulnessSurchargeMoney,
                        averagePaymentHours = combinedPartial.averagePaymentHours,
                        averagePaymentMoney = combinedPartial.averagePaymentMoney,
                        caringForDisableChildrenHours = combinedPartial.caringForDisableChildrenHours,
                        caringForDisableChildrenMoney = combinedPartial.caringForDisableChildrenMoney,
                        totalChargedMoney = combinedPartial.totalChargedMoney,
                        retentionNdfl = combinedPartial.retentionNdfl,
                        unionistsRetention = combinedPartial.unionistsRetention,
                        otherSurchargeMoney = combinedPartial.otherSurchargeMoney,
                        otherSurchargePercent = combinedPartial.otherSurchargePercent,
                        otherRetention = combinedPartial.otherRetention,
                        totalRetention = combinedPartial.totalRetention,
                        toBeCredited = combinedPartial.toBeCredited
                    )
                }

            }
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
    ): PartialState {
        val normaHours = currentMonthOfYear.getStandardNormaHours()
        val totalWorkTime = helper.getTotalWorkTime().first()
        val tariffText = if (currentMonthOfYear.dateSetTariffRate == null) {
            "${currentMonthOfYear.tariffRate.str()} ₽"
        } else {
            "${currentMonthOfYear.dateSetTariffRate!!.oldRate.str()} / ${currentMonthOfYear.tariffRate.str()} ₽"
        }
        return PartialState(
            month = getMonthFullText(currentMonthOfYear.month),
            normaHours = normaHours,
            totalWorkTime = totalWorkTime,
            tariffRate = tariffText
        )
    }

    // Метод для установки данных по тарифному времени (часы и сумма).
    private suspend fun setToTariffTimeData(helper: SalaryCalculationHelper): PartialState {
        val workTime = helper.getWorkTimeAtTariffFlow().first()
        val money = helper.getMoneyAtWorkTimeAtTariff().first()

        return PartialState(
            paymentAtTariffHours = workTime,
            paymentAtTariffMoney = money
        )
    }

    // Метод для установки данных по ночному времени (часы, процент, сумма).
    private suspend fun setNightTimeData(
        salarySetting: SalarySetting,
        helper: SalaryCalculationHelper
    ): PartialState {
        val paymentNightTimePercent = salarySetting.nightTimePercent
        val nightTime = helper.getNightTimeFlow().first()
        val money = helper.getMoneyAtNightTimeFlow().first()

        return PartialState(
            paymentNightTimeHours = nightTime,
            paymentNightTimePercent = paymentNightTimePercent,
            paymentNightTimeMoney = money
        )
    }

    // Метод для установки данных по одиночному локомотиву (часы, сумма).
    private suspend fun setSingleLocomotiveData(helper: SalaryCalculationHelper): PartialState {
        val time = helper.getSingleLocomotiveTimeFlow().first()
        val money = helper.getMoneyAtSingleLocomotiveFlow().first()

        return PartialState(
            paymentAtSingleLocomotiveHours = time,
            paymentAtSingleLocomotiveMoney = money
        )
    }

    // Метод для установки данных по пассажирским поездам (часы, сумма).
    private suspend fun setPassengerData(helper: SalaryCalculationHelper): PartialState {
        val passengerTime = helper.getPassengerTimeFlow().first()
        val money = helper.getMoneyAtPassengerFlow().first()

        return PartialState(
            paymentAtPassengerHours = passengerTime,
            paymentAtPassengerMoney = money
        )
    }

    // Метод для установки данных по праздничным дням (часы, сумма, надбавка).
    private suspend fun setHolidayData(helper: SalaryCalculationHelper): PartialState {
        val holidayTime = helper.getHolidayTimeFlow().first()
        val money = helper.getMoneyAtHolidayFlow().first()

        return PartialState(
            paymentHolidayHours = holidayTime,
            surchargeHolidayHours = holidayTime,
            paymentHolidayMoney = money,
            surchargeHolidayMoney = money
        )
    }

    // Метод для установки данных по надбавке за класс квалификации (процент, сумма).
    private suspend fun setQualificationClassSurchargeData(
        salarySetting: SalarySetting,
        helper: SalaryCalculationHelper
    ): PartialState {
        val percent = salarySetting.surchargeQualificationClass
        val money = helper.getMoneyAtQualificationClassFlow().first()

        return PartialState(
            surchargeQualificationClassPercent = percent,
            surchargeQualificationClassMoney = money
        )
    }

    // Метод для установки данных по надбавке за расширенную зону обслуживания (списки часов, процентов, сумм).
    private suspend fun setSurchargeExtendedServicePhase(helper: SalaryCalculationHelper): PartialState {
        val timeList = helper.getTimeListSurchargeServicePhaseFlow().first()
        val percentList = helper.getPercentListSurchargeExtendedServicePhaseFlow().first()
        val moneyList = helper.getMoneyListSurchargeExtendedServicePhaseFlow().first()

        return PartialState(
            surchargeExtendedServicePhaseHour = timeList,
            surchargeExtendedServicePhasePercent = percentList,
            surchargeExtendedServicePhaseMoney = moneyList
        )
    }

    // Метод для установки данных по надбавке за управление одним лицом (процент, сумма).
    private suspend fun setSurchargeOnePersonOperationData(helper: SalaryCalculationHelper): PartialState {
        val percent = helper.getPercentOnePersonOperationFlow().first()
        val money = helper.getMoneyOnePersonOperationFlow().first()

        return PartialState(
            onePersonOperationPercent = percent,
            onePersonOperationMoney = money
        )
    }

    // Метод для установки данных по надбавке за управление одним лицом в пассажирском поезде (процент, сумма).
    private suspend fun setSurchargeOnePersonOperationPassengerTrainData(helper: SalaryCalculationHelper): PartialState {
        val percent = helper.getPercentOnePersonOperationPassengerTrainFlow().first()
        val money = helper.getMoneyOnePersonOperationPassengerTrainFlow().first()

        return PartialState(
            onePersonOperationPassengerTrainPercent = percent,
            onePersonOperationPassengerTrainMoney = money
        )
    }

    // Метод для установки данных по надбавке за вредность (процент, сумма).
    private suspend fun setSurchargeHarmfulnessData(helper: SalaryCalculationHelper): PartialState {
        val percent = helper.getPercentHarmfulnessFlow().first()
        val money = helper.getMoneyHarmfulnessFlow().first()

        return PartialState(
            harmfulnessSurchargePercent = percent,
            harmfulnessSurchargeMoney = money
        )
    }

    // Метод для установки данных по надбавке за дальние поезда (часы, процент, сумма).
    private suspend fun setSurchargeLongDistanceData(helper: SalaryCalculationHelper): PartialState {
        val percent = helper.getPercentLongDistanceTrainFlow().first()
        val time = helper.getTimeLongDistanceTrainFlow().first()
        val money = helper.getMoneyLongDistanceTrainFlow().first()

        return PartialState(
            surchargeLongDistanceTrainsPercent = percent,
            surchargeLongDistanceTrainsHours = time,
            surchargeLongDistanceTrainsMoney = money
        )
    }

    // Метод для установки данных по надбавке за тяжелые поезда (списки часов, процентов, сумм).
    private suspend fun setSurchargeHeavyTransData(helper: SalaryCalculationHelper): PartialState {
        val timeList = helper.getTimeListSurchargeHeavyTrainsFlow().first()
        val percentList = helper.getPercentListSurchargeExtendedHeavyTrainsFlow().first()
        val moneyList = helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first()

        return PartialState(
            surchargeHeavyTransHour = timeList,
            surchargeHeavyTransPercent = percentList,
            surchargeHeavyTransMoney = moneyList
        )
    }

    private suspend fun setSurchargeLongTrainData(helper: SalaryCalculationHelper): PartialState {
        val timeList = helper.getTimeListSurchargeLongTrainsFlow().first()
        val percentList = helper.getPercentListSurchargeLongTrainsFlow().first()
        val moneyList = helper.getMoneyListSurchargeLongTrainsFlow().first()

        return PartialState(
            surchargeLongTrainHour = timeList,
            surchargeLongTrainPercent = percentList,
            surchargeLongTrainMoney = moneyList
        )
    }

    // Метод для установки данных по надбавке за сдвоенные поезда (первый 30%, второй 15%).
    private suspend fun setSurchargeDoubledTrainData(helper: SalaryCalculationHelper): PartialState {
        val timeFirst = helper.getTimeDoubledTrainFirstSurchargeFlow().first()
        val moneyFirst = helper.getMoneyDoubledTrainFirstSurchargeFlow().first()
        val timeSecond = helper.getTimeDoubledTrainSecondSurchargeFlow().first()
        val moneySecond = helper.getMoneyDoubledTrainSecondSurchargeFlow().first()

        return PartialState(
            surchargeDoubledTrainFirstHours = timeFirst,
            surchargeDoubledTrainFirstMoney = moneyFirst,
            surchargeDoubledTrainSecondHours = timeSecond,
            surchargeDoubledTrainSecondMoney = moneySecond
        )
    }

    // Метод для установки данных по зональной надбавке (процент, сумма).
    private suspend fun setZonalSurchargeData(helper: SalaryCalculationHelper): PartialState {
        val percent = helper.getPercentZonalSurchargeFlow().first()
        val money = helper.getMoneyZonalSurchargeFlow().first()

        return PartialState(
            zonalSurchargePercent = percent,
            zonalSurchargeMoney = money
        )
    }

    // Метод для установки данных по сверхурочным часам (часы, сумма).
    private suspend fun setOvertimeData(helper: SalaryCalculationHelper): PartialState {
        val overtimeHours = helper.getTimeOvertimeFlow().first()
        val overtimeMoney = helper.getMoneyOvertimeFlow().first()

        return PartialState(
            paymentAtOvertimeHours = overtimeHours,
            paymentAtOvertimeMoney = overtimeMoney
        )
    }

    // Метод для установки данных по надбавке за сверхурочные (часы и суммы для 0.5 и полной ставки).
    private suspend fun setSurchargeOvertimeData(helper: SalaryCalculationHelper): PartialState {
        val surchargeAtOvertime05Hour = helper.getTimeSurchargeAtOvertime05Flow().first()
        val surchargeAtOvertime05Money = helper.getMoneySurchargeOvertime05Flow().first()
        val surchargeAtOvertimeHour = helper.getTimeSurchargeAtOvertimeFlow().first()
        val surchargeAtOvertimeMoney = helper.getMoneySurchargeOvertimeFlow().first()
        return PartialState(
            surchargeAtOvertime05Hours = surchargeAtOvertime05Hour,
            surchargeAtOvertime05Money = surchargeAtOvertime05Money,
            surchargeAtOvertimeHours = surchargeAtOvertimeHour,
            surchargeAtOvertimeMoney = surchargeAtOvertimeMoney
        )
    }

    // Метод для установки данных по районной надбавке (коэффициент, сумма).
    private suspend fun setDistrictSurchargeData(helper: SalaryCalculationHelper): PartialState {
        val percent = helper.getPercentDistrictSurcharge().first()
        val money = helper.getMoneyDistrictSurcharge().first()

        return PartialState(
            districtSurchargeCoefficient = percent,
            districtSurchargeMoney = money
        )
    }

    // Метод для установки данных по северной надбавке (процент, сумма).
    private suspend fun setNordicSurchargeData(helper: SalaryCalculationHelper): PartialState {
        val percent = helper.getPercentNordicSurcharge().first()
        val money = helper.getMoneyNordicSurcharge().first()

        return PartialState(
            nordicSurchargePercent = percent,
            nordicSurchargeMoney = money
        )
    }

    // Метод для установки данных по средней оплате (часы, сумма).
    private suspend fun setAveragePaymentData(helper: SalaryCalculationHelper): PartialState {
        val hours = helper.getDayOffHoursFlow().first()
        val money = helper.getMoneyAverageFlow().first()

        return PartialState(
            averagePaymentHours = hours,
            averagePaymentMoney = money
        )
    }

    // Метод для установки данных по уходу за ребенком инвалидом
    private suspend fun setCaringForDisableChildrenPaymentData(helper: SalaryCalculationHelper): PartialState {
        val hours = helper.getHoursCaringForDisableChildren().first()
        val money = helper.getMoneyCaringForDisableChildren().first()

        return PartialState(
            caringForDisableChildrenHours = hours,
            caringForDisableChildrenMoney = money
        )
    }

    // Метод для установки данных по переотдыху (время, сумма).
    private suspend fun setRestInExcessOfTheNormData(helper: SalaryCalculationHelper): PartialState {
        val time = helper.getOverRestTimeFlow().first()
        val money = helper.getMoneyOverRestFlow().first()
        return PartialState(
            restInExcessOfTheNormTime = time,
            restInExcessOfTheNormMoney = money
        )
    }

    // Метод для установки данных по прочим надбавкам (процент, сумма).
    private suspend fun setOtherSurchargeData(helper: SalaryCalculationHelper): PartialState {
        val percent = helper.getPercentOtherSurchargeFlow().first()
        val money = helper.getMoneyOtherSurchargeFlow().first()

        return PartialState(
            otherSurchargePercent = percent,
            otherSurchargeMoney = money
        )
    }

    // Метод для установки общей начисленной суммы.
    private suspend fun setTotalCharged(helper: SalaryCalculationHelper): PartialState {
        val money = helper.getMoneyTotalChargedFlow().first()

        return PartialState(
            totalChargedMoney = money
        )
    }

    // Метод для установки данных по удержаниям (НДФЛ, профсоюз, прочие, итого).
    private suspend fun setRetentionData(helper: SalaryCalculationHelper): PartialState {
        val moneyNDFL = helper.getMoneyNDFLRetentionFlow().first()
        val moneyUnionists = helper.getMoneyUnionistsRetentionFlow().first()
        val moneyOther = helper.getMoneyOtherRetentionFlow().first()
        val moneyTotal = helper.getMoneyTotalRetentionFlow().first()

        return PartialState(
            retentionNdfl = moneyNDFL,
            unionistsRetention = moneyUnionists,
            otherRetention = moneyOther,
            totalRetention = moneyTotal
        )
    }

    // Метод для установки суммы к выдаче и завершения загрузки (установка Success).
    private suspend fun setToBeCredited(helper: SalaryCalculationHelper): PartialState {
        val toBeCredited = helper.getMoneyToBeCredited().first()

        return PartialState(
            toBeCredited = toBeCredited
        )
    }
}

data class PartialState(
    val month: String = "",
    val tariffRate: String? = null,
    val normaHours: Int? = null,
    val totalWorkTime: Long? = null,
    val paymentAtTariffHours: Long? = null,
    val paymentAtTariffMoney: Double? = null,
    val paymentAtPassengerHours: Long? = null,
    val paymentAtPassengerMoney: Double? = null,
    val paymentAtSingleLocomotiveHours: Long? = null,
    val paymentAtSingleLocomotiveMoney: Double? = null,
    val paymentHolidayHours: Long? = null,
    val paymentHolidayMoney: Double? = null,
    val surchargeHolidayHours: Long? = null,
    val surchargeHolidayMoney: Double? = null,
    val paymentAtOvertimeHours: Long? = null,
    val paymentAtOvertimeMoney: Double? = null,
    val surchargeAtOvertime05Hours: Long? = null,
    val surchargeAtOvertime05Money: Double? = null,
    val surchargeAtOvertimeHours: Long? = null,
    val surchargeAtOvertimeMoney: Double? = null,
    val zonalSurchargePercent: Double? = null,
    val zonalSurchargeMoney: Double? = null,
    val surchargeQualificationClassPercent: Double? = null,
    val surchargeQualificationClassMoney: Double? = null,
    val surchargeExtendedServicePhaseHour: List<Long?> = emptyList(),
    val surchargeExtendedServicePhasePercent: List<String?> = emptyList(),
    val surchargeExtendedServicePhaseMoney: List<Double?> = emptyList(),
    val surchargeHeavyTransHour: List<Long?> = emptyList(),
    val surchargeHeavyTransPercent: List<String?> = emptyList(),
    val surchargeHeavyTransMoney: List<Double?> = emptyList(),
    val surchargeLongTrainHour: List<Long?> = emptyList(),
    val surchargeLongTrainPercent: List<String?> = emptyList(),
    val surchargeLongTrainMoney: List<Double?> = emptyList(),
    val surchargeLongDistanceTrainsHours: Long? = null,
    val surchargeLongDistanceTrainsPercent: Double? = null,
    val surchargeLongDistanceTrainsMoney: Double? = null,
    val surchargeDoubledTrainFirstHours: Long? = null,
    val surchargeDoubledTrainFirstMoney: Double? = null,
    val surchargeDoubledTrainSecondHours: Long? = null,
    val surchargeDoubledTrainSecondMoney: Double? = null,
    val paymentAtTimeOfWorkLong: Long? = null,
    val paymentAtTimeOfWorkMoney: Double? = null,
    val paymentNightTimeHours: Long? = null,
    val paymentNightTimePercent: Double? = null,
    val paymentNightTimeMoney: Double? = null,
    val nordicSurchargePercent: Double? = null,
    val nordicSurchargeMoney: Double? = null,
    val districtSurchargeCoefficient: Double? = null,
    val districtSurchargeMoney: Double? = null,
    val onePersonOperationPercent: Double? = null,
    val onePersonOperationMoney: Double? = null,
    val onePersonOperationPassengerTrainPercent: Double? = null,
    val onePersonOperationPassengerTrainMoney: Double? = null,
    val restInExcessOfTheNormTime: Long? = null,
    val restInExcessOfTheNormMoney: Double? = null,
    val harmfulnessSurchargePercent: Double? = null,
    val harmfulnessSurchargeMoney: Double? = null,
    val averagePaymentHours: Long? = null,
    val averagePaymentMoney: Double? = null,
    val caringForDisableChildrenHours: Long? = null,
    val caringForDisableChildrenMoney: Double? = null,
    val totalChargedMoney: Double? = null,
    val retentionNdfl: Double? = null,
    val unionistsRetention: Double? = null,
    val otherSurchargeMoney: Double? = null,
    val otherSurchargePercent: Double? = null,
    val otherRetention: Double? = null,
    val totalRetention: Double? = null,
    val toBeCredited: Double? = null
)