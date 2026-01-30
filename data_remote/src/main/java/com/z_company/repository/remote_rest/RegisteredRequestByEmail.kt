package com.z_company.repository.remote_rest

data class RegisteredRequestByEmail(
    val login: String,
    val email: String,
    val password: String,
//    val vkId: String
)