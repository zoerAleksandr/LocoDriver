package com.z_company.route.component.servicephase

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
private const val LINE_SOURCE = "phase-route-line"

/**
 * Просмотр плеча на карте (SCREEN_SPECS §6.6): станции с координатами по порядку —
 * крайние (крупные тёмные точки), станции, о.п. (мелкие) — с подписями и линией между ними.
 * Камера охватывает всё плечо. Только просмотр; «назад» — закрыть.
 * Запросов к отрисовке (queryRenderedFeatures) нет — см. PhaseMapPicker про нативное падение.
 */
@Composable
internal fun PhaseRouteMap(
    title: String,
    stops: List<RouteMapStop>,
    missing: Int,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val context = LocalContext.current
        remember { MapLibre.getInstance(context); true }
        val density = context.resources.displayMetrics.density
        val mapView = remember {
            MapView(context).also { mv ->
                mv.getMapAsync { m ->
                    m.uiSettings.isRotateGesturesEnabled = false
                    m.uiSettings.isTiltGesturesEnabled = false
                    m.uiSettings.isLogoEnabled = false
                    m.uiSettings.isAttributionEnabled = true
                    m.cameraPosition = CameraPosition.Builder()
                        .target(stops.firstOrNull()?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(55.75, 37.62))
                        .zoom(if (stops.isEmpty()) 5.0 else 11.0)
                        .build()
                    m.setStyle(STYLE_URL) { style ->
                        addRailwayLayers(style)
                        val points = stops.map { Point.fromLngLat(it.longitude, it.latitude) }
                        if (points.size >= 2) {
                            style.addSource(GeoJsonSource(LINE_SOURCE, Feature.fromGeometry(LineString.fromLngLats(points))))
                            style.addLayer(
                                LineLayer("phase-route-line-layer", LINE_SOURCE).withProperties(
                                    PropertyFactory.lineColor("#00A0F5"),
                                    PropertyFactory.lineWidth(3f),
                                    PropertyFactory.lineOpacity(0.7f),
                                    PropertyFactory.lineDasharray(arrayOf(2f, 1.5f)),
                                )
                            )
                        }
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
                            m.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, pad, pad * 2, pad, pad * 3))
                        } else if (stops.size == 1) {
                            m.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(stops[0].latitude, stops[0].longitude), 13.0))
                        }
                    }
                }
            }
        }
        DisposableEffect(Unit) {
            mapView.onStart()
            mapView.onResume()
            onDispose {
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
                    Text(
                        text = "На карте ${stops.size} " + pstPlural(stops.size, "станция", "станции", "станций") +
                            if (missing > 0) " · $missing без координат" else "",
                        style = Pst.sans.copy(fontSize = 12.sp),
                        color = if (missing > 0) Pst.warning else Pst.textMuted,
                    )
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
