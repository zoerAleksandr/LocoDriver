package com.z_company.domain.entities.diagnostic

data class DiagnosticOutboxEvent(
    val eventId: String,
    val installationId: String,
    val eventType: String,
    val reasonCode: String?,
    val occurredAt: Long,
    val appVersion: String?,
    val appBuild: Long?,
    val dbVersion: Long?,
    val detailsJson: String?,
    val attemptCount: Long,
)

object DiagnosticOutboxPolicy {
    const val MAX_BATCH_SIZE = 20
    private const val MAX_BACKOFF_MILLIS = 24L * 60L * 60L * 1000L
    private const val BASE_BACKOFF_MILLIS = 60_000L

    fun batchSize(requested: Int): Long = requested.coerceIn(1, MAX_BATCH_SIZE).toLong()

    fun nextAttemptAt(now: Long, attemptCount: Long): Long {
        val exponent = attemptCount.coerceIn(0L, 10L).toInt()
        val delay = (BASE_BACKOFF_MILLIS * (1L shl exponent))
            .coerceAtMost(MAX_BACKOFF_MILLIS)
        return now + delay
    }
}
