package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.use_cases.PassengerUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * KMP ViewModel for the passenger train create/edit form.
 *
 * Registered as Koin single. State resets on [loadPassenger] call.
 *
 * @param passengerUseCase UseCase from domain module (KMP-compatible).
 */
class PassengerFormIosViewModel(
    private val passengerUseCase: PassengerUseCase,
) : ViewModel() {

    private val _passenger = MutableStateFlow<Passenger?>(null)
    val passenger: StateFlow<Passenger?> = _passenger.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var loadJob: Job? = null

    /**
     * Loads passenger by [passengerId]. If null, creates a new empty passenger for [basicId].
     */
    fun loadPassenger(passengerId: String?, basicId: String) {
        loadJob?.cancel()
        _isSaved.value = false
        _errorMessage.value = null

        if (passengerId == null) {
            _passenger.value = Passenger(basicId = basicId)
            _isLoading.value = false
            return
        }

        loadJob = viewModelScope.launch {
            _isLoading.value = true
            passengerUseCase.getPassengerById(passengerId).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        _passenger.value = result.data ?: Passenger(basicId = basicId)
                        _isLoading.value = false
                    }
                    is ResultState.Error -> {
                        _errorMessage.value =
                            result.entity.message ?: "Ошибка загрузки пассажирского"
                        _isLoading.value = false
                    }
                    is ResultState.Loading -> {
                        _isLoading.value = true
                    }
                }
            }
        }
    }

    /** Updates passenger train number. */
    fun updateTrainNumber(value: String) {
        val current = _passenger.value ?: return
        _passenger.value = current.copy(trainNumber = value.ifBlank { null })
    }

    /** Updates departure station name. */
    fun updateStationDeparture(value: String) {
        val current = _passenger.value ?: return
        _passenger.value = current.copy(stationDeparture = value.ifBlank { null })
    }

    /** Updates arrival station name. */
    fun updateStationArrival(value: String) {
        val current = _passenger.value ?: return
        _passenger.value = current.copy(stationArrival = value.ifBlank { null })
    }

    /** Updates departure time (epoch millis). */
    fun setTimeDeparture(millis: Long?) {
        val current = _passenger.value ?: return
        _passenger.value = current.copy(timeDeparture = millis)
    }

    /** Updates arrival time (epoch millis). */
    fun setTimeArrival(millis: Long?) {
        val current = _passenger.value ?: return
        _passenger.value = current.copy(timeArrival = millis)
    }

    /** Updates notes. */
    fun updateNotes(value: String) {
        val current = _passenger.value ?: return
        _passenger.value = current.copy(notes = value.ifBlank { null })
    }

    /** Saves the current passenger to the DB. */
    fun savePassenger() {
        val current = _passenger.value ?: return
        viewModelScope.launch {
            passengerUseCase.savePassenger(current).collect { result ->
                when (result) {
                    is ResultState.Success -> _isSaved.value = true
                    is ResultState.Error -> _errorMessage.value =
                        result.entity.message ?: "Ошибка сохранения"
                    is ResultState.Loading -> {}
                }
            }
        }
    }

    /** Clears the error message after it has been shown. */
    fun clearError() {
        _errorMessage.value = null
    }
}
