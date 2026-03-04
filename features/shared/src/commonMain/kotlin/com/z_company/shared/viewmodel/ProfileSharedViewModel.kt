package com.z_company.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.shared.platform.AuthProvider
import com.z_company.shared.platform.AuthUiState
import com.z_company.shared.platform.PlatformActions
import com.z_company.shared.platform.SyncUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared KMP ViewModel for the profile screen.
 *
 * Delegates authentication, sync, and subscription to [AuthProvider].
 * Loads local route stats from [RouteUseCase].
 */
class ProfileSharedViewModel(
    private val routeUseCase: RouteUseCase,
    private val platformActions: PlatformActions,
    private val authProvider: AuthProvider,
) : ViewModel() {

    // --- Auth state (delegated to AuthProvider) ---
    val isLoggedIn: StateFlow<Boolean> = authProvider.isLoggedIn
    val authState: StateFlow<AuthUiState> = authProvider.authState
    val userEmail: StateFlow<String?> = authProvider.userEmail
    val hasSubscription: StateFlow<Boolean> = authProvider.hasSubscription
    val subscriptionEndTime: StateFlow<Long> = authProvider.subscriptionEndTime
    val vkUserName: StateFlow<String?> = authProvider.vkUserName
    val isVkAttached: StateFlow<Boolean> = authProvider.isVkAttached
    val syncState: StateFlow<SyncUiState> = authProvider.syncState

    // --- Local route stats ---
    private val _totalRouteCount = MutableStateFlow(0)
    val totalRouteCount: StateFlow<Int> = _totalRouteCount.asStateFlow()

    private val _favoritesCount = MutableStateFlow(0)
    val favoritesCount: StateFlow<Int> = _favoritesCount.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            routeUseCase.getListRoutesAsFlow().collect { routes ->
                val active = routes.filter { !it.basicData.isDeleted }
                _totalRouteCount.value = active.size
                _favoritesCount.value = active.count { it.basicData.isFavorite }
                _isLoading.value = false
            }
        }
    }

    // --- Auth actions ---

    fun authWithEmail(email: String, password: String) = authProvider.authWithEmail(email, password)

    fun registerWithEmail(email: String, password: String) = authProvider.registerWithEmail(email, password)

    fun authWithVkId(vkId: String) = authProvider.authWithVkId(vkId)

    fun logout() = authProvider.logout()

    fun refresh() = authProvider.refresh()

    fun updateEmail(newEmail: String) = authProvider.updateEmail(newEmail)

    fun forgotPassword(email: String) = authProvider.forgotPassword(email)

    fun attachVkId(vkId: String) = authProvider.attachVkId(vkId)

    fun removeVkId() = authProvider.removeVkId()

    // --- Sync actions ---

    fun startSyncUpload() = authProvider.startSyncUpload()

    fun startSyncDownload() = authProvider.startSyncDownload()

    fun restoreSubscription() = authProvider.restoreSubscription()

    // --- Platform ---

    fun openUrl(url: String) = platformActions.openUrl(url)

    fun getAppVersion(): String = platformActions.getAppVersion()
}
