package com.z_company.repository.remote_rest.request

import kotlinx.serialization.Serializable

/**
 * Тело `PATCH /v1/auth/vkId/add`.
 *
 * [token] — легаси vk id, оставлен ради серверов без проверки токена.
 * Новый бэкенд берёт vk_id из [vkAccessToken] и [token] игнорирует.
 */
@Serializable
data class AddVKIDRequest(
    val token: String? = null,
    val vkAccessToken: String? = null,
    val vkClientId: String? = null
)
