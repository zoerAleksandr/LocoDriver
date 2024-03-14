package com.z_company.route.viewmodel

enum class ElectricSectionType {
    ACCEPTED, DELIVERY, RECOVERY_ACCEPTED, RECOVERY_DELIVERY
}

data class ElectricSectionFieldState(
    val data: String? = null,
    val type: ElectricSectionType
)

data class ElectricSectionFormState(
    val sectionId: String,
    val accepted: ElectricSectionFieldState = ElectricSectionFieldState(type = ElectricSectionType.ACCEPTED),
    val delivery: ElectricSectionFieldState = ElectricSectionFieldState(type = ElectricSectionType.DELIVERY),
    val recoveryAccepted: ElectricSectionFieldState = ElectricSectionFieldState(type = ElectricSectionType.RECOVERY_ACCEPTED),
    val recoveryDelivery: ElectricSectionFieldState = ElectricSectionFieldState(type = ElectricSectionType.RECOVERY_DELIVERY),
    val formValid: Boolean = true,
    val errorMessage: String = "",
    val resultVisibility: Boolean = false,
    val expandItemState: Boolean = false
)

enum class DieselSectionType {
    ACCEPTED, DELIVERY, COEFFICIENT, REFUEL
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
    val formValid: Boolean = true,
    val errorMessage: String = "",
)