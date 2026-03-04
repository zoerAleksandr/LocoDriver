package com.z_company.shared.platform

import com.z_company.core.ResultState
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.AuthManager
import com.z_company.repository.remote_rest.AuthState
import com.z_company.repository.remote_rest.GetUserProfileState
import com.z_company.repository.remote_rest.RegistrationState
import com.z_company.repository.remote_rest.ResponseState
import com.z_company.repository.remote_rest.SettingManager
import com.z_company.repository.remote_rest.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Android implementation of AuthProvider.
 *
 * Ported from ProfileViewModel logic. Uses:
 * - AuthManager (KMP) for email/VK auth, registration, profile, password recovery
 * - SecureTokenStorage (expect/actual) for encrypted token storage
 * - SyncManager (KMP) for cloud data synchronization
 * - SettingManager (KMP) for remote settings (subscription restore)
 * - SettingsUseCase (KMP) for local settings
 * - SharedPreferencesRepositories (KMP) for sync timestamp, migration state
 *
 * VK ID SDK (OneTap, VKID.instance) stays in features/route composable slots.
 * This class only calls authManager.authWithVKID/attachVKID/removeVKID (already KMP).
 */
class AndroidAuthProvider(
    private val authManager: AuthManager,
    private val secureTokenStorage: SecureTokenStorage,
    private val syncManager: SyncManager,
    private val settingManager: SettingManager,
    private val settingsUseCase: SettingsUseCase,
    private val sharedPrefs: SharedPreferencesRepositories,
) : AuthProvider {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isLoggedIn = MutableStateFlow(false)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    override val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    override val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    override val hasSubscription: StateFlow<Boolean> = settingsUseCase.getUserSettingFlow()
        .map { it.subscriptionPeriod > Clock.System.now().toEpochMilliseconds() }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), false)

    private val _subscriptionEndTime = MutableStateFlow(0L)
    override val subscriptionEndTime: StateFlow<Long> = _subscriptionEndTime.asStateFlow()

    private val _vkUserName = MutableStateFlow<String?>(null)
    override val vkUserName: StateFlow<String?> = _vkUserName.asStateFlow()

    private val _isVkAttached = MutableStateFlow(false)
    override val isVkAttached: StateFlow<Boolean> = _isVkAttached.asStateFlow()

    private val _syncState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    override val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    override val userId: StateFlow<String?> = _userId.asStateFlow()

    private var loginJob: Job? = null

    init {
        // Check if user is logged in and load profile
        scope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (!token.isNullOrBlank()) {
                loadUserProfile()
            }
        }
        // Observe VK ID changes
        scope.launch {
            secureTokenStorage.getVkIdFlow().onEach { vkId ->
                _isVkAttached.value = !vkId.isNullOrEmpty()
            }.launchIn(scope)
        }
        // Load subscription end time
        scope.launch {
            settingsUseCase.getUserSettingFlow().onEach { settings ->
                _subscriptionEndTime.value = settings.subscriptionPeriod
            }.launchIn(scope)
        }
        // Load user ID
        scope.launch {
            secureTokenStorage.getUserIdFlow().onEach { id ->
                _userId.value = id
            }.launchIn(scope)
        }
    }

    override fun authWithEmail(email: String, password: String) {
        loginJob?.cancel()
        _authState.value = AuthUiState.Loading
        loginJob = scope.launch {
            authManager.authWithEmail(email, password).collect { state ->
                when (state) {
                    is AuthState.Success -> {
                        if (state.accessToken.isNotEmpty()) {
                            secureTokenStorage.saveAuthToken(state.accessToken)
                            _isLoggedIn.value = true
                            _authState.value = AuthUiState.Success("Вход выполнен")
                            loadUserProfile()
                            // Sync from remote after login
                            syncManager.syncFromRemote("Bearer ${state.accessToken}").collect {}
                        }
                    }
                    is AuthState.Error -> {
                        _authState.value = AuthUiState.Error(state.errorMessage)
                    }
                    is AuthState.Loading -> {
                        _authState.value = AuthUiState.Loading
                    }
                    is AuthState.Initial -> {}
                }
            }
        }
    }

    override fun registerWithEmail(email: String, password: String) {
        loginJob?.cancel()
        _authState.value = AuthUiState.Loading
        loginJob = scope.launch {
            authManager.registerByEmail(email, password).collect { state ->
                when (state) {
                    is RegistrationState.Success -> {
                        if (state.accessToken.isNotEmpty()) {
                            secureTokenStorage.saveAuthToken(state.accessToken)
                            // Transfer local subscription if exists
                            val localSettings = settingsUseCase.getUserSettingFlow().first()
                            val endTime = sharedPrefs.getSubscriptionExpiration()
                            if (endTime != 0L) {
                                val updated = localSettings.copy(subscriptionPeriod = endTime)
                                settingsUseCase.saveSetting(updated)
                                    .first { it is ResultState.Success || it is ResultState.Error }
                            }
                            // First upload after registration
                            syncManager.firstSyncAfterRegistration("Bearer ${state.accessToken}")
                                .collect {}
                            _isLoggedIn.value = true
                            _authState.value = AuthUiState.Success("Регистрация выполнена")
                            loadUserProfile()
                        }
                    }
                    is RegistrationState.Error -> {
                        val msg = if (state.code == 409) "Пользователь уже зарегистрирован"
                        else state.message
                        _authState.value = AuthUiState.Error(msg)
                    }
                    is RegistrationState.Loading -> {
                        _authState.value = AuthUiState.Loading
                    }
                    is RegistrationState.Initial -> {}
                }
            }
        }
    }

    override fun authWithVkId(vkId: String) {
        loginJob?.cancel()
        _authState.value = AuthUiState.Loading
        loginJob = scope.launch {
            authManager.authWithVKID(vkId).collect { state ->
                when (state) {
                    is AuthState.Success -> {
                        if (state.accessToken.isNotEmpty()) {
                            secureTokenStorage.saveAuthToken(state.accessToken)
                            _isLoggedIn.value = true
                            _authState.value = AuthUiState.Success("Вход через VK выполнен")
                            loadUserProfile()
                            syncManager.syncFromRemote("Bearer ${state.accessToken}").collect {}
                        }
                    }
                    is AuthState.Error -> {
                        _authState.value = AuthUiState.Error(state.errorMessage)
                    }
                    is AuthState.Loading -> _authState.value = AuthUiState.Loading
                    is AuthState.Initial -> {}
                }
            }
        }
    }

    override fun logout() {
        scope.launch {
            secureTokenStorage.saveAuthToken("")
            secureTokenStorage.saveVkId("")
            _isLoggedIn.value = false
            _userEmail.value = null
            _vkUserName.value = null
            _isVkAttached.value = false
            _authState.value = AuthUiState.Initial
        }
    }

    override fun refresh() {
        scope.launch { loadUserProfile() }
    }

    override fun updateEmail(newEmail: String) {
        scope.launch {
            _authState.value = AuthUiState.Loading
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            authManager.updateEmail("Bearer $token", newEmail).collect { state ->
                when (state) {
                    is ResponseState.Success -> {
                        _authState.value = AuthUiState.Success("Email обновлён")
                        loadUserProfile()
                    }
                    is ResponseState.Error -> {
                        _authState.value = AuthUiState.Error(state.errorMessage)
                    }
                    else -> {}
                }
            }
        }
    }

    override fun forgotPassword(email: String) {
        scope.launch {
            _authState.value = AuthUiState.Loading
            authManager.forgotPassword(email).collect { state ->
                when (state) {
                    is ResponseState.Success -> {
                        _authState.value = AuthUiState.Success("Письмо отправлено на $email")
                    }
                    is ResponseState.Error -> {
                        _authState.value = AuthUiState.Error(state.errorMessage)
                    }
                    else -> {}
                }
            }
        }
    }

    override fun attachVkId(vkId: String) {
        scope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            authManager.attachVKID("Bearer $token", vkId).collect { state ->
                when (state) {
                    is GetUserProfileState.Success -> {
                        secureTokenStorage.saveVkId(vkId)
                        _isVkAttached.value = true
                    }
                    is GetUserProfileState.Error -> {
                        _authState.value = AuthUiState.Error("Ошибка привязки VK: ${state.message}")
                    }
                    else -> {}
                }
            }
        }
    }

    override fun removeVkId() {
        scope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            authManager.removeVKID("Bearer $token").collect { state ->
                when (state) {
                    is GetUserProfileState.Success -> {
                        secureTokenStorage.saveVkId("")
                        _isVkAttached.value = false
                        _vkUserName.value = null
                    }
                    is GetUserProfileState.Error -> {
                        _authState.value = AuthUiState.Error("Ошибка отвязки VK: ${state.message}")
                    }
                    else -> {}
                }
            }
        }
    }

    override fun startSyncUpload() {
        scope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first() ?: return@launch
            _syncState.value = SyncUiState.InProgress("Загрузка на сервер...")
            syncManager.syncToRemote("Bearer $token").collect { state ->
                when (state) {
                    is ResultState.Success -> {
                        val r = state.data
                        val msg = buildString {
                            append("Синхронизация завершена.\n")
                            if (r.userSettingsSaved) append("✓ Настройки\n")
                            if (r.salarySettingsSaved) append("✓ Зарплата\n")
                            if (r.monthsSaved) append("✓ Месяцы\n")
                            if (r.routesSavedCount >= 0) append("✓ Маршруты: ${r.routesSavedCount}")
                        }
                        r.timestamp?.let { sharedPrefs.setLastSyncTimestamp(it) }
                        _syncState.value = SyncUiState.Complete(msg)
                    }
                    is ResultState.Error -> {
                        _syncState.value = SyncUiState.Error(
                            state.entity.message ?: "Ошибка синхронизации"
                        )
                    }
                    is ResultState.Loading -> {}
                }
            }
        }
    }

    override fun startSyncDownload() {
        scope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first() ?: return@launch
            _syncState.value = SyncUiState.InProgress("Загрузка с сервера...")
            syncManager.syncFromRemote("Bearer $token").collect { state ->
                when (state) {
                    is ResultState.Success -> {
                        val r = state.data
                        val msg = buildString {
                            append("Синхронизация завершена.\n")
                            if (r.monthsLoaded) append("✓ Месяцы\n")
                            if (r.salarySettingsLoaded) append("✓ Зарплата\n")
                            if (r.userSettingsLoaded) append("✓ Настройки\n")
                            if (r.routesLoadedCount >= 0) append("✓ Маршруты: ${r.routesLoadedCount}")
                        }
                        _syncState.value = SyncUiState.Complete(msg)
                    }
                    is ResultState.Error -> {
                        _syncState.value = SyncUiState.Error(
                            state.entity.message ?: "Ошибка синхронизации"
                        )
                    }
                    is ResultState.Loading -> {}
                }
            }
        }
    }

    override fun restoreSubscription() {
        scope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first() ?: run {
                _authState.value = AuthUiState.Error("Необходима авторизация")
                return@launch
            }
            _authState.value = AuthUiState.Loading
            val settingState = settingManager.getUserSettingFromRemote("Bearer $token")
                .first { it !is ResultState.Loading }

            when (settingState) {
                is ResultState.Success -> {
                    val purchaseTimeEnd = settingState.data.subscriptionPeriod
                    if (purchaseTimeEnd > Clock.System.now().toEpochMilliseconds()) {
                        settingsUseCase.updateSubscriptionPeriod(purchaseTimeEnd)
                            .first { it !is ResultState.Loading }
                        _subscriptionEndTime.value = purchaseTimeEnd
                        _authState.value = AuthUiState.Success("Подписка восстановлена")
                    } else if (purchaseTimeEnd == 0L) {
                        _authState.value = AuthUiState.Error("Информация об оплате не найдена")
                    } else {
                        _authState.value = AuthUiState.Error("Срок оплаты истёк")
                    }
                }
                is ResultState.Error -> {
                    _authState.value = AuthUiState.Error("Ошибка связи с сервером")
                }
                else -> {}
            }
        }
    }

    // --- Internal ---

    private suspend fun loadUserProfile() {
        val token = secureTokenStorage.getAuthBearerTokenFlow().first()
        if (token.isNullOrBlank()) {
            _isLoggedIn.value = false
            return
        }
        authManager.getUserProfile("Bearer $token").collect { state ->
            when (state) {
                is GetUserProfileState.Success -> {
                    _userEmail.value = state.user.email
                    secureTokenStorage.saveUserId(state.user.id)
                    _isLoggedIn.value = true
                    // Restore subscription silently
                    restoreSubscriptionSilent(token)
                }
                is GetUserProfileState.Error -> {
                    if (state.code == 401) {
                        _isLoggedIn.value = false
                    }
                }
                else -> {}
            }
        }
    }

    private suspend fun restoreSubscriptionSilent(token: String) {
        try {
            val settingState = settingManager.getUserSettingFromRemote("Bearer $token")
                .first { it !is ResultState.Loading }
            if (settingState is ResultState.Success) {
                val purchaseTimeEnd = settingState.data.subscriptionPeriod
                if (purchaseTimeEnd > Clock.System.now().toEpochMilliseconds()) {
                    settingsUseCase.updateSubscriptionPeriod(purchaseTimeEnd)
                        .first { it !is ResultState.Loading }
                    _subscriptionEndTime.value = purchaseTimeEnd
                }
            }
        } catch (_: Exception) {
            // Silent restore — don't propagate errors
        }
    }
}

actual fun createAuthProvider(): AuthProvider {
    // Will be replaced by Koin injection
    throw IllegalStateException("Use Koin to provide AuthProvider")
}
