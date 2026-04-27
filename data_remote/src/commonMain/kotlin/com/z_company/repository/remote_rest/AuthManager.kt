package com.z_company.repository.remote_rest

import com.z_company.core.AppError
import com.z_company.domain.entities.route.Route
import com.z_company.repository.remote_rest.request.AddVKIDRequest
import com.z_company.repository.remote_rest.request.AuthRequest
import com.z_company.repository.remote_rest.request.RegisteredRequestByEmail
import com.z_company.repository.remote_rest.request.RegisteredRequestByVKID
import com.z_company.repository.remote_rest.request.UpdateEmailRequest
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Менеджер аутентификации.
 * Шаг 2 KMP-миграции: object → class с инжекцией зависимостей через Koin.
 */
class AuthManager(
    private val remoteRestApi: RemoteRestApi,
    private val apiForSendEmail: ApiForSendEmail
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
            emit(RegistrationState.Error("Ошибка: ${e.response.status.value}", code = e.response.status.value, appError = e.toAppError()))
        } catch (e: ServerResponseException) {
            emit(RegistrationState.Error("Ошибка сервера: ${e.response.status.value}", code = e.response.status.value, appError = e.toAppError()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(RegistrationState.Error("Ошибка: ${e.message}", appError = e.toAppError()))
        }
    }

    fun registerByVKID(
        vkId: String,
        email: String
    ): Flow<RegistrationState> = flow {
        emit(RegistrationState.Loading)
        try {
            val request = RegisteredRequestByVKID(
                login = vkId,
                vkId = vkId,
                password = "",
                email = email
            )
            val body = remoteRestApi.registerUserByVKID(request)
            emit(
                RegistrationState.Success(
                    accessToken = body.accessToken,
                    tokenType = body.tokenType
                )
            )
        } catch (e: ClientRequestException) {
            emit(RegistrationState.Error("Ошибка: ${e.response.status.value}", code = e.response.status.value, appError = e.toAppError()))
        } catch (e: ServerResponseException) {
            emit(RegistrationState.Error("Ошибка сервера: ${e.response.status.value}", code = e.response.status.value, appError = e.toAppError()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(RegistrationState.Error("Ошибка: ${e.message}", appError = e.toAppError()))
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
            emit(AuthState.Error(text, appError = e.toAppError()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(AuthState.Error("Ошибка: ${e.message}", appError = e.toAppError()))
        }
    }

    fun authWithVKID(vkId: String): Flow<AuthState> = flow {
        emit(AuthState.Loading)
        try {
            val authRequest = AuthRequest(
                auth_param = vkId,
                password = "",
                methodAuth = "vkId",
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
            emit(AuthState.Error(text, appError = e.toAppError()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(AuthState.Error("Ошибка: ${e.message}", appError = e.toAppError()))
        }
    }

    fun removeVKID(token: String): Flow<GetUserProfileState> = flow {
        emit(GetUserProfileState.Loading)
        try {
            val body = remoteRestApi.removeVKID(token = token)
            emit(
                GetUserProfileState.Success(
                    user = body.user,
                )
            )
        } catch (e: ClientRequestException) {
            emit(GetUserProfileState.Error("Ошибка: ${e.message}", code = e.response.status.value, appError = e.toAppError()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(GetUserProfileState.Error("Ошибка: ${e.message}", appError = e.toAppError()))
        }
    }

    fun attachVKID(bearerToken: String, vkId: String): Flow<GetUserProfileState> = flow {
        emit(GetUserProfileState.Loading)
        try {
            val addVKIDRequest = AddVKIDRequest(token = vkId)
            val body = remoteRestApi.attachVKID(token = bearerToken, data = addVKIDRequest)
            emit(
                GetUserProfileState.Success(
                    user = body.user,
                )
            )
        } catch (e: ClientRequestException) {
            emit(GetUserProfileState.Error("Ошибка: ${e.message}", code = e.response.status.value, appError = e.toAppError()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(GetUserProfileState.Error("Ошибка: ${e.message}", appError = e.toAppError()))
        }
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
            emit(GetUserProfileState.Error(message = "Ошибка: ${e.message}", code = e.response.status.value, appError = e.toAppError()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(GetUserProfileState.Error("Ошибка: ${e.message}", appError = e.toAppError()))
        }
    }

    fun forgotPassword(email: String): Flow<ResponseState> = flow {
        emit(ResponseState.Loading)
        try {
            apiForSendEmail.forgotPassword(email)
            emit(ResponseState.Success)
        } catch (e: ClientRequestException) {
            emit(ResponseState.Error("Ошибка: ${e.response.status.value} - ${e.message}", appError = e.toAppError()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(ResponseState.Error("Ошибка: ${e.message}", appError = e.toAppError()))
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
            emit(ResponseState.Error("Ошибка: ${e.response.status.value} - ${e.message}", appError = e.toAppError()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(ResponseState.Error("Ошибка: ${e.message}", appError = e.toAppError()))
        }
    }
}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Success(val accessToken: String, val tokenType: String? = null) : AuthState()
    data class Error(
        val errorMessage: String,
        val appError: AppError? = null,
    ) : AuthState()
}

sealed class RegistrationState {
    object Initial : RegistrationState()
    object Loading : RegistrationState()
    data class Success(val accessToken: String, val tokenType: String? = null) : RegistrationState()
    data class Error(
        val message: String,
        val code: Int = 0,
        val appError: AppError? = null,
    ) : RegistrationState()
}

sealed class GetUserProfileState {
    object Initial : GetUserProfileState()
    object Loading : GetUserProfileState()
    data class Success(val user: UserRemote) : GetUserProfileState()
    data class Error(
        val message: String,
        val code: Int = 0,
        val appError: AppError? = null,
    ) : GetUserProfileState()
}

sealed class ResponseState {
    object Initial : ResponseState()
    object Loading : ResponseState()
    object Success : ResponseState()
    data class Error(
        val errorMessage: String,
        val appError: AppError? = null,
    ) : ResponseState()
}
