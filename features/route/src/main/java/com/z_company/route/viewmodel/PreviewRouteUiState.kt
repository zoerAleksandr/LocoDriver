package com.z_company.route.viewmodel

import com.z_company.core.ResultState

data class PreviewRouteUiState(
    val homeRestState: ResultState<Long?> = ResultState.Loading
)