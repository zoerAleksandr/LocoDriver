package com.z_company.repository.remote_rest.diagnostic

import com.z_company.domain.entities.diagnostic.DiagnosticOutboxEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiagnosticPayloadTest {
    @Test
    fun payloadDropsUnapprovedDetailsAndIdentifiers() {
        val mapped = DiagnosticPayloadMapper.event(
            DiagnosticOutboxEvent(
                eventId = "event",
                installationId = "installation",
                eventType = "ROUTE_PURGED",
                reasonCode = "USER_EMPTIED_TRASH",
                occurredAt = 1000,
                appVersion = "1.0",
                appBuild = 1,
                dbVersion = 13,
                detailsJson = "{\"routeId\":\"secret\",\"email\":\"secret@example.test\"}",
                attemptCount = 0,
            )
        )
        val json = Json.encodeToString(mapped)

        assertTrue(json.contains("ROUTE_PURGED"))
        assertFalse(json.contains("installation"))
        assertFalse(json.contains("routeId"))
        assertFalse(json.contains("email"))
        assertFalse(json.contains("secret"))
    }
}
