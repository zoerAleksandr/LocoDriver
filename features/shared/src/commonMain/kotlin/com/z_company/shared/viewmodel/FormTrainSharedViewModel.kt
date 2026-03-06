package com.z_company.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.TrainAssist
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.use_cases.TrainUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared KMP ViewModel for the train create/edit form.
 *
 * Based on Android TrainFormViewModel with constructor injection.
 * Supports stations, service phases, train assist (pusher / doubleTraction / doubledTrain).
 */
class FormTrainSharedViewModel(
    private val trainUseCase: TrainUseCase,
    private val settingsUseCase: SettingsUseCase,
) : ViewModel() {

    private val _train = MutableStateFlow<Train?>(null)
    val train: StateFlow<Train?> = _train.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _stationNames = MutableStateFlow<List<String>>(emptyList())
    val stationNames: StateFlow<List<String>> = _stationNames.asStateFlow()

    private val _servicePhases = MutableStateFlow<List<ServicePhase>>(emptyList())
    val servicePhases: StateFlow<List<ServicePhase>> = _servicePhases.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            settingsUseCase.getUserSettingFlow().collect { settings ->
                _stationNames.value = settings.stationList
                _servicePhases.value = settings.servicePhases
            }
        }
    }

    /**
     * Loads train by [trainId]. If null, creates a new train for [basicId].
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
                    is ResultState.Loading -> _isLoading.value = true
                }
            }
        }
    }

    // --- Basic fields ---

    fun updateNumber(value: String) = updateTrain { copy(number = value.ifBlank { null }) }
    fun updateDistance(value: String) = updateTrain { copy(distance = value.ifBlank { null }) }
    fun updateWeight(value: String) = updateTrain { copy(weight = value.ifBlank { null }) }
    fun updateAxle(value: String) = updateTrain { copy(axle = value.ifBlank { null }) }
    fun updateConditionalLength(value: String) = updateTrain { copy(conditionalLength = value.ifBlank { null }) }

    // --- Service phase ---

    fun selectServicePhase(phase: ServicePhase?) {
        updateTrain { copy(servicePhase = phase) }
    }

    // --- Stations ---

    fun addStation() {
        val current = _train.value ?: return
        val station = Station(trainId = current.trainId)
        _train.value = current.copy(
            stations = (current.stations + station).toMutableList()
        )
    }

    fun removeStation(index: Int) {
        val current = _train.value ?: return
        val list = current.stations.toMutableList()
        if (index in list.indices) list.removeAt(index)
        _train.value = current.copy(stations = list)
    }

    fun updateStationName(index: Int, name: String) =
        updateStation(index) { it.copy(stationName = name.ifBlank { null }) }

    fun updateStationArrival(index: Int, millis: Long?) =
        updateStation(index) { it.copy(timeArrival = millis) }

    fun updateStationDeparture(index: Int, millis: Long?) =
        updateStation(index) { it.copy(timeDeparture = millis) }

    // --- Train assist ---

    fun updatePusher(assist: TrainAssist?) = updateTrain { copy(pusher = assist) }
    fun updateDoubleTraction(assist: TrainAssist?) = updateTrain { copy(doubleTraction = assist) }
    fun updateDoubledTrain(assist: TrainAssist?) = updateTrain { copy(doubledTrain = assist) }

    // --- Save ---

    fun saveTrain() {
        val current = _train.value ?: return
        viewModelScope.launch {
            trainUseCase.saveTrain(current).collect { result ->
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

    private inline fun updateTrain(block: Train.() -> Train) {
        _train.value = _train.value?.block()
    }

    private inline fun updateStation(index: Int, transform: (Station) -> Station) {
        val current = _train.value ?: return
        val list = current.stations.toMutableList()
        if (index in list.indices) list[index] = transform(list[index])
        _train.value = current.copy(stations = list)
    }
}
