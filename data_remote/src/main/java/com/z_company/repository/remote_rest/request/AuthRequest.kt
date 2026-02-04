package com.z_company.repository.remote_rest.request

data class AuthRequest(
    val password: String,
    val methodAuth: String,
    val auth_param: String
)