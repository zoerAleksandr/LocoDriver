package com.z_company.use_case

import com.parse.ParseUser
import com.z_company.core.ResultState
import kotlinx.coroutines.flow.Flow
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.domain.entities.User

class LoginUseCase {
    fun getUser(): Flow<ResultState<User>> {
        val user = ParseUser.getCurrentUser()
        return flowRequest {
            User(name = user.username, email = user.email)
        }
    }
}