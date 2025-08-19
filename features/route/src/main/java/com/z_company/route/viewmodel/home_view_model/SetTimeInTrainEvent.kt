package com.z_company.route.viewmodel.home_view_model

data class SetTimeInTrainEvent(
    val message: String?,
    val isOnTheWay: Boolean,
    val isShowSnackbar: Boolean = true
)