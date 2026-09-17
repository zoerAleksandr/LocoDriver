@file:OptIn(ExperimentalJsExport::class, kotlinx.coroutines.DelicateCoroutinesApi::class)

package com.z_company.domain.salary

import com.z_company.domain.entities.WorkScheduleProfile
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getOverRestTime
import com.z_company.domain.entities.route.UtilsForEntities.getPureWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.passengerTrainNumberList
import com.z_company.domain.util.toIntOrZero
import com.z_company.domain.entities.salary.PayrollPaymentCatalog
import com.z_company.domain.entities.salary.PayrollCodeReferenceCatalog
import com.z_company.domain.entities.salary.PayrollPaymentType
import com.z_company.domain.entities.salary.SalaryPaymentId
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.Promise
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.promise
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class PwaSalaryRequest(
    val userSettings: UserSettings,
    val salarySetting: SalarySetting,
    val routes: List<Route>,
    val effectiveNormaHours: Int = 0,
    val annualOvertimeBeforePeriod: Long = 0L,
    // Личный график продолжительности рабочей недели (PWA: stores/settingsPreferences).
    // Отсутствует в запросе → стандартная пятидневка, как раньше.
    val workScheduleProfile: WorkScheduleProfile = WorkScheduleProfile.standard(),
)

@Serializable
private data class PwaSalaryLine(
    val id: String,
    val code: String,
    val plainName: String,
    val payrollSheetName: String,
    val type: String,
    val hoursMillis: Long? = null,
    val percent: Double? = null,
    val amount: Double,
)

@Serializable
private data class PwaSalaryResult(
    val month: Int,
    val year: Int,
    val tariffRate: Double,
    val normaHours: Int,
    val totalWorkedMillis: Long,
    val accruals: List<PwaSalaryLine>,
    val deductions: List<PwaSalaryLine>,
    val totalAccrued: Double,
    val totalDeducted: Double,
    val payable: Double,
)

/** Расчёт одной поездки — вход FormViewModel.calculateSalary. */
@Serializable
private data class PwaTripRequest(
    val userSettings: UserSettings,
    val salarySetting: SalarySetting,
    val route: Route,
    // Предыдущий по времени маршрут — для переотдыха в пункте оборота.
    val previousRoute: Route? = null,
    val workScheduleProfile: WorkScheduleProfile = WorkScheduleProfile.standard(),
)

@Serializable
private data class PwaTripRow(
    val id: String,
    val amount: Double,
    val hoursMillis: Long? = null,
    val percent: Double? = null,
    val hint: String? = null,
)

/** Зеркало SalaryForRouteState: итог и составляющие расчёта одной поездки. */
@Serializable
private data class PwaTripResult(
    val isCalculated: Boolean,
    val isSetTariffRate: Boolean,
    val tariffRate: Double,
    val totalPayment: Double,
    val isBusinessTrip: Boolean,
    val businessTripMoney: Double,
    val workTimeMillis: Long,
    val rows: List<PwaTripRow>,
)

@Serializable
private data class PwaCatalogItem(
    val code: String,
    val shortName: String,
    val description: String,
    val type: String,
)

/** JSON boundary used by the dependency-free PWA. All formulas stay in commonMain. */
@JsExport
object PwaSalaryBridge {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    fun catalogJson(): String = json.encodeToString(
        PayrollCodeReferenceCatalog.entries.map {
            PwaCatalogItem(
                code = it.code,
                shortName = it.shortName,
                description = it.description,
                type = it.type.name,
            )
        }
    )

    fun calculate(requestJson: String): Promise<String> = GlobalScope.promise {
        val request = json.decodeFromString<PwaSalaryRequest>(requestJson)
        val helper = SalaryCalculationHelper(
            userSettings = request.userSettings,
            salarySetting = request.salarySetting,
            allRoutes = request.routes,
            effectiveNormaHoursForUnderwork = request.effectiveNormaHours,
            annualOvertimeBeforePeriod = request.annualOvertimeBeforePeriod,
            workScheduleProfile = request.workScheduleProfile,
        )
        json.encodeToString(buildResult(request, helper))
    }

    /**
     * Расчёт одной поездки — 1:1 с Android FormViewModel.calculateSalary:
     * тот же SalaryCalculationHelper на списке из одного маршрута, без
     * сверхурочных, недоработки и удержаний; переотдых — от предыдущего
     * маршрута с отдыхом в пункте оборота.
     */
    fun calculateTrip(requestJson: String): Promise<String> = GlobalScope.promise {
        val request = json.decodeFromString<PwaTripRequest>(requestJson)
        json.encodeToString(buildTripResult(request))
    }

    private suspend fun buildTripResult(request: PwaTripRequest): PwaTripResult {
        val route = request.route
        val settings = request.userSettings
        val salary = request.salarySetting
        val tariffRate = settings.selectMonthOfYear.tariffRate
        val isSetTariffRate = tariffRate != 0.0
        val workTime = route.getPureWorkTime()
            ?: return PwaTripResult(
                isCalculated = false,
                isSetTariffRate = isSetTariffRate,
                tariffRate = tariffRate,
                totalPayment = 0.0,
                isBusinessTrip = false,
                businessTripMoney = 0.0,
                workTimeMillis = 0L,
                rows = emptyList(),
            )
        val helper = SalaryCalculationHelper(
            userSettings = settings,
            salarySetting = salary,
            allRoutes = listOf(route),
            workScheduleProfile = request.workScheduleProfile,
        )

        val businessTripMoney = helper.getMoneyBusinessTripFlow().first()
        if (helper.isEntirelyBusinessTrip()) {
            return PwaTripResult(
                isCalculated = true,
                isSetTariffRate = isSetTariffRate,
                tariffRate = tariffRate,
                totalPayment = businessTripMoney,
                isBusinessTrip = true,
                businessTripMoney = businessTripMoney,
                workTimeMillis = workTime,
                rows = if (businessTripMoney > 0.0) {
                    listOf(PwaTripRow(id = "BUSINESS_TRIP", amount = businessTripMoney, hoursMillis = workTime))
                } else {
                    emptyList()
                },
            )
        }

        val moneyAtTariffRate = helper.getMoneyAtWorkTimeAtTariffSingleRoute().first().coerceAtLeast(0.0)
        val workTimeForPay = helper.getWorkTimeAtTariffSingleRouteFlow().first().coerceAtLeast(0L)
        val moneyAtNightHours = helper.getMoneyAtNightTimeFlow().first()
        val nightTime = helper.getNightTimeFlow().first()
        val zonalSurchargeMoney = helper.getMoneyZonalSurchargeFlow().first()
        val zonalPercent = helper.getPercentZonalSurchargeFlow().first()
        val zonalTime = helper.getTimeZonalSurchargeFlow().first()
        val moneyAtPassengerTime = helper.getMoneyAtPassengerFlow().first()
        val passengerTime = helper.getPassengerTimeFlow().first()
        val moneyAtPassengerWaiting = helper.getMoneyAtPassengerWaitingFlow().first()
        val passengerWaitingTime = helper.getPassengerWaitingTimeFlow().first()
        val moneyAtPassengerOutside = helper.getMoneyAtPassengerOutsideWorkFlow().first()
        val passengerOutsideTime = helper.getPassengerOutsideWorkTimeFlow().first()
        val moneyAtHoliday = helper.getMoneyAtHolidayFlow().first()
        val holidayTime = helper.getHolidayTimeFlow().first()
        val linearMileageMoney = helper.getMoneyLinearMileageFlow().first()
        val linearMileageAccruals = helper.getLinearMileageAccrualsFlow().first()
        val surchargeAtExtendedServicePhase = helper.getMoneyListSurchargeExtendedServicePhaseFlow().first().sum()
        val surchargeAtHeavyTrains = helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first().sum()
        val surchargeAtLongTrains = helper.getMoneyListSurchargeLongTrainsFlow().first().sum()
        val surchargeAtDoubledTrainFirst = helper.getMoneyDoubledTrainFirstSurchargeFlow(listOf(route)).first()
        val surchargeAtDoubledTrainSecond = helper.getMoneyDoubledTrainSecondSurchargeFlow(listOf(route)).first()
        val moneyAtQualificationClass = helper.getMoneyAtQualificationClassFlow().first()
        val nordicSurcharge = helper.getMoneyNordicSurcharge().first()
        val districtSurcharge = helper.getMoneyDistrictSurcharge().first()
        val moneyAtHarmfulness = helper.getMoneyHarmfulnessFlow().first()
        val otherSurchargeMoney = helper.getMoneyOtherSurchargeFlow().first()

        val isPassengerTrain = route.trains.any { train ->
            passengerTrainNumberList.any { it.contains(train.number.toIntOrZero()) }
        }
        val moneyAtOnePerson = if (isPassengerTrain) {
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
            salary.onePersonOperationPassengerTrainPercent
        } else {
            salary.onePersonOperationPercent
        }

        // Переотдых: предыдущий маршрут с отдыхом в пункте оборота, 2/3 тарифа.
        val previous = request.previousRoute
        val overRestTime = if (previous != null && previous.basicData.restPointOfTurnover) {
            previous.getOverRestTime(route, settings.minTimeRestPointOfTurnover)
        } else {
            0L
        }
        val overRestMoney = if (overRestTime > 0L) {
            overRestTime * (tariffRate * (2.0 / 3.0)) / 3_600_000.0
        } else {
            0.0
        }

        val surchargeAtTrains = surchargeAtExtendedServicePhase + surchargeAtHeavyTrains +
            surchargeAtLongTrains + surchargeAtDoubledTrainFirst + surchargeAtDoubledTrainSecond
        val trainSurchargeTypes = buildList {
            if (surchargeAtHeavyTrains != 0.0) add("тяжеловесный")
            if (surchargeAtLongTrains != 0.0) add("длинносоставный")
            if (surchargeAtExtendedServicePhase != 0.0) add("удлинённое плечо")
            if (surchargeAtDoubledTrainFirst + surchargeAtDoubledTrainSecond != 0.0) add("сдвоенный")
        }
        val otherSurcharge = moneyAtQualificationClass + nordicSurcharge + districtSurcharge +
            moneyAtHarmfulness + otherSurchargeMoney

        val totalMoney = moneyAtTariffRate + moneyAtNightHours + zonalSurchargeMoney +
            moneyAtPassengerTime + moneyAtPassengerWaiting + moneyAtPassengerOutside +
            moneyAtHoliday + linearMileageMoney + surchargeAtTrains + moneyAtOnePerson +
            otherSurcharge + overRestMoney + businessTripMoney

        val rows = buildList {
            fun add(id: String, amount: Double, hours: Long? = null, percent: Double? = null, hint: String? = null) {
                if (amount.isFinite() && amount != 0.0) add(PwaTripRow(id, amount, hours, percent, hint))
            }
            add("TARIFF", moneyAtTariffRate, workTimeForPay)
            add("HOLIDAY", moneyAtHoliday, holidayTime)
            add("ZONAL", zonalSurchargeMoney, zonalTime, zonalPercent)
            add("NIGHT", moneyAtNightHours, nightTime, salary.nightTimePercent)
            add("PASSENGER", moneyAtPassengerTime, passengerTime)
            add("PASSENGER_WAITING", moneyAtPassengerWaiting, passengerWaitingTime)
            add("PASSENGER_OUTSIDE", moneyAtPassengerOutside, passengerOutsideTime)
            linearMileageAccruals.forEach { accrual ->
                add("LINEAR_MILEAGE", accrual.money, hint = "${accrual.phaseName}: ${formatDistance(accrual.distance)} км × ${accrual.rate}/км")
            }
            add("ONE_PERSON", moneyAtOnePerson, onePersonTime, onePersonPercent)
            add("TRAIN_SURCHARGE", surchargeAtTrains, hint = trainSurchargeTypes.joinToString(", "))
            add("OTHER_SURCHARGE", otherSurcharge)
            add("OVER_REST", overRestMoney, overRestTime, 200.0 / 3.0)
            add("BUSINESS_TRIP", businessTripMoney)
        }

        return PwaTripResult(
            isCalculated = true,
            isSetTariffRate = isSetTariffRate,
            tariffRate = tariffRate,
            totalPayment = totalMoney,
            isBusinessTrip = helper.hasBusinessTripRoutes(),
            businessTripMoney = businessTripMoney,
            workTimeMillis = workTime,
            rows = rows,
        )
    }

    private fun formatDistance(distance: Double): String =
        if (distance % 1.0 == 0.0) distance.toLong().toString() else distance.toString().replace('.', ',')

    private suspend fun buildResult(
        request: PwaSalaryRequest,
        helper: SalaryCalculationHelper,
    ): PwaSalaryResult {
        val accruals = mutableListOf<PwaSalaryLine>()
        suspend fun add(
            id: SalaryPaymentId,
            hours: Long? = null,
            percent: Double? = null,
            amount: Double,
            nameSuffix: String = "",
        ) {
            if (!amount.isFinite() || amount <= 0.0) return
            val definition = PayrollPaymentCatalog[id]
            accruals += PwaSalaryLine(
                id = id.name,
                code = definition.codeLabel,
                plainName = definition.plainName + nameSuffix,
                payrollSheetName = definition.payrollSheetName,
                type = PayrollPaymentType.ACCRUAL.name,
                hoursMillis = hours,
                percent = percent,
                amount = amount,
            )
        }

        val tariffHours = helper.getWorkTimeAtTariffFlow().first()
        val passengerHours = helper.getPassengerTimeFlow().first()
        val reserveHours = helper.getSingleLocomotiveTimeFlow().first()
        val holidayHours = helper.getHolidayTimeFlow().first()
        val businessHours = helper.getBusinessTripTimeFlow().first()
        add(SalaryPaymentId.TARIFF, tariffHours, amount = helper.getMoneyAtWorkTimeAtTariff().first())
        add(SalaryPaymentId.NIGHT, helper.getNightTimeFlow().first(), request.salarySetting.nightTimePercent, helper.getMoneyAtNightTimeFlow().first())
        add(SalaryPaymentId.PASSENGER, passengerHours, amount = helper.getMoneyAtPassengerFlow().first())
        add(SalaryPaymentId.PASSENGER_WAITING, helper.getPassengerWaitingTimeFlow().first(), amount = helper.getMoneyAtPassengerWaitingFlow().first())
        add(SalaryPaymentId.RESERVE, reserveHours, amount = helper.getMoneyAtSingleLocomotiveFlow().first())
        add(SalaryPaymentId.HOLIDAY, holidayHours, amount = helper.getMoneyAtHolidayFlow().first())
        add(SalaryPaymentId.AVERAGE, helper.getDayOffHoursFlow().first(), amount = helper.getMoneyAverageFlow().first())
        add(SalaryPaymentId.UNDERWORK, helper.getUnderworkTimeFlow().first(), amount = helper.getMoneyUnderworkFlow().first())
        add(SalaryPaymentId.DISABLED_CHILD_CARE, helper.getHoursCaringForDisableChildren().first(), amount = helper.getMoneyCaringForDisableChildren().first())
        add(SalaryPaymentId.BUSINESS_TRIP, businessHours, amount = helper.getMoneyBusinessTripFlow().first())
        add(SalaryPaymentId.TECHNICAL_STUDY, helper.getTechnicalStudyTimeFlow().first(), amount = helper.getMoneyTechnicalStudyFlow().first())
        add(SalaryPaymentId.ZONAL, percent = helper.getPercentZonalSurchargeFlow().first(), amount = helper.getMoneyZonalSurchargeFlow().first())
        add(SalaryPaymentId.QUALIFICATION_CLASS, percent = request.salarySetting.surchargeQualificationClass, amount = helper.getMoneyAtQualificationClassFlow().first())
        helper.getLinearMileageAccrualsFlow().first().forEach {
            add(SalaryPaymentId.LINEAR_MILEAGE, amount = it.money, nameSuffix = ": ${it.phaseName}")
        }
        add(SalaryPaymentId.ONE_PERSON_FREIGHT, helper.getTimeOnePersonOperationFlow().first(), helper.getPercentOnePersonOperationFlow().first(), helper.getMoneyOnePersonOperationFlow().first())
        add(SalaryPaymentId.ONE_PERSON_PASSENGER, helper.getTimeOnePersonOperationPassengerTrainFlow().first(), helper.getPercentOnePersonOperationPassengerTrainFlow().first(), helper.getMoneyOnePersonOperationPassengerTrainFlow().first())
        add(SalaryPaymentId.HARMFULNESS, percent = helper.getPercentHarmfulnessFlow().first(), amount = helper.getMoneyHarmfulnessFlow().first())
        add(SalaryPaymentId.DISTRICT, percent = helper.getPercentDistrictSurcharge().first(), amount = helper.getMoneyDistrictSurcharge().first())
        add(SalaryPaymentId.NORDIC, percent = helper.getPercentNordicSurcharge().first(), amount = helper.getMoneyNordicSurcharge().first())
        add(SalaryPaymentId.EXCESS_REST, helper.getOverRestTimeFlow().first(), amount = helper.getMoneyOverRestFlow().first())

        addTiered(accruals, SalaryPaymentId.EXTENDED_SERVICE, helper.getTimeListSurchargeServicePhaseFlow().first(), helper.getPercentListSurchargeExtendedServicePhaseFlow().first(), helper.getMoneyListSurchargeExtendedServicePhaseFlow().first())
        addTiered(accruals, SalaryPaymentId.HEAVY_TRAIN, helper.getTimeListSurchargeHeavyTrainsFlow().first(), helper.getPercentListSurchargeExtendedHeavyTrainsFlow().first(), helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first())
        addTiered(accruals, SalaryPaymentId.LONG_TRAIN, helper.getTimeListSurchargeLongTrainsFlow().first(), helper.getPercentListSurchargeLongTrainsFlow().first(), helper.getMoneyListSurchargeLongTrainsFlow().first())
        add(SalaryPaymentId.HEAVY_LONG_DISTANCE, helper.getTimeHeavyLongDistanceTrainsFlow().first(), helper.getPercentHeavyLongDistanceTrainsFlow().first(), helper.getMoneyHeavyLongDistanceTrainsFlow().first())
        add(SalaryPaymentId.DOUBLED_TRAIN, helper.getTimeDoubledTrainFirstSurchargeFlow().first(), 30.0, helper.getMoneyDoubledTrainFirstSurchargeFlow().first(), " (30%)")
        add(SalaryPaymentId.DOUBLED_TRAIN, helper.getTimeDoubledTrainSecondSurchargeFlow().first(), 15.0, helper.getMoneyDoubledTrainSecondSurchargeFlow().first(), " (15%)")
        add(SalaryPaymentId.OVERTIME_BASE, helper.getTimeOvertimeFlow().first(), amount = helper.getMoneyOvertimeFlow().first())
        add(SalaryPaymentId.OVERTIME_HALF, helper.getTimeSurchargeAtOvertime05Flow().first(), 50.0, helper.getMoneySurchargeOvertime05Flow().first())
        add(SalaryPaymentId.OVERTIME_FULL, helper.getTimeSurchargeAtOvertimeFlow().first(), 100.0, helper.getMoneySurchargeOvertimeFlow().first())
        add(SalaryPaymentId.OTHER_SURCHARGE, percent = helper.getPercentOtherSurchargeFlow().first(), amount = helper.getMoneyOtherSurchargeFlow().first())

        val totalAccrued = helper.getMoneyTotalChargedFlow().first()
        val deductions = buildDeductions(request.salarySetting, helper)
        val totalDeducted = helper.getMoneyTotalRetentionFlow().first()
        return PwaSalaryResult(
            month = request.userSettings.selectMonthOfYear.month,
            year = request.userSettings.selectMonthOfYear.year,
            tariffRate = request.userSettings.selectMonthOfYear.tariffRate,
            normaHours = request.effectiveNormaHours,
            totalWorkedMillis = helper.getTotalWorkTimeWithCommute().first(),
            accruals = accruals,
            deductions = deductions,
            totalAccrued = totalAccrued,
            totalDeducted = totalDeducted,
            payable = helper.getMoneyToBeCredited().first(),
        )
    }

    private fun addTiered(
        target: MutableList<PwaSalaryLine>,
        id: SalaryPaymentId,
        hours: List<Long>,
        percents: List<String>,
        amounts: List<Double>,
    ) {
        val definition = PayrollPaymentCatalog[id]
        amounts.forEachIndexed { index, amount ->
            if (amount.isFinite() && amount > 0.0) target += PwaSalaryLine(
                id = id.name,
                code = definition.codeLabel,
                plainName = definition.plainName,
                payrollSheetName = definition.payrollSheetName,
                type = PayrollPaymentType.ACCRUAL.name,
                hoursMillis = hours.getOrNull(index),
                percent = percents.getOrNull(index)?.replace(',', '.')?.toDoubleOrNull(),
                amount = amount,
            )
        }
    }

    private suspend fun buildDeductions(
        settings: SalarySetting,
        helper: SalaryCalculationHelper,
    ): List<PwaSalaryLine> {
        suspend fun line(id: SalaryPaymentId, percent: Double, amount: Double): PwaSalaryLine? {
            if (!amount.isFinite() || amount <= 0.0) return null
            val definition = PayrollPaymentCatalog[id]
            return PwaSalaryLine(id.name, definition.codeLabel, definition.plainName, definition.payrollSheetName, PayrollPaymentType.DEDUCTION.name, percent = percent, amount = amount)
        }
        return listOfNotNull(
            line(SalaryPaymentId.NDFL, settings.ndfl, helper.getMoneyNDFLRetentionFlow().first()),
            line(SalaryPaymentId.UNION, settings.unionistsRetention, helper.getMoneyUnionistsRetentionFlow().first()),
            line(SalaryPaymentId.WELFARE, settings.welfarePercent, helper.getMoneyWelfareRetentionFlow().first()),
            line(SalaryPaymentId.ALIMONY, settings.alimonyPercent, helper.getMoneyAlimonyRetentionFlow().first()),
            line(SalaryPaymentId.OTHER_DEDUCTION, settings.otherRetention, helper.getMoneyOtherRetentionFlow().first()),
        )
    }
}
