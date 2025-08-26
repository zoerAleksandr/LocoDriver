package com.z_company.repository.ru_store_api.DTO

import com.google.gson.annotations.SerializedName

data class JWEAnswerDTO(
    val code: String,
    val message: String,
    val body: BodyAnswerDTO,
    val timestamp: String
)

data class BodyAnswerDTO(
    val jwe: String,
    val ttl: Int
)

data class BodyResponse(
    @SerializedName("keyId")val keyId: String,
    @SerializedName("timestamp")val timestamp: String,
    @SerializedName("signature")val signature: String
)
