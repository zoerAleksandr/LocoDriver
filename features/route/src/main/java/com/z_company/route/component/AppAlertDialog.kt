package com.z_company.route.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Единый диалог подтверждения по стандарту `docs/DIALOGS_STANDARD.md`
 * (Android · Material 3). Только короткое подтверждение или деструктивное
 * действие (≤ 2 кнопки, без полей ввода). Для ввода/выбора — нижняя шторка.
 *
 * Контейнер: surface, радиус 28dp, паддинг 24dp.
 * Заголовок: 22/600 (text). Текст: 14/1.45 (textMuted).
 * Кнопки (текстовые, прижаты вправо, «отмена» слева от основного действия):
 *  - deструктивное действие → [confirm] = danger, weight 700; [dismiss] = accent, 600;
 *  - обычное действие → [confirm] = accent, 600; [dismiss] = серый (textMuted), 500.
 *
 * Токены темы: accent = tertiary, danger = error, text = primary,
 * textMuted = onSurfaceVariant, surface = surface.
 */
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String? = null,
    confirmText: String,
    onConfirm: () -> Unit,
    isDestructive: Boolean = false,
    dismissText: String? = null,
    onDismiss: () -> Unit = onDismissRequest,
    // произвольное тело вместо простого текста (например, список строк) —
    // остаётся в рамках «без полей ввода».
    content: (@Composable () -> Unit)? = null,
) {
    // Текст/контент ограничен по высоте и скроллится: при длинном списке (как в
    // подтверждении массового удаления маршрутов) или увеличенном системном
    // шрифте контент не должен обрезаться или выталкивать кнопки за экран.
    val bodyMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.4f).dp
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.primary,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.W600)
        },
        text = when {
            content != null -> {
                {
                    Column(
                        modifier = Modifier
                            .heightIn(max = bodyMaxHeight)
                            .verticalScroll(rememberScrollState())
                    ) { content() }
                }
            }
            text != null -> {
                {
                    Text(
                        text = text,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier
                            .heightIn(max = bodyMaxHeight)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
            else -> null
        },
        confirmButton = {
            TextButton(onClick = onConfirm, shape = RoundedCornerShape(20.dp)) {
                Text(
                    text = confirmText,
                    fontSize = 14.sp,
                    fontWeight = if (isDestructive) FontWeight.W700 else FontWeight.W600,
                    color = if (isDestructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.tertiary
                )
            }
        },
        dismissButton = dismissText?.let {
            {
                TextButton(onClick = onDismiss, shape = RoundedCornerShape(20.dp)) {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        // При деструктиве «Отмена» — синяя (accent); иначе — серая.
                        fontWeight = if (isDestructive) FontWeight.W600 else FontWeight.W500,
                        color = if (isDestructive) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}
