package com.z_company.data_local

import android.database.sqlite.SQLiteDatabase
import com.z_company.data_local.route.db.RouteDatabase
import java.io.File

data class RouteDatabaseProfile(
    val userVersion: Int,
    val hasBasicData: Boolean,
    val hasLegacyRoomTrain: Boolean,
    val hasLegacyRoomLocomotive: Boolean,
    val hasPassengerArrivalFlag: Boolean,
    val hasWorkStartBeforeArrival: Boolean,
    val hasOtherWork: Boolean,
    val hasRoutePartner: Boolean,
    val hasTrashFields: Boolean,
    val hasDiagnosticTables: Boolean,
) {
    val isRecognized: Boolean
        get() = hasBasicData && userVersion in MIN_RECOGNIZED_VERSION..MAX_RECOGNIZED_VERSION

    companion object {
        private const val MIN_RECOGNIZED_VERSION = 1
        private val MAX_RECOGNIZED_VERSION = RouteDatabase.Schema.version.toInt()
    }
}

/** Reads schema metadata only. No user rows or values are loaded. */
class RouteDatabaseProfileDetector {
    fun detect(databaseFile: File): RouteDatabaseProfile {
        require(databaseFile.isFile) { "Route database does not exist" }
        val database = SQLiteDatabase.openDatabase(
            databaseFile.path,
            null,
            SQLiteDatabase.OPEN_READONLY,
        )
        return try {
            RouteDatabaseProfile(
                userVersion = database.version,
                hasBasicData = hasTable(database, "BasicData"),
                hasLegacyRoomTrain = hasColumn(database, "Train", "remoteObjectId"),
                hasLegacyRoomLocomotive = hasColumn(database, "Locomotive", "removeObjectId"),
                hasPassengerArrivalFlag = hasColumn(database, "Passenger", "isWorkStartByArrival"),
                hasWorkStartBeforeArrival = hasColumn(database, "BasicData", "timeStartWorkBeforeArrival"),
                hasOtherWork = hasTable(database, "OtherWork"),
                hasRoutePartner = hasTable(database, "RoutePartner"),
                hasTrashFields = TRASH_COLUMNS.all { hasColumn(database, "BasicData", it) },
                hasDiagnosticTables = DIAGNOSTIC_TABLES.all { hasTable(database, it) },
            )
        } finally {
            database.close()
        }
    }

    private fun hasTable(database: SQLiteDatabase, table: String): Boolean =
        database.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(table),
        ).use { it.moveToFirst() }

    private fun hasColumn(database: SQLiteDatabase, table: String, column: String): Boolean {
        if (!hasTable(database, table)) return false
        return database.rawQuery("PRAGMA table_info(`$table`)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) return@use true
            }
            false
        }
    }

    private companion object {
        val TRASH_COLUMNS = setOf(
            "deletedAt",
            "deletionReason",
            "remoteDeletionPending",
            "remoteDeletedAt",
        )
        val DIAGNOSTIC_TABLES = setOf(
            "DiagnosticInstallation",
            "RouteEvent",
            "DiagnosticOutbox",
            "MigrationStatus",
        )
    }
}
