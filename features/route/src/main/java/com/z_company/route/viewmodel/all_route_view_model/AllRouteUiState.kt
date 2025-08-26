package com.z_company.route.viewmodel.all_route_view_model

import com.z_company.core.ResultState
import com.z_company.route.viewmodel.home_view_model.ItemState

data class AllRouteUiState(
    val uiState: ResultState<Unit> = ResultState.Loading(),
    val listItemState: MutableList<ItemState> = mutableListOf<ItemState>(),
)