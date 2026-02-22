package com.z_company.repository.remote_rest.request

import kotlinx.serialization.Serializable

@Serializable
data class AddEmailRequest(
    val email: String,
    val password: String
)
