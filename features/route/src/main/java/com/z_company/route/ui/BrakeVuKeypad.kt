package com.z_company.route.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.domain.util.sanitizeDecimalInput
import com.z_company.domain.util.sanitizeIntegerInput
import kotlinx.coroutines.delay

/**
 * Собственная цифровая клавиатура справки ВУ-45 (дизайн Claude Design, VuKeypad): цифры, «,», «⌫», «Готово».
 * Числовые поля справки не открывают системную клавиатуру (IME): значение набирается этой панелью либо физической клавиатурой
 * (цифры, запятая/точка, Backspace). Так «8,25» можно набрать в любом числовом поле независимо от раскладки и IME.
 * Источник истины поля - строка; десятичный разделитель всегда запятая (`sanitizeDecimalInput`).
 */
internal class VuKeypadState {
    var activeId by mutableStateOf<Any?>(null)
        private set
    var decimal by mutableStateOf(true)
        private set
    var label by mutableStateOf("")
        private set
    var current by mutableStateOf("")
        private set
    private var maxLen = 10
    private var apply: (String) -> Unit = {}

    fun activate(id: Any, decimal: Boolean, maxLen: Int, label: String, value: String, apply: (String) -> Unit) {
        activeId = id
        this.decimal = decimal
        this.maxLen = maxLen
        this.label = label
        this.current = value
        this.apply = apply
    }

    /** Поле сообщает актуальное значение (после пересчёта извне). */
    fun sync(id: Any, value: String, apply: (String) -> Unit) {
        if (activeId == id) {
            current = value
            this.apply = apply
        }
    }

    fun type(c: Char) {
        val raw = current + (if (c == '.') ',' else c)
        val next = if (decimal) raw.sanitizeDecimalInput(maxLen) else raw.sanitizeIntegerInput(maxLen)
        commit(next)
    }

    fun backspace() = commit(current.dropLast(1))
    fun clear() = commit("")
    fun close() { activeId = null }

    private fun commit(v: String) {
        current = v
        apply(v)
    }
}

internal val LocalVuKeypad = staticCompositionLocalOf<VuKeypadState?> { null }

internal class VuNumHandle(val modifier: Modifier, val active: Boolean)

/**
 * Делает обычный Box числовым полем без IME: тап активирует панель, физическая клавиатура вводит цифры, запятую/точку и Backspace.
 * [value] - строка поля (источник истины), [onChange] получает уже очищенную строку.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberVuNumeric(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    decimal: Boolean = true,
    maxLen: Int = 10,
): VuNumHandle {
    val kp = LocalVuKeypad.current
    val id = remember { Any() }
    val valueState: State<String> = rememberUpdatedState(value)
    val changeState = rememberUpdatedState(onChange)
    val requester = remember { FocusRequester() }
    val bring = remember { BringIntoViewRequester() }
    val active = kp != null && kp.activeId == id
    // синхронизация значения активного поля с панелью (в т.ч. после изменения извне)
    LaunchedEffect(value, active) { if (active) kp?.sync(id, value) { changeState.value(it) } }
    // при показе панели поле прокручивается в видимую область
    LaunchedEffect(active) {
        if (active) {
            delay(120)
            bring.bringIntoView()
        }
    }
    val activate = {
        requester.requestFocus()
        kp?.activate(id, decimal, maxLen, label, valueState.value) { changeState.value(it) }
        Unit
    }
    val mod = Modifier
        .bringIntoViewRequester(bring)
        .focusRequester(requester)
        .focusable()
        .onPreviewKeyEvent { ev ->
            if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val k = kp ?: return@onPreviewKeyEvent false
            if (k.activeId != id) k.activate(id, decimal, maxLen, label, valueState.value) { changeState.value(it) }
            val ch = keyToChar(ev.key)
            when {
                ch != null -> { k.type(ch); true }
                ev.key == Key.Backspace || ev.key == Key.Delete -> { k.backspace(); true }
                ev.key == Key.Enter || ev.key == Key.NumPadEnter -> { k.close(); true }
                else -> false
            }
        }
        .pointerInput(Unit) { detectTapGestures { activate() } }
    return VuNumHandle(mod, active)
}

private fun keyToChar(key: Key): Char? = when (key) {
    Key.Zero, Key.NumPad0 -> '0'
    Key.One, Key.NumPad1 -> '1'
    Key.Two, Key.NumPad2 -> '2'
    Key.Three, Key.NumPad3 -> '3'
    Key.Four, Key.NumPad4 -> '4'
    Key.Five, Key.NumPad5 -> '5'
    Key.Six, Key.NumPad6 -> '6'
    Key.Seven, Key.NumPad7 -> '7'
    Key.Eight, Key.NumPad8 -> '8'
    Key.Nine, Key.NumPad9 -> '9'
    Key.Comma, Key.Period, Key.NumPadComma, Key.NumPadDot -> ','
    else -> null
}

/** Панель клавиатуры; показывается, пока какое-либо числовое поле активно. */
@Composable
internal fun VuKeypadPanel(state: VuKeypadState, modifier: Modifier = Modifier) {
    if (state.activeId == null) return
    val t = vuColors()
    Column(
        modifier
            .fillMaxWidth()
            .background(t.bgSubtle)
            .navigationBarsPadding()
            .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(state.label, Modifier.weight(1f), fontSize = 12.sp, color = t.muted, maxLines = 1)
            Text(state.current.ifEmpty { "—" }, fontFamily = VuMono, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = t.accent)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("123", "456", "789").forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { c -> Key(c.toString(), Modifier.weight(1f)) { state.type(c) } }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Key(",", Modifier.weight(1f), enabled = state.decimal) { state.type(',') }
                    Key("0", Modifier.weight(1f)) { state.type('0') }
                    Key("C", Modifier.weight(1f)) { state.clear() }
                }
            }
            Column(Modifier.width(76.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Key("⌫", Modifier.fillMaxWidth()) { state.backspace() }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp * 3 + 12.dp)
                        .background(t.accent, RoundedCornerShape(10.dp))
                        .clickable { state.close() },
                    contentAlignment = Alignment.Center,
                ) { Text("Готово", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = t.accentInk) }
            }
        }
    }
}

@Composable
private fun Key(text: String, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val t = vuColors()
    Box(
        modifier
            .height(52.dp)
            .background(if (enabled) t.surface else t.surface.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontSize = 22.sp, fontFamily = VuMono, color = if (enabled) t.text else t.faint) }
}
