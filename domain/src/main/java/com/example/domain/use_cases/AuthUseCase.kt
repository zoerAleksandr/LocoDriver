package com.example.domain.use_cases

import com.example.core.ErrorEntity
import com.example.core.ResultState
import com.example.core.auth.Auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AuthUseCase {
    private val auth = object : Auth<Unit, Unit>() {
        override fun isLoggedIn(): Boolean {
            return true
        }

        override fun signOut() {
            TODO("Not yet implemented")
        }

        override fun signIn(onSuccess: () -> Unit, onError: (t: Throwable?) -> Unit, input: Unit?) {
            TODO("Not yet implemented")
        }
    }
    fun login(): Flow<ResultState<Unit>> {
        return callbackFlow {
            trySend(ResultState.Loading)
            auth.signIn(
                onSuccess = {
                    trySend(ResultState.Success(Unit))
                    close()
                },
                onError = {
                    trySend(ResultState.Error(ErrorEntity(it)))
                    close()
                }
            )
            awaitClose()
        }
    }

    fun logout() {
        auth.signOut()
    }
}