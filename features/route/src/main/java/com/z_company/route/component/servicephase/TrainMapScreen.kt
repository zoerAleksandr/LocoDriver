package com.z_company.route.component.servicephase

import android.Manifest
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import android.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.z_company.core.gps.GpsRecorder
import com.z_company.domain.track.HeadingEstimator
import androidx.compose.runtime.collectAsState
import com.z_company.domain.entities.setting.ServicePhaseStationKind
import com.z_company.domain.track.TrackAnalysis
import com.z_company.domain.track.TrackPoint
import com.z_company.domain.track.TrainStopMark
import com.z_company.domain.util.LabelPlacer
import com.z_company.domain.util.MapLabelItem
import com.z_company.route.viewmodel.TrainFormViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import com.z_company.domain.util.GeoFix
import com.z_company.domain.util.TrainMapRoute
import com.z_company.domain.util.TrainMapStop
import com.z_company.route.viewmodel.ServicePhaseAutoFillViewModel
import com.z_company.route.viewmodel.message
import com.z_company.route.util.DeviceLocation
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import java.util.concurrent.atomic.AtomicBoolean

private const val TRAIN_ME_SOURCE = "train-map-me"
private const val TRAIN_ROUTE_SOURCE = "train-map-route"
private const val TRAIN_STOPS_SOURCE = "train-map-stops"
private const val TRAIN_TRACK_SOURCE = "train-map-track"
private const val TRAIN_MARKS_SOURCE = "train-map-marks"
private const val TRAIN_SEL_SOURCE = "train-map-sel"
private const val TRAIN_TAIL_SOURCE = "train-map-tail"
private const val TRAIN_OFF_SOURCE = "train-map-off"
private const val ME_ICON_IMAGE = "train-me-icon"

/** Выбранное тапом на карте: точка пройденного пути или остановка. */
private sealed interface TrainMapSelection {
    data class PathPoint(val point: TrackPoint) : TrainMapSelection
    data class Stop(val mark: TrainStopMark) : TrainMapSelection
}

/** Шкала скорости (км/ч → цвет): красный — стоим, далее оранжевый, жёлтый, зелёный, бирюзовый, синий. */
private val SPEED_STOPS = com.z_company.domain.track.SpeedScale.STOPS.map { (v, rgb) -> v to String.format("#%06X", rgb) }

/**
 * Карта поезда (SCREEN_SPECS §6.9): полноэкранная карта на общей подложке карты плеч (цепочка тайлов, ж/д слои).
 * Состояние 1 — пустая карта с текущим местоположением; состояние 3 — если у станции поезда/плеча нет координат,
 * внизу предложение «Определить координаты по GPS» ([onDetermine] пишет их в справочник станций; существующие
 * координаты не перезаписываются). Состояние 2 — если у плеча есть ≥ 2 станции с координатами ([stops]), маршрут
 * по рельсам ([loadRailLine], тот же запрос, что у карты плеч) рисуется тонким пунктиром, станции — точками,
 * ближайшая впереди выделена, камера охватывает маршрут и текущее место. Запросов к отрисовке нет (см. PhaseMapPicker про падение).
 */
@Composable
internal fun TrainMapScreen(
    missingStation: () -> String?,
    stops: List<TrainMapStop>,
    loadRailLine: suspend (List<Pair<Double, Double>>) -> ServicePhaseAutoFillViewModel.RailLineResult,
    currentFix: suspend () -> GeoFix?,
    loadTrack: suspend () -> TrainFormViewModel.TrainMapTrack? = { null },
    onDetermine: suspend (String) -> Boolean,
    onOpenConsentSettings: () -> Unit = {},
    onDismiss: () -> Unit,
    /** Заголовок плашки; для просмотра трека из «Истории» — название поездки. */
    title: String = "Карта поезда",
    /** Только просмотр записанного трека (§17.8): без геолокации, живой скорости и обновления по таймеру. */
    viewOnly: Boolean = false,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val context = LocalContext.current
        remember { MapLibre.getInstance(context); true }
        var map by remember { mutableStateOf<MapLibreMap?>(null) }
        var styleReady by remember { mutableStateOf(false) }
        var locationDenied by remember { mutableStateOf(false) }
        var missing by remember { mutableStateOf(missingStation()) }
        var determining by remember { mutableStateOf(false) }
        var determineFailed by remember { mutableStateOf(false) }
        var savedName by remember { mutableStateOf<String?>(null) }
        var myFix by remember { mutableStateOf<GeoFix?>(null) }
        val hasRoute = TrainMapRoute.hasRoute(stops)
        var railState by remember { mutableStateOf<ServicePhaseAutoFillViewModel.RailLineResult?>(null) }
        var loadAttempt by remember { mutableStateOf(0) }
        var fittedKey by remember { mutableStateOf<String?>(null) }
        var track by remember { mutableStateOf<TrainFormViewModel.TrainMapTrack?>(null) }
        var liveSpeed by remember { mutableStateOf<Double?>(null) }
        var liveAtMs by remember { mutableStateOf(0L) }
        var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
        // Следование камеры за положением (§6.9): включено по умолчанию; жест пользователя выключает, «Моё местоположение» возвращает.
        var follow by remember { mutableStateOf(true) }
        var permGranted by remember { mutableStateOf(DeviceLocation.hasPermission(context)) }
        var selection by remember { mutableStateOf<TrainMapSelection?>(null) }
        var showChart by rememberSaveable { mutableStateOf(false) }
        // Курс маркера (Location.bearing, иначе по двум точкам; на стоянке сохраняется), значок положения, режим «еду по карте».
        val heading = remember { HeadingEstimator() }
        var headingDeg by remember { mutableStateOf<Double?>(null) }
        val positionIcon by PositionIconStore.selected(context).collectAsState()
        var showIconPicker by remember { mutableStateOf(false) }
        var ride by rememberSaveable { mutableStateOf(false) }
        // Масштаб, выбранный пользователем кнопками «+»/«−»: сохраняется при следовании (иначе слежение возвращало 14).
        var userZoom by remember { mutableStateOf<Double?>(null) }
        var snappedLast by remember { mutableStateOf<TrackPoint?>(null) }
        var offSegments by remember { mutableStateOf(0) }
        val autoRunning by GpsRecorder.isRunning.collectAsState()
        val density = context.resources.displayMetrics.density
        val alive = remember { AtomicBoolean(true) }
        val scope = rememberCoroutineScope()

        // Маркер положения: стрелка по курсу в движении (скорость ≥ 2 км/ч, курс известен), иначе синяя точка.
        fun setMe(m: MapLibreMap, lat: Double, lon: Double, bearing: Double?) {
            m.style?.getSourceAs<GeoJsonSource>(TRAIN_ME_SOURCE)?.setGeoJson(
                Feature.fromGeometry(Point.fromLngLat(lon, lat)).apply {
                    addNumberProperty("moving", if (bearing != null) 1 else 0)
                    addNumberProperty("bearing", bearing ?: 0.0)
                }
            )
        }

        fun showMyLocation(center: Boolean = true) {
            scope.launch {
                val fix = currentFix() ?: run {
                    locationDenied = true
                    return@launch
                }
                locationDenied = false
                val m = map ?: return@launch
                if (!alive.get()) return@launch
                setMe(m, fix.latitude, fix.longitude, heading.heading)
                myFix = fix
                if (center) {
                    userZoom = 14.0
                    m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(fix.latitude, fix.longitude), 14.0))
                }
            }
        }
        val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.any { it }) {
                permGranted = true
                showMyLocation(!hasRoute)
            } else locationDenied = true
        }
        // center = false: при маршруте камеру ведёт охват маршрута, кнопка «Моё местоположение» центрирует на себе.
        fun requestOrShowLocation(center: Boolean = true) {
            if (DeviceLocation.hasPermission(context)) showMyLocation(center)
            else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }

        // «+»/«−»: масштаб запоминается как выбор пользователя и сохраняется при следовании за положением.
        fun zoomBy(delta: Double) {
            val m = map ?: return
            val z = ((userZoom ?: m.cameraPosition.zoom) + delta).coerceIn(m.minZoomLevel, m.maxZoomLevel)
            userZoom = z
            m.animateCamera(CameraUpdateFactory.zoomTo(z))
        }

        val mapView = remember {
            MapView(context).also { mv ->
                mv.getMapAsync { m ->
                    map = m
                    m.uiSettings.isRotateGesturesEnabled = false
                    m.uiSettings.isTiltGesturesEnabled = false
                    m.uiSettings.isLogoEnabled = false
                    m.uiSettings.isAttributionEnabled = true
                    m.cameraPosition = CameraPosition.Builder().target(LatLng(55.75, 37.62)).zoom(5.0).build()
                    loadMapStyle(mv, m) { style ->
                        addRailwayLayers(style)
                        style.addSource(GeoJsonSource(TRAIN_ME_SOURCE, FeatureCollection.fromFeatures(emptyList())))
                        style.addLayer(
                            CircleLayer("train-map-me-layer", TRAIN_ME_SOURCE).withProperties(
                                PropertyFactory.circleRadius(7f),
                                PropertyFactory.circleColor("#00A0F5"),
                                PropertyFactory.circleStrokeColor("#FFFFFF"),
                                PropertyFactory.circleStrokeWidth(3f),
                            ).also { it.setFilter(Expression.neq(Expression.get("moving"), 1)) }
                        )
                        style.addImage(ME_ICON_IMAGE, renderPositionIcon(context, positionIcon, (positionIcon.mapHeightDp * density).toInt()))
                        style.addLayer(
                            SymbolLayer("train-map-me-arrow", TRAIN_ME_SOURCE).withProperties(
                                PropertyFactory.iconImage(ME_ICON_IMAGE),
                                PropertyFactory.iconRotate(Expression.get("bearing")),
                                PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                                PropertyFactory.iconAllowOverlap(true),
                                PropertyFactory.iconIgnorePlacement(true),
                            ).also { it.setFilter(Expression.eq(Expression.get("moving"), 1)) }
                        )
                        // Будущий маршрут (§6.9, состояние 2): пунктир под точками станций; данные — по готовности.
                        style.addSource(GeoJsonSource(TRAIN_ROUTE_SOURCE, FeatureCollection.fromFeatures(emptyList())))
                        style.addSource(GeoJsonSource(TRAIN_STOPS_SOURCE, FeatureCollection.fromFeatures(emptyList())))
                        style.addLayerBelow(
                            LineLayer("train-map-route-layer", TRAIN_ROUTE_SOURCE).withProperties(
                                PropertyFactory.lineColor("#00A0F5"),
                                PropertyFactory.lineWidth(2.5f),
                                PropertyFactory.lineDasharray(arrayOf(2f, 2f)),
                                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                            ),
                            "train-map-me-layer",
                        )
                        val role = Expression.get("role")
                        style.addLayerBelow(
                            CircleLayer("train-map-stops-layer", TRAIN_STOPS_SOURCE).withProperties(
                                PropertyFactory.circleRadius(
                                    Expression.match(role, Expression.literal(5f),
                                        Expression.stop("ahead", 9f), Expression.stop("terminal", 7f), Expression.stop("halt", 3.5f))
                                ),
                                PropertyFactory.circleColor(
                                    Expression.match(role, Expression.literal("#FFFFFF"),
                                        Expression.stop("ahead", "#00A0F5"), Expression.stop("terminal", "#0A0E14"))
                                ),
                                PropertyFactory.circleStrokeColor(
                                    Expression.match(role, Expression.literal("#0A0E14"), Expression.stop("ahead", "#FFFFFF"))
                                ),
                                PropertyFactory.circleStrokeWidth(
                                    Expression.match(role, Expression.literal(2f), Expression.stop("halt", 1.5f), Expression.stop("ahead", 3f))
                                ),
                            ),
                            "train-map-me-layer",
                        )
                        style.addLayerBelow(
                            SymbolLayer("train-map-stops-label", TRAIN_STOPS_SOURCE).withProperties(
                                PropertyFactory.textField(Expression.get("name")),
                                PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
                                PropertyFactory.textSize(
                                    Expression.match(role, Expression.literal(11f),
                                        Expression.stop("ahead", 13f), Expression.stop("terminal", 13f))
                                ),
                                PropertyFactory.symbolSortKey(Expression.get("sort")),
                                PropertyFactory.textOffset(arrayOf(0f, 1.2f)),
                                PropertyFactory.textAnchor("top"),
                                PropertyFactory.textColor("#0A0E14"),
                                PropertyFactory.textHaloColor("#FFFFFF"),
                                PropertyFactory.textHaloWidth(1.5f),
                                // Какие подписи помещаются — решает LabelPlacer (свойство show), см. refreshStopSources.
                                PropertyFactory.textAllowOverlap(true),
                                PropertyFactory.textIgnorePlacement(true),
                            ).also { it.setFilter(Expression.eq(Expression.get("show"), 1)) },
                            "train-map-me-layer",
                        )
                        // Пройденный путь (§6.9, состояние 4): белая подложка + жирная линия цвета скорости, под точками станций.
                        style.addSource(GeoJsonSource(TRAIN_TRACK_SOURCE, FeatureCollection.fromFeatures(emptyList())))
                        val lineWidth = Expression.interpolate(
                            Expression.linear(), Expression.zoom(),
                            Expression.stop(6, 3f), Expression.stop(12, 5.5f), Expression.stop(16, 8f),
                        )
                        val casingWidth = Expression.interpolate(
                            Expression.linear(), Expression.zoom(),
                            Expression.stop(6, 5.5f), Expression.stop(12, 8.5f), Expression.stop(16, 11f),
                        )
                        val speedColor = Expression.switchCase(
                            Expression.lt(Expression.get("speed"), Expression.literal(0)),
                            Expression.color(Color.parseColor("#78909C")),
                            Expression.interpolate(
                                Expression.linear(), Expression.get("speed"),
                                *SPEED_STOPS.map { (v, c) -> Expression.stop(v, Expression.color(Color.parseColor(c))) }.toTypedArray(),
                            ),
                        )
                        style.addLayerBelow(
                            LineLayer("train-map-track-casing", TRAIN_TRACK_SOURCE).withProperties(
                                PropertyFactory.lineColor("#FFFFFF"),
                                PropertyFactory.lineOpacity(0.9f),
                                PropertyFactory.lineWidth(casingWidth),
                                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                            ),
                            "train-map-stops-layer",
                        )
                        style.addLayerBelow(
                            LineLayer("train-map-track-layer", TRAIN_TRACK_SOURCE).withProperties(
                                PropertyFactory.lineColor(speedColor),
                                PropertyFactory.lineWidth(lineWidth),
                                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                            ),
                            "train-map-stops-layer",
                        )
                        // «Хвост» от последней точки трека до текущего положения (жирная линия не отстаёт от маркера) и
                        // пунктир поверх участков вне путей (отклонение от рельсов > 50 м).
                        style.addSource(GeoJsonSource(TRAIN_TAIL_SOURCE, FeatureCollection.fromFeatures(emptyList())))
                        style.addSource(GeoJsonSource(TRAIN_OFF_SOURCE, FeatureCollection.fromFeatures(emptyList())))
                        style.addLayerBelow(
                            LineLayer("train-map-tail-casing", TRAIN_TAIL_SOURCE).withProperties(
                                PropertyFactory.lineColor("#FFFFFF"),
                                PropertyFactory.lineOpacity(0.9f),
                                PropertyFactory.lineWidth(casingWidth),
                                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                            ),
                            "train-map-stops-layer",
                        )
                        style.addLayerBelow(
                            LineLayer("train-map-tail-layer", TRAIN_TAIL_SOURCE).withProperties(
                                PropertyFactory.lineColor(speedColor),
                                PropertyFactory.lineWidth(lineWidth),
                                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                            ),
                            "train-map-stops-layer",
                        )
                        style.addLayerBelow(
                            LineLayer("train-map-off-layer", TRAIN_OFF_SOURCE).withProperties(
                                PropertyFactory.lineColor("#0A0E14"),
                                PropertyFactory.lineWidth(1.6f),
                                PropertyFactory.lineDasharray(arrayOf(1.5f, 2f)),
                            ),
                            "train-map-stops-layer",
                        )
                        // Остановки (станции поезда и стоянки по треку) и выбранная тапом точка — поверх всего, кроме «я».
                        style.addSource(GeoJsonSource(TRAIN_MARKS_SOURCE, FeatureCollection.fromFeatures(emptyList())))
                        style.addSource(GeoJsonSource(TRAIN_SEL_SOURCE, FeatureCollection.fromFeatures(emptyList())))
                        style.addLayerBelow(
                            CircleLayer("train-map-marks-layer", TRAIN_MARKS_SOURCE).withProperties(
                                PropertyFactory.circleRadius(7f),
                                PropertyFactory.circleColor("#E65100"),
                                PropertyFactory.circleStrokeColor("#FFFFFF"),
                                PropertyFactory.circleStrokeWidth(2.5f),
                            ),
                            "train-map-me-layer",
                        )
                        style.addLayerBelow(
                            SymbolLayer("train-map-marks-label", TRAIN_MARKS_SOURCE).withProperties(
                                PropertyFactory.textField(Expression.get("name")),
                                PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
                                PropertyFactory.textSize(11f),
                                PropertyFactory.textOffset(arrayOf(0f, 1.3f)),
                                PropertyFactory.textAnchor("top"),
                                PropertyFactory.textColor("#0A0E14"),
                                PropertyFactory.textHaloColor("#FFFFFF"),
                                PropertyFactory.textHaloWidth(1.5f),
                                PropertyFactory.textAllowOverlap(true),
                                PropertyFactory.textIgnorePlacement(true),
                            ).also { it.setFilter(Expression.eq(Expression.get("show"), 1)) },
                            "train-map-me-layer",
                        )
                        style.addLayerBelow(
                            CircleLayer("train-map-sel-layer", TRAIN_SEL_SOURCE).withProperties(
                                PropertyFactory.circleRadius(9f),
                                PropertyFactory.circleColor("#FFFFFF"),
                                PropertyFactory.circleStrokeColor("#0A0E14"),
                                PropertyFactory.circleStrokeWidth(3.5f),
                            ),
                            "train-map-me-layer",
                        )
                        styleReady = true
                    }
                }
            }
        }
        // Текущее место — как только карта и стиль готовы (разрешение — при необходимости).
        LaunchedEffect(styleReady) { if (styleReady && !viewOnly) requestOrShowLocation(center = !hasRoute) }
        // Линия по рельсам с сервера (всегда запрос; сохранённая — запас без сети), как на карте плеч.
        LaunchedEffect(loadAttempt) {
            if (hasRoute) {
                railState = null
                railState = loadRailLine(stops.map { it.latitude to it.longitude })
            }
        }
        // Станции плеча и остановки по треку точками; подписи — только те, что помещаются на текущем зуме
        // (LabelPlacer: не перекрывают точки и друг друга, в приоритете крайние и «впереди»).
        fun refreshStopSources() {
            val m = map ?: return
            val style = m.style ?: return
            if (!alive.get()) return
            // «Впереди» — вдоль рельсовой линии с сервера (не по прямым между станциями); без линии не выделяется.
            val railLine = (railState as? ServicePhaseAutoFillViewModel.RailLineResult.Ok)?.line
            val ahead = if (hasRoute) TrainMapRoute.nextAhead(stops, railLine, myFix)?.index else null
            val marks = track?.stops.orEmpty()
            data class Row(val lat: Double, val lon: Double, val name: String, val role: String, val sizeSp: Float, val dotR: Float, val prio: Int)
            val routeRows = if (!hasRoute) emptyList() else stops.mapIndexed { i, st ->
                val role = when {
                    i == ahead -> "ahead"
                    st.kind == null -> "terminal"
                    st.kind == ServicePhaseStationKind.HALT -> "halt"
                    else -> "station"
                }
                Row(
                    st.latitude, st.longitude, st.name, role,
                    if (role == "ahead" || role == "terminal") 13f else 11f,
                    when (role) { "ahead" -> 10.5f; "terminal" -> 8f; "halt" -> 4.25f; else -> 6f },
                    if (role == "ahead" || role == "terminal") 0 else 1,
                )
            }
            val markRows = marks.map { Row(it.latitude, it.longitude, it.name.orEmpty(), "mark", 11f, 8.5f, 1) }
            val rows = routeRows + markRows
            val items = rows.map { r ->
                val pt = m.projection.toScreenLocation(LatLng(r.lat, r.lon))
                val fs = r.sizeSp * density
                MapLabelItem(
                    pt.x.toDouble(), pt.y.toDouble(),
                    labelW = if (r.name.isBlank()) 0.0 else r.name.length * fs * 0.55,
                    labelH = fs * 1.3,
                    dotRadius = r.dotR.toDouble() * density,
                    priority = r.prio,
                )
            }
            val shown = LabelPlacer.visible(items)
            fun feature(i: Int, r: Row) = Feature.fromGeometry(Point.fromLngLat(r.lon, r.lat)).apply {
                addStringProperty("name", r.name)
                addStringProperty("role", r.role)
                addNumberProperty("sort", r.prio)
                addNumberProperty("show", if (i in shown) 1 else 0)
            }
            style.getSourceAs<GeoJsonSource>(TRAIN_STOPS_SOURCE)?.setGeoJson(
                FeatureCollection.fromFeatures(routeRows.mapIndexed { i, r -> feature(i, r) })
            )
            style.getSourceAs<GeoJsonSource>(TRAIN_MARKS_SOURCE)?.setGeoJson(
                FeatureCollection.fromFeatures(markRows.mapIndexed { i, r -> feature(routeRows.size + i, r) })
            )
        }
        LaunchedEffect(styleReady, myFix, track, map, railState) { if (styleReady) refreshStopSources() }
        DisposableEffect(map) {
            val m = map
            val listener = MapLibreMap.OnCameraIdleListener { refreshStopSources() }
            m?.addOnCameraIdleListener(listener)
            onDispose { m?.removeOnCameraIdleListener(listener) }
        }
        LaunchedEffect(Unit) {
            while (isActive) {
                nowMs = System.currentTimeMillis()
                delay(5_000)
            }
        }
        // Ручной жест (перетаскивание/зум) выключает следование; программные движения камеры — нет.
        DisposableEffect(map) {
            val m = map
            val l = MapLibreMap.OnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                    follow = false
                    userZoom = null
                    if (ride) {
                        ride = false
                        map?.let { mm -> mm.easeCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder(mm.cameraPosition).bearing(0.0).build()), 300) }
                    }
                }
            }
            m?.addOnCameraMoveStartedListener(l)
            onDispose { m?.removeOnCameraMoveStartedListener(l) }
        }
        // Живая геолокация: подписка только пока карта открыта (раньше было разовое «последнее известное» — позиция не
        // обновлялась). Обновляет маркер, скорость и, в режиме «следовать» при движении, плавно ведёт камеру.
        LaunchedEffect(styleReady, permGranted) {
            if (!styleReady || !permGranted || viewOnly) return@LaunchedEffect
            DeviceLocation.updates(context).collect { f ->
                val m = map ?: return@collect
                if (!alive.get()) return@collect
                val moving = (f.speedKmh ?: 0.0) >= 2.0
                locationDenied = false
                myFix = GeoFix(f.latitude, f.longitude, f.accuracyM)
                liveSpeed = f.speedKmh
                liveAtMs = System.currentTimeMillis()
                val h = heading.update(f.latitude, f.longitude, f.speedKmh, f.bearing)
                headingDeg = h
                setMe(m, f.latitude, f.longitude, h)
                if (follow && (moving || (ride && h != null))) {
                    // Масштаб — выбранный пользователем (кнопки «+»/«−»), иначе не меньше 14; в режиме «еду по карте»
                    // карта поворачивается по ходу движения.
                    val zoom = userZoom ?: maxOf(m.cameraPosition.zoom, 14.0)
                    val cam = CameraPosition.Builder().target(LatLng(f.latitude, f.longitude)).zoom(zoom)
                        .bearing(if (ride && h != null) h else 0.0).tilt(0.0).build()
                    m.easeCamera(CameraUpdateFactory.newCameraPosition(cam), 900)
                }
            }
        }
        // Загрузка записанного трека поезда — асинхронно; пока «АВТО» пишет, обновляется раз в 10 с.
        LaunchedEffect(Unit) {
            track = runCatching { loadTrack() }.getOrNull()
            while (isActive) {
                delay(10_000)
                if (!viewOnly && GpsRecorder.isRunning.value) track = runCatching { loadTrack() }.getOrNull() ?: track
            }
        }
        // Текущая скорость — живые точки записи, пока карта открыта.
        LaunchedEffect(Unit) {
            if (viewOnly) return@LaunchedEffect
            GpsRecorder.points.collect { p ->
                liveSpeed = p.speedKmh
                liveAtMs = p.timeMs
            }
        }
        // Пройденный путь жирной линией: сегменты со скоростью, разрыв по времени линию прерывает.
        LaunchedEffect(styleReady, track, railState) {
            if (!styleReady) return@LaunchedEffect
            // Привязка к рельсовой линии маршрута там, где отклонение ≤ 50 м; иначе сырые точки («вне путей», пунктир сверху).
            val railLine = (railState as? ServicePhaseAutoFillViewModel.RailLineResult.Ok)?.line
            val raw = track?.drawPoints.orEmpty()
            val pts = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                TrainMapRoute.snapMarked(raw, railLine)
            }
            val feats = ArrayList<Feature>(pts.size)
            val offFeats = ArrayList<Feature>()
            for (i in 1 until pts.size) {
                val a = pts[i - 1]
                val b = pts[i]
                if (b.point.timeMs - a.point.timeMs > TrackAnalysis.GAP_MS) continue
                val v = b.point.speedKmh ?: a.point.speedKmh
                val seg = LineString.fromLngLats(
                    listOf(
                        Point.fromLngLat(a.point.longitude, a.point.latitude),
                        Point.fromLngLat(b.point.longitude, b.point.latitude),
                    )
                )
                feats += Feature.fromGeometry(seg).apply { addNumberProperty("speed", v ?: -1.0) }
                if (!a.onRail || !b.onRail) offFeats += Feature.fromGeometry(seg)
            }
            offSegments = offFeats.size
            snappedLast = pts.lastOrNull()?.point
            map?.style?.getSourceAs<GeoJsonSource>(TRAIN_TRACK_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(feats))
            map?.style?.getSourceAs<GeoJsonSource>(TRAIN_OFF_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(offFeats))
        }
        // «Хвост»: пока «АВТО» пишет, жирная линия дорисовывается от последней точки трека до текущего положения
        // (трек прорежен и подгружается раз в 10 с, из-за чего линия отставала от маркера на 60-100 м).
        val railLineNow = (railState as? ServicePhaseAutoFillViewModel.RailLineResult.Ok)?.line
        val railCum = remember(railLineNow) { railLineNow?.let { TrainMapRoute.cumulativeM(it) } }
        LaunchedEffect(styleReady, myFix, snappedLast, autoRunning, liveSpeed) {
            if (!styleReady || viewOnly) return@LaunchedEffect
            val src = map?.style?.getSourceAs<GeoJsonSource>(TRAIN_TAIL_SOURCE) ?: return@LaunchedEffect
            val last = snappedLast
            val fix = myFix
            val now = System.currentTimeMillis()
            if (!autoRunning || last == null || fix == null || now - last.timeMs > TrackAnalysis.GAP_MS ||
                TrackAnalysis.distanceM(last.latitude, last.longitude, fix.latitude, fix.longitude) < 3.0
            ) {
                src.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
                return@LaunchedEffect
            }
            var endLat = fix.latitude
            var endLon = fix.longitude
            if (railLineNow != null && railCum != null) {
                val pr = TrainMapRoute.project(railLineNow, railCum, endLat, endLon)
                if (pr != null && pr.offsetM <= TrainMapRoute.SNAP_TOLERANCE_M) {
                    endLat = pr.latitude
                    endLon = pr.longitude
                }
            }
            src.setGeoJson(
                Feature.fromGeometry(
                    LineString.fromLngLats(listOf(Point.fromLngLat(last.longitude, last.latitude), Point.fromLngLat(endLon, endLat)))
                ).apply { addNumberProperty("speed", liveSpeed ?: last.speedKmh ?: -1.0) }
            )
        }
        // Смена значка положения (настройки или быстрый выбор) - заново регистрируем картинку слоя.
        LaunchedEffect(styleReady, positionIcon) {
            if (!styleReady) return@LaunchedEffect
            map?.style?.addImage(ME_ICON_IMAGE, renderPositionIcon(context, positionIcon, (positionIcon.mapHeightDp * density).toInt()))
        }
        // Маркер выбранной точки/остановки.
        LaunchedEffect(styleReady, selection) {
            if (!styleReady) return@LaunchedEffect
            val src = map?.style?.getSourceAs<GeoJsonSource>(TRAIN_SEL_SOURCE) ?: return@LaunchedEffect
            when (val sel = selection) {
                is TrainMapSelection.PathPoint -> src.setGeoJson(Point.fromLngLat(sel.point.longitude, sel.point.latitude))
                is TrainMapSelection.Stop -> src.setGeoJson(Point.fromLngLat(sel.mark.longitude, sel.mark.latitude))
                null -> src.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            }
        }
        // Тап: остановка (до 24 dp) → иначе ближайшая точка пройденного пути (до 30 dp) → иначе снять выбор.
        DisposableEffect(map) {
            val m = map
            val click = MapLibreMap.OnMapClickListener { latLng ->
                val t = track ?: return@OnMapClickListener false
                val tap = m!!.projection.toScreenLocation(latLng)
                val stopHit = t.stops.minByOrNull { st ->
                    val p = m.projection.toScreenLocation(LatLng(st.latitude, st.longitude))
                    Math.hypot((p.x - tap.x).toDouble(), (p.y - tap.y).toDouble())
                }?.takeIf { st ->
                    val p = m.projection.toScreenLocation(LatLng(st.latitude, st.longitude))
                    Math.hypot((p.x - tap.x).toDouble(), (p.y - tap.y).toDouble()) <= 24 * density
                }
                if (stopHit != null) {
                    selection = TrainMapSelection.Stop(stopHit)
                    return@OnMapClickListener true
                }
                val tolM = m.projection.getMetersPerPixelAtLatitude(latLng.latitude) * 30.0 * density
                val hit = TrackAnalysis.nearestPoint(t.points, latLng.latitude, latLng.longitude, tolM)
                selection = hit?.let { TrainMapSelection.PathPoint(it) }
                hit != null
            }
            m?.addOnMapClickListener(click)
            onDispose { m?.removeOnMapClickListener(click) }
        }
        // Пунктир, когда линия получена.
        LaunchedEffect(styleReady, railState) {
            if (!styleReady) return@LaunchedEffect
            val line = (railState as? ServicePhaseAutoFillViewModel.RailLineResult.Ok)?.line ?: return@LaunchedEffect
            map?.style?.getSourceAs<GeoJsonSource>(TRAIN_ROUTE_SOURCE)?.setGeoJson(
                Feature.fromGeometry(LineString.fromLngLats(line.map { (lat, lon) -> Point.fromLngLat(lon, lat) }))
            )
        }
        // Камера охватывает маршрут (линию или станции), пройденный путь и текущее место; повторно — когда пришла линия,
        // трек или первая точка.
        LaunchedEffect(styleReady, railState, myFix != null, track != null) {
            val hasTrack = track != null
            if (!styleReady || (!hasRoute && !hasTrack)) return@LaunchedEffect
            // В движении при включённом следовании камеру ведёт положение, а не охват маршрута.
            if (follow && (ride || liveSpeed?.let { it >= 2.0 } == true) && nowMs - liveAtMs < 15_000) return@LaunchedEffect
            val m = map ?: return@LaunchedEffect
            val line = (railState as? ServicePhaseAutoFillViewModel.RailLineResult.Ok)?.line
            val key = "${line?.size}|${myFix != null}|$hasTrack"
            if (fittedKey == key) return@LaunchedEffect
            fittedKey = key
            val pts = TrainMapRoute.cameraPoints(if (hasRoute) stops else emptyList(), line, myFix) +
                track?.drawPoints.orEmpty().map { it.latitude to it.longitude }
            if (pts.isEmpty()) return@LaunchedEffect
            val bounds = LatLngBounds.Builder().apply { pts.forEach { include(LatLng(it.first, it.second)) } }.build()
            val pad = (48 * density).toInt()
            // Сверху плашка, справа кнопки, снизу панели.
            if (bounds.latitudeSpan < 1e-5 && bounds.longitudeSpan < 1e-5) {
                m.animateCamera(CameraUpdateFactory.newLatLngZoom(bounds.center, 14.0))
            } else {
                m.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, pad, pad * 2, pad * 2, pad * 3))
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

        // Движение по маршруту: расстояние и время до ближайшей станции и до конечной (по рельсовой линии), обновляется с каждой точкой.
        val liveFreshTop = liveSpeed != null && nowMs - liveAtMs < 15_000
        val progress = remember(stops, railLineNow, myFix) {
            if (viewOnly || !hasRoute) null else TrainMapRoute.progress(stops, railLineNow, myFix)
        }
        val etaSpeed = remember(progress, liveSpeed, liveFreshTop, track) {
            TrainMapRoute.etaSpeedKmh(if (liveFreshTop) liveSpeed else null, track?.points.orEmpty(), System.currentTimeMillis())
        }
        // Вне путей: текущее положение дальше 50 м от рельсовой линии маршрута.
        val offNow = remember(myFix, railLineNow, railCum) {
            val f = myFix
            val line = railLineNow
            val cum = railCum
            if (viewOnly || f == null || line == null || cum == null) false
            else (TrainMapRoute.project(line, cum, f.latitude, f.longitude)?.offsetM ?: 0.0) > TrainMapRoute.SNAP_TOLERANCE_M
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Pst.bg)) {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView })

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
                        text = title,
                        style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        color = Pst.text,
                        maxLines = 1,
                    )
                    if (locationDenied) {
                        Text(
                            text = "Нет доступа к местоположению",
                            style = Pst.sans.copy(fontSize = 12.sp),
                            color = Pst.textMuted,
                            maxLines = 1,
                        )
                    }
                }
            }


            // Текущая скорость (живая точка записи, иначе последняя точка трека) и карточка выбранного (§6.9).
            val lastPoint = track?.points?.lastOrNull()
            val liveFresh = liveSpeed != null && nowMs - liveAtMs < 15_000
            val speedNow: Double? = if (viewOnly) null else if (liveFresh) liveSpeed else lastPoint?.speedKmh
            val zone = track?.zone ?: "GMT+3"
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 12.dp, end = 72.dp, top = 64.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (progress != null) {
                    val nowEta = System.currentTimeMillis()
                    val nextEta = TrainMapRoute.Eta.arrivalMs(nowEta, progress.nextDistanceM, etaSpeed)
                    val termEta = TrainMapRoute.Eta.arrivalMs(nowEta, progress.terminalDistanceM, etaSpeed)
                    EtaRibbon(
                        nextName = stops[progress.nextIndex].name,
                        nextDistanceM = progress.nextDistanceM,
                        nextEtaMs = nextEta,
                        terminalName = stops.last().name.takeIf { progress.nextIndex != stops.lastIndex },
                        terminalDistanceM = progress.terminalDistanceM,
                        terminalEtaMs = termEta,
                        zone = zone,
                    )
                }
                if (offNow || offSegments > 0) {
                    Text(
                        text = if (offNow) "Вне путей" else "Участки вне путей - пунктиром",
                        style = Pst.sans.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                        color = Pst.text,
                        maxLines = 1,
                        modifier = Modifier
                            .shadow(4.dp, RoundedCornerShape(12.dp))
                            .background(Pst.warning, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
                if (speedNow != null) {
                    Column(
                        modifier = Modifier
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .background(Pst.surface, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = speedNow.toInt().toString(),
                                style = Pst.mono.copy(fontSize = if (ride) 72.sp else 34.sp, fontWeight = FontWeight.Bold),
                                color = Pst.text,
                                maxLines = 1,
                            )
                            Text(
                                text = "км/ч",
                                style = Pst.sans.copy(fontSize = if (ride) 18.sp else 13.sp),
                                color = Pst.textMuted,
                                modifier = Modifier.padding(bottom = if (ride) 12.dp else 6.dp),
                                maxLines = 1,
                            )
                        }
                        if (!liveFresh && lastPoint != null) {
                            Text(
                                text = "последняя точка " + TrackAnalysis.clock(lastPoint.timeMs, zone, seconds = true),
                                style = Pst.sans.copy(fontSize = 11.sp),
                                color = Pst.textMuted,
                                maxLines = 1,
                            )
                        }
                    }
                }
                selection?.let { sel ->
                    Column(
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                            .background(Pst.surface, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        when (sel) {
                            is TrainMapSelection.PathPoint -> {
                                Text(
                                    text = TrackAnalysis.clock(sel.point.timeMs, zone, seconds = true),
                                    style = Pst.mono.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                                    color = Pst.text,
                                )
                                Text(
                                    text = (sel.point.speedKmh?.let { "${it.toInt()} км/ч" } ?: "скорость неизвестна"),
                                    style = Pst.mono.copy(fontSize = 14.sp),
                                    color = Pst.textMuted,
                                )
                            }
                            is TrainMapSelection.Stop -> {
                                val mk = sel.mark
                                Text(
                                    text = mk.name ?: "Остановка",
                                    style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                    color = Pst.text,
                                    maxLines = 2,
                                )
                                val parts = buildList {
                                    mk.arrivalMs?.let { add("Прибытие " + TrackAnalysis.clock(it, zone)) }
                                    mk.departureMs?.let { add("Отправление " + TrackAnalysis.clock(it, zone)) }
                                    val a = mk.arrivalMs
                                    val d = mk.departureMs
                                    if (a != null && d != null && d > a) add("стоянка ${TrackAnalysis.dwellMinutes(a, d)} м")
                                }
                                Text(
                                    text = parts.joinToString(" · "),
                                    style = Pst.mono.copy(fontSize = 13.sp, lineHeight = 18.sp),
                                    color = Pst.textMuted,
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp).offset(y = maxHeight * 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapButton(onClick = { zoomBy(+1.0) }, description = "Приблизить") {
                    Text("+", style = Pst.sans.copy(fontSize = 22.sp), color = Pst.text)
                }
                MapButton(onClick = { zoomBy(-1.0) }, description = "Отдалить") {
                    Text("−", style = Pst.sans.copy(fontSize = 22.sp), color = Pst.text)
                }
                if (!viewOnly || track?.points.orEmpty().isNotEmpty()) {
                    MapButton(onClick = { showChart = true }, description = "График скорости") {
                        PstIcon(PstIcons.SpeedChart, 22.dp, Pst.text)
                    }
                }
                if (!viewOnly) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .background(if (ride) Pst.accent else Pst.surface, RoundedCornerShape(16.dp))
                            .clickable(onClickLabel = "Еду по карте") {
                                val m = map
                                if (ride) {
                                    ride = false
                                    m?.easeCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder(m.cameraPosition).bearing(0.0).build()), 400)
                                } else {
                                    ride = true
                                    follow = true
                                    val f = myFix
                                    if (m != null && f != null) {
                                        val zoom = userZoom ?: maxOf(m.cameraPosition.zoom, 15.0)
                                        userZoom = zoom
                                        m.easeCamera(
                                            CameraUpdateFactory.newCameraPosition(
                                                CameraPosition.Builder().target(LatLng(f.latitude, f.longitude)).zoom(zoom)
                                                    .bearing(headingDeg ?: 0.0).tilt(0.0).build()
                                            ),
                                            600,
                                        )
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) { PstIcon(PstIcons.Navigate, 22.dp, if (ride) androidx.compose.ui.graphics.Color.White else Pst.text) }
                    MapButton(onClick = { showIconPicker = !showIconPicker }, description = "Значок положения") {
                        PositionIconTile(positionIcon, 36.dp)
                    }
                    MapButton(onClick = { follow = true; requestOrShowLocation(center = true) }, description = "Моё местоположение") {
                        Box(Modifier.size(14.dp).border(2.dp, Pst.accent, CircleShape), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(4.dp).background(Pst.accent, CircleShape))
                        }
                    }
                }
            }

            if (showIconPicker && !viewOnly) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 72.dp)
                        .offset(y = maxHeight * 0.4f)
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .background(Pst.surface, RoundedCornerShape(16.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Значок положения", style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
                    PositionIcon.entries.chunked(3).forEach { rowIcons ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowIcons.forEach { ic ->
                                Column(
                                    modifier = Modifier
                                        .width(78.dp)
                                        .border(if (ic == positionIcon) 2.dp else 0.dp, if (ic == positionIcon) Pst.accent else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(14.dp))
                                        .padding(4.dp)
                                        .clickable(onClickLabel = ic.label) {
                                            PositionIconStore.set(context, ic)
                                            showIconPicker = false
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    PositionIconTile(ic, 64.dp)
                                    Text(
                                        ic.label,
                                        style = Pst.sans.copy(fontSize = 10.sp, lineHeight = 12.sp),
                                        color = Pst.text,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 2,
                                        modifier = Modifier.padding(top = 3.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showChart) {
                SpeedChartScreen(
                    track = track,
                    selectedTimeMs = (selection as? TrainMapSelection.PathPoint)?.point?.timeMs,
                    onSelect = { p -> selection = p?.let { TrainMapSelection.PathPoint(it) } },
                    onOpenConsent = { showChart = false; onOpenConsentSettings() },
                    onDismiss = {
                        showChart = false
                        // Точка, выбранная на графике, подсвечивается на карте — камера переходит к ней (зум сохраняется).
                        (selection as? TrainMapSelection.PathPoint)?.point?.let { pt ->
                            follow = false
                            map?.animateCamera(CameraUpdateFactory.newLatLng(LatLng(pt.latitude, pt.longitude)))
                        }
                    },
                )
            }

            val name = missing
            val rail = railState
            if (hasRoute && (rail !is ServicePhaseAutoFillViewModel.RailLineResult.Ok ||
                    (rail.fromCache && name == null && savedName == null))
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .background(
                            if (rail is ServicePhaseAutoFillViewModel.RailLineResult.Ok) Pst.warning else Pst.surface,
                            RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (rail) {
                        null -> {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Pst.accent,
                            )
                            Text(text = "Строим путь по рельсам…", style = Pst.sans.copy(fontSize = 14.sp), color = Pst.text)
                        }
                        is ServicePhaseAutoFillViewModel.RailLineResult.Ok ->
                            Text(
                                text = "Маршрут показан из сохранённых данных",
                                style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                color = Pst.text,
                            )
                        else -> {
                            Text(
                                text = when (rail) {
                                    ServicePhaseAutoFillViewModel.RailLineResult.NoPath ->
                                        "Не удалось проложить путь по рельсам между станциями плеча. Проверьте координаты станций — " +
                                            "точка могла оказаться в стороне от железной дороги."
                                    is ServicePhaseAutoFillViewModel.RailLineResult.Failed ->
                                        "Путь по рельсам не построен. " + rail.failure.message() + "."
                                    else -> ""
                                }.replace("..", "."),
                                style = Pst.sans.copy(fontSize = 14.sp, lineHeight = 20.sp),
                                color = Pst.text,
                                modifier = Modifier.weight(1f),
                            )
                            if (rail is ServicePhaseAutoFillViewModel.RailLineResult.Failed) {
                                PstButton(
                                    text = "Повторить",
                                    onClick = { loadAttempt++ },
                                    kind = PstButtonKind.TEXT,
                                    height = 36.dp,
                                    fontSize = 14,
                                )
                            }
                        }
                    }
                }
            }
            if (name != null || savedName != null) {
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
                    savedName?.let {
                        Text(
                            text = "Координаты станции «$it» сохранены",
                            style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 18.sp),
                            color = Pst.textMuted,
                        )
                    }
                    if (name != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(36.dp).background(Pst.accentSoft, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) { PstIcon(PstIcons.MapPin, 18.dp, Pst.accent) }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "У станции «$name» нет координат",
                                    style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                    color = Pst.text,
                                )
                                Text(
                                    text = if (determineFailed) {
                                        "Не удалось определить местоположение. Проверьте GPS и доступ к местоположению."
                                    } else {
                                        "Определим по вашему текущему месту и сохраним в справочник станций."
                                    },
                                    style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp),
                                    color = if (determineFailed) Pst.danger else Pst.textMuted,
                                )
                            }
                        }
                        PstButton(
                            text = if (determining) "Определяем…" else "Определить координаты по GPS",
                            kind = PstButtonKind.CTA,
                            enabled = !determining,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                determining = true
                                determineFailed = false
                                scope.launch {
                                    val ok = onDetermine(name)
                                    determining = false
                                    if (ok) {
                                        savedName = name
                                        missing = missingStation()
                                    } else {
                                        determineFailed = true
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Лента «до ближайшей станции / до конечной»: расстояние по рельсовой линии и ожидаемое время прибытия (SCREEN_SPECS §6.9). */
@Composable
private fun EtaRibbon(
    nextName: String,
    nextDistanceM: Double,
    nextEtaMs: Long?,
    terminalName: String?,
    terminalDistanceM: Double,
    terminalEtaMs: Long?,
    zone: String,
) {
    Column(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(Pst.surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        EtaLine(if (terminalName == null) "Конечная" else "Следующая", nextName, nextDistanceM, nextEtaMs, zone, strong = true)
        if (terminalName != null) EtaLine("Конечная", terminalName, terminalDistanceM, terminalEtaMs, zone, strong = false)
    }
}

@Composable
private fun EtaLine(label: String, name: String, distanceM: Double, etaMs: Long?, zone: String, strong: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label $name",
            style = Pst.sans.copy(fontSize = if (strong) 13.sp else 12.sp, fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal),
            color = if (strong) Pst.text else Pst.textMuted,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            text = TrainMapEta.distance(distanceM) + " · " + (etaMs?.let { TrackAnalysis.clock(it, zone) } ?: "-"),
            style = Pst.mono.copy(fontSize = if (strong) 13.sp else 12.sp),
            color = if (strong) Pst.text else Pst.textMuted,
            maxLines = 1,
        )
    }
}

/** Форматирование расстояния ленты: метры до километра, далее километры с запятой. */
internal object TrainMapEta {
    fun distance(m: Double): String =
        if (m < 1000.0) "${(Math.round(m / 10.0) * 10)} м" else String.format(java.util.Locale.US, "%.1f", m / 1000.0).replace('.', ',') + " км"
}
