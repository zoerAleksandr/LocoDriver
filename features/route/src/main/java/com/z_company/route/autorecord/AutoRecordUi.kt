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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.domain.autorecord.AutoTargetRules
import com.z_company.route.component.AppAlertDialog
import kotlinx.coroutines.launch

/**
 * Возвращает обработчик переключателя «АВТО»: выключает сразу, а при включении сначала
 * запрашивает геолокацию (и уведомления на Android 13+). Подтверждения не нужно. После включения
 * (один раз, а также после «прервано») показывает подсказку про исключение из оптимизации батареи.
 */
@Composable
fun rememberAutoRecordToggle(
    manager: AutoRecordManager,
    trainId: String? = null,
    /**
     * Определяет поезд-цель в момент включения (кнопка записи на карте без поезда: создаёт маршрут/поезд);
     * null - берётся [trainId]. Вызывается только когда геолокация уже разрешена.
     */
    resolveTarget: (suspend () -> String?)? = null,
): () -> Unit {
    val currentTrainId by rememberUpdatedState(trainId)
    val currentResolver by rememberUpdatedState(resolveTarget)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    var showHelp by remember { mutableStateOf(false) }
    val afterEnable = {
        val wasInterrupted = manager.state.value.interrupted
        if (wasInterrupted || needsBatteryHelp(context)) showHelp = true
    }
    val enableNow: (onFail: () -> Unit) -> Unit = { onFail ->
        val resolver = currentResolver
        if (resolver == null) {
            if (manager.enable(currentTrainId)) afterEnable() else onFail()
        } else if (!com.z_company.core.gps.GpsPermissions.hasLocation(context)) {
            onFail()
        } else {
            scope.launch {
                val id = resolver() ?: currentTrainId
                if (manager.enable(id)) afterEnable() else onFail()
            }
        }
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Отказ от уведомлений запись не блокирует; без геолокации — включать нечего.
        enableNow({ Toast.makeText(context, "Автозапись: нужен доступ к геолокации", Toast.LENGTH_LONG).show() })
    }
    if (showHelp) {
        markBatteryHelpShown(context)
        AppAlertDialog(
            onDismissRequest = { showHelp = false },
            title = "Чтобы автозапись не отключалась",
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
    val consent: TrackConsentStore = org.koin.compose.koinInject()
    val startEnable = {
        val missing = manager.missingPermissions()
        if (missing.isEmpty()) {
            enableNow {}
        } else launcher.launch(missing)
        Unit
    }
    // Экран согласия №1 — при первом включении «АВТО» (SCREEN_SPECS §6.9).
    var showConsent by remember { mutableStateOf(false) }
    if (showConsent) {
        TrackConsentDialog(
            onContinue = { checked, shareAltitudes ->
                showConsent = false
                consent.setConsent(checked)
                if (shareAltitudes) consent.setAltitudeContrib(true)
                startEnable()
            },
            onDismiss = { showConsent = false },
        )
    }
    // «Единственное активное АВТО»: включение на другом поезде, пока пишется трек для поезда № X, - диалог.
    var conflictText by remember { mutableStateOf<String?>(null) }
    conflictText?.let { text ->
        AppAlertDialog(
            onDismissRequest = { conflictText = null },
            title = AutoTargetRules.CONFLICT_TITLE,
            text = text,
            confirmText = AutoTargetRules.CONFLICT_CONFIRM,
            onConfirm = {
                conflictText = null
                // Текущий трек завершается, следующая точка открывает новый трек для этого поезда.
                currentTrainId?.let { manager.setTarget(it) }
            },
            dismissText = AutoTargetRules.CONFLICT_DISMISS,
            onDismiss = { conflictText = null },
        )
    }
    return remember(manager) {
        {
            val st = manager.state.value
            if (AutoTargetRules.conflictsWith(st.enabled, manager.targetTrainId(), currentTrainId)) {
                scope.launch { conflictText = manager.conflictText() }
            } else if (st.enabled) {
                manager.disable()
            } else if (!consent.hasDecided()) {
                showConsent = true
            } else {
                startEnable()
            }
        }
    }
}

/**
 * Состояние «АВТО» для экрана поезда [trainId]: включённым показывается только у поезда, который пишется
 * (`target_train_id`); у остальных - выключенным. Без поезда ([trainId] == null) - общее состояние.
 */
@Composable
fun rememberAutoStateFor(manager: AutoRecordManager, trainId: String?): AutoRecordState {
    val state by manager.state.collectAsState()
    val target by manager.target.collectAsState()
    return if (state.enabled && !AutoTargetRules.isRecordingThis(true, target, trainId)) {
        state.copy(enabled = false, interrupted = false)
    } else state
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

/**
 * Цвета «АВТО» (SCREEN_SPECS §6.2 п.6а): включено — зелёный, выключено — серый, «прервано» — оранжевый.
 * Подобраны под WCAG: текст/иконка ≥ 4.5:1 к фону кнопки, контур/заливка ≥ 3:1 к фону экрана.
 */
@Immutable
data class AutoRecordColors(val container: Color, val content: Color, val border: Color?)

object AutoRecordStyle {
    /** Скругление — то же, что у кнопки «GO» (▶/⏸): штатная форма Material-кнопки. */
    val shape: Shape
        @Composable get() = ButtonDefaults.shape

    /** Высота — как у кнопки «GO» на экране «Поезд». */
    val height: Dp = ButtonDefaults.MinHeight

    @Composable
    fun colors(state: AutoRecordState): AutoRecordColors {
        val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        return when {
            state.enabled -> if (dark) AutoRecordColors(Color(0xFF4ADE80), Color(0xFF0A0E14), null)
            else AutoRecordColors(Color(0xFF15803D), Color(0xFFFFFFFF), null)
            state.interrupted -> if (dark) AutoRecordColors(Color(0xFF4A3410), Color(0xFFFFB547), Color(0xFFFFB547))
            else AutoRecordColors(Color(0xFFFFE8CC), Color(0xFF8A4B00), Color(0xFFC26A00))
            else -> if (dark) AutoRecordColors(Color(0xFF3A3C40), Color(0xFFC9CDD2), Color(0xFF8A929B))
            else AutoRecordColors(Color(0xFFE7E9EC), Color(0xFF4A515A), Color(0xFF7A828B))
        }
    }
}

/**
 * Чип-переключатель «АВТО»: включён — зелёный с пульсирующей точкой, выключен — серый, «прервано» —
 * оранжевый. Времени окончания на кнопке нет (оно только в уведомлении службы). Форма и высота — как у «GO».
 */
@Composable
fun AutoRecordChip(
    state: AutoRecordState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    /** Высота как у кнопки «GO» (экран «Поезд»); на главном — компактный чип. */
    goHeight: Boolean = false,
) {
    val colors = AutoRecordStyle.colors(state)
    val broken = state.interrupted && !state.enabled
    // Узкий экран (~343 dp): левая треть строки не вмещает «АВТО · прервано» в одну строку, текст обрезался.
    // Тогда «прервано» уходит на вторую строку мелким шрифтом (высота чипа та же).
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
        val twoLines = broken && maxWidth < 170.dp
        Surface(
            shape = AutoRecordStyle.shape,
            color = colors.container,
            border = colors.border?.let { BorderStroke(1.dp, it) },
        ) {
            Row(
                modifier = Modifier
                    .then(if (goHeight) Modifier.height(AutoRecordStyle.height) else Modifier)
                    .clickable(onClick = onToggle)
                    .padding(horizontal = if (twoLines) 10.dp else 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (twoLines) 6.dp else 8.dp),
            ) {
                PulseDot(active = state.enabled, color = colors.content)
                if (twoLines) {
                    Column {
                        Text(
                            text = "АВТОЗАПИСЬ",
                            color = colors.content,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            lineHeight = 13.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "прервано",
                            color = colors.content,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            lineHeight = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Text(
                        text = if (broken) "АВТОЗАПИСЬ · прервано" else "АВТОЗАПИСЬ",
                        color = colors.content,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PulseDot(active: Boolean, color: Color) {
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

/**
 * Плашка под «АВТО» (SCREEN_SPECS §6.2 п.6а): пока запись включена, а связь со спутниками плохая (слабый сигнал, нет
 * сигнала, помеха), об этом нужно знать и вне карты - иначе машинист рассчитывает на запись, которая идёт неточно или
 * приостановлена. При хорошем сигнале и «поиске спутников» (первый fix) плашки нет. Уровень уже сглажен
 * (`GnssQualityEvaluator`), поэтому плашка не мигает.
 */
@Composable
fun AutoGnssNotice(
    autoEnabled: Boolean,
    quality: com.z_company.domain.gnss.GnssQuality,
    modifier: Modifier = Modifier,
) {
    val level = quality.level
    val bad = autoEnabled && (level == com.z_company.domain.gnss.GnssLevel.WEAK ||
        level == com.z_company.domain.gnss.GnssLevel.LOST ||
        level == com.z_company.domain.gnss.GnssLevel.INTERFERENCE)
    androidx.compose.animation.AnimatedVisibility(visible = bad, modifier = modifier) {
        val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val accent = GnssSignalStyle.color(level, dark)
        val (title, hint) = when (level) {
            com.z_company.domain.gnss.GnssLevel.LOST ->
                "Нет сигнала GPS" to "Запись стоянок и отправлений приостановлена"
            com.z_company.domain.gnss.GnssLevel.INTERFERENCE ->
                "Помеха GPS" to "Координаты скачут, такие точки не учитываются"
            else -> "Слабый сигнал GPS" to "Стоянки и отправления могут записаться неточно"
        }
        Surface(
            modifier = Modifier.padding(top = 8.dp),
            shape = AutoRecordStyle.shape,
            color = accent.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
                Column {
                    Text(
                        text = title,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                    )
                    Text(
                        text = hint,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                    )
                }
            }
        }
    }
}
