package com.z_company.route.component.servicephase

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import java.util.UUID

/** Станция или перегон участка, из которого создаётся предупреждение; [startKm]/[endKm] - км оси (у станции равны). */
private class WarningPlace(val title: String, val startKm: Double, val endKm: Double, val isStation: Boolean)

/** Станции и перегоны участка по ходу движения: станция, перегон до следующей, ... */
private fun warningPlaces(section: SectionProfile, reversed: Boolean): List<WarningPlace> {
    val st = section.stations.sortedBy { it.km }.let { if (reversed) it.asReversed() else it }
    val out = ArrayList<WarningPlace>()
    st.forEachIndexed { i, s ->
        out += WarningPlace(s.name, s.km, s.km, true)
        st.getOrNull(i + 1)?.let { n -> out += WarningPlace("${s.name} — ${n.name}", minOf(s.km, n.km), maxOf(s.km, n.km), false) }
    }
    return out
}

private fun kmText(section: SectionProfile, w: TrackWarning): String {
    val a = w.startAbsKm
    val b = w.endAbsKm
    if (a != null && b != null) return KmMarks.position(a).text() + " — " + KmMarks.position(b).text()
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
internal fun WarningsDialog(model: ProfileModel, onVoice: () -> Unit, onDismiss: () -> Unit) {
    val live = model.live
    if (live == null) { onDismiss(); return }
    val section = live.section
    var place by remember { mutableStateOf<WarningPlace?>(null) }
    var added by remember { mutableStateOf(0) }
    val places = remember(section.id, live.reversed) { warningPlaces(section, live.reversed) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val p = place
        if (p == null) {
            WarningPanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Предупреждения", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
                        Text(
                            text = section.name + if (added > 0) " · добавлено: $added" else "",
                            style = Pst.sans.copy(fontSize = 12.sp), color = Pst.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    PstButton("Голос", onVoice, kind = PstButtonKind.TONAL, height = 40.dp, fontSize = 14)
                    PstButton("Готово", onDismiss, kind = PstButtonKind.FILLED, height = 40.dp, fontSize = 14)
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (model.savedWarnings.isNotEmpty()) {
                        item { SmallCaps("ДОБАВЛЕННЫЕ — галочка: действует на этом проезде") }
                        items(model.savedWarnings, key = { it.id }) { w ->
                            SavedWarningRow(model, section, w)
                        }
                    }
                    item { SmallCaps("ВЫБЕРИТЕ СТАНЦИЮ ИЛИ ПЕРЕГОН") }
                    items(places) { pl ->
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Pst.surface, RoundedCornerShape(12.dp))
                                .clickable { place = pl }.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (pl.isStation) "СТ" else "ПЕР",
                                style = Pst.mono.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold), color = Pst.textMuted,
                            )
                            Text(
                                pl.title, style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Pst.text,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        } else {
            WarningForm(model, section, p, onBack = { place = null }, onSaved = { added++; place = null })
        }
    }
}

@Composable
private fun SmallCaps(text: String) {
    Text(text, style = Pst.mono.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold), color = Pst.textMuted, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun SavedWarningRow(model: ProfileModel, section: SectionProfile, w: TrackWarning) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Pst.surface, RoundedCornerShape(12.dp)).padding(start = 6.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = model.isWarningActive(w.id), onCheckedChange = { model.setWarningActive(w.id, it) })
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(
                w.label,
                style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = Pst.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${w.place} · ${kmText(section, w)}",
                style = Pst.sans.copy(fontSize = 11.sp), color = Pst.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            "Удалить", style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = Pst.danger,
            modifier = Modifier.clickable { model.deleteWarning(w.id) }.padding(8.dp),
        )
    }
}

@Composable
private fun WarningForm(
    model: ProfileModel, section: SectionProfile, place: WarningPlace, existing: TrackWarning? = null,
    onBack: () -> Unit, onSaved: () -> Unit,
) {
    val marks = section.kmMarks
    val hasKm = marks.isNotEmpty()
    fun initial(rel: Double): Pair<String, String> =
        KmMarks.toAbs(marks, rel)?.let { TrackWarnings.split(it) }?.let { (k, p) -> k.toString() to p.toString() } ?: ("" to "")
    fun fromAbs(abs: Double?): Pair<String, String>? = abs?.let { TrackWarnings.split(it) }?.let { (k, p) -> k.toString() to p.toString() }
    val (sk, sp) = remember(place) { existing?.let { fromAbs(it.startAbsKm) } ?: initial(place.startKm) }
    // Зона на один пикет (конец = начало + 100 м) показывается с пустым концом - как вводилась.
    val oneSection = existing?.let { it.startAbsKm != null && it.endAbsKm != null && kotlin.math.abs(it.endAbsKm!! - it.startAbsKm!! - 0.1) < 1e-6 } == true
    val (ek, ep) = remember(place) {
        if (existing != null) { if (oneSection) ("" to "") else fromAbs(existing.endAbsKm) ?: ("" to "") }
        else if (place.isStation) ("" to "") else initial(place.endKm)
    }
    var startKm by remember { mutableStateOf(TextFieldValue(sk)) }
    var startPk by remember { mutableStateOf(TextFieldValue(sp)) }
    var endKm by remember { mutableStateOf(TextFieldValue(ek)) }
    var endPk by remember { mutableStateOf(TextFieldValue(ep)) }
    var type by remember { mutableStateOf(existing?.type ?: WarningType.SPEED) }
    var speed by remember { mutableStateOf(TextFieldValue(existing?.speedKmh?.toString() ?: "")) }
    var note by remember { mutableStateOf(TextFieldValue(existing?.note ?: "")) }
    var error by remember { mutableStateOf<String?>(null) }
    val numeric = KeyboardOptions(keyboardType = KeyboardType.Number)

    fun save() {
        val aAbs: Double?
        val bAbs: Double?
        if (hasKm) {
            val k1 = startKm.text.trim().toIntOrNull()
            val p1 = startPk.text.trim().ifEmpty { "1" }.toIntOrNull()
            val noEnd = endKm.text.isBlank()
            val k2 = if (noEnd) k1 else endKm.text.trim().toIntOrNull()
            val p2 = if (noEnd) p1 else endPk.text.trim().ifEmpty { "1" }.toIntOrNull()
            if (k1 == null || k2 == null || k1 < 0 || k2 < 0) { error = "Введите километр начала (и конца, если зона длиннее пикета)."; return }
            if (p1 == null || p2 == null || p1 !in 1..10 || p2 !in 1..10) { error = "Пикет — число от 1 до 10."; return }
            aAbs = TrackWarnings.absKm(k1, p1)
            // конец не указан или совпал с началом - предупреждение действует на одном пикете (100 м)
            bAbs = TrackWarnings.absKm(k2, p2).let { if (noEnd || it == aAbs) aAbs + 0.1 else it }
        } else { aAbs = null; bAbs = null }
        val speedV = speed.text.trim().toIntOrNull()
        if (type == WarningType.SPEED && (speedV == null || speedV !in 1..200)) { error = "Введите ограничение скорости, км/ч."; return }
        if (type == WarningType.OTHER && note.text.isBlank()) { error = "Напишите примечание."; return }
        val w = TrackWarning(
            id = existing?.id ?: UUID.randomUUID().toString(), sectionId = section.id,
            startAbsKm = aAbs, endAbsKm = bAbs, startRelKm = place.startKm,
            endRelKm = if (place.endKm - place.startKm < 0.0999) place.startKm + 0.1 else place.endKm,
            type = type, speedKmh = if (type == WarningType.SPEED) speedV else null,
            note = note.text.trim(), place = place.title,
        )
        val r = TrackWarnings.resolve(section, w)
        if (r == null) { error = "Такого километра нет на этом участке."; return }
        if (r.toKm < -0.001 || r.fromKm > section.lengthKm + 0.001) { error = "Место за пределами участка."; return }
        if (existing != null) model.updateWarning(w) else model.addWarning(w)
        onSaved()
    }

    val portrait = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val titleBlock: @Composable (Modifier) -> Unit = { m ->
        Column(m) {
            Text(if (existing != null) "Предупреждение" else "Новое предупреждение", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
            Text(place.title, style = Pst.sans.copy(fontSize = 12.sp), color = Pst.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    val buttons: @Composable () -> Unit = {
        if (existing != null) {
            PstButton("Удалить", { model.deleteWarning(existing.id); onSaved() }, kind = PstButtonKind.DANGER, height = 40.dp, fontSize = 14)
        }
        PstButton(if (existing != null) "Отмена" else "Назад", onBack, kind = PstButtonKind.TEXT, height = 40.dp, fontSize = 14)
        PstButton("Сохранить", { save() }, kind = PstButtonKind.FILLED, height = 40.dp, fontSize = 14)
    }
    WarningPanel(widthFraction = if (portrait) 0.94f else 0.8f, fill = !portrait) {
        if (portrait) {
            titleBlock(Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) { buttons() }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                titleBlock(Modifier.weight(1f))
                buttons()
            }
        }
        Column(Modifier.weight(1f, fill = !portrait).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (hasKm) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PstTextField(startKm, { startKm = it; error = null }, "Начало, км", Modifier.weight(1f), keyboardOptions = numeric, mono = true)
                    PstTextField(startPk, { startPk = it; error = null }, "Начало, пикет", Modifier.weight(1f), placeholder = "1", keyboardOptions = numeric, mono = true)
                    PstTextField(endKm, { endKm = it; error = null }, "Конец, км", Modifier.weight(1f), keyboardOptions = numeric, mono = true)
                    PstTextField(endPk, { endPk = it; error = null }, "Конец, пикет", Modifier.weight(1f), placeholder = "1", keyboardOptions = numeric, mono = true)
                }
                if (endKm.text.isBlank()) Text(
                    "Конец не указан — предупреждение действует на одном пикете (100 м).",
                    style = Pst.sans.copy(fontSize = 12.sp), color = Pst.textMuted,
                )
            } else {
                Text(
                    "У участка нет километровой привязки — предупреждение займёт весь выбранный отрезок.",
                    style = Pst.sans.copy(fontSize = 13.sp), color = Pst.textMuted,
                )
            }
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                WarningType.values().forEach { t -> Chip(t.title, type == t) { type = t; error = null } }
            }
            if (type == WarningType.SPEED) {
                PstTextField(speed, { speed = it; error = null }, "Ограничение, км/ч", Modifier.fillMaxWidth(0.4f), keyboardOptions = numeric, mono = true)
            }
            PstTextField(
                note, { note = it; error = null },
                if (type == WarningType.OTHER) "Примечание" else "Примечание (необязательно)", Modifier.fillMaxWidth(),
            )
            error?.let { Text(it, style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 17.sp), color = Pst.danger) }
        }
    }
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
            Text("Предупреждения на участке", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
            Text(
                "На участке «${section.name}» уже были предупреждения. Применить их на этом проезде?",
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Голос предупреждений", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text, modifier = Modifier.weight(1f))
                PstButton("Проверить", { model.testVoice() }, kind = PstButtonKind.TONAL, height = 40.dp, fontSize = 14)
                PstButton("Готово", onDismiss, kind = PstButtonKind.FILLED, height = 40.dp, fontSize = 14)
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
    val w = model.editingWarning ?: return
    val section = model.section ?: return
    Dialog(onDismissRequest = { model.closeWarningEdit() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WarningForm(
            model, section,
            WarningPlace(w.place, w.startRelKm, w.endRelKm, false),
            existing = w,
            onBack = { model.closeWarningEdit() },
            onSaved = { model.closeWarningEdit() },
        )
    }
}
