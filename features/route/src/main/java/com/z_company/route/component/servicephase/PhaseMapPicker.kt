package com.z_company.route.component.servicephase

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PointF
import android.graphics.RectF
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import java.util.concurrent.Executors

/** Векторная подложка без ключа (OpenMapTiles): ж/д линии и станции видны. */
internal const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val NEIGHBOURS_SOURCE = "phase-neighbours"
private const val NEIGHBOURS_LAYER = "phase-neighbours-layer"
private const val OMT_SOURCE = "openmaptiles"
private const val RAIL_LINE_LAYER = "phase-rail-line"
private const val RAIL_STATION_LAYER = "phase-rail-station"
private const val RAIL_STATION_LABEL = "phase-rail-station-label"

/**
 * Ж/д линии и станции поверх подложки из того же векторного источника (OpenMapTiles):
 * в стиле Liberty пути бледные, а значков станций на среднем масштабе нет.
 */
internal fun addRailwayLayers(style: org.maplibre.android.maps.Style) {
    if (style.getSource(OMT_SOURCE) == null) return
    val isRail = Expression.eq(Expression.get("class"), "rail")
    style.addLayer(
        LineLayer(RAIL_LINE_LAYER, OMT_SOURCE).withSourceLayer("transportation").withFilter(isRail).withProperties(
            PropertyFactory.lineColor("#5B6475"),
            PropertyFactory.lineWidth(Expression.interpolate(Expression.linear(), Expression.zoom(),
                Expression.stop(8, 1f), Expression.stop(14, 2.5f))),
        )
    )
    // OpenMapTiles: слой poi, class = railway, subclass = station | halt (метро — subway, не берём).
    // В тайлах станции есть с масштаба 14.
    val isStation = Expression.all(
        Expression.eq(Expression.get("class"), "railway"),
        Expression.any(
            Expression.eq(Expression.get("subclass"), "station"),
            Expression.eq(Expression.get("subclass"), "halt"),
        ),
    )
    style.addLayer(
        CircleLayer(RAIL_STATION_LAYER, OMT_SOURCE).withSourceLayer("poi").withFilter(isStation).withProperties(
            PropertyFactory.circleRadius(5f),
            PropertyFactory.circleColor("#4A5A74"),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleStrokeWidth(2f),
        )
    )
    style.addLayer(
        SymbolLayer(RAIL_STATION_LABEL, OMT_SOURCE).withSourceLayer("poi").withFilter(isStation).withProperties(
            PropertyFactory.textField(Expression.coalesce(Expression.get("name:ru"), Expression.get("name"))),
            // Шрифт — из самого стиля: шрифта по умолчанию у OpenFreeMap нет, без него ломаются все подписи.
            PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
            PropertyFactory.textSize(12f),
            PropertyFactory.textOffset(arrayOf(0f, 1.1f)),
            PropertyFactory.textAnchor("top"),
            PropertyFactory.textColor("#2A3140"),
            PropertyFactory.textHaloColor("#FFFFFF"),
            PropertyFactory.textHaloWidth(1.5f),
        )
    )
}

/** Точка, выбранная на карте. [snappedName] — название станции OSM, если привязались тапом к ней. */
internal data class PickedPoint(val latitude: Double, val longitude: Double, val snappedName: String?)

/**
 * Полноэкранный выбор точки станции (дизайн, экран 16): карта двигается под
 * неподвижной меткой; «Выбрать» — координаты под меткой. Тап по станции на карте
 * ставит точку на неё. Соседние станции плеча с координатами — ориентиры.
 * «Назад» / системный назад — выйти без изменений.
 */
@Composable
internal fun PhaseMapPicker(
    stationName: String,
    kindLabel: String,
    start: Pair<Double, Double>?,
    neighbours: List<Pair<Double, Double>>,
    onPick: (PickedPoint) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val context = LocalContext.current
        remember { MapLibre.getInstance(context); true }
        var map by remember { mutableStateOf<MapLibreMap?>(null) }
        var snapped by remember { mutableStateOf<String?>(null) }
        var locationDenied by remember { mutableStateOf(false) }
        val density = LocalDensity.current
        // Карта жива (до onDestroy). После закрытия в очереди ещё бывают события камеры —
        // запрос к уничтоженному отрисовщику (queryRenderedFeatures) роняет приложение
        // нативно (SIGSEGV в MapRenderer::actor). Все обработчики проверяют этот флаг.
        val alive = remember { java.util.concurrent.atomic.AtomicBoolean(true) }

        val mapView = remember {
            MapView(context).also { mv ->
                mv.getMapAsync { m ->
                    map = m
                    m.uiSettings.isRotateGesturesEnabled = false
                    m.uiSettings.isTiltGesturesEnabled = false
                    m.uiSettings.isAttributionEnabled = true
                    m.uiSettings.isLogoEnabled = false
                    m.cameraPosition = CameraPosition.Builder()
                        .target(initialTarget(start, neighbours))
                        .zoom(if (start != null) 15.0 else if (neighbours.isNotEmpty()) 12.0 else 5.0)
                        .build()
                    m.setStyle(STYLE_URL) { style ->
                        addRailwayLayers(style)
                        val features = neighbours.map { (lat, lon) -> Feature.fromGeometry(Point.fromLngLat(lon, lat)) }
                        style.addSource(GeoJsonSource(NEIGHBOURS_SOURCE, FeatureCollection.fromFeatures(features)))
                        style.addLayer(
                            CircleLayer(NEIGHBOURS_LAYER, NEIGHBOURS_SOURCE).withProperties(
                                PropertyFactory.circleRadius(6f),
                                PropertyFactory.circleColor("#FFFFFF"),
                                PropertyFactory.circleStrokeColor("#8A96AA"),
                                PropertyFactory.circleStrokeWidth(2f),
                            )
                        )
                    }
                    m.addOnCameraMoveStartedListener { reason ->
                        if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) snapped = null
                    }
                    // Центр камеры = острие метки (42% высоты): нижний отступ 16% высоты карты.
                    mv.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                        m.setPadding(0, 0, 0, (v.height * 0.16).toInt())
                    }
                    if (start == null && neighbours.isEmpty() && hasLocationPermission(context)) moveToMyLocation(context, m, alive)
                    // Запросов к отрисовке (queryRenderedFeatures) из событий камеры не делаем:
                    // событие успевает прийти, когда окно карты уже закрывается и отрисовщик
                    // уничтожен, — это нативное падение (SIGSEGV в MapRenderer::actor).
                    // Запрос — только на тап пользователя по карте.
                    m.addOnMapClickListener { latLng ->
                        if (!alive.get() || m.style?.isFullyLoaded != true) return@addOnMapClickListener false
                        val station = stationAt(m, m.projection.toScreenLocation(latLng), with(density) { 24.dp.toPx() })
                        if (station != null) {
                            m.animateCamera(CameraUpdateFactory.newLatLng(station.first), 300)
                            snapped = station.second
                        }
                        station != null
                    }
                }
            }
        }
        DisposableEffect(Unit) {
            mapView.onStart()
            mapView.onResume()
            onDispose {
                alive.set(false)
                map = null
                mapView.onPause()
                mapView.onStop()
                mapView.onDestroy()
            }
        }

        val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.any { it }) moveToMyLocation(context, map, alive) else locationDenied = true
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Pst.bg)) {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView })

            // Метка: нижняя точка — на 42% высоты карты (центр карты — 50%, метку поднимаем)
            val pinBottom = maxHeight * 0.42f
            Column(
                modifier = Modifier.align(Alignment.TopCenter).offset(y = pinBottom - 44.dp - 14.dp - 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(44.dp).shadow(6.dp, CircleShape).background(Pst.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { PstIcon(PstIcons.MapPin, 22.dp, Pst.onAccent) }
                Box(Modifier.size(width = 2.dp, height = 14.dp).background(Pst.accent))
                Box(Modifier.size(22.dp).background(Pst.accent.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(10.dp).background(Pst.accent, CircleShape))
                }
            }

            // Верхняя панель
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapButton(onClick = onDismiss, description = "Назад") { PstIcon(PstIcons.ChevronLeft, 22.dp, Pst.text) }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .background(Pst.surface, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stationName,
                        style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        color = Pst.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(text = "Укажите место станции", style = Pst.sans.copy(fontSize = 12.sp), color = Pst.textMuted)
                }
            }

            // Кнопки справа
            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp).offset(y = maxHeight * 0.46f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapButton(onClick = { map?.animateCamera(CameraUpdateFactory.zoomIn()) }, description = "Приблизить") {
                    Text("+", style = Pst.sans.copy(fontSize = 22.sp), color = Pst.text)
                }
                MapButton(onClick = { map?.animateCamera(CameraUpdateFactory.zoomOut()) }, description = "Отдалить") {
                    Text("−", style = Pst.sans.copy(fontSize = 22.sp), color = Pst.text)
                }
                MapButton(
                    onClick = {
                        if (hasLocationPermission(context)) moveToMyLocation(context, map, alive)
                        else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    },
                    description = "Моё местоположение",
                ) {
                    Box(Modifier.size(14.dp).border(2.dp, Pst.accent, CircleShape), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(4.dp).background(Pst.accent, CircleShape))
                    }
                }
            }

            // Нижняя панель
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Pst.bg, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(Pst.accentSoft, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) { PstIcon(PstIcons.MapPin, 18.dp, Pst.accent) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$stationName · $kindLabel",
                            style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            color = Pst.text,
                        )
                        Text(
                            text = when {
                                locationDenied -> "Нет доступа к местоположению — двигайте карту вручную"
                                snapped != null -> "Точка на станции «$snapped» с карты"
                                else -> "Двигайте карту — точка под меткой станет координатами станции"
                            },
                            style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp),
                            color = Pst.textMuted,
                        )
                    }
                }
                PstButton(
                    text = "Выбрать",
                    kind = PstButtonKind.CTA,
                    enabled = map != null,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val point = map?.cameraPosition?.target ?: return@PstButton
                        onPick(PickedPoint(point.latitude, point.longitude, snapped))
                    },
                )
            }
        }
    }
}

@Composable
private fun MapButton(onClick: () -> Unit, description: String, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(Pst.surface, RoundedCornerShape(16.dp))
            .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private fun initialTarget(start: Pair<Double, Double>?, neighbours: List<Pair<Double, Double>>): LatLng = when {
    start != null -> LatLng(start.first, start.second)
    neighbours.isNotEmpty() -> LatLng(neighbours.map { it.first }.average(), neighbours.map { it.second }.average())
    else -> LatLng(55.75, 37.62)
}

/** Станция OSM под пальцем (наш слой станций): (точка, название). */
private fun stationAt(map: MapLibreMap, screen: PointF, radiusPx: Float): Pair<LatLng, String>? {
    val box = RectF(screen.x - radiusPx, screen.y - radiusPx, screen.x + radiusPx, screen.y + radiusPx)
    return map.queryRenderedFeatures(box, RAIL_STATION_LAYER).firstNotNullOfOrNull { f ->
        val name = f.getStringProperty("name:ru") ?: f.getStringProperty("name")
        val point = f.geometry() as? Point
        if (point != null && name != null) {
            LatLng(point.latitude(), point.longitude()) to name
        } else null
    }
}


private fun hasLocationPermission(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

@SuppressLint("MissingPermission")
private fun moveToMyLocation(context: Context, map: MapLibreMap?, alive: java.util.concurrent.atomic.AtomicBoolean) {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
    val provider = when {
        lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> return
    }
    // Координата может прийти, когда карту уже закрыли: тогда map уже null — ничего не делаем.
    val move = { lat: Double, lon: Double ->
        if (alive.get()) map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 15.0))
    }
    lm.getLastKnownLocation(provider)?.let { move(it.latitude, it.longitude) }
    LocationManagerCompat.getCurrentLocation(
        lm, provider, null as androidx.core.os.CancellationSignal?, Executors.newSingleThreadExecutor(),
        androidx.core.util.Consumer<android.location.Location?> { loc ->
            if (loc != null) ContextCompat.getMainExecutor(context).execute { move(loc.latitude, loc.longitude) }
        },
    )
}
