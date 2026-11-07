@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import com.z_company.core.ResultState
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.iosapp.session.AuthSessionHolder
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.AuthManager
import com.z_company.repository.remote_rest.AuthState
import com.z_company.repository.remote_rest.ForgotPasswordState
import com.z_company.repository.remote_rest.GetUserProfileState
import com.z_company.repository.remote_rest.NetworkErrorMapper
import com.z_company.repository.remote_rest.RegistrationState
import com.z_company.repository.remote_rest.ResponseState
import com.z_company.repository.remote_rest.SettingManager
import com.z_company.repository.remote_rest.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant
import platform.Foundation.NSUserDefaults

// ─────────────────────────────────────────────────────────────────────────────
// Модели состояния для Swift (SCREEN_SPECS §18).
// Статусы шагов синхронизации — строки ("loading"/"success"/"error"):
// sealed-классы из Kotlin в Swift переключать неудобно.
// ─────────────────────────────────────────────────────────────────────────────

data class ProfileIosSyncStep(
    val key: String = "",
    val label: String = "",
    val status: String = "loading",
    val details: String = "",
)

/**
 * Диалог «Синхронизация данных» (§18.5). Диалог не запускает sync сам —
 * только отображает состояние VM. Порядок шагов фиксированный:
 * UserSettings → SalarySettings → ReleaseDays → Routes (шаг 25%).
 */
data class ProfileIosSyncDialog(
    val visible: Boolean = false,
    val steps: List<ProfileIosSyncStep> = emptyList(),
    /** Текущий этап: «Подготовка» до первого завершённого, «Завершено» когда все готовы. */
    val currentLabel: String = "Подготовка",
    val percent: Int = 0,
    val firstError: String? = null,
    val isComplete: Boolean = false,
    /** Полный успех: вместо прогресса AlertDialog «Синхронизация завершена!». */
    val isSuccess: Boolean = false,
    val isNetworkError: Boolean = false,
    val isSessionExpired: Boolean = false,
    /** «Отправить отчет об ошибке» — только при не-сетевых ошибках (§18.5). */
    val showReportButton: Boolean = false,
    val reportSubject: String = "",
    val reportBody: String = "",
)

/**
 * Состояние экрана «Профиль» (§18.S/§18.1/§18.3). Взаимоисключающие ветки
 * экрана выбираются в порядке §18.S: `!sessionChecked` → заглушка;
 * `needsMigration` → миграция (на iOS всегда false, §18.5 «Для iOS — не переносить»);
 * `isAuthorized` → профиль; иначе → форма входа.
 */
data class ProfileIosState(
    val sessionChecked: Boolean = false,
    val isAuthorized: Boolean = false,
    val needsMigration: Boolean = false,
    // Вход/регистрация поверх любого состояния (§18.S)
    val authLoading: Boolean = false,
    val authErrorMessage: String? = null,
    val registrationLoading: Boolean = false,
    val registrationErrorMessage: String? = null,
    // Профиль (§18.1)
    val profileLoading: Boolean = false,
    /** Нет сети при загрузке профиля → баннер ProfileDataBanner (§18.S). */
    val profileNetworkError: Boolean = false,
    /** Не-сетевая ошибка загрузки → «Ошибка загрузки» в группе EMAIL (§18.3). */
    val emailBlockError: Boolean = false,
    val email: String? = null,
    val vkId: String? = null,
    /** Имя из VK SDK; на iOS VK ID SDK не реализован (§18.4) — всегда null. */
    val vkProfileName: String? = null,
    /** Фото профиля VK (user_info); null — фото нет или не загрузилось. */
    val vkAvatarUrl: String? = null,
    /** Вход в VK через SDK выполнен (§18.1): карточка перестаёт просить «Войдите в VK»,
     *  даже если имя из токена не досталось. */
    val vkSdkSignedIn: Boolean = false,
    /** Ошибка привязки/отвязки VK — snackbar (§18.2). */
    val vkLinkMessage: String? = null,
    /** Вход по VK, аккаунта нет: диалог «Аккаунта с этим VK нет» → «Создать»/«Отмена». */
    val vkRegistrationOffer: Boolean = false,
    // Подписка (§18.1)
    val hasActiveSubscription: Boolean = false,
    /** «Оплачено до dd.MM.yy HH:mm» в поясе отображения; пусто — подписки не было. */
    val subscriptionEndLabel: String = "",
    // Синхронизация (§18.5)
    val isSyncing: Boolean = false,
    /** «Последняя синхронизация: dd.MM.yy HH:mm». */
    val lastSyncLabel: String = "",
    val syncDialog: ProfileIosSyncDialog = ProfileIosSyncDialog(),
    // Шторки ввода (§18.6)
    val updateEmailLoading: Boolean = false,
    val updateEmailError: String? = null,
    val addEmailLoading: Boolean = false,
    val addEmailError: String? = null,
    // Сброс пароля (§18.0)
    val forgotLoading: Boolean = false,
    val forgotCooldownSeconds: Int = 0,
    /** Количество маршрутов в корзине (§18.1 группа ДАННЫЕ). */
    val trashCount: Int = 0,
    // Подсказка «Приглашайте друзей» (§6.9) над строкой «Пригласить друга».
    val showReferralHint: Boolean = false,
    /** Подсказка про VPN под баннером (§18.S) — детекция VPN на iOS не реализована, всегда null. */
    val vpnHint: String? = null,
)

/**
 * Одноразовые события для Swift (закрыть шторку и т.п.). `kind`:
 * "emailUpdated" (§18.4), "emailAdded" (§18.4).
 */
data class ProfileIosEvent(val kind: String, val text: String = "")

/**
 * iOS-аналог Android `ProfileViewModel` (§18). Вся логика аутентификации,
 * синхронизации и профиля — здесь; SwiftUI только отображает [ProfileIosState].
 *
 * VK ID SDK подключён в Swift (`iosApp/iosApp/Shared/VKIDService.swift`):
 * one-tap авторизация отдаёт access token, который уходит в [authWithVKID],
 * [attachVKID] и [registerByVKID] (§18.2/§18.4). Кнопки VK в UI —
 * `ProfileAuthFormView`/`ProfileView` через `ProfileViewModelWrapper`.
 */
class ProfileIosViewModel(
    private val authManager: AuthManager,
    private val syncManager: SyncManager,
    private val authSession: AuthSessionHolder,
    private val secureTokenStorage: SecureTokenStorage,
    private val sharedPrefs: SharedPreferencesRepositories,
    private val settingsUseCase: SettingsUseCase,
    private val settingManager: SettingManager,
    private val routeUseCase: com.z_company.domain.use_cases.RouteUseCase,
) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(ProfileIosState())
    val state: StateFlow<ProfileIosState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)

    private var loginJob: Job? = null
    private var registerJob: Job? = null
    private var syncJob: Job? = null
    private var forgotJob: Job? = null
    private var forgotCooldownJob: Job? = null
    private var profileJob: Job? = null

    /** Данные для регистрации по VK до подтверждения (§18.2); access token живёт только в памяти. */
    private data class PendingVkRegistration(
        val vkId: String,
        val vkAccessToken: String,
        val email: String,
        val vkName: String,
        val vkAvatar: String,
    )

    private var pendingVkRegistration: PendingVkRegistration? = null

    /** Смещение пояса отображения (§0.1); обновляется при изменении UserSettings. */
    private var displayOffsetCache: Long = 3 * 3_600_000L

    init {
        // ВК-профиль (имя/фото/вход) переживает перезапуск — иначе карточка
        // откатывается к «Войдите в VK, чтобы показать профиль».
        restoreVkProfile()
        // Токен — единый источник истины (С4): сессию могли закрыть по 401 с
        // другого экрана — профиль переходит в форму входа (§18.5).
        scope.launch {
            authSession.isAuthorized.collect { authorized ->
                if (!authorized && _state.value.isAuthorized) {
                    showLoggedOutState()
                }
            }
        }
        // Срок подписки и метка последней синхронизации — из локальных настроек.
        scope.launch {
            settingsUseCase.getUserSettingFlow().collect { settings ->
                updateSubscriptionFields(settings)
            }
        }
        // Количество маршрутов в корзине (§18.1 группа ДАННЫЕ).
        scope.launch {
            routeUseCase.getListRoutesAsFlow().collect { routes ->
                _state.updateCopy(trashCount = routes.count { it.basicData.isDeleted })
            }
        }
        getUserInfo()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // watch* для Swift
    // ═══════════════════════════════════════════════════════════════════════

    fun watchState(callback: (ProfileIosState) -> Unit) {
        scope.launch { _state.collect { callback(it) } }
    }

    fun watchMessages(callback: (String) -> Unit) {
        scope.launch { _messages.collect { callback(it) } }
    }

    fun watchEvents(callback: (ProfileIosEvent) -> Unit) {
        eventSink = callback
        pendingEvents.toList().also { pendingEvents.clear() }.forEach(callback)
    }

    private var eventSink: ((ProfileIosEvent) -> Unit)? = null
    private val pendingEvents = mutableListOf<ProfileIosEvent>()

    private fun emit(event: ProfileIosEvent) {
        val sink = eventSink
        if (sink != null) sink(event) else pendingEvents.add(event)
    }

    fun dispose() {
        loginJob?.cancel()
        registerJob?.cancel()
        syncJob?.cancel()
        forgotJob?.cancel()
        forgotCooldownJob?.cancel()
        profileJob?.cancel()
        scope.cancel()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Валидация формы (§18.0) — Swift вызывает перед включением кнопок
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Валидный email. Эквивалент `android.util.Patterns.EMAIL_ADDRESS`
     * (в commonMain android.* запрещён, см. AGENTS.md).
     */
    fun isEmailValid(email: String): Boolean =
        EMAIL_REGEX.matches(email.trim())

    /** Пароль не короче MIN_LENGTH_PASSWORD (= 4, §18.0). */
    fun isPasswordLongEnough(password: String): Boolean =
        password.length >= MIN_LENGTH_PASSWORD

    fun minPasswordLength(): Int = MIN_LENGTH_PASSWORD

    // ═══════════════════════════════════════════════════════════════════════
    // Вход / регистрация (§18.0, §18.2)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Вход по email/паролю. Успех — смена аккаунта, перечитывание профиля и
     * однократная полная загрузка `syncFromRemote` (§18.0) — без проверки подписки.
     */
    fun authWithEmail(email: String, password: String) {
        loginJob?.cancel()
        registerJob?.cancel()
        println("AuthFlow: authWithEmail start (login form)")
        _state.updateCopy(
            authErrorMessage = null,
            registrationErrorMessage = null,
        )
        loginJob = scope.launch {
            authManager.authWithEmail(email = email, password = password).collect { authState ->
                when (authState) {
                    is AuthState.Loading -> {
                        println("AuthFlow: authWithEmail → Loading")
                        _state.updateCopy(authLoading = true)
                    }
                    is AuthState.Success -> {
                        val token = authState.accessToken
                        println("AuthFlow: authWithEmail → Success (token ${if (token.isEmpty()) "EMPTY" else "present"})")
                        if (token.isNotEmpty()) {
                            logInAs(token)
                            refresh()
                            syncManager.syncFromRemote("Bearer $token").collect {}
                        }
                        _state.updateCopy(authLoading = false)
                    }
                    is AuthState.Error -> {
                        // Текст ошибки уже обезличен (без пароля/токена).
                        println("AuthFlow: authWithEmail → Error: ${authState.errorMessage}")
                        _state.updateCopy(authLoading = false, authErrorMessage = authState.errorMessage)
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Регистрация по email/паролю. Успех — смена аккаунта, перенос легаси-срока
     * локальной покупки (§18.0) и первая выгрузка `firstSyncAfterRegistration`.
     * 409 → «Пользователь с таким email уже существует» (§18.2).
     */
    fun registerByEmail(email: String, password: String) {
        loginJob?.cancel()
        registerJob?.cancel()
        println("AuthFlow: registerByEmail start")
        _state.updateCopy(
            authErrorMessage = null,
            registrationErrorMessage = null,
        )
        registerJob = scope.launch {
            authManager.registerByEmail(email = email, password = password).collect { regState ->
                when (regState) {
                    is RegistrationState.Loading -> {
                        println("AuthFlow: registerByEmail → Loading")
                        _state.updateCopy(registrationLoading = true)
                    }
                    is RegistrationState.Success -> {
                        val token = regState.accessToken
                        println("AuthFlow: registerByEmail → Success (token ${if (token.isEmpty()) "EMPTY" else "present"})")
                        if (token.isNotEmpty()) {
                            logInAs(token)
                            // Легаси-срок локальной покупки переносим в UserSettings (§18.0).
                            val legacyEnd = sharedPrefs.getSubscriptionExpiration()
                            if (legacyEnd != 0L) {
                                val local = settingsUseCase.getUserSettingFlow().first()
                                settingsUseCase.saveSetting(local.copy(subscriptionPeriod = legacyEnd))
                                    .first { it is ResultState.Success || it is ResultState.Error }
                            }
                            firstUpload()
                            refresh()
                        }
                        _state.updateCopy(registrationLoading = false)
                    }
                    is RegistrationState.Error -> {
                        val text = if (regState.code == 409) {
                            "Пользователь с таким email уже существует"
                        } else {
                            regState.message
                        }
                        println("AuthFlow: registerByEmail → Error(${regState.code}): $text")
                        _state.updateCopy(registrationLoading = false, registrationErrorMessage = text)
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Вход по VK ID (§18.2). `vkAccessToken` — токен VKID SDK, уходит ровно в
     * один сетевой запрос и нигде не сохраняется. Аккаунта с этим VK нет →
     * [ProfileIosState.vkRegistrationOffer], а не ошибка входа.
     *
     * Ошибка самого VK SDK сюда не приходит: её показывает Swift-обёртка
     * (`ProfileViewModelWrapper.signInWithVKID`) русским сообщением — в отличие
     * от Android, где ошибка SDK только логируется (BUG-52).
     */
    fun authWithVKID(vkId: String, vkAccessToken: String, email: String, vkName: String, vkAvatar: String) {
        loginJob?.cancel()
        loginJob = scope.launch {
            println("AuthFlow: authWithVKID → start")
            authManager.authWithVKID(vkId, vkAccessToken).collect { authState ->
                when {
                    authState is AuthState.Success -> {
                        val token = authState.accessToken
                        println("AuthFlow: authWithVKID → success")
                        if (token.isNotEmpty()) {
                            logInAs(token, vkId)
                            refresh()
                            syncManager.syncFromRemote("Bearer $token").collect {}
                        }
                        // Имя профиля — из VK SDK (§18.1), сервер его не хранит.
                        _state.updateCopy(
                            authLoading = false,
                            vkProfileName = vkName.ifBlank { null },
                            vkAvatarUrl = vkAvatar.ifBlank { null },
                            vkSdkSignedIn = true,
                        )
                        persistVkProfile(vkName.ifBlank { null }, vkAvatar.ifBlank { null }, true)
                    }
                    authState is AuthState.Error &&
                        authState.vkError == com.z_company.repository.remote_rest.VkAuthError.UserNotFound -> {
                        println("AuthFlow: authWithVKID → vkUserNotFound (registration offer)")
                        // Токен держим в памяти до подтверждения (§18.2).
                        pendingVkRegistration = PendingVkRegistration(vkId, vkAccessToken, email, vkName, vkAvatar)
                        _state.updateCopy(
                            authLoading = false,
                            vkRegistrationOffer = true,
                            authErrorMessage = null,
                        )
                    }
                    authState is AuthState.Error -> {
                        println("AuthFlow: authWithVKID → error: ${authState.errorMessage}")
                        _state.updateCopy(authLoading = false, authErrorMessage = authState.errorMessage)
                    }
                    authState is AuthState.Loading -> _state.updateCopy(authLoading = true)
                    else -> {}
                }
            }
        }
    }

    /** Согласие «Создать» в диалоге «Аккаунта с этим VK нет» (§18.2). */
    fun confirmVkRegistration() {
        val pending = pendingVkRegistration ?: return
        pendingVkRegistration = null
        _state.updateCopy(vkRegistrationOffer = false)
        println("AuthFlow: confirmVkRegistration → registerByVKID")
        registerByVKID(pending.vkId, pending.vkAccessToken, pending.email, pending.vkName, pending.vkAvatar)
    }

    /** «Отмена» — отложенный access token стирается (§18.2). */
    fun dismissVkRegistration() {
        pendingVkRegistration = null
        _state.updateCopy(vkRegistrationOffer = false)
    }

    /** Регистрация по VK ID (§18.0): без пароля, почта — из данных VK. */
    fun registerByVKID(vkId: String, vkAccessToken: String, email: String, vkName: String, vkAvatar: String) {
        registerJob?.cancel()
        registerJob = scope.launch {
            println("AuthFlow: registerByVKID → start")
            authManager.registerByVKID(vkId, vkAccessToken, email).collect { regState ->
                when (regState) {
                    is RegistrationState.Loading -> _state.updateCopy(registrationLoading = true)
                    is RegistrationState.Success -> {
                        val token = regState.accessToken
                        println("AuthFlow: registerByVKID → success")
                        if (token.isNotEmpty()) {
                            logInAs(token, vkId)
                            // Легаси-срок при регистрации по VK не переносится (§18.0).
                            firstUpload()
                            refresh()
                        }
                        _state.updateCopy(
                            registrationLoading = false,
                            vkProfileName = vkName.ifBlank { null },
                            vkAvatarUrl = vkAvatar.ifBlank { null },
                            vkSdkSignedIn = true,
                        )
                        persistVkProfile(vkName.ifBlank { null }, vkAvatar.ifBlank { null }, true)
                    }
                    is RegistrationState.Error -> {
                        val text = if (regState.code == 409) {
                            "Пользователь с таким email уже существует"
                        } else {
                            regState.message
                        }
                        println("AuthFlow: registerByVKID → error(${regState.code}): $text")
                        _state.updateCopy(registrationLoading = false, registrationErrorMessage = text)
                    }
                    else -> {}
                }
            }
        }
    }

    /** Крестик на GenericLoading «Выполняется вход...»/«Регистрируем…» (§18.S). */
    fun cancelAuth() {
        loginJob?.cancel()
        registerJob?.cancel()
        loginJob = null
        registerJob = null
        _state.updateCopy(
            authLoading = false,
            registrationLoading = false,
            authErrorMessage = null,
            registrationErrorMessage = null,
        )
    }

    /**
     * Смена аккаунта (семантика Android `AccountSwitcher`, §18.0): прежний userId
     * стирается, сохраняется токен, срок подписки берётся с сервера (без ответа
     * сервера — 0, чтобы чужой срок не утёк новому аккаунту).
     */
    private suspend fun logInAs(token: String, vkId: String? = null) {
        println("AuthFlow: logInAs — storing token (len=${token.length}), switching account")
        authSession.beginAccountSwitch(token, vkId)
        // Сначала переключаем экран на профиль (§18.1): сетевые вызовы ниже не
        // должны задерживать или откатывать UI, даже если подписка не загрузилась.
        _state.updateCopy(isAuthorized = true)
        val period = try {
            settingManager.getAccountSubscriptionPeriod("Bearer $token")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        settingsUseCase.updateSubscriptionPeriod(period ?: 0L).collect {}
        println("AuthFlow: logInAs done — isAuthorized=true, subscription period ${if (period == null) "NOT loaded" else "loaded"}")
        if (period == null) {
            _messages.tryEmit("Вход выполнен, но данные подписки не загрузились")
        }
    }

    /** Первая выгрузка после регистрации (§18.0). */
    private fun firstUpload() {
        scope.launch {
            val token = authSession.currentToken() ?: return@launch
            syncManager.firstSyncAfterRegistration("Bearer $token").collect { result ->
                if (result is ResultState.Success) {
                    result.data.timestamp?.let { sharedPrefs.setLastSyncTimestamp(it) }
                    refresh()
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Профиль (§18.1)
    // ═══════════════════════════════════════════════════════════════════════

    /** Перечитывание профиля (`GET /v1/users/me`, §18.1). */
    fun refresh() {
        getUserInfo()
    }

    fun getUserInfo() {
        profileJob?.cancel()
        profileJob = scope.launch {
            val token = authSession.currentToken()
            if (token.isNullOrBlank()) {
                _state.updateCopy(
                    sessionChecked = true,
                    isAuthorized = false,
                    profileLoading = false,
                    profileNetworkError = false,
                    emailBlockError = false,
                )
                return@launch
            }
            // Токен есть — сразу показываем профиль (не ждём сети), на 401 откатываемся.
            _state.updateCopy(
                sessionChecked = true,
                isAuthorized = true,
                profileLoading = true,
                emailBlockError = false,
            )
            authManager.getUserProfile("Bearer $token").collect { profileState ->
                when (profileState) {
                    is GetUserProfileState.Loading -> {
                        _state.updateCopy(profileLoading = true)
                    }
                    is GetUserProfileState.Success -> {
                        secureTokenStorage.saveUserId(profileState.user.id)
                        _state.updateCopy(
                            profileLoading = false,
                            profileNetworkError = false,
                            emailBlockError = false,
                            email = profileState.user.email.ifBlank { null },
                            vkId = profileState.user.vkId.ifBlank { null },
                        )
                        // Если сервер знает vk_id, а локально его нет — сохраняем (§18.1).
                        val serverVkId = profileState.user.vkId
                        if (serverVkId.isNotEmpty()) {
                            val localVkId = authSession.currentVkId()
                            if (localVkId.isNullOrEmpty()) {
                                authSession.saveVkId(serverVkId)
                            }
                        }
                        // Тихое обновление срока подписки с сервера (§18.1): локальный
                        // срок меняется, только если серверный в будущем.
                        // Полный restorePurchases — §19 (ios/purchases), «не проверено».
                        try {
                            val serverPeriod =
                                settingManager.getAccountSubscriptionPeriod("Bearer $token")
                            val now = nowMs()
                            if (serverPeriod != null && serverPeriod > now) {
                                val local = settingsUseCase.getUserSettingFlow().first()
                                if (serverPeriod > local.subscriptionPeriod) {
                                    settingsUseCase.updateSubscriptionPeriod(serverPeriod).collect {}
                                }
                            }
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (_: Exception) {
                        }
                    }
                    is GetUserProfileState.Error -> {
                        if (profileState.code == 401) {
                            handleSessionExpired()
                        } else {
                            val networkErr = profileState.code == 0 ||
                                NetworkErrorMapper.isConnectivityMessage(profileState.message)
                            _state.updateCopy(
                                profileLoading = false,
                                profileNetworkError = networkErr,
                                emailBlockError = !networkErr,
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /** 401 — сессия закрывается как logOut (§18.5), экран переходит в форму входа. */
    private suspend fun handleSessionExpired() {
        authSession.clearSession()
        showLoggedOutState()
        _messages.tryEmit(NetworkErrorMapper.SESSION_EXPIRED_MESSAGE)
    }

    private fun showLoggedOutState() {
        _state.updateCopy(
            isAuthorized = false,
            profileLoading = false,
            profileNetworkError = false,
            emailBlockError = false,
            email = null,
            vkId = null,
            vkProfileName = null,
            vkAvatarUrl = null,
            vkSdkSignedIn = false,
        )
        persistVkProfile(null, null, false)
    }

    /**
     * Выход из аккаунта (§18.1) — без подтверждения: токен, vk_id и userId
     * очищаются, курсор дельта-синхронизации сбрасывается. Локальные маршруты и
     * настройки (включая срок подписки) не удаляются; сессия VK SDK не завершается.
     */
    fun logOut() {
        scope.launch {
            authSession.clearSession()
            showLoggedOutState()
        }
    }

    private fun updateSubscriptionFields(settings: UserSettings) {
        displayOffsetCache = displayOffsetMs(settings)
        val end = settings.subscriptionPeriod
        val now = nowMs()
        val active = end > now
        val endLabel = when {
            end == 0L -> ""
            else -> "Оплачено до ${formatDateTime(end, displayOffsetMs(settings))}"
        }
        // BUG-53 (паритет с Android): если синхронизаций ещё не было (метка = 0),
        // в строке «Последняя синхронизация» подставляется текущее время — вводит
        // в заблуждение. Поведение сохранено как на Android.
        val syncAt = sharedPrefs.getLastSyncTimestamp().let { if (it == 0L) nowMs() else it }
        _state.updateCopy(
            hasActiveSubscription = active,
            subscriptionEndLabel = endLabel,
            lastSyncLabel = "Последняя синхронизация: ${formatDateTime(syncAt, displayOffsetMs(settings))}",
            showReferralHint = isReferralHintVisible(),
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Email, пароль (§18.4)
    // ═══════════════════════════════════════════════════════════════════════

    /** Смена email (`PATCH /v1/auth/email/update`, §18.4). */
    fun updateEmail(newEmail: String) {
        scope.launch {
            _state.updateCopy(updateEmailLoading = true, updateEmailError = null)
            val bearer = authSession.currentBearer() ?: run {
                _state.updateCopy(updateEmailLoading = false)
                return@launch
            }
            authManager.updateEmail(token = bearer, email = newEmail).collect { result ->
                when (result) {
                    is ResponseState.Success -> {
                        _state.updateCopy(updateEmailLoading = false, updateEmailError = null)
                        emit(ProfileIosEvent(kind = "emailUpdated"))
                        _messages.tryEmit("Email успешно изменён")
                        getUserInfo()
                    }
                    is ResponseState.Error -> {
                        _state.updateCopy(
                            updateEmailLoading = false,
                            updateEmailError = result.errorMessage,
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    /** Правка поля в шторке стирает ошибку (§18.4). */
    fun clearUpdateEmailError() {
        _state.updateCopy(updateEmailError = null)
    }

    /** Сброс состояния при открытии шторки «Новый email» (§18.4). */
    fun resetUpdateEmailState() {
        _state.updateCopy(updateEmailLoading = false, updateEmailError = null)
    }

    /**
     * Привязка почты и пароля к VK-аккаунту (`PATCH /v1/auth/email/add`, §18.2/§18.4).
     */
    fun addEmail(newEmail: String, password: String) {
        scope.launch {
            _state.updateCopy(addEmailLoading = true, addEmailError = null)
            val bearer = authSession.currentBearer() ?: run {
                _state.updateCopy(addEmailLoading = false)
                return@launch
            }
            authManager.addEmail(token = bearer, email = newEmail, password = password).collect { result ->
                when (result) {
                    is ResponseState.Success -> {
                        _state.updateCopy(addEmailLoading = false, addEmailError = null)
                        emit(ProfileIosEvent(kind = "emailAdded"))
                        _messages.tryEmit("Почта добавлена — теперь можно входить и по паролю")
                        getUserInfo()
                    }
                    is ResponseState.Error -> {
                        _state.updateCopy(
                            addEmailLoading = false,
                            addEmailError = result.errorMessage,
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun clearAddEmailError() {
        _state.updateCopy(addEmailError = null)
    }

    fun resetAddEmailState() {
        _state.updateCopy(addEmailLoading = false, addEmailError = null)
    }

    /**
     * Сброс пароля (§18.0): `POST /v1/page/forgot_password?email=…`, локальный
     * кулдаун 60 с после 200 и 429. Клиентской проверки формата email нет —
     * запрос уходит и с пустым полем (как на Android).
     */
    fun forgotRequest(email: String) {
        if (_state.value.forgotCooldownSeconds > 0) return
        forgotJob?.cancel()
        forgotJob = scope.launch {
            _state.updateCopy(forgotLoading = true)
            authManager.forgotPassword(email.filterNot { it.isWhitespace() }).collect { forgotState ->
                when (forgotState) {
                    is ForgotPasswordState.Loading -> _state.updateCopy(forgotLoading = true)
                    is ForgotPasswordState.Success -> {
                        _state.updateCopy(forgotLoading = false)
                        _messages.tryEmit(
                            "Если такой email зарегистрирован, мы отправили на него ссылку. Проверьте входящие и папку «Спам»."
                        )
                        startForgotCooldown()
                    }
                    is ForgotPasswordState.RateLimited -> {
                        _state.updateCopy(forgotLoading = false)
                        _messages.tryEmit("Слишком много попыток, попробуйте позже")
                        startForgotCooldown()
                    }
                    is ForgotPasswordState.Error -> {
                        _state.updateCopy(forgotLoading = false)
                        _messages.tryEmit(forgotState.errorMessage)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun startForgotCooldown() {
        forgotCooldownJob?.cancel()
        forgotCooldownJob = scope.launch {
            var left = FORGOT_COOLDOWN_SECONDS
            while (left > 0) {
                _state.updateCopy(forgotCooldownSeconds = left)
                delay(1000L)
                left--
            }
            _state.updateCopy(forgotCooldownSeconds = 0)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VK (§18.2, §18.4). Access token приходит из VK ID SDK (Swift-обёртка
    // `VKIDService`) и уходит ровно в один сетевой запрос, нигде не сохраняясь.
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Привязка VK к текущему аккаунту (`PATCH /v1/auth/vkId/add`). Ошибка
     * (в т.ч. 409) показывается через [ProfileIosState.vkLinkMessage].
     */
    fun attachVKID(vkId: String, vkAccessToken: String, vkName: String, vkAvatar: String) {
        scope.launch {
            val bearer = authSession.currentBearer() ?: return@launch
            println("AuthFlow: attachVKID → start")
            authManager.attachVKID(bearer, vkId, vkAccessToken).collect { result ->
                when (result) {
                    is GetUserProfileState.Success -> {
                        println("AuthFlow: attachVKID → success")
                        authSession.saveVkId(vkId)
                        _state.updateCopy(
                            vkProfileName = vkName.ifBlank { null },
                            vkAvatarUrl = vkAvatar.ifBlank { null },
                            vkSdkSignedIn = true,
                        )
                        persistVkProfile(vkName.ifBlank { null }, vkAvatar.ifBlank { null }, true)
                        refresh()
                    }
                    is GetUserProfileState.Error -> {
                        println("AuthFlow: attachVKID → error: ${result.message}")
                        _state.updateCopy(vkLinkMessage = result.message)
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Отвязка VK (`PATCH /v1/auth/vkId/remove`): сервер отзывает все токены и
     * возвращает новый — сохраняем его, чистим локальный vk_id. Сессия VK ID SDK
     * завершается на стороне Swift (`VKIDService.logout`, аналог Android
     * `VKID.instance.logout`).
     */
    fun removeUsersVKID() {
        scope.launch {
            val bearer = authSession.currentBearer() ?: return@launch
            println("AuthFlow: removeUsersVKID → start")
            authManager.removeVKID(bearer).collect { result ->
                when (result) {
                    is GetUserProfileState.Success -> {
                        println("AuthFlow: removeUsersVKID → success")
                        result.accessToken?.let { authSession.saveToken(it) }
                        authSession.saveVkId("")
                        _state.updateCopy(vkProfileName = null, vkAvatarUrl = null, vkSdkSignedIn = false)
                        persistVkProfile(null, null, false)
                        refresh()
                    }
                    is GetUserProfileState.Error -> {
                        println("AuthFlow: removeUsersVKID → error: ${result.message}")
                        _state.updateCopy(vkLinkMessage = result.message)
                    }
                    else -> {}
                }
            }
        }
    }

    /** Локальная авторизация VK SDK для уже привязанного аккаунта (§18.1). */
    fun onVkAuthForLinkedAccount(vkId: String, vkName: String, vkAvatar: String) {
        scope.launch {
            authSession.saveVkId(vkId)
        }
        // Имя профиля — из VK SDK (§18.1): без него карточка остаётся
        // с надписью «Войдите в VK, чтобы показать профиль».
        _state.updateCopy(
            vkId = vkId,
            vkProfileName = vkName.ifBlank { null },
            vkAvatarUrl = vkAvatar.ifBlank { null },
            vkSdkSignedIn = true,
        )
        persistVkProfile(vkName.ifBlank { null }, vkAvatar.ifBlank { null }, true)
    }

    fun clearVkLinkMessage() {
        _state.updateCopy(vkLinkMessage = null)
    }

    // ── Персистентность VK-профиля (имя/фото/флаг входа) ──────────────────

    private fun persistVkProfile(name: String?, avatar: String?, signedIn: Boolean) {
        val ud = NSUserDefaults.standardUserDefaults
        ud.setObject(name, forKey = "ld.vkProfileName")
        ud.setObject(avatar, forKey = "ld.vkAvatarUrl")
        ud.setBool(signedIn, forKey = "ld.vkSdkSignedIn")
    }

    private fun restoreVkProfile() {
        val ud = NSUserDefaults.standardUserDefaults
        if (!ud.boolForKey("ld.vkSdkSignedIn")) return
        _state.updateCopy(
            vkProfileName = ud.stringForKey("ld.vkProfileName"),
            vkAvatarUrl = ud.stringForKey("ld.vkAvatarUrl"),
            vkSdkSignedIn = true,
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Синхронизация (§18.5)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Единая двусторонняя синхронизация (кнопка «Синхронизация», §18.2/§18.5).
     * Диалог прогресса ведёт четыре этапа по 25%. Ручной запуск гейтится
     * подпиской; повторный запуск ждёт общий mutex `SyncManager`.
     */
    fun startSync() {
        syncJob?.cancel()
        syncJob = scope.launch {
            val setting = settingsUseCase.getUserSettingFlow().first()
            if (setting.subscriptionPeriod <= nowMs()) {
                _messages.tryEmit("Синхронизация доступна по подписке")
                return@launch
            }
            val token = authSession.currentToken() ?: return@launch
            val userId = authSession.currentUserId()
            _state.updateCopy(
                isSyncing = true,
                syncDialog = ProfileIosSyncDialog(
                    visible = true,
                    steps = defaultSyncSteps(),
                    currentLabel = "Подготовка",
                    percent = 0,
                    isComplete = false,
                    isSuccess = false,
                    isNetworkError = false,
                    isSessionExpired = false,
                    showReportButton = false,
                ),
            )
            var networkErrorStopped = false
            try {
                syncManager.syncBidirectional("Bearer $token").collect { result ->
                    if (networkErrorStopped) return@collect
                    when (result) {
                        is ResultState.Loading -> {}
                        is ResultState.Success -> {
                            applySyncSuccess(result.data, userId)
                        }
                        is ResultState.Error -> {
                            val msg = result.entity.message ?: ""
                            val cleanMsg = cleanSyncErrorMessage(msg)
                            if (NetworkErrorMapper.isConnectivityMessage(cleanMsg)) {
                                networkErrorStopped = true
                                val d = _state.value.syncDialog
                                _state.updateCopy(
                                    isSyncing = false,
                                    syncDialog = d.copy(
                                        isNetworkError = true,
                                        isComplete = true,
                                        visible = false,
                                    ),
                                )
                                return@collect
                            }
                            if (NetworkErrorMapper.isSessionExpiredMessage(cleanMsg)) {
                                networkErrorStopped = true
                                val d = _state.value.syncDialog
                                _state.updateCopy(
                                    isSyncing = false,
                                    syncDialog = d.copy(
                                        isSessionExpired = true,
                                        isComplete = true,
                                        visible = false,
                                    ),
                                )
                                handleSessionExpired()
                                return@collect
                            }
                            applySyncStepError(cleanMsg, userId)
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                val d = _state.value.syncDialog
                _state.updateCopy(
                    isSyncing = false,
                    syncDialog = d.copy(
                        steps = d.steps.map {
                            if (it.status == "loading") {
                                it.copy(status = "error", details = e.message ?: "Ошибка синхронизации")
                            } else it
                        },
                        isComplete = true,
                        firstError = d.firstError ?: (e.message ?: "Ошибка синхронизации"),
                    ),
                )
            }
        }
    }

    /** Промежуточный/финальный успех: обновляет шаги по мере их завершения. */
    private fun applySyncSuccess(
        r: com.z_company.repository.remote_rest.SyncBidirectionalResult,
        userId: String?,
    ) {
        val d = _state.value.syncDialog
        val byKey = d.steps.associateBy { it.key }.toMutableMap()
        fun done(key: String, details: String, error: Boolean = false) {
            val step = byKey[key] ?: return
            byKey[key] = step.copy(
                status = if (error) "error" else "success",
                details = details,
            )
        }
        if (r.userSettingsSynced) done("UserSettings", "синхронизированы")
        if (r.salarySettingsSynced) done("SalarySettings", "синхронизированы")
        if (r.releaseDaysSynced) done("ReleaseDays", "синхронизированы")
        if (r.routesDone) {
            if (r.routeErrors.isNotEmpty()) {
                done("Routes", "синхронизировано с ошибками", error = true)
            } else {
                val details = buildString {
                    append("↑${r.routesUploaded}  ↓${r.routesDownloaded}")
                    val del = r.routesDeletedRemote + r.routesDeletedLocal
                    if (del > 0) append("  удалено $del")
                    if (r.routeWarnings.isNotEmpty()) append("\n${r.routeWarnings.joinToString("\n")}")
                }
                done("Routes", details)
            }
        }
        val ordered = d.steps.map { byKey[it.key] ?: it }
        val isFullSuccess = r.timestamp != null && r.routeErrors.isEmpty()
        _state.updateCopy(
            isSyncing = !(isFullSuccess || r.routesDone),
            syncDialog = d.copy(
                steps = ordered,
                currentLabel = syncCurrentLabel(ordered),
                percent = syncPercent(ordered),
                firstError = d.firstError ?: r.routeErrors.firstOrNull(),
                isComplete = !isFullSuccess && r.routesDone,
                isSuccess = isFullSuccess,
                // При полном успехе прогресс закрывается — AlertDialog «Синхронизация завершена!».
                visible = !isFullSuccess,
                showReportButton = !isFullSuccess && ordered.any { it.status == "error" } &&
                    r.routeErrors.all { NetworkErrorMapper.isConnectivityMessage(it).not() },
                reportSubject = "Отчет об ошибках синхронизации",
                reportBody = buildSyncReport(ordered, r.routeErrors, userId),
            ),
        )
        r.timestamp?.let {
            sharedPrefs.setLastSyncTimestamp(it)
            _state.updateCopy(
                lastSyncLabel = "Последняя синхронизация: ${formatDateTime(it, displayOffsetCache)}",
            )
        }
        if (isFullSuccess) refresh()
    }

    /** Ошибка конкретного этапа (или всех оставшихся, если этап не распознан). */
    private fun applySyncStepError(cleanMsg: String, userId: String?) {
        val d = _state.value.syncDialog
        val stepKey = parseSyncStep(cleanMsg)
        val ordered = d.steps.map {
            when {
                stepKey != null && it.key == stepKey && it.status == "loading" ->
                    it.copy(status = "error", details = cleanMsg)
                stepKey == null && it.status == "loading" ->
                    it.copy(status = "error", details = cleanMsg)
                else -> it
            }
        }
        val allDone = ordered.none { it.status == "loading" }
        _state.updateCopy(
            isSyncing = !allDone,
            syncDialog = d.copy(
                steps = ordered,
                currentLabel = syncCurrentLabel(ordered),
                percent = syncPercent(ordered),
                firstError = d.firstError ?: cleanMsg,
                isComplete = allDone,
                showReportButton = allDone && ordered.any { it.status == "error" } &&
                    !NetworkErrorMapper.isConnectivityMessage(cleanMsg),
                reportSubject = "Отчет об ошибках синхронизации",
                reportBody = buildSyncReport(ordered, emptyList(), userId),
            ),
        )
    }

    private fun parseSyncStep(message: String): String? = when {
        message.contains("UserSettings") -> "UserSettings"
        message.contains("SalarySetting") -> "SalarySettings"
        message.contains("отвлечений") -> "ReleaseDays"
        message.contains("маршрут", ignoreCase = true) -> "Routes"
        else -> null
    }

    private fun cleanSyncErrorMessage(message: String): String {
        val prefixes = listOf(
            "Ошибка сохранения UserSettings: ",
            "Ошибка сохранения SalarySetting: ",
            "Ошибка сохранения дней отвлечений: ",
            "Ошибка загрузки UserSettings: ",
            "Ошибка загрузки SalarySetting: ",
            "Ошибка загрузки дней отвлечений: ",
        )
        for (prefix in prefixes) {
            if (message.startsWith(prefix)) return message.removePrefix(prefix)
        }
        return message
    }

    /**
     * Отмена идущей синхронизации (кнопка «Отмена» в диалоге, §18.S): прерывает
     * job и закрывает диалог. Данные не портятся (merge по updatedAt / full-replace
     * идемпотентны), недоставленные маршруты остаются несинхронизированными.
     */
    fun cancelSync() {
        syncJob?.cancel()
        syncJob = null
        resetSyncState()
        _messages.tryEmit("Синхронизация отменена")
    }

    /** «Понятно» / тап вне диалога (§18.5) — сбрасывает прогресс, операция не отменяется. */
    fun resetSyncState() {
        _state.updateCopy(
            isSyncing = false,
            syncDialog = ProfileIosSyncDialog(),
        )
    }

    /**
     * Pull-to-refresh (§18.2): полная ручная `syncBidirectional` + перечитывание
     * профиля. Итоговое сообщение — в [onFinished] (snackbar экрана). Повторный
     * жест во время синхронизации игнорируется — [onFinished] получает null.
     */
    fun pullToSync(onFinished: (String?) -> Unit) {
        if (_state.value.isSyncing) {
            onFinished(null)
            return
        }
        scope.launch {
            var message: String? = null
            try {
                val setting = settingsUseCase.getUserSettingFlow().first()
                if (setting.subscriptionPeriod <= nowMs()) {
                    message = "Синхронизация доступна по подписке"
                    return@launch
                }
                val token = authSession.currentToken()
                if (token.isNullOrBlank()) {
                    message = "Необходимо войти в профиль"
                    return@launch
                }
                _state.updateCopy(isSyncing = true)
                var completed = false
                var failureMessage: String? = null
                var sessionExpired = false
                syncManager.syncBidirectional("Bearer $token").collect { result ->
                    when (result) {
                        is ResultState.Error -> {
                            if (NetworkErrorMapper.isSessionExpiredMessage(result.entity.message)) {
                                sessionExpired = true
                            }
                            failureMessage = NetworkErrorMapper.syncFailureMessage(
                                result.entity.message,
                                result.entity.throwable,
                            )
                        }
                        is ResultState.Success -> completed = result.data.routesDone
                        is ResultState.Loading -> {}
                    }
                }
                if (sessionExpired) {
                    authSession.clearSession()
                    showLoggedOutState()
                }
                message = failureMessage ?: if (!completed) {
                    "Синхронизация не выполнена: сервер не завершил обработку данных"
                } else {
                    "Синхронизация завершена"
                }
                getUserInfo()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                message = NetworkErrorMapper.syncFailureMessage(e.message, e)
            } finally {
                _state.updateCopy(isSyncing = false)
                onFinished(message)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Подсказка «Пригласить друга» (§6.9): показывается один раз
    // ═══════════════════════════════════════════════════════════════════════

    private fun isReferralHintVisible(): Boolean =
        !NSUserDefaults.standardUserDefaults.boolForKey(REFERRAL_HINT_KEY)

    fun dismissReferralHint() {
        NSUserDefaults.standardUserDefaults.setBool(true, forKey = REFERRAL_HINT_KEY)
        _state.updateCopy(showReferralHint = false)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Вспомогательное
    // ═══════════════════════════════════════════════════════════════════════

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    /**
     * Смещение пояса отображения (§0.1): Россия/Беларусь — GMT+3, Казахстан —
     * местный (`UserSettings.timeZone` — миллисекунды относительно Москвы).
     * Зеркалит `UserSettings.displayTimeZone()` из domain.
     */
    private fun displayOffsetMs(settings: UserSettings?): Long =
        when {
            settings == null -> 3 * 3_600_000L
            settings.country == "KZ" -> settings.timeZone + 10_800_000L
            else -> 3 * 3_600_000L
        }

    /** `dd.MM.yy HH:mm` (формат `DateAndTimeConverter.getDateAndTime`, core_android). */
    private fun formatDateTime(ms: Long, offsetMs: Long): String {
        val ldt = Instant.fromEpochMilliseconds(ms + offsetMs).toLocalDateTime(TimeZone.UTC)
        return "${two(ldt.dayOfMonth)}.${two(ldt.monthNumber)}.${two(ldt.year % 100)} " +
            "${two(ldt.hour)}:${two(ldt.minute)}"
    }

    private fun two(v: Int) = v.toString().padStart(2, '0')

    private fun defaultSyncSteps() = listOf(
        ProfileIosSyncStep(key = "UserSettings", label = "Настройки пользователя"),
        ProfileIosSyncStep(key = "SalarySettings", label = "Настройки зарплаты"),
        ProfileIosSyncStep(key = "ReleaseDays", label = "Отвлечения"),
        ProfileIosSyncStep(key = "Routes", label = "Маршруты"),
    )

    private fun syncCurrentLabel(steps: List<ProfileIosSyncStep>): String = when {
        steps.all { it.status == "success" } -> "Завершено"
        steps.none { it.status != "loading" } -> "Подготовка"
        else -> steps.firstOrNull { it.status == "loading" }?.label
            ?: steps.firstOrNull { it.status == "error" }?.label
            ?: "Подготовка"
    }

    private fun syncPercent(steps: List<ProfileIosSyncStep>): Int =
        steps.count { it.status != "loading" } * 25

    /**
     * Текст отчёта об ошибках синхронизации (§18.5).
     * ⚠️ Поведение Android (возможный баг, BUG-55): для полной синхронизации тип
     * пишется как «Загрузка с сервера» (подпись выбирается только между Upload
     * и остальным) — сохранено как на Android.
     */
    private fun buildSyncReport(
        steps: List<ProfileIosSyncStep>,
        routeErrors: List<String>,
        userId: String?,
    ): String = buildString {
        appendLine("Тип: Загрузка с сервера")
        appendLine("ID пользователя: ${userId ?: "—"}")
        appendLine("Ошибки этапов:")
        steps.filter { it.status == "error" }.forEach {
            appendLine("- ${it.label}: ${it.details}")
        }
        if (routeErrors.isNotEmpty()) {
            appendLine("Ошибки маршрутов:")
            routeErrors.forEach { appendLine("- $it") }
        }
    }

    private inline fun MutableStateFlow<ProfileIosState>.updateCopy(
        sessionChecked: Boolean = value.sessionChecked,
        isAuthorized: Boolean = value.isAuthorized,
        needsMigration: Boolean = value.needsMigration,
        authLoading: Boolean = value.authLoading,
        authErrorMessage: String? = value.authErrorMessage,
        registrationLoading: Boolean = value.registrationLoading,
        registrationErrorMessage: String? = value.registrationErrorMessage,
        profileLoading: Boolean = value.profileLoading,
        profileNetworkError: Boolean = value.profileNetworkError,
        emailBlockError: Boolean = value.emailBlockError,
        email: String? = value.email,
        vkId: String? = value.vkId,
        vkProfileName: String? = value.vkProfileName,
        vkAvatarUrl: String? = value.vkAvatarUrl,
        vkSdkSignedIn: Boolean = value.vkSdkSignedIn,
        vkLinkMessage: String? = value.vkLinkMessage,
        vkRegistrationOffer: Boolean = value.vkRegistrationOffer,
        hasActiveSubscription: Boolean = value.hasActiveSubscription,
        subscriptionEndLabel: String = value.subscriptionEndLabel,
        isSyncing: Boolean = value.isSyncing,
        lastSyncLabel: String = value.lastSyncLabel,
        syncDialog: ProfileIosSyncDialog = value.syncDialog,
        updateEmailLoading: Boolean = value.updateEmailLoading,
        updateEmailError: String? = value.updateEmailError,
        addEmailLoading: Boolean = value.addEmailLoading,
        addEmailError: String? = value.addEmailError,
        forgotLoading: Boolean = value.forgotLoading,
        forgotCooldownSeconds: Int = value.forgotCooldownSeconds,
        trashCount: Int = value.trashCount,
        showReferralHint: Boolean = value.showReferralHint,
        vpnHint: String? = value.vpnHint,
    ) {
        value = ProfileIosState(
            sessionChecked = sessionChecked,
            isAuthorized = isAuthorized,
            needsMigration = needsMigration,
            authLoading = authLoading,
            authErrorMessage = authErrorMessage,
            registrationLoading = registrationLoading,
            registrationErrorMessage = registrationErrorMessage,
            profileLoading = profileLoading,
            profileNetworkError = profileNetworkError,
            emailBlockError = emailBlockError,
            email = email,
            vkId = vkId,
            vkProfileName = vkProfileName,
            vkAvatarUrl = vkAvatarUrl,
            vkSdkSignedIn = vkSdkSignedIn,
            vkLinkMessage = vkLinkMessage,
            vkRegistrationOffer = vkRegistrationOffer,
            hasActiveSubscription = hasActiveSubscription,
            subscriptionEndLabel = subscriptionEndLabel,
            isSyncing = isSyncing,
            lastSyncLabel = lastSyncLabel,
            syncDialog = syncDialog,
            updateEmailLoading = updateEmailLoading,
            updateEmailError = updateEmailError,
            addEmailLoading = addEmailLoading,
            addEmailError = addEmailError,
            forgotLoading = forgotLoading,
            forgotCooldownSeconds = forgotCooldownSeconds,
            trashCount = trashCount,
            showReferralHint = showReferralHint,
            vpnHint = vpnHint,
        )
    }

    private companion object {
        const val MIN_LENGTH_PASSWORD = 4
        const val FORGOT_COOLDOWN_SECONDS = 60
        const val REFERRAL_HINT_KEY = "ld.profileReferralHintDismissed"
        val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")
    }
}
