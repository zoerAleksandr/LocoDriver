package com.z_company.data_local.recovery

import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.os.StatFs
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.security.MessageDigest

data class FrozenDatabaseSnapshot(
    val file: File,
    val sourceVersion: Int,
    val tableRowCounts: Map<String, Long>,
    val sha256: String,
)

class RecoverySnapshotBusyException :
    IllegalStateException("Recovery snapshot source has active database users")

class RecoverySnapshotLowStorageException(requiredBytes: Long, availableBytes: Long) :
    IllegalStateException("Recovery snapshot needs $requiredBytes bytes; $availableBytes available")

/**
 * Creates a frozen copy while holding SQLite's EXCLUSIVE locking mode.
 *
 * This must run in a quiesced application phase before repositories/drivers are exposed. Existing
 * SQLite users cause the checkpoint/lock acquisition to fail rather than producing a partial copy.
 */
class AndroidFrozenDatabaseSnapshotter {
    fun snapshot(source: File, destination: File): FrozenDatabaseSnapshot {
        require(source.isFile) { "Recovery snapshot source does not exist" }
        require(!destination.exists()) { "Recovery snapshot destination already exists" }
        val directory = requireNotNull(destination.parentFile)
        require(directory.isDirectory || directory.mkdirs()) {
            "Cannot create recovery snapshot directory"
        }
        val requiredBytes = source.length() * 2L + MIN_FREE_MARGIN_BYTES
        val availableBytes = StatFs(directory.path).availableBytes
        if (availableBytes < requiredBytes) {
            throw RecoverySnapshotLowStorageException(requiredBytes, availableBytes)
        }
        val temporary = File(directory, destination.name + ".tmp")
        temporary.delete()

        val database = openPreservingCorruption(source, SQLiteDatabase.OPEN_READWRITE)
        try {
            val lockingMode = database.rawQuery("PRAGMA locking_mode=EXCLUSIVE", null).use { cursor ->
                require(cursor.moveToFirst())
                cursor.getString(0)
            }
            require(lockingMode.equals("exclusive", ignoreCase = true)) {
                "Cannot acquire exclusive recovery snapshot mode"
            }
            val checkpoint = database.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { cursor ->
                require(cursor.moveToFirst())
                Triple(cursor.getInt(0), cursor.getInt(1), cursor.getInt(2))
            }
            if (checkpoint.first != 0 || checkpoint.second != checkpoint.third) {
                throw RecoverySnapshotBusyException()
            }
            // Force this connection to retain the EXCLUSIVE file lock before copying.
            database.rawQuery("SELECT count(*) FROM sqlite_master", null).use { it.moveToFirst() }
            val expected = fingerprint(database)
            val expectedSha256 = sha256(source)
            source.copyTo(temporary, overwrite = false)
            syncFile(temporary)
            validateSnapshot(temporary, expected, expectedSha256)
            Os.rename(temporary.path, destination.path)
            syncDirectory(directory)
            return FrozenDatabaseSnapshot(
                destination,
                expected.version,
                expected.tableRowCounts,
                expectedSha256,
            )
        } finally {
            database.close()
            temporary.delete()
        }
    }

    fun inspect(snapshot: File): FrozenDatabaseSnapshot {
        require(snapshot.isFile) { "Frozen database snapshot does not exist" }
        val database = openPreservingCorruption(snapshot, SQLiteDatabase.OPEN_READONLY)
        return try {
            val fingerprint = fingerprint(database)
            FrozenDatabaseSnapshot(
                snapshot,
                fingerprint.version,
                fingerprint.tableRowCounts,
                sha256(snapshot),
            )
        } finally {
            database.close()
        }
    }

    private fun validateSnapshot(
        file: File,
        expected: DatabaseFingerprint,
        expectedSha256: String,
    ) {
        require(sha256(file) == expectedSha256) { "Recovery snapshot checksum mismatch" }
        val snapshot = openPreservingCorruption(file, SQLiteDatabase.OPEN_READONLY)
        try {
            require(fingerprint(snapshot) == expected) { "Recovery snapshot fingerprint mismatch" }
        } finally {
            snapshot.close()
        }
    }

    private fun fingerprint(database: SQLiteDatabase): DatabaseFingerprint {
        val integrity = database.rawQuery("PRAGMA quick_check", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else "missing_result"
        }
        require(integrity.equals("ok", ignoreCase = true)) {
            "Recovery snapshot source integrity check failed"
        }
        val tables = database.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' " +
                "ORDER BY name",
            null,
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }
        val counts = tables.associateWith { table ->
            database.rawQuery("SELECT count(*) FROM `${table.replace("`", "``")}`", null)
                .use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
        }
        return DatabaseFingerprint(database.version, counts)
    }

    private fun openPreservingCorruption(file: File, flags: Int): SQLiteDatabase =
        SQLiteDatabase.openDatabase(file.path, null, flags, DatabaseErrorHandler { })

    private fun syncFile(file: File) {
        FileOutputStream(file, true).use { it.fd.sync() }
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

    private fun syncDirectory(directory: File) {
        val descriptor = Os.open(directory.path, OsConstants.O_RDONLY, 0)
        try {
            Os.fsync(descriptor)
        } finally {
            Os.close(descriptor)
        }
    }

    private data class DatabaseFingerprint(
        val version: Int,
        val tableRowCounts: Map<String, Long>,
    )

    private companion object {
        const val MIN_FREE_MARGIN_BYTES: Long = 4L * 1024L * 1024L
    }
}
