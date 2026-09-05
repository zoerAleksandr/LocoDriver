package com.z_company.domain.entities.diagnostic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiagnosticOutboxPolicyTest {
    @Test
    fun batchIsAlwaysBounded() {
        assertEquals(1L, DiagnosticOutboxPolicy.batchSize(0))
        assertEquals(10L, DiagnosticOutboxPolicy.batchSize(10))
        assertEquals(20L, DiagnosticOutboxPolicy.batchSize(100))
    }

    @Test
    fun retryUsesBoundedExponentialBackoff() {
        val now = 1_000L
        assertEquals(now + 60_000L, DiagnosticOutboxPolicy.nextAttemptAt(now, 0))
        assertEquals(now + 120_000L, DiagnosticOutboxPolicy.nextAttemptAt(now, 1))
        assertTrue(
            DiagnosticOutboxPolicy.nextAttemptAt(now, 100) <=
                now + 24L * 60L * 60L * 1000L
        )
    }
}
