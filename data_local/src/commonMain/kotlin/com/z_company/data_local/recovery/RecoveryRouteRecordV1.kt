package com.z_company.data_local.recovery

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
data class RecoveryRouteRecordV1(
    val routeId: String,
    val tables: Map<String, List<RecoveryRowV1>>,
)

@Serializable
data class RecoveryRowV1(
    val values: Map<String, JsonElement>,
)

object RecoveryRouteRecordJson {
    private val json = Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = true
    }

    fun encode(record: RecoveryRouteRecordV1): String =
        json.encodeToString(RecoveryRouteRecordV1.serializer(), record)

    fun decode(line: String): RecoveryRouteRecordV1 =
        json.decodeFromString(RecoveryRouteRecordV1.serializer(), line)
}
