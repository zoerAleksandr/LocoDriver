package com.z_company.route.component

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.z_company.core.util.isOnline
import com.z_company.core.util.onlineFlow

/** Тексты диалога «Нет интернета» (SCREEN_SPECS §6.9) - единые для всех функций, которым нужна сеть. */
object NetworkRequired {
    const val TITLE = "Нет интернета"
    const val TEXT = "Эта функция работает при наличии интернета. Проверьте подключение и повторите."
    const val CONFIRM = "Понятно"
}

/** Результат [rememberNetworkGate]: [run] выполняет действие при наличии сети, иначе показывает диалог. */
class NetworkGate internal constructor(
    private val context: Context,
    private val showDialog: () -> Unit,
) {
    /** Выполнить [action], если есть интернет; иначе показать диалог «Нет интернета» и вернуть false. */
    fun run(action: () -> Unit): Boolean {
        if (isOnline(context)) {
            action()
            return true
        }
        showDialog()
        return false
    }
}

/**
 * Общий хелпер «функция требует интернет»: `val gate = rememberNetworkGate(); gate.run { ... }`.
 * Диалог ([AppAlertDialog] по DIALOGS_STANDARD) рисуется самим хелпером. Этап 5 (планирование прибытия по тапу)
 * использует тот же хелпер.
 */
@Composable
fun rememberNetworkGate(): NetworkGate {
    val context = LocalContext.current
    var show by remember { mutableStateOf(false) }
    if (show) NetworkRequiredDialog(onDismiss = { show = false })
    return remember(context) { NetworkGate(context) { show = true } }
}

/** Диалог «Нет интернета» с одной кнопкой «Понятно». */
@Composable
fun NetworkRequiredDialog(onDismiss: () -> Unit) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = NetworkRequired.TITLE,
        text = NetworkRequired.TEXT,
        confirmText = NetworkRequired.CONFIRM,
        onConfirm = onDismiss,
    )
}

/** Текущее состояние связи (обновляется при смене сети). */
@Composable
fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    val online by produceState(initialValue = isOnline(context), context) {
        onlineFlow(context).collect { value = it }
    }
    return online
}
