package com.z_company.route.component.servicephase

import com.z_company.route.component.separateSheetFromScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.BottomSheetDefaults
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.z_company.repository.remote_rest.response.OsmStationCandidate
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.domain.entities.setting.ServicePhaseStation
import com.z_company.domain.entities.setting.ServicePhaseStationKind
import com.z_company.domain.entities.setting.ServicePhaseStationSource
import com.z_company.route.component.AppAlertDialog

/**
 * Шторка станции плеча («Станция» / «Новая станция»), поверх редактора плеча.
 * Закрытие ✕ / свайпом / «назад» — отмена без сохранения.
 *
 * [findCoordinates] и [onPickOnMap] — действия карточки «Координаты»; null — действия нет.
 * Найденные координаты применяются по «Готово».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhaseStationSheet(
    station: ServicePhaseStation?,
    stationList: List<String>,
    phaseDistance: Int,
    onSave: (ServicePhaseStation) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    /** Соседние станции плеча с координатами — ориентиры на карте; null — карты нет. */
    mapNeighbours: List<Pair<Double, Double>>? = null,
    /** Поиск координат по названию (сервер); null в ответе — ошибка сети/сервиса. */
    findCoordinates: (suspend (String) -> List<OsmStationCandidate>?)? = null,
    /** Соседи по плечу — для подписи найденной станции «между A и B». */
    neighbours: Pair<String?, String?> = null to null,
    /**
     * Крайняя станция плеча: заголовок «Станция отправления/прибытия», без типа, км и удаления
     * (км крайних — 0 и расстояние плеча; название меняет поле «От/До станции»).
     */
    terminalTitle: String? = null,
    /** «Написать в поддержку» при ненайденной станции (название); null — кнопки нет. */
    onWriteSupport: ((String) -> Unit)? = null,
    /** Станция справочника по названию (с координатами) или null; координаты подставляются сами, поиск не нужен. */
    directoryStation: ((String) -> com.z_company.domain.entities.norma_time.StationNorm?)? = null,
    /** Названия станций справочника с координатами - в подсказки названия. */
    directoryNames: List<String> = emptyList(),
) {
    val terminal = terminalTitle != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isNew = station == null
    var name by remember { mutableStateOf(TextFieldValue(station?.name ?: "", TextRange(station?.name?.length ?: 0))) }
    var kind by remember { mutableStateOf(station?.kind ?: ServicePhaseStationKind.STATION) }
    var kmText by remember {
        mutableStateOf(TextFieldValue(station?.kmFromStart?.let { formatKmInput(it) } ?: ""))
    }
    var suggestionsOpen by remember { mutableStateOf(isNew) }
    var confirmDelete by remember { mutableStateOf(false) }
    var coords by remember { mutableStateOf<CoordState>(CoordState.Idle) }
    var picked by remember { mutableStateOf<PickedPoint?>(null) }
    var mapOpen by remember { mutableStateOf(false) }
    /** Несколько точных или любые неточные варианты — явный выбор станции. */
    var choosing by remember { mutableStateOf<List<OsmStationCandidate>?>(null) }
    /** Кандидаты последнего поиска — для «Не та станция» в карточке «Нашлось». */
    var lookupCandidates by remember { mutableStateOf<List<OsmStationCandidate>>(emptyList()) }
    var rejecting by remember { mutableStateOf(false) }
    val newStationDraft = remember { ServicePhaseStation(name = "") }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val nameFocus = remember { FocusRequester() }

    val km = kmText.text.replace(',', '.').toDoubleOrNull()
    val kmInvalid = kmText.text.isNotBlank() &&
        (km == null || km <= 0.0 || (phaseDistance > 0 && km >= phaseDistance))
    val canSave = name.text.isNotBlank() && !kmInvalid

    val allNames = remember(stationList, directoryNames) { (stationList + directoryNames).distinctBy { it.trim().lowercase() } }
    val suggestions = remember(name.text, allNames) { suggest(allNames, name.text) }
    // Координаты из справочника по введённому названию: уступают явному выбору (карта, поиск), но побеждают сохранённые в плече.
    val dirHit = directoryStation?.invoke(name.text.trim())?.takeIf { it.hasCoordinates && picked == null && coords !is CoordState.Found }

    LaunchedEffect(Unit) { if (isNew) nameFocus.requestFocus() }

    val coordinateStation = (station ?: newStationDraft).copy(name = name.text.trim()).let { st ->
        if (dirHit == null) st else st.copy(
            latitude = dirHit.latitude, longitude = dirHit.longitude,
            esr = dirHit.esr ?: st.esr, osmId = dirHit.osmId ?: st.osmId, source = ServicePhaseStationSource.MAP,
        )
    }
    if (mapOpen && mapNeighbours != null) {
        val found = (coords as? CoordState.Found)?.candidate
        PhaseMapPicker(
            stationName = name.text.trim().ifBlank { coordinateStation.name },
            kindLabel = if (kind == ServicePhaseStationKind.HALT) "О.П." else "станция",
            start = picked?.let { it.latitude to it.longitude }
                ?: found?.let { it.latitude to it.longitude }
                ?: coordinateStation.latitude?.let { lat -> coordinateStation.longitude?.let { lat to it } },
            neighbours = mapNeighbours,
            onPick = {
                picked = it
                coords = CoordState.Idle
                mapOpen = false
            },
            onDismiss = { mapOpen = false },
        )
    }

    choosing?.let { candidates ->
        StationChoiceSheet(
            query = name.text.trim(),
            candidates = candidates,
            current = if (rejecting) (coords as? CoordState.Found)?.candidate else null,
            title = if (rejecting) "Выберите станцию" else null,
            onChoose = {
                coords = CoordState.Found(it)
                choosing = null
                rejecting = false
            },
            onDismiss = {
                choosing = null
                rejecting = false
            },
            onPickOnMap = if (mapNeighbours != null) ({
                choosing = null
                rejecting = false
                mapOpen = true
            }) else null,
        )
    }

    if (confirmDelete && station != null && onDelete != null) {
        AppAlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = "Удалить станцию?",
            text = "«${station.name}» будет убрана из плеча. Название останется в справочнике станций.",
            confirmText = "Удалить",
            onConfirm = {
                confirmDelete = false
                onDelete()
                onDismiss()
            },
            isDestructive = true,
            dismissText = "Отмена",
        )
    }

    // Пока открыт выбор точки на карте, шторка скрыта (состояние сохраняется): карта видна целиком; после «Выбрать»
    // шторка возвращается с новыми координатами, «назад» — без изменений.
    val pickerShown = mapOpen && mapNeighbours != null
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (pickerShown) androidx.compose.ui.graphics.Color.Transparent else Pst.bg,
        scrimColor = if (pickerShown) androidx.compose.ui.graphics.Color.Transparent else BottomSheetDefaults.ScrimColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { if (!pickerShown) PstGrabber() },
    ) {
        Column(modifier = Modifier.fillMaxWidth().imePadding().alpha(if (pickerShown) 0f else 1f)) {
            PstSheetHeader(title = terminalTitle ?: if (isNew) "Новая станция" else "Станция", onClose = onDismiss)
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .separateSheetFromScroll()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column {
                    PstTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            suggestionsOpen = true
                        },
                        label = "Название",
                        placeholder = "Станция",
                        modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        trailing = if (isNew && name.text.isNotEmpty()) {
                            {
                                Box(
                                    modifier = Modifier.size(28.dp).clickable { name = TextFieldValue("") },
                                    contentAlignment = Alignment.Center,
                                ) { PstIcon(PstIcons.Close, 18.dp, Pst.textMuted, description = "Очистить") }
                            }
                        } else null,
                    )
                    if (suggestionsOpen && suggestions.isNotEmpty()) {
                        SuggestionList(
                            query = name.text,
                            items = suggestions,
                            onPick = {
                                name = TextFieldValue(it, TextRange(it.length))
                                suggestionsOpen = false
                            },
                        )
                    }
                }
                if (!terminal) PstSegmented(
                    options = listOf("Станция", "О.П."),
                    selected = if (kind == ServicePhaseStationKind.HALT) 1 else 0,
                    onSelect = { kind = if (it == 1) ServicePhaseStationKind.HALT else ServicePhaseStationKind.STATION },
                )
                if (!terminal) Column {
                    PstTextField(
                        value = kmText,
                        onValueChange = { v -> kmText = v.copy(text = v.text.filter { it.isDigit() || it == ',' || it == '.' }) },
                        label = "Км от начала плеча",
                        placeholder = "Не указано",
                        suffix = "км",
                        mono = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    )
                    Text(
                        text = if (kmInvalid) {
                            "Км — число больше 0" + if (phaseDistance > 0) " и меньше $phaseDistance" else ""
                        } else {
                            "Необязательно. Помогает поставить станцию на место при повторном подборе."
                        },
                        style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 17.sp),
                        color = if (kmInvalid) Pst.danger else Pst.textMuted,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp),
                    )
                }
                if (station != null || findCoordinates != null || mapNeighbours != null) {
                    CoordinatesCard(
                        station = picked?.let {
                            coordinateStation.copy(latitude = it.latitude, longitude = it.longitude, source = ServicePhaseStationSource.MAP)
                        } ?: coordinateStation,
                        state = coords,
                        neighbours = neighbours,
                        onPickOnMap = if (mapNeighbours != null) ({ mapOpen = true }) else null,
                        notFoundName = name.text.trim().ifBlank { coordinateStation.name },
                        fromDirectory = dirHit != null,
                        onWriteSupport = onWriteSupport,
                        onFind = findCoordinates?.let { find ->
                            {
                                val query = name.text.trim().ifBlank { coordinateStation.name }
                                coords = CoordState.Searching(query)
                                searchJob = scope.launch {
                                    val found = find(query)
                                    lookupCandidates = found.orEmpty()
                                    coords = when {
                                        found.isNullOrEmpty() -> CoordState.NotFound
                                        // Не выбирать автоматически при нескольких совпадениях или неточном совпадении.
                                        found.size > 1 || found.any { it.approximate } -> {
                                            choosing = found
                                            CoordState.Idle
                                        }
                                        else -> CoordState.Found(found.first())
                                    }
                                }
                            }
                        },
                        onCancelSearch = {
                            searchJob?.cancel()
                            coords = CoordState.Idle
                        },
                        // «Не та станция»: шторка выбора среди найденных одноимённых/похожих.
                        onReject = {
                            if (lookupCandidates.isEmpty()) {
                                coords = CoordState.Idle
                            } else {
                                rejecting = true
                                choosing = lookupCandidates
                            }
                        },
                    )
                    if (station?.source == ServicePhaseStationSource.OSM) {
                        Text(
                            text = "Добавлена автоподбором" + (station.esr?.let { " · ЕСР $it" } ?: ""),
                            style = Pst.mono.copy(fontSize = 11.sp, letterSpacing = 0.2.sp),
                            color = Pst.textFaint,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }
            PstSheetFooter {
                PstButton(
                    text = "Готово",
                    kind = PstButtonKind.CTA,
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val trimmed = name.text.trim()
                        val kmValue = km?.let { kotlin.math.round(it * 10) / 10 }
                        val found = (coords as? CoordState.Found)?.candidate
                        val point = picked ?: dirHit?.let { PickedPoint(it.latitude!!, it.longitude!!, null) }
                        val result = when {
                            station == null -> ServicePhaseStation(
                                id = newStationDraft.id,
                                name = trimmed,
                                kind = kind,
                                kmFromStart = kmValue,
                                latitude = point?.latitude ?: found?.latitude,
                                longitude = point?.longitude ?: found?.longitude,
                                esr = found?.esr?.takeIf(String::isNotBlank),
                                osmId = found?.osmId,
                                source = if (point != null) ServicePhaseStationSource.MAP
                                else if (found != null) ServicePhaseStationSource.OSM
                                else ServicePhaseStationSource.MANUAL,
                            )
                            point != null -> station.copy(
                                name = trimmed, kind = kind, kmFromStart = kmValue,
                                latitude = point.latitude, longitude = point.longitude,
                                source = ServicePhaseStationSource.MAP,
                            )
                            else -> station.copy(
                                name = trimmed,
                                kind = kind,
                                kmFromStart = kmValue,
                                latitude = found?.latitude ?: station.latitude,
                                longitude = found?.longitude ?: station.longitude,
                                esr = station.esr ?: found?.esr?.takeIf(String::isNotBlank),
                                osmId = found?.osmId ?: station.osmId,
                            )
                        }
                        onSave(result)
                        onDismiss()
                    },
                )
                if (station != null && onDelete != null && !terminal) {
                    PstButton(
                        text = "Удалить станцию",
                        kind = PstButtonKind.DANGER,
                        icon = PstIcons.Trash,
                        iconSize = 16.dp,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        onClick = { confirmDelete = true },
                    )
                }
            }
        }
    }
}

internal sealed interface CoordState {
    data object Idle : CoordState
    data class Searching(val query: String) : CoordState
    data class Found(val candidate: OsmStationCandidate) : CoordState
    data object NotFound : CoordState
}

@Composable
private fun CoordinatesCard(
    station: ServicePhaseStation,
    state: CoordState,
    neighbours: Pair<String?, String?>,
    onPickOnMap: (() -> Unit)?,
    onFind: (() -> Unit)?,
    onCancelSearch: () -> Unit,
    onReject: () -> Unit,
    notFoundName: String = station.name,
    fromDirectory: Boolean = false,
    onWriteSupport: ((String) -> Unit)? = null,
) {
    val has = station.hasCoordinates
    PstCard {
        Column {
            when (state) {
                is CoordState.Searching -> CoordRow(
                    color = Pst.accent,
                    spinner = true,
                    title = "Ищем «${state.query}» на карте…",
                    subtitle = "Обычно несколько секунд",
                    trailing = {
                        PstButton(text = "Отменить", onClick = onCancelSearch, kind = PstButtonKind.TEXT, height = 36.dp, fontSize = 14)
                    },
                )
                is CoordState.Found -> {
                    val c = state.candidate
                    val between = listOfNotNull(neighbours.first, neighbours.second)
                    val place = listOfNotNull(
                        c.railwayLabel() ?: c.operator ?: c.region,
                        between.takeIf { it.size == 2 }?.let { "между ${it[0]} и ${it[1]}" },
                    ).joinToString(" · ")
                    CoordRow(
                        color = Pst.success,
                        title = "Нашлось: ${c.name}, ${if (c.kind == "halt") "О.П." else "станция"}",
                        subtitle = place.ifBlank { c.esr?.let { "ЕСР $it" } ?: "" },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        PstButton(text = "Выбрать другую", onClick = onReject, kind = PstButtonKind.TEXT, height = 36.dp, fontSize = 14)
                    }
                    if (onFind != null) {
                        PstButton(
                            text = "Определить заново",
                            onClick = onFind,
                            kind = PstButtonKind.OUTLINED,
                            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        )
                    }
                }
                CoordState.NotFound -> {
                    CoordRow(
                        color = Pst.warning,
                        title = "Станция «$notFoundName» не найдена на карте",
                        subtitle = "Напишите в техническую поддержку — мы добавим её. Или укажите точку на карте вручную.",
                    )
                    if (onWriteSupport != null) {
                        PstButton(
                            text = WRITE_TO_SUPPORT,
                            onClick = { onWriteSupport(notFoundName) },
                            kind = PstButtonKind.TONAL,
                            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
                        )
                    }
                    if (onFind != null) {
                        PstButton(
                            text = if (has) "Определить заново" else "Искать ещё раз",
                            onClick = onFind,
                            kind = PstButtonKind.OUTLINED,
                            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        )
                    }
                }
                CoordState.Idle -> {
                    CoordRow(
                        color = if (has) Pst.success else Pst.warning,
                        title = if (has) "Координаты есть" else "Нет координат",
                        subtitle = when {
                            !has -> "Без них станция не определится по GPS"
                            fromDirectory -> "Из справочника станций"
                            station.source == ServicePhaseStationSource.MAP -> "Указаны на карте"
                            else -> "Станция определится по GPS"
                        },
                    )
                    if (onFind != null) {
                        PstButton(
                            text = if (has) "Определить заново" else "Найти координаты",
                            onClick = onFind,
                            kind = if (has) PstButtonKind.OUTLINED else PstButtonKind.TONAL,
                            icon = if (has) null else PstIcons.Search,
                            iconSize = if (has) 0.dp else 17.dp,
                            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        )
                    }
                }
            }
            if (onPickOnMap != null) {
                HorizontalDivider(color = Pst.border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { onPickOnMap() }
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                        PstIcon(PstIcons.MapPin, 18.dp, Pst.accent)
                    }
                    Text(
                        text = if (has) "Уточнить на карте" else "Указать на карте",
                        style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        color = Pst.accent,
                        modifier = Modifier.weight(1f),
                    )
                    PstIcon(PstIcons.ChevronRight, 14.dp, Pst.textFaint)
                }
            }
        }
    }
}

@Composable
private fun CoordRow(
    color: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    spinner: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(if (spinner) androidx.compose.ui.graphics.Color.Transparent else color.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (spinner) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Pst.accent, trackColor = Pst.accent.copy(alpha = 0.22f))
            } else {
                PstIcon(PstIcons.MapPin, 18.dp, color)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Pst.textMuted)
            }
        }
        trailing?.invoke()
    }
}

@Composable
internal fun SuggestionList(query: String, items: List<String>, onPick: (String) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .shadow(3.dp, shape)
            .background(Pst.surface, shape),
    ) {
        items.forEachIndexed { i, item ->
            if (i > 0) HorizontalDivider(color = Pst.border, modifier = Modifier.padding(start = 16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(item) }
                    .padding(horizontal = 16.dp, vertical = 11.dp),
            ) {
                Text(text = highlight(item, query), style = Pst.sans.copy(fontSize = 15.sp), color = Pst.text)
                Text(text = "из справочника", style = Pst.sans.copy(fontSize = 12.sp), color = Pst.textMuted)
            }
        }
    }
}

private fun highlight(item: String, query: String) = buildAnnotatedString {
    val q = query.trim()
    val start = matchStart(item, q)
    if (q.isEmpty() || start < 0) {
        append(item)
        return@buildAnnotatedString
    }
    append(item.substring(0, start))
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(item.substring(start, start + q.length)) }
    append(item.substring(start + q.length))
}

/**
 * Начало слова, с которого совпадает запрос (слова делятся пробелом и дефисом); -1 — нет.
 * Без учёта регистра и е/ё (замена один к одному — позиции для подсветки сохраняются).
 */
internal fun matchStart(item: String, query: String): Int {
    if (query.isEmpty()) return -1
    val text = foldYo(item)
    val q = foldYo(query)
    var i = 0
    while (i <= text.length - q.length) {
        val wordStart = i == 0 || text[i - 1] == ' ' || text[i - 1] == '-'
        if (wordStart && text.regionMatches(i, q, 0, q.length, ignoreCase = true)) return i
        i++
    }
    return -1
}

internal fun suggest(stationList: List<String>, query: String): List<String> {
    val q = query.trim()
    if (q.isEmpty()) return emptyList()
    return stationList
        .filter { !foldYo(it).equals(foldYo(q), ignoreCase = true) && matchStart(it, q) >= 0 }
        .take(5)
}

private fun foldYo(s: String) = s.replace('ё', 'е').replace('Ё', 'Е')

private fun formatKmInput(km: Double): String =
    (kotlin.math.round(km * 10) / 10).toString().removeSuffix(".0").replace('.', ',')

// ─── Каркас шторки: грэббер, шапка, закреплённый футер ─────────────────

@Composable
internal fun PstGrabber() {
    Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 36.dp, height = 4.dp).background(Pst.borderStrong, RoundedCornerShape(3.dp)))
    }
}

@Composable
internal fun PstSheetHeader(title: String, onClose: () -> Unit, subtitle: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = Pst.sans.copy(fontSize = 22.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.2).sp),
                color = Pst.text,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = Pst.sans.copy(fontSize = 13.sp),
                    color = Pst.textMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Box(
            modifier = Modifier.size(40.dp).clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) { PstIcon(PstIcons.Close, 22.dp, Pst.textMuted, description = "Закрыть") }
    }
}

@Composable
internal fun PstSheetFooter(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = Pst.border)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Pst.bg)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}
