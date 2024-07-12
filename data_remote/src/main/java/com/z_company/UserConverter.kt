package com.z_company

import com.parse.ParseUser
import com.z_company.domain.entities.User
import com.z_company.work_manager.UserFieldName

internal object UserConverter {
    fun toData(parseUser: ParseUser): User {
        return User(
            name = parseUser.username,
            email = parseUser.email,
            updateAt = parseUser.updatedAt.time,
            isVerification = parseUser.getBoolean(UserFieldName.EMAIL_VERIFIED_FIELD_NAME_REMOTE)
        )
    }
}