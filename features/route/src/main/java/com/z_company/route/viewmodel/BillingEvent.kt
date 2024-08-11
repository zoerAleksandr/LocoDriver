package com.z_company.route.viewmodel

import androidx.annotation.StringRes

sealed class BillingEvent {
    data class ShowDialog(val dialogInfo: InfoDialogState): BillingEvent()
    data class ShowError(val error: Throwable): BillingEvent()
}


data class InfoDialogState(
    @StringRes val titleRes: Int,
    val message: String
)