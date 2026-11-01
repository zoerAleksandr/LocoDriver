package com.z_company.route.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Иконка «Закрепление поезда»: колесо на рельсе и тормозной башмак-клин под ободом. */
@Composable
fun HoldingIcon(modifier: Modifier = Modifier, size: Dp = 24.dp, tint: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val stroke = w * 0.075f
        val r = w * 0.30f
        val c = Offset(w * 0.46f, w * 0.40f)
        drawCircle(tint, radius = r, center = c, style = Stroke(stroke))
        drawCircle(tint, radius = r * 0.22f, center = c, style = Fill)
        // рельс
        drawLine(tint, Offset(w * 0.06f, w * 0.80f), Offset(w * 0.94f, w * 0.80f), strokeWidth = stroke)
        // башмак: клин под ободом
        val wedge = Path().apply {
            moveTo(c.x + r * 0.55f, c.y + r * 0.95f)
            lineTo(w * 0.90f, w * 0.80f - stroke / 2)
            lineTo(c.x + r * 0.30f, w * 0.80f - stroke / 2)
            close()
        }
        drawPath(wedge, tint, style = Fill)
    }
}
