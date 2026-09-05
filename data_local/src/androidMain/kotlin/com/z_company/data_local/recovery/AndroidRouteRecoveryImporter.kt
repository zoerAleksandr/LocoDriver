package com.z_company.data_local.recovery

import android.content.Context
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.z_company.data_local.route.db.RouteDatabase
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

data class ImportedRecoveryRoutes(
    val databaseFile: File,
    val routeCount: Long,
)

class AndroidRouteRecoveryImporter(private val context: Context) {
    fun importToFreshDatabase(
        routesSection: File,
        expected: RecoveryArchiveSectionDigest,
        databaseName: String = "Route.recovery.import.db",
    ): ImportedRecoveryRoutes {
        require(routesSection.isFile) { "Recovery routes section does not exist" }
        require(expected.itemCount >= 0L) { "Recovery route count is invalid" }
        require(sha256(routesSection).equals(expected.sha256, ignoreCase = true)) {
            "Recovery routes checksum mismatch"
        }

        val databaseFile = context.getDatabasePath(databaseName)
        require(!databaseFile.exists()) { "Recovery import database already exists" }
        AndroidSqliteDriver(RouteDatabase.Schema, context, databaseName).use { driver ->
            driver.execute(null, "UPDATE BasicData SET id = id WHERE 0", 0).value
        }
        check(databaseFile.isFile) { "Recovery import database was created at an unexpected path" }

        var importedRoutes = 0L
        val seenRouteIds = mutableSetOf<String>()
        val expectedRows = INSERT_ORDER.associateWith { 0L }.toMutableMap()
        val database = openPreservingCorruption(databaseFile)
        try {
            database.setForeignKeyConstraintsEnabled(true)
            database.beginTransaction()
            try {
                readBoundedLines(routesSection) { line ->
                    if (line.isBlank()) return@readBoundedLines
                    val record = RecoveryRouteRecordValidator.decodeAndValidate(line)
                    require(seenRouteIds.add(record.routeId)) { "Duplicate recovery route ID" }
                    insertRecord(database, record)
                    INSERT_ORDER.forEach { table ->
                        expectedRows[table] = expectedRows.getValue(table) +
                            record.tables[table].orEmpty().size
                    }
                    importedRoutes++
                }
                require(importedRoutes == expected.itemCount) {
                    "Recovery route count mismatch"
                }
                expectedRows.forEach { (table, expectedCount) ->
                    require(countRows(database, table) == expectedCount) {
                        "Imported recovery row count mismatch for $table"
                    }
                }
                require(integrityCheck(database) == "ok") { "Recovery import integrity check failed" }
                require(!hasForeignKeyViolation(database)) {
                    "Recovery import has foreign key violations"
                }
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        } catch (error: Throwable) {
            database.close()
            context.deleteDatabase(databaseName)
            throw error
        }
        database.close()
        return ImportedRecoveryRoutes(databaseFile, importedRoutes)
    }

    private fun insertRecord(database: SQLiteDatabase, record: RecoveryRouteRecordV1) {
        INSERT_ORDER.forEach { table ->
            val destinationColumns = tableColumns(database, table)
            record.tables[table].orEmpty().forEach { row ->
                val values = row.values.filterKeys(destinationColumns::contains)
                require(values.isNotEmpty()) { "Recovery row has no supported columns" }
                val names = values.keys.joinToString(", ") { "`$it`" }
                val placeholders = values.keys.joinToString(", ") { "?" }
                val bindArgs = values.values.map(::sqliteValue).toTypedArray()
                database.execSQL(
                    "INSERT INTO `$table` ($names) VALUES ($placeholders)",
                    bindArgs,
                )
            }
        }
    }

    private fun sqliteValue(value: kotlinx.serialization.json.JsonElement): Any? = when (value) {
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
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            buildSet {
                while (cursor.moveToNext()) add(cursor.getString(nameIndex))
            }
        }

    private fun readBoundedLines(file: File, consume: (String) -> Unit) {
        BufferedInputStream(FileInputStream(file)).use { input ->
            val line = ByteArrayOutputStream()
            while (true) {
                val byte = input.read()
                if (byte < 0) {
                    if (line.size() > 0) consume(line.toByteArray().decodeToString())
                    break
                }
                if (byte == '\n'.code) {
                    val bytes = line.toByteArray()
                    val length = if (bytes.lastOrNull() == '\r'.code.toByte()) bytes.size - 1 else bytes.size
                    consume(bytes.decodeToString(endIndex = length))
                    line.reset()
                } else {
                    if (line.size() >= RecoveryRouteRecordJson.MAX_LINE_BYTES) {
                        throw RecoveryRouteValidationException(
                            RecoveryRouteValidationCode.LINE_TOO_LARGE
                        )
                    }
                    line.write(byte)
                }
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
    }

    private fun countRows(database: SQLiteDatabase, table: String): Long =
        database.rawQuery("SELECT count(*) FROM `$table`", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }

    private fun integrityCheck(database: SQLiteDatabase): String =
        database.rawQuery("PRAGMA integrity_check", null).use { cursor ->
            require(cursor.moveToFirst()) { "Recovery integrity check returned no result" }
            cursor.getString(0)
        }

    private fun hasForeignKeyViolation(database: SQLiteDatabase): Boolean =
        database.rawQuery("PRAGMA foreign_key_check", null).use { it.moveToFirst() }

    private fun openPreservingCorruption(file: File): SQLiteDatabase =
        SQLiteDatabase.openDatabase(
            file.path,
            null,
            SQLiteDatabase.OPEN_READWRITE,
            DatabaseErrorHandler { },
        )

    private companion object {
        val INSERT_ORDER = listOf(
            "BasicData",
            "Locomotive",
            "Train",
            "Passenger",
            "OtherWork",
            "RoutePartner",
            "Photo",
        )
    }
}
