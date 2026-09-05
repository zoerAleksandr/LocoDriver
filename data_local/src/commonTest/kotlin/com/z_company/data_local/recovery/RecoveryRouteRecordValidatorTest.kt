package com.z_company.data_local.recovery

import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecoveryRouteRecordValidatorTest {
    @Test
    fun validRecordPassesRoundTripValidation() {
        val record = validRecord()

        val decoded = RecoveryRouteRecordValidator.decodeAndValidate(
            RecoveryRouteRecordJson.encode(record)
        )

        assertEquals(record, decoded)
    }

    @Test
    fun mismatchedChildParentIsRejected() {
        val record = validRecord().let { source ->
            source.copy(
                tables = source.tables + (
                    "Train" to listOf(
                        row("trainId" to "train-1", "basicId" to "another-route")
                    )
                )
            )
        }

        assertCode(RecoveryRouteValidationCode.ROUTE_ID_MISMATCH) {
            RecoveryRouteRecordValidator.validate(record)
        }
    }

    @Test
    fun unknownTableIsRejected() {
        val record = validRecord().let { source ->
            source.copy(tables = source.tables + ("sqlite_master" to emptyList()))
        }

        assertCode(RecoveryRouteValidationCode.UNKNOWN_TABLE) {
            RecoveryRouteRecordValidator.validate(record)
        }
    }

    @Test
    fun duplicatePrimaryKeyIsRejected() {
        val duplicate = row("trainId" to "train-1", "basicId" to "route-1")
        val record = validRecord().let { source ->
            source.copy(tables = source.tables + ("Train" to listOf(duplicate, duplicate)))
        }

        assertCode(RecoveryRouteValidationCode.DUPLICATE_PRIMARY_KEY) {
            RecoveryRouteRecordValidator.validate(record)
        }
    }

    @Test
    fun oversizedLineIsRejectedBeforeJsonDecode() {
        val line = " ".repeat(RecoveryRouteRecordJson.MAX_LINE_BYTES + 1)

        assertCode(RecoveryRouteValidationCode.LINE_TOO_LARGE) {
            RecoveryRouteRecordValidator.decodeAndValidate(line)
        }
    }

    private fun validRecord(): RecoveryRouteRecordV1 = RecoveryRouteRecordV1(
        routeId = "route-1",
        tables = mapOf(
            "BasicData" to listOf(row("id" to "route-1")),
            "Locomotive" to listOf(row("locoId" to "loco-1", "basicId" to "route-1")),
            "Train" to listOf(row("trainId" to "train-1", "basicId" to "route-1")),
            "Passenger" to listOf(row("passengerId" to "passenger-1", "basicId" to "route-1")),
            "Photo" to listOf(row("photoId" to "photo-1", "basicId" to "route-1")),
        ),
    )

    private fun row(vararg values: Pair<String, String>): RecoveryRowV1 = RecoveryRowV1(
        values = values.associate { (name, value) -> name to JsonPrimitive(value) }
    )

    private fun assertCode(expected: RecoveryRouteValidationCode, block: () -> Unit) {
        val error = assertFailsWith<RecoveryRouteValidationException>(block = block)
        assertEquals(expected, error.code)
    }
}
