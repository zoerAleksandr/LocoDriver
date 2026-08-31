package com.z_company.data_local.recovery

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

object RecoveryRouteRecordValidator {
    const val MAX_ROWS_PER_ROUTE: Int = 10_000

    private val identifier = Regex("^[A-Za-z_][A-Za-z0-9_]{0,127}$")
    private val primaryKeys = mapOf(
        "BasicData" to "id",
        "Locomotive" to "locoId",
        "Train" to "trainId",
        "Passenger" to "passengerId",
        "OtherWork" to "otherWorkId",
        "RoutePartner" to "routePartnerId",
        "Photo" to "photoId",
    )
    private val requiredTables = setOf("BasicData", "Locomotive", "Train", "Passenger", "Photo")

    fun decodeAndValidate(line: String): RecoveryRouteRecordV1 =
        RecoveryRouteRecordJson.decode(line).also(::validate)

    fun validate(record: RecoveryRouteRecordV1) {
        requireValid(
            record.routeId.isNotBlank() && record.routeId.length <= 128,
            RecoveryRouteValidationCode.INVALID_ROUTE_ID,
        )
        requireValid(
            record.tables.keys.all { it in primaryKeys },
            RecoveryRouteValidationCode.UNKNOWN_TABLE,
        )
        requireValid(
            record.tables.keys.containsAll(requiredTables),
            RecoveryRouteValidationCode.MISSING_REQUIRED_TABLE,
        )
        requireValid(
            record.tables.values.sumOf { it.size } <= MAX_ROWS_PER_ROUTE,
            RecoveryRouteValidationCode.INVALID_ROW_COUNT,
        )

        record.tables.forEach { (table, rows) ->
            if (table == "BasicData") {
                requireValid(rows.size == 1, RecoveryRouteValidationCode.INVALID_ROW_COUNT)
            }
            val primaryKey = primaryKeys.getValue(table)
            val seenPrimaryKeys = mutableSetOf<String>()
            rows.forEach { row ->
                requireValid(
                    row.values.keys.all(identifier::matches),
                    RecoveryRouteValidationCode.INVALID_COLUMN_NAME,
                )
                val rowId = (row.values[primaryKey] as? JsonPrimitive)?.contentOrNull
                if (rowId.isNullOrBlank()) {
                    throw RecoveryRouteValidationException(
                        RecoveryRouteValidationCode.MISSING_PRIMARY_KEY
                    )
                }
                requireValid(seenPrimaryKeys.add(rowId), RecoveryRouteValidationCode.DUPLICATE_PRIMARY_KEY)
                val parentIdColumn = if (table == "BasicData") "id" else "basicId"
                val parentId = (row.values[parentIdColumn] as? JsonPrimitive)?.contentOrNull
                requireValid(parentId == record.routeId, RecoveryRouteValidationCode.ROUTE_ID_MISMATCH)
            }
        }
    }

    private fun requireValid(condition: Boolean, code: RecoveryRouteValidationCode) {
        if (!condition) throw RecoveryRouteValidationException(code)
    }
}
