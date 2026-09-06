package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.CarInspector
import com.z_company.domain.entities.route.Train
import com.z_company.domain.use_cases.RouteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TrainFormIosViewModel(
    private val routeUseCase: RouteUseCase,
) : ViewModel() {

    private val _train = MutableStateFlow<Train?>(null)
    val train: StateFlow<Train?> = _train.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Загружает поезд ОДИН раз (`first`, не `collect`): постоянная подписка на
     * routeDetails переиздаёт маршрут при любой записи в БД и затирала бы
     * несохранённые правки пользователя значением из базы.
     */
    fun loadTrain(routeId: String, trainId: String?) {
        viewModelScope.launch {
            _isSaved.value = false
            _errorMessage.value = null
            // Маршрута может ещё не быть в БД: у нового маршрута FormIosViewModel
            // держит Route() только в памяти до нажатия «Сохранить». Тогда просто
            // заводим пустой поезд, чтобы форма была рабочей.
            val route = (routeUseCase.routeDetails(routeId)
                .first { it is ResultState.Success } as? ResultState.Success)?.data
            _train.value = route?.trains?.find { it.trainId == trainId }
                ?: Train(basicId = routeId)
        }
    }

    fun setNumber(value: String) { _train.update { it?.copy(number = value.ifBlank { null }) } }
    fun setWeight(value: String) { _train.update { it?.copy(weight = value.ifBlank { null }) } }
    fun setAxle(value: String) { _train.update { it?.copy(axle = value.ifBlank { null }) } }
    fun setDistance(value: String) { _train.update { it?.copy(distance = value.ifBlank { null }) } }
    fun setLength(value: String) { _train.update { it?.copy(conditionalLength = value.ifBlank { null }) } }

    // ── Вагонник: осматривает и закрепляет состав перед прицепкой ────────────
    // Опционален, поэтому добавляется/удаляется целиком, как толкач на Android.

    fun addCarInspector() {
        _train.update { it?.copy(carInspector = CarInspector()) }
    }

    fun removeCarInspector() {
        _train.update { it?.copy(carInspector = null) }
    }

    fun setCarInspectorFullName(value: String) {
        _train.update {
            it?.copy(
                carInspector = (it.carInspector ?: CarInspector())
                    .copy(fullName = value.ifBlank { null })
            )
        }
    }

    fun setCarInspectorTabNumber(value: String) {
        _train.update {
            it?.copy(
                carInspector = (it.carInspector ?: CarInspector())
                    .copy(tabNumber = value.ifBlank { null })
            )
        }
    }

    /** [ms] — миллисекунды epoch; 0 трактуем как «не задано». */
    fun setCarInspectorCouplingTime(ms: Long) {
        _train.update {
            it?.copy(
                carInspector = (it.carInspector ?: CarInspector())
                    .copy(couplingTime = ms.takeIf { v -> v > 0L })
            )
        }
    }

    /**
     * Сохраняет поезд, обновляя список поездов маршрута.
     *
     * Раньше форма не сохраняла вовсе: сеттеры меняли только объект в памяти,
     * а `_isSaved` никогда не выставлялся — весь ввод пропадал при выходе.
     * Маршрут читаем через `first`, иначе сохранение переиздаёт поток и
     * обработчик срабатывает повторно.
     */
    fun saveTrain() {
        val current = _train.value ?: return
        val routeId = current.basicId

        viewModelScope.launch {
            val route = (routeUseCase.routeDetails(routeId)
                .first { it is ResultState.Success } as? ResultState.Success)?.data
            if (route == null) {
                // Родительский маршрут ещё не сохранён — прицепить поезд не к чему.
                // Молча терять ввод нельзя, поэтому говорим об этом явно.
                _errorMessage.value = "Сначала сохраните маршрут, затем добавьте поезд"
                return@launch
            }

            val updatedTrains = route.trains.toMutableList()
            val index = updatedTrains.indexOfFirst { it.trainId == current.trainId }
            if (index >= 0) updatedTrains[index] = current else updatedTrains.add(current)

            routeUseCase.saveRoute(route.copy(trains = updatedTrains))
                .first { it !is ResultState.Loading }
                .let { if (it is ResultState.Success) _isSaved.value = true }
        }
    }

    // ── watch helpers for Swift callbacks ─────────────────────────────────────

    fun watchTrain(callback: (Train?) -> Unit) {
        viewModelScope.launch { train.collect { callback(it) } }
    }

    fun watchIsSaved(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isSaved.collect { callback(it) } }
    }

    fun watchErrorMessage(callback: (String?) -> Unit) {
        viewModelScope.launch { errorMessage.collect { callback(it) } }
    }

    fun clearError() { _errorMessage.value = null }

    private fun MutableStateFlow<Train?>.update(transform: (Train?) -> Train?) {
        value = transform(value)
    }
}
