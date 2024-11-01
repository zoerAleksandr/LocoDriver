package com.z_company.repository

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import java.security.Timestamp

interface RuStoreApi {
    @Headers("Content-Type: application/json")
    @POST("public/auth")
    fun getJWE(
        @Body signature: ResponseBody
    ): Call<JWEAnswerDTO>
}

data class ResponseBody(
    val keyId: String,
    val timestamp: String,
    val signature: String
)

data class JWEAnswerDTO(
    val code: String,
    val message: String,
    val body: BodyAnswerDTO,
    val timestamp: Timestamp
)

data class BodyAnswerDTO(
    val jwe: String,
    val ttl: Int
)