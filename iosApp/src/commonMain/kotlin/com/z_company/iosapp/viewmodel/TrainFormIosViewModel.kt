package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.route.Train
import com.z_company.domain.use_cases.RouteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrainFormIosViewModel(
    private val routeUseCase: RouteUseCase,
) : ViewModel() {

    private val _train = MutableStateFlow<Train?>(null)
    val train: StateFlow<Train?> = _train.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    fun loadTrain(routeId: String, trainId: String?) {
        viewModelScope.launch {
            routeUseCase.routeDetails(routeId).collect { result ->
                val route = (result as? com.z_company.core.ResultState.Success)?.data ?: return@collect
                val found = route.trains.find { it.trainId == trainId }
                _train.value = found ?: Train(basicId = routeId)
            }
        }
    }

    fun setNumber(value: String) { _train.update { it?.copy(number = value.ifBlank { null }) } }
    fun setWeight(value: String) { _train.update { it?.copy(weight = value.ifBlank { null }) } }
    fun setAxle(value: String) { _train.update { it?.copy(axle = value.ifBlank { null }) } }
    fun setDistance(value: String) { _train.update { it?.copy(distance = value.ifBlank { null }) } }
    fun setLength(value: String) { _train.update { it?.copy(conditionalLength = value.ifBlank { null }) } }
    fun setIsHeavy(value: Boolean) { _train.update { it?.copy(isHeavyLongDistance = value) } }

    fun watchTrain(callback: (Train?) -> Unit) {
        viewModelScope.launch { train.collect { callback(it) } }
    }

    fun watchIsSaved(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isSaved.collect { callback(it) } }
    }

    private fun MutableStateFlow<Train?>.update(transform: (Train?) -> Train?) {
        value = transform(value)
    }
}
