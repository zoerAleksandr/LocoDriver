package com.z_company.data_local.recovery

import android.content.Context
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.data_local.setting.salarydb.SalarySettingDatabase
import java.io.File
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

data class ImportedRecoveryTables(
    val databaseFile: File,
    val itemCount: Long,
)

class AndroidSettingsRecoveryImporter(private val context: Context) {
    fun importSettingsAndNormsToFreshDatabase(
        settingsFile: File,
        settingsDigest: RecoveryArchiveSectionDigest,
        normsFile: File,
        normsDigest: RecoveryArchiveSectionDigest,
        databaseName: String = "Settings.recovery.import.db",
    ): ImportedRecoveryTables {
        val sections = listOf(
            readSection(settingsFile, settingsDigest, RecoveryTableSectionKind.SETTINGS),
            readSection(normsFile, normsDigest, RecoveryTableSectionKind.NORMS),
        )
        return importFresh(databaseName, sections, SETTINGS_INSERT_ORDER) {
            AndroidSqliteDriver(SettingsDatabase.Schema, context, databaseName).use { driver ->
                driver.execute(null, "UPDATE UserSettings SET settingsKey = settingsKey WHERE 0", 0)
                    .value
            }
        }
    }

    fun importSalaryToFreshDatabase(
        salaryFile: File,
        salaryDigest: RecoveryArchiveSectionDigest,
        databaseName: String = "SalarySetting.recovery.import.db",
    ): ImportedRecoveryTables {
        val section = readSection(
            salaryFile,
            salaryDigest,
            RecoveryTableSectionKind.SALARY_SETTINGS,
        )
        return importFresh(databaseName, listOf(section), listOf("SalarySetting")) {
            AndroidSqliteDriver(SalarySettingDatabase.Schema, context, databaseName).use { driver ->
                driver.execute(
                    null,
                    "UPDATE SalarySetting SET salarySettingKey = salarySettingKey WHERE 0",
                    0,
                ).value
            }
        }
    }

    private fun readSection(
        file: File,
        expected: RecoveryArchiveSectionDigest,
        kind: RecoveryTableSectionKind,
    ): RecoveryTableSectionV1 {
        require(file.isFile) { "Recovery table section does not exist" }
        require(file.length() <= RecoveryTableSectionJson.MAX_SECTION_BYTES) {
            "Recovery table section exceeds size limit"
        }
        val bytes = file.readBytes()
        require(RecoverySha256.digestHex(bytes).equals(expected.sha256, ignoreCase = true)) {
            "Recovery table section checksum mismatch"
        }
        val section = RecoveryTableSectionJson.decodeAndValidate(bytes.decodeToString(), kind)
        require(section.rowCount() == expected.itemCount) {
            "Recovery table section row count mismatch"
        }
        return section
    }

    private fun importFresh(
        databaseName: String,
        sections: List<RecoveryTableSectionV1>,
        insertOrder: List<String>,
        createSchema: () -> Unit,
    ): ImportedRecoveryTables {
        val databaseFile = context.getDatabasePath(databaseName)
        require(!databaseFile.exists()) { "Recovery import database already exists" }
        try {
            createSchema()
            check(databaseFile.isFile) { "Recovery import database was not created" }
            val expectedRows = insertOrder.associateWith { 0L }.toMutableMap()
            val database = openPreservingCorruption(databaseFile)
            try {
                database.setForeignKeyConstraintsEnabled(true)
                database.beginTransaction()
                try {
                    insertOrder.forEach { table ->
                        sections.flatMap { it.tables[table].orEmpty() }.forEach { row ->
                            insertRow(database, table, row)
                            expectedRows[table] = expectedRows.getValue(table) + 1L
                        }
                    }
                    expectedRows.forEach { (table, expected) ->
                        require(countRows(database, table) == expected) {
                            "Imported recovery row count mismatch for $table"
                        }
                    }
                    require(integrityCheck(database) == "ok") {
                        "Recovery settings integrity check failed"
                    }
                    require(!database.rawQuery("PRAGMA foreign_key_check", null).use {
                        it.moveToFirst()
                    }) { "Recovery settings have foreign key violations" }
                    database.setTransactionSuccessful()
                } finally {
                    database.endTransaction()
                }
            } finally {
                database.close()
            }
            return ImportedRecoveryTables(databaseFile, expectedRows.values.sum())
        } catch (error: Throwable) {
            context.deleteDatabase(databaseName)
            throw error
        }
    }

    private fun insertRow(database: SQLiteDatabase, table: String, row: RecoveryRowV1) {
        val destinationColumns = tableColumns(database, table)
        val values = row.values.filterKeys(destinationColumns::contains)
        require(values.isNotEmpty()) { "Recovery row has no supported columns" }
        val names = values.keys.joinToString(", ") { "`$it`" }
        val placeholders = values.keys.joinToString(", ") { "?" }
        database.execSQL(
            "INSERT INTO `$table` ($names) VALUES ($placeholders)",
            values.values.map(::sqliteValue).toTypedArray(),
        )
    }

    private fun sqliteValue(value: JsonElement): Any? = when (value) {
        JsonNull -> null
        is JsonPrimitive -> when {
            value.isString -> value.content
            value.booleanOrNull != null -> if (value.booleanOrNull == true) 1L else 0L
            value.longOrNull != null -> value.longOrNull
            value.doubleOrNull != null -> value.doubleOrNull
            else -> throw IllegalArgumentException("Unsupported recovery JSON primitive")
        }
        else -> throw IllegalArgumentException("Nested recovery JSON values are not supported")
    }

    private fun tableColumns(database: SQLiteDatabase, table: String): Set<String> =
        database.rawQuery("PRAGMA table_info(`$table`)", null).use { cursor ->
            val name = cursor.getColumnIndexOrThrow("name")
            buildSet { while (cursor.moveToNext()) add(cursor.getString(name)) }
        }

    private fun countRows(database: SQLiteDatabase, table: String): Long =
        database.rawQuery("SELECT count(*) FROM `$table`", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }

    private fun integrityCheck(database: SQLiteDatabase): String =
        database.rawQuery("PRAGMA integrity_check", null).use { cursor ->
            require(cursor.moveToFirst())
            cursor.getString(0)
        }

    private fun openPreservingCorruption(file: File): SQLiteDatabase =
        SQLiteDatabase.openDatabase(
            file.path,
            null,
            SQLiteDatabase.OPEN_READWRITE,
            DatabaseErrorHandler { },
        )

    private fun RecoveryTableSectionV1.rowCount(): Long =
        tables.values.sumOf { it.size.toLong() }

    private companion object {
        val SETTINGS_INSERT_ORDER = listOf(
            "UserSettings",
            "MonthOfYear",
            "ReleaseDay",
            "ProductionCalendarDay",
            "RegionalHoliday",
            "Partner",
            "LocomotiveSeries",
            "StationNorm",
        )
    }
}
