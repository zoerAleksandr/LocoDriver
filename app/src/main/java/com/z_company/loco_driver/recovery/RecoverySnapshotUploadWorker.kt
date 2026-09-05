package com.z_company.loco_driver.recovery

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.z_company.data_local.recovery.AndroidRecoveryArchiveBundle
import com.z_company.data_local.recovery.RecoveryArchiveJson
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.recovery.AndroidRecoveryCloudClient
import com.z_company.repository.remote_rest.recovery.RecoveryCloudHttpException
import com.z_company.repository.remote_rest.recovery.RecoverySnapshotCreateRequest
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RecoverySnapshotUploadWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters), KoinComponent {
    private val secureTokenStorage: SecureTokenStorage by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val token = secureTokenStorage.getAuthBearerTokenFlow().first()
        if (token.isNullOrBlank()) return@withContext Result.success()
        val archive = latestPendingArchive() ?: return@withContext Result.success()
        val bundle = File(archive.parentFile, archive.name + BUNDLE_SUFFIX)
        return@withContext try {
            if (bundle.exists() && !bundle.delete()) return@withContext Result.retry()
            val bundler = AndroidRecoveryArchiveBundle()
            bundler.pack(archive, bundle)
            val manifest = RecoveryArchiveJson.decodeManifest(
                File(archive, MANIFEST_FILE).readText()
            )
            val sha256 = bundler.sha256(bundle)
            val client = AndroidRecoveryCloudClient()
            val snapshot = client.create(
                "Bearer $token",
                RecoverySnapshotCreateRequest(
                    idempotencyKey = "${manifest.installationId}:${manifest.appBuild}:${manifest.sourceDbVersion}:$sha256",
                    archiveFormatVersion = manifest.archiveFormatVersion,
                    sourceDbVersion = manifest.sourceDbVersion,
                    appBuild = manifest.appBuild,
                    installationId = manifest.installationId,
                    sizeBytes = bundle.length(),
                    sha256 = sha256,
                ),
            )
            val ready = if (snapshot.status == "READY") snapshot else {
                client.upload("Bearer $token", snapshot, bundle)
            }
            if (ready.status != "READY") return@withContext Result.retry()
            uploadPreferences().edit().putString(archive.name, ready.snapshotId).commit()
            bundle.delete()
            RecoveryTelemetryQueue(applicationContext).record(
                RecoveryTelemetryType.SNAPSHOT_UPLOAD_SUCCEEDED,
            )
            Result.success()
        } catch (error: RecoveryCloudHttpException) {
            RecoveryTelemetryQueue(applicationContext).record(
                RecoveryTelemetryType.SNAPSHOT_UPLOAD_FAILED,
                if (error.statusCode == 401 || error.statusCode == 403) {
                    RecoveryTelemetryReason.AUTHORIZATION_REQUIRED
                } else {
                    RecoveryTelemetryReason.NETWORK_OR_SERVER
                },
            )
            if (error.statusCode == 401 || error.statusCode == 403) Result.success() else Result.retry()
        } catch (_: Throwable) {
            RecoveryTelemetryQueue(applicationContext).record(
                RecoveryTelemetryType.SNAPSHOT_UPLOAD_FAILED,
                RecoveryTelemetryReason.UNKNOWN,
            )
            Result.retry()
        }
    }

    private fun latestPendingArchive(): File? {
        val root = File(applicationContext.filesDir, RECOVERY_ROOT)
        val uploaded = uploadPreferences()
        return root.listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory && it.name.startsWith(ARCHIVE_PREFIX) }
            ?.filter { uploaded.getString(it.name, null) == null }
            ?.maxByOrNull { it.lastModified() }
    }

    private fun uploadPreferences() = applicationContext.getSharedPreferences(
        UPLOAD_PREFERENCES,
        Context.MODE_PRIVATE,
    )

    private companion object {
        const val RECOVERY_ROOT = "data_safety/recovery"
        const val ARCHIVE_PREFIX = "archive-"
        const val MANIFEST_FILE = "manifest.json"
        const val BUNDLE_SUFFIX = ".bundle"
        const val UPLOAD_PREFERENCES = "recovery_snapshot_uploads"
    }
}
