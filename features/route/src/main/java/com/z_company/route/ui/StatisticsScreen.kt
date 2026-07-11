package com.z_company.route.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.route.component.StatIcons
import androidx.compose.foundation.layout.PaddingValues
import com.z_company.route.viewmodel.StatCompareOption
import com.z_company.route.viewmodel.StatDelta
import com.z_company.route.viewmodel.StatDetailBar
import com.z_company.route.viewmodel.StatDetailState
import com.z_company.route.viewmodel.StatDonutSeg
import com.z_company.route.viewmodel.StatMetric
import com.z_company.route.viewmodel.StatMonthBar
import com.z_company.route.viewmodel.StatTab
import com.z_company.route.viewmodel.StatTopDirection
import com.z_company.route.viewmodel.StatYearRow
import com.z_company.route.viewmodel.StatisticsUiState

// ════════════════════════════════════════════════════════════════════
// Экран «Статистика» — рендер по референсу (концепт B, см. design/
// screenshots/21-statistika.png). Данные — из StatisticsViewModel.
// ════════════════════════════════════════════════════════════════════

// Категориальная палитра donut (бренд-независимая — как в макете)
private val DONUT_RAMP = listOf(
    Color(0xFF0079C2), Color(0xFF17B0A8), Color(0xFFF2A33C), Color(0xFFE5683C), Color(0xFF7C6CD6),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    state: StatisticsUiState,
    onBack: () -> Unit,
    onTab: (StatTab) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onCompareSelect: (String) -> Unit,
    onOpenDetail: (String) -> Unit = {},
    onCloseDetail: () -> Unit = {},
    onSelectDetailMonth: (Int) -> Unit = {},
    onPickBaselineMonth: (Int, Int) -> Unit = { _, _ -> },
    onPickBaselineYear: (Int) -> Unit = {},
    onSelectHistoryMetric: (String) -> Unit = {},
    onExportPdf: () -> Unit = {},
    isExportingPdf: Boolean = false,
) {
    // null | "compare" | "pickMonth" | "pickYear"
    var sheet by remember { mutableStateOf<String?>(null) }
    var infoKey by remember { mutableStateOf<String?>(null) }
    val detail = state.detail

    if (detail != null) BackHandler(enabled = true) { onCloseDetail() }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text(
                        text = detail?.title ?: "Статистика",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (detail != null) onCloseDetail() else onBack() }) {
                        Icon(StatIcons.ChevronLeft, "Назад", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    if (detail == null) {
                        IconButton(onClick = onExportPdf, enabled = !isExportingPdf) {
                            if (isExportingPdf) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            } else {
                                Icon(StatIcons.Pdf, "Сохранить в PDF", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (detail != null) {
            DetailContent(detail, padding, onSelectDetailMonth)
            return@Scaffold
        }
        // Данные вкладки ещё считаются (сегмент уже переключён мгновенно, см. setTab).
        val contentLoading = state.loading && when (state.tab) {
            StatTab.HISTORY -> state.historyRows.isEmpty()
            else -> state.metrics.isEmpty()
        }
        // Шапка (сегмент + навигация по периодам) закреплена, скроллится только контент.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            PeriodSegment(state.tab, onTab)
            Spacer(Modifier.height(12.dp))
            if (!contentLoading && state.tab != StatTab.HISTORY) {
                PeriodNav(state.periodLabel, state.periodSub, state.canGoPrev, state.canGoNext, onPrev, onNext)
                Spacer(Modifier.height(12.dp))
            }

            if (contentLoading) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    LoadingContent()
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    when (state.tab) {
                        StatTab.HISTORY -> HistoryTab(state, onSelectHistoryMetric)
                        else -> {
                            CompareSelector(state.compareTitle, none = !state.compareEnabled, onOpen = { sheet = "compare" })
                            if (state.compareNoData) {
                                CompareNoDataNote(state.compareTitle)
                            }
                            Spacer(Modifier.height(8.dp))
                            if (state.empty) {
                                EmptyBlock(state.tab)
                            } else {
                                if (state.tab == StatTab.YEAR) {
                                    YearHero(state.yearHeroValue, state.yearHeroDelta, state.yearBars, state.compareEnabled)
                                    Spacer(Modifier.height(20.dp))
                                }
                                MetricGrid(state.metrics, state.wideFirst, state.compareEnabled, state.currency, onOpenDetail, onInfo = { infoKey = it })
                                Spacer(Modifier.height(4.dp))
                                if (state.topDirections.isNotEmpty()) {
                                    SectionHeader(if (state.tab == StatTab.YEAR) "Топ направлений за год" else "Топ направлений")
                                    StatCard { TopDirectionsDonut(state.topDirections) }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }

    when (sheet) {
        "compare" -> ComparePicker(
            title = if (state.tab == StatTab.YEAR) "Сравнить год с" else "Сравнить месяц с",
            options = state.compareOptions,
            selectedId = state.compareSelectedId,
            onSelect = { onCompareSelect(it); sheet = null },
            onAction = { sheet = if (state.tab == StatTab.YEAR) "pickYear" else "pickMonth" },
            onDismiss = { sheet = null },
        )
        "pickMonth" -> MonthGridPicker(
            years = state.pickYears,
            curYm = state.pickCurYm,
            available = state.availableMonthsYm,
            onPick = { y, m -> onPickBaselineMonth(y, m); sheet = null },
            onBack = { sheet = "compare" },
            onDismiss = { sheet = null },
        )
        "pickYear" -> YearListPicker(
            years = state.pickYearOptions,
            onPick = { onPickBaselineYear(it); sheet = null },
            onBack = { sheet = "compare" },
            onDismiss = { sheet = null },
        )
    }

    infoKey?.let { key ->
        MetricInfoSheet(key = key, onDismiss = { infoKey = null })
    }
}

@Composable
private fun LoadingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.tertiary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Считаем статистику…",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Собираем показатели за период",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyBlock(tab: StatTab) {
    Column(
        modifier = Modifier.fillMaxWidth().height(280.dp).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(76.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceBright),
            contentAlignment = Alignment.Center,
        ) {
            Icon(StatIcons.Gauge, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = if (tab == StatTab.YEAR) "За год данных нет" else "За месяц данных нет",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "В этом периоде ещё не закрыто ни одного маршрута.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Сегмент Месяц / Год / История ────────────────────────────────────
@Composable
private fun PeriodSegment(tab: StatTab, onTab: (StatTab) -> Unit) {
    val items = listOf(StatTab.MONTH to "Месяц", StatTab.YEAR to "Год", StatTab.HISTORY to "История")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceBright)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { (t, label) ->
            val active = t == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .then(if (active) Modifier.background(MaterialTheme.colorScheme.surface) else Modifier)
                    .clickable { onTab(t) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Навигация по периодам  ‹ Март 2026 › ─────────────────────────────
@Composable
private fun PeriodNav(label: String, sub: String, canPrev: Boolean, canNext: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev, enabled = canPrev) {
            Icon(
                StatIcons.ChevronLeft, "Предыдущий период", Modifier.size(20.dp),
                tint = if (canPrev) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary)
            Text(sub, style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onNext, enabled = canNext) {
            Icon(
                StatIcons.ChevronRight, "Следующий период", Modifier.size(20.dp),
                tint = if (canNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            )
        }
    }
}

// ── Выбор периода-эталона ────────────────────────────────────────────
@Composable
private fun CompareSelector(title: String, none: Boolean, onOpen: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (none) "режим" else "в сравнении с",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (none) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onOpen)
                .padding(start = 11.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = if (none) "Без сравнения" else title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = if (none) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            )
            Icon(
                StatIcons.ChevronDown, null, Modifier.size(14.dp),
                tint = if (none) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

// Информер: выбранный для сравнения период без маршрутов — сравнивать не с чем.
@Composable
private fun CompareNoDataNote(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceBright)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            StatIcons.Gauge, null, Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "В периоде «$title» нет маршрутов — сравнивать не с чем",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 12.dp, start = 4.dp),
    )
}

@Composable
private fun StatCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) { content() }
}

// ── Дельта-чип ───────────────────────────────────────────────────────
@Composable
private fun DeltaTag(delta: StatDelta, plain: Boolean = false, small: Boolean = false) {
    val isNorm = delta.kind == "norm"
    val color: Color
    val bg: Color
    val arrow: String
    if (isNorm) {
        when (delta.state) {
            "over" -> { color = MaterialTheme.colorScheme.error; bg = MaterialTheme.colorScheme.error.copy(alpha = 0.12f); arrow = "up" }
            "ok" -> { color = MaterialTheme.colorScheme.surfaceTint; bg = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.12f); arrow = "down" }
            else -> { color = MaterialTheme.colorScheme.onSurfaceVariant; bg = MaterialTheme.colorScheme.surfaceBright; arrow = "flat" }
        }
    } else {
        color = MaterialTheme.colorScheme.onSurfaceVariant
        bg = MaterialTheme.colorScheme.surfaceBright
        arrow = delta.dir ?: "flat"
    }
    Row(
        modifier = if (plain) Modifier
        else Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        DeltaArrow(arrow, color, if (small) 9.dp else 10.dp)
        Text(delta.pct ?: "", fontFamily = MonoFont, fontSize = if (small) 11.sp else 12.sp,
            fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun DeltaArrow(dir: String, color: Color, size: Dp) {
    when (dir) {
        "flat" -> Icon(StatIcons.DeltaFlat, null, Modifier.size(size), tint = color)
        "down" -> Icon(StatIcons.DeltaUp, null, Modifier.size(size).rotate(180f), tint = color)
        else -> Icon(StatIcons.DeltaUp, null, Modifier.size(size), tint = color)
    }
}

// ── Мини-пара столбцов «было / стало» ────────────────────────────────
@Composable
private fun MiniPair(prev: Float, cur: Float, curColor: Color, height: Dp) {
    val max = maxOf(prev, cur, 1f)
    val prevFill = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        MiniBar(prev / max, height, prevFill)
        MiniBar(cur / max, height, curColor)
    }
}

@Composable
private fun MiniBar(frac: Float, height: Dp, fill: Color) {
    Box(modifier = Modifier.width(12.dp).height(height), contentAlignment = Alignment.BottomCenter) {
        Box(
            Modifier.fillMaxWidth().height(height * frac.coerceIn(0.12f, 1f))
                .clip(RoundedCornerShape(3.dp)).background(fill)
        )
    }
}

// Метрики с пояснением по «?».
private val INFO_KEYS = setOf("tkm")

// ── Сетка плашек метрик ──────────────────────────────────────────────
@Composable
private fun MetricGrid(
    metrics: List<StatMetric>,
    wideFirst: Boolean,
    compare: Boolean,
    currency: String,
    onOpenDetail: (String) -> Unit,
    onInfo: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        var i = 0
        if (wideFirst && metrics.isNotEmpty()) {
            MetricPlate(metrics[0], wide = true, compare = compare, currency = currency, onClick = { onOpenDetail(metrics[0].key) }, onInfo = onInfo)
            i = 1
        }
        while (i < metrics.size) {
            val left = metrics[i]
            val right = metrics.getOrNull(i + 1)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    MetricPlate(left, wide = false, compare = compare, currency = currency, onClick = { onOpenDetail(left.key) }, onInfo = onInfo)
                }
                Box(Modifier.weight(1f)) {
                    if (right != null) MetricPlate(right, wide = false, compare = compare, currency = currency, onClick = { onOpenDetail(right.key) }, onInfo = onInfo)
                }
            }
            i += 2
        }
    }
}

// Иконка метрики. Для «Заработано» показываем символ валюты по стране (₽ / ₸ / Br) —
// «Br» это буквы, а не глиф, поэтому рендерим текстом; остальные метрики — вектор-иконки.
@Composable
private fun MetricKeyIcon(key: String, currency: String, sizeDp: Dp, tint: Color) {
    if (key == "earnings") {
        Text(
            text = currency,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = (sizeDp.value - 2f).sp,
            ),
            color = tint,
            maxLines = 1,
        )
    } else {
        Icon(StatIcons.forKey(key), null, Modifier.size(sizeDp), tint = tint)
    }
}

@Composable
private fun MetricPlate(
    m: StatMetric,
    wide: Boolean,
    compare: Boolean,
    currency: String,
    onClick: () -> Unit,
    onInfo: (String) -> Unit,
) {
    val isNorm = m.delta?.kind == "norm"
    val curColor = when {
        isNorm && m.delta?.state == "over" -> MaterialTheme.colorScheme.error
        isNorm && m.delta?.state == "ok" -> MaterialTheme.colorScheme.surfaceTint
        else -> MaterialTheme.colorScheme.tertiary
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = if (wide) 10.dp else 12.dp),
            ) {
                MetricKeyIcon(m.key, currency, 17.dp, MaterialTheme.colorScheme.tertiary)
                Text(m.label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false))
                if (m.key in INFO_KEYS) InfoDot { onInfo(m.key) }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(m.value, fontFamily = MonoFont, fontSize = if (wide) 40.sp else 26.sp,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp,
                        lineHeight = if (wide) 40.sp else 26.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                    // Строку единицы резервируем всегда (в сетке) — чтобы плашки с
                    // единицей и без были одной высоты и ровнялись по сетке.
                    if (m.unit.isNotEmpty()) {
                        Text(m.unit, fontFamily = MonoFont, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp), maxLines = 1)
                    } else if (!wide) {
                        Text(" ", fontFamily = MonoFont, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
                    }
                }
                if (compare && m.hasPrev) MiniPair(m.prevBar, m.curBar, curColor, if (wide) 44.dp else 34.dp)
            }
            if (compare && m.delta != null) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DeltaTag(m.delta, plain = true, small = true)
                    if (m.prevValue.isNotEmpty()) {
                        Text("было ${m.prevValue}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// Кружок «?» на плашке метрики — открывает пояснение. Свой clickable, чтобы тап
// по «?» не открывал детализацию плашки.
@Composable
private fun InfoDot(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("?", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// Шторка с пояснением к метрике (пока — только грузооборот).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricInfoSheet(key: String, onDismiss: () -> Unit) {
    val (title, body) = when (key) {
        "tkm" -> "Грузооборот" to
            "Тонно-км брутто — объём выполненной перевозочной работы.\n\n" +
            "Считается как вес поезда брутто (в тоннах) × пройденное расстояние (в км), " +
            "просуммированный по всем поездам за период.\n\n" +
            "Единица — млн ткм (миллионы тонно-километров)."
        else -> return
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
        }
    }
}

// ── Год: hero-итог + помесячные столбцы ──────────────────────────────
@Composable
private fun YearHero(value: String, delta: StatDelta?, bars: List<StatMonthBar>, compare: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(22.dp)) {
            Text("ОТРАБОТАНО ЗА ГОД · Ч", fontFamily = MonoFont, fontSize = 11.sp, letterSpacing = 1.4.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(value, fontFamily = MonoFont, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.5).sp, lineHeight = 46.sp, color = MaterialTheme.colorScheme.primary)
                if (delta != null) DeltaTag(delta)
            }
            if (bars.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                YearBarsChart(bars, compare)
            }
        }
    }
}

@Composable
private fun YearBarsChart(bars: List<StatMonthBar>, compare: Boolean) {
    val overlayPrev = compare && bars.any { it.prev > 0f }
    val max = (bars.maxOfOrNull { maxOf(it.cur, it.prev) } ?: 1f).coerceAtLeast(1f) * 1.05f
    val accent = MaterialTheme.colorScheme.tertiary
    val prevFill = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(accent, "текущий", false)
            if (overlayPrev) LegendDot(prevFill, "предыдущий", true)
        }
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth().height(150.dp), verticalAlignment = Alignment.Bottom) {
            bars.forEachIndexed { i, m ->
                Column(
                    modifier = Modifier.weight(1f).height(150.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                    ) {
                        if (overlayPrev) {
                            Box(Modifier.width(5.dp).height(130.dp * (m.prev / max))
                                .clip(RoundedCornerShape(2.dp)).background(prevFill))
                        }
                        Box(Modifier.width(6.dp).height(130.dp * (m.cur / max))
                            .clip(RoundedCornerShape(2.dp)).background(accent))
                    }
                    Text(if (i % 2 == 0) m.label else "", fontFamily = MonoFont, fontSize = 8.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String, border: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Топ направлений (donut) ──────────────────────────────────────────
private data class DonutSegment(val label: String, val value: Float, val sub: String, val color: Color)

@Composable
private fun TopDirectionsDonut(items: List<StatTopDirection>) {
    val segments = items.mapIndexed { i, d ->
        DonutSegment("${d.from} → ${d.to}", d.trips.toFloat(), "${d.trips} раз · ${d.hours} ч",
            DONUT_RAMP[i % DONUT_RAMP.size])
    }
    Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
        Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            DonutChart(segments, items.sumOf { it.trips }.toString(), "поездок")
        }
        Spacer(Modifier.height(10.dp))
        val total = segments.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
        segments.forEachIndexed { i, s ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(11.dp).clip(RoundedCornerShape(3.dp)).background(s.color))
                Column(Modifier.weight(1f)) {
                    Text(s.label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(s.sub, fontFamily = MonoFont, fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), modifier = Modifier.padding(top = 2.dp))
                }
                val pct = Math.round(s.value / total * 100)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$pct", fontFamily = MonoFont, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text("%", fontFamily = MonoFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun DonutChart(segments: List<DonutSegment>, centerMain: String, centerSub: String) {
    val track = MaterialTheme.colorScheme.surfaceBright
    val total = segments.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(150.dp)) {
            val strokePx = 26.dp.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(track, -90f, 360f, false, topLeft = topLeft, size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Butt))
            var start = -90f
            val gap = 4f
            segments.forEach { s ->
                val sweep = s.value / total * 360f
                drawArc(s.color, start + gap / 2f, (sweep - gap).coerceAtLeast(0.5f), false,
                    topLeft = topLeft, size = arcSize, style = Stroke(width = strokePx, cap = StrokeCap.Butt))
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerMain, fontFamily = MonoFont, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp, color = MaterialTheme.colorScheme.primary)
            Text(centerSub, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Вкладка «История» ────────────────────────────────────────────────
@Composable
private fun HistoryTab(state: StatisticsUiState, onSelectMetric: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Всё время", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary)
    }
    Spacer(Modifier.height(12.dp))
    // Итог за всё время по ВЫБРАННОЙ метрике
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(22.dp)) {
            Text(state.historyTotalCaption, fontFamily = MonoFont, fontSize = 11.sp, letterSpacing = 1.4.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(state.historyTotal, fontFamily = MonoFont, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.5).sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            Text(
                "За ${state.historyYearsCount} ${com.z_company.route.viewmodel.StatFormat.yearsWord(state.historyYearsCount)} · ${state.historyRoutesTotal} смен",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    // Сетка метрик — тап выбирает метрику для итога и разбивки
    Spacer(Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.historyMetrics.chunked(2).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { m ->
                    Box(Modifier.weight(1f)) {
                        HistoryMetricPlate(m, selected = m.key == state.historySelected, currency = state.currency) { onSelectMetric(m.key) }
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    SectionHeader("Год за годом")
    StatCard {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            state.historyRows.forEach { y -> HistoryRow(y) }
        }
    }
}

@Composable
private fun HistoryMetricPlate(m: com.z_company.route.viewmodel.StatHistoryMetric, selected: Boolean, currency: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surface,
        shadowElevation = if (selected) 2.dp else 1.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricKeyIcon(m.key, currency, 17.dp,
                    if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary)
                Text(m.label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(12.dp))
            Text(m.total, fontFamily = MonoFont, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.8).sp, maxLines = 1,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
            if (m.unit.isNotEmpty()) {
                Text(m.unit, fontFamily = MonoFont, fontSize = 11.5.sp, modifier = Modifier.padding(top = 3.dp),
                    color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HistoryRow(y: StatYearRow) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${y.year}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary)
                y.note?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(y.value, fontFamily = MonoFont, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                if (y.unit.isNotEmpty()) {
                    Text(y.unit, fontFamily = MonoFont, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.surfaceBright)) {
            Box(
                Modifier.fillMaxWidth(y.fraction.coerceIn(0f, 1f)).height(14.dp).clip(RoundedCornerShape(7.dp))
                    .background(if (y.partial) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.tertiary)
            )
        }
    }
}

// ── Детализация метрики ──────────────────────────────────────────────
@Composable
private fun DetailContent(detail: StatDetailState, padding: PaddingValues, onSelectMonth: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        // Hero выбранного месяца
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
        ) {
            Column(Modifier.padding(22.dp)) {
                Text(detail.caption, fontFamily = MonoFont, fontSize = 11.sp, letterSpacing = 1.4.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(detail.heroValue, fontFamily = MonoFont, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1.5).sp, lineHeight = 46.sp, color = MaterialTheme.colorScheme.primary)
                        if (detail.heroUnit.isNotEmpty()) {
                            Text(" ${detail.heroUnit}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                        }
                    }
                    if (detail.heroDelta != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            DeltaTag(detail.heroDelta)
                            if (detail.heroPrevCaption.isNotEmpty()) {
                                Text(detail.heroPrevCaption, fontFamily = MonoFont, fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("тап по столбцу — выбрать месяц", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            }
        }
        SectionHeader("По месяцам")
        StatCard {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 18.dp)) {
                DetailBars(detail.bars, detail.selectedIndex, onSelectMonth)
            }
        }
        if (detail.breakdown.isNotEmpty()) {
            SectionHeader("Детали · по направлениям")
            StatCard {
                BreakdownDonut(detail.breakdown, detail.breakdownCenter, detail.breakdownCenterSub)
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

// Разбивка метрики по направлениям: donut + строки-легенда с долями.
@Composable
private fun BreakdownDonut(segments: List<StatDonutSeg>, centerMain: String, centerSub: String) {
    val donutSegs = segments.mapIndexed { i, s ->
        DonutSegment(s.label, s.value, s.sub, DONUT_RAMP[i % DONUT_RAMP.size])
    }
    Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
        Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            DonutChart(donutSegs, centerMain, centerSub)
        }
        Spacer(Modifier.height(10.dp))
        val total = donutSegs.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
        donutSegs.forEachIndexed { i, s ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(11.dp).clip(RoundedCornerShape(3.dp)).background(s.color))
                Column(Modifier.weight(1f)) {
                    Text(s.label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(s.sub, fontFamily = MonoFont, fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), modifier = Modifier.padding(top = 2.dp))
                }
                val pct = Math.round(s.value / total * 100)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$pct", fontFamily = MonoFont, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text("%", fontFamily = MonoFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun DetailBars(bars: List<StatDetailBar>, selected: Int, onSelect: (Int) -> Unit) {
    val max = (bars.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(1f) * 1.12f
    val accent = MaterialTheme.colorScheme.tertiary
    Row(modifier = Modifier.fillMaxWidth().height(180.dp), verticalAlignment = Alignment.Bottom) {
        bars.forEachIndexed { i, b ->
            val active = i == selected
            Column(
                modifier = Modifier.weight(1f).height(180.dp).clip(RoundedCornerShape(6.dp)).clickable { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Text(
                            text = if (active) StatFormatBarLabel(b) else "",
                            fontFamily = MonoFont, fontSize = 9.5.sp, fontWeight = FontWeight.Bold,
                            color = accent, maxLines = 1, modifier = Modifier.padding(bottom = 4.dp),
                        )
                        Box(
                            Modifier.width(if (active) 14.dp else 10.dp)
                                .height((150.dp * (b.value / max)).coerceAtLeast(3.dp))
                                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                .background(if (active) accent else accent.copy(alpha = 0.18f))
                        )
                    }
                }
                Text(b.short, fontFamily = MonoFont, fontSize = 9.5.sp,
                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Normal,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 7.dp))
            }
        }
    }
}

// Короткая подпись значения над выбранным столбцом (без единиц, компактно).
private fun StatFormatBarLabel(b: StatDetailBar): String {
    // Значение уже отражено в hero; над столбцом показываем месяц-значение кратко.
    val v = b.value
    return when {
        v >= 100 -> v.toInt().toString()
        v >= 10 -> ((v * 10).toInt() / 10f).toString().trimEnd('0').trimEnd('.').replace(".", ",")
        else -> ((v * 100).toInt() / 100f).toString().replace(".", ",")
    }
}

// ── Шторка выбора периода-эталона ────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComparePicker(
    title: String,
    options: List<StatCompareOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
) {
    SheetShell(title = title, subtitle = "С чем сравнивать выбранный период", onDismiss = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
            Column {
                options.forEachIndexed { i, o ->
                    val active = o.id == selectedId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (active) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier)
                            .clickable { if (o.action) onAction() else onSelect(o.id) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (o.action) {
                            Icon(StatIcons.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.tertiary)
                        } else {
                            RadioDot(active)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(o.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (o.action) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                            Text(o.note, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 1.dp))
                        }
                    }
                    if (i != options.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

// Обёртка шторки: ручка + опц. кнопка «назад» + заголовок + подзаголовок.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetShell(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onBack: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                        Icon(StatIcons.ChevronLeft, "Назад", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.tertiary)
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = if (onBack == null) 4.dp else 0.dp))
            }
            Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 14.dp))
            content()
        }
    }
}

// Пикер «выбрать месяц»: по годам, сетка 4×3. Будущие/текущий недоступны.
@Composable
private fun MonthGridPicker(
    years: List<Int>,
    curYm: Int,
    available: Set<Int>,
    onPick: (Int, Int) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    val shortMonths = listOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек")
    SheetShell(title = "Выбрать месяц", subtitle = "Сравнить можно с месяцем, где есть маршруты", onDismiss = onDismiss, onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            years.forEach { year ->
                Column {
                    Text("$year", fontFamily = MonoFont, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp))
                    (0..2).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (0..3).forEach { col ->
                                val m = row * 4 + col
                                val ym = year * 12 + m
                                // Доступны месяцы с маршрутами (в т.ч. будущие), кроме текущего.
                                val disabled = ym !in available || ym == curYm
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .then(if (disabled) Modifier else Modifier.clickable { onPick(year, m) })
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        shortMonths[m],
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (disabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        else MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Пикер «выбрать год»: список прошлых лет.
@Composable
private fun YearListPicker(
    years: List<Int>,
    onPick: (Int) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    SheetShell(title = "Выбрать год", subtitle = "Сравнить с любым годом из истории", onDismiss = onDismiss, onBack = onBack) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
            Column {
                years.forEachIndexed { i, y ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onPick(y) }.padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("$y год", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        Icon(StatIcons.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (i != years.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun RadioDot(active: Boolean) {
    Box(
        modifier = Modifier.size(20.dp).clip(RoundedCornerShape(10.dp))
            .background(if (active) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(if (active) 8.dp else 16.dp).clip(RoundedCornerShape(8.dp))
                .background(if (active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background)
        )
    }
}
