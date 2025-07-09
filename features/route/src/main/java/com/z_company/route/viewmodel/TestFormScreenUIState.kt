package com.z_company.route.viewmodel

import com.z_company.domain.entities.route.Route

data class TestFormScreenUIState(
    val isLoading: Boolean = true,
    val route: Route,
    val changesHaveState: Boolean = false
)