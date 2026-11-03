package com.z_company.route.component.servicephase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.domain.track.TrackWarning
import com.z_company.domain.track.TrackWarnings
import com.z_company.domain.track.WarningType
import com.z_company.domain.util.generateId

/**
 * Единая форма добавления/правки ДУ-61: одна и та же из любого места (профиль пути, карта, раздел «Поезд»). Шторка: вид, станция или
 * перегон, скорость (под местом), начало (км и пикет), конец (ниже; не указан - действует один пикет, 100 м), примечание.
 *
 * [kmRequired] - километр начала обязателен (у участка без километровых отметок км не вводится: берётся место целиком).
 * [placeBounds] - границы выбранного места на оси участка (профиль), null - место задано только названием (журнал без участка).
 * [onSave] получает готовое предупреждение; возвращает текст ошибки (форма остаётся открытой) или null.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RestrictionFormSheet(
    sectionId: String,
    place: String,
    existing: TrackWarning? = null,
    kmRequired: Boolean = true,
    placeBounds: Pair<Double, Double>? = null,
    prefillStart: Pair<String, String>? = null,
    hint: String? = null,
    onSave: (TrackWarning) -> String?,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    fun fromAbs(abs: Double?): Pair<String, String>? = abs?.let { TrackWarnings.split(it) }?.let { (k, p) -> k.toString() to p.toString() }
    val oneSection = existing?.let { it.startAbsKm != null && it.endAbsKm != null && kotlin.math.abs(it.endAbsKm!! - it.startAbsKm!! - 0.1) < 1e-6 } == true
    val (sk, sp) = remember(existing?.id) { existing?.let { fromAbs(it.startAbsKm) } ?: prefillStart ?: ("" to "") }
    val (ek, ep) = remember(existing?.id) { if (existing != null && !oneSection) fromAbs(existing.endAbsKm) ?: ("" to "") else ("" to "") }
    var type by remember { mutableStateOf(existing?.type ?: WarningType.SPEED) }
    var placeText by remember { mutableStateOf(TextFieldValue(existing?.place ?: place)) }
    var speed by remember { mutableStateOf(TextFieldValue(existing?.speedKmh?.toString() ?: "")) }
    var startKm by remember { mutableStateOf(TextFieldValue(sk)) }
    var startPk by remember { mutableStateOf(TextFieldValue(sp)) }
    var endKm by remember { mutableStateOf(TextFieldValue(ek)) }
    var endPk by remember { mutableStateOf(TextFieldValue(ep)) }
    var note by remember { mutableStateOf(TextFieldValue(existing?.note ?: "")) }
    var error by remember { mutableStateOf<String?>(null) }
    val numeric = KeyboardOptions(keyboardType = KeyboardType.Number)
    val noEnd = endKm.text.isBlank()

    fun save() {
        var aAbs: Double? = null
        var bAbs: Double? = null
        if (kmRequired) {
            val k1 = startKm.text.trim().toIntOrNull()
            val p1 = startPk.text.trim().ifEmpty { "1" }.toIntOrNull()
            val k2 = if (noEnd) k1 else endKm.text.trim().toIntOrNull()
            val p2 = if (noEnd) p1 else endPk.text.trim().ifEmpty { "1" }.toIntOrNull()
            if (k1 == null || k1 < 0) { error = "Введите километр начала."; return }
            if (k2 == null || k2 < 0) { error = "Конец: введите километр или оставьте поля пустыми."; return }
            if (p1 == null || p2 == null || p1 !in 1..10 || p2 !in 1..10) { error = "Пикет — число от 1 до 10."; return }
            aAbs = TrackWarnings.absKm(k1, p1)
            // конец не указан или совпал с началом - предупреждение действует на одном пикете (100 м)
            bAbs = TrackWarnings.absKm(k2, p2).let { if (noEnd || it == aAbs) aAbs + 0.1 else it }
        }
        val speedV = speed.text.trim().toIntOrNull()
        if (type == WarningType.SPEED && (speedV == null || speedV !in 1..200)) { error = "Введите ограничение скорости, км/ч."; return }
        if (type == WarningType.OTHER && note.text.isBlank()) { error = "Напишите примечание."; return }
        val startRel = placeBounds?.first ?: existing?.startRelKm ?: 0.0
        val endRel = placeBounds?.let { if (it.second - it.first < 0.0999) it.first + 0.1 else it.second } ?: existing?.endRelKm ?: 0.0
        val w = TrackWarning(
            id = existing?.id ?: generateId(), sectionId = existing?.sectionId ?: sectionId,
            startAbsKm = aAbs, endAbsKm = bAbs, startRelKm = startRel, endRelKm = endRel,
            type = type, speedKmh = if (type == WarningType.SPEED) speedV else null,
            note = note.text.trim(), place = placeText.text.trim(),
        )
        error = onSave(w)
    }

    FollowDeviceOrientation()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Pst.bg,
    ) {
        Column(
            Modifier.fillMaxWidth().imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (existing != null) "ДУ-61: предупреждение" else "ДУ-61: новое предупреждение", style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WarningType.values().forEach { t -> RestrictionChip(t.title, type == t) { type = t; error = null } }
            }
            PstTextField(placeText, { placeText = it }, "Станция или перегон")
            if (type == WarningType.SPEED) {
                PstTextField(speed, { speed = it; error = null }, "Ограничение, км/ч", Modifier.fillMaxWidth(0.4f), keyboardOptions = numeric, mono = true)
            }
            if (kmRequired) {
                hint?.let { Text(it, style = Pst.sans.copy(fontSize = 12.sp), color = Pst.textMuted) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PstTextField(startKm, { startKm = it; error = null }, "Начало, км", Modifier.weight(1f), keyboardOptions = numeric, mono = true)
                    PstTextField(startPk, { startPk = it; error = null }, "Начало, пикет", Modifier.weight(1f), keyboardOptions = numeric, mono = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PstTextField(endKm, { endKm = it; error = null }, "Конец, км", Modifier.weight(1f), keyboardOptions = numeric, mono = true)
                    PstTextField(endPk, { endPk = it; error = null }, "Конец, пикет", Modifier.weight(1f), keyboardOptions = numeric, mono = true)
                }
                if (noEnd) Text("Конец не указан — предупреждение действует на одном пикете (100 м).", style = Pst.sans.copy(fontSize = 12.sp), color = Pst.textMuted)
            } else {
                Text("У участка нет километровой привязки — предупреждение займёт весь выбранный отрезок.", style = Pst.sans.copy(fontSize = 13.sp), color = Pst.textMuted)
            }
            PstTextField(note, { note = it; error = null }, if (type == WarningType.OTHER) "Примечание" else "Примечание (необязательно)", singleLine = false)
            error?.let { Text(it, style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 17.sp), color = Pst.danger) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (existing != null && onDelete != null) PstButton("Удалить", onDelete, kind = PstButtonKind.DANGER, height = 44.dp, fontSize = 14)
                PstButton("Отмена", onDismiss, kind = PstButtonKind.TEXT, height = 44.dp, fontSize = 14)
                PstButton("Сохранить", { save() }, kind = PstButtonKind.FILLED, height = 44.dp, fontSize = 14)
            }
        }
    }
}

@Composable
private fun RestrictionChip(text: String, on: Boolean, onClick: () -> Unit) {
    PstButton(text, onClick, kind = if (on) PstButtonKind.FILLED else PstButtonKind.TONAL, height = 36.dp, fontSize = 13)
}
