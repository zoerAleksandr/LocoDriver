package com.z_company.repository.remote_rest.request

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val password: String,
    val methodAuth: String,
    val auth_param: String
)
