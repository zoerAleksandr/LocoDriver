package com.z_company.domain.track

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/*
 * «Виртуальный участок»: обычный SectionProfile, собранный из цепочки рёбер графа
 * (EdgeTraversal) и реальных участков-владельцев. Работает со всеми экранами профиля
 * без переделок (LiveProfile, ProfileModel, ProfileUi). Км накапливаются от начала
 * пути; обратный проход ребра разворачивает профиль (уклон меняет знак, светофоры — facing).
 * Чистая логика KMP.
 */

object VirtualSection {
    /** Допуск на границу диапазона ребра (км): станция/светофор ровно на стыке принадлежит обоим рёбрам. */
    private const val EDGE_EPS_KM = 0.05

    /** Собирает виртуальный участок из проезда рёбер [traversals] по ходу поезда. null — нет данных владельца. */
    fun compose(
        traversals: List<EdgeTraversal>,
        sectionsById: Map<String, SectionProfile>,
        name: String = "Маршрут по факту",
    ): SectionProfile? {
        if (traversals.isEmpty()) return null
        val axisPoints = ArrayList<AxisPoint>()
        val elements = ArrayList<ProfileElement>()
        val kmMarks = ArrayList<KmMark>()
        val stations = ArrayList<SectionStation>()
        val signals = ArrayList<TrackSignal>()
        val crossings = ArrayList<SectionCrossing>()
        val brakeTests = ArrayList<SectionBrakeTest>()
        var offset = 0.0
        for (t in traversals) {
            val owner = sectionsById[t.edge.sectionId] ?: return null
            val lo = min(t.startKm, t.endKm)
            val hi = max(t.startKm, t.endKm)
            fun mapKm(km: Double): Double = offset + if (t.reverse) (t.startKm - km) else (km - t.startKm)
            fun inside(km: Double): Boolean = km >= lo - EDGE_EPS_KM && km <= hi + EDGE_EPS_KM

            // Ось: границы с интерполяцией + внутренние точки; в обратную сторону порядок разворачивается.
            val slice = ArrayList<Pair<Double, Pair<Double, Double>>>()
            slice += lo to (owner.axis.latLonAt(lo) ?: return null)
            for (p in owner.axis.points) if (p.km > lo + 1e-6 && p.km < hi - 1e-6) slice += p.km to (p.lat to p.lon)
            slice += hi to (owner.axis.latLonAt(hi) ?: return null)
            for ((km, ll) in slice) {
                val v = mapKm(km)
                if (axisPoints.isNotEmpty() && abs(axisPoints.last().km - v) < 1e-6) continue
                axisPoints += AxisPoint(v, ll.first, ll.second)
            }

            for (e in owner.elements) {
                if (e.endKm <= lo + 1e-9 || e.startKm >= hi - 1e-9) continue
                val s = max(e.startKm, lo)
                val x = min(e.endKm, hi)
                val vs = mapKm(s)
                val vx = mapKm(x)
                elements += e.copy(
                    startKm = min(vs, vx),
                    endKm = max(vs, vx),
                    gradePermille = if (t.reverse) -e.gradePermille else e.gradePermille,
                )
            }

            for (m in owner.kmMarks) {
                if (!inside(m.km)) continue
                kmMarks += m.copy(km = mapKm(m.km), dir = if (t.reverse) -m.dir else m.dir)
            }
            for (st in owner.stations) {
                if (!inside(st.km)) continue
                stations += st.copy(km = mapKm(st.km))
            }
            for (sg in owner.signals) {
                if (!inside(sg.km)) continue
                signals += sg.copy(km = mapKm(sg.km), facing = if (t.reverse) -sg.facing else sg.facing)
            }
            for (cr in owner.crossings) {
                if (!inside(cr.km)) continue
                crossings += cr.copy(km = mapKm(cr.km))
            }
            for (bt in owner.brakeTests) {
                if (!inside(bt.km)) continue
                brakeTests += bt.copy(km = mapKm(bt.km))
            }
            offset += t.lengthKm
        }

        // Стыки: одна и та же станция/светофор с обеих сторон границы схлопывается по (км, имя).
        fun <T> dedupeBy(items: List<T>, km: (T) -> Double, key: (T) -> String): List<T> {
            val seen = HashSet<String>()
            return items.sortedBy(km).filter { seen.add("${km(it).round3()}|${key(it)}") }
        }
        val axisOut = axisPoints.sortedBy { it.km }
        val lengthKm = axisOut.lastOrNull()?.km ?: 0.0
        val virtualId = "virtual:" + traversals.joinToString("+") { it.edge.id }
        val stationsOut = dedupeBy(stations, { it.km }, { it.name })
        return SectionProfile(
            id = virtualId,
            name = name,
            fromStation = stationsOut.firstOrNull()?.name ?: "",
            toStation = stationsOut.lastOrNull()?.name ?: "",
            lengthKm = lengthKm,
            axisHash = "virt-" + virtualId.hashCode().toUInt().toString(16),
            axis = SectionAxis(virtualId, "virt-" + virtualId.hashCode().toUInt().toString(16), axisOut),
            elements = elements.sortedBy { it.startKm },
            kmMarks = dedupeBy(kmMarks, { it.km }, { it.kmAbs.toString() }),
            stations = stationsOut,
            signals = dedupeBy(signals, { it.km }, { it.label }),
            crossings = dedupeBy(crossings, { it.km }, { it.label }),
            brakeTests = dedupeBy(brakeTests, { it.km }, { it.label }),
        )
    }

    private fun Double.round3(): Double = (this * 1000.0).toLong() / 1000.0
}
