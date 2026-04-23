package com.z_company.repository.remote_rest

import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ProductionCalendarDay
import com.z_company.domain.entities.ReleaseDay
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
import com.z_company.repository.remote_rest.response.SaveRouteResponse
import com.z_company.repository.remote_rest.response.ShareRouteResponse
import com.z_company.repository.remote_rest.response.UserResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.parameter
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

    override suspend fun saveRoute(token: String, data: Route): SaveRouteResponse {
        val responseText = client.post("v1/route/") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(data)
        }.bodyAsText()
        return try {
            RemoteRestClient.appJson.decodeFromString<SaveRouteResponse>(responseText)
        } catch (_: Exception) {
            SaveRouteResponse(message = responseText)
        }
    }

    override suspend fun getRoutes(token: String): List<Route> =
        client.get("v1/route/") {
            header("Authorization", token)
        }.body()

    override suspend fun deleteRoute(token: String, routeId: String) {
        // Сервер регистрирует endpoint как `/v1/route/{route_id}` БЕЗ trailing slash
        // (см. backend: APIRouter(prefix="/route") + @router.delete("/{route_id}")).
        // Раньше здесь был слэш — FastAPI отвечал 307, а Ktor HttpRedirect не
        // следует редиректам для DELETE, запрос до БД не доходил.
        client.delete("v1/route/$routeId") {
            header("Authorization", token)
        }
    }

    override suspend fun attachVKID(token: String, data: AddVKIDRequest): UserResponse =
        client.patch("v1/auth/vkId/add") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(data)
        }.body()

    override suspend fun saveUserSetting(token: String, body: UserSettings) {
        client.post("v1/user_settings/") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(body)
        }.bodyAsText()
    }

    override suspend fun getUserSetting(token: String): UserSettings =
        client.get("v1/user_settings/") {
            header("Authorization", token)
        }.body()

    override suspend fun saveSalarySetting(token: String, body: SalarySetting) {
        client.post("v1/salary_settings/") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(body)
        }.bodyAsText()
    }

    override suspend fun getSalarySetting(token: String): SalarySetting =
        client.get("v1/salary_settings/") {
            header("Authorization", token)
        }.body()

    override suspend fun saveMonthOfYearList(token: String, body: List<MonthOfYear>) {
        client.post("v1/year/") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(body)
        }.bodyAsText()
    }

    override suspend fun getMonthOfYearList(token: String): List<MonthOfYear> =
        client.get("v1/year/") {
            header("Authorization", token)
        }.body()

    override suspend fun updateEmail(token: String, data: UpdateEmailRequest) {
        client.patch("v1/auth/email/update") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(data)
        }.bodyAsText()
    }

    override suspend fun addEmailToUser(token: String, body: AddEmailRequest) {
        client.patch("v1/auth/email/add") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(body)
        }.bodyAsText()
    }

    override suspend fun saveReleaseDays(token: String, body: List<ReleaseDay>) {
        client.post("v1/release_days/") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(body)
        }.bodyAsText()
    }

    override suspend fun getReleaseDays(token: String): List<ReleaseDay> =
        client.get("v1/release_days/") {
            header("Authorization", token)
        }.body()

    override suspend fun getProductionCalendar(country: String, year: Int): List<ProductionCalendarDay> =
        client.get("v1/production_calendar/") {
            parameter("country", country)
            parameter("year", year)
        }.body()

    override suspend fun getRegions(country: String): List<com.z_company.domain.entities.calendar.Region> =
        client.get("v1/regions/") {
            parameter("country", country)
        }.body()

    override suspend fun getRegionalHolidays(
        region: String,
        year: Int,
    ): List<com.z_company.domain.entities.calendar.RegionalHoliday> =
        client.get("v1/regional_holidays/") {
            parameter("region", region)
            parameter("year", year)
        }.body()

    override suspend fun createSharedRoute(token: String, data: Route): ShareRouteResponse =
        client.post("v1/share/route") {
            contentType(ContentType.Application.Json)
            header("Authorization", token)
            setBody(data)
        }.body()

    override suspend fun getSharedRoute(shareId: String): Route =
        client.get("v1/share/route/$shareId").body()
}
