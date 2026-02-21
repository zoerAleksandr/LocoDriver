package com.z_company.repository.remote_rest

import kotlinx.serialization.Serializable

@Serializable
data class UserRemote(
    val id: String = "",
    val login: String = "",
    val email: String = "",
)
