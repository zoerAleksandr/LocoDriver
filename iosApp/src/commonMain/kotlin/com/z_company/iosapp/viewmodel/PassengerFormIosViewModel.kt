package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.z_company.core.AppError
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.use_cases.RouteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PassengerFormIosViewModel(
    private val routeUseCase: RouteUseCase,
) : ViewModel() {

    private val _passenger = MutableStateFlow<Passenger?>(null)
    val passenger: StateFlow<Passenger?> = _passenger.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // load — silent + Kermit (passive); save — explicit publish.
    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    /**
     * Загружает пассажирский поезд по [passengerId] из маршрута [routeId].
     * Если [passengerId] == null — создаёт новый пустой объект Passenger.
     */
    fun loadPassenger(routeId: String, passengerId: String?) {
        _isSaved.value = false
        _error.value = null
        viewModelScope.launch {
            _isLoading.value = true
            routeUseCase.routeDetails(routeId).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        val route = result.data ?: return@collect
                        val found = route.passengers.find { it.passengerId == passengerId }
                        _passenger.value = found ?: Passenger(basicId = routeId)
                        _isLoading.value = false
                    }
                    // silent + Kermit: load — passive.
                    is ResultState.Error -> {
                        Logger.withTag("PassengerForm").w {
                            "loadPassenger failed silently: ${result.entity.message}"
                        }
                        _isLoading.value = false
                    }
                    is ResultState.Loading -> {}
                }
            }
        }
    }

    fun setTrainNumber(value: String) {
        _passenger.update { it?.copy(trainNumber = value.ifBlank { null }) }
    }

    fun setDepartureStation(value: String) {
        _passenger.update { it?.copy(stationDeparture = value.ifBlank { null }) }
    }

    fun setArrivalStation(value: String) {
        _passenger.update { it?.copy(stationArrival = value.ifBlank { null }) }
    }

    fun setTimeDeparture(ms: Long?) {
        _passenger.update { it?.copy(timeDeparture = ms) }
    }

    fun setTimeArrival(ms: Long?) {
        _passenger.update { it?.copy(timeArrival = ms) }
    }

    fun setNotes(value: String) {
        _passenger.update { it?.copy(notes = value.ifBlank { null }) }
    }

    /**
     * Сохраняет текущего пассажира, обновляя список пассажиров в маршруте.
     */
    fun savePassenger() {
        val current = _passenger.value ?: return
        val routeId = current.basicId

        viewModelScope.launch {
            routeUseCase.routeDetails(routeId).collect { result ->
                val route = (result as? ResultState.Success)?.data ?: return@collect

                val updatedPassengers = route.passengers.toMutableList()
                val index = updatedPassengers.indexOfFirst { it.passengerId == current.passengerId }
                if (index >= 0) {
                    updatedPassengers[index] = current
                } else {
                    updatedPassengers.add(current)
                }

                val updatedRoute = route.copy(passengers = updatedPassengers)
                routeUseCase.saveRoute(updatedRoute).collect { saveResult ->
                    when (saveResult) {
                        is ResultState.Success -> _isSaved.value = true
                        // explicit publish: пользователь нажал «Сохранить».
                        is ResultState.Error -> {
                            _error.value = saveResult.entity.appError
                            Logger.withTag("PassengerForm").e {
                                "savePassenger failed: ${saveResult.entity.message}"
                            }
                        }
                        is ResultState.Loading -> {}
                    }
                }
            }
        }
    }

    // ── watch helpers for Swift callbacks ─────────────────────────────────────

    fun watchPassenger(callback: (Passenger?) -> Unit): WatchHandle =
        WatchHandle(viewModelScope.launch { passenger.collect { callback(it) } })

    fun watchIsSaved(callback: (Boolean) -> Unit): WatchHandle =
        WatchHandle(viewModelScope.launch { isSaved.collect { callback(it) } })

    fun watchIsLoading(callback: (Boolean) -> Unit): WatchHandle =
        WatchHandle(viewModelScope.launch { isLoading.collect { callback(it) } })

    fun watchError(callback: (AppError?) -> Unit): WatchHandle =
        WatchHandle(viewModelScope.launch { error.collect { callback(it) } })

    fun clearError() { _error.value = null }

    private fun MutableStateFlow<Passenger?>.update(transform: (Passenger?) -> Passenger?) {
        value = transform(value)
    }
}
