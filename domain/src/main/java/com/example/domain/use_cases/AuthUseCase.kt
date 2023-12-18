package com.example.domain.use_cases

import com.example.core.ErrorEntity
import com.example.core.ResultState
import com.example.core.auth.Auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AuthUseCase(
    private val auth: Auth<*, *>
) {
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