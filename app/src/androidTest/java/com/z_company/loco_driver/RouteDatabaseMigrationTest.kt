package com.z_company.loco_driver

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.z_company.data_local.DatabaseDriverFactory
import com.z_company.data_local.RouteDatabaseProfileDetector
import com.z_company.data_local.route.db.RouteDatabase
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RouteDatabaseMigrationTest {
    /** Fixture context redirects every mutable migration artifact away from the app database. */
    private lateinit var isolatedContext: Context
    private lateinit var schemaContext: Context

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        schemaContext = instrumentation.context
        isolatedContext = FixtureContext(instrumentation.targetContext)
        check(
            isolatedContext.getDatabasePath(DATABASE_NAME).canonicalPath !=
                instrumentation.targetContext.getDatabasePath(DATABASE_NAME).canonicalPath
        )
        deleteFixtureDatabase()
    }

    @After
    fun tearDown() {
        deleteFixtureDatabase()
    }

    @Test
    fun roomV12FixtureMigratesWithoutChangingRouteIdentity() {
        createFromRoomSchema(version = 12)
        val before = openDatabase().use { db -> routeIds(db) }
        val roomProfile = RouteDatabaseProfileDetector().detect(
            isolatedContext.getDatabasePath(DATABASE_NAME)
        )
        assertTrue(roomProfile.isRecognized)
        assertTrue(roomProfile.hasLegacyRoomTrain)
        assertTrue(roomProfile.hasLegacyRoomLocomotive)
        assertFalse(roomProfile.hasTrashFields)

        DatabaseDriverFactory(isolatedContext).createRouteDriver().close()

        val migratedProfile = RouteDatabaseProfileDetector().detect(
            isolatedContext.getDatabasePath(DATABASE_NAME)
        )
        assertTrue("Unexpected migrated profile: $migratedProfile", migratedProfile.isRecognized)
        assertEquals(RouteDatabase.Schema.version.toInt(), migratedProfile.userVersion)
        assertFalse(migratedProfile.hasLegacyRoomTrain)
        assertFalse(migratedProfile.hasLegacyRoomLocomotive)
        assertTrue(migratedProfile.hasTrashFields)
        assertTrue(migratedProfile.hasDiagnosticTables)

        openDatabase().use { migrated ->
            assertEquals(RouteDatabase.Schema.version.toInt(), migrated.version)
            assertEquals(before, routeIds(migrated))
            assertTrue(hasColumn(migrated, "BasicData", "remoteDeletionPending"))
            assertFalse(hasColumn(migrated, "Train", "remoteObjectId"))
            assertFalse(hasColumn(migrated, "Locomotive", "removeObjectId"))
            assertTrue(hasTable(migrated, "RouteEvent"))
            assertTrue(hasTable(migrated, "DiagnosticOutbox"))
        }
    }

    @Test
    fun failedCandidateNeverReplacesSourceDatabase() {
        createFromRoomSchema(version = 12)
        openDatabase().use { source ->
            source.execSQL("DROP TABLE Train")
            source.execSQL(
                """CREATE TABLE Train (
                    trainId TEXT NOT NULL PRIMARY KEY,
                    basicId TEXT NOT NULL,
                    remoteObjectId TEXT NOT NULL
                )""".trimIndent()
            )
            source.execSQL(
                "INSERT INTO Train(trainId, basicId, remoteObjectId) VALUES (?, ?, ?)",
                arrayOf<Any>("fixture-train", "fixture-route", "legacy-object"),
            )
        }

        val failed = runCatching {
            DatabaseDriverFactory(isolatedContext).createRouteDriver().close()
        }.isFailure

        assertTrue(failed)
        openDatabase().use { sourceAfterFailure ->
            assertEquals(setOf("fixture-route"), routeIds(sourceAfterFailure))
            assertTrue(hasColumn(sourceAfterFailure, "Train", "remoteObjectId"))
            assertFalse(hasTable(sourceAfterFailure, "RouteEvent"))
        }
        assertFalse(isolatedContext.getDatabasePath("Route.candidate.db").exists())
    }

    private fun createFromRoomSchema(version: Int) {
        val schemaPath = "com.z_company.data_local.route.data_base.RouteDB/$version.json"
        val schema = schemaContext.assets.open(schemaPath).bufferedReader()
            .use { JSONObject(it.readText()) }
        val database = schema.getJSONObject("database")
        val db = openDatabase()
        try {
            val entities = database.getJSONArray("entities")
            for (entityPosition in 0 until entities.length()) {
                val entity = entities.getJSONObject(entityPosition)
                val tableName = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("${'$'}{TABLE_NAME}", tableName))
                val indices = entity.optJSONArray("indices") ?: continue
                for (indexPosition in 0 until indices.length()) {
                    db.execSQL(
                        indices.getJSONObject(indexPosition)
                            .getString("createSql")
                            .replace("${'$'}{TABLE_NAME}", tableName)
                    )
                }
            }
            db.version = version
            db.execSQL(
                """INSERT INTO BasicData(
                    id, isSynchronizedRoute, remoteRouteId, isOnePersonOperation,
                    isSynchronized, remoteObjectId, isDeleted, updatedAt, number,
                    timeStartWork, timeEndWork, restPointOfTurnover, notes, isFavorite
                ) VALUES (?, 0, NULL, 0, 0, NULL, 0, ?, ?, ?, NULL, 0, NULL, 0)""".trimIndent(),
                arrayOf<Any>(
                    "fixture-route",
                    "2026-01-01T00:00:00Z",
                    "42",
                    1_767_225_600_000L,
                ),
            )
        } finally {
            db.close()
        }
    }

    private fun routeIds(db: SQLiteDatabase): Set<String> =
        db.rawQuery("SELECT id FROM BasicData", null).use { cursor ->
            buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }

    private fun hasTable(db: SQLiteDatabase, table: String): Boolean =
        db.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(table),
        ).use { it.moveToFirst() }

    private fun hasColumn(db: SQLiteDatabase, table: String, column: String): Boolean =
        db.rawQuery("PRAGMA table_info(`$table`)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }
                .any { it == column }
        }

    private fun openDatabase(): SQLiteDatabase {
        return SQLiteDatabase.openOrCreateDatabase(
            isolatedContext.getDatabasePath(DATABASE_NAME),
            null,
        )
    }

    private fun deleteFixtureDatabase() {
        check((isolatedContext as FixtureContext).isFixturePath(DATABASE_NAME))
        isolatedContext.deleteDatabase(DATABASE_NAME)
        isolatedContext.filesDir.resolve("data_safety").deleteRecursively()
        isolatedContext.getSharedPreferences("data_safety_status", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private companion object {
        const val DATABASE_NAME = "Route.db"
    }

    private class FixtureContext(base: Context) : ContextWrapper(base) {
        private val fixtureRoot: File = File(base.cacheDir, "route_migration_fixture")
            .apply { check(isDirectory || mkdirs()) }
        private val fixtureFiles: File = File(fixtureRoot, "files").apply { mkdirs() }

        override fun getDatabasePath(name: String): File = File(fixtureRoot, name)

        override fun getFilesDir(): File = fixtureFiles

        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            baseContext.getSharedPreferences("route_fixture_$name", mode)

        override fun deleteDatabase(name: String): Boolean {
            val database = getDatabasePath(name)
            val deleted = !database.exists() || database.delete()
            File(database.path + "-wal").delete()
            File(database.path + "-shm").delete()
            return deleted
        }

        fun isFixturePath(name: String): Boolean =
            getDatabasePath(name).canonicalPath.startsWith(fixtureRoot.canonicalPath + File.separator)
    }
}
