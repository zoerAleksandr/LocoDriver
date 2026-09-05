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
    const val MAX_LINE_BYTES: Int = 2 * 1024 * 1024

    private val json = Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = true
    }

    fun encode(record: RecoveryRouteRecordV1): String =
        json.encodeToString(RecoveryRouteRecordV1.serializer(), record)

    fun decode(line: String): RecoveryRouteRecordV1 {
        if (line.encodeToByteArray().size > MAX_LINE_BYTES) {
            throw RecoveryRouteValidationException(RecoveryRouteValidationCode.LINE_TOO_LARGE)
        }
        return json.decodeFromString(RecoveryRouteRecordV1.serializer(), line)
    }
}

enum class RecoveryRouteValidationCode {
    LINE_TOO_LARGE,
    INVALID_ROUTE_ID,
    UNKNOWN_TABLE,
    MISSING_REQUIRED_TABLE,
    INVALID_ROW_COUNT,
    INVALID_COLUMN_NAME,
    ROUTE_ID_MISMATCH,
    MISSING_PRIMARY_KEY,
    DUPLICATE_PRIMARY_KEY,
}

class RecoveryRouteValidationException(val code: RecoveryRouteValidationCode) :
    IllegalArgumentException(code.name)
