package com.z_company.domain.entities.diagnostic

object DiagnosticReportFormatter {
    fun format(
        summary: DiagnosticSummary,
        appVersion: String,
        androidVersion: String,
        device: String,
    ): String = buildString {
        appendLine("LocoDriver diagnostic report")
        appendLine("diagnosticCode=${summary.diagnosticCode}")
        appendLine("appVersion=$appVersion")
        appendLine("androidVersion=$androidVersion")
        appendLine("device=$device")
        appendLine("dbVersion=${summary.dbVersion}")
        appendLine("pendingEvents=${summary.pendingEvents}")
        appendLine("migrationStatus=${summary.migrationStatus ?: "unknown"}")
        appendLine("migrationFrom=${summary.migrationFromVersion ?: "unknown"}")
        appendLine("migrationTo=${summary.migrationToVersion ?: "unknown"}")
        appendLine("routesBeforeMigration=${summary.routesBeforeMigration ?: "unknown"}")
        appendLine("routesAfterMigration=${summary.routesAfterMigration ?: "unknown"}")
        appendLine("recentEvents:")
        summary.recentEvents.forEach { event ->
            append("- ").append(event.createdAt).append(' ')
                .append(event.eventType)
            event.reasonCode?.let { append(" reason=").append(it) }
            appendLine()
        }
    }
}
