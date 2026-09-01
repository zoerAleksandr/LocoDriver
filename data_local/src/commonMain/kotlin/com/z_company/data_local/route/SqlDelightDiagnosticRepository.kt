package com.z_company.data_local.route

import com.z_company.data_local.route.db.RouteDatabase
import com.z_company.domain.entities.diagnostic.DiagnosticEventSummary
import com.z_company.domain.entities.diagnostic.DiagnosticSummary
import com.z_company.domain.entities.diagnostic.DiagnosticOutboxEvent
import com.z_company.domain.entities.diagnostic.DiagnosticOutboxPolicy
import com.z_company.domain.repositories.DiagnosticRepository
import com.z_company.domain.util.generateId
import kotlin.time.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@OptIn(kotlin.time.ExperimentalTime::class)
class SqlDelightDiagnosticRepository : DiagnosticRepository, KoinComponent {
    private val db: RouteDatabase by inject()

    override fun enqueueTechnicalEvent(
        eventType: String,
        reasonCode: String?,
        occurredAt: Long,
        appVersion: String?,
        appBuild: Long?,
    ) {
        require(eventType.matches(Regex("[A-Z_]{3,48}")))
        require(reasonCode == null || reasonCode.matches(Regex("[A-Z0-9_]{3,64}")))
        val eventId = generateId()
        db.transaction {
            db.routeEventQueries.insertEvent(
                eventId = eventId,
                installationId = installationId(),
                routeIdHash = null,
                eventType = eventType,
                reasonCode = reasonCode,
                createdAt = occurredAt,
                appVersion = appVersion,
                appBuild = appBuild,
                dbVersion = RouteDatabase.Schema.version,
                detailsJson = null,
                uploadedAt = null,
            )
            db.diagnosticOutboxQueries.enqueue(eventId = eventId, createdAt = occurredAt)
        }
    }

    override fun getSummary(recentLimit: Long): DiagnosticSummary {
        val installationId = installationId()
        val migration = db.migrationStatusQueries.getStatus().executeAsOneOrNull()
        return DiagnosticSummary(
            diagnosticCode = diagnosticCode(installationId),
            dbVersion = RouteDatabase.Schema.version,
            pendingEvents = db.diagnosticOutboxQueries.countPending().executeAsOne(),
            recentEvents = db.routeEventQueries.getRecent(recentLimit).executeAsList().map { row ->
                DiagnosticEventSummary(
                    eventType = row.eventType,
                    reasonCode = row.reasonCode,
                    createdAt = row.createdAt,
                )
            },
            migrationStatus = migration?.status,
            migrationFromVersion = migration?.fromVersion,
            migrationToVersion = migration?.toVersion,
            routesBeforeMigration = migration?.routesBefore,
            routesAfterMigration = migration?.routesAfter,
        )
    }

    override fun getReadyBatch(now: Long, requestedLimit: Int): List<DiagnosticOutboxEvent> =
        db.diagnosticOutboxQueries.getReadyBatch(
            now = now,
            limit = DiagnosticOutboxPolicy.batchSize(requestedLimit),
        ).executeAsList().map { row ->
            DiagnosticOutboxEvent(
                eventId = row.eventId,
                installationId = row.installationId,
                eventType = row.eventType,
                reasonCode = row.reasonCode,
                occurredAt = row.createdAt,
                appVersion = row.appVersion,
                appBuild = row.appBuild,
                dbVersion = row.dbVersion,
                detailsJson = row.detailsJson,
                attemptCount = row.attemptCount,
            )
        }

    override fun markUploaded(eventIds: List<String>, uploadedAt: Long) {
        db.transaction {
            eventIds.distinct().forEach { eventId ->
                db.routeEventQueries.markUploaded(uploadedAt, eventId)
                db.diagnosticOutboxQueries.deleteUploaded(eventId)
            }
        }
    }

    override fun scheduleRetry(
        eventId: String,
        attemptCount: Long,
        now: Long,
        errorCode: String?,
    ) {
        db.diagnosticOutboxQueries.scheduleRetry(
            nextAttemptAt = DiagnosticOutboxPolicy.nextAttemptAt(now, attemptCount),
            lastErrorCode = errorCode?.take(64),
            eventId = eventId,
        )
    }

    private fun installationId(): String {
        db.diagnosticInstallationQueries.getInstallationId().executeAsOneOrNull()?.let { return it }
        val generated = generateId()
        db.diagnosticInstallationQueries.insertInstallation(
            installationId = generated,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
        return db.diagnosticInstallationQueries.getInstallationId().executeAsOne()
    }

    private fun diagnosticCode(installationId: String): String {
        val compact = installationId.filter(Char::isLetterOrDigit).uppercase()
        return "LD-${compact.takeLast(6).padStart(6, '0')}"
    }
}
