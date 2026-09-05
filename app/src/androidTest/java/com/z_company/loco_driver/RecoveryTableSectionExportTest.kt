package com.z_company.loco_driver

import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.z_company.data_local.recovery.AndroidTableRecoveryExporter
import com.z_company.data_local.recovery.AndroidSettingsRecoveryImporter
import com.z_company.data_local.recovery.RecoveryArchiveSectionDigest
import com.z_company.data_local.recovery.RecoverySha256
import com.z_company.data_local.recovery.RecoveryTableSectionJson
import com.z_company.data_local.recovery.RecoveryTableSectionKind
import com.z_company.data_local.recovery.RecoveryTableSectionV1
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.data_local.setting.salarydb.SalarySettingDatabase
import java.io.File
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecoveryTableSectionExportTest {
    private lateinit var schemaContext: Context
    private lateinit var fixtureContext: Context
    private lateinit var recoveryContext: Context

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        schemaContext = instrumentation.context
        recoveryContext = instrumentation.targetContext
        fixtureContext = FixtureContext(instrumentation.targetContext)
        cleanup()
    }

    @After
    fun tearDown() = cleanup()

    @Test
    fun everyArchivedSettingsDatabaseExportsDeterministically() {
        for (version in 1..14) {
            cleanup()
            createRoomFixture(SETTINGS_SCHEMA, SETTINGS_DATABASE, version)
            val source = fixtureContext.getDatabasePath(SETTINGS_DATABASE)
            val first = File(fixtureContext.filesDir, "settings-$version.json")
            val second = File(fixtureContext.filesDir, "settings-$version-copy.json")

            val firstDigest = AndroidTableRecoveryExporter().export(
                source,
                first,
                RecoveryTableSectionKind.SETTINGS,
            )
            val secondDigest = AndroidTableRecoveryExporter().export(
                source,
                second,
                RecoveryTableSectionKind.SETTINGS,
            )

            assertEquals("Settings v$version rows", 2L, firstDigest.itemCount)
            assertEquals(first.readBytes().toList(), second.readBytes().toList())
            assertEquals(RecoverySha256.digestHex(first.readBytes()), firstDigest.sha256)
            val decoded = RecoveryTableSectionJson.decodeAndValidate(
                first.readText(),
                RecoveryTableSectionKind.SETTINGS,
            )
            assertEquals(setOf("MonthOfYear", "UserSettings"), decoded.tables.keys)
            val norms = File(fixtureContext.filesDir, "norms-$version.json").apply {
                writeText(RecoveryTableSectionJson.encode(RecoveryTableSectionV1(1, emptyMap())))
            }
            val imported = AndroidSettingsRecoveryImporter(recoveryContext)
                .importSettingsAndNormsToFreshDatabase(
                    first,
                    RecoveryArchiveSectionDigest(firstDigest.itemCount, firstDigest.sha256),
                    norms,
                    RecoveryArchiveSectionDigest(
                        0L,
                        RecoverySha256.digestHex(norms.readBytes()),
                    ),
                    SETTINGS_IMPORT_DATABASE,
                )
            assertEquals(2L, imported.itemCount)
            open(imported.databaseFile).use { restored ->
                assertEquals(SettingsDatabase.Schema.version.toInt(), restored.version)
                assertEquals(1L, countRows(restored, "UserSettings"))
                assertEquals(1L, countRows(restored, "MonthOfYear"))
            }
            open(source).use { assertEquals(version, it.version) }
        }
    }

    @Test
    fun everyArchivedSalaryDatabaseExportsDeterministically() {
        for (version in 1..7) {
            cleanup()
            createRoomFixture(SALARY_SCHEMA, SALARY_DATABASE, version)
            val source = fixtureContext.getDatabasePath(SALARY_DATABASE)
            val destination = File(fixtureContext.filesDir, "salary-$version.json")

            val digest = AndroidTableRecoveryExporter().export(
                source,
                destination,
                RecoveryTableSectionKind.SALARY_SETTINGS,
            )

            assertEquals("Salary v$version rows", 1L, digest.itemCount)
            assertEquals(RecoverySha256.digestHex(destination.readBytes()), digest.sha256)
            val decoded = RecoveryTableSectionJson.decodeAndValidate(
                destination.readText(),
                RecoveryTableSectionKind.SALARY_SETTINGS,
            )
            assertEquals(setOf("SalarySetting"), decoded.tables.keys)
            val imported = AndroidSettingsRecoveryImporter(recoveryContext)
                .importSalaryToFreshDatabase(
                    destination,
                    RecoveryArchiveSectionDigest(digest.itemCount, digest.sha256),
                    SALARY_IMPORT_DATABASE,
                )
            assertEquals(1L, imported.itemCount)
            open(imported.databaseFile).use { restored ->
                assertEquals(SalarySettingDatabase.Schema.version.toInt(), restored.version)
                assertEquals(1L, countRows(restored, "SalarySetting"))
            }
            open(source).use { assertEquals(version, it.version) }
        }
    }

    @Test
    fun corruptSourceIsNotDeletedOrReplaced() {
        val source = fixtureContext.getDatabasePath(SETTINGS_DATABASE)
        val bytes = ByteArray(4_096) { (it % 239).toByte() }
        source.writeBytes(bytes)
        val destination = File(fixtureContext.filesDir, "settings.json")

        val error = runCatching {
            AndroidTableRecoveryExporter().export(
                source,
                destination,
                RecoveryTableSectionKind.SETTINGS,
            )
        }.exceptionOrNull()

        assertFalse(error == null)
        assertEquals(bytes.toList(), source.readBytes().toList())
        assertFalse(destination.exists())
        assertFalse(File(destination.path + ".tmp").exists())
    }

    private fun createRoomFixture(schemaRoot: String, databaseName: String, version: Int) {
        val schema = schemaContext.assets.open("$schemaRoot/$version.json").bufferedReader()
            .use { JSONObject(it.readText()) }
            .getJSONObject("database")
        val database = open(fixtureContext.getDatabasePath(databaseName))
        try {
            val entities = schema.getJSONArray("entities")
            for (position in 0 until entities.length()) {
                val entity = entities.getJSONObject(position)
                val table = entity.getString("tableName")
                database.execSQL(
                    entity.getString("createSql").replace("${'$'}{TABLE_NAME}", table)
                )
                insertRequiredRow(database, table)
            }
            database.version = version
        } finally {
            database.close()
        }
    }

    private fun insertRequiredRow(database: SQLiteDatabase, table: String) {
        val columns = database.rawQuery("PRAGMA table_info(`$table`)", null).use { cursor ->
            val name = cursor.getColumnIndexOrThrow("name")
            val type = cursor.getColumnIndexOrThrow("type")
            val notNull = cursor.getColumnIndexOrThrow("notnull")
            val default = cursor.getColumnIndexOrThrow("dflt_value")
            val primaryKey = cursor.getColumnIndexOrThrow("pk")
            buildList {
                while (cursor.moveToNext()) {
                    if ((cursor.getInt(notNull) == 1 || cursor.getInt(primaryKey) > 0) &&
                        cursor.isNull(default)
                    ) {
                        add(cursor.getString(name) to cursor.getString(type).uppercase())
                    }
                }
            }
        }
        val names = columns.joinToString(", ") { "`${it.first}`" }
        val placeholders = columns.joinToString(", ") { "?" }
        val values = columns.map { (name, type) ->
            when {
                type.contains("INT") -> 0L
                type.contains("REAL") || type.contains("FLOA") -> 0.0
                name.endsWith("Key") -> "fixture-key"
                name == "id" -> "fixture-id"
                name.endsWith("List") || name in setOf("days", "monthOfYear") -> "[]"
                else -> "fixture"
            }
        }.toTypedArray()
        database.execSQL("INSERT INTO `$table` ($names) VALUES ($placeholders)", values)
    }

    private fun open(file: File): SQLiteDatabase =
        SQLiteDatabase.openOrCreateDatabase(file, null)

    private fun countRows(database: SQLiteDatabase, table: String): Long =
        database.rawQuery("SELECT count(*) FROM `$table`", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }

    private fun cleanup() {
        if (!::fixtureContext.isInitialized) return
        fixtureContext.deleteDatabase(SETTINGS_DATABASE)
        fixtureContext.deleteDatabase(SALARY_DATABASE)
        if (::recoveryContext.isInitialized) {
            recoveryContext.deleteDatabase(SETTINGS_IMPORT_DATABASE)
            recoveryContext.deleteDatabase(SALARY_IMPORT_DATABASE)
        }
        fixtureContext.filesDir.listFiles()?.forEach(File::delete)
    }

    private companion object {
        const val SETTINGS_DATABASE = "Settings.fixture.db"
        const val SALARY_DATABASE = "SalarySetting.fixture.db"
        const val SETTINGS_IMPORT_DATABASE = "Settings.recovery.fixture.import.db"
        const val SALARY_IMPORT_DATABASE = "SalarySetting.recovery.fixture.import.db"
        const val SETTINGS_SCHEMA = "com.z_company.data_local.setting.data_base.SettingsDB"
        const val SALARY_SCHEMA = "com.z_company.data_local.setting.data_base.SalarySettingDB"
    }

    private class FixtureContext(base: Context) : ContextWrapper(base) {
        private val root = File(base.cacheDir, "recovery_table_fixture").apply { mkdirs() }
        private val files = File(root, "files").apply { mkdirs() }

        override fun getDatabasePath(name: String): File = File(root, name)
        override fun getFilesDir(): File = files
        override fun deleteDatabase(name: String): Boolean {
            val database = getDatabasePath(name)
            val deleted = !database.exists() || database.delete()
            File(database.path + "-wal").delete()
            File(database.path + "-shm").delete()
            return deleted
        }
    }
}
