package com.z_company.data_local.recovery

import android.content.Context
import android.system.Os
import android.system.OsConstants
import com.z_company.data_local.RouteDatabaseProfileDetector
import com.z_company.data_local.route.db.RouteDatabase
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom

enum class PreMigrationRecoveryState {
    SKIPPED,
    RAW_SNAPSHOTS_READY,
    ARCHIVE_READY,
}

data class PreMigrationRecoveryResult(
    val state: PreMigrationRecoveryState,
    val rawSnapshotDirectory: File? = null,
    val archiveDirectory: File? = null,
    val archiveErrorCode: String? = null,
)

/** Runs only during bootstrap, before Koin exposes any database drivers. */
class AndroidPreMigrationRecoveryCoordinator(
    private val context: Context,
    private val appBuild: Int,
) {
    fun prepareIfNeeded(): PreMigrationRecoveryResult {
        val route = context.getDatabasePath(ROUTE_DATABASE)
        if (!route.isFile) return PreMigrationRecoveryResult(PreMigrationRecoveryState.SKIPPED)
        val profile = RouteDatabaseProfileDetector().detect(route)
        if (!requiresMigration(profile)) {
            return PreMigrationRecoveryResult(PreMigrationRecoveryState.SKIPPED)
        }

        val root = File(context.filesDir, RECOVERY_ROOT).apply { mkdirs() }
        require(root.isDirectory) { "Cannot create pre-migration recovery directory" }
        val rawDirectory = File(root, "raw-$appBuild-${profile.userVersion}")
        val settings = context.getDatabasePath(SETTINGS_DATABASE)
        val salary = context.getDatabasePath(SALARY_DATABASE)
        val expectedSnapshots = buildList {
            add(ROUTE_SNAPSHOT)
            if (settings.isFile) add(SETTINGS_SNAPSHOT)
            if (salary.isFile) add(SALARY_SNAPSHOT)
        }

        if (!isValidRawDirectory(rawDirectory, expectedSnapshots)) {
            if (rawDirectory.exists()) quarantineInvalidRawDirectory(rawDirectory)
            createRawSnapshotsAtomically(route, settings, salary, rawDirectory)
        }

        if (!settings.isFile || !salary.isFile) {
            recordStatus(PreMigrationRecoveryState.RAW_SNAPSHOTS_READY, "MISSING_OPTIONAL_DB")
            return PreMigrationRecoveryResult(
                PreMigrationRecoveryState.RAW_SNAPSHOTS_READY,
                rawSnapshotDirectory = rawDirectory,
                archiveErrorCode = "MISSING_OPTIONAL_DB",
            )
        }

        val archive = File(root, "archive-$appBuild-${profile.userVersion}")
        if (archive.isDirectory) {
            recordStatus(PreMigrationRecoveryState.ARCHIVE_READY, null)
            return PreMigrationRecoveryResult(
                PreMigrationRecoveryState.ARCHIVE_READY,
                rawDirectory,
                archive,
            )
        }
        return try {
            AndroidRecoveryArchiveAssembler().assemble(
                routeSnapshot = File(rawDirectory, ROUTE_SNAPSHOT),
                settingsSnapshot = File(rawDirectory, SETTINGS_SNAPSHOT),
                salarySnapshot = File(rawDirectory, SALARY_SNAPSHOT),
                destinationDirectory = archive,
                metadata = RecoveryArchiveMetadata(
                    appBuild = appBuild,
                    createdAt = System.currentTimeMillis(),
                    installationId = installationId(),
                ),
            )
            recordStatus(PreMigrationRecoveryState.ARCHIVE_READY, null)
            PreMigrationRecoveryResult(
                PreMigrationRecoveryState.ARCHIVE_READY,
                rawDirectory,
                archive,
            )
        } catch (error: Throwable) {
            val errorCode = error::class.simpleName ?: "ArchiveAssemblyError"
            recordStatus(PreMigrationRecoveryState.RAW_SNAPSHOTS_READY, errorCode)
            PreMigrationRecoveryResult(
                PreMigrationRecoveryState.RAW_SNAPSHOTS_READY,
                rawDirectory,
                archiveErrorCode = errorCode,
            )
        }
    }

    private fun createRawSnapshotsAtomically(
        route: File,
        settings: File,
        salary: File,
        destination: File,
    ) {
        val building = File(destination.parentFile, destination.name + ".building")
        building.deleteRecursively()
        require(building.mkdir()) { "Cannot create raw snapshot staging directory" }
        try {
            val snapshotter = AndroidFrozenDatabaseSnapshotter()
            val snapshots = buildList {
                add(snapshotter.snapshot(route, File(building, ROUTE_SNAPSHOT)))
                if (settings.isFile) {
                    add(snapshotter.snapshot(settings, File(building, SETTINGS_SNAPSHOT)))
                }
                if (salary.isFile) {
                    add(snapshotter.snapshot(salary, File(building, SALARY_SNAPSHOT)))
                }
            }
            val manifest = RecoveryRawSnapshotManifestV1(
                RecoveryRawSnapshotManifestJson.FORMAT_VERSION,
                snapshots.map { snapshot ->
                    RecoveryRawSnapshotFileV1(
                        snapshot.file.name,
                        snapshot.file.length(),
                        snapshot.sha256,
                        snapshot.sourceVersion,
                    )
                },
            )
            val manifestBytes = RecoveryRawSnapshotManifestJson.encode(manifest).encodeToByteArray()
            FileOutputStream(File(building, RAW_MANIFEST)).use { output ->
                output.write(manifestBytes)
                output.fd.sync()
            }
            syncDirectory(building)
            require(!destination.exists()) { "Raw snapshot destination already exists" }
            Os.rename(building.path, destination.path)
            syncDirectory(destination.parentFile)
        } catch (error: Throwable) {
            building.deleteRecursively()
            throw error
        }
    }

    private fun requiresMigration(profile: com.z_company.data_local.RouteDatabaseProfile): Boolean =
        profile.userVersion < RouteDatabase.Schema.version.toInt() ||
            profile.hasLegacyRoomTrain ||
            profile.hasLegacyRoomLocomotive ||
            !profile.hasTrashFields ||
            !profile.hasDiagnosticTables

    private fun isValidRawDirectory(directory: File, names: List<String>): Boolean {
        if (!directory.isDirectory) return false
        return runCatching {
            val manifestFile = File(directory, RAW_MANIFEST)
            require(manifestFile.isFile && manifestFile.length() <= RecoveryRawSnapshotManifestJson.MAX_BYTES)
            val manifest = RecoveryRawSnapshotManifestJson.decodeAndValidate(manifestFile.readText())
            require(manifest.files.map { it.name }.toSet() == names.toSet())
            val snapshotter = AndroidFrozenDatabaseSnapshotter()
            manifest.files.forEach { expected ->
                val file = File(directory, expected.name)
                val actual = snapshotter.inspect(file)
                require(file.length() == expected.sizeBytes)
                require(actual.sourceVersion == expected.databaseVersion)
                require(actual.sha256.equals(expected.sha256, ignoreCase = true))
            }
        }.isSuccess
    }

    private fun quarantineInvalidRawDirectory(directory: File) {
        val quarantine = File(
            directory.parentFile,
            directory.name + ".invalid-" + System.currentTimeMillis(),
        )
        Os.rename(directory.path, quarantine.path)
        syncDirectory(directory.parentFile)
    }

    private fun installationId(): String {
        val preferences = context.getSharedPreferences(INSTALLATION_PREFERENCES, Context.MODE_PRIVATE)
        preferences.getString(KEY_INSTALLATION_ID, null)?.let { return it }
        val bytes = ByteArray(16).also(SecureRandom()::nextBytes)
        val generated = bytes.joinToString("") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
        check(preferences.edit().putString(KEY_INSTALLATION_ID, generated).commit())
        return generated
    }

    private fun recordStatus(state: PreMigrationRecoveryState, errorCode: String?) {
        context.getSharedPreferences(STATUS_PREFERENCES, Context.MODE_PRIVATE).edit()
            .putString(KEY_STATE, state.name)
            .putString(KEY_ARCHIVE_ERROR, errorCode)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .commit()
    }

    private fun syncDirectory(directory: File?) {
        val target = requireNotNull(directory)
        val descriptor = Os.open(target.path, OsConstants.O_RDONLY, 0)
        try {
            Os.fsync(descriptor)
        } finally {
            Os.close(descriptor)
        }
    }

    private companion object {
        const val ROUTE_DATABASE = "Route.db"
        const val SETTINGS_DATABASE = "Settings.db"
        const val SALARY_DATABASE = "SalarySetting.db"
        const val ROUTE_SNAPSHOT = "Route.snapshot.db"
        const val SETTINGS_SNAPSHOT = "Settings.snapshot.db"
        const val SALARY_SNAPSHOT = "Salary.snapshot.db"
        const val RAW_MANIFEST = "raw-manifest.json"
        const val RECOVERY_ROOT = "data_safety/recovery"
        const val INSTALLATION_PREFERENCES = "recovery_installation"
        const val KEY_INSTALLATION_ID = "installation_id"
        const val STATUS_PREFERENCES = "pre_migration_recovery"
        const val KEY_STATE = "state"
        const val KEY_ARCHIVE_ERROR = "archive_error"
        const val KEY_UPDATED_AT = "updated_at"
    }
}
