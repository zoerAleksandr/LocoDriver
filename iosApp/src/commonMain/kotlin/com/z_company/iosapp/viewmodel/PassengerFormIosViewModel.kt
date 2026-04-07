package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    /**
     * Загружает пассажирский поезд по [passengerId] из маршрута [routeId].
     * Если [passengerId] == null — создаёт новый пустой объект Passenger.
     */
    fun loadPassenger(routeId: String, passengerId: String?) {
        _isSaved.value = false
        viewModelScope.launch {
            _isLoading.value = true
            routeUseCase.routeDetails(routeId).collect { result ->
                val route = (result as? ResultState.Success)?.data ?: return@collect
                val found = route.passengers.find { it.passengerId == passengerId }
                _passenger.value = found ?: Passenger(basicId = routeId)
                _isLoading.value = false
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
                        is ResultState.Error -> { /* could expose errorMessage if needed */ }
                        is ResultState.Loading -> {}
                    }
                }
            }
        }
    }

    // ── watch helpers for Swift callbacks ─────────────────────────────────────

    fun watchPassenger(callback: (Passenger?) -> Unit) {
        viewModelScope.launch { passenger.collect { callback(it) } }
    }

    fun watchIsSaved(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isSaved.collect { callback(it) } }
    }

    fun watchIsLoading(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isLoading.collect { callback(it) } }
    }

    private fun MutableStateFlow<Passenger?>.update(transform: (Passenger?) -> Passenger?) {
        value = transform(value)
    }
}
