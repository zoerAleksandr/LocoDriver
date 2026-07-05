package com.z_company.route.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.Passenger

data class PassengerFormUiState(
    val passengerDetailState: ResultState<Passenger?> = ResultState.Loading(),
    val savePassengerState: ResultState<Unit>? = null,
    val errorMessage: String? = null,
    val resultTime: Long? = null,
    val formValid: Boolean = true,
    val exitFromScreen: Boolean = false,
    val changesHaveState: Boolean = false,
    val confirmExitDialogShow: Boolean = false,
    val isExpandMenuDepartureStation: Boolean = false,
    val isExpandMenuArrivalStation: Boolean = false,
    val stationList: SnapshotStateList<String> = mutableStateListOf(),
    // Рабочее окно маршрута — чтобы показать, что следование вне [явка, сдача] не
    // входит в оплату (если «явка по прибытию» выключена).
    val routeWorkStart: Long? = null,
    val routeWorkEnd: Long? = null,
    var dateAndTimeConverter: DateAndTimeConverter? = null
)