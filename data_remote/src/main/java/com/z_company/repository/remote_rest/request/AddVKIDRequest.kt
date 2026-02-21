package com.z_company.repository.remote_rest.request

import kotlinx.serialization.Serializable

@Serializable
data class AddVKIDRequest(
    val token: String
)
