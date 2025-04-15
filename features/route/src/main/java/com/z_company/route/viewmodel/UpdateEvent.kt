package com.z_company.route.viewmodel

sealed class UpdateEvent {
    object UpdateCompleted : UpdateEvent()
}