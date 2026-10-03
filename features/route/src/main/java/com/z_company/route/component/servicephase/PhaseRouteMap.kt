package com.z_company.route.component.servicephase

import com.z_company.route.viewmodel.ServicePhaseAutoFillViewModel
import com.z_company.route.viewmodel.message

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import org.maplibre.android.geometry.LatLngBounds
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

/**
 * Просмотр плеча на карте (SCREEN_SPECS §6.6): станции с координатами по порядку —
 * крайние (крупные тёмные точки), станции, о.п. (мелкие) — с подписями и линией пути.
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
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val context = LocalContext.current
        remember { MapLibre.getInstance(context); true }
        val density = context.resources.displayMetrics.density
        var map by remember { mutableStateOf<MapLibreMap?>(null) }
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
        // Путь по рельсам: грузим один раз и подменяем пунктир сплошной линией, когда карта готова.
        // Путь по рельсам: грузим один раз и рисуем, когда карта готова.
        var railState by remember { mutableStateOf<ServicePhaseAutoFillViewModel.RailLineResult?>(null) }
        var loadAttempt by remember { mutableStateOf(0) }
        LaunchedEffect(loadAttempt) {
            if (stops.size >= 2) {
                railState = null
                railState = loadRailLine(stops.map { it.latitude to it.longitude })
            }
        }
        var styleReady by remember { mutableStateOf(false) }
        LaunchedEffect(railState, styleReady) {
            val line = (railState as? ServicePhaseAutoFillViewModel.RailLineResult.Ok)?.line ?: return@LaunchedEffect
            if (!styleReady) return@LaunchedEffect
            val style = map?.style ?: return@LaunchedEffect
            if (style.getLayer(RAIL_LAYER) != null) return@LaunchedEffect
            style.addSource(
                GeoJsonSource(
                    RAIL_SOURCE,
                    Feature.fromGeometry(LineString.fromLngLats(line.map { (lat, lon) -> Point.fromLngLat(lon, lat) })),
                )
            )
            style.addLayerBelow(
                LineLayer(RAIL_LAYER, RAIL_SOURCE).withProperties(
                    PropertyFactory.lineColor("#00A0F5"),
                    PropertyFactory.lineWidth(4f),
                    PropertyFactory.lineOpacity(0.85f),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                ),
                "phase-route-stops-layer",
            )
        }
        val mapView = remember {
            MapView(context).also { mv ->
                mv.getMapAsync { m ->
                    map = m
                    m.uiSettings.isRotateGesturesEnabled = false
                    m.uiSettings.isTiltGesturesEnabled = false
                    m.uiSettings.isLogoEnabled = false
                    m.uiSettings.isAttributionEnabled = true
                    m.cameraPosition = CameraPosition.Builder()
                        .target(stops.firstOrNull()?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(55.75, 37.62))
                        .zoom(if (stops.isEmpty()) 5.0 else 11.0)
                        .build()
                    loadMapStyle(mv, m) { style ->
                        addRailwayLayers(style)
                        // Моё местоположение — синяя точка (заполняется по кнопке).
                        style.addSource(GeoJsonSource(ME_SOURCE, FeatureCollection.fromFeatures(emptyList())))
                        style.addLayer(
                            CircleLayer("phase-route-me-layer", ME_SOURCE).withProperties(
                                PropertyFactory.circleRadius(7f),
                                PropertyFactory.circleColor("#00A0F5"),
                                PropertyFactory.circleStrokeColor("#FFFFFF"),
                                PropertyFactory.circleStrokeWidth(3f),
                            )
                        )
                        val features = stops.map { st ->
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
                        style.addSource(GeoJsonSource(STOPS_SOURCE, FeatureCollection.fromFeatures(features)))
                        val kind = Expression.get("kind")
                        style.addLayer(
                            CircleLayer("phase-route-stops-layer", STOPS_SOURCE).withProperties(
                                PropertyFactory.circleRadius(
                                    Expression.match(kind, Expression.literal(5f),
                                        Expression.stop("terminal", 8f), Expression.stop("halt", 4f))
                                ),
                                PropertyFactory.circleColor(
                                    Expression.match(kind, Expression.literal("#FFFFFF"),
                                        Expression.stop("terminal", "#0A0E14"))
                                ),
                                PropertyFactory.circleStrokeColor("#0A0E14"),
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
                                PropertyFactory.textColor("#0A0E14"),
                                PropertyFactory.textHaloColor("#FFFFFF"),
                                PropertyFactory.textHaloWidth(1.5f),
                                PropertyFactory.textOptional(true),
                            )
                        )
                        if (stops.size >= 2) {
                            val bounds = LatLngBounds.Builder().apply {
                                stops.forEach { include(LatLng(it.latitude, it.longitude)) }
                            }.build()
                            val pad = (48 * density).toInt()
                            // Справа — кнопки «+ − ◎», поэтому отступ шире.
                            m.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, pad, pad * 2, pad * 2, pad * 3))
                        } else if (stops.size == 1) {
                            m.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(stops[0].latitude, stops[0].longitude), 13.0))
                        }
                        styleReady = true
                    }
                }
            }
        }
        DisposableEffect(Unit) {
            mapView.onStart()
            mapView.onResume()
            onDispose {
                map = null
                mapView.onPause()
                mapView.onStop()
                mapView.onDestroy()
            }
        }

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
            if (stops.size >= 2 && rail !is ServicePhaseAutoFillViewModel.RailLineResult.Ok) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
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
                    }
                }
            }
            if (stops.isEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
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
    }
}
