package com.z_company.loco_driver

import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.z_company.data_local.recovery.AndroidRecoveryCandidatePreparer
import com.z_company.data_local.recovery.AndroidRecoveryDatabaseInstaller
import com.z_company.data_local.recovery.PreparedRecoveryCandidate
import com.z_company.data_local.recovery.RecoveryInstallStep
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecoveryDatabaseInstallerTest {
    private lateinit var fixtureContext: FixtureContext

    @Before
    fun setUp() {
        fixtureContext = FixtureContext(InstrumentationRegistry.getInstrumentation().targetContext)
        fixtureContext.clear()
    }

    @After
    fun tearDown() = fixtureContext.clear()

    @Test
    fun interruptionBeforeCommitRollsBackEveryDatabaseOnNextBootstrap() {
        val interruptionSteps = listOf(
            RecoveryInstallStep.PREPARED,
            RecoveryInstallStep.ROUTE_REPLACED,
            RecoveryInstallStep.SETTINGS_REPLACED,
            RecoveryInstallStep.SALARY_REPLACED,
        )

        interruptionSteps.forEach { interruptedAt ->
            fixtureContext.clear()
            createDatabases()
            try {
                AndroidRecoveryDatabaseInstaller(fixtureContext) { step ->
                    if (step == interruptedAt) throw SimulatedProcessDeath
                }.install(candidate())
                throw AssertionError("Expected simulated process death at $interruptedAt")
            } catch (expected: SimulatedProcessDeath) {
                // A process death bypasses in-process rollback and leaves the durable journal.
            }

            assertTrue(journal().isFile)
            AndroidRecoveryDatabaseInstaller(fixtureContext).recoverInterruptedInstall()

            assertVersions(LIVE_ROUTE, LIVE_SETTINGS, LIVE_SALARY)
            assertFalse(journal().exists())
            assertTrue(backupDirectories().isNotEmpty())
        }
    }

    @Test
    fun interruptionAfterCommitKeepsAllRecoveredDatabases() {
        createDatabases()
        try {
            AndroidRecoveryDatabaseInstaller(fixtureContext) { step ->
                if (step == RecoveryInstallStep.COMMITTED) throw SimulatedProcessDeath
            }.install(candidate())
            throw AssertionError("Expected simulated process death after commit")
        } catch (expected: SimulatedProcessDeath) {
            // The journal records that all three validated replacements are committed.
        }

        assertTrue(journal().isFile)
        AndroidRecoveryDatabaseInstaller(fixtureContext).recoverInterruptedInstall()

        assertVersions(CANDIDATE_ROUTE, CANDIDATE_SETTINGS, CANDIDATE_SALARY)
        assertFalse(journal().exists())
        assertTrue(backupDirectories().isNotEmpty())
    }

    @Test
    fun ordinaryInstallFailureRollsBackImmediately() {
        createDatabases()
        runCatching {
            AndroidRecoveryDatabaseInstaller(fixtureContext) { step ->
                if (step == RecoveryInstallStep.SETTINGS_REPLACED) error("injected failure")
            }.install(candidate())
        }.onSuccess { throw AssertionError("Expected injected installation failure") }

        assertVersions(LIVE_ROUTE, LIVE_SETTINGS, LIVE_SALARY)
        assertFalse(journal().exists())
    }

    private fun createDatabases() {
        createDatabase("Route.db", LIVE_ROUTE)
        createDatabase("Settings.db", LIVE_SETTINGS)
        createDatabase("SalarySetting.db", LIVE_SALARY)
        createDatabase(AndroidRecoveryCandidatePreparer.ROUTE_CANDIDATE, CANDIDATE_ROUTE)
        createDatabase(AndroidRecoveryCandidatePreparer.SETTINGS_CANDIDATE, CANDIDATE_SETTINGS)
        createDatabase(AndroidRecoveryCandidatePreparer.SALARY_CANDIDATE, CANDIDATE_SALARY)
    }

    private fun createDatabase(name: String, version: Int) {
        SQLiteDatabase.openOrCreateDatabase(fixtureContext.getDatabasePath(name), null).use { db ->
            db.execSQL("CREATE TABLE marker(value INTEGER NOT NULL)")
            db.execSQL("INSERT INTO marker(value) VALUES (?)", arrayOf(version))
            db.version = version
        }
    }

    private fun candidate() = PreparedRecoveryCandidate(
        fixtureContext.getDatabasePath(AndroidRecoveryCandidatePreparer.ROUTE_CANDIDATE),
        fixtureContext.getDatabasePath(AndroidRecoveryCandidatePreparer.SETTINGS_CANDIDATE),
        fixtureContext.getDatabasePath(AndroidRecoveryCandidatePreparer.SALARY_CANDIDATE),
        1,
        1,
        1,
    )

    private fun assertVersions(route: Int, settings: Int, salary: Int) {
        assertEquals(route, version("Route.db"))
        assertEquals(settings, version("Settings.db"))
        assertEquals(salary, version("SalarySetting.db"))
    }

    private fun version(name: String): Int = SQLiteDatabase.openDatabase(
        fixtureContext.getDatabasePath(name).path,
        null,
        SQLiteDatabase.OPEN_READONLY,
    ).use { it.version }

    private fun journal() = File(fixtureContext.filesDir, "data_safety/recovery/install-journal.json")

    private fun backupDirectories(): List<File> =
        journal().parentFile?.listFiles()?.filter { it.name.startsWith("install-backup-") }.orEmpty()

    private companion object {
        const val LIVE_ROUTE = 12
        const val LIVE_SETTINGS = 14
        const val LIVE_SALARY = 7
        const val CANDIDATE_ROUTE = 112
        const val CANDIDATE_SETTINGS = 114
        const val CANDIDATE_SALARY = 107
        object SimulatedProcessDeath : Error()
    }

    private class FixtureContext(base: Context) : ContextWrapper(base) {
        private val root = File(base.cacheDir, "recovery_database_installer_fixture")
        private val databases = File(root, "databases")
        private val files = File(root, "files")

        init {
            databases.mkdirs()
            files.mkdirs()
        }

        override fun getDatabasePath(name: String): File = File(databases, name)
        override fun getFilesDir(): File = files

        fun clear() {
            root.deleteRecursively()
            databases.mkdirs()
            files.mkdirs()
        }
    }
}
