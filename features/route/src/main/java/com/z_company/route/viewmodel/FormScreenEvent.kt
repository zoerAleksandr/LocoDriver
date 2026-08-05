package com.z_company.route.viewmodel

enum class ChildEntityType { LOCOMOTIVE, TRAIN, PASSENGER, OTHER_WORK }

sealed class FormScreenEvent {
    object ActivatedFavoriteRoute: FormScreenEvent()
    object DeactivatedFavoriteRoute: FormScreenEvent()
    object RouteSaved: FormScreenEvent()
    data class NavigateToChildForm(val basicId: String, val entityType: ChildEntityType) : FormScreenEvent()
    /** Открыть экран выбора напарников (мультивыбор из справочника). */
    data class NavigateToPartnerPicker(val basicId: String) : FormScreenEvent()
}
