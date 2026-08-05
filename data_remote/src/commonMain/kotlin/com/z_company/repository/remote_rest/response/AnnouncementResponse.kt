package com.z_company.repository.remote_rest.response

import kotlinx.serialization.Serializable

/**
 * DTO ответа `GET /v1/announcements/latest`.
 * Соответствует `AnnouncementResponse` на сервере.
 */
@Serializable
data class AnnouncementResponse(
    val number: Int,
    val title: String,
    val body: String,
    val platform: String = "all",
    val minBuild: Int? = null,
    val maxBuild: Int? = null,
    val isActive: Boolean = true,
)
