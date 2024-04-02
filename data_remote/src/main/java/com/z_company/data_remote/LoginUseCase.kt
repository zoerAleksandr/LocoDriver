package com.z_company.data_remote

import com.z_company.core.ResultState
import kotlinx.coroutines.flow.Flow
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.domain.entities.User

class LoginUseCase {
    fun getUser(): Flow<ResultState<User>> {
        return flowRequest {
            User(name = "default name", email = "")
        }
    }
}