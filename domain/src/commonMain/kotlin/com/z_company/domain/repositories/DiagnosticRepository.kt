package com.z_company.domain.repositories

import com.z_company.domain.entities.diagnostic.DiagnosticSummary
import com.z_company.domain.entities.diagnostic.DiagnosticOutboxEvent

interface DiagnosticRepository {
    fun enqueueTechnicalEvent(
        eventType: String,
        reasonCode: String?,
        occurredAt: Long,
        appVersion: String?,
        appBuild: Long?,
    )
    fun getSummary(recentLimit: Long = 20): DiagnosticSummary
    fun getReadyBatch(now: Long, requestedLimit: Int = 20): List<DiagnosticOutboxEvent>
    fun markUploaded(eventIds: List<String>, uploadedAt: Long)
    fun scheduleRetry(eventId: String, attemptCount: Long, now: Long, errorCode: String?)
}
