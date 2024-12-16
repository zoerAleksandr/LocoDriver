package com.z_company.repository.ru_store_api

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.gson.gson
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RuStoreRepositoryKtor {
    private val BASE_URL = "https://public-api.rustore.ru/"
    val ktor = HttpClient(OkHttp) {
        expectSuccess = true

        engine {
            addInterceptor(HttpLoggingInterceptor().apply {
                setLevel(
                    HttpLoggingInterceptor.Level.BODY
                )
            })
        }

        defaultRequest {
            url(BASE_URL)
        }

        install(ContentNegotiation) {
            gson()
        }
    }

    val service = RuStoreApiKtorService(ktor)


}

object RetrofitClient {
    private var retrofit: Retrofit? = null

    fun getClient(): Retrofit {
        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("https://public-api.rustore.ru/")
                .build()
        }
        return retrofit!!
    }
}