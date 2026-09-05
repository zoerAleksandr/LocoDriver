package com.z_company.data_local.recovery

import android.content.Context
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.FileOutputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class RecoveryInstallStep {
    PREPARED,
    ROUTE_REPLACED,
    SETTINGS_REPLACED,
    SALARY_REPLACED,
    COMMITTED,
}

@Serializable
private data class RecoveryInstallJournalV1(
    val formatVersion: Int,
    val backupId: String,
    val step: String,
)

/** Must run before Koin exposes database drivers. */
class AndroidRecoveryDatabaseInstaller(
    private val context: Context,
    private val afterStep: (RecoveryInstallStep) -> Unit = {},
) {
    fun install(candidate: PreparedRecoveryCandidate) {
        require(candidate.routeDatabase == context.getDatabasePath(ROUTE_CANDIDATE))
        require(candidate.settingsDatabase == context.getDatabasePath(SETTINGS_CANDIDATE))
        require(candidate.salaryDatabase == context.getDatabasePath(SALARY_CANDIDATE))
        recoverInterruptedInstall()
        validateDatabase(candidate.routeDatabase)
        validateDatabase(candidate.settingsDatabase)
        validateDatabase(candidate.salaryDatabase)

        val liveFiles = liveFiles()
        require(liveFiles.all { it.isFile }) { "Recovery install requires all live databases" }
        val backupId = System.currentTimeMillis().toString()
        val backupDirectory = File(recoveryRoot(), "install-backup-$backupId")
        require(backupDirectory.mkdir()) { "Cannot create recovery install backup" }
        val snapshotter = AndroidFrozenDatabaseSnapshotter()
        liveFiles.forEach { live ->
            snapshotter.snapshot(live, File(backupDirectory, live.name))
        }
        syncDirectory(backupDirectory)
        writeJournal(backupId, RecoveryInstallStep.PREPARED)
        try {
            afterStep(RecoveryInstallStep.PREPARED)
            replace(candidate.routeDatabase, liveFiles[0])
            writeJournal(backupId, RecoveryInstallStep.ROUTE_REPLACED)
            afterStep(RecoveryInstallStep.ROUTE_REPLACED)
            replace(candidate.settingsDatabase, liveFiles[1])
            writeJournal(backupId, RecoveryInstallStep.SETTINGS_REPLACED)
            afterStep(RecoveryInstallStep.SETTINGS_REPLACED)
            replace(candidate.salaryDatabase, liveFiles[2])
            writeJournal(backupId, RecoveryInstallStep.SALARY_REPLACED)
            afterStep(RecoveryInstallStep.SALARY_REPLACED)
            liveFiles.forEach(::validateDatabase)
            writeJournal(backupId, RecoveryInstallStep.COMMITTED)
            afterStep(RecoveryInstallStep.COMMITTED)
            finalizeCommittedInstall()
        } catch (error: Exception) {
            rollback(backupId)
            throw error
        }
    }

    fun recoverInterruptedInstall() {
        val journalFile = journalFile()
        if (!journalFile.isFile) return
        val journal = decodeJournal(journalFile)
        if (journal.step == RecoveryInstallStep.COMMITTED.name) {
            liveFiles().forEach(::validateDatabase)
            finalizeCommittedInstall()
        } else {
            rollback(journal.backupId)
        }
    }

    private fun rollback(backupId: String) {
        require(BACKUP_ID.matches(backupId)) { "Invalid recovery backup ID" }
        val backupDirectory = File(recoveryRoot(), "install-backup-$backupId")
        require(backupDirectory.isDirectory) { "Recovery install backup is missing" }
        liveFiles().forEach { live ->
            val backup = File(backupDirectory, live.name)
            validateDatabase(backup)
            val temporary = File(live.parentFile, live.name + ".recovery-rollback.tmp")
            backup.copyTo(temporary, overwrite = true)
            syncFile(temporary)
            deleteSidecars(live)
            Os.rename(temporary.path, live.path)
            syncDirectory(live.parentFile)
        }
        liveFiles().forEach(::validateDatabase)
        deleteJournal()
        syncDirectory(recoveryRoot())
    }

    private fun replace(candidate: File, live: File) {
        validateDatabase(candidate)
        syncFile(candidate)
        deleteSidecars(live)
        Os.rename(candidate.path, live.path)
        syncDirectory(live.parentFile)
    }

    private fun finalizeCommittedInstall() {
        deleteJournal()
        syncDirectory(recoveryRoot())
    }

    private fun validateDatabase(file: File) {
        AndroidFrozenDatabaseSnapshotter().inspect(file)
    }

    private fun writeJournal(backupId: String, step: RecoveryInstallStep) {
        val journal = RecoveryInstallJournalV1(JOURNAL_FORMAT, backupId, step.name)
        val bytes = JSON.encodeToString(RecoveryInstallJournalV1.serializer(), journal)
            .encodeToByteArray()
        require(bytes.size <= MAX_JOURNAL_BYTES)
        val temporary = File(recoveryRoot(), JOURNAL_TEMPORARY)
        FileOutputStream(temporary).use { output ->
            output.write(bytes)
            output.fd.sync()
        }
        Os.rename(temporary.path, journalFile().path)
        syncDirectory(recoveryRoot())
    }

    private fun decodeJournal(file: File): RecoveryInstallJournalV1 {
        require(file.length() in 1..MAX_JOURNAL_BYTES.toLong())
        return JSON.decodeFromString(RecoveryInstallJournalV1.serializer(), file.readText()).also {
            require(it.formatVersion == JOURNAL_FORMAT)
            require(BACKUP_ID.matches(it.backupId))
            require(RecoveryInstallStep.entries.any { step -> step.name == it.step })
        }
    }

    private fun liveFiles(): List<File> = listOf(
        context.getDatabasePath(ROUTE_DATABASE),
        context.getDatabasePath(SETTINGS_DATABASE),
        context.getDatabasePath(SALARY_DATABASE),
    )

    private fun recoveryRoot(): File = File(context.filesDir, RECOVERY_ROOT).apply {
        require(isDirectory || mkdirs())
    }

    private fun journalFile(): File = File(recoveryRoot(), JOURNAL_FILE)

    private fun syncFile(file: File) {
        val descriptor = Os.open(file.path, OsConstants.O_RDONLY, 0)
        try {
            Os.fsync(descriptor)
        } finally {
            Os.close(descriptor)
        }
    }

    private fun deleteSidecars(database: File) {
        listOf(File(database.path + "-wal"), File(database.path + "-shm")).forEach { sidecar ->
            require(!sidecar.exists() || sidecar.delete()) {
                "Cannot remove closed recovery database sidecar"
            }
        }
        syncDirectory(database.parentFile)
    }

    private fun deleteJournal() {
        val journal = journalFile()
        require(!journal.exists() || journal.delete()) { "Cannot finalize recovery install journal" }
    }

    private fun syncDirectory(directory: File?) {
        val target = requireNotNull(directory)
        val descriptor = Os.open(target.path, OsConstants.O_RDONLY, 0)
        try {
            Os.fsync(descriptor)
        } finally {
            Os.close(descriptor)
        }
    }

    private companion object {
        const val ROUTE_DATABASE = "Route.db"
        const val SETTINGS_DATABASE = "Settings.db"
        const val SALARY_DATABASE = "SalarySetting.db"
        const val ROUTE_CANDIDATE = AndroidRecoveryCandidatePreparer.ROUTE_CANDIDATE
        const val SETTINGS_CANDIDATE = AndroidRecoveryCandidatePreparer.SETTINGS_CANDIDATE
        const val SALARY_CANDIDATE = AndroidRecoveryCandidatePreparer.SALARY_CANDIDATE
        const val RECOVERY_ROOT = "data_safety/recovery"
        const val JOURNAL_FILE = "install-journal.json"
        const val JOURNAL_TEMPORARY = "install-journal.json.tmp"
        const val JOURNAL_FORMAT = 1
        const val MAX_JOURNAL_BYTES = 4 * 1024
        val BACKUP_ID = Regex("[0-9]{1,32}")
        val JSON = Json { ignoreUnknownKeys = false; encodeDefaults = true }
    }
}
