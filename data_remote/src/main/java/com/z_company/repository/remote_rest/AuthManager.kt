package com.z_company.repository.remote_rest

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.User
import com.z_company.domain.entities.route.Route
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import retrofit2.HttpException
import java.io.IOException

object AuthManager : KoinComponent {
    private val remoteRestApi: RemoteRestApi by inject()
    private val apiForSendEmail: ApiForSendEmail by inject()


    fun registerByEmail(
        email: String,
        password: String
    ): Flow<RegistrationState> =
        flow {  // Пояснение: flow — builder для создания Flow, внутри которого можно emit значения асинхронно.
            emit(RegistrationState.Loading)  // Сначала эмитим состояние загрузки

            try {
                val request = RegisteredRequestByEmail(
                    login = email,
                    password = password,
                    email = email,
                )

                // Асинхронный вызов Retrofit (suspend)
                val response = remoteRestApi.registerUserByEmail(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    emit(
                        RegistrationState.Success(  // Эмитим успех
                            accessToken = body?.accessToken ?: "",
                            tokenType = body?.tokenType
                        )
                    )
                } else {
                    emit(RegistrationState.Error("Ошибка: ${response.code()} - ${response.message()}"))  // Эмитим ошибку HTTP
                }
            } catch (e: IOException) {
                emit(RegistrationState.Error("Нет соединения: ${e.message}"))  // Ошибка сети
            } catch (e: HttpException) {
                emit(RegistrationState.Error("HTTP ошибка: ${e.message}"))  // HTTP-исключение
            } catch (e: Exception) {
                emit(RegistrationState.Error("Неизвестная ошибка: ${e.message}"))  // Другие ошибки
            }
        }

    fun registerByVKID(
        vkId: String,
    ): Flow<RegistrationState> =
        flow {  // Пояснение: flow — builder для создания Flow, внутри которого можно emit значения асинхронно.
            emit(RegistrationState.Loading)  // Сначала эмитим состояние загрузки

            try {
                val request = RegisteredRequestByVKID(
                    login = vkId,
                    vkId = vkId,
                    password = "",
                    email = ""
                )

                // Асинхронный вызов Retrofit (suspend)
                val response = remoteRestApi.registerUserByVKID(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    emit(
                        RegistrationState.Success(  // Эмитим успех
                            accessToken = body?.accessToken ?: "",
                            tokenType = body?.tokenType
                        )
                    )
                } else {
                    emit(RegistrationState.Error("Ошибка: ${response.code()} - ${response.message()}"))  // Эмитим ошибку HTTP
                }
            } catch (e: IOException) {
                emit(RegistrationState.Error("Нет соединения: ${e.message}"))  // Ошибка сети
            } catch (e: HttpException) {
                emit(RegistrationState.Error("HTTP ошибка: ${e.message}"))  // HTTP-исключение
            } catch (e: Exception) {
                emit(RegistrationState.Error("Неизвестная ошибка: ${e.message}"))  // Другие ошибки
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

            val response = remoteRestApi.authWithEmail(authRequest)
            if (response.isSuccessful) {
                val body = response.body()
                emit(
                    AuthState.Success(
                        accessToken = body?.accessToken ?: "",
                        tokenType = body?.tokenType
                    )
                )
            } else {
                val text = when (response.code()) {
                    401 -> "Неверная почта или пароль"
                    else -> "Ошибка: $response ${response.code()} - ${response.message()}"
                }
                emit(AuthState.Error(text))  // Эмитим ошибку HTTP
            }
        } catch (e: IOException) {
            emit(AuthState.Error("Нет соединения: ${e.message}"))  // Ошибка сети
        } catch (e: HttpException) {
            emit(AuthState.Error("HTTP ошибка: ${e.message}"))  // HTTP-исключение
        } catch (e: Exception) {
            emit(AuthState.Error("Неизвестная ошибка: ${e.message}"))  // Другие ошибки
        }
    }

    fun removeVKID(token: String): Flow<GetUserProfileState> = flow {
        emit(GetUserProfileState.Loading)
        try {
            val response = remoteRestApi.removeVKID(
                token = token
            )
            val body = response.body()
            emit(
                GetUserProfileState.Success(
                    user = body?.user ?: UserRemote(login = "Неизвестный пользователь"),
                    routes = body?.routes ?: emptyList()
                )
            )
        } catch (e: IOException) {
            emit(GetUserProfileState.Error("Нет соединения: ${e.message}"))  // Ошибка сети
        } catch (e: HttpException) {
            emit(GetUserProfileState.Error("HTTP ошибка: ${e.message}"))  // HTTP-исключение
        } catch (e: Exception) {
            emit(GetUserProfileState.Error("Неизвестная ошибка: ${e.message}"))  // Другие ошибки
        }
    }

    fun attachVKID(bearerToken: String, vkId: String): Flow<GetUserProfileState> = flow {
        emit(GetUserProfileState.Loading)
        try {
            val response = remoteRestApi.attachVKID(
                token = bearerToken,
                vkId = vkId
            )
            if (response.isSuccessful) {
                val body = response.body()
                emit(
                    GetUserProfileState.Success(  // Эмитим успех
                        user = body?.user ?: UserRemote(login = "Неизвестный пользователь"),
                        routes = body?.routes ?: emptyList()
                    )
                )
            } else {
                emit(GetUserProfileState.Error("Ошибка: ${response.code()} - ${response.message()}"))  // Эмитим ошибку HTTP
            }
        } catch (e: IOException) {
            emit(GetUserProfileState.Error("Нет соединения: ${e.message}"))  // Ошибка сети
        } catch (e: HttpException) {
            emit(GetUserProfileState.Error("HTTP ошибка: ${e.message}"))// HTTP-исключение
        } catch (e: Exception) {
            emit(GetUserProfileState.Error("Неизвестная ошибка: ${e.message}"))  // Другие ошибки
        }
    }

    fun getUserProfile(token: String): Flow<GetUserProfileState> = flow {
        emit(GetUserProfileState.Loading)
        try {
            val response = remoteRestApi.getUserProfile(
                token = token
            )  // Асинхронный вызов Retrofit (suspend)

            if (response.isSuccessful) {
                val body = response.body()
                emit(
                    GetUserProfileState.Success(
                        user = body?.user ?: UserRemote(login = "Неизвестный пользователь"),
                        routes = body?.routes ?: emptyList()
                    )
                )
            } else {
                emit(GetUserProfileState.Error("Ошибка: ${response.code()} - ${response.message()}"))  // Эмитим ошибку HTTP
            }
        } catch (e: IOException) {
            emit(GetUserProfileState.Error("Нет соединения: ${e.message}"))  // Ошибка сети
        } catch (e: HttpException) {
            emit(GetUserProfileState.Error("HTTP ошибка: ${e.message}"))  // HTTP-исключение
        } catch (e: Exception) {
            emit(GetUserProfileState.Error("Неизвестная ошибка: ${e.message}"))  // Другие ошибки
        }
    }

    fun forgotPassword(email: String): Flow<ForgotEmailState> = flow {
        emit(ForgotEmailState.Loading)
        try {
            val response = apiForSendEmail.forgotPassword(email)
            if (response.isSuccessful) {
                emit(ForgotEmailState.Success)
            } else {
                emit(ForgotEmailState.Error("Ошибка: ${response.code()} - ${response.message()}"))  // Эмитим ошибку HTTP
            }
        } catch (e: IOException) {
            emit(ForgotEmailState.Error("Нет соединения: ${e.message}"))  // Ошибка сети
        } catch (e: HttpException) {
            emit(ForgotEmailState.Error("HTTP ошибка: ${e.message}")) // HTTP-исключение
        } catch (e: Exception) {
            emit(ForgotEmailState.Error("Неизвестная ошибка: ${e.message}"))  // Другие ошибки
        }
    }
}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()  // Состояние загрузки
    data class Success(val accessToken: String, val tokenType: String? = null) :
        AuthState()  // Успех с сообщением и опциональным токеном

    data class Error(val errorMessage: String) : AuthState()  // Ошибка с сообщением
}


sealed class RegistrationState {
    object Initial : RegistrationState()
    object Loading : RegistrationState()  // Состояние загрузки
    data class Success(val accessToken: String, val tokenType: String? = null) :
        RegistrationState()  // Успех с сообщением и опциональным токеном

    data class Error(val errorMessage: String) : RegistrationState()  // Ошибка с сообщением
}

sealed class GetUserProfileState {
    object Initial : GetUserProfileState()
    object Loading : GetUserProfileState()  // Состояние загрузки
    data class Success(val user: UserRemote, val routes: List<Route> = emptyList()) :
        GetUserProfileState()

    data class Error(val errorMessage: String) : GetUserProfileState()
}

sealed class ForgotEmailState {
    object Initial : ForgotEmailState()
    object Loading : ForgotEmailState()
    object Success : ForgotEmailState()
    data class Error(val errorMessage: String) : ForgotEmailState()
}