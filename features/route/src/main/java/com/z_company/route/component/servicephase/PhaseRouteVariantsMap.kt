package com.z_company.route.component.servicephase

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.z_company.route.viewmodel.RouteVariant
import com.z_company.route.viewmodel.addableStations
import com.z_company.route.viewmodel.variantViaLabels
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/** Цвета вариантов: кратчайший — синий, далее оранжевый, фиолетовый (SCREEN_SPECS §6.6). */
private val VARIANT_COLORS = listOf("#2F6BFF", "#FF8A00", "#8E4FE0")
private const val ENDS_SOURCE = "variants-ends"
private const val ENDS_LAYER = "variants-ends-layer"
private const val VARIANT_LINE_WIDTH = 6f
private fun sourceId(i: Int) = "variants-src-$i"
private fun layerId(i: Int) = "variants-layer-$i"
private fun variantColor(i: Int) = VARIANT_COLORS[i % VARIANT_COLORS.size]
private fun variantComposeColor(i: Int) = Color(android.graphics.Color.parseColor(variantColor(i)))

/**
 * Экран выбора варианта маршрута (SCREEN_SPECS §6.6, «Варианты маршрута плеча»): все варианты разными цветами,
 * линии одинаковой толщины; выбранный — непрозрачный и поверх, остальные полупрозрачные; внизу чипы «{N} км · через {станция}»
 * (тап по чипу или по линии на карте выбирает вариант, по умолчанию — кратчайший). «Выбрать маршрут» → [onSelect].
 * Запросов к отрисовке нет: линия под пальцем ищется по экранным координатам (см. PhaseMapPicker про падение).
 */
@Composable
internal fun PhaseRouteVariantsMap(
    from: String,
    to: String,
    variants: List<RouteVariant>,
    onSelect: (RouteVariant) -> Unit,
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
        var selected by remember { mutableStateOf(0) }
        var styleReady by remember { mutableStateOf(false) }
        val alive = remember { AtomicBoolean(true) }
        val labels = remember(variants) { variantViaLabels(variants) }

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
                        val ends = listOfNotNull(variants.firstOrNull()?.line?.first(), variants.firstOrNull()?.line?.last())
                        style.addSource(
                            GeoJsonSource(
                                ENDS_SOURCE,
                                FeatureCollection.fromFeatures(ends.map { (lat, lon) -> Feature.fromGeometry(Point.fromLngLat(lon, lat)) }),
                            )
                        )
                        style.addLayer(
                            CircleLayer(ENDS_LAYER, ENDS_SOURCE).withProperties(
                                PropertyFactory.circleRadius(8f),
                                PropertyFactory.circleColor("#0A0E14"),
                                PropertyFactory.circleStrokeColor("#FFFFFF"),
                                PropertyFactory.circleStrokeWidth(3f),
                            )
                        )
                        variants.forEachIndexed { i, v ->
                            style.addSource(
                                GeoJsonSource(
                                    sourceId(i),
                                    Feature.fromGeometry(LineString.fromLngLats(v.line.map { (lat, lon) -> Point.fromLngLat(lon, lat) })),
                                )
                            )
                        }
                        val all = variants.flatMap { it.line }
                        if (all.size >= 2) {
                            val bounds = LatLngBounds.Builder().apply { all.forEach { include(LatLng(it.first, it.second)) } }.build()
                            val pad = (40 * density).toInt()
                            m.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, pad, pad * 2, pad, pad * 4))
                        }
                        styleReady = true
                    }
                    // Тап по линии: ближайшая к пальцу линия в пределах ~28 dp (по экранным координатам).
                    m.addOnMapClickListener { latLng ->
                        if (!alive.get() || m.style?.isFullyLoaded != true) return@addOnMapClickListener false
                        val tap = m.projection.toScreenLocation(latLng)
                        var best = -1
                        var bestDist = 28f * density
                        variants.forEachIndexed { i, v ->
                            val d = v.line.minOf { (lat, lon) ->
                                val p = m.projection.toScreenLocation(LatLng(lat, lon))
                                kotlin.math.hypot(p.x - tap.x, p.y - tap.y)
                            }
                            if (d < bestDist) {
                                bestDist = d
                                best = i
                            }
                        }
                        if (best >= 0) selected = best
                        best >= 0
                    }
                }
            }
        }
        // Слои заново при каждой смене выбора: выбранный добавляется последним — поверх остальных.
        LaunchedEffect(selected, styleReady) {
            if (!styleReady) return@LaunchedEffect
            val style = map?.style ?: return@LaunchedEffect
            val order = variants.indices.filter { it != selected } + selected
            variants.indices.forEach { i -> style.getLayer(layerId(i))?.let { style.removeLayer(it) } }
            order.forEach { i ->
                val isSel = i == selected
                style.addLayerBelow(
                    LineLayer(layerId(i), sourceId(i)).withProperties(
                        PropertyFactory.lineColor(variantColor(i)),
                        PropertyFactory.lineWidth(VARIANT_LINE_WIDTH),
                        PropertyFactory.lineOpacity(if (isSel) 1f else 0.55f),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    ),
                    ENDS_LAYER,
                )
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

        Box(modifier = Modifier.fillMaxSize().background(Pst.bg)) {
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
                        text = "$from → $to",
                        style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        color = Pst.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Варианты маршрута: ${variants.size}",
                        style = Pst.sans.copy(fontSize = 12.sp),
                        color = Pst.textMuted,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(12.dp)
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .background(Pst.surface, RoundedCornerShape(20.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    variants.forEachIndexed { i, v ->
                        val isSel = i == selected
                        Row(
                            modifier = Modifier
                                .border(if (isSel) 2.dp else 1.dp, if (isSel) variantComposeColor(i) else Pst.borderStrong, RoundedCornerShape(12.dp))
                                .background(if (isSel) variantComposeColor(i).copy(alpha = 0.10f) else Pst.surface, RoundedCornerShape(12.dp))
                                .clickable { selected = i }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(Modifier.size(10.dp).background(variantComposeColor(i), CircleShape))
                            Text(
                                text = String.format(Locale.US, "%.0f км", v.lengthKm).replace(".", ",") +
                                    (labels[i]?.let { " · через $it" } ?: ""),
                                style = Pst.sans.copy(fontSize = 14.sp, fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium),
                                color = Pst.text,
                                maxLines = 1,
                            )
                        }
                    }
                }
                val current = variants.getOrNull(selected)
                if (current != null && current.addableStations().isEmpty()) {
                    Text(
                        text = "Чтобы замер шёл этим путём, добавьте промежуточные станции.",
                        style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 17.sp),
                        color = Pst.textMuted,
                    )
                }
                PstButton(
                    text = "Выбрать маршрут",
                    onClick = { current?.let(onSelect) },
                    kind = PstButtonKind.CTA,
                    enabled = current != null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
