package com.z_company.route.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
 * Иконка справки ВУ-45: буквы «ВУ-45» жирным моно-шрифтом приложения (без растровых ресурсов и новых шрифтов).
 * Размер текста подбирается под размер иконки [size]; не масштабируется системным размером шрифта, чтобы читаться в иконке.
 */
@Composable
fun Vu45Icon(modifier: Modifier = Modifier, size: Dp = 24.dp, tint: Color = MaterialTheme.colorScheme.primary) {
    val fontSize: TextUnit = (size.value * 0.27f).sp
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        androidx.compose.material3.Text(
            text = "ВУ-45", maxLines = 1, softWrap = false, textAlign = TextAlign.Center,
            style = TextStyle(fontFamily = MonoFont, fontWeight = FontWeight.ExtraBold, fontSize = fontSize, color = tint, letterSpacing = (-0.5).sp),
        )
    }
}
