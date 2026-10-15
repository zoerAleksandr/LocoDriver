package com.z_company.route.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.domain.brake.InstructionRef
import com.z_company.domain.util.sanitizeDecimalInput
import com.z_company.domain.util.sanitizeIntegerInput
import com.z_company.route.component.separateSheetFromScroll

/**
 * Примитивы экрана «Справка ВУ-45» по дизайну (префикс Vu): цвета - из токенов приложения
 * (primary = текст, tertiary = акцент, secondary = поверхность карточки, surfaceBright = bgSubtle,
 * surfaceTint = success, error = danger, onPrimary = accentInk, primaryContainer = accentSoft).
 */
internal class VuColors(
    val text: Color,
    val muted: Color,
    val faint: Color,
    val border: Color,
    val borderStrong: Color,
    val accent: Color,
    val accentInk: Color,
    val accentSoft: Color,
    val bg: Color,
    val surface: Color,
    val bgSubtle: Color,
    val success: Color,
    val danger: Color,
)

@Composable
internal fun vuColors(): VuColors {
    val c = MaterialTheme.colorScheme
    return VuColors(
        text = c.primary, muted = c.onSurfaceVariant, faint = c.primary.copy(alpha = 0.38f),
        border = c.outlineVariant, borderStrong = c.outline,
        accent = c.tertiary, accentInk = c.onPrimary, accentSoft = c.primaryContainer,
        bg = c.background, surface = c.secondary, bgSubtle = c.surfaceBright,
        success = c.surfaceTint, danger = c.error,
    )
}

internal val VuMono = MonoFont

/** Сегментный переключатель (вкладки): контейнер bgSubtle r16, активный - accent/accentInk. */
@Composable
internal fun VuTabs(items: List<String>, active: Int, onSelect: (Int) -> Unit, h: Dp = 48.dp, mono: Boolean = false, modifier: Modifier = Modifier) {
    val t = vuColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(t.bgSubtle, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEachIndexed { i, x ->
            val on = i == active
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(h - 8.dp)
                    .background(if (on) t.accent else Color.Transparent, RoundedCornerShape(12.dp))
                    .clickable { onSelect(i) }
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    x, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp,
                    fontFamily = if (mono) VuMono else null,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (on) t.accentInk else t.text,
                )
            }
        }
    }
}

/** «?» - видимый круг 24dp, зона нажатия 48dp. */
@Composable
internal fun VuHelp(onClick: () -> Unit, modifier: Modifier = Modifier, touch: Dp = 48.dp) {
    val t = vuColors()
    Box(
        modifier = modifier
            .size(touch)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(24.dp).border(1.5.dp, t.borderStrong, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = t.muted) }
    }
}

/**
 * Поле ввода: подпись 13 + «?», бокс h56 r12 (surface; sunk - bgSubtle), значение mono 20/500, суффикс 13.
 * [onClick] != null - поле-пикер (значение не редактируется, справа шеврон).
 */
@Composable
internal fun VuInput(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onChange: ((String) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    unit: String? = null,
    placeholder: String = "",
    onHelp: (() -> Unit)? = null,
    sunk: Boolean = false,
    error: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Decimal,
    maxLen: Int = 10,
    text: Boolean = false,
) {
    val t = vuColors()
    var focused by remember { mutableStateOf(false) }
    val select = onClick != null
    val numeric = !select && !text
    val numHandle = if (numeric) rememberVuNumeric(label, value, { onChange?.invoke(it) }, decimal = keyboard != KeyboardType.Number, maxLen = maxLen) else null
    val numActive = numHandle?.active == true
    Column(modifier.widthIn(min = 0.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (focused || numActive) t.accent else t.muted,
            )
            if (onHelp != null) VuHelp(onHelp, touch = 32.dp)
        }
        val shape = RoundedCornerShape(12.dp)
        val borderColor = when {
            error -> t.danger
            focused || numActive -> t.accent
            else -> Color.Transparent
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .then(if (!sunk && !focused && !numActive && !error) Modifier.shadow(1.dp, shape) else Modifier)
                .background(if (sunk) t.bgSubtle else t.surface, shape)
                .border(2.dp, borderColor, shape)
                .then(if (select) Modifier.clickable { onClick?.invoke() } else Modifier)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (select) {
                Text(
                    value.ifEmpty { placeholder }, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (value.isEmpty()) t.faint else t.text,
                )
                Text("▾", fontSize = 12.sp, color = t.muted)
            } else if (!text) {
                // Числовое поле: без системной клавиатуры (IME) - собственная панель справки и физическая клавиатура.
                val h = numHandle!!
                Box(Modifier.weight(1f).then(h.modifier), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, fontFamily = VuMono, fontSize = 20.sp, color = t.faint)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(value, fontFamily = VuMono, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = t.text, maxLines = 1)
                        if (h.active) Box(Modifier.padding(start = 1.dp).width(2.dp).height(24.dp).background(t.accent))
                    }
                }
            } else {
                BasicTextField(
                    value = value,
                    onValueChange = { raw -> onChange?.invoke(raw.take(40)) },
                    modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium, color = t.text),
                    cursorBrush = SolidColor(t.accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty() && placeholder.isNotEmpty()) {
                                Text(placeholder, fontSize = 20.sp, color = t.faint)
                            }
                            inner()
                        }
                    },
                )
            }
            if (unit != null) Text(unit, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = t.muted, maxLines = 1)
        }
    }
}

/** Чип: h48 r12, mono 15; активный - accent/accentInk. [small] - h40. */
@Composable
internal fun VuChip(text: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, small: Boolean = false) {
    val t = vuColors()
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(if (small) 40.dp else 48.dp)
            .then(if (!on) Modifier.shadow(1.dp, shape) else Modifier)
            .background(if (on) t.accent else t.surface, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = if (small) 12.dp else 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, maxLines = 1, fontFamily = VuMono, fontSize = if (small) 14.sp else 15.sp,
            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium, color = if (on) t.accentInk else t.text,
        )
    }
}

internal enum class VuBtnKind { Filled, Tonal, Text, Danger, Cta }

@Composable
internal fun VuBtn(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: VuBtnKind = VuBtnKind.Filled,
    h: Dp = 48.dp,
    enabled: Boolean = true,
) {
    val t = vuColors()
    val (bg, fg) = when (kind) {
        VuBtnKind.Filled -> t.accent to t.accentInk
        VuBtnKind.Tonal -> t.accentSoft to t.accent
        VuBtnKind.Text -> Color.Transparent to t.accent
        VuBtnKind.Danger -> t.danger to Color.White
        VuBtnKind.Cta -> t.text to t.bg
    }
    Box(
        modifier = modifier
            .height(h)
            .background(if (enabled) bg else bg.copy(alpha = bg.alpha * 0.4f), RoundedCornerShape(h / 2))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = if (kind == VuBtnKind.Text) 12.dp else 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, maxLines = 1, fontSize = if (h >= 60.dp) 20.sp else 15.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) fg else fg.copy(alpha = 0.5f))
    }
}

/** Вердикт: success (норма) / danger (нехватка, превышение). */
@Composable
internal fun VuVerdict(ok: Boolean, text: String, size: Int = 15) {
    val t = vuColors()
    val c = if (ok) t.success else t.danger
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 1.dp).size(20.dp).background(c, CircleShape), contentAlignment = Alignment.Center) {
            Text(if (ok) "✓" else "!", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = t.bg)
        }
        Text(InstructionRef.annotate(text), fontSize = size.sp, fontWeight = FontWeight.SemiBold, color = c, lineHeight = (size * 1.35f).sp)
    }
}

/** Карточка: surface, r16, тень sm. */
@Composable
internal fun VuCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val t = vuColors()
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.fillMaxWidth().shadow(1.dp, shape).background(t.surface, shape)) { content() }
}

/** Подпись «тег» (ЗАГЛАВНЫЕ, mono, muted). */
@Composable
internal fun VuTag(text: String, color: Color? = null) {
    val t = vuColors()
    Text(text.uppercase(), fontFamily = VuMono, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium, color = color ?: t.muted)
}

/** Подсказка-абзац (13, muted). */
@Composable
internal fun VuHint(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    val t = vuColors()
    Text(InstructionRef.annotate(text), modifier = modifier, fontSize = 13.sp, lineHeight = 18.sp, color = color ?: t.muted)
}

/** Нижняя шторка M3 (r28, грэббер, заголовок 22/500, подзаголовок, кнопка «закрыть», опциональный footer). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VuSheet(
    title: String,
    onDismiss: () -> Unit,
    sub: String? = null,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val t = vuColors()
    val sheetKeypad = remember { VuKeypadState() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = t.bg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
      androidx.compose.runtime.CompositionLocalProvider(LocalVuKeypad provides sheetKeypad) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(top = 4.dp)) {
                    Text(title, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = t.text, lineHeight = 26.sp)
                    if (sub != null) Text(sub, fontSize = 13.sp, color = t.muted, modifier = Modifier.padding(top = 4.dp))
                }
                Box(Modifier.size(48.dp).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                    Text("✕", fontSize = 18.sp, color = t.muted)
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .separateSheetFromScroll()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
            ) { content() }
            if (footer != null) Box(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp)) { footer() }
            VuKeypadPanel(sheetKeypad)
        }
      }
    }
}

// ───────────────────────── Лист-бланк ─────────────────────────

/** Лист: surface, рамка 1 borderStrong, r6, тень md. */
@Composable
internal fun VuPaper(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val t = vuColors()
    val shape = RoundedCornerShape(6.dp)
    Column(modifier.fillMaxWidth().shadow(3.dp, shape).background(t.surface, shape).border(1.dp, t.borderStrong, shape)) { content() }
}

/** Печатный текст бланка (sans 14). */
@Composable
internal fun VuPrint(text: String, size: Int = 14, bold: Boolean = false, muted: Boolean = false, modifier: Modifier = Modifier) {
    val t = vuColors()
    Text(text, modifier = modifier, fontSize = size.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = if (muted) t.muted else t.text)
}

/**
 * Линейка «____» со вписанным значением: «чернила» - mono 17/600 accent (вписанное), mono 17/700 text (расчётное).
 * [onChange] == null - только показ. Цель нажатия - вся строка (min h30; строка бланка h48).
 */
@Composable
internal fun VuInk(
    value: String,
    modifier: Modifier = Modifier,
    onChange: ((String) -> Unit)? = null,
    calc: Boolean = false,
    placeholder: String = "",
    align: TextAlign = TextAlign.Start,
    keyboard: KeyboardType = KeyboardType.Text,
    maxLen: Int = 40,
) {
    val t = vuColors()
    var focused by remember { mutableStateOf(false) }
    val ink = TextStyle(fontFamily = VuMono, fontSize = 17.sp, fontWeight = if (calc) FontWeight.Bold else FontWeight.SemiBold, color = if (calc) t.text else t.accent, textAlign = align)
    val numeric = onChange != null && (keyboard == KeyboardType.Number || keyboard == KeyboardType.Decimal)
    val numHandle = if (numeric) rememberVuNumeric("Значение", value, { onChange?.invoke(it) }, decimal = keyboard == KeyboardType.Decimal, maxLen = maxLen) else null
    val active = focused || numHandle?.active == true
    Box(
        modifier = modifier
            .heightIn(min = 30.dp)
            .background(if (active) t.accentSoft else Color.Transparent)
            .drawBehind {
                val w = 1.5.dp.toPx()
                drawLine(if (active) t.accent else t.faint, Offset(0f, size.height - w / 2), Offset(size.width, size.height - w / 2), w)
            }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (onChange == null) {
            Text(value.ifEmpty { placeholder }, style = ink.copy(color = if (value.isEmpty()) t.faint else ink.color, fontSize = if (value.isEmpty()) 13.sp else 17.sp, fontWeight = if (value.isEmpty()) FontWeight.Normal else ink.fontWeight), modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else if (numHandle != null) {
            Box(Modifier.fillMaxWidth().then(numHandle.modifier), contentAlignment = if (align == TextAlign.End) Alignment.BottomEnd else Alignment.BottomStart) {
                if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, fontSize = 13.sp, color = t.faint, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(value, style = ink, maxLines = 1)
                    if (numHandle.active) Box(Modifier.width(2.dp).height(18.dp).background(t.accent))
                }
            }
        } else {
            BasicTextField(
                value = value,
                onValueChange = { raw -> onChange(raw.take(maxLen)) },
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                singleLine = true,
                textStyle = ink,
                cursorBrush = SolidColor(t.accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.BottomStart) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, fontSize = 13.sp, color = t.faint, maxLines = 1)
                        inner()
                    }
                },
            )
        }
    }
}

/** Строка бланка: min h48, отступы 14, элементы по нижней линии. */
@Composable
internal fun VuFillRow(modifier: Modifier = Modifier, end: Boolean = false, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = if (end) Arrangement.spacedBy(6.dp, Alignment.End) else Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
internal fun VuMiniHelp(onClick: () -> Unit) {
    val t = vuColors()
    Box(Modifier.padding(bottom = 4.dp).size(26.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Box(Modifier.size(18.dp).border(1.dp, t.borderStrong, CircleShape), contentAlignment = Alignment.Center) {
            Text("?", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = t.muted)
        }
    }
}

/** Стиль «обведено» (выбор из напечатанных вариантов): кольцо 2dp accent у выбранного. */
@Composable
internal fun VuCircled(items: List<String>, on: Set<String>, onToggle: (String) -> Unit) {
    val t = vuColors()
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { x ->
            val s = x in on
            Box(
                modifier = Modifier
                    .widthIn(min = 48.dp)
                    .height(48.dp)
                    .border(BorderStroke(2.dp, if (s) t.accent else t.border), RoundedCornerShape(24.dp))
                    .clickable { onToggle(x) }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(x, fontFamily = VuMono, fontSize = 15.sp, fontWeight = if (s) FontWeight.Bold else FontWeight.Medium, color = if (s) t.accent else t.text)
            }
        }
    }
}

/** Вертикальная/горизонтальная линия ячейки таблицы: правая и нижняя границы. */
internal fun Modifier.vuCellBorders(color: Color, right: Boolean = true, bottom: Boolean = true): Modifier = drawBehind {
    val w = 1.dp.toPx()
    if (right) drawLine(color, Offset(size.width - w / 2, 0f), Offset(size.width - w / 2, size.height), w)
    if (bottom) drawLine(color, Offset(0f, size.height - w / 2), Offset(size.width, size.height - w / 2), w)
}

@Composable
internal fun VuSpacer(h: Dp) = Spacer(Modifier.height(h))

@Composable
internal fun VuRowDivider() {
    val t = vuColors()
    Box(Modifier.fillMaxWidth().height(1.dp).background(t.border))
}

@Suppress("unused")
private val keepWidth = Modifier.width(0.dp)
