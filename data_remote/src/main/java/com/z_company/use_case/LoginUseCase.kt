package com.z_company.use_case

import com.parse.ParseUser
import com.z_company.core.ResultState
import kotlinx.coroutines.flow.Flow
import com.z_company.domain.entities.User
import com.z_company.work_manager.UserFieldName
import kotlinx.coroutines.flow.flow

class LoginUseCase {
    suspend fun getUser(): Flow<ResultState<User>> {
        return flow {
            emit(ResultState.Loading)
            val parseUser = ParseUser.getCurrentUser().fetch()
            val isVerification = parseUser.getBoolean(UserFieldName.EMAIL_VERIFIED_FIELD_NAME_REMOTE)
            val user = User(
                name = parseUser.username,
                email = parseUser.email,
                updateAt = parseUser.updatedAt.time,
                isVerification = isVerification
            )
            emit(ResultState.Success(user))
        }
    }
}