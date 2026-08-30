package com.z_company.repository.remote_rest.request

import kotlinx.serialization.Serializable

/**
 * Тело `POST /v1/auth`.
 *
 * Для `methodAuth = "vkId"` начиная с 3.0.4 личность подтверждает
 * [vkAccessToken]: сервер сам меняет его на VK user id через
 * `id.vk.com/oauth2/user_info` и присланному клиентом id больше не верит.
 *
 * [auth_param] при VK-входе новый сервер игнорирует, но мы продолжаем класть
 * туда vk id — пока бэкенд с проверкой токена не раскатан, старый прод
 * авторизует именно по нему (порядок релиза: сначала Android, потом сервер).
 *
 * Поля опциональные: при `explicitNulls = false` null'ы в тело не попадают,
 * поэтому вход по email уходит на сервер ровно в прежнем виде.
 */
@Serializable
data class AuthRequest(
    val password: String,
    val methodAuth: String,
    val auth_param: String,
    val vkAccessToken: String? = null,
    val vkClientId: String? = null
)
