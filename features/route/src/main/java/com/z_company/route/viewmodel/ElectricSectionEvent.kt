package com.z_company.route.viewmodel

sealed class ElectricSectionEvent {
    data class EnteredAccepted(val index: Int, val data: String?) : ElectricSectionEvent()
    data class EnteredDelivery(val index: Int, val data: String?) : ElectricSectionEvent()
    data class EnteredRecoveryAccepted(val index: Int, val data: String?) : ElectricSectionEvent()
    data class EnteredRecoveryDelivery(val index: Int, val data: String?) : ElectricSectionEvent()
    data class FocusChange(val index: Int, val fieldName: ElectricSectionType) :
        ElectricSectionEvent()
}