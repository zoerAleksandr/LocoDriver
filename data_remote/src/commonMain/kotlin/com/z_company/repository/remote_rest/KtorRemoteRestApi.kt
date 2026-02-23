package com.z_company.repository.remote_rest

import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.repository.remote_rest.request.AddEmailRequest
import com.z_company.repository.remote_rest.request.AddVKIDRequest
import com.z_company.repository.remote_rest.request.AuthRequest
import com.z_company.repository.remote_rest.request.RegisteredRequestByEmail
import com.z_company.repository.remote_rest.request.RegisteredRequestByVKID
import com.z_company.repository.remote_rest.request.UpdateEmailRequest
import com.z_company.repository.remote_rest.response.AuthResponse
import com.z_company.repository.remote_rest.response.LoginResponse
import com.z_company.repository.remote_rest.response.SuccessResponse
import com.z_company.repository.remote_rest.response.UserResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Реализация RemoteRestApi через Ktor.
 * Заменяет Retrofit-интерфейс.
 */
class KtorRemoteRestApi(private val client: HttpClient) : RemoteRestApi {

    override suspend fun authWithEmail(authRequest: AuthRequest): AuthResponse =
        client.post("v1/auth") {
            contentType(ContentType.Application.Json)
            setBody(authRequest)
        }.body()

    override suspend fun registerUserByEmail(request: RegisteredRequestByEmail): LoginResponse =
        client.post("v1/auth/create") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun registerUserByVKID(request: RegisteredRequestByVKID): LoginResponse =
        client.post("v1/auth/create") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun getUserProfile(token: String): UserResponse =
        client.get("v1/auth") {
            header("Authorization", token)
        }.body()

    override suspend fun removeVKID(token: String): UserResponse =
        client.patch("v1/auth/vkId/remove") {
            header("Authorization", token)
        }.body()

    override suspend fun saveRoute(token: String, data: Route) {
        client.post("v1/route") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(data)
        }
    }

    override suspend fun getRoutes(token: String): List<Route> =
        client.get("v1/route") {
            header("Authorization", token)
        }.body()

    override suspend fun attachVKID(token: String, data: AddVKIDRequest): UserResponse =
        client.patch("v1/auth/vkId/add") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(data)
        }.body()

    override suspend fun saveUserSetting(token: String, body: UserSettings): SuccessResponse =
        client.post("v1/user_settings") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(body)
        }.body()

    override suspend fun getUserSetting(token: String): UserSettings =
        client.get("v1/user_settings") {
            header("Authorization", token)
        }.body()

    override suspend fun saveSalarySetting(token: String, body: SalarySetting): SuccessResponse =
        client.post("v1/salary_settings") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(body)
        }.body()

    override suspend fun getSalarySetting(token: String): SalarySetting =
        client.get("v1/salary_settings") {
            header("Authorization", token)
        }.body()

    override suspend fun saveMonthOfYearList(token: String, body: List<MonthOfYear>): SuccessResponse =
        client.post("v1/year/") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(body)
        }.body()

    override suspend fun getMonthOfYearList(token: String): List<MonthOfYear> =
        client.get("v1/year/") {
            header("Authorization", token)
        }.body()

    override suspend fun updateEmail(token: String, data: UpdateEmailRequest): SuccessResponse =
        client.patch("v1/auth/email/update") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(data)
        }.body()

    override suspend fun addEmailToUser(token: String, body: AddEmailRequest): SuccessResponse =
        client.patch("v1/auth/email/add") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(body)
        }.body()
}
