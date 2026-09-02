package com.z_company.repository.remote_rest

import com.z_company.domain.entities.route.Route
import com.z_company.repository.remote_rest.request.AddEmailRequest
import com.z_company.repository.remote_rest.request.AddVKIDRequest
import com.z_company.repository.remote_rest.request.AuthRequest
import com.z_company.repository.remote_rest.request.RegisteredRequestByEmail
import com.z_company.repository.remote_rest.request.RegisteredRequestByVKID
import com.z_company.repository.remote_rest.request.UpdateEmailRequest
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Менеджер аутентификации.
 * Шаг 2 KMP-миграции: object → class с инжекцией зависимостей через Koin.
 */
class AuthManager(
    private val remoteRestApi: RemoteRestApi,
    private val apiForSendEmail: ApiForSendEmail,
    /**
     * client_id приложения из secret.properties (`VKIDClientID`).
     * Нужен серверу, чтобы проверить VK access token у нужного приложения;
     * без него сервер переберёт allowlist — сработает, но лишним запросом к VK.
     */
    private val vkClientId: String? = null
) {

    fun registerByEmail(
        email: String,
        password: String
    ): Flow<RegistrationState> = flow {
        emit(RegistrationState.Loading)
        try {
            val request = RegisteredRequestByEmail(
                login = email,
                password = password,
                email = email,
            )
            val body = remoteRestApi.registerUserByEmail(request)
            emit(
                RegistrationState.Success(
                    accessToken = body.accessToken,
                    tokenType = body.tokenType
                )
            )
        } catch (e: ClientRequestException) {
            emit(RegistrationState.Error("Ошибка: ${e.response.status.value}", code = e.response.status.value))
        } catch (e: ServerResponseException) {
            emit(RegistrationState.Error("Ошибка сервера: ${e.response.status.value}", code = e.response.status.value))
        } catch (e: Exception) {
            emit(RegistrationState.Error("Ошибка: ${e.message}"))
        }
    }

    /**
     * Регистрация через VK ID.
     *
     * @param vkId легаси-id, уходит в `login` и `vkId`; новый сервер берёт
     *   vk_id из ответа VK на [vkAccessToken], а присланный id игнорирует.
     * @param vkAccessToken токен из VKID SDK. Нигде не сохраняется.
     */
    fun registerByVKID(
        vkId: String,
        vkAccessToken: String,
        email: String
    ): Flow<RegistrationState> = flow {
        emit(RegistrationState.Loading)
        try {
            val request = RegisteredRequestByVKID(
                login = vkId,
                vkId = vkId,
                password = "",
                email = email,
                vkAccessToken = vkAccessToken,
                vkClientId = vkClientId
            )
            val body = remoteRestApi.registerUserByVKID(request)
            emit(
                RegistrationState.Success(
                    accessToken = body.accessToken,
                    tokenType = body.tokenType
                )
            )
        } catch (e: ClientRequestException) {
            emit(RegistrationState.Error("Ошибка: ${e.response.status.value}", code = e.response.status.value))
        } catch (e: ServerResponseException) {
            emit(RegistrationState.Error("Ошибка сервера: ${e.response.status.value}", code = e.response.status.value))
        } catch (e: Exception) {
            emit(RegistrationState.Error("Ошибка: ${e.message}"))
        }
    }

    fun authWithEmail(email: String, password: String): Flow<AuthState> = flow {
        emit(AuthState.Loading)
        try {
            val authRequest = AuthRequest(
                auth_param = email,
                password = password,
                methodAuth = "email",
            )
            val body = remoteRestApi.authWithEmail(authRequest)
            emit(
                AuthState.Success(
                    accessToken = body.accessToken,
                    tokenType = body.tokenType
                )
            )
        } catch (e: ClientRequestException) {
            val text = when (e.response.status.value) {
                401 -> "Неверная почта или пароль"
                else -> "Ошибка: ${e.response.status.value} - ${e.message}"
            }
            emit(AuthState.Error(text))
        } catch (e: Exception) {
            emit(AuthState.Error("Ошибка: ${e.message}"))
        }
    }

    /**
     * Вход по VK ID.
     *
     * @param vkId легаси-идентификатор, уходит в `auth_param`. Новый сервер
     *   его игнорирует, но старый прод (бэкенд выкатывается уже после
     *   публикации 3.0.4) авторизует именно по нему.
     * @param vkAccessToken токен из VKID SDK — единственное, чему верит сервер
     *   с проверкой. Нигде не сохраняется, в логи и Sentry не попадает.
     *
     * Ошибки разложены по [VkAuthError]: «аккаунта нет» — это отдельное
     * состояние (флоу регистрации), а не «неверная почта или пароль».
     */
    fun authWithVKID(vkId: String, vkAccessToken: String): Flow<AuthState> = flow {
        emit(AuthState.Loading)
        try {
            val authRequest = AuthRequest(
                auth_param = vkId,
                password = "",
                methodAuth = "vkId",
                vkAccessToken = vkAccessToken,
                vkClientId = vkClientId,
            )
            val body = remoteRestApi.authWithEmail(authRequest)
            emit(
                AuthState.Success(
                    accessToken = body.accessToken,
                    tokenType = body.tokenType
                )
            )
        } catch (e: ClientRequestException) {
            emit(vkAuthError(e.response.status.value, detailOf(e.response)))
        } catch (e: ServerResponseException) {
            emit(vkAuthError(e.response.status.value, detailOf(e.response)))
        } catch (e: Exception) {
            emit(AuthState.Error("Ошибка: ${e.message}"))
        }
    }


    /**
     * Отвязка VK от аккаунта.
     *
     * `PATCH /v1/auth/vkId/remove` отдаёт SuccessResponse, а не пользователя,
     * поэтому обновлённый профиль дочитываем отдельным запросом. Раньше клиент
     * пытался разобрать ответ как UserResponse, падал на этом и всегда уходил
     * в Error: сервер отвязывал VK, а локальный vk_id так и оставался.
     */
    fun removeVKID(token: String): Flow<GetUserProfileState> = flow {
        emit(GetUserProfileState.Loading)
        try {
            remoteRestApi.removeVKID(token = token)
            val body = remoteRestApi.getUserProfile(token = token)
            emit(
                GetUserProfileState.Success(
                    user = body.user,
                )
            )
        } catch (e: ClientRequestException) {
            emit(GetUserProfileState.Error("Ошибка: ${e.message}", code = e.response.status.value))
        } catch (e: Exception) {
            emit(GetUserProfileState.Error("Ошибка: ${e.message}"))
        }
    }

    /**
     * Привязка VK ID к текущему аккаунту.
     *
     * @param vkId легаси-поле `token`, оставлено ради ещё не обновлённого прода.
     * @param vkAccessToken токен из VKID SDK: именно из него новый сервер
     *   берёт vk_id. Нигде не сохраняется.
     *
     * Ответ сервера — SuccessResponse, а не пользователь, поэтому профиль
     * дочитываем отдельным запросом (см. [removeVKID]).
     */
    fun attachVKID(
        bearerToken: String,
        vkId: String,
        vkAccessToken: String
    ): Flow<GetUserProfileState> = flow {
        emit(GetUserProfileState.Loading)
        try {
            val addVKIDRequest = AddVKIDRequest(
                token = vkId,
                vkAccessToken = vkAccessToken,
                vkClientId = vkClientId,
            )
            remoteRestApi.attachVKID(token = bearerToken, data = addVKIDRequest)
            val body = remoteRestApi.getUserProfile(token = bearerToken)
            emit(
                GetUserProfileState.Success(
                    user = body.user,
                )
            )
        } catch (e: ClientRequestException) {
            val code = e.response.status.value
            emit(GetUserProfileState.Error(attachErrorMessage(code, detailOf(e.response)), code = code))
        } catch (e: ServerResponseException) {
            val code = e.response.status.value
            emit(GetUserProfileState.Error(attachErrorMessage(code, detailOf(e.response)), code = code))
        } catch (e: Exception) {
            emit(GetUserProfileState.Error("Ошибка: ${e.message}"))
        }
    }

    /**
     * Достаёт строковый `detail` из тела ошибки FastAPI.
     * null — тела нет, оно не JSON или detail не строка (422-валидация).
     */
    private suspend fun detailOf(response: HttpResponse): String? = try {
        val body = response.bodyAsText()
        if (body.isBlank()) null
        else errorJson.parseToJsonElement(body).jsonObject["detail"]?.jsonPrimitive?.content
    } catch (_: Exception) {
        null
    }

    fun getUserProfile(token: String): Flow<GetUserProfileState> = flow {
        emit(GetUserProfileState.Loading)
        try {
            val body = remoteRestApi.getUserProfile(token = token)
            emit(
                GetUserProfileState.Success(
                    user = body.user,
                )
            )
        } catch (e: ClientRequestException) {
            emit(GetUserProfileState.Error(message = "Ошибка: ${e.message}", code = e.response.status.value))
        } catch (e: Exception) {
            emit(GetUserProfileState.Error("Ошибка: ${e.message}"))
        }
    }

    /**
     * Запуск сброса пароля.
     *
     * Контракт сервера (harden/password-reset): ответ намеренно не раскрывает,
     * зарегистрирован ли email — при 200 письмо отправляется только если аккаунт
     * существует, но клиент этого различить НЕ может (защита от перебора).
     *  - 200 → [ForgotPasswordState.Success] (нейтральное «проверьте почту»).
     *  - 429 → [ForgotPasswordState.RateLimited] (превышен лимит запросов).
     *  - прочее → [ForgotPasswordState.Error] (сеть/сервер).
     *
     * Тело ответа НЕ парсим — ориентируемся только на статус-код, т.к. текст
     * на сервере может меняться.
     */
    fun forgotPassword(email: String): Flow<ForgotPasswordState> = flow {
        emit(ForgotPasswordState.Loading)
        try {
            apiForSendEmail.forgotPassword(email)
            emit(ForgotPasswordState.Success)
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.TooManyRequests) {
                emit(ForgotPasswordState.RateLimited)
            } else {
                emit(ForgotPasswordState.Error("Не удалось отправить запрос. Попробуйте позже."))
            }
        } catch (e: ServerResponseException) {
            emit(ForgotPasswordState.Error("Сервер временно недоступен. Попробуйте позже."))
        } catch (e: Exception) {
            emit(ForgotPasswordState.Error("Не удалось отправить запрос. Проверьте подключение к интернету."))
        }
    }

    /**
     * Привязка почты и пароля к аккаунту, у которого их ещё нет
     * (`PATCH /v1/auth/email/add`). Нужна аккаунтам, заведённым через VK:
     * пароль у них — пустая строка, и VK остаётся единственным способом
     * войти. Если почта уже привязана, сервер отвечает 200 и ничего не
     * меняет — менять её нужно через [updateEmail].
     */
    fun addEmail(token: String, email: String, password: String): Flow<ResponseState> = flow {
        emit(ResponseState.Loading)
        try {
            val request = AddEmailRequest(email = email, password = password)
            remoteRestApi.addEmailToUser(token = token, body = request)
            emit(ResponseState.Success)
        } catch (e: ClientRequestException) {
            emit(
                ResponseState.Error(
                    addEmailErrorMessage(e.response.status.value, detailOf(e.response))
                )
            )
        } catch (e: ServerResponseException) {
            emit(
                ResponseState.Error(
                    addEmailErrorMessage(e.response.status.value, detailOf(e.response))
                )
            )
        } catch (e: Exception) {
            emit(ResponseState.Error("Ошибка: ${e.message}"))
        }
    }

    fun updateEmail(token: String, email: String): Flow<ResponseState> = flow {
        emit(ResponseState.Loading)
        delay(2000L)
        try {
            val request = UpdateEmailRequest(email)
            remoteRestApi.updateEmail(token = token, data = request)
            emit(ResponseState.Success)
        } catch (e: ClientRequestException) {
            emit(ResponseState.Error("Ошибка: ${e.response.status.value} - ${e.message}"))
        } catch (e: Exception) {
            emit(ResponseState.Error("Ошибка: ${e.message}"))
        }
    }

    private companion object {
        val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}

// Коды `detail` из ответов бэкенда с проверкой VK-токена
// (см. таблицу в VKID_AUTH_3.0.4_TASK.md).
internal const val VK_DETAIL_TOKEN_INVALID = "vk_token_invalid"
internal const val VK_DETAIL_TOKEN_REQUIRED = "vk_access_token_required"
internal const val VK_DETAIL_USER_NOT_FOUND = "vk_user_not_found"
internal const val VK_DETAIL_UNAVAILABLE = "vk_unavailable"
internal const val VK_DETAIL_ALREADY_LINKED = "vk_id_already_linked"

/** Раскладывает ответ `POST /v1/auth` c `methodAuth = "vkId"` по состояниям UI. */
internal fun vkAuthError(statusCode: Int, detail: String?): AuthState.Error = when {
    detail == VK_DETAIL_TOKEN_INVALID -> AuthState.Error(
        "Не удалось подтвердить вход через VK ID. Попробуйте ещё раз.",
        VkAuthError.TokenInvalid
    )

    detail == VK_DETAIL_TOKEN_REQUIRED -> AuthState.Error(
        "Обновите приложение: этот способ входа через VK ID больше не поддерживается.",
        VkAuthError.ClientOutdated
    )

    detail == VK_DETAIL_UNAVAILABLE || statusCode == 503 -> AuthState.Error(
        "VK ID временно недоступен, попробуйте позже.",
        VkAuthError.VkUnavailable
    )

    // 404 — сервер с проверкой токена; 401 без известного detail — ещё не
    // обновлённый прод, где «аккаунта с таким VK нет» приходило как 401.
    detail == VK_DETAIL_USER_NOT_FOUND || statusCode == 404 || statusCode == 401 -> AuthState.Error(
        "Аккаунта с этим VK ID нет. Зарегистрируйтесь.",
        VkAuthError.UserNotFound
    )

    else -> AuthState.Error("Ошибка: $statusCode")
}

/**
 * Текст для пользователя по ответу `PATCH /v1/auth/email/add`.
 * 400 — сервер не принял пароль, 409 — почта занята другим аккаунтом.
 */
internal fun addEmailErrorMessage(statusCode: Int, detail: String?): String = when {
    statusCode == 400 -> "Не удалось сохранить пароль. Проверьте, что он заполнен."
    statusCode == 409 -> "Эта почта уже занята другим аккаунтом «Машиниста»."
    statusCode == 401 || statusCode == 403 -> "Сессия устарела. Войдите в аккаунт заново."
    statusCode == 404 -> "Аккаунт не найден."
    statusCode >= 500 -> "Сервер временно недоступен. Попробуйте позже."
    detail != null -> detail
    else -> "Не удалось привязать почту. Ошибка $statusCode."
}

/** Текст для пользователя по ответу `PATCH /v1/auth/vkId/add`. */
internal fun attachErrorMessage(statusCode: Int, detail: String?): String = when {
    detail == VK_DETAIL_ALREADY_LINKED || statusCode == 409 ->
        "Этот VK ID уже привязан к другому аккаунту «Машиниста»."

    detail == VK_DETAIL_TOKEN_INVALID ->
        "Не удалось подтвердить вход через VK ID. Попробуйте ещё раз."

    detail == VK_DETAIL_TOKEN_REQUIRED ->
        "Обновите приложение: привязка VK ID изменилась."

    detail == VK_DETAIL_UNAVAILABLE || statusCode == 503 ->
        "VK ID временно недоступен, попробуйте позже."

    else -> "Не удалось привязать VK ID (ошибка $statusCode)."
}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Success(val accessToken: String, val tokenType: String? = null) : AuthState()

    /**
     * [vkError] заполняется только для входа через VK ID — по нему UI решает,
     * показать повтор, предложить регистрацию или попросить обновить приложение.
     */
    data class Error(val errorMessage: String, val vkError: VkAuthError? = null) : AuthState()
}

/** Разбор ошибок входа через VK ID (см. таблицу кодов в VKID_AUTH_3.0.4_TASK.md). */
enum class VkAuthError {
    /** 401 vk_token_invalid — VK не подтвердил токен. */
    TokenInvalid,

    /** 401 vk_access_token_required — на сервере выключен переходный флаг. */
    ClientOutdated,

    /** 404 vk_user_not_found — аккаунта с этим VK нет, нужна регистрация. */
    UserNotFound,

    /** 503 vk_unavailable — VK ID временно недоступен. */
    VkUnavailable
}

sealed class RegistrationState {
    object Initial : RegistrationState()
    object Loading : RegistrationState()
    data class Success(val accessToken: String, val tokenType: String? = null) : RegistrationState()
    data class Error(val message: String, val code: Int = 0) : RegistrationState()
}

sealed class GetUserProfileState {
    object Initial : GetUserProfileState()
    object Loading : GetUserProfileState()
    data class Success(val user: UserRemote) : GetUserProfileState()
    data class Error(val message: String, val code: Int = 0) : GetUserProfileState()
}

sealed class ResponseState {
    object Initial : ResponseState()
    object Loading : ResponseState()
    object Success : ResponseState()
    data class Error(val errorMessage: String) : ResponseState()
}

/**
 * Состояние запроса на сброс пароля (POST /v1/page/forgot_password).
 * [Success] — успех по статус-коду 200 (нейтральный ответ, письмо могло не
 * прийти, если аккаунта нет — клиент это не различает).
 * [RateLimited] — сервер вернул 429 (превышен лимит).
 */
sealed class ForgotPasswordState {
    object Initial : ForgotPasswordState()
    object Loading : ForgotPasswordState()
    object Success : ForgotPasswordState()
    object RateLimited : ForgotPasswordState()
    data class Error(val errorMessage: String) : ForgotPasswordState()
}