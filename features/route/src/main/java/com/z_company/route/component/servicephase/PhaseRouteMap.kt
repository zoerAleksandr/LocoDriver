package com.z_company.route.component.servicephase

import com.z_company.route.viewmodel.ServicePhaseAutoFillViewModel
import com.z_company.route.viewmodel.message
import com.z_company.domain.util.PointStation
import com.z_company.domain.util.PointStationResult
import com.z_company.domain.util.RenderedStation
import com.z_company.route.component.AppAlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberUpdatedState

import org.maplibre.android.maps.MapLibreMap
import kotlinx.coroutines.launch
import com.z_company.route.util.DeviceLocation
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.Manifest
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.z_company.domain.entities.setting.ServicePhaseStationKind
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/** Станция плеча для карты. [kind] null — крайняя станция (отправление/прибытие). */
internal data class RouteMapStop(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val kind: ServicePhaseStationKind? = null,
)

private const val STOPS_SOURCE = "phase-route-stops"
private const val ME_SOURCE = "phase-route-me"
private const val RAIL_SOURCE = "phase-route-rail"
private const val RAIL_LAYER = "phase-route-rail-layer"
private const val PICK_SOURCE = "phase-route-pick"
private const val PICK_LAYER = "phase-route-pick-layer"

/** Нажатая точка, по которой идёт или ждёт подтверждения поиск станции. */
private class PendingPick(val isStart: Boolean, val latitude: Double, val longitude: Double, val confirm: PickConfirm)

/**
 * Просмотр плеча на карте (SCREEN_SPECS §6.6): станции с координатами по порядку —
 * крайние (крупные точки), станции и о.п. (мелкие) — с подписями и линией пути; цвета подстраиваются под тему карты.
 * Линия — по рельсам ([loadRailLine], сервер); пока строится — индикатор, ошибка — понятное сообщение
 * (напрямик между станциями линию не рисуем: поезд по земле не ездит).
 * Камера охватывает всё плечо. Только просмотр; «назад» — закрыть.
 * Запросов к отрисовке (queryRenderedFeatures) нет — см. PhaseMapPicker про нативное падение.
 */
@Composable
internal fun PhaseRouteMap(
    title: String,
    stops: List<RouteMapStop>,
    missing: Int,
    loadRailLine: suspend (List<Pair<Double, Double>>) -> ServicePhaseAutoFillViewModel.RailLineResult,
    onDismiss: () -> Unit,
    /** Названия крайних станций в полях (пусто - не указана) и есть ли у них координаты. */
    departureName: String = "",
    arrivalName: String = "",
    departureKnown: Boolean = false,
    arrivalKnown: Boolean = false,
    /** Последняя станция справочника с координатами - регион камеры, когда станций плеча нет. */
    lastStation: Pair<Double, Double>? = null,
    /** Поиск ближайшей станции по нажатой точке; null - выбор станций на карте недоступен. */
    findStation: (suspend (Double, Double, RenderedStation?) -> PointStationResult)? = null,
    onApplyStation: (isStart: Boolean, station: PointStation) -> Unit = { _, _ -> },
    /** Станций рядом нет, в поле есть название: поставить его на нажатую точку. */
    onApplyTypedName: (isStart: Boolean, latitude: Double, longitude: Double) -> Unit = { _, _, _ -> },
    /** «Измерить по карте» (когда обе станции с координатами); null - кнопки нет. */
    onMeasure: (() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val context = LocalContext.current
        val darkMap = rememberMapThemeIsDark()
        val morePathsAtLowZoom = rememberMorePathsAtLowZoom()
        remember { MapLibre.getInstance(context); true }
        val density = context.resources.displayMetrics.density
        var map by remember { mutableStateOf<MapLibreMap?>(null) }
        // Карта жива (до onDestroy): запросы к отрисовке после закрытия роняют приложение нативно (см. PhaseMapPicker).
        val alive = remember { java.util.concurrent.atomic.AtomicBoolean(true) }
        var locationDenied by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        fun showMyLocation() {
            scope.launch {
                val fix = DeviceLocation.currentFix(context) ?: return@launch
                val m = map ?: return@launch
                m.style?.getSourceAs<GeoJsonSource>(ME_SOURCE)
                    ?.setGeoJson(Point.fromLngLat(fix.longitude, fix.latitude))
                m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(fix.latitude, fix.longitude), 13.0))
            }
        }
        val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.any { it }) showMyLocation() else locationDenied = true
        }
        // Путь по рельсам: грузим при открытии и при изменении станций (после выбора на карте), рисуем, когда карта готова.
        var railState by remember { mutableStateOf<ServicePhaseAutoFillViewModel.RailLineResult?>(null) }
        var loadAttempt by remember { mutableStateOf(0) }
        LaunchedEffect(loadAttempt, stops) {
            railState = null
            if (stops.size >= 2) {
                railState = loadRailLine(stops.map { it.latitude to it.longitude })
            }
        }
        var styleReady by remember { mutableStateOf(false) }
        LaunchedEffect(railState, styleReady) {
            if (!styleReady) return@LaunchedEffect
            val style = map?.style ?: return@LaunchedEffect
            style.getLayer(RAIL_LAYER)?.let { style.removeLayer(it) }
            style.getSource(RAIL_SOURCE)?.let { style.removeSource(it) }
            val line = (railState as? ServicePhaseAutoFillViewModel.RailLineResult.Ok)?.line ?: return@LaunchedEffect
            style.addSource(
                GeoJsonSource(
                    RAIL_SOURCE,
                    Feature.fromGeometry(LineString.fromLngLats(line.map { (lat, lon) -> Point.fromLngLat(lon, lat) })),
                )
            )
            style.addLayerBelow(
                LineLayer(RAIL_LAYER, RAIL_SOURCE).withProperties(
                    PropertyFactory.lineColor(if (darkMap) "#59B5D8" else "#00A0F5"),
                    PropertyFactory.lineWidth(4f),
                    PropertyFactory.lineOpacity(0.85f),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                ),
                "phase-route-stops-layer",
            )
        }

        // Станции на карте обновляются после выбора на ней; камера охватывает новые станции.
        val shownStops = remember { mutableStateOf<List<RouteMapStop>?>(null) }
        LaunchedEffect(stops, styleReady) {
            if (!styleReady) return@LaunchedEffect
            val m = map ?: return@LaunchedEffect
            val style = m.style ?: return@LaunchedEffect
            style.getSourceAs<GeoJsonSource>(STOPS_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(stopFeatures(stops)))
            if (shownStops.value != null && shownStops.value != stops) {
                val pad = (48 * density).toInt()
                fitCameraTo(m, stops.map { it.latitude to it.longitude }, pad, pad * 2, pad * 2, pad * 3, animate = true)
            }
            shownStops.value = stops
        }

        // Станций с координатами нет - камера на местоположение (если разрешено), иначе на регион последней станции / Россию.
        LaunchedEffect(map) {
            val m = map ?: return@LaunchedEffect
            if (stops.isNotEmpty()) return@LaunchedEffect
            val fix = DeviceLocation.currentFix(context) ?: return@LaunchedEffect
            if (stops.isEmpty() && alive.get()) {
                val go = PhaseMapPick.startWithoutStops(fix.latitude to fix.longitude, lastStation)
                m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(go.latitude, go.longitude), go.zoom))
            }
        }

        // Выбор станции на карте: кнопка -> режим (нажать точку) -> поиск ближайшей -> диалог подтверждения.
        var pickFor by remember { mutableStateOf<Boolean?>(null) }
        var searching by remember { mutableStateOf(false) }
        var pending by remember { mutableStateOf<PendingPick?>(null) }
        fun setPickPoint(lat: Double?, lon: Double?) {
            val src = map?.style?.getSourceAs<GeoJsonSource>(PICK_SOURCE) ?: return
            if (lat == null || lon == null) src.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            else src.setGeoJson(Point.fromLngLat(lon, lat))
        }
        fun nameFor(isStart: Boolean) = if (isStart) departureName else arrivalName
        val onTap by rememberUpdatedState<(LatLng) -> Unit> { latLng ->
            val isStart = PhaseMapPick.target(pickFor, departureName, arrivalName)
            val find = findStation
            val m = map
            if (isStart != null && find != null && m != null && !searching && pending == null) {
                // Станция OSM под пальцем (слой подложки) - запасной источник, если сервер не ответил.
                val rendered = stationAt(m, m.projection.toScreenLocation(latLng), 24 * density)
                    ?.let { (p, name) -> RenderedStation(name, p.latitude, p.longitude) }
                searching = true
                setPickPoint(latLng.latitude, latLng.longitude)
                scope.launch {
                    val result = try {
                        find(latLng.latitude, latLng.longitude, rendered)
                    } finally {
                        searching = false
                    }
                    pending = PendingPick(isStart, latLng.latitude, latLng.longitude, PhaseMapPick.confirm(result, nameFor(isStart)))
                }
            }
        }
        val pickActive by rememberUpdatedState(PhaseMapPick.target(pickFor, departureName, arrivalName) != null)
        val mapBackground = PstMapColors.forMap(darkMap).bg.toArgb()
        val mapView = remember {
            newMapView(context, mapBackground).also { mv ->
                mv.getMapAsync { m ->
                    map = m
                    m.uiSettings.isRotateGesturesEnabled = false
                    m.uiSettings.isTiltGesturesEnabled = false
                    m.uiSettings.isLogoEnabled = false
                    m.uiSettings.isAttributionEnabled = true
                    val start = PhaseMapPick.startWithoutStops(null, lastStation)
                    m.cameraPosition = CameraPosition.Builder()
                        .target(stops.firstOrNull()?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(start.latitude, start.longitude))
                        .zoom(if (stops.isEmpty()) start.zoom else 11.0)
                        .build()
                    m.addOnMapClickListener { latLng ->
                        if (!alive.get() || !pickActive || m.style?.isFullyLoaded != true) return@addOnMapClickListener false
                        onTap(latLng)
                        true
                    }
                    loadMapStyle(mv, m, darkMap, morePathsAtLowZoom) { style ->
                        addRailwayLayers(style, darkMap)
                        // Моё местоположение — синяя точка (заполняется по кнопке).
                        style.addSource(GeoJsonSource(ME_SOURCE, FeatureCollection.fromFeatures(emptyList())))
                        style.addLayer(
                            CircleLayer("phase-route-me-layer", ME_SOURCE).withProperties(
                                PropertyFactory.circleRadius(7f),
                                PropertyFactory.circleColor(if (darkMap) "#58B8DF" else "#00A0F5"),
                                PropertyFactory.circleStrokeColor(if (darkMap) "#D6E3E8" else "#FFFFFF"),
                                PropertyFactory.circleStrokeWidth(3f),
                            )
                        )
                        // Нажатая точка выбора станции.
                        style.addSource(GeoJsonSource(PICK_SOURCE, FeatureCollection.fromFeatures(emptyList())))
                        style.addLayer(
                            CircleLayer(PICK_LAYER, PICK_SOURCE).withProperties(
                                PropertyFactory.circleRadius(9f),
                                PropertyFactory.circleColor(if (darkMap) "#C98D5B" else "#E8590C"),
                                PropertyFactory.circleStrokeColor(if (darkMap) "#303A40" else "#FFFFFF"),
                                PropertyFactory.circleStrokeWidth(3f),
                            )
                        )
                        val features = stopFeatures(stops)
                        style.addSource(GeoJsonSource(STOPS_SOURCE, FeatureCollection.fromFeatures(features)))
                        val kind = Expression.get("kind")
                        style.addLayer(
                            CircleLayer("phase-route-stops-layer", STOPS_SOURCE).withProperties(
                                PropertyFactory.circleRadius(
                                    Expression.match(kind, Expression.literal(5f),
                                        Expression.stop("terminal", 8f), Expression.stop("halt", 4f))
                                ),
                                PropertyFactory.circleColor(
                                    Expression.match(kind, Expression.literal(if (darkMap) "#B9C5CB" else "#FFFFFF"),
                                        Expression.stop("terminal", if (darkMap) "#D5BC86" else "#0A0E14"))
                                ),
                                PropertyFactory.circleStrokeColor(if (darkMap) "#344047" else "#0A0E14"),
                                PropertyFactory.circleStrokeWidth(
                                    Expression.match(kind, Expression.literal(2f), Expression.stop("halt", 1.5f))
                                ),
                            )
                        )
                        style.addLayer(
                            SymbolLayer("phase-route-stops-label", STOPS_SOURCE).withProperties(
                                PropertyFactory.textField(Expression.get("name")),
                                PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
                                PropertyFactory.textSize(
                                    Expression.match(kind, Expression.literal(12f), Expression.stop("terminal", 14f))
                                ),
                                PropertyFactory.textOffset(arrayOf(0f, 1.2f)),
                                PropertyFactory.textAnchor("top"),
                                PropertyFactory.textColor(if (darkMap) "#E0E5E8" else "#263238"),
                                PropertyFactory.textHaloColor(if (darkMap) "#252A2E" else "#FFFFFF"),
                                PropertyFactory.textHaloWidth(1.5f),
                                PropertyFactory.textOptional(true),
                            )
                        )
                        val pad = (48 * density).toInt()
                        // Справа — кнопки «+ − ◎», поэтому отступ шире.
                        fitCameraTo(m, stops.map { it.latitude to it.longitude }, pad, pad * 2, pad * 2, pad * 3, animate = false)
                        shownStops.value = stops
                        styleReady = true
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

        CompositionLocalProvider(LocalPstMapColors provides PstMapColors.forMap(darkMap)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Pst.bg)) {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView })
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .background(Pst.surface, RoundedCornerShape(16.dp))
                        .clickable(onClickLabel = "Назад", onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { PstIcon(PstIcons.ChevronLeft, 22.dp, Pst.text) }
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
                        overflow = TextOverflow.Ellipsis,
                    )
                    val total = stops.size + missing
                    Text(
                        text = when {
                            locationDenied -> "Нет доступа к местоположению"
                            stops.isEmpty() -> "Станции на карте не указаны"
                            missing == 0 -> "$total " + pstPlural(total, "станция", "станции", "станций")
                            else -> "Показано ${stops.size} из $total · у $missing нет координат"
                        },
                        style = Pst.sans.copy(fontSize = 12.sp),
                        color = if (missing > 0 || locationDenied) Pst.warning else Pst.textMuted,
                    )
                }
            }
            // «+», «−», «Моё местоположение» — как на карте выбора точки.
            Column(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
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
                        if (hasLocationPermission(context)) showMyLocation()
                        else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    },
                    description = "Моё местоположение",
                ) {
                    Box(Modifier.size(14.dp).border(2.dp, Pst.accent, CircleShape), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(4.dp).background(Pst.accent, CircleShape))
                    }
                }
            }
            val rail = railState
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (rail is ServicePhaseAutoFillViewModel.RailLineResult.Ok && rail.fromCache) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .background(Pst.warning, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = "Маршрут показан из сохранённых данных",
                            style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            color = Pst.text,
                        )
                    }
                }
                if (stops.size >= 2 && rail !is ServicePhaseAutoFillViewModel.RailLineResult.Ok) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .background(Pst.surface, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (rail == null) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Pst.accent,
                            )
                            Text(
                                text = "Строим путь по рельсам…",
                                style = Pst.sans.copy(fontSize = 14.sp),
                                color = Pst.text,
                            )
                        } else {
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
                            // Путь не найден: карта открыта из редактора плеча - «Уточнить» возвращает к нему.
                            if (rail == ServicePhaseAutoFillViewModel.RailLineResult.NoPath) {
                                PstButton(
                                    text = "Уточнить",
                                    onClick = onDismiss,
                                    kind = PstButtonKind.TEXT,
                                    height = 36.dp,
                                    fontSize = 14,
                                )
                            }
                        }
                    }
                }
                if (findStation != null) {
                    val picking = pickFor
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .background(Pst.surface, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (picking != null) {
                            // Режим выбора: нажать точку на карте.
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (searching) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Pst.accent,
                                    )
                                }
                                Text(
                                    text = if (searching) "Ищем ближайшую станцию…" else PhaseMapPick.prompt(PhaseMapPick.target(picking, departureName, arrivalName) ?: picking),
                                    style = Pst.sans.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
                                    color = Pst.text,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            PstButton(
                                text = "Отмена",
                                onClick = { pickFor = null; searching = false; setPickPoint(null, null) },
                                kind = PstButtonKind.TONAL,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            if (stops.isEmpty()) {
                                Text(
                                    text = if (departureName.isBlank() && arrivalName.isBlank()) {
                                        "Станции не выбраны. Укажите их на карте: первое нажатие на карту определяет станцию отправления, второе — станцию прибытия."
                                    } else {
                                        "У станций плеча пока нет координат. Нажмите кнопку и укажите место станции на карте."
                                    },
                                    style = Pst.sans.copy(fontSize = 14.sp, lineHeight = 20.sp),
                                    color = Pst.textMuted,
                                )
                            }
                            PhaseMapPick.buttons(departureName, arrivalName).forEach { b ->
                                PstButton(
                                    text = b.label,
                                    onClick = { pickFor = b.isStart },
                                    enabled = b.enabled,
                                    kind = PstButtonKind.TONAL,
                                    icon = PstIcons.MapPin,
                                    iconSize = 18.dp,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            if (onMeasure != null && PhaseMapPick.canMeasure(departureName, arrivalName, departureKnown, arrivalKnown)) {
                                PstButton(
                                    text = "Измерить по карте",
                                    onClick = onMeasure,
                                    kind = PstButtonKind.CTA,
                                    icon = PstIcons.Measure,
                                    iconSize = 18.dp,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                } else if (stops.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Pst.surface, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        Text(
                            text = "У станций плеча пока нет координат. Найдите станции автоматически или укажите точки на карте.",
                            style = Pst.sans.copy(fontSize = 14.sp, lineHeight = 20.sp),
                            color = Pst.textMuted,
                        )
                    }
                }
            }
            pending?.let { p ->
                val c = p.confirm
                fun finish(keepPicking: Boolean) {
                    pending = null
                    setPickPoint(null, null)
                    if (!keepPicking) pickFor = null
                }
                AppAlertDialog(
                    onDismissRequest = { finish(keepPicking = false) },
                    title = c.title,
                    confirmText = c.confirmText,
                    onConfirm = {
                        when (c) {
                            is PickConfirm.Found -> onApplyStation(p.isStart, c.station)
                            is PickConfirm.UseTypedName -> onApplyTypedName(p.isStart, p.latitude, p.longitude)
                            is PickConfirm.NothingNearby -> Unit
                        }
                        // «Выбрать другую» (станций рядом нет) остаётся в режиме выбора; «Применить» - выходит из него.
                        finish(keepPicking = c is PickConfirm.NothingNearby)
                    },
                    dismissText = c.dismissText,
                    onDismiss = { finish(keepPicking = false) },
                    content = {
                        if (c.text.isNotEmpty()) Text(text = c.text, fontSize = 14.sp, lineHeight = 20.sp)
                        if (c is PickConfirm.Found) {
                            TextButton(onClick = { finish(keepPicking = true) }) {
                                Text(text = "Выбрать другую", fontSize = 14.sp, fontWeight = FontWeight.W600, color = Pst.accent)
                            }
                        }
                    },
                )
            }
        }
        }
    }
}

private fun stopFeatures(stops: List<RouteMapStop>): List<Feature> = stops.map { st ->
    Feature.fromGeometry(Point.fromLngLat(st.longitude, st.latitude)).apply {
        addStringProperty("name", st.name)
        addStringProperty(
            "kind",
            when (st.kind) {
                null -> "terminal"
                ServicePhaseStationKind.HALT -> "halt"
                ServicePhaseStationKind.STATION -> "station"
            },
        )
    }
}
