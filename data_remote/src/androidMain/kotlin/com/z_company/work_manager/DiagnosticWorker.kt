package com.z_company.work_manager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.z_company.domain.repositories.DiagnosticRepository
import com.z_company.repository.remote_rest.RemoteRestClient
import com.z_company.repository.remote_rest.diagnostic.DiagnosticEventsRequest
import com.z_company.repository.remote_rest.diagnostic.DiagnosticPayloadMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DiagnosticWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), KoinComponent {
    private val diagnostics: DiagnosticRepository by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val now = Clock.System.now().toEpochMilliseconds()
        val batch = diagnostics.getReadyBatch(now)
        if (batch.isEmpty()) return@withContext Result.success()
        val summary = diagnostics.getSummary()
        val first = batch.first()
        try {
            RemoteRestClient.remoteRestApi.sendDiagnosticEvents(
                DiagnosticEventsRequest(
                    installationId = first.installationId,
                    diagnosticCode = summary.diagnosticCode,
                    appVersion = first.appVersion ?: "unknown",
                    appBuild = first.appBuild ?: 0,
                    androidVersion = "android",
                    deviceManufacturer = "unknown",
                    deviceModel = "unknown",
                    dbVersion = summary.dbVersion,
                    migrationStatus = summary.migrationStatus,
                    events = batch.map(DiagnosticPayloadMapper::event),
                ),
            )
            diagnostics.markUploaded(batch.map { it.eventId }, now)
            Result.success()
        } catch (error: Exception) {
            batch.forEach { event ->
                diagnostics.scheduleRetry(event.eventId, event.attemptCount, now, error::class.simpleName)
            }
            Result.retry()
        }
    }
}
