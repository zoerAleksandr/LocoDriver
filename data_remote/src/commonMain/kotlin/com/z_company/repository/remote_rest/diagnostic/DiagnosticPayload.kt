package com.z_company.repository.remote_rest.diagnostic

import com.z_company.domain.entities.diagnostic.DiagnosticOutboxEvent
import kotlinx.serialization.Serializable

@Serializable
data class DiagnosticEventsRequest(
    val installationId: String,
    val diagnosticCode: String,
    val appVersion: String,
    val appBuild: Long,
    val androidVersion: String,
    val deviceManufacturer: String,
    val deviceModel: String,
    val dbVersion: Long? = null,
    val migrationStatus: String? = null,
    val events: List<DiagnosticEventRequest>,
)

@Serializable
data class DiagnosticEventRequest(
    val eventId: String,
    val eventType: String,
    val occurredAt: Long,
    val reasonCode: String? = null,
    val dbVersion: Long? = null,
)

@Serializable
data class DiagnosticEventsResponse(
    val accepted: Int,
    val duplicates: Int,
    val serverTime: Long,
)

object DiagnosticPayloadMapper {
    fun event(source: DiagnosticOutboxEvent): DiagnosticEventRequest =
        DiagnosticEventRequest(
            eventId = source.eventId,
            eventType = source.eventType,
            occurredAt = source.occurredAt,
            reasonCode = source.reasonCode,
            dbVersion = source.dbVersion,
        )
}
