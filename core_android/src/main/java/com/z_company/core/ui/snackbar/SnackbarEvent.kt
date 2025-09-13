package com.z_company.core.ui.snackbar

import androidx.compose.material3.SnackbarDuration
import java.util.UUID

data class SnackbarEvent(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val actionLabel: String? = null,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    /**
     * Suspend action which will be launched from UI coroutine scope when user taps action button.
     * Keep it nullable so UI decides whether to execute it.
     */
    val onAction: (suspend () -> Unit)? = null,
    /**
     * If provided, event with the same showOnceKey will be shown only once per process (in-memory).
     * Use for messages like "Ищем вашу подписку..." that must not reappear on back navigation.
     */
    val showOnceKey: String? = null
)
