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
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val buttonWidth = 96.dp
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
        // Фон: красная кнопка «Удалить» справа
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(6.dp),
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
                .pointerInput(revealed) {
                    if (revealed) {
                        // В раскрытом состоянии тап по карточке — закрыть (вернуть на место)
                        detectTapGestures(onTap = { close() })
                    }
                }
        ) {
            content()
        }
    }
}
