package com.z_company.route.viewmodel

sealed class AlertBeforePurchasesEvent {
    data class ShowDialog(val dialogInfo: InfoDialogState): AlertBeforePurchasesEvent()
}