package com.z_company.repository.remote_rest.response

import com.z_company.repository.remote_rest.UserRemote
import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val user: UserRemote,
)
