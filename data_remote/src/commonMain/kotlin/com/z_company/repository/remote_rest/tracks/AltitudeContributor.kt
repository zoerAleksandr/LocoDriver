@file:OptIn(ExperimentalEncodingApi::class)

package com.z_company.repository.remote_rest.tracks

import com.z_company.domain.track.AltitudeContribCodec
import com.z_company.domain.track.AltitudeContribBuilder
import com.z_company.domain.track.AltitudeContribSettings
import com.z_company.domain.track.AltitudeContribStore
import com.z_company.domain.track.AxisCache
import com.z_company.domain.track.AxisPoint
import com.z_company.domain.track.PendingContribSession
import com.z_company.domain.track.SectionAxis
import com.z_company.domain.track.TrackBlobTools
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock

/**
 * Отправка обезличенных вкладов высот (согласие №2; SCREEN_SPECS §6.9, §17.7).
 *
 * Берёт накопленные за сеанс «АВТО» точки из [AltitudeContribStore], привязывает их к осям опубликованных участков
 * (`GET /v1/advisor/sections[/{id}]`, оси кэшируются по `axisHash`) и шлёт фрагменты `POST /v1/advisor/altitude-contrib`.
 * Тело вклада без user_id, времени и координат. Повторы безопасны: `contribId` детерминирован от сеанса/участка/оси/начала,
 * сервер отвечает `duplicate`. 409 `axis_changed` - ось перекачивается и вклад пересобирается под новый хэш.
 * Запись сеанса удаляется только когда все оси обработаны (удаление = «отправлено»). Координаты не логируются.
 */
class AltitudeContributor(
    private val api: AdvisorApi,
    private val store: AltitudeContribStore,
    private val axes: AxisCache,
    private val settings: AltitudeContribSettings,
    private val tools: TrackBlobTools,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    enum class Outcome {
        /** Всё, что можно было отправить, обработано (сеансы удалены). */
        DONE,

        /** Сеть, 429/5xx: повторить позже; сеансы сохранены. */
        RETRY_LATER,

        /** Согласия №2 нет (отозвано): накопленное удалено. */
        NO_CONSENT,

        /** Сессия недействительна (401): сеансы сохранены до следующего входа. */
        UNAUTHORIZED,
    }

    data class Report(val outcome: Outcome, val sent: Int = 0, val duplicates: Int = 0, val skipped: Int = 0)

    private class Counters {
        var sent = 0
        var duplicates = 0
        var skipped = 0
    }

    private enum class Step { OK, RETRY, UNAUTHORIZED, STOP }

    suspend fun run(token: String): Report {
        val c = Counters()
        return try {
            runInternal(token, c)
        } catch (e: CancellationException) {
            throw e
        } catch (e: AdvisorApiException) {
            Report(if (e.status == 401) Outcome.UNAUTHORIZED else Outcome.RETRY_LATER, c.sent, c.duplicates, c.skipped)
        } catch (e: Exception) {
            // Сеть/таймаут/разбор ответа: детали наружу не отдаём.
            Report(Outcome.RETRY_LATER, c.sent, c.duplicates, c.skipped)
        }
    }

    private fun report(o: Outcome, c: Counters) = Report(o, c.sent, c.duplicates, c.skipped)

    private suspend fun runInternal(token: String, c: Counters): Report {
        if (!settings.enabled()) {
            store.clear()
            return report(Outcome.NO_CONSENT, c)
        }
        val now = nowMs()
        val sessions = ArrayList<PendingContribSession>()
        for (id in store.pendingIds()) {
            val s = store.load(id)
            if (s == null || s.samples.isEmpty() || now - s.createdAtMs > MAX_AGE_MS) store.delete(id) else sessions += s
        }
        if (sessions.isEmpty()) return report(Outcome.DONE, c)

        val summaries = api.sections().filter { it.hasGeometry && it.axisHash != null }
        axes.retain(summaries.map { it.id }.toSet())
        for (summary in summaries) {
            val axis = fetchAxis(summary) ?: continue
            for (s in sessions) {
                if (!settings.enabled()) {
                    store.clear()
                    return report(Outcome.NO_CONSENT, c)
                }
                when (sendAxis(token, s, axis, c, allowRefetch = true)) {
                    Step.OK, Step.STOP -> Unit
                    Step.RETRY -> return report(Outcome.RETRY_LATER, c)
                    Step.UNAUTHORIZED -> return report(Outcome.UNAUTHORIZED, c)
                }
            }
        }
        sessions.forEach { store.delete(it.id) }
        return report(Outcome.DONE, c)
    }

    private suspend fun fetchAxis(summary: AdvisorSectionSummaryDto): SectionAxis? {
        val cached = axes.load(summary.id)
        if (cached != null && cached.axisHash == summary.axisHash) return cached
        return downloadAxis(summary.id)
    }

    private suspend fun downloadAxis(id: String): SectionAxis? {
        val d = api.section(id)
        val hash = d.axisHash ?: return null
        if (d.geometry.size < 2) return null
        val axis = SectionAxis(d.id, hash, d.geometry.map { AxisPoint(it.km, it.lat, it.lon) })
        axes.save(axis)
        return axis
    }

    private suspend fun sendAxis(
        token: String,
        session: PendingContribSession,
        axis: SectionAxis,
        c: Counters,
        allowRefetch: Boolean,
    ): Step {
        val fragments = withContext(Dispatchers.Default) { AltitudeContribBuilder.build(axis, session.samples) }
        for (f in fragments) {
            val body = AltitudeContribRequest(
                contribId = AltitudeContribCodec.contribId(session.id, f, tools),
                sectionId = f.sectionId,
                axisHash = f.axisHash,
                kmFromM = f.kmFromM,
                stepM = f.stepM,
                source = f.source,
                meanAccM = f.meanAccM,
                data = Base64.encode(AltitudeContribCodec.encode(f.altsDm, tools)),
                offsetM = f.offsetM,
            )
            try {
                val r = api.postAltitudeContrib(token, body)
                if (r.status == "duplicate") c.duplicates++ else c.sent++
            } catch (e: AdvisorApiException) {
                when {
                    e.status == 401 -> return Step.UNAUTHORIZED
                    e.status == 409 && e.code == "axis_changed" -> {
                        // Ось участка изменилась: старый вклад не шлём. Перекачиваем и пересобираем под новый хэш.
                        if (!allowRefetch) return Step.STOP
                        val fresh = downloadAxis(axis.sectionId)
                        if (fresh == null || fresh.axisHash == axis.axisHash) {
                            c.skipped++
                            return Step.STOP
                        }
                        return sendAxis(token, session, fresh, c, allowRefetch = false)
                    }
                    // Участок снят с публикации: остальные фрагменты этой оси бессмысленны.
                    e.status == 404 -> {
                        c.skipped++
                        return Step.STOP
                    }
                    // Данные отвергнуты окончательно (422/400/413): повтор не поможет, берём следующий фрагмент.
                    e.status in 400..499 && e.status != 408 && e.status != 429 -> c.skipped++
                    else -> return Step.RETRY
                }
            }
        }
        return Step.OK
    }

    companion object {
        /** Неотправленный сеанс старше недели удаляется (устарел, не копим точки на устройстве). */
        const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000
    }
}
