package com.z_company.route.component.servicephase

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.z_company.route.viewmodel.RouteVariant
import com.z_company.route.viewmodel.addableStations
import com.z_company.route.viewmodel.variantViaLabels
import com.z_company.route.util.DeviceLocation
import com.z_company.repository.remote_rest.response.OsmStationCandidate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
private val VARIANT_COLORS = listOf("#0057D9", "#C64F00", "#6F2DBD")
private const val ENDS_SOURCE = "variants-ends"
private const val ENDS_LAYER = "variants-ends-layer"
private const val ME_SOURCE = "variants-me"
private const val VARIANT_LINE_WIDTH = 6f
private fun sourceId(i: Int) = "variants-src-$i"
private fun layerId(i: Int) = "variants-layer-$i"
private fun variantColor(i: Int) = VARIANT_COLORS[i % VARIANT_COLORS.size]
private fun variantComposeColor(i: Int) = Color(android.graphics.Color.parseColor(variantColor(i)))

/**
 * Экран выбора варианта маршрута (SCREEN_SPECS §6.6, «Варианты маршрута плеча»): все варианты разными цветами,
 * линии одинаковой толщины; выбранный — непрозрачный и поверх, остальные полупрозрачные; внизу равные карточки
 * с различающей станцией и километражем
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
    findCustomRouteStations: (suspend (String) -> List<OsmStationCandidate>?)? = null,
    onBuildCustomRoute: ((OsmStationCandidate) -> Unit)? = null,
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
        var locationDenied by remember { mutableStateOf(false) }
        var customMode by remember { mutableStateOf(false) }
        var customQuery by remember { mutableStateOf(TextFieldValue("")) }
        var customCandidates by remember { mutableStateOf<List<OsmStationCandidate>>(emptyList()) }
        var customSelected by remember { mutableStateOf<OsmStationCandidate?>(null) }
        var customSearching by remember { mutableStateOf(false) }
        var customSearchFailed by remember { mutableStateOf(false) }
        val alive = remember { AtomicBoolean(true) }
        val scope = rememberCoroutineScope()
        fun showMyLocation() {
            scope.launch {
                val fix = DeviceLocation.currentFix(context) ?: run {
                    locationDenied = true
                    return@launch
                }
                val m = map ?: return@launch
                m.style?.getSourceAs<GeoJsonSource>(ME_SOURCE)
                    ?.setGeoJson(Point.fromLngLat(fix.longitude, fix.latitude))
                m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(fix.latitude, fix.longitude), 13.0))
            }
        }
        val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.any { it }) showMyLocation() else locationDenied = true
        }
        val labels = remember(variants) { variantViaLabels(variants) }
        LaunchedEffect(customMode, customQuery.text) {
            val find = findCustomRouteStations ?: return@LaunchedEffect
            val query = customQuery.text.trim()
            customSelected = null
            customCandidates = emptyList()
            customSearchFailed = false
            if (!customMode || query.length < 2) return@LaunchedEffect
            delay(350)
            customSearching = true
            val result = find(query)
            customSearching = false
            if (result == null) customSearchFailed = true else customCandidates = result
                .distinctBy { it.osmId ?: "${it.name}:${it.latitude}:${it.longitude}" }
                .take(4)
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
                        style.addSource(GeoJsonSource(ME_SOURCE, FeatureCollection.fromFeatures(emptyList())))
                        style.addLayer(
                            CircleLayer("variants-me-layer", ME_SOURCE).withProperties(
                                PropertyFactory.circleRadius(7f),
                                PropertyFactory.circleColor("#00A0F5"),
                                PropertyFactory.circleStrokeColor("#FFFFFF"),
                                PropertyFactory.circleStrokeWidth(3f),
                            )
                        )
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
                        PropertyFactory.lineOpacity(if (isSel) 1f else 0.85f),
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
                        .heightIn(min = 56.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .background(Pst.surface, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "$from → $to",
                        style = Pst.sans.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                        color = Pst.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
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
                        locationDenied = false
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
                if (!customMode) Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    variants.forEachIndexed { i, v ->
                        val isSel = i == selected
                        Column(
                            modifier = Modifier
                                .border(if (isSel) 2.dp else 1.dp, if (isSel) variantComposeColor(i) else Pst.borderStrong, RoundedCornerShape(12.dp))
                                .background(Pst.surface, RoundedCornerShape(12.dp))
                                .clickable { selected = i }
                                .weight(1f)
                                .height(104.dp)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(width = 28.dp, height = 4.dp).background(variantComposeColor(i), RoundedCornerShape(2.dp)))
                                // Разрыв после дефиса: «Гатчина-Варшавская» переносится, а не обрезается многоточием.
                                Text(
                                    text = labels[i]?.let { "через ${it.replace("-", "-\u200B")}" }
                                        ?: if (i == 0) "Основной маршрут" else "Вариант маршрута",
                                    style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold),
                                    color = Pst.text,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            // Километраж — отдельной строкой у нижнего края карточки (у всех карточек на одной линии).
                            Text(
                                text = String.format(Locale.US, "%.0f км", v.lengthKm).replace(".", ","),
                                style = Pst.mono.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                                color = Pst.textMuted,
                                maxLines = 1,
                            )
                        }
                    }
                }
                val current = variants.getOrNull(selected)
                if (!customMode && current != null && current.addableStations().isEmpty()) {
                    Text(
                        text = "Чтобы замер шёл этим путём, добавьте промежуточные станции.",
                        style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 17.sp),
                        color = Pst.textMuted,
                    )
                }
                if (locationDenied) {
                    Text(
                        text = "Нет доступа к местоположению",
                        style = Pst.sans.copy(fontSize = 12.sp),
                        color = Pst.warning,
                    )
                }
                if (!customMode && onBuildCustomRoute != null && findCustomRouteStations != null) {
                    PstButton(
                        text = "Построить другой маршрут",
                        onClick = { customMode = true },
                        kind = PstButtonKind.TONAL,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (customMode) {
                    Text("Через какую станцию построить?", style = Pst.sans.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
                    PstTextField(
                        value = customQuery,
                        onValueChange = { customQuery = it.copy(selection = TextRange(it.text.length)) },
                        label = "Станция по пути",
                        placeholder = "Начните вводить название",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    when {
                        customSearching -> Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(color = Pst.accent, modifier = Modifier.size(24.dp))
                        }
                        customSearchFailed -> Text("Не удалось найти станции. Проверьте подключение.", style = Pst.sans.copy(fontSize = 12.sp), color = Pst.danger)
                        customQuery.text.trim().length >= 2 && customCandidates.isEmpty() -> Text("Станции не найдены", style = Pst.sans.copy(fontSize = 12.sp), color = Pst.textMuted)
                        else -> customCandidates.forEach { candidate ->
                            val chosen = candidate == customSelected
                            Column(
                                Modifier.fillMaxWidth()
                                    .background(if (chosen) Pst.accentSoft else Pst.bg, RoundedCornerShape(12.dp))
                                    .border(if (chosen) 2.dp else 1.dp, if (chosen) Pst.accent else Pst.border, RoundedCornerShape(12.dp))
                                    .clickable { customSelected = candidate }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(candidate.name, style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
                                listOfNotNull(candidate.esr?.let { "ЕСР $it" }, candidate.operator, candidate.region).takeIf { it.isNotEmpty() }?.let {
                                    Text(it.joinToString(" · "), style = Pst.sans.copy(fontSize = 11.sp), color = Pst.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PstButton(text = "Назад", onClick = { customMode = false }, kind = PstButtonKind.OUTLINED, modifier = Modifier.weight(1f))
                        PstButton(text = "Построить", onClick = { customSelected?.let { onBuildCustomRoute?.invoke(it) } }, enabled = customSelected != null, modifier = Modifier.weight(1f))
                    }
                } else PstButton(
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
