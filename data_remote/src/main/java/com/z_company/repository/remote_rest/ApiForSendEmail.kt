package com.z_company.repository.remote_rest

import retrofit2.Response
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiForSendEmail {
    @Headers("Content-Type: application/json")
    @POST("v1/page/forgot_password")
    suspend fun forgotPassword(
        @Query("email")
        email: String
    ): Response<String>
}