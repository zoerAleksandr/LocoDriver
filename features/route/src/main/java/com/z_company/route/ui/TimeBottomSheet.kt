@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.repositories.LocomotiveSeriesRepository
import com.z_company.domain.repositories.StationNormRepository
import com.z_company.domain.util.generateId
import com.z_company.route.component.AppDateTimePicker
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Calendar
import kotlin.time.Clock

// ── Private models ─────────────────────────────────────────────────────────
private data class WarnItem(val text: String, val hint: String?, val cta: String?, val isSeries: Boolean)

/** Structured delta info for smart formatting. */
private data class DeltaInfo(val actualMinutes: Int, val normMinutes: Int?)

// ── Public models ──────────────────────────────────────────────────────────
data class TimeSheetResult(
    val startTime: Long?,
    val endTime: Long?,
    val barrierOut: Long?,
    val barrierIn: Long?,
    val routeEndWork: Long?,
    val stationId: String?,
)

// ── Color helpers ──────────────────────────────────────────────────────────
@Composable private fun bgSubtle() = MaterialTheme.colorScheme.surface
@Composable private fun accent() = MaterialTheme.colorScheme.tertiary
@Composable private fun accentSoft() = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
@Composable private fun accentInk() = Color.White
@Composable private fun textColor() = MaterialTheme.colorScheme.primary
@Composable private fun textMuted() = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
@Composable private fun textFaint() = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
@Composable private fun borderColor() = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
@Composable private fun warnColor() = Color(0xFFD98F20)
@Composable private fun warnSoft() = Color(0xFFD98F20).copy(alpha = 0.10f)
@Composable private fun surfaceCard() = MaterialTheme.colorScheme.secondary
private val greenOk = Color(0xFF00B341)

// ──────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeBottomSheet(
    kind: String,
    seriesName: String?,
    locoType: LocoType = LocoType.ELECTRIC,
    initialStartTime: Long?,
    initialEndTime: Long?,
    initialBarrierOut: Long?,
    initialBarrierIn: Long?,
    routeStartWork: Long?,
    routeEndWork: Long?,
    initialStationId: String?,
    timeZoneText: String,
    onSave: (TimeSheetResult) -> Unit,
    onClose: () -> Unit,
    onNavigateToSeriesSettings: () -> Unit,
    onNavigateToStationSettings: () -> Unit,
    onEditStation: ((String) -> Unit)? = null,
    onSeriesChanged: ((String) -> Unit)? = null,
    onEditSeries: ((String) -> Unit)? = null,
) {
    val seriesRepo: LocomotiveSeriesRepository = koinInject()
    val stationRepo: StationNormRepository = koinInject()
    val scope = rememberCoroutineScope()

    val allSeries by seriesRepo.getAllFlow().collectAsState(initial = emptyList())
    val allStations by stationRepo.getAllFlow().collectAsState(initial = emptyList())

    var startTime by remember { mutableStateOf(initialStartTime) }
    var endTime by remember { mutableStateOf(initialEndTime) }
    var barrierOut by remember { mutableStateOf(initialBarrierOut) }
    var barrierIn by remember { mutableStateOf(initialBarrierIn) }
    var workEnd by remember { mutableStateOf(routeEndWork) }

    var selectedStation by remember(initialStationId, allStations) {
        mutableStateOf(allStations.find { it.stationId == initialStationId })
    }
    var selectedSeriesName by remember { mutableStateOf(seriesName) }
    val selectedSeries: LocomotiveSeries? = allSeries.find { it.name == selectedSeriesName }
    val seriesInRepo = selectedSeries != null

    var showStationPicker by remember { mutableStateOf(false) }
    var showSeriesPicker by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var showBarrierOutPicker by remember { mutableStateOf(false) }
    var showBarrierInPicker by remember { mutableStateOf(false) }
    var showWorkEndPicker by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ── Time formatting ──
    fun fmtTime(ms: Long?): String? {
        if (ms == null) return null
        return try {
            val offsetMs = run {
                val gmt = timeZoneText.replace("GMT", "").trim()
                val sign = if (gmt.startsWith("-")) -1 else 1
                val num = gmt.removePrefix("+").removePrefix("-").toDoubleOrNull() ?: 3.0
                (sign * num * 3_600_000).toLong()
            }
            val d = java.util.Date(ms + offsetMs)
            val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            cal.time = d
            "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        } catch (e: Exception) { null }
    }

    // ── Smart delta builder ──
    fun buildDelta(fromMs: Long?, toMs: Long?, normMinutes: Int?): DeltaInfo? {
        if (fromMs == null || toMs == null) return null
        val actualMin = ((toMs - fromMs) / 60_000).toInt()
        return DeltaInfo(actualMin, normMinutes)
    }

    // ── Norm logic ──
    val noSeriesNorm = selectedSeries != null && run {
        if (kind == "acceptance") selectedSeries.acceptanceDurationMin == null
        else selectedSeries.deliveryDurationMin == null
    }
    val stationEmpty = selectedStation == null
    val noStationNorm = !stationEmpty && selectedStation!!.let { s ->
        s.appearanceToStartMin == null || s.endToBarrierMin == null ||
            s.barrierToStartMin == null || s.endToWorkEndMin == null
    }
    val canApplyNorms = selectedSeries != null || selectedStation != null

    fun applyNorms() {
        if (kind == "acceptance") {
            val base = routeStartWork ?: return
            val appearMin = selectedStation?.appearanceToStartMin ?: 0
            val newStart = base + appearMin * 60_000L
            val durMin = selectedSeries?.acceptanceDurationMin ?: 0
            val newEnd = newStart + durMin * 60_000L
            val barrierMin = selectedStation?.endToBarrierMin ?: 0
            startTime = newStart; endTime = newEnd; barrierOut = newEnd + barrierMin * 60_000L
        } else {
            val bi = barrierIn ?: return
            val barrierMin = selectedStation?.barrierToStartMin ?: 0
            val newStart = bi + barrierMin * 60_000L
            val durMin = selectedSeries?.deliveryDurationMin ?: 0
            val newEnd = newStart + durMin * 60_000L
            val workEndMin = selectedStation?.endToWorkEndMin ?: 0
            startTime = newStart; endTime = newEnd; workEnd = newEnd + workEndMin * 60_000L
        }
    }

    // ── Sequence validation ──
    val startTimeError: Boolean
    val endTimeError: Boolean
    val barrierOutError: Boolean  // acceptance only
    val workEndError: Boolean     // delivery only

    if (kind == "acceptance") {
        startTimeError = startTime != null && routeStartWork != null && startTime!! <= routeStartWork
        endTimeError = endTime != null && startTime != null && endTime!! <= startTime!!
        barrierOutError = barrierOut != null && endTime != null && barrierOut!! <= endTime!!
        workEndError = false
    } else {
        startTimeError = startTime != null && barrierIn != null && startTime!! <= barrierIn!!
        endTimeError = endTime != null && startTime != null && endTime!! <= startTime!!
        barrierOutError = false
        workEndError = workEnd != null && endTime != null && workEnd!! <= endTime!!
    }
    val hasSequenceError = startTimeError || endTimeError || barrierOutError || workEndError

    // ── Save norm state ──

    // Series: active if times filled, whether series is in repo or not
    val timesFilledForSeries = startTime != null && endTime != null
    val canSaveSeriesNorm = !selectedSeriesName.isNullOrBlank() && timesFilledForSeries && run {
        if (seriesInRepo && selectedSeries != null) {
            val dur = ((endTime!! - startTime!!) / 60_000).toInt()
            val saved = if (kind == "acceptance") selectedSeries.acceptanceDurationMin
            else selectedSeries.deliveryDurationMin
            saved == null || saved != dur
        } else {
            true // not in repo → always can add
        }
    }

    val seriesNormSubtitle = if (!seriesInRepo && !selectedSeriesName.isNullOrBlank())
        "Добавить в справочник"
    else if (kind == "acceptance") "Длительность приёмки" else "Длительность сдачи"

    val canSaveStationNorm = selectedStation != null && run {
        val st = selectedStation!!
        if (kind == "acceptance") {
            routeStartWork != null && startTime != null && endTime != null &&
                (((startTime!! - routeStartWork) / 60_000).toInt() != st.appearanceToStartMin ||
                    (barrierOut != null && ((barrierOut!! - endTime!!) / 60_000).toInt() != st.endToBarrierMin))
        } else {
            barrierIn != null && startTime != null && endTime != null && workEnd != null &&
                (((startTime!! - barrierIn!!) / 60_000).toInt() != st.barrierToStartMin ||
                    ((workEnd!! - endTime!!) / 60_000).toInt() != st.endToWorkEndMin)
        }
    }

    // ── Pickers ──
    if (showStartPicker) AppDateTimePicker(
        title = if (kind == "acceptance") "Начало приёмки" else "Начало сдачи",
        onDateTimeSelected = { startTime = it; showStartPicker = false },
        onDismiss = { showStartPicker = false },
        startDateTime = startTime ?: Calendar.getInstance().timeInMillis,
        timeZoneStr = timeZoneText, recentTimes = emptyList(), onRecentTimeSaved = {}
    )
    if (showEndPicker) AppDateTimePicker(
        title = if (kind == "acceptance") "Окончание приёмки" else "Окончание сдачи",
        onDateTimeSelected = { endTime = it; showEndPicker = false },
        onDismiss = { showEndPicker = false },
        startDateTime = endTime ?: Calendar.getInstance().timeInMillis,
        timeZoneStr = timeZoneText, recentTimes = emptyList(), onRecentTimeSaved = {}
    )
    if (showBarrierOutPicker) AppDateTimePicker(
        title = "Выход на КП",
        onDateTimeSelected = { barrierOut = it; showBarrierOutPicker = false },
        onDismiss = { showBarrierOutPicker = false },
        startDateTime = barrierOut ?: Calendar.getInstance().timeInMillis,
        timeZoneStr = timeZoneText, recentTimes = emptyList(), onRecentTimeSaved = {}
    )
    if (showBarrierInPicker) AppDateTimePicker(
        title = "Заход на КП",
        onDateTimeSelected = { barrierIn = it; showBarrierInPicker = false },
        onDismiss = { showBarrierInPicker = false },
        startDateTime = barrierIn ?: Calendar.getInstance().timeInMillis,
        timeZoneStr = timeZoneText, recentTimes = emptyList(), onRecentTimeSaved = {}
    )
    if (showWorkEndPicker) AppDateTimePicker(
        title = "Окончание работы",
        onDateTimeSelected = { workEnd = it; showWorkEndPicker = false },
        onDismiss = { showWorkEndPicker = false },
        startDateTime = workEnd ?: Calendar.getInstance().timeInMillis,
        timeZoneStr = timeZoneText, recentTimes = emptyList(), onRecentTimeSaved = {}
    )
    if (showStationPicker) StationPickerSheet(
        onSelect = { st -> selectedStation = st; showStationPicker = false },
        onClose = { showStationPicker = false },
        onNavigateToSettings = { showStationPicker = false; onClose(); onNavigateToStationSettings() },
        onEditStation = if (onEditStation != null) { st ->
            showStationPicker = false; onClose(); onEditStation(st.stationId)
        } else null
    )
    if (showSeriesPicker) SeriesPickerSheet(
        onSelect = { s ->
            selectedSeriesName = s.name
            onSeriesChanged?.invoke(s.name)
            showSeriesPicker = false
        },
        onClose = { showSeriesPicker = false },
        onNavigateToSettings = { showSeriesPicker = false; onClose(); onNavigateToSeriesSettings() },
        onEditSeries = if (onEditSeries != null) { s ->
            showSeriesPicker = false; onClose(); onEditSeries(s.seriesId)
        } else null
    )

    // ── Sheet ──
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(32.dp, 4.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // ── Header: title + "По нормам" pill ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (kind == "acceptance") "Приёмка" else "Сдача",
                    fontSize = 22.sp, fontWeight = FontWeight.W500, color = textColor()
                )
                val normsActive = canApplyNorms && !noSeriesNorm && !stationEmpty && !noStationNorm
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (normsActive) accent() else bgSubtle())
                        .then(
                            if (!normsActive) Modifier.border(1.dp, borderColor(), RoundedCornerShape(999.dp))
                            else Modifier
                        )
                        .clickable(enabled = canApplyNorms) { applyNorms() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(com.z_company.route.R.drawable.electric_bolt_24px),
                        contentDescription = null, modifier = Modifier.size(13.dp),
                        tint = if (normsActive) accentInk() else textFaint()
                    )
                    Text(
                        text = "По нормам", fontSize = 13.sp, fontWeight = FontWeight.W500,
                        color = if (normsActive) accentInk() else textFaint()
                    )
                }
            }

            // ── Context fields ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContextFieldItem(
                    iconRes = com.z_company.route.R.drawable.electric_bolt_24px,
                    label = "Серия",
                    value = selectedSeriesName ?: "Выберите серию",
                    isEmpty = selectedSeriesName == null,
                    isLocked = false,
                    onClick = { showSeriesPicker = true }
                )
                val stationLabel = if (kind == "acceptance") "Станция приёмки" else "Станция сдачи"
                ContextFieldItem(
                    iconRes = com.z_company.route.R.drawable.zoom_in_map_24px,
                    label = stationLabel,
                    value = selectedStation?.name ?: "Выберите станцию",
                    isEmpty = selectedStation == null,
                    isLocked = false,
                    onClick = { showStationPicker = true }
                )
            }

            // ── Warnings ──
            val warnings: List<WarnItem> = buildList {
                if (noSeriesNorm && selectedSeriesName != null)
                    add(WarnItem("Нет нормы для серии $selectedSeriesName", null, "Настроить серию", true))
                if (stationEmpty)
                    add(WarnItem(
                        "Станция ${if (kind == "acceptance") "приёмки" else "сдачи"} не выбрана",
                        "Выберите станцию, чтобы применить нормы интервалов",
                        null, false
                    ))
                else if (noStationNorm)
                    add(WarnItem("Нет нормы для станции ${selectedStation!!.name}", null, "Настроить станцию", false))
            }
            if (warnings.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    warnings.forEach { warn ->
                        NormWarnItem(
                            text = warn.text, hint = warn.hint, cta = warn.cta,
                            onCta = {
                                onClose()
                                if (warn.isSeries) onNavigateToSeriesSettings()
                                else onNavigateToStationSettings()
                            }
                        )
                    }
                }
            }

            // ── Time rows ──
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                if (kind == "acceptance") {
                    SheetTimeRowItem(
                        label = "Явка", time = fmtTime(routeStartWork), delta = null,
                        isFirst = true, isLocked = true, isHighlighted = false, sequenceError = false,
                        iconRes = com.z_company.route.R.drawable.schedule_24px, onClick = null
                    )
                    RowConnector()
                    SheetTimeRowItem(
                        label = "Начало приёмки", time = fmtTime(startTime),
                        delta = buildDelta(routeStartWork, startTime, selectedStation?.appearanceToStartMin),
                        isFirst = false, isLocked = false, isHighlighted = false,
                        sequenceError = startTimeError,
                        iconRes = com.z_company.route.R.drawable.electric_bolt_24px,
                        onClick = { showStartPicker = true }
                    )
                    RowConnector()
                    SheetTimeRowItem(
                        label = "Окончание приёмки", time = fmtTime(endTime),
                        delta = buildDelta(startTime, endTime, selectedSeries?.acceptanceDurationMin),
                        isFirst = false, isLocked = false, isHighlighted = false,
                        sequenceError = endTimeError,
                        iconRes = com.z_company.route.R.drawable.check_circle_24px,
                        onClick = { showEndPicker = true }
                    )
                    RowConnector()
                    SheetTimeRowItem(
                        label = "Выход на КП", time = fmtTime(barrierOut),
                        delta = buildDelta(endTime, barrierOut, selectedStation?.endToBarrierMin),
                        isFirst = false, isLocked = false, isHighlighted = true,
                        sequenceError = barrierOutError,
                        iconRes = com.z_company.route.R.drawable.swap_vert_24px,
                        onClick = { showBarrierOutPicker = true }
                    )
                } else {
                    SheetTimeRowItem(
                        label = "Заход на КП", time = fmtTime(barrierIn), delta = null,
                        isFirst = true, isLocked = false, isHighlighted = true, sequenceError = false,
                        iconRes = com.z_company.route.R.drawable.swap_vert_24px,
                        onClick = { showBarrierInPicker = true }
                    )
                    RowConnector()
                    SheetTimeRowItem(
                        label = "Начало сдачи", time = fmtTime(startTime),
                        delta = buildDelta(barrierIn, startTime, selectedStation?.barrierToStartMin),
                        isFirst = false, isLocked = false, isHighlighted = false,
                        sequenceError = startTimeError,
                        iconRes = com.z_company.route.R.drawable.electric_bolt_24px,
                        onClick = { showStartPicker = true }
                    )
                    RowConnector()
                    SheetTimeRowItem(
                        label = "Окончание сдачи", time = fmtTime(endTime),
                        delta = buildDelta(startTime, endTime, selectedSeries?.deliveryDurationMin),
                        isFirst = false, isLocked = false, isHighlighted = false,
                        sequenceError = endTimeError,
                        iconRes = com.z_company.route.R.drawable.check_circle_24px,
                        onClick = { showEndPicker = true }
                    )
                    RowConnector()
                    SheetTimeRowItem(
                        label = "Окончание работы", time = fmtTime(workEnd),
                        delta = buildDelta(endTime, workEnd, selectedStation?.endToWorkEndMin),
                        isFirst = false, isLocked = false, isHighlighted = false,
                        sequenceError = workEndError,
                        iconRes = com.z_company.route.R.drawable.nest_clock_farsight_analog_24px,
                        onClick = { showWorkEndPicker = true }
                    )
                }
            }

            // ── Save norm buttons ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val seriesTitle = if (!selectedSeriesName.isNullOrBlank())
                    "Сохранить норму серии $selectedSeriesName" else "Сохранить норму серии"

                SaveNormItem(
                    iconRes = com.z_company.route.R.drawable.electric_bolt_24px,
                    title = seriesTitle,
                    subtitle = seriesNormSubtitle,
                    isReady = canSaveSeriesNorm,
                    isDisabled = selectedSeriesName.isNullOrBlank(),
                    onClick = {
                        if (canSaveSeriesNorm && !selectedSeriesName.isNullOrBlank()
                            && startTime != null && endTime != null
                        ) {
                            val dur = ((endTime!! - startTime!!) / 60_000).toInt()
                            scope.launch {
                                val all = seriesRepo.getAll().toMutableList()
                                if (seriesInRepo && selectedSeries != null) {
                                    val updated = if (kind == "acceptance")
                                        selectedSeries.copy(acceptanceDurationMin = dur)
                                    else selectedSeries.copy(deliveryDurationMin = dur)
                                    val idx = all.indexOfFirst { it.seriesId == updated.seriesId }
                                    if (idx >= 0) all[idx] = updated else all.add(updated)
                                } else {
                                    // Create new entry
                                    val newSeries = LocomotiveSeries(
                                        seriesId = generateId(),
                                        name = selectedSeriesName!!.trim(),
                                        type = locoType,
                                        acceptanceDurationMin = if (kind == "acceptance") dur else null,
                                        deliveryDurationMin = if (kind == "delivery") dur else null,
                                        updatedAt = Clock.System.now().toEpochMilliseconds(),
                                    )
                                    all.add(newSeries)
                                }
                                seriesRepo.replaceAll(all).collect {}
                            }
                        }
                    }
                )

                val stationTitle = if (stationEmpty) "Сохранить норму станции"
                else "Сохранить норму станции ${selectedStation!!.name}"
                SaveNormItem(
                    iconRes = com.z_company.route.R.drawable.zoom_in_map_24px,
                    title = stationTitle,
                    subtitle = "Интервалы явки и КП",
                    isReady = canSaveStationNorm,
                    isDisabled = stationEmpty,
                    onClick = {
                        if (canSaveStationNorm && selectedStation != null) {
                            val st = selectedStation!!.copy()
                            val updated = if (kind == "acceptance") {
                                val newAppear = if (routeStartWork != null && startTime != null)
                                    ((startTime!! - routeStartWork) / 60_000).toInt() else st.appearanceToStartMin
                                val newBarrier = if (endTime != null && barrierOut != null)
                                    ((barrierOut!! - endTime!!) / 60_000).toInt() else st.endToBarrierMin
                                st.copy(appearanceToStartMin = newAppear, endToBarrierMin = newBarrier)
                            } else {
                                val newBarrier = if (barrierIn != null && startTime != null)
                                    ((startTime!! - barrierIn!!) / 60_000).toInt() else st.barrierToStartMin
                                val newWorkEnd = if (endTime != null && workEnd != null)
                                    ((workEnd!! - endTime!!) / 60_000).toInt() else st.endToWorkEndMin
                                st.copy(barrierToStartMin = newBarrier, endToWorkEndMin = newWorkEnd)
                            }
                            scope.launch {
                                val all = stationRepo.getAll().toMutableList()
                                val idx = all.indexOfFirst { it.stationId == updated.stationId }
                                if (idx >= 0) all[idx] = updated else all.add(updated)
                                stationRepo.replaceAll(all).collect {}
                            }
                            selectedStation = updated
                        }
                    }
                )
            }

            // ── Done button — disabled if sequence error ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (hasSequenceError) bgSubtle() else accent())
                    .then(
                        if (!hasSequenceError) Modifier.border(0.dp, Color.Transparent, RoundedCornerShape(12.dp))
                        else Modifier.border(1.dp, borderColor(), RoundedCornerShape(12.dp))
                    )
                    .then(
                        if (!hasSequenceError) Modifier.clickable {
                            scope.launch {
                                sheetState.hide()
                                onSave(TimeSheetResult(startTime, endTime, barrierOut, barrierIn, workEnd, selectedStation?.stationId))
                            }
                        } else Modifier
                    )
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Готово", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    color = if (hasSequenceError) textFaint() else accentInk()
                )
            }
        }
    }
}

// ── ContextFieldItem ─────────────────────────────────────────────────────
@Composable
private fun ContextFieldItem(
    iconRes: Int,
    label: String,
    value: String,
    isEmpty: Boolean,
    isLocked: Boolean,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(surfaceCard())
            .border(1.dp, borderColor(), RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isEmpty) bgSubtle() else accentSoft()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes), contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isEmpty) textFaint() else accent()
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(), fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                letterSpacing = 1.1.sp, color = textMuted(), lineHeight = 10.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = if (isEmpty) textFaint() else textColor(),
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                lineHeight = (13 * 1.2).sp
            )
        }
        if (isLocked) {
            Text(
                text = "лок", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = textMuted(),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(bgSubtle())
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        } else {
            Icon(
                painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_right_24px),
                contentDescription = null, modifier = Modifier.size(12.dp), tint = textFaint()
            )
        }
    }
}

// ── NormWarnItem ─────────────────────────────────────────────────────────
@Composable
private fun NormWarnItem(
    text: String,
    hint: String? = null,
    cta: String? = null,
    onCta: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(warnSoft())
            .border(1.dp, warnColor(), RoundedCornerShape(12.dp))
            .padding(10.dp, 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(20.dp).clip(CircleShape).background(warnColor()),
            contentAlignment = Alignment.Center
        ) {
            Text("!", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, color = Color.White)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = textColor(), lineHeight = (13 * 1.3).sp)
            if (hint != null) {
                Spacer(Modifier.height(3.dp))
                Text(hint, fontSize = 12.sp, color = textMuted(), lineHeight = (12 * 1.4).sp)
            }
            if (cta != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(surfaceCard())
                        .border(1.dp, borderColor(), RoundedCornerShape(8.dp))
                        .clickable(onClick = onCta)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(cta, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accent())
                    Icon(
                        painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_right_24px),
                        contentDescription = null, modifier = Modifier.size(10.dp), tint = accent()
                    )
                }
            }
        }
    }
}

// ── Row connector ─────────────────────────────────────────────────────────
@Composable
private fun RowConnector() {
    Box(modifier = Modifier.padding(start = 35.dp)) {
        Box(Modifier.size(2.dp, 8.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)))
    }
}

// ── SheetTimeRowItem ─────────────────────────────────────────────────────
@Composable
private fun SheetTimeRowItem(
    iconRes: Int,
    label: String,
    time: String?,
    delta: DeltaInfo?,
    isFirst: Boolean,
    isLocked: Boolean,
    isHighlighted: Boolean,
    sequenceError: Boolean,
    onClick: (() -> Unit)?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isHighlighted) accentSoft() else Color.Transparent)
                .then(if (onClick != null && !isLocked) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isHighlighted) accent() else bgSubtle())
                    .alpha(if (isLocked) 0.7f else 1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes), contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = when {
                        isHighlighted -> accentInk()
                        isLocked -> textMuted()
                        else -> textColor()
                    }
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(label, fontSize = 14.sp, fontWeight = FontWeight.W500, color = textColor())
                    if (isLocked) {
                        Text(
                            text = "из маршрута", fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace, color = textMuted(),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(bgSubtle())
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
                // Smart delta
                if (delta != null) {
                    Spacer(Modifier.height(4.dp))
                    SmartDelta(delta)
                }
            }

            // Time button
            Box(
                modifier = Modifier
                    .widthIn(min = 76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (time != null) MaterialTheme.colorScheme.background else bgSubtle())
                    .then(
                        if (time != null) Modifier.border(1.dp, borderColor(), RoundedCornerShape(12.dp))
                        else Modifier
                    )
                    .then(if (onClick != null && !isLocked) Modifier.clickable(onClick = onClick) else Modifier)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = time ?: "—:—", fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace,
                    color = if (time != null) textColor() else textFaint()
                )
            }
        }

        // Inline sequence error message
        if (sequenceError) {
            Row(
                modifier = Modifier.padding(start = 66.dp, end = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    painter = painterResource(com.z_company.route.R.drawable.dangerous_24px),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Время должно быть позже предыдущего",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/** Smart delta row: colours based on actual vs norm. */
@Composable
private fun SmartDelta(delta: DeltaInfo) {
    val actualStr = if (delta.actualMinutes >= 0) "+${delta.actualMinutes} мин"
    else "${delta.actualMinutes} мин"

    if (delta.normMinutes == null) {
        // No norm — show actual only
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                painter = painterResource(com.z_company.route.R.drawable.electric_bolt_24px),
                contentDescription = null, modifier = Modifier.size(10.dp), tint = textMuted()
            )
            Text(actualStr, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = textMuted())
        }
    } else {
        val diff = delta.actualMinutes - delta.normMinutes
        val (accentColor, annotation) = when {
            diff == 0 -> textMuted() to "норма"  // exact match
            diff > 0 -> MaterialTheme.colorScheme.error to "на $diff мин больше"
            else -> greenOk to "на ${-diff} мин меньше"
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                painter = painterResource(com.z_company.route.R.drawable.electric_bolt_24px),
                contentDescription = null, modifier = Modifier.size(10.dp), tint = accentColor
            )
            Text(actualStr, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = accentColor)
            Text(annotation, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = accentColor)
        }
    }
}

// ── SaveNormItem ─────────────────────────────────────────────────────────
@Composable
private fun SaveNormItem(
    iconRes: Int,
    title: String,
    subtitle: String,
    isReady: Boolean,
    isDisabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isReady && !isDisabled) accentSoft() else bgSubtle())
            .then(
                if (isDisabled) Modifier.border(1.dp, borderColor(), RoundedCornerShape(12.dp))
                else Modifier
            )
            .then(if (!isDisabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when {
                        isDisabled -> MaterialTheme.colorScheme.background
                        isReady -> accent()
                        else -> bgSubtle()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes), contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = when {
                    isDisabled -> textFaint()
                    isReady -> accentInk()
                    else -> accent()
                }
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = if (isDisabled) textFaint() else textColor(), lineHeight = (13 * 1.2).sp
            )
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = textMuted())
        }
        Icon(
            painter = painterResource(com.z_company.route.R.drawable.check_circle_24px),
            contentDescription = null, modifier = Modifier.size(16.dp),
            tint = if (isDisabled) textFaint() else accent()
        )
    }
}
