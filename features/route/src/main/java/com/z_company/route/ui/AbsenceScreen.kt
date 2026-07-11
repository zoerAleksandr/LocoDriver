package com.z_company.route.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.domain.entities.ReleaseType
import com.z_company.route.viewmodel.AbsenceUiState
import java.time.LocalDate

private fun absTypeColor(type: ReleaseType): Color = when (type) {
    ReleaseType.Vacation -> Color(0xFF34C759)
    ReleaseType.SickLeave -> Color(0xFFFF9F0A)
    ReleaseType.Courses -> Color(0xFFAF52DE)
    ReleaseType.Donor -> Color(0xFFFF3B30)
    ReleaseType.ChildCare -> Color(0xFF00A0F5)
    ReleaseType.DayOff -> Color(0xFFF4433C)
    ReleaseType.BusinessTrip -> Color(0xFF30B0C7)
    ReleaseType.Other -> Color(0xFF8A8278)
}

private val WEEK_RU_ABS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

private fun monthGen(monthIndex: Int): String = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря"
).getOrElse(monthIndex) { "" }

@Composable
fun AbsenceScreen(
    state: AbsenceUiState,
    types: List<ReleaseType>,
    onBack: () -> Unit,
    onDayTap: (Int) -> Unit,
    onSetType: (ReleaseType) -> Unit,
    onToggleTypePicker: () -> Unit,
    onSave: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val typeColor = absTypeColor(state.selectedType)
    val hasRange = state.rangeStart != null
    val days = if (state.rangeStart != null && state.rangeEnd != null)
        maxOf(state.rangeStart, state.rangeEnd) - minOf(state.rangeStart, state.rangeEnd) + 1 else 0

    Scaffold(
        containerColor = cs.background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cs.background)
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 26.dp),
            ) {
                val enabled = hasRange && !state.isSaving
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (enabled) cs.tertiary else cs.surfaceBright)
                        .clickable(enabled = enabled) { onSave() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(color = cs.surface, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            "Сохранить",
                            color = if (enabled) cs.surface else cs.onSurfaceVariant,
                            fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
    ) { padding ->
        if (state.isLoading && state.year == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = cs.tertiary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Шапка
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onBack() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text("‹ Назад", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = cs.primary)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Новое отвлечение", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = cs.primary)
                    Text(
                        when {
                            days > 1 -> "Выбран диапазон дат"
                            days == 1 -> "Выбрана 1 дата"
                            else -> "Выберите даты"
                        },
                        fontSize = 12.sp, color = cs.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(56.dp))
            }

            // Месяц
            Text(
                "${state.monthName} ${state.year}",
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = cs.primary,
                modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 4.dp),
            )

            // Дни недели
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                WEEK_RU_ABS.forEachIndexed { i, d ->
                    Text(
                        d.uppercase(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
                        color = if (i >= 5) cs.tertiary else cs.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }

            // Сетка выбора диапазона
            RangeGrid(state = state, typeColor = typeColor, onDayTap = onDayTap,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))

            Text(
                "Нажмите дату для одного дня или две даты — начало и конец диапазона.",
                fontSize = 12.sp, color = cs.onSurfaceVariant, lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )

            // Тип отвлечения
            TypeSelector(
                state = state, types = types, typeColor = typeColor,
                onToggle = onToggleTypePicker, onSelect = onSetType,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )

            // Сводка
            SummaryCard(
                typeColor = typeColor,
                typeLabel = state.selectedType.text,
                rangeText = when {
                    days > 1 -> "${minOf(state.rangeStart!!, state.rangeEnd!!)}–${maxOf(state.rangeStart, state.rangeEnd)} ${monthGen(state.month)}"
                    days == 1 -> "${state.rangeStart} ${monthGen(state.month)}"
                    else -> "Не выбрано"
                },
                days = days,
                hours = state.rangeHours,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RangeGrid(
    state: AbsenceUiState,
    typeColor: Color,
    onDayTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.year == 0) return
    val first = LocalDate.of(state.year, state.month + 1, 1)
    val offset = first.dayOfWeek.value - 1
    val cells = ArrayList<Int?>()
    repeat(offset) { cells.add(null) }
    for (d in 1..state.daysInMonth) cells.add(d)
    while (cells.size % 7 != 0) cells.add(null)

    val lo = state.rangeStart?.let { s -> state.rangeEnd?.let { e -> minOf(s, e) } ?: s }
    val hi = state.rangeStart?.let { s -> state.rangeEnd?.let { e -> maxOf(s, e) } ?: s }

    Column(modifier = modifier.fillMaxWidth()) {
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { d ->
                    Box(modifier = Modifier.weight(1f)) {
                        RangeCell(state, d, lo, hi, typeColor, onClick = { d?.let(onDayTap) })
                    }
                }
                repeat(7 - week.size) { Box(modifier = Modifier.weight(1f)) {} }
            }
            Spacer(Modifier.height(3.dp))
        }
    }
}

@Composable
private fun RangeCell(
    state: AbsenceUiState,
    day: Int?,
    lo: Int?,
    hi: Int?,
    typeColor: Color,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    if (day == null) {
        Spacer(Modifier.height(52.dp))
        return
    }
    val inRange = lo != null && hi != null && day in lo..hi
    val isAnchor = day == lo || day == hi
    val isMid = inRange && !isAnchor
    val existing = state.existingAbsences[day]
    val ec = existing?.let { absTypeColor(it) }

    val bg = when {
        isAnchor -> typeColor
        isMid -> typeColor.copy(alpha = 0.30f)
        ec != null -> ec.copy(alpha = 0.10f)
        else -> cs.surface
    }
    val border = when {
        isAnchor || isMid -> typeColor
        ec != null -> ec.copy(alpha = 0.30f)
        else -> cs.outlineVariant
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(if (isAnchor || isMid) 1.5.dp else 1.dp, border, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(5.dp),
    ) {
        Text(
            day.toString(),
            fontFamily = MonoFont, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = if (isAnchor) cs.surface else cs.primary,
        )
        if (existing != null && !inRange && ec != null) {
            Spacer(Modifier.weight(1f))
            Text(
                absBadgeAbs(existing),
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End,
                fontSize = 8.sp, fontWeight = FontWeight.Bold, color = ec,
            )
        }
    }
}

private fun absBadgeAbs(type: ReleaseType): String = when (type) {
    ReleaseType.Vacation -> "ОТП"
    ReleaseType.SickLeave -> "Б/Л"
    ReleaseType.Courses -> "КУР"
    ReleaseType.Donor -> "ДОН"
    ReleaseType.ChildCare -> "УХ"
    ReleaseType.DayOff -> "ВЫХ"
    ReleaseType.BusinessTrip -> "КОМ"
    ReleaseType.Other -> "ОТВ"
}

@Composable
private fun TypeSelector(
    state: AbsenceUiState,
    types: List<ReleaseType>,
    typeColor: Color,
    onToggle: () -> Unit,
    onSelect: (ReleaseType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cs.surface)
            .border(1.dp, cs.outlineVariant, RoundedCornerShape(14.dp)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Тип отвлечения", modifier = Modifier.weight(1f), fontSize = 15.sp, color = cs.primary)
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(typeColor))
            Spacer(Modifier.width(10.dp))
            Text(state.selectedType.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = cs.primary)
            Spacer(Modifier.width(8.dp))
            Text(if (state.typePickerOpen) "⌃" else "⌄", fontSize = 13.sp, color = cs.onSurfaceVariant)
        }
        if (state.typePickerOpen) {
            types.forEach { type ->
                val active = type == state.selectedType
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(type) }
                        .background(if (active) cs.surfaceBright else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(absTypeColor(type)))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        type.text, modifier = Modifier.weight(1f), fontSize = 15.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, color = cs.primary,
                    )
                    if (active) Text("✓", color = cs.tertiary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    typeColor: Color,
    typeLabel: String,
    rangeText: String,
    days: Int,
    hours: Int,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(typeColor.copy(alpha = 0.10f))
            .border(1.dp, typeColor.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                typeLabel.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp, color = typeColor,
            )
            Text(
                rangeText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = cs.primary,
                fontFamily = MonoFont, modifier = Modifier.padding(top = 4.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(1.dp).background(typeColor.copy(alpha = 0.2f)),
        ) {}
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell("Дней", days.toString(), if (days == 1) "день" else if (days < 5) "дня" else "дней",
                Modifier.weight(1f))
            Box(modifier = Modifier.width(1.dp).height(56.dp).background(typeColor.copy(alpha = 0.2f)))
            StatCell("Часов", "$hours:00", "по рабочим дням", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = cs.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = cs.primary, fontFamily = MonoFont)
            Spacer(Modifier.width(6.dp))
            Text(sub, fontSize = 12.sp, color = cs.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}
