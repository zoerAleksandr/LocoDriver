package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.use_cases.RouteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * KMP ViewModel for the profile screen.
 *
 * Loads total route count and favorites count from the local DB.
 */
class ProfileIosViewModel(
    private val routeUseCase: RouteUseCase,
) : ViewModel() {

    private val _totalRouteCount = MutableStateFlow(0)
    val totalRouteCount: StateFlow<Int> = _totalRouteCount.asStateFlow()

    private val _favoritesCount = MutableStateFlow(0)
    val favoritesCount: StateFlow<Int> = _favoritesCount.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            routeUseCase.getListRoutesAsFlow().collect { routes ->
                val active = routes.filter { !it.basicData.isDeleted }
                _totalRouteCount.value = active.size
                _favoritesCount.value = active.count { it.basicData.isFavorite }
                _isLoading.value = false
            }
        }
    }
}
