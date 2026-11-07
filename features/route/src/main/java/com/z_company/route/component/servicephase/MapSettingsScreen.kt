package com.z_company.route.component.servicephase

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.z_company.domain.util.LocationSensitivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/** Личные настройки карты поезда: хранятся на устройстве (SharedPreferences), на сервер не уходят. */
internal object TrainMapPrefs {
    private const val PREFS = "train_map_prefs"
    private const val KEY_SENSITIVITY = "location_sensitivity"
    private const val KEY_PROFILE = "profile_visible"
    private const val KEY_SPEED = "speed_visible"
    private const val KEY_TRAIN_LINE = "train_as_line"

    private val _sensitivity = MutableStateFlow(LocationSensitivity.DEFAULT)
    private val _profileVisible = MutableStateFlow(true)
    private val _speedVisible = MutableStateFlow(true)
    private val _trainAsLine = MutableStateFlow(false)  // по умолчанию точка (решение владельца 07.11)
    private var loaded = false

    fun sensitivity(context: Context): StateFlow<Int> {
        ensureLoaded(context)
        return _sensitivity.asStateFlow()
    }

    fun setSensitivity(context: Context, level: Int) {
        ensureLoaded(context)
        val v = LocationSensitivity.clamp(level)
        runCatching { prefs(context).edit().putInt(KEY_SENSITIVITY, v).apply() }
        _sensitivity.value = v
    }

    /** Панель «Профиль пути» показана (по умолчанию да). */
    fun profileVisible(context: Context): StateFlow<Boolean> {
        ensureLoaded(context)
        return _profileVisible.asStateFlow()
    }

    fun setProfileVisible(context: Context, value: Boolean) {
        ensureLoaded(context)
        runCatching { prefs(context).edit().putBoolean(KEY_PROFILE, value).apply() }
        _profileVisible.value = value
    }

    /** Полоса скорости показана (по умолчанию да). */
    fun speedVisible(context: Context): StateFlow<Boolean> {
        ensureLoaded(context)
        return _speedVisible.asStateFlow()
    }

    fun setSpeedVisible(context: Context, value: Boolean) {
        ensureLoaded(context)
        runCatching { prefs(context).edit().putBoolean(KEY_SPEED, value).apply() }
        _speedVisible.value = value
    }

    /** Поезд на плане: линией (true, по умолчанию) или точкой со значком навигации (false). */
    fun trainAsLine(context: Context): StateFlow<Boolean> {
        ensureLoaded(context)
        return _trainAsLine.asStateFlow()
    }

    fun setTrainAsLine(context: Context, value: Boolean) {
        ensureLoaded(context)
        runCatching { prefs(context).edit().putBoolean(KEY_TRAIN_LINE, value).apply() }
        _trainAsLine.value = value
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    private fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        runCatching {
            val p = prefs(context)
            _sensitivity.value = LocationSensitivity.clamp(p.getInt(KEY_SENSITIVITY, LocationSensitivity.DEFAULT))
            _profileVisible.value = p.getBoolean(KEY_PROFILE, true)
            _speedVisible.value = p.getBoolean(KEY_SPEED, true)
            _trainAsLine.value = p.getBoolean(KEY_TRAIN_LINE, false)
        }
    }
}

/**
 * Экран «Настройки карты» (SCREEN_SPECS §6.9): значок положения, показ зон плохой связи, чувствительность локации.
 * В режиме просмотра записанного трека ([viewOnly]) остаются только зоны плохой связи.
 */
@Composable
internal fun MapSettingsScreen(
    viewOnly: Boolean,
    gnssOn: Boolean,
    onGnssChange: (Boolean) -> Unit,
    trainAsLine: Boolean,
    onTrainAsLine: (Boolean) -> Unit,
    positionIcon: PositionIcon,
    onPositionIcon: (PositionIcon) -> Unit,
    sensitivity: Int,
    onSensitivity: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(modifier = Modifier.fillMaxSize().background(Pst.bg).statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapButton(onClick = onDismiss, description = "Назад") { PstIcon(PstIcons.ChevronLeft, 22.dp, Pst.text) }
                Text(
                    text = "Настройки карты",
                    style = Pst.sans.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                    color = Pst.text,
                )
            }
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Зоны плохой связи",
                                style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                color = Pst.text,
                            )
                            Text(
                                text = "Красноватые пятна на карте там, где GPS пропадал или давал сбои.",
                                style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp),
                                color = Pst.textMuted,
                            )
                        }
                        Switch(
                            checked = gnssOn,
                            onCheckedChange = onGnssChange,
                            colors = SwitchDefaults.colors(checkedTrackColor = Pst.accent),
                        )
                    }
                }
                if (!viewOnly) {
                    SettingsCard {
                        Text(
                            text = "Поезд на плане",
                            style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            color = Pst.text,
                        )
                        Text(
                            text = if (trainAsLine) {
                                "Линия длиной с поезд вдоль участка. Если длина поезда не указана или участок не найден, показывается значок."
                            } else {
                                "Точка с выбранным ниже значком навигации."
                            },
                            style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp),
                            color = Pst.textMuted,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                            listOf(true to "Линией", false to "Точкой со значком").forEach { (line, label) ->
                                val on = line == trainAsLine
                                Text(
                                    text = label,
                                    style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                    color = if (on) Pst.onAccent else Pst.text,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (on) Pst.accent else Pst.bgSubtle, RoundedCornerShape(12.dp))
                                        .clickable(onClickLabel = label) { onTrainAsLine(line) }
                                        .padding(vertical = 10.dp),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    // Пример карты: линия поезда либо выбранный значок на пути; значок меняется сразу при выборе ниже.
                    SettingsCard {
                        Text(
                            text = "Так это будет выглядеть",
                            style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            color = Pst.text,
                        )
                        Text(
                            text = if (trainAsLine) {
                                "Синяя линия повторяет изгиб пути и идёт назад от положения поезда."
                            } else {
                                "Значок стоит на пути в положении поезда и поворачивается по ходу движения."
                            },
                            style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp),
                            color = Pst.textMuted,
                        )
                        val previewModifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(14.dp))
                        if (trainAsLine) TrainLinePreview(previewModifier) else TrainIconPreview(positionIcon, previewModifier)
                    }
                    // Выбор значка нужен только при показе поезда точкой: при линии значок на плане не используется.
                    if (!trainAsLine) {
                    SettingsCard {
                        Text(
                            text = "Значок навигации",
                            style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            color = Pst.text,
                        )
                        Text(
                            text = "Так вы выглядите на карте.",
                            style = Pst.sans.copy(fontSize = 12.sp),
                            color = Pst.textMuted,
                        )
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val gap = 8.dp
                            val cell = (maxWidth - gap * 2) / 3
                            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                                PositionIcon.entries.chunked(3).forEach { rowIcons ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                        rowIcons.forEach { ic ->
                                            val on = ic == positionIcon
                                            Column(
                                                modifier = Modifier
                                                    .width(cell)
                                                    .border(if (on) 2.dp else 0.dp, if (on) Pst.accent else Color.Transparent, RoundedCornerShape(14.dp))
                                                    .clickable(onClickLabel = ic.label) { onPositionIcon(ic) }
                                                    .padding(4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                            ) {
                                                PositionIconTile(ic, 64.dp)
                                                Text(
                                                    ic.label,
                                                    style = Pst.sans.copy(fontSize = 11.sp, lineHeight = 13.sp),
                                                    color = Pst.text,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 2,
                                                    modifier = Modifier.padding(top = 3.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                    SettingsCard {
                        // Во время перетаскивания показываем выбранный уровень, применяем и сохраняем - по отпусканию.
                        var drag by remember(sensitivity) { mutableStateOf(sensitivity.toFloat()) }
                        val level = LocationSensitivity.clamp(drag.roundToInt())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Чувствительность локации",
                                style = Pst.sans.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                color = Pst.text,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = LocationSensitivity.title(level),
                                style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                color = Pst.accent,
                            )
                        }
                        Slider(
                            value = drag,
                            onValueChange = { drag = it },
                            onValueChangeFinished = { onSensitivity(level) },
                            valueRange = LocationSensitivity.MIN.toFloat()..LocationSensitivity.MAX.toFloat(),
                            steps = LocationSensitivity.MAX - LocationSensitivity.MIN - 1,
                            colors = SliderDefaults.colors(thumbColor = Pst.accent, activeTrackColor = Pst.accent),
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Меньше шума", style = Pst.sans.copy(fontSize = 11.sp), color = Pst.textMuted)
                            Text("Быстрее реакция", style = Pst.sans.copy(fontSize = 11.sp), color = Pst.textMuted)
                        }
                        Text(
                            text = LocationSensitivity.description(level),
                            style = Pst.sans.copy(fontSize = 13.sp, lineHeight = 18.sp),
                            color = Pst.text,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            text = LocationSensitivity.GENERAL_NOTE,
                            style = Pst.sans.copy(fontSize = 12.sp, lineHeight = 16.sp),
                            color = Pst.textMuted,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Мини-карта для примеров в настройках: фон, зелёное пятно, изогнутый путь и станция. Рисуется на Canvas теми же цветами, что и
 * настоящая карта; возвращает путь, по которому рисуются линия поезда или значок.
 */
private fun DrawScope.drawPreviewMap(measurer: TextMeasurer, labelStyle: TextStyle): Path {
    val w = size.width
    val h = size.height
    val dp = density
    drawRect(Color(0xFFF7F4EF))
    // Зелёное пятно, как лес на карте.
    drawOval(Color(0xFFE3EED6), topLeft = Offset(w * 0.52f, h * 0.45f), size = Size(w * 0.6f, h * 0.9f))
    // Путь: плавный изгиб слева направо вверх.
    val rail = Path().apply {
        moveTo(-10f, h * 0.88f)
        cubicTo(w * 0.28f, h * 0.88f, w * 0.34f, h * 0.30f, w * 0.62f, h * 0.36f)
        cubicTo(w * 0.80f, h * 0.40f, w * 0.88f, h * 0.18f, w + 10f, h * 0.12f)
    }
    drawPath(rail, Color(0xFF5A6478), style = Stroke(width = 3f * dp, cap = StrokeCap.Round))
    // Станция.
    val st = Offset(w * 0.12f, h * 0.875f)
    drawCircle(Color(0xFF5A6478), radius = 5f * dp, center = st)
    drawText(measurer.measure("Станция", labelStyle), topLeft = Offset(st.x + 10f * dp, st.y + 2f * dp))
    return rail
}

/** Пример линии поезда на плане: синяя линия с белой окантовкой идёт по изгибу пути назад от положения поезда. */
@Composable
private fun TrainLinePreview(modifier: Modifier = Modifier) {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = Pst.sans.copy(fontSize = 11.sp, color = Color(0xFF3C4654))
    Canvas(modifier) {
        val rail = drawPreviewMap(measurer, labelStyle)
        val dp = density
        val pm = PathMeasure()
        pm.setPath(rail, false)
        val total = pm.length
        val train = Path()
        pm.getSegment(total * 0.30f, total * 0.66f, train, true)
        drawPath(train, Color.White.copy(alpha = 0.95f), style = Stroke(width = 9f * dp, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(train, Color(0xFF0A84FF), style = Stroke(width = 6f * dp, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Пример значка на плане: выбранный значок стоит на пути и повёрнут по ходу движения, как на настоящей карте (там же берётся
 * тот же растр вида сверху нужной высоты). Меняется сразу вместе с выбором в настройках.
 */
@Composable
private fun TrainIconPreview(icon: PositionIcon, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = Pst.sans.copy(fontSize = 11.sp, color = Color(0xFF3C4654))
    val bitmap = remember(icon, density) {
        renderPositionIcon(context, icon, (icon.mapHeightDp * density).toInt().coerceAtLeast(1)).asImageBitmap()
    }
    Canvas(modifier) {
        val rail = drawPreviewMap(measurer, labelStyle)
        val pm = PathMeasure()
        pm.setPath(rail, false)
        val at = pm.length * 0.52f
        val pos = pm.getPosition(at)
        val tan = pm.getTangent(at)
        // Значок нарисован носом вверх (курс 0 - на север): поворачиваем по касательной пути.
        val degrees = Math.toDegrees(kotlin.math.atan2(tan.x.toDouble(), -tan.y.toDouble())).toFloat()
        rotate(degrees, pivot = pos) {
            drawImage(bitmap, topLeft = Offset(pos.x - bitmap.width / 2f, pos.y - bitmap.height / 2f))
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .background(Pst.surface, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}
