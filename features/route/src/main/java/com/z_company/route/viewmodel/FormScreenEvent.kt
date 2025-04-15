package com.z_company.route.viewmodel

sealed class FormScreenEvent {
    object ActivatedFavoriteRoute: FormScreenEvent()
    object DeactivatedFavoriteRoute: FormScreenEvent()
}