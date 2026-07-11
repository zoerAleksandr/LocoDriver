package com.z_company.route.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import com.z_company.route.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Свайп секции влево раскрывает красную кнопку «Удалить» и фиксирует карточку
 * в сдвинутом положении. Карточка не удаляется автоматически: ждём нажатия на
 * кнопку (тогда вызывается [onDeleteClick] — показывается подтверждение).
 * Тап по самой карточке в раскрытом состоянии возвращает её на место.
 */
@Composable
fun SwipeToRevealDelete(
    modifier: Modifier = Modifier,
    onDeleteClick: () -> Unit,
    closeSignal: Int = 0,
    // compact — для невысоких строк (таймлайн станций): только иконка без подписи,
    // чтобы контент не обрезался по высоте.
    compact: Boolean = false,
    // Клик / долгое нажатие по контенту. Если заданы — обрабатываются здесь же,
    // в одном pointerInput со свайпом, чтобы не конфликтовать с горизонтальным
    // драгом (иначе внешний clickable «съедает» жест и свайп не срабатывает).
    // Контент в этом случае НЕ должен иметь собственный clickable.
    onContentClick: (() -> Unit)? = null,
    onContentLongClick: (() -> Unit)? = null,
    // Вертикальный отступ красного фона от краёв карточки. По умолчанию 6dp
    // (карточки секций имеют свой внешний отступ). Для карточек без отступа —
    // передать 0.dp, чтобы фон «Удалить» был ровно высотой карточки.
    backgroundVerticalPadding: androidx.compose.ui.unit.Dp = 6.dp,
    // content получает флаг revealed — на случай если контенту нужно знать состояние.
    content: @Composable (revealed: Boolean) -> Unit,
) {
    val density = LocalDensity.current
    val buttonWidth = if (compact) 72.dp else 96.dp
    val buttonWidthPx = with(density) { buttonWidth.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var revealed by remember { mutableStateOf(false) }

    fun close() {
        revealed = false
        scope.launch { offsetX.animateTo(0f) }
    }

    // Внешний сигнал (например, отмена в шторке подтверждения) — вернуть карточку на место
    androidx.compose.runtime.LaunchedEffect(closeSignal) {
        if (offsetX.value != 0f) close()
    }

    fun open() {
        revealed = true
        scope.launch { offsetX.animateTo(-buttonWidthPx) }
    }

    Box(modifier = modifier) {
        // Фон: красная кнопка «Удалить» справа.
        // compact — фон занимает всю высоту карточки (без вертикальных отступов).
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(
                    horizontal = if (compact) 0.dp else 6.dp,
                    vertical = if (compact) 0.dp else backgroundVerticalPadding,
                ),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .width(buttonWidth)
                    .fillMaxHeight()
                    // Скругляем только со стороны края экрана (правую), к карточке — встык
                    .clip(
                        androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 0.dp, bottomStart = 0.dp,
                            topEnd = 16.dp, bottomEnd = 16.dp
                        )
                    )
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                    .clickable(enabled = revealed) {
                        onDeleteClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (compact) {
                    Icon(
                        painter = painterResource(R.drawable.delete_24px),
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.delete_24px),
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "УДАЛИТЬ",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.W700),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // Передний план: карточка секции, сдвигается по offsetX
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newX = (offsetX.value + dragAmount).coerceIn(-buttonWidthPx, 0f)
                            scope.launch { offsetX.snapTo(newX) }
                        },
                        onDragEnd = {
                            if (offsetX.value < -buttonWidthPx / 2f) open() else close()
                        }
                    )
                }
                .pointerInput(revealed, onContentClick, onContentLongClick) {
                    detectTapGestures(
                        onTap = {
                            // В раскрытом состоянии тап по контенту — закрыть; иначе — клик
                            if (revealed) close() else onContentClick?.invoke()
                        },
                        onLongPress = {
                            if (!revealed) onContentLongClick?.invoke()
                        }
                    )
                }
        ) {
            content(revealed)
        }
    }
}
