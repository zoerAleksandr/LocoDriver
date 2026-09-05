package com.z_company.data_local.recovery

import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.system.Os
import android.system.OsConstants
import android.util.Base64
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
            val rows = database.rawQuery(
                "SELECT photoId, basicId, remoteObjectId, url, dateOfCreate " +
                    "FROM Photo ORDER BY photoId",
                null,
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val photoId = cursor.getString(0)
                        val url = cursor.getString(3)
                        add(PhotoRow(photoId, cursor.getString(1), if (cursor.isNull(2)) null else cursor.getString(2), url, cursor.getLong(4)))
                    }
                }
            }
            val attachments = rows.mapNotNull { row ->
                if (RecoveryAttachmentsManifestJson.isSafeRemoteUrl(row.url)) {
                    RecoveryRemoteAttachmentV1(row.photoId, row.routeId, row.remoteObjectId, row.url, row.createdAt)
                } else null
            }
            val embedded = rows.mapNotNull { row ->
                if (RecoveryAttachmentsManifestJson.isSafeRemoteUrl(row.url)) return@mapNotNull null
                val bytes = decodeLegacyBase64(row.url)
                    ?: throw LocalRecoveryAttachmentRequiresContentException(row.photoId)
                require(bytes.isNotEmpty() && bytes.size <= RecoveryAttachmentsManifestJson.MAX_EMBEDDED_ATTACHMENT_BYTES) {
                    "Embedded recovery attachment exceeds size limit"
                }
                RecoveryEmbeddedAttachmentV1(
                    row.photoId,
                    row.routeId,
                    "base64",
                    bytes.size.toLong(),
                    RecoverySha256.digestHex(bytes),
                    row.createdAt,
                )
            }
            val manifest = RecoveryAttachmentsManifestV1(
                RecoveryAttachmentsManifestJson.CURRENT_FORMAT_VERSION,
                attachments,
                embedded,
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
            return ExportedRecoverySection(destination, rows.size.toLong(), sha256)
        } finally {
            database.close()
            temporary.delete()
        }
    }

    private fun decodeLegacyBase64(value: String): ByteArray? {
        if (
            value.length !in 4..MAX_BASE64_CHARACTERS ||
            value.length % 4 != 0 ||
            !BASE64.matches(value)
        ) return null
        return runCatching { Base64.decode(value, Base64.NO_WRAP) }.getOrNull()
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

    private data class PhotoRow(
        val photoId: String,
        val routeId: String,
        val remoteObjectId: String?,
        val url: String,
        val createdAt: Long,
    )

    private companion object {
        val BASE64 = Regex("[A-Za-z0-9+/]+={0,2}")
        const val MAX_BASE64_CHARACTERS = 27_962_032
    }
}
