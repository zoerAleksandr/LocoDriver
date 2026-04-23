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

    suspend fun getUserProfile(token: String): UserResponse

    suspend fun removeVKID(token: String): UserResponse

    suspend fun saveRoute(token: String, data: Route): SaveRouteResponse

    suspend fun getRoutes(token: String): List<Route>

    suspend fun deleteRoute(token: String, routeId: String)

    suspend fun attachVKID(token: String, data: AddVKIDRequest): UserResponse

    suspend fun saveUserSetting(token: String, body: UserSettings)

    suspend fun getUserSetting(token: String): UserSettings

    suspend fun saveSalarySetting(token: String, body: SalarySetting)

    suspend fun getSalarySetting(token: String): SalarySetting

    suspend fun saveMonthOfYearList(token: String, body: List<MonthOfYear>)

    suspend fun getMonthOfYearList(token: String): List<MonthOfYear>

    suspend fun updateEmail(token: String, data: UpdateEmailRequest)

    suspend fun addEmailToUser(token: String, body: AddEmailRequest)

    // --- ReleaseDay (отвлечения пользователя) ---

    /** Сохранить список дней отвлечений на сервере (полная замена) */
    suspend fun saveReleaseDays(token: String, body: List<ReleaseDay>)

    /** Получить список дней отвлечений с сервера */
    suspend fun getReleaseDays(token: String): List<ReleaseDay>

    // --- ProductionCalendar (производственный календарь) ---

    /** Получить производственный календарь для страны и года (без авторизации) */
    suspend fun getProductionCalendar(country: String, year: Int): List<ProductionCalendarDay>

    // --- Regional Holidays (региональные праздники субъектов) ---

    /**
     * Получить список регионов (субъектов) для страны.
     * Эндпоинт: GET /v1/regions/?country=RU — без авторизации.
     * Ответ: список [Region] с ISO 3166-2 кодами.
     */
    suspend fun getRegions(country: String): List<com.z_company.domain.entities.calendar.Region>

    /**
     * Получить региональные праздники для конкретного региона на год.
     * Эндпоинт: GET /v1/regional_holidays/?region=RU-TA&year=2026 — без авторизации.
     * Ответ: список [RegionalHoliday] накладываемых поверх стандартного календаря.
     */
    suspend fun getRegionalHolidays(
        region: String,
        year: Int,
    ): List<com.z_company.domain.entities.calendar.RegionalHoliday>

    // --- Shared Routes (публичные ссылки на маршруты) ---

    /**
     * Создаёт публичную ссылку на маршрут.
     * Эндпоинт: POST /v1/share/route — принимает Route, возвращает [ShareRouteResponse.id].
     * Требует авторизации.
     */
    suspend fun createSharedRoute(token: String, data: Route): ShareRouteResponse

    /**
     * Получает маршрут по короткому идентификатору публичной ссылки.
     * Эндпоинт: GET /v1/share/route/{shareId} — возвращает Route целиком.
     * Не требует авторизации: получатель может не иметь аккаунта.
     */
    suspend fun getSharedRoute(shareId: String): Route
}
