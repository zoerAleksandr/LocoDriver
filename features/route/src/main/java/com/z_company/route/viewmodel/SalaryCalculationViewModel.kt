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
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.NormaUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.domain.util.currencySymbol
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import com.z_company.domain.entities.route.Route
import kotlinx.coroutines.launch
import com.z_company.core.sendToSentry
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar
import kotlin.String

internal fun shouldShowAverageHourInfo(
    underworkTime: Long,
    averagePaymentHour: Double,
    alreadyDismissed: Boolean,
): Boolean = underworkTime > 0L &&
        (!averagePaymentHour.isFinite() || averagePaymentHour <= 0.0) &&
        !alreadyDismissed

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SalaryCalculationViewModel : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val salarySettingUseCase: SalarySettingUseCase by inject()
    private val normaUseCase: NormaUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val sharedPreferenceStorage: com.z_company.domain.repositories.SharedPreferencesRepositories by inject()
    private val _userSetting = MutableStateFlow(UserSettings())
    val userSetting = _userSetting.asStateFlow()
    private var job: Job? = null
    private var setMonthJob: Job? = null

    private val _uiState = MutableStateFlow(SalaryCalculationUIState())
    val uiState = _uiState.asStateFlow()

    // Список доступных месяцев (год, месяц 0-based) из производственного календаря —
    // для стрелок переключения месяца на экране (как на главном).
    private val _monthYearList = MutableStateFlow<List<Pair<Int, Int>>>(emptyList())
    val monthYearList = _monthYearList.asStateFlow()

    init {
        viewModelScope.launch {
            calendarUseCase.loadFlowMonthOfYearListState().collect { list ->
                _monthYearList.value = list
                    .map { it.year to it.month }
                    .distinct()
                    .sortedWith(compareBy({ it.first }, { it.second }))
            }
        }
        viewModelScope.launch {
            try {
                combine(
                    settingsUseCase.getUserSettingFlow(),
                    salarySettingUseCase.salarySettingFlow()
                ) { userRes, salaryRes ->
                    _userSetting.value = userRes
                    Pair(userRes, salaryRes)
                }.flatMapLatest { (userRes, salaryRes) ->
                    // Реактивная подписка на маршруты: при добавлении/удалении маршрута
                    // автоматически пересчитывается зарплата без смены настроек.
                    routeUseCase.listRoutesByMonth(
                        userRes.selectMonthOfYear,
                        TimeCalculationContext.from(userRes)
                    )
                        .filter { it is ResultState.Success }
                        .map { Triple(userRes, salaryRes, (it as ResultState.Success).data) }
                }.collectLatest { (userRes, salaryRes, routes) ->
                    _uiState.update { it.copy(screenState = ResultState.Loading("Пересчет...")) }
                    calculationSalary(userRes, salaryRes, routes)
                }
            } catch (e: Exception) {
                e.sendToSentry("SalaryCalculationViewModel", "init")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
        setMonthJob?.cancel()
        viewModelScope.coroutineContext.cancelChildren()  // Отмена всех дочерних корутин
    }

    /**
     * Переключить месяц расчёта. Записываем выбранный месяц в настройки —
     * реактивная подписка в init пересчитает зарплату за новый месяц.
     * [yearAndMonth] — (год, месяц 0-based), как в [monthYearList].
     */
    fun selectYearAndMonth(yearAndMonth: Pair<Int, Int>) {
        setMonthJob?.cancel()
        setMonthJob = viewModelScope.launch {
            val month = calendarUseCase.loadFlowMonthOfYearListState().first()
                .find { it.year == yearAndMonth.first && it.month == yearAndMonth.second }
                ?: return@launch
            settingsUseCase.setCurrentMonthOfYear(month).first()
        }
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
        salarySetting: SalarySetting,
        allRoutes: List<Route>
    ) {
        val currentTimeInMillis = Calendar.getInstance().timeInMillis
        val currentMonthOfYear = userSettings.selectMonthOfYear
        val currency = currencySymbol(userSettings.country)
        val routeList = if (userSettings.isConsiderFutureRoute) {
            allRoutes
        } else {
            allRoutes.filter { it.basicData.timeStartWork!! < currentTimeInMillis }
        }
        val effectiveNormaHours = computeEffectiveNormaHours(currentMonthOfYear)
        val annualOvertimeBeforeMonth = computeAnnualOvertimeBeforeMonth(
            userSettings = userSettings,
            salarySetting = salarySetting,
            selectedMonth = currentMonthOfYear,
        )
        run {
            val salaryCalculationHelper = SalaryCalculationHelper(
                userSettings = userSettings,
                salarySetting = salarySetting,
                allRoutes = routeList,
                effectiveNormaHoursForUnderwork = effectiveNormaHours,
                annualOvertimeBeforePeriod = annualOvertimeBeforeMonth,
                workScheduleProfile = sharedPreferenceStorage.getWorkScheduleProfile(),
            )
            job?.cancel()
            // Параллельный вызов методов с ожиданием завершения
            job = CoroutineScope(Dispatchers.IO).launch {
                // Список асинхронных задач, каждая возвращает PartialState
                val tasks = listOf(
                    async { setHeaderData(currentMonthOfYear, salaryCalculationHelper, currency) },
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
                    async { setLinearMileageData(salaryCalculationHelper) },
                    async { setSurchargeExtendedServicePhase(salaryCalculationHelper) },
                    async { setSurchargeOnePersonOperationData(salaryCalculationHelper) },
                    async { setSurchargeOnePersonOperationPassengerTrainData(salaryCalculationHelper) },
                    async { setSurchargeHarmfulnessData(salaryCalculationHelper) },
                    async { setSurchargeHeavyTransData(salaryCalculationHelper) },
                    async { setSurchargeLongTrainData(salaryCalculationHelper) },
                    async { setSurchargeHeavyLongDistanceTrainsData(salaryCalculationHelper) },
                    async { setSurchargeDoubledTrainData(salaryCalculationHelper) },
                    async { setZonalSurchargeData(salaryCalculationHelper) },
                    async { setOvertimeData(salaryCalculationHelper) },
                    async { setSurchargeOvertimeData(salaryCalculationHelper) },
                    async { setDistrictSurchargeData(salaryCalculationHelper) },
                    async { setNordicSurchargeData(salaryCalculationHelper) },
                    async { setAveragePaymentData(salaryCalculationHelper) },
                    async { setUnderworkData(salaryCalculationHelper, salarySetting) },
                    async { setBusinessTripData(salaryCalculationHelper) },
                    async { setTechnicalStudyData(salaryCalculationHelper) },
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
                        month = mergeNonEmptyText(acc.month, partial.month),
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
                        linearMileageDistance = partial.linearMileageDistance
                            ?: acc.linearMileageDistance,
                        linearMileageMoney = partial.linearMileageMoney
                            ?: acc.linearMileageMoney,
                        linearMileageAccruals = partial.linearMileageAccruals.ifEmpty {
                            acc.linearMileageAccruals
                        },
                        surchargeExtendedServicePhaseHour = partial.surchargeExtendedServicePhaseHour.ifEmpty { acc.surchargeExtendedServicePhaseHour },
                        surchargeExtendedServicePhasePercent = partial.surchargeExtendedServicePhasePercent.ifEmpty { acc.surchargeExtendedServicePhasePercent },
                        surchargeExtendedServicePhaseMoney = partial.surchargeExtendedServicePhaseMoney.ifEmpty { acc.surchargeExtendedServicePhaseMoney },
                        surchargeHeavyTransHour = partial.surchargeHeavyTransHour.ifEmpty { acc.surchargeHeavyTransHour },
                        surchargeHeavyTransPercent = partial.surchargeHeavyTransPercent.ifEmpty { acc.surchargeHeavyTransPercent },
                        surchargeHeavyTransMoney = partial.surchargeHeavyTransMoney.ifEmpty { acc.surchargeHeavyTransMoney },
                        surchargeLongTrainHour = partial.surchargeLongTrainHour.ifEmpty { acc.surchargeLongTrainHour },
                        surchargeLongTrainPercent = partial.surchargeLongTrainPercent.ifEmpty { acc.surchargeLongTrainPercent },
                        surchargeLongTrainMoney = partial.surchargeLongTrainMoney.ifEmpty { acc.surchargeLongTrainMoney },
                        surchargeHeavyLongDistanceTrainsHours = partial.surchargeHeavyLongDistanceTrainsHours
                            ?: acc.surchargeHeavyLongDistanceTrainsHours,
                        surchargeHeavyLongDistanceTrainsPercent = partial.surchargeHeavyLongDistanceTrainsPercent
                            ?: acc.surchargeHeavyLongDistanceTrainsPercent,
                        surchargeHeavyLongDistanceTrainsMoney = partial.surchargeHeavyLongDistanceTrainsMoney
                            ?: acc.surchargeHeavyLongDistanceTrainsMoney,
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
                        onePersonOperationHours = partial.onePersonOperationHours
                            ?: acc.onePersonOperationHours,
                        onePersonOperationPercent = partial.onePersonOperationPercent
                            ?: acc.onePersonOperationPercent,
                        onePersonOperationMoney = partial.onePersonOperationMoney
                            ?: acc.onePersonOperationMoney,
                        onePersonOperationPassengerTrainHours = partial.onePersonOperationPassengerTrainHours
                            ?: acc.onePersonOperationPassengerTrainHours,
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
                        underworkTime = partial.underworkTime ?: acc.underworkTime,
                        underworkMoney = partial.underworkMoney ?: acc.underworkMoney,
                        showSetAverageHourInfo = partial.showSetAverageHourInfo
                            ?: acc.showSetAverageHourInfo,
                        caringForDisableChildrenHours = partial.caringForDisableChildrenHours
                            ?: acc.caringForDisableChildrenHours,
                        caringForDisableChildrenMoney = partial.caringForDisableChildrenMoney
                            ?: acc.caringForDisableChildrenMoney,
                        businessTripHours = partial.businessTripHours
                            ?: acc.businessTripHours,
                        businessTripMoney = partial.businessTripMoney
                            ?: acc.businessTripMoney,
                        technicalStudyHours = partial.technicalStudyHours
                            ?: acc.technicalStudyHours,
                        technicalStudyMoney = partial.technicalStudyMoney
                            ?: acc.technicalStudyMoney,
                        totalChargedMoney = partial.totalChargedMoney ?: acc.totalChargedMoney,
                        retentionNdfl = partial.retentionNdfl ?: acc.retentionNdfl,
                        unionistsRetention = partial.unionistsRetention ?: acc.unionistsRetention,
                        otherSurchargeMoney = partial.otherSurchargeMoney
                            ?: acc.otherSurchargeMoney,
                        otherSurchargePercent = partial.otherSurchargePercent
                            ?: acc.otherSurchargePercent,
                        otherRetention = partial.otherRetention ?: acc.otherRetention,
                        welfareRetention = partial.welfareRetention ?: acc.welfareRetention,
                        alimonyRetention = partial.alimonyRetention ?: acc.alimonyRetention,
                        totalRetention = partial.totalRetention ?: acc.totalRetention,
                        toBeCredited = partial.toBeCredited ?: acc.toBeCredited
                    )
                }

                // Теперь один раз обновляем _uiState, используя combinedPartial
                _uiState.update { currentState ->
                    currentState.copy(  // Пояснение: copy на полном UIState, заменяем все поля из combinedPartial
                        screenState = ResultState.Success(Unit),  // Устанавливаем Success в конце
                        month = combinedPartial.month,
                        monthIndex = currentMonthOfYear.month,
                        year = currentMonthOfYear.year,
                        currency = currency,
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
                        linearMileageDistance = combinedPartial.linearMileageDistance,
                        linearMileageMoney = combinedPartial.linearMileageMoney,
                        linearMileageAccruals = combinedPartial.linearMileageAccruals,
                        surchargeExtendedServicePhaseHour = combinedPartial.surchargeExtendedServicePhaseHour,
                        surchargeExtendedServicePhasePercent = combinedPartial.surchargeExtendedServicePhasePercent,
                        surchargeExtendedServicePhaseMoney = combinedPartial.surchargeExtendedServicePhaseMoney,
                        surchargeHeavyTransHour = combinedPartial.surchargeHeavyTransHour,
                        surchargeHeavyTransPercent = combinedPartial.surchargeHeavyTransPercent,
                        surchargeHeavyTransMoney = combinedPartial.surchargeHeavyTransMoney,
                        surchargeLongTrainHour = combinedPartial.surchargeLongTrainHour,
                        surchargeLongTrainPercent = combinedPartial.surchargeLongTrainPercent,
                        surchargeLongTrainMoney = combinedPartial.surchargeLongTrainMoney,
                        surchargeHeavyLongDistanceTrainsHours = combinedPartial.surchargeHeavyLongDistanceTrainsHours,
                        surchargeHeavyLongDistanceTrainsPercent = combinedPartial.surchargeHeavyLongDistanceTrainsPercent,
                        surchargeHeavyLongDistanceTrainsMoney = combinedPartial.surchargeHeavyLongDistanceTrainsMoney,
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
                        onePersonOperationHours = combinedPartial.onePersonOperationHours,
                        onePersonOperationPercent = combinedPartial.onePersonOperationPercent,
                        onePersonOperationMoney = combinedPartial.onePersonOperationMoney,
                        onePersonOperationPassengerTrainHours = combinedPartial.onePersonOperationPassengerTrainHours,
                        onePersonOperationPassengerTrainPercent = combinedPartial.onePersonOperationPassengerTrainPercent,
                        onePersonOperationPassengerTrainMoney = combinedPartial.onePersonOperationPassengerTrainMoney,
                        restInExcessOfTheNormTime = combinedPartial.restInExcessOfTheNormTime,
                        restInExcessOfTheNormMoney = combinedPartial.restInExcessOfTheNormMoney,
                        harmfulnessSurchargePercent = combinedPartial.harmfulnessSurchargePercent,
                        harmfulnessSurchargeMoney = combinedPartial.harmfulnessSurchargeMoney,
                        averagePaymentHours = combinedPartial.averagePaymentHours,
                        averagePaymentMoney = combinedPartial.averagePaymentMoney,
                        underworkHours = combinedPartial.underworkTime,
                        underworkMoney = combinedPartial.underworkMoney,
                        showSetAverageHourInfo = combinedPartial.showSetAverageHourInfo ?: false,
                        caringForDisableChildrenHours = combinedPartial.caringForDisableChildrenHours,
                        caringForDisableChildrenMoney = combinedPartial.caringForDisableChildrenMoney,
                        businessTripHours = combinedPartial.businessTripHours,
                        businessTripMoney = combinedPartial.businessTripMoney,
                        technicalStudyHours = combinedPartial.technicalStudyHours,
                        technicalStudyMoney = combinedPartial.technicalStudyMoney,
                        totalChargedMoney = combinedPartial.totalChargedMoney,
                        retentionNdfl = combinedPartial.retentionNdfl,
                        unionistsRetention = combinedPartial.unionistsRetention,
                        otherSurchargeMoney = combinedPartial.otherSurchargeMoney,
                        otherSurchargePercent = combinedPartial.otherSurchargePercent,
                        otherRetention = combinedPartial.otherRetention,
                        welfareRetention = combinedPartial.welfareRetention,
                        alimonyRetention = combinedPartial.alimonyRetention,
                        totalRetention = combinedPartial.totalRetention,
                        toBeCredited = combinedPartial.toBeCredited
                    )
                }

            }
        }
    }

    /**
     * Для месяцев с 09.2026 суммирует фактическую переработку всех предыдущих
     * месяцев этого года. Каждый месяц использует свою норму и свои release-дни;
     * командировочные маршруты входят в фактическое время.
     */
    private suspend fun computeAnnualOvertimeBeforeMonth(
        userSettings: UserSettings,
        salarySetting: SalarySetting,
        selectedMonth: MonthOfYear,
    ): Long {
        if (!isFederalLaw144Effective(selectedMonth.year, selectedMonth.month)) return 0L

        val previousMonths = calendarUseCase.loadFlowMonthOfYearListState().first()
            .filter { it.year == selectedMonth.year && it.month < selectedMonth.month }
            .distinctBy { it.month }
            .sortedBy { it.month }

        var annualOvertime = 0L
        previousMonths.forEach { month ->
            val routesState = routeUseCase.listRoutesByMonth(
                month,
                TimeCalculationContext.from(userSettings)
            )
                .first { it is ResultState.Success || it is ResultState.Error }
            val routes = (routesState as? ResultState.Success)?.data.orEmpty()
            val settingsForMonth = userSettings.copy(selectMonthOfYear = month)
            val helper = SalaryCalculationHelper(
                userSettings = settingsForMonth,
                salarySetting = salarySetting,
                allRoutes = routes,
                workScheduleProfile = sharedPreferenceStorage.getWorkScheduleProfile(),
            )
            annualOvertime += helper.getTimeOvertimeFlow().first()
        }
        return annualOvertime
    }

    // Метод для установки заголовочных данных (месяц, норма часов, общее время работы, тариф).
    private suspend fun setHeaderData(
        currentMonthOfYear: MonthOfYear,
        helper: SalaryCalculationHelper,
        currency: String
    ): PartialState {
        // Используем NormaUseCase — учитывает региональные праздники и дни отвлечений
        val normaHours = normaUseCase.normaHoursFlow(
            year = currentMonthOfYear.year,
            month = currentMonthOfYear.month
        ).first()
        val totalWorkTime = helper.getTotalWorkTimeWithCommute().first()
        val tariffText = if (currentMonthOfYear.dateSetTariffRate == null) {
            "${currentMonthOfYear.tariffRate.str()} $currency"
        } else {
            "${currentMonthOfYear.dateSetTariffRate!!.oldRate.str()} / ${currentMonthOfYear.tariffRate.str()} $currency"
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
        val passengerInsideTime = helper.getPassengerTimeFlow().first()
        val passengerOutsideTime = helper.getPassengerOutsideWorkTimeFlow().first()
        val money = helper.getMoneyAtPassengerFlow().first() +
                helper.getMoneyAtPassengerOutsideWorkFlow().first()

        return PartialState(
            paymentAtPassengerHours = passengerInsideTime + passengerOutsideTime,
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

    private suspend fun setLinearMileageData(helper: SalaryCalculationHelper): PartialState = PartialState(
        linearMileageDistance = helper.getLinearMileageDistanceFlow().first(),
        linearMileageMoney = helper.getMoneyLinearMileageFlow().first(),
        linearMileageAccruals = helper.getLinearMileageAccrualsFlow().first(),
    )

    // Метод для установки данных по надбавке за управление одним лицом (часы, процент, сумма).
    private suspend fun setSurchargeOnePersonOperationData(helper: SalaryCalculationHelper): PartialState {
        val hours = helper.getTimeOnePersonOperationFlow().first()
        val percent = helper.getPercentOnePersonOperationFlow().first()
        val money = helper.getMoneyOnePersonOperationFlow().first()

        return PartialState(
            onePersonOperationHours = hours,
            onePersonOperationPercent = percent,
            onePersonOperationMoney = money
        )
    }

    // Метод для установки данных по надбавке за управление одним лицом в пассажирском поезде (часы, процент, сумма).
    private suspend fun setSurchargeOnePersonOperationPassengerTrainData(helper: SalaryCalculationHelper): PartialState {
        val hours = helper.getTimeOnePersonOperationPassengerTrainFlow().first()
        val percent = helper.getPercentOnePersonOperationPassengerTrainFlow().first()
        val money = helper.getMoneyOnePersonOperationPassengerTrainFlow().first()

        return PartialState(
            onePersonOperationPassengerTrainHours = hours,
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

    private suspend fun setSurchargeHeavyLongDistanceTrainsData(
        helper: SalaryCalculationHelper
    ): PartialState = PartialState(
        surchargeHeavyLongDistanceTrainsHours = helper.getTimeHeavyLongDistanceTrainsFlow().first(),
        surchargeHeavyLongDistanceTrainsPercent = helper.getPercentHeavyLongDistanceTrainsFlow().first(),
        surchargeHeavyLongDistanceTrainsMoney = helper.getMoneyHeavyLongDistanceTrainsFlow().first()
    )

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

    // Оплата недоработки (время недоработки + сумма по среднему часу).
    // Если недоработка есть, но средний час не задан — поднимаем флаг для инфо-окна.
    private suspend fun setUnderworkData(
        helper: SalaryCalculationHelper,
        salarySetting: SalarySetting
    ): PartialState {
        val time = helper.getUnderworkTimeFlow().first()
        val money = helper.getMoneyUnderworkFlow().first()
        // Инфо-окно не показываем, если пользователь уже закрыл его через «Понятно».
        val alreadyDismissed = sharedPreferenceStorage.isUnderworkInfoDismissed()
        return PartialState(
            underworkTime = time,
            underworkMoney = money,
            showSetAverageHourInfo = shouldShowAverageHourInfo(
                underworkTime = time,
                averagePaymentHour = salarySetting.averagePaymentHour,
                alreadyDismissed = alreadyDismissed,
            )
        )
    }

    // «Понятно» в инфо-окне про недоработку — запоминаем навсегда, больше не показываем.
    fun dismissUnderworkInfoForever() {
        sharedPreferenceStorage.setUnderworkInfoDismissed()
        _uiState.update { it.copy(showSetAverageHourInfo = false) }
    }

    // Норма для расчёта недоработки: текущий месяц → на сегодня; завершённый →
    // полная; будущий → 0 (недоработки нет). Месяцы 0-based (как в Calendar).
    private suspend fun computeEffectiveNormaHours(month: com.z_company.domain.entities.MonthOfYear): Int {
        val calendar = Calendar.getInstance()
        val nowYm = calendar.get(Calendar.YEAR) * 12 + calendar.get(Calendar.MONTH)
        val selectedYm = month.year * 12 + month.month
        return when {
            selectedYm > nowYm -> 0
            selectedYm < nowYm -> normaUseCase.normaHoursFlow(month.year, month.month).first()
            else -> normaUseCase.normaHoursToDateFlow(
                month.year, month.month, calendar.get(Calendar.DAY_OF_MONTH)
            ).first()
        }
    }

    // Метод для установки данных по командировке (часы, сумма по среднему).
    private suspend fun setBusinessTripData(helper: SalaryCalculationHelper): PartialState {
        val hours = helper.getBusinessTripTimeFlow().first()
        val money = helper.getMoneyBusinessTripFlow().first()

        return PartialState(
            businessTripHours = hours,
            businessTripMoney = money
        )
    }

    // Метод для установки данных по техническим занятиям (часы, сумма по среднему).
    private suspend fun setTechnicalStudyData(helper: SalaryCalculationHelper): PartialState {
        val hours = helper.getTechnicalStudyTimeFlow().first()
        val money = helper.getMoneyTechnicalStudyFlow().first()

        return PartialState(
            technicalStudyHours = hours,
            technicalStudyMoney = money
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
        val moneyWelfare = helper.getMoneyWelfareRetentionFlow().first()
        val moneyAlimony = helper.getMoneyAlimonyRetentionFlow().first()
        val moneyTotal = helper.getMoneyTotalRetentionFlow().first()

        return PartialState(
            retentionNdfl = moneyNDFL,
            unionistsRetention = moneyUnionists,
            otherRetention = moneyOther,
            welfareRetention = moneyWelfare,
            alimonyRetention = moneyAlimony,
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
    val linearMileageDistance: Double? = null,
    val linearMileageMoney: Double? = null,
    val linearMileageAccruals: List<LinearMileageAccrual> = emptyList(),
    val surchargeExtendedServicePhaseHour: List<Long?> = emptyList(),
    val surchargeExtendedServicePhasePercent: List<String?> = emptyList(),
    val surchargeExtendedServicePhaseMoney: List<Double?> = emptyList(),
    val surchargeHeavyTransHour: List<Long?> = emptyList(),
    val surchargeHeavyTransPercent: List<String?> = emptyList(),
    val surchargeHeavyTransMoney: List<Double?> = emptyList(),
    val surchargeLongTrainHour: List<Long?> = emptyList(),
    val surchargeLongTrainPercent: List<String?> = emptyList(),
    val surchargeLongTrainMoney: List<Double?> = emptyList(),
    val surchargeHeavyLongDistanceTrainsHours: Long? = null,
    val surchargeHeavyLongDistanceTrainsPercent: Double? = null,
    val surchargeHeavyLongDistanceTrainsMoney: Double? = null,
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
    val onePersonOperationHours: Long? = null,
    val onePersonOperationPercent: Double? = null,
    val onePersonOperationMoney: Double? = null,
    val onePersonOperationPassengerTrainHours: Long? = null,
    val onePersonOperationPassengerTrainPercent: Double? = null,
    val onePersonOperationPassengerTrainMoney: Double? = null,
    val restInExcessOfTheNormTime: Long? = null,
    val restInExcessOfTheNormMoney: Double? = null,
    val harmfulnessSurchargePercent: Double? = null,
    val harmfulnessSurchargeMoney: Double? = null,
    val averagePaymentHours: Long? = null,
    val averagePaymentMoney: Double? = null,
    val underworkTime: Long? = null,
    val underworkMoney: Double? = null,
    val showSetAverageHourInfo: Boolean? = null,
    val caringForDisableChildrenHours: Long? = null,
    val caringForDisableChildrenMoney: Double? = null,
    val businessTripHours: Long? = null,
    val businessTripMoney: Double? = null,
    val technicalStudyHours: Long? = null,
    val technicalStudyMoney: Double? = null,
    val totalChargedMoney: Double? = null,
    val retentionNdfl: Double? = null,
    val unionistsRetention: Double? = null,
    val otherSurchargeMoney: Double? = null,
    val otherSurchargePercent: Double? = null,
    val otherRetention: Double? = null,
    val welfareRetention: Double? = null,
    val alimonyRetention: Double? = null,
    val totalRetention: Double? = null,
    val toBeCredited: Double? = null
)

internal fun mergeNonEmptyText(current: String, next: String): String =
    next.takeIf { it.isNotEmpty() } ?: current
