package com.z_company.repository.remote_rest

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Контракт для отправки email (восстановление пароля).
 */
interface ApiForSendEmail {
    suspend fun forgotPassword(email: String)
}

/**
 * Ktor-реализация ApiForSendEmail.
 */
class KtorApiForSendEmail(private val client: HttpClient) : ApiForSendEmail {
    override suspend fun forgotPassword(email: String) {
        client.post("v1/page/forgot_password") {
            contentType(ContentType.Application.Json)
            parameter("email", email)
        }
    }
}
