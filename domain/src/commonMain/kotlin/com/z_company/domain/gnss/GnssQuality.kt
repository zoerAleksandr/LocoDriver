package com.z_company.domain.gnss

import kotlin.math.ceil

/** Уровень связи со спутниками (SCREEN_SPECS §6.9, §6.2 п.6а). */
enum class GnssLevel { GOOD, WEAK, LOST, UNKNOWN, INTERFERENCE }

/**
 * Качество GNSS-сигнала.
 *
 * @property satellitesUsed спутников в решении; null - статус спутников недоступен.
 * @property satellitesVisible спутников в поле зрения; null - статус недоступен.
 * @property accuracyM точность последнего fix, м; null - нет fix или провайдер её не сообщил.
 * @property lastFixAgeMs возраст последнего fix, мс; null - fix ещё не было.
 */
data class GnssQuality(
    val level: GnssLevel = GnssLevel.UNKNOWN,
    val satellitesUsed: Int? = null,
    val satellitesVisible: Int? = null,
    val accuracyM: Float? = null,
    val lastFixAgeMs: Long? = null,
) {
    /** Сигнала нет: записи стоянок и отправлений приостановлены. */
    val isLost: Boolean get() = level == GnssLevel.LOST
}

/** Все пороги в одном месте. */
object GnssQualityConfig {
    /** GOOD: fix не старше. */
    const val GOOD_MAX_AGE_MS = 5_000L
    /** GOOD: точность не хуже, м. */
    const val GOOD_MAX_ACCURACY_M = 20f
    /** GOOD: спутников в решении не меньше. */
    const val GOOD_MIN_SATELLITES = 4
    /** LOST: fix старше этого. */
    const val LOST_AGE_MS = 15_000L
    /** Точность хуже этой - отсчёт недостоверен (детектор остановок, запись точек стоянки). */
    const val DETECTOR_MAX_ACCURACY_M = 50f

    /** Гистерезис: ухудшение (GOOD->WEAK) подтверждается столько. LOST наступает сразу по возрасту fix. */
    const val DOWNGRADE_HOLD_MS = 3_000L
    /** Гистерезис: улучшение подтверждается столько. */
    const val UPGRADE_HOLD_MS = 5_000L
    /** Сколько после старта записи уровень UNKNOWN (ждём первый fix), потом LOST. */
    const val FIRST_FIX_GRACE_MS = LOST_AGE_MS
    /** LOST дольше этого - одноразовое уведомление «Потерян сигнал GPS». */
    const val LOST_NOTIFY_AFTER_MS = 30_000L
    /** «Помеха GPS»: отвергнутых как подмена/скачок фиксов за последние 60 с не меньше. */
    const val INTERFERENCE_MIN_REJECTS = 2
}

/** Сырой (без сглаживания) уровень по мгновенным показателям. */
object GnssQualityRules {
    /**
     * [fixAgeMs] null - fix ещё не было. [satellitesUsed] null - статус спутников неизвестен (критерий не применяется).
     * Точность хуже 50 м при свежем fix - тоже WEAK.
     */
    fun rawLevel(fixAgeMs: Long?, accuracyM: Float?, satellitesUsed: Int?, providerEnabled: Boolean = true): GnssLevel {
        if (!providerEnabled) return GnssLevel.LOST
        if (fixAgeMs == null) return GnssLevel.UNKNOWN
        if (fixAgeMs > GnssQualityConfig.LOST_AGE_MS) return GnssLevel.LOST
        val accOk = accuracyM == null || accuracyM <= GnssQualityConfig.GOOD_MAX_ACCURACY_M
        val satsOk = satellitesUsed == null || satellitesUsed >= GnssQualityConfig.GOOD_MIN_SATELLITES
        val fresh = fixAgeMs <= GnssQualityConfig.GOOD_MAX_AGE_MS
        return if (fresh && accOk && satsOk) GnssLevel.GOOD else GnssLevel.WEAK
    }

    /** Отсчёт пригоден для детектора/записи стоянок: достоверная точность и не устаревший. */
    fun isUsableFix(fixAgeMs: Long, accuracyM: Float?): Boolean =
        fixAgeMs <= GnssQualityConfig.LOST_AGE_MS &&
            (accuracyM == null || accuracyM <= GnssQualityConfig.DETECTOR_MAX_ACCURACY_M)
}

/**
 * Сглаживание уровня, чтобы значок не мигал: LOST - сразу; ухудшение - после [GnssQualityConfig.DOWNGRADE_HOLD_MS]
 * стабильного худшего значения; улучшение - после [GnssQualityConfig.UPGRADE_HOLD_MS]. Не потокобезопасен.
 */
class GnssQualityEvaluator {
    private var shown = GnssLevel.UNKNOWN
    private var candidate: GnssLevel? = null
    private var candidateSince = 0L

    fun reset() {
        shown = GnssLevel.UNKNOWN
        candidate = null
    }

    /**
     * [startedMs] - момент старта записи (для ожидания первого fix), [lastFixMs] - момент (по тем же часам, что [nowMs])
     * получения последнего fix или null. [lostWithoutFix] - переводить ли «fix так и не было» в LOST после
     * [GnssQualityConfig.FIRST_FIX_GRACE_MS] (запись) или оставлять UNKNOWN (просмотр на карте).
     */
    fun update(
        nowMs: Long,
        startedMs: Long,
        lastFixMs: Long?,
        accuracyM: Float?,
        satellitesUsed: Int?,
        satellitesVisible: Int?,
        providerEnabled: Boolean = true,
        lostWithoutFix: Boolean = true,
        rejectedRecent: Int = 0,
    ): GnssQuality {
        val age = lastFixMs?.let { (nowMs - it).coerceAtLeast(0L) }
        var raw = GnssQualityRules.rawLevel(age, accuracyM, satellitesUsed, providerEnabled)
        // [lostWithoutFix] = false (индикатор на карте без записи): пока fix не было ни разу - «нет данных» (UNKNOWN), не LOST.
        if (lostWithoutFix && raw == GnssLevel.UNKNOWN && nowMs - startedMs > GnssQualityConfig.FIRST_FIX_GRACE_MS) raw = GnssLevel.LOST
        // «Помеха GPS» (подмена/скачки): >= 2 отвергнутых фикса за 60 с - приоритетнее остальных уровней, без сглаживания.
        if (providerEnabled && rejectedRecent >= GnssQualityConfig.INTERFERENCE_MIN_REJECTS) raw = GnssLevel.INTERFERENCE
        shown = if (raw == GnssLevel.INTERFERENCE) raw.also { candidate = null } else smooth(raw, nowMs)
        return GnssQuality(shown, satellitesUsed, satellitesVisible, accuracyM, age)
    }

    private fun rank(l: GnssLevel) = when (l) {
        GnssLevel.GOOD -> 2
        GnssLevel.WEAK -> 1
        GnssLevel.LOST, GnssLevel.INTERFERENCE -> 0
        GnssLevel.UNKNOWN -> -1
    }

    private fun smooth(raw: GnssLevel, nowMs: Long): GnssLevel {
        if (raw == shown) {
            candidate = null
            return shown
        }
        // Потеря и «ещё не знаем» - без задержки.
        if (raw == GnssLevel.LOST || raw == GnssLevel.UNKNOWN || shown == GnssLevel.UNKNOWN) {
            candidate = null
            return raw
        }
        if (candidate != raw) {
            candidate = raw
            candidateSince = nowMs
        }
        val hold = if (rank(raw) < rank(shown)) GnssQualityConfig.DOWNGRADE_HOLD_MS else GnssQualityConfig.UPGRADE_HOLD_MS
        if (nowMs - candidateSince >= hold) {
            candidate = null
            return raw
        }
        return shown
    }
}

/** Тексты подсказок (SCREEN_SPECS §6.9). */
object GnssQualityText {
    private fun satellites(n: Int): String {
        val m100 = n % 100
        val m10 = n % 10
        val word = when {
            m100 in 11..14 -> "спутников"
            m10 == 1 -> "спутник"
            m10 in 2..4 -> "спутника"
            else -> "спутников"
        }
        return "$n $word"
    }

    private fun details(q: GnssQuality): String = buildList {
        q.satellitesUsed?.let { add(satellites(it)) }
        q.accuracyM?.let { add("точность ±${ceil(it.toDouble()).toInt()} м") }
    }.joinToString(", ")

    fun tooltip(q: GnssQuality): String = when (q.level) {
        GnssLevel.GOOD -> "GPS: хороший сигнал" + details(q).let { if (it.isEmpty()) "" else ", $it" }
        GnssLevel.WEAK -> "GPS: слабый сигнал" + details(q).let { if (it.isEmpty()) "" else ", $it" }
        GnssLevel.LOST -> "Нет сигнала GPS — записи стоянок и отправлений приостановлены"
        GnssLevel.UNKNOWN -> "GPS: поиск спутников"
        GnssLevel.INTERFERENCE -> "Помеха GPS — координаты скачут или подменены, такие точки не учитываются"
    }

    /** Для чтения скринридером. */
    fun description(q: GnssQuality): String = "Сигнал GPS. " + tooltip(q)
}
