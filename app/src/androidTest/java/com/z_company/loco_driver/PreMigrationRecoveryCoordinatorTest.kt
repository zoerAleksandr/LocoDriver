package com.z_company.loco_driver

import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.z_company.data_local.recovery.AndroidPreMigrationRecoveryCoordinator
import com.z_company.data_local.recovery.PreMigrationRecoveryState
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
class PreMigrationRecoveryCoordinatorTest {
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
    fun allOldDatabasesProduceReusableCompleteArchiveBeforeMigration() {
        createRoomDatabase(ROUTE_SCHEMA, ROUTE_DATABASE, 12)
        createRoomDatabase(SETTINGS_SCHEMA, SETTINGS_DATABASE, 14)
        createRoomDatabase(SALARY_SCHEMA, SALARY_DATABASE, 7)

        val first = AndroidPreMigrationRecoveryCoordinator(fixtureContext, 9_001)
            .prepareIfNeeded()
        val second = AndroidPreMigrationRecoveryCoordinator(fixtureContext, 9_001)
            .prepareIfNeeded()

        assertEquals(PreMigrationRecoveryState.ARCHIVE_READY, first.state)
        assertEquals(first.rawSnapshotDirectory, second.rawSnapshotDirectory)
        assertEquals(first.archiveDirectory, second.archiveDirectory)
        assertTrue(first.archiveDirectory?.resolve("manifest.json")?.isFile == true)
        assertTrue(first.rawSnapshotDirectory?.resolve("Route.snapshot.db")?.isFile == true)
        assertTrue(first.rawSnapshotDirectory?.resolve("Settings.snapshot.db")?.isFile == true)
        assertTrue(first.rawSnapshotDirectory?.resolve("Salary.snapshot.db")?.isFile == true)
        assertEquals(12, version(ROUTE_DATABASE))
        assertEquals(14, version(SETTINGS_DATABASE))
        assertEquals(7, version(SALARY_DATABASE))
    }

    @Test
    fun missingOptionalDatabasesStillPreserveRouteSnapshot() {
        createRoomDatabase(ROUTE_SCHEMA, ROUTE_DATABASE, 12)

        val result = AndroidPreMigrationRecoveryCoordinator(fixtureContext, 9_002)
            .prepareIfNeeded()

        assertEquals(PreMigrationRecoveryState.RAW_SNAPSHOTS_READY, result.state)
        assertEquals("MISSING_OPTIONAL_DB", result.archiveErrorCode)
        assertTrue(result.rawSnapshotDirectory?.resolve("Route.snapshot.db")?.isFile == true)
        assertFalse(result.archiveDirectory?.exists() == true)
        assertEquals(12, version(ROUTE_DATABASE))
    }

    private fun createRoomDatabase(schemaRoot: String, name: String, version: Int) {
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
            db.version = version
        }
    }

    private fun version(name: String): Int = SQLiteDatabase.openDatabase(
        fixtureContext.getDatabasePath(name).path,
        null,
        SQLiteDatabase.OPEN_READONLY,
    ).use { it.version }

    private companion object {
        const val ROUTE_DATABASE = "Route.db"
        const val SETTINGS_DATABASE = "Settings.db"
        const val SALARY_DATABASE = "SalarySetting.db"
        const val ROUTE_SCHEMA = "com.z_company.data_local.route.data_base.RouteDB"
        const val SETTINGS_SCHEMA = "com.z_company.data_local.setting.data_base.SettingsDB"
        const val SALARY_SCHEMA = "com.z_company.data_local.setting.data_base.SalarySettingDB"
    }

    private class FixtureContext(base: Context) : ContextWrapper(base) {
        private val root = File(base.cacheDir, "pre_migration_recovery_fixture")
        private val databases = File(root, "databases")
        private val files = File(root, "files")

        init {
            databases.mkdirs()
            files.mkdirs()
        }

        override fun getDatabasePath(name: String): File = File(databases, name)
        override fun getFilesDir(): File = files
        override fun getSharedPreferences(name: String, mode: Int) =
            baseContext.getSharedPreferences("pre_migration_fixture_$name", mode)

        fun clear() {
            root.deleteRecursively()
            databases.mkdirs()
            files.mkdirs()
            listOf("recovery_installation", "pre_migration_recovery").forEach { name ->
                getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
            }
        }
    }
}
