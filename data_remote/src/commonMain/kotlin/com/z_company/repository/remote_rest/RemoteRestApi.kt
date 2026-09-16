package com.z_company.repository.remote_rest

import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ProductionCalendarDay
import com.z_company.domain.entities.ReleaseDay
import com.z_company.domain.entities.WorkScheduleProfile
import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.norma_time.StationNorm
import com.z_company.domain.entities.partner.Partner
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.repository.remote_rest.request.AddEmailRequest
import com.z_company.repository.remote_rest.request.AddVKIDRequest
import com.z_company.repository.remote_rest.request.AuthRequest
import com.z_company.repository.remote_rest.request.CkassaCheckoutRequest
import com.z_company.repository.remote_rest.request.AnnouncementSeenRequest
import com.z_company.repository.remote_rest.request.ClientInfoRequest
import com.z_company.repository.remote_rest.request.RegisteredRequestByEmail
import com.z_company.repository.remote_rest.request.RegisteredRequestByVKID
import com.z_company.repository.remote_rest.request.UpdateEmailRequest
import com.z_company.repository.remote_rest.response.AnnouncementResponse
import com.z_company.repository.remote_rest.response.CkassaCheckoutResponse
import com.z_company.repository.remote_rest.response.TariffsResponse
import com.z_company.repository.remote_rest.response.AuthResponse
import com.z_company.repository.remote_rest.response.RevokeSessionsResponse
import com.z_company.repository.remote_rest.response.LoginResponse
import com.z_company.repository.remote_rest.response.RouteDeltaResponse
import com.z_company.repository.remote_rest.response.SaveRouteResponse
import com.z_company.repository.remote_rest.response.ShareRouteResponse
import com.z_company.repository.remote_rest.response.UserResponse
import com.z_company.repository.remote_rest.diagnostic.DiagnosticEventsRequest
import com.z_company.repository.remote_rest.diagnostic.DiagnosticEventsResponse

/**
 * Контракт для взаимодействия с REST API.
 * Реализация: KtorRemoteRestApi.
 * Методы возвращают результат напрямую (без обёртки Response<T>).
 * При HTTP-ошибке бросается ResponseException из Ktor.
 */
interface RemoteRestApi {

    /** Сохраняет актуальную версию текущей платформы в профиле пользователя. */
    suspend fun saveClientInfo(token: String, body: ClientInfoRequest)

    suspend fun sendDiagnosticEvents(body: DiagnosticEventsRequest): DiagnosticEventsResponse

    suspend fun authWithEmail(authRequest: AuthRequest): AuthResponse

    suspend fun registerUserByEmail(request: RegisteredRequestByEmail): LoginResponse

    suspend fun registerUserByVKID(request: RegisteredRequestByVKID): LoginResponse

    suspend fun getUserProfile(token: String): UserResponse

    /**
     * Продлить сессию: `POST /v1/auth/refresh` по текущему bearer-токену
     * (сервер принимает и просроченный в пределах своего грейса) отдаёт
     * новый. См. [SessionRefresher].
     */
    suspend fun refreshToken(token: String): AuthResponse

    /**
     * Отвязать VK. Сервер отзывает все токены пользователя и отдаёт новый в
     * `access_token` ([RevokeSessionsResponse]) — прежний bearer после этого
     * не принимается. Профиль берём отдельным getUserProfile уже с новым.
     */
    suspend fun removeVKID(token: String): RevokeSessionsResponse

    suspend fun saveRoute(token: String, data: Route): SaveRouteResponse

    suspend fun getRoutes(token: String): List<Route>

    /**
     * Одна страница изменений маршрутов, `GET /v1/route/delta`.
     *
     * Отдельный эндпоинт, а не параметр к [getRoutes]: полный список обязан
     * остаться полным. Сборка, которая получит неполный список из [getRoutes],
     * решит, что маршрутов нет на сервере, и пометит локальные на удаление.
     *
     * @param cursor граница прошлой синхронизации; null — сервер отдаёт всё с нуля.
     */
    suspend fun getRouteDelta(token: String, cursor: String?, limit: Int?): RouteDeltaResponse

    suspend fun deleteRoute(token: String, routeId: String)

    /** Привязать VK. Ответ — SuccessResponse, см. [removeVKID]. */
    suspend fun attachVKID(token: String, data: AddVKIDRequest)

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

    // --- WorkScheduleProfile (персональный график, отдельный безопасный ресурс) ---

    suspend fun saveWorkScheduleProfile(token: String, body: WorkScheduleProfile): WorkScheduleProfile

    suspend fun getWorkScheduleProfile(token: String): WorkScheduleProfile

    // --- NormaTime (нормы времени приёмки/сдачи) ---

    /** Сохранить серии локомотивов на сервере (полная замена) */
    suspend fun saveNormaTimeLocomotives(token: String, body: List<LocomotiveSeries>)

    /** Получить серии локомотивов с сервера */
    suspend fun getNormaTimeLocomotives(token: String): List<LocomotiveSeries>

    /** Сохранить станции с нормами на сервере (полная замена) */
    suspend fun saveNormaTimeStations(token: String, body: List<StationNorm>)

    /** Получить станции с нормами с сервера */
    suspend fun getNormaTimeStations(token: String): List<StationNorm>

    // --- Partners (справочник напарников) ---

    /** Сохранить справочник напарников на сервере (полная замена) */
    suspend fun savePartners(token: String, body: List<Partner>)

    /** Получить справочник напарников с сервера */
    suspend fun getPartners(token: String): List<Partner>

    // --- Announcements (сообщения при запуске) ---

    /**
     * Актуальное сообщение-«новость при запуске» для платформы/версии (без авторизации).
     * Возвращает null, если показывать нечего (сервер ответил 204).
     * `type` ("news" | "update") — только сообщения этого типа; null — любой.
     */
    suspend fun getLatestAnnouncement(platform: String, build: Long, type: String? = null): AnnouncementResponse?

    /** Отметить просмотр сообщения установкой (`POST /v1/announcements/{number}/seen`, без авторизации). */
    suspend fun postAnnouncementSeen(number: Int, body: AnnouncementSeenRequest)

    // --- Tariffs (тарифы подписки: цены и скидки) ---

    /**
     * Активные тарифы подписки со скидками (без авторизации, `GET /v1/tariffs`).
     * Единый источник цен для приложения и сайта.
     */
    suspend fun getTariffs(): TariffsResponse

    // --- CKassa (новая платёжная система, работает параллельно с Robokassa) ---

    /**
     * Создать инвойс CKassa и получить ссылку оплаты
     * (`POST /v1/payment/ckassa/checkout`, требует токен).
     *
     * Цена/срок берутся на сервере по коду тарифа (со скидками из кабинета).
     * После оплаты сервер продлевает подписку по S2S-callback от CKassa —
     * клиент об оплате серверу не сообщает, а поллит статус подписки.
     */
    suspend fun createCkassaCheckout(
        token: String,
        request: CkassaCheckoutRequest,
    ): CkassaCheckoutResponse

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
