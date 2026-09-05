package com.z_company.domain.entities.diagnostic

data class DiagnosticEventSummary(
    val eventType: String,
    val reasonCode: String?,
    val createdAt: Long,
)

data class DiagnosticSummary(
    val diagnosticCode: String,
    val dbVersion: Long,
    val pendingEvents: Long,
    val recentEvents: List<DiagnosticEventSummary>,
    val migrationStatus: String?,
    val migrationFromVersion: Long?,
    val migrationToVersion: Long?,
    val routesBeforeMigration: Long?,
    val routesAfterMigration: Long?,
)
