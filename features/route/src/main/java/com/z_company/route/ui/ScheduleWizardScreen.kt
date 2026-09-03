package com.z_company.route.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import com.z_company.core.ui.snackbar.ISnackbarManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.domain.entities.SchedulePattern
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.viewmodel.CUSTOM_PATTERN_ID
import com.z_company.route.viewmodel.ShiftKind
import com.z_company.route.viewmodel.WizardUiState
import kotlin.math.cos
import kotlin.math.sin

// ── Цвета типов смен (по референсу schedule-wizard.jsx: день — светлая плашка,
// ночь — тёмная, выходной — приглушённая) ──────────────────────────
private data class ShiftPalette(val plate: Color, val ink: Color, val border: Color)

@Composable
private fun shiftPalette(kind: ShiftKind): ShiftPalette {
    val cs = MaterialTheme.colorScheme
    // День — светлая плашка, ночь — тёмная, выходной — нейтральная. В тёмной теме
    // слоты surface/primary меняются местами по яркости, а простой светлый/тёмный
    // (pure white / surface) сливается с фоном сетки и слепит, поэтому для тёмной
    // темы задаём три отдельных тона, разнесённых по яркости и отделённых от фона.
    val isDark = cs.surface.luminance() < 0.5f
    return if (isDark) {
        when (kind) {
            // День — приглушённо-светлая плашка (не чистый белый) с тёмным глифом.
            ShiftKind.DAY -> ShiftPalette(plate = Color(0xFFCED1D6), ink = Color(0xFF16171A), border = Color.Transparent)
            // Ночь — глубокая тёмная плашка (темнее фона сетки) со светлым глифом и обводкой.
            ShiftKind.NIGHT -> ShiftPalette(plate = Color(0xFF141517), ink = Color(0xFFCED1D6), border = cs.outline)
            // Выходной — нейтральная плашка чуть светлее фона.
            ShiftKind.OFF -> ShiftPalette(plate = cs.surfaceVariant, ink = cs.onSurfaceVariant, border = Color.Transparent)
        }
    } else {
        when (kind) {
            // Дневная — светлая плашка (surface) с тёмным глифом, тонкая обводка.
            ShiftKind.DAY -> ShiftPalette(plate = cs.surface, ink = cs.primary, border = cs.outline)
            // Ночная — мягкая тёмная плашка (не почти-чёрный cs.primary) со светлым глифом.
            ShiftKind.NIGHT -> ShiftPalette(plate = Color(0xFF363B44), ink = cs.surface, border = Color.Transparent)
            // Выходной — приглушённая плашка.
            ShiftKind.OFF -> ShiftPalette(plate = cs.surfaceBright, ink = cs.onSurfaceVariant, border = Color.Transparent)
        }
    }
}

private fun shiftShort(k: ShiftKind): String = when (k) {
    ShiftKind.DAY -> "Дн"
    ShiftKind.NIGHT -> "Ноч"
    ShiftKind.OFF -> "Вых"
}

private fun shiftLabel(k: ShiftKind): String = when (k) {
    ShiftKind.DAY -> "День"
    ShiftKind.NIGHT -> "Ночь"
    ShiftKind.OFF -> "Выходной"
}

/** Ограниченная плотность для подписей типов смен: узкие сегменты фиксированной
 *  ширины — при крупном шрифте «Выходной» иначе рвётся посреди слова. */
@Composable
private fun shiftLabelDensity(max: Float = 1.15f): Density {
    val d = LocalDensity.current
    return if (d.fontScale > max) Density(d.density, max) else d
}

/** Глиф типа смены: солнце (день), месяц (ночь), тире (выходной). Система 24×24. */
@Composable
private fun ShiftGlyph(kind: ShiftKind, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val sw = 2f * s
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(color, p(x1, y1), p(x2, y2), strokeWidth = sw, cap = StrokeCap.Round)
        when (kind) {
            ShiftKind.DAY -> {
                drawCircle(color, radius = 4.5f * s, center = p(12f, 12f), style = Stroke(sw))
                for (k in 0..7) {
                    val a = Math.toRadians(k * 45.0)
                    val cx = cos(a).toFloat(); val cy = sin(a).toFloat()
                    line(12f + cx * 7f, 12f + cy * 7f, 12f + cx * 8.7f, 12f + cy * 8.7f)
                }
            }
            ShiftKind.NIGHT -> {
                // Месяц: большая дуга минус смещённый круг (рисуем полумесяц штрихом).
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(20f * s, 13.5f * s)
                    // Дуга внешнего контура
                    cubicTo(19f * s, 17.5f * s, 15f * s, 20.5f * s, 11f * s, 19.5f * s)
                    cubicTo(6.5f * s, 18.3f * s, 4f * s, 13.5f * s, 5.2f * s, 9f * s)
                    cubicTo(6.1f * s, 5.6f * s, 9f * s, 3.3f * s, 12.5f * s, 3.2f * s)
                    cubicTo(9.5f * s, 6f * s, 9.2f * s, 10.8f * s, 12f * s, 13.6f * s)
                    cubicTo(14.2f * s, 15.8f * s, 17.4f * s, 16f * s, 20f * s, 13.5f * s)
                    close()
                }
                drawPath(path, color, style = Stroke(sw))
            }
            ShiftKind.OFF -> line(6f, 12f, 18f, 12f)
        }
    }
}

/**
 * Мастер «Заполнить месяц» (2 шага). Соответствует дизайну schedule-wizard.jsx.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleWizardScreen(
    state: WizardUiState,
    onBack: () -> Unit,
    onSelectPattern: (String) -> Unit,
    onDeletePattern: (String) -> Unit,
    onSetDayStart: (Int, Int) -> Unit,
    onSetDayEnd: (Int, Int) -> Unit,
    onSetNightStart: (Int, Int) -> Unit,
    onSetNightEnd: (Int, Int) -> Unit,
    onSetFirstDay: (Int) -> Unit,
    onSetExtendToNextMonth: (Boolean) -> Unit,
    onShiftMonth: (Int) -> Unit,
    onContinuePrevious: () -> Unit,
    onDeclineContinuePrevious: () -> Unit,
    onGoToStep: (Int) -> Unit,
    onApply: () -> Unit,
    onOpenTypePicker: (Int) -> Unit,
    onCloseTypePicker: () -> Unit,
    onSetCycleDayType: (Int, ShiftKind) -> Unit,
    onAddCycleDay: () -> Unit,
    onRemoveCycleDay: (Int) -> Unit,
    onDismissSubscriptionLimit: () -> Unit = {},
    onPurchasesClick: () -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme

    if (state.isSaving) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Создаём маршруты") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text(
                        "Это может занять несколько секунд",
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            },
        )
    }
    var timePickerFor by remember { mutableStateOf<String?>(null) }
    var patternToDelete by remember { mutableStateOf<SchedulePattern?>(null) }

    // Свой SnackbarHost — иначе сообщения мастера («создано черновиков»,
    // ошибки) всплывали только после возврата на Главный экран.
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarManager: ISnackbarManager = koinInject()
    val snackbarScope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        snackbarManager.events
            .flowWithLifecycle(lifecycle)
            .collect { event ->
                val result = snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = event.actionLabel,
                    duration = event.duration,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    event.onAction?.let { action ->
                        snackbarScope.launch { runCatching { action() } }
                    }
                }
            }
    }

    state.subscriptionLimit?.let { limit ->
        SubscriptionLimitDialog(
            limit = limit,
            onDismiss = onDismissSubscriptionLimit,
            onPurchases = { onDismissSubscriptionLimit(); onPurchasesClick() },
        )
    }

    Scaffold(
        containerColor = cs.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.background(cs.background)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(cs.surfaceBright)
                            .clickable { onBack() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("‹", fontSize = 16.sp, color = cs.primary)
                        Spacer(Modifier.size(4.dp))
                        Text(
                            state.monthName.ifBlank { "Назад" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.primary,
                        )
                    }
                    Text(
                        "Заполнить месяц",
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.primary,
                    )
                    Spacer(Modifier.size(56.dp))
                }
                StepRail(step = state.step)
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cs.background)
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.step == 2) {
                    Box(
                        modifier = Modifier
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, cs.outline, RoundedCornerShape(14.dp))
                            .clickable { onGoToStep(1) }
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("‹ Назад", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = cs.primary)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cs.tertiary)
                        .clickable(enabled = !state.isSaving) {
                            if (state.step == 1) onGoToStep(2) else onApply()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(color = cs.surface, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            if (state.step == 1) "Далее →" else "Применить",
                            color = cs.surface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            if (state.step == 1) {
                Step1(
                    state = state,
                    onSelectPattern = onSelectPattern,
                    onRequestDeletePattern = { patternToDelete = it },
                    onOpenTime = { field -> timePickerFor = field },
                    onOpenTypePicker = onOpenTypePicker,
                    onCloseTypePicker = onCloseTypePicker,
                    onSetCycleDayType = onSetCycleDayType,
                    onAddCycleDay = onAddCycleDay,
                    onRemoveCycleDay = onRemoveCycleDay,
                    onContinuePrevious = onContinuePrevious,
                    onDeclineContinuePrevious = onDeclineContinuePrevious,
                )
            } else {
                Step2(state, onSetFirstDay, onShiftMonth, onSetExtendToNextMonth)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    timePickerFor?.let { which ->
        val initial = when (which) {
            "dayStart" -> state.dayStartText
            "dayEnd" -> state.dayEndText
            "nightStart" -> state.nightStartText
            else -> state.nightEndText
        }
        val (h, m) = initial.split(":").let {
            (it.getOrNull(0)?.toIntOrNull() ?: 8) to (it.getOrNull(1)?.toIntOrNull() ?: 0)
        }
        TimeInputDialog(
            initialHour = h,
            initialMinute = m,
            onConfirm = { hh, mm ->
                when (which) {
                    "dayStart" -> onSetDayStart(hh, mm)
                    "dayEnd" -> onSetDayEnd(hh, mm)
                    "nightStart" -> onSetNightStart(hh, mm)
                    else -> onSetNightEnd(hh, mm)
                }
                timePickerFor = null
            },
            onDismiss = { timePickerFor = null },
        )
    }

    patternToDelete?.let { p ->
        AppBottomSheet(
            onDismissRequest = { patternToDelete = null },
            sheetState = rememberModalBottomSheetState(),
            headerContent = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Удалить график «${p.title}»?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            },
            actions = listOf(
                BottomSheetAction(text = "Да, удалить") {
                    onDeletePattern(p.id)
                    patternToDelete = null
                }
            ),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun Step1(
    state: WizardUiState,
    onSelectPattern: (String) -> Unit,
    onRequestDeletePattern: (SchedulePattern) -> Unit,
    onOpenTime: (String) -> Unit,
    onOpenTypePicker: (Int) -> Unit,
    onCloseTypePicker: () -> Unit,
    onSetCycleDayType: (Int, ShiftKind) -> Unit,
    onAddCycleDay: () -> Unit,
    onRemoveCycleDay: (Int) -> Unit,
    onContinuePrevious: () -> Unit,
    onDeclineContinuePrevious: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    if (state.showContinuePreviousSheet) {
        AppBottomSheet(
            // Сброс флага обязателен: без него шторка остаётся в композиции
            // и её scrim перехватывает нажатия по всему экрану мастера.
            onDismissRequest = onDeclineContinuePrevious,
            sheetState = rememberModalBottomSheetState(),
            title = "Продолжить график прошлого месяца?",
            actions = listOf(BottomSheetAction(text = "Продолжить") { onContinuePrevious() }),
            cancelText = "Выбрать заново",
        )
    }

    // Отклонённая шторка не должна закрывать доступ к продолжению — оставляем
    // кнопку на шаге выбора графика, пока прошлый месяц заполнен мастером.
    if (state.canContinuePrevious) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, cs.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .clickable { onContinuePrevious() }
                .padding(vertical = 14.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Продолжить график прошлого месяца",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W600,
                    color = cs.primary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Цикл продолжится с той же фазы, с 1 числа",
                    fontSize = 12.sp,
                    color = cs.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    SectionLabel("Варианты графика")
    // Плитки паттернов из хранилища + плитка-конструктор «Свой».
    val tiles = state.patterns.map { it to false } + (customPattern() to true)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (p, isCustomTile) ->
                    PatternTile(
                        pattern = p,
                        active = p.id == state.selectedId,
                        deletable = !isCustomTile,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectPattern(p.id) },
                        onLongClick = { if (!isCustomTile) onRequestDeletePattern(p) },
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "Удерживайте график, чтобы удалить его.",
        fontSize = 12.sp,
        color = cs.onSurfaceVariant,
        modifier = Modifier.padding(start = 2.dp),
    )

    if (state.isCustom) {
        Spacer(Modifier.height(24.dp))
        SectionLabel("Цикл смен")
        CycleEditor(
            cycle = state.customCycle,
            pickerIndex = state.pickerIndex,
            onOpenPicker = onOpenTypePicker,
            onClosePicker = onCloseTypePicker,
            onSetType = onSetCycleDayType,
            onAdd = onAddCycleDay,
            onRemove = onRemoveCycleDay,
        )
    }

    // Карточки времени: показываем дневную/ночную по составу выбранного паттерна.
    val hasDay = state.hasDayShift
    val hasNight = state.hasNightShift
    if (hasDay || hasNight) {
        Spacer(Modifier.height(24.dp))
        SectionLabel("Время смены")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (hasDay) {
                ShiftTimeCard(
                    title = if (hasNight) "Дневная" else null,
                    kind = ShiftKind.DAY,
                    start = state.dayStartText, end = state.dayEndText,
                    onOpenStart = { onOpenTime("dayStart") }, onOpenEnd = { onOpenTime("dayEnd") },
                )
            }
            if (hasNight) {
                ShiftTimeCard(
                    title = "Ночная",
                    kind = ShiftKind.NIGHT,
                    start = state.nightStartText, end = state.nightEndText,
                    onOpenStart = { onOpenTime("nightStart") }, onOpenEnd = { onOpenTime("nightEnd") },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Смены разложатся по всем рабочим дням паттерна.",
            fontSize = 12.5.sp,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

/** UI-модель плитки-конструктора «Свой». */
private fun customPattern() = SchedulePattern(
    id = CUSTOM_PATTERN_ID,
    title = "Свой",
    subtitle = "Настроить вручную",
    cycle = emptyList(),
)

@Composable
private fun ShiftTimeCard(
    title: String?,
    kind: ShiftKind,
    start: String,
    end: String,
    onOpenStart: () -> Unit,
    onOpenEnd: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cs.surface)
            .border(1.dp, cs.outlineVariant, RoundedCornerShape(16.dp)),
    ) {
        if (title != null) {
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ShiftBadge(kind, size = 22.dp)
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cs.onSurfaceVariant)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeField("Начало", start, Modifier.weight(1f), onOpenStart)
            Box(modifier = Modifier.height(48.dp).width(1.dp).background(cs.outlineVariant))
            TimeField("Конец", end, Modifier.weight(1f), onOpenEnd)
        }
    }
}

/** Небольшой значок типа смены (плашка + глиф). */
@Composable
private fun ShiftBadge(kind: ShiftKind, size: androidx.compose.ui.unit.Dp) {
    val pal = shiftPalette(kind)
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3))
            .background(pal.plate)
            .border(1.dp, pal.border, RoundedCornerShape(size / 3)),
        contentAlignment = Alignment.Center,
    ) {
        ShiftGlyph(kind, pal.ink, modifier = Modifier.size(size * 0.6f))
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TimeField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(label, fontSize = 12.sp, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(3.dp))
        Text(value, fontFamily = MonoFont, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = cs.primary)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PatternTile(
    pattern: SchedulePattern,
    active: Boolean,
    deletable: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .heightIn(min = 84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) cs.primaryContainer else cs.surface)
            .border(
                width = if (active) 1.5.dp else 1.dp,
                color = if (active) cs.tertiary else cs.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(16.dp),
    ) {
        if (active) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(cs.tertiary),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓", color = cs.surface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column {
            Text(
                pattern.title,
                fontFamily = MonoFont,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (active) cs.tertiary else cs.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(pattern.subtitle, fontSize = 12.5.sp, color = cs.onSurfaceVariant, lineHeight = 15.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CycleEditor(
    cycle: List<ShiftKind>,
    pickerIndex: Int?,
    onOpenPicker: (Int) -> Unit,
    onClosePicker: () -> Unit,
    onSetType: (Int, ShiftKind) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        cycle.forEachIndexed { i, kind ->
            val pal = shiftPalette(kind)
            val isPicking = pickerIndex == i
            Box(
                modifier = Modifier
                    .size(width = 54.dp, height = 60.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(pal.plate)
                    .border(
                        width = if (isPicking) 2.dp else 1.dp,
                        color = if (isPicking) cs.primary else pal.border,
                        RoundedCornerShape(13.dp),
                    )
                    // Тап по дню открывает режим редактирования (пикер типа + удаление).
                    .clickable { onOpenPicker(i) },
            ) {
                Text(
                    "${i + 1}",
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 4.dp),
                    fontFamily = MonoFont,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink.copy(alpha = 0.65f),
                )
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    ShiftGlyph(kind, pal.ink, modifier = Modifier.size(18.dp))
                    Text(shiftShort(kind), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = pal.ink)
                }
            }
        }
        // Кнопка «+» — добавить день цикла
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 60.dp)
                .clip(RoundedCornerShape(13.dp))
                .border(1.5.dp, cs.outline, RoundedCornerShape(13.dp))
                .clickable { onAdd() },
            contentAlignment = Alignment.Center,
        ) {
            Text("+", fontSize = 22.sp, color = cs.onSurfaceVariant)
        }
    }

    // Пикер типа дня — раскрывается под рядом (безопасный способ: без промаха по ×).
    if (pickerIndex != null && pickerIndex in cycle.indices) {
        Spacer(Modifier.height(12.dp))
        TypePicker(
            dayNum = pickerIndex + 1,
            current = cycle[pickerIndex],
            canRemove = cycle.size > 1,
            onPick = { onSetType(pickerIndex, it) },
            onRemove = { onRemove(pickerIndex) },
            onClose = onClosePicker,
        )
    }

    val day = cycle.count { it == ShiftKind.DAY }
    val night = cycle.count { it == ShiftKind.NIGHT }
    val off = cycle.count { it == ShiftKind.OFF }
    val parts = buildList {
        if (day > 0) add("$day ${plural(day, "дневная", "дневных", "дневных")}")
        if (night > 0) add("$night ${plural(night, "ночная", "ночных", "ночных")}")
        if (off > 0) add("$off ${plural(off, "выходной", "выходных", "выходных")}")
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "Цикл: ${parts.joinToString(", ")}. Нажмите день, чтобы изменить тип или удалить.",
        fontSize = 12.5.sp,
        color = cs.onSurfaceVariant,
        lineHeight = 17.sp,
        modifier = Modifier.padding(horizontal = 2.dp),
    )

    // Легенда
    Spacer(Modifier.height(14.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cs.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LegendItem(ShiftKind.DAY)
        LegendItem(ShiftKind.NIGHT)
        LegendItem(ShiftKind.OFF)
    }
}

@Composable
private fun TypePicker(
    dayNum: Int,
    current: ShiftKind,
    canRemove: Boolean,
    onPick: (ShiftKind) -> Unit,
    onRemove: () -> Unit,
    onClose: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cs.surfaceVariant)
            .border(1.dp, cs.outlineVariant, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("День $dayNum — выберите тип", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = cs.primary)
            Box(modifier = Modifier.clip(CircleShape).clickable { onClose() }.padding(4.dp)) {
                Text("✕", fontSize = 14.sp, color = cs.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(ShiftKind.DAY, ShiftKind.NIGHT, ShiftKind.OFF).forEach { kind ->
                val pal = shiftPalette(kind)
                val active = kind == current
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (active) pal.plate else cs.surface)
                        .border(
                            1.dp,
                            if (active) (if (kind == ShiftKind.OFF) cs.outlineVariant else pal.border) else cs.outlineVariant,
                            RoundedCornerShape(11.dp),
                        )
                        .clickable { onPick(kind) }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShiftGlyph(
                        kind,
                        if (active) pal.ink else cs.onSurfaceVariant,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    CompositionLocalProvider(LocalDensity provides shiftLabelDensity()) {
                        Text(
                            shiftLabel(kind),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (active) pal.ink else cs.primary,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
        if (canRemove) {
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(11.dp))
                    .background(cs.error.copy(alpha = 0.08f))
                    .border(1.dp, cs.error.copy(alpha = 0.35f), RoundedCornerShape(11.dp))
                    .clickable { onRemove() }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Удалить день $dayNum из цикла", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = cs.error)
            }
        }
    }
}

@Composable
private fun LegendItem(kind: ShiftKind) {
    val cs = MaterialTheme.colorScheme
    val pal = shiftPalette(kind)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(pal.plate)
                .border(1.dp, pal.border, RoundedCornerShape(4.dp)),
        )
        CompositionLocalProvider(LocalDensity provides shiftLabelDensity()) {
            Text(shiftLabel(kind), fontSize = 12.sp, color = cs.onSurfaceVariant,
                maxLines = 1, softWrap = false)
        }
    }
}

private fun plural(n: Int, one: String, few: String, many: String): String {
    val m10 = n % 10
    val m100 = n % 100
    return when {
        m100 in 11..14 -> many
        m10 == 1 -> one
        m10 in 2..4 -> few
        else -> many
    }
}

@Composable
private fun Step2(
    state: WizardUiState,
    onSetFirstDay: (Int) -> Unit,
    onShiftMonth: (Int) -> Unit,
    onSetExtendToNextMonth: (Boolean) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        SectionLabel("Первый день цикла")
        Row {
            TextButton(onClick = { onShiftMonth(-1) }) { Text("‹") }
            TextButton(onClick = { onShiftMonth(1) }) { Text("›") }
        }
    }
    Text(
        "Число месяца, с которого начинается ваш цикл смен.",
        fontSize = 12.5.sp,
        color = cs.onSurfaceVariant,
        modifier = Modifier.padding(start = 2.dp, bottom = 12.dp),
    )
    FlowDayGrid(daysInMonth = state.daysInMonth, selected = state.firstDay, onSelect = onSetFirstDay)
    Spacer(Modifier.height(24.dp))
    ExtendToNextMonthCheckbox(
        checked = state.extendToNextMonth,
        onCheckedChange = onSetExtendToNextMonth,
    )
    Spacer(Modifier.height(4.dp))
    val shiftCount = state.preview.count { it != ShiftKind.OFF }
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        SectionLabelInline("Предпросмотр · ${state.monthName.lowercase()}")
        Text("$shiftCount смен", fontSize = 12.sp, fontFamily = MonoFont, color = cs.onSurfaceVariant)
    }
    PreviewGrid(state.preview)
}

@Composable
private fun ExtendToNextMonthCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "Продлить на следующий месяц",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = cs.primary,
        )
    }
}

@Composable
private fun FlowDayGrid(daysInMonth: Int, selected: Int, onSelect: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val rows = (daysInMonth + 6) / 7
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (r in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val day = r * 7 + c + 1
                    if (day <= daysInMonth) {
                        val sel = day == selected
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) cs.tertiary else cs.surface)
                                .border(
                                    width = 1.dp,
                                    color = if (sel) Color.Transparent else cs.outlineVariant,
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable { onSelect(day) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                day.toString(),
                                fontFamily = MonoFont,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) cs.surface else cs.primary,
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f)) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewGrid(preview: List<ShiftKind>) {
    val cs = MaterialTheme.colorScheme
    val rows = (preview.size + 6) / 7
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cs.surface)
            .border(1.dp, cs.outlineVariant, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        for (r in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val i = r * 7 + c
                    if (i < preview.size) {
                        val kind = preview[i]
                        val pal = shiftPalette(kind)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(pal.plate)
                                .border(1.dp, pal.border, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = (i + 1).toString(),
                                fontFamily = MonoFont,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = pal.ink.copy(alpha = 0.75f),
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f)) {}
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cs.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LegendItem(ShiftKind.DAY)
        LegendItem(ShiftKind.NIGHT)
        LegendItem(ShiftKind.OFF)
    }
}

@Composable
private fun StepRail(step: Int) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepDot(number = 1, done = step > 1, active = step == 1)
        Spacer(Modifier.size(10.dp))
        Text("График", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (step == 1) cs.primary else cs.onSurfaceVariant)
        Box(modifier = Modifier.padding(horizontal = 10.dp).height(2.dp).width(28.dp).clip(RoundedCornerShape(1.dp)).background(if (step > 1) cs.tertiary else cs.surfaceBright))
        StepDot(number = 2, done = false, active = step == 2)
        Spacer(Modifier.size(10.dp))
        Text("Старт и предпросмотр", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (step == 2) cs.primary else cs.onSurfaceVariant)
    }
}

@Composable
private fun StepDot(number: Int, done: Boolean, active: Boolean) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(if (done || active) cs.tertiary else cs.surfaceBright),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (done) "✓" else number.toString(),
            fontFamily = MonoFont,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (done || active) cs.surface else cs.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 2.dp, top = 4.dp, bottom = 10.dp),
    )
}

@Composable
private fun SectionLabelInline(text: String) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeInputDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val timeState = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = {
            TextButton(onClick = { onConfirm(timeState.hour, timeState.minute) }) { Text("ОК") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = timeState)
            }
        },
    )
}
