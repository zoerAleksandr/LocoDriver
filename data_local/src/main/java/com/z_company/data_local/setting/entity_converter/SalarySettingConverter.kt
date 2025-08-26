package com.z_company.data_local.setting.entity_converter

import com.z_company.domain.entities.SalarySetting
import com.z_company.data_local.setting.entity.SalarySetting as SalarySettingEntity

internal object SalarySettingConverter {
    fun fromData(salarySetting: SalarySetting) = SalarySettingEntity(
        salarySettingKey = salarySetting.key,
        tariffRate = salarySetting.tariffRate,
        nightTimePercent = salarySetting.nightTimePercent,
        averagePaymentHour = salarySetting.averagePaymentHour,
        districtCoefficient = salarySetting.districtCoefficient,
        nordicCoefficient = salarySetting.nordicPercent,
        zonalSurcharge = salarySetting.zonalSurcharge,
        surchargeQualificationClass = salarySetting.surchargeQualificationClass,
        surchargeExtendedServicePhaseList = SurchargeExtendedServicePhaseConverter.fromDataList(
            salarySetting.surchargeExtendedServicePhaseList
        ),
        harmfulnessPercent = salarySetting.harmfulnessPercent,
        onePersonOperationPercent = salarySetting.onePersonOperationPercent,
        surchargeHeavyTrainsList = SurchargeHeavyTrainsConverter.fromDataList(salarySetting.surchargeHeavyTrainsList),
        surchargeLongTrain = salarySetting.percentLongDistanceTrain,
        lengthLongDistanceTrain = salarySetting.lengthLongDistanceTrain,
        otherSurcharge = salarySetting.otherSurcharge,
        ndfl = salarySetting.ndfl,
        unionistsRetention = salarySetting.unionistsRetention,
        otherRetention = salarySetting.otherRetention,
        onePersonOperationPassengerTrainPercent = salarySetting.onePersonOperationPassengerTrainPercent
    )

    fun toData(entity: SalarySettingEntity) = SalarySetting(
        key = entity.salarySettingKey,
        tariffRate = entity.tariffRate,
        nightTimePercent = entity.nightTimePercent,
        averagePaymentHour = entity.averagePaymentHour,
        districtCoefficient = entity.districtCoefficient,
        nordicPercent = entity.nordicCoefficient,
        zonalSurcharge = entity.zonalSurcharge,
        surchargeQualificationClass = entity.surchargeQualificationClass,
        harmfulnessPercent = entity.harmfulnessPercent,
        onePersonOperationPercent = entity.onePersonOperationPercent,
        surchargeHeavyTrainsList = SurchargeHeavyTrainsConverter.toDataList(entity.surchargeHeavyTrainsList),
        percentLongDistanceTrain = entity.surchargeLongTrain,
        lengthLongDistanceTrain = entity.lengthLongDistanceTrain,
        surchargeExtendedServicePhaseList = SurchargeExtendedServicePhaseConverter.toDataList(entity.surchargeExtendedServicePhaseList),
        otherSurcharge = entity.otherSurcharge,
        ndfl = entity.ndfl,
        unionistsRetention = entity.unionistsRetention,
        otherRetention = entity.otherRetention,
        onePersonOperationPassengerTrainPercent = entity.onePersonOperationPassengerTrainPercent
    )
}