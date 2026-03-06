package com.z_company.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getBreakDuration
import com.z_company.domain.use_cases.RouteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shared KMP ViewModel for the All Routes screen.
 *
 * Based on Android AllRouteViewModel with constructor injection.
 * Supports loading all routes, filtering, sorting, delete, favorite.
 */
class AllRouteSharedViewModel(
    private val routeUseCase: RouteUseCase,
) : ViewModel() {

    enum class SortOption { DATE_ASC, DATE_DESC, WORKTIME_ASC, WORKTIME_DESC }

    enum class RouteFilter {
        ALL, FAVORITES, HEAVY, HAS_BREAK, OVER_12_HOURS, ONE_PERSON
    }

    private val _allRoutes = MutableStateFlow<List<Route>>(emptyList())

    private val _filteredRoutes = MutableStateFlow<List<Route>>(emptyList())
    val filteredRoutes: StateFlow<List<Route>> = _filteredRoutes.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.DATE_DESC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _selectedFilters = MutableStateFlow<Set<RouteFilter>>(setOf(RouteFilter.ALL))
    val selectedFilters: StateFlow<Set<RouteFilter>> = _selectedFilters.asStateFlow()

    init {
        // Load routes
        viewModelScope.launch {
            routeUseCase.getListRoutesAsFlow().collect { routes ->
                _allRoutes.value = routes.filter { !it.basicData.isDeleted }
                _isLoading.value = false
            }
        }

        // React to filter/sort changes
        viewModelScope.launch {
            combine(_allRoutes, _selectedFilters, _sortOption) { routes, filters, sort ->
                Triple(routes, filters, sort)
            }.collect { (routes, filters, sort) ->
                val filtered = applyFilters(routes, filters)
                _filteredRoutes.value = applySorting(filtered, sort)
            }
        }
    }

    fun setSort(option: SortOption) {
        _sortOption.value = option
    }

    fun toggleFilter(filter: RouteFilter) {
        _selectedFilters.update { current ->
            val newSet = current.toMutableSet()
            if (filter == RouteFilter.ALL) {
                newSet.clear()
                newSet.add(RouteFilter.ALL)
            } else {
                newSet.remove(RouteFilter.ALL)
                if (newSet.contains(filter)) newSet.remove(filter) else newSet.add(filter)
                if (newSet.isEmpty()) newSet.add(RouteFilter.ALL)
            }
            newSet
        }
    }

    fun deleteRoute(route: Route) {
        viewModelScope.launch {
            routeUseCase.markAsRemoved(route).collect { result ->
                if (result is ResultState.Error) {
                    _errorMessage.value = result.entity.message ?: "Ошибка удаления"
                }
            }
        }
    }

    fun setFavoriteRoute(route: Route) {
        viewModelScope.launch {
            val updated = route.copy(
                basicData = route.basicData.copy(isFavorite = !route.basicData.isFavorite)
            )
            routeUseCase.saveRoute(updated).collect { result ->
                if (result is ResultState.Error) {
                    _errorMessage.value = result.entity.message ?: "Ошибка"
                }
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun applyFilters(routes: List<Route>, filters: Set<RouteFilter>): List<Route> {
        if (filters.contains(RouteFilter.ALL)) return routes

        return routes.filter { route ->
            var ok = true
            if (filters.contains(RouteFilter.FAVORITES)) {
                ok = ok && route.basicData.isFavorite
            }
            if (filters.contains(RouteFilter.ONE_PERSON)) {
                ok = ok && route.basicData.isOnePersonOperation
            }
            if (filters.contains(RouteFilter.OVER_12_HOURS)) {
                val start = route.basicData.timeStartWork ?: 0L
                val end = route.basicData.timeEndWork ?: 0L
                ok = ok && (end > start && (end - start) > 43_200_000L)
            }
            if (filters.contains(RouteFilter.HAS_BREAK)) {
                ok = ok && (route.getBreakDuration() > 0L)
            }
            ok
        }
    }

    private fun applySorting(routes: List<Route>, sort: SortOption): List<Route> {
        return when (sort) {
            SortOption.DATE_ASC -> routes.sortedBy { it.basicData.timeStartWork ?: 0L }
            SortOption.DATE_DESC -> routes.sortedByDescending { it.basicData.timeStartWork ?: 0L }
            SortOption.WORKTIME_ASC -> routes.sortedBy { workTime(it) }
            SortOption.WORKTIME_DESC -> routes.sortedByDescending { workTime(it) }
        }
    }

    private fun workTime(route: Route): Long {
        val start = route.basicData.timeStartWork ?: return 0L
        val end = route.basicData.timeEndWork ?: return 0L
        return if (end > start) end - start else 0L
    }
}
