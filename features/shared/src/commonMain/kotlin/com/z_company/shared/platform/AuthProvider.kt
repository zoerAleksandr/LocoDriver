package com.z_company.shared.platform

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-specific authentication and profile provider.
 *
 * Android: email/password + VK ID SDK, cloud sync via SyncManager.
 * iOS: email/password only (VK ID not available), same cloud sync.
 */
interface AuthProvider {
    /** Whether user is currently logged in (has valid token). */
    val isLoggedIn: StateFlow<Boolean>

    /** Current auth operation state (login/register/loading/error). */
    val authState: StateFlow<AuthUiState>

    /** User email from server profile. */
    val userEmail: StateFlow<String?>

    /** Whether user has active subscription. */
    val hasSubscription: StateFlow<Boolean>

    /** Subscription end time in epoch millis. 0 if none. */
    val subscriptionEndTime: StateFlow<Long>

    /** VK user display name. Always null on iOS. */
    val vkUserName: StateFlow<String?>

    /** Whether VK ID is attached to current account. */
    val isVkAttached: StateFlow<Boolean>

    /** Current sync operation state. */
    val syncState: StateFlow<SyncUiState>

    /** User ID from secure storage. */
    val userId: StateFlow<String?>

    // --- Auth methods ---

    fun authWithEmail(email: String, password: String)
    fun registerWithEmail(email: String, password: String)
    fun authWithVkId(vkId: String) {} // default no-op for iOS
    fun logout()
    fun refresh()

    // --- Profile methods ---

    fun updateEmail(newEmail: String)
    fun forgotPassword(email: String)

    // --- VK ID methods (no-op on iOS) ---

    fun attachVkId(vkId: String) {}
    fun removeVkId() {}

    // --- Sync methods ---

    fun startSyncUpload()
    fun startSyncDownload()

    // --- Subscription ---

    fun restoreSubscription()
}

// --- UI State models ---

sealed class AuthUiState {
    data object Initial : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val message: String? = null) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

sealed class SyncUiState {
    data object Idle : SyncUiState()
    data class InProgress(val message: String = "") : SyncUiState()
    data class Complete(val message: String = "") : SyncUiState()
    data class Error(val message: String) : SyncUiState()
}

/**
 * Factory function. Each platform provides its own implementation.
 */
expect fun createAuthProvider(): AuthProvider
