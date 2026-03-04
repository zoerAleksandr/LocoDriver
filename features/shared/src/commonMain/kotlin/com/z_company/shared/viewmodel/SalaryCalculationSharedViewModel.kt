package com.z_company.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.shared.util.ConverterLongToTime
import com.z_company.shared.util.MonthFullText.getMonthFullText
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
import kotlin.time.Clock

/**
 * UIState for salary calculation screen — mirrors Android SalaryCalculationUIState.
 */
data class SalaryCalculationUIState(
    val screenState: ResultState<Unit> = ResultState.Loading(),
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
    val surchargeLongDistanceTrainsHours: Long? = null,
    val surchargeLongDistanceTrainsPercent: Double? = null,
    val surchargeLongDistanceTrainsMoney: Double? = null,
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
    val toBeCredited: Double? = null,
)

/**
 * Partial state for parallel async assembly.
 */
data class SalaryPartialState(
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
    val surchargeLongDistanceTrainsHours: Long? = null,
    val surchargeLongDistanceTrainsPercent: Double? = null,
    val surchargeLongDistanceTrainsMoney: Double? = null,
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
    val toBeCredited: Double? = null,
)

/**
 * Shared ViewModel for the salary calculation screen.
 * Based on Android SalaryCalculationViewModel with full 23+ parallel calculations.
 */
class SalaryCalculationSharedViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val salarySettingUseCase: SalarySettingUseCase,
) : ViewModel() {

    private val _userSetting = MutableStateFlow(UserSettings())
    val userSetting = _userSetting.asStateFlow()
    private var job: Job? = null

    private val _uiState = MutableStateFlow(SalaryCalculationUIState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
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
        }
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
        viewModelScope.coroutineContext.cancelChildren()
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
        val currentTimeInMillis = Clock.System.now().toEpochMilliseconds()
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
            val helper = SalaryCalculationHelper(
                userSettings = userSettings,
                salarySetting = salarySetting,
                routeList = routeList
            )
            job?.cancel()
            job = CoroutineScope(Dispatchers.Default).launch {
                val tasks = listOf(
                    async { setHeaderData(currentMonthOfYear, helper) },
                    async { setToTariffTimeData(helper) },
                    async { setNightTimeData(salarySetting, helper) },
                    async { setSingleLocomotiveData(helper) },
                    async { setPassengerData(helper) },
                    async { setHolidayData(helper) },
                    async { setQualificationClassSurchargeData(salarySetting, helper) },
                    async { setSurchargeExtendedServicePhase(helper) },
                    async { setSurchargeOnePersonOperationData(helper) },
                    async { setSurchargeOnePersonOperationPassengerTrainData(helper) },
                    async { setSurchargeHarmfulnessData(helper) },
                    async { setSurchargeLongDistanceData(helper) },
                    async { setSurchargeHeavyTransData(helper) },
                    async { setZonalSurchargeData(helper) },
                    async { setOvertimeData(helper) },
                    async { setSurchargeOvertimeData(helper) },
                    async { setDistrictSurchargeData(helper) },
                    async { setNordicSurchargeData(helper) },
                    async { setAveragePaymentData(helper) },
                    async { setCaringForDisableChildrenPaymentData(helper) },
                    async { setOtherSurchargeData(helper) },
                    async { setTotalCharged(helper) },
                    async { setRetentionData(helper) },
                    async { setToBeCredited(helper) }
                )

                val allPartialStates = tasks.awaitAll()

                val combined = allPartialStates.fold(SalaryPartialState()) { acc, partial ->
                    acc.copy(
                        month = partial.month,
                        normaHours = partial.normaHours ?: acc.normaHours,
                        totalWorkTime = partial.totalWorkTime ?: acc.totalWorkTime,
                        tariffRate = partial.tariffRate ?: acc.tariffRate,
                        paymentAtTariffHours = partial.paymentAtTariffHours ?: acc.paymentAtTariffHours,
                        paymentAtTariffMoney = partial.paymentAtTariffMoney ?: acc.paymentAtTariffMoney,
                        paymentAtPassengerHours = partial.paymentAtPassengerHours ?: acc.paymentAtPassengerHours,
                        paymentAtPassengerMoney = partial.paymentAtPassengerMoney ?: acc.paymentAtPassengerMoney,
                        paymentAtSingleLocomotiveHours = partial.paymentAtSingleLocomotiveHours ?: acc.paymentAtSingleLocomotiveHours,
                        paymentAtSingleLocomotiveMoney = partial.paymentAtSingleLocomotiveMoney ?: acc.paymentAtSingleLocomotiveMoney,
                        paymentHolidayHours = partial.paymentHolidayHours ?: acc.paymentHolidayHours,
                        paymentHolidayMoney = partial.paymentHolidayMoney ?: acc.paymentHolidayMoney,
                        surchargeHolidayHours = partial.surchargeHolidayHours ?: acc.surchargeHolidayHours,
                        surchargeHolidayMoney = partial.surchargeHolidayMoney ?: acc.surchargeHolidayMoney,
                        paymentAtOvertimeHours = partial.paymentAtOvertimeHours ?: acc.paymentAtOvertimeHours,
                        paymentAtOvertimeMoney = partial.paymentAtOvertimeMoney ?: acc.paymentAtOvertimeMoney,
                        surchargeAtOvertime05Hours = partial.surchargeAtOvertime05Hours ?: acc.surchargeAtOvertime05Hours,
                        surchargeAtOvertime05Money = partial.surchargeAtOvertime05Money ?: acc.surchargeAtOvertime05Money,
                        surchargeAtOvertimeHours = partial.surchargeAtOvertimeHours ?: acc.surchargeAtOvertimeHours,
                        surchargeAtOvertimeMoney = partial.surchargeAtOvertimeMoney ?: acc.surchargeAtOvertimeMoney,
                        zonalSurchargePercent = partial.zonalSurchargePercent ?: acc.zonalSurchargePercent,
                        zonalSurchargeMoney = partial.zonalSurchargeMoney ?: acc.zonalSurchargeMoney,
                        surchargeQualificationClassPercent = partial.surchargeQualificationClassPercent ?: acc.surchargeQualificationClassPercent,
                        surchargeQualificationClassMoney = partial.surchargeQualificationClassMoney ?: acc.surchargeQualificationClassMoney,
                        surchargeExtendedServicePhaseHour = partial.surchargeExtendedServicePhaseHour.ifEmpty { acc.surchargeExtendedServicePhaseHour },
                        surchargeExtendedServicePhasePercent = partial.surchargeExtendedServicePhasePercent.ifEmpty { acc.surchargeExtendedServicePhasePercent },
                        surchargeExtendedServicePhaseMoney = partial.surchargeExtendedServicePhaseMoney.ifEmpty { acc.surchargeExtendedServicePhaseMoney },
                        surchargeHeavyTransHour = partial.surchargeHeavyTransHour.ifEmpty { acc.surchargeHeavyTransHour },
                        surchargeHeavyTransPercent = partial.surchargeHeavyTransPercent.ifEmpty { acc.surchargeHeavyTransPercent },
                        surchargeHeavyTransMoney = partial.surchargeHeavyTransMoney.ifEmpty { acc.surchargeHeavyTransMoney },
                        surchargeLongDistanceTrainsHours = partial.surchargeLongDistanceTrainsHours ?: acc.surchargeLongDistanceTrainsHours,
                        surchargeLongDistanceTrainsPercent = partial.surchargeLongDistanceTrainsPercent ?: acc.surchargeLongDistanceTrainsPercent,
                        surchargeLongDistanceTrainsMoney = partial.surchargeLongDistanceTrainsMoney ?: acc.surchargeLongDistanceTrainsMoney,
                        paymentAtTimeOfWorkLong = partial.paymentAtTimeOfWorkLong ?: acc.paymentAtTimeOfWorkLong,
                        paymentAtTimeOfWorkMoney = partial.paymentAtTimeOfWorkMoney ?: acc.paymentAtTimeOfWorkMoney,
                        paymentNightTimeHours = partial.paymentNightTimeHours ?: acc.paymentNightTimeHours,
                        paymentNightTimePercent = partial.paymentNightTimePercent ?: acc.paymentNightTimePercent,
                        paymentNightTimeMoney = partial.paymentNightTimeMoney ?: acc.paymentNightTimeMoney,
                        nordicSurchargePercent = partial.nordicSurchargePercent ?: acc.nordicSurchargePercent,
                        nordicSurchargeMoney = partial.nordicSurchargeMoney ?: acc.nordicSurchargeMoney,
                        districtSurchargeCoefficient = partial.districtSurchargeCoefficient ?: acc.districtSurchargeCoefficient,
                        districtSurchargeMoney = partial.districtSurchargeMoney ?: acc.districtSurchargeMoney,
                        onePersonOperationPercent = partial.onePersonOperationPercent ?: acc.onePersonOperationPercent,
                        onePersonOperationMoney = partial.onePersonOperationMoney ?: acc.onePersonOperationMoney,
                        onePersonOperationPassengerTrainPercent = partial.onePersonOperationPassengerTrainPercent ?: acc.onePersonOperationPassengerTrainPercent,
                        onePersonOperationPassengerTrainMoney = partial.onePersonOperationPassengerTrainMoney ?: acc.onePersonOperationPassengerTrainMoney,
                        restInExcessOfTheNormTime = partial.restInExcessOfTheNormTime ?: acc.restInExcessOfTheNormTime,
                        restInExcessOfTheNormMoney = partial.restInExcessOfTheNormMoney ?: acc.restInExcessOfTheNormMoney,
                        harmfulnessSurchargePercent = partial.harmfulnessSurchargePercent ?: acc.harmfulnessSurchargePercent,
                        harmfulnessSurchargeMoney = partial.harmfulnessSurchargeMoney ?: acc.harmfulnessSurchargeMoney,
                        averagePaymentHours = partial.averagePaymentHours ?: acc.averagePaymentHours,
                        averagePaymentMoney = partial.averagePaymentMoney ?: acc.averagePaymentMoney,
                        caringForDisableChildrenHours = partial.caringForDisableChildrenHours ?: acc.caringForDisableChildrenHours,
                        caringForDisableChildrenMoney = partial.caringForDisableChildrenMoney ?: acc.caringForDisableChildrenMoney,
                        totalChargedMoney = partial.totalChargedMoney ?: acc.totalChargedMoney,
                        retentionNdfl = partial.retentionNdfl ?: acc.retentionNdfl,
                        unionistsRetention = partial.unionistsRetention ?: acc.unionistsRetention,
                        otherSurchargeMoney = partial.otherSurchargeMoney ?: acc.otherSurchargeMoney,
                        otherSurchargePercent = partial.otherSurchargePercent ?: acc.otherSurchargePercent,
                        otherRetention = partial.otherRetention ?: acc.otherRetention,
                        totalRetention = partial.totalRetention ?: acc.totalRetention,
                        toBeCredited = partial.toBeCredited ?: acc.toBeCredited,
                    )
                }

                _uiState.update { currentState ->
                    currentState.copy(
                        screenState = ResultState.Success(Unit),
                        month = combined.month,
                        normaHours = combined.normaHours,
                        totalWorkTime = combined.totalWorkTime,
                        tariffRate = combined.tariffRate,
                        paymentAtTariffHours = combined.paymentAtTariffHours,
                        paymentAtTariffMoney = combined.paymentAtTariffMoney,
                        paymentAtPassengerHours = combined.paymentAtPassengerHours,
                        paymentAtPassengerMoney = combined.paymentAtPassengerMoney,
                        paymentAtSingleLocomotiveHours = combined.paymentAtSingleLocomotiveHours,
                        paymentAtSingleLocomotiveMoney = combined.paymentAtSingleLocomotiveMoney,
                        paymentHolidayHours = combined.paymentHolidayHours,
                        paymentHolidayMoney = combined.paymentHolidayMoney,
                        surchargeHolidayHours = combined.surchargeHolidayHours,
                        surchargeHolidayMoney = combined.surchargeHolidayMoney,
                        paymentAtOvertimeHours = combined.paymentAtOvertimeHours,
                        paymentAtOvertimeMoney = combined.paymentAtOvertimeMoney,
                        surchargeAtOvertime05Hours = combined.surchargeAtOvertime05Hours,
                        surchargeAtOvertime05Money = combined.surchargeAtOvertime05Money,
                        surchargeAtOvertimeHours = combined.surchargeAtOvertimeHours,
                        surchargeAtOvertimeMoney = combined.surchargeAtOvertimeMoney,
                        zonalSurchargePercent = combined.zonalSurchargePercent,
                        zonalSurchargeMoney = combined.zonalSurchargeMoney,
                        surchargeQualificationClassPercent = combined.surchargeQualificationClassPercent,
                        surchargeQualificationClassMoney = combined.surchargeQualificationClassMoney,
                        surchargeExtendedServicePhaseHour = combined.surchargeExtendedServicePhaseHour,
                        surchargeExtendedServicePhasePercent = combined.surchargeExtendedServicePhasePercent,
                        surchargeExtendedServicePhaseMoney = combined.surchargeExtendedServicePhaseMoney,
                        surchargeHeavyTransHour = combined.surchargeHeavyTransHour,
                        surchargeHeavyTransPercent = combined.surchargeHeavyTransPercent,
                        surchargeHeavyTransMoney = combined.surchargeHeavyTransMoney,
                        surchargeLongDistanceTrainsHours = combined.surchargeLongDistanceTrainsHours,
                        surchargeLongDistanceTrainsPercent = combined.surchargeLongDistanceTrainsPercent,
                        surchargeLongDistanceTrainsMoney = combined.surchargeLongDistanceTrainsMoney,
                        paymentAtTimeOfWorkLong = combined.paymentAtTimeOfWorkLong,
                        paymentAtTimeOfWorkMoney = combined.paymentAtTimeOfWorkMoney,
                        paymentNightTimeHours = combined.paymentNightTimeHours,
                        paymentNightTimePercent = combined.paymentNightTimePercent,
                        paymentNightTimeMoney = combined.paymentNightTimeMoney,
                        nordicSurchargePercent = combined.nordicSurchargePercent,
                        nordicSurchargeMoney = combined.nordicSurchargeMoney,
                        districtSurchargeCoefficient = combined.districtSurchargeCoefficient,
                        districtSurchargeMoney = combined.districtSurchargeMoney,
                        onePersonOperationPercent = combined.onePersonOperationPercent,
                        onePersonOperationMoney = combined.onePersonOperationMoney,
                        onePersonOperationPassengerTrainPercent = combined.onePersonOperationPassengerTrainPercent,
                        onePersonOperationPassengerTrainMoney = combined.onePersonOperationPassengerTrainMoney,
                        restInExcessOfTheNormTime = combined.restInExcessOfTheNormTime,
                        restInExcessOfTheNormMoney = combined.restInExcessOfTheNormMoney,
                        harmfulnessSurchargePercent = combined.harmfulnessSurchargePercent,
                        harmfulnessSurchargeMoney = combined.harmfulnessSurchargeMoney,
                        averagePaymentHours = combined.averagePaymentHours,
                        averagePaymentMoney = combined.averagePaymentMoney,
                        caringForDisableChildrenHours = combined.caringForDisableChildrenHours,
                        caringForDisableChildrenMoney = combined.caringForDisableChildrenMoney,
                        totalChargedMoney = combined.totalChargedMoney,
                        retentionNdfl = combined.retentionNdfl,
                        unionistsRetention = combined.unionistsRetention,
                        otherSurchargeMoney = combined.otherSurchargeMoney,
                        otherSurchargePercent = combined.otherSurchargePercent,
                        otherRetention = combined.otherRetention,
                        totalRetention = combined.totalRetention,
                        toBeCredited = combined.toBeCredited,
                    )
                }
            }
        } else if (loadRouteState is ResultState.Error) {
            _uiState.update { it.copy(screenState = ResultState.Error(ErrorEntity(message = loadRouteState.entity.message))) }
        } else {
            _uiState.update { it.copy(screenState = ResultState.Error(ErrorEntity(message = "Ошибка загрузки маршрутов"))) }
        }
    }

    private suspend fun setHeaderData(currentMonthOfYear: MonthOfYear, helper: SalaryCalculationHelper): SalaryPartialState {
        val normaHours = currentMonthOfYear.getStandardNormaHours()
        val totalWorkTime = helper.getTotalWorkTime().first()
        val tariffText = if (currentMonthOfYear.dateSetTariffRate == null) {
            "${currentMonthOfYear.tariffRate.str()} ₽"
        } else {
            "${currentMonthOfYear.dateSetTariffRate!!.oldRate.str()} / ${currentMonthOfYear.tariffRate.str()} ₽"
        }
        return SalaryPartialState(
            month = getMonthFullText(currentMonthOfYear.month),
            normaHours = normaHours,
            totalWorkTime = totalWorkTime,
            tariffRate = tariffText
        )
    }

    private suspend fun setToTariffTimeData(helper: SalaryCalculationHelper): SalaryPartialState {
        val workTime = helper.getWorkTimeAtTariffFlow().first()
        val money = helper.getMoneyAtWorkTimeAtTariff().first()
        return SalaryPartialState(paymentAtTariffHours = workTime, paymentAtTariffMoney = money)
    }

    private suspend fun setNightTimeData(salarySetting: SalarySetting, helper: SalaryCalculationHelper): SalaryPartialState {
        val paymentNightTimePercent = salarySetting.nightTimePercent
        val nightTime = helper.getNightTimeFlow().first()
        val money = helper.getMoneyAtNightTimeFlow().first()
        return SalaryPartialState(paymentNightTimeHours = nightTime, paymentNightTimePercent = paymentNightTimePercent, paymentNightTimeMoney = money)
    }

    private suspend fun setSingleLocomotiveData(helper: SalaryCalculationHelper): SalaryPartialState {
        val time = helper.getSingleLocomotiveTimeFlow().first()
        val money = helper.getMoneyAtSingleLocomotiveFlow().first()
        return SalaryPartialState(paymentAtSingleLocomotiveHours = time, paymentAtSingleLocomotiveMoney = money)
    }

    private suspend fun setPassengerData(helper: SalaryCalculationHelper): SalaryPartialState {
        val passengerTime = helper.getPassengerTimeFlow().first()
        val money = helper.getMoneyAtPassengerFlow().first()
        return SalaryPartialState(paymentAtPassengerHours = passengerTime, paymentAtPassengerMoney = money)
    }

    private suspend fun setHolidayData(helper: SalaryCalculationHelper): SalaryPartialState {
        val holidayTime = helper.getHolidayTimeFlow().first()
        val money = helper.getMoneyAtHolidayFlow().first()
        return SalaryPartialState(paymentHolidayHours = holidayTime, surchargeHolidayHours = holidayTime, paymentHolidayMoney = money, surchargeHolidayMoney = money)
    }

    private suspend fun setQualificationClassSurchargeData(salarySetting: SalarySetting, helper: SalaryCalculationHelper): SalaryPartialState {
        val percent = salarySetting.surchargeQualificationClass
        val money = helper.getMoneyAtQualificationClassFlow().first()
        return SalaryPartialState(surchargeQualificationClassPercent = percent, surchargeQualificationClassMoney = money)
    }

    private suspend fun setSurchargeExtendedServicePhase(helper: SalaryCalculationHelper): SalaryPartialState {
        val timeList = helper.getTimeListSurchargeServicePhaseFlow().first()
        val percentList = helper.getPercentListSurchargeExtendedServicePhaseFlow().first()
        val moneyList = helper.getMoneyListSurchargeExtendedServicePhaseFlow().first()
        return SalaryPartialState(surchargeExtendedServicePhaseHour = timeList, surchargeExtendedServicePhasePercent = percentList, surchargeExtendedServicePhaseMoney = moneyList)
    }

    private suspend fun setSurchargeOnePersonOperationData(helper: SalaryCalculationHelper): SalaryPartialState {
        val percent = helper.getPercentOnePersonOperationFlow().first()
        val money = helper.getMoneyOnePersonOperationFlow().first()
        return SalaryPartialState(onePersonOperationPercent = percent, onePersonOperationMoney = money)
    }

    private suspend fun setSurchargeOnePersonOperationPassengerTrainData(helper: SalaryCalculationHelper): SalaryPartialState {
        val percent = helper.getPercentOnePersonOperationPassengerTrainFlow().first()
        val money = helper.getMoneyOnePersonOperationPassengerTrainFlow().first()
        return SalaryPartialState(onePersonOperationPassengerTrainPercent = percent, onePersonOperationPassengerTrainMoney = money)
    }

    private suspend fun setSurchargeHarmfulnessData(helper: SalaryCalculationHelper): SalaryPartialState {
        val percent = helper.getPercentHarmfulnessFlow().first()
        val money = helper.getMoneyHarmfulnessFlow().first()
        return SalaryPartialState(harmfulnessSurchargePercent = percent, harmfulnessSurchargeMoney = money)
    }

    private suspend fun setSurchargeLongDistanceData(helper: SalaryCalculationHelper): SalaryPartialState {
        val percent = helper.getPercentLongDistanceTrainFlow().first()
        val time = helper.getTimeLongDistanceTrainFlow().first()
        val money = helper.getMoneyLongDistanceTrainFlow().first()
        return SalaryPartialState(surchargeLongDistanceTrainsPercent = percent, surchargeLongDistanceTrainsHours = time, surchargeLongDistanceTrainsMoney = money)
    }

    private suspend fun setSurchargeHeavyTransData(helper: SalaryCalculationHelper): SalaryPartialState {
        val timeList = helper.getTimeListSurchargeHeavyTrainsFlow().first()
        val percentList = helper.getPercentListSurchargeExtendedHeavyTrainsFlow().first()
        val moneyList = helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first()
        return SalaryPartialState(surchargeHeavyTransHour = timeList, surchargeHeavyTransPercent = percentList, surchargeHeavyTransMoney = moneyList)
    }

    private suspend fun setZonalSurchargeData(helper: SalaryCalculationHelper): SalaryPartialState {
        val percent = helper.getPercentZonalSurchargeFlow().first()
        val money = helper.getMoneyZonalSurchargeFlow().first()
        return SalaryPartialState(zonalSurchargePercent = percent, zonalSurchargeMoney = money)
    }

    private suspend fun setOvertimeData(helper: SalaryCalculationHelper): SalaryPartialState {
        val overtimeHours = helper.getTimeOvertimeFlow().first()
        val overtimeMoney = helper.getMoneyOvertimeFlow().first()
        return SalaryPartialState(paymentAtOvertimeHours = overtimeHours, paymentAtOvertimeMoney = overtimeMoney)
    }

    private suspend fun setSurchargeOvertimeData(helper: SalaryCalculationHelper): SalaryPartialState {
        val surchargeAtOvertime05Hour = helper.getTimeSurchargeAtOvertime05Flow().first()
        val surchargeAtOvertime05Money = helper.getMoneySurchargeOvertime05Flow().first()
        val surchargeAtOvertimeHour = helper.getTimeSurchargeAtOvertimeFlow().first()
        val surchargeAtOvertimeMoney = helper.getMoneySurchargeOvertimeFlow().first()
        return SalaryPartialState(
            surchargeAtOvertime05Hours = surchargeAtOvertime05Hour, surchargeAtOvertime05Money = surchargeAtOvertime05Money,
            surchargeAtOvertimeHours = surchargeAtOvertimeHour, surchargeAtOvertimeMoney = surchargeAtOvertimeMoney
        )
    }

    private suspend fun setDistrictSurchargeData(helper: SalaryCalculationHelper): SalaryPartialState {
        val percent = helper.getPercentDistrictSurcharge().first()
        val money = helper.getMoneyDistrictSurcharge().first()
        return SalaryPartialState(districtSurchargeCoefficient = percent, districtSurchargeMoney = money)
    }

    private suspend fun setNordicSurchargeData(helper: SalaryCalculationHelper): SalaryPartialState {
        val percent = helper.getPercentNordicSurcharge().first()
        val money = helper.getMoneyNordicSurcharge().first()
        return SalaryPartialState(nordicSurchargePercent = percent, nordicSurchargeMoney = money)
    }

    private suspend fun setAveragePaymentData(helper: SalaryCalculationHelper): SalaryPartialState {
        val hours = helper.getDayOffHoursFlow().first()
        val money = helper.getMoneyAverageFlow().first()
        return SalaryPartialState(averagePaymentHours = hours, averagePaymentMoney = money)
    }

    private suspend fun setCaringForDisableChildrenPaymentData(helper: SalaryCalculationHelper): SalaryPartialState {
        val hours = helper.getHoursCaringForDisableChildren().first()
        val money = helper.getMoneyCaringForDisableChildren().first()
        return SalaryPartialState(caringForDisableChildrenHours = hours, caringForDisableChildrenMoney = money)
    }

    private suspend fun setOtherSurchargeData(helper: SalaryCalculationHelper): SalaryPartialState {
        val percent = helper.getPercentOtherSurchargeFlow().first()
        val money = helper.getMoneyOtherSurchargeFlow().first()
        return SalaryPartialState(otherSurchargePercent = percent, otherSurchargeMoney = money)
    }

    private suspend fun setTotalCharged(helper: SalaryCalculationHelper): SalaryPartialState {
        val money = helper.getMoneyTotalChargedFlow().first()
        return SalaryPartialState(totalChargedMoney = money)
    }

    private suspend fun setRetentionData(helper: SalaryCalculationHelper): SalaryPartialState {
        val moneyNDFL = helper.getMoneyNDFLRetentionFlow().first()
        val moneyUnionists = helper.getMoneyUnionistsRetentionFlow().first()
        val moneyOther = helper.getMoneyOtherRetentionFlow().first()
        val moneyTotal = helper.getMoneyTotalRetentionFlow().first()
        return SalaryPartialState(retentionNdfl = moneyNDFL, unionistsRetention = moneyUnionists, otherRetention = moneyOther, totalRetention = moneyTotal)
    }

    private suspend fun setToBeCredited(helper: SalaryCalculationHelper): SalaryPartialState {
        val toBeCredited = helper.getMoneyToBeCredited().first()
        return SalaryPartialState(toBeCredited = toBeCredited)
    }
}
