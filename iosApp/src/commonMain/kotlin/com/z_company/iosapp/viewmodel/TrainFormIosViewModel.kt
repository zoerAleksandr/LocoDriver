package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.z_company.core.AppError
import com.z_company.core.ResultState
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

    // Пустой _error для consistency с другими Form-VM (save-метод
    // здесь не реализован, поэтому publish'ить пока некуда). Шаг 5
    // Swift Wrapper всё равно ожидает поле для единого паттерна alert.
    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    fun loadTrain(routeId: String, trainId: String?) {
        viewModelScope.launch {
            routeUseCase.routeDetails(routeId).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        val route = result.data ?: return@collect
                        val found = route.trains.find { it.trainId == trainId }
                        _train.value = found ?: Train(basicId = routeId)
                    }
                    // silent + Kermit: load — passive.
                    is ResultState.Error -> Logger.withTag("TrainForm").w {
                        "loadTrain failed silently: ${result.entity.message}"
                    }
                    is ResultState.Loading -> {}
                }
            }
        }
    }

    fun setNumber(value: String) { _train.update { it?.copy(number = value.ifBlank { null }) } }
    fun setWeight(value: String) { _train.update { it?.copy(weight = value.ifBlank { null }) } }
    fun setAxle(value: String) { _train.update { it?.copy(axle = value.ifBlank { null }) } }
    fun setDistance(value: String) { _train.update { it?.copy(distance = value.ifBlank { null }) } }
    fun setLength(value: String) { _train.update { it?.copy(conditionalLength = value.ifBlank { null }) } }
    fun setIsHeavy(value: Boolean) { _train.update { it?.copy(isHeavyLongDistance = value) } }

    fun watchTrain(callback: (Train?) -> Unit): WatchHandle =
        WatchHandle(viewModelScope.launch { train.collect { callback(it) } })

    fun watchIsSaved(callback: (Boolean) -> Unit): WatchHandle =
        WatchHandle(viewModelScope.launch { isSaved.collect { callback(it) } })

    fun watchError(callback: (AppError?) -> Unit): WatchHandle =
        WatchHandle(viewModelScope.launch { error.collect { callback(it) } })

    fun clearError() { _error.value = null }

    private fun MutableStateFlow<Train?>.update(transform: (Train?) -> Train?) {
        value = transform(value)
    }
}
