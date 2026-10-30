package com.z_company.route.component.servicephase

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.z_company.domain.track.KmMarks
import com.z_company.domain.track.SectionProfile
import com.z_company.domain.track.TrackWarning
import com.z_company.domain.track.TrackWarnings
import com.z_company.domain.track.WarningType
import com.z_company.domain.track.RestrictionPass
import com.z_company.domain.track.RestrictionReport
import java.util.UUID

/** Станция или перегон участка, из которого создаётся предупреждение; [startKm]/[endKm] - км оси (у станции равны). */
private class WarningPlace(val title: String, val startKm: Double, val endKm: Double, val isStation: Boolean)

/**
 * Места ДУ-61 для участка: станции плеча поезда (полный список с координатами; на участке часть станций может отсутствовать) и перегоны
 * между соседними, в порядке движения; берутся те, что лежат на оси участка ([TrackWarnings.stationKm]). Плеча нет или на оси нет ни одной
 * его станции - станции самого участка по ходу движения.
 */
private fun warningPlaces(section: SectionProfile, reversed: Boolean, legStops: List<com.z_company.domain.util.TrainMapStop>): List<WarningPlace> {
    val out = ArrayList<WarningPlace>()
    val onAxis = legStops.mapNotNull { st -> TrackWarnings.stationKm(section, st.name, legStops)?.let { st.name to it } }
    if (onAxis.isNotEmpty()) {
        onAxis.forEachIndexed { i, (name, km) ->
            out += WarningPlace(name, km, km, true)
            onAxis.getOrNull(i + 1)?.let { (n2, km2) -> out += WarningPlace("$name — $n2", minOf(km, km2), maxOf(km, km2), false) }
        }
        return out
    }
    val st = section.stations.sortedBy { it.km }.let { if (reversed) it.asReversed() else it }
    st.forEachIndexed { i, s ->
        out += WarningPlace(s.name, s.km, s.km, true)
        st.getOrNull(i + 1)?.let { n -> out += WarningPlace("${s.name} — ${n.name}", minOf(s.km, n.km), maxOf(s.km, n.km), false) }
    }
    return out
}

private fun kmText(section: SectionProfile, w: TrackWarning): String {
    val a = w.startAbsKm
    val b = w.endAbsKm
    if (a != null && b != null) return KmMarks.position(a).textToPicket() + " — " + KmMarks.position(b).textToPicket()
    return "${fmt1(w.startRelKm)} — ${fmt1(w.endRelKm)} км от начала"
}

private fun fmt1(v: Double): String = ((v * 10).toInt() / 10.0).toString().replace('.', ',')

@Composable
private fun WarningPanel(widthFraction: Float = 0.85f, fill: Boolean = true, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .then(if (fill) Modifier.fillMaxHeight(0.94f) else Modifier.heightIn(max = 480.dp))
            .background(Pst.bg, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

/** Отдельный экран предупреждений: на весь экран (не диалог-карточка), с отступами под системные панели и клавиатуру. */
@Composable
private fun WarningScreen(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Pst.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

/**
 * На время показа экрана предупреждений снимает горизонтальную блокировку профиля: экран поворачивается вслед за телефоном
 * (в любую сторону). На выходе возвращается прежняя ориентация (у профиля - горизонталь).
 */
@Composable
internal fun FollowDeviceOrientation() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) { context.findActivity() }
    androidx.compose.runtime.DisposableEffect(activity) {
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        onDispose { previous?.let { activity.requestedOrientation = it } }
    }
}

@Composable
private fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
        color = if (on) Pst.onAccent else Pst.text,
        modifier = Modifier
            .background(if (on) Pst.accent else Pst.bgSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        maxLines = 1,
    )
}

/**
 * «Предупреждения»: сначала список станций и перегонов участка (и уже добавленные предупреждения с галочкой «действует» и
 * удалением); выбор места открывает форму, после «Сохранить» возвращаемся к списку, «Готово» закрывает.
 */
@Composable
internal fun WarningsDialog(model: ProfileModel, onVoice: () -> Unit, onDismiss: () -> Unit, leg: LegContext? = null) {
    val live = model.live
    if (live == null) { onDismiss(); return }
    val section = live.section
    var place by remember { mutableStateOf<WarningPlace?>(null) }
    var editing by remember { mutableStateOf<TrackWarning?>(null) }
    var showWhere by remember { mutableStateOf(false) }
    var toRemove by remember { mutableStateOf<RestrictionPass?>(null) }
    var editingOther by remember { mutableStateOf<RestrictionPass?>(null) }
    var toForget by remember { mutableStateOf<TrackWarning?>(null) }
    val zone = remember { java.util.TimeZone.getDefault().id }
    val order = remember(model.stopPoints) { model.stopPoints.map { it.name }.filter { it.isNotBlank() } }
    // Действующие - весь список поезда (его же показывает «Ограничения поезда»), не только то, что лежит на этом участке.
    val active = sortRestrictions(model.trainRestrictions, order)
    val past = model.pastWarnings
    val pastHidden = rememberPastHidden()
    var pastChecked by remember(section.id) { mutableStateOf(emptySet<String>()) }
    // Выбранные, которых уже нет среди прошлых (стали действующими/удалены), из выбора выпадают.
    val pastIds = past.map { it.id }.toSet()
    val checkedNow = pastChecked.filter { it in pastIds }.toSet()
    val placements by rememberPlacements(active, model.stopPoints)
    var added by remember { mutableStateOf(0) }
    val places = remember(section.id, live.reversed, model.stopPoints) { warningPlaces(section, live.reversed, model.stopPoints) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        FollowDeviceOrientation()
        val portrait = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
        val p = place
        WarningScreen {
            val titleBlock: @Composable (Modifier) -> Unit = { m ->
                Column(m, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ДУ-61", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
                    Text(
                        text = section.name + if (added > 0) " · добавлено: $added" else "",
                        style = Pst.sans.copy(fontSize = 12.sp), color = Pst.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            val buttons: @Composable () -> Unit = {
                PstButton("Добавить", { showWhere = true }, kind = PstButtonKind.FILLED, height = 40.dp, fontSize = 14)
                PstButton("Голос", onVoice, kind = PstButtonKind.TONAL, height = 40.dp, fontSize = 14)
                PstButton("Готово", onDismiss, kind = PstButtonKind.TONAL, height = 40.dp, fontSize = 14)
            }
            if (portrait) {
                titleBlock(Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) { buttons() }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    titleBlock(Modifier.weight(1f))
                    buttons()
                }
            }
            // Плечо поезда: то же, что в «Поезде» и «Ограничениях поезда» (выбор плеча, пометка «проверено на карте», кнопка «Определить»).
            if (leg != null) LegBlock(leg)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (active.isNotEmpty()) {
                    item { SmallCaps("ДЕЙСТВУЮЩИЕ — общий список поезда") }
                    items(active, key = { "a" + it.warningId }) { e ->
                        val row = RestrictionReport.rows(listOf(e), zone).first()
                        com.z_company.route.component.SwipeToRevealDelete(
                            modifier = Modifier.fillMaxWidth(),
                            onDeleteClick = { toRemove = e },
                            onContentClick = { if (e.sectionId == section.id) editing = e.toWarning() else editingOther = e },
                            backgroundVerticalPadding = 0.dp,
                            itemKey = e.warningId,
                        ) { _ -> RestrictionCard(row, e, placementNote(placements[e.warningId])) }
                    }
                }
                if (past.isNotEmpty()) item(key = "past_block") {
                    PastRestrictionsBlock(
                        title = "Прошлые на этом участке",
                        rows = past.map { w ->
                            PastRow(
                                id = w.id, place = w.place.ifBlank { w.label },
                                limit = if (w.type == WarningType.SPEED) w.speedKmh?.let { "$it км/ч" } else null,
                                kmText = kmText(section, w),
                                boldTitle = w.label.replaceFirstChar { it.uppercase() }.takeIf { w.type != WarningType.SPEED },
                            )
                        },
                        checked = checkedNow,
                        hidden = pastHidden.hidden,
                        onToggleHidden = pastHidden.toggle,
                        onToggle = { id -> pastChecked = if (id in checkedNow) checkedNow - id else checkedNow + id },
                        onSelectAll = { pastChecked = past.map { it.id }.toSet() },
                        onClear = { pastChecked = emptySet() },
                        onApply = {
                            checkedNow.forEach { model.activatePast(it) }
                            pastChecked = emptySet()
                        },
                        onForget = { id -> toForget = past.firstOrNull { it.id == id } },
                    )
                }
            }
        }
        if (p != null) ProfileRestrictionForm(model, section, p, onBack = { place = null }, onSaved = { added++; place = null })
        if (showWhere) {
            WhereSheet(
                leg = leg,
                places = places.map { WhereItem(it.title, it.isStation) },
                hint = "Выберите станцию или перегон участка.",
                onPick = { place = places[it]; showWhere = false },
                onOther = null,
                onClose = { showWhere = false },
            )
        }
        toRemove?.let { e ->
            com.z_company.route.component.AppAlertDialog(
                onDismissRequest = { toRemove = null },
                title = "Убрать ограничение из поезда?",
                text = if (e.hasData) "Записанные время и скорость проследования будут удалены." else "Ограничение останется в прошлых ограничениях участка.",
                confirmText = "Убрать", isDestructive = true, dismissText = "Отмена",
                onConfirm = { toRemove = null; model.deleteWarning(e.warningId) },
            )
        }
        editingOther?.let { e ->
            // Ограничение на другом участке плеча (или без участка): правится той же шторкой и тем же путём, что в «Ограничениях поезда».
            RestrictionFormSheet(
                sectionId = e.sectionId,
                place = e.place,
                existing = e.toWarning(),
                kmRequired = e.startAbsKm != null || !e.tracked,
                onSave = { w -> model.saveJournalEdit(e, w); editingOther = null; null },
                onDelete = { toRemove = e; editingOther = null },
                onDismiss = { editingOther = null },
            )
        }
        toForget?.let { w ->
            com.z_company.route.component.AppAlertDialog(
                onDismissRequest = { toForget = null },
                title = "Удалить из прошлых?",
                text = "Запись ДУ-61 будет стёрта из истории участка.",
                confirmText = "Удалить", isDestructive = true, dismissText = "Отмена",
                onConfirm = { toForget = null; model.forgetPast(w.id) },
            )
        }
        editing?.let { e ->
            ProfileRestrictionForm(
                model, section, WarningPlace(e.place, e.startRelKm, e.endRelKm, false), existing = e,
                onBack = { editing = null }, onSaved = { editing = null },
            )
        }
    }
}

/** Строка «Плечо» экранов предупреждений и ограничений: текущее плечо поезда и кнопка выбора/смены. */
@Composable
internal fun LegRow(title: String?, onSelectLeg: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().background(Pst.surface, RoundedCornerShape(12.dp)).padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("ПЛЕЧО", style = Pst.mono.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold), color = Pst.textMuted)
            Text(
                title ?: "не выбрано",
                style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = if (title == null) Pst.textMuted else Pst.text,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        PstButton(if (title == null) "Выбрать плечо" else "Сменить", onSelectLeg, kind = if (title == null) PstButtonKind.FILLED else PstButtonKind.TEXT, height = 40.dp, fontSize = 14)
    }
}

/**
 * Компактный выбор плеча для карты, открытой с Главного (там нет формы «Поезд» с её окном «Плечи»): те же плечи, тот же путь
 * сохранения (`TrainFormViewModel.setSelectedServicePhase`), что у окна «Плечи» в «Поезде».
 */
@Composable
internal fun LegPickerDialog(
    phases: List<com.z_company.domain.entities.setting.ServicePhase>,
    selectedId: String?,
    onSelect: (com.z_company.domain.entities.setting.ServicePhase) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WarningPanel(widthFraction = 0.9f, fill = false) {
            Text("Плечо поезда", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
            if (phases.isEmpty()) {
                Text("Плеч пока нет. Создайте плечо в разделе «Поезд».", style = Pst.sans.copy(fontSize = 13.sp), color = Pst.textMuted)
            }
            LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(phases, key = { it.id }) { p ->
                    val on = p.id == selectedId
                    Row(
                        modifier = Modifier.fillMaxWidth().background(if (on) Pst.accentSoft else Pst.surface, RoundedCornerShape(12.dp))
                            .clickable { onSelect(p); onDismiss() }.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "${p.departureStation} — ${p.arrivalStation}",
                            style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Pst.text,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        )
                        Text("${p.distance} км", style = Pst.mono.copy(fontSize = 12.sp), color = Pst.textMuted)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                PstButton("Закрыть", onDismiss, kind = PstButtonKind.TEXT, height = 40.dp, fontSize = 14)
            }
        }
    }
}

@Composable
private fun SmallCaps(text: String) {
    Text(text, style = Pst.mono.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold), color = Pst.textMuted, modifier = Modifier.padding(top = 6.dp))
}

/**
 * Форма ДУ-61 профиля пути - та же шторка [RestrictionFormSheet], что и у раздела «Поезд»; отличается только тем, куда сохраняется:
 * предупреждение проверяется по оси участка и попадает в хранилище участка (видно на профиле и карте) и в журнал поезда.
 */
@Composable
private fun ProfileRestrictionForm(
    model: ProfileModel, section: SectionProfile, place: WarningPlace, existing: TrackWarning? = null,
    onBack: () -> Unit, onSaved: () -> Unit,
) {
    val marks = section.kmMarks
    // У станции предзаполнено только начало; на перегоне подсказка, где проходят его границы (поля не предзаполняются).
    val prefill = if (existing == null && place.isStation) {
        KmMarks.toAbs(marks, place.startKm)?.let { TrackWarnings.split(it) }?.let { (k, p) -> k.toString() to p.toString() }
    } else null
    val hint = if (existing == null && !place.isStation && marks.isNotEmpty()) {
        val a = KmMarks.toAbs(marks, place.startKm)?.let { KmMarks.position(it).text() }
        val b = KmMarks.toAbs(marks, place.endKm)?.let { KmMarks.position(it).text() }
        if (a != null && b != null) "Перегон: $a — $b" else null
    } else null
    RestrictionFormSheet(
        sectionId = section.id,
        place = place.title,
        existing = existing,
        kmRequired = marks.isNotEmpty(),
        placeBounds = if (existing == null) place.startKm to place.endKm else null,
        prefillStart = prefill,
        hint = hint,
        onSave = { w ->
            val r = TrackWarnings.resolve(section, w, model.stopPoints)
            when {
                r == null -> "Такого километра нет на этом участке."
                r.toKm < -0.001 || r.fromKm > section.lengthKm + 0.001 -> "Место за пределами участка."
                TrackWarnings.placementIssue(section, r) != null -> TrackWarnings.UNKNOWN_KM_MESSAGE
                else -> {
                    if (existing != null) model.updateWarning(w) else model.addWarning(w)
                    onSaved()
                    null
                }
            }
        },
        onDelete = existing?.let { e -> { if (model.isWarningActive(e.id)) model.deleteWarning(e.id) else model.forgetPast(e.id); onSaved() } },
        onDismiss = onBack,
    )
}

/** Прошлые предупреждения участка: предложение применить их снова (с галочками по каждому). */
@Composable
internal fun WarningOfferDialog(model: ProfileModel) {
    val offer = model.warningOffer
    val section = model.section
    if (offer.isEmpty() || section == null) return
    var checked by remember(section.id) { mutableStateOf(offer.map { it.id }.toSet()) }
    Dialog(onDismissRequest = { model.answerOffer(emptySet()) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WarningPanel(widthFraction = 0.9f, fill = false) {
            Text("ДУ-61 на участке", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
            Text(
                "На участке «${section.name}» уже были записи ДУ-61. Сделать их действующими?",
                style = Pst.sans.copy(fontSize = 13.sp), color = Pst.textMuted,
            )
            LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(offer, key = { it.id }) { w ->
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Pst.surface, RoundedCornerShape(12.dp))
                            .clickable { checked = if (w.id in checked) checked - w.id else checked + w.id }
                            .padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = w.id in checked, onCheckedChange = { checked = if (it) checked + w.id else checked - w.id })
                        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                            Text(w.label, style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = Pst.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${w.place} · ${kmText(section, w)}", style = Pst.sans.copy(fontSize = 11.sp), color = Pst.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
                PstButton("Не нужны", { model.answerOffer(emptySet()) }, kind = PstButtonKind.TEXT, height = 40.dp, fontSize = 14)
                PstButton("Применить", { model.answerOffer(checked) }, kind = PstButtonKind.FILLED, height = 40.dp, fontSize = 14)
            }
        }
    }
}

/** «Голос предупреждений»: включение, за сколько метров, как часто повторять, голос и скорость речи. */
@Composable
internal fun WarningVoiceDialog(model: ProfileModel, onDismiss: () -> Unit) {
    val settings = model.voiceSettings ?: run { onDismiss(); return }
    var enabled by remember { mutableStateOf(settings.enabled) }
    var distance by remember { mutableStateOf(settings.distanceM) }
    var repeat by remember { mutableStateOf(settings.repeatSec) }
    var rate by remember { mutableStateOf(settings.rate) }
    var voiceName by remember { mutableStateOf(settings.voiceName) }
    val voices = remember { model.voices() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WarningPanel(widthFraction = 0.75f) {
            val portrait = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
            val title: @Composable (Modifier) -> Unit = { m ->
                Text("Голос ДУ-61", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text, modifier = m)
            }
            val buttons: @Composable () -> Unit = {
                PstButton("Проверить", { model.testVoice() }, kind = PstButtonKind.TONAL, height = 40.dp, fontSize = 14)
                PstButton("Готово", onDismiss, kind = PstButtonKind.FILLED, height = 40.dp, fontSize = 14)
            }
            if (portrait) {
                // Вертикально: заголовок над кнопками (в одну строку он не помещается и ломается по буквам).
                title(Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) { buttons() }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    title(Modifier.weight(1f))
                    buttons()
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip("Озвучка включена", enabled) { enabled = true; settings.enabled = true }
                    Chip("Выключена", !enabled) { enabled = false; settings.enabled = false }
                }
                SettingBlock("ЗА СКОЛЬКО ДО МЕСТА") {
                    listOf(500, 1000, 1500, 2000, 3000).forEach { d ->
                        Chip(if (d >= 1000) "${d / 1000.0}".replace('.', ',').removeSuffix(",0") + " км" else "$d м", distance == d) { distance = d; settings.distanceM = d }
                    }
                }
                SettingBlock("ПОВТОР") {
                    listOf(0 to "Один раз", 15 to "15 с", 30 to "30 с", 60 to "60 с").forEach { (s, t) ->
                        Chip(t, repeat == s) { repeat = s; settings.repeatSec = s }
                    }
                }
                SettingBlock("СКОРОСТЬ РЕЧИ") {
                    listOf(0.8f to "Медленно", 1.0f to "Обычно", 1.25f to "Быстро").forEach { (r, t) ->
                        Chip(t, rate == r) { rate = r; settings.rate = r; model.applyVoice() }
                    }
                }
                SettingBlock("ГОЛОС") {
                    Chip("По умолчанию", voiceName.isEmpty()) { voiceName = ""; settings.voiceName = ""; model.applyVoice() }
                    voices.forEachIndexed { i, v ->
                        Chip("Голос ${i + 1}", voiceName == v.name) { voiceName = v.name; settings.voiceName = v.name; model.applyVoice() }
                    }
                }
                if (voices.isEmpty()) {
                    Text(
                        "Русские голоса не найдены. Установите синтез речи Google и русский голос в настройках Android.",
                        style = Pst.sans.copy(fontSize = 12.sp), color = Pst.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingBlock(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = Pst.mono.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold), color = Pst.textMuted)
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
        ) { content() }
    }
}

/** Редактирование предупреждения по нажатию на карточку на карте или на зону на профиле (окно открывает [ProfileModel.editWarning]). */
@Composable
internal fun WarningEditDialog(model: ProfileModel) {
    model.editingEntry?.let { e ->
        RestrictionFormSheet(
            sectionId = e.sectionId,
            place = e.place,
            existing = e.toWarning(),
            kmRequired = e.startAbsKm != null || !e.tracked,
            onSave = { w -> model.saveJournalEdit(e, w); model.closeWarningEdit(); null },
            onDelete = { model.removeEntry(e) },
            onDismiss = { model.closeWarningEdit() },
        )
        return
    }
    val w = model.editingWarning ?: return
    val section = model.section ?: return
    ProfileRestrictionForm(
        model, section,
        WarningPlace(w.place, w.startRelKm, w.endRelKm, false),
        existing = w,
        onBack = { model.closeWarningEdit() },
        onSaved = { model.closeWarningEdit() },
    )
}
