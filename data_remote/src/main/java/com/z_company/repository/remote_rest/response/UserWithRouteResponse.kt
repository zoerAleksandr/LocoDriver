package com.z_company.repository.remote_rest.response

import com.z_company.domain.entities.route.Route
import com.z_company.repository.remote_rest.UserRemote
import kotlinx.serialization.Serializable

@Serializable
data class UserWithRouteResponse(
    val user: UserRemote,
    val routes: List<Route>
)
