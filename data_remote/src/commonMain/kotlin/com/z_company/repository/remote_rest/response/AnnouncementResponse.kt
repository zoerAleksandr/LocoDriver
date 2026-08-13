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
    // 'once' | 'always'. Дефолт нужен для совместимости со старым сервером
    // (поля ещё нет в ответе) — тогда сообщение показывается один раз.
    val displayMode: String = "once",
    // 'news' | 'update'. Дефолт для совместимости со старым сервером — новость.
    val type: String = "news",
    // Для type == 'update': список фич карусели (иначе пусто).
    val features: List<AnnouncementFeatureResponse> = emptyList(),
)

/**
 * DTO фичи обновления. `imageUrl` — относительный путь на эндпоинт с байтами
 * картинки (`/v1/announcements/feature-image/{id}`) либо null.
 */
@Serializable
data class AnnouncementFeatureResponse(
    val position: Int = 0,
    val title: String,
    val description: String,
    val imageUrl: String? = null,
)
