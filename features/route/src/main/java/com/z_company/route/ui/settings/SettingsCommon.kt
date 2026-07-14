package com.z_company.route.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import com.z_company.route.component.OutlinedTextFieldApp

/**
 * Общие примитивы подэкранов настроек — единый стиль карточек, заголовков и
 * разделителей, как в справочниках (Серии/Станции/Плечи).
 */

// UPPERCASE-mono заголовок группы
@Composable
internal fun SettingsGroupHeader(text: String, top: Dp = 0.dp, startPad: Dp = 16.dp) {
    Text(
        modifier = Modifier.padding(start = startPad, top = top, bottom = 8.dp),
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// Карточка-группа: тень + secondary + 16r (тот же вид, что в справочниках).
@Composable
internal fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = Shapes.medium)
            .background(color = MaterialTheme.colorScheme.secondary, shape = Shapes.medium),
        content = content,
    )
}

// Разделитель строк — инсет слева 16, как в справочниках.
@Composable
internal fun SettingsRowDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
}

/* ─── Форм-примитивы в стиле «Настройки → Зарплата» (design/src/salary-settings.jsx) ───
 * Белая карточка с мягкой тенью, лейбл-подпись сверху, filled-поле bgSubtle снизу.
 * Используется на экранах-формах настроек (Норма/Регион и т.п.), чтобы визуально
 * совпадать с экраном ЗП. */

// Карточка-группа формы: secondary, мягкая тень, 16r, верхний отступ 12dp.
@Composable
internal fun SettingsFormCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .shadow(1.dp, Shapes.medium)
            .background(MaterialTheme.colorScheme.secondary, Shapes.medium),
        content = content,
    )
}

// Full-width разделитель строк внутри карточки формы.
@Composable
internal fun SettingsFormSep() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

// Пояснительная подпись ВНУТРИ карточки (как hint в Норма/Регион): bodySmall,
// приглушённый. Ставится последним элементом карточки.
@Composable
internal fun SettingsCardHint(text: String) {
    Text(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp),
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        maxLines = Int.MAX_VALUE,
        overflow = TextOverflow.Visible,
    )
}

// Слот поля формы: лейбл (+ опц. действие справа) сверху, контент снизу,
// затем опциональная пояснительная подпись.
@Composable
internal fun SettingsFormFieldSlot(
    label: String,
    action: (@Composable () -> Unit)? = null,
    hint: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            action?.invoke()
        }
        Spacer(Modifier.height(8.dp))
        content()
        if (hint != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = Int.MAX_VALUE,
                overflow = TextOverflow.Visible,
            )
        }
    }
}

// Filled read-only поле (bgSubtle, 12r, без тени) — статичное значение без стрелки.
@Composable
internal fun SettingsFilledReadonlyField(value: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    OutlinedTextFieldApp(
        modifier = modifier,
        value = value,
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(12.dp),
        fieldElevation = 0.dp,
        colorBackgroundEmptyField = MaterialTheme.colorScheme.surfaceBright,
        colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surfaceBright,
    )
}

// Пункт выпадающего списка формы.
internal data class SettingsDropdownOption(
    val text: String,
    val isSelected: Boolean = false,
    val onSelect: () -> Unit,
)

// Filled выпадающий список (bgSubtle, 12r, стрелка) в стиле полей ЗП.
// Пункты разделены линиями; выбранный подсвечен акцентом.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsFilledDropdown(
    value: String,
    options: List<SettingsDropdownOption>,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextFieldApp(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(12.dp),
            fieldElevation = 0.dp,
            colorBackgroundEmptyField = MaterialTheme.colorScheme.surfaceBright,
            colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surfaceBright,
        )
        ExposedDropdownMenu(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, option ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.text,
                            color = if (option.isSelected) {
                                MaterialTheme.colorScheme.tertiary
                            } else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    onClick = {
                        option.onSelect()
                        expanded = false
                    }
                )
            }
        }
    }
}
