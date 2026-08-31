package com.z_company.data_local.recovery

import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.FileOutputStream

class AndroidAttachmentsManifestExporter {
    fun export(sourceRouteDatabase: File, destination: File): ExportedRecoverySection {
        require(sourceRouteDatabase.isFile) { "Route recovery source does not exist" }
        val directory = requireNotNull(destination.parentFile)
        require(directory.isDirectory || directory.mkdirs()) { "Cannot create recovery directory" }
        val temporary = File(directory, destination.name + ".tmp")
        temporary.delete()
        val database = openReadOnlyPreservingCorruption(sourceRouteDatabase)
        try {
            require(hasTable(database, "Photo")) { "Route recovery source has no Photo table" }
            val attachments = database.rawQuery(
                "SELECT photoId, basicId, remoteObjectId, url, dateOfCreate " +
                    "FROM Photo ORDER BY photoId",
                null,
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val photoId = cursor.getString(0)
                        val url = cursor.getString(3)
                        if (!RecoveryAttachmentsManifestJson.isSafeRemoteUrl(url)) {
                            throw LocalRecoveryAttachmentRequiresContentException(photoId)
                        }
                        add(
                            RecoveryRemoteAttachmentV1(
                                photoId = photoId,
                                routeId = cursor.getString(1),
                                remoteObjectId = if (cursor.isNull(2)) null else cursor.getString(2),
                                url = url,
                                createdAt = cursor.getLong(4),
                            )
                        )
                    }
                }
            }
            val manifest = RecoveryAttachmentsManifestV1(
                RecoveryAttachmentsManifestJson.CURRENT_FORMAT_VERSION,
                attachments,
            )
            val bytes = RecoveryAttachmentsManifestJson.encode(manifest).encodeToByteArray()
            require(bytes.size <= RecoveryAttachmentsManifestJson.MAX_SECTION_BYTES) {
                "Attachments manifest exceeds size limit"
            }
            FileOutputStream(temporary).use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            val sha256 = RecoverySha256.digestHex(bytes)
            Os.rename(temporary.path, destination.path)
            syncDirectory(directory)
            return ExportedRecoverySection(destination, attachments.size.toLong(), sha256)
        } finally {
            database.close()
            temporary.delete()
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
