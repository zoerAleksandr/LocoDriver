package com.z_company.route.viewmodel.home_view_model

sealed class UpdateEvent {
    object UpdateCompleted : UpdateEvent()
}