package com.z_company.repository.remote_rest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserRemote(
    val id: String = "",
    val email: String = "",
    @SerialName("vk_id")
    val vkId: String = "",
)
