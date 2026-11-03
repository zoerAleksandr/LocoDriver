package com.z_company.domain.gnss

/**
 * Диагностика «дыр» в потоке принятых GNSS-фиксов (SCREEN_SPECS §6.9, «Диагностика дыр»). Без координат и PII:
 * только время, длительность, причины отказов и класс точности. Чистый код; не потокобезопасен (вызывать с одного потока).
 *
 * Дыра = принятый фикс пришёл не раньше чем через [GAP_MIN_MS] после предыдущего принятого. В запись дыры попадают число
 * доставленных системой фиксов за время дыры (0 - система молчала: Doze/ограничения батареи/смерть подписки; > 0 - фиксы были,
 * но отвергнуты) и счётчики отказов по причинам.
 */
class GpsFixDiagnostics(private val maxGaps: Int = MAX_GAPS) {

    /** Одна дыра. [startedWallMs] - стенные часы последнего принятого фикса до дыры, [durationMs] - длительность. */
    data class Gap(
        val startedWallMs: Long,
        val durationMs: Long,
        val delivered: Int,
        val rejected: Map<String, Int>,
        val accuracyClass: String,
    ) {
        fun serialize(): String =
            "t=$startedWallMs;dur=${durationMs / 1000};deliv=$delivered;acc=$accuracyClass;rej=" +
                rejected.entries.joinToString(",") { "${it.key}:${it.value}" }
    }

    private val totals = LinkedHashMap<String, Int>()
    private val gapRejected = LinkedHashMap<String, Int>()
    private val gaps = ArrayDeque<Gap>()
    private var deliveredInGap = 0

    var delivered = 0
        private set
    var accepted = 0
        private set
    var maxGapMs = 0L
        private set

    /** Стенные часы последней доставки фикса системой (любого, даже отвергнутого); null - не было. */
    var lastDeliveryWallMs: Long? = null
        private set

    /** Стенные часы последнего принятого фикса; null - не было. */
    var lastAcceptedWallMs: Long? = null
        private set

    /** Фикс доставлен системой (до всех проверок). */
    fun onDelivered(wallMs: Long) {
        delivered++
        deliveredInGap++
        lastDeliveryWallMs = wallMs
    }

    /** Фикс отвергнут по причине [reason] (короткий код: TIME, SPEED_CLAIM, JUMP, NETWORK_MISMATCH ...). */
    fun onRejected(reason: String) {
        totals[reason] = (totals[reason] ?: 0) + 1
        gapRejected[reason] = (gapRejected[reason] ?: 0) + 1
    }

    /** Фикс принят; возвращает дыру, если перед ним была пауза >= [GAP_MIN_MS]. */
    fun onAccepted(wallMs: Long, accuracyM: Float?): Gap? {
        accepted++
        val prev = lastAcceptedWallMs
        var gap: Gap? = null
        if (prev != null && wallMs - prev >= GAP_MIN_MS) {
            val g = Gap(prev, wallMs - prev, deliveredInGap, LinkedHashMap(gapRejected), accuracyClass(accuracyM))
            if (g.durationMs > maxGapMs) maxGapMs = g.durationMs
            gaps.addLast(g)
            while (gaps.size > maxGaps) gaps.removeFirst()
            gap = g
        }
        lastAcceptedWallMs = wallMs
        deliveredInGap = 0
        gapRejected.clear()
        return gap
    }

    fun reset() {
        totals.clear()
        gapRejected.clear()
        gaps.clear()
        deliveredInGap = 0
        delivered = 0
        accepted = 0
        maxGapMs = 0L
        lastDeliveryWallMs = null
        lastAcceptedWallMs = null
    }

    fun rejectedTotals(): Map<String, Int> = LinkedHashMap(totals)

    fun recentGaps(): List<Gap> = gaps.toList()

    companion object {
        const val GAP_MIN_MS = 20_000L
        const val MAX_GAPS = 20

        /** Грубый класс точности (без значений): G < 10 м, M < 30 м, B >= 30 м, N - неизвестна. */
        fun accuracyClass(accuracyM: Float?): String = when {
            accuracyM == null -> "N"
            accuracyM < 10f -> "G"
            accuracyM < 30f -> "M"
            else -> "B"
        }
    }
}
