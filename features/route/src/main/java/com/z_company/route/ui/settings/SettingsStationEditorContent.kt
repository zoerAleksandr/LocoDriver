package com.z_company.route.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.z_company.route.component.AppInputBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.route.R
import com.z_company.route.viewmodel.StationNormEditorViewModel
import com.z_company.route.viewmodel.StationEditorState
import com.z_company.route.viewmodel.ServicePhaseAutoFillViewModel
import com.z_company.route.component.servicephase.PhaseMapPicker
import com.z_company.route.component.servicephase.StationChoiceSheet
import com.z_company.domain.entities.setting.ServicePhaseStationKind
import com.z_company.repository.remote_rest.response.OsmStationCandidate
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.z_company.route.viewmodel.StationNormField
import kotlinx.coroutines.delay

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsStationEditorContent(
    viewModel: StationNormEditorViewModel,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    // Navigate only when deleted
    LaunchedEffect(state.deleted) {
        if (state.deleted) onDone()
    }

    // Шторка подтверждения удаления — как при удалении маршрута.
    var showConfirmDelete by remember { mutableStateOf(false) }
    val deleteSheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    if (showConfirmDelete) {
        com.z_company.route.component.AppBottomSheet(
            onDismissRequest = { showConfirmDelete = false },
            sheetState = deleteSheetState,
            headerContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Удалить станцию?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    if (state.name.isNotBlank()) {
                        Text(
                            text = state.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            actions = listOf(
                com.z_company.route.component.BottomSheetAction(text = "Да, удалить") {
                    viewModel.delete()
                }
            )
        )
    }

    // Autosave with 500ms debounce — triggers whenever any editable field changes
    LaunchedEffect(
        state.name,
        state.appearanceToStartMin,
        state.endToBarrierMin,
        state.barrierToStartMin,
        state.endToWorkEndMin,
    ) {
        if (state.name.isNotBlank()) {
            delay(500)
            viewModel.save()
        }
    }

    // Inline numeric value editor
    var dialogField by remember { mutableStateOf<StationNormField?>(null) }
    var dialogText by remember { mutableStateOf("") }

    dialogField?.let { field ->
        AppInputBottomSheet(
            onDismissRequest = { dialogField = null },
            title = when (field) {
                StationNormField.APPEARANCE_TO_START -> "Явка → Начало"
                StationNormField.END_TO_BARRIER -> "Конец → КП"
                StationNormField.BARRIER_TO_START -> "КП → Начало"
                StationNormField.END_TO_WORK_END -> "Конец → Окончание работы"
            },
            initialValue = dialogText,
            onConfirm = { v ->
                v.toIntOrNull()?.let { viewModel.setField(field, it) }
                dialogField = null
            },
            label = "Значение",
            suffix = "мин",
            keyboardType = KeyboardType.Number,
            transform = { it.filter { c -> c.isDigit() }.take(3) },
            isValid = { it.toIntOrNull() != null },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        // ОСНОВНОЕ
        Text(
            text = "ОСНОВНОЕ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shapes.medium)
                .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Название",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
                BasicTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        if (state.name.isEmpty()) {
                            Text(
                                "Лянгасово",
                                style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        inner()
                    }
                )
            }
        }

        StationCoordinatesSection(viewModel = viewModel, state = state)
        StationAxisSection(state = state, onSet = viewModel::setAxis)

        // ПРИЁМКА
        Text(
            text = "Нормы ниже используются автоматически при заполнении времени приёмки и сдачи локомотива на этой станции.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp, top = 16.dp)
        )
        Text(
            text = "ПРИЁМКА",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, Shapes.medium)
                .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
        ) {
            StepperRow(
                label = "Явка → Начало",
                sub = "от явки до начала приёмки",
                value = state.appearanceToStartMin,
                onIncrement = { viewModel.increment(StationNormField.APPEARANCE_TO_START) },
                onDecrement = { viewModel.decrement(StationNormField.APPEARANCE_TO_START) },
                onValueClick = {
                    dialogText = (state.appearanceToStartMin ?: 0).toString()
                    dialogField = StationNormField.APPEARANCE_TO_START
                }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            StepperRow(
                label = "Конец → КП",
                sub = "от окончания приёмки до выхода на КП",
                value = state.endToBarrierMin,
                onIncrement = { viewModel.increment(StationNormField.END_TO_BARRIER) },
                onDecrement = { viewModel.decrement(StationNormField.END_TO_BARRIER) },
                onValueClick = {
                    dialogText = (state.endToBarrierMin ?: 0).toString()
                    dialogField = StationNormField.END_TO_BARRIER
                }
            )
        }

        // СДАЧА
        Text(
            text = "СДАЧА",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, Shapes.medium)
                .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
        ) {
            StepperRow(
                label = "КП → Начало",
                sub = "от выхода на КП до начала сдачи",
                value = state.barrierToStartMin,
                onIncrement = { viewModel.increment(StationNormField.BARRIER_TO_START) },
                onDecrement = { viewModel.decrement(StationNormField.BARRIER_TO_START) },
                onValueClick = {
                    dialogText = (state.barrierToStartMin ?: 0).toString()
                    dialogField = StationNormField.BARRIER_TO_START
                }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            StepperRow(
                label = "Конец → Окончание работы",
                sub = "от окончания сдачи до окончания работы",
                value = state.endToWorkEndMin,
                onIncrement = { viewModel.increment(StationNormField.END_TO_WORK_END) },
                onDecrement = { viewModel.decrement(StationNormField.END_TO_WORK_END) },
                onValueClick = {
                    dialogText = (state.endToWorkEndMin ?: 0).toString()
                    dialogField = StationNormField.END_TO_WORK_END
                }
            )
        }

        // Delete — always visible
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shapes.medium)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), Shapes.medium)
                .clickable { showConfirmDelete = true }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                painter = painterResource(R.drawable.delete_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Удалить станцию",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
internal fun StepperRow(
    label: String,
    value: Int?,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onValueClick: () -> Unit,
    sub: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label (+ пояснение) — takes all remaining space, wraps if long
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                softWrap = true
            )
            if (sub != null) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    softWrap = true,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Stepper control — always on the right
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // − button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(enabled = (value ?: 0) > 0, onClick = onDecrement),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "−",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if ((value ?: 0) > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    lineHeight = 18.sp
                )
            }

            // Value box — tappable for direct numeric input
            Box(
                modifier = Modifier
                    .widthIn(min = 78.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        RoundedCornerShape(10.dp)
                    )
                    .background(MaterialTheme.colorScheme.background)
                    .clickable(onClick = onValueClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (value != null) "$value мин" else "— мин",
                    fontFamily = MonoFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // + button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(enabled = (value ?: 0) < 120, onClick = onIncrement),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if ((value ?: 0) < 120) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}


// ─── Координаты и км оси (SCREEN_SPECS §6.6, справочник станций) ──────────────

private sealed interface CoordSearch {
    data object Idle : CoordSearch
    data object Searching : CoordSearch
    /** Применена станция [candidate] из найденных [candidates] (несколько — можно выбрать другую). */
    data class Found(val candidate: OsmStationCandidate, val candidates: List<OsmStationCandidate>) : CoordSearch
    data object NotFound : CoordSearch
    data object Failed : CoordSearch
}

@Composable
private fun StationCoordinatesSection(viewModel: StationNormEditorViewModel, state: StationEditorState) {
    val lookupVm: ServicePhaseAutoFillViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val scope = rememberCoroutineScope()
    var search by remember { mutableStateOf<CoordSearch>(CoordSearch.Idle) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var mapOpen by remember { mutableStateOf(false) }
    /** Несколько станций с таким названием — шторка выбора. */
    var choosing by remember { mutableStateOf<List<OsmStationCandidate>?>(null) }
    val has = state.hasCoordinates

    fun apply(c: OsmStationCandidate, all: List<OsmStationCandidate>) {
        viewModel.setCoordinates(
            latitude = c.latitude,
            longitude = c.longitude,
            esr = c.esr?.takeIf(String::isNotBlank),
            osmId = c.osmId,
            kind = if (c.kind == "halt") ServicePhaseStationKind.HALT else ServicePhaseStationKind.STATION,
        )
        search = CoordSearch.Found(c, all)
    }

    choosing?.let { candidates ->
        StationChoiceSheet(
            query = state.name.trim(),
            candidates = candidates,
            current = (search as? CoordSearch.Found)?.candidate,
            onChoose = {
                apply(it, candidates)
                choosing = null
            },
            onDismiss = {
                // Отказ от выбора при первом поиске — координаты не меняем.
                if (search == CoordSearch.Searching) search = CoordSearch.Idle
                choosing = null
            },
        )
    }

    if (mapOpen) {
        PhaseMapPicker(
            stationName = state.name.ifBlank { "Станция" },
            kindLabel = if (state.kind == ServicePhaseStationKind.HALT) "О.П." else "станция",
            start = if (has) state.latitude!! to state.longitude!! else null,
            neighbours = state.neighbours,
            onPick = {
                viewModel.setCoordinates(it.latitude, it.longitude, pickedOnMap = true)
                search = CoordSearch.Idle
                mapOpen = false
            },
            onDismiss = { mapOpen = false },
        )
    }

    fun find() {
        val name = state.name.trim()
        if (name.isBlank()) return
        search = CoordSearch.Searching
        searchJob?.cancel()
        searchJob = scope.launch {
            val found = lookupVm.lookup(name, state.country, state.neighbours)
            when {
                found == null -> search = CoordSearch.Failed
                found.isEmpty() -> search = CoordSearch.NotFound
                found.size == 1 -> apply(found.first(), found)
                else -> choosing = found
            }
        }
    }

    SectionTitle("КООРДИНАТЫ")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, Shapes.medium)
            .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (has) StatusOk else StatusWarn)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (has) "Координаты есть" else "Нет координат",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (has) {
                        listOfNotNull(
                            "%.5f, %.5f".format(java.util.Locale.US, state.latitude, state.longitude),
                            state.esr?.let { "ЕСР $it" },
                        ).joinToString(" · ")
                    } else {
                        "Без них станция не определится по GPS и не попадёт на карту плеча"
                    },
                    style = if (has) MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont)
                    else MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                )
            }
        }
        when (val s = search) {
            CoordSearch.Searching -> SearchNote(
                text = "Ищем «${state.name.trim()}» на карте…",
                action = "Отменить",
                onAction = {
                    searchJob?.cancel()
                    search = CoordSearch.Idle
                },
            )
            is CoordSearch.Found -> {
                val c = s.candidate
                val title = listOfNotNull(
                    "Нашлась: ${c.name}" + if (c.kind == "halt") ", О.П." else "",
                    c.region ?: c.operator,
                ).joinToString(" · ")
                if (s.candidates.size > 1) {
                    SearchNote(text = title, action = "Выбрать другую", onAction = { choosing = s.candidates })
                } else {
                    SearchNote(text = "$title. Если это не та станция — укажите точку на карте вручную.")
                }
            }
            CoordSearch.NotFound -> SearchNote(text = "Не нашлось на карте — проверьте название или укажите точку вручную.")
            CoordSearch.Failed -> SearchNote(text = "Не удалось связаться с сервером. Попробуйте позже.")
            CoordSearch.Idle -> Unit
        }
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        ButtonRow(
            text = if (has) "Определить заново" else "Определить автоматически",
            enabled = state.name.isNotBlank() && search != CoordSearch.Searching,
            onClick = ::find,
        )
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        ActionRow(
            text = if (has) "Уточнить на карте" else "Указать на карте",
            enabled = state.name.isNotBlank(),
            onClick = { mapOpen = true },
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun StationAxisSection(state: StationEditorState, onSet: (Int?, Int?) -> Unit) {
    var editing by remember { mutableStateOf<Boolean?>(null) } // true — км, false — пикет
    editing?.let { km ->
        AppInputBottomSheet(
            onDismissRequest = { editing = null },
            title = if (km) "Километр оси станции" else "Пикет оси станции",
            initialValue = (if (km) state.axisKm else state.axisPicket)?.toString() ?: "",
            onConfirm = { v ->
                val n = v.toIntOrNull()
                if (km) onSet(n, state.axisPicket) else onSet(state.axisKm, n)
                editing = null
            },
            label = if (km) "Километр" else "Пикет, 1–10",
            suffix = if (km) "км" else "пк",
            keyboardType = KeyboardType.Number,
            transform = { it.filter { c -> c.isDigit() }.take(if (km) 5 else 2) },
            isValid = { v -> v.isEmpty() || (if (km) v.toIntOrNull() != null else v.toIntOrNull() in 1..10) },
        )
    }
    SectionTitle("КИЛОМЕТРАЖ")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, Shapes.medium)
            .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
    ) {
        ValueRow(label = "КМ", value = state.axisKm?.let { "$it км" }, onClick = { editing = true })
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        ValueRow(
            label = "ПК",
            value = state.axisPicket?.let { "$it пк" },
            enabled = state.axisKm != null,
            onClick = { editing = false },
        )
    }
    Text(
        text = "Ось станции по ж/д километражу.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
    )
}

private val StatusOk = androidx.compose.ui.graphics.Color(0xFF2E9E5B)
private val StatusWarn = androidx.compose.ui.graphics.Color(0xFFE59A1A)

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun SearchNote(text: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 38.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.clip(Shapes.small).clickable(onClick = onAction).padding(8.dp),
            )
        }
    }
}

@Composable
private fun ActionRow(text: String, enabled: Boolean, onClick: () -> Unit, icon: Int? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.weight(1f),
        )
        androidx.compose.material3.Icon(
            painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_right_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ValueRow(label: String, value: String?, onClick: () -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value ?: "—",
            fontFamily = MonoFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (value != null && enabled) 1f else 0.4f),
        )
    }
}

/** Строка-кнопка действия в карточке (без шеврона — действие на месте, не переход). */
@Composable
private fun ButtonRow(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = if (enabled) 1f else 0.4f),
        )
    }
}
