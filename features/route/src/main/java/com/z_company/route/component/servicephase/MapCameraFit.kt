package com.z_company.route.component.servicephase

import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap

/** Что делать камере с набором точек (чистая часть, без MapLibre). */
sealed interface CameraFitPlan {
    /** Нет точек — камеру не трогаем. */
    data object None : CameraFitPlan

    /** Одна точка (или все совпадают / почти совпадают) — центрируем на ней, зум ≥ [MapCameraFit.POINT_ZOOM]. */
    data class Point(val lat: Double, val lon: Double) : CameraFitPlan

    /** Две и более различающиеся точки — охват границами. */
    data class Bounds(val south: Double, val west: Double, val north: Double, val east: Double) : CameraFitPlan
}

object MapCameraFit {
    const val POINT_ZOOM = 14.0

    /** Порог «одной точки»: охват меньше ~11 м по обеим осям. */
    private const val MIN_SPAN_DEG = 1e-4

    fun plan(points: List<Pair<Double, Double>>): CameraFitPlan {
        if (points.isEmpty()) return CameraFitPlan.None
        val south = points.minOf { it.first }
        val north = points.maxOf { it.first }
        val west = points.minOf { it.second }
        val east = points.maxOf { it.second }
        return if (north - south < MIN_SPAN_DEG && east - west < MIN_SPAN_DEG) {
            CameraFitPlan.Point((south + north) / 2, (west + east) / 2)
        } else {
            CameraFitPlan.Bounds(south, west, north, east)
        }
    }
}

/**
 * Подгоняет камеру под точки (lat to lon). 0 точек — ничего; одна/совпадающие — центр на точке с зумом
 * [userZoom] (если пользователь уже менял) или [MapCameraFit.POINT_ZOOM]; иначе — охват с отступами.
 * `LatLngBounds.Builder.build()` на одной точке бросает InvalidLatLngBoundsException, поэтому границы строим только из 2+.
 */
fun fitCameraTo(
    map: MapLibreMap,
    points: List<Pair<Double, Double>>,
    padLeft: Int,
    padTop: Int,
    padRight: Int,
    padBottom: Int,
    animate: Boolean,
    userZoom: Double? = null,
) {
    val update = when (val p = MapCameraFit.plan(points)) {
        CameraFitPlan.None -> return
        is CameraFitPlan.Point -> CameraUpdateFactory.newLatLngZoom(
            LatLng(p.lat, p.lon),
            userZoom ?: MapCameraFit.POINT_ZOOM,
        )
        is CameraFitPlan.Bounds -> CameraUpdateFactory.newLatLngBounds(
            LatLngBounds.from(p.north, p.east, p.south, p.west),
            padLeft, padTop, padRight, padBottom,
        )
    }
    if (animate) map.animateCamera(update) else map.moveCamera(update)
}
