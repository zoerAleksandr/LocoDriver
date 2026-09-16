package com.z_company.repository.remote_rest.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ действия, которое отозвало все токены пользователя (сейчас —
 * `PATCH /v1/auth/vkId/remove`). Сервер инкрементирует `user.token_version`,
 * прежний bearer перестаёт приниматься, и в `access_token` приходит новый —
 * его надо сохранить до следующего запроса. `null` — отзыва не было
 * (VK и так не был привязан) либо сервер ещё старый.
 */
@Serializable
data class RevokeSessionsResponse(
    @SerialName("access_token")
    val accessToken: String? = null,
)
