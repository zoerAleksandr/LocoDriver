package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.use_cases.TrainUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * KMP ViewModel for the train create/edit form.
 *
 * Registered as Koin single. State resets on [loadTrain] call.
 *
 * @param trainUseCase UseCase from domain module (KMP-compatible).
 */
class TrainFormIosViewModel(
    private val trainUseCase: TrainUseCase,
) : ViewModel() {

    private val _train = MutableStateFlow<Train?>(null)
    val train: StateFlow<Train?> = _train.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var loadJob: Job? = null

    /**
     * Loads train by [trainId]. If null, creates a new empty train for [basicId].
     */
    fun loadTrain(trainId: String?, basicId: String) {
        loadJob?.cancel()
        _isSaved.value = false
        _errorMessage.value = null

        if (trainId == null) {
            _train.value = Train(basicId = basicId)
            _isLoading.value = false
            return
        }

        loadJob = viewModelScope.launch {
            _isLoading.value = true
            trainUseCase.getTrainById(trainId).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        _train.value = result.data ?: Train(basicId = basicId)
                        _isLoading.value = false
                    }
                    is ResultState.Error -> {
                        _errorMessage.value = result.entity.message ?: "Ошибка загрузки поезда"
                        _isLoading.value = false
                    }
                    is ResultState.Loading -> {
                        _isLoading.value = true
                    }
                }
            }
        }
    }

    /** Updates train number. */
    fun updateNumber(value: String) {
        val current = _train.value ?: return
        _train.value = current.copy(number = value.ifBlank { null })
    }

    /** Updates train weight. */
    fun updateWeight(value: String) {
        val current = _train.value ?: return
        _train.value = current.copy(weight = value.ifBlank { null })
    }

    /** Updates train axle count. */
    fun updateAxle(value: String) {
        val current = _train.value ?: return
        _train.value = current.copy(axle = value.ifBlank { null })
    }

    /** Updates train conditional length. */
    fun updateConditionalLength(value: String) {
        val current = _train.value ?: return
        _train.value = current.copy(conditionalLength = value.ifBlank { null })
    }

    /** Updates train distance. */
    fun updateDistance(value: String) {
        val current = _train.value ?: return
        _train.value = current.copy(distance = value.ifBlank { null })
    }

    // --- Stations ---

    /** Adds a new empty station to the train. */
    fun addStation() {
        val current = _train.value ?: return
        val newStation = Station(trainId = current.trainId)
        _train.value = current.copy(
            stations = (current.stations + newStation).toMutableList()
        )
    }

    /** Removes the station at [index]. */
    fun removeStation(index: Int) {
        val current = _train.value ?: return
        val updated = current.stations.toMutableList()
        if (index in updated.indices) updated.removeAt(index)
        _train.value = current.copy(stations = updated)
    }

    /** Updates the station name at [index]. */
    fun updateStationName(index: Int, name: String) {
        updateStation(index) { it.copy(stationName = name.ifBlank { null }) }
    }

    /** Updates the arrival time for station at [index]. */
    fun updateStationTimeArrival(index: Int, millis: Long?) {
        updateStation(index) { it.copy(timeArrival = millis) }
    }

    /** Updates the departure time for station at [index]. */
    fun updateStationTimeDeparture(index: Int, millis: Long?) {
        updateStation(index) { it.copy(timeDeparture = millis) }
    }

    private fun updateStation(index: Int, transform: (Station) -> Station) {
        val current = _train.value ?: return
        val updated = current.stations.toMutableList()
        if (index in updated.indices) updated[index] = transform(updated[index])
        _train.value = current.copy(stations = updated)
    }

    /** Saves the current train to the DB. */
    fun saveTrain() {
        val current = _train.value ?: return
        viewModelScope.launch {
            trainUseCase.saveTrain(current).collect { result ->
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
