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
import com.z_company.repository.remote_rest.response.UserWithRouteResponse

/**
 * Контракт для взаимодействия с REST API.
 * Реализация: KtorRemoteRestApi.
 * Методы возвращают результат напрямую (без обёртки Response<T>).
 * При HTTP-ошибке бросается ResponseException из Ktor.
 */
interface RemoteRestApi {

    suspend fun authWithEmail(authRequest: AuthRequest): AuthResponse

    suspend fun registerUserByEmail(request: RegisteredRequestByEmail): LoginResponse

    suspend fun registerUserByVKID(request: RegisteredRequestByVKID): LoginResponse

    suspend fun getUserProfile(token: String): UserWithRouteResponse

    suspend fun removeVKID(token: String): UserWithRouteResponse

    suspend fun saveRoute(token: String, data: Route)

    suspend fun getRoutes(token: String): List<Route>

    suspend fun attachVKID(token: String, data: AddVKIDRequest): UserWithRouteResponse

    suspend fun saveUserSetting(token: String, body: UserSettings): SuccessResponse

    suspend fun getUserSetting(token: String): UserSettings

    suspend fun saveSalarySetting(token: String, body: SalarySetting): SuccessResponse

    suspend fun getSalarySetting(token: String): SalarySetting

    suspend fun saveMonthOfYearList(token: String, body: List<MonthOfYear>): SuccessResponse

    suspend fun getMonthOfYearList(token: String): List<MonthOfYear>

    suspend fun updateEmail(token: String, data: UpdateEmailRequest): SuccessResponse

    suspend fun addEmailToUser(token: String, body: AddEmailRequest): SuccessResponse
}
