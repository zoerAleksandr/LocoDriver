package com.z_company.iosapp.viewmodel

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route
import com.z_company.domain.use_cases.RouteUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

/**
 * KMP ViewModel for the route create/edit screen.
 *
 * Registered as Koin single. State resets on [loadRoute] call.
 *
 * @param routeUseCase UseCase from domain module (KMP-compatible).
 */
class FormIosViewModel(
    private val routeUseCase: RouteUseCase,
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
     * Loads route by [routeId]. If routeId == null, creates a new empty route.
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

    /** Updates route number. */
    fun updateNumber(value: String) {
        val current = _route.value ?: return
        _route.value = current.copy(
            basicData = current.basicData.copy(number = value.ifBlank { null })
        )
    }

    /** Updates notes. */
    fun updateNotes(value: String) {
        val current = _route.value ?: return
        _route.value = current.copy(
            basicData = current.basicData.copy(notes = value.ifBlank { null })
        )
    }

    /** Toggles favorite status. */
    fun toggleFavorite(value: Boolean) {
        val current = _route.value ?: return
        _route.value = current.copy(
            basicData = current.basicData.copy(isFavorite = value)
        )
    }

    /** Saves the current route to the DB. */
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
}
