package com.z_company.data_local.setting.entity_converter

import com.z_company.domain.entities.SalarySetting
import com.z_company.data_local.setting.entity.SalarySetting as SalarySettingEntity

internal object SalarySettingConverter {
    fun fromData(salarySetting: SalarySetting) = SalarySettingEntity(
        salarySettingKey = salarySetting.key,
        tariffRate = salarySetting.tariffRate,
        averagePaymentHour = salarySetting.averagePaymentHour,
        districtCoefficient = salarySetting.districtCoefficient,
        nordicCoefficient = salarySetting.nordicCoefficient,
        zonalSurcharge = salarySetting.zonalSurcharge,
        surchargeQualificationClass = salarySetting.surchargeQualificationClass,
        surchargeExtendedServicePhaseList = SurchargeExtendedServicePhaseConverter.fromDataList(
            salarySetting.surchargeExtendedServicePhaseList
        ),
        surchargeHeavyLongDistanceTrains = salarySetting.surchargeHeavyLongDistanceTrains,
        otherSurcharge = salarySetting.otherSurcharge,
        ndfl = salarySetting.ndfl,
        unionistsRetention = salarySetting.unionistsRetention,
        otherRetention = salarySetting.otherRetention
    )

    fun toData(entity: SalarySettingEntity) = SalarySetting(
        key = entity.salarySettingKey,
        tariffRate = entity.tariffRate,
        averagePaymentHour = entity.averagePaymentHour,
        districtCoefficient = entity.districtCoefficient,
        nordicCoefficient = entity.nordicCoefficient,
        zonalSurcharge = entity.zonalSurcharge,
        surchargeQualificationClass = entity.surchargeQualificationClass,
        surchargeExtendedServicePhaseList = SurchargeExtendedServicePhaseConverter.toDataList(entity.surchargeExtendedServicePhaseList),
        surchargeHeavyLongDistanceTrains = entity.surchargeHeavyLongDistanceTrains,
        otherSurcharge = entity.otherSurcharge,
        ndfl = entity.ndfl,
        unionistsRetention = entity.unionistsRetention,
        otherRetention = entity.otherRetention
    )
}