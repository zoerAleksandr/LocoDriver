package com.z_company.domain.entities.route

import com.z_company.domain.entities.serializers.NumberAsDoubleSerializer
import com.z_company.domain.util.generateId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class Locomotive(
    var locoId: String = generateId(),
    var basicId: String,
    var remoteObjectId: String? = null,
    var series: String? = null,
    var number: String? = null,
    var type: LocoType = LocoType.ELECTRIC,
    var electricSectionList: MutableList<SectionElectric> = mutableListOf(),
    var dieselSectionList: MutableList<SectionDiesel> = mutableListOf(),
    var timeStartOfAcceptance: Long? = null,
    var timeEndOfAcceptance: Long? = null,
    var timeStartOfDelivery: Long? = null,
    var timeEndOfDelivery: Long? = null,
    @Serializable(with = NumberAsDoubleSerializer::class)
    var normaElectricCurrent1: Double? = null,
    @Serializable(with = NumberAsDoubleSerializer::class)
    var normaElectricCurrent2: Double? = null,
    var normaDiesel: String? = null,
    var heatingCounterAccepted: Double? = null,
    var heatingCounterDelivery: Double? = null,
    var auxiliaryCounterAccepted: Double? = null,
    var auxiliaryCounterDelivery: Double? = null,
)

@Serializable
data class SectionElectric(
    var sectionId: String = generateId(),
    var locoId: String = "",
    var type: LocoType = LocoType.ELECTRIC,
    // @Contextual — использует DoubleAsStringSerializer через RemoteRestClient.appJson.
    // Числа отправляются на сервер как строки ("1783.32"), что предотвращает
    // потерю дробной части при хранении на сервере.
    @Contextual var acceptedEnergy: Double? = null,
    @Contextual var deliveryEnergy: Double? = null,
    @Contextual var acceptedRecovery: Double? = null,
    @Contextual var deliveryRecovery: Double? = null,
    @Contextual var acceptedEnergyOtherCurrent: Double? = null,
    @Contextual var deliveryEnergyOtherCurrent: Double? = null,
    @Contextual var acceptedRecoveryOtherCurrent: Double? = null,
    @Contextual var deliveryRecoveryOtherCurrent: Double? = null
)

@Serializable
data class SectionDiesel(
    var sectionId: String = generateId(),
    var locoId: String = "",
    var type: LocoType = LocoType.DIESEL,
    var acceptedFuel: Double? = null,
    var deliveryFuel: Double? = null,
    var coefficient: Double? = null,
    var fuelSupply: Double? = null,
    var fuelSupplyInKilo: Double? = null,
    var coefficientSupply: Double? = null,
)
