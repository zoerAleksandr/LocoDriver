package com.z_company.repository.remote_rest.request

import kotlinx.serialization.Serializable

/**
 * Тело `POST /v1/announcements/{number}/seen` — установка увидела сообщение.
 * `installationId` — анонимный id установки (тот же, что в диагностике).
 */
@Serializable
data class AnnouncementSeenRequest(
    val platform: String,
    val installationId: String,
)
