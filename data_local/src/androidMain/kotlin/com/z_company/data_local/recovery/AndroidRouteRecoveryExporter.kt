package com.z_company.data_local.recovery

import android.database.Cursor
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.system.Os
import android.system.OsConstants
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

data class ExportedRecoverySection(
    val file: File,
    val itemCount: Long,
    val sha256: String,
)

class RouteRecoveryOrphansPresentException(val orphanCount: Long) :
    IllegalStateException("Route recovery source contains orphan rows")

class AndroidRouteRecoveryExporter {
    fun export(sourceDatabase: File, destination: File): ExportedRecoverySection {
        require(sourceDatabase.isFile) { "Route recovery source does not exist" }
        val destinationDirectory = requireNotNull(destination.parentFile) {
            "Recovery export destination must have a parent directory"
        }
        require(destinationDirectory.isDirectory || destinationDirectory.mkdirs()) {
            "Cannot create recovery export directory"
        }
        val temporary = File(destinationDirectory, destination.name + ".tmp")
        temporary.delete()

        var itemCount = 0L
        val database = openReadOnlyPreservingCorruption(sourceDatabase)
        try {
            require(REQUIRED_TABLES.all { hasTable(database, it) }) {
                "Route recovery source is missing a required table"
            }
            val orphanCount = countOrphans(database)
            if (orphanCount > 0L) throw RouteRecoveryOrphansPresentException(orphanCount)
            FileOutputStream(temporary).use { fileOutput ->
                BufferedOutputStream(fileOutput).use { output ->
                    database.rawQuery("SELECT id FROM BasicData ORDER BY id", null).use { routes ->
                        while (routes.moveToNext()) {
                            val routeId = routes.getString(0)
                            val record = RecoveryRouteRecordV1(
                                routeId = routeId,
                                tables = TABLES.filter { hasTable(database, it.name) }
                                    .associate { table ->
                                        table.name to readRows(database, table, routeId)
                                    },
                            )
                            output.write(RecoveryRouteRecordJson.encode(record).encodeToByteArray())
                            output.write('\n'.code)
                            itemCount++
                        }
                    }
                    output.flush()
                    fileOutput.fd.sync()
                }
            }
            val digest = sha256(temporary)
            Os.rename(temporary.path, destination.path)
            syncDirectory(destination.parentFile)
            return ExportedRecoverySection(destination, itemCount, digest)
        } finally {
            database.close()
            temporary.delete()
        }
    }

    private fun readRows(
        database: SQLiteDatabase,
        table: RouteTable,
        routeId: String,
    ): List<RecoveryRowV1> {
        val selection = if (table.name == "BasicData") "id = ?" else "basicId = ?"
        val orderBy = table.primaryKey.takeIf { hasColumn(database, table.name, it) }
        val sql = buildString {
            append("SELECT * FROM `${table.name}` WHERE $selection")
            if (orderBy != null) append(" ORDER BY `$orderBy`")
        }
        return database.rawQuery(sql, arrayOf(routeId)).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toRecoveryRow())
            }
        }
    }

    private fun Cursor.toRecoveryRow(): RecoveryRowV1 = RecoveryRowV1(
        values = buildMap {
            for (index in 0 until columnCount) {
                val value = when (getType(index)) {
                    Cursor.FIELD_TYPE_NULL -> JsonNull
                    Cursor.FIELD_TYPE_INTEGER -> JsonPrimitive(getLong(index))
                    Cursor.FIELD_TYPE_FLOAT -> JsonPrimitive(getDouble(index))
                    Cursor.FIELD_TYPE_STRING -> JsonPrimitive(getString(index))
                    Cursor.FIELD_TYPE_BLOB -> throw IllegalStateException(
                        "BLOB column is not supported in recovery routes v1"
                    )
                    else -> throw IllegalStateException("Unknown SQLite field type")
                }
                put(getColumnName(index), value)
            }
        },
    )

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

    private fun syncDirectory(directory: File?) {
        requireNotNull(directory) { "Recovery export directory is missing" }
        val descriptor = Os.open(directory.path, OsConstants.O_RDONLY, 0)
        try {
            Os.fsync(descriptor)
        } finally {
            Os.close(descriptor)
        }
    }

    private fun openReadOnlyPreservingCorruption(file: File): SQLiteDatabase =
        SQLiteDatabase.openDatabase(
            file.path,
            null,
            SQLiteDatabase.OPEN_READONLY,
            DatabaseErrorHandler { },
        )

    private fun hasTable(database: SQLiteDatabase, table: String): Boolean =
        database.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(table),
        ).use { it.moveToFirst() }

    private fun hasColumn(database: SQLiteDatabase, table: String, column: String): Boolean =
        database.rawQuery("PRAGMA table_info(`$table`)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) return@use true
            }
            false
        }

    private fun countOrphans(database: SQLiteDatabase): Long =
        TABLES.asSequence()
            .filter { it.name != "BasicData" && hasTable(database, it.name) }
            .sumOf { table ->
                database.rawQuery(
                    "SELECT count(*) FROM `${table.name}` child LEFT JOIN BasicData parent " +
                        "ON parent.id = child.basicId WHERE parent.id IS NULL",
                    null,
                ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
            }

    private data class RouteTable(val name: String, val primaryKey: String)

    private companion object {
        val TABLES = listOf(
            RouteTable("BasicData", "id"),
            RouteTable("Locomotive", "locoId"),
            RouteTable("Train", "trainId"),
            RouteTable("Passenger", "passengerId"),
            RouteTable("OtherWork", "otherWorkId"),
            RouteTable("RoutePartner", "routePartnerId"),
            RouteTable("Photo", "photoId"),
        )
        val REQUIRED_TABLES = setOf("BasicData", "Locomotive", "Train", "Passenger", "Photo")
    }
}
