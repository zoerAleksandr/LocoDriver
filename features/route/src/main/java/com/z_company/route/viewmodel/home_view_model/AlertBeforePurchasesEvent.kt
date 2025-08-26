package com.z_company.route.viewmodel.home_view_model

sealed class AlertBeforePurchasesEvent {
    object ShowDialogNeedSubscribe: AlertBeforePurchasesEvent()
    object ShowDialogAlertSubscribe: AlertBeforePurchasesEvent()
}