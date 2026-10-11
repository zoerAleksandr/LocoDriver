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

    private data class Stop(val name: String, val lat: Double?, val lon: Double?)

    private fun stops(phase: ServicePhase): List<Stop> = buildList {
        add(Stop(phase.departureStation, phase.departurePoint?.latitude, phase.departurePoint?.longitude))
        phase.intermediateStations.forEach { add(Stop(it.name, it.latitude, it.longitude)) }
        add(Stop(phase.arrivalStation, phase.arrivalPoint?.latitude, phase.arrivalPoint?.longitude))
    }

    /** Есть ли у плеча хоть одна станция с координатами (иначе подставлять нечего). */
    fun hasCoordinates(phase: ServicePhase): Boolean = stops(phase).any { it.lat != null && it.lon != null }

    /** Боковое отклонение станции справочника от рельсовой линии плеча, при котором она «на пути плеча», м. */
    const val LINE_TOLERANCE_M = 100.0

    /** То же при запасном варианте (нет линии - прямые между станциями плеча), м. */
    const val STRAIGHT_TOLERANCE_M = 150.0

    private class Cand(
        val name: String,
        val lat: Double?,
        val lon: Double?,
        val along: Double,
        /** Индекс станции плеча; у станции справочника - Int.MAX_VALUE. */
        val rank: Int,
    )

    /**
     * ЕДИНЫЙ выбор названия станции остановки (АВТО, карта, просмотр трека; SCREEN_SPECS §6.9, §6.2 п.6а), радиус [radiusM]
     * (по умолчанию 1 км, [com.z_company.domain.autorecord.StopDetectionConfig.STATION_RADIUS_M]):
     * - плечо выбрано: кандидаты = станции плеча (с промежуточными) + станции справочника ([points]/[names]), лежащие НА ПУТИ
     *   плеча (отклонение от рельсовой линии [railLine] <= [LINE_TOLERANCE_M]; нет линии - от прямых между станциями плеча,
     *   <= [STRAIGHT_TOLERANCE_M]). Все кандидаты упорядочены по положению вдоль линии; берутся не ранее последней записанной
     *   станции поезда (повтор допускается); из подходящих в радиусе - ближайшая по пути к точке остановки (при равенстве -
     *   впереди по порядку); если у поезда ничего не записано - ограничения «не ранее» нет;
     * - иначе (плеча нет или подходящих кандидатов нет) - ближайшая станция справочника в радиусе;
     * - иначе null (название остаётся пустым).
     */
    fun pickForStop(
        phase: ServicePhase?,
        trainStationNames: List<String?>,
        latitude: Double,
        longitude: Double,
        points: Map<String, Pair<Double, Double>>,
        names: Map<String, String>,
        radiusM: Double = com.z_company.domain.autorecord.StopDetectionConfig.STATION_RADIUS_M,
        railLine: List<Pair<Double, Double>>? = null,
    ): String? {
        if (phase != null) {
            pickOnPhase(phase, trainStationNames, latitude, longitude, points, names, radiusM, railLine)?.let { return it }
        }
        return points.map { (key, p) -> key to distanceM(latitude, longitude, p.first, p.second) }
            .filter { it.second <= radiusM }
            .minByOrNull { it.second }
            ?.let { names[it.first] }
    }

    private fun pickOnPhase(
        phase: ServicePhase,
        trainStationNames: List<String?>,
        latitude: Double,
        longitude: Double,
        points: Map<String, Pair<Double, Double>>,
        names: Map<String, String>,
        radiusM: Double,
        railLine: List<Pair<Double, Double>>?,
    ): String? {
        val all = stops(phase)
        val withCoords = all.withIndex().filter { it.value.lat != null && it.value.lon != null }
        val realLine = railLine?.takeIf { it.size >= 2 }
        val line = realLine ?: withCoords.map { it.value.lat!! to it.value.lon!! }.takeIf { it.size >= 2 }
        val tolerance = if (realLine != null) LINE_TOLERANCE_M else STRAIGHT_TOLERANCE_M
        val cum = line?.let(TrainMapRoute::cumulativeM)

        // Положение станций плеча вдоль линии (не убывает по порядку плеча); без координат - по предыдущей.
        val cands = ArrayList<Cand>()
        var seg = 0
        var prevAlong = 0.0
        all.forEachIndexed { i, st ->
            var along = prevAlong
            if (line != null && cum != null && st.lat != null && st.lon != null) {
                TrainMapRoute.project(line, cum, st.lat, st.lon, seg)?.let { pr ->
                    seg = pr.segment
                    along = maxOf(pr.alongM, prevAlong)
                }
            }
            prevAlong = along
            cands.add(Cand(st.name, st.lat, st.lon, along, i))
        }
        // Станции справочника на пути плеча.
        if (line != null && cum != null) {
            val phaseNames = all.map { ServicePhaseStationsMerge.normalizeName(it.name) }.toSet()
            for ((key, p) in points) {
                val name = names[key]?.takeIf { it.isNotBlank() } ?: continue
                if (key in phaseNames || ServicePhaseStationsMerge.normalizeName(name) in phaseNames) continue
                val pr = TrainMapRoute.project(line, cum, p.first, p.second) ?: continue
                if (pr.offsetM <= tolerance) cands.add(Cand(name, p.first, p.second, pr.alongM, Int.MAX_VALUE))
            }
        }
        val ordered = cands.sortedWith(compareBy<Cand> { it.along }.thenBy { it.rank })
        val norm = ordered.map { ServicePhaseStationsMerge.normalizeName(it.name) }
        val lastKnown = trainStationNames.asReversed().asSequence()
            .mapNotNull { it?.takeIf(String::isNotBlank) }
            .map { ServicePhaseStationsMerge.normalizeName(it) }
            .map { norm.indexOf(it) }
            .firstOrNull { it >= 0 } ?: -1

        val stop = if (line != null && cum != null) TrainMapRoute.project(line, cum, latitude, longitude) else null
        val stopOnLine = stop != null && stop.offsetM <= tolerance
        val best = ordered.withIndex()
            .filter { (i, c) -> i >= maxOf(lastKnown, 0) && c.lat != null && c.lon != null }
            .mapNotNull { (i, c) ->
                val straight = distanceM(latitude, longitude, c.lat!!, c.lon!!)
                if (straight > radiusM) return@mapNotNull null
                val d = if (stopOnLine) kotlin.math.abs(c.along - stop!!.alongM) else straight
                Triple(i, c, d)
            }
            // Ближайшая по пути; при равенстве - впереди по порядку (меньший индекс).
            .minWithOrNull(compareBy<Triple<Int, Cand, Double>> { it.third }.thenBy { it.first })
        return best?.second?.name
    }

    /** Нормализованное название → как оно записано в плече. */
    fun knownNames(phases: List<ServicePhase>): Map<String, String> {
        val out = mutableMapOf<String, String>()
        phases.forEach { phase -> stops(phase).forEach { out.getOrPut(ServicePhaseStationsMerge.normalizeName(it.name)) { it.name } } }
        return out
    }

    /**
     * Названия станций поезда для [pickForStop]: при выбранном плече последняя станция формы - прибытие плеча,
     * её не считаем записанной (как в «GO», SCREEN_SPECS §6.2 п.6), иначе все станции плеча оказались бы «позади».
     */
    fun writtenNames(trainStationNames: List<String?>, hasPhase: Boolean): List<String?> =
        if (hasPhase && trainStationNames.size >= 2) trainStationNames.dropLast(1) else trainStationNames

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
     * Текущая станция для порядка плеч: первая станция поезда, если от неё есть плечи; иначе станция отправления плеча
     * (с координатами), ближайшая к [fix] в пределах [ServicePhaseStationPicker.NEAR_RADIUS_M]; иначе первая станция
     * поезда как есть (или null). Учитываются только отправления плеч - ближайшая «просто известная» станция
     * (прибытие, промежуточная) не должна затмевать станцию, от которой реально есть плечи.
     */
    fun currentStation(phases: List<ServicePhase>, firstStationName: String?, fix: GeoFix?): String? {
        val typed = firstStationName?.takeIf { it.isNotBlank() }
        if (typed != null) {
            val key = ServicePhaseStationsMerge.normalizeName(typed)
            if (phases.any { ServicePhaseStationsMerge.normalizeName(it.departureStation) == key }) return typed
        }
        if (fix != null) {
            phases.mapNotNull { ph ->
                val p = ph.departurePoint ?: return@mapNotNull null
                ph.departureStation to ServicePhaseStationPicker.distanceM(fix.latitude, fix.longitude, p.latitude, p.longitude)
            }
                .filter { it.second <= ServicePhaseStationPicker.NEAR_RADIUS_M }
                .minByOrNull { it.second }
                ?.let { return it.first }
        }
        return typed
    }

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
