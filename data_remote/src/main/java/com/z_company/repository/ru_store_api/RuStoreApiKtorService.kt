package com.z_company.repository.ru_store_api

import com.z_company.repository.ru_store_api.DTO.JWEAnswerDTO
import com.z_company.repository.ru_store_api.DTO.SubscriptionAnswerDTO
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

class RuStoreApiKtorService(private val ktor: HttpClient) {
    suspend fun createJWEToken(response: String): Result<JWEAnswerDTO> = runCatching {
        ktor.post("public/auth") {
            contentType(ContentType.Application.Json)
            setBody(response)
        }.body()
    }
}

interface RetrofitServices {
    @GET("public/v3/subscription/{packageName}/{subscriptionId}/{subscriptionToken}")
    fun getDetails(
        @Header("Public-Token") jweToken: String,
        @Path("packageName") packageName: String,
        @Path("subscriptionId") subscriptionId: String,
        @Path("subscriptionToken") subscriptionToken: String,
    ): Call<SubscriptionAnswerDTO>

}

