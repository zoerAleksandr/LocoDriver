package com.z_company.repository.remote_rest.response

import kotlinx.serialization.Serializable

/**
 * Ответ сервера при сохранении маршрута.
 * Сервер может вернуть warnings (невалидные значения записаны как NULL).
 */
@Serializable
data class SaveRouteResponse(
    val message: String? = null,
    val warnings: List<String> = emptyList()
)
