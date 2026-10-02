package com.z_company.route.component.servicephase

import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.toMutableStateList
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.domain.entities.setting.ServicePhasePoint
import com.z_company.domain.entities.setting.ServicePhaseStation
import com.z_company.domain.entities.setting.ServicePhaseStationKind
import com.z_company.domain.entities.setting.ServicePhaseStationSource
import com.z_company.domain.util.ServicePhaseStationsMerge
import com.z_company.repository.remote_rest.response.OsmStationCandidate
import com.z_company.route.viewmodel.AutoFillFailure
import com.z_company.route.viewmodel.AutoFillState
import com.z_company.route.viewmodel.PhaseEnd
import kotlin.math.abs
import kotlin.math.roundToInt

/** Результат автоподбора, подтверждённый пользователем. */
internal class AutoFillResult(
    val stations: List<ServicePhaseStation>,
    val addedIds: Set<String>,
    val newNames: List<String>,
    val departurePoint: ServicePhasePoint,
    val arrivalPoint: ServicePhasePoint,
    /** Длина пути по карте (км) — если расстояние плеча совпадает с ней, оно «измерено по карте». */
    val lengthKm: Double,
)

/**
 * Шторка «Автоподбор станций» (дизайн design/service_phase_stations, экраны 17–26).
 * Закрытие на любом шаге отменяет запрос; плечо меняется только по «Добавить N станций».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhaseAutoFillSheet(
    state: AutoFillState,
    departure: String,
    arrival: String,
    distanceKm: Int,
    existing: List<ServicePhaseStation>,
    stationList: List<String>,
    onChoose: (PhaseEnd, OsmStationCandidate) -> Unit,
    onRetry: () -> Unit,
    onApply: (AutoFillResult) -> Unit,
    onSetDistance: (Int) -> Unit,
    onEditName: (PhaseEnd) -> Unit,
    onAddManually: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Pst.bg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { PstGrabber() },
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
            PstSheetHeader(title = "Автоподбор станций", subtitle = "$departure → $arrival", onClose = onDismiss)
            when (state) {
                AutoFillState.Idle, is AutoFillState.Loading ->
                    LoadingContent(step = (state as? AutoFillState.Loading)?.step ?: 0, onCancel = onDismiss)
                is AutoFillState.Choose -> ChooseContent(state, onChoose = onChoose, onCancel = onDismiss)
                is AutoFillState.Ready -> PreviewContent(
                    state = state,
                    departure = departure,
                    arrival = arrival,
                    distanceKm = distanceKm,
                    existing = existing,
                    stationList = stationList,
                    onApply = onApply,
                    onSetDistance = onSetDistance,
                    onCancel = onDismiss,
                )
                is AutoFillState.Failed -> FailureContent(
                    state = state,
                    departure = departure,
                    arrival = arrival,
                    onRetry = onRetry,
                    onEditName = onEditName,
                    onAddManually = onAddManually,
                    onClose = onDismiss,
                )
            }
        }
    }
}

// ─── 5.1 Загрузка ──────────────────────────────────────────────────────

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.LoadingContent(step: Int, onCancel: () -> Unit) {
    val titles = listOf("Ищем крайние станции…", "Ищем путь между станциями…", "Собираем станции и О.П. на пути…")
    val steps = listOf("Нашли крайние станции на карте", "Прокладываем путь", "Собираем станции и О.П. на пути")
    Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp).padding(top = 20.dp)) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = Pst.accent,
            trackColor = Pst.bgSubtle,
        )
        Text(
            text = titles[step.coerceIn(0, 2)],
            style = Pst.sans.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
            color = Pst.text,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = "Обычно несколько секунд. Можно отменить — ничего не изменится.",
            style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 19.sp),
            color = Pst.textMuted,
            modifier = Modifier.padding(top = 6.dp),
        )
        Column(modifier = Modifier.padding(top = 18.dp)) {
            steps.forEachIndexed { i, label ->
                Row(
                    modifier = Modifier.padding(vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                        when {
                            i < step -> Box(
                                Modifier.size(22.dp).background(Pst.success.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { PstIcon(PstIcons.Check, 14.dp, Pst.success) }
                            i == step -> CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Pst.accent,
                                trackColor = Pst.accent.copy(alpha = 0.22f),
                            )
                            else -> Box(Modifier.size(10.dp).border(2.dp, Pst.borderStrong, CircleShape))
                        }
                    }
                    Text(
                        text = label,
                        style = Pst.sans.copy(fontSize = 15.sp, fontWeight = if (i == step) FontWeight.SemiBold else FontWeight.Medium),
                        color = if (i > step) Pst.textFaint else Pst.text,
                    )
                }
            }
        }
    }
    PstSheetFooter {
        PstButton(text = "Отменить", onClick = onCancel, kind = PstButtonKind.OUTLINED, height = 48.dp, modifier = Modifier.fillMaxWidth())
    }
}

// ─── 5.2 Уточнение крайней станции ─────────────────────────────────────

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ChooseContent(
    state: AutoFillState.Choose,
    onChoose: (PhaseEnd, OsmStationCandidate) -> Unit,
    onCancel: () -> Unit,
) {
    var selected by remember(state) { mutableStateOf<OsmStationCandidate?>(null) }
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
    ) {
        item {
            Text(
                text = if (state.end == PhaseEnd.DEPARTURE) "СТАНЦИЯ ОТПРАВЛЕНИЯ" else "СТАНЦИЯ ПРИБЫТИЯ",
                style = Pst.mono.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp),
                color = Pst.accent,
            )
            Text(
                text = "Какая «${state.query}»?",
                style = Pst.sans.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                color = Pst.text,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = if (state.approximate) {
                    "Станции с таким названием нет — возможно, это сокращение. Похожие станции:"
                } else {
                    "На карте несколько станций с похожим названием."
                },
                style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 19.sp),
                color = Pst.textMuted,
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
            )
        }
        item {
            CandidateList(state.candidates, selected, onSelect = { selected = it })
            if (state.end == PhaseEnd.DEPARTURE) {
                PstNote(
                    text = "Следом уточним станцию прибытия, если она тоже неоднозначна.",
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp),
                )
            }
        }
    }
    PstSheetFooter {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PstButton(text = "Отмена", onClick = onCancel, kind = PstButtonKind.OUTLINED, height = 48.dp)
            PstButton(
                text = "Далее",
                onClick = { selected?.let { onChoose(state.end, it) } },
                enabled = selected != null,
                height = 48.dp,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ─── 5.3 Предпросмотр ──────────────────────────────────────────────────

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.PreviewContent(
    state: AutoFillState.Ready,
    departure: String,
    arrival: String,
    distanceKm: Int,
    existing: List<ServicePhaseStation>,
    stationList: List<String>,
    onApply: (AutoFillResult) -> Unit,
    onSetDistance: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    // Изменяемый: названия предложенных станций можно поправить (✎) до добавления.
    val found = remember(state) {
        state.stations.map {
            ServicePhaseStation(
                name = it.name,
                latitude = it.latitude,
                longitude = it.longitude,
                kmFromStart = it.kmFromStart,
                kind = if (it.kind == "halt") ServicePhaseStationKind.HALT else ServicePhaseStationKind.STATION,
                esr = it.esr?.takeIf(String::isNotBlank),
                osmId = it.osmId,
                source = ServicePhaseStationSource.OSM,
            )
        }.toMutableStateList()
    }
    var renaming by remember(state) { mutableStateOf<ServicePhaseStation?>(null) }
    renaming?.let { st ->
        RenameStationSheet(
            name = st.name,
            onDone = { newName ->
                val idx = found.indexOfFirst { it.id == st.id }
                if (idx >= 0 && newName.isNotBlank()) found[idx] = found[idx].copy(name = newName.trim())
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
    val already = remember(found, existing) {
        found.filter { ServicePhaseStationsMerge.existingMatch(it, existing) != null }.map { it.id }.toSet()
    }
    val halts = found.count { it.kind == ServicePhaseStationKind.HALT }
    val mainCount = found.size - halts
    var showHalts by remember(state) { mutableStateOf(false) }
    var checked by remember(state) {
        mutableStateOf(found.filter { it.kind == ServicePhaseStationKind.STATION && it.id !in already }.map { it.id }.toSet())
    }
    var addNames by remember(state) { mutableStateOf(true) }
    var distanceApplied by remember(state) { mutableStateOf(false) }

    val visible = found.filter { showHalts || it.kind == ServicePhaseStationKind.STATION || it.id in already }
    val selectedNew = found.filter { it.id in checked && it.id !in already }
    val newNames = ServicePhaseStationsMerge.newNames(selectedNew, stationList)
    val length = state.lengthKm
    val rounded = length.roundToInt()
    val mismatch = distanceKm > 0 && (abs(length - distanceKm) > distanceKm * 0.05 || abs(length - distanceKm) > 3)

    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
    ) {
        item(key = "summary") {
            Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Найдено: $mainCount ${pstPlural(mainCount, "станция", "станции", "станций")}, $halts О.П.",
                    style = Pst.sans.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                    color = Pst.text,
                )
                Text(text = "длина пути ${pstKm(length)}", style = Pst.mono.copy(fontSize = 12.sp), color = Pst.textMuted)
            }
        }
        if (!distanceApplied && (distanceKm <= 0 || mismatch)) {
            item(key = "distance") {
                PstNotice(
                    color = if (distanceKm <= 0) Pst.accent else Pst.warning,
                    title = if (distanceKm <= 0) "Расстояние не указано" else "Расстояние не совпадает с картой",
                    text = if (distanceKm <= 0) "По карте длина пути — ${pstKm(length)}."
                    else "Вы указали $distanceKm км, по карте — ${pstKm(length)}.",
                    actionText = "Установить $rounded км",
                    onAction = {
                        onSetDistance(rounded)
                        distanceApplied = true
                    },
                )
            }
        }
        if (existing.isNotEmpty()) {
            item(key = "repeat") {
                PstNotice(
                    color = Pst.accent,
                    text = "Станции, которые уже есть в плече, останутся как есть. Добавятся только отмеченные новые.",
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        if (halts > 0) {
            item(key = "halts") {
                Box(Modifier.padding(top = 12.dp)) {
                    PstCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showHalts = !showHalts
                                    if (!showHalts) checked = checked.filterTo(mutableSetOf()) { id -> found.first { it.id == id }.kind == ServicePhaseStationKind.STATION }
                                }
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Показывать О.П.",
                                    style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                                    color = Pst.text,
                                )
                                if (!showHalts) {
                                    Text(
                                        text = "Скрыто $halts О.П. — по умолчанию не добавляются",
                                        style = Pst.sans.copy(fontSize = 12.sp),
                                        color = Pst.textMuted,
                                    )
                                }
                            }
                            Switch(
                                checked = showHalts,
                                onCheckedChange = null,
                                colors = SwitchDefaults.colors(checkedThumbColor = Pst.surface, checkedTrackColor = Pst.accent),
                            )
                        }
                    }
                }
            }
        }
        item(key = "chips") {
            Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Отметить все станции") {
                    checked = checked + found.filter { it.kind == ServicePhaseStationKind.STATION && it.id !in already }.map { it.id }
                }
                Chip("Снять все") { checked = emptySet() }
            }
        }
        item(key = "gap") { Box(Modifier.height(12.dp)) }
        pickScheme(
            departure = departure,
            arrival = arrival,
            lengthKm = length,
            stations = visible,
            checked = checked,
            already = already,
            onToggle = { id -> checked = if (id in checked) checked - id else checked + id },
            onRename = { st -> renaming = st },
        )
    }
    PstSheetFooter {
        if (newNames.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { addNames = !addNames }.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PstCheckbox(checked = addNames)
                Text(
                    text = "Добавить ${newNames.size} ${pstPlural(newNames.size, "новое название", "новых названия", "новых названий")} в справочник станций",
                    style = Pst.sans.copy(fontSize = 14.sp),
                    color = Pst.text,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PstButton(text = "Отмена", onClick = onCancel, kind = PstButtonKind.OUTLINED, height = 48.dp)
            val n = selectedNew.size
            PstButton(
                text = if (n == 0) "Ничего не выбрано" else "Добавить $n ${stationsWord(n)}",
                enabled = n > 0,
                height = 48.dp,
                modifier = Modifier.weight(1f),
                onClick = {
                    val (merged, added) = ServicePhaseStationsMerge.merge(existing, found, checked)
                    onApply(
                        AutoFillResult(
                            stations = merged,
                            addedIds = added,
                            newNames = if (addNames) newNames else emptyList(),
                            departurePoint = state.departure.toPoint(),
                            arrivalPoint = state.arrival.toPoint(),
                            lengthKm = state.lengthKm,
                        )
                    )
                },
            )
        }
        Text(
            text = "© участники OpenStreetMap",
            style = Pst.mono.copy(fontSize = 10.sp),
            color = Pst.textFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun OsmStationCandidate.toPoint() =
    ServicePhasePoint(latitude = latitude, longitude = longitude, osmId = osmId, esr = esr?.takeIf(String::isNotBlank))

@Composable
private fun Chip(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .height(32.dp)
            .border(1.dp, Pst.borderStrong, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium), color = Pst.text)
    }
}

private fun LazyListScope.pickScheme(
    departure: String,
    arrival: String,
    lengthKm: Double,
    stations: List<ServicePhaseStation>,
    checked: Set<String>,
    already: Set<String>,
    onToggle: (String) -> Unit,
    onRename: (ServicePhaseStation) -> Unit,
) {
    item(key = "pick:dep") {
        SchemeSegment(top = true) {
            PhaseStopRow(name = departure, type = StopType.TERMINAL, isStart = true, isFirst = true, isLast = false, mode = SchemeMode.PICK)
        }
    }
    stations.forEach { st ->
        item(key = "pick:" + st.id) {
            SchemeSegment {
                val isAlready = st.id in already
                PhaseStopRow(
                    name = st.name,
                    type = if (st.kind == ServicePhaseStationKind.HALT) StopType.HALT else StopType.STATION,
                    isFirst = false,
                    isLast = false,
                    mode = SchemeMode.PICK,
                    km = st.kmFromStart,
                    esr = st.esr,
                    already = isAlready,
                    checked = st.id in checked,
                    onClick = if (isAlready) null else ({ onToggle(st.id) }),
                    onEdit = if (isAlready) null else ({ onRename(st) }),
                )
            }
        }
    }
    item(key = "pick:arr") {
        SchemeSegment(bottom = true) {
            PhaseStopRow(name = arrival, type = StopType.TERMINAL, isFirst = false, isLast = true, mode = SchemeMode.PICK, km = lengthKm)
        }
    }
}

@Composable
internal fun SchemeSegment(top: Boolean = false, bottom: Boolean = false, content: @Composable () -> Unit) {
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

// ─── 5.4 Ошибки ────────────────────────────────────────────────────────

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.FailureContent(
    state: AutoFillState.Failed,
    departure: String,
    arrival: String,
    onRetry: () -> Unit,
    onEditName: (PhaseEnd) -> Unit,
    onAddManually: () -> Unit,
    onClose: () -> Unit,
) {
    data class Spec(val icon: ImageVector, val title: String, val text: String, val primary: Pair<String, () -> Unit>, val secondary: Pair<String, () -> Unit>)
    val spec = when (state.failure) {
        AutoFillFailure.NO_INTERNET -> Spec(
            PstIcons.CloudOff, "Нет подключения к интернету",
            "Автоподбор ищет станции на сервере. Проверьте связь и повторите — или добавьте станции вручную.",
            "Повторить" to onRetry, "Добавить вручную" to onAddManually,
        )
        AutoFillFailure.NO_CONNECTION, AutoFillFailure.VPN -> Spec(
            PstIcons.CloudOff, "Нет связи с сервером",
            if (state.failure == AutoFillFailure.VPN) com.z_company.core.util.VPN_ERROR_HINT
            else "Интернет на телефоне есть, но до сервера не достучаться. Проверьте подключение и повторите — или добавьте станции вручную.",
            "Повторить" to onRetry, "Добавить вручную" to onAddManually,
        )
        AutoFillFailure.STATION_NOT_FOUND -> Spec(
            PstIcons.MapPin, "Не нашли станцию «${state.name ?: ""}»",
            "На карте нет станции с таким или похожим названием. Проверьте написание. " +
                "Если это не станция (пути необщего пользования, терминал) — укажите её точку на карте вручную, " +
                "а промежуточные станции добавьте вручную.",
            "Изменить название" to { onEditName(state.end ?: PhaseEnd.DEPARTURE) }, "Добавить вручную" to onAddManually,
        )
        AutoFillFailure.NO_PATH -> Spec(
            PstIcons.Shoulder, "Путь между станциями не найден",
            "На карте нет связанного пути от «$departure» до «$arrival». Возможно, станции на разных линиях или в данных карты разрыв.",
            "Добавить вручную" to onAddManually, "Закрыть" to onClose,
        )
        AutoFillFailure.RATE_LIMITED -> Spec(
            PstIcons.Clock, "Слишком много запросов",
            "Автоподбор запускали много раз подряд. Попробуйте позже — или добавьте станции вручную.",
            "Добавить вручную" to onAddManually, "Закрыть" to onClose,
        )
        AutoFillFailure.SERVICE -> Spec(
            PstIcons.Clock, "Сервис не ответил",
            "Поиск занял больше 30 секунд или сервис временно недоступен. Попробуйте ещё раз через минуту.",
            "Повторить" to onRetry, "Закрыть" to onClose,
        )
    }
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp).padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(64.dp).background(Pst.bgSubtle, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) { PstIcon(spec.icon, 30.dp, Pst.textMuted) }
        Text(
            text = spec.title,
            style = Pst.sans.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
            color = Pst.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            text = spec.text,
            style = Pst.sans.copy(fontSize = 14.sp, lineHeight = 20.sp),
            color = Pst.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp).widthIn(max = 300.dp),
        )
    }
    PstSheetFooter {
        PstButton(text = spec.primary.first, onClick = spec.primary.second, height = 48.dp, modifier = Modifier.fillMaxWidth())
        PstButton(text = spec.secondary.first, onClick = spec.secondary.second, kind = PstButtonKind.OUTLINED, height = 48.dp, modifier = Modifier.fillMaxWidth())
    }
}

/** Переименование предложенной станции в предпросмотре автоподбора. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenameStationSheet(name: String, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(name, androidx.compose.ui.text.TextRange(name.length))) }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(Unit) { focus.requestFocus() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Pst.bg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { PstGrabber() },
    ) {
        Column(modifier = Modifier.fillMaxWidth().imePadding()) {
            PstSheetHeader(title = "Название станции", onClose = onDismiss)
            PstTextField(
                value = value,
                onValueChange = { value = it },
                label = "Название",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .focusRequester(focus),
            )
            PstSheetFooter {
                PstButton(
                    text = "Готово",
                    kind = PstButtonKind.CTA,
                    enabled = value.text.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onDone(value.text) },
                )
            }
        }
    }
}


/** Карточка-список станций OSM с радиокнопками (выбор неоднозначной станции). */
@Composable
internal fun CandidateList(
    candidates: List<OsmStationCandidate>,
    selected: OsmStationCandidate?,
    onSelect: (OsmStationCandidate) -> Unit,
) {
    PstCard {
        Column {
            candidates.forEachIndexed { i, c ->
                if (i > 0) HorizontalDivider(color = Pst.border, modifier = Modifier.padding(start = 50.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (selected == c) Pst.accentSoft else Pst.surface)
                        .clickable { onSelect(c) }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PstRadio(selected = selected == c)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = c.name,
                            style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            color = Pst.text,
                        )
                        c.esr?.let {
                            Text(
                                text = "ЕСР $it",
                                style = Pst.mono.copy(fontSize = 11.sp),
                                color = Pst.textMuted,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        listOfNotNull(c.operator, c.region).takeIf { it.isNotEmpty() }?.let {
                            Text(text = it.joinToString(", "), style = Pst.sans.copy(fontSize = 12.sp), color = Pst.textMuted)
                        }
                        if (c.neighbours.isNotEmpty()) {
                            Text(
                                text = "Соседние: " + c.neighbours.joinToString(" · "),
                                style = Pst.sans.copy(fontSize = 12.sp),
                                color = Pst.text,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Шторка «Какая «{name}»?» — выбор станции из нескольких найденных с таким названием
 * (карточка станции справочника, «Определить автоматически»).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StationChoiceSheet(
    query: String,
    candidates: List<OsmStationCandidate>,
    current: OsmStationCandidate?,
    onChoose: (OsmStationCandidate) -> Unit,
    onDismiss: () -> Unit,
    /** «Указать на карте вручную» под списком (для неточных вариантов). */
    onPickOnMap: (() -> Unit)? = null,
) {
    val approximate = candidates.any { it.approximate }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by remember(candidates) { mutableStateOf(current) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Pst.bg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { PstGrabber() },
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
            PstSheetHeader(title = if (approximate) "Похоже на «$query»" else "Какая «$query»?", onClose = onDismiss)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
            ) {
                item {
                    Text(
                        text = if (approximate) {
                            "Станции с таким названием нет — возможно, это сокращение. Выберите свою станцию или укажите точку на карте."
                        } else {
                            "На карте несколько станций с таким названием."
                        },
                        style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 19.sp),
                        color = Pst.textMuted,
                        modifier = Modifier.padding(start = 4.dp, bottom = 14.dp),
                    )
                }
                item { CandidateList(candidates, selected, onSelect = { selected = it }) }
                if (onPickOnMap != null) item {
                    PstButton(
                        text = "Указать на карте вручную",
                        onClick = onPickOnMap,
                        kind = PstButtonKind.TEXT,
                        icon = PstIcons.MapPin,
                        iconSize = 16.dp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            PstSheetFooter {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PstButton(text = "Отмена", onClick = onDismiss, kind = PstButtonKind.OUTLINED, height = 48.dp)
                    PstButton(
                        text = "Выбрать",
                        onClick = { selected?.let(onChoose) },
                        enabled = selected != null,
                        height = 48.dp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
