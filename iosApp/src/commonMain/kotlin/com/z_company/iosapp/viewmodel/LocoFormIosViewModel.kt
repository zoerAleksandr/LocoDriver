package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.z_company.core.AppError
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.SectionElectric
import com.z_company.domain.entities.route.SectionDiesel
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.entities.route.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LocoFormIosViewModel(
    private val routeUseCase: RouteUseCase,
) : ViewModel() {

    private val _loco = MutableStateFlow<Locomotive?>(null)
    val loco: StateFlow<Locomotive?> = _loco.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    // Параллельный StateFlow типизированных ошибок (Шаг 4 промпта).
    // load — silent + Kermit (passive, ошибка маловероятна);
    // save — explicit publish (пользователь нажал «Сохранить»).
    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    // Последний загруженный маршрут — нужен для saveLoco, чтобы сохранить
    // локомотив в составе всего Route (репозиторий умеет только saveRoute).
    private var currentRoute: Route? = null

    fun loadLoco(routeId: String, locoId: String?) {
        // На всякий случай сбросим «залипший» isSaved от прошлой сессии —
        // иначе onChange(of: isSaved) в SwiftUI сразу закроет форму.
        _isSaved.value = false
        _error.value = null
        viewModelScope.launch {
            routeUseCase.routeDetails(routeId).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        val route = result.data ?: return@collect
                        currentRoute = route
                        val found = route.locomotives.find { it.locoId == locoId }
                        _loco.value = if (found != null) {
                            found
                        } else {
                            // Новый локомотив — сразу добавляем первую секцию в
                            // зависимости от типа тяги (по умолчанию электровоз).
                            withAtLeastOneSection(Locomotive(basicId = routeId))
                        }
                    }
                    // silent + Kermit: load — passive, ошибку в UI не показываем.
                    is ResultState.Error -> Logger.withTag("LocoForm").w {
                        "loadLoco failed silently: ${result.entity.message}"
                    }
                    is ResultState.Loading -> {}
                }
            }
        }
    }

    /// Сохраняет текущий локомотив в составе маршрута. Если локомотив уже
    /// есть в списке (по `locoId`), он заменяется; иначе добавляется.
    fun saveLoco() {
        val loco = _loco.value ?: return
        val route = currentRoute ?: return
        viewModelScope.launch {
            val locos = route.locomotives
            val existingIndex = locos.indexOfFirst { it.locoId == loco.locoId }
            if (existingIndex >= 0) {
                locos[existingIndex] = loco
            } else {
                locos.add(loco)
            }
            val state = routeUseCase.saveRoute(route).first { it !is ResultState.Loading }
            when (state) {
                is ResultState.Success -> _isSaved.value = true
                // explicit publish: пользователь нажал «Сохранить» — алерт обязателен.
                is ResultState.Error -> {
                    _error.value = state.entity.appError
                    Logger.withTag("LocoForm").e { "saveLoco failed: ${state.entity.message}" }
                }
                is ResultState.Loading -> {}
            }
        }
    }

    fun setSeries(value: String) {
        _loco.update { it?.copy(series = value.ifBlank { null }) }
    }

    fun setNumber(value: String) {
        _loco.update { it?.copy(number = value.ifBlank { null }) }
    }

    /// Переключение вида тяги. Если для нового типа ещё нет секций —
    /// добавляем одну, чтобы пользователю было с чем работать сразу.
    fun setType(type: LocoType) {
        _loco.update { current ->
            val loco = current ?: return@update null
            withAtLeastOneSection(loco.copy(type = type))
        }
    }

    /// Добавляет новую пустую секцию в список, соответствующий текущему
    /// типу тяги. Используется `copy()` с НОВОЙ MutableList — StateFlow
    /// сравнивает через `equals`, а data-class equals проверяет содержимое,
    /// поэтому мутация in-place + copy() не триггерит эмит.
    fun addSection() {
        _loco.update { current ->
            val loco = current ?: return@update null
            when (loco.type) {
                LocoType.ELECTRIC -> loco.copy(
                    electricSectionList = (loco.electricSectionList +
                        SectionElectric(locoId = loco.locoId)).toMutableList()
                )
                LocoType.DIESEL -> loco.copy(
                    dieselSectionList = (loco.dieselSectionList +
                        SectionDiesel(locoId = loco.locoId)).toMutableList()
                )
            }
        }
    }

    /// Возвращает локомотив с гарантированно непустым списком секций
    /// текущего типа (новая MutableList, чтобы StateFlow эмитил изменение).
    private fun withAtLeastOneSection(loco: Locomotive): Locomotive =
        when (loco.type) {
            LocoType.ELECTRIC -> if (loco.electricSectionList.isEmpty()) {
                loco.copy(electricSectionList = mutableListOf(SectionElectric(locoId = loco.locoId)))
            } else loco
            LocoType.DIESEL -> if (loco.dieselSectionList.isEmpty()) {
                loco.copy(dieselSectionList = mutableListOf(SectionDiesel(locoId = loco.locoId)))
            } else loco
        }

    fun setTimeStartAcceptance(ms: Long?) {
        _loco.update { it?.copy(timeStartOfAcceptance = ms) }
    }

    fun setTimeEndAcceptance(ms: Long?) {
        _loco.update { it?.copy(timeEndOfAcceptance = ms) }
    }

    fun setTimeStartDelivery(ms: Long?) {
        _loco.update { it?.copy(timeStartOfDelivery = ms) }
    }

    fun setTimeEndDelivery(ms: Long?) {
        _loco.update { it?.copy(timeEndOfDelivery = ms) }
    }

    fun watchLoco(callback: (Locomotive?) -> Unit): WatchHandle =
        WatchHandle(viewModelScope.launch { loco.collect { callback(it) } })

    fun watchIsSaved(callback: (Boolean) -> Unit): WatchHandle =
        WatchHandle(viewModelScope.launch { isSaved.collect { callback(it) } })

    fun watchError(callback: (AppError?) -> Unit): WatchHandle =
        WatchHandle(viewModelScope.launch { error.collect { callback(it) } })

    fun clearError() { _error.value = null }

    private fun MutableStateFlow<Locomotive?>.update(transform: (Locomotive?) -> Locomotive?) {
        value = transform(value)
    }
}
