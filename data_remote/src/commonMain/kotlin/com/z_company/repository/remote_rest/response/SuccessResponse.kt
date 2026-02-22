package com.z_company.repository.remote_rest.response

import kotlinx.serialization.Serializable

@Serializable
data class SuccessResponse(
    val statusCode: Int,
    val content: String
)
