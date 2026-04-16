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

    override fun show(
        message: String,
        actionLabel: String?,
        duration: SnackbarDuration,
        onAction: (suspend () -> Unit)?,
        showOnceKey: String?
    ) {
        if (showOnceKey != null) {
            synchronized(shownKeys) {
                if (shownKeys.contains(showOnceKey)) return
                shownKeys.add(showOnceKey)
            }
        }

        _channel.trySend(
            SnackbarEvent(
                message = message,
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
}
