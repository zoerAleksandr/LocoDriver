package com.z_company.domain.entities.diagnostic

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class DiagnosticReportFormatterTest {
    @Test
    fun reportContainsOnlySummaryFields() {
        val report = DiagnosticReportFormatter.format(
            summary = DiagnosticSummary(
                diagnosticCode = "LD-ABC123",
                dbVersion = 13,
                pendingEvents = 2,
                recentEvents = listOf(DiagnosticEventSummary("ROUTE_RESTORED", null, 1000)),
                migrationStatus = "SUCCEEDED",
                migrationFromVersion = 12,
                migrationToVersion = 13,
                routesBeforeMigration = 4,
                routesAfterMigration = 4,
            ),
            appVersion = "1.0",
            androidVersion = "15",
            device = "Test device",
        )

        assertContains(report, "diagnosticCode=LD-ABC123")
        assertContains(report, "ROUTE_RESTORED")
        assertFalse(report.contains("routeId"))
        assertFalse(report.contains("email", ignoreCase = true))
        assertFalse(report.contains("token", ignoreCase = true))
    }
}
