package com.z_company.domain.track

/*
 * Плечо без сети: расстояние, координаты крайних станций и промежуточные станции берутся из участков советника, уже
 * скачанных на телефон (экран «Скачанные участки»). Чистая логика KMP. Работает, когда обе станции плеча лежат на оси
 * ОДНОГО участка, в любом направлении; плечо через несколько участков здесь не собирается.
 */

/** Станция плеча, найденная по оси участка. [kmFromStart] - расстояние от станции отправления плеча, км. */
data class OfflineLegStation(val name: String, val lat: Double?, val lon: Double?, val kmFromStart: Double)

/** Плечо, определённое по скачанному участку. [stations] - только между крайними, по порядку от отправления к прибытию. */
data class OfflineLegResult(
    val sectionId: String,
    val sectionName: String,
    val lengthKm: Double,
    val departure: OfflineLegStation,
    val arrival: OfflineLegStation,
    val stations: List<OfflineLegStation>,
    /** Геометрия оси между крайними станциями по порядку плеча. */
    val line: List<Pair<Double, Double>>,
)

object OfflineLeg {
    /** Минимальное расстояние между станциями плеча, км: меньше - это одна и та же станция/дубль названия на оси. */
    private const val MIN_LENGTH_KM = 0.05

    /**
     * Плечо [from] -> [to] по скачанным [sections]. Среди участков, где есть обе станции, берётся с самым коротким расстоянием
     * (на пересечениях участков одна пара станций может быть на двух осях). null - ни на одном участке обеих станций нет.
     */
    fun determine(sections: Collection<SectionProfile>, from: String, to: String): OfflineLegResult? {
        if (from.isBlank() || to.isBlank() || SectionPicker.sameStation(from, to)) return null
        return sections.mapNotNull { build(it, from, to) }.minByOrNull { it.lengthKm }
    }

    private fun build(section: SectionProfile, from: String, to: String): OfflineLegResult? {
        val a = section.stations.firstOrNull { SectionPicker.sameStation(it.name, from) } ?: return null
        val b = section.stations.firstOrNull { SectionPicker.sameStation(it.name, to) } ?: return null
        val length = kotlin.math.abs(b.km - a.km)
        if (length < MIN_LENGTH_KM) return null
        val forward = b.km > a.km
        fun station(s: SectionStation): OfflineLegStation {
            val point = if (s.lat != null && s.lon != null) s.lat to s.lon else SectionAxisPoint.at(section, s.km)
            return OfflineLegStation(s.name, point?.first, point?.second, kotlin.math.abs(s.km - a.km))
        }
        val lo = minOf(a.km, b.km)
        val hi = maxOf(a.km, b.km)
        val between = section.stations
            .filter { it !== a && it !== b && it.km > lo + 1e-9 && it.km < hi - 1e-9 }
            .sortedBy { if (forward) it.km else -it.km }
            .filter { !SectionPicker.sameStation(it.name, a.name) && !SectionPicker.sameStation(it.name, b.name) }
            // На оси бывают платформы/пути с одним названием подряд - в плече одна станция.
            .fold(emptyList<SectionStation>()) { acc, st -> if (acc.lastOrNull()?.let { SectionPicker.sameStation(it.name, st.name) } == true) acc else acc + st }
            .map(::station)
        val line = AxisSlice.smooth(section.axis.points, a.km, b.km)
            .let { if (forward) it else it.asReversed() }
        return OfflineLegResult(section.id, section.name, length, station(a), station(b), between, line)
    }
}

/** Координаты точки оси участка по километру (когда у станции в данных участка нет своих координат). */
internal object SectionAxisPoint {
    fun at(section: SectionProfile, km: Double): Pair<Double, Double>? = AxisSlice.pointAt(section.axis.points, km)
}
