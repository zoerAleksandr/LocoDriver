package com.z_company.data_local.setting.entity_converter

import com.z_company.domain.entities.SalarySetting
import com.z_company.data_local.setting.entity.SalarySetting as SalarySettingEntity

internal object SalarySettingConverter {
    fun fromData(salarySetting: SalarySetting) = SalarySettingEntity(
        salarySettingKey = salarySetting.key,
        tariffRate = salarySetting.tariffRate,
        zonalSurcharge = salarySetting.zonalSurcharge,
        surchargeQualificationClass = salarySetting.surchargeQualificationClass,
        surchargeExtendedServicePhase = salarySetting.surchargeExtendedServicePhase,
        surchargeHeavyLongDistanceTrains = salarySetting.surchargeHeavyLongDistanceTrains,
        otherSurcharge = salarySetting.otherSurcharge,
        ndfl = salarySetting.ndfl,
        unionistsRetention = salarySetting.unionistsRetention,
        otherRetention = salarySetting.otherRetention
    )

    fun toData(entity: SalarySettingEntity) = SalarySetting(
        key = entity.salarySettingKey,
        tariffRate = entity.tariffRate,
        zonalSurcharge = entity.zonalSurcharge,
        surchargeQualificationClass = entity.surchargeQualificationClass,
        surchargeExtendedServicePhase = entity.surchargeExtendedServicePhase,
        surchargeHeavyLongDistanceTrains = entity.surchargeHeavyLongDistanceTrains,
        otherSurcharge = entity.otherSurcharge,
        ndfl = entity.ndfl,
        unionistsRetention = entity.unionistsRetention,
        otherRetention = entity.otherRetention
    )
}