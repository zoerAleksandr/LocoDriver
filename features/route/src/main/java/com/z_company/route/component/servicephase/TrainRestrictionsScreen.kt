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
import androidx.compose.foundation.lazy.itemsIndexed
import com.z_company.domain.onboarding.HintId
import com.z_company.domain.onboarding.OnboardingHints
import com.z_company.domain.onboarding.RestrictionsTourStep
import com.z_company.route.component.CoachTour
import com.z_company.route.component.TourStep
import com.z_company.route.component.coachAnchor
import com.z_company.route.component.separateSheetFromScroll
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
import com.z_company.domain.track.LegWarnings
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
 * «ДУ-61» (раздел «Поезд», бывшие «Ограничения поезда»): журнал проследования (заход головой и выход всем составом, скорость, максимум,
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
    // Порядок станций плеча по ходу движения: по нему сортируется список и подбираются прошлые предупреждения.
    val order = remember(stops, legFrom, legTo) { stops.map { it.name }.filter { it.isNotBlank() }.ifEmpty { listOfNotNull(legFrom, legTo) } }
    val warningStore = remember { TrackWarningStore(context.applicationContext) }
    var pastAll by remember { mutableStateOf(emptyList<TrackWarning>()) }
    LaunchedEffect(trainId, legSelected) {
        pastAll = if (legSelected) withContext(Dispatchers.Default) { warningStore.loadEvery() } else emptyList()
    }
    val pastCandidates = remember(pastAll, entries, order) {
        val active = entries.filter { !it.isDeleted }.map { it.warningId }.toSet()
        val t = trains.firstOrNull { it.id == trainId }
        val planned = pastAll.filter { it.id !in active && LegWarnings.matches(it.place, order) }
            .map { RestrictionPass.planned(trainId, t?.routeId.orEmpty(), it, 0L, t?.lengthM) }
        sortRestrictions(planned, order)
    }
    var pastChecked by remember { mutableStateOf(emptySet<String>()) }
    // «Из прошлых поездок» сворачивается кнопкой «Скрыть»; состояние общее для всех экранов ДУ-61 и помнится на устройстве.
    val past = rememberPastHidden()
    val placements by rememberPlacements(entries, stops)
    val rows = remember(entries, zone) { RestrictionReport.rows(entries, zone) }
    val ready = RestrictionReport.isReady(entries)
    val left = RestrictionReport.remaining(entries)

    // Экскурсия по экрану: один раз при первом открытии, «?» в шапке открывает снова.
    val onboardingHints = com.z_company.route.component.rememberOnboardingHints()
    val tourAnchors = remember { RestrictionsTourStep.entries.associateWith { com.z_company.route.component.CoachAnchor() } }
    var tourOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (OnboardingHints.canShow(
                id = HintId.RESTRICTIONS_SCREEN,
                shown = onboardingHints.shown, homeReached = onboardingHints.homeReached, recording = false,
            )
        ) {
            tourOpen = true
            onboardingHints.markShown(HintId.RESTRICTIONS_SCREEN)
        }
    }

    Dialog(onDismissRequest = { if (tourOpen) tourOpen = false else onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
      Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().background(Pst.bg).statusBarsPadding().navigationBarsPadding().imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.z_company.route.component.HeaderBackTile(onDismiss)
                Column(Modifier.weight(1f)) {
                    Text("ДУ-61", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
                    Text(trainTitle, style = Pst.sans.copy(fontSize = 12.sp), color = Pst.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.coachAnchor(tourAnchors.getValue(RestrictionsTourStep.HELP))) {
                    com.z_company.route.component.HeaderTile(onClick = { tourOpen = true }, description = "Подсказки") {
                        Text("?", style = Pst.sans.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold), color = Pst.textMuted)
                    }
                }
            }
            LegBlock(leg, Modifier.coachAnchor(tourAnchors.getValue(RestrictionsTourStep.LEG)))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PstButton(
                    "Добавить", { showAdd = true }, kind = PstButtonKind.FILLED, enabled = legSelected, height = 44.dp, fontSize = 14,
                    modifier = Modifier.weight(1f).coachAnchor(tourAnchors.getValue(RestrictionsTourStep.ADD)),
                )
                PstButton(
                    if (exporting) "Формируем…" else "Отчёт PDF",
                    {
                        exporting = true
                        scope.launch {
                            try {
                                val file = withContext(Dispatchers.IO) { RestrictionPdfGenerator(context).generate(trainTitle, rows, RestrictionReport.progress(entries)) }
                                pdfUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Не удалось сформировать PDF", Toast.LENGTH_LONG).show()
                            } finally {
                                exporting = false
                            }
                        }
                    },
                    kind = PstButtonKind.TONAL, enabled = rows.isNotEmpty() && !exporting, height = 44.dp, fontSize = 14,
                    modifier = Modifier.weight(1f).coachAnchor(tourAnchors.getValue(RestrictionsTourStep.PDF)),
                )
            }
            val hint = when {
                !legSelected -> "Чтобы добавлять ограничения, выберите плечо поезда: участок определяется по нему."
                rows.isEmpty() -> "Ограничений нет. «Добавить» открывает «Где действует»: выберите станцию или перегон; при включённой «Автозаписи» время и скорость проследования запишутся сами."
                !ready -> "Отчёт можно сформировать в любое время: непроследованные ограничения будут без времени (осталось проследовать: $left)."
                entries.none { it.tracked && !it.isDeleted } -> "Ограничения без участка автоматически не отслеживаются — в отчёте они помечены. Отчёт можно сформировать."
                else -> "Все отслеживаемые ограничения проследованы — можно сформировать отчёт."
            }
            Text(hint, style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Pst.textMuted)
            if (!legSelected) {
                Text("Выберите плечо для подбора предупреждений из прошлых поездок", style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Pst.textMuted)
            }
            // Порядок по ходу движения: места плеча (станции и перегоны) по порядку станций плеча; ограничения без места плеча - в конце.
            val sorted = remember(entries, order) { sortRestrictions(entries, order) }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pastCandidates.isNotEmpty()) item(key = "past_offer") {
                    PastRestrictionsBlock(
                        title = "Из прошлых поездок",
                        rows = pastCandidates.map { c ->
                            val row = RestrictionReport.rows(listOf(c), zone).first()
                            val isSpeed = c.kind == "speed"
                            PastRow(c.warningId, row.place.ifBlank { row.title }, row.limit.takeIf { isSpeed }, row.kmText, row.title.takeIf { !isSpeed })
                        },
                        checked = pastChecked,
                        hidden = past.hidden,
                        onToggleHidden = past.toggle,
                        onToggle = { id -> pastChecked = if (id in pastChecked) pastChecked - id else pastChecked + id },
                        onSelectAll = { pastChecked = pastCandidates.map { it.warningId }.toSet() },
                        onClear = { pastChecked = emptySet() },
                        onApply = {
                            val chosen = pastAll.filter { it.id in pastChecked }
                            val t = trains.firstOrNull { it.id == trainId }
                            pastChecked = emptySet()
                            scope.launch(Dispatchers.Default) {
                                runCatching {
                                    chosen.forEach { w ->
                                        val old = store.peek(trainId, w.id)
                                        if (old != null && !old.isDeleted) return@forEach
                                        store.upsert(RestrictionPass.planned(trainId, t?.routeId.orEmpty(), w, maxOf(System.currentTimeMillis(), (old?.updatedAt ?: 0L) + 1), t?.lengthM))
                                    }
                                    entries = store.forTrain(trainId)
                                }
                                upload.trigger()
                            }
                        },
                    )
                }
                itemsIndexed(sorted, key = { _, e -> e.warningId }) { index, e ->
                    val row = RestrictionReport.rows(listOf(e), zone).first()
                    // Тот же свайп-удаление, что у маршрутов на Главном (красная кнопка, затем подтверждение); нажатие - правка.
                    com.z_company.route.component.SwipeToRevealDelete(
                        modifier = Modifier.fillMaxWidth().let { if (index == 0) it.coachAnchor(tourAnchors.getValue(RestrictionsTourStep.LIST)) else it },
                        onDeleteClick = { toDelete = e },
                        onContentClick = { editing = e },
                        backgroundVerticalPadding = 0.dp,
                        itemKey = e.warningId,
                    ) { _ -> RestrictionCard(row, e, placementNote(placements[e.warningId])) }
                }
            }
        }
        if (tourOpen) {
            CoachTour(
                steps = RestrictionsTourStep.entries.map { st ->
                    TourStep(st.title, st.text, tourAnchors.getValue(st), present = st != RestrictionsTourStep.LIST || entries.isNotEmpty())
                },
                onDismiss = { tourOpen = false },
            )
        }
      }
    }

    editing?.let { e ->
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

/** Строка прошлого ограничения для [PastRestrictionsBlock]: одинаково выглядит в «Ограничениях поезда» и в ДУ-61 на карте. */
internal class PastRow(val id: String, val place: String, val limit: String?, val kmText: String?, val boldTitle: String?)

/**
 * Прошлые ограничения плеча/участка - один и тот же блок на обоих экранах ДУ-61: заголовок с «Скрыть/Показать», строки
 * (`RestrictionItem`) с чекбоксами, внизу «Выбрать все», «Отменить» (снять выбор) и «Добавить выбранные (N)».
 * [onForget] - свайп «убрать из прошлых» (есть только в ДУ-61 на карте).
 */
@Composable
internal fun PastRestrictionsBlock(
    title: String,
    rows: List<PastRow>,
    checked: Set<String>,
    hidden: Boolean,
    onToggleHidden: () -> Unit,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onApply: () -> Unit,
    onForget: ((String) -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PastHeader(
            title = if (hidden) "$title (${rows.size})" else title,
            hidden = hidden, onToggle = onToggleHidden,
            titleStyle = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), titleColor = Pst.text,
        )
        if (hidden) return@Column
        Text(
            "Отметьте действующие и добавьте их в журнал поезда.",
            style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Pst.textMuted,
        )
        rows.forEach { r ->
            val item: @Composable (Modifier) -> Unit = { m ->
                RestrictionItem(
                    place = r.place, limit = r.limit, exceeded = false, kmText = r.kmText, boldTitle = r.boldTitle,
                    modifier = m,
                    leading = { androidx.compose.material3.Checkbox(checked = r.id in checked, onCheckedChange = { onToggle(r.id) }) },
                )
            }
            if (onForget != null) {
                com.z_company.route.component.SwipeToRevealDelete(
                    modifier = Modifier.fillMaxWidth(),
                    onDeleteClick = { onForget(r.id) },
                    onContentClick = { onToggle(r.id) },
                    backgroundVerticalPadding = 0.dp,
                    itemKey = r.id,
                ) { _ -> item(Modifier) }
            } else item(Modifier.clickable { onToggle(r.id) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PstButton("Выбрать все", onSelectAll, kind = PstButtonKind.TONAL, enabled = checked.size < rows.size, height = 40.dp, fontSize = 14, modifier = Modifier.weight(1f))
            PstButton("Отменить", onClear, kind = PstButtonKind.TONAL, enabled = checked.isNotEmpty(), height = 40.dp, fontSize = 14, modifier = Modifier.weight(1f))
        }
        PstButton("Добавить выбранные (${checked.size})", onApply, kind = PstButtonKind.FILLED, enabled = checked.isNotEmpty(), height = 40.dp, fontSize = 14, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun RestrictionCard(row: RestrictionReport.Row, e: RestrictionPass, note: Pair<String, Boolean>? = null) {
    val isSpeed = e.kind == "speed"
    RestrictionItem(
        place = row.place.ifBlank { row.title }, limit = row.limit.takeIf { isSpeed }, exceeded = row.exceeded, kmText = row.kmText,
        boldTitle = row.title.takeIf { !isSpeed },
    ) {
        // Не на карте (нет профиля, серая зона, нет км): выделено, чтобы машинист видел, что это ограничение только в списке.
        note?.let { PlacementTag(it) }
        // Данные проследования - когда они уже есть.
        if (!row.untracked && e.enterAt != null) {
            Text("Заход головой: ${row.enter}", style = Pst.sans.copy(fontSize = 12.sp), color = Pst.text)
            if (isSpeed && e.exitAt != null) Text("Выход составом: ${row.exit}", style = Pst.sans.copy(fontSize = 12.sp), color = Pst.text)
        }
    }
}

/** Число ограничений поезда для значка на нижней панели «Поезда»; журнал меняют и другие экраны, поэтому перечитывается раз в 2 с. */
@Composable
internal fun rememberRestrictionCount(trainId: String?): Int {
    val store: RestrictionJournalStore = koinInject()
    return androidx.compose.runtime.produceState(0, trainId) {
        while (true) {
            value = if (trainId == null) 0 else withContext(Dispatchers.Default) { runCatching { store.forTrain(trainId).size }.getOrDefault(0) }
            delay(2_000)
        }
    }.value
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
    WhereSheet(
        leg = leg,
        places = places.map { WhereItem(it, isStation = " — " !in it) },
        hint = "Выберите станцию или перегон. Когда профиль пути загрузится на карте, ограничение появится на нём.",
        onPick = { place = places[it]; showForm = true },
        onOther = { place = null; showForm = true },
        onClose = onClose,
    )
}

/** Положение места на плече по ходу движения: станция - её номер среди станций плеча, перегон - между номерами концов; нет на плече - в конец. */
/**
 * Порядок ограничений поезда по ходу движения: место на плече, затем км начала (в сторону движения: при убывающем километраже - по убыванию),
 * время захода; общий для «Ограничений поезда» и экрана ДУ-61.
 */
internal fun sortRestrictions(entries: List<RestrictionPass>, order: List<String>): List<RestrictionPass> {
    val live = entries.filter { !it.isDeleted }
    val sign = if (kmDecreasesAlongTravel(live, order)) -1.0 else 1.0
    return live.sortedWith(
        compareBy<RestrictionPass>({ locationKey(it.place, order) }, { it.startAbsKm?.let { km -> km * sign } ?: Double.MAX_VALUE }, { it.enterAt ?: Long.MAX_VALUE }, { it.updatedAt }),
    )
}

/**
 * Убывает ли железнодорожный км по ходу поезда. Главное - сами записи: у мест, идущих по плечу друг за другом, км начала растёт или падает
 * (наклон по номеру места на плече). Место одно - смотрим стороны оси у проследованных записей: направление по оси и соотношение км оси и
 * железнодорожного км. Данных нет - считаем, что растёт.
 */
internal fun kmDecreasesAlongTravel(entries: List<RestrictionPass>, order: List<String>): Boolean {
    val pts = entries.mapNotNull { e ->
        val km = e.startAbsKm ?: return@mapNotNull null
        val loc = locationKey(e.place, order).takeIf { it != Double.MAX_VALUE } ?: return@mapNotNull null
        loc to km
    }
    if (pts.map { it.first }.distinct().size >= 2) {
        val mx = pts.sumOf { it.first } / pts.size
        val my = pts.sumOf { it.second } / pts.size
        val cov = pts.sumOf { (it.first - mx) * (it.second - my) }
        if (cov != 0.0) return cov < 0
    }
    val bySide = entries.sumOf { e ->
        val sa = e.startAbsKm; val ea = e.endAbsKm
        if (e.dir == 0 || !e.tracked || sa == null || ea == null) return@sumOf 0
        e.dir * Math.signum(ea - sa).toInt() * Math.signum(e.endRelKm - e.startRelKm).toInt()
    }
    return bySide < 0
}

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

/** Место для списка «Где действует»: станция или перегон. */
internal class WhereItem(val title: String, val isStation: Boolean)

/**
 * Шторка «Где действует»: станции и перегоны, выбранное идёт в форму ограничения. Одна и та же из раздела «Поезд» и из профиля пути
 * (там места берутся с оси участка). [onOther] - «Другое место» (только в «Поезде»: место вводится вручную).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun WhereSheet(
    leg: LegContext?,
    places: List<WhereItem>,
    hint: String,
    onPick: (Int) -> Unit,
    onOther: (() -> Unit)?,
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
            Text("Где действует", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
            if (leg != null) LegBlock(leg)
            Text(hint, style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Pst.textMuted)
            LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 480.dp).separateSheetFromScroll(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(places.size) { i ->
                    val place = places[i]
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Pst.surface, RoundedCornerShape(12.dp)).clickable { onPick(i) }.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (place.isStation) "СТ" else "ПЕР",
                            style = Pst.mono.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold), color = Pst.textMuted,
                        )
                        Text(
                            place.title, style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Pst.text,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            if (onOther != null) PstButton("Другое место", onOther, kind = PstButtonKind.TONAL, height = 44.dp, fontSize = 14, modifier = Modifier.fillMaxWidth())
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
            LazyColumn(state = listState, modifier = Modifier.weight(1f, fill = false).fillMaxWidth().separateSheetFromScroll(listState)) {
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


/** Состояние «прошлые ограничения скрыты» - одно на все экраны ДУ-61 («Ограничения поезда», ДУ-61 на карте), помнится на устройстве. */
internal class PastHiddenState(val hidden: Boolean, val toggle: () -> Unit)

@Composable
internal fun rememberPastHidden(): PastHiddenState {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.applicationContext.getSharedPreferences("restrictions_ui", android.content.Context.MODE_PRIVATE) }
    var hidden by remember { mutableStateOf(runCatching { prefs.getBoolean("past_hidden", false) }.getOrDefault(false)) }
    return PastHiddenState(hidden) {
        hidden = !hidden
        runCatching { prefs.edit().putBoolean("past_hidden", hidden).apply() }
    }
}

/** Заголовок блока прошлых ограничений с кнопкой «Скрыть» / «Показать» - общий для обоих экранов ДУ-61. */
@Composable
internal fun PastHeader(
    title: String,
    hidden: Boolean,
    onToggle: () -> Unit,
    titleStyle: androidx.compose.ui.text.TextStyle,
    titleColor: androidx.compose.ui.graphics.Color,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = titleStyle, color = titleColor, modifier = Modifier.weight(1f))
        PstButton(if (hidden) "Показать" else "Скрыть", onToggle, kind = PstButtonKind.TEXT, height = 36.dp, fontSize = 14)
    }
}
