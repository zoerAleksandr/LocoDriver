package com.z_company.repository.remote_rest.response

import kotlinx.serialization.Serializable

/**
 * Ответ сервера при создании публичной ссылки на маршрут.
 *
 * Эндпоинт: POST /v1/share/route — принимает Route, возвращает короткий [id].
 * Ссылка формируется на клиенте: locodriver://share/{id}.
 */
@Serializable
data class ShareRouteResponse(
    val id: String,
    val expiresAt: Long? = null
)
