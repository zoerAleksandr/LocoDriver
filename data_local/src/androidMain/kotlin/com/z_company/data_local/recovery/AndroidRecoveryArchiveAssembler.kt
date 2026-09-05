package com.z_company.data_local.recovery

import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.FileOutputStream

data class RecoveryArchiveMetadata(
    val appBuild: Int,
    val createdAt: Long,
    val installationId: String,
    val accountIdHash: String? = null,
)

/**
 * Assembles an archive from frozen SQLite snapshots. Callers must checkpoint, close and copy the
 * live databases before invoking this class; accepting live database paths here would allow
 * routes and attachment metadata to come from different points in time.
 */
class AndroidRecoveryArchiveAssembler {
    fun assemble(
        routeSnapshot: File,
        settingsSnapshot: File,
        salarySnapshot: File,
        destinationDirectory: File,
        metadata: RecoveryArchiveMetadata,
    ): RecoveryArchiveManifest {
        require(routeSnapshot.isFile && settingsSnapshot.isFile && salarySnapshot.isFile) {
            "Recovery archive requires frozen database snapshots"
        }
        require(!destinationDirectory.exists()) { "Recovery archive destination already exists" }
        val parent = requireNotNull(destinationDirectory.parentFile)
        require(parent.isDirectory || parent.mkdirs()) { "Cannot create recovery archive parent" }
        val staging = File(parent, destinationDirectory.name + ".building")
        require(!staging.exists()) { "Recovery archive staging directory already exists" }
        require(staging.mkdir()) { "Cannot create recovery archive staging directory" }

        try {
            val exported = linkedMapOf<String, ExportedRecoverySection>()
            exported["routes.ndjson"] = AndroidRouteRecoveryExporter().export(
                routeSnapshot,
                File(staging, "routes.ndjson"),
            )
            exported["settings.json"] = AndroidTableRecoveryExporter().export(
                settingsSnapshot,
                File(staging, "settings.json"),
                RecoveryTableSectionKind.SETTINGS,
            )
            exported["salary-settings.json"] = AndroidTableRecoveryExporter().export(
                salarySnapshot,
                File(staging, "salary-settings.json"),
                RecoveryTableSectionKind.SALARY_SETTINGS,
            )
            exported["norms.json"] = AndroidTableRecoveryExporter().export(
                settingsSnapshot,
                File(staging, "norms.json"),
                RecoveryTableSectionKind.NORMS,
            )
            exported["attachments-manifest.json"] = AndroidAttachmentsManifestExporter().export(
                routeSnapshot,
                File(staging, "attachments-manifest.json"),
            )

            val manifest = RecoveryArchiveManifest(
                archiveFormatVersion = RecoveryArchiveContract.CURRENT_FORMAT_VERSION,
                sourceDbVersion = databaseVersion(routeSnapshot),
                appBuild = metadata.appBuild,
                createdAt = metadata.createdAt,
                installationId = metadata.installationId,
                accountIdHash = metadata.accountIdHash,
                sections = exported.map { (name, section) ->
                    RecoveryArchiveSection(name, section.itemCount, section.sha256)
                },
            )
            val actual = exported.mapValues { (_, section) ->
                RecoveryArchiveSectionDigest(section.itemCount, section.sha256)
            }
            RecoveryArchiveValidator.validatePayloadDigests(manifest, actual)
            val manifestBytes = RecoveryArchiveJson.encodeManifest(manifest).encodeToByteArray()
            require(manifestBytes.size <= RecoveryArchiveContract.MAX_MANIFEST_BYTES) {
                "Recovery archive manifest exceeds size limit"
            }
            FileOutputStream(File(staging, "manifest.json")).use { output ->
                output.write(manifestBytes)
                output.fd.sync()
            }
            syncDirectory(staging)
            Os.rename(staging.path, destinationDirectory.path)
            syncDirectory(parent)
            return manifest
        } catch (error: Throwable) {
            staging.deleteRecursively()
            throw error
        }
    }

    private fun databaseVersion(file: File): Int =
        SQLiteDatabase.openDatabase(
            file.path,
            null,
            SQLiteDatabase.OPEN_READONLY,
            DatabaseErrorHandler { },
        ).use { it.version }

    private fun syncDirectory(directory: File) {
        val descriptor = Os.open(directory.path, OsConstants.O_RDONLY, 0)
        try {
            Os.fsync(descriptor)
        } finally {
            Os.close(descriptor)
        }
    }
}
