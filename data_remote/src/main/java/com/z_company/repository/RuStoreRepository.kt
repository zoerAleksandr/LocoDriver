package com.z_company.repository

import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RuStoreRepository {
    private val ruStoreApi = Retrofit.Builder()
        .baseUrl("https://public-api.rustore.ru/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RuStoreApi::class.java)

    fun getJWE(keyId: String, timestamp: String, signature: String, callback: Callback<JWEAnswerDTO>){
        ruStoreApi.getJWE(signature = ResponseBody(keyId, timestamp, signature)).enqueue(callback)
    }

}