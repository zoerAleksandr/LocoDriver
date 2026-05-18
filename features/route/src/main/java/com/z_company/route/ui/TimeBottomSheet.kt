package com.z_company.route.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.norma_time.StationNorm
import com.z_company.domain.repositories.LocomotiveSeriesRepository
import com.z_company.domain.repositories.StationNormRepository
import com.z_company.route.component.AppDateTimePicker
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Calendar

data class TimeSheetResult(
    val startTime: Long?,
    val endTime: Long?,
    val barrierOut: Long?,
    val barrierIn: Long?,
    val routeEndWork: Long?,
    val stationId: String?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeBottomSheet(
    kind: String,
    seriesName: String?,
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
) {
    val seriesRepo: LocomotiveSeriesRepository = koinInject()
    val stationRepo: StationNormRepository = koinInject()

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
    val selectedSeries = allSeries.find { it.name == seriesName }

    var showStationPicker by remember { mutableStateOf(false) }

    // Time pickers
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var showBarrierOutPicker by remember { mutableStateOf(false) }
    var showBarrierInPicker by remember { mutableStateOf(false) }
    var showWorkEndPicker by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun fmtTime(ms: Long?): String {
        if (ms == null) return "—"
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
        } catch (e: Exception) { "—" }
    }

    fun deltaStr(from: Long?, to: Long?): String? {
        if (from == null || to == null) return null
        val diff = ((to - from) / 60_000).toInt()
        if (diff == 0) return null
        return if (diff >= 0) "+$diff мин" else "$diff мин"
    }

    fun canApplyNorms() = selectedSeries != null || selectedStation != null

    fun applyNorms() {
        if (kind == "acceptance") {
            val base = routeStartWork ?: return
            val appearMin = selectedStation?.appearanceToStartMin ?: 0
            val newStart = base + appearMin * 60_000L
            val durMin = selectedSeries?.acceptanceDurationMin ?: 0
            val newEnd = newStart + durMin * 60_000L
            val barrierMin = selectedStation?.endToBarrierMin ?: 0
            val newBarrier = newEnd + barrierMin * 60_000L
            startTime = newStart
            endTime = newEnd
            barrierOut = newBarrier
        } else {
            val bi = barrierIn ?: return
            val barrierMin = selectedStation?.barrierToStartMin ?: 0
            val newStart = bi + barrierMin * 60_000L
            val durMin = selectedSeries?.deliveryDurationMin ?: 0
            val newEnd = newStart + durMin * 60_000L
            val workEndMin = selectedStation?.endToWorkEndMin ?: 0
            workEnd = newEnd + workEndMin * 60_000L
            startTime = newStart
            endTime = newEnd
        }
    }

    // Warnings
    val warnings = buildList {
        if (selectedSeries != null) {
            val hasNorm = if (kind == "acceptance") selectedSeries.acceptanceDurationMin != null
            else selectedSeries.deliveryDurationMin != null
            if (!hasNorm) add(Triple("Нет нормы для серии ${selectedSeries.name}", "Настроить серию", true))
        }
        if (selectedStation == null) {
            add(Triple("Станция не выбрана — нормы интервалов недоступны", null, false))
        } else {
            val s = selectedStation!!
            val ok = s.appearanceToStartMin != null && s.endToBarrierMin != null &&
                s.barrierToStartMin != null && s.endToWorkEndMin != null
            if (!ok) add(Triple("Нет норм для станции ${s.name}", "Настроить станцию", false))
        }
    }

    // Can save norms
    val canSaveSeriesNorm = run {
        if (selectedSeries == null || startTime == null || endTime == null) false
        else {
            val dur = ((endTime!! - startTime!!) / 60_000).toInt()
            val saved = if (kind == "acceptance") selectedSeries.acceptanceDurationMin
            else selectedSeries.deliveryDurationMin
            saved == null || saved != dur
        }
    }
    val canSaveStationNorm = run {
        if (selectedStation == null) false
        else if (kind == "acceptance") {
            if (routeStartWork == null || startTime == null || endTime == null) false
            else {
                val st = selectedStation!!
                val newAppear = ((startTime!! - routeStartWork) / 60_000).toInt()
                val newBarrier = if (barrierOut != null) ((barrierOut!! - endTime!!) / 60_000).toInt() else null
                newAppear != st.appearanceToStartMin || (newBarrier != null && newBarrier != st.endToBarrierMin)
            }
        } else {
            if (barrierIn == null || startTime == null || endTime == null || workEnd == null) false
            else {
                val st = selectedStation!!
                val newBarrier = ((startTime!! - barrierIn!!) / 60_000).toInt()
                val newWorkEnd = ((workEnd!! - endTime!!) / 60_000).toInt()
                newBarrier != st.barrierToStartMin || newWorkEnd != st.endToWorkEndMin
            }
        }
    }

    // Time pickers
    if (showStartPicker) {
        AppDateTimePicker(
            title = if (kind == "acceptance") "Начало приёмки" else "Начало сдачи",
            onDateTimeSelected = { startTime = it; showStartPicker = false },
            onDismiss = { showStartPicker = false },
            startDateTime = startTime ?: Calendar.getInstance().timeInMillis,
            timeZoneStr = timeZoneText,
            recentTimes = emptyList(),
            onRecentTimeSaved = {}
        )
    }
    if (showEndPicker) {
        AppDateTimePicker(
            title = if (kind == "acceptance") "Окончание приёмки" else "Окончание сдачи",
            onDateTimeSelected = { endTime = it; showEndPicker = false },
            onDismiss = { showEndPicker = false },
            startDateTime = endTime ?: Calendar.getInstance().timeInMillis,
            timeZoneStr = timeZoneText,
            recentTimes = emptyList(),
            onRecentTimeSaved = {}
        )
    }
    if (showBarrierOutPicker) {
        AppDateTimePicker(
            title = "Выход на КП",
            onDateTimeSelected = { barrierOut = it; showBarrierOutPicker = false },
            onDismiss = { showBarrierOutPicker = false },
            startDateTime = barrierOut ?: Calendar.getInstance().timeInMillis,
            timeZoneStr = timeZoneText,
            recentTimes = emptyList(),
            onRecentTimeSaved = {}
        )
    }
    if (showBarrierInPicker) {
        AppDateTimePicker(
            title = "Заход на КП",
            onDateTimeSelected = { barrierIn = it; showBarrierInPicker = false },
            onDismiss = { showBarrierInPicker = false },
            startDateTime = barrierIn ?: Calendar.getInstance().timeInMillis,
            timeZoneStr = timeZoneText,
            recentTimes = emptyList(),
            onRecentTimeSaved = {}
        )
    }
    if (showWorkEndPicker) {
        AppDateTimePicker(
            title = "Окончание работы",
            onDateTimeSelected = { workEnd = it; showWorkEndPicker = false },
            onDismiss = { showWorkEndPicker = false },
            startDateTime = workEnd ?: Calendar.getInstance().timeInMillis,
            timeZoneStr = timeZoneText,
            recentTimes = emptyList(),
            onRecentTimeSaved = {}
        )
    }

    if (showStationPicker) {
        StationPickerSheet(
            onSelect = { station ->
                selectedStation = station
                showStationPicker = false
            },
            onClose = { showStationPicker = false },
            onNavigateToSettings = {
                showStationPicker = false
                onClose()
                onNavigateToStationSettings()
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onClose) {
                    Text("Отмена", color = MaterialTheme.colorScheme.tertiary)
                }
                Text(
                    text = if (kind == "acceptance") "Приёмка" else "Сдача",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = {
                    scope.launch {
                        sheetState.hide()
                        onSave(TimeSheetResult(startTime, endTime, barrierOut, barrierIn, workEnd, selectedStation?.stationId))
                    }
                }) {
                    Text("Готово", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold)
                }
            }

            // Context card: series + station + "По нормам" button
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(2.dp, Shapes.medium)
                    .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
            ) {
                // Series (read-only)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "СЕРИЯ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Text(
                            text = seriesName ?: "Не задана",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (seriesName != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(start = 44.dp))

                // Station (tappable)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStationPicker = true }
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "СТАНЦИЯ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Text(
                            text = selectedStation?.name ?: "Выберите станцию",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selectedStation != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    }
                    Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                }

                // "По нормам" button
                Button(
                    onClick = { applyNorms() },
                    enabled = canApplyNorms(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                ) {
                    Text("⚡ По нормам", fontWeight = FontWeight.SemiBold)
                }
            }

            // Warnings
            if (warnings.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    warnings.forEach { (text, actionLabel, isSeries) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFF9500).copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF9500),
                                modifier = Modifier.weight(1f)
                            )
                            if (actionLabel != null) {
                                TextButton(
                                    onClick = {
                                        onClose()
                                        if (isSeries) onNavigateToSeriesSettings() else onNavigateToStationSettings()
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                ) {
                                    Text(actionLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }
                    }
                }
            }

            // Time rows
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(2.dp, Shapes.medium)
                    .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
            ) {
                if (kind == "acceptance") {
                    // Явка (locked)
                    TimeRow(
                        label = "Явка",
                        tag = "из маршрута",
                        delta = null,
                        timeText = fmtTime(routeStartWork),
                        highlighted = false,
                        locked = true,
                        onClick = null
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    // Начало приёмки
                    TimeRow(
                        label = "Начало приёмки",
                        tag = null,
                        delta = deltaStr(routeStartWork, startTime),
                        timeText = fmtTime(startTime),
                        highlighted = false,
                        locked = false,
                        onClick = { showStartPicker = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    // Окончание приёмки
                    TimeRow(
                        label = "Окончание приёмки",
                        tag = null,
                        delta = deltaStr(startTime, endTime),
                        timeText = fmtTime(endTime),
                        highlighted = false,
                        locked = false,
                        onClick = { showEndPicker = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    // Выход на КП (highlighted)
                    TimeRow(
                        label = "Выход на КП",
                        tag = null,
                        delta = deltaStr(endTime, barrierOut),
                        timeText = fmtTime(barrierOut),
                        highlighted = true,
                        locked = false,
                        onClick = { showBarrierOutPicker = true }
                    )
                } else {
                    // Заход на КП (highlighted, user input)
                    TimeRow(
                        label = "Заход на КП",
                        tag = null,
                        delta = null,
                        timeText = fmtTime(barrierIn),
                        highlighted = true,
                        locked = false,
                        onClick = { showBarrierInPicker = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    // Начало сдачи
                    TimeRow(
                        label = "Начало сдачи",
                        tag = null,
                        delta = deltaStr(barrierIn, startTime),
                        timeText = fmtTime(startTime),
                        highlighted = false,
                        locked = false,
                        onClick = { showStartPicker = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    // Окончание сдачи
                    TimeRow(
                        label = "Окончание сдачи",
                        tag = null,
                        delta = deltaStr(startTime, endTime),
                        timeText = fmtTime(endTime),
                        highlighted = false,
                        locked = false,
                        onClick = { showEndPicker = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    // Окончание работы
                    TimeRow(
                        label = "Окончание работы",
                        tag = null,
                        delta = deltaStr(endTime, workEnd),
                        timeText = fmtTime(workEnd),
                        highlighted = false,
                        locked = false,
                        onClick = { showWorkEndPicker = true }
                    )
                }
            }

            // Save norm buttons
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SaveNormButton(
                    label = "🚂 Сохранить норму серии ${seriesName ?: "…"}",
                    enabled = canSaveSeriesNorm,
                    onClick = {
                        if (selectedSeries != null && startTime != null && endTime != null) {
                            val dur = ((endTime!! - startTime!!) / 60_000).toInt()
                            val updated = if (kind == "acceptance")
                                selectedSeries.copy(acceptanceDurationMin = dur)
                            else
                                selectedSeries.copy(deliveryDurationMin = dur)
                            // Save via repository — rebuild list
                        }
                    }
                )
                SaveNormButton(
                    label = "📍 Сохранить норму станции ${selectedStation?.name ?: "…"}",
                    enabled = canSaveStationNorm,
                    onClick = {
                        // Save station norm logic handled by VM after sheet closes
                    }
                )
            }
        }
    }
}

@Composable
private fun TimeRow(
    label: String,
    tag: String?,
    delta: String?,
    timeText: String,
    highlighted: Boolean,
    locked: Boolean,
    onClick: (() -> Unit)?,
) {
    val bgColor = if (highlighted) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f) else Color.Transparent
    val textColor = if (timeText == "—") MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    else if (highlighted) MaterialTheme.colorScheme.tertiary
    else if (locked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    else MaterialTheme.colorScheme.tertiary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .then(if (onClick != null && !locked) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (locked) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary
                )
                if (tag != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            if (delta != null) {
                Text(
                    text = delta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
        }
        // Time button
        Text(
            text = timeText,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = textColor
        )
    }
}

@Composable
private fun SaveNormButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (enabled) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp)
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    }
}
