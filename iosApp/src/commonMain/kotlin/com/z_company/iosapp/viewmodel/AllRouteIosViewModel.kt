package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.route.Route
import com.z_company.domain.use_cases.RouteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AllRouteIosViewModel(
    private val routeUseCase: RouteUseCase,
) : ViewModel() {

    private val _routes = MutableStateFlow<List<Route>>(emptyList())
    val routes: StateFlow<List<Route>> = _routes.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _isLoading.value = true
            routeUseCase.getListRoutesAsFlow().collect { allRoutes ->
                _routes.value = allRoutes
                    .filter { !it.basicData.isDeleted }
                    .sortedByDescending { it.basicData.timeStartWork ?: 0L }
                _isLoading.value = false
            }
        }
    }
}
