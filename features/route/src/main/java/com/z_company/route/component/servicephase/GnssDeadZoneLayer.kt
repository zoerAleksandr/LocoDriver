package com.z_company.route.component.servicephase

import android.content.Context
import com.z_company.domain.gnss.GnssDeadZoneDetector
import com.z_company.domain.gnss.GnssBadSpan
import com.z_company.domain.gnss.GnssDeadZones
import com.z_company.domain.gnss.GnssTrackSpans
import com.z_company.domain.gnss.GnssZone
import com.z_company.domain.track.TrackAnalysis
import com.z_company.domain.track.TrackRepository
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.style.expressions.Expression
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/** Слой «Зоны без GPS» на карте поезда (SCREEN_SPECS §6.9): мягкие красноватые пятна по локальным трекам. */

internal const val GNSS_ZONES_SOURCE = "train-map-gnss"
internal const val GNSS_ZONES_LAYER = "train-map-gnss-layer"

/** Радиус пятна одной ячейки, м (ячейка ≈250 м: соседние пятна сливаются). */
private const val GNSS_BLOB_RADIUS_M = 240.0

/** Тап ближе этого к центру ячейки открывает карточку, м. */
private const val GNSS_HIT_RADIUS_M = 200.0

/** Метров на пиксель стиля на нулевом масштабе экватора (плитки MapLibre 512 px). */
private const val MPP_Z0 = 78271.517

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

internal fun gnssZoneFeatures(zones: List<GnssZone>): FeatureCollection =
    FeatureCollection.fromFeatures(
        zones.map { z ->
            Feature.fromGeometry(Point.fromLngLat(z.longitude, z.latitude)).apply {
                // Вес: 1 поездка - бледно, 4 и больше - насыщенно.
                addNumberProperty("w", (z.tracks / 4.0).coerceIn(0.0, 1.0))
            }
        },
    )

/** Радиус пятна в пикселях стиля растёт вдвое с каждым масштабом - пятно держит ≈240 м на земле. */
internal fun gnssRadiusExpression(latitude: Double): Expression {
    val k = GNSS_BLOB_RADIUS_M / (MPP_Z0 * cos(latitude * PI / 180.0).coerceAtLeast(0.2))
    return Expression.interpolate(
        Expression.exponential(2), Expression.zoom(),
        Expression.stop(4, (k * 16.0).toFloat()),
        Expression.stop(18, (k * 262_144.0).toFloat()),
    )
}

/** Зона под тапом (ближайший центр ячейки не дальше [GNSS_HIT_RADIUS_M] и не дальше [minRadiusM]); null - нет. */
internal fun gnssZoneAt(zones: List<GnssZone>, latitude: Double, longitude: Double, minRadiusM: Double): GnssZone? {
    val limit = maxOf(GNSS_HIT_RADIUS_M, minRadiusM)
    return zones
        .map { it to TrackAnalysis.distanceM(latitude, longitude, it.latitude, it.longitude) }
        .filter { it.second <= limit }
        .minByOrNull { it.second }?.first
}
