package com.z_company.loco_driver

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.z_company.data_local.recovery.AndroidRecoveryArchiveAssembler
import com.z_company.data_local.recovery.AndroidFrozenDatabaseSnapshotter
import com.z_company.data_local.recovery.RecoveryArchiveJson
import com.z_company.data_local.recovery.RecoveryArchiveMetadata
import com.z_company.data_local.recovery.RecoveryArchiveSectionDigest
import com.z_company.data_local.recovery.RecoveryArchiveValidator
import com.z_company.data_local.recovery.RecoveryAttachmentsManifestJson
import com.z_company.data_local.recovery.RecoverySha256
import com.z_company.data_local.recovery.RecoveryTableSectionJson
import com.z_company.data_local.recovery.RecoveryTableSectionKind
import android.database.sqlite.SQLiteDatabase
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
class RecoveryArchiveAssemblerTest {
    private lateinit var context: Context
    private lateinit var schemaContext: Context
    private lateinit var archive: File
    private lateinit var frozenDirectory: File

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        schemaContext = InstrumentationRegistry.getInstrumentation().context
        archive = File(context.cacheDir, "complete-recovery-archive")
        frozenDirectory = File(context.cacheDir, "recovery-frozen-fixture")
        cleanup()
    }

    @After
    fun tearDown() = cleanup()

    @Test
    fun frozenSnapshotsPublishOnlyAfterEverySectionValidates() {
        createEmptySnapshots()
        val snapshotter = AndroidFrozenDatabaseSnapshotter()
        val routeSnapshot = snapshotter.snapshot(
            context.getDatabasePath(ROUTE_SOURCE),
            File(frozenDirectory, "Route.snapshot.db"),
        )
        val settingsSnapshot = snapshotter.snapshot(
            context.getDatabasePath(SETTINGS_SOURCE),
            File(frozenDirectory, "Settings.snapshot.db"),
        )
        val salarySnapshot = snapshotter.snapshot(
            context.getDatabasePath(SALARY_SOURCE),
            File(frozenDirectory, "Salary.snapshot.db"),
        )

        val manifest = AndroidRecoveryArchiveAssembler().assemble(
            routeSnapshot = routeSnapshot.file,
            settingsSnapshot = settingsSnapshot.file,
            salarySnapshot = salarySnapshot.file,
            destinationDirectory = archive,
            metadata = RecoveryArchiveMetadata(
                appBuild = 9_999,
                createdAt = 1_800_000_000_000L,
                installationId = "fixture-installation",
            ),
        )

        assertTrue(archive.isDirectory)
        assertFalse(File(archive.parentFile, archive.name + ".building").exists())
        val decodedManifest = RecoveryArchiveJson.decodeManifest(
            File(archive, "manifest.json").readText()
        )
        assertEquals(manifest, decodedManifest)
        val actual = decodedManifest.sections.associate { section ->
            val file = File(archive, section.name)
            section.name to RecoveryArchiveSectionDigest(
                section.itemCount,
                RecoverySha256.digestHex(file.readBytes()),
            )
        }
        RecoveryArchiveValidator.validatePayloadDigests(decodedManifest, actual)
        assertTrue(File(archive, "routes.ndjson").readText().isEmpty())
        RecoveryTableSectionJson.decodeAndValidate(
            File(archive, "settings.json").readText(),
            RecoveryTableSectionKind.SETTINGS,
        )
        RecoveryTableSectionJson.decodeAndValidate(
            File(archive, "salary-settings.json").readText(),
            RecoveryTableSectionKind.SALARY_SETTINGS,
        )
        RecoveryTableSectionJson.decodeAndValidate(
            File(archive, "norms.json").readText(),
            RecoveryTableSectionKind.NORMS,
        )
        RecoveryAttachmentsManifestJson.decodeAndValidate(
            File(archive, "attachments-manifest.json").readText()
        )
    }

    @Test
    fun committedWalRowsAreCheckpointedIntoFrozenSnapshot() {
        val source = context.getDatabasePath(WAL_SOURCE)
        val destination = File(frozenDirectory, "Wal.snapshot.db")
        SQLiteDatabase.openOrCreateDatabase(source, null).use { database ->
            database.execSQL("CREATE TABLE Item(id TEXT NOT NULL PRIMARY KEY)")
            database.version = 3
            database.rawQuery("PRAGMA journal_mode=WAL", null).use { it.moveToFirst() }
            database.rawQuery("PRAGMA wal_autocheckpoint=0", null).use { it.moveToFirst() }
            database.execSQL("INSERT INTO Item(id) VALUES ('committed-in-wal')")
            assertTrue(File(source.path + "-wal").length() > 0L)
        }

        val snapshot = AndroidFrozenDatabaseSnapshotter().snapshot(source, destination)

        assertEquals(3, snapshot.sourceVersion)
        assertEquals(1L, snapshot.tableRowCounts.getValue("Item"))
        SQLiteDatabase.openDatabase(
            destination.path,
            null,
            SQLiteDatabase.OPEN_READONLY,
        ).use { frozen ->
            assertEquals(1L, frozen.rawQuery("SELECT count(*) FROM Item", null).use { cursor ->
                cursor.moveToFirst()
                cursor.getLong(0)
            })
        }
    }

    @Test
    fun corruptSourceCannotFinalizeFrozenSnapshot() {
        val source = context.getDatabasePath(CORRUPT_SOURCE)
        val bytes = ByteArray(4_096) { (it % 241).toByte() }
        source.writeBytes(bytes)
        val destination = File(frozenDirectory, "Corrupt.snapshot.db")

        val error = runCatching {
            AndroidFrozenDatabaseSnapshotter().snapshot(source, destination)
        }.exceptionOrNull()

        assertTrue(error != null)
        assertEquals(bytes.toList(), source.readBytes().toList())
        assertFalse(destination.exists())
        assertFalse(File(destination.path + ".tmp").exists())
    }

    private fun createEmptySnapshots() {
        createRoomSnapshot(
            "com.z_company.data_local.route.data_base.RouteDB/12.json",
            ROUTE_SOURCE,
            12,
        )
        createRoomSnapshot(
            "com.z_company.data_local.setting.data_base.SettingsDB/14.json",
            SETTINGS_SOURCE,
            14,
        )
        createRoomSnapshot(
            "com.z_company.data_local.setting.data_base.SalarySettingDB/7.json",
            SALARY_SOURCE,
            7,
        )
    }

    private fun createRoomSnapshot(asset: String, databaseName: String, version: Int) {
        val schema = schemaContext.assets.open(asset).bufferedReader()
            .use { JSONObject(it.readText()) }
            .getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(databaseName), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (position in 0 until entities.length()) {
                val entity = entities.getJSONObject(position)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("${'$'}{TABLE_NAME}", table))
            }
            db.version = version
        }
    }

    private fun cleanup() {
        if (::context.isInitialized) {
            context.deleteDatabase(ROUTE_SOURCE)
            context.deleteDatabase(SETTINGS_SOURCE)
            context.deleteDatabase(SALARY_SOURCE)
            context.deleteDatabase(WAL_SOURCE)
            context.deleteDatabase(CORRUPT_SOURCE)
        }
        if (::archive.isInitialized) {
            archive.deleteRecursively()
            File(archive.parentFile, archive.name + ".building").deleteRecursively()
        }
        if (::frozenDirectory.isInitialized) frozenDirectory.deleteRecursively()
    }

    private companion object {
        const val ROUTE_SOURCE = "Route.recovery.fixture.source.db"
        const val SETTINGS_SOURCE = "Settings.recovery.fixture.source.db"
        const val SALARY_SOURCE = "Salary.recovery.fixture.source.db"
        const val WAL_SOURCE = "Wal.recovery.fixture.source.db"
        const val CORRUPT_SOURCE = "Corrupt.recovery.fixture.source.db"
    }
}
