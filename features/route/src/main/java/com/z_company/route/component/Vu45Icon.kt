package com.z_company.route.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.MonoFont

/**
 * Иконка справки ВУ-45: буквы «ВУ-45» жирным моно-шрифтом приложения в прямоугольной рамке (скругление 3 dp, цвет иконки, 1,5-2 dp) (без растровых ресурсов и новых шрифтов).
 * Размер текста подбирается под размер иконки [size]; не масштабируется системным размером шрифта, чтобы читаться в иконке.
 */
@Composable
fun Vu45Icon(modifier: Modifier = Modifier, size: Dp = 24.dp, tint: Color = MaterialTheme.colorScheme.primary, duotone: Boolean = false) {
    val fontSize: TextUnit = (size.value * 0.29f).sp
    val stroke = if (size.value >= 48f) 2.dp else 1.5.dp
    Box(
        modifier.width(size).height(size * 0.64f)
            // двухтонный вариант (плитки «Инструменты»): бледная заливка под яркой рамкой и буквами
            .then(if (duotone) Modifier.background(tint.copy(alpha = 0.35f), RoundedCornerShape(3.dp)) else Modifier)
            .border(stroke, tint, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = "ВУ-45", maxLines = 1, softWrap = false, textAlign = TextAlign.Center,
            style = TextStyle(fontFamily = MonoFont, fontWeight = FontWeight.ExtraBold, fontSize = fontSize, color = tint, letterSpacing = (-0.5).sp),
        )
    }
}

/**
 * Вертикальный бланк ВУ-45 для плитки «Инструменты»: лист с яркой рамкой и бледной заливкой,
 * вверху «ВУ-45», ниже строки-заглушки (двухтонный стиль, как у остальных плиток).
 */
@Composable
fun Vu45BlankIcon(modifier: Modifier = Modifier, size: Dp = 72.dp, tint: Color = MaterialTheme.colorScheme.primary) {
    val shape = RoundedCornerShape(4.dp)
    androidx.compose.foundation.layout.Column(
        modifier
            .width(size * 0.72f).height(size)
            .background(tint.copy(alpha = 0.35f), shape)
            .border(2.dp, tint, shape)
            .padding(horizontal = size * 0.09f, vertical = size * 0.09f),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(size * 0.07f),
    ) {
        androidx.compose.material3.Text(
            text = "ВУ-45", maxLines = 1, softWrap = false, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(fontFamily = MonoFont, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.19f).sp, color = tint, letterSpacing = (-0.5).sp),
        )
        listOf(1f, 1f, 0.7f, 1f, 0.5f).forEach { w ->
            Box(Modifier.fillMaxWidth(w).height(size * 0.045f).background(tint, RoundedCornerShape(50)))
        }
    }
}
