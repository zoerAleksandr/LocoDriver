package com.z_company.route.component

import com.z_company.repository.remote_rest.response.OsmStationCandidate

import com.z_company.route.component.servicephase.StationChoiceSheet

import kotlinx.coroutines.flow.collectLatest

import kotlinx.coroutines.flow.drop

import androidx.compose.runtime.snapshotFlow

import com.z_company.route.viewmodel.message

import androidx.compose.foundation.layout.Spacer

import androidx.compose.ui.text.style.TextAlign

import com.z_company.domain.util.toMoneyString

import com.z_company.domain.util.roundMoneyToCents

import com.z_company.domain.util.currencySymbol

import org.koin.compose.koinInject

import com.z_company.domain.util.ServicePhaseStationsMerge

import com.z_company.domain.util.StationDirectory

import com.z_company.domain.use_cases.StationDirectoryUseCase

import com.z_company.route.viewmodel.AutoFillFailure
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.z_company.route.component.servicephase.RouteMapStop
import com.z_company.route.component.servicephase.PhaseRouteMap
import com.z_company.domain.entities.setting.ServicePhaseStationSource
import com.z_company.domain.entities.setting.ServicePhasePoint
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.z_company.route.viewmodel.ServicePhaseAutoFillViewModel
import com.z_company.route.viewmodel.PhaseEnd
import com.z_company.route.component.servicephase.PhaseAutoFillSheet
import com.z_company.route.component.servicephase.PhaseRouteVariantsMap
import com.z_company.route.viewmodel.RouteVariant
import com.z_company.route.viewmodel.RouteVia
import com.z_company.route.viewmodel.addableStations
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.ServicePhaseStation
import com.z_company.domain.entities.setting.ServicePhaseStationKind
import com.z_company.domain.util.toIntOrZero
import com.z_company.route.component.servicephase.PhaseInsertRow
import com.z_company.route.component.servicephase.PhaseStationSheet
import com.z_company.route.component.servicephase.PhaseStopRow
import com.z_company.route.component.servicephase.Pst
import com.z_company.route.component.servicephase.PstButton
import com.z_company.route.component.servicephase.PstButtonKind
import com.z_company.route.component.servicephase.PstCard
import com.z_company.route.component.servicephase.PstGroupHead
import com.z_company.route.component.servicephase.PstIcon
import com.z_company.route.component.servicephase.PstIcons
import com.z_company.route.component.servicephase.PstNote
import com.z_company.route.component.servicephase.PstSheetFooter
import com.z_company.route.component.servicephase.PstTextField
import com.z_company.route.component.servicephase.SchemeMode
import com.z_company.route.component.servicephase.StopType
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private const val KEY_DEPARTURE = "stop:departure"
private const val KEY_ARRIVAL = "stop:arrival"

/**
 * Шторка создания и редактирования плеча (поверх «Плечи» — в Настройках и в форме поезда).
 * Макет — design/service_phase_stations (экраны 02–08). Автосохранение, как на других экранах:
 * изменения сохраняются через ~0,6 с после правки и при закрытии («назад»); кнопки «Сохранить» нет.
 * Новое плечо появляется, когда заполнены станции и расстояние; обратное — при закрытии.
 * Бизнес-логика (сохранение в UserSettings) — снаружи через колбэки.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoulderEditBottomSheet(
    phase: ServicePhase?,
    stationList: List<String>,
    onSave: (phase: ServicePhase, createReverse: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDeleteStationName: (String) -> Unit = {},
    onDismiss: () -> Unit,
    /** Страна пользователя (UserSettings.country) — для автоподбора. */
    country: String = "RU",
    /** Новые названия для справочника станций (из автоподбора) — вызывается при сохранении. */
    onAddStationNames: (List<String>) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val isNew = phase == null
    // Координаты станций — из справочника (единый источник); в плече — запасная копия.
    val stationDirectory: StationDirectoryUseCase = koinInject()
    @Suppress("NAME_SHADOWING")
    val phase = remember { phase?.let(stationDirectory::resolve) }
    val directoryPoints = remember { StationDirectory.byName(stationDirectory.stations()) }
    /** Точка крайней станции: заданная в редакторе для этого названия, иначе — из справочника. */
    fun pointFor(own: Pair<ServicePhasePoint?, String>?, name: String): ServicePhasePoint? =
        own?.takeIf { sameName(it.second, name) }?.first
            ?: directoryPoints[ServicePhaseStationsMerge.normalizeName(name)]?.let {
                ServicePhasePoint(it.latitude!!, it.longitude!!, osmId = it.osmId, esr = it.esr)
            }

    var departure by remember { mutableStateOf(textValue(phase?.departureStation)) }
    var arrival by remember { mutableStateOf(textValue(phase?.arrivalStation)) }
    var distance by remember { mutableStateOf(TextFieldValue(phase?.distance?.takeIf { it > 0 }?.toString() ?: "")) }
    var rate by remember {
        mutableStateOf(TextFieldValue(phase?.linearMileageRate?.takeIf { it > 0.0 }?.toString()?.replace('.', ',') ?: ""))
    }
    var createReverse by remember { mutableStateOf(true) }
    val stations = remember { mutableStateListOf<ServicePhaseStation>().apply { addAll(phase?.intermediateStations.orEmpty()) } }
    var editMode by remember { mutableStateOf(false) }
    var confirmDeletePhase by remember { mutableStateOf(false) }

    // Шторка станции: editing — существующая; insertAt — позиция новой (null = по км / в конец)
    var stationSheetOpen by remember { mutableStateOf(false) }
    var showRouteMap by remember { mutableStateOf(false) }

    // «Измерено по карте»: расстояние и координаты станций, по которым оно измерено — поле плеча
    // `measuredSignature` (синхронизируется). Прежнее локальное хранилище читаем один раз (миграция)
    // и очищаем при первом сохранении плеча.
    val context = LocalContext.current
    val measuredPrefs = remember { context.getSharedPreferences("service_phase_measured", android.content.Context.MODE_PRIVATE) }
    var measured by remember {
        mutableStateOf(
            phase?.let { p ->
                (p.measuredSignature ?: measuredPrefs.getString(p.id, null))?.let(MeasuredDistance::parse)
            }
        )
    }
    var measuring by remember { mutableStateOf(false) }
    val measureScope = rememberCoroutineScope()
    /** Открыта шторка крайней станции: true — отправление, false — прибытие. */
    var editingTerminal by remember { mutableStateOf<Boolean?>(null) }
    var editingStation by remember { mutableStateOf<ServicePhaseStation?>(null) }
    var insertAt by remember { mutableStateOf<Int?>(null) }

    var snackbar by remember { mutableStateOf<EditorSnackbar?>(null) }

    // Автоподбор: координаты крайних станций запоминаются вместе с названием, к которому относятся.
    val autoVm: ServicePhaseAutoFillViewModel = viewModel()
    val autoState by autoVm.state.collectAsState()
    var autoFillOpen by remember { mutableStateOf(false) }
    var depPoint by remember { mutableStateOf(phase?.departurePoint?.let { it to phase.departureStation }) }
    var arrPoint by remember { mutableStateOf(phase?.arrivalPoint?.let { it to phase.arrivalStation }) }
    var pendingNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var freshIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val departureFocus = remember { FocusRequester() }
    val arrivalFocus = remember { FocusRequester() }
    LaunchedEffect(freshIds) {
        if (freshIds.isNotEmpty()) {
            delay(2_000)
            freshIds = emptySet()
        }
    }
    val canAutoFill = departure.text.isNotBlank() && arrival.text.isNotBlank()
    fun startAutoFill() {
        val dep = departure.text.trim()
        val arr = arrival.text.trim()
        autoVm.start(
            departure = dep,
            arrival = arr,
            country = country,
            departureOsmId = pointFor(depPoint, dep)?.osmId,
            arrivalOsmId = pointFor(arrPoint, arr)?.osmId,
            // Точка, указанная пользователем, — от неё и путь, и км промежуточных (как «Измерить по карте»).
            departurePoint = pointFor(depPoint, dep)?.let { it.latitude to it.longitude },
            arrivalPoint = pointFor(arrPoint, arr)?.let { it.latitude to it.longitude },
        )
        autoFillOpen = true
    }
    fun closeAutoFill() {
        autoVm.cancel()
        autoFillOpen = false
    }

    /** Координаты станций плеча по порядку — «подпись», по которой проверяется «измерено по карте». */
    fun coordinateSignature(): String {
        val dep = departure.text.trim()
        val arr = arrival.text.trim()
        val points = buildList {
            add(pointFor(depPoint, dep)?.let { it.latitude to it.longitude })
            stations.filter { it.hasCoordinates }.forEach { add(it.latitude!! to it.longitude!!) }
            add(pointFor(arrPoint, arr)?.let { it.latitude to it.longitude })
        }
        return points.joinToString(";") { p -> p?.let { "%.5f,%.5f".format(java.util.Locale.US, it.first, it.second) } ?: "-" }
    }

    /** Выбор крайней станции из неточных вариантов (сокращение) во время «Измерить по карте». */
    var choosingTerminal by remember { mutableStateOf<TerminalChoice?>(null) }
    /** Варианты маршрута (route/length, alternatives) — экран выбора на карте; null — не показан. */
    var variantChoice by remember { mutableStateOf<VariantChoice?>(null) }
    /** Уже загруженные варианты для текущих точек: «Карта» не должна повторять сетевой запрос. */
    var cachedVariantChoice by remember { mutableStateOf<CachedVariantChoice?>(null) }
    /** Состояние до временного добавления via-точки; возвращается при закрытии предпросмотра. */
    var pendingCustomRoute by remember { mutableStateOf<PendingCustomRoute?>(null) }
    /** Ошибка построения своего маршрута: панель выбора станций открывается снова с тем же списком. */
    var customRetry by remember { mutableStateOf<CustomRetry?>(null) }

    /** Своя трасса не построилась: прежние станции возвращаются, вернуться в панель с ошибкой (или снекбар). */
    fun failCustomRoute(message: String) {
        val pending = pendingCustomRoute ?: return
        stations.clear()
        stations.addAll(pending.stationsBefore)
        pendingCustomRoute = null
        if (pending.choiceBefore != null) {
            customRetry = CustomRetry(pending.chosen, message)
            variantChoice = pending.choiceBefore
        } else {
            snackbar = EditorSnackbar(message)
        }
    }

    fun notifyAutoFound(autoFound: List<Pair<String, Boolean>>) {
        if (autoFound.isEmpty()) return
        val names = autoFound.joinToString(" и ") { "«${it.first}»" }
        snackbar = EditorSnackbar(
            text = if (autoFound.size == 1) {
                "Координаты станции $names определены автоматически — проверьте, что точка верная"
            } else {
                "Координаты станций $names определены автоматически — проверьте, что точки верные"
            },
            undo = { editingTerminal = autoFound.first().second },
            actionLabel = "Проверить",
            durationMs = 12_000,
        )
    }

    /** «Добавить станции маршрута»: станции выбранного варианта → промежуточные станции плеча (без дублей). */
    fun addVariantStations(vias: List<RouteVia>) {
        val found = vias.map { v ->
            ServicePhaseStation(
                name = v.name,
                latitude = v.latitude,
                longitude = v.longitude,
                kmFromStart = v.km,
                kind = ServicePhaseStationKind.STATION,
                esr = v.esr?.takeIf(String::isNotBlank),
                osmId = v.osmId,
                source = ServicePhaseStationSource.OSM,
            )
        }
        val (merged, added) = ServicePhaseStationsMerge.merge(stations.toList(), found, found.map { it.id }.toSet())
        stations.clear()
        stations.addAll(merged)
        freshIds = added
        // Эти точки входят в только что выбранный и измеренный сервером маршрут. После изменения
        // списка станций подпись нужно собрать заново, иначе чип «Измерено» исчезнет при автосохранении.
        measured = MeasuredDistance(distance.text.toIntOrZero(), coordinateSignature())
    }

    /** «Построить другой маршрут»: выбранная существующая станция сразу становится via-точкой. */
    /** Свой маршрут: список промежуточных станций заменяется выбранными — по порядку, без дублей. */
    fun setCustomRouteVias(candidates: List<OsmStationCandidate>) {
        val vias = candidates.distinctBy { it.osmId }.map { candidate ->
            ServicePhaseStation(
                name = candidate.name,
                latitude = candidate.latitude,
                longitude = candidate.longitude,
                kind = if (candidate.kind == "halt") ServicePhaseStationKind.HALT else ServicePhaseStationKind.STATION,
                esr = candidate.esr?.takeIf(String::isNotBlank),
                osmId = candidate.osmId,
                source = ServicePhaseStationSource.OSM,
            )
        }
        stations.clear()
        stations.addAll(vias)
    }

    /** «Выбрать маршрут»: расстояние плеча = длина варианта; станции варианта — по предложению. */
    fun applyVariant(choice: VariantChoice, v: RouteVariant) {
        val km = kotlin.math.round(v.lengthKm).toInt()
        distance = TextFieldValue(km.toString(), TextRange(km.toString().length))
        stations.filter { it.hasCoordinates }.forEachIndexed { i, station ->
            val kmAt = choice.kmAt.getOrNull(i + 1) ?: return@forEachIndexed
            val index = stations.indexOfFirst { it.id == station.id }
            if (index >= 0) stations[index] = stations[index].copy(kmFromStart = kmAt)
        }
        measured = MeasuredDistance(km, coordinateSignature())
        val addable = v.addableStations()
        val shortest = v.id == choice.variants.first().id
        when {
            // Кратчайший — это и есть путь, по которому дальше ходит замер без станций.
            shortest -> notifyAutoFound(choice.autoFound)
            addable.isNotEmpty() -> snackbar = EditorSnackbar(
                text = "Маршрут $km км выбран. Чтобы замер шёл этим путём, добавьте станции маршрута",
                actionLabel = "Добавить станции маршрута",
                durationMs = 12_000,
                undo = { addVariantStations(addable) },
            )
            else -> snackbar = EditorSnackbar(
                "Маршрут $km км выбран. Чтобы замер шёл этим путём, добавьте промежуточные станции",
                durationMs = 10_000,
            )
        }
    }

    fun measureByMap(
        previewSingleRoute: Boolean = false,
        fallbackToCachedMap: Boolean = false,
    ) {
        val dep = departure.text.trim()
        val arr = arrival.text.trim()
        if (dep.isBlank() || arr.isBlank()) {
            snackbar = EditorSnackbar("Укажите станции отправления и прибытия")
            return
        }
        measuring = true
        measureScope.launch {
            // Крайние станции без координат — сначала ищем на карте по названию (как «Найти координаты»).
            val autoFound = mutableListOf<Pair<String, Boolean>>() // название, true — отправление
            for (isStart in listOf(true, false)) {
                val name = if (isStart) dep else arr
                if (pointFor(if (isStart) depPoint else arrPoint, name) != null) continue
                val near = buildList {
                    pointFor(if (isStart) arrPoint else depPoint, if (isStart) arr else dep)
                        ?.let { add(it.latitude to it.longitude) }
                    stations.filter { it.hasCoordinates }.forEach { add(it.latitude!! to it.longitude!!) }
                }
                val found = autoVm.lookup(name, country, near)
                if (found == null) {
                    measuring = false
                    val why = autoVm.lastLookupFailure?.message() ?: AutoFillFailure.SERVICE.message()
                    snackbar = EditorSnackbar("Не удалось найти «$name» на карте. $why")
                    return@launch
                }
                if (found.isEmpty()) {
                    measuring = false
                    snackbar = EditorSnackbar(
                        "«$name» не нашлась на карте — укажите её координаты в схеме. Если это не станция " +
                            "(пути необщего пользования, терминал) — точку на карте вручную.",
                        durationMs = 10_000,
                    )
                    return@launch
                }
                if (found.any { it.approximate }) {
                    // Сокращение — пусть выберет сам; после выбора замер продолжится.
                    measuring = false
                    choosingTerminal = TerminalChoice(isStart, name, found)
                    return@launch
                }
                val c = found.first()
                val point = ServicePhasePoint(c.latitude, c.longitude, osmId = c.osmId, esr = c.esr?.takeIf(String::isNotBlank))
                if (isStart) depPoint = point to dep else arrPoint = point to arr
                autoFound += name to isStart
            }
            val depP = pointFor(depPoint, dep)
            val arrP = pointFor(arrPoint, arr)
            if (depP == null || arrP == null) {
                measuring = false
                return@launch
            }
            val withCoords = stations.filter { it.hasCoordinates }
            val points = listOf(depP.latitude to depP.longitude) +
                withCoords.map { it.latitude!! to it.longitude!! } +
                listOf(arrP.latitude to arrP.longitude)
            // Варианты маршрута — только пока нет промежуточных станций (со станциями путь один, строго через них).
            val res = autoVm.measure(
                points,
                withVariants = stations.isEmpty(),
                withGeometry = stations.isEmpty() || previewSingleRoute,
            )
            measuring = false
            when (res) {
                is ServicePhaseAutoFillViewModel.MeasureResult.Ok -> {
                    if (res.variants.isNotEmpty() && (res.variants.size > 1 || previewSingleRoute)) {
                        val previewVariants = if (previewSingleRoute && withCoords.isNotEmpty()) {
                            res.variants.map { variant ->
                                if (variant.via.isNotEmpty()) variant else variant.copy(
                                    via = withCoords.mapIndexed { index, station ->
                                        RouteVia(
                                            name = station.name,
                                            km = res.kmAt.getOrNull(index + 1) ?: station.kmFromStart ?: 0.0,
                                            osmId = station.osmId,
                                            esr = station.esr,
                                            latitude = station.latitude,
                                            longitude = station.longitude,
                                        )
                                    }
                                )
                            }
                        } else {
                            res.variants
                        }
                        // Экран выбора; пока не выбрано — ничего не применяется.
                        val choice = VariantChoice(dep, arr, previewVariants, autoFound.toList(), res.kmAt)
                        variantChoice = choice
                        if (stations.isEmpty() && previewVariants.size > 1) {
                            cachedVariantChoice = CachedVariantChoice(coordinateSignature(), choice)
                        }
                        return@launch
                    }
                    if (previewSingleRoute) {
                        if (pendingCustomRoute != null) failCustomRoute("Не удалось показать построенный маршрут на карте")
                        else snackbar = EditorSnackbar("Не удалось показать построенный маршрут на карте")
                        return@launch
                    }
                    val km = kotlin.math.round(res.lengthKm).toInt()
                    distance = TextFieldValue(km.toString(), TextRange(km.toString().length))
                    // Км промежуточных станций — по тому же пути.
                    withCoords.forEachIndexed { i, st ->
                        val kmAt = res.kmAt.getOrNull(i + 1) ?: return@forEachIndexed
                        val idx = stations.indexOfFirst { it.id == st.id }
                        if (idx >= 0) stations[idx] = stations[idx].copy(kmFromStart = kmAt)
                    }
                    measured = MeasuredDistance(km, coordinateSignature())
                    notifyAutoFound(autoFound)
                }
                ServicePhaseAutoFillViewModel.MeasureResult.NoPath -> {
                    if (previewSingleRoute && pendingCustomRoute != null) {
                        failCustomRoute("Путь через эти станции не найден — измените список")
                    } else {
                        snackbar = EditorSnackbar("Путь по железной дороге между станциями не найден — проверьте координаты")
                    }
                }
                is ServicePhaseAutoFillViewModel.MeasureResult.Failed -> {
                    if (previewSingleRoute && pendingCustomRoute != null) {
                        failCustomRoute(res.failure.message())
                    } else if (fallbackToCachedMap) showRouteMap = true
                    else snackbar = EditorSnackbar(res.failure.message())
                }
            }
        }
    }

    val distanceKm = distance.text.toIntOrZero()

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIdx = stations.indexOfFirst { it.id == from.key }
        val toIdx = stations.indexOfFirst { it.id == to.key }
        if (fromIdx >= 0 && toIdx >= 0) stations.add(toIdx, stations.removeAt(fromIdx))
    }

    fun openStation(station: ServicePhaseStation?, at: Int?) {
        editingStation = station
        insertAt = at
        stationSheetOpen = true
    }

    if (showRouteMap) {
        val dep = departure.text.trim()
        val arr = arrival.text.trim()
        val depP = pointFor(depPoint, dep)
        val arrP = pointFor(arrPoint, arr)
        val stops = buildList {
            depP?.let { add(RouteMapStop(dep, it.latitude, it.longitude)) }
            stations.filter { it.hasCoordinates }.forEach { add(RouteMapStop(it.name, it.latitude!!, it.longitude!!, it.kind)) }
            arrP?.let { add(RouteMapStop(arr, it.latitude, it.longitude)) }
        }
        val total = stations.size + 2
        PhaseRouteMap(
            title = listOf(dep, arr).filter { it.isNotBlank() }.joinToString(" — ").ifBlank { "Плечо" },
            stops = stops,
            missing = total - stops.size,
            loadRailLine = { autoVm.railLine(it) },
            onDismiss = { showRouteMap = false },
        )
    }

    // Крайняя станция (отправление/прибытие) — та же шторка в упрощённом виде.
    editingTerminal?.let { isStart ->
        val name = (if (isStart) departure else arrival).text.trim()
        val point = pointFor(if (isStart) depPoint else arrPoint, name)
        val proxy = ServicePhaseStation(
            id = if (isStart) "terminal:departure" else "terminal:arrival",
            name = name,
            latitude = point?.latitude,
            longitude = point?.longitude,
            esr = point?.esr,
            osmId = point?.osmId,
            source = when {
                point == null -> ServicePhaseStationSource.MANUAL
                point.osmId != null -> ServicePhaseStationSource.OSM
                else -> ServicePhaseStationSource.MAP
            },
        )
        val neighbour = (if (isStart) stations.firstOrNull { it.hasCoordinates } else stations.lastOrNull { it.hasCoordinates })
        val near = listOfNotNull(neighbour?.let { it.latitude!! to it.longitude!! })
        PhaseStationSheet(
            station = proxy,
            stationList = stationList,
            phaseDistance = distanceKm,
            terminalTitle = if (isStart) "Станция отправления" else "Станция прибытия",
            onSave = { saved ->
                val newName = saved.name.trim()
                val value = TextFieldValue(newName, TextRange(newName.length))
                val newPoint = if (saved.hasCoordinates) {
                    ServicePhasePoint(saved.latitude!!, saved.longitude!!, saved.osmId, saved.esr) to newName
                } else null
                if (isStart) {
                    departure = value
                    depPoint = newPoint ?: depPoint?.takeIf { sameName(it.second, newName) }
                } else {
                    arrival = value
                    arrPoint = newPoint ?: arrPoint?.takeIf { sameName(it.second, newName) }
                }
            },
            onDelete = null,
            onDismiss = { editingTerminal = null },
            mapNeighbours = near,
            findCoordinates = { query -> autoVm.lookup(query, country, near) },
            neighbours = if (isStart) null to stations.firstOrNull()?.name else stations.lastOrNull()?.name to null,
        )
    }

    if (stationSheetOpen) {
        PhaseStationSheet(
            station = editingStation,
            stationList = stationList,
            phaseDistance = distanceKm,
            onSave = { saved ->
                val idx = stations.indexOfFirst { it.id == saved.id }
                when {
                    idx >= 0 -> stations[idx] = saved
                    insertAt != null -> stations.add(insertAt!!.coerceIn(0, stations.size), saved)
                    else -> stations.add(positionByKm(stations, saved.kmFromStart), saved)
                }
            },
            onDelete = editingStation?.let { st -> { stations.removeAll { it.id == st.id } } },
            findCoordinates = { query ->
                // Ищем рядом с соседями по плечу, у которых координаты уже известны.
                val idx = stations.indexOfFirst { it.id == editingStation?.id }
                val near = buildList {
                    stations.getOrNull(idx - 1)?.takeIf { it.hasCoordinates }?.let { add(it.latitude!! to it.longitude!!) }
                    stations.getOrNull(idx + 1)?.takeIf { it.hasCoordinates }?.let { add(it.latitude!! to it.longitude!!) }
                    depPoint?.first?.let { add(it.latitude to it.longitude) }
                    arrPoint?.first?.let { add(it.latitude to it.longitude) }
                }
                autoVm.lookup(query, country, near)
            },
            mapNeighbours = buildList {
                val idx = stations.indexOfFirst { it.id == editingStation?.id }
                listOfNotNull(stations.getOrNull(idx - 1), stations.getOrNull(idx + 1))
                    .filter { it.hasCoordinates }
                    .forEach { add(it.latitude!! to it.longitude!!) }
                if (idx == 0) depPoint?.first?.let { add(it.latitude to it.longitude) }
                if (idx == stations.lastIndex) arrPoint?.first?.let { add(it.latitude to it.longitude) }
            },
            neighbours = run {
                val idx = stations.indexOfFirst { it.id == editingStation?.id }
                val prev = stations.getOrNull(idx - 1)?.name ?: departure.text.trim().takeIf { idx == 0 }
                val next = stations.getOrNull(idx + 1)?.name ?: arrival.text.trim().takeIf { idx == stations.lastIndex }
                prev to next
            },
            onDismiss = {
                stationSheetOpen = false
                editingStation = null
                insertAt = null
            },
        )
    }

    if (autoFillOpen) {
        PhaseAutoFillSheet(
            state = autoState,
            departure = departure.text.trim(),
            arrival = arrival.text.trim(),
            distanceKm = distanceKm,
            existing = stations.toList(),
            stationList = stationList,
            onChoose = autoVm::choose,
            lookup = { query, near -> autoVm.lookup(query, country, near) },
            loadVariants = { pts ->
                (autoVm.measure(pts, withVariants = true) as? ServicePhaseAutoFillViewModel.MeasureResult.Ok)?.variants.orEmpty()
            },
            measureKm = { pts ->
                (autoVm.measure(pts) as? ServicePhaseAutoFillViewModel.MeasureResult.Ok)?.lengthKm
            },
            onRetry = autoVm::retry,
            onSetDistance = { km -> distance = TextFieldValue(km.toString(), TextRange(km.toString().length)) },
            onApply = { result ->
                val before = stations.toList()
                val beforeNames = pendingNames
                val beforeDep = depPoint
                val beforeArr = arrPoint
                stations.clear()
                // Автоподбор только дописывает координаты: у станций, уже известных справочнику
                // (в т.ч. указанных вручную), остаются их координаты, а не точки OSM.
                stations.addAll(result.stations.map { st ->
                    directoryPoints[ServicePhaseStationsMerge.normalizeName(st.name)]?.let { known ->
                        st.copy(latitude = known.latitude, longitude = known.longitude, esr = known.esr ?: st.esr)
                    } ?: st
                })
                pendingNames = (pendingNames + result.newNames).distinct()
                val dep = departure.text.trim()
                val arr = arrival.text.trim()
                depPoint = (pointFor(depPoint, dep) ?: result.departurePoint) to dep
                arrPoint = (pointFor(arrPoint, arr) ?: result.arrivalPoint) to arr
                freshIds = result.addedIds
                // Расстояние совпало с длиной пути по карте («Установить N км» или уже было таким).
                if (distance.text.toIntOrNull() == kotlin.math.round(result.lengthKm).toInt()) {
                    measured = MeasuredDistance(distance.text.toInt(), coordinateSignature())
                }
                closeAutoFill()
                val n = result.addedIds.size
                val m = result.newNames.size
                val text = "Добавлено $n ${pluralStations(n)}" +
                    if (m > 0) " · $m ${pluralNames(m)} — в справочник" else ""
                snackbar = EditorSnackbar(text) {
                    stations.clear()
                    stations.addAll(before)
                    pendingNames = beforeNames
                    depPoint = beforeDep
                    arrPoint = beforeArr
                    freshIds = emptySet()
                }
            },
            onEditName = { end ->
                closeAutoFill()
                (if (end == PhaseEnd.DEPARTURE) departureFocus else arrivalFocus).requestFocus()
            },
            onAddManually = {
                closeAutoFill()
                openStation(null, null)
            },
            onBuildCustomRoute = { candidates ->
                pendingCustomRoute = PendingCustomRoute(stations.toList(), null, candidates)
                setCustomRouteVias(candidates)
                closeAutoFill()
                measureByMap(previewSingleRoute = true)
            },
            onDismiss = ::closeAutoFill,
        )
    }

    // ─── Автосохранение ───────────────────────────────────────────────
    // id нового плеча — один на всё редактирование: повторные сохранения обновляют то же плечо.
    val phaseId = remember { phase?.id ?: com.z_company.domain.util.generateId() }
    var deleted by remember { mutableStateOf(false) }
    var closed by remember { mutableStateOf(false) }
    /** Последнее сохранённое состояние — без правок повторно не сохраняем. */
    var lastSaved by remember { mutableStateOf(phase) }

    /** Текущее состояние редактора как плечо; null — не заполнены станции или расстояние. */
    fun currentPhase(): ServicePhase? {
        val dep = departure.text.trim()
        val arr = arrival.text.trim()
        // Для сохранения достаточно двух станций; расстояние можно указать (или измерить) позже.
        val km = distance.text.toIntOrZero().coerceAtLeast(0)
        if (dep.isBlank() || arr.isBlank()) return null
        val mileageRate = rate.text.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val base = phase ?: ServicePhase(id = phaseId, departureStation = dep, arrivalStation = arr, distance = km)
        return base.copy(
            departureStation = dep,
            arrivalStation = arr,
            distance = km,
            linearMileageRate = mileageRate,
            intermediateStations = stations.toList(),
            // Координаты относились к прежней станции — при переименовании сбрасываем.
            departurePoint = pointFor(depPoint, dep),
            arrivalPoint = pointFor(arrPoint, arr),
        )
    }

    /** Сохранить; [final] — при закрытии: названия в справочник и обратное плечо (для нового). */
    fun persist(final: Boolean) {
        if (deleted) return
        val base = currentPhase() ?: return
        val isMeasured = measured?.let { it.distance == base.distance && it.signature == coordinateSignature() } == true
        val result = base.copy(measuredSignature = if (isMeasured) measured!!.encode() else null)
        // Миграция: значение теперь в самом плече — локальную запись убираем.
        if (measuredPrefs.contains(result.id)) measuredPrefs.edit().remove(result.id).apply()
        if (final && pendingNames.isNotEmpty()) onAddStationNames(pendingNames)
        val reverse = final && isNew && createReverse
        if (result == lastSaved && !reverse) return
        onSave(stationDirectory.prepareSave(result), reverse)
        lastSaved = result
    }

    fun close() {
        if (closed) return
        closed = true
        persist(final = true)
        onDismiss()
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            listOf(departure.text, arrival.text, distance.text, rate.text, stations.toList(), depPoint, arrPoint)
        }
            .drop(1)
            .collectLatest {
                delay(600)
                if (!closed && pendingCustomRoute == null) persist(final = false)
            }
    }

    variantChoice?.let { choice ->
        PhaseRouteVariantsMap(
            from = choice.from,
            to = choice.to,
            variants = choice.variants,
            onSelect = { v ->
                variantChoice = null
                applyVariant(choice, v)
                pendingCustomRoute = null
                customRetry = null
            },
            onDismiss = {
                customRetry = null
                val pending = pendingCustomRoute
                if (pending != null) {
                    // «Назад» из предпросмотра своего маршрута: временные станции не сохраняются, снова все варианты.
                    stations.clear()
                    stations.addAll(pending.stationsBefore)
                    pendingCustomRoute = null
                    variantChoice = pending.choiceBefore
                } else {
                    variantChoice = null
                }
            },
            findCustomRouteStations = { query ->
                val near = buildList {
                    pointFor(depPoint, departure.text.trim())?.let { add(it.latitude to it.longitude) }
                    pointFor(arrPoint, arrival.text.trim())?.let { add(it.latitude to it.longitude) }
                }
                autoVm.lookup(query, country, near)
            },
            initialCustom = customRetry?.chosen,
            initialCustomError = customRetry?.message,
            onBuildCustomRoute = { candidates ->
                pendingCustomRoute = PendingCustomRoute(stations.toList(), choice, candidates)
                customRetry = null
                setCustomRouteVias(candidates)
                variantChoice = null
                measureByMap(previewSingleRoute = true)
            },
        )
    }

    choosingTerminal?.let { choice ->
        StationChoiceSheet(
            query = choice.name,
            candidates = choice.candidates,
            current = null,
            onChoose = { c ->
                val point = ServicePhasePoint(c.latitude, c.longitude, osmId = c.osmId, esr = c.esr?.takeIf(String::isNotBlank))
                if (choice.isStart) depPoint = point to departure.text.trim() else arrPoint = point to arrival.text.trim()
                choosingTerminal = null
                measureByMap()
            },
            onDismiss = { choosingTerminal = null },
            onPickOnMap = {
                choosingTerminal = null
                editingTerminal = choice.isStart
            },
        )
    }

    if (confirmDeletePhase && onDelete != null) {
        AppAlertDialog(
            onDismissRequest = { confirmDeletePhase = false },
            title = "Удалить плечо?",
            text = "«${phase?.departureStation} — ${phase?.arrivalStation}» будет удалено из справочника вместе со станциями.",
            confirmText = "Удалить",
            onConfirm = {
                confirmDeletePhase = false
                deleted = true
                closed = true
                onDelete()
                onDismiss()
            },
            isDestructive = true,
            dismissText = "Отмена",
        )
    }

    // Отдельный полноэкранный экран (не шторка): в шторке длинный список станций при прокрутке
    // конфликтовал с жестом шторки — она «прыгала» вверх-вниз. «Назад» — закрыть с сохранением.
    Dialog(
        onDismissRequest = ::close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Pst.bg)
                .systemBarsPadding()
                .imePadding()
        ) {
            EditorTopBar(
                title = if (isNew) "Новое плечо" else "Редактировать плечо",
                onBack = ::close,
                onMap = {
                    val cached = cachedVariantChoice
                    if (
                        stations.isEmpty() &&
                        cached?.signature == coordinateSignature() &&
                        cached.choice.from == departure.text.trim() &&
                        cached.choice.to == arrival.text.trim()
                    ) {
                        variantChoice = cached.choice
                    } else {
                        measureByMap(
                            previewSingleRoute = true,
                            fallbackToCachedMap = true,
                        )
                    }
                },
            )
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                ) {
                    item(key = "fields") {
                        PhaseFields(
                            departure = departure,
                            onDeparture = { departure = it },
                            arrival = arrival,
                            onArrival = { arrival = it },
                            distance = distance,
                            onDistance = { v -> distance = v.copy(text = v.text.filter(Char::isDigit)) },
                            rate = rate,
                            onRate = { v -> rate = v.copy(text = v.text.filter { it.isDigit() || it == ',' || it == '.' }) },
                            stationList = stationList,
                            onDeleteStationName = onDeleteStationName,
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            onDone = { focusManager.clearFocus() },
                            departureFocus = departureFocus,
                            arrivalFocus = arrivalFocus,
                            currency = currencySymbol(country),
                            measure = {
                                val isMeasured = measured?.let {
                                    it.distance == distanceKm && it.signature == coordinateSignature()
                                } == true
                                MeasureByMap(isMeasured = isMeasured, measuring = measuring, onMeasure = ::measureByMap)
                            },
                        )
                    }
                    if (isNew) {
                        item(key = "reverse") {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                PstCard {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { createReverse = !createReverse }
                                            .padding(horizontal = 18.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "Создать плечо в обратном направлении",
                                            style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                                            color = Pst.text,
                                            modifier = Modifier.weight(1f).padding(end = 12.dp),
                                        )
                                        Switch(
                                            checked = createReverse,
                                            onCheckedChange = { createReverse = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Pst.surface,
                                                checkedTrackColor = Pst.accent,
                                            ),
                                        )
                                    }
                                }
                                PstNote(
                                    text = "Промежуточные станции тоже скопируются — в обратном порядке.",
                                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
                                )
                            }
                        }
                    }
                    item(key = "stationsHead") {
                        PstGroupHead(
                            text = "Промежуточные станции · ${stations.size}",
                            modifier = Modifier.padding(top = 22.dp, bottom = 8.dp).heightIn(min = 32.dp),
                            action = if (stations.isEmpty()) null else {
                                {
                                    PstButton(
                                        text = if (editMode) "Готово" else "Изменить",
                                        onClick = { editMode = !editMode },
                                        kind = PstButtonKind.TEXT,
                                        height = 32.dp,
                                        fontSize = 14,
                                    )
                                }
                            },
                        )
                    }
                    // Схема с крайними станциями видна всегда: у них можно открыть шторку
                    // (название, координаты) и без промежуточных.
                    stationScheme(
                        departure = departure.text.trim(),
                        arrival = arrival.text.trim(),
                        distanceKm = distanceKm,
                        stations = stations,
                        editMode = editMode,
                        reorderState = reorderState,
                        freshIds = freshIds,
                        activeInsert = if (stationSheetOpen && editingStation == null) insertAt else null,
                        onOpen = { openStation(it, null) },
                        onOpenTerminal = { isStart -> editingTerminal = isStart },
                        onInsert = { at -> openStation(null, at) },
                        onRemove = { st ->
                            val idx = stations.indexOfFirst { it.id == st.id }
                            if (idx >= 0) {
                                stations.removeAt(idx)
                                snackbar = EditorSnackbar("Станция убрана") {
                                    stations.add(idx.coerceAtMost(stations.size), st)
                                }
                            }
                        },
                    )
                    if (stations.isEmpty()) {
                        item(key = "empty") {
                            Box(Modifier.padding(top = 10.dp)) {
                                EmptyStations(
                                    canAutoFill = canAutoFill,
                                    onAutoFill = ::startAutoFill,
                                    onAddManually = { openStation(null, null) },
                                )
                            }
                        }
                    } else item(key = "stationsActions") {
                        if (editMode) {
                            PstNote(
                                text = "Перетащите за ≡, чтобы изменить порядок. «+» — вставить станцию " +
                                    "между соседними, ✕ — убрать.",
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
                            )
                        } else {
                            Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PstButton(
                                    text = "Найти автоматически",
                                    onClick = ::startAutoFill,
                                    enabled = canAutoFill,
                                    kind = PstButtonKind.TONAL,
                                    icon = PstIcons.Search,
                                    iconSize = 17.dp,
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.weight(1f),
                                )
                                PstButton(
                                    text = "Станция",
                                    onClick = { openStation(null, null) },
                                    kind = PstButtonKind.TONAL,
                                    icon = PstIcons.Plus,
                                    iconSize = 17.dp,
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                )
                            }
                        }
                    }
                    if (!isNew && !editMode && onDelete != null) {
                        item(key = "deletePhase") {
                            Box(Modifier.fillMaxWidth().padding(top = 18.dp), contentAlignment = Alignment.Center) {
                                PstButton(
                                    text = "Удалить плечо",
                                    onClick = { confirmDeletePhase = true },
                                    kind = PstButtonKind.DANGER,
                                    icon = PstIcons.Trash,
                                    iconSize = 16.dp,
                                )
                            }
                        }
                    }
                }
                snackbar?.let { bar ->
                    EditorSnackbarView(
                        bar = bar,
                        onDismiss = { snackbar = null },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

// ─── Поля плеча ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhaseFields(
    departure: TextFieldValue,
    onDeparture: (TextFieldValue) -> Unit,
    arrival: TextFieldValue,
    onArrival: (TextFieldValue) -> Unit,
    distance: TextFieldValue,
    onDistance: (TextFieldValue) -> Unit,
    rate: TextFieldValue,
    onRate: (TextFieldValue) -> Unit,
    stationList: List<String>,
    onDeleteStationName: (String) -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit,
    departureFocus: FocusRequester,
    arrivalFocus: FocusRequester,
    currency: String,
    measure: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StationNameField("От станции", departure, onDeparture, stationList, onDeleteStationName, onNext, departureFocus)
        StationNameField("До станции", arrival, onArrival, stationList, onDeleteStationName, onNext, arrivalFocus)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PstTextField(
                value = distance,
                onValueChange = onDistance,
                label = "Расстояние, км",
                placeholder = "—",
                mono = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { onNext() }),
            )
            Column(modifier = Modifier.weight(1f)) {
                PstTextField(
                    value = rate,
                    onValueChange = onRate,
                    label = "Доплата за пробег, $currency/км",
                    placeholder = "—",
                    mono = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onDone() }),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            measure()
            Spacer(Modifier.weight(1f))
            // Сколько получит машинист за всё плечо: расстояние × ставка (как в расчёте зарплаты).
            val km = distance.text.toIntOrNull()?.takeIf { it > 0 }
            val perKm = rate.text.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 && it.isFinite() }
            if (km != null && perKm != null) {
                Text(
                    text = "За плечо: ${(km * perKm).roundMoneyToCents().toMoneyString(currency)}",
                    style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    color = Pst.textMuted,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationNameField(
    label: String,
    value: TextFieldValue,
    onValue: (TextFieldValue) -> Unit,
    stationList: List<String>,
    onDeleteStationName: (String) -> Unit,
    onNext: () -> Unit,
    focusRequester: FocusRequester,
) {
    var expanded by remember { mutableStateOf(false) }
    val filtered = remember(value.text, stationList) { stationList.filter { it.contains(value.text, ignoreCase = true) } }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        PstTextField(
            value = value,
            onValueChange = {
                onValue(it)
                expanded = it.text.isNotEmpty()
            },
            label = label,
            placeholder = "Начните вводить название",
            leadingIcon = PstIcons.MapPin,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                expanded = false
                onNext()
            }),
        )
        StationDropdownMenu(
            expanded = expanded && filtered.isNotEmpty(),
            stations = filtered,
            onSelect = {
                onValue(TextFieldValue(it, TextRange(it.length)))
                expanded = false
            },
            onDelete = onDeleteStationName,
            onDismiss = { expanded = false },
        )
    }
}

@Composable
private fun EmptyStations(canAutoFill: Boolean, onAutoFill: () -> Unit, onAddManually: () -> Unit) {
    PstCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Найдём их по карте или добавьте вручную.",
                style = Pst.sans.copy(fontSize = 14.sp, lineHeight = 20.sp),
                color = Pst.textMuted,
            )
            Column(modifier = Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PstButton(
                    text = "Найти автоматически",
                    onClick = onAutoFill,
                    enabled = canAutoFill,
                    icon = PstIcons.Search,
                    modifier = Modifier.fillMaxWidth(),
                )
                PstButton(
                    text = "Добавить вручную",
                    onClick = onAddManually,
                    kind = PstButtonKind.OUTLINED,
                    icon = PstIcons.Plus,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!canAutoFill) {
                PstNote(
                    text = "Для автоподбора укажите обе крайние станции",
                    align = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
            }
        }
    }
}

// ─── Схема участка в LazyColumn ────────────────────────────────────────
// Карточка разбита на элементы списка: у крайних станций скруглён верх/низ.

private fun LazyListScope.stationScheme(
    departure: String,
    arrival: String,
    distanceKm: Int,
    stations: List<ServicePhaseStation>,
    editMode: Boolean,
    reorderState: sh.calvin.reorderable.ReorderableLazyListState,
    freshIds: Set<String>,
    activeInsert: Int?,
    onOpen: (ServicePhaseStation) -> Unit,
    onOpenTerminal: (isStart: Boolean) -> Unit,
    onInsert: (Int) -> Unit,
    onRemove: (ServicePhaseStation) -> Unit,
) {
    val mode = if (editMode) SchemeMode.EDIT else SchemeMode.VIEW
    item(key = KEY_DEPARTURE) {
        CardSegment(top = true) {
            PhaseStopRow(
                name = departure.ifBlank { "Станция отправления" },
                type = StopType.TERMINAL,
                isStart = true,
                isFirst = true,
                isLast = false,
                mode = mode,
                onClick = if (editMode) null else ({ onOpenTerminal(true) }),
            )
        }
    }
    stations.forEachIndexed { index, st ->
        item(key = st.id) {
            ReorderableItem(reorderState, key = st.id) { isDragging ->
                CardSegment {
                    Column {
                        if (editMode) PhaseInsertRow(active = activeInsert == index, onClick = { onInsert(index) })
                        PhaseStopRow(
                            name = st.name,
                            type = if (st.kind == ServicePhaseStationKind.HALT) StopType.HALT else StopType.STATION,
                            isFirst = false,
                            isLast = false,
                            mode = mode,
                            km = st.kmFromStart,
                            esr = st.esr,
                            noCoordinates = !st.hasCoordinates,
                            lifted = isDragging,
                            fresh = st.id in freshIds,
                            onClick = if (editMode) null else ({ onOpen(st) }),
                            onRemove = { onRemove(st) },
                            modifier = if (editMode) Modifier.longPressDraggableHandle() else Modifier,
                            dragHandle = {
                                PstIcon(
                                    PstIcons.Drag,
                                    20.dp,
                                    if (isDragging) Pst.accent else Pst.textMuted,
                                    modifier = Modifier.draggableHandle(),
                                    description = "Перетащить",
                                )
                            },
                        )
                    }
                }
            }
        }
    }
    item(key = KEY_ARRIVAL) {
        CardSegment(bottom = true) {
            Column {
                if (editMode) PhaseInsertRow(active = activeInsert == stations.size, onClick = { onInsert(stations.size) })
                PhaseStopRow(
                    name = arrival.ifBlank { "Станция прибытия" },
                    type = StopType.TERMINAL,
                    isStart = false,
                    isFirst = false,
                    isLast = true,
                    mode = mode,
                    terminalKm = distanceKm.takeIf { it > 0 },
                    onClick = if (editMode) null else ({ onOpenTerminal(false) }),
                )
            }
        }
    }
}

@Composable
private fun CardSegment(top: Boolean = false, bottom: Boolean = false, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(
        topStart = if (top) 16.dp else 0.dp,
        topEnd = if (top) 16.dp else 0.dp,
        bottomStart = if (bottom) 16.dp else 0.dp,
        bottomEnd = if (bottom) 16.dp else 0.dp,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Pst.surface, shape)
            .padding(start = 6.dp, end = 8.dp, top = if (top) 4.dp else 0.dp, bottom = if (bottom) 4.dp else 0.dp),
    ) { content() }
}

// ─── Снекбар с «Отменить» ──────────────────────────────────────────────

private class VariantChoice(
    val from: String,
    val to: String,
    val variants: List<RouteVariant>,
    val autoFound: List<Pair<String, Boolean>>,
    val kmAt: List<Double> = emptyList(),
)

private class CachedVariantChoice(val signature: String, val choice: VariantChoice)

private class PendingCustomRoute(
    val stationsBefore: List<ServicePhaseStation>,
    val choiceBefore: VariantChoice?,
    val chosen: List<OsmStationCandidate>,
)

private class CustomRetry(val chosen: List<OsmStationCandidate>, val message: String)

/** [undo] null — просто сообщение, без «Отменить». */
private class TerminalChoice(val isStart: Boolean, val name: String, val candidates: List<OsmStationCandidate>)

/** [undo] — действие кнопки [actionLabel] (по умолчанию «Отменить»); null — без кнопки. */
private class EditorSnackbar(
    val text: String,
    val actionLabel: String = "Отменить",
    val durationMs: Long = 6_000,
    val undo: (() -> Unit)? = null,
)

@Composable
private fun EditorSnackbarView(bar: EditorSnackbar, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(bar) {
        delay(bar.durationMs)
        onDismiss()
    }
    // На инверсном фоне акцент берём контрастный: в светлой теме — светлый, в тёмной — тёмный.
    val darkTheme = Pst.bg.luminance() < 0.5f
    val actionColor = if (darkTheme) Color(0xFF0091D9) else Color(0xFF33BFFF)
    Row(
        modifier = modifier
            .padding(12.dp)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(Pst.cta, RoundedCornerShape(8.dp))
            .padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = bar.text,
            style = Pst.sans.copy(fontSize = 14.sp, lineHeight = 19.sp),
            color = Pst.ctaInk,
            modifier = Modifier.weight(1f),
        )
        val undo = bar.undo
        if (undo != null) Text(
            text = bar.actionLabel,
            style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            color = actionColor,
            modifier = Modifier
                .clickable {
                    undo()
                    onDismiss()
                }
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) else Box(Modifier.heightIn(min = 36.dp))
    }
}

// ─── Утилиты ───────────────────────────────────────────────────────────

private fun textValue(text: String?) = TextFieldValue(text ?: "", TextRange(text?.length ?: 0))

private fun sameName(a: String?, b: String) = a != null && a.trim().equals(b.trim(), ignoreCase = true)

/** Позиция новой станции по км: перед первой станцией с большим км; без км — в конец. */
private fun positionByKm(stations: List<ServicePhaseStation>, km: Double?): Int {
    if (km == null) return stations.size
    val idx = stations.indexOfFirst { (it.kmFromStart ?: return@indexOfFirst false) > km }
    return if (idx < 0) stations.size else idx
}

private fun pluralStations(n: Int) = when {
    n % 100 in 11..19 -> "станций"
    n % 10 == 1 -> "станция"
    n % 10 in 2..4 -> "станции"
    else -> "станций"
}

private fun pluralNames(n: Int) = when {
    n % 100 in 11..19 -> "названий"
    n % 10 == 1 -> "название"
    n % 10 in 2..4 -> "названия"
    else -> "названий"
}

@Composable
private fun EditorTopBar(title: String, onBack: () -> Unit, onMap: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // «Готово» — закрыть (изменения уже сохраняются автоматически), как в карточке станции.
        Box(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .clickable(onClickLabel = "Готово", onClick = onBack)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Готово",
                style = Pst.sans.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                color = Pst.accent,
            )
        }
        Text(
            text = title,
            style = Pst.sans.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
            color = Pst.text,
            textAlign = TextAlign.Center,
            // Одна строка: при крупном системном шрифте иначе рвалось посреди слова.
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        // Всё плечо на карте: крайние и промежуточные станции с координатами.
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .clickable(onClickLabel = "Плечо на карте", onClick = onMap)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PstIcon(PstIcons.MapPin, 18.dp, Pst.accent)
            Text(
                text = "Карта",
                style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                color = Pst.accent,
            )
        }
    }
}

/** Расстояние, измеренное по карте, и координаты станций, по которым его измерили. */
private data class MeasuredDistance(val distance: Int, val signature: String) {
    fun encode() = "$distance|$signature"

    companion object {
        fun parse(v: String): MeasuredDistance? {
            val i = v.indexOf('|')
            if (i <= 0) return null
            val d = v.substring(0, i).toIntOrNull() ?: return null
            return MeasuredDistance(d, v.substring(i + 1))
        }
    }
}

/**
 * Под расстоянием: «Измерить по карте» (расстояние введено вручную / не введено / координаты
 * станций изменились после измерения) или чип «Измерено по карте».
 */
@Composable
private fun MeasureByMap(isMeasured: Boolean, measuring: Boolean, onMeasure: () -> Unit) {
    if (isMeasured) {
        Row(
            modifier = Modifier
                .padding(start = 4.dp)
                .background(Pst.success.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PstIcon(PstIcons.Check, 14.dp, Pst.success)
            Text(
                text = "Измерено по карте",
                style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                color = Pst.text,
            )
        }
    } else {
        PstButton(
            text = if (measuring) "Измеряем…" else "Измерить по карте",
            onClick = onMeasure,
            enabled = !measuring,
            kind = PstButtonKind.TONAL,
            icon = PstIcons.Measure,
            iconSize = 18.dp,
            height = 36.dp,
            fontSize = 14,
            contentPadding = PaddingValues(horizontal = 14.dp),
        )
    }
}
