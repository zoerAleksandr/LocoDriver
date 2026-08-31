package com.z_company.data_local.recovery

import android.database.Cursor
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.FileOutputStream
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

class AndroidTableRecoveryExporter {
    fun export(
        sourceDatabase: File,
        destination: File,
        kind: RecoveryTableSectionKind,
    ): ExportedRecoverySection {
        require(sourceDatabase.isFile) { "Recovery source database does not exist" }
        val directory = requireNotNull(destination.parentFile)
        require(directory.isDirectory || directory.mkdirs()) { "Cannot create recovery directory" }
        val temporary = File(directory, destination.name + ".tmp")
        temporary.delete()

        val database = openReadOnlyPreservingCorruption(sourceDatabase)
        try {
            val tables = kind.allowedTables.sorted().filter { hasTable(database, it) }
                .associateWith { table -> readRows(database, table) }
            val section = RecoveryTableSectionV1(
                sectionFormatVersion = RecoveryTableSectionJson.CURRENT_FORMAT_VERSION,
                tables = tables,
            )
            val bytes = RecoveryTableSectionJson.encode(section).encodeToByteArray()
            require(bytes.size <= RecoveryTableSectionJson.MAX_SECTION_BYTES) {
                "Recovery section exceeds size limit"
            }
            FileOutputStream(temporary).use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            val itemCount = tables.values.sumOf { it.size.toLong() }
            val sha256 = RecoverySha256.digestHex(bytes)
            Os.rename(temporary.path, destination.path)
            syncDirectory(directory)
            return ExportedRecoverySection(destination, itemCount, sha256)
        } finally {
            database.close()
            temporary.delete()
        }
    }

    private fun readRows(database: SQLiteDatabase, table: String): List<RecoveryRowV1> {
        val order = primaryKeyColumns(database, table)
        val query = buildString {
            append("SELECT * FROM `$table`")
            if (order.isNotEmpty()) append(order.joinToString(", ", " ORDER BY ") { "`$it`" })
        }
        return database.rawQuery(query, null).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toRecoveryRow()) }
        }
    }

    private fun Cursor.toRecoveryRow(): RecoveryRowV1 = RecoveryRowV1(
        buildMap {
            for (index in 0 until columnCount) {
                put(
                    getColumnName(index),
                    when (getType(index)) {
                        Cursor.FIELD_TYPE_NULL -> JsonNull
                        Cursor.FIELD_TYPE_INTEGER -> JsonPrimitive(getLong(index))
                        Cursor.FIELD_TYPE_FLOAT -> JsonPrimitive(getDouble(index))
                        Cursor.FIELD_TYPE_STRING -> JsonPrimitive(getString(index))
                        Cursor.FIELD_TYPE_BLOB -> throw IllegalStateException(
                            "BLOB is not supported in recovery table sections"
                        )
                        else -> throw IllegalStateException("Unknown SQLite field type")
                    },
                )
            }
        }
    )

    private fun primaryKeyColumns(database: SQLiteDatabase, table: String): List<String> =
        database.rawQuery("PRAGMA table_info(`$table`)", null).use { cursor ->
            val name = cursor.getColumnIndexOrThrow("name")
            val pk = cursor.getColumnIndexOrThrow("pk")
            buildList {
                val indexed = mutableListOf<Pair<Int, String>>()
                while (cursor.moveToNext()) {
                    val position = cursor.getInt(pk)
                    if (position > 0) indexed += position to cursor.getString(name)
                }
                addAll(indexed.sortedBy { it.first }.map { it.second })
            }
        }

    private fun hasTable(database: SQLiteDatabase, table: String): Boolean =
        database.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(table),
        ).use { it.moveToFirst() }

    private fun openReadOnlyPreservingCorruption(file: File): SQLiteDatabase =
        SQLiteDatabase.openDatabase(
            file.path,
            null,
            SQLiteDatabase.OPEN_READONLY,
            DatabaseErrorHandler { },
        )

    private fun syncDirectory(directory: File) {
        val descriptor = Os.open(directory.path, OsConstants.O_RDONLY, 0)
        try {
            Os.fsync(descriptor)
        } finally {
            Os.close(descriptor)
        }
    }
}
