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
import com.z_company.data_local.recovery.AndroidRouteRecoveryExporter
import com.z_company.data_local.recovery.AndroidRouteRecoveryImporter
import com.z_company.data_local.recovery.AndroidAttachmentsManifestExporter
import com.z_company.data_local.recovery.LocalRecoveryAttachmentRequiresContentException
import com.z_company.data_local.recovery.RecoveryArchiveSectionDigest
import com.z_company.data_local.recovery.RecoveryAttachmentsManifestJson
import com.z_company.data_local.recovery.RecoveryRouteRecordValidator
import com.z_company.data_local.recovery.RecoverySha256
import com.z_company.data_local.recovery.RouteRecoveryOrphansPresentException
import com.z_company.data_local.route.db.RouteDatabase
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertArrayEquals
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
    private lateinit var recoveryContext: Context

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        schemaContext = instrumentation.context
        recoveryContext = instrumentation.targetContext
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
        val backupFile = File(isolatedContext.filesDir, "data_safety/Route.pre_migration.db")
        assertTrue("Room v$version backup is missing", backupFile.isFile)
        SQLiteDatabase.openDatabase(
            backupFile.path,
            null,
            SQLiteDatabase.OPEN_READONLY,
        ).use { backup ->
            assertEquals("Room v$version backup version", version, backup.version)
            assertEquals("Room v$version backup routes", before, routeIds(backup))
            assertEquals("Room v$version backup children", childrenBefore, childCounts(backup))
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

    @Test
    fun corruptedSourceAndExistingBackupAreNeverOverwritten() {
        createFromRoomSchema(version = 12)
        val dbFile = isolatedContext.getDatabasePath(DATABASE_NAME)
        val backupDir = File(isolatedContext.filesDir, "data_safety").apply { mkdirs() }
        val existingBackup = File(backupDir, "Route.pre_migration.db")
        dbFile.copyTo(existingBackup, overwrite = true)
        val backupBytes = existingBackup.readBytes()
        val corruptedBytes = ByteArray(4_096) { index -> (index % 251).toByte() }
        dbFile.writeBytes(corruptedBytes)

        val error = runCatching {
            DatabaseDriverFactory(isolatedContext).createRouteDriver().close()
        }.exceptionOrNull()

        assertTrue(error != null)
        assertArrayEquals(corruptedBytes, dbFile.readBytes())
        assertArrayEquals(backupBytes, existingBackup.readBytes())
        assertEquals("FAILED", migrationPreferences().getString("migration_stage", null))
        assertFalse(isolatedContext.getDatabasePath("Route.candidate.db").exists())
    }

    @Test
    fun largeRoomDatabasePreservesRoutesAndEveryChildCount() {
        createFromRoomSchema(version = 12, populate = false)
        openDatabase().use { source ->
            source.beginTransaction()
            try {
                repeat(LARGE_FIXTURE_ROUTE_COUNT) { index ->
                    val routeId = "large-fixture-route-$index"
                    insertRequiredRow(source, "BasicData", routeId)
                    FIXTURE_CHILD_TABLES.forEach { table ->
                        insertRequiredRow(source, table, routeId)
                    }
                }
                source.setTransactionSuccessful()
            } finally {
                source.endTransaction()
            }
        }
        val idsBefore = openDatabase().use(::routeIds)
        val childrenBefore = openDatabase().use(::childCounts)

        DatabaseDriverFactory(isolatedContext).createRouteDriver().close()

        openDatabase().use { migrated ->
            assertEquals(LARGE_FIXTURE_ROUTE_COUNT, routeIds(migrated).size)
            assertEquals(idsBefore, routeIds(migrated))
            assertEquals(childrenBefore, childCounts(migrated))
        }
    }

    @Test
    fun preExistingOrphanDoesNotBlockMigrationOrIncreaseOrphanCount() {
        createFromRoomSchema(version = 12)
        openDatabase().use { source ->
            source.execSQL("UPDATE Train SET basicId = ?", arrayOf<Any>("missing-route"))
            assertEquals(1L, orphanCount(source))
        }

        DatabaseDriverFactory(isolatedContext).createRouteDriver().close()

        openDatabase().use { migrated ->
            assertEquals(setOf("fixture-route"), routeIds(migrated))
            assertEquals(1L, orphanCount(migrated))
            assertEquals(1L, childCounts(migrated).getValue("Train"))
        }
    }

    @Test
    fun failedBuildDoesNotRetryUntilUserExplicitlyAllowsIt() {
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
        val bootstrap = MigrationRecoveryBootstrap(isolatedContext, appBuild = 9_999)

        assertEquals(MigrationBootstrapState.RECOVERY_REQUIRED, bootstrap.prepare())

        // A fixed source represents data restored by support or a corrected build.
        // The same build must still wait for the explicit recovery action.
        isolatedContext.deleteDatabase(DATABASE_NAME)
        createFromRoomSchema(version = 12)
        assertEquals(MigrationBootstrapState.RECOVERY_REQUIRED, bootstrap.prepare())
        openDatabase().use { untouched -> assertEquals(12, untouched.version) }

        assertEquals(
            MigrationBootstrapState.READY,
            bootstrap.prepare(allowRetry = true),
        )
        openDatabase().use { migrated ->
            assertEquals(RouteDatabase.Schema.version.toInt(), migrated.version)
            assertEquals(setOf("fixture-route"), routeIds(migrated))
        }
    }

    @Test
    fun newerBuildGetsOneAutomaticRetryAfterPreviousBuildFailed() {
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
        val failedBuild = MigrationRecoveryBootstrap(isolatedContext, appBuild = 9_999)
        assertEquals(MigrationBootstrapState.RECOVERY_REQUIRED, failedBuild.prepare())

        isolatedContext.deleteDatabase(DATABASE_NAME)
        createFromRoomSchema(version = 12)
        val fixedBuild = MigrationRecoveryBootstrap(isolatedContext, appBuild = 10_000)

        assertEquals(MigrationBootstrapState.READY, fixedBuild.prepare())
        openDatabase().use { migrated ->
            assertEquals(RouteDatabase.Schema.version.toInt(), migrated.version)
            assertEquals(setOf("fixture-route"), routeIds(migrated))
        }
    }

    @Test
    fun missingRequiredTableCannotBeAcceptedAsSuccessfulMigration() {
        createFromRoomSchema(version = 12)
        openDatabase().use { source -> source.execSQL("DROP TABLE Train") }

        val error = runCatching {
            DatabaseDriverFactory(isolatedContext).createRouteDriver().close()
        }.exceptionOrNull()

        assertTrue(error != null)
        openDatabase().use { source ->
            assertEquals(12, source.version)
            assertEquals(setOf("fixture-route"), routeIds(source))
            assertFalse(hasTable(source, "Train"))
        }
        assertEquals("FAILED", migrationPreferences().getString("migration_stage", null))
    }

    @Test
    fun roomRouteExportsToDeterministicNdjsonWithoutChangingSource() {
        createFromRoomSchema(version = 12)
        val destination = File(isolatedContext.filesDir, "recovery/routes.ndjson")

        val section = AndroidRouteRecoveryExporter().export(
            isolatedContext.getDatabasePath(DATABASE_NAME),
            destination,
        )

        assertEquals(1L, section.itemCount)
        assertEquals(RecoverySha256.digestHex(destination.readBytes()), section.sha256)
        val lines = destination.readLines().filter { it.isNotBlank() }
        assertEquals(1, lines.size)
        val record = RecoveryRouteRecordValidator.decodeAndValidate(lines.single())
        assertEquals("fixture-route", record.routeId)
        assertEquals(1, record.tables.getValue("BasicData").size)
        FIXTURE_CHILD_TABLES.forEach { table ->
            assertEquals("Exported $table count", 1, record.tables.getValue(table).size)
        }
        openDatabase().use { source ->
            assertEquals(12, source.version)
            assertEquals(setOf("fixture-route"), routeIds(source))
        }
    }

    @Test
    fun everyArchivedRoomFixtureExportsBeforeMigration() {
        for (version in ARCHIVED_ROOM_VERSIONS) {
            deleteFixtureDatabase()
            createFromRoomSchema(version = version)
            val destination = File(isolatedContext.filesDir, "recovery/routes-v$version.ndjson")

            val section = AndroidRouteRecoveryExporter().export(
                isolatedContext.getDatabasePath(DATABASE_NAME),
                destination,
            )

            assertEquals("Room v$version export count", 1L, section.itemCount)
            assertEquals(
                "Room v$version export digest",
                RecoverySha256.digestHex(destination.readBytes()),
                section.sha256,
            )
            val record = RecoveryRouteRecordValidator.decodeAndValidate(
                destination.readLines().single()
            )
            assertEquals("Room v$version route", "fixture-route", record.routeId)
            assertEquals("Room v$version tables", 5, record.tables.size)
            openDatabase().use { source -> assertEquals(version, source.version) }
        }
    }

    @Test
    fun everyArchivedRoomFixtureRoundTripsIntoFreshCurrentDatabase() {
        for (version in ARCHIVED_ROOM_VERSIONS) {
            deleteFixtureDatabase()
            recoveryContext.deleteDatabase(RECOVERY_IMPORT_DATABASE_NAME)
            createFromRoomSchema(version = version)
            val sourceIds = openDatabase().use(::routeIds)
            val sourceChildren = openDatabase().use(::childCounts)
            val destination = File(isolatedContext.filesDir, "recovery/routes-v$version.ndjson")
            val exported = AndroidRouteRecoveryExporter().export(
                isolatedContext.getDatabasePath(DATABASE_NAME),
                destination,
            )

            val imported = AndroidRouteRecoveryImporter(recoveryContext).importToFreshDatabase(
                routesSection = destination,
                expected = RecoveryArchiveSectionDigest(exported.itemCount, exported.sha256),
                databaseName = RECOVERY_IMPORT_DATABASE_NAME,
            )

            assertEquals("Room v$version import count", exported.itemCount, imported.routeCount)
            SQLiteDatabase.openDatabase(
                imported.databaseFile.path,
                null,
                SQLiteDatabase.OPEN_READONLY,
            ).use { restored ->
                assertEquals(
                    "Room v$version imported schema",
                    RouteDatabase.Schema.version.toInt(),
                    restored.version,
                )
                assertEquals("Room v$version imported routes", sourceIds, routeIds(restored))
                assertEquals("Room v$version imported children", sourceChildren, childCounts(restored))
                assertEquals(0L, orphanCount(restored))
            }
            openDatabase().use { source ->
                assertEquals("Room v$version source version changed", version, source.version)
                assertEquals("Room v$version source routes changed", sourceIds, routeIds(source))
            }
        }
    }

    @Test
    fun checksumMismatchCannotCreateRecoveryImportDatabase() {
        createFromRoomSchema(version = 12)
        val destination = File(isolatedContext.filesDir, "recovery/routes.ndjson")
        val exported = AndroidRouteRecoveryExporter().export(
            isolatedContext.getDatabasePath(DATABASE_NAME),
            destination,
        )

        val error = runCatching {
            AndroidRouteRecoveryImporter(recoveryContext).importToFreshDatabase(
                destination,
                RecoveryArchiveSectionDigest(exported.itemCount, "0".repeat(64)),
                RECOVERY_IMPORT_DATABASE_NAME,
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertFalse(recoveryContext.getDatabasePath(RECOVERY_IMPORT_DATABASE_NAME).exists())
        openDatabase().use { source -> assertEquals(12, source.version) }
    }

    @Test
    fun malformedRecoveryRecordDeletesDisposableImportDatabase() {
        val destination = File(isolatedContext.filesDir, "recovery/routes.ndjson")
            .apply {
                parentFile?.mkdirs()
                writeText("{not-json}\n")
            }
        val digest = RecoverySha256.digestHex(destination.readBytes())

        val error = runCatching {
            AndroidRouteRecoveryImporter(recoveryContext).importToFreshDatabase(
                destination,
                RecoveryArchiveSectionDigest(1L, digest),
                RECOVERY_IMPORT_DATABASE_NAME,
            )
        }.exceptionOrNull()

        assertTrue(error != null)
        assertFalse(recoveryContext.getDatabasePath(RECOVERY_IMPORT_DATABASE_NAME).exists())
    }

    @Test
    fun remotePhotoProducesValidatedAttachmentsManifestWithoutChangingSource() {
        createFromRoomSchema(version = 12)
        openDatabase().use { source ->
            source.execSQL(
                "UPDATE Photo SET url = ?, dateOfCreate = ?",
                arrayOf<Any>("https://files.example.test/photo.jpg", 123L),
            )
        }
        val destination = File(isolatedContext.filesDir, "recovery/attachments-manifest.json")

        val exported = AndroidAttachmentsManifestExporter().export(
            isolatedContext.getDatabasePath(DATABASE_NAME),
            destination,
        )

        assertEquals(1L, exported.itemCount)
        assertEquals(RecoverySha256.digestHex(destination.readBytes()), exported.sha256)
        val manifest = RecoveryAttachmentsManifestJson.decodeAndValidate(destination.readText())
        assertEquals(1, manifest.attachments.size)
        assertEquals("fixture-route", manifest.attachments.single().routeId)
        openDatabase().use { source -> assertEquals(12, source.version) }
    }

    @Test
    fun localPhotoReferenceCannotProduceFalseCompleteAttachmentsManifest() {
        createFromRoomSchema(version = 12)
        openDatabase().use { source ->
            source.execSQL(
                "UPDATE Photo SET url = ?",
                arrayOf<Any>("content://photos/local-image"),
            )
        }
        val destination = File(isolatedContext.filesDir, "recovery/attachments-manifest.json")

        val error = runCatching {
            AndroidAttachmentsManifestExporter().export(
                isolatedContext.getDatabasePath(DATABASE_NAME),
                destination,
            )
        }.exceptionOrNull()

        assertTrue(error is LocalRecoveryAttachmentRequiresContentException)
        assertFalse(destination.exists())
        assertFalse(File(destination.path + ".tmp").exists())
        openDatabase().use { source -> assertEquals(12, source.version) }
    }

    @Test
    fun legacyBase64PhotoProducesVerifiedEmbeddedAttachmentMetadata() {
        createFromRoomSchema(version = 12)
        openDatabase().use { source ->
            source.execSQL(
                "UPDATE Photo SET url = ?, dateOfCreate = ?",
                arrayOf<Any>("aGVsbG8=", 123L),
            )
        }
        val destination = File(isolatedContext.filesDir, "recovery/attachments-manifest.json")

        val exported = AndroidAttachmentsManifestExporter().export(
            isolatedContext.getDatabasePath(DATABASE_NAME),
            destination,
        )

        val manifest = RecoveryAttachmentsManifestJson.decodeAndValidate(destination.readText())
        assertEquals(1L, exported.itemCount)
        assertEquals(0, manifest.attachments.size)
        assertEquals(1, manifest.embeddedAttachments.size)
        assertEquals(5L, manifest.embeddedAttachments.single().sizeBytes)
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            manifest.embeddedAttachments.single().sha256,
        )
        openDatabase().use { source -> assertEquals(12, source.version) }
    }

    @Test
    fun orphanedRowsCannotProduceSilentlyIncompleteRecoveryExport() {
        createFromRoomSchema(version = 12)
        openDatabase().use { source ->
            source.execSQL("UPDATE Train SET basicId = ?", arrayOf<Any>("missing-route"))
        }
        val destination = File(isolatedContext.filesDir, "recovery/routes.ndjson")

        val error = runCatching {
            AndroidRouteRecoveryExporter().export(
                isolatedContext.getDatabasePath(DATABASE_NAME),
                destination,
            )
        }.exceptionOrNull()

        assertTrue(error is RouteRecoveryOrphansPresentException)
        assertEquals(1L, (error as RouteRecoveryOrphansPresentException).orphanCount)
        assertFalse(destination.exists())
        assertFalse(File(destination.path + ".tmp").exists())
        openDatabase().use { source ->
            assertEquals(12, source.version)
            assertEquals(1L, orphanCount(source))
        }
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
                    name.endsWith("Id") -> "fixture-$routeId-${name.lowercase()}"
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

    private fun orphanCount(db: SQLiteDatabase): Long = FIXTURE_CHILD_TABLES.sumOf { table ->
        if (!hasTable(db, table)) 0L else db.rawQuery(
            "SELECT count(*) FROM `$table` child LEFT JOIN BasicData parent " +
                "ON parent.id = child.basicId WHERE parent.id IS NULL",
            null,
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
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
        if (::recoveryContext.isInitialized) {
            recoveryContext.deleteDatabase(RECOVERY_IMPORT_DATABASE_NAME)
        }
        isolatedContext.filesDir.resolve("data_safety").deleteRecursively()
        isolatedContext.filesDir.resolve("recovery").deleteRecursively()
        isolatedContext.getSharedPreferences("data_safety_status", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        isolatedContext.getSharedPreferences("migration_recovery_bootstrap", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun migrationPreferences(): SharedPreferences =
        isolatedContext.getSharedPreferences("data_safety_status", Context.MODE_PRIVATE)

    private companion object {
        const val DATABASE_NAME = "Route.db"
        const val RECOVERY_IMPORT_DATABASE_NAME = "Route.recovery.fixture.import.db"
        val ARCHIVED_ROOM_VERSIONS = 1..12
        val FIXTURE_CHILD_TABLES = listOf("Locomotive", "Train", "Passenger", "Photo")
        const val LARGE_FIXTURE_ROUTE_COUNT = 500
    }

    private class FixtureContext(base: Context) : ContextWrapper(base) {
        private val fixtureRoot: File = File(base.cacheDir, "route_migration_fixture")
            .apply { check(isDirectory || mkdirs()) }
        private val fixtureFiles: File = File(fixtureRoot, "files").apply { mkdirs() }

        override fun getDatabasePath(name: String): File = File(fixtureRoot, name)

        override fun getApplicationContext(): Context = this

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
