package com.z_company.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.use_cases.PassengerUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared KMP ViewModel for the passenger train create/edit form.
 *
 * Based on Android PassengerFormViewModel with constructor injection.
 * Supports station name autocomplete from settings.
 */
class FormPassengerSharedViewModel(
    private val passengerUseCase: PassengerUseCase,
    private val settingsUseCase: SettingsUseCase,
) : ViewModel() {

    private val _passenger = MutableStateFlow<Passenger?>(null)
    val passenger: StateFlow<Passenger?> = _passenger.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _stationNames = MutableStateFlow<List<String>>(emptyList())
    val stationNames: StateFlow<List<String>> = _stationNames.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            settingsUseCase.getUserSettingFlow().collect { settings ->
                _stationNames.value = settings.stationList
            }
        }
    }

    /**
     * Loads passenger by [passengerId]. If null, creates a new passenger for [basicId].
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
                    is ResultState.Loading -> _isLoading.value = true
                }
            }
        }
    }

    // --- Fields ---

    fun updateTrainNumber(value: String) = updatePassenger { copy(trainNumber = value.ifBlank { null }) }
    fun updateStationDeparture(value: String) = updatePassenger { copy(stationDeparture = value.ifBlank { null }) }
    fun updateStationArrival(value: String) = updatePassenger { copy(stationArrival = value.ifBlank { null }) }
    fun setTimeDeparture(millis: Long?) = updatePassenger { copy(timeDeparture = millis) }
    fun setTimeArrival(millis: Long?) = updatePassenger { copy(timeArrival = millis) }
    fun updateNotes(value: String) = updatePassenger { copy(notes = value.ifBlank { null }) }

    // --- Save ---

    fun savePassenger() {
        val current = _passenger.value ?: return
        viewModelScope.launch {
            passengerUseCase.savePassenger(current).collect { result ->
                when (result) {
                    is ResultState.Success -> _isSaved.value = true
                    is ResultState.Error -> _errorMessage.value =
                        result.entity.message ?: "Ошибка сохранения"
                    is ResultState.Loading -> { /* waiting */ }
                }
            }
        }
    }

    fun clearError() { _errorMessage.value = null }

    // --- Helpers ---

    private inline fun updatePassenger(block: Passenger.() -> Passenger) {
        _passenger.value = _passenger.value?.block()
    }
}
