package com.z_company.route.autorecord

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.gps.GpsPermissions

/**
 * Возвращает обработчик переключателя «АВТО»: выключает сразу, а при включении сначала
 * запрашивает геолокацию (и уведомления на Android 13+). Подтверждения не нужно.
 */
@Composable
fun rememberAutoRecordToggle(manager: AutoRecordManager): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Отказ от уведомлений запись не блокирует; без геолокации — включать нечего.
        if (!manager.enable()) {
            Toast.makeText(context, "АВТО: нужен доступ к геолокации", Toast.LENGTH_LONG).show()
        }
    }
    return remember(manager) {
        {
            if (manager.state.value.enabled) {
                manager.disable()
            } else {
                val missing = manager.missingPermissions()
                if (missing.isEmpty()) manager.enable() else launcher.launch(missing)
                Unit
            }
        }
    }
}

/** Чип-переключатель «АВТО»: выключен — контурный, включён — залит, с пульсирующей точкой и «до чч:мм». */
@Composable
fun AutoRecordChip(
    state: AutoRecordState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    /** Подпись «до чч:мм» внутри чипа (на главном — в одну строку «АВТО · до чч:мм»). */
    singleLine: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val on = state.enabled
    val bg = if (on) scheme.primary else scheme.surface
    val fg = if (on) scheme.onPrimary else scheme.onSurfaceVariant
    val until = if (on) "до ${AutoRecordManager.formatTime(state.untilMs)}" else null
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = if (on) null else BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PulseDot(active = on, color = fg)
            if (singleLine || until == null) {
                Text(
                    text = if (singleLine && until != null) "АВТО · $until" else "АВТО",
                    color = fg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            } else {
                Column {
                    Text("АВТО", color = fg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(until, color = fg, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun PulseDot(active: Boolean, color: androidx.compose.ui.graphics.Color) {
    val alpha = if (active) {
        val t = rememberInfiniteTransition(label = "auto_pulse")
        val a by t.animateFloat(
            initialValue = 1f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
            label = "auto_pulse_alpha",
        )
        a
    } else 0.5f
    Box(
        Modifier
            .size(8.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color)
    )
}
