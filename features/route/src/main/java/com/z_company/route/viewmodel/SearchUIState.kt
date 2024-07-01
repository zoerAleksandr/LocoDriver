package com.z_company.route.viewmodel

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.FilterSearch
import com.z_company.domain.entities.RouteWithTag
import com.z_company.domain.entities.SearchStateScreen
import com.z_company.domain.entities.SearchTag

data class SearchUIState(
    val searchState: SearchStateScreen<List<RouteWithTag>?> = SearchStateScreen.Success(null),
    val preliminarySearch: Boolean = false,
    val searchFilter: FilterSearch = FilterSearch(),
    val isVisibleHistory: Boolean = false,
    val isVisibleHints: Boolean = false,
    val isVisibleResult: Boolean = false,
    val hints: List<String> = mutableListOf("Подсказка1", "Подсказка", "Текст", "Подсказка4"),
    val searchResultList: List<RouteWithTag> = mutableListOf(),
    val searchHistoryList: List<String> = mutableListOf("История1", "История2"),
)