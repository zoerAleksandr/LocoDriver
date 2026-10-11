package com.z_company.route.component.servicephase

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.alpha
import androidx.activity.compose.BackHandler
import kotlin.math.abs
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.z_company.domain.util.ActualRails
import com.z_company.domain.util.ArrivalPlan
import com.z_company.domain.util.ArrivalPlanData
import com.z_company.domain.util.PlanTarget
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
import com.z_company.route.component.NetworkRequired
import com.z_company.route.component.rememberIsOnline
import com.z_company.route.component.rememberNetworkGate
import com.z_company.domain.track.HeadingEstimator
import androidx.compose.runtime.collectAsState
import com.z_company.domain.entities.setting.ServicePhaseStationKind
import com.z_company.domain.track.SpeedBand
import com.z_company.domain.track.SpeedProfile
import com.z_company.domain.track.SpeedScale
import com.z_company.domain.track.TrackAnalysis
import com.z_company.route.util.GnssQualityMonitor
import com.z_company.domain.track.TrackPoint
import com.z_company.domain.track.TrainStopMark
import com.z_company.domain.util.AppMapLabel
import com.z_company.domain.util.BaseMapLabel
import com.z_company.domain.util.FollowReturn
import com.z_company.domain.util.LabelDedup
import com.z_company.domain.util.LabelPlacer
import com.z_company.domain.util.PathKeeper
import com.z_company.domain.util.RailRefetch
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
import kotlinx.coroutines.CancellationException

private const val TRAIN_ME_SOURCE = "train-map-me"
private const val TRAIN_ROUTE_SOURCE = "train-map-route"
private const val TRAIN_STOPS_SOURCE = "train-map-stops"
private const val TRAIN_TRACK_SOURCE = "train-map-track"
private const val TRAIN_MARKS_SOURCE = "train-map-marks"
private const val TRAIN_SEL_SOURCE = "train-map-sel"
private const val TRAIN_TAIL_SOURCE = "train-map-tail"
private const val TRAIN_OFF_SOURCE = "train-map-off"
private const val TRAIN_NODATA_SOURCE = "train-map-nodata"
private const val TRAIN_PLAN_SOURCE = "train-map-plan"
private const val TRAIN_ENDS_SOURCE = "train-map-ends"
private const val END_START_IMAGE = "train-end-start"
private const val END_FINISH_IMAGE = "train-end-finish"
private const val ME_ICON_IMAGE = "train-me-icon"
private const val PLAN_FLAG_IMAGE = "train-plan-flag"

/** Выбранное тапом на карте: точка пройденного пути или остановка. */
private sealed interface TrainMapSelection {
    data class PathPoint(val point: TrackPoint) : TrainMapSelection
    data class Stop(val mark: TrainStopMark) : TrainMapSelection
    /** Начало ([isStart]) или окончание пройденного пути. */
    data class Endpoint(val point: TrackPoint, val isStart: Boolean) : TrainMapSelection
    /** Зона без GPS по локальным трекам (§6.9, «Зоны без GPS»). */
    data class GnssZoneSel(val zone: com.z_company.domain.gnss.GnssZone) : TrainMapSelection
}

/**
 * Цвет линии пути по свойству `speed` (км/ч, < 0 - неизвестна): общая шкала [SpeedScale] (в одном месте с графиком),
 * диапазоны с границами включительно сверху. Карта всегда светлая, поэтому оттенки для светлого фона.
 */
private fun speedColorExpression(profile: SpeedProfile): Expression {
    val (g, y, o) = SpeedScale.limits(profile)
    fun c(b: SpeedBand) = Expression.color(Color.parseColor(String.format("#%06X", SpeedScale.rgb(b))))
    val eps = 0.0001
    return Expression.switchCase(
        Expression.lt(Expression.get("speed"), Expression.literal(0)),
        c(SpeedBand.UNKNOWN),
        Expression.step(
            Expression.get("speed"), c(SpeedBand.GREEN),
            Expression.stop(g + eps, c(SpeedBand.YELLOW)),
            Expression.stop(y + eps, c(SpeedBand.ORANGE)),
            Expression.stop(o + eps, c(SpeedBand.RED)),
        ),
    )
}

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
    /** Состояние записи «АВТО» для точного пустого экрана графика (§6.9); null - неизвестно. */
    loadRecordingState: suspend () -> com.z_company.domain.track.TrackRecordingState? = { null },
    /** Сделать этот поезд целью записи «АВТО»; true - цель назначена. */
    makeTarget: () -> Boolean = { false },
    /** Текст предупреждения «Запись уже идёт» для кнопки «Сделать целью»; null - пишется не другой поезд. */
    loadConflictText: suspend () -> String? = { null },
    onDetermine: suspend (String) -> Boolean,
    /** Пояс отображения времени (displayTimeZone), нужен для ввода планового времени, когда трека нет. */
    loadZone: suspend () -> String = { "GMT+3" },
    /** Ключ хранения плана прибытия (id поезда); null - план не сохраняется между открытиями карты. */
    planKey: String? = null,
    onOpenConsentSettings: () -> Unit = {},
    /** Включить «АВТО» для этого поезда (кнопка пустого графика); null - кнопки нет (просмотр трека из «Истории»). */
    onEnableAuto: (() -> Unit)? = null,
    /** Режим «АВТО» включён (запись трека идёт): состояние кнопки записи в колонке кнопок карты. */
    autoRecording: Boolean = false,
    /** Нажатие кнопки «Начать/Остановить запись» (то же переключение, что чип «АВТО»); null - кнопки нет. */
    onToggleRecording: (() -> Unit)? = null,
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
        // Поколение стиля: растёт каждый раз, когда слои приложения добавлены в (новый) стиль - данные перерисовываются.
        var styleGen by remember { mutableStateOf(0) }
        var locationDenied by remember { mutableStateOf(false) }
        var missing by remember { mutableStateOf(missingStation()) }
        var determining by remember { mutableStateOf(false) }
        var determineFailed by remember { mutableStateOf(false) }
        var savedName by remember { mutableStateOf<String?>(null) }
        var myFix by remember { mutableStateOf<GeoFix?>(null) }
        val hasRoute = TrainMapRoute.hasRoute(stops)
        var railState by remember { mutableStateOf<ServicePhaseAutoFillViewModel.RailLineResult?>(null) }
        var loadAttempt by remember { mutableStateOf(0) }
        // «Линии по факту» (ActualRails): пути, по которым поезд реально ехал вне линии плеча (соседний/параллельный путь).
        var factLines by remember { mutableStateOf(emptyList<List<Pair<Double, Double>>>()) }
        val triedFactRuns = remember { mutableSetOf<String>() }
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
        // «Зоны без GPS» (§6.9): личный переключатель, зоны считаются по локальным трекам.
        val trackRepo: com.z_company.domain.track.TrackRepository = org.koin.compose.koinInject()
        var gnssOn by remember { mutableStateOf(GnssDeadZonePrefs.enabled(context)) }
        var gnssZones by remember { mutableStateOf(emptyList<com.z_company.domain.gnss.GnssZone>()) }
        // Кнопка записи: подтверждаем смену состояния короткой подсказкой (включение может идти через разрешения/согласие).
        var prevRecording by remember { mutableStateOf(autoRecording) }
        LaunchedEffect(autoRecording) {
            if (autoRecording != prevRecording) {
                prevRecording = autoRecording
                if (onToggleRecording != null && !viewOnly) {
                    android.widget.Toast.makeText(
                        context, if (autoRecording) "Запись трека включена" else "Запись трека остановлена",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
        // Курс маркера (Location.bearing, иначе по двум точкам; на стоянке сохраняется), значок положения, режим «еду по карте».
        val heading = remember { HeadingEstimator() }
        var headingDeg by remember { mutableStateOf<Double?>(null) }
        val positionIcon by PositionIconStore.selected(context).collectAsState()
        var showIconPicker by remember { mutableStateOf(false) }
        var ride by rememberSaveable { mutableStateOf(false) }
        // Поворот карты (градусы, 0 = север сверху): для кнопки-компаса.
        var mapBearing by remember { mutableStateOf(0.0) }
        // Масштаб, выбранный пользователем кнопками «+»/«−»: сохраняется при следовании (иначе слежение возвращало 14).
        var userZoom by remember { mutableStateOf<Double?>(null) }
        var snappedLast by remember { mutableStateOf<TrackPoint?>(null) }
        // Живые точки записи (запасной источник пройденного пути, не зависит от БД и сети).
        var livePoints by remember { mutableStateOf(emptyList<TrackPoint>()) }
        // Возврат в слежение через FollowReturn.RETURN_DELAY_MS без касаний (SCREEN_SPECS §6.9).
        var lastInteractionMs by remember { mutableStateOf(System.currentTimeMillis()) }
        var fingerDown by remember { mutableStateOf(false) }
        var rideBeforeGesture by remember { mutableStateOf(false) }
        var zoomBeforeGesture by remember { mutableStateOf<Double?>(null) }
        val autoRunning by GpsRecorder.isRunning.collectAsState()
        // Функции, которым нужен интернет (перестроение линии), без сети показывают диалог «Нет интернета».
        val networkGate = rememberNetworkGate()
        // Планирование прибытия (этап 5, §6.9 п.7): сохранённый план, ещё не рассчитанная точка (draft) и пояснение «недоступно».
        var plan by remember(planKey) {
            mutableStateOf(TrainMapPlanStore.load(context, planKey)?.takeIf { !ArrivalPlan.isStale(it, System.currentTimeMillis()) })
        }
        var draft by remember { mutableStateOf<PlanTarget?>(null) }
        var planNote by remember { mutableStateOf<String?>(null) }
        var planOpen by remember { mutableStateOf(true) }
        var displayZone by remember { mutableStateOf("GMT+3") }
        val online = rememberIsOnline()
        val density = context.resources.displayMetrics.density
        // Шкала цвета скорости: пассажирский/пригородный поезд - свои пороги (§6.9); неизвестная категория - грузовая.
        val speedProfile = if (track?.passenger == true) SpeedProfile.PASSENGER else SpeedProfile.FREIGHT
        // Индикатор GPS на карте считается по тем же живым обновлениям, что маркер, и не зависит от записи «АВТО».
        val gnssMonitor = remember { GnssQualityMonitor(context) }
        DisposableEffect(permGranted, viewOnly) {
            if (!viewOnly && permGranted) gnssMonitor.start()
            onDispose { gnssMonitor.stop() }
        }
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
            lastInteractionMs = System.currentTimeMillis()
            userZoom = z
            m.animateCamera(CameraUpdateFactory.zoomTo(z))
        }

        val mapView = remember {
            MapView(context).also { mv ->
                mv.getMapAsync { m ->
                    map = m
                    m.uiSettings.isRotateGesturesEnabled = true
                    m.uiSettings.isTiltGesturesEnabled = false
                    // Компас один - кнопка в колонке справа; системный компас MapLibre (вверху, под названием) отключён.
                    m.uiSettings.isCompassEnabled = false
                    m.uiSettings.isLogoEnabled = false
                    m.uiSettings.isAttributionEnabled = true
                    m.cameraPosition = CameraPosition.Builder().target(LatLng(55.75, 37.62)).zoom(5.0).build()
                    // Слои приложения добавляются в КАЖДЫЙ загруженный стиль (смена стиля при плохой сети: прокси ->
                    // OpenFreeMap -> растр, перезагрузка стиля): иначе пройденный путь и точки пропадали.
                    fun install(style: org.maplibre.android.maps.Style) {
                        if (style.getSource(TRAIN_ME_SOURCE) != null) return
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
                        // «Зоны без GPS»: мягкие красноватые пятна под всеми слоями приложения и под подписями базовой карты.
                        style.addSource(GeoJsonSource(GNSS_ZONES_SOURCE, FeatureCollection.fromFeatures(emptyList())))
                        style.addLayerBelow(
                            CircleLayer(GNSS_ZONES_LAYER, GNSS_ZONES_SOURCE).withProperties(
                                PropertyFactory.circleColor("#E53935"),
                                PropertyFactory.circleBlur(1f),
                                PropertyFactory.circleRadius(gnssRadiusExpression(55.0)),
                                PropertyFactory.circleOpacity(
                                    Expression.interpolate(
                                        Expression.linear(), Expression.get("w"),
                                        Expression.stop(0.0, 0.30f), Expression.stop(1.0, 0.60f),
                                    )
                                ),
                                PropertyFactory.circlePitchAlignment(Property.CIRCLE_PITCH_ALIGNMENT_MAP),
                            ),
                            style.layers.firstOrNull { it is SymbolLayer }?.id ?: "train-map-route-layer",
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
                        val speedColor = speedColorExpression(SpeedProfile.FREIGHT)
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
                        // Участки «нет данных» (потеря сигнала, разрыв записи > 90 с): серый пунктир, не стоянка и не скорость.
                        style.addSource(GeoJsonSource(TRAIN_NODATA_SOURCE, FeatureCollection.fromFeatures(emptyList())))
                        style.addLayerBelow(
                            LineLayer("train-map-nodata-layer", TRAIN_NODATA_SOURCE).withProperties(
                                PropertyFactory.lineColor("#78909C"),
                                PropertyFactory.lineWidth(2.2f),
                                PropertyFactory.lineDasharray(arrayOf(1.2f, 2f)),
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
                        // Флажок точки плана прибытия (этап 5).
                        style.addSource(GeoJsonSource(TRAIN_PLAN_SOURCE, FeatureCollection.fromFeatures(emptyList())))
                        style.addImage(PLAN_FLAG_IMAGE, renderPlanFlag(density))
                        style.addLayerBelow(
                            SymbolLayer("train-map-plan-layer", TRAIN_PLAN_SOURCE).withProperties(
                                PropertyFactory.iconImage(PLAN_FLAG_IMAGE),
                                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM_LEFT),
                                PropertyFactory.iconAllowOverlap(true),
                                PropertyFactory.iconIgnorePlacement(true),
                            ),
                            "train-map-me-layer",
                        )
                        // Начало и окончание пройденного пути (§6.9): значки поверх линии, под маркером «я».
                        style.addSource(GeoJsonSource(TRAIN_ENDS_SOURCE, FeatureCollection.fromFeatures(emptyList())))
                        style.addImage(END_START_IMAGE, renderTrackEndIcon(density, true))
                        style.addImage(END_FINISH_IMAGE, renderTrackEndIcon(density, false))
                        style.addLayerBelow(
                            SymbolLayer("train-map-ends-layer", TRAIN_ENDS_SOURCE).withProperties(
                                PropertyFactory.iconImage(Expression.get("icon")),
                                PropertyFactory.iconAllowOverlap(true),
                                PropertyFactory.iconIgnorePlacement(true),
                            ),
                            "train-map-me-layer",
                        )
                        styleReady = true
                        styleGen++
                    }
                    mv.addOnDidFinishLoadingStyleListener {
                        m.style?.let { st -> if (st.isFullyLoaded) runCatching { install(st) } }
                    }
                    loadMapStyle(mv, m) { style -> install(style) }
                }
                // Касания карты: палец на карте и момент последнего касания (возврат в слежение через 10 с без касаний).
                mv.setOnTouchListener { _, ev ->
                    when (ev.actionMasked) {
                        android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_POINTER_DOWN -> {
                            fingerDown = true
                            lastInteractionMs = System.currentTimeMillis()
                        }
                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                            fingerDown = false
                            lastInteractionMs = System.currentTimeMillis()
                        }
                    }
                    false
                }
            }
        }
        // Текущее место — как только карта и стиль готовы (разрешение — при необходимости).
        LaunchedEffect(styleReady) { if (styleReady && !viewOnly) requestOrShowLocation(center = !hasRoute) }
        // Линия по рельсам с сервера (всегда запрос; сохранённая — запас без сети), как на карте плеч.
        LaunchedEffect(loadAttempt) {
            if (hasRoute) {
                // Уже показанную линию (из кэша) не убираем на время перезапроса - карта не мигает.
                if (railState !is ServicePhaseAutoFillViewModel.RailLineResult.Ok) railState = null
                railState = loadRailLine(stops.map { it.latitude to it.longitude })
            }
        }
        // Станции плеча и остановки по треку точками; подписи — только те, что помещаются на текущем зуме
        // (LabelPlacer: не перекрывают точки и друг друга, в приоритете крайние и «впереди»).
        val lastStopsSig = remember { object { var value = "" } }
        fun refreshStopSources() {
            val m = map ?: return
            val style = m.style ?: return
            if (!alive.get()) return
            // «Впереди» — вдоль рельсовой линии с сервера (не по прямым между станциями); без линии не выделяется.
            val railLine = (railState as? ServicePhaseAutoFillViewModel.RailLineResult.Ok)?.line
            val ahead = if (hasRoute) TrainMapRoute.nextAhead(stops, railLine, ActualRails.alongRouteFix(railLine, factLines, myFix))?.index else null
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
            val markRows = marks.map { Row(it.latitude, it.longitude, stopTitle(it, stops), "mark", 11f, 8.5f, 1) }
            val rows = routeRows + markRows
            val screenPts = rows.map { r -> m.projection.toScreenLocation(LatLng(r.lat, r.lon)) }
            // Подписи базовой карты (станции/места стиля): своя подпись с тем же названием рядом (<= 40 dp) не рисуется -
            // иначе два одинаковых названия накладываются друг на друга (SCREEN_SPECS §6.9).
            val dups = LabelDedup.duplicates(
                rows.mapIndexed { i, r -> AppMapLabel(screenPts[i].x.toDouble(), screenPts[i].y.toDouble(), r.name) },
                baseMapLabels(m, mapView.width, mapView.height),
                LabelDedup.RADIUS_DP * density,
            )
            val items = rows.mapIndexed { i, r ->
                val pt = screenPts[i]
                val fs = r.sizeSp * density
                MapLabelItem(
                    pt.x.toDouble(), pt.y.toDouble(),
                    labelW = if (r.name.isBlank() || i in dups) 0.0 else r.name.length * fs * 0.55,
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
            // Перерисовка по событию «карта в покое» не должна зацикливаться: пишем источники, только если результат изменился.
            val sig = "$styleGen|${rows.joinToString("|") { it.name + it.role + it.lat + it.lon }}|${shown.sorted()}"
            if (sig == lastStopsSig.value) return
            lastStopsSig.value = sig
            style.getSourceAs<GeoJsonSource>(TRAIN_STOPS_SOURCE)?.setGeoJson(
                FeatureCollection.fromFeatures(routeRows.mapIndexed { i, r -> feature(i, r) })
            )
            style.getSourceAs<GeoJsonSource>(TRAIN_MARKS_SOURCE)?.setGeoJson(
                FeatureCollection.fromFeatures(markRows.mapIndexed { i, r -> feature(routeRows.size + i, r) })
            )
        }
        LaunchedEffect(styleGen, styleReady, myFix, track, map, railState) { if (styleReady) refreshStopSources() }
        DisposableEffect(map) {
            val m = map
            val listener = MapLibreMap.OnCameraIdleListener { refreshStopSources() }
            // Подписи базовой карты подгружаются после остановки камеры (тайлы) - пересчитываем и по «карта в покое».
            val idle = MapView.OnDidBecomeIdleListener { refreshStopSources() }
            m?.addOnCameraIdleListener(listener)
            mapView.addOnDidBecomeIdleListener(idle)
            onDispose {
                m?.removeOnCameraIdleListener(listener)
                mapView.removeOnDidBecomeIdleListener(idle)
            }
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
                    lastInteractionMs = System.currentTimeMillis()
                    if (follow) {
                        // Запоминаем, что было до жеста: через FollowReturn.RETURN_DELAY_MS вернём слежение, масштаб и «еду по карте».
                        rideBeforeGesture = ride
                        zoomBeforeGesture = userZoom ?: m?.cameraPosition?.zoom
                    }
                    follow = false
                    userZoom = null
                    // Ручной жест (в т.ч. поворот двумя пальцами) выключает «еду по карте»; поворот карты не сбрасываем —
                    // возвращает север кнопка-компас.
                    if (ride) ride = false
                }
            }
            val bearingListener = MapLibreMap.OnCameraMoveListener { mapBearing = m?.cameraPosition?.bearing ?: 0.0 }
            m?.addOnCameraMoveStartedListener(l)
            m?.addOnCameraMoveListener(bearingListener)
            onDispose {
                m?.removeOnCameraMoveStartedListener(l)
                m?.removeOnCameraMoveListener(bearingListener)
            }
        }
        // Живая геолокация: подписка только пока карта открыта (раньше было разовое «последнее известное» — позиция не
        // обновлялась). Обновляет маркер, скорость и, в режиме «следовать» при движении, плавно ведёт камеру.
        LaunchedEffect(styleReady, permGranted) {
            if (!styleReady || !permGranted || viewOnly) return@LaunchedEffect
            DeviceLocation.updates(context, onRejected = { gnssMonitor.onRejected() }).collect { f ->
                val m = map ?: return@collect
                if (!alive.get()) return@collect
                val moving = (f.speedKmh ?: 0.0) >= 2.0
                locationDenied = false
                myFix = GeoFix(f.latitude, f.longitude, f.accuracyM)
                gnssMonitor.onFix(f.accuracyM)
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
                        .bearing(if (ride && h != null) h else m.cameraPosition.bearing).tilt(0.0).build()
                    m.easeCamera(CameraUpdateFactory.newCameraPosition(cam), 900)
                }
            }
        }
        // Загрузка записанного трека поезда — асинхронно; пока «АВТО» пишет, обновляется раз в 10 с.
        // Ошибка, пустой или «усохший» результат загрузки прежний путь не затирают (PathKeeper); пока пути нет - повторяем.
        suspend fun reloadTrack() {
            val loaded = try {
                loadTrack()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            track = PathKeeper.keep(track, loaded) { it.points.size }
        }
        LaunchedEffect(Unit) {
            reloadTrack()
            while (isActive) {
                delay(10_000)
                if (!viewOnly && (GpsRecorder.isRunning.value || track == null)) reloadTrack()
            }
        }
        // Текущая скорость — живые точки записи, пока карта открыта.
        LaunchedEffect(Unit) {
            if (viewOnly) return@LaunchedEffect
            GpsRecorder.points.collect { p ->
                livePoints = PathKeeper.appendLive(
                    livePoints,
                    TrackPoint(p.timeMs, p.latitude, p.longitude, p.speedKmh, p.accuracyM, p.altitudeM, p.baroAltitudeM),
                )
                gnssMonitor.onFix(p.accuracyM)
                liveSpeed = p.speedKmh
                liveAtMs = p.timeMs
            }
        }
        // Зоны без GPS: считаем при включении; пока идёт запись - обновляем раз в минуту.
        LaunchedEffect(gnssOn, autoRunning) {
            if (!gnssOn) {
                gnssZones = emptyList()
                return@LaunchedEffect
            }
            var first = true
            while (isActive) {
                val loaded = try {
                    GnssDeadZoneLocal.load(trackRepo)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
                if (loaded != null) gnssZones = loaded
                if (first && loaded != null && loaded.isEmpty()) {
                    android.widget.Toast.makeText(context, com.z_company.domain.gnss.GnssDeadZoneText.EMPTY, android.widget.Toast.LENGTH_SHORT).show()
                }
                first = false
                if (!autoRunning) break
                delay(60_000)
            }
        }
        LaunchedEffect(styleGen, styleReady, gnssOn, gnssZones) {
            if (!styleReady) return@LaunchedEffect
            val style = map?.style ?: return@LaunchedEffect
            val shown = if (gnssOn) gnssZones else emptyList()
            style.getSourceAs<GeoJsonSource>(GNSS_ZONES_SOURCE)?.setGeoJson(gnssZoneFeatures(shown))
            if (shown.isNotEmpty()) {
                style.getLayerAs<CircleLayer>(GNSS_ZONES_LAYER)
                    ?.setProperties(PropertyFactory.circleRadius(gnssRadiusExpression(shown.map { it.latitude }.average())))
            }
            if (!gnssOn && selection is TrainMapSelection.GnssZoneSel) selection = null
        }
        val drawnCount = remember { object { var value = 0 } }
        // Пройденный путь жирной линией: сегменты со скоростью, разрыв по времени линию прерывает.
        // Путь рисуется из ЛОКАЛЬНЫХ данных (Track.db + живые точки записи) и не зависит от интернета и рельсовой линии:
        // привязка к рельсам - улучшение; сбой привязки -> сырые точки. Слои переживают смену стиля (styleGen).
        LaunchedEffect(styleGen, styleReady, track, railState, factLines, livePoints.lastOrNull()?.timeMs) {
            if (!styleReady) return@LaunchedEffect
            // Привязка к ближайшему из путей (линия плеча + линии по факту) там, где отклонение ≤ 50 м; иначе сырые точки
            // («вне путей» - только если дальше 50 м от ЛЮБОГО известного пути; пунктир сверху).
            val railLine = (railState as? ServicePhaseAutoFillViewModel.RailLineResult.Ok)?.line
            val railLines = listOfNotNull(railLine) + factLines
            val raw = PathKeeper.withLive(track?.drawPoints.orEmpty(), livePoints)
            if (raw.isEmpty() && drawnCount.value > 0) return@LaunchedEffect
            val pts = try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    ActualRails.snapMarked(raw, railLines)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                raw.map { TrainMapRoute.SnappedPoint(it, true) }
            }
            val feats = ArrayList<Feature>(pts.size)
            val offFeats = ArrayList<Feature>()
            val noDataFeats = ArrayList<Feature>()
            for (i in 1 until pts.size) {
                val a = pts[i - 1]
                val b = pts[i]
                val dt = b.point.timeMs - a.point.timeMs
                if (dt > TrackAnalysis.GAP_MS) continue
                if (dt > TrackAnalysis.DATA_GAP_MS) {
                    noDataFeats += Feature.fromGeometry(
                        LineString.fromLngLats(
                            listOf(
                                Point.fromLngLat(a.point.longitude, a.point.latitude),
                                Point.fromLngLat(b.point.longitude, b.point.latitude),
                            ),
                        ),
                    )
                    continue
                }
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
            drawnCount.value = feats.size
            snappedLast = pts.lastOrNull()?.point
            map?.style?.getSourceAs<GeoJsonSource>(TRAIN_TRACK_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(feats))
            map?.style?.getSourceAs<GeoJsonSource>(TRAIN_OFF_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(offFeats))
            map?.style?.getSourceAs<GeoJsonSource>(TRAIN_NODATA_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(noDataFeats))
        }
        // Линия по факту: трек устойчиво ушёл (>15 м) от всех известных путей (соседний путь многопутного участка) ->
        // сервер строит линию по рельсам через via-точки трека (route/length, без изменения контракта); принимается,
        // только если проходит по точкам трека и не делает крупного объезда. Не удалось - остаётся прежнее поведение.
        LaunchedEffect(track, railState, livePoints.lastOrNull()?.timeMs) {
            val routeLine = (railState as? ServicePhaseAutoFillViewModel.RailLineResult.Ok)?.line ?: return@LaunchedEffect
            if (viewOnly || !hasRoute) return@LaunchedEffect
            val raw = PathKeeper.withLive(track?.drawPoints.orEmpty(), livePoints)
            val run = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                ActualRails.deviationRun(raw, listOf(routeLine) + factLines)
            } ?: return@LaunchedEffect
            val key = ActualRails.runKey(run)
            if (!triedFactRuns.add(key)) return@LaunchedEffect
            val res = loadRailLine(ActualRails.viaPoints(run))
            val line = (res as? ServicePhaseAutoFillViewModel.RailLineResult.Ok)?.line
            if (line != null && ActualRails.accept(run, line)) {
                factLines = ActualRails.withFactLine(factLines, line)
            }
            // Сбой/отказ: тот же участок не запрашиваем повторно; растущий участок (другой ключ) попробует снова.
        }
        // «Хвост»: пока «АВТО» пишет, жирная линия дорисовывается от последней точки трека до текущего положения
        // (трек прорежен и подгружается раз в 10 с, из-за чего линия отставала от маркера на 60-100 м).
        val railLineNow = (railState as? ServicePhaseAutoFillViewModel.RailLineResult.Ok)?.line
        LaunchedEffect(styleGen, styleReady, myFix, snappedLast, autoRunning, liveSpeed, factLines) {
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
            val pr = ActualRails.project(listOfNotNull(railLineNow) + factLines, endLat, endLon)?.projection
            if (pr != null && pr.offsetM <= TrainMapRoute.SNAP_TOLERANCE_M) {
                endLat = pr.latitude
                endLon = pr.longitude
            }
            src.setGeoJson(
                Feature.fromGeometry(
                    LineString.fromLngLats(listOf(Point.fromLngLat(last.longitude, last.latitude), Point.fromLngLat(endLon, endLat)))
                ).apply { addNumberProperty("speed", liveSpeed ?: last.speedKmh ?: -1.0) }
            )
        }
        LaunchedEffect(Unit) { displayZone = runCatching { loadZone() }.getOrDefault(displayZone) }
        // Флажок на карте: точка плана или ещё не рассчитанная выбранная точка.
        val activeTarget = draft ?: plan?.target
        LaunchedEffect(styleGen, styleReady, activeTarget) {
            if (!styleReady) return@LaunchedEffect
            val src = map?.style?.getSourceAs<GeoJsonSource>(TRAIN_PLAN_SOURCE) ?: return@LaunchedEffect
            val t = activeTarget
            if (t == null) src.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            else src.setGeoJson(Point.fromLngLat(t.longitude, t.latitude))
        }
        // Смена значка положения (настройки или быстрый выбор) - заново регистрируем картинку слоя.
        LaunchedEffect(styleGen, styleReady, positionIcon) {
            if (!styleReady) return@LaunchedEffect
            map?.style?.addImage(ME_ICON_IMAGE, renderPositionIcon(context, positionIcon, (positionIcon.mapHeightDp * density).toInt()))
        }
        // Смена шкалы (пассажирский поезд) - перекрасить линию пути и «хвост».
        LaunchedEffect(styleGen, styleReady, speedProfile) {
            if (!styleReady) return@LaunchedEffect
            val expr = speedColorExpression(speedProfile)
            map?.style?.getLayerAs<LineLayer>("train-map-track-layer")?.setProperties(PropertyFactory.lineColor(expr))
            map?.style?.getLayerAs<LineLayer>("train-map-tail-layer")?.setProperties(PropertyFactory.lineColor(expr))
        }
        // Начало и окончание пути (§6.9). Окончание - только когда запись не идёт (иначе конец - это текущее положение).
        LaunchedEffect(styleGen, styleReady, track, autoRunning) {
            if (!styleReady) return@LaunchedEffect
            val src = map?.style?.getSourceAs<GeoJsonSource>(TRAIN_ENDS_SOURCE) ?: return@LaunchedEffect
            val pts = track?.points.orEmpty()
            val feats = ArrayList<Feature>(2)
            pts.firstOrNull()?.let {
                feats += Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude)).apply { addStringProperty("icon", END_START_IMAGE) }
            }
            if (pts.size >= 2 && (viewOnly || !autoRunning)) {
                pts.last().let {
                    feats += Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude)).apply { addStringProperty("icon", END_FINISH_IMAGE) }
                }
            }
            src.setGeoJson(FeatureCollection.fromFeatures(feats))
        }
        // Маркер выбранной точки/остановки.
        LaunchedEffect(styleGen, styleReady, selection) {
            if (!styleReady) return@LaunchedEffect
            val src = map?.style?.getSourceAs<GeoJsonSource>(TRAIN_SEL_SOURCE) ?: return@LaunchedEffect
            when (val sel = selection) {
                is TrainMapSelection.PathPoint -> src.setGeoJson(Point.fromLngLat(sel.point.longitude, sel.point.latitude))
                is TrainMapSelection.Stop -> src.setGeoJson(Point.fromLngLat(sel.mark.longitude, sel.mark.latitude))
                is TrainMapSelection.Endpoint -> src.setGeoJson(Point.fromLngLat(sel.point.longitude, sel.point.latitude))
                is TrainMapSelection.GnssZoneSel -> src.setGeoJson(Point.fromLngLat(sel.zone.longitude, sel.zone.latitude))
                null -> src.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            }
        }
        // Тап: точка маршрута впереди (план прибытия, этап 5) → остановка (до 24 dp) → ближайшая точка пройденного пути (до 30 dp)
        // → иначе снять выбор. Точки позади - время и скорость пройденного, без плана.
        DisposableEffect(map) {
            val m = map
            val click = MapLibreMap.OnMapClickListener { latLng ->
                val tap = m!!.projection.toScreenLocation(latLng)
                val mpp = m.projection.getMetersPerPixelAtLatitude(latLng.latitude) * density
                val railLine = (railState as? ServicePhaseAutoFillViewModel.RailLineResult.Ok)?.line
                // Тап по флажку сохранённого плана - снова открыть карточку.
                plan?.let { pl ->
                    val fp = m.projection.toScreenLocation(LatLng(pl.target.latitude, pl.target.longitude))
                    if (Math.hypot((fp.x - tap.x).toDouble(), (fp.y - tap.y).toDouble()) <= 28 * density) {
                        planOpen = true
                        draft = null
                        return@OnMapClickListener true
                    }
                }
                val planTap = if (viewOnly || !hasRoute) null
                else ArrivalPlan.resolveTap(stops, railLine, ActualRails.alongRouteFix(railLine, factLines, myFix), latLng.latitude, latLng.longitude, mpp * 30.0, mpp * 24.0)
                if (planTap is ArrivalPlan.TapResult.Ahead) {
                    selection = null
                    planNote = null
                    draft = planTap.target
                    return@OnMapClickListener true
                }
                val t = track
                // Начало/окончание пути (значки 28 dp) - раньше остановок: значок лежит сверху.
                fun nearPx(lat: Double, lon: Double): Double {
                    val p = m.projection.toScreenLocation(LatLng(lat, lon))
                    return Math.hypot((p.x - tap.x).toDouble(), (p.y - tap.y).toDouble())
                }
                val endPts = t?.points.orEmpty()
                val finishShown = endPts.size >= 2 && (viewOnly || !autoRunning)
                val endHit = listOfNotNull(
                    endPts.firstOrNull()?.let { TrainMapSelection.Endpoint(it, true) },
                    if (finishShown) TrainMapSelection.Endpoint(endPts.last(), false) else null,
                ).minByOrNull { nearPx(it.point.latitude, it.point.longitude) }
                    ?.takeIf { nearPx(it.point.latitude, it.point.longitude) <= 24 * density }
                if (endHit != null) {
                    selection = endHit
                    return@OnMapClickListener true
                }
                val stopHit = t?.stops?.minByOrNull { st ->
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
                // Точки пишутся раз в 5 с: у линии (до 48 dp) берём ближайшую из двух соседних точек, иначе - ближайшую точку (30 dp).
                val hit = t?.let {
                    TrackAnalysis.nearestOnTrack(it.points, latLng.latitude, latLng.longitude, mpp * 30.0, mpp * 48.0)
                }
                if (hit != null) {
                    selection = TrainMapSelection.PathPoint(hit)
                    return@OnMapClickListener true
                }
                // Зона без GPS (пятно под линией): после пути и остановок, чтобы не мешать им.
                if (gnssOn) {
                    val zoneHit = gnssZoneAt(gnssZones, latLng.latitude, latLng.longitude, mpp * 24.0)
                    if (zoneHit != null) {
                        draft = null
                        planNote = null
                        selection = TrainMapSelection.GnssZoneSel(zoneHit)
                        return@OnMapClickListener true
                    }
                }
                // Положение вне путей / не определено: карточка планирования недоступна, с пояснением.
                if (planTap == ArrivalPlan.TapResult.OffRail || planTap == ArrivalPlan.TapResult.NoFix) {
                    selection = null
                    draft = null
                    planNote = if (planTap == ArrivalPlan.TapResult.OffRail) TrainMapPlanTexts.OFF_RAIL else TrainMapPlanTexts.NO_FIX
                    return@OnMapClickListener true
                }
                // Линии нет (не построилась): тап по станции плеча требует интернета - повторить построение или «Нет интернета».
                if (!viewOnly && hasRoute && railLine == null && railState is ServicePhaseAutoFillViewModel.RailLineResult.Failed &&
                    stops.any { TrackAnalysis.distanceM(latLng.latitude, latLng.longitude, it.latitude, it.longitude) <= mpp * 24.0 }
                ) {
                    networkGate.run { loadAttempt++ }
                    return@OnMapClickListener true
                }
                selection = null
                draft = null
                planNote = null
                false
            }
            m?.addOnMapClickListener(click)
            onDispose { m?.removeOnMapClickListener(click) }
        }
        // Пунктир, когда линия получена.
        LaunchedEffect(styleGen, styleReady, railState) {
            if (!styleReady) return@LaunchedEffect
            val line = (railState as? ServicePhaseAutoFillViewModel.RailLineResult.Ok)?.line ?: return@LaunchedEffect
            map?.style?.getSourceAs<GeoJsonSource>(TRAIN_ROUTE_SOURCE)?.setGeoJson(
                Feature.fromGeometry(LineString.fromLngLats(line.map { (lat, lon) -> Point.fromLngLat(lon, lat) }))
            )
        }
        // Камера охватывает маршрут (линию или станции), пройденный путь и текущее место; повторно — когда пришла линия,
        // трек или первая точка.
        LaunchedEffect(styleGen, styleReady, railState, myFix != null, track != null) {
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
            val pad = (48 * density).toInt()
            // Сверху плашка, справа кнопки, снизу панели.
            fitCameraTo(m, pts, pad, pad * 2, pad * 2, pad * 3, animate = true, userZoom = userZoom)
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
        // Положение для расчётов вдоль линии плеча: на «линии по факту» берётся проекция на линию плеча (не «вне путей»).
        val alongFix = remember(myFix, railLineNow, factLines) { ActualRails.alongRouteFix(railLineNow, factLines, myFix) }
        val liveFreshTop = liveSpeed != null && nowMs - liveAtMs < 15_000
        val progress = remember(stops, railLineNow, alongFix) {
            if (viewOnly || !hasRoute) null else TrainMapRoute.progress(stops, railLineNow, alongFix)
        }
        val etaSpeed = remember(progress, liveSpeed, liveFreshTop, track) {
            TrainMapRoute.etaSpeedKmh(if (liveFreshTop) liveSpeed else null, track?.points.orEmpty(), System.currentTimeMillis())
        }
        // Вне путей: текущее положение дальше 50 м от рельсовой линии маршрута.
        val offNow = remember(myFix, railLineNow, factLines) {
            val f = myFix
            if (viewOnly || f == null || railLineNow == null) false
            else (ActualRails.offsetFromAny(listOf(railLineNow) + factLines, f.latitude, f.longitude) ?: 0.0) > TrainMapRoute.SNAP_TOLERANCE_M
        }

        // Планирование прибытия: расстояние до точки плана по линии пересчитывается раз в ~5 с (тик nowMs) и с первым положением.
        val planTarget = draft ?: plan?.target
        val planDistance = remember(planTarget, railLineNow, nowMs, alongFix != null) {
            planTarget?.let { ArrivalPlan.distance(railLineNow, alongFix, it) }
        }
        // Точка достигнута/пройдена, не лежит на линии (сменили плечо) или план устарел - снять.
        LaunchedEffect(planDistance, plan, nowMs) {
            val d = planDistance
            val p = plan
            if (p != null && (ArrivalPlan.isStale(p, System.currentTimeMillis()) ||
                    (d == ArrivalPlan.PlanDistance.Arrived && draft == null) || d == ArrivalPlan.PlanDistance.TargetNotOnLine && draft == null)
            ) {
                plan = null
                TrainMapPlanStore.clear(context, planKey)
            }
            if (draft != null && (d == ArrivalPlan.PlanDistance.Arrived || d == ArrivalPlan.PlanDistance.TargetNotOnLine)) draft = null
        }

        // Сеть вернулась, а линия из кэша / не построена / ещё строится - перезапрашиваем автоматически (плашка исчезнет сама).
        var wasOnline by remember { mutableStateOf(online) }
        LaunchedEffect(online) {
            val rs = railState
            val cachedOrFailed = rs == null || (rs is ServicePhaseAutoFillViewModel.RailLineResult.Ok && rs.fromCache) ||
                rs is ServicePhaseAutoFillViewModel.RailLineResult.Failed
            if (hasRoute && RailRefetch.shouldRefetch(wasOnline, online, cachedOrFailed)) loadAttempt++
            wasOnline = online
        }

        // Возврат в слежение: через FollowReturn.RETURN_DELAY_MS без касаний камера снова ведётся за положением
        // (и «еду по карте», если было включено). Не возвращаем: открыта карточка планирования/выбранной точки/график/панель
        // значка, палец на карте, просмотр записанного трека.
        val followBlocked = selection != null || showChart || showIconPicker || draft != null || planNote != null ||
            (plan != null && planOpen)
        LaunchedEffect(followBlocked) { lastInteractionMs = System.currentTimeMillis() }
        LaunchedEffect(follow, followBlocked, fingerDown, lastInteractionMs, viewOnly) {
            if (follow || viewOnly || followBlocked || fingerDown) return@LaunchedEffect
            delay(FollowReturn.remainingMs(System.currentTimeMillis(), lastInteractionMs))
            if (!FollowReturn.shouldReturn(follow, viewOnly, followBlocked, fingerDown, System.currentTimeMillis(), lastInteractionMs)) {
                return@LaunchedEffect
            }
            follow = true
            val wasRide = rideBeforeGesture
            rideBeforeGesture = false
            ride = wasRide
            val m = map
            val f = myFix
            if (m != null && f != null && alive.get()) {
                val zoom = zoomBeforeGesture?.coerceAtLeast(12.0) ?: maxOf(m.cameraPosition.zoom, 14.0)
                val h = headingDeg
                val cam = CameraPosition.Builder().target(LatLng(f.latitude, f.longitude)).zoom(zoom)
                    .bearing(if (wasRide && h != null) h else m.cameraPosition.bearing).tilt(0.0).build()
                m.animateCamera(CameraUpdateFactory.newCameraPosition(cam), 600)
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
            val zone = track?.zone ?: displayZone
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 12.dp, end = 72.dp, top = 64.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Блок «Следующая / Конечная» - только когда позиция на пути следования (не дальше 50 м от линии).
                if (progress != null && !offNow) {
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
                if (offNow) {
                    Text(
                        text = "Вне путей",
                        style = Pst.sans.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                        color = Pst.text,
                        maxLines = 1,
                        modifier = Modifier
                            .shadow(4.dp, RoundedCornerShape(12.dp))
                            .background(Pst.warning, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
                if (!viewOnly) {
                    // Связь со спутниками (§6.9): единственное место индикатора; считается по живым обновлениям карты.
                    val gnss by gnssMonitor.quality.collectAsState()
                    Box(
                        modifier = Modifier
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .background(Pst.surface, RoundedCornerShape(16.dp)),
                    ) { com.z_company.route.autorecord.GnssSignalIndicator(quality = gnss, labelColor = Pst.textMuted) }
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
                            is TrainMapSelection.Endpoint -> {
                                Text(
                                    text = (if (sel.isStart) "Старт " else "Финиш ") + TrackAnalysis.clock(sel.point.timeMs, zone, seconds = true),
                                    style = Pst.mono.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                                    color = Pst.text,
                                )
                                Text(
                                    text = (sel.point.speedKmh?.let { "${it.toInt()} км/ч" } ?: "скорость неизвестна"),
                                    style = Pst.mono.copy(fontSize = 14.sp),
                                    color = Pst.textMuted,
                                )
                            }
                            is TrainMapSelection.GnssZoneSel -> {
                                val z = sel.zone
                                val t = com.z_company.domain.gnss.GnssDeadZoneText
                                Text(
                                    text = t.TITLE,
                                    style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                    color = Pst.text,
                                    maxLines = 2,
                                )
                                Text(
                                    text = listOf(
                                        t.trips(z.tracks),
                                        t.kinds(z.kinds) + " (" + t.duration(z.maxDurationMs) + ")",
                                        "Последний раз " + t.date(z.lastMs, zone),
                                    ).joinToString("\n"),
                                    style = Pst.mono.copy(fontSize = 13.sp, lineHeight = 18.sp),
                                    color = Pst.textMuted,
                                )
                                Text(
                                    text = t.NOTE,
                                    style = Pst.sans.copy(fontSize = 11.sp, lineHeight = 14.sp),
                                    color = Pst.textMuted,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            is TrainMapSelection.Stop -> {
                                val mk = sel.mark
                                Text(
                                    text = stopTitle(mk, stops),
                                    style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                    color = Pst.text,
                                    maxLines = 2,
                                )
                                val parts = buildList {
                                    mk.arrivalMs?.let { add("Прибытие " + TrackAnalysis.clock(it, zone)) }
                                    mk.departureMs?.let { add("Отправление " + TrackAnalysis.clock(it, zone)) }
                                    val a = mk.arrivalMs
                                    val d = mk.departureMs
                                    if (a != null && d != null && d > a) add("стоянка ${TrackAnalysis.dwellMinutes(a, d)} мин")
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
                val bearingNorm = ((mapBearing % 360.0) + 540.0) % 360.0 - 180.0
                if (abs(bearingNorm) > 0.5) {
                    MapButton(
                        onClick = {
                            ride = false
                            map?.let { mm ->
                                mm.animateCamera(
                                    CameraUpdateFactory.newCameraPosition(CameraPosition.Builder(mm.cameraPosition).bearing(0.0).tilt(0.0).build()),
                                    400,
                                )
                            }
                        },
                        description = "Север сверху",
                    ) { CompassNeedle(bearingNorm.toFloat()) }
                }
                if (!viewOnly || track?.points.orEmpty().isNotEmpty()) {
                    MapButton(onClick = { showChart = true }, description = "График скорости") {
                        PstIcon(PstIcons.SpeedChart, 22.dp, Pst.text)
                    }
                }
                // «Зоны без GPS»: красноватые пятна по локальным трекам; включено - акцентная кнопка.
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .background(if (gnssOn) Pst.accent else Pst.surface, RoundedCornerShape(16.dp))
                        .clickable(onClickLabel = com.z_company.domain.gnss.GnssDeadZoneText.TOGGLE) {
                            gnssOn = !gnssOn
                            GnssDeadZonePrefs.set(context, gnssOn)
                        },
                    contentAlignment = Alignment.Center,
                ) { PstIcon(PstIcons.GnssZones, 22.dp, if (gnssOn) androidx.compose.ui.graphics.Color.White else Pst.text) }
                if (!viewOnly && onToggleRecording != null) {
                    // Кнопка записи трека (= «АВТО»): запись идёт - красный квадрат «стоп» и пульсирующая красная точка.
                    val recDesc = if (autoRecording) "Остановить запись" else "Начать запись"
                    MapButton(onClick = onToggleRecording, description = recDesc) { RecordIcon(autoRecording) }
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
                // Нажатие мимо панели (карта, любая кнопка, системный «Назад») закрывает её, не передаваясь дальше.
                BackHandler { showIconPicker = false }
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showIconPicker = false }
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 72.dp)
                        .offset(y = maxHeight * 0.3f)
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
                    profile = speedProfile,
                    loadRecordingState = loadRecordingState,
                    makeTarget = makeTarget,
                    loadConflictText = loadConflictText,
                    onEnableAuto = onEnableAuto,
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

            // Карточка планирования прибытия (этап 5): ввод времени / результат расчёта / «недоступно» с пояснением.
            val planCardVisible = !viewOnly && ((planTarget != null && (draft != null || planOpen)) || planNote != null)
            val planChipVisible = !viewOnly && draft == null && plan != null && !planOpen && planNote == null
            if (planCardVisible) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                        .background(Pst.surface, RoundedCornerShape(20.dp))
                        .heightIn(max = maxHeight * 0.7f)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                ) {
                    val d = planDistance
                    if (planTarget != null && planNote == null) {
                        ArrivalPlanCard(
                            target = planTarget,
                            plan = if (draft == null) plan else null,
                            distanceM = (d as? ArrivalPlan.PlanDistance.Ahead)?.meters,
                            note = when (d) {
                                ArrivalPlan.PlanDistance.OffRail -> TrainMapPlanTexts.OFF_RAIL
                                ArrivalPlan.PlanDistance.NoFix -> TrainMapPlanTexts.NO_FIX
                                ArrivalPlan.PlanDistance.NoLine -> TrainMapPlanTexts.NO_LINE
                                else -> null
                            },
                            nowMs = nowMs,
                            zone = zone,
                            speedKmh = etaSpeed,
                            onSave = { ms ->
                                val t = draft ?: plan?.target
                                if (t != null) {
                                    val np = ArrivalPlanData(t, ms)
                                    plan = np
                                    TrainMapPlanStore.save(context, planKey, np)
                                    draft = null
                                    planOpen = true
                                }
                            },
                            onReset = {
                                plan = null
                                draft = null
                                TrainMapPlanStore.clear(context, planKey)
                            },
                            onClose = {
                                if (draft != null) draft = null else planOpen = false
                            },
                        )
                    } else {
                        Text(planNote.orEmpty(), style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 18.sp), color = Pst.text)
                        PstButton(
                            TrainMapPlanTexts.CLOSE, onClick = { planNote = null },
                            kind = PstButtonKind.TEXT, height = 40.dp, fontSize = 14,
                        )
                    }
                }
            } else if (planChipVisible) {
                val pl = plan
                if (pl != null) {
                    Text(
                        text = "План прибытия: " + (pl.target.name?.let { "$it · " } ?: "") + TrackAnalysis.clock(pl.plannedMs, zone),
                        style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        color = Pst.text,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .navigationBarsPadding()
                            .padding(16.dp)
                            .shadow(4.dp, RoundedCornerShape(14.dp))
                            .background(Pst.surface, RoundedCornerShape(14.dp))
                            .clickable { planOpen = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            val name = missing
            val rail = railState
            if (hasRoute && !(planCardVisible || planChipVisible) && (rail !is ServicePhaseAutoFillViewModel.RailLineResult.Ok ||
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
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Маршрут показан из сохранённых данных",
                                    style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                    color = Pst.text,
                                )
                                if (RailRefetch.showOfflineCaption(online)) {
                                    Text(
                                        text = NetworkRequired.TITLE,
                                        style = Pst.sans.copy(fontSize = 12.sp),
                                        color = Pst.textMuted,
                                    )
                                }
                            }
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
                                    onClick = { networkGate.run { loadAttempt++ } },
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

/** Иконка компаса: красная стрелка указывает на север карты (поворачивается против поворота карты). */
@Composable
private fun CompassNeedle(bearingDeg: Float) {
    val north = androidx.compose.ui.graphics.Color(0xFFE53935)
    val south = Pst.textMuted
    androidx.compose.foundation.Canvas(Modifier.size(26.dp).rotate(-bearingDeg)) {
        val c = center
        val h = size.height / 2f
        val w = size.width * 0.2f
        val up = androidx.compose.ui.graphics.Path().apply {
            moveTo(c.x, c.y - h); lineTo(c.x + w, c.y); lineTo(c.x - w, c.y); close()
        }
        val down = androidx.compose.ui.graphics.Path().apply {
            moveTo(c.x, c.y + h); lineTo(c.x + w, c.y); lineTo(c.x - w, c.y); close()
        }
        drawPath(down, south)
        drawPath(up, north)
    }
}

/**
 * Подпись остановки: название станции поезда; иначе станция плеча в пределах 1 км (если плечо есть);
 * иначе «Стоянка N мин» (по времени прибытия/отправления из трека), иначе «Остановка».
 */
private fun stopTitle(mk: com.z_company.domain.track.TrainStopMark, routeStops: List<com.z_company.domain.util.TrainMapStop>): String {
    mk.name?.takeIf { it.isNotBlank() }?.let { return it }
    routeStops
        .minByOrNull { TrackAnalysis.distanceM(mk.latitude, mk.longitude, it.latitude, it.longitude) }
        ?.takeIf { TrackAnalysis.distanceM(mk.latitude, mk.longitude, it.latitude, it.longitude) <= com.z_company.domain.autorecord.StopDetectionConfig.STATION_RADIUS_M && it.name.isNotBlank() }
        ?.let { return it.name }
    val a = mk.arrivalMs
    val d = mk.departureMs
    return if (a != null && d != null && d > a) "Стоянка ${TrackAnalysis.dwellMinutes(a, d)} мин" else "Остановка"
}

/** Иконка кнопки записи: кольцо с красной точкой (запись выключена) / красный квадрат «стоп» с пульсирующей точкой-бейджем. */
@Composable
private fun RecordIcon(recording: Boolean) {
    val red = androidx.compose.ui.graphics.Color(0xFFE53935)
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        if (recording) {
            Box(Modifier.size(14.dp).background(red, RoundedCornerShape(3.dp)))
            val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "rec_pulse")
            val a by t.animateFloat(
                initialValue = 1f,
                targetValue = 0.25f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    androidx.compose.animation.core.tween(900, easing = androidx.compose.animation.core.LinearEasing),
                    androidx.compose.animation.core.RepeatMode.Reverse,
                ),
                label = "rec_pulse_alpha",
            )
            Box(
                Modifier.align(Alignment.TopEnd).size(8.dp)
                    .alpha(a).background(red, CircleShape),
            )
        } else {
            Box(Modifier.size(22.dp).border(2.dp, Pst.text, CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(10.dp).background(red, CircleShape))
            }
        }
    }
}

/**
 * Подписи базовой карты (symbol-слои стиля: станции, населённые пункты) на экране - для отсева дублей своих подписей.
 * Слои приложения (`train-map-*`) не учитываются. Только точечные объекты с названием.
 */
private fun baseMapLabels(m: MapLibreMap, width: Int, height: Int): List<BaseMapLabel> {
    val style = m.style ?: return emptyList()
    if (width <= 0 || height <= 0) return emptyList()
    return try {
        val ids = style.layers.filter { it is SymbolLayer && !it.id.startsWith("train-map-") }.map { it.id }.toTypedArray()
        if (ids.isEmpty()) return emptyList()
        m.queryRenderedFeatures(android.graphics.RectF(0f, 0f, width.toFloat(), height.toFloat()), *ids).mapNotNull { f ->
            val pt = f.geometry() as? Point ?: return@mapNotNull null
            val name = listOf("name:ru", "name").firstNotNullOfOrNull { k ->
                if (f.hasProperty(k)) f.getStringProperty(k)?.takeIf { it.isNotBlank() } else null
            } ?: return@mapNotNull null
            val sp = m.projection.toScreenLocation(LatLng(pt.latitude(), pt.longitude()))
            BaseMapLabel(sp.x.toDouble(), sp.y.toDouble(), name)
        }
    } catch (e: Exception) {
        emptyList()
    }
}
