package com.z_company.route.viewmodel

import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear

data class MoreInfoUiState(
    val currentMonthOfYearState: ResultState<MonthOfYear?> = ResultState.Loading
)
