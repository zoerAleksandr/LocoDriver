package com.z_company.core.ui.snackbar

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import androidx.compose.material3.SnackbarDuration

class SnackbarManagerImpl : ISnackbarManager {
    private val _events = MutableSharedFlow<SnackbarEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<SnackbarEvent> = _events

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
        // avoid duplicate showing for same key in-memory
        if (showOnceKey != null) {
            // mutex to avoid race on shownKeys
            var shouldEmit = false
            // fast-path check without lock
            if (!shownKeys.contains(showOnceKey)) {
                // lock and check
                // Note: this is not suspend; launch new coroutine for locking is unnecessary here;
                // We'll use tryEmit for fire-and-forget semantics. If two threads call nearly simultaneously,
                // one will add key first and next one will detect presence and skip.
                synchronized(shownKeys) {
                    if (!shownKeys.contains(showOnceKey)) {
                        shownKeys.add(showOnceKey)
                        shouldEmit = true
                    }
                }
            }
            if (!shouldEmit) return
        }

        _events.tryEmit(
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