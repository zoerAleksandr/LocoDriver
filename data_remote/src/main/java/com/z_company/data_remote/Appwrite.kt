package com.z_company.data_remote

import android.content.Context
import androidx.activity.ComponentActivity
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.models.*
import io.appwrite.services.*

object Appwrite {
    lateinit var client: Client
    lateinit var account: Account

    const val PROJECT_ID = "65f31ed3a05b46ec15df"
    const val ENDPOINT = "https://cloud.appwrite.io/v1"

    fun init(context: Context) {
        client = Client(context)
            .setEndpoint(ENDPOINT)
            .setProject(PROJECT_ID)

        account = Account(client)
    }

    suspend fun requestingSMSCode(number: String): String {
        val sessionToken = account.createPhoneSession(
            userId = ID.unique(),
            phone = number
        );

        return sessionToken.userId;
    }

    suspend fun loginWithPhone(userId: String, secret: String): Session {
        return account.updatePhoneSession(
            userId = userId,
            secret = secret
        )
    }

    suspend fun getSession(): Session{
       return account.getSession("current")
    }

    suspend fun onLogout() {
        account.deleteSession("current")
    }
}
