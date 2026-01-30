package com.z_company.repository.remote_rest

import com.z_company.domain.entities.route.Route

data class UserWithRouteResponse(
    val user: UserRemote,
    val routes: List<Route>
)
