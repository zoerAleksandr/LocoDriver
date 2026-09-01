package com.z_company.loco_driver

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.z_company.domain.entities.diagnostic.DiagnosticOutboxEvent
import com.z_company.domain.entities.diagnostic.DiagnosticSummary
import com.z_company.domain.repositories.DiagnosticRepository
import com.z_company.loco_driver.recovery.RecoveryTelemetryQueue
import com.z_company.loco_driver.recovery.RecoveryTelemetryReason
import com.z_company.loco_driver.recovery.RecoveryTelemetryType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecoveryTelemetryQueueTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun setUp() = clear()
    @After fun tearDown() = clear()

    @Test
    fun queueIsBoundedAndDrainsOnlyOnce() {
        val queue = RecoveryTelemetryQueue(context)
        repeat(55) {
            queue.record(
                RecoveryTelemetryType.RECOVERY_FAILED,
                RecoveryTelemetryReason.VALIDATION_FAILED,
            )
        }
        val repository = RecordingDiagnosticRepository()

        queue.drainTo(repository, "1.0-test", 999)
        queue.drainTo(repository, "1.0-test", 999)

        assertEquals(50, repository.events.size)
        repository.events.forEach { event ->
            assertEquals("RECOVERY_FAILED", event.eventType)
            assertEquals("VALIDATION_FAILED", event.reasonCode)
            assertEquals("1.0-test", event.appVersion)
            assertEquals(999L, event.appBuild)
        }
    }

    private fun clear() {
        context.getSharedPreferences("recovery_telemetry_queue", 0).edit().clear().commit()
    }

    private data class RecordedEvent(
        val eventType: String,
        val reasonCode: String?,
        val appVersion: String?,
        val appBuild: Long?,
    )

    private class RecordingDiagnosticRepository : DiagnosticRepository {
        val events = mutableListOf<RecordedEvent>()

        override fun enqueueTechnicalEvent(
            eventType: String,
            reasonCode: String?,
            occurredAt: Long,
            appVersion: String?,
            appBuild: Long?,
        ) {
            require(occurredAt > 0)
            events += RecordedEvent(eventType, reasonCode, appVersion, appBuild)
        }

        override fun getSummary(recentLimit: Long): DiagnosticSummary = error("unused")
        override fun getReadyBatch(now: Long, requestedLimit: Int): List<DiagnosticOutboxEvent> =
            error("unused")
        override fun markUploaded(eventIds: List<String>, uploadedAt: Long) = Unit
        override fun scheduleRetry(
            eventId: String,
            attemptCount: Long,
            now: Long,
            errorCode: String?,
        ) = Unit
    }
}
