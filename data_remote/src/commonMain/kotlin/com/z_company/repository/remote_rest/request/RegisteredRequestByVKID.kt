package com.z_company.repository.remote_rest.request

import kotlinx.serialization.Serializable

/**
 * Тело `POST /v1/auth/create` для регистрации через VK ID.
 *
 * [vkId] — легаси: новый бэкенд пишет в `user.vk_id` id из ответа VK на
 * [vkAccessToken], а присланный id игнорирует. Оставлен ради старого прода,
 * который выкатывается уже после публикации 3.0.4.
 */
@Serializable
data class RegisteredRequestByVKID(
    val login: String,
    val email: String,
    val password: String,
    val vkId: String,
    val vkAccessToken: String? = null,
    val vkClientId: String? = null
)
