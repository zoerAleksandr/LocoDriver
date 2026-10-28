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
    /** Плечо поезда, например «Лужская — Гатчина»; null - не выбрано (добавлять ограничения нельзя, пока не выбрано). */
    legTitle: String?,
    /** Крайние станции плеча (названия): по ним ищется участок независимо от координат станций. */
    legFrom: String?,
    legTo: String?,
    /** У плеча нет промежуточных станций в справочнике: предложить загрузить (то же окно плеча и автоподбор, что в справочнике). */
    canLoadStations: Boolean,
    onLoadStations: () -> Unit,
    /** Открыть существующий выбор плеча «Поезда» (результат сохраняется в поезд тем же путём и сразу виден в «Поезде», на карте и профиле). */
    onSelectLeg: () -> Unit,
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
    var pdfUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var exporting by remember { mutableStateOf(false) }

    // Запись идёт в фоне (АВТО), поэтому список обновляется, пока экран открыт.
    LaunchedEffect(trainId, showAdd) {
        while (true) {
            entries = withContext(Dispatchers.Default) { runCatching { store.forTrain(trainId) }.getOrDefault(emptyList()) }
            delay(2_000)
        }
    }
    val legSelected = legTitle != null
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
            LegRow(title = legTitle, onSelectLeg = onSelectLeg)
            if (legSelected && canLoadStations) {
                Row(
                    Modifier.fillMaxWidth().background(Pst.surface, RoundedCornerShape(12.dp)).padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "У плеча не загружены промежуточные станции: с ними участок и места ограничений находятся точнее.",
                        style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Pst.textMuted, modifier = Modifier.weight(1f),
                    )
                    PstButton("Загрузить", onLoadStations, kind = PstButtonKind.TONAL, height = 40.dp, fontSize = 14)
                }
            }
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
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries.filter { !it.isDeleted }.sortedWith(compareBy({ it.enterAt ?: Long.MAX_VALUE }, { it.updatedAt })), key = { it.warningId }) { e ->
                    val row = RestrictionReport.rows(listOf(e), zone).first()
                    RestrictionCard(row, e) { toDelete = e }
                }
            }
        }
    }

    if (showAdd) {
        RestrictionAddHost(trainId = trainId, stops = stops, trains = trains, legFrom = legFrom, legTo = legTo, onSelectLeg = onSelectLeg, onClose = { showAdd = false; upload.trigger() })
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
private fun RestrictionCard(row: RestrictionReport.Row, e: RestrictionPass, onRemove: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Pst.surface, RoundedCornerShape(14.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(row.title, style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Pst.text, modifier = Modifier.weight(1f), maxLines = 2)
            if (row.exceeded) Text("ПРЕВЫШЕНИЕ", style = Pst.mono.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = Pst.danger)
        }
        val where = listOfNotNull(row.place.ifBlank { null }, row.kmText).joinToString(" · ")
        if (where.isNotEmpty()) Text(where, style = Pst.sans.copy(fontSize = 11.sp), color = Pst.textMuted)
        Text("Заход головой: ${row.enter}", style = Pst.sans.copy(fontSize = 12.sp), color = Pst.text)
        if (e.kind == "speed") {
            Text("Выход составом: ${row.exit}", style = Pst.sans.copy(fontSize = 12.sp), color = Pst.text)
            Text(
                "Максимум: ${row.maxSpeed}" + (row.limit?.let { " при ограничении $it" } ?: ""),
                style = Pst.sans.copy(fontSize = 12.sp, fontWeight = if (row.exceeded) FontWeight.Bold else FontWeight.Normal),
                color = if (row.exceeded) Pst.danger else Pst.text,
            )
        }
        val status = when (e.status) {
            RestrictionStatus.PLANNED -> if (row.untracked) "Не отслеживается автоматически (нет профиля участка)" else "Ожидает проследования"
            RestrictionStatus.IN_ZONE -> "Состав в зоне"
            RestrictionStatus.DONE -> "Проследовано"
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(status, style = Pst.mono.copy(fontSize = 10.sp), color = Pst.textMuted, modifier = Modifier.weight(1f))
            Text(
                "Убрать", style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = Pst.danger,
                modifier = Modifier.clickable(onClick = onRemove).padding(8.dp),
            )
        }
    }
}

/**
 * Добавление предупреждений из раздела «Поезд»: тот же экран [WarningsDialog], что и в профиле пути. Участок определяется по
 * плечу поезда: берутся только участки, относящиеся к плечу ([ProfileModel.findLegSections]); один - выбирается сам, несколько -
 * шторка выбора ([LegSectionsSheet]); плеча нет - предлагается выбрать плечо. Добавленное сохраняется в хранилище предупреждений
 * участка (видно на профиле и на карте) и привязывается к поезду (журнал).
 */
@Composable
private fun RestrictionAddHost(
    trainId: String,
    stops: List<TrainMapStop>,
    trains: List<ProfileTrain>,
    legFrom: String?,
    legTo: String?,
    onSelectLeg: () -> Unit,
    onClose: () -> Unit,
) {
    val model = rememberProfileModel(null, null, 0L, stops, trains, trainId)
    val store: RestrictionJournalStore = koinInject()
    androidx.compose.runtime.SideEffect { model.legEnds = if (legFrom != null && legTo != null) legFrom to legTo else null }
    var showVoice by rememberSaveable { mutableStateOf(false) }
    var showManual by rememberSaveable { mutableStateOf(false) }
    var picked by remember { mutableStateOf(false) }
    var attempt by remember { mutableStateOf(0) }
    var legSections by remember { mutableStateOf<List<SectionSummary>?>(null) }
    val hasLeg = legFrom != null && legTo != null
    LaunchedEffect(model.loaded, model.needLogin, hasLeg, attempt) {
        legSections = if (model.loaded && !model.needLogin && model.live == null && hasLeg) model.findLegSections() else null
    }

    if (model.live != null) {
        WarningsDialog(model, onVoice = { showVoice = true }, onDismiss = onClose, onSelectLeg = onSelectLeg)
        if (showVoice) WarningVoiceDialog(model, onDismiss = { showVoice = false })
        WarningEditDialog(model)
        WarningOfferDialog(model)
        return
    }
    if (showManual) {
        val t = trains.firstOrNull { it.id == trainId }
        ManualRestrictionDialog(
            defaultPlace = if (hasLeg) "$legFrom — $legTo" else "",
            onSave = { w ->
                runCatching {
                    val old = store.peek(trainId, w.id)
                    store.upsert(RestrictionPass.planned(trainId, t?.routeId.orEmpty(), w, maxOf(System.currentTimeMillis(), (old?.updatedAt ?: 0L) + 1), t?.lengthM))
                }
                showManual = false
                onClose()
            },
            onDismiss = { showManual = false },
        )
        return
    }
    val found = legSections
    val manual = { showManual = true }
    when {
        model.needLogin -> NoSectionDialog("Профиль участков доступен после входа в аккаунт. Ограничение можно добавить без участка: оно попадёт в журнал и отчёт, но время и скорость проследования автоматически не запишутся.", onClose, manual)
        !hasLeg -> NoSectionDialog("Выберите плечо поезда: участок для ограничений определяется по нему.", onClose, manual, onSelectLeg = onSelectLeg)
        !model.loaded || found == null -> Unit
        found.isEmpty() -> NoSectionDialog(
            "Для плеча «$legFrom — $legTo» не удалось найти участок с профилем пути (нет сети, профиль не загружен или нет такого участка). " +
                "Ограничение можно добавить без участка: оно попадёт в журнал и отчёт, но время и скорость проследования автоматически не запишутся; " +
                "когда профиль появится, ограничение с километром подцепится к нему само.",
            onClose, manual, onRetry = { legSections = null; attempt++ }, onSelectLeg = onSelectLeg,
        )
        found.size == 1 -> LaunchedEffect(found[0].id) {
            if (!picked) { picked = true; model.choose(found[0].id) }
        }
        else -> LegSectionsSheet(
            title = "Участок плеча",
            subtitle = "$legFrom — $legTo",
            sections = found,
            onPick = { model.choose(it.id) },
            onDismiss = onClose,
            onManual = manual,
        )
    }
    model.chooseError?.let { NoSectionDialog(it, onClose, manual) }
}

/** Диалог без тупика: когда участка нет, всегда есть действие - добавить ограничение без участка (а также повторить поиск / сменить плечо). */
@Composable
private fun NoSectionDialog(text: String, onClose: () -> Unit, onManual: () -> Unit, onRetry: (() -> Unit)? = null, onSelectLeg: (() -> Unit)? = null) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth(0.9f).background(Pst.bg, RoundedCornerShape(20.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text, style = Pst.sans.copy(fontSize = 14.sp, lineHeight = 20.sp), color = Pst.text)
            PstButton("Добавить без участка", onManual, kind = PstButtonKind.FILLED, height = 44.dp, fontSize = 14, modifier = Modifier.fillMaxWidth())
            if (onRetry != null) PstButton("Повторить поиск", onRetry, kind = PstButtonKind.TONAL, height = 44.dp, fontSize = 14, modifier = Modifier.fillMaxWidth())
            if (onSelectLeg != null) PstButton("Выбрать плечо", { onClose(); onSelectLeg() }, kind = PstButtonKind.TONAL, height = 44.dp, fontSize = 14, modifier = Modifier.fillMaxWidth())
            PstButton("Закрыть", onClose, kind = PstButtonKind.TEXT, height = 40.dp, fontSize = 14, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * Ограничение без участка (нет профиля): вид, скорость, место и, по желанию, километр/пикет «от» и «до». Хранится только в журнале поезда
 * (`sectionId = none`): автоматически не отслеживается, в журнале и отчёте есть; если позже подходящий участок появится и такой километр
 * на нём есть, оно подцепится к участку ([ProfileModel] `adoptManualRestrictions`).
 */
@Composable
private fun ManualRestrictionDialog(defaultPlace: String, onSave: (TrackWarning) -> Unit, onDismiss: () -> Unit) {
    var type by rememberSaveable { mutableStateOf(WarningType.SPEED.name) }
    var speed by remember { mutableStateOf(TextFieldValue("")) }
    var place by remember { mutableStateOf(TextFieldValue(defaultPlace)) }
    var note by remember { mutableStateOf(TextFieldValue("")) }
    var kmA by remember { mutableStateOf(TextFieldValue("")) }
    var pkA by remember { mutableStateOf(TextFieldValue("")) }
    var kmB by remember { mutableStateOf(TextFieldValue("")) }
    var pkB by remember { mutableStateOf(TextFieldValue("")) }
    val wType = WarningType.valueOf(type)
    val num = KeyboardOptions(keyboardType = KeyboardType.Number)
    val limit = speed.text.toIntOrNull()?.takeIf { it in 1..400 }
    fun abs(km: TextFieldValue, pk: TextFieldValue): Double? {
        val k = km.text.toIntOrNull() ?: return null
        val p = if (pk.text.isBlank()) 1 else (pk.text.toIntOrNull() ?: return null)
        return if (k >= 0 && p in 1..10) TrackWarnings.absKm(k, p) else null
    }
    val a = abs(kmA, pkA)
    val b = abs(kmB, pkB)
    val kmGiven = kmA.text.isNotBlank() || kmB.text.isNotBlank()
    val kmOk = !kmGiven || (a != null && b != null)
    val noteOk = wType != WarningType.OTHER || note.text.isNotBlank()
    val valid = (wType != WarningType.SPEED || limit != null) && kmOk && noteOk
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(
            Modifier.fillMaxWidth(0.94f).heightIn(max = 640.dp).background(Pst.bg, RoundedCornerShape(20.dp)).imePadding()
                .verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Ограничение без участка", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
            Text(
                "Профиля участка нет: время и скорость проследования автоматически не запишутся, но ограничение будет в журнале и отчёте.",
                style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Pst.textMuted,
            )
            WarningType.entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { t ->
                        PstButton(
                            t.title, { type = t.name }, kind = if (t == wType) PstButtonKind.FILLED else PstButtonKind.TONAL,
                            height = 40.dp, fontSize = 14, modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            if (wType == WarningType.SPEED) {
                PstTextField(speed, { speed = it }, "Скорость", suffix = "км/ч", keyboardOptions = num, mono = true, isError = speed.text.isNotBlank() && limit == null)
            }
            PstTextField(place, { place = it }, "Место (станция или перегон)")
            Text("Километр и пикет (по желанию, пикет 1–10)", style = Pst.sans.copy(fontSize = 12.sp), color = Pst.textMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PstTextField(kmA, { kmA = it }, "Км от", keyboardOptions = num, mono = true, modifier = Modifier.weight(1f), isError = kmGiven && a == null)
                PstTextField(pkA, { pkA = it }, "Пк", keyboardOptions = num, mono = true, modifier = Modifier.weight(0.6f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PstTextField(kmB, { kmB = it }, "Км до", keyboardOptions = num, mono = true, modifier = Modifier.weight(1f), isError = kmGiven && b == null)
                PstTextField(pkB, { pkB = it }, "Пк", keyboardOptions = num, mono = true, modifier = Modifier.weight(0.6f))
            }
            PstTextField(note, { note = it }, if (wType == WarningType.OTHER) "Что за предупреждение" else "Примечание", singleLine = false, isError = !noteOk)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PstButton("Отмена", onDismiss, kind = PstButtonKind.TEXT, height = 44.dp, fontSize = 14, modifier = Modifier.weight(1f))
                PstButton(
                    "Сохранить",
                    {
                        onSave(
                            TrackWarning(
                                id = generateId(), sectionId = RestrictionPass.NO_SECTION,
                                startAbsKm = a.takeIf { kmGiven }, endAbsKm = b.takeIf { kmGiven }, startRelKm = 0.0, endRelKm = 0.0,
                                type = wType, speedKmh = limit.takeIf { wType == WarningType.SPEED }, note = note.text.trim().take(RestrictionPass.MAX_NOTE),
                                place = place.text.trim().take(RestrictionPass.MAX_TEXT),
                            ),
                        )
                    },
                    kind = PstButtonKind.FILLED, enabled = valid, height = 44.dp, fontSize = 14, modifier = Modifier.weight(1f),
                )
            }
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
