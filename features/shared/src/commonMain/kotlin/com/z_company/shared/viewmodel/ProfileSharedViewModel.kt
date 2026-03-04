package com.z_company.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.shared.platform.PlatformActions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared KMP ViewModel for the profile screen.
 *
 * Based on Android ProfileViewModel with constructor injection.
 * Loads total route count and favorites count from the local DB.
 */
class ProfileSharedViewModel(
    private val routeUseCase: RouteUseCase,
    private val platformActions: PlatformActions,
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

    fun openUrl(url: String) {
        platformActions.openUrl(url)
    }
}
