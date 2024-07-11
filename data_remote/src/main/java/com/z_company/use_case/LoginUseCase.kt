package com.z_company.use_case

import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import com.parse.coroutines.suspendFind
import com.z_company.core.ResultState
import kotlinx.coroutines.flow.Flow
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.domain.entities.User
import com.z_company.work_manager.UserFieldName
import kotlinx.coroutines.flow.flow

class LoginUseCase {
    suspend fun getUser(): Flow<ResultState<User>> {
        return flow {
            emit(ResultState.Loading)
            val parseUser = ParseUser.getCurrentUser()
            var user = User(
                name = parseUser.username,
                email = parseUser.email,
                updateAt = parseUser.updatedAt.time,
            )
            val parseQuery: ParseQuery<ParseObject> =
                ParseQuery(UserFieldName.USER_CLASS_NAME_REMOTE)
            parseQuery.whereEqualTo(UserFieldName.USERNAME_FIELD_NAME_REMOTE, parseUser.username)
            val parseObject = parseQuery.suspendFind().first()
            parseObject.let {
                user = user.copy(
                    isAuthenticated = it.getBoolean(UserFieldName.EMAIL_VERIFIED_FIELD_NAME_REMOTE)
                )
            }
            emit(ResultState.Success(user))
        }
    }
}