package com.z_company.data_remote

import com.parse.ParseUser
import com.parse.coroutines.parseLogIn
import com.parse.coroutines.suspendSignUp
import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.domain.entities.User
import kotlinx.coroutines.flow.Flow

class AuthUseCase {

    fun loginWithEmail(email: String, password: String): Flow<ResultState<User>> {
        return flowRequest {
            UserConverter.toData(parseLogIn(email, password))
        }
    }

    suspend fun registeredUserByEmail(){
        ParseUser().apply {
            setUsername("my name")
            setPassword("my pass")
            setEmail("email@example.com")
        }.also {
            it.suspendSignUp()
        }
    }

    suspend fun logout() {

    }
}