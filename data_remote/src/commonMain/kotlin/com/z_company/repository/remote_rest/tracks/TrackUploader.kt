@file:OptIn(ExperimentalEncodingApi::class)

package com.z_company.repository.remote_rest.tracks

import com.z_company.domain.track.TrackBlobTools
import com.z_company.domain.track.TrackInfo
import com.z_company.domain.track.TrackPacking
import com.z_company.domain.track.TrackRepository
import com.z_company.domain.track.TrackSyncSettings
import com.z_company.domain.track.TrackTrainResolution
import com.z_company.domain.track.TrackTrainResolver
import com.z_company.domain.track.TrackUploadState
import com.z_company.domain.track.TrackWireCodec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock

/**
 * Отправка локальных треков на сервер (этап 3б; сервер — `/v1/tracks`, дизайн — docs/TRAIN_TRACKS_SERVER_DESIGN.md).
 *
 * Идемпотентна и безопасна к повторам: чанки детерминированы (одни и те же точки -> те же блобы и sha256),
 * повтор чанка сервер отвечает `duplicate`. Координаты не логируются и не попадают в тексты ошибок.
 *
 * Порядок прогона: согласие (только если расходится с серверным) -> очередь удалений -> треки, которые
 * закончились и не получают новых точек дольше [TrackPacking.QUIET_MS] -> доставка изменённой звезды.
 */
class TrackUploader(
    private val api: TracksApi,
    private val repository: TrackRepository,
    private val tools: TrackBlobTools,
    private val resolver: TrackTrainResolver,
    private val settings: TrackSyncSettings,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    enum class Outcome {
        /** Всё, что можно было отправить, отправлено. */
        DONE,

        /** Сеть, 429/5xx или рейс ещё не синхронизирован: повторить позже с задержкой. */
        RETRY_LATER,

        /** Согласия нет (или отозвано) — отправка не нужна. */
        NO_CONSENT,

        /** Квота на сервере исчерпана: повторы бессмысленны, пока пользователь не удалит треки. */
        QUOTA_EXCEEDED,

        /** Сессия недействительна (401). */
        UNAUTHORIZED,
    }

    data class Report(
        val outcome: Outcome,
        val uploaded: Int = 0,
        val remoteDeleted: Int = 0,
        val rejected: Int = 0,
        /** Есть треки, которые ещё пишутся (тишина < [TrackPacking.QUIET_MS]): запустить прогон позже. */
        val waitingForQuiet: Boolean = false,
    )

    private class Counters {
        var uploaded = 0
        var remoteDeleted = 0
        var rejected = 0
        var waiting = false
    }

    suspend fun run(token: String): Report {
        val c = Counters()
        return try {
            runInternal(token, c)
        } catch (e: CancellationException) {
            throw e
        } catch (e: TracksApiException) {
            report(classify(e), c)
        } catch (e: Exception) {
            // Сеть/таймаут/разбор ответа: детали (возможно, с данными) наружу не отдаём.
            report(Outcome.RETRY_LATER, c)
        }
    }

    private fun report(outcome: Outcome, c: Counters) =
        Report(outcome, c.uploaded, c.remoteDeleted, c.rejected, c.waiting)

    private fun classify(e: TracksApiException): Outcome = when {
        e.status == 401 -> Outcome.UNAUTHORIZED
        e.status == 413 && e.code == "quota_exceeded" -> Outcome.QUOTA_EXCEEDED
        else -> Outcome.RETRY_LATER
    }

    private suspend fun runInternal(token: String, c: Counters): Report {
        // 1. Согласие: сервер должен знать то же, что и устройство.
        val local = settings.localConsent()
        if (settings.serverConsent() != local) {
            if (!local && settings.serverConsent() == null) {
                // Согласие никогда не давалось: серверу сообщать нечего.
                settings.setServerConsent(false)
            } else {
                api.putConsent(token, TrackConsentRequest(local, POLICY_VERSION))
                settings.setServerConsent(local)
                // Отзыв не удаляет треки (ни на устройстве, ни на сервере): метки «отправлено» остаются верными.
            }
        }

        // 2. Очередь удалений (удаление трека/поезда/рейса на устройстве). Доставляется и без согласия.
        for (id in repository.pendingRemoteDeletes()) {
            api.delete(token, id)
            repository.clearPendingRemoteDelete(id)
            c.remoteDeleted++
        }
        if (!local) return report(Outcome.NO_CONSENT, c)

        // 3. Отправка законченных треков.
        val now = nowMs()
        var retry = false
        for (info in withContext(Dispatchers.Default) { repository.tracks() }) {
            val count = withContext(Dispatchers.Default) { repository.pointCount(info.trackId) }
            if (!needsUpload(info, count)) continue
            if (now - info.endedAt < TrackPacking.QUIET_MS) {
                c.waiting = true
                continue
            }
            when (uploadOne(token, info)) {
                Step.Uploaded -> c.uploaded++
                Step.Rejected -> c.rejected++
                Step.Skipped -> Unit
                Step.RetryLater -> retry = true
            }
        }

        // 4. Звезда, изменённая у уже отправленных треков.
        for (info in withContext(Dispatchers.Default) { repository.tracks() }) {
            if (info.uploadState == TrackUploadState.UPLOADED && info.favoriteDirty) {
                try {
                    api.patch(token, info.trackId, TrackPatchRequest(isFavorite = info.isFavorite))
                    repository.clearFavoriteDirty(info.trackId)
                } catch (e: TracksApiException) {
                    if (e.status == 404) {
                        // Сервер трека не знает (истёк/удалён): отправим заново целиком.
                        repository.setUploadState(info.trackId, TrackUploadState.PENDING, 0)
                        retry = true
                    } else {
                        throw e
                    }
                }
            }
        }
        return report(if (retry) Outcome.RETRY_LATER else Outcome.DONE, c)
    }

    private fun needsUpload(info: TrackInfo, pointCount: Long): Boolean = when (info.uploadState) {
        TrackUploadState.PENDING -> true
        // Трек продолжили после отправки (тот же сеанс): отправляем заново целиком.
        TrackUploadState.UPLOADED -> pointCount > info.uploadedPoints
        else -> false
    }

    private enum class Step { Uploaded, Rejected, Skipped, RetryLater }

    private suspend fun uploadOne(token: String, info: TrackInfo): Step {
        val resolution = resolver.resolve(info.trainId)
        val (routeId, title) = when (resolution) {
            is TrackTrainResolution.Found -> resolution.routeId to resolution.title
            // Поезд удалён: осиротевший трек уберёт TrackOrphanCleaner, отправлять нечего.
            TrackTrainResolution.Missing, TrackTrainResolution.Unknown -> return Step.Skipped
        }
        val prepared = withContext(Dispatchers.Default) { prepare(info.trackId) } ?: run {
            // Точек нет — отправлять нечего.
            repository.deleteTrack(info.trackId, queueRemoteDelete = false)
            return Step.Skipped
        }
        try {
            if (info.uploadState == TrackUploadState.UPLOADED) api.delete(token, info.trackId)
            sendTrack(token, info.trackId, routeId, title, prepared)
        } catch (e: TracksApiException) {
            return when {
                e.status == 409 && e.code == "route_not_synced" -> Step.RetryLater
                e.status == 409 && e.code == "chunk_conflict" -> {
                    // На сервере осталась частичная другая версия трека: стираем и отправим заново.
                    api.delete(token, info.trackId)
                    Step.RetryLater
                }
                // Рейс удалён на сервере: трек удаляется вместе с рейсом (решение 08.10.2026).
                e.status == 410 -> {
                    repository.deleteTrack(info.trackId, queueRemoteDelete = false)
                    Step.Skipped
                }
                e.status == 403 && e.code == "consent_required" -> {
                    // Сервер не знает о согласии (например, сброс): отправим его заново в следующем прогоне.
                    settings.setServerConsent(null)
                    Step.RetryLater
                }
                e.status == 413 && e.code == "quota_exceeded" -> {
                    repository.setUploadState(info.trackId, TrackUploadState.REJECTED, 0)
                    throw e
                }
                // Данные отвергнуты окончательно (422/404/413 по размеру): повтор не поможет.
                e.status == 404 || e.status == 413 || e.status == 422 -> {
                    repository.setUploadState(info.trackId, TrackUploadState.REJECTED, 0)
                    Step.Rejected
                }
                else -> throw e
            }
        }
        repository.setUploadState(info.trackId, TrackUploadState.UPLOADED, prepared.pointsCount.toLong())
        // Звезда не входит в мету: доставляем отдельным PATCH.
        if (info.isFavorite) {
            api.patch(token, info.trackId, TrackPatchRequest(isFavorite = true))
        }
        repository.clearFavoriteDirty(info.trackId)
        return Step.Uploaded
    }

    private class Prepared(
        val meta: TrackMetaRequest,
        val blobs: List<ByteArray>,
        val pointsCount: Int,
    )

    private fun prepare(trackId: String): Prepared? {
        val points = repository.points(trackId)
        if (points.isEmpty()) return null
        val chunks = TrackPacking.chunks(points)
        val blobs = chunks.map { TrackWireCodec.encodeChunk(it, tools) }
        val stats = TrackPacking.stats(points)
        val preview = TrackWireCodec.encodeChunk(TrackPacking.preview(points), tools)
        val meta = TrackMetaRequest(
            title = "",
            startedAt = points.first().timeMs,
            endedAt = points.last().timeMs,
            distanceKm = stats.distanceKm,
            maxSpeedKmh = stats.maxSpeedKmh,
            avgSpeedKmh = stats.avgSpeedKmh,
            pointsCount = points.size,
            chunksExpected = blobs.size,
            preview = TrackEncPayload(TrackWireCodec.ENC_NAME, Base64.encode(preview)),
        )
        return Prepared(meta, blobs, points.size)
    }

    private suspend fun sendTrack(token: String, id: String, routeId: String, title: String, p: Prepared) {
        api.putTrack(token, id, p.meta.copy(routeId = routeId, title = title))
        for (n in p.blobs.indices) sendChunk(token, id, n, p.blobs[n])
        var missing = api.complete(token, id, TrackCompleteRequest(p.blobs.size)).missing
        var rounds = 0
        while (missing.isNotEmpty()) {
            // Докачка пропущенных чанков (повтор безопасен: тот же sha256 -> duplicate).
            if (++rounds > MAX_COMPLETE_ROUNDS) throw TracksApiException(0, "incomplete")
            for (n in missing) if (n in p.blobs.indices) sendChunk(token, id, n, p.blobs[n])
            missing = api.complete(token, id, TrackCompleteRequest(p.blobs.size)).missing
        }
    }

    private suspend fun sendChunk(token: String, id: String, n: Int, blob: ByteArray) {
        api.putChunk(
            token, id, n,
            TrackChunkRequest(TrackWireCodec.ENC_NAME, Base64.encode(blob), tools.sha256Hex(blob)),
        )
    }

    companion object {
        /** Версия политики конфиденциальности, под которую дано согласие (сервер хранит ≤ 16 символов). */
        const val POLICY_VERSION = "2026-10"
        private const val MAX_COMPLETE_ROUNDS = 3
    }
}
