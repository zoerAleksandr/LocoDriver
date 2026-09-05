package com.z_company.loco_driver.recovery

import android.content.Context
import com.z_company.data_local.recovery.AndroidRecoveryArchiveBundle
import com.z_company.data_local.recovery.AndroidRecoveryCandidatePreparer
import com.z_company.data_local.recovery.AndroidRecoveryDatabaseInstaller
import com.z_company.repository.remote_rest.recovery.AndroidRecoveryCloudClient
import com.z_company.repository.remote_rest.recovery.RecoveryCloudClient
import com.z_company.repository.remote_rest.recovery.RecoverySnapshotContractValidator
import java.io.File

data class RecoveryRestoreResult(
    val snapshotId: String,
    val routeCount: Long,
    val settingsRowCount: Long,
    val salaryRowCount: Long,
)

/** Runs before database drivers are exposed and never writes archive data into a live DB directly. */
class RecoverySnapshotRestoreCoordinator(
    private val context: Context,
    private val cloudClient: RecoveryCloudClient = AndroidRecoveryCloudClient(),
) {
    fun restoreLatest(bearerToken: String): RecoveryRestoreResult = synchronized(RESTORE_LOCK) {
        require(bearerToken.isNotBlank()) { "Recovery authorization is missing" }
        AndroidRecoveryDatabaseInstaller(context).recoverInterruptedInstall()
        val authorization = normalizeToken(bearerToken)
        val snapshot = cloudClient.latest(authorization)
        RecoverySnapshotContractValidator.validate(snapshot)
        require(snapshot.status == "READY") { "Latest recovery snapshot is not ready" }

        val workDirectory = File(recoveryRoot(), RESTORE_WORK_DIRECTORY)
        resetDisposableDirectory(workDirectory)
        val bundle = File(workDirectory, BUNDLE_FILE)
        val archive = File(workDirectory, ARCHIVE_DIRECTORY)
        try {
            cloudClient.download(authorization, snapshot, bundle)
            val bundler = AndroidRecoveryArchiveBundle()
            require(bundle.isFile && bundle.length() == snapshot.sizeBytes) {
                "Downloaded recovery snapshot size mismatch"
            }
            require(bundler.sha256(bundle).equals(snapshot.sha256, ignoreCase = true)) {
                "Downloaded recovery snapshot checksum mismatch"
            }
            bundler.unpack(bundle, archive)
            val candidate = AndroidRecoveryCandidatePreparer(context).prepare(archive)
            AndroidRecoveryDatabaseInstaller(context).install(candidate)
            RecoveryRestoreResult(
                snapshot.snapshotId,
                candidate.routeCount,
                candidate.settingsRowCount,
                candidate.salaryRowCount,
            )
        } finally {
            workDirectory.deleteRecursively()
        }
    }

    private fun normalizeToken(token: String): String =
        if (token.startsWith("Bearer ")) token else "Bearer $token"

    private fun recoveryRoot(): File = File(context.filesDir, RECOVERY_ROOT).apply {
        require(isDirectory || mkdirs()) { "Cannot create recovery root" }
    }

    private fun resetDisposableDirectory(directory: File) {
        require(directory.parentFile == recoveryRoot())
        require(!directory.exists() || directory.deleteRecursively()) {
            "Cannot clear disposable cloud recovery data"
        }
        require(directory.mkdir()) { "Cannot create cloud recovery workspace" }
    }

    private companion object {
        val RESTORE_LOCK = Any()
        const val RECOVERY_ROOT = "data_safety/recovery"
        const val RESTORE_WORK_DIRECTORY = "cloud-restore-working"
        const val BUNDLE_FILE = "snapshot.bundle"
        const val ARCHIVE_DIRECTORY = "archive"
    }
}
