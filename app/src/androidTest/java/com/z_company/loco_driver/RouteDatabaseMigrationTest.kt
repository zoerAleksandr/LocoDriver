package com.z_company.loco_driver

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.z_company.data_local.DatabaseDriverFactory
import com.z_company.data_local.route.db.RouteDatabase
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RouteDatabaseMigrationTest {
    /** Test-APK context: never points at the installed LocoDriver app data. */
    private lateinit var isolatedContext: Context

    @Before
    fun setUp() {
        isolatedContext = InstrumentationRegistry.getInstrumentation().context
        check(isolatedContext.packageName.endsWith(".test"))
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

        DatabaseDriverFactory(isolatedContext).createRouteDriver().close()

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
        val schema = isolatedContext.assets.open(schemaPath).bufferedReader()
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

    private fun openDatabase(): SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(
        isolatedContext.getDatabasePath(DATABASE_NAME),
        null,
    )

    private fun deleteFixtureDatabase() {
        check(isolatedContext.packageName.endsWith(".test"))
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
}
