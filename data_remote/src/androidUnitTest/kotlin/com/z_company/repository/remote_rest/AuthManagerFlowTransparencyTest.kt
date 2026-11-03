package com.z_company.repository.remote_rest

import com.z_company.repository.remote_rest.response.AuthResponse
import com.z_company.repository.remote_rest.response.UserResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Регресс: потоки AuthManager отправляли результат (emit) внутри try, а
 * `catch (e: Exception)` перехватывал исключения подписчика — `first { }`
 * останавливает поток AbortFlowException, catch превращал её в Error и снова
 * вызывал emit → «Flow exception transparency is violated» и падение при входе
 * (AccountSwitcher.fetchUserId внутри collect у authWithEmail).
 */
class AuthManagerFlowTransparencyTest {

    /** RemoteRestApi, в котором реализованы только нужные тесту методы. */
    private fun api(vararg answers: Pair<String, Any>): RemoteRestApi {
        val byName = answers.toMap()
        return Proxy.newProxyInstance(
            RemoteRestApi::class.java.classLoader,
            arrayOf(RemoteRestApi::class.java),
        ) { _, method, _ ->
            byName[method.name] ?: throw UnsupportedOperationException(method.name)
        } as RemoteRestApi
    }

    private val emailApi = Proxy.newProxyInstance(
        ApiForSendEmail::class.java.classLoader,
        arrayOf(ApiForSendEmail::class.java),
    ) { _, method, _ -> throw UnsupportedOperationException(method.name) } as ApiForSendEmail

    @Test
    fun `first по профилю возвращает Success без нарушения прозрачности`() = runBlocking<Unit> {
        val manager = AuthManager(api("getUserProfile" to UserResponse(UserRemote(id = "u1"))), emailApi)

        val state = manager.getUserProfile("Bearer t").first { it !is GetUserProfileState.Loading }

        assertIs<GetUserProfileState.Success>(state)
        assertEquals("u1", state.user.id)
    }

    @Test
    fun `вход с first по профилю внутри collect не падает`() = runBlocking<Unit> {
        val manager = AuthManager(
            api(
                "authWithEmail" to AuthResponse(accessToken = "tok"),
                "getUserProfile" to UserResponse(UserRemote(id = "u1")),
            ),
            emailApi,
        )
        val states = mutableListOf<AuthState>()
        var userId: String? = null

        // Как ProfileViewModel.authWithEmail → logInAs → AccountSwitcher.fetchUserId.
        manager.authWithEmail("a@b.c", "p").collect { state ->
            if (state is AuthState.Success) {
                userId = (manager.getUserProfile("Bearer ${state.accessToken}")
                    .first { it !is GetUserProfileState.Loading } as GetUserProfileState.Success).user.id
            }
            states += state
        }

        assertEquals("u1", userId)
        assertEquals(listOf(AuthState.Loading::class, AuthState.Success::class), states.map { it::class })
    }

    @Test
    fun `ошибка сети по-прежнему превращается в Error`() = runBlocking<Unit> {
        val manager = AuthManager(api(), emailApi) // getUserProfile не реализован → исключение

        val state = manager.getUserProfile("Bearer t").first { it !is GetUserProfileState.Loading }

        assertIs<GetUserProfileState.Error>(state)
    }
}
