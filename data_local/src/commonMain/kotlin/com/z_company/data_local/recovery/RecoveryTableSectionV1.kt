package com.z_company.data_local.recovery

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RecoveryTableSectionV1(
    val sectionFormatVersion: Int,
    val tables: Map<String, List<RecoveryRowV1>>,
)

enum class RecoveryTableSectionKind(val fileName: String, val allowedTables: Set<String>) {
    SETTINGS(
        "settings.json",
        setOf(
            "UserSettings",
            "MonthOfYear",
            "ReleaseDay",
            "ProductionCalendarDay",
            "RegionalHoliday",
            "Partner",
        ),
    ),
    SALARY_SETTINGS("salary-settings.json", setOf("SalarySetting")),
    NORMS("norms.json", setOf("LocomotiveSeries", "StationNorm")),
}

enum class RecoveryTableSectionValidationCode {
    SECTION_TOO_LARGE,
    UNSUPPORTED_FORMAT,
    UNKNOWN_TABLE,
    INVALID_COLUMN_NAME,
    TOO_MANY_ROWS,
}

class RecoveryTableSectionValidationException(
    val code: RecoveryTableSectionValidationCode,
) : IllegalArgumentException(code.name)

object RecoveryTableSectionJson {
    const val CURRENT_FORMAT_VERSION: Int = 1
    const val MAX_SECTION_BYTES: Int = 16 * 1024 * 1024
    const val MAX_TOTAL_ROWS: Int = 200_000

    private val json = Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = true
    }

    fun encode(section: RecoveryTableSectionV1): String =
        json.encodeToString(RecoveryTableSectionV1.serializer(), section)

    fun decodeAndValidate(
        content: String,
        kind: RecoveryTableSectionKind,
    ): RecoveryTableSectionV1 {
        if (content.encodeToByteArray().size > MAX_SECTION_BYTES) {
            throw RecoveryTableSectionValidationException(
                RecoveryTableSectionValidationCode.SECTION_TOO_LARGE
            )
        }
        val section = json.decodeFromString(RecoveryTableSectionV1.serializer(), content)
        if (section.sectionFormatVersion != CURRENT_FORMAT_VERSION) {
            throw RecoveryTableSectionValidationException(
                RecoveryTableSectionValidationCode.UNSUPPORTED_FORMAT
            )
        }
        if (!kind.allowedTables.containsAll(section.tables.keys)) {
            throw RecoveryTableSectionValidationException(
                RecoveryTableSectionValidationCode.UNKNOWN_TABLE
            )
        }
        var rowCount = 0L
        section.tables.values.flatten().forEach { row ->
            rowCount++
            if (row.values.keys.any { !COLUMN_NAME.matches(it) }) {
                throw RecoveryTableSectionValidationException(
                    RecoveryTableSectionValidationCode.INVALID_COLUMN_NAME
                )
            }
        }
        if (rowCount > MAX_TOTAL_ROWS) {
            throw RecoveryTableSectionValidationException(
                RecoveryTableSectionValidationCode.TOO_MANY_ROWS
            )
        }
        return section
    }

    private val COLUMN_NAME = Regex("[A-Za-z][A-Za-z0-9_]{0,63}")
}
