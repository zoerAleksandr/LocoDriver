package com.z_company.route.component.servicephase

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.MonoFont

/* ══════════════════════════════════════════════════════════════════════
 * Примитивы хендоффа design/service_phase_stations (pst-ui.jsx).
 *
 * Маппинг токенов дизайна на тему приложения (см. app/.../Theme.kt):
 *   bg → background, surface → secondary, text → primary,
 *   textMuted → onSurfaceVariant, textFaint → primary @ 38%,
 *   border → outlineVariant, borderStrong → outline, bgSubtle → surfaceBright,
 *   accent → tertiary, accentSoft → primaryContainer, accentInk/ctaInk → onPrimary,
 *   cta → primary, warning → surfaceContainerHigh, success → surfaceTint,
 *   danger → error, chipBgActive → surfaceDim.
 * ══════════════════════════════════════════════════════════════════════ */

internal object Pst {
    val bg: Color @Composable get() = MaterialTheme.colorScheme.background
    val surface: Color @Composable get() = MaterialTheme.colorScheme.secondary
    val text: Color @Composable get() = MaterialTheme.colorScheme.primary
    val textMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val textFaint: Color @Composable get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
    val border: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val borderStrong: Color @Composable get() = MaterialTheme.colorScheme.outline
    val bgSubtle: Color @Composable get() = MaterialTheme.colorScheme.surfaceBright
    val accent: Color @Composable get() = MaterialTheme.colorScheme.tertiary
    val accentSoft: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
    val onAccent: Color @Composable get() = MaterialTheme.colorScheme.onPrimary
    val cta: Color @Composable get() = MaterialTheme.colorScheme.primary
    val ctaInk: Color @Composable get() = MaterialTheme.colorScheme.onPrimary
    val warning: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
    val success: Color @Composable get() = MaterialTheme.colorScheme.surfaceTint
    val danger: Color @Composable get() = MaterialTheme.colorScheme.error
    val chipActive: Color @Composable get() = MaterialTheme.colorScheme.surfaceDim

    val sans: TextStyle @Composable get() = MaterialTheme.typography.bodyLarge
    val mono = TextStyle(fontFamily = MonoFont)
}

// ─── Форматы ───────────────────────────────────────────────────────────

/** «47,3 км»; null → «—». */
internal fun pstKm(km: Double?): String =
    if (km == null) "—" else "${((km * 10).let { kotlin.math.round(it) } / 10).toString().replace('.', ',')} км"

internal fun pstPlural(n: Int, one: String, few: String, many: String): String {
    val a = kotlin.math.abs(n) % 100
    val b = a % 10
    return when {
        a in 11..19 -> many
        b in 2..4 -> few
        b == 1 -> one
        else -> many
    }
}

internal fun stationsWord(n: Int) = pstPlural(n, "станцию", "станции", "станций")

// ─── Иконки (контурные, viewBox 24, stroke 2 — как icons.jsx) ───────────

private fun strokeIcon(name: String, vararg paths: String, filledDots: List<Triple<Float, Float, Float>> = emptyList()): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach { d ->
            addPath(
                pathData = addPathNodes(d),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        filledDots.forEach { (cx, cy, r) ->
            addPath(pathData = addPathNodes(circle(cx, cy, r)), fill = SolidColor(Color.Black))
        }
    }.build()

private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"

internal object PstIcons {
    val MapPin = strokeIcon("pin", "M12 22s8-7 8-13a8 8 0 0 0-16 0c0 6 8 13 8 13z", circle(12f, 9f, 3f))
    /** «Еду по карте»: навигационная стрелка. */
    val Navigate = strokeIcon("navigate", "M12 3l7 18-7-4-7 4z")
    val Search = strokeIcon("search", circle(11f, 11f, 7f), "M20 20l-4-4")
    val Plus = strokeIcon("plus", "M12 5v14M5 12h14")
    val Trash = strokeIcon(
        "trash",
        "M4 7h16M10 4h4a1 1 0 0 1 1 1v2H9V5a1 1 0 0 1 1-1z",
        "M6 7l1 13a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-13",
    )
    val Check = strokeIcon("check", "M5 12l5 5L20 7")
    val Info = strokeIcon("info", circle(12f, 12f, 9f), "M12 11v5", filledDots = listOf(Triple(12f, 7.7f, 0.9f)))
    val CloudOff = strokeIcon("cloudOff", "M7 18a4 4 0 0 1-1.5-7.7 5 5 0 0 1 8.5-2.8 4 4 0 0 1 3 6.3", "M3 3l18 18")
    val Shoulder = strokeIcon(
        "shoulder", "M6 18V8a3 3 0 0 1 6 0v8a3 3 0 0 0 6 0V6",
        filledDots = listOf(Triple(6f, 18.4f, 1.7f), Triple(18f, 5.6f, 1.7f)),
    )
    val Clock = strokeIcon("clock", circle(12f, 12f, 9f), "M12 7v5l3.5 2")
    val ChevronRight = strokeIcon("chevronRight", "M9 5l7 7-7 7")
    val ChevronLeft = strokeIcon("chevronLeft", "M15 5l-7 7 7 7")
    val ChevronUp = strokeIcon("chevronUp", "M5 15l7-7 7 7")
    val ChevronDown = strokeIcon("chevronDown", "M5 9l7 7 7-7")
    val Close = strokeIcon("close", "M6 6l12 12M6 18L18 6")
    val Drag = strokeIcon("drag", "M5 9h14M5 15h14")
    /** «Измерить по карте»: две точки на карте, соединённые путём. */
    val Measure = strokeIcon(
        "measure",
        "M6 21s-4-3.5-4-6.5a4 4 0 0 1 8 0c0 3-4 6.5-4 6.5z",
        "M18 12s-4-3.5-4-6.5a4 4 0 0 1 8 0c0 3-4 6.5-4 6.5z",
        "M6 21h9a3 3 0 0 0 3-3v-6",
        filledDots = listOf(Triple(6f, 14.5f, 1.3f), Triple(18f, 5.5f, 1.3f)),
    )
    /** «График скорости»: оси и ломаная линия. */
    val SpeedChart = strokeIcon("speedChart", "M4 4v16h16", "M7 15l4-5 3 3 5-7")
    /** «Зоны без GPS»: волны сигнала, перечёркнутые. */
    val GnssZones = strokeIcon("gnssZones", circle(12f, 12f, 2f), "M7.8 7.8a6 6 0 0 0 0 8.4M16.2 7.8a6 6 0 0 1 0 8.4", "M4 4l16 16")
    /** «Настройки карты»: три ползунка. */
    val Settings = strokeIcon(
        "settings", "M4 6h9M17 6h3M4 12h3M11 12h9M4 18h11M19 18h1",
        circle(15f, 6f, 2f), circle(9f, 12f, 2f), circle(17f, 18f, 2f),
    )
    /** «Развернуть на весь экран»: четыре угла. */
    val Expand = strokeIcon("expand", "M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5")
    /** «Сменить направление»: две стрелки навстречу. */
    val Swap = strokeIcon("swap", "M7 4L3 8l4 4M3 8h14M17 20l4-4-4-4M21 16H7")
    val Edit = strokeIcon("edit", "M4 20h4L18.5 9.5a2.12 2.12 0 0 0-3-3L5 17v3z", "M13.5 6.5l3 3")
}

@Composable
internal fun PstIcon(icon: ImageVector, size: Dp, tint: Color, modifier: Modifier = Modifier, description: String? = null) {
    Icon(imageVector = icon, contentDescription = description, tint = tint, modifier = modifier.size(size))
}

// ─── Кнопки ────────────────────────────────────────────────────────────

internal enum class PstButtonKind { FILLED, TONAL, OUTLINED, TEXT, CTA, DANGER }

@Composable
internal fun PstButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: PstButtonKind = PstButtonKind.FILLED,
    icon: ImageVector? = null,
    iconSize: Dp = 18.dp,
    enabled: Boolean = true,
    height: Dp = if (kind == PstButtonKind.CTA) 52.dp else 44.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = if (kind == PstButtonKind.TEXT) 12.dp else 20.dp),
    fontSize: Int = 15,
) {
    val (bg, fg) = when (kind) {
        PstButtonKind.FILLED -> Pst.accent to Pst.onAccent
        PstButtonKind.TONAL -> Pst.accentSoft to Pst.accent
        PstButtonKind.OUTLINED -> Color.Transparent to Pst.text
        PstButtonKind.TEXT -> Color.Transparent to Pst.accent
        PstButtonKind.CTA -> Pst.cta to Pst.ctaInk
        PstButtonKind.DANGER -> Color.Transparent to Pst.danger
    }
    val disabledBg = kind != PstButtonKind.TEXT && kind != PstButtonKind.DANGER && kind != PstButtonKind.OUTLINED
    val background = if (!enabled && disabledBg) Pst.bgSubtle else bg
    val content = if (!enabled) Pst.textFaint else fg
    // Как кнопки и карточки в остальном приложении (Shapes.medium = 16 dp); маленькие — 12 dp.
    val shape = RoundedCornerShape(if (height >= 44.dp) 16.dp else 12.dp)
    Row(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(background, shape)
            .then(
                if (kind == PstButtonKind.OUTLINED) {
                    Modifier.border(BorderStroke(1.dp, if (enabled) Pst.borderStrong else Pst.border), shape)
                } else Modifier
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) PstIcon(icon, iconSize, content)
        Text(
            text = text,
            color = content,
            style = Pst.sans.copy(fontSize = fontSize.sp, fontWeight = FontWeight.SemiBold),
            maxLines = 1,
        )
    }
}

// ─── Поле «лейбл сверху / значение» на surface ─────────────────────────

@Composable
internal fun PstTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    suffix: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    mono: Boolean = false,
    /** Ошибочное состояние: рамка и подпись цвета ошибки (контраст >= 3:1 к фону карточки в обеих темах), лёгкая заливка. */
    isError: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    val empty = value.text.isEmpty()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        interactionSource = interaction,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        cursorBrush = SolidColor(Pst.accent),
        textStyle = Pst.sans.copy(
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
            lineHeight = 20.sp,
            color = Pst.text,
            fontFamily = if (mono) MonoFont else Pst.sans.fontFamily,
        ),
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .then(if (focused) Modifier else Modifier.shadow(1.dp, shape))
                    .background(Pst.surface, shape)
                    .then(if (isError) Modifier.background(Pst.danger.copy(alpha = 0.10f), shape) else Modifier)
                    .border(2.dp, if (isError) Pst.danger else if (focused) Pst.accent else Color.Transparent, shape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (leadingIcon != null) {
                    PstIcon(leadingIcon, 18.dp, if (empty) Pst.textFaint else Pst.accent)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = label,
                        style = Pst.sans.copy(fontSize = 12.sp),
                        color = if (isError) Pst.danger else if (focused) Pst.accent else Pst.textMuted,
                    )
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.weight(1f, fill = false)) {
                            if (empty) {
                                Text(
                                    text = placeholder,
                                    style = Pst.sans.copy(fontSize = 16.sp, lineHeight = 20.sp),
                                    color = Pst.textFaint,
                                    maxLines = 1,
                                )
                            }
                            inner()
                        }
                        if (suffix != null && !empty) {
                            Text(text = suffix, style = Pst.sans.copy(fontSize = 13.sp), color = Pst.textMuted)
                        }
                    }
                }
                trailing?.invoke(this)
            }
        },
    )
}

// ─── Сегменты (2 варианта) ─────────────────────────────────────────────

@Composable
internal fun PstSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .border(1.dp, Pst.borderStrong, shape),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            if (i > 0) Box(Modifier.size(width = 1.dp, height = 40.dp).background(Pst.borderStrong))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(if (on) Pst.chipActive else Color.Transparent)
                    .clickable { onSelect(i) },
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (on) PstIcon(PstIcons.Check, 16.dp, Pst.text)
                Text(
                    text = label,
                    style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    color = Pst.text,
                )
            }
        }
    }
}

// ─── Чекбокс / радио ───────────────────────────────────────────────────

@Composable
internal fun PstCheckbox(checked: Boolean, disabled: Boolean = false) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(shape)
            .then(
                if (checked) Modifier.background(if (disabled) Pst.textFaint else Pst.accent, shape)
                else Modifier.border(2.dp, Pst.textMuted, shape)
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) PstIcon(PstIcons.Check, 14.dp, Pst.onAccent)
    }
}

@Composable
internal fun PstRadio(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .border(2.dp, if (selected) Pst.accent else Pst.borderStrong, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(10.dp).background(Pst.accent, CircleShape))
    }
}

// ─── Плашка (warning / accent) ─────────────────────────────────────────

@Composable
internal fun PstNotice(
    color: Color,
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PstIcon(PstIcons.Info, 18.dp, color, Modifier.padding(top = 1.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (title != null) {
                Text(
                    text = title,
                    style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    color = Pst.text,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Text(
                text = text,
                style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 19.sp),
                color = Pst.text.copy(alpha = 0.8f),
            )
            if (actionText != null && onAction != null) {
                PstButton(
                    text = actionText,
                    onClick = onAction,
                    kind = PstButtonKind.TEXT,
                    height = 34.dp,
                    modifier = Modifier.padding(top = 6.dp).padding(start = 0.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp),
                )
            }
        }
    }
}

// ─── Заголовок группы (mono UPPERCASE) и пояснение ─────────────────────

@Composable
internal fun PstGroupHead(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text.uppercase(),
            style = Pst.mono.copy(fontSize = 11.sp, letterSpacing = 1.4.sp),
            color = Pst.textMuted,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        action?.invoke()
    }
}

@Composable
internal fun PstNote(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 17.sp),
        color = Pst.textMuted,
        textAlign = align,
        modifier = modifier,
    )
}

/** Карточка на surface с радиусом 16 и тенью sm (MCard пакета). */
@Composable
internal fun PstCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(1.dp, shape)
            .background(Pst.surface, shape)
            .clip(shape),
    ) { content() }
}
