package com.z_company.route.viewmodel

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private fun RouteVia.key(): String =
    name.trim().lowercase().replace(Regex("\\s+"), " ").ifBlank { osmId?.toString().orEmpty() }

/**
 * Подпись «через …» для каждого варианта: первая станция на его отличающемся участке.
 * Станцию привязываем к ближайшей точке своей полной геометрии и считаем общей, пока эта точка
 * находится в коридоре хотя бы одного конкурирующего маршрута. Так общая станция не становится
 * ложным различителем только из-за отличающегося osmId или состава viaStations.
 *
 * При отсутствии координат используется упорядоченный fallback: общий префикс и встречающиеся
 * у конкурентов имена пропускаются. Если надёжного различителя нет, подписи «через» нет.
 */
fun variantViaLabels(variants: List<RouteVariant>): List<String?> = variants.mapIndexed { index, variant ->
    val others = variants.filterIndexed { otherIndex, _ -> otherIndex != index }
    val geometric = variant.via.sortedBy { it.km }.firstOrNull { station ->
        val latitude = station.latitude ?: return@firstOrNull false
        val longitude = station.longitude ?: return@firstOrNull false
        val ownPoint = variant.line.minByOrNull { point -> distanceMeters(latitude, longitude, point.first, point.second) }
            ?: return@firstOrNull false
        distanceMeters(latitude, longitude, ownPoint.first, ownPoint.second) <= STATION_TO_LINE_TOLERANCE_METERS &&
            others.all { other ->
                other.line.minOfOrNull { point -> distanceMeters(ownPoint.first, ownPoint.second, point.first, point.second) }
                    ?.let { it > SHARED_CORRIDOR_METERS } == true
            }
    }
    geometric?.name ?: if (variant.via.none { it.latitude != null && it.longitude != null }) {
        fallbackViaLabel(variant, others)
    } else {
        null
    }
}

private fun fallbackViaLabel(variant: RouteVariant, others: List<RouteVariant>): String? {
    val stations = variant.via.sortedBy { it.km }
    val keys = stations.map(RouteVia::key)
    val prefixToSkip = others.maxOfOrNull { other ->
        val otherKeys = other.via.sortedBy { it.km }.map(RouteVia::key)
        keys.zip(otherKeys).takeWhile { (left, right) -> left == right }.size
    } ?: 0
    val namesInOtherRoutes = others.flatMap { it.via }.map(RouteVia::key).toSet()
    return stations.drop(prefixToSkip).firstOrNull { it.key() !in namesInOtherRoutes }?.name
}

private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    return EARTH_RADIUS_METERS * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
}

private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val SHARED_CORRIDOR_METERS = 750.0
private const val STATION_TO_LINE_TOLERANCE_METERS = 2_000.0

/** Станции варианта, которые можно добавить в плечо (есть координаты), в порядке по пути. */
fun RouteVariant.addableStations(): List<RouteVia> =
    via.filter { it.latitude != null && it.longitude != null }.sortedBy { it.km }
