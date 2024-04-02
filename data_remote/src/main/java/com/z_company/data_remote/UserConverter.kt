package com.z_company.data_remote

import com.parse.ParseUser
import com.z_company.domain.entities.User
internal object UserConverter {
    fun toData(parseUser: ParseUser): User {
        return User(
            name = parseUser.username,
            email = parseUser.email
        )
    }
}