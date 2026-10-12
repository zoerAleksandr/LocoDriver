package com.z_company.data_local.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SchemaRepairPlannerTest {
    private val specs = listOf(
        TableSpec(
            "A",
            listOf(
                "id" to "TEXT NOT NULL",
                "x" to "INTEGER",
                "y" to "INTEGER NOT NULL DEFAULT 1",
                "z" to "TEXT NOT NULL",
            ),
        ),
        TableSpec("B", listOf("k" to "TEXT")),
    )

    @Test
    fun missingTablesDetected() {
        assertEquals(listOf("B"), SchemaRepairPlanner.missingTables(specs, mapOf("A" to setOf("id"))))
    }

    @Test
    fun missingColumnsGetAlterWithDefaults() {
        val sql = SchemaRepairPlanner.missingColumnStatements(
            specs,
            mapOf("A" to setOf("id"), "B" to setOf("k")),
        )
        assertEquals(
            listOf(
                "ALTER TABLE A ADD COLUMN x INTEGER",
                "ALTER TABLE A ADD COLUMN y INTEGER NOT NULL DEFAULT 1",
                "ALTER TABLE A ADD COLUMN z TEXT NOT NULL DEFAULT ''",
            ),
            sql,
        )
    }

    @Test
    fun absentTableIsSkippedAndCompleteSchemaIsNoop() {
        assertTrue(SchemaRepairPlanner.missingColumnStatements(specs, emptyMap()).isEmpty())
        val full = mapOf("A" to setOf("id", "x", "y", "z"), "B" to setOf("k"))
        assertTrue(SchemaRepairPlanner.missingColumnStatements(specs, full).isEmpty())
    }

    @Test
    fun realSpecsContainTombstones() {
        val names = SchemaSpecs.settings.map { it.name }
        assertTrue("StationTombstone" in names && "LocomotiveSeriesTombstone" in names)
    }
}
