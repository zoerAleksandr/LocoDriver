package com.z_company.iosapp.viewmodel

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

/**
 * KMP ViewModel для экрана создания / редактирования маршрута.
 *
 * Зарегистрирован как Koin single — один экземпляр на время жизни приложения.
 * Состояние сбрасывается при вызове [loadRoute].
 *
 * @param routeUseCase UseCase из domain-модуля (KMP-совместимый).
 */
class FormIosViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
) : ViewModel() {

    private val _route = MutableStateFlow<Route?>(null)
    val route: StateFlow<Route?> = _route.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var loadJob: Job? = null

    /**
     * Загружает маршрут по [routeId]. Если routeId == null — создаёт новый пустой маршрут.
     */
    fun loadRoute(routeId: String?) {
        loadJob?.cancel()
        _isSaved.value = false
        _errorMessage.value = null

        if (routeId == null) {
            _route.value = Route()
            _isLoading.value = false
            return
        }

        loadJob = viewModelScope.launch {
            _isLoading.value = true
            routeUseCase.routeDetails(routeId).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        _route.value = result.data ?: Route()
                        _isLoading.value = false
                    }
                    is ResultState.Error -> {
                        _errorMessage.value = result.entity.message ?: "Ошибка загрузки маршрута"
                        _isLoading.value = false
                    }
                    is ResultState.Loading -> {
                        _isLoading.value = true
                    }
                }
            }
        }
    }

    /** Обновляет номер маршрута. */
    fun updateNumber(value: String) {
        val current = _route.value ?: return
        _route.value = current.copy(
            basicData = current.basicData.copy(number = value.ifBlank { null })
        )
    }

    /** Обновляет заметки. */
    fun updateNotes(value: String) {
        val current = _route.value ?: return
        _route.value = current.copy(
            basicData = current.basicData.copy(notes = value.ifBlank { null })
        )
    }

    /** Устанавливает время начала работы (миллисекунды UTC). Null — сбрасывает значение. */
    fun setTimeStartWork(ms: Long?) {
        val current = _route.value ?: return
        val start = ms?.let { it - it % 60_000L }
        val settings = settingsUseCase.getUserSetting()
        _route.value = current.copy(
            basicData = current.basicData.copy(
                timeStartWork = start,
                timeEndWork = if (start != null && settings.usingDefaultWorkTime) {
                    start + settings.defaultWorkTime
                } else {
                    current.basicData.timeEndWork
                }
            )
        )
    }

    /** Устанавливает время окончания работы (миллисекунды UTC). Null — сбрасывает значение. */
    fun setTimeEndWork(ms: Long?) {
        val current = _route.value ?: return
        _route.value = current.copy(
            basicData = current.basicData.copy(timeEndWork = ms)
        )
    }

    /** Сохраняет текущий маршрут в БД. */
    fun saveRoute() {
        val currentRoute = _route.value ?: return
        viewModelScope.launch {
            routeUseCase.saveRoute(currentRoute).collect { result ->
                when (result) {
                    is ResultState.Success -> _isSaved.value = true
                    is ResultState.Error -> _errorMessage.value =
                        result.entity.message ?: "Ошибка сохранения"
                    is ResultState.Loading -> {}
                }
            }
        }
    }

    // ── watchState helpers ────────────────────────────────────────────────────

    fun watchRoute(callback: (Route?) -> Unit) {
        viewModelScope.launch { route.collect { callback(it) } }
    }

    fun watchIsLoading(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isLoading.collect { callback(it) } }
    }

    fun watchIsSaved(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isSaved.collect { callback(it) } }
    }

    fun watchErrorMessage(callback: (String?) -> Unit) {
        viewModelScope.launch { errorMessage.collect { callback(it) } }
    }
}
