package com.z_company.domain.track

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Правила отчёта о проследовании ограничений (PDF формируется в приложении, SCREEN_SPECS §21 / «Журнал проследования»). */
object RestrictionReport {
    /**
     * Отчёт можно сформировать, когда у поезда есть записи и ВСЕ отслеживаемые проследованы. Ограничения без участка
     * ([RestrictionPass.tracked] = false) автоматически не отслеживаются и отчёт не блокируют (в отчёте они помечены).
     */
    fun isReady(entries: List<RestrictionPass>): Boolean {
        val live = entries.filter { !it.isDeleted }
        return live.isNotEmpty() && live.filter { it.tracked }.all { it.status == RestrictionStatus.DONE }
    }

    /** Сколько отслеживаемых записей ещё не проследовано (для подсказки на неактивной кнопке). */
    fun remaining(entries: List<RestrictionPass>): Int = entries.count { !it.isDeleted && it.tracked && it.status != RestrictionStatus.DONE }

    /** Сколько ограничений скорости превышено. */
    fun exceededCount(entries: List<RestrictionPass>): Int = entries.count { !it.isDeleted && it.exceeded }

    /** Строка отчёта/журнала в готовом для показа виде. */
    data class Row(
        val title: String,
        val place: String,
        val kmText: String?,
        val limit: String?,
        val enter: String,
        val exit: String,
        val maxSpeed: String,
        val exceeded: Boolean,
        val status: RestrictionStatus,
        /** Нет участка с профилем: данные проследования автоматически не пишутся. */
        val untracked: Boolean = false,
    )

    /** «28.10 14:05:09»; zone - id пояса отображения ("GMT+3"), при ошибке пояса - UTC. */
    fun formatTime(ms: Long, zone: String): String {
        val tz = runCatching { TimeZone.of(zone) }.getOrDefault(TimeZone.UTC)
        val dt = Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz)
        fun two(v: Int) = v.toString().padStart(2, '0')
        return "${two(dt.dayOfMonth)}.${two(dt.monthNumber)} ${two(dt.hour)}:${two(dt.minute)}:${two(dt.second)}"
    }

    private fun kmh(v: Double?): String = if (v == null) "—" else "${v.toInt()} км/ч"

    fun rows(entries: List<RestrictionPass>, zone: String): List<Row> =
        entries.filter { !it.isDeleted }.sortedWith(compareBy({ it.enterAt ?: Long.MAX_VALUE }, { it.updatedAt })).map { e ->
            val enter = if (!e.tracked && e.enterAt == null) "не отслеживается автоматически (нет профиля участка)"
            else if (e.enterAt == null) "не проследовано"
            else formatTime(e.enterAt, zone) + " · " + kmh(e.enterSpeedKmh) + if (e.enterApprox) " (по первому фиксу в зоне)" else ""
            val exit = when {
                e.kind != RestrictionKind.SPEED || !e.tracked -> "—"
                e.exitAt == null -> if (e.enterAt == null) "—" else "в зоне"
                else -> formatTime(e.exitAt, zone) + " · " + kmh(e.exitSpeedKmh) + if (e.exitByHead) " (по голове)" else ""
            }
            Row(
                title = e.title.replaceFirstChar { it.uppercase() },
                place = e.place,
                kmText = e.kmText(),
                limit = e.limitKmh?.let { "$it км/ч" },
                enter = enter,
                exit = exit,
                maxSpeed = if (e.kind == RestrictionKind.SPEED) kmh(e.maxSpeedKmh) else "—",
                exceeded = e.exceeded,
                status = e.status,
                untracked = !e.tracked,
            )
        }
}
