package com.z_company.route.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes

/* ══════════════════════════════════════════════════════════════════════
 * Единый визуальный язык подэкранов Настроек — из хендоффа
 * design_handoff_settings_subscreens (README + settings-screens.jsx).
 *
 * Маппинг токенов дизайна на тему приложения:
 *   surface  → secondary,  text → primary,  textMuted → onSurfaceVariant,
 *   border   → outlineVariant,  borderStrong → outline,  accent → tertiary,
 *   bgSubtle → surfaceBright,  cardRadius 16 → Shapes.medium, input 12 → Shapes.small
 *
 * Строк ровно 4 типа: FieldRow, SelectRow, SwitchRow, RadioRow. Плюс
 * SelectTile (плитка-справочник) и SettingsPickerSheet (кастомный пикер).
 * ══════════════════════════════════════════════════════════════════════ */

// ─── Разделитель строк в карточке: во всю ширину (Android inset=0) ───
@Composable
internal fun SettingsCardSep() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

// ─── SectionNote — пояснение ПОД карточкой группы (sans 12, muted) ───
@Composable
internal fun SettingsSectionNote(text: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 8.dp),
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = Int.MAX_VALUE,
        overflow = TextOverflow.Visible,
    )
}

// ─── FieldRow — лейбл (muted) слева + значение (primary, mono/sans 17/600) справа ───
// Только чтение / открывает редактор (onClick). Без шеврона.
@Composable
internal fun SettingsFieldRow(
    label: String,
    value: String,
    mono: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (mono) MonoFont else null,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

// ─── SelectRow — лейбл (primary 15) + значение (muted 14, mono опц.) + шеврон ───
// Открывает пикер-шторку (onClick).
@Composable
internal fun SettingsSelectRow(
    label: String,
    value: String,
    sub: String? = null,
    mono: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (sub != null) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = if (mono) MonoFont else null,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_right_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp),
        )
    }
}

// ─── SwitchRow — лейбл (+ опц. sub) + Material-свитч (accent при on) ───
@Composable
internal fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    sub: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (sub != null) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = settingsSwitchColors(),
        )
    }
}

// Accent-свитч (track = tertiary при on) — как в дизайне.
@Composable
internal fun settingsSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
    checkedTrackColor = MaterialTheme.colorScheme.tertiary,
    checkedBorderColor = MaterialTheme.colorScheme.tertiary,
)

// ─── RadioRow — радио-кружок + лейбл (600 при selected) + опц. sub ───
@Composable
internal fun SettingsRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    sub: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsRadioCircle(selected = selected)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            if (sub != null) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsRadioCircle(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .border(
                width = 2.dp,
                color = if (selected) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape)
            )
        }
    }
}

// ─── SelectTile — плитка-справочник: surface + опц. эмодзи + значение + шеврон-вниз ───
// Открывает пикер-шторку. Аналог SelectField из хендоффа (Страна/Регион/Пояс).
@Composable
internal fun SettingsSelectTile(
    value: String,
    onClick: () -> Unit,
    leadingEmoji: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, Shapes.medium)
            .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
            .clickable { onClick() }
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingEmoji != null) {
            Text(text = leadingEmoji, fontSize = 19.sp)
            Spacer(Modifier.width(10.dp))
        }
        Text(
            modifier = Modifier.weight(1f),
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            painter = painterResource(com.z_company.route.R.drawable.keyboard_arrow_down_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

// ─── Пикер-шторка: список radio-строк (замена системным dropdown/диалогам) ───
internal data class SettingsPickerOption(
    val label: String,
    val sub: String? = null,
    val leadingEmoji: String? = null,
    val selected: Boolean = false,
    val onSelect: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsPickerSheet(
    title: String,
    options: List<SettingsPickerOption>,
    onDismiss: () -> Unit,
    hint: String? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.secondary,
        shape = Shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (hint != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            options.forEachIndexed { index, option ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = if (option.leadingEmoji != null) 38.dp else 0.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                SettingsPickerRow(option = option, onDismiss = onDismiss)
            }
        }
    }
}

@Composable
private fun SettingsPickerRow(option: SettingsPickerOption, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                option.onSelect()
                onDismiss()
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsRadioCircle(selected = option.selected)
        if (option.leadingEmoji != null) {
            Text(text = option.leadingEmoji, fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (option.selected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            if (option.sub != null) {
                Text(
                    text = option.sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
