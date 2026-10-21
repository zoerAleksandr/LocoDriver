package com.z_company.route.component.servicephase

import android.content.Context
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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

    private val _sensitivity = MutableStateFlow(LocationSensitivity.DEFAULT)
    private val _profileVisible = MutableStateFlow(true)
    private val _speedVisible = MutableStateFlow(true)
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
