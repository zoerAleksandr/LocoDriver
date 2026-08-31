package com.z_company.loco_driver

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.z_company.data_local.recovery.AndroidRecoveryArchiveAssembler
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

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        schemaContext = InstrumentationRegistry.getInstrumentation().context
        archive = File(context.cacheDir, "complete-recovery-archive")
        cleanup()
    }

    @After
    fun tearDown() = cleanup()

    @Test
    fun frozenSnapshotsPublishOnlyAfterEverySectionValidates() {
        createEmptySnapshots()

        val manifest = AndroidRecoveryArchiveAssembler().assemble(
            routeSnapshot = context.getDatabasePath(ROUTE_SNAPSHOT),
            settingsSnapshot = context.getDatabasePath(SETTINGS_SNAPSHOT),
            salarySnapshot = context.getDatabasePath(SALARY_SNAPSHOT),
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

    private fun createEmptySnapshots() {
        createRoomSnapshot(
            "com.z_company.data_local.route.data_base.RouteDB/12.json",
            ROUTE_SNAPSHOT,
            12,
        )
        createRoomSnapshot(
            "com.z_company.data_local.setting.data_base.SettingsDB/14.json",
            SETTINGS_SNAPSHOT,
            14,
        )
        createRoomSnapshot(
            "com.z_company.data_local.setting.data_base.SalarySettingDB/7.json",
            SALARY_SNAPSHOT,
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
            context.deleteDatabase(ROUTE_SNAPSHOT)
            context.deleteDatabase(SETTINGS_SNAPSHOT)
            context.deleteDatabase(SALARY_SNAPSHOT)
        }
        if (::archive.isInitialized) {
            archive.deleteRecursively()
            File(archive.parentFile, archive.name + ".building").deleteRecursively()
        }
    }

    private companion object {
        const val ROUTE_SNAPSHOT = "Route.recovery.fixture.snapshot.db"
        const val SETTINGS_SNAPSHOT = "Settings.recovery.fixture.snapshot.db"
        const val SALARY_SNAPSHOT = "Salary.recovery.fixture.snapshot.db"
    }
}
