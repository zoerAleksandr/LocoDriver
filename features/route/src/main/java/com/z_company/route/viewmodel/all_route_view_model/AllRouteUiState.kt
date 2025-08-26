package com.z_company.route.viewmodel.all_route_view_model

import com.z_company.route.viewmodel.home_view_model.ItemState

data class AllRouteUiState(
    val listItemState: MutableList<ItemState> = mutableListOf<ItemState>(),
    val isLoading: Boolean = true,
    val routes: List<ItemState> = emptyList(),
    val filteredRoutes: List<ItemState> = emptyList(),
    val selectedFilters: Set<RouteFilter> = setOf(RouteFilter.ALL),
    val errorMessage: String? = null
)