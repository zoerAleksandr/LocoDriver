package com.z_company.core.ui.snackbar

import kotlinx.coroutines.flow.SharedFlow

interface ISnackbarManager {
    val events: SharedFlow<SnackbarEvent>
    fun show(
        message: String,
        actionLabel: String? = null,
        duration: androidx.compose.material3.SnackbarDuration = androidx.compose.material3.SnackbarDuration.Short,
        onAction: (suspend () -> Unit)? = null,
        showOnceKey: String? = null
    )

    /**
     * Clear shown keys (for example on logout).
     */
    fun clearShownKeys()
}