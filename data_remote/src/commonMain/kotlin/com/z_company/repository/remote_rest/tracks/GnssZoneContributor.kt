package com.z_company.repository.remote_rest.tracks

import com.z_company.domain.gnss.GnssContribBuilder
import com.z_company.domain.gnss.GnssContribSettings
import com.z_company.domain.gnss.GnssContribStore
import com.z_company.domain.gnss.GnssContribTag
import kotlinx.coroutines.CancellationException
import kotlin.time.Clock

/**
 * Отправка обезличенного вклада «места без GPS» (согласие №2 в редакции с GPS; SCREEN_SPECS §6.9).
 *
 * Берёт неотправленные сеансы из [GnssContribStore] и шлёт `POST /v1/gnss/zones/contrib`: ячейки сетки ~250 м с маской видов,
 * случайный tag устройства (ротация 30 дней, не связан с аккаунтом), UTC-день сеанса. Идемпотентно: `contribId` = id сеанса,
 * повтор сервер считает `duplicate`. Файл сеанса удаляется только после ответа сервера (или окончательного отказа).
 * Пустой вклад не отправляется. 404/503 (сервер не готов), 429, сеть - сеанс остаётся до следующей попытки. Ячейки и tag не логируются.
 */
class GnssZoneContributor(
    private val api: GnssZonesApi,
    private val store: GnssContribStore,
    private val settings: GnssContribSettings,
    private val tag: GnssContribTag,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    enum class Outcome {
        DONE,

        /** Сеть, 404/429/5xx: повторить позже; сеансы сохранены. */
        RETRY_LATER,

        /** Согласия нет (отозвано или прежняя редакция): накопленное удалено. */
        NO_CONSENT,

        /** 401: сеансы сохранены до следующего входа. */
        UNAUTHORIZED,
    }

    data class Report(val outcome: Outcome, val sent: Int = 0, val duplicates: Int = 0, val dropped: Int = 0)

    suspend fun run(token: String): Report {
        var sent = 0
        var dup = 0
        var dropped = 0
        try {
            if (!settings.enabled()) {
                store.clear()
                return Report(Outcome.NO_CONSENT)
            }
            val now = nowMs()
            for (id in store.pendingIds()) {
                // Согласие могло быть отозвано посреди отправки.
                if (!settings.enabled()) {
                    store.clear()
                    return Report(Outcome.NO_CONSENT, sent, dup, dropped)
                }
                val s = store.load(id)
                if (s == null || s.cells.isEmpty() || now - s.createdAtMs > MAX_AGE_MS || s.createdAtMs > now + DAY_MS) {
                    store.delete(id)
                    dropped++
                    continue
                }
                val body = GnssZoneContribRequest(
                    contribId = s.id,
                    tag = tag.current(now),
                    step = GRID_STEP,
                    day = GnssContribBuilder.utcDay(s.createdAtMs),
                    cells = s.cells.take(GnssContribBuilder.MAX_CELLS)
                        .map { GnssContribCellDto(it.cell.iy, it.cell.ix, it.kinds, it.count) },
                )
                try {
                    val r = api.postContrib(token, body)
                    if (r.duplicate) dup++ else sent++
                    store.delete(id)
                } catch (e: GnssZonesApiException) {
                    when {
                        e.status == 401 -> return Report(Outcome.UNAUTHORIZED, sent, dup, dropped)
                        // Окончательный отказ (формат/размер/версия сетки): повтор не поможет.
                        e.status in listOf(400, 413, 422) -> {
                            store.delete(id)
                            dropped++
                        }
                        // 404 (сервер ещё не развёрнут), 403, 408, 429, 5xx: ждём.
                        else -> return Report(Outcome.RETRY_LATER, sent, dup, dropped)
                    }
                }
            }
            return Report(Outcome.DONE, sent, dup, dropped)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Сеть/таймаут/разбор ответа: детали наружу не отдаём.
            return Report(Outcome.RETRY_LATER, sent, dup, dropped)
        }
    }

    companion object {
        /** Версия сетки (шаг 0,00225°). */
        const val GRID_STEP = 1

        /** Сервер принимает день поездки не старше 7 суток: неотправленное старше 6 суток выбрасываем. */
        const val MAX_AGE_MS = 6L * 24 * 60 * 60 * 1000
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
