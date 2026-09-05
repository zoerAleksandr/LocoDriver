package com.z_company.data_local.recovery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.JsonPrimitive

class RecoveryTableSectionValidatorTest {
    @Test
    fun roundTripKnownSettingsTable() {
        val source = RecoveryTableSectionV1(
            sectionFormatVersion = 1,
            tables = mapOf(
                "UserSettings" to listOf(
                    RecoveryRowV1(mapOf("settingsKey" to JsonPrimitive("default")))
                )
            ),
        )

        val decoded = RecoveryTableSectionJson.decodeAndValidate(
            RecoveryTableSectionJson.encode(source),
            RecoveryTableSectionKind.SETTINGS,
        )

        assertEquals(source, decoded)
    }

    @Test
    fun tableCannotCrossSectionBoundary() {
        val content = RecoveryTableSectionJson.encode(
            RecoveryTableSectionV1(1, mapOf("SalarySetting" to emptyList()))
        )

        val error = assertFailsWith<RecoveryTableSectionValidationException> {
            RecoveryTableSectionJson.decodeAndValidate(content, RecoveryTableSectionKind.SETTINGS)
        }

        assertEquals(RecoveryTableSectionValidationCode.UNKNOWN_TABLE, error.code)
    }

    @Test
    fun unsafeColumnNameIsRejected() {
        val content = RecoveryTableSectionJson.encode(
            RecoveryTableSectionV1(
                1,
                mapOf(
                    "SalarySetting" to listOf(
                        RecoveryRowV1(mapOf("value);DROP" to JsonPrimitive(1)))
                    )
                ),
            )
        )

        val error = assertFailsWith<RecoveryTableSectionValidationException> {
            RecoveryTableSectionJson.decodeAndValidate(
                content,
                RecoveryTableSectionKind.SALARY_SETTINGS,
            )
        }

        assertEquals(RecoveryTableSectionValidationCode.INVALID_COLUMN_NAME, error.code)
    }
}
