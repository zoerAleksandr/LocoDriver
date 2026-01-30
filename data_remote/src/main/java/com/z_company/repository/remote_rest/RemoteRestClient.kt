package com.z_company.repository.remote_rest

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import android.util.Log
import com.google.gson.GsonBuilder
import com.z_company.domain.entities.ReleaseType
import okhttp3.ResponseBody
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RemoteRestClient {
    private const val BASE_URL = "http://87.228.110.32:8766/"
    private const val BASE_URL_FOR_SEND_EMAIL = "http://locodrivers.freemyip.com/"
    private const val API_KEY = "test-key"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor { chain ->  // Добавь кастомный для raw JSON
            val response = chain.proceed(chain.request())
            val rawJson = response.body?.string()  // Читай тело как строку
            // Верни response с новым body (чтобы не сломать)
            response.newBuilder()
                .body(ResponseBody.create(response.body?.contentType(), rawJson ?: ""))
                .build()
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val gson = GsonBuilder()
        .registerTypeAdapter(Date::class.java, DateDeserializer())
        .registerTypeAdapter(ReleaseType::class.java, ReleaseTypeAdapter())
        .create()

    val remoteRestApi: RemoteRestApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(RemoteRestApi::class.java)
    }

    val apiForSendEmail: ApiForSendEmail by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_FOR_SEND_EMAIL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiForSendEmail::class.java)
    }
}

class DateDeserializer : JsonDeserializer<Date> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Date {
        val dateStr = json?.asString ?: return Date()  // Возвращаем текущую дату, если значение null

        val format = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US)
        try {
            return format.parse(dateStr) ?: Date()  // Если парсинг неудачный, возвращаем текущую дату
        } catch (e: Exception) {
            Log.e("DateDeserializer", "Failed to parse date: $dateStr", e)
            return Date()  // Возвращаем текущую дату в случае ошибки
        }
    }
}