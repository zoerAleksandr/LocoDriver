package com.z_company.route.viewmodel

import com.z_company.domain.entities.route.Route
import com.z_company.route.ui.FilterSearch

data class SearchUIState(
    val preliminarySearch: Boolean = false,
    val searchFilter: FilterSearch = FilterSearch(),
    val isVisibleHistory: Boolean = false,
    val isVisibleHints: Boolean = false,
    val isVisibleResult: Boolean = false,
    val hints: List<String> = mutableListOf(),
    val searchResultList: List<Route> = listOf(),
    val searchHistoryList: List<String> = listOf(),
)