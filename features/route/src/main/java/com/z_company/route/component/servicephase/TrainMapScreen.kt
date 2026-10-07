package com.z_company.route.component.servicephase

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.z_company.domain.util.GeoFix
import com.z_company.route.util.DeviceLocation
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import java.util.concurrent.atomic.AtomicBoolean

private const val TRAIN_ME_SOURCE = "train-map-me"

/**
 * Карта поезда (SCREEN_SPECS §6.9): полноэкранная карта на общей подложке карты плеч (цепочка тайлов, ж/д слои).
 * Состояние 1 — пустая карта с текущим местоположением; состояние 3 — если у станции поезда/плеча нет координат,
 * внизу предложение «Определить координаты по GPS» ([onDetermine] пишет их в справочник станций; существующие
 * координаты не перезаписываются). Запросов к отрисовке нет (см. PhaseMapPicker про падение).
 */
@Composable
internal fun TrainMapScreen(
    missingStation: () -> String?,
    currentFix: suspend () -> GeoFix?,
    onDetermine: suspend (String) -> Boolean,
    onDismiss: () -> Unit,
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
        val alive = remember { AtomicBoolean(true) }
        val scope = rememberCoroutineScope()

        fun showMyLocation() {
            scope.launch {
                val fix = currentFix() ?: run {
                    locationDenied = true
                    return@launch
                }
                locationDenied = false
                val m = map ?: return@launch
                if (!alive.get()) return@launch
                m.style?.getSourceAs<GeoJsonSource>(TRAIN_ME_SOURCE)
                    ?.setGeoJson(Point.fromLngLat(fix.longitude, fix.latitude))
                m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(fix.latitude, fix.longitude), 14.0))
            }
        }
        val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.any { it }) showMyLocation() else locationDenied = true
        }
        fun requestOrShowLocation() {
            if (DeviceLocation.hasPermission(context)) showMyLocation()
            else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
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
                            )
                        )
                        styleReady = true
                    }
                }
            }
        }
        // Текущее место — как только карта и стиль готовы (разрешение — при необходимости).
        LaunchedEffect(styleReady) { if (styleReady) requestOrShowLocation() }
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
                        text = "Карта поезда",
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

            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp).offset(y = maxHeight * 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapButton(onClick = { map?.animateCamera(CameraUpdateFactory.zoomIn()) }, description = "Приблизить") {
                    Text("+", style = Pst.sans.copy(fontSize = 22.sp), color = Pst.text)
                }
                MapButton(onClick = { map?.animateCamera(CameraUpdateFactory.zoomOut()) }, description = "Отдалить") {
                    Text("−", style = Pst.sans.copy(fontSize = 22.sp), color = Pst.text)
                }
                MapButton(onClick = { requestOrShowLocation() }, description = "Моё местоположение") {
                    Box(Modifier.size(14.dp).border(2.dp, Pst.accent, CircleShape), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(4.dp).background(Pst.accent, CircleShape))
                    }
                }
            }

            val name = missing
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
