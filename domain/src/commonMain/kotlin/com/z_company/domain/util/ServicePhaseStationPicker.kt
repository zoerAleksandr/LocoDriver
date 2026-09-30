package com.z_company.domain.util

import com.z_company.domain.entities.setting.ServicePhase
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Координата телефона в момент записи времени / добавления станции. */
data class GeoFix(val latitude: Double, val longitude: Double, val accuracyMeters: Float? = null)

/**
 * Подстановка названия станции из плеча по местоположению (SCREEN_SPECS §6.6, «Станции плеча
 * в форме поезда»). Станции плеча в поезд целиком не копируются — при записи времени и при
 * добавлении станции подставляется ближайшая станция плеча **впереди** по ходу.
 */
object ServicePhaseStationPicker {

    /** Радиус, в котором станция плеча считается «здесь». */
    const val NEAR_RADIUS_M = 2_000.0

    /** Сдвиг меньше этого с прошлого добавления — «стоим на месте» (дописывают задним числом). */
    const val STATIONARY_M = 300.0

    private data class Stop(val name: String, val lat: Double?, val lon: Double?)

    private fun stops(phase: ServicePhase): List<Stop> = buildList {
        add(Stop(phase.departureStation, phase.departurePoint?.latitude, phase.departurePoint?.longitude))
        phase.intermediateStations.forEach { add(Stop(it.name, it.latitude, it.longitude)) }
        add(Stop(phase.arrivalStation, phase.arrivalPoint?.latitude, phase.arrivalPoint?.longitude))
    }

    /** Есть ли у плеча хоть одна станция с координатами (иначе подставлять нечего). */
    fun hasCoordinates(phase: ServicePhase): Boolean = stops(phase).any { it.lat != null && it.lon != null }

    /**
     * Ближайшая к [fix] станция плеча в радиусе [NEAR_RADIUS_M], стоящая по порядку плеча
     * после последней уже записанной в поезде станции ([trainStationNames], по порядку поезда).
     * null — рядом ничего нет.
     */
    fun nearestAhead(phase: ServicePhase, trainStationNames: List<String?>, fix: GeoFix): String? {
        val all = stops(phase)
        val names = all.map { ServicePhaseStationsMerge.normalizeName(it.name) }
        val lastKnown = trainStationNames.asReversed().asSequence()
            .mapNotNull { it?.takeIf(String::isNotBlank) }
            .map { ServicePhaseStationsMerge.normalizeName(it) }
            .map { names.indexOf(it) }
            .firstOrNull { it >= 0 } ?: -1
        return all.drop(lastKnown + 1)
            .filter { it.lat != null && it.lon != null }
            .map { it to distanceM(fix.latitude, fix.longitude, it.lat!!, it.lon!!) }
            .filter { it.second <= NEAR_RADIUS_M }
            .minByOrNull { it.second }
            ?.first?.name
    }

    /** С прошлой фиксации телефон почти не сдвинулся. */
    fun isStationary(previous: GeoFix?, fix: GeoFix): Boolean =
        previous != null && distanceM(previous.latitude, previous.longitude, fix.latitude, fix.longitude) < STATIONARY_M

    /** Ближайшая к [fix] станция плеча в радиусе (любая, без учёта хода). */
    fun nearestInPhase(phase: ServicePhase, fix: GeoFix): String? =
        stops(phase)
            .filter { it.lat != null && it.lon != null }
            .map { it to distanceM(fix.latitude, fix.longitude, it.lat!!, it.lon!!) }
            .filter { it.second <= NEAR_RADIUS_M }
            .minByOrNull { it.second }
            ?.first?.name

    /**
     * Название для новой станции по плечу:
     * - в движении — ближайшая станция **впереди** по ходу ([nearestAhead]);
     * - стоя на месте (сдвиг < [STATIONARY_M] с прошлого добавления) — ближайшая станция плеча
     *   вообще ([nearestInPhase]): машинист дописывает станцию, у которой стоит.
     * Повтор предыдущей станции поезда не подставляется — это была бы та же строка ещё раз.
     */
    fun nameForNewStation(
        phase: ServicePhase,
        trainStationNames: List<String?>,
        fix: GeoFix,
        previousFix: GeoFix?,
    ): String? {
        val name = if (isStationary(previousFix, fix)) nearestInPhase(phase, fix)
        else nearestAhead(phase, trainStationNames, fix)
        return name?.takeUnless { isSameAsPrevious(it, trainStationNames) }
    }

    /** Без плеча: ближайшая станция из известных координат (всех плеч) в радиусе. */
    fun nearestKnown(points: Map<String, Pair<Double, Double>>, names: Map<String, String>, fix: GeoFix): String? =
        points.map { (norm, p) -> norm to distanceM(fix.latitude, fix.longitude, p.first, p.second) }
            .filter { it.second <= NEAR_RADIUS_M }
            .minByOrNull { it.second }
            ?.first?.let { names[it] }

    /** Нормализованное название → как оно записано в плече (для [nearestKnown]). */
    fun knownNames(phases: List<ServicePhase>): Map<String, String> {
        val out = mutableMapOf<String, String>()
        phases.forEach { phase -> stops(phase).forEach { out.getOrPut(ServicePhaseStationsMerge.normalizeName(it.name)) { it.name } } }
        return out
    }

    fun isSameAsPrevious(name: String, trainStationNames: List<String?>): Boolean {
        val prev = trainStationNames.lastOrNull { !it.isNullOrBlank() } ?: return false
        return ServicePhaseStationsMerge.normalizeName(prev) == ServicePhaseStationsMerge.normalizeName(name)
    }

    /** Координаты станций по названию из всех плеч (для сортировки подсказок по близости). */
    fun knownPoints(phases: List<ServicePhase>): Map<String, Pair<Double, Double>> {
        val out = mutableMapOf<String, Pair<Double, Double>>()
        phases.forEach { phase ->
            stops(phase).forEach { st ->
                if (st.lat != null && st.lon != null) out.getOrPut(ServicePhaseStationsMerge.normalizeName(st.name)) { st.lat to st.lon }
            }
        }
        return out
    }

    fun distanceM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val p1 = lat1.toRadians()
        val p2 = lat2.toRadians()
        val dp = p2 - p1
        val dl = (lon2 - lon1).toRadians()
        val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * r * asin(sqrt(min(1.0, a)))
    }

    private fun Double.toRadians() = this * kotlin.math.PI / 180.0
}

/**
 * Порядок подсказок станций (форма поезда, шторка «Локомотив»): сначала ближайшая по
 * местоположению (из известных координат плеч, в радиусе [ServicePhaseStationPicker.NEAR_RADIUS_M]),
 * затем по частоте использования в рейсах, затем остальные в прежнем порядке.
 */
object StationSuggestionOrder {
    fun sort(
        names: List<String>,
        usage: Map<String, Int>,
        points: Map<String, Pair<Double, Double>> = emptyMap(),
        fix: GeoFix? = null,
    ): List<String> {
        val nearest = fix?.let { f ->
            names.mapNotNull { n ->
                points[ServicePhaseStationsMerge.normalizeName(n)]?.let { (lat, lon) ->
                    n to ServicePhaseStationPicker.distanceM(f.latitude, f.longitude, lat, lon)
                }
            }.filter { it.second <= ServicePhaseStationPicker.NEAR_RADIUS_M }.minByOrNull { it.second }?.first
        }
        val count = { n: String -> usage[ServicePhaseStationsMerge.normalizeName(n)] ?: 0 }
        val rest = names.filter { it != nearest }
            .withIndex()
            .sortedWith(compareByDescending<IndexedValue<String>> { count(it.value) }.thenBy { it.index })
            .map { it.value }
        return listOfNotNull(nearest) + rest
    }

    /** Частота станций по рейсам: нормализованное название → сколько раз встречается. */
    fun usage(stationNames: Sequence<String?>): Map<String, Int> =
        stationNames.mapNotNull { it?.takeIf(String::isNotBlank) }
            .groupingBy { ServicePhaseStationsMerge.normalizeName(it) }
            .eachCount()
}

/**
 * Порядок плеч в шторке выбора: последнее использованное с текущей станции, затем другие плечи
 * с этой станции, затем по частоте использования, затем остальные.
 */
object ServicePhaseOrder {
    /**
     * @param currentStation станция, где сейчас машинист (ближайшая по GPS или последняя в поезде)
     * @param usage id плеча → сколько раз выбирали в рейсах
     * @param lastUsedAt id плеча → время последнего рейса с ним (мс)
     */
    fun sort(
        phases: List<ServicePhase>,
        currentStation: String?,
        usage: Map<String, Int>,
        lastUsedAt: Map<String, Long>,
    ): List<ServicePhase> {
        val here = currentStation?.let { ServicePhaseStationsMerge.normalizeName(it) }
        val fromHere = phases.filter { here != null && ServicePhaseStationsMerge.normalizeName(it.departureStation) == here }
        val lastFromHere = fromHere.filter { lastUsedAt.containsKey(it.id) }.maxByOrNull { lastUsedAt.getValue(it.id) }
        val byUsage = compareByDescending<IndexedValue<ServicePhase>> { usage[it.value.id] ?: 0 }.thenBy { it.index }
        val otherFromHere = fromHere.filter { it != lastFromHere }.withIndex().sortedWith(byUsage).map { it.value }
        val rest = phases.filter { it !in fromHere }.withIndex().sortedWith(byUsage).map { it.value }
        return listOfNotNull(lastFromHere) + otherFromHere + rest
    }
}
