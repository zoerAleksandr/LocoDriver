package com.z_company.route.component.servicephase

import android.content.Context
import com.z_company.domain.gnss.GnssDeadZoneDetector
import com.z_company.domain.gnss.GnssBadSpan
import com.z_company.domain.gnss.GnssDeadZones
import com.z_company.domain.gnss.GnssTrackSpans
import com.z_company.domain.gnss.GnssMapZone
import com.z_company.domain.gnss.GnssZone
import com.z_company.domain.track.TrackAnalysis
import com.z_company.domain.track.TrackRepository
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

/** Слой «Зоны без GPS» на карте поезда (SCREEN_SPECS §6.9): строгие красноватые шестиугольники по трекам. */

internal const val GNSS_ZONES_SOURCE = "train-map-gnss"
internal const val GNSS_ZONES_LAYER = "train-map-gnss-layer"
internal const val GNSS_ZONES_OUTLINE_LAYER = "train-map-gnss-outline"

/** Сторона шестиугольника, м (площадь ≈ как у ячейки 250 м). */
private const val GNSS_HEX_SIDE_M = 150.0

private const val M_PER_DEG_LAT = 111_320.0

/** Тап ближе этого к центру ячейки открывает карточку, м. */
private const val GNSS_HIT_RADIUS_M = 200.0

/** Включённость слоя - личная настройка просмотра на этом устройстве. */
internal object GnssDeadZonePrefs {
    private const val FILE = "gnss_dead_zones"
    private const val KEY = "enabled"

    fun enabled(context: Context): Boolean =
        runCatching { context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY, false) }.getOrDefault(false)

    fun set(context: Context, value: Boolean) {
        runCatching { context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY, value).apply() }
    }
}

/**
 * Локальные зоны из собственных треков (Track.db). Плохие участки трека считаются один раз на (трек, число точек) и
 * держатся в памяти процесса; на диск ничего не пишется. Работает офлайн, без согласий и подписки (треки уже на устройстве).
 */
internal object GnssDeadZoneLocal {
    private val cache = ConcurrentHashMap<String, Pair<Long, List<GnssBadSpan>>>()

    suspend fun load(repository: TrackRepository): List<GnssZone> = withContext(Dispatchers.Default) {
        val infos = repository.tracks()
        cache.keys.retainAll(infos.map { it.trackId }.toSet())
        val perTrack = infos.map { info ->
            val count = repository.pointCount(info.trackId)
            val cached = cache[info.trackId]
            val spans = if (cached != null && cached.first == count) {
                cached.second
            } else {
                GnssDeadZoneDetector.detect(repository.points(info.trackId)).also { cache[info.trackId] = count to it }
            }
            GnssTrackSpans(info.trackId, spans)
        }
        GnssDeadZones.aggregate(perTrack)
    }
}

/**
 * Зоны как строгая сетка шестиугольников (плоская вершина сверху, как «зоны повышенного спроса»): ячейки ≈250 м
 * пересчитываются в шестиугольники со стороной [GNSS_HEX_SIDE_M]; вес шестиугольника - наибольший из попавших в него ячеек.
 * Сетка фиксирована (не зависит от набора зон и масштаба), поэтому при панорамировании шестиугольники не «ползут».
 */
internal fun gnssZoneFeatures(zones: List<GnssMapZone>): FeatureCollection {
    if (zones.isEmpty()) return FeatureCollection.fromFeatures(emptyList())
    // Равнопромежуточная проекция с широтой, округлённой до градуса: стабильна между перерисовками.
    val lat0 = Math.round(zones.map { it.latitude }.average()).toDouble()
    val mx = M_PER_DEG_LAT * cos(lat0 * PI / 180.0)
    val r = GNSS_HEX_SIDE_M
    val weights = HashMap<Pair<Int, Int>, Double>()
    for (z in zones) {
        val x = z.longitude * mx
        val y = z.latitude * M_PER_DEG_LAT
        // Плоская ориентация, осевые координаты (q, r) -> кубическое округление.
        val qf = (2.0 / 3.0 * x) / r
        val rf = (-1.0 / 3.0 * x + sqrt(3.0) / 3.0 * y) / r
        val key = hexRound(qf, rf)
        weights.merge(key, z.weight) { a, b -> maxOf(a, b) }
    }
    return FeatureCollection.fromFeatures(
        weights.map { (key, w) ->
            val cx = r * 1.5 * key.first
            val cy = r * sqrt(3.0) * (key.second + key.first / 2.0)
            val ring = (0..6).map { i ->
                val ang = PI / 3.0 * (i % 6)
                Point.fromLngLat((cx + r * cos(ang)) / mx, (cy + r * sin(ang)) / M_PER_DEG_LAT)
            }
            Feature.fromGeometry(Polygon.fromLngLats(listOf(ring))).apply {
                // Вес: 1 поездка - бледно, 4 и больше - насыщенно; общий уровень 1/2/3 - 0,5/0,75/1 (берётся больший).
                addNumberProperty("w", w)
            }
        },
    )
}

private fun hexRound(q: Double, r: Double): Pair<Int, Int> {
    val x = q
    val z = r
    val y = -x - z
    var rx = Math.round(x).toDouble()
    var ry = Math.round(y).toDouble()
    var rz = Math.round(z).toDouble()
    val dx = abs(rx - x)
    val dy = abs(ry - y)
    val dz = abs(rz - z)
    if (dx > dy && dx > dz) rx = -ry - rz else if (dy > dz) ry = -rx - rz else rz = -rx - ry
    return rx.toInt() to rz.toInt()
}

/** Зона под тапом (ближайший центр ячейки не дальше [GNSS_HIT_RADIUS_M] и не дальше [minRadiusM]); null - нет. */
internal fun gnssZoneAt(zones: List<GnssMapZone>, latitude: Double, longitude: Double, minRadiusM: Double): GnssMapZone? {
    val limit = maxOf(GNSS_HIT_RADIUS_M, minRadiusM)
    return zones
        .map { it to TrackAnalysis.distanceM(latitude, longitude, it.latitude, it.longitude) }
        .filter { it.second <= limit }
        .minByOrNull { it.second }?.first
}
