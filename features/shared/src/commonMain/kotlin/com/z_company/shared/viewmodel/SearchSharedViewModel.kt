package com.z_company.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.route.Route
import com.z_company.domain.use_cases.RouteUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Shared ViewModel for the search screen.
 *
 * Loads all routes from repository, then filters them locally by
 * route number or notes when the query changes.
 */
@OptIn(FlowPreview::class)
class SearchSharedViewModel(
    private val routeUseCase: RouteUseCase,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<Route>>(emptyList())
    val results: StateFlow<List<Route>> = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _allRoutes = MutableStateFlow<List<Route>>(emptyList())

    private var loadJob: Job? = null
    private var searchJob: Job? = null

    init {
        loadAllRoutes()
        observeQueryChanges()
    }

    private fun loadAllRoutes() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            routeUseCase.getListRoutesAsFlow().collect { routes ->
                _allRoutes.value = routes.sortedByDescending { it.basicData.timeStartWork ?: 0L }
                applyFilter(_query.value)
            }
        }
    }

    private fun observeQueryChanges() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _query
                .debounce(200)
                .distinctUntilChanged()
                .collect { q -> applyFilter(q) }
        }
    }

    private fun applyFilter(q: String) {
        val trimmed = q.trim()
        _isLoading.value = true
        _results.value = if (trimmed.isBlank()) {
            emptyList()
        } else {
            val lower = trimmed.lowercase()
            _allRoutes.value.filter { route ->
                val number = route.basicData.number?.lowercase() ?: ""
                val notes = route.basicData.notes?.lowercase() ?: ""
                number.contains(lower) || notes.contains(lower)
            }
        }
        _isLoading.value = false
    }

    fun search(query: String) {
        _query.value = query
    }

    fun clearSearch() {
        _query.value = ""
        _results.value = emptyList()
    }
}
