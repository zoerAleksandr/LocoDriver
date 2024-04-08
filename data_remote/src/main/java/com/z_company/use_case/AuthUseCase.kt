package com.z_company.use_case

import com.parse.ParseUser
import com.parse.coroutines.parseLogIn
import com.parse.coroutines.suspendSignUp
import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_remote.UserConverter
import com.z_company.domain.entities.User
import kotlinx.coroutines.flow.Flow

class AuthUseCase {

    fun loginWithEmail(email: String, password: String): Flow<ResultState<User>> {
        return flowRequest {
            UserConverter.toData(parseLogIn(email, password))
        }
    }

    suspend fun registeredUserByEmail(
        name: String,
        password: String,
        email: String
    ): Flow<ResultState<User>> {
        return flowRequest {
            val user = ParseUser().apply {
                username = name
                setPassword(password)
                setEmail(email)
            }.also {
                it.suspendSignUp()
            }
            UserConverter.toData(user)
        }
    }

    suspend fun logout() {

    }
}