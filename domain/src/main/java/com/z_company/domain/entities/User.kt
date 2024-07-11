package com.z_company.domain.entities

data class User(
    val name: String,
    val email: String,
    val updateAt: Long,
    val isAuthenticated: Boolean = false
)
