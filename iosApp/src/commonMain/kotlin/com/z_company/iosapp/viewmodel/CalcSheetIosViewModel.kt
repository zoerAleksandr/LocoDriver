package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.OverRestRoutes.overRestPayment
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getPureWorkTime
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.salary.SalaryCalculationHelper
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.domain.util.currencySymbol
import com.z_company.domain.util.toMoneyString
import com.z_company.domain.util.usesOnePersonPassengerRate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * «Расчёт за смену» (§11.9) для маршрута [basicId].
 *
 * Считает тем же `SalaryCalculationHelper(allRoutes = [маршрут])`, что и форма
 * маршрута: `totalPayment` = тариф + ночные + зональная + пассажиром + ожидание +
 * проезд до явки + праздничные + пробег + поездные доплаты + одно лицо +
 * «прочие доплаты» + переотдых + командировка, без округления.
 * ⚠️ BUG-40 (паритет с Android): ожидание пассажиром (018M) входит в
 * «Заработано», но отдельной строки в шторке нет.
 * ⚠️ BUG-41 (паритет с Android): расчёт использует `selectMonthOfYear`
 * (ставку и границы месяца), а не месяц маршрута.
 */
class CalcSheetIosViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val salarySettingUseCase: SalarySettingUseCase,
    private val basicId: String,
) : ViewModel() {

    private val _salary = MutableStateFlow(SalaryUi())
    val salary: StateFlow<SalaryUi> = _salary.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsUseCase.getUserSetting()
            val salSetting = salarySettingUseCase.getSalarySetting()
            val routeState = routeUseCase.routeDetails(basicId).first()
            val route = (routeState as? ResultState.Success)?.data
            if (route == null) {
                _salary.value = SalaryUi(isCalculated = false)
                return@launch
            }
            compute(route, settings, salSetting)
        }
    }

    private suspend fun compute(route: Route, setting: UserSettings, salSetting: SalarySetting) {
        val currency = currencySymbol(setting.country)
        val workTime = route.getPureWorkTime()
        val subtitle = route.basicData.number?.takeIf { it.isNotBlank() }?.let { "Маршрут №$it" }
        if (workTime == null) {
            _salary.value = SalaryUi(subtitle = subtitle, isCalculated = false)
            return
        }
        val tariffRate = setting.selectMonthOfYear.tariffRate
        val isSetTariffRate = tariffRate != 0.0
        val helper = SalaryCalculationHelper(
            userSettings = setting,
            salarySetting = salSetting,
            allRoutes = listOf(route),
        )

        val moneyTariff = helper.getMoneyAtWorkTimeAtTariffSingleRoute().first().coerceAtLeast(0.0)
        val workForPay = helper.getWorkTimeAtTariffSingleRouteFlow().first().coerceAtLeast(0L)
        val night = helper.getMoneyAtNightTimeFlow().first()
        val nightTime = helper.getNightTimeFlow().first()
        val zonal = helper.getMoneyZonalSurchargeFlow().first()
        val zonalPercent = helper.getPercentZonalSurchargeFlow().first()
        val zonalTime = helper.getTimeZonalSurchargeFlow().first()
        val passenger = helper.getMoneyAtPassengerFlow().first()
        val passengerTime = helper.getPassengerTimeFlow().first()
        val waiting = helper.getMoneyAtPassengerWaitingFlow().first()
        val waitingTime = helper.getPassengerWaitingTimeFlow().first()
        val outside = helper.getMoneyAtPassengerOutsideWorkFlow().first()
        val outsideTime = helper.getPassengerOutsideWorkTimeFlow().first()
        val holiday = helper.getMoneyAtHolidayFlow().first()
        val holidayTime = helper.getHolidayTimeFlow().first()
        val mileageMoney = helper.getMoneyLinearMileageFlow().first()
        val accruals = helper.getLinearMileageAccrualsFlow().first()
        val extended = helper.getMoneyListSurchargeExtendedServicePhaseFlow().first().sum()
        val heavy = helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first().sum()
        val long = helper.getMoneyListSurchargeLongTrainsFlow().first().sum()
        val doubled1 = helper.getMoneyDoubledTrainFirstSurchargeFlow(listOf(route)).first()
        val doubled2 = helper.getMoneyDoubledTrainSecondSurchargeFlow(listOf(route)).first()
        val qualification = helper.getMoneyAtQualificationClassFlow().first()
        val nordic = helper.getMoneyNordicSurcharge().first()
        val district = helper.getMoneyDistrictSurcharge().first()
        val harmful = helper.getMoneyHarmfulnessFlow().first()
        val other = helper.getMoneyOtherSurchargeFlow().first()
        val previous = routeUseCase
            .previousRouteForOverRestFlow(route, TimeCalculationContext.from(setting))
            .first()
        val overRestPayment = route.overRestPayment(
            previous = previous,
            minTimeRest = setting.minTimeRestPointOfTurnover,
            tariffRate = tariffRate,
        )
        val overRestMoney = overRestPayment.money
        val overRestTime = overRestPayment.timeMillis
        val isPassengerTrain = route.usesOnePersonPassengerRate()
        val onePerson = if (isPassengerTrain) {
            helper.getMoneyOnePersonOperationPassengerTrainFlow().first()
        } else {
            helper.getMoneyOnePersonOperationFlow().first()
        }
        val onePersonTime = if (isPassengerTrain) {
            helper.getTimeOnePersonOperationPassengerTrainFlow().first()
        } else {
            helper.getTimeOnePersonOperationFlow().first()
        }
        val onePersonPercent = if (isPassengerTrain) {
            salSetting.onePersonOperationPassengerTrainPercent
        } else {
            salSetting.onePersonOperationPercent
        }
        val businessTripMoney = helper.getMoneyBusinessTripFlow().first()
        val hasBusinessTripPart = helper.hasBusinessTripRoutes()

        // Жёсткий инвариант командировки: маршрут целиком в командировке — только её оплата.
        if (helper.isEntirelyBusinessTrip()) {
            _salary.value = SalaryUi(
                subtitle = subtitle,
                isCalculated = true,
                total = businessTripMoney.toMoneyString(currency),
                rows = listOfNotNull(
                    businessTripMoney.takeIf { it != 0.0 }?.let {
                        SalaryRowUi("Командировка (по среднему)", "оплата только по среднему часу", it.toMoneyString(currency))
                    }
                ),
                averageHourMissing = businessTripMoney == 0.0,
                tariffMissing = !isSetTariffRate,
            )
            return
        }

        val surchargeAtTrains = extended + heavy + long + doubled1 + doubled2
        val trainTypes = buildList {
            if (heavy != 0.0) add("тяжеловесный")
            if (long != 0.0) add("длинносоставный")
            if (extended != 0.0) add("удлинённое плечо")
            if (doubled1 + doubled2 != 0.0) add("сдвоенный")
        }
        val total = moneyTariff + night + zonal + passenger + waiting + outside + holiday +
            mileageMoney + surchargeAtTrains + onePerson + (qualification + nordic + district + harmful + other) +
            overRestMoney + businessTripMoney

        val rateStr = tariffRate.takeIf { it != 0.0 }?.toMoneyString(currency)
        fun timeRatePercentHint(time: Long?, percent: Double?, fallback: String, percentText: String? = null): String =
            if (rateStr != null && time != null && time > 0L && (percent != null || percentText != null)) {
                val pct = percentText ?: "${formatHintPercent(percent!!)} %"
                "${formatHintHhMm(time)} × $rateStr/ч × $pct"
            } else fallback

        fun timeRateHint(time: Long?, fallback: String): String =
            if (rateStr != null && time != null && time > 0L) "${formatHintHhMm(time)} × $rateStr/ч" else fallback

        val hourlyHint = if (rateStr != null) "${formatHintHhMm(workForPay)} × $rateStr/ч" else "оплата рабочего времени по тарифу"
        val holidayHint = holidayTime.takeIf { it > 0L }
            ?.let { "${formatHintHhMm(it)} в праздничные дни" } ?: "доплата за работу в праздничные дни"

        val rows = mutableListOf<SalaryRowUi>()
        fun add(label: String, hint: String, value: Double?) {
            if (value != null && value != 0.0) rows.add(SalaryRowUi(label, hint, value.toMoneyString(currency)))
        }
        add("Почасовая оплата", hourlyHint, moneyTariff)
        add("Праздничные", holidayHint, holiday)
        add("Зональная надбавка", timeRatePercentHint(zonalTime, zonalPercent, "надбавка за разъездной характер работы"), zonal)
        add("Ночные", timeRatePercentHint(nightTime, salSetting.nightTimePercent, "доплата за работу в ночное время"), night)
        add("Пассажиром", timeRateHint(passengerTime, "оплата времени следования пассажиром"), passenger)
        add("Пассажиром до явки", timeRateHint(outsideTime, "следование пассажиром до явки"), outside)
        // BUG-40 (паритет с Android): ожидание 018M входит в итог; строка
        // показывается при ненулевой сумме.
        add("Ожидание пассажиром", timeRateHint(waitingTime, "ожидание следования пассажиром"), waiting)
        accruals.forEach { accrual ->
            val distance = if (accrual.distance % 1.0 == 0.0) accrual.distance.toLong().toString()
            else formatHintOneDecimal(accrual.distance)
            add("Доплата за пробег", "${accrual.phaseName}: $distance км × ${accrual.rate.toMoneyString(currency)}/км", accrual.money)
        }
        add("Одно лицо", timeRatePercentHint(onePersonTime, onePersonPercent, "надбавка за работу в одно лицо"), onePerson)
        add("Доплаты за поезд", trainTypes.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "доплаты за категорию поезда", surchargeAtTrains)
        add("Прочие доплаты", "классность, северная, районный, вредность, прочие", qualification + nordic + district + harmful + other)
        add("Переотдых", timeRatePercentHint(overRestTime, null, "компенсация за переотдых", percentText = "2/3"), overRestMoney)
        add("Командировка (по среднему)", "оплата только по среднему часу", businessTripMoney)

        _salary.value = SalaryUi(
            subtitle = subtitle,
            isCalculated = true,
            total = total.toMoneyString(currency),
            rows = rows,
            averageHourMissing = hasBusinessTripPart && businessTripMoney == 0.0,
            tariffMissing = !isSetTariffRate,
        )
    }

    private fun formatHintHhMm(ms: Long): String {
        val totalMinutes = ms / 60_000L
        return "${totalMinutes / 60}:${(totalMinutes % 60).toString().padStart(2, '0')}"
    }

    private fun formatHintPercent(value: Double): String {
        val rounded = kotlin.math.floor(value * 10 + 0.5) / 10
        val intPart = rounded.toLong()
        val frac = kotlin.math.round((rounded - intPart) * 10).toInt()
        return "$intPart,$frac"
    }

    private fun formatHintOneDecimal(value: Double): String {
        val rounded = kotlin.math.floor(value * 10 + 0.5) / 10
        return rounded.str()
    }

    // ── Watch callbacks (Swift) ──────────────────────────────────────────────

    fun watchSalary(callback: (SalaryUi) -> Unit) {
        viewModelScope.launch { salary.collect { callback(it) } }
    }
}

private fun Double.str(): String {
    val s = toString()
    return if (s.endsWith(".0")) s.dropLast(2) else s
}
