package com.z_company.repository.remote_rest.response

import com.z_company.domain.entities.setting.SALARY_SETTINGS_KEY
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.SurchargeExtendedServicePhase
import com.z_company.domain.entities.setting.SurchargeHeavyTrains
import com.z_company.domain.entities.setting.SurchargeLongTrains
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO `GET/POST /v1/salary_settings/`. Поля совпадают с [SalarySetting], плюс
 * серверная метка `updated_at` (snake_case — исключение из camelCase-контракта,
 * серверный `SalarySettingResponse.updated_at`, epoch ms). Сервер проставляет её
 * сам на каждом POST и клиентское значение игнорирует, поэтому при выгрузке
 * поле не отправляем. Принимаем как Double: у соседних norma_time-эндпоинтов
 * метка приходит с дробной частью, здесь страхуемся от того же.
 */
@Serializable
data class SalarySettingResponse(
    val key: String = SALARY_SETTINGS_KEY,
    val nightTimePercent: Double = 40.0,
    val averagePaymentHour: Double = 0.0,
    val districtCoefficient: Double = 0.0,
    val nordicPercent: Double = 0.0,
    val onePersonOperationPercent: Double = 40.0,
    val onePersonOperationPassengerTrainPercent: Double = 50.0,
    val harmfulnessPercent: Double = 4.0,
    val percentLongDistanceTrain: Double = 0.0,
    val lengthLongDistanceTrain: Int = 0,
    val zonalSurcharge: Double = 25.0,
    val surchargeQualificationClass: Double = 0.0,
    val surchargeExtendedServicePhaseList: List<SurchargeExtendedServicePhase> = emptyList(),
    val surchargeHeavyTrainsList: List<SurchargeHeavyTrains> = emptyList(),
    val surchargeLongTrainsList: List<SurchargeLongTrains> = emptyList(),
    val surchargeHeavyLongDistanceTrains: Double = 5.0,
    val otherSurcharge: Double = 0.0,
    val ndfl: Double = 13.0,
    val unionistsRetention: Double = 1.0,
    val otherRetention: Double = 0.0,
    val welfarePercent: Double = 0.0,
    val alimonyPercent: Double = 0.0,
    val showUnderworkPayments: Boolean = true,
    @SerialName("updated_at") val updatedAt: Double? = null
) {
    fun toDomain(): SalarySetting = SalarySetting(
        key = key,
        nightTimePercent = nightTimePercent,
        averagePaymentHour = averagePaymentHour,
        districtCoefficient = districtCoefficient,
        nordicPercent = nordicPercent,
        onePersonOperationPercent = onePersonOperationPercent,
        onePersonOperationPassengerTrainPercent = onePersonOperationPassengerTrainPercent,
        harmfulnessPercent = harmfulnessPercent,
        percentLongDistanceTrain = percentLongDistanceTrain,
        lengthLongDistanceTrain = lengthLongDistanceTrain,
        zonalSurcharge = zonalSurcharge,
        surchargeQualificationClass = surchargeQualificationClass,
        surchargeExtendedServicePhaseList = surchargeExtendedServicePhaseList,
        surchargeHeavyTrainsList = surchargeHeavyTrainsList,
        surchargeLongTrainsList = surchargeLongTrainsList,
        surchargeHeavyLongDistanceTrains = surchargeHeavyLongDistanceTrains,
        otherSurcharge = otherSurcharge,
        ndfl = ndfl,
        unionistsRetention = unionistsRetention,
        otherRetention = otherRetention,
        welfarePercent = welfarePercent,
        alimonyPercent = alimonyPercent,
        showUnderworkPayments = showUnderworkPayments,
        updatedAt = updatedAt?.toLong() ?: 0L
    )

    companion object {
        fun fromDomain(s: SalarySetting): SalarySettingResponse = SalarySettingResponse(
            key = s.key,
            nightTimePercent = s.nightTimePercent,
            averagePaymentHour = s.averagePaymentHour,
            districtCoefficient = s.districtCoefficient,
            nordicPercent = s.nordicPercent,
            onePersonOperationPercent = s.onePersonOperationPercent,
            onePersonOperationPassengerTrainPercent = s.onePersonOperationPassengerTrainPercent,
            harmfulnessPercent = s.harmfulnessPercent,
            percentLongDistanceTrain = s.percentLongDistanceTrain,
            lengthLongDistanceTrain = s.lengthLongDistanceTrain,
            zonalSurcharge = s.zonalSurcharge,
            surchargeQualificationClass = s.surchargeQualificationClass,
            surchargeExtendedServicePhaseList = s.surchargeExtendedServicePhaseList,
            surchargeHeavyTrainsList = s.surchargeHeavyTrainsList,
            surchargeLongTrainsList = s.surchargeLongTrainsList,
            surchargeHeavyLongDistanceTrains = s.surchargeHeavyLongDistanceTrains,
            otherSurcharge = s.otherSurcharge,
            ndfl = s.ndfl,
            unionistsRetention = s.unionistsRetention,
            otherRetention = s.otherRetention,
            welfarePercent = s.welfarePercent,
            alimonyPercent = s.alimonyPercent,
            showUnderworkPayments = s.showUnderworkPayments,
            updatedAt = null
        )
    }
}
