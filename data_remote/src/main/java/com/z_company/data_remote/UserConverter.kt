package com.z_company.data_remote

import io.appwrite.models.User as UserAppwrite
import com.z_company.domain.entities.User
internal object UserConverter {
    fun toData(userApprite: UserAppwrite<Map<String, Any>>): User {
        return User(
            name = userApprite.name,
            phone = userApprite.phone,
            email = userApprite.email
        )
    }
}