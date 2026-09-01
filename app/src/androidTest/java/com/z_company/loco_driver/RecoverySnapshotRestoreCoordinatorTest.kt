package com.z_company.loco_driver

import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.z_company.data_local.recovery.AndroidFrozenDatabaseSnapshotter
import com.z_company.data_local.recovery.AndroidRecoveryArchiveAssembler
import com.z_company.data_local.recovery.AndroidRecoveryArchiveBundle
import com.z_company.data_local.recovery.RecoveryArchiveMetadata
import com.z_company.loco_driver.recovery.RecoverySnapshotRestoreCoordinator
import com.z_company.repository.remote_rest.recovery.RecoveryCloudClient
import com.z_company.repository.remote_rest.recovery.RecoverySnapshotResponse
import java.io.File
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecoverySnapshotRestoreCoordinatorTest {
    private lateinit var schemaContext: Context
    private lateinit var fixtureContext: FixtureContext

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        schemaContext = instrumentation.context
        fixtureContext = FixtureContext(instrumentation.targetContext)
        fixtureContext.clear()
    }

    @After
    fun tearDown() = fixtureContext.clear()

    @Test
    fun verifiedCloudBundleReplacesAllDatabasesAndClearsOnlyDisposableFiles() {
        createLiveDatabase(ROUTE_SCHEMA, ROUTE_DATABASE, 12)
        createLiveDatabase(SETTINGS_SCHEMA, SETTINGS_DATABASE, 14)
        createLiveDatabase(SALARY_SCHEMA, SALARY_DATABASE, 7)
        val frozen = File(fixtureContext.cacheDir, "frozen").apply { mkdirs() }
        val snapshotter = AndroidFrozenDatabaseSnapshotter()
        val archive = File(fixtureContext.cacheDir, "server-archive")
        AndroidRecoveryArchiveAssembler().assemble(
            snapshotter.snapshot(
                fixtureContext.getDatabasePath(ROUTE_DATABASE),
                File(frozen, "Route.snapshot.db"),
            ).file,
            snapshotter.snapshot(
                fixtureContext.getDatabasePath(SETTINGS_DATABASE),
                File(frozen, "Settings.snapshot.db"),
            ).file,
            snapshotter.snapshot(
                fixtureContext.getDatabasePath(SALARY_DATABASE),
                File(frozen, "Salary.snapshot.db"),
            ).file,
            archive,
            RecoveryArchiveMetadata(9_999, 1_800_000_000_000L, "restore-fixture"),
        )
        val serverBundle = File(fixtureContext.cacheDir, "server.bundle")
        val bundler = AndroidRecoveryArchiveBundle()
        bundler.pack(archive, serverBundle)
        val response = RecoverySnapshotResponse(
            "snapshot-fixture",
            "READY",
            1,
            12,
            9_999,
            serverBundle.length(),
            bundler.sha256(serverBundle),
            1_800_000_000_000L,
            1_800_000_000_001L,
        )
        val cloud = CopyingCloudClient(response, serverBundle)

        val result = RecoverySnapshotRestoreCoordinator(fixtureContext, cloud)
            .restoreLatest("test-token")

        assertEquals("snapshot-fixture", result.snapshotId)
        assertEquals("Bearer test-token", cloud.latestAuthorization)
        assertEquals("Bearer test-token", cloud.downloadAuthorization)
        assertFalse(hasTable(ROUTE_DATABASE, OLD_ONLY_TABLE))
        assertFalse(hasTable(SETTINGS_DATABASE, OLD_ONLY_TABLE))
        assertFalse(hasTable(SALARY_DATABASE, OLD_ONLY_TABLE))
        assertFalse(File(fixtureContext.filesDir, "data_safety/recovery/cloud-restore-working").exists())
        assertFalse(File(fixtureContext.filesDir, "data_safety/recovery/install-journal.json").exists())
        assertTrue(
            File(fixtureContext.filesDir, "data_safety/recovery").listFiles()
                ?.any { it.name.startsWith("install-backup-") } == true
        )
    }

    @Test
    fun checksumMismatchNeverTouchesLiveDatabasesAndClearsDownloadedData() {
        createAllLiveDatabases()
        val downloaded = File(fixtureContext.cacheDir, "corrupted.bundle")
            .apply { writeBytes("not-a-recovery-archive".encodeToByteArray()) }
        val response = validResponse(
            snapshotId = "snapshot-corrupted",
            sizeBytes = downloaded.length(),
            sha256 = "0".repeat(64),
        )

        val error = runCatching {
            RecoverySnapshotRestoreCoordinator(
                fixtureContext,
                CopyingCloudClient(response, downloaded),
            ).restoreLatest("test-token")
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertAllLiveMarkersPresent()
        assertFalse(File(fixtureContext.filesDir, "data_safety/recovery/cloud-restore-working").exists())
        assertFalse(File(fixtureContext.filesDir, "data_safety/recovery/install-journal.json").exists())
    }

    @Test
    fun unsafeSnapshotIdIsRejectedBeforeDownloadAndNeverTouchesLiveDatabases() {
        createAllLiveDatabases()
        val response = validResponse(snapshotId = "../../another-account")
        val cloud = CopyingCloudClient(response, File(fixtureContext.cacheDir, "unused"))

        val error = runCatching {
            RecoverySnapshotRestoreCoordinator(fixtureContext, cloud)
                .restoreLatest("test-token")
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals(null, cloud.downloadAuthorization)
        assertAllLiveMarkersPresent()
        assertFalse(File(fixtureContext.filesDir, "data_safety/recovery/cloud-restore-working").exists())
    }

    private fun createAllLiveDatabases() {
        createLiveDatabase(ROUTE_SCHEMA, ROUTE_DATABASE, 12)
        createLiveDatabase(SETTINGS_SCHEMA, SETTINGS_DATABASE, 14)
        createLiveDatabase(SALARY_SCHEMA, SALARY_DATABASE, 7)
    }

    private fun assertAllLiveMarkersPresent() {
        assertTrue(hasTable(ROUTE_DATABASE, OLD_ONLY_TABLE))
        assertTrue(hasTable(SETTINGS_DATABASE, OLD_ONLY_TABLE))
        assertTrue(hasTable(SALARY_DATABASE, OLD_ONLY_TABLE))
    }

    private fun validResponse(
        snapshotId: String,
        sizeBytes: Long = 1L,
        sha256: String = "0".repeat(64),
    ) = RecoverySnapshotResponse(
        snapshotId = snapshotId,
        status = "READY",
        archiveFormatVersion = 1,
        sourceDbVersion = 12,
        appBuild = 9_999,
        sizeBytes = sizeBytes,
        sha256 = sha256,
        createdAt = 1_800_000_000_000L,
        completedAt = 1_800_000_000_001L,
    )

    private fun createLiveDatabase(schemaRoot: String, name: String, version: Int) {
        val schema = schemaContext.assets.open("$schemaRoot/$version.json").bufferedReader()
            .use { JSONObject(it.readText()) }
            .getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(fixtureContext.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (position in 0 until entities.length()) {
                val entity = entities.getJSONObject(position)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("${'$'}{TABLE_NAME}", table))
            }
            db.execSQL("CREATE TABLE $OLD_ONLY_TABLE(value TEXT)")
            db.execSQL("INSERT INTO $OLD_ONLY_TABLE(value) VALUES ('must-not-survive')")
            db.version = version
        }
    }

    private fun hasTable(databaseName: String, table: String): Boolean =
        SQLiteDatabase.openDatabase(
            fixtureContext.getDatabasePath(databaseName).path,
            null,
            SQLiteDatabase.OPEN_READONLY,
        ).use { database ->
            database.rawQuery(
                "SELECT count(*) FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(table),
            ).use { cursor -> cursor.moveToFirst() && cursor.getInt(0) == 1 }
        }

    private class CopyingCloudClient(
        private val response: RecoverySnapshotResponse,
        private val source: File,
    ) : RecoveryCloudClient {
        var latestAuthorization: String? = null
        var downloadAuthorization: String? = null

        override fun latest(token: String): RecoverySnapshotResponse {
            latestAuthorization = token
            return response
        }

        override fun download(
            token: String,
            snapshot: RecoverySnapshotResponse,
            destination: File,
        ): File {
            downloadAuthorization = token
            require(snapshot == response)
            source.copyTo(destination)
            return destination
        }
    }

    private companion object {
        const val ROUTE_DATABASE = "Route.db"
        const val SETTINGS_DATABASE = "Settings.db"
        const val SALARY_DATABASE = "SalarySetting.db"
        const val ROUTE_SCHEMA = "com.z_company.data_local.route.data_base.RouteDB"
        const val SETTINGS_SCHEMA = "com.z_company.data_local.setting.data_base.SettingsDB"
        const val SALARY_SCHEMA = "com.z_company.data_local.setting.data_base.SalarySettingDB"
        const val OLD_ONLY_TABLE = "RecoveryOldOnly"
    }

    private class FixtureContext(base: Context) : ContextWrapper(base) {
        private val root = File(base.cacheDir, "recovery_snapshot_restore_fixture")
        private val databases = File(root, "databases")
        private val files = File(root, "files")
        private val cache = File(root, "cache")

        init {
            resetDirectories()
        }

        override fun getDatabasePath(name: String): File = File(databases, name)
        override fun getFilesDir(): File = files
        override fun getCacheDir(): File = cache

        fun clear() {
            root.deleteRecursively()
            resetDirectories()
        }

        private fun resetDirectories() {
            databases.mkdirs()
            files.mkdirs()
            cache.mkdirs()
        }
    }
}
