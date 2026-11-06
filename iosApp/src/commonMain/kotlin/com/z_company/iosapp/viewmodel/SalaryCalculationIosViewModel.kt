@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.entities.salary.SalaryPaymentId
import com.z_company.domain.salary.SalaryCalculationHelper
import com.z_company.domain.salary.isFederalLaw144Effective
import com.z_company.domain.salary.isUnderworkPeriodClosed
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.NormaUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.core.ResultState
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.domain.util.str
import com.z_company.domain.util.str2decimalSign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * KMP ViewModel экрана «Расчёт зарплаты» (§11).
 *
 * Формулы — только общий `SalaryCalculationHelper` (§11.1): iOS передаёт все
 * входы таблицы §11.1 (включая `annualOvertimeBeforePeriod` и
 * `workScheduleProfile`), чтобы результат совпадал с Android до копейки.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SalaryCalculationIosViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val salarySettingUseCase: SalarySettingUseCase,
    private val normaUseCase: NormaUseCase,
    private val calendarUseCase: CalendarUseCase,
    private val sharedPrefs: SharedPreferencesRepositories,
) : ViewModel() {

    private val _statement = MutableStateFlow(SalaryStatement())
    val statement: StateFlow<SalaryStatement> = _statement.asStateFlow()

    private val _monthYearList = MutableStateFlow<List<MonthYearItem>>(emptyList())
    val monthYearList: StateFlow<List<MonthYearItem>> = _monthYearList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Скрыто до конца сессии экрана (тап вне инфо-окна / «В настройки»). */
    private var underworkInfoHiddenForSession = false

    private var calcJob: Job? = null

    init {
        viewModelScope.launch {
            calendarUseCase.loadFlowMonthOfYearListState().collect { list ->
                _monthYearList.value = list
                    .map { it.year to it.month }
                    .distinct()
                    .sortedWith(compareBy({ it.first }, { it.second }))
                    .map { (year, month) -> MonthYearItem(year, month, salaryMonthLabel(month, year)) }
            }
        }
        viewModelScope.launch {
            combine(
                settingsUseCase.getUserSettingFlow(),
                salarySettingUseCase.salarySettingFlow(),
                calendarUseCase.loadFlowMonthOfYearListState(),
            ) { user, salary, months -> Triple(user, salary, months) }
                .flatMapLatest { (user, salary, months) ->
                    val context = TimeCalculationContext.from(user)
                    combine(
                        routeUseCase.listRoutesByMonth(user.selectMonthOfYear, context)
                            .map { state -> (state as? ResultState.Success)?.data ?: emptyList() },
                        routeUseCase.adjacentRoutesOfMonthFlow(user.selectMonthOfYear, context),
                    ) { routes, adjacent -> CalcInput(user, salary, months, routes, adjacent) }
                }
                .collectLatest { input -> recalculate(input) }
        }
    }

    private data class CalcInput(
        val user: UserSettings,
        val salary: SalarySetting,
        val months: List<MonthOfYear>,
        val routes: List<Route>,
        val adjacent: List<Route>,
    )

    // ── Пересчёт (§11.1) ────────────────────────────────────────────────────

    private suspend fun recalculate(input: CalcInput) = withContext(Dispatchers.Default) {
        _isLoading.value = true
        val user = input.user
        val moy = user.selectMonthOfYear
        val context = TimeCalculationContext.from(user)
        val nowMs = Clock.System.now().toEpochMilliseconds()
        // BUG-35 (паритет с Android): фильтр будущих использует timeStartWork!!,
        // маршрут месяца без явки дал бы падение (на практике SQL-выборка такие
        // маршруты, вероятно, не возвращает — ❓ не проверено).
        val routeList = if (user.isConsiderFutureRoute) {
            input.routes
        } else {
            input.routes.filter { it.basicData.timeStartWork!! < nowMs }
        }
        val adjacentRouteList = if (user.isConsiderFutureRoute) {
            input.adjacent
        } else {
            input.adjacent.filter { it.basicData.timeStartWork!! < nowMs }
        }
        val effectiveNormaHours = effectiveNormaHours(moy)
        val annualOvertime = computeAnnualOvertimeBeforeMonth(user, input.salary, moy, input.months)
        val helper = SalaryCalculationHelper(
            userSettings = user,
            salarySetting = input.salary,
            allRoutes = routeList,
            effectiveNormaHoursForUnderwork = effectiveNormaHours,
            annualOvertimeBeforePeriod = annualOvertime,
            workScheduleProfile = sharedPrefs.getWorkScheduleProfile(),
            adjacentRoutes = adjacentRouteList,
        )

        val isDecimal = user.isDecimalTime
        val currency = com.z_company.domain.util.currencySymbol(user.country)

        // Все денежные потоки — из того же helper (единственный источник формул).
        val tariffHours = helper.getWorkTimeAtTariffFlow().first()
        val tariffMoney = helper.getMoneyAtWorkTimeAtTariff().first()
        val nightHours = helper.getNightTimeFlow().first()
        val nightPercent = input.salary.nightTimePercent
        val nightMoney = helper.getMoneyAtNightTimeFlow().first()
        val passengerHours = helper.getPassengerTimeFlow().first() + helper.getPassengerOutsideWorkTimeFlow().first()
        val passengerMoney = helper.getMoneyAtPassengerFlow().first() + helper.getMoneyAtPassengerOutsideWorkFlow().first()
        val waitingHours = helper.getPassengerWaitingTimeFlow().first()
        val waitingMoney = helper.getMoneyAtPassengerWaitingFlow().first()
        val reserveHours = helper.getSingleLocomotiveTimeFlow().first()
        val reserveMoney = helper.getMoneyAtSingleLocomotiveFlow().first()
        val holidayHours = helper.getHolidayTimeFlow().first()
        val holidayMoney = helper.getMoneyAtHolidayFlow().first()
        val averageHours = helper.getDayOffHoursFlow().first()
        val averageMoney = helper.getMoneyAverageFlow().first()
        val underworkHours = helper.getUnderworkTimeFlow().first()
        val underworkMoney = helper.getMoneyUnderworkFlow().first()
        val childCareHours = helper.getHoursCaringForDisableChildren().first()
        val childCareMoney = helper.getMoneyCaringForDisableChildren().first()
        val businessTripHours = helper.getBusinessTripTimeFlow().first()
        val businessTripMoney = helper.getMoneyBusinessTripFlow().first()
        val technicalHours = helper.getTechnicalStudyTimeFlow().first()
        val technicalMoney = helper.getMoneyTechnicalStudyFlow().first()
        val zonalPercent = helper.getPercentZonalSurchargeFlow().first()
        val zonalMoney = helper.getMoneyZonalSurchargeFlow().first()
        val qualificationPercent = input.salary.surchargeQualificationClass
        val qualificationMoney = helper.getMoneyAtQualificationClassFlow().first()
        val mileageAccruals = helper.getLinearMileageAccrualsFlow().first()
        val onePersonHours = helper.getTimeOnePersonOperationFlow().first()
        val onePersonPercent = helper.getPercentOnePersonOperationFlow().first()
        val onePersonMoney = helper.getMoneyOnePersonOperationFlow().first()
        val onePersonPassengerHours = helper.getTimeOnePersonOperationPassengerTrainFlow().first()
        val onePersonPassengerPercent = helper.getPercentOnePersonOperationPassengerTrainFlow().first()
        val onePersonPassengerMoney = helper.getMoneyOnePersonOperationPassengerTrainFlow().first()
        val harmfulnessPercent = helper.getPercentHarmfulnessFlow().first()
        val harmfulnessMoney = helper.getMoneyHarmfulnessFlow().first()
        val districtPercent = helper.getPercentDistrictSurcharge().first()
        val districtMoney = helper.getMoneyDistrictSurcharge().first()
        val nordicPercent = helper.getPercentNordicSurcharge().first()
        val nordicMoney = helper.getMoneyNordicSurcharge().first()
        val overRestHours = helper.getOverRestTimeFlow().first()
        val overRestMoney = helper.getMoneyOverRestFlow().first()
        val extendedHours = helper.getTimeListSurchargeServicePhaseFlow().first()
        val extendedPercents = helper.getPercentListSurchargeExtendedServicePhaseFlow().first()
        val extendedMoney = helper.getMoneyListSurchargeExtendedServicePhaseFlow().first()
        val heavyHours = helper.getTimeListSurchargeHeavyTrainsFlow().first()
        val heavyPercents = helper.getPercentListSurchargeExtendedHeavyTrainsFlow().first()
        val heavyMoney = helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first()
        val longHours = helper.getTimeListSurchargeLongTrainsFlow().first()
        val longPercents = helper.getPercentListSurchargeLongTrainsFlow().first()
        val longMoney = helper.getMoneyListSurchargeLongTrainsFlow().first()
        val pdmHours = helper.getTimeHeavyLongDistanceTrainsFlow().first()
        val pdmPercent = helper.getPercentHeavyLongDistanceTrainsFlow().first()
        val pdmMoney = helper.getMoneyHeavyLongDistanceTrainsFlow().first()
        val doubled1Hours = helper.getTimeDoubledTrainFirstSurchargeFlow().first()
        val doubled1Money = helper.getMoneyDoubledTrainFirstSurchargeFlow().first()
        val doubled2Hours = helper.getTimeDoubledTrainSecondSurchargeFlow().first()
        val doubled2Money = helper.getMoneyDoubledTrainSecondSurchargeFlow().first()
        val overtimeHours = helper.getTimeOvertimeFlow().first()
        val overtimeMoney = helper.getMoneyOvertimeFlow().first()
        val overtime05Hours = helper.getTimeSurchargeAtOvertime05Flow().first()
        val overtime05Money = helper.getMoneySurchargeOvertime05Flow().first()
        val overtime100Hours = helper.getTimeSurchargeAtOvertimeFlow().first()
        val overtime100Money = helper.getMoneySurchargeOvertimeFlow().first()
        val otherSurchargePercent = helper.getPercentOtherSurchargeFlow().first()
        val otherSurchargeMoney = helper.getMoneyOtherSurchargeFlow().first()
        val totalCharged = helper.getMoneyTotalChargedFlow().first()
        val retentionNdfl = helper.getMoneyNDFLRetentionFlow().first()
        val retentionUnion = helper.getMoneyUnionistsRetentionFlow().first()
        val retentionOther = helper.getMoneyOtherRetentionFlow().first()
        val retentionWelfare = helper.getMoneyWelfareRetentionFlow().first()
        val retentionAlimony = helper.getMoneyAlimonyRetentionFlow().first()
        val totalRetention = helper.getMoneyTotalRetentionFlow().first()
        val toBeCredited = helper.getMoneyToBeCredited().first()
        val totalWorkWithCommute = helper.getTotalWorkTimeWithCommute().first()

        // ── Строки начислений в исходном порядке §11.6 ──
        val accrualRaw = buildList {
            add(RawSalaryRow(SalaryPaymentId.TARIFF, "Оплата по тарифу", tariffHours, null, tariffMoney))
            add(RawSalaryRow(SalaryPaymentId.NIGHT, "Ночные часы", nightHours, nightPercent, nightMoney))
            add(RawSalaryRow(SalaryPaymentId.PASSENGER, "Пассажиром", passengerHours, null, passengerMoney))
            if (waitingMoney > 0) {
                add(RawSalaryRow(SalaryPaymentId.PASSENGER_WAITING, "Ожидание пассажиром", waitingHours, null, waitingMoney))
            }
            add(RawSalaryRow(SalaryPaymentId.RESERVE, "Резервом", reserveHours, null, reserveMoney))
            add(RawSalaryRow(SalaryPaymentId.HOLIDAY, "Праздничные", holidayHours, null, holidayMoney))
            add(RawSalaryRow(SalaryPaymentId.AVERAGE, "Оплата по среднему", averageHours, null, averageMoney))
            if (underworkMoney > 0) {
                add(RawSalaryRow(SalaryPaymentId.UNDERWORK, "Оплата недоработки", underworkHours, null, underworkMoney))
            }
            add(
                RawSalaryRow(
                    SalaryPaymentId.DISABLED_CHILD_CARE, "По уходу за ребенком-инвалидом",
                    childCareHours, null, childCareMoney,
                )
            )
            if (businessTripHours > 0) {
                add(RawSalaryRow(SalaryPaymentId.BUSINESS_TRIP, "Командировка (по среднему)", businessTripHours, null, businessTripMoney))
            }
            if (technicalHours > 0) {
                add(RawSalaryRow(SalaryPaymentId.TECHNICAL_STUDY, "Технические занятия", technicalHours, null, technicalMoney))
            }
            add(RawSalaryRow(SalaryPaymentId.ZONAL, "Зональная надбавка", null, zonalPercent, zonalMoney))
            add(
                RawSalaryRow(
                    SalaryPaymentId.QUALIFICATION_CLASS, "Надбавка за класс квалификации",
                    null, qualificationPercent, qualificationMoney,
                )
            )
            mileageAccruals.forEach { accrual ->
                add(
                    RawSalaryRow(
                        SalaryPaymentId.LINEAR_MILEAGE, "Доплата за пробег: ${accrual.phaseName}",
                        null, null, accrual.money,
                    )
                )
            }
            add(RawSalaryRow(SalaryPaymentId.ONE_PERSON_FREIGHT, "В одно лицо", onePersonHours, onePersonPercent, onePersonMoney))
            add(
                RawSalaryRow(
                    SalaryPaymentId.ONE_PERSON_PASSENGER, "В одно лицо (пассажирские)",
                    onePersonPassengerHours, onePersonPassengerPercent, onePersonPassengerMoney,
                )
            )
            add(RawSalaryRow(SalaryPaymentId.HARMFULNESS, "Вредность", null, harmfulnessPercent, harmfulnessMoney))
            add(RawSalaryRow(SalaryPaymentId.DISTRICT, "Районный коэффициент", null, districtPercent, districtMoney))
            add(RawSalaryRow(SalaryPaymentId.NORDIC, "Северная надбавка", null, nordicPercent, nordicMoney))
            if (overRestMoney > 0) {
                add(RawSalaryRow(SalaryPaymentId.EXCESS_REST, "Переотдых", overRestHours, null, overRestMoney))
            }
            val tiers = minOf(extendedHours.size, extendedPercents.size, extendedMoney.size)
            for (i in 0 until tiers) {
                val money = extendedMoney[i] ?: 0.0
                if (money > 0) {
                    add(
                        RawSalaryRow(
                            SalaryPaymentId.EXTENDED_SERVICE, "Удлиненное плечо (${extendedPercents[i] ?: ""}%)",
                            extendedHours[i], extendedPercents[i]?.toDoubleOrNull(), money,
                        )
                    )
                }
            }
            val heavyTiers = minOf(heavyHours.size, heavyPercents.size, heavyMoney.size)
            for (i in 0 until heavyTiers) {
                val money = heavyMoney[i] ?: 0.0
                if (money > 0) {
                    add(
                        RawSalaryRow(
                            SalaryPaymentId.HEAVY_TRAIN, "Тяжелые поезда (${heavyPercents[i] ?: ""}%)",
                            heavyHours[i], heavyPercents[i]?.toDoubleOrNull(), money,
                        )
                    )
                }
            }
            val longTiers = minOf(longHours.size, longPercents.size, longMoney.size)
            for (i in 0 until longTiers) {
                val money = longMoney[i] ?: 0.0
                if (money > 0) {
                    add(
                        RawSalaryRow(
                            SalaryPaymentId.LONG_TRAIN, "Длинносост. (${longPercents[i] ?: ""}%)",
                            longHours[i], longPercents[i]?.toDoubleOrNull(), money,
                        )
                    )
                }
            }
            if (pdmMoney > 0) {
                add(
                    RawSalaryRow(
                        SalaryPaymentId.HEAVY_LONG_DISTANCE, "Доплата за ПДМ (>6000 т. и >350 осей)",
                        pdmHours, pdmPercent, pdmMoney,
                    )
                )
            }
            if (doubled1Money > 0) {
                add(RawSalaryRow(SalaryPaymentId.DOUBLED_TRAIN, "Сдвоенные поезда (30%)", doubled1Hours, 30.0, doubled1Money))
            }
            if (doubled2Money > 0) {
                add(RawSalaryRow(SalaryPaymentId.DOUBLED_TRAIN, "Сдвоенные поезда (15%)", doubled2Hours, 15.0, doubled2Money))
            }
            add(RawSalaryRow(SalaryPaymentId.OVERTIME_BASE, "Сверхурочные часы", overtimeHours, null, overtimeMoney))
            if (overtime05Money > 0) {
                add(
                    RawSalaryRow(
                        SalaryPaymentId.OVERTIME_HALF, "Доплата за сверхурочные (50%)",
                        overtime05Hours, 50.0, overtime05Money,
                    )
                )
            }
            if (overtime100Money > 0) {
                add(
                    RawSalaryRow(
                        SalaryPaymentId.OVERTIME_FULL, "Доплата за сверхурочные (100%)",
                        overtime100Hours, 100.0, overtime100Money,
                    )
                )
            }
            add(RawSalaryRow(SalaryPaymentId.OTHER_SURCHARGE, "Прочие надбавки", null, otherSurchargePercent, otherSurchargeMoney))
        }

        val deductionRows = buildSalaryDeductionRows(
            ndfl = retentionNdfl,
            union = retentionUnion,
            welfare = retentionWelfare,
            alimony = retentionAlimony,
            other = retentionOther,
        )

        // Инфо-окно недоработки (§11.6): underworkTime > 0 и средний час не задан.
        val averageHour = input.salary.averagePaymentHour
        val showUnderworkInfo = !underworkInfoHiddenForSession &&
            !sharedPrefs.isUnderworkInfoDismissed() &&
            underworkHours > 0 &&
            !(averageHour.isFinite() && averageHour > 0)

        // BUG-38 (паритет с Android): при смене тарифа проверяется старая ставка —
        // первое число строки «старая / новая ₽».
        val tariffText = if (moy.dateSetTariffRate == null) {
            "${moy.tariffRate.str()} $currency"
        } else {
            "${moy.dateSetTariffRate!!.oldRate.str()} / ${moy.tariffRate.str()} $currency"
        }
        val firstRate = tariffText.split(' ').firstOrNull()?.toDoubleOrNull() ?: 0.0

        _statement.value = SalaryStatement(
            monthLabel = salaryMonthLabel(moy.month, moy.year),
            monthIndex = moy.month,
            year = moy.year,
            currency = currency,
            toBeCreditedText = toBeCredited.str2decimalSign(),
            totalWorkText = formatSalaryHours(totalWorkWithCommute, isDecimal),
            isDecimalTime = isDecimal,
            tariffRateText = tariffText,
            tariffMissing = firstRate == 0.0,
            accrualRows = buildSalaryAccrualRows(accrualRaw, isDecimal),
            deductionRows = deductionRows,
            totalChargedText = totalCharged.str2decimalSign(),
            totalRetentionText = totalRetention.str2decimalSign(),
            showUnderworkInfo = showUnderworkInfo,
        )
        _isLoading.value = false
    }

    /** Сумма сверхурочных с января до начала месяца (§11.1) — только после ФЗ №144. */
    private suspend fun computeAnnualOvertimeBeforeMonth(
        userSettings: UserSettings,
        salarySetting: SalarySetting,
        selectedMonth: MonthOfYear,
        months: List<MonthOfYear>,
    ): Long {
        if (!isFederalLaw144Effective(selectedMonth.year, selectedMonth.month)) return 0L
        val previousMonths = months
            .filter { it.year == selectedMonth.year && it.month < selectedMonth.month }
            .distinctBy { it.month }
            .sortedBy { it.month }
        var annualOvertime = 0L
        previousMonths.forEach { month ->
            val routes = routeUseCase.listRoutesByMonth(month, TimeCalculationContext.from(userSettings))
                .first { it is com.z_company.core.ResultState.Success || it is com.z_company.core.ResultState.Error }
                .let { (it as? com.z_company.core.ResultState.Success)?.data.orEmpty() }
            val helper = SalaryCalculationHelper(
                userSettings = userSettings.copy(selectMonthOfYear = month),
                salarySetting = salarySetting,
                allRoutes = routes,
                workScheduleProfile = sharedPrefs.getWorkScheduleProfile(),
            )
            annualOvertime += helper.getTimeOvertimeFlow().first()
        }
        return annualOvertime
    }

    /** Недоработка — только по закрытому месяцу (§12 «isUnderworkPeriodClosed»). */
    private suspend fun effectiveNormaHours(monthOfYear: MonthOfYear): Int {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return if (isUnderworkPeriodClosed(monthOfYear.year, monthOfYear.month, today)) {
            normaUseCase.normaHoursFlow(monthOfYear.year, monthOfYear.month).first()
        } else 0
    }

    // ── Месяц (§11.6 п.1, §11.7) ────────────────────────────────────────────

    /** Выбор месяца из шторки §11.7: пишет месяц в настройки (как Android selectYearAndMonth). */
    fun selectYearAndMonth(year: Int, month: Int) {
        viewModelScope.launch {
            val target = calendarUseCase.loadFlowMonthOfYearListState().first()
                .find { it.year == year && it.month == month }
                ?: return@launch
            settingsUseCase.setCurrentMonthOfYear(target).first()
        }
    }

    fun nextMonth() {
        val list = _monthYearList.value
        val current = _statement.value
        val index = list.indexOfFirst { it.year == current.year && it.month == current.monthIndex }
        val next = list.getOrNull(index + 1) ?: list.getOrNull(
            list.indexOfFirst { it.year > current.year || (it.year == current.year && it.month > current.monthIndex) }
        )
        if (next != null) selectYearAndMonth(next.year, next.month)
    }

    fun previousMonth() {
        val list = _monthYearList.value
        val current = _statement.value
        val candidates = list.filter { it.year < current.year || (it.year == current.year && it.month < current.monthIndex) }
        val prev = candidates.lastOrNull()
        if (prev != null) selectYearAndMonth(prev.year, prev.month)
    }

    // ── Инфо-окно недоработки (§11.6) ───────────────────────────────────────

    /** «Понятно» — больше никогда не показывать (флаг в SharedPreferences). */
    fun dismissUnderworkInfoForever() {
        sharedPrefs.setUnderworkInfoDismissed()
        underworkInfoHiddenForSession = true
        _statement.value = _statement.value.copy(showUnderworkInfo = false)
    }

    /** «В настройки» и тап вне окна — скрыть до конца сессии экрана. */
    fun hideUnderworkInfoForSession() {
        underworkInfoHiddenForSession = true
        _statement.value = _statement.value.copy(showUnderworkInfo = false)
    }

    // ── Watch callbacks (Swift) ──────────────────────────────────────────────

    fun watchStatement(callback: (SalaryStatement) -> Unit) {
        viewModelScope.launch { statement.collect { callback(it) } }
    }

    fun watchMonthYearList(callback: (List<MonthYearItem>) -> Unit) {
        viewModelScope.launch { monthYearList.collect { callback(it) } }
    }

    fun watchIsLoading(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isLoading.collect { callback(it) } }
    }
}
