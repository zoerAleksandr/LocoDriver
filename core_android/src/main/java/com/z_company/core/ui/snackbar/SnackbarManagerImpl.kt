package com.z_company.core.ui.snackbar

import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex

class SnackbarManagerImpl : ISnackbarManager {
    // Channel вместо SharedFlow: буферизует события независимо от наличия подписчика.
    // SharedFlow(replay=0) терял события, отправленные до того как HomeScreen
    // успевал подписаться после навигации (STOPPED → STARTED переход).
    private val _channel = Channel<SnackbarEvent>(capacity = 64)
    override val events: Flow<SnackbarEvent> = _channel.receiveAsFlow()

    // keys that already shown in this process
    private val shownKeys = mutableSetOf<String>()
    private val mutex = Mutex()

    // Последнее показанное сообщение — чтобы не плодить одинаковые тосты (например, «Нет сети»
    // при синхронизации нескольких маршрутов подряд без интернета).
    private var lastMessage: String? = null
    private var lastMessageAt = 0L

    override fun show(
        message: String,
        actionLabel: String?,
        duration: SnackbarDuration,
        onAction: (suspend () -> Unit)?,
        showOnceKey: String?
    ) {
        // Любая сетевая ошибка → один короткий тост «Нет сети», без деталей и повторов.
        val text = if (isConnectivityText(message)) NO_NETWORK_MESSAGE else message
        // Повтор того же сообщения (без действия) в течение окна — отбрасываем.
        if (actionLabel == null) {
            synchronized(this) {
                val now = System.currentTimeMillis()
                if (text == lastMessage && now - lastMessageAt < DEDUP_WINDOW_MS) return
                lastMessage = text
                lastMessageAt = now
            }
        }
        if (showOnceKey != null) {
            synchronized(shownKeys) {
                if (shownKeys.contains(showOnceKey)) return
                shownKeys.add(showOnceKey)
            }
        }

        _channel.trySend(
            SnackbarEvent(
                message = text,
                actionLabel = actionLabel,
                duration = duration,
                onAction = onAction,
                showOnceKey = showOnceKey
            )
        )
    }

    override fun clearShownKeys() {
        synchronized(shownKeys) {
            shownKeys.clear()
        }
    }

    private fun isConnectivityText(message: String): Boolean {
        val m = message.lowercase()
        return CONNECTIVITY_MARKERS.any { m.contains(it) }
    }

    private companion object {
        const val NO_NETWORK_MESSAGE = "Нет сети"
        const val DEDUP_WINDOW_MS = 6_000L
        val CONNECTIVITY_MARKERS = listOf(
            "нет соединения", "нет интернета", "нет сети", "проверьте интернет", "проверьте подключение",
            "не удалось связаться", "превышено время ожидания", "unable to resolve", "unknownhost",
            "failed to connect", "network is unreachable", "connection refused", "timed out",
        )
    }
}
