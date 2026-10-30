package com.z_company.route.component.servicephase

import android.content.Context
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/** Запасная векторная подложка (в России без VPN бывает недоступна — тогда работает только свой прокси). */
internal const val STYLE_URL_FALLBACK = "https://tiles.openfreemap.org/styles/liberty"

/** Последний запас — растровая подложка OSM (с атрибуцией); линия плеча и точки рисуются поверх. */
private const val RASTER_STYLE_JSON = """{"version":8,"sources":{"osm":{"type":"raster","tiles":["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],"tileSize":256,"maxzoom":19,"attribution":"© OpenStreetMap contributors"}},"layers":[{"id":"osm","type":"raster","source":"osm"}]}"""

/** Сколько ждём загрузки стиля, прежде чем перейти к следующему варианту (сеть «висит», ошибки нет). */
private const val STYLE_TIMEOUT_MS = 10_000L

/**
 * Загружает подложку цепочкой: 1) свой прокси [STYLE_URL]; 2) OpenFreeMap [STYLE_URL_FALLBACK];
 * 3) растр OSM. К следующему варианту переходим по ошибке загрузки карты или по таймауту.
 * [onLoaded] вызывается один раз — для первого загрузившегося стиля (слои добавляются в него).
 */
internal fun loadMapStyle(mapView: MapView, map: MapLibreMap, onLoaded: (Style) -> Unit) {
    val builders = listOf(
        { Style.Builder().fromUri(STYLE_URL) },
        { Style.Builder().fromUri(STYLE_URL_FALLBACK) },
        { Style.Builder().fromJson(RASTER_STYLE_JSON) },
    )
    var token = 0
    var settled = false
    var index = -1

    fun next(from: Int) {
        if (settled || from != index || index >= builders.lastIndex) return
        index++
        val mine = ++token
        map.setStyle(builders[index]()) { style ->
            if (!settled && mine == token) {
                settled = true
                onLoaded(style)
            }
        }
        val at = index
        mapView.postDelayed({ if (mine == token && mapView.isAttachedToWindow) next(at) }, STYLE_TIMEOUT_MS)
    }

    mapView.addOnDidFailLoadingMapListener { if (!settled) next(index) }
    next(-1)
}

/**
 * Карта с непрозрачным фоном на время загрузки стиля: пока стиль не пришёл (первое открытие, плохая сеть), поверхность карты
 * иначе остаётся прозрачной и сквозь неё виден экран под картой. [backgroundColor] - цвет фона темы приложения.
 */
internal fun newMapView(context: Context, backgroundColor: Int): MapView =
    MapView(context, MapLibreMapOptions.createFromAttributes(context).foregroundLoadColor(backgroundColor)).also {
        it.setBackgroundColor(backgroundColor)
    }
