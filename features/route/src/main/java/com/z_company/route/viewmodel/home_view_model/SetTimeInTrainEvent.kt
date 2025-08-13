package com.z_company.route.viewmodel.home_view_model

sealed class SetTimeInTrainEvent {
    data class SetTimeArrival(val message: String?, val isOnTheWay: Boolean): SetTimeInTrainEvent()
    data class SetTimeDeparture(val message: String?, val isOnTheWay: Boolean): SetTimeInTrainEvent()
}