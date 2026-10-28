package com.z_company.route.component.servicephase

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.z_company.domain.track.SectionSummary
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.z_company.domain.track.TrackWarning
import com.z_company.domain.track.TrackWarnings
import com.z_company.domain.track.WarningType
import com.z_company.domain.util.generateId
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.z_company.domain.track.RestrictionJournalStore
import com.z_company.domain.track.RestrictionPass
import com.z_company.domain.track.RestrictionReport
import com.z_company.domain.track.RestrictionStatus
import com.z_company.domain.util.TrainMapStop
import com.z_company.route.autorecord.TrackUploadTrigger
import com.z_company.route.component.AppAlertDialog
import com.z_company.route.component.PdfActionSheet
import com.z_company.route.util.RestrictionPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/**
 * «Ограничения поезда» (раздел «Поезд»): журнал проследования (заход головой и выход всем составом, скорость, максимум,
 * превышение), добавление предупреждений (тот же экран «Предупреждения», что и в профиле пути; участок определяется по
 * плечу поезда - добавлять можно, только если плечо выбрано) и PDF-отчёт, когда все ограничения проследованы.
 */
@Composable
internal fun TrainRestrictionsScreen(
    trainId: String,
    trainTitle: String,
    /** Плечо поезда: строка, пометка «проверено на карте» и кнопка «Определить» - одно и то же на всех экранах ДУ-61. */
    leg: LegContext,
    /** Крайние станции плеча (названия). */
    legFrom: String?,
    legTo: String?,
    stops: List<TrainMapStop>,
    trains: List<ProfileTrain>,
    zone: String,
    onDismiss: () -> Unit,
) {
    val store: RestrictionJournalStore = koinInject()
    val upload: TrackUploadTrigger = koinInject()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf(emptyList<RestrictionPass>()) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<RestrictionPass?>(null) }
    var editing by remember { mutableStateOf<RestrictionPass?>(null) }
    var pdfUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var exporting by remember { mutableStateOf(false) }

    // Запись идёт в фоне (АВТО), поэтому список обновляется, пока экран открыт.
    LaunchedEffect(trainId, showAdd) {
        while (true) {
            entries = withContext(Dispatchers.Default) { runCatching { store.forTrain(trainId) }.getOrDefault(emptyList()) }
            delay(2_000)
        }
    }
    val legSelected = leg.title != null
    val rows = remember(entries, zone) { RestrictionReport.rows(entries, zone) }
    val ready = RestrictionReport.isReady(entries)
    val left = RestrictionReport.remaining(entries)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(
            modifier = Modifier.fillMaxSize().background(Pst.bg).statusBarsPadding().navigationBarsPadding().imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MapButton(onClick = onDismiss, description = "Назад") { PstIcon(PstIcons.ChevronLeft, 22.dp, Pst.text) }
                Column(Modifier.weight(1f)) {
                    Text("Ограничения поезда", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
                    Text(trainTitle, style = Pst.sans.copy(fontSize = 12.sp), color = Pst.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            LegBlock(leg)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PstButton(
                    "Добавить", { showAdd = true }, kind = PstButtonKind.FILLED, enabled = legSelected, height = 44.dp, fontSize = 14,
                    modifier = Modifier.weight(1f),
                )
                PstButton(
                    if (exporting) "Формируем…" else "Отчёт PDF",
                    {
                        exporting = true
                        scope.launch {
                            try {
                                val file = withContext(Dispatchers.IO) { RestrictionPdfGenerator(context).generate(trainTitle, rows) }
                                pdfUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Не удалось сформировать PDF", Toast.LENGTH_LONG).show()
                            } finally {
                                exporting = false
                            }
                        }
                    },
                    kind = PstButtonKind.TONAL, enabled = ready && !exporting, height = 44.dp, fontSize = 14, modifier = Modifier.weight(1f),
                )
            }
            val hint = when {
                !legSelected -> "Чтобы добавлять ограничения, выберите плечо поезда: участок определяется по нему."
                rows.isEmpty() -> "Ограничений нет. «Добавить» открывает ДУ-61: выберите станции или перегоны, где действуют предупреждения; при включённом «АВТО» время и скорость проследования запишутся сами."
                !ready -> "Отчёт будет доступен, когда все ограничения проследованы (осталось: $left)."
                entries.none { it.tracked && !it.isDeleted } -> "Ограничения без участка автоматически не отслеживаются — в отчёте они помечены. Отчёт можно сформировать."
                else -> "Все отслеживаемые ограничения проследованы — можно сформировать отчёт."
            }
            Text(hint, style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Pst.textMuted)
            // Порядок по ходу движения: места плеча (станции и перегоны) по порядку станций плеча; ограничения без места плеча - в конце.
            val order = remember(stops, legFrom, legTo) { stops.map { it.name }.filter { it.isNotBlank() }.ifEmpty { listOfNotNull(legFrom, legTo) } }
            val sorted = remember(entries, order) { sortRestrictions(entries, order) }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sorted, key = { it.warningId }) { e ->
                    val row = RestrictionReport.rows(listOf(e), zone).first()
                    // Тот же свайп-удаление, что у маршрутов на Главном (красная кнопка, затем подтверждение); нажатие - правка.
                    com.z_company.route.component.SwipeToRevealDelete(
                        modifier = Modifier.fillMaxWidth(),
                        onDeleteClick = { toDelete = e },
                        onContentClick = { editing = e },
                        backgroundVerticalPadding = 0.dp,
                        itemKey = e.warningId,
                    ) { _ -> RestrictionCard(row, e) }
                }
            }
        }
    }

    editing?.let { e ->
        val warningStore = remember { TrackWarningStore(context.applicationContext) }
        RestrictionFormSheet(
            sectionId = e.sectionId,
            place = e.place,
            existing = e.toWarning(),
            kmRequired = e.startAbsKm != null || !e.tracked,
            onSave = { w ->
                runCatching { applyRestrictionEdit(store, warningStore, e, w, System.currentTimeMillis()) }
                editing = null
                upload.trigger()
                null
            },
            onDelete = { toDelete = e; editing = null },
            onDismiss = { editing = null },
        )
    }
    if (showAdd) {
        RestrictionAddHost(trainId = trainId, stops = stops, trains = trains, legFrom = legFrom, legTo = legTo, leg = leg, onClose = { showAdd = false; upload.trigger() })
    }
    toDelete?.let { e ->
        AppAlertDialog(
            onDismissRequest = { toDelete = null },
            title = "Убрать ограничение из поезда?",
            text = if (e.hasData) "Записанные время и скорость проследования будут удалены." else "Запись ДУ-61 останется на участке, но этот поезд её больше не учитывает.",
            confirmText = "Убрать",
            isDestructive = true,
            dismissText = "Отмена",
            onConfirm = {
                toDelete = null
                scope.launch(Dispatchers.Default) {
                    runCatching { store.remove(e.trainId, e.warningId, System.currentTimeMillis()) }
                    entries = runCatching { store.forTrain(trainId) }.getOrDefault(emptyList())
                    upload.trigger()
                }
            },
        )
    }
    pdfUri?.let { PdfActionSheet(uri = it, onDismiss = { pdfUri = null }) }
}

@Composable
internal fun RestrictionCard(row: RestrictionReport.Row, e: RestrictionPass) {
    val isSpeed = e.kind == "speed"
    RestrictionItem(
        place = row.place.ifBlank { row.title }, limit = row.limit.takeIf { isSpeed }, exceeded = row.exceeded, kmText = row.kmText,
        boldTitle = row.title.takeIf { !isSpeed },
    ) {
        // Данные проследования - когда они уже есть.
        if (!row.untracked && e.enterAt != null) {
            Text("Заход головой: ${row.enter}", style = Pst.sans.copy(fontSize = 12.sp), color = Pst.text)
            if (isSpeed && e.exitAt != null) Text("Выход составом: ${row.exit}", style = Pst.sans.copy(fontSize = 12.sp), color = Pst.text)
        }
    }
}

/** Карточка ограничения - общая для «Ограничений поезда» и списка добавленных на экране ДУ-61 (профиль пути, карта). */
@Composable
internal fun RestrictionItem(
    place: String,
    limit: String?,
    exceeded: Boolean,
    kmText: String?,
    boldTitle: String?,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    extra: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().background(Pst.surface, RoundedCornerShape(14.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    place, style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium), color = Pst.text,
                    modifier = Modifier.weight(1f), maxLines = 2,
                )
                if (limit != null) {
                    Text(limit, style = Pst.mono.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = if (exceeded) Pst.danger else Pst.text)
                }
            }
            kmText?.let { Text(it, style = Pst.sans.copy(fontSize = 12.sp), color = Pst.textMuted) }
            // Опускание, выключение тока, прочее: жирным вид (для прочего - текст предупреждения).
            if (boldTitle != null) Text(boldTitle, style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Pst.text)
            extra()
        }
    }
}

/**
 * Добавление ДУ-61 из раздела «Поезд»: профиль пути НЕ загружается (сеть не нужна, лимиты загрузки профилей не тратятся). Место берётся из
 * станций плеча (станции и перегоны), начало километра/пикета обязательно, конец - нет (тогда действует один пикет, 100 м). Запись идёт в
 * журнал поезда без участка; когда карта или профиль загрузят подходящий участок, запись подцепится к нему по километру
 * ([ProfileModel] `adoptManualRestrictions`) и начнёт отслеживаться автоматически.
 */
@Composable
private fun RestrictionAddHost(
    trainId: String,
    stops: List<TrainMapStop>,
    trains: List<ProfileTrain>,
    legFrom: String?,
    legTo: String?,
    leg: LegContext,
    onClose: () -> Unit,
) {
    val store: RestrictionJournalStore = koinInject()
    val hasLeg = legFrom != null && legTo != null
    val places = remember(stops, legFrom, legTo) { legPlaces(stops, legFrom, legTo) }
    var showForm by rememberSaveable { mutableStateOf(false) }
    var place by rememberSaveable { mutableStateOf<String?>(null) }
    if (showForm) {
        val t = trains.firstOrNull { it.id == trainId }
        RestrictionFormSheet(
            sectionId = RestrictionPass.NO_SECTION,
            place = place ?: if (hasLeg) "$legFrom — $legTo" else "",
            onSave = { w ->
                runCatching {
                    val old = store.peek(trainId, w.id)
                    store.upsert(RestrictionPass.planned(trainId, t?.routeId.orEmpty(), w, maxOf(System.currentTimeMillis(), (old?.updatedAt ?: 0L) + 1), t?.lengthM))
                }
                showForm = false
                onClose()
                null
            },
            onDismiss = { showForm = false },
        )
        return
    }
    if (!hasLeg) {
        NoSectionDialog("Выберите плечо поезда: места ДУ-61 берутся из его станций.", onClose, { place = null; showForm = true }, onSelectLeg = leg.onSelect)
        return
    }
    LegPlacesDialog(
        leg = leg,
        places = places,
        onPick = { place = it; showForm = true },
        onOther = { place = null; showForm = true },
        onClose = onClose,
    )
}

/** Положение места на плече по ходу движения: станция - её номер среди станций плеча, перегон - между номерами концов; нет на плече - в конец. */
/** Порядок ограничений поезда по ходу движения: место на плече, затем км начала, время захода; общий для «Ограничений поезда» и экрана ДУ-61. */
internal fun sortRestrictions(entries: List<RestrictionPass>, order: List<String>): List<RestrictionPass> =
    entries.filter { !it.isDeleted }.sortedWith(
        compareBy<RestrictionPass>({ locationKey(it.place, order) }, { it.startAbsKm ?: Double.MAX_VALUE }, { it.enterAt ?: Long.MAX_VALUE }, { it.updatedAt }),
    )

/** Правка ограничения поезда формой ДУ-61: журнал поезда и история участка обновляются одинаково из любого экрана. */
internal fun applyRestrictionEdit(
    store: RestrictionJournalStore,
    warningStore: TrackWarningStore,
    e: RestrictionPass,
    w: TrackWarning,
    nowMs: Long,
) {
    val updated = e.withWarning(w, maxOf(nowMs, e.updatedAt + 1))
    if (e.tracked && updated.sectionId == RestrictionPass.NO_SECTION) warningStore.removeEverywhere(e.sectionId, e.warningId)
    else if (e.tracked) warningStore.updateEverywhere(w)
    store.upsert(updated)
}

internal fun locationKey(place: String, order: List<String>): Double {
    val names = place.split(" — ").map { it.trim() }.filter { it.isNotEmpty() }
    val idx = names.mapNotNull { n -> order.indexOfFirst { com.z_company.domain.track.SectionPicker.sameStation(it, n) }.takeIf { it >= 0 } }
    return when {
        idx.isEmpty() -> Double.MAX_VALUE
        names.size == 2 && idx.size == 2 -> idx.min() + 0.5
        else -> idx.first().toDouble()
    }
}

internal fun RestrictionPass.toWarning() = TrackWarning(
    id = warningId, sectionId = sectionId, startAbsKm = startAbsKm, endAbsKm = endAbsKm, startRelKm = startRelKm, endRelKm = endRelKm,
    type = WarningType.entries.firstOrNull { com.z_company.domain.track.RestrictionKind.of(it) == kind } ?: WarningType.OTHER,
    speedKmh = limitKmh, note = note, place = place,
)

/**
 * Запись с правками из формы. Километры изменились у записи с участком - ось-привязка (км оси) устарела: запись становится «без участка»
 * и подцепится заново при открытии профиля пути (данные проследования, если были, остаются).
 */
internal fun RestrictionPass.withWarning(w: TrackWarning, nowMs: Long): RestrictionPass {
    val kmChanged = w.startAbsKm != startAbsKm || w.endAbsKm != endAbsKm
    val untrack = tracked && kmChanged
    return copy(
        sectionId = if (untrack) RestrictionPass.NO_SECTION else sectionId,
        startRelKm = if (untrack) 0.0 else startRelKm,
        endRelKm = if (untrack) 0.0 else endRelKm,
        kind = com.z_company.domain.track.RestrictionKind.of(w.type),
        title = w.label.take(RestrictionPass.MAX_TEXT),
        place = w.place.take(RestrictionPass.MAX_TEXT),
        note = w.note.take(RestrictionPass.MAX_NOTE),
        limitKmh = if (w.type == WarningType.SPEED) w.speedKmh else null,
        startAbsKm = w.startAbsKm,
        endAbsKm = w.endAbsKm,
        exceeded = RestrictionPass.exceeded(maxSpeedKmh, if (w.type == WarningType.SPEED) w.speedKmh else null),
        updatedAt = nowMs,
    )
}

/** Места ДУ-61 по плечу: станции (с промежуточными) и перегоны между соседними; нет станций с координатами - концы плеча. */
private fun legPlaces(stops: List<TrainMapStop>, legFrom: String?, legTo: String?): List<String> {
    val names = stops.map { it.name }.filter { it.isNotBlank() }.ifEmpty { listOfNotNull(legFrom, legTo) }
    if (names.isEmpty()) return emptyList()
    return buildList {
        names.forEachIndexed { i, n ->
            add(n)
            names.getOrNull(i + 1)?.let { add("$n — $it") }
        }
    }
}

/** Места ДУ-61 плеча: станции и перегоны; выбранное идёт в форму ограничения. Шторка, как и форма. Профиль пути не нужен. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun LegPlacesDialog(
    leg: LegContext,
    places: List<String>,
    onPick: (String) -> Unit,
    onOther: () -> Unit,
    onClose: () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Pst.bg,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("ДУ-61: где действует", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
            LegBlock(leg)
            Text(
                "Выберите станцию или перегон. Когда профиль пути загрузится на карте, ограничение появится на нём.",
                style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Pst.textMuted,
            )
            LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(places) { place ->
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Pst.surface, RoundedCornerShape(12.dp)).clickable { onPick(place) }.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (" — " in place) "ПЕР" else "СТ",
                            style = Pst.mono.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold), color = Pst.textMuted,
                        )
                        Text(
                            place, style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Pst.text,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            PstButton("Другое место", onOther, kind = PstButtonKind.TONAL, height = 44.dp, fontSize = 14, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Плечо не выбрано: предложить выбрать плечо (или добавить ограничение с вручную введённым местом). */
@Composable
private fun NoSectionDialog(text: String, onClose: () -> Unit, onManual: () -> Unit, onSelectLeg: (() -> Unit)? = null) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth(0.9f).background(Pst.bg, RoundedCornerShape(20.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text, style = Pst.sans.copy(fontSize = 14.sp, lineHeight = 20.sp), color = Pst.text)
            PstButton("Ввести место вручную", onManual, kind = PstButtonKind.FILLED, height = 44.dp, fontSize = 14, modifier = Modifier.fillMaxWidth())
            if (onSelectLeg != null) PstButton("Выбрать плечо", { onClose(); onSelectLeg() }, kind = PstButtonKind.TONAL, height = 44.dp, fontSize = 14, modifier = Modifier.fillMaxWidth())
            PstButton("Закрыть", onClose, kind = PstButtonKind.TEXT, height = 40.dp, fontSize = 14, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * Шторка выбора участка плеча - в том же стиле, что окно «Плечи» формы «Поезд»: ModalBottomSheet с ручкой, заголовок с кнопкой закрытия,
 * прокручиваемый список; закрывается свайпом вниз, тапом вне шторки и крестиком. Высота ограничена, длинный список прокручивается.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun LegSectionsSheet(
    title: String,
    subtitle: String?,
    sections: List<SectionSummary>,
    onPick: (SectionSummary) -> Unit,
    onDismiss: () -> Unit,
    onManual: (() -> Unit)? = null,
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(bottom = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = Pst.sans.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = Pst.text)
                    if (subtitle != null) Text(subtitle, style = Pst.sans.copy(fontSize = 12.sp), color = Pst.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Pst.surface).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { PstIcon(PstIcons.Close, 18.dp, Pst.text) }
            }
            LazyColumn(state = listState, modifier = Modifier.weight(1f, fill = false).fillMaxWidth()) {
                items(sections, key = { it.id }) { s ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(s) }.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            s.name, style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium), color = Pst.text,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        )
                        Text("${s.lengthKm.toInt()} км", style = Pst.mono.copy(fontSize = 12.sp), color = Pst.textMuted)
                    }
                    androidx.compose.material3.HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = Pst.textMuted.copy(alpha = 0.3f))
                }
            }
            if (onManual != null) {
                PstButton(
                    "Добавить без участка", onManual, kind = PstButtonKind.TEXT, height = 44.dp, fontSize = 14,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun InfoDialog(text: String, onClose: () -> Unit, actionText: String? = null, onAction: (() -> Unit)? = null) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth(0.85f).background(Pst.bg, RoundedCornerShape(20.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text, style = Pst.sans.copy(fontSize = 14.sp, lineHeight = 20.sp), color = Pst.text)
            if (actionText != null && onAction != null) {
                PstButton(actionText, { onClose(); onAction() }, kind = PstButtonKind.FILLED, height = 40.dp, fontSize = 14)
                PstButton("Закрыть", onClose, kind = PstButtonKind.TEXT, height = 40.dp, fontSize = 14)
            } else {
                PstButton("Понятно", onClose, kind = PstButtonKind.FILLED, height = 40.dp, fontSize = 14)
            }
        }
    }
}
