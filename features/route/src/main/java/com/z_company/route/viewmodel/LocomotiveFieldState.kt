package com.z_company.route.viewmodel

enum class ElectricSectionType {
    ACCEPTED, DELIVERY, RECOVERY_ACCEPTED, RECOVERY_DELIVERY,
    ACCEPTED2, DELIVERY2, RECOVERY_ACCEPTED2, RECOVERY_DELIVERY2
}

data class ElectricSectionFieldState(
    val data: String? = null,
    val type: ElectricSectionType
)

data class ElectricSectionFormState(
    val sectionId: String,
    val accepted: ElectricSectionFieldState = ElectricSectionFieldState(type = ElectricSectionType.ACCEPTED),
    val accepted2: ElectricSectionFieldState = ElectricSectionFieldState(type = ElectricSectionType.ACCEPTED2),
    val delivery: ElectricSectionFieldState = ElectricSectionFieldState(type = ElectricSectionType.DELIVERY),
    val delivery2: ElectricSectionFieldState = ElectricSectionFieldState(type = ElectricSectionType.DELIVERY2),
    val recoveryAccepted: ElectricSectionFieldState = ElectricSectionFieldState(type = ElectricSectionType.RECOVERY_ACCEPTED),
    val recoveryAccepted2: ElectricSectionFieldState = ElectricSectionFieldState(type = ElectricSectionType.RECOVERY_ACCEPTED2),
    val recoveryDelivery: ElectricSectionFieldState = ElectricSectionFieldState(type = ElectricSectionType.RECOVERY_DELIVERY),
    val recoveryDelivery2: ElectricSectionFieldState = ElectricSectionFieldState(type = ElectricSectionType.RECOVERY_DELIVERY2),
    val formValid: Boolean = true,
    val errorMessage: String = "",
    val resultVisibility: Boolean = false,
    val expandItemState: Boolean = false,
    val showOtherCurrent: Boolean = false
)

enum class DieselSectionType {
    ACCEPTED, DELIVERY, COEFFICIENT, REFUEL, REFUEL_IN_KILO, REFUEL_COEFFICIENT
}

data class DieselSectionFieldState(
    val data: String? = null,
    val type: DieselSectionType
)
data class DieselSectionFormState(
    val sectionId: String,
    val accepted: DieselSectionFieldState = DieselSectionFieldState(type = DieselSectionType.ACCEPTED),
    val delivery: DieselSectionFieldState = DieselSectionFieldState(type = DieselSectionType.DELIVERY),
    val coefficient: DieselSectionFieldState = DieselSectionFieldState(type = DieselSectionType.COEFFICIENT),
    val refuel: DieselSectionFieldState = DieselSectionFieldState(type = DieselSectionType.REFUEL),
    val refuelInKilo: DieselSectionFieldState = DieselSectionFieldState(type = DieselSectionType.REFUEL_IN_KILO),
    val refuelCoefficient: DieselSectionFieldState = DieselSectionFieldState(type = DieselSectionType.REFUEL_COEFFICIENT),
    val formValid: Boolean = true,
    val errorMessage: String = "",
)