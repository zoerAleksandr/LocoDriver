package com.z_company.route.component.servicephase

import android.content.Context
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory

/** Запасная векторная подложка (в России без VPN бывает недоступна — тогда работает только свой прокси). */
internal const val STYLE_URL_FALLBACK = "https://tiles.openfreemap.org/styles/liberty"

/** Последний запас — растровая подложка OSM (с атрибуцией); линия плеча и точки рисуются поверх. */
private const val RASTER_STYLE_JSON = """{"version":8,"sources":{"osm":{"type":"raster","tiles":["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],"tileSize":256,"maxzoom":19,"attribution":"© OpenStreetMap contributors"}},"layers":[{"id":"osm","type":"raster","source":"osm"}]}"""

/** Сколько ждём загрузки стиля, прежде чем перейти к следующему варианту (сеть «висит», ошибки нет). */
private const val STYLE_TIMEOUT_MS = 10_000L
private const val DARK_STYLE_URL = "https://tiles.openfreemap.org/styles/dark"

/**
 * Загружает подложку цепочкой: 1) свой прокси [STYLE_URL]; 2) OpenFreeMap [STYLE_URL_FALLBACK];
 * 3) растр OSM. К следующему варианту переходим по ошибке загрузки карты или по таймауту.
 * [onLoaded] вызывается один раз — для первого загрузившегося стиля (слои добавляются в него).
 */
internal fun loadMapStyle(
    mapView: MapView,
    map: MapLibreMap,
    dark: Boolean = false,
    morePathsAtLowZoom: Boolean = false,
    onLoaded: (Style) -> Unit,
) {
    val builders = if (dark) listOf(
        { Style.Builder().fromUri(DARK_STYLE_URL) },
        { Style.Builder().fromUri(STYLE_URL_FALLBACK) },
        { Style.Builder().fromJson(RASTER_STYLE_JSON) },
    ) else listOf(
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
                if (morePathsAtLowZoom) makeMinorPathsVisibleEarlier(style)
                onLoaded(style)
            }
        }
        val at = index
        mapView.postDelayed({ if (mine == token && mapView.isAttachedToWindow) next(at) }, STYLE_TIMEOUT_MS)
    }

    mapView.addOnDidFailLoadingMapListener { if (!settled) next(index) }
    next(-1)
}

/** Low-zoom visibility override for minor OSM/OpenMapTiles paths; the source may still omit generalized features. */
private fun makeMinorPathsVisibleEarlier(style: Style) {
    fun widenAtLowerZoom(layerId: String, minZoom: Float, startWidth: Float, midWidth: Float, fullWidth: Float) {
        val layer = style.getLayerAs<LineLayer>(layerId) ?: return
        layer.setMinZoom(minZoom)
        layer.setProperties(
            PropertyFactory.lineWidth(
                Expression.interpolate(
                    Expression.linear(), Expression.zoom(),
                    Expression.stop(minZoom, startWidth),
                    Expression.stop(minZoom + 1.5f, midWidth),
                    Expression.stop(minZoom + 4.5f, fullWidth),
                )
            )
        )
    }
    widenAtLowerZoom("road_minor", 10.5f, 0.3f, 0.65f, 1.6f)
    widenAtLowerZoom("road_service_track", 11.5f, 0.25f, 0.55f, 1.25f)
    widenAtLowerZoom("road_path_pedestrian", 12.5f, 0.35f, 0.7f, 1.5f)
}

/**
 * Карта с непрозрачным фоном на время загрузки стиля: пока стиль не пришёл (первое открытие, плохая сеть), поверхность карты
 * иначе остаётся прозрачной и сквозь неё виден экран под картой. [backgroundColor] - цвет фона темы приложения.
 */
internal fun newMapView(context: Context, backgroundColor: Int): MapView =
    MapView(context, MapLibreMapOptions.createFromAttributes(context).foregroundLoadColor(backgroundColor)).also {
        it.setBackgroundColor(backgroundColor)
    }
