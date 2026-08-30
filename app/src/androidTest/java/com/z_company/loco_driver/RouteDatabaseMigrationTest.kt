package com.z_company.loco_driver

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.z_company.data_local.DatabaseDriverFactory
import com.z_company.data_local.RouteMigrationAlreadyRunningException
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
import java.io.RandomAccessFile

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
    fun everyArchivedRoomFixtureMigratesWithoutChangingRouteIdentity() {
        for (version in ARCHIVED_ROOM_VERSIONS) {
            deleteFixtureDatabase()
            try {
                migrateRoomFixture(version)
            } catch (error: Throwable) {
                throw AssertionError("Room v$version migration failed", error)
            }
        }
    }

    @Test
    fun everyArchivedEmptyRoomFixtureMigrates() {
        for (version in ARCHIVED_ROOM_VERSIONS) {
            deleteFixtureDatabase()
            try {
                createFromRoomSchema(version, populate = false)
                DatabaseDriverFactory(isolatedContext).createRouteDriver().close()
                openDatabase().use { migrated ->
                    assertEquals("Empty Room v$version", RouteDatabase.Schema.version.toInt(), migrated.version)
                    assertTrue("Empty Room v$version gained routes", routeIds(migrated).isEmpty())
                }
            } catch (error: Throwable) {
                throw AssertionError("Empty Room v$version migration failed", error)
            }
        }
    }

    private fun migrateRoomFixture(version: Int) {
        createFromRoomSchema(version)
        val before = openDatabase().use { db -> routeIds(db) }
        val childrenBefore = openDatabase().use(::childCounts)
        val roomProfile = RouteDatabaseProfileDetector().detect(
            isolatedContext.getDatabasePath(DATABASE_NAME)
        )
        assertTrue("Room v$version profile is not recognized: $roomProfile", roomProfile.isRecognized)

        DatabaseDriverFactory(isolatedContext).createRouteDriver().close()

        val migratedProfile = RouteDatabaseProfileDetector().detect(
            isolatedContext.getDatabasePath(DATABASE_NAME)
        )
        assertTrue("Unexpected profile after Room v$version: $migratedProfile", migratedProfile.isRecognized)
        assertEquals("Room v$version", RouteDatabase.Schema.version.toInt(), migratedProfile.userVersion)
        assertFalse(migratedProfile.hasLegacyRoomTrain)
        assertFalse(migratedProfile.hasLegacyRoomLocomotive)
        assertTrue(migratedProfile.hasTrashFields)
        assertTrue(migratedProfile.hasDiagnosticTables)

        openDatabase().use { migrated ->
            assertEquals("Room v$version", RouteDatabase.Schema.version.toInt(), migrated.version)
            assertEquals("Room v$version", before, routeIds(migrated))
            assertEquals("Room v$version child rows", childrenBefore, childCounts(migrated))
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
        assertEquals("FAILED", migrationPreferences().getString("migration_stage", null))
    }

    @Test
    fun interruptedCandidateIsDiscardedAndMigrationRestartsFromSource() {
        createFromRoomSchema(version = 12)
        val sourceIds = openDatabase().use(::routeIds)
        val staleCandidate = isolatedContext.getDatabasePath("Route.candidate.db")
        staleCandidate.writeText("incomplete candidate")
        migrationPreferences().edit()
            .putString("migration_stage", "CANDIDATE_MIGRATING")
            .commit()

        DatabaseDriverFactory(isolatedContext).createRouteDriver().close()

        assertFalse(staleCandidate.exists())
        assertEquals("SUCCEEDED", migrationPreferences().getString("migration_stage", null))
        openDatabase().use { migrated ->
            assertEquals(RouteDatabase.Schema.version.toInt(), migrated.version)
            assertEquals(sourceIds, routeIds(migrated))
        }
    }

    @Test
    fun interruptionAfterAtomicSwapAcceptsValidatedCurrentDatabase() {
        createFromRoomSchema(version = 12)
        DatabaseDriverFactory(isolatedContext).createRouteDriver().close()
        val migratedIds = openDatabase().use(::routeIds)
        migrationPreferences().edit()
            .putString("migration_stage", "SWAPPED")
            .putInt("migration_from", 12)
            .commit()

        DatabaseDriverFactory(isolatedContext).createRouteDriver().close()

        assertEquals("SUCCEEDED", migrationPreferences().getString("migration_stage", null))
        openDatabase().use { current ->
            assertEquals(RouteDatabase.Schema.version.toInt(), current.version)
            assertEquals(migratedIds, routeIds(current))
        }
    }

    @Test
    fun concurrentMigrationCannotTouchSourceDatabase() {
        createFromRoomSchema(version = 12)
        val sourceIds = openDatabase().use(::routeIds)
        val lockFile = File(isolatedContext.filesDir, "data_safety/Route.migration.lock")
            .apply { parentFile?.mkdirs() }

        RandomAccessFile(lockFile, "rw").channel.use { channel ->
            channel.lock().use {
                val error = runCatching {
                    DatabaseDriverFactory(isolatedContext).createRouteDriver().close()
                }.exceptionOrNull()
                assertTrue(error is RouteMigrationAlreadyRunningException)
            }
        }

        openDatabase().use { source ->
            assertEquals(12, source.version)
            assertEquals(sourceIds, routeIds(source))
        }
    }

    @Test
    fun committedWalRouteIsIncludedInBackupAndMigration() {
        createFromRoomSchema(version = 12)
        val dbFile = isolatedContext.getDatabasePath(DATABASE_NAME)
        val walSnapshot = File(isolatedContext.cacheDir, "fixture-wal-snapshot")
        val mainSnapshot = File(isolatedContext.cacheDir, "fixture-main-snapshot")

        openDatabase().use { source ->
            source.rawQuery("PRAGMA journal_mode=WAL", null).use { it.moveToFirst() }
            source.rawQuery("PRAGMA wal_autocheckpoint=0", null).use { it.moveToFirst() }
            insertRequiredRow(source, "BasicData", routeId = "fixture-wal-route")
            dbFile.copyTo(mainSnapshot, overwrite = true)
            val liveWal = File(dbFile.path + "-wal")
            assertTrue("Fixture WAL was not created", liveWal.length() > 0L)
            liveWal.copyTo(walSnapshot, overwrite = true)
        }

        isolatedContext.deleteDatabase(DATABASE_NAME)
        mainSnapshot.copyTo(dbFile, overwrite = true)
        walSnapshot.copyTo(File(dbFile.path + "-wal"), overwrite = true)

        DatabaseDriverFactory(isolatedContext).createRouteDriver().close()

        openDatabase().use { migrated ->
            assertEquals(
                setOf("fixture-route", "fixture-wal-route"),
                routeIds(migrated),
            )
        }
        mainSnapshot.delete()
        walSnapshot.delete()
    }

    private fun createFromRoomSchema(version: Int, populate: Boolean = true) {
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
            if (populate) {
                insertRequiredRow(db, "BasicData")
                FIXTURE_CHILD_TABLES.filter { hasTable(db, it) }.forEach { table ->
                    insertRequiredRow(db, table)
                }
            }
        } finally {
            db.close()
        }
    }

    private fun insertRequiredRow(
        db: SQLiteDatabase,
        table: String,
        routeId: String = "fixture-route",
    ) {
        val columns = db.rawQuery("PRAGMA table_info(`$table`)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val typeIndex = cursor.getColumnIndexOrThrow("type")
            val notNullIndex = cursor.getColumnIndexOrThrow("notnull")
            val defaultIndex = cursor.getColumnIndexOrThrow("dflt_value")
            val primaryKeyIndex = cursor.getColumnIndexOrThrow("pk")
            buildList {
                while (cursor.moveToNext()) {
                    val isRequired = cursor.getInt(notNullIndex) == 1 || cursor.getInt(primaryKeyIndex) == 1
                    if (isRequired && cursor.isNull(defaultIndex)) {
                        add(cursor.getString(nameIndex) to cursor.getString(typeIndex).uppercase())
                    }
                }
            }
        }
        val names = columns.joinToString(", ") { "`${it.first}`" }
        val placeholders = columns.joinToString(", ") { "?" }
        val values = columns.map<Pair<String, String>, Any> { (name, type) ->
            when (name) {
                "id" -> routeId
                "basicId" -> routeId
                else -> when {
                    name.endsWith("Id") -> "fixture-${name.lowercase()}"
                    name == "updatedAt" && type.contains("TEXT") -> "2026-01-01T00:00:00Z"
                    type.contains("INT") -> 0L
                    type.contains("REAL") || type.contains("FLOA") || type.contains("DOUB") -> 0.0
                    name.endsWith("List") || name == "stations" -> "[]"
                    else -> ""
                }
            }
        }.toTypedArray()
        db.execSQL("INSERT INTO `$table`($names) VALUES ($placeholders)", values)
    }

    private fun routeIds(db: SQLiteDatabase): Set<String> =
        db.rawQuery("SELECT id FROM BasicData", null).use { cursor ->
            buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }

    private fun childCounts(db: SQLiteDatabase): Map<String, Long> =
        FIXTURE_CHILD_TABLES.associateWith { table ->
            if (!hasTable(db, table)) 0L else db.rawQuery("SELECT count(*) FROM `$table`", null)
                .use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
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

    private fun migrationPreferences(): SharedPreferences =
        isolatedContext.getSharedPreferences("data_safety_status", Context.MODE_PRIVATE)

    private companion object {
        const val DATABASE_NAME = "Route.db"
        val ARCHIVED_ROOM_VERSIONS = 1..12
        val FIXTURE_CHILD_TABLES = listOf("Locomotive", "Train", "Passenger", "Photo")
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
