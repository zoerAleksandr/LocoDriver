package com.z_company.route.viewmodel

import com.z_company.domain.entities.setting.SurchargeExtendedServicePhase
import com.z_company.domain.entities.setting.SurchargeHeavyTrains
import com.z_company.domain.entities.setting.SurchargeLongTrains

/** Совместимые имена для Android-кода; реализация расчёта находится в KMP domain. */
typealias SalaryCalculationHelper = com.z_company.domain.salary.SalaryCalculationHelper
typealias LinearMileageAccrual = com.z_company.domain.salary.LinearMileageAccrual

internal fun validHeavyTrainSurcharges(values: List<SurchargeHeavyTrains>) =
    com.z_company.domain.salary.validHeavyTrainSurcharges(values)

internal fun validLongTrainSurcharges(values: List<SurchargeLongTrains>) =
    com.z_company.domain.salary.validLongTrainSurcharges(values)

internal fun validExtendedServicePhaseSurcharges(values: List<SurchargeExtendedServicePhase>) =
    com.z_company.domain.salary.validExtendedServicePhaseSurcharges(values)

internal fun isFederalLaw144Effective(year: Int, month: Int) =
    com.z_company.domain.salary.isFederalLaw144Effective(year, month)

internal fun isExpandedOvertimeBaseEffective(year: Int, month: Int) =
    com.z_company.domain.salary.isExpandedOvertimeBaseEffective(year, month)

internal fun calculateHalfRateOvertime(
    overtime: Long,
    shiftCount: Int,
    year: Int,
    month: Int,
    annualOvertimeBeforePeriod: Long = 0L,
) = com.z_company.domain.salary.calculateHalfRateOvertime(
    overtime = overtime,
    shiftCount = shiftCount,
    year = year,
    month = month,
    annualOvertimeBeforePeriod = annualOvertimeBeforePeriod,
)
