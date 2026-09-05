package com.z_company.loco_driver.recovery

import android.content.Context
import com.z_company.domain.repositories.DiagnosticRepository
import org.json.JSONArray
import org.json.JSONObject

enum class RecoveryTelemetryType {
    RECOVERY_MODE_ENTERED,
    RECOVERY_SOURCE_SELECTED,
    RECOVERY_SUCCEEDED,
    RECOVERY_FAILED,
    SNAPSHOT_UPLOAD_SUCCEEDED,
    SNAPSHOT_UPLOAD_FAILED,
}

enum class RecoveryTelemetryReason {
    MIGRATION_FAILED,
    LOW_STORAGE,
    LOCAL,
    CLOUD,
    AUTHORIZATION_REQUIRED,
    SNAPSHOT_NOT_FOUND,
    NETWORK_OR_SERVER,
    VALIDATION_FAILED,
    UNKNOWN,
}

class RecoveryTelemetryQueue(private val context: Context) {
    fun record(type: RecoveryTelemetryType, reason: RecoveryTelemetryReason? = null) =
        synchronized(QUEUE_LOCK) {
        val current = decode()
        current += PendingRecoveryTelemetry(type, reason, System.currentTimeMillis())
        val bounded = current.takeLast(MAX_EVENTS)
        preferences().edit().putString(KEY_EVENTS, encode(bounded)).commit()
        }

    fun drainTo(
        diagnostics: DiagnosticRepository,
        appVersion: String,
        appBuild: Long,
    ) = synchronized(QUEUE_LOCK) {
        val pending = decode()
        if (pending.isEmpty()) return@synchronized
        pending.forEach { event ->
            diagnostics.enqueueTechnicalEvent(
                event.type.name,
                event.reason?.name,
                event.occurredAt,
                appVersion,
                appBuild,
            )
        }
        preferences().edit().remove(KEY_EVENTS).commit()
    }

    private fun decode(): MutableList<PendingRecoveryTelemetry> {
        val text = preferences().getString(KEY_EVENTS, null) ?: return mutableListOf()
        return runCatching {
            val array = JSONArray(text)
            buildList {
                for (index in 0 until minOf(array.length(), MAX_EVENTS)) {
                    val item = array.getJSONObject(index)
                    val type = RecoveryTelemetryType.valueOf(item.getString("type"))
                    val reason = item.optString("reason").takeIf(String::isNotBlank)
                        ?.let(RecoveryTelemetryReason::valueOf)
                    val occurredAt = item.getLong("occurredAt")
                    require(occurredAt > 0L)
                    add(PendingRecoveryTelemetry(type, reason, occurredAt))
                }
            }.toMutableList()
        }.getOrElse {
            preferences().edit().remove(KEY_EVENTS).commit()
            mutableListOf()
        }
    }

    private fun encode(events: List<PendingRecoveryTelemetry>): String =
        JSONArray().apply {
            events.forEach { event ->
                put(JSONObject().apply {
                    put("type", event.type.name)
                    event.reason?.let { put("reason", it.name) }
                    put("occurredAt", event.occurredAt)
                })
            }
        }.toString()

    private fun preferences() = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    private data class PendingRecoveryTelemetry(
        val type: RecoveryTelemetryType,
        val reason: RecoveryTelemetryReason?,
        val occurredAt: Long,
    )

    private companion object {
        val QUEUE_LOCK = Any()
        const val PREFERENCES = "recovery_telemetry_queue"
        const val KEY_EVENTS = "events_v1"
        const val MAX_EVENTS = 50
    }
}
