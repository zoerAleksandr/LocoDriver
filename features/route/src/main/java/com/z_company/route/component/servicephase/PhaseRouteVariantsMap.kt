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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.width
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
private val VARIANT_COLORS = listOf("#0B57D0", "#C2410C", "#6D28D9")
private const val ENDS_SOURCE = "variants-ends"
private const val ENDS_LAYER = "variants-ends-layer"
private const val ME_SOURCE = "variants-me"
private const val VARIANT_LINE_WIDTH = 5f
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
    onBuildCustomRoute: ((List<OsmStationCandidate>) -> Unit)? = null,
    /** Список станций и ошибка после неудачного построения: панель открывается снова с ними. */
    initialCustom: List<OsmStationCandidate>? = null,
    initialCustomError: String? = null,
    /** Предпросмотр построенного своего маршрута: одна карточка, подсказка и «Назад» / «Выбрать маршрут». */
    customPreview: Boolean = false,
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
        var customMode by remember { mutableStateOf(initialCustom != null) }
        var customError by remember { mutableStateOf(initialCustomError) }
        var customQuery by remember { mutableStateOf(TextFieldValue("")) }
        var customCandidates by remember { mutableStateOf<List<OsmStationCandidate>>(emptyList()) }
        // Выбранные промежуточные станции — по порядку от отправления к прибытию; путь строится строго через них.
        val customChosen = remember { mutableStateListOf<OsmStationCandidate>().apply { initialCustom?.let(::addAll) } }
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
            customCandidates = emptyList()
            customSearchFailed = false
            if (!customMode || query.length < 2) return@LaunchedEffect
            delay(350)
            customSearching = true
            val result = find(query)
            customSearching = false
            // Все совпадения как есть — порядок задаёт сервер (старый сервер вернёт до 5).
            if (result == null) customSearchFailed = true else customCandidates = result
                .distinctBy { it.osmId ?: "${it.name}:${it.latitude}:${it.longitude}" }
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

        // Цвета по PWA (эталон внешнего вида, §6.6): шторка и верхняя карточка — «поверхность» панели,
        // поля/карточки внутри — чуть светлее (тёмная тема) или серее (светлая).
        val dark = Pst.bg.luminance() < 0.5f
        val panel = if (dark) Pst.bgSubtle else Pst.surface
        val field = if (dark) Pst.surface else Pst.bg
        val panelShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
        val maxPanelHeight = (LocalConfiguration.current.screenHeightDp * 0.75f).dp

        Box(modifier = Modifier.fillMaxSize().background(Pst.bg)) {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView })
            // Верхняя карточка: «✕» закрыть и «A → B».
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .background(panel, RoundedCornerShape(16.dp))
                    .padding(start = 6.dp, top = 6.dp, end = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClickLabel = "Закрыть", onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { PstIcon(PstIcons.Close, 20.dp, Pst.accent) }
                Text(
                    text = "$from → $to",
                    style = Pst.sans.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                    color = Pst.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                )
            }
            // «+», «−», «Моё местоположение» — под верхней карточкой справа.
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 16.dp, top = 82.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RoundMapButton(panel, "Приблизить", onClick = { map?.animateCamera(CameraUpdateFactory.zoomIn()) }) {
                    Text("+", style = Pst.sans.copy(fontSize = 22.sp), color = Pst.text)
                }
                RoundMapButton(panel, "Отдалить", onClick = { map?.animateCamera(CameraUpdateFactory.zoomOut()) }) {
                    Text("−", style = Pst.sans.copy(fontSize = 22.sp), color = Pst.text)
                }
                RoundMapButton(panel, "Моё местоположение", onClick = {
                    locationDenied = false
                    if (hasLocationPermission(context)) showMyLocation()
                    else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }) {
                    Box(Modifier.size(14.dp).border(2.dp, Pst.text, CircleShape), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(4.dp).background(Pst.text, CircleShape))
                    }
                }
            }
            // Нижняя шторка со скруглёнными верхними углами.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(max = maxPanelHeight)
                    .shadow(8.dp, panelShape)
                    .background(panel, panelShape)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            ) {
                val current = variants.getOrNull(selected)
                when {
                    customMode -> {
                        Text("Через какие станции построить?", style = Pst.sans.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
                        Spacer(Modifier.height(10.dp))
                        if (customChosen.isNotEmpty()) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(field),
                            ) {
                                customChosen.forEachIndexed { index, c ->
                                    if (index > 0) HorizontalDivider(color = Pst.border)
                                    Row(
                                        Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text("${index + 1}", style = Pst.sans.copy(fontSize = 13.sp), color = Pst.textMuted, modifier = Modifier.width(20.dp))
                                        Text(
                                            (c.name + if (c.kind == "halt") ", о.п." else "").softBreaks(),
                                            style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                            color = Pst.text,
                                            modifier = Modifier.weight(1f),
                                        )
                                        MiniAction(PstIcons.ChevronUp, "Выше", enabled = index > 0) {
                                            customChosen.add(index - 1, customChosen.removeAt(index))
                                        }
                                        MiniAction(PstIcons.ChevronDown, "Ниже", enabled = index < customChosen.lastIndex) {
                                            customChosen.add(index + 1, customChosen.removeAt(index))
                                        }
                                        MiniAction(PstIcons.Close, "Убрать", enabled = true) { customChosen.removeAt(index) }
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        } else {
                            Text(
                                "Добавьте станции в порядке прохождения пути — маршрут пройдёт строго через них.",
                                style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 18.sp),
                                color = Pst.textMuted,
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        ViaSearchField(
                            value = customQuery,
                            onValueChange = { customQuery = it.copy(selection = TextRange(it.text.length)) },
                            placeholder = if (customChosen.isEmpty()) "Название станции" else "Добавить ещё станцию",
                            background = field,
                        )
                        val query = customQuery.text.trim()
                        when {
                            customSearching -> PanelHint("Ищем на карте…")
                            customSearchFailed -> PanelHint("Не удалось найти станции. Проверьте подключение.", error = true)
                            query.length >= 2 && customCandidates.isEmpty() -> PanelHint("Станция с таким названием не нашлась на карте")
                        }
                        if (customCandidates.isNotEmpty() && query.length >= 2) {
                            Column(
                                Modifier
                                    .padding(top = 10.dp)
                                    .fillMaxWidth()
                                    .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.32f).dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(field)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                customCandidates.forEachIndexed { i, candidate ->
                                    val added = customChosen.any {
                                        it.osmId == candidate.osmId ||
                                            (kotlin.math.abs(it.latitude - candidate.latitude) < 1e-5 && kotlin.math.abs(it.longitude - candidate.longitude) < 1e-5)
                                    }
                                    if (i > 0) HorizontalDivider(color = Pst.border)
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !added) {
                                                customChosen.add(candidate)
                                                customError = null
                                                customQuery = TextFieldValue("")
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                (candidate.name + if (candidate.kind == "halt") ", о.п." else "").softBreaks(),
                                                style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                                color = if (added) Pst.textMuted else Pst.text,
                                            )
                                            candidate.esr?.takeIf { it.isNotBlank() }?.let {
                                                Text("ЕСР $it", style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Pst.textMuted)
                                            }
                                            listOfNotNull(candidate.operator, candidate.region).takeIf { it.isNotEmpty() }?.let {
                                                Text(it.joinToString(" · "), style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Pst.textMuted)
                                            }
                                            if (candidate.neighbours.isNotEmpty()) {
                                                Text(
                                                    "Соседние: " + candidate.neighbours.joinToString(" · "),
                                                    style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp),
                                                    color = Pst.textMuted,
                                                )
                                            }
                                        }
                                        if (added) Text("добавлена", style = Pst.sans.copy(fontSize = 12.sp), color = Pst.textMuted)
                                        else PstIcon(PstIcons.Plus, 18.dp, Pst.accent)
                                    }
                                }
                            }
                        }
                        customError?.let { PanelHint(it, error = true) }
                        PanelActions(
                            leftText = "Назад", onLeft = { customMode = false },
                            rightText = "Построить", rightEnabled = customChosen.isNotEmpty(),
                            onRight = { onBuildCustomRoute?.invoke(customChosen.toList()) },
                            field = field,
                        )
                    }
                    customPreview && current != null -> {
                        Row(Modifier.fillMaxWidth()) {
                            VariantChip(
                                title = (if (variants.size == 1 && current.via.isNotEmpty()) current.via.joinToString(", ") { it.name } else labels[selected])
                                    ?.let { "через $it" } ?: "Свой маршрут",
                                distance = String.format(Locale.US, "%.0f км", current.lengthKm).replace(".", ","),
                                color = variantComposeColor(0),
                                selected = true,
                                field = field,
                                modifier = Modifier.weight(1f),
                            ) {}
                        }
                        PanelHint("Станции и километраж применятся к плечу после «Выбрать маршрут».")
                        PanelActions(
                            leftText = "Назад", onLeft = onDismiss,
                            rightText = "Выбрать маршрут", rightEnabled = true,
                            onRight = { onSelect(current) },
                            field = field,
                        )
                    }
                    else -> {
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            variants.forEachIndexed { i, v ->
                                VariantChip(
                                    title = (if (variants.size == 1 && v.via.isNotEmpty()) v.via.joinToString(", ") { it.name } else labels[i])
                                        ?.let { "через $it" } ?: if (i == 0) "Основной маршрут" else "Вариант маршрута",
                                    distance = String.format(Locale.US, "%.0f км", v.lengthKm).replace(".", ","),
                                    color = variantComposeColor(i),
                                    selected = i == selected,
                                    field = field,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                ) { selected = i }
                            }
                        }
                        if (current != null && current.addableStations().isEmpty()) {
                            PanelHint("Чтобы замер шёл этим путём, добавьте промежуточные станции.")
                        }
                        if (locationDenied) PanelHint("Нет доступа к местоположению", error = true)
                        PanelActions(
                            leftText = "Построить другой маршрут", onLeft = { customMode = true },
                            leftEnabled = onBuildCustomRoute != null && findCustomRouteStations != null,
                            rightText = "Выбрать маршрут", rightEnabled = current != null,
                            onRight = { current?.let(onSelect) },
                            field = field,
                        )
                    }
                }
            }
        }
    }
}

/** Перенос строки после дефиса («Санкт-Петербург-/Товарный»), а не посреди слова. */
private fun String.softBreaks() = replace("-", "-\u200B")

/** Круглая кнопка карты («+», «−», «Моё местоположение»): 44 dp, скругление 12, цвет поверхности панели. */
@Composable
private fun RoundMapButton(panel: Color, description: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(panel, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Карточка варианта: цветная точка, «через …» жирным, километраж серым внизу; выбранная — рамка цвета линии. */
@Composable
private fun VariantChip(
    title: String,
    distance: String,
    color: Color,
    selected: Boolean,
    field: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .heightIn(min = 84.dp)
            .clip(shape)
            .background(field)
            .border(2.dp, if (selected) color else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Text(
            text = title.softBreaks(),
            style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 14.4.sp, fontWeight = FontWeight.SemiBold),
            color = Pst.text,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        Text(distance, style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 14.4.sp), color = Pst.textMuted, maxLines = 1)
    }
}

/** Подсказка/ошибка панели: 13 sp, серый (или цвет ошибки), отступ сверху 10 dp. */
@Composable
private fun PanelHint(text: String, error: Boolean = false) {
    Text(
        text = text,
        style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 18.sp),
        color = if (error) Pst.danger else Pst.textMuted,
        modifier = Modifier.padding(top = 10.dp),
    )
}

/** Два равных действия в ряд: слева «призрачное» (синий текст на подложке), справа CTA (светлая заливка). */
@Composable
private fun PanelActions(
    leftText: String,
    onLeft: () -> Unit,
    rightText: String,
    rightEnabled: Boolean,
    onRight: () -> Unit,
    field: Color,
    leftEnabled: Boolean = true,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .heightIn(min = 48.dp)
                .clip(shape)
                .background(field)
                .clickable(enabled = leftEnabled, onClick = onLeft)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(leftText, style = Pst.sans.copy(fontSize = 14.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold), color = Pst.accent, textAlign = TextAlign.Center)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .heightIn(min = 48.dp)
                .alpha(if (rightEnabled) 1f else 0.35f)
                .clip(shape)
                .background(Pst.cta)
                .clickable(enabled = rightEnabled, onClick = onRight)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(rightText, style = Pst.sans.copy(fontSize = 14.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold), color = Pst.ctaInk, textAlign = TextAlign.Center)
        }
    }
}

/** Поле поиска станции: 44 dp, скругление 10, фон полей, без подписи. */
@Composable
private fun ViaSearchField(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, placeholder: String, background: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = Pst.sans.copy(fontSize = 15.sp, color = Pst.text),
            cursorBrush = SolidColor(Pst.accent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (value.text.isEmpty()) Text(placeholder, style = Pst.sans.copy(fontSize = 15.sp), color = Pst.textMuted)
                    inner()
                }
            },
        )
    }
}

/** Иконка-кнопка в строке выбранной станции (↑ ↓ ✕): 36 dp, акцентный цвет, неактивная — бледнее. */
@Composable
private fun MiniAction(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { PstIcon(icon, 18.dp, if (enabled) Pst.accent else Pst.accent.copy(alpha = 0.3f)) }
}

