package com.z_company.repository.remote_rest.request

import kotlinx.serialization.Serializable

@Serializable
data class RegisteredRequestByVKID(
    val login: String,
    val email: String,
    val password: String,
    val vkId: String
)
