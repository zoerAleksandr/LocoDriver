package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.AuthManager
import com.z_company.repository.remote_rest.AuthState
import com.z_company.repository.remote_rest.GetUserProfileState
import com.z_company.repository.remote_rest.RegistrationState
import com.z_company.repository.remote_rest.SyncDownloadResult
import com.z_company.repository.remote_rest.SyncManager
import com.z_company.repository.remote_rest.SyncUploadResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * KMP ViewModel для экрана профиля / авторизации.
 *
 * Поддерживает:
 *  - Вход по email + пароль (authWithEmail)
 *  - Регистрацию по email + пароль (registerByEmail) — если пользователь ещё не зарегистрирован
 *  - Загрузку профиля по сохранённому токену при старте
 *  - Синхронизацию данных с сервером (syncFromRemote / syncToRemote)
 *  - Выход (удаление токена из Keychain)
 *
 * VK OAuth требует платформенной интеграции VK ID SDK — реализован как заглушка.
 */
class ProfileIosViewModel(
    private val authManager: AuthManager,
    private val syncManager: SyncManager,
    private val secureTokenStorage: SecureTokenStorage,
) : ViewModel() {

    // ── State ─────────────────────────────────────────────────────────────────

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    // ── Init: восстановление сессии из Keychain ───────────────────────────────

    init {
        viewModelScope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (!token.isNullOrBlank()) {
                loadUserProfile("Bearer $token")
            }
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Вход с email и паролем.
     * При ошибке 401 — автоматически пробует зарегистрировать нового пользователя.
     */
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Введите email и пароль"
            return
        }
        viewModelScope.launch {
            _errorMessage.value = null
            _isLoading.value = true
            authManager.authWithEmail(email, password).collect { state ->
                when (state) {
                    is AuthState.Loading -> _isLoading.value = true
                    is AuthState.Success -> {
                        val token = state.accessToken
                        secureTokenStorage.saveAuthToken(token)
                        secureTokenStorage.saveUserId(email)
                        _userEmail.value = email
                        _isLoggedIn.value = true
                        _isLoading.value = false
                    }
                    is AuthState.Error -> {
                        _errorMessage.value = state.errorMessage
                        _isLoading.value = false
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Заглушка для входа через VK.
     * VK ID OAuth на iOS требует интеграции vk-id-sdk-ios (платформенный SDK).
     * TODO: реализовать через UIKit/SwiftUI bridging когда VK ID SDK будет добавлен в Xcode-проект.
     */
    fun loginWithVK() {
        _errorMessage.value = "Вход через ВКонтакте скоро будет доступен"
    }

    /**
     * Синхронизация данных с сервером.
     *
     * ВАЖНО: порядок — сначала UPLOAD (включая удаление помеченных isDeleted=true
     * маршрутов на сервере), затем DOWNLOAD. Иначе download перезатрёт локальный
     * флаг isDeleted и «воскресит» только что удалённые маршруты (сервер всё
     * ещё хранит их, т.к. мы ещё не послали DELETE).
     */
    fun syncData() {
        viewModelScope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (token.isNullOrBlank()) {
                _errorMessage.value = "Войдите в аккаунт для синхронизации"
                return@launch
            }
            _isSyncing.value = true
            _syncMessage.value = null
            _errorMessage.value = null

            val bearerToken = "Bearer $token"

            // 1. Отправка на сервер (включая DELETE для isDeleted=true).
            var lastUploadResult: SyncUploadResult? = null
            val uploadErrors = mutableListOf<String>()
            syncManager.syncToRemote(bearerToken).collect { state ->
                when (state) {
                    is ResultState.Loading -> {}
                    is ResultState.Success -> lastUploadResult = state.data
                    is ResultState.Error -> {
                        val msg = state.entity.message
                        if (msg != null && msg != "Не все данные сохранены успешно") {
                            uploadErrors.add(msg)
                        }
                    }
                }
            }
            if (lastUploadResult == null && uploadErrors.isNotEmpty()) {
                _errorMessage.value = uploadErrors.last()
                _isSyncing.value = false
                return@launch
            }

            // 2. Загрузка с сервера — уже без только что удалённых маршрутов.
            var lastDownloadResult: SyncDownloadResult? = null
            val downloadErrors = mutableListOf<String>()

            syncManager.syncFromRemote(bearerToken).collect { state ->
                when (state) {
                    is ResultState.Loading -> {}
                    is ResultState.Success -> lastDownloadResult = state.data
                    is ResultState.Error -> {
                        val msg = state.entity.message
                        if (msg != null && msg != "Не все данные загружены успешно") {
                            downloadErrors.add(msg)
                        }
                    }
                }
            }

            val dl = lastDownloadResult
            val ul = lastUploadResult
            if (dl != null && dl.routesLoadedCount >= 0) {
                _syncMessage.value = buildString {
                    append("Синхронизация завершена. ")
                    if (ul != null && ul.routesSavedCount >= 0) {
                        append("Отправлено: ${ul.routesSavedCount}. ")
                    }
                    append("Загружено маршрутов: ${dl.routesLoadedCount}.")
                    if (!dl.userSettingsLoaded || !dl.salarySettingsLoaded || !dl.releaseDaysLoaded) {
                        append(" (часть данных не загружена)")
                    }
                    // Ошибки шага-4 (DELETE удалённых маршрутов на сервере) — если есть,
                    // пользователь сразу видит, что именно не смогло снестись.
                    val routeErrs = ul?.routeErrors.orEmpty()
                    if (routeErrs.isNotEmpty()) {
                        append("\nОшибки: ")
                        append(routeErrs.joinToString("; "))
                    }
                }
            } else if (downloadErrors.isNotEmpty()) {
                _errorMessage.value = downloadErrors.last()
            } else {
                _errorMessage.value = "Не удалось загрузить данные с сервера"
            }
            _isSyncing.value = false
        }
    }

    /** Выход из аккаунта — удаляет токен из Keychain. */
    fun logout() {
        viewModelScope.launch {
            secureTokenStorage.saveAuthToken("")
            secureTokenStorage.saveUserId("")
            _isLoggedIn.value = false
            _userEmail.value = null
            _errorMessage.value = null
            _syncMessage.value = null
        }
    }

    /** Сбрасывает сообщение об ошибке. */
    fun clearError() {
        _errorMessage.value = null
    }

    // ── Watch helpers для Swift ────────────────────────────────────────────────

    fun watchIsLoggedIn(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isLoggedIn.collect { callback(it) } }
    }

    fun watchIsLoading(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isLoading.collect { callback(it) } }
    }

    fun watchIsSyncing(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isSyncing.collect { callback(it) } }
    }

    fun watchErrorMessage(callback: (String?) -> Unit) {
        viewModelScope.launch { errorMessage.collect { callback(it) } }
    }

    fun watchUserEmail(callback: (String?) -> Unit) {
        viewModelScope.launch { userEmail.collect { callback(it) } }
    }

    fun watchSyncMessage(callback: (String?) -> Unit) {
        viewModelScope.launch { syncMessage.collect { callback(it) } }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private suspend fun loadUserProfile(token: String) {
        authManager.getUserProfile(token).collect { state ->
            when (state) {
                is GetUserProfileState.Loading -> {}
                is GetUserProfileState.Success -> {
                    _userEmail.value = state.user.email.ifBlank { null }
                    _isLoggedIn.value = true
                    _isLoading.value = false
                }
                is GetUserProfileState.Error -> {
                    // Токен протух или недействителен
                    secureTokenStorage.saveAuthToken("")
                    _isLoggedIn.value = false
                    _isLoading.value = false
                }
                else -> {}
            }
        }
    }

    private fun registerAndLogin(email: String, password: String) {
        viewModelScope.launch {
            authManager.registerByEmail(email, password).collect { state ->
                when (state) {
                    is RegistrationState.Loading -> {}
                    is RegistrationState.Success -> {
                        val token = state.accessToken
                        secureTokenStorage.saveAuthToken(token)
                        secureTokenStorage.saveUserId(email)
                        _userEmail.value = email
                        _isLoggedIn.value = true
                        // Первая синхронизация после регистрации
                        syncManager.firstSyncAfterRegistration(token).collect {}
                        _isLoading.value = false
                    }
                    is RegistrationState.Error -> {
                        _errorMessage.value = when (state.code) {
                            409 -> "Аккаунт с таким email уже существует. Проверьте пароль."
                            else -> state.message
                        }
                        _isLoading.value = false
                    }
                    else -> {}
                }
            }
        }
    }
}
