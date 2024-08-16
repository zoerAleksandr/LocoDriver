package com.z_company.route.viewmodel

sealed class AlertBeforePurchasesEvent {
    object ShowDialogNeedSubscribe: AlertBeforePurchasesEvent()
    object ShowDialogAlertSubscribe: AlertBeforePurchasesEvent()
}