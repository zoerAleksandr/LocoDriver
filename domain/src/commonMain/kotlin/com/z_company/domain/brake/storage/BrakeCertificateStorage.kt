package com.z_company.domain.brake.storage

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import com.z_company.domain.brake.BrakeCertificateData

/**
 * Справка ВУ-45 в локальном хранилище: одна текущая справка на поезд (`trainId`) рейса (`routeId`).
 * [payload] - JSON `BrakeCertificateData` (строка; схема может расти без миграций БД). [updatedAt] - мс, LWW.
 */
data class StoredBrakeCertificate(
    val certificateId: String,
    val routeId: String,
    val trainId: String?,
    val payload: String,
    val updatedAt: Long,
    /** Надгробие: справка удалена локально, но ещё не доставлена на сервер (full-replace без неё). */
    val isDeleted: Boolean = false,
)

/** Элемент `brakeCertificates` в SyncData (контракт: docs сервера VU45_CERTIFICATES_API.md); `data` непрозрачный JSON-объект. */
@Serializable
data class BrakeCertificateSync(
    val certificateId: String,
    val routeId: String? = null,
    val trainId: String? = null,
    val updatedAt: Long = 0L,
    val data: JsonElement,
)

interface BrakeCertificateRepository {
    fun getByTrain(trainId: String): StoredBrakeCertificate?
    fun listByRoute(routeId: String, includeDeleted: Boolean = false): List<StoredBrakeCertificate>
    fun upsert(cert: StoredBrakeCertificate)
    /** Помечает справки поезда удалёнными (надгробие до следующей отправки рейса). */
    fun markDeletedByTrain(trainId: String, now: Long)
    /** Физически удаляет все справки рейса (рейс удалён навсегда). */
    fun deleteByRoute(routeId: String)
    /** Удаляет доставленные надгробия рейса. */
    fun purgeTombstones(routeId: String)
}

object BrakeCertificateMerge {
    /** LWW по updatedAt: что делать с пришедшей с сервера справкой. */
    enum class Action { INSERT, UPDATE, KEEP_LOCAL }

    fun decide(local: StoredBrakeCertificate?, remoteUpdatedAt: Long): Action = when {
        local == null -> Action.INSERT
        remoteUpdatedAt > local.updatedAt -> Action.UPDATE
        else -> Action.KEEP_LOCAL
    }
}

class BrakeCertificateUseCase(
    private val repository: BrakeCertificateRepository,
    private val now: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }

    fun loadForTrain(trainId: String): BrakeCertificateData? {
        val stored = repository.getByTrain(trainId)?.takeIf { !it.isDeleted } ?: return null
        return runCatching { json.decodeFromString(BrakeCertificateData.serializer(), stored.payload) }
            .getOrNull()?.copy(id = stored.certificateId, updatedAt = stored.updatedAt)
    }

    /** Сохраняет справку поезда. Возвращает сохранённую (с id и updatedAt). [data.id] пуст - id создаётся. */
    fun save(data: BrakeCertificateData, routeId: String, trainId: String, newId: () -> String): BrakeCertificateData {
        val id = data.id.ifBlank { repository.getByTrain(trainId)?.certificateId ?: newId() }
        val ts = now()
        val saved = data.copy(id = id, routeId = routeId, trainId = trainId, updatedAt = ts)
        repository.upsert(
            StoredBrakeCertificate(
                certificateId = id, routeId = routeId, trainId = trainId,
                payload = json.encodeToString(BrakeCertificateData.serializer(), saved.copy(updatedAt = 0L)), updatedAt = ts,
            ),
        )
        return saved
    }

    fun deleteForTrain(trainId: String) = repository.markDeletedByTrain(trainId, now())
    fun deleteForRoute(routeId: String) = repository.deleteByRoute(routeId)
    fun purgeTombstones(routeId: String) = repository.purgeTombstones(routeId)

    /**
     * Что отправить в `brakeCertificates`: null - у рейса нет справок и нет недоставленных удалений (поле не трогаем);
     * список (в т.ч. пустой) - full-replace на сервере.
     */
    fun forUpload(routeId: String): List<BrakeCertificateSync>? {
        val all = repository.listByRoute(routeId, includeDeleted = true)
        if (all.isEmpty()) return null
        return all.filter { !it.isDeleted }.mapNotNull { c ->
            runCatching {
                BrakeCertificateSync(c.certificateId, c.routeId, c.trainId, c.updatedAt, json.parseToJsonElement(c.payload))
            }.getOrNull()
        }
    }

    /** Слияние пришедших с сервера справок рейса (LWW); null - сервер поле не знает, ничего не делаем. Удалений не применяет. */
    fun mergeRemote(routeId: String, remote: List<BrakeCertificateSync>?): Int {
        if (remote.isNullOrEmpty()) return 0
        var changed = 0
        for (r in remote) {
            val local = repository.listByRoute(routeId, includeDeleted = true).firstOrNull { it.certificateId == r.certificateId }
            if (local?.isDeleted == true && r.updatedAt <= local.updatedAt) continue
            val action = BrakeCertificateMerge.decide(local, r.updatedAt)
            if (action == BrakeCertificateMerge.Action.KEEP_LOCAL) continue
            repository.upsert(
                StoredBrakeCertificate(r.certificateId, routeId, r.trainId, r.data.toString(), r.updatedAt),
            )
            changed++
        }
        return changed
    }
}
