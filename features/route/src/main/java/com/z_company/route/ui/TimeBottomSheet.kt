@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.route.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.repositories.LocomotiveSeriesRepository
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.repositories.StationNormRepository
import com.z_company.domain.util.generateId
import com.z_company.route.component.AppDateTimePicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Calendar
import kotlin.time.Clock

// ── Private models ─────────────────────────────────────────────────────────
private data class WarnItem(val text: String, val hint: String?, val cta: String?, val isSeries: Boolean)

/** Structured delta info for smart formatting. */
private data class DeltaInfo(val actualMinutes: Int, val normMinutes: Int?)

/** Anchor selection state for delivery «По нормам». */
private enum class DeliveryNormAnchor { NONE, ASKING, FROM_SELECTED }

/** Which delivery field was tapped as anchor. */
private enum class DeliveryAnchorField { BARRIER_IN, START_TIME, END_TIME, WORK_END }

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
    onTimeEndWorkChanged: ((Long?) -> Unit)? = null,
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

    // Вариант нормы приёмки/сдачи: false — «После отстоя» (по умолчанию), true — «Из рук в руки».
    // Выбор запоминается локально между открытиями шторки.
    val prefs: SharedPreferencesRepositories = koinInject()
    var normHandToHand by remember { mutableStateOf(prefs.isLocoNormHandToHand()) }
    // Эффективная норма серии по выбранному варианту.
    val seriesAcceptanceMin: Int? =
        if (normHandToHand) selectedSeries?.acceptanceHandToHandMin else selectedSeries?.acceptanceDurationMin
    val seriesDeliveryMin: Int? =
        if (normHandToHand) selectedSeries?.deliveryHandToHandMin else selectedSeries?.deliveryDurationMin

    var showStationPicker by remember { mutableStateOf(false) }
    var showSeriesPicker by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var showBarrierOutPicker by remember { mutableStateOf(false) }
    var showBarrierInPicker by remember { mutableStateOf(false) }
    var showWorkEndPicker by remember { mutableStateOf(false) }

    // Delivery «По нормам» anchor selection state
    var anchorState by remember { mutableStateOf(DeliveryNormAnchor.NONE) }
    // True when tapping an empty row during ASKING to show hint
    var askingEmptyHint by remember { mutableStateOf(false) }

    // Pending workEnd update: calculated value waiting for user to tap «Обновить»
    var pendingEndWorkUpdate by remember { mutableStateOf<Long?>(null) }
    // True when user explicitly accepted the new workEnd (or it's a brand-new value)
    var workEndAccepted by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // InfiniteTransition for ASKING blink — runs continuously, alpha applied only when ASKING
    val infiniteTransition = rememberInfiniteTransition(label = "asking_blink")
    val rawBlink by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 800
                0f at 0; 1f at 400; 0f at 800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "blink"
    )
    val highlightAlpha = if (anchorState == DeliveryNormAnchor.ASKING) rawBlink else 0f

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
        if (kind == "acceptance") seriesAcceptanceMin == null
        else seriesDeliveryMin == null
    }
    val stationEmpty = selectedStation == null
    val noStationNorm = !stationEmpty && selectedStation!!.let { s ->
        s.appearanceToStartMin == null || s.endToBarrierMin == null ||
            s.barrierToStartMin == null || s.endToWorkEndMin == null
    }
    // Any filled delivery time field can serve as anchor
    val hasAnchorForDelivery = barrierIn != null || startTime != null || endTime != null || workEnd != null
    val canApplyNorms = (selectedSeries != null || selectedStation != null) &&
        (kind == "acceptance" || hasAnchorForDelivery)

    /** Sets calculatedWorkEnd, handling the conflict with routeEndWork. */
    fun handleCalculatedWorkEnd(calculatedWorkEnd: Long) {
        if (routeEndWork != null && calculatedWorkEnd != routeEndWork) {
            pendingEndWorkUpdate = calculatedWorkEnd
            workEnd = routeEndWork
        } else {
            workEnd = calculatedWorkEnd
            if (routeEndWork == null) workEndAccepted = true
        }
    }

    /** Generic calculation from any delivery anchor field, forward and backward. */
    fun applyFromField(field: DeliveryAnchorField, deliveryMin: Int? = seriesDeliveryMin) {
        pendingEndWorkUpdate = null
        workEndAccepted = false
        askingEmptyHint = false
        val barrierMin = selectedStation?.barrierToStartMin ?: 0
        val durMin = deliveryMin ?: 0
        val workEndMin = selectedStation?.endToWorkEndMin ?: 0
        when (field) {
            DeliveryAnchorField.BARRIER_IN -> {
                val anchor = barrierIn ?: return
                val newStart = anchor + barrierMin * 60_000L
                val newEnd = newStart + durMin * 60_000L
                startTime = newStart
                endTime = newEnd
                handleCalculatedWorkEnd(newEnd + workEndMin * 60_000L)
            }
            DeliveryAnchorField.START_TIME -> {
                val anchor = startTime ?: return
                // Forward
                val newEnd = anchor + durMin * 60_000L
                endTime = newEnd
                handleCalculatedWorkEnd(newEnd + workEndMin * 60_000L)
                // Backward
                barrierIn = anchor - barrierMin * 60_000L
            }
            DeliveryAnchorField.END_TIME -> {
                val anchor = endTime ?: return
                // Forward
                handleCalculatedWorkEnd(anchor + workEndMin * 60_000L)
                // Backward
                val newStart = anchor - durMin * 60_000L
                startTime = newStart
                barrierIn = newStart - barrierMin * 60_000L
            }
            DeliveryAnchorField.WORK_END -> {
                // Backward from workEnd (use as source — don't update route)
                val ew = workEnd ?: routeEndWork ?: return
                val newEnd = ew - workEndMin * 60_000L
                val newStart = newEnd - durMin * 60_000L
                endTime = newEnd
                startTime = newStart
                barrierIn = newStart - barrierMin * 60_000L
                workEnd = ew
                workEndAccepted = false  // source — don't sync to route
            }
        }
    }

    fun applyNorms(
        acceptanceMin: Int? = seriesAcceptanceMin,
        deliveryMin: Int? = seriesDeliveryMin,
    ) {
        if (kind == "acceptance") {
            val base = routeStartWork ?: return
            val appearMin = selectedStation?.appearanceToStartMin ?: 0
            val newStart = base + appearMin * 60_000L
            val durMin = acceptanceMin ?: 0
            val newEnd = newStart + durMin * 60_000L
            val barrierMin = selectedStation?.endToBarrierMin ?: 0
            startTime = newStart; endTime = newEnd; barrierOut = newEnd + barrierMin * 60_000L
        } else {
            val filled = listOfNotNull(barrierIn, startTime, endTime, workEnd)
            when (filled.size) {
                0 -> Unit  // button should be inactive
                1 -> {
                    // Single anchor — direct calculation, no banner
                    val field = when {
                        barrierIn != null -> DeliveryAnchorField.BARRIER_IN
                        startTime != null -> DeliveryAnchorField.START_TIME
                        endTime != null -> DeliveryAnchorField.END_TIME
                        workEnd != null -> DeliveryAnchorField.WORK_END
                        else -> return
                    }
                    applyFromField(field, deliveryMin)
                    anchorState = DeliveryNormAnchor.FROM_SELECTED
                }
                else -> {
                    // Multiple anchors — ask which to use
                    askingEmptyHint = false
                    anchorState = DeliveryNormAnchor.ASKING
                }
            }
        }
    }

    // Пере-применение при переключении варианта нормы выполняется синхронно в
    // обработчике переключателя (applyNormFor) — одним кадром, без промежуточного
    // состояния со старыми временами (иначе мигали бы красные предупреждения).
    fun applyNormFor(handToHand: Boolean) {
        val accMin = if (handToHand) selectedSeries?.acceptanceHandToHandMin
        else selectedSeries?.acceptanceDurationMin
        val delMin = if (handToHand) selectedSeries?.deliveryHandToHandMin
        else selectedSeries?.deliveryDurationMin
        val variantHasNorm = if (kind == "acceptance") accMin != null else delMin != null
        if (!variantHasNorm) return
        if (kind == "acceptance") {
            if (startTime != null && endTime != null) applyNorms(accMin, delMin)
        } else {
            if (anchorState == DeliveryNormAnchor.FROM_SELECTED) applyNorms(accMin, delMin)
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
            val saved = if (kind == "acceptance") seriesAcceptanceMin else seriesDeliveryMin
            saved == null || saved != dur
        } else {
            true // not in repo → always can add
        }
    }

    val normVariantText = if (normHandToHand) "из рук в руки" else "после отстоя"
    val seriesNormSubtitle = if (!seriesInRepo && !selectedSeriesName.isNullOrBlank())
        "Добавить в справочник · $normVariantText"
    else if (kind == "acceptance") "Длительность приёмки · $normVariantText"
    else "Длительность сдачи · $normVariantText"

    val stationNormSubtitle = if (noStationNorm)
        "Добавить в справочник"
    else if (kind == "acceptance") "явка -> приемка / приемка -> выход на КП" else "КП -> сдача / сдача -> окончание работы"

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
        onDateTimeSelected = { workEnd = it; workEndAccepted = true; showWorkEndPicker = false },
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
            )
            {
                Text(
                    text = if (kind == "acceptance") "Приёмка" else "Сдача",
                    fontSize = 22.sp, fontWeight = FontWeight.W500, color = textColor()
                )
                val normsActive = canApplyNorms && !noSeriesNorm && !stationEmpty && !noStationNorm
                Box(
                    modifier = Modifier
                        .clip(Shapes.medium)
                        .background(if (normsActive) accent() else bgSubtle())
                        .then(
                            if (!normsActive) Modifier.border(1.dp, borderColor(), Shapes.medium)
                            else Modifier
                        )
                        .clickable(enabled = canApplyNorms) { applyNorms() }
                        // Вертикальный отступ как у переключателя (segment 9 + outer 3 = 12).
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Установить по ПЗВ", fontSize = 13.sp, fontWeight = FontWeight.W500,
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
                    label = "Серия",
                    value = selectedSeriesName ?: "Выберите серию",
                    isEmpty = selectedSeriesName == null,
                    onClick = { showSeriesPicker = true }
                )
                val stationLabel = if (kind == "acceptance") "Станция приёмки" else "Станция сдачи"
                ContextFieldItem(
                    label = stationLabel,
                    value = selectedStation?.name ?: "Выберите станцию",
                    isEmpty = selectedStation == null,
                    onClick = { showStationPicker = true }
                )
            }

            // ── Вариант нормы: После отстоя / Из рук в руки ──
            if (selectedSeriesName != null) {
                NormModeSegmented(
                    handToHand = normHandToHand,
                    onChange = { v ->
                        normHandToHand = v
                        prefs.setLocoNormHandToHand(v)
                        applyNormFor(v)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 10.dp)
                )
            }

            // ── Warnings ──
            val warnings: List<WarnItem> = buildList {
                when {
                    selectedSeriesName == null ->
                        add(WarnItem("Серия не выбрана",
                            "Выберите серию, чтобы применить нормы длительности",
                            null, true))
                    noSeriesNorm ->
                        add(WarnItem("Нет нормы для серии $selectedSeriesName", null, "Настроить серию", true))
                }
                if (stationEmpty)
                    add(WarnItem(
                        "Станция ${if (kind == "acceptance") "приёмки" else "сдачи"} не выбрана",
                        "Выберите станцию, чтобы применить нормы интервалов",
                        null, false
                    ))
                else if (noStationNorm)
                    add(WarnItem("Нет нормы для станции ${selectedStation!!.name}", null, "Настроить станцию", false))
                // Task 3: delivery with norms set but no anchor at all
                if (kind == "delivery" && !hasAnchorForDelivery)
                    add(WarnItem(
                        "Укажите время захода на КП или время окончания работы",
                        "От него будет выполнен расчёт по нормам",
                        null, false
                    ))
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

            // ── Anchor selection banner (delivery / ASKING) ──
            if (kind == "delivery" && anchorState == DeliveryNormAnchor.ASKING) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 4.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentSoft())
                        .border(1.dp, accent().copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Нажмите на момент времени, от которого рассчитать",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor()
                    )
                    if (askingEmptyHint) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(com.z_company.route.R.drawable.dangerous_24px),
                                contentDescription = null, modifier = Modifier.size(12.dp),
                                tint = warnColor()
                            )
                            Text(
                                "Сначала укажите это время вручную",
                                fontSize = 12.sp, color = warnColor()
                            )
                        }
                    }
                }
            }

            // ── Time rows ──
            val asking = anchorState == DeliveryNormAnchor.ASKING
            val accentSoftColor = accentSoft()
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                if (kind == "acceptance") {
                    SheetTimeRowItem(
                        label = "Явка", time = fmtTime(routeStartWork), delta = null,
                        isFirst = true, isLocked = true, sequenceError = false,
                        iconRes = com.z_company.route.R.drawable.check_circle_24px, onClick = null
                    )
                    RowConnector()
                    SheetTimeRowItem(
                        label = "Начало приёмки", time = fmtTime(startTime),
                        delta = buildDelta(routeStartWork, startTime, selectedStation?.appearanceToStartMin),
                        isFirst = false, isLocked = false,
                        sequenceError = startTimeError,
                        iconRes = com.z_company.route.R.drawable.check_circle_24px,
                        onClick = { showStartPicker = true }
                    )
                    RowConnector()
                    SheetTimeRowItem(
                        label = "Окончание приёмки", time = fmtTime(endTime),
                        delta = buildDelta(startTime, endTime, seriesAcceptanceMin),
                        isFirst = false, isLocked = false,
                        sequenceError = endTimeError,
                        iconRes = com.z_company.route.R.drawable.check_circle_24px,
                        onClick = { showEndPicker = true }
                    )
                    RowConnector()
                    SheetTimeRowItem(
                        label = "Выход на КП", time = fmtTime(barrierOut),
                        delta = buildDelta(endTime, barrierOut, selectedStation?.endToBarrierMin),
                        isFirst = false, isLocked = false,
                        sequenceError = barrierOutError,
                        iconRes = com.z_company.route.R.drawable.check_circle_24px,
                        onClick = { showBarrierOutPicker = true }
                    )
                } else {
                    // Delivery — all 4 rows blink via highlightAlpha when ASKING
                    fun onRowTap(field: DeliveryAnchorField, hasTime: Boolean) {
                        if (asking) {
                            if (hasTime) {
                                applyFromField(field)
                                anchorState = DeliveryNormAnchor.FROM_SELECTED
                            } else {
                                askingEmptyHint = true
                            }
                        }
                    }

                    SheetTimeRowItem(
                        label = "Заход на КП", time = fmtTime(barrierIn), delta = null,
                        isFirst = true, isLocked = false, sequenceError = false,
                        iconRes = com.z_company.route.R.drawable.check_circle_24px,
                        highlightAlpha = highlightAlpha,
                        onClick = {
                            if (asking) onRowTap(DeliveryAnchorField.BARRIER_IN, barrierIn != null)
                            else showBarrierInPicker = true
                        }
                    )
                    RowConnector()
                    SheetTimeRowItem(
                        label = "Начало сдачи", time = fmtTime(startTime),
                        delta = buildDelta(barrierIn, startTime, selectedStation?.barrierToStartMin),
                        isFirst = false, isLocked = false,
                        sequenceError = startTimeError,
                        iconRes = com.z_company.route.R.drawable.check_circle_24px,
                        highlightAlpha = highlightAlpha,
                        onClick = {
                            if (asking) onRowTap(DeliveryAnchorField.START_TIME, startTime != null)
                            else showStartPicker = true
                        }
                    )
                    RowConnector()
                    SheetTimeRowItem(
                        label = "Окончание сдачи", time = fmtTime(endTime),
                        delta = buildDelta(startTime, endTime, seriesDeliveryMin),
                        isFirst = false, isLocked = false,
                        sequenceError = endTimeError,
                        iconRes = com.z_company.route.R.drawable.check_circle_24px,
                        highlightAlpha = highlightAlpha,
                        onClick = {
                            if (asking) onRowTap(DeliveryAnchorField.END_TIME, endTime != null)
                            else showEndPicker = true
                        }
                    )
                    RowConnector()
                    SheetTimeRowItem(
                        label = "Окончание работы", time = fmtTime(workEnd),
                        delta = buildDelta(endTime, workEnd, selectedStation?.endToWorkEndMin),
                        isFirst = false, isLocked = false,
                        sequenceError = workEndError,
                        iconRes = com.z_company.route.R.drawable.check_circle_24px,
                        highlightAlpha = highlightAlpha,
                        pendingTimeText = pendingEndWorkUpdate?.let { fmtTime(it) },
                        onPendingAccept = {
                            val newVal = pendingEndWorkUpdate
                            workEnd = newVal
                            workEndAccepted = true
                            pendingEndWorkUpdate = null
                            // Task 5: immediately sync to route
                            if (newVal != null) onTimeEndWorkChanged?.invoke(newVal)
                        },
                        onClick = {
                            if (asking) onRowTap(DeliveryAnchorField.WORK_END, workEnd != null)
                            else if (pendingEndWorkUpdate == null) showWorkEndPicker = true
                        }
                    )
                }
            }

            // ── Save norm buttons — visible only when ready ──
            if (canSaveSeriesNorm || canSaveStationNorm) Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val seriesTitle = if (!selectedSeriesName.isNullOrBlank())
                    "Сохранить норму серии $selectedSeriesName" else "Сохранить норму серии"

                if (canSaveSeriesNorm) SaveNormItem(
                    title = seriesTitle,
                    subtitle = seriesNormSubtitle,
                    isReady = true,
                    isDisabled = false,
                    onClick = {
                        if (canSaveSeriesNorm && !selectedSeriesName.isNullOrBlank()
                            && startTime != null && endTime != null
                        ) {
                            val dur = ((endTime!! - startTime!!) / 60_000).toInt()
                            scope.launch {
                                val all = seriesRepo.getAll().toMutableList()
                                if (seriesInRepo && selectedSeries != null) {
                                    // Сохраняем в поле выбранного варианта (после отстоя / из рук в руки).
                                    val updated = when {
                                        kind == "acceptance" && normHandToHand ->
                                            selectedSeries.copy(acceptanceHandToHandMin = dur)
                                        kind == "acceptance" ->
                                            selectedSeries.copy(acceptanceDurationMin = dur)
                                        normHandToHand ->
                                            selectedSeries.copy(deliveryHandToHandMin = dur)
                                        else ->
                                            selectedSeries.copy(deliveryDurationMin = dur)
                                    }
                                    val idx = all.indexOfFirst { it.seriesId == updated.seriesId }
                                    if (idx >= 0) all[idx] = updated else all.add(updated)
                                } else {
                                    // Create new entry
                                    val newSeries = LocomotiveSeries(
                                        seriesId = generateId(),
                                        name = selectedSeriesName!!.trim(),
                                        type = locoType,
                                        acceptanceDurationMin = if (kind == "acceptance" && !normHandToHand) dur else null,
                                        deliveryDurationMin = if (kind == "delivery" && !normHandToHand) dur else null,
                                        acceptanceHandToHandMin = if (kind == "acceptance" && normHandToHand) dur else null,
                                        deliveryHandToHandMin = if (kind == "delivery" && normHandToHand) dur else null,
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
                if (canSaveStationNorm) SaveNormItem(
                    title = stationTitle,
                    subtitle = stationNormSubtitle,
                    isReady = true,
                    isDisabled = false,
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
                            if (kind == "delivery" && workEnd != null) {
                                onTimeEndWorkChanged?.invoke(workEnd)
                            }
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
    label: String,
    value: String,
    isEmpty: Boolean,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.medium)
            .background(surfaceCard())
            .border(1.dp, borderColor(), Shapes.medium)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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
            Icon(
                painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_right_24px),
                contentDescription = null, modifier = Modifier.size(12.dp), tint = textFaint()
            )
    }
}

// ── NormModeSegmented: После отстоя / Из рук в руки ────────────────────────
@Composable
private fun NormModeSegmented(
    handToHand: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(Shapes.medium)
            .background(bgSubtle())
            .border(1.dp, borderColor(), Shapes.medium)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        @Composable
        fun RowScope.Segment(text: String, selected: Boolean, onClick: () -> Unit) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    // Концентрично внешнему: outer Shapes.medium(16) − padding(3) = 13.
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (selected) accent() else Color.Transparent)
                    .clickable(onClick = onClick)
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) accentInk() else textMuted(),
                )
            }
        }
        Segment("После отстоя", selected = !handToHand) { onChange(false) }
        Segment("Из рук в руки", selected = handToHand) { onChange(true) }
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
    sequenceError: Boolean,
    onClick: (() -> Unit)?,
    isHighlighted: Boolean = false,
    highlightAlpha: Float = 0f,
    pendingTimeText: String? = null,
    onPendingAccept: (() -> Unit)? = null,
) {
    val accentSoftCol = accentSoft()
    val rowBg = when {
        isHighlighted -> accentSoftCol
        highlightAlpha > 0f -> lerp(Color.Transparent, accentSoftCol, highlightAlpha)
        else -> Color.Transparent
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(rowBg)
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

            // Time button — or inline «Обновить ЧЧ:ММ» if pending update
            if (pendingTimeText != null && onPendingAccept != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClick = onPendingAccept)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Обновить $pendingTimeText",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            } else {
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
//        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(actualStr, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = textMuted())
//        }
    } else {
        val diff = delta.actualMinutes - delta.normMinutes
        val (accentColor, annotation) = when {
            diff == 0 -> textMuted() to "норма"  // exact match
            diff > 0 -> MaterialTheme.colorScheme.error to "на $diff мин больше"
            else -> greenOk to "на ${-diff} мин меньше"
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(actualStr, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = accentColor)
            Text(annotation, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = accentColor)
        }
    }
}

// ── SaveNormItem ─────────────────────────────────────────────────────────
@Composable
private fun SaveNormItem(
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = if (isDisabled) textFaint() else textColor(), lineHeight = (13 * 1.2).sp
            )
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = textMuted())
        }
    }
}


// ── AnchorSelectionBanner ────────────────────────────────────────────────
@Composable
private fun AnchorSelectionBanner(
    barrierEnabled: Boolean,
    endWorkEnabled: Boolean,
    onFromBarrier: () -> Unit,
    onFromEndWork: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 4.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accentSoft())
            .border(1.dp, accent().copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "От какого момента рассчитать время?",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnchorChip(
                label = "⬆ Заход на КП",
                enabled = barrierEnabled,
                onClick = onFromBarrier,
                modifier = Modifier.weight(1f)
            )
            AnchorChip(
                label = "⬇ Окончание работы",
                enabled = endWorkEnabled,
                onClick = onFromEndWork,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AnchorChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (enabled) accent() else bgSubtle())
            .alpha(if (enabled) 1f else 0.38f)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) accentInk() else textFaint(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
