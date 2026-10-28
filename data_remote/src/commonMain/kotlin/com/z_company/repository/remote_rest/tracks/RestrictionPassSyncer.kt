package com.z_company.repository.remote_rest.tracks

import com.z_company.domain.track.RestrictionJournalStore
import com.z_company.domain.track.TrackSyncSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Хранилище метки последней загруженной записи (для догрузки изменений с запасом по часам). */
interface RestrictionSyncCursor {
    fun lastPulledMs(): Long
    fun setLastPulledMs(value: Long)
}

/**
 * Синхронизация журнала проследования с сервером (`/v1/restriction_passes`). Порядок: отправить изменённые записи
 * (LWW по `updatedAt`), затем догрузить изменения с сервера (других устройств) и слить по LWW. Идемпотентно, безопасно к
 * повторам. Без согласия №1 уходят только удаления (сервер так же); без подписки - ничего (как треки). Содержимое не логируется.
 */
class RestrictionPassSyncer(
    private val api: RestrictionPassApi,
    private val store: RestrictionJournalStore,
    private val settings: TrackSyncSettings,
    private val cursor: RestrictionSyncCursor,
    private val isSubscribed: suspend () -> Boolean = { true },
) {
    enum class Outcome { DONE, RETRY_LATER, NO_CONSENT, NO_SUBSCRIPTION, UNAUTHORIZED }

    data class Report(val outcome: Outcome, val sent: Int = 0, val pulled: Int = 0)

    suspend fun run(token: String): Report {
        var sent = 0
        var pulled = 0
        return try {
            val consent = settings.localConsent()
            if (!isSubscribed()) return Report(Outcome.NO_SUBSCRIPTION)
            var retry = false
            // 1. Отправка. Без согласия №1 - только надгробия (пользователь вправе убрать то, что уже на сервере).
            while (true) {
                val batch = withContext(Dispatchers.Default) { store.pendingUpload(RestrictionPassApi.MAX_BATCH) }
                    .filter { consent || it.isDeleted }
                if (batch.isEmpty()) break
                val resp = api.put(token, batch)
                // Записи, чей рейс ещё не доехал до сервера, и упёршиеся в квоту остаются неотправленными (повтор позже).
                val keep = resp.skipped.filter { it.reason == "route_unavailable" || it.reason == "quota" }
                    .map { it.trainId to it.warningId }.toSet()
                val done = batch.filter { (it.trainId to it.warningId) !in keep }
                withContext(Dispatchers.Default) { store.markUploaded(done) }
                sent += resp.accepted
                if (keep.isNotEmpty()) { retry = true; break }
                if (done.isEmpty()) break
            }
            if (!consent) return Report(if (retry) Outcome.RETRY_LATER else Outcome.NO_CONSENT, sent, 0)
            // 2. Догрузка: с запасом в сутки назад от последней метки (часы устройств расходятся), слияние идемпотентно.
            val since = (cursor.lastPulledMs() - SKEW_MS).coerceAtLeast(0L)
            var next: String? = null
            var maxSeen = cursor.lastPulledMs()
            while (true) {
                val page = api.list(token, next, since.takeIf { it > 0 }, RestrictionPassApi.MAX_BATCH)
                if (page.passes.isNotEmpty()) {
                    withContext(Dispatchers.Default) { store.mergeRemote(page.passes) }
                    pulled += page.passes.size
                    maxSeen = maxOf(maxSeen, page.passes.maxOf { it.updatedAt })
                }
                next = page.nextCursor
                if (next == null) break
            }
            cursor.setLastPulledMs(maxSeen)
            Report(if (retry) Outcome.RETRY_LATER else Outcome.DONE, sent, pulled)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RestrictionPassApiException) {
            Report(
                when {
                    e.status == 401 -> Outcome.UNAUTHORIZED
                    e.status == 403 && e.code == "consent_required" -> Outcome.NO_CONSENT
                    else -> Outcome.RETRY_LATER
                },
                sent, pulled,
            )
        } catch (e: Exception) {
            // Сеть/таймаут/разбор: детали (возможно, с данными) наружу не отдаём.
            Report(Outcome.RETRY_LATER, sent, pulled)
        }
    }

    private companion object {
        const val SKEW_MS = 24L * 3600_000L
    }
}
