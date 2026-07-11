package com.z_company.route.viewmodel

import kotlin.math.abs
import kotlin.math.roundToInt

// ════════════════════════════════════════════════════════════════════
// UI-модель экрана «Статистика». ViewModel считает реальные метрики и
// собирает уже отформатированный StatisticsUiState — экран его рендерит.
// ════════════════════════════════════════════════════════════════════

enum class StatTab { MONTH, YEAR, HISTORY }

data class StatDelta(
    val kind: String,          // "info" | "norm"
    val dir: String? = null,   // "up" | "down" | "flat" — для info
    val state: String? = null, // "over" | "ok" | "none" — для norm
    val pct: String? = null,
)

data class StatMetric(
    val key: String,
    val label: String,
    val value: String,
    val unit: String = "",
    val delta: StatDelta?,
    val prevValue: String,
    val prevBar: Float,
    val curBar: Float,
    val hasPrev: Boolean = false,
)

data class StatTopDirection(
    val from: String,
    val to: String,
    val trips: Int,
    val hours: String,
)

data class StatMonthBar(val label: String, val cur: Float, val prev: Float)

/** Строка «год за годом» для выбранной в «Истории» метрики. */
data class StatYearRow(
    val year: Int,
    val note: String?,
    val value: String,   // отформатированное значение метрики за год
    val unit: String,
    val fraction: Float, // доля от максимума (для полосы)
    val partial: Boolean,
)

/** Плашка метрики в сетке вкладки «История» (итог за всё время). */
data class StatHistoryMetric(val key: String, val label: String, val total: String, val unit: String)

data class StatCompareOption(
    val id: String,
    val title: String,
    val note: String,
    val off: Boolean = false,
    val action: Boolean = false, // открывает пикер «выбрать месяц/год», а не выбирает сразу
)

/** Один столбец в помесячном графике детализации метрики. */
data class StatDetailBar(val short: String, val value: Float, val fullLabel: String)

/** Сегмент круговой разбивки (donut): подпись, доля-значение, подстрочник. */
data class StatDonutSeg(val label: String, val value: Float, val sub: String)

/** Экран детализации метрики (тап по плашке): hero + интерактивные столбцы. */
data class StatDetailState(
    val key: String,
    val title: String,          // короткое имя метрики в топбаре
    val caption: String,        // «РАССТОЯНИЕ · МАРТ 2026»
    val heroValue: String,
    val heroUnit: String,
    val heroDelta: StatDelta?,
    val heroPrevCaption: String,// «Февраль 3,0»
    val bars: List<StatDetailBar>,
    val selectedIndex: Int,
    // разбивка по направлениям за выбранный месяц (для метрик на основе поездов)
    val breakdown: List<StatDonutSeg> = emptyList(),
    val breakdownCenter: String = "",
    val breakdownCenterSub: String = "",
)

data class StatisticsUiState(
    val loading: Boolean = true,
    val tab: StatTab = StatTab.MONTH,
    val empty: Boolean = false,
    // период
    val periodLabel: String = "",
    val periodSub: String = "",
    val canGoPrev: Boolean = false,
    val canGoNext: Boolean = false,
    // сравнение
    val compareEnabled: Boolean = true,
    val compareTitle: String = "",
    val compareOptions: List<StatCompareOption> = emptyList(),
    val compareSelectedId: String = "prev",
    // данные для пикеров «выбрать месяц/год» (баз-период сравнения)
    val pickYears: List<Int> = emptyList(),      // годы для сетки месяцев (по убыванию)
    val pickCurYm: Int = 0,                       // selYear*12+selMonth — сам с собой не сравниваем
    val pickYearOptions: List<Int> = emptyList(), // годы для списка (по убыванию)
    val availableMonthsYm: Set<Int> = emptySet(), // месяцы (year*12+month), где есть маршруты — доступны для выбора
    // выбранный для сравнения период есть, но в нём нет маршрутов — сравнивать не с чем
    val compareNoData: Boolean = false,
    // символ валюты по стране из настроек (₽ / ₸ / Br) — для иконки метрики «Заработано»
    val currency: String = "₽",
    // сетка метрик (первая — крупная во вкладке «Месяц»)
    val metrics: List<StatMetric> = emptyList(),
    val wideFirst: Boolean = false,
    // топ направлений
    val topDirections: List<StatTopDirection> = emptyList(),
    // год: hero + помесячные столбцы
    val yearHeroValue: String = "",
    val yearHeroDelta: StatDelta? = null,
    val yearBars: List<StatMonthBar> = emptyList(),
    // история
    val historyMetrics: List<StatHistoryMetric> = emptyList(),
    val historySelected: String = "worked",
    val historyTotal: String = "",
    val historyTotalUnit: String = "",
    val historyTotalCaption: String = "",
    val historyYearsCount: Int = 0,
    val historyRoutesTotal: Int = 0,
    val historyRows: List<StatYearRow> = emptyList(),
    // детализация метрики (overlay поверх вкладки)
    val detail: StatDetailState? = null,
)

// ── Форматтеры (повторяют design/src/stats-data.jsx) ─────────────────
object StatFormat {
    /** Часы:минуты из миллисекунд. «187:20», со знаком «+11:20» / «−0:45». */
    fun hm(ms: Long, signed: Boolean = false): String {
        val neg = ms < 0
        val totalMin = (abs(ms) / 60_000L).toInt()
        val h = totalMin / 60
        val m = totalMin % 60
        val sign = if (neg) "−" else if (signed) "+" else ""
        return "$sign$h:${m.toString().padStart(2, '0')}"
    }

    /** Целое с разделением разрядов неразрывным-обычным пробелом: 2122 → «2 122». */
    fun int(n: Int): String = n.toString().reversed().chunked(3).joinToString(" ").reversed()
        .let { if (n < 0) "−" + it.removePrefix("−") else it }

    /** Русский десятичный формат с заданной точностью: 47.3 → «47,3». */
    fun dec(v: Double, digits: Int = 1): String {
        val rounded = (v * pow10(digits)).roundToInt() / pow10(digits)
        val s = if (digits == 0) rounded.roundToInt().toString()
        else {
            val str = kotlin.math.abs(rounded).toString()
            // гарантируем нужное число знаков после точки
            val parts = str.split(".")
            val whole = parts[0]
            val frac = (parts.getOrElse(1) { "" } + "0".repeat(digits)).take(digits)
            (if (rounded < 0) "-" else "") + "$whole.$frac"
        }
        return s.replace(".", ",").replace("-", "−")
    }

    /**
     * Деньги в компактном виде: 142800 → «142,8 тыс. ₽», 1680000 → «1,68 млн ₽».
     * [currency] — символ валюты по стране из настроек (₽ / ₸ / Br).
     */
    fun money(rub: Double, currency: String = "₽"): Pair<String, String> {
        val a = abs(rub)
        return when {
            a >= 1_000_000 -> dec(rub / 1_000_000, 2) to "млн $currency"
            a >= 1_000 -> dec(rub / 1_000, 1) to "тыс. $currency"
            else -> int(rub.roundToInt()) to currency
        }
    }

    /** Расстояние в км → компактно: 3420 → «3,4» + «тыс. км». */
    fun distance(km: Double): Pair<String, String> =
        if (abs(km) >= 1_000) dec(km / 1_000, 1) to "тыс. км" else int(km.roundToInt()) to "км"

    private fun pow10(d: Int): Double {
        var r = 1.0; repeat(d) { r *= 10 }; return r
    }

    /** Склонение слова «год»: 1 год · 2 года · 5 лет (с учётом 11–14). */
    fun yearsWord(n: Int): String {
        val a = abs(n)
        if (a % 100 in 11..14) return "лет"
        return when (a % 10) {
            1 -> "год"
            2, 3, 4 -> "года"
            else -> "лет"
        }
    }
}
