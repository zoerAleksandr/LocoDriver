package com.z_company.route.autorecord

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.route.component.AppAlertDialog

/**
 * Возвращает обработчик переключателя «АВТО»: выключает сразу, а при включении сначала
 * запрашивает геолокацию (и уведомления на Android 13+). Подтверждения не нужно. После включения
 * (один раз, а также после «прервано») показывает подсказку про исключение из оптимизации батареи.
 */
@Composable
fun rememberAutoRecordToggle(manager: AutoRecordManager, trainId: String? = null): () -> Unit {
    val currentTrainId by rememberUpdatedState(trainId)
    val context = LocalContext.current
    var showHelp by remember { mutableStateOf(false) }
    val afterEnable = {
        val wasInterrupted = manager.state.value.interrupted
        if (wasInterrupted || needsBatteryHelp(context)) showHelp = true
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Отказ от уведомлений запись не блокирует; без геолокации — включать нечего.
        if (manager.enable(currentTrainId)) afterEnable()
        else Toast.makeText(context, "АВТО: нужен доступ к геолокации", Toast.LENGTH_LONG).show()
    }
    if (showHelp) {
        markBatteryHelpShown(context)
        AppAlertDialog(
            onDismissRequest = { showHelp = false },
            title = "Чтобы АВТО не отключалось",
            text = "Android может останавливать фоновую запись ради экономии батареи. Разрешите LocoDriver " +
                "работать без ограничений батареи.\n\n" +
                "Xiaomi, Huawei, Samsung: дополнительно включите «Автозапуск» и выберите «Без ограничений» " +
                "в настройках батареи приложения (Настройки → Приложения → LocoDriver).",
            confirmText = "Разрешить",
            onConfirm = {
                showHelp = false
                openBatteryExemption(context)
            },
            dismissText = "Настройки приложения",
            onDismiss = {
                showHelp = false
                openAppSettings(context)
            },
        )
    }
    return remember(manager) {
        {
            if (manager.state.value.enabled) {
                manager.disable()
            } else {
                val missing = manager.missingPermissions()
                if (missing.isEmpty()) {
                    if (manager.enable(currentTrainId)) afterEnable()
                } else launcher.launch(missing)
                Unit
            }
        }
    }
}

private fun needsBatteryHelp(context: Context): Boolean {
    val prefs = context.getSharedPreferences(AutoRecordManager.PREFS_NAME, Context.MODE_PRIVATE)
    if (prefs.getBoolean(AutoRecordManager.KEY_BATTERY_ASKED, false)) return false
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return pm?.isIgnoringBatteryOptimizations(context.packageName) != true
}

private fun markBatteryHelpShown(context: Context) {
    context.getSharedPreferences(AutoRecordManager.PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putBoolean(AutoRecordManager.KEY_BATTERY_ASKED, true).apply()
}

private fun openBatteryExemption(context: Context) {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val intent = if (pm?.isIgnoringBatteryOptimizations(context.packageName) == true) {
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    } else {
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
    }
    runCatching { context.startActivity(intent) }.onFailure { openAppSettings(context) }
}

private fun openAppSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
        )
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
    val broken = state.interrupted && !on
    val bg = if (on) scheme.primary else scheme.surface
    val fg = when {
        on -> scheme.onPrimary
        broken -> scheme.error
        else -> scheme.onSurfaceVariant
    }
    val until = when {
        on -> "до ${AutoRecordManager.formatTime(LocalContext.current, state.untilMs)}"
        broken -> "прервано"
        else -> null
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = if (on) null else BorderStroke(1.dp, if (broken) scheme.error else scheme.outlineVariant),
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
