package com.z_company.iosapp.pdf

import com.z_company.domain.entities.WorkScheduleProfile
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.salary.PayrollPaymentCatalog
import com.z_company.domain.entities.salary.SalaryPaymentId
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.salary.LinearMileageAccrual
import com.z_company.domain.salary.SalaryCalculationHelper
import com.z_company.domain.util.currencySymbol
import com.z_company.domain.util.str
import com.z_company.domain.util.str2decimalSign
import kotlinx.coroutines.flow.first

/**
 * Расчётный листок (§21 «Раздел «Расчётный листок»»): строки начислений/удержаний
 * и итоги, собранные из `SalaryCalculationHelper` (общий расчёт — те же данные,
 * что экран «Расчёт зарплаты» §11).
 *
 * Состав строк — порт `buildAccrualRows`/`buildDeductionRows` из Android
 * (`features/route/.../ui/SalaryCalculationScreen.kt`); коды и названия —
 * из `PayrollPaymentCatalog` (режим PAYROLL_SHEET).
 */
internal class PdfSalaryStatementData(
    val title: String,
    val tariffText: String,
    val normaText: String,
    val totalWorkText: String,
    val currencyText: String,
    val monthCell: String,
    val accruals: List<PdfStatementRowData>,
    val deductions: List<PdfStatementRowData>,
    val totalChargedText: String,
    val totalRetentionText: String,
    val totalHoursText: String,
    val toBeCreditedText: String,
) {
    fun toStatementBlock(): PdfSalaryStatementBlock = PdfSalaryStatementBlock(
        title = title,
        tariffText = tariffText,
        normaText = normaText,
        totalWorkText = totalWorkText,
        currencyText = currencyText,
        monthCell = monthCell,
        accruals = accruals,
        deductions = deductions,
        totalChargedText = totalChargedText,
        totalRetentionText = totalRetentionText,
        totalHoursText = totalHoursText,
        toBeCreditedText = toBeCreditedText,
    )
}

/** Промежуточная строка до форматирования (как `AccrualRow`/`DeductionRow` Android). */
private class RawRow(
    val paymentId: SalaryPaymentId,
    val hours: Long?,
    val percent: Double?,
    val money: Double?,
)

/**
 * Собирает данные листка из `SalaryCalculationHelper`.
 *
 * Отличие от Android: экран расчёта не передаёт `SalaryCalculationUIState` в PDF
 * (на iOS его нет) — строки считаются напрямую из того же helper'а, что и экран
 * расчёта. Поведение §21 (состав строк, коды, форматы) сохранено.
 */
internal suspend fun collectSalaryStatement(
    userSettings: UserSettings,
    salarySetting: SalarySetting,
    routes: List<Route>,
    adjacentRoutes: List<Route>,
    effectiveNormaHoursForUnderwork: Int,
    annualOvertimeBeforePeriod: Long,
    workScheduleProfile: WorkScheduleProfile,
    monthLabel: String,
    normaHours: Int,
): PdfSalaryStatementData {
    val helper = SalaryCalculationHelper(
        userSettings = userSettings,
        salarySetting = salarySetting,
        allRoutes = routes,
        effectiveNormaHoursForUnderwork = effectiveNormaHoursForUnderwork,
        annualOvertimeBeforePeriod = annualOvertimeBeforePeriod,
        workScheduleProfile = workScheduleProfile,
        adjacentRoutes = adjacentRoutes,
    )
    val currency = currencySymbol(userSettings.country)

    // ── Начисления (состав и порядок — buildAccrualRows) ───────────────────
    val accrualRaws = mutableListOf<RawRow>()

    accrualRaws += RawRow(
        SalaryPaymentId.TARIFF,
        helper.getWorkTimeAtTariffFlow().first(),
        null,
        helper.getMoneyAtWorkTimeAtTariff().first(),
    )
    accrualRaws += RawRow(
        SalaryPaymentId.NIGHT,
        helper.getNightTimeFlow().first(),
        salarySetting.nightTimePercent,
        helper.getMoneyAtNightTimeFlow().first(),
    )
    accrualRaws += RawRow(
        SalaryPaymentId.PASSENGER,
        helper.getPassengerTimeFlow().first() + helper.getPassengerOutsideWorkTimeFlow().first(),
        null,
        helper.getMoneyAtPassengerFlow().first() + helper.getMoneyAtPassengerOutsideWorkFlow().first(),
    )
    run {
        val money = helper.getMoneyAtPassengerWaitingFlow().first()
        if (money != null && money > 0) {
            accrualRaws += RawRow(
                SalaryPaymentId.PASSENGER_WAITING,
                helper.getPassengerWaitingTimeFlow().first(),
                null,
                money,
            )
        }
    }
    accrualRaws += RawRow(
        SalaryPaymentId.RESERVE,
        helper.getSingleLocomotiveTimeFlow().first(),
        null,
        helper.getMoneyAtSingleLocomotiveFlow().first(),
    )
    accrualRaws += RawRow(
        SalaryPaymentId.HOLIDAY,
        helper.getHolidayTimeFlow().first(),
        null,
        helper.getMoneyAtHolidayFlow().first(),
    )
    accrualRaws += RawRow(
        SalaryPaymentId.AVERAGE,
        helper.getDayOffHoursFlow().first(),
        null,
        helper.getMoneyAverageFlow().first(),
    )
    run {
        val money = helper.getMoneyUnderworkFlow().first()
        if (money != null && money > 0) {
            accrualRaws += RawRow(
                SalaryPaymentId.UNDERWORK,
                helper.getUnderworkTimeFlow().first(),
                null,
                money,
            )
        }
    }
    accrualRaws += RawRow(
        SalaryPaymentId.DISABLED_CHILD_CARE,
        helper.getHoursCaringForDisableChildren().first(),
        null,
        helper.getMoneyCaringForDisableChildren().first(),
    )
    run {
        val hours = helper.getBusinessTripTimeFlow().first()
        if (hours != null && hours > 0) {
            accrualRaws += RawRow(SalaryPaymentId.BUSINESS_TRIP, hours, null, helper.getMoneyBusinessTripFlow().first())
        }
    }
    run {
        val hours = helper.getTechnicalStudyTimeFlow().first()
        if (hours != null && hours > 0) {
            accrualRaws += RawRow(
                SalaryPaymentId.TECHNICAL_STUDY,
                hours,
                null,
                helper.getMoneyTechnicalStudyFlow().first(),
            )
        }
    }

    // Надбавки
    accrualRaws += RawRow(
        SalaryPaymentId.ZONAL,
        null,
        helper.getPercentZonalSurchargeFlow().first(),
        helper.getMoneyZonalSurchargeFlow().first(),
    )
    accrualRaws += RawRow(
        SalaryPaymentId.QUALIFICATION_CLASS,
        null,
        salarySetting.surchargeQualificationClass,
        helper.getMoneyAtQualificationClassFlow().first(),
    )
    helper.getLinearMileageAccrualsFlow().first().forEach { accrual: LinearMileageAccrual ->
        accrualRaws += RawRow(SalaryPaymentId.LINEAR_MILEAGE, null, null, accrual.money)
    }
    accrualRaws += RawRow(
        SalaryPaymentId.ONE_PERSON_FREIGHT,
        helper.getTimeOnePersonOperationFlow().first(),
        helper.getPercentOnePersonOperationFlow().first(),
        helper.getMoneyOnePersonOperationFlow().first(),
    )
    accrualRaws += RawRow(
        SalaryPaymentId.ONE_PERSON_PASSENGER,
        helper.getTimeOnePersonOperationPassengerTrainFlow().first(),
        helper.getPercentOnePersonOperationPassengerTrainFlow().first(),
        helper.getMoneyOnePersonOperationPassengerTrainFlow().first(),
    )
    accrualRaws += RawRow(
        SalaryPaymentId.HARMFULNESS,
        null,
        helper.getPercentHarmfulnessFlow().first(),
        helper.getMoneyHarmfulnessFlow().first(),
    )
    accrualRaws += RawRow(
        SalaryPaymentId.DISTRICT,
        null,
        helper.getPercentDistrictSurcharge().first(),
        helper.getMoneyDistrictSurcharge().first(),
    )
    accrualRaws += RawRow(
        SalaryPaymentId.NORDIC,
        null,
        helper.getPercentNordicSurcharge().first(),
        helper.getMoneyNordicSurcharge().first(),
    )
    run {
        val money = helper.getMoneyOverRestFlow().first()
        if (money != null && money > 0) {
            accrualRaws += RawRow(SalaryPaymentId.EXCESS_REST, helper.getOverRestTimeFlow().first(), null, money)
        }
    }

    run {
        val times = helper.getTimeListSurchargeServicePhaseFlow().first()
        val percents = helper.getPercentListSurchargeExtendedServicePhaseFlow().first()
        val monies = helper.getMoneyListSurchargeExtendedServicePhaseFlow().first()
        for (i in 0 until minOf(times.size, percents.size, monies.size)) {
            val money = monies[i] ?: 0.0
            if (money > 0) {
                accrualRaws += RawRow(
                    SalaryPaymentId.EXTENDED_SERVICE,
                    times[i],
                    percents[i]?.toDoubleOrNull(),
                    money,
                )
            }
        }
    }
    run {
        val times = helper.getTimeListSurchargeHeavyTrainsFlow().first()
        val percents = helper.getPercentListSurchargeExtendedHeavyTrainsFlow().first()
        val monies = helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first()
        for (i in 0 until minOf(times.size, percents.size, monies.size)) {
            val money = monies[i] ?: 0.0
            if (money > 0) {
                accrualRaws += RawRow(
                    SalaryPaymentId.HEAVY_TRAIN,
                    times[i],
                    percents[i]?.toDoubleOrNull(),
                    money,
                )
            }
        }
    }
    run {
        val times = helper.getTimeListSurchargeLongTrainsFlow().first()
        val percents = helper.getPercentListSurchargeLongTrainsFlow().first()
        val monies = helper.getMoneyListSurchargeLongTrainsFlow().first()
        for (i in 0 until minOf(times.size, percents.size, monies.size)) {
            val money = monies[i] ?: 0.0
            if (money > 0) {
                accrualRaws += RawRow(
                    SalaryPaymentId.LONG_TRAIN,
                    times[i],
                    percents[i]?.toDoubleOrNull(),
                    money,
                )
            }
        }
    }
    run {
        val money = helper.getMoneyHeavyLongDistanceTrainsFlow().first()
        if (money != null && money > 0) {
            accrualRaws += RawRow(
                SalaryPaymentId.HEAVY_LONG_DISTANCE,
                helper.getTimeHeavyLongDistanceTrainsFlow().first(),
                helper.getPercentHeavyLongDistanceTrainsFlow().first(),
                money,
            )
        }
    }
    run {
        val money = helper.getMoneyDoubledTrainFirstSurchargeFlow().first()
        if (money != null && money > 0) {
            accrualRaws += RawRow(
                SalaryPaymentId.DOUBLED_TRAIN,
                helper.getTimeDoubledTrainFirstSurchargeFlow().first(),
                30.0,
                money,
            )
        }
    }
    run {
        val money = helper.getMoneyDoubledTrainSecondSurchargeFlow().first()
        if (money != null && money > 0) {
            accrualRaws += RawRow(
                SalaryPaymentId.DOUBLED_TRAIN,
                helper.getTimeDoubledTrainSecondSurchargeFlow().first(),
                15.0,
                money,
            )
        }
    }

    // Сверхурочные
    accrualRaws += RawRow(
        SalaryPaymentId.OVERTIME_BASE,
        helper.getTimeOvertimeFlow().first(),
        null,
        helper.getMoneyOvertimeFlow().first(),
    )
    run {
        val money = helper.getMoneySurchargeOvertime05Flow().first()
        if (money != null && money > 0) {
            accrualRaws += RawRow(
                SalaryPaymentId.OVERTIME_HALF,
                helper.getTimeSurchargeAtOvertime05Flow().first(),
                50.0,
                money,
            )
        }
    }
    run {
        val money = helper.getMoneySurchargeOvertimeFlow().first()
        if (money != null && money > 0) {
            accrualRaws += RawRow(
                SalaryPaymentId.OVERTIME_FULL,
                helper.getTimeSurchargeAtOvertimeFlow().first(),
                100.0,
                money,
            )
        }
    }

    // Пользовательские суммы завершают список штатных начислений.
    accrualRaws += RawRow(
        SalaryPaymentId.OTHER_SURCHARGE,
        null,
        helper.getPercentOtherSurchargeFlow().first(),
        helper.getMoneyOtherSurchargeFlow().first(),
    )

    val accruals = accrualRaws
        .filter { it.money != null && it.money > 0 }
        .sortedByPayrollCode()
        .map { row ->
            PdfStatementRowData(
                code = PayrollPaymentCatalog[row.paymentId].codeLabel,
                name = PayrollPaymentCatalog[row.paymentId].payrollSheetName,
                hours = statementHours(row.hours),
                percent = statementPercent(row.percent),
                amount = statementMoney(row.money),
            )
        }

    // ── Удержания (buildDeductionRows) ─────────────────────────────────────
    val deductionRaws = mutableListOf<RawRow>()
    helper.getMoneyNDFLRetentionFlow().first().takeIf { it > 0 }?.let {
        deductionRaws += RawRow(SalaryPaymentId.NDFL, null, 13.0, it)
    }
    helper.getMoneyUnionistsRetentionFlow().first().takeIf { it > 0 }?.let {
        deductionRaws += RawRow(SalaryPaymentId.UNION, null, null, it)
    }
    helper.getMoneyWelfareRetentionFlow().first().takeIf { it > 0 }?.let {
        deductionRaws += RawRow(SalaryPaymentId.WELFARE, null, null, it)
    }
    helper.getMoneyAlimonyRetentionFlow().first().takeIf { it > 0 }?.let {
        deductionRaws += RawRow(SalaryPaymentId.ALIMONY, null, null, it)
    }
    helper.getMoneyOtherRetentionFlow().first().takeIf { it > 0 }?.let {
        deductionRaws += RawRow(SalaryPaymentId.OTHER_DEDUCTION, null, null, it)
    }
    val deductions = deductionRaws
        .sortedByPayrollCode()
        .map { row ->
            PdfStatementRowData(
                code = PayrollPaymentCatalog[row.paymentId].codeLabel,
                name = PayrollPaymentCatalog[row.paymentId].payrollSheetName,
                hours = "",
                percent = statementPercent(row.percent),
                amount = statementMoney(row.money),
            )
        }

    // ── Шапка и итоги (§21 «Расчётный листок») ────────────────────────────
    val titleParts = monthLabel.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val periodTitle = if (titleParts.size >= 2) {
        "за ${titleParts.first().lowercase()} месяц ${titleParts[1]} года"
    } else {
        "за $monthLabel"
    }
    val tariffText = if (userSettings.selectMonthOfYear.dateSetTariffRate == null) {
        "Оклад/Тариф: ${userSettings.selectMonthOfYear.tariffRate.str()} $currency"
    } else {
        "Оклад/Тариф: ${userSettings.selectMonthOfYear.dateSetTariffRate!!.oldRate.str()} / " +
            "${userSettings.selectMonthOfYear.tariffRate.str()} $currency"
    }
    val totalWorkMs = helper.getTotalWorkTimeWithCommute().first()

    return PdfSalaryStatementData(
        title = "Расчётный листок $periodTitle",
        tariffText = tariffText,
        normaText = "Норма часов: ${statementDecimal(normaHours.toDouble())}",
        totalWorkText = "Всего отработано часов: ${statementHours(totalWorkMs)}",
        currencyText = "Валюта расчёта: $currency",
        monthCell = titleParts.first().lowercase(),
        accruals = accruals,
        deductions = deductions,
        totalChargedText = statementMoney(helper.getMoneyTotalChargedFlow().first()),
        totalRetentionText = statementMoney(helper.getMoneyTotalRetentionFlow().first()),
        totalHoursText = statementHours(totalWorkMs),
        toBeCreditedText = statementMoney(helper.getMoneyToBeCredited().first()),
    )
}

/** Сортировка как в бумажном листке: по первому коду (порт `sortedByPayrollCode`). */
private fun List<RawRow>.sortedByPayrollCode(): List<RawRow> =
    sortedWith(
        compareBy<RawRow> { PayrollPaymentCatalog[it.paymentId].codes.firstOrNull() == null }
            .thenBy { PayrollPaymentCatalog[it.paymentId].codes.firstOrNull().orEmpty() },
    )

/** `69 928,32` — ru_RU, 2 знака (порт `statementDecimal` через `str2decimalSign`). */
private fun statementDecimal(value: Double): String = value.str2decimalSign()

private fun statementMoney(value: Double?): String = value?.let { statementDecimal(it) }.orEmpty()

/** Часы десятичные (ч): `167,35`; 0 и null — пусто (§21). */
private fun statementHours(value: Long?): String =
    value?.takeIf { it > 0L }?.let { statementDecimal(it.toDouble() / 3_600_000.0) }.orEmpty()

private fun statementPercent(value: Double?): String =
    value?.takeIf { it != 0.0 }?.let { statementDecimal(it) }.orEmpty()
