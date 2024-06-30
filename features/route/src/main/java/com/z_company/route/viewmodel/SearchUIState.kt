package com.z_company.route.viewmodel

import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.route.ui.FilterSearch
import com.z_company.route.ui.SearchTag

data class SearchUIState(
    val preliminarySearch: Boolean = false,
    val searchFilter: FilterSearch = FilterSearch(),
    val isVisibleHistory: Boolean = true,
    val isVisibleHints: Boolean = true,
    val isVisibleResult: Boolean = true,
    val hints: List<String> = mutableListOf("Подсказка1", "Подсказка", "Текст", "Подсказка4"),
    val searchResultList: List<Pair<Route, SearchTag>> = mutableListOf(
        Pair(first = Route(BasicData(timeStartWork = 1719608243139)), second = SearchTag.LOCO),
        Pair(first = Route(BasicData(timeStartWork = 1719608343139)), second = SearchTag.BASIC_DATA),
    ),
    val searchHistoryList: List<String> = mutableListOf("История1", "История2"),
)