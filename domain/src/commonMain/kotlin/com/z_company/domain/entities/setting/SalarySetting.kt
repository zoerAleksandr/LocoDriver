package com.z_company.domain.entities.setting

import com.z_company.domain.util.generateId
import kotlinx.serialization.Serializable

const val SALARY_SETTINGS_KEY = "Salary_Setting_Key"

@Serializable
data class SalarySetting(
    val key: String = SALARY_SETTINGS_KEY,
    val nightTimePercent: Double = 40.0,
    val averagePaymentHour: Double = 0.0,
    val districtCoefficient : Double = 0.0,
    val nordicPercent: Double = 0.0,
    val onePersonOperationPercent: Double = 40.0,
    val onePersonOperationPassengerTrainPercent: Double = 50.0,
    // Памятка по оплате локомотивных бригад: базовая доплата за вредные условия
    // составляет 4%. Пользователь может изменить значение по локальному акту.
    val harmfulnessPercent: Double = 4.0,
    val percentLongDistanceTrain: Double = 0.0,
    val lengthLongDistanceTrain: Int = 0,
    val zonalSurcharge: Double = 25.0,
    val surchargeQualificationClass: Double = 0.0,
    var surchargeExtendedServicePhaseList: List<SurchargeExtendedServicePhase> = emptyList(),
    var surchargeHeavyTrainsList: List<SurchargeHeavyTrains> = emptyList(),
    var surchargeLongTrainsList: List<SurchargeLongTrains> = emptyList(),
    val surchargeHeavyLongDistanceTrains: Double = 5.0,
    val otherSurcharge: Double = 0.0,
    val ndfl: Double = 13.0,
    val unionistsRetention: Double = 1.0,
    val otherRetention: Double = 0.0,
    // Благосостояние — удержание от грязной суммы начисления (как Профсоюз/Прочие).
    val welfarePercent: Double = 0.0,
    // Алименты — удержание от чистой суммы к выдаче (после НДФЛ, Профсоюза,
    // Прочих удержаний и Благосостояния).
    val alimonyPercent: Double = 0.0,
    // Показывать в начислениях строку «Оплата недоработки» (доплата по среднему
    // часу за часы ниже индивидуальной нормы). Выключено — строка не считается
    // и не отображается на экране «Расчёт зарплаты».
    val showUnderworkPayments: Boolean = true
)

@Serializable
data class SurchargeExtendedServicePhase(
    val id: String = generateId(),
    var distance: String = "",
    var percentSurcharge: String = ""
)

@Serializable
data class SurchargeHeavyTrains(
    val id: String = generateId(),
    val weight: String = "",
    val percentSurcharge: String = ""
)

@Serializable
data class SurchargeLongTrains(
    val id: String = generateId(),
    val conditionalLength: String = "",
    val percentSurcharge: String = ""
)
