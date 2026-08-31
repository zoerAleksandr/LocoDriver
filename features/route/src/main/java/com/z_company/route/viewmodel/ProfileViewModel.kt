package com.z_company.route.viewmodel

import android.util.Log
import io.sentry.kotlin.multiplatform.Sentry
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vk.id.AccessToken
import com.vk.id.VKID
import com.vk.id.VKIDUser
import com.vk.id.logout.VKIDLogoutCallback
import com.vk.id.logout.VKIDLogoutFail
import com.vk.id.refresh.VKIDRefreshTokenCallback
import com.vk.id.refresh.VKIDRefreshTokenFail
import com.vk.id.refreshuser.VKIDGetUserCallback
import com.vk.id.refreshuser.VKIDGetUserFail
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.sendToSentry
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.ReleaseDayUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.AuthManager
import com.z_company.repository.remote_rest.AuthState
import com.z_company.repository.remote_rest.ForgotPasswordState
import com.z_company.repository.remote_rest.ResponseState
import com.z_company.repository.remote_rest.GetUserProfileState
import com.z_company.repository.remote_rest.NetworkErrorMapper
import com.z_company.repository.remote_rest.RegistrationState
import com.z_company.repository.remote_rest.RoutesManager
import com.z_company.repository.remote_rest.SettingManager
import com.z_company.repository.remote_rest.SyncManager
import com.z_company.repository.remote_rest.UserRemote
import com.z_company.repository.remote_rest.VkAuthError
import com.z_company.use_case.SubscriptionHelper
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.util.Calendar

data class VkUserInfo(val name: String, val photoUrl: String?)

data class ProfileUiState(
    val userDetailsState: ResultState<UserRemote?> = ResultState.Loading(),
    val purchasesEndTime: ResultState<String> = ResultState.Loading(),
    val uploadState: ResultState<Int>? = null,
    val downloadState: ResultState<Int>? = null,
    val logOutState: ResultState<Unit>? = null,
    val updateAt: Long? = null,
    val dateAndTimeConverter: DateAndTimeConverter? = null,
    val isRefreshing: Boolean = false,
    val resentVerificationEmailButton: Boolean = false,
    val vkUserState: ResultState<VkUserInfo?> = ResultState.Loading(),
    val downloadRouteProgress: Pair<Int, Int>? = null,
    val updateEmailState: ResultState<Unit>? = null,
    // Привязка почты с паролем к аккаунту, заведённому через VK (email/add).
    val addEmailState: ResultState<Unit>? = null,
    val syncUploadProgress: Map<String, SyncStepState> = emptyMap(),  // Прогресс для upload (ключ - этап, значение - состояние)
    val syncDownloadProgress: Map<String, SyncStepState> = emptyMap(),  // Прогресс для download
    val syncProgress: Map<String, SyncStepState> = emptyMap(),  // Прогресс единой двусторонней синхронизации
    val showSyncDialog: Boolean = false,  // Флаг показа диалога синхронизации
    val isSyncComplete: Boolean = false,  // Флаг завершения синхронизации (для показа кнопки)
    val isSyncSuccess: Boolean = false,  // Флаг полного успеха (показывает AlertDialog вместо диалога прогресса)
    val syncType: SyncType? = null,  // Тип синхронизации (Upload или Download), чтобы знать, какой progress отображать
    val syncRouteErrors: List<String> = emptyList(),
    val syncRoutesTotalAttempted: Int = 0,
    val syncRoutesSavedCount: Int = 0,
    val syncReportUserId: String? = null,
    val isNetworkError: Boolean = false,
    val isProfileNetworkError: Boolean = false,
    // Маршруты, пропавшие с сервера в объёме, который SyncManager счёл значительным
    // (см. SyncManager.isSignificantRouteDeletion) — ждут явного подтверждения
    // пользователя перед тем, как их удалят локально.
    val pendingRouteDeletionIds: List<String> = emptyList(),
    val pendingRouteDeletionLabels: List<String> = emptyList(),
    // Текст об ошибке привязки VK ID (409 и т.п.) — показывается в snackbar
    // и сбрасывается через clearVkLinkMessage().
    val vkLinkMessage: String? = null,
    // Вход по VK, а аккаунта с этим VK нет: предлагаем создать его одним
    // подтверждением. Почта и пароль не спрашиваются — почту можно добавить
    // позже в профиле.
    val vkRegistrationOffer: Boolean = false
)

// Описание: Определяет тип синхронизации (загрузка на сервер или с сервера) для выбора правильного progress map в UI.
enum class SyncType {
    Upload,
    Download,
    Sync   // Единая двусторонняя синхронизация (одна кнопка «Синхронизация»)
}

// Описание: Состояние каждого этапа синхронизации (Loading - в процессе, Success с деталями, Error с сообщением).
sealed class SyncStepState {
    object Loading : SyncStepState()
    data class Success(val details: String) :
        SyncStepState()  // Детали успеха, напр. "загружены 15(шт)"

    data class Error(val message: String) : SyncStepState()
}

data class MigrationState(
    val isMigrating: Boolean = false,
    val migrationStep: String = "",
    val migrationResult: ResultState<Unit>? = null,
    val routesProgress: Pair<Int, Int> = 0 to 0,
    val settingsProgress: Float = 0f
)

class ProfileViewModel : ViewModel(), KoinComponent {
    private val sharedPrefs: SharedPreferencesRepositories by inject()
    private val settingsUseCase: SettingsUseCase by inject()

    private val salarySettingUseCase: SalarySettingUseCase by inject()
    private val routeUseCase: RouteUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val releaseDayUseCase: ReleaseDayUseCase by inject()
    private val authManager: AuthManager by inject()
    private val routesManager: RoutesManager by inject()
    private val syncManager: SyncManager by inject()
    private val settingManager: SettingManager by inject()

    private val subscriptionHelper: SubscriptionHelper by inject()
    private val secureTokenStorage: SecureTokenStorage by inject()
    private val snackbarManager: ISnackbarManager by inject()

    // Изменения в ProfileViewModel.kt
    // Добавлено: Новое состояние _hasSubscription для проверки наличия активной подписки (subscriptionPeriod > 0L).
    // Для чего: Чтобы в UI (ProfileScreen) условно показывать раздел "Синхронизация" только если subscriptionPeriod != 0L (пользователь оплатил). Это предотвращает доступ к синхронизации для неоплаченных пользователей и, косвенно, запись 0 на сервер (пользователь не сможет вручную запустить sync).
    val hasSubscription: StateFlow<Boolean> = settingsUseCase.getUserSettingFlow()
        .map { it.subscriptionPeriod > Calendar.getInstance().timeInMillis }  // Проверяем != 0L, предполагая, что 0L значит "не оплачено"
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )  // Дефолт false, если flow пустой

    var loginJob: Job? = null
    var forgotJob: Job? = null

    private val _userSetting = MutableStateFlow(UserSettings())
    val userSetting = _userSetting.asStateFlow()

    private val _salarySetting = MutableStateFlow(SalarySetting())
    val salarySetting = _salarySetting.asStateFlow()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    // Чтобы отслеживать, существует ли токен авторизации (залогинен ли пользователь). Это позволит в ProfileScreen условно рендерить профиль или форму входа.
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    // Чтобы определить что показывать пользователю по умолчанию, экран входа или экран регистрации
    private val _isFirstAppEntry = MutableStateFlow(false)
    val isFirstAppEntry = _isFirstAppEntry.asStateFlow()

    // Чтобы отслеживать процесс авторизации (Initial, Loading, Success, Error), аналогично _registeredUiState для регистрации. Это позволит в ProfileScreen показывать loading или ошибки.
    private val _authUiState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authUiState: StateFlow<AuthState> = _authUiState.asStateFlow()

    // Добавлено: Состояние для отслеживания, выполнена ли миграция на новый сервер
    // Для чего: Чтобы определять, нужно ли показывать экран миграции для старых пользователей
    private val _isMigrated = MutableStateFlow(false)
    val isMigrated = _isMigrated.asStateFlow()

    // Добавлено: Состояние процесса миграции
    // Для чего: Чтобы отслеживать шаги переноса данных (маршруты, настройки) и результат
    private val _migrationUiState = MutableStateFlow(MigrationState())
    val migrationUiState = _migrationUiState.asStateFlow()

    private val _responseState = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Initial)
    val forgotEmailState = _responseState.asStateFlow()

    // Локальный кулдаун кнопки «Создать новый пароль» — защита от 429.
    private val _forgotCooldownSeconds = MutableStateFlow(0)
    val forgotCooldownSeconds = _forgotCooldownSeconds.asStateFlow()
    private var forgotCooldownJob: Job? = null


    var currentEmail by mutableStateOf("")
        private set

    private var loadSettingsJob: Job? = null
    private var getUserInfoJob: Job? = null

    init {
        getUserInfo()
        loadSettingsForSyncInfo()

        // Чтобы сразу определить, залогинен ли пользователь (токен существует и не пустой). Это обновит _isLoggedIn, которое используется в ProfileScreen для условного рендеринга.
//        viewModelScope.launch(Dispatchers.IO) {
//            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
//            _isLoggedIn.value = !token.isNullOrEmpty()
//        }
        viewModelScope.launch {
            _isFirstAppEntry.value = sharedPrefs.tokenIsFirstAppEntry()
            _isMigrated.value = sharedPrefs.isMigrated()
        }
        viewModelScope.launch(Dispatchers.IO) {
            secureTokenStorage.getVkIdFlow().onEach { vkId ->
                if (vkId != null && vkId.isNotEmpty()) {
                    vkIdRefreshToken()
                } else {
                    _uiState.update { it.copy(vkUserState = ResultState.Success(null)) }
                }
            }.launchIn(viewModelScope)
        }
    }

    /**
     * Единая двусторонняя синхронизация («Синхронизация» — одна кнопка вместо
     * «Сохранить в облако» / «Загрузить из облака»). Push+pull настроек и merge
     * маршрутов по updatedAt с распространением удалений в обе стороны — всё в
     * [SyncManager.syncBidirectional].
     */
    fun startSync() {
        viewModelScope.launch(Dispatchers.IO) {
            val setting = settingsUseCase.getUserSettingFlow().first()
            if (setting.subscriptionPeriod <= Calendar.getInstance().timeInMillis) {
                snackbarManager.show("Синхронизация доступна по подписке")
                return@launch
            }
            val token = secureTokenStorage.getAuthBearerTokenFlow().first() ?: return@launch
            val userId = secureTokenStorage.getUserIdFlow().first()
            _uiState.update {
                it.copy(
                    showSyncDialog = true,
                    syncType = SyncType.Sync,
                    syncProgress = mapOf(
                        "UserSettings" to SyncStepState.Loading,
                        "SalarySettings" to SyncStepState.Loading,
                        "ReleaseDays" to SyncStepState.Loading,
                        "Routes" to SyncStepState.Loading
                    ),
                    isSyncComplete = false,
                    isSyncSuccess = false,
                    isNetworkError = false,
                    syncReportUserId = userId,
                    syncRouteErrors = emptyList(),
                    syncRoutesTotalAttempted = 0,
                    syncRoutesSavedCount = 0
                )
            }

            var networkErrorStopped = false
            try {
                syncManager.syncBidirectional("Bearer $token").collect { state ->
                    if (networkErrorStopped) return@collect
                    when (state) {
                        is ResultState.Loading -> {}
                        is ResultState.Success -> {
                            val r = state.data
                            val p = _uiState.value.syncProgress.toMutableMap()
                            if (r.userSettingsSynced) p["UserSettings"] = SyncStepState.Success("синхронизированы")
                            if (r.salarySettingsSynced) p["SalarySettings"] = SyncStepState.Success("синхронизированы")
                            if (r.releaseDaysSynced) p["ReleaseDays"] = SyncStepState.Success("синхронизированы")
                            if (r.routesDone) {
                                if (r.routeErrors.isNotEmpty()) {
                                    p["Routes"] = SyncStepState.Error("синхронизировано с ошибками")
                                } else {
                                    val details = buildString {
                                        append("↑${r.routesUploaded}  ↓${r.routesDownloaded}")
                                        val del = r.routesDeletedRemote + r.routesDeletedLocal
                                        if (del > 0) append("  удалено $del")
                                        if (r.routeWarnings.isNotEmpty()) append("\n${r.routeWarnings.joinToString("\n")}")
                                    }
                                    p["Routes"] = SyncStepState.Success(details)
                                }
                            }
                            val isFullSuccess = r.timestamp != null && r.routeErrors.isEmpty()
                            val hasPendingDeletion = r.pendingDeletionRouteIds.isNotEmpty()
                            _uiState.update {
                                it.copy(
                                    syncProgress = p,
                                    isSyncComplete = !isFullSuccess && r.routesDone,
                                    isSyncSuccess = isFullSuccess,
                                    // Диалог прогресса закрываем и при полном успехе, и когда
                                    // нужно подтверждение удаления — его показывает отдельный
                                    // AppAlertDialog поверх экрана (см. pendingRouteDeletionIds).
                                    showSyncDialog = !isFullSuccess && !hasPendingDeletion,
                                    syncRouteErrors = r.routeErrors,
                                    syncRoutesTotalAttempted = r.routesUploaded + r.routesDownloaded + r.routeErrors.size,
                                    syncRoutesSavedCount = r.routesUploaded + r.routesDownloaded,
                                    pendingRouteDeletionIds = r.pendingDeletionRouteIds,
                                    pendingRouteDeletionLabels = r.pendingDeletionLabels
                                )
                            }
                            r.timestamp?.let { sharedPrefs.setLastSyncTimestamp(it) }
                            if (isFullSuccess) refresh()
                        }

                        is ResultState.Error -> {
                            val msg = state.entity.message ?: ""
                            val cleanMsg = cleanSyncErrorMessage(msg)
                            if (isNetworkErrorMessage(cleanMsg)) {
                                networkErrorStopped = true
                                _uiState.update { it.copy(isNetworkError = true, isSyncComplete = true) }
                                return@collect
                            }
                            val stepKey = parseSyncStep(msg)
                            val p = _uiState.value.syncProgress.toMutableMap()
                            if (stepKey != null && p[stepKey] is SyncStepState.Loading) {
                                p[stepKey] = SyncStepState.Error(cleanMsg)
                                _uiState.update { it.copy(syncProgress = p) }
                            } else {
                                p.replaceAll { _, v -> if (v is SyncStepState.Loading) SyncStepState.Error(cleanMsg) else v }
                                _uiState.update { it.copy(syncProgress = p, isSyncComplete = true) }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                val p = _uiState.value.syncProgress.mapValues {
                    if (it.value is SyncStepState.Loading) SyncStepState.Error(e.message ?: "Ошибка синхронизации") else it.value
                }
                _uiState.update { it.copy(syncProgress = p, isSyncComplete = true) }
            }
        }
    }

    /**
     * Пользователь подтвердил в диалоге, что маршруты из [ProfileUiState.pendingRouteDeletionIds]
     * действительно нужно удалить локально (они уже отсутствуют на сервере).
     */
    fun confirmPendingRouteDeletions() {
        val ids = _uiState.value.pendingRouteDeletionIds
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            syncManager.applyPendingRouteDeletions(ids).collect { state ->
                when (state) {
                    is ResultState.Success -> {
                        _uiState.update {
                            it.copy(pendingRouteDeletionIds = emptyList(), pendingRouteDeletionLabels = emptyList())
                        }
                        snackbarManager.show("Удалено маршрутов: ${state.data}")
                        refresh()
                    }
                    is ResultState.Error -> {
                        snackbarManager.show(
                            "Не удалось удалить маршруты: ${state.entity.message ?: NetworkErrorMapper.humanMessage(state.entity.throwable)}"
                        )
                    }
                    is ResultState.Loading -> {}
                }
            }
        }
    }

    /**
     * Пользователь отказался удалять — маршруты остаются локально как есть.
     * При следующей синхронизации SyncManager снова предложит то же самое (если
     * сервер по-прежнему их не отдаёт), пока пользователь не подтвердит удаление
     * или маршруты не появятся на сервере вновь.
     */
    fun dismissPendingRouteDeletions() {
        _uiState.update {
            it.copy(pendingRouteDeletionIds = emptyList(), pendingRouteDeletionLabels = emptyList())
        }
    }

    private fun parseSyncStep(message: String): String? = when {
        message.contains("UserSettings") -> "UserSettings"
        message.contains("SalarySetting") -> "SalarySettings"
        message.contains("отвлечений") -> "ReleaseDays"
        message.contains("маршрут", ignoreCase = true) -> "Routes"
        else -> null
    }

    // Описание: Запускает синхронизацию на сервер (upload), показывает диалог, обновляет прогресс поэтапно через collect Flow.
    // При получении промежуточных Success - обновляет progress map. При final Success/Error - устанавливает isSyncComplete = true.
    fun startSyncUpload() {
        viewModelScope.launch(Dispatchers.IO) {
            // Защита на случай, если раздел «Синхронизация» окажется доступен без
            // активной подписки: upload в облако — платная функция (тот же гейт,
            // что в SyncWorker и в ручной синхронизации маршрутов).
            val setting = settingsUseCase.getUserSettingFlow().first()
            if (setting.subscriptionPeriod <= Calendar.getInstance().timeInMillis) {
                snackbarManager.show("Синхронизация доступна по подписке")
                return@launch
            }
            val token = secureTokenStorage.getAuthBearerTokenFlow().first() ?: return@launch
            val userId = secureTokenStorage.getUserIdFlow().first()
            _uiState.update {
                it.copy(
                    showSyncDialog = true,
                    syncType = SyncType.Upload,
                    syncUploadProgress = mapOf(  // Инициализируем шаги с Loading
                        "UserSettings" to SyncStepState.Loading,
                        "SalarySettings" to SyncStepState.Loading,
                        "ReleaseDays" to SyncStepState.Loading,
                        "Routes" to SyncStepState.Loading
                    ),
                    isSyncComplete = false,
                    syncReportUserId = userId
                )
            }

            var networkErrorStopped = false
            try {
                syncManager.syncToRemote("Bearer $token").collect { state ->
                    if (networkErrorStopped) return@collect
                    when (state) {
                        is ResultState.Loading -> {}
                        is ResultState.Success -> {
                            val result = state.data
                            val newProgress = _uiState.value.syncUploadProgress.toMutableMap()

                            if (result.userSettingsSaved) newProgress["UserSettings"] = SyncStepState.Success("загружены")
                            if (result.salarySettingsSaved) newProgress["SalarySettings"] = SyncStepState.Success("загружены")
                            if (result.releaseDaysSaved) newProgress["ReleaseDays"] = SyncStepState.Success("загружены")
                            val routeErrors = result.routeErrors
                            if (result.routesSavedCount >= 0) {
                                if (routeErrors.isNotEmpty()) {
                                    newProgress["Routes"] = SyncStepState.Error("синхронизировано ${result.routesSavedCount} из ${routeErrors.size + result.routesSavedCount}")
                                } else {
                                    val details = buildString {
                                        append("загружены ${result.routesSavedCount}(шт)")
                                        if (result.routeWarnings.isNotEmpty()) append("\n${result.routeWarnings.joinToString("\n")}")
                                    }
                                    newProgress["Routes"] = SyncStepState.Success(details)
                                }
                            }

                            // Финальный Success (timestamp != null) → всё успешно, показываем AlertDialog
                            val isFullSuccess = result.timestamp != null && routeErrors.isEmpty()
                            _uiState.update {
                                it.copy(
                                    syncUploadProgress = newProgress,
                                    isSyncComplete = !isFullSuccess,
                                    isSyncSuccess = isFullSuccess,
                                    showSyncDialog = !isFullSuccess,
                                    syncRouteErrors = routeErrors,
                                    syncRoutesTotalAttempted = routeErrors.size + result.routesSavedCount,
                                    syncRoutesSavedCount = result.routesSavedCount
                                )
                            }
                            result.timestamp?.let { sharedPrefs.setLastSyncTimestamp(it) }
                            if (isFullSuccess) refresh()
                        }

                        is ResultState.Error -> {
                            val msg = state.entity.message ?: ""
                            val cleanMsg = cleanSyncErrorMessage(msg)
                            if (isNetworkErrorMessage(cleanMsg)) {
                                networkErrorStopped = true
                                _uiState.update { it.copy(isNetworkError = true, isSyncComplete = true) }
                                return@collect
                            }
                            val stepKey = parseSyncUploadStep(msg)
                            val newProgress = _uiState.value.syncUploadProgress.toMutableMap()
                            if (stepKey != null) {
                                // Промежуточная ошибка конкретного шага — обновляем только его
                                newProgress[stepKey] = SyncStepState.Error(message = cleanMsg)
                                _uiState.update { it.copy(syncUploadProgress = newProgress) }
                            } else {
                                // Финальная ошибка — помечаем оставшиеся Loading и завершаем
                                newProgress.replaceAll { _, v ->
                                    if (v is SyncStepState.Loading) SyncStepState.Error(message = msg) else v
                                }
                                _uiState.update {
                                    it.copy(syncUploadProgress = newProgress, isSyncComplete = true)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                val newProgress = _uiState.value.syncUploadProgress.mapValues {
                    if (it.value is SyncStepState.Loading) SyncStepState.Error(message = e.message ?: "Ошибка синхронизации") else it.value
                }
                _uiState.update { it.copy(syncUploadProgress = newProgress, isSyncComplete = true) }
            }
        }
    }

    // Экран «Нет интернета» показываем только для реальных транспортных ошибок.
    // Ответ сервера с кодом ошибки (валидация 422, сбой 5xx) сюда не попадает —
    // такие сообщения показываются как ошибка конкретного шага с деталями.
    private fun isNetworkErrorMessage(msg: String): Boolean =
        NetworkErrorMapper.isConnectivityMessage(msg)


    private fun parseSyncUploadStep(message: String): String? = when {
        message.contains("UserSettings") -> "UserSettings"
        message.contains("SalarySetting") -> "SalarySettings"
        message.contains("отвлечений") -> "ReleaseDays"
        else -> null
    }

    private fun cleanSyncErrorMessage(message: String): String {
        val prefixes = listOf(
            "Ошибка сохранения UserSettings: ",
            "Ошибка сохранения SalarySetting: ",
            "Ошибка сохранения дней отвлечений: ",
            "Ошибка загрузки UserSettings: ",
            "Ошибка загрузки SalarySetting: ",
            "Ошибка загрузки дней отвлечений: "
        )
        for (prefix in prefixes) {
            if (message.startsWith(prefix)) return message.removePrefix(prefix)
        }
        return message
    }

    fun firstUpload() {
        viewModelScope.launch(Dispatchers.IO) {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first() ?: return@launch
            syncManager.firstSyncAfterRegistration("Bearer $token").collect { result ->
                when (result) {
                    is ResultState.Loading -> {

                    }

                    is ResultState.Success -> {
                        result.data.timestamp?.let { sharedPrefs.setLastSyncTimestamp(it) }
                        refresh()
                    }

                    is ResultState.Error -> {

                    }
                }
            }
        }
    }

    // Описание: Аналогично startSyncUpload, но для загрузки с сервера (download).
    fun startSyncDownload() {
        viewModelScope.launch(Dispatchers.IO) {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first() ?: return@launch
            val userId = secureTokenStorage.getUserIdFlow().first()
            _uiState.update {
                it.copy(
                    showSyncDialog = true,
                    syncType = SyncType.Download,
                    syncDownloadProgress = mapOf(  // Инициализируем шаги с Loading
                        "ReleaseDays" to SyncStepState.Loading,
                        "SalarySettings" to SyncStepState.Loading,
                        "UserSettings" to SyncStepState.Loading,
                        "Routes" to SyncStepState.Loading
                    ),
                    isSyncComplete = false,
                    syncReportUserId = userId
                )
            }

            var networkErrorStopped = false
            try {
                syncManager.syncFromRemote("Bearer $token").collect { state ->
                    if (networkErrorStopped) return@collect
                    when (state) {
                        is ResultState.Loading -> {}
                        is ResultState.Success -> {
                            val result = state.data
                            val newProgress = _uiState.value.syncDownloadProgress.toMutableMap()

                            if (result.releaseDaysLoaded) newProgress["ReleaseDays"] = SyncStepState.Success("загружены")
                            if (result.salarySettingsLoaded) newProgress["SalarySettings"] = SyncStepState.Success("загружены")
                            if (result.userSettingsLoaded) newProgress["UserSettings"] = SyncStepState.Success("загружены")
                            if (result.routesLoadedCount >= 0) newProgress["Routes"] = SyncStepState.Success("загружены ${result.routesLoadedCount}(шт)")

                            val isFullSuccess = result.userSettingsLoaded && result.salarySettingsLoaded &&
                                result.releaseDaysLoaded && result.routesLoadedCount >= 0
                            _uiState.update {
                                it.copy(
                                    syncDownloadProgress = newProgress,
                                    isSyncComplete = !isFullSuccess,
                                    isSyncSuccess = isFullSuccess,
                                    showSyncDialog = !isFullSuccess
                                )
                            }
                            if (isFullSuccess) refresh()
                        }

                        is ResultState.Error -> {
                            val msg = state.entity.message ?: ""
                            val cleanMsg = cleanSyncErrorMessage(msg)
                            if (isNetworkErrorMessage(cleanMsg)) {
                                networkErrorStopped = true
                                _uiState.update { it.copy(isNetworkError = true, isSyncComplete = true) }
                                return@collect
                            }
                            val stepKey = parseSyncDownloadStep(msg)
                            val newProgress = _uiState.value.syncDownloadProgress.toMutableMap()
                            if (stepKey != null) {
                                newProgress[stepKey] = SyncStepState.Error(message = cleanMsg)
                                _uiState.update { it.copy(syncDownloadProgress = newProgress) }
                            } else {
                                newProgress.replaceAll { _, v ->
                                    if (v is SyncStepState.Loading) SyncStepState.Error(message = msg) else v
                                }
                                _uiState.update {
                                    it.copy(syncDownloadProgress = newProgress, isSyncComplete = true)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                val newProgress = _uiState.value.syncDownloadProgress.mapValues {
                    if (it.value is SyncStepState.Loading) SyncStepState.Error(message = e.message ?: "Ошибка синхронизации") else it.value
                }
                _uiState.update { it.copy(syncDownloadProgress = newProgress, isSyncComplete = true) }
            }
        }
    }

    private fun parseSyncDownloadStep(message: String): String? = when {
        message.contains("UserSettings") -> "UserSettings"
        message.contains("SalarySetting") -> "SalarySettings"
        message.contains("отвлечений") -> "ReleaseDays"
        message.contains("маршрут") -> "Routes"
        else -> null
    }


    // Для чего: Чтобы сбросить прогресс и диалог после закрытия (вызывается из UI).
    fun resetSyncState() {
        _uiState.update {
            it.copy(
                showSyncDialog = false,
                syncUploadProgress = emptyMap(),
                syncDownloadProgress = emptyMap(),
                syncProgress = emptyMap(),
                isSyncComplete = false,
                isSyncSuccess = false,
                isNetworkError = false,
                syncType = null,
                syncRouteErrors = emptyList(),
                syncRoutesTotalAttempted = 0,
                syncRoutesSavedCount = 0,
                syncReportUserId = null
            )
        }
    }

    // Для чего: Вызывается, когда VK ID существует, чтобы получить имя и фамилию с помощью VKID.instance.getUserInfo, который обрабатывает refresh токена автоматически.
    private suspend fun getVkUserInfo() {
        VKID.instance.getUserData(object : VKIDGetUserCallback {
            override fun onSuccess(user: VKIDUser) {
                val fullName = "${user.firstName} ${user.lastName}"
                _uiState.update {
                    it.copy(vkUserState = ResultState.Success(VkUserInfo(fullName, user.photo200)))
                }
            }

            override fun onFail(fail: VKIDGetUserFail) {
                _uiState.update { it.copy(vkUserState = ResultState.Error(ErrorEntity(message = fail.description))) }
            }
        })
    }

    private fun loadSettingsForSyncInfo() {
        viewModelScope.launch {
            val ss = salarySettingUseCase.salarySettingFlow().first()
            _salarySetting.value = ss
        }
        var updateAt = sharedPrefs.getLastSyncTimestamp()
        if (updateAt == 0L) {
            updateAt = Calendar.getInstance().timeInMillis
        }
        loadSettingsJob = settingsUseCase.getFlowCurrentSettingsState().onEach { result ->
            if (result is ResultState.Success) {
                result.data?.let { settings ->
                    _userSetting.value = settings
                    val purchaseTimeEnd = settings.subscriptionPeriod
                    val dateAndTimeConverter = DateAndTimeConverter(settings)
                    val date = dateAndTimeConverter.getDateAndTime(purchaseTimeEnd)
                    val textPurchase = if (purchaseTimeEnd > Calendar.getInstance().timeInMillis) {
                        "Оплачено до $date"
                    } else if (purchaseTimeEnd == 0L) {
                        "Оплатить 69₽ -> 1 месяц"
                    } else {
                        "Срок оплаты истек $date"
                    }
                    _uiState.update {
                        it.copy(
                            updateAt = updateAt,
                            dateAndTimeConverter = dateAndTimeConverter,
                            purchasesEndTime = ResultState.Success(textPurchase)
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch(Dispatchers.IO) {
            getUserInfo()
            loadSettingsForSyncInfo()
            // Перезагружаем пользователя (flow уже обновится)
            delay(500)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    // Для чего: Запускает корутину для API-запроса на смену email. Обновляет uiState: сначала Loading, затем Success или Error на основе ответа API.
    fun updateEmail(newEmail: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(updateEmailState = ResultState.Loading()) }
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            val fullToken = "Bearer $token"
            val result = authManager.updateEmail(
                token = fullToken,
                email = newEmail
            )

            result.collect { state ->
                when (state) {
                    is ResponseState.Success -> {
                        _uiState.update {
                            it.copy(
                                updateEmailState = ResultState.Success(Unit),
                            )
                        }
//                        currentEmail = newEmail  // Обновляем currentEmail в ViewModel
                        getUserInfo()
                    }

                    is ResponseState.Error -> {
                        _uiState.update {
                            it.copy(
                                updateEmailState = ResultState.Error(
                                    ErrorEntity(
                                        message = state.errorMessage
                                    )
                                )
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    // Новый метод: resetUpdateEmailState
    // Для чего: Сбрасывает состояние updateEmailState в null после обработки Success/Error, чтобы избежать повторных LaunchedEffect в UI.
    fun resetUpdateEmailState() {
        _uiState.update { it.copy(updateEmailState = null) }
    }

    /**
     * Привязка почты и пароля к аккаунту, заведённому через VK.
     * У таких аккаунтов пароль пустой, и VK — единственный способ войти:
     * отвалится привязка VK — доступ потерян. После успеха профиль
     * перечитывается, чтобы в UI появилась почта.
     */
    fun addEmail(newEmail: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(addEmailState = ResultState.Loading()) }
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            val fullToken = "Bearer $token"
            authManager.addEmail(
                token = fullToken,
                email = newEmail,
                password = password
            ).collect { state ->
                when (state) {
                    is ResponseState.Success -> {
                        _uiState.update { it.copy(addEmailState = ResultState.Success(Unit)) }
                        getUserInfo()
                    }

                    is ResponseState.Error -> {
                        _uiState.update {
                            it.copy(
                                addEmailState = ResultState.Error(
                                    ErrorEntity(message = state.errorMessage)
                                )
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    /** Сброс состояния привязки почты после показа результата в UI. */
    fun resetAddEmailState() {
        _uiState.update { it.copy(addEmailState = null) }
    }

    fun resetUploadState() = _uiState.update { it.copy(uploadState = null) }
    fun resetDownloadState() = _uiState.update { it.copy(downloadState = null) }

    fun logOut() {
        viewModelScope.launch(Dispatchers.IO) {
            secureTokenStorage.saveAuthToken("")
            secureTokenStorage.saveVkId("")
            _isLoggedIn.value = false
        }
    }

    /**
     * Централизованная обработка просроченного (невалидного) bearer-токена.
     *
     * Сервер вернул 401 — локальная сессия больше не действительна. Полностью
     * разлогиниваем пользователя (как [logOut]), чтобы стухший токен не остался
     * в хранилище и не вызывал мигание «залогинен → форма входа» при следующем
     * открытии экрана. Профиль сбрасываем в состояние формы входа
     * (`Success(null)`), а не «ошибка загрузки», и поясняем причину.
     */
    private suspend fun handleSessionExpired() {
        secureTokenStorage.saveAuthToken("")
        secureTokenStorage.saveVkId("")
        _isLoggedIn.value = false
        _uiState.update {
            it.copy(
                userDetailsState = ResultState.Success(null),
                vkUserState = ResultState.Success(null),
                isProfileNetworkError = false
            )
        }
        snackbarManager.show("Сессия истекла. Войдите снова.")
    }

    // Чтобы выполнять вход с email и password, обновлять состояние авторизации, сохранять токен при успехе и обновлять _isLoggedIn. Это вызывается из кнопки "Войти" в ProfileScreen.
    fun authWithEmail(email: String, password: String) {
        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            authManager.authWithEmail(email = email, password = password).collect { state ->
                if (state is AuthState.Success) {
                    val token = state.accessToken
                    if (token.isNotEmpty()) {
                        secureTokenStorage.saveAuthToken(token)  // Сохранение зашифрованного токена
                        _isLoggedIn.value = true  // Обновляем состояние логина после успеха
                        restoreSubscriptionAfterLogin(token)
                        refresh()  // Перезагружаем данные после входа
                        syncManager.syncFromRemote("Bearer $token").collect {}
                    }
                }
                _authUiState.value = state  // Обновляем UI-состояние
            }
        }
    }

    /**
     * @param vkAccessToken токен из VKID SDK. Уходит ровно в один сетевой
     *   запрос: не сохраняем его ни в SecureTokenStorage, ни в логи/Sentry.
     *   Локально по-прежнему храним только vk id — как признак «VK привязан».
     * @param email почта из данных VK. Нужна не для входа, а на случай
     *   [VkAuthError.UserNotFound]: тогда из неё соберётся регистрация.
     *
     * Если аккаунта с этим VK нет, ошибка в UI не уходит — вместо неё
     * поднимается [ProfileUiState.vkRegistrationOffer] («создать аккаунт?»).
     */
    fun authWithVKID(vkid: String, vkAccessToken: String, email: String) {
        loginJob?.cancel()
        val handler = CoroutineExceptionHandler { _, throwable ->
            // VK SDK может бросить при отмене капчи во время OAuth
            Sentry.captureMessage("authWithVKID uncaught: ${throwable.message}")
        }
        loginJob = viewModelScope.launch(handler) {
            try {
                authManager.authWithVKID(vkid, vkAccessToken).collect { state ->
                    if (state is AuthState.Success) {
                        val token = state.accessToken
                        if (token.isNotEmpty()) {
                            secureTokenStorage.saveAuthToken(token)
                            secureTokenStorage.saveVkId(vkid)
                            _isLoggedIn.value = true
                            restoreSubscriptionAfterLogin(token)
                            refresh()
                            syncManager.syncFromRemote("Bearer $token").collect {}
                        }
                    }
                    if (state is AuthState.Error && state.vkError == VkAuthError.UserNotFound) {
                        // Аккаунта с этим VK нет. Не показываем ошибку входа —
                        // держим токен в памяти VM и предлагаем регистрацию.
                        pendingVkRegistration =
                            PendingVkRegistration(vkid, vkAccessToken, email)
                        _uiState.update { it.copy(vkRegistrationOffer = true) }
                        _authUiState.value = AuthState.Initial
                    } else {
                        _authUiState.value = state
                    }
                }
            } catch (e: Exception) {
                Sentry.captureMessage("authWithVKID exception: ${e.message}")
            }
        }
    }

    /**
     * Данные для регистрации по VK, отложенные до подтверждения пользователем.
     * Живут только в памяти ViewModel и стираются сразу после ответа сервера
     * или отказа — access token не должен пережить флоу.
     */
    private data class PendingVkRegistration(
        val vkid: String,
        val vkAccessToken: String,
        val email: String
    )

    private var pendingVkRegistration: PendingVkRegistration? = null

    /**
     * Срок подписки должен обновиться как обязательная часть успешного входа,
     * а не побочный эффект последующего refresh/full sync. Полная синхронизация
     * может занять до 25 секунд или завершиться частично, при этом вход уже
     * считается успешным и локальное значение иначе осталось бы равным 0.
     */
    private suspend fun restoreSubscriptionAfterLogin(token: String) {
        when (subscriptionHelper.restorePurchases(token = token)) {
            is ResultState.Error -> snackbarManager.show(
                "Вход выполнен, но данные подписки не загрузились"
            )
            else -> Unit
        }
    }

    /**
     * Согласие на создание аккаунта по VK. Почта берётся из данных VK (может
     * быть пустой), пароль не задаётся — его и почту пользователь при желании
     * добавит в профиле.
     */
    fun confirmVkRegistration() {
        val pending = pendingVkRegistration ?: return
        pendingVkRegistration = null
        _uiState.update { it.copy(vkRegistrationOffer = false) }
        registeredUserByVKID(
            vkid = pending.vkid,
            vkAccessToken = pending.vkAccessToken,
            email = pending.email
        )
    }

    /** Отказ от регистрации: забываем access token вместе с остальными данными. */
    fun dismissVkRegistration() {
        pendingVkRegistration = null
        _uiState.update { it.copy(vkRegistrationOffer = false) }
    }

    // Чтобы сбросить _authUiState в Initial после показа ошибки в Snackbar в ProfileScreen, чтобы избежать повторных отображений.
    fun resetAuthState() {
        _authUiState.value = AuthState.Initial
    }

    fun resetRegisteredState() {
        _registeredUiState.value = RegistrationState.Initial
    }

    private val _registeredUiState =
        MutableStateFlow<RegistrationState>(RegistrationState.Initial)  // Начальное состояние (опционально)
    val registeredUiState: StateFlow<RegistrationState> = _registeredUiState.asStateFlow()

    fun registeredUserByEmail(email: String, password: String) {
        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            // Пояснение: Запускаем корутину для collect Flow (Flow холодный, стартует здесь).
            authManager.registerByEmail(email = email, password = password)
                .collect { state ->  // Collect эмитит значения из Flow
                    if (state is RegistrationState.Success) {
                        val token = state.accessToken
                        if (token.isNotEmpty()) {
                            // Сохранение зашифрованного токена
                            secureTokenStorage.saveAuthToken(token)
                            val localUserSettings = settingsUseCase.getUserSettingFlow().first()
                            val endTimeSubscription = sharedPrefs.getSubscriptionExpiration()
                            if (endTimeSubscription != 0L) {
                                val l = localUserSettings.copy(
                                    subscriptionPeriod = endTimeSubscription
                                )
                                settingsUseCase.saveSetting(l)
                                    .first { it is ResultState.Success || it is ResultState.Error }
                            }

                            firstUpload()
                            _isLoggedIn.value = true  // Обновляем состояние логина после успеха
                            refresh()  // Перезагружаем данные после входа}
                        }
                    }
                    // Обновляем UI-состояние на основе эмитов (Loading, Success, Error)
                    _registeredUiState.value = state
                }
        }
    }

    fun registeredUserByVKID(vkid: String, vkAccessToken: String, email: String) {
        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            // Пояснение: Запускаем корутину для collect Flow (Flow холодный, стартует здесь).
            authManager.registerByVKID(vkid, vkAccessToken, email)
                .collect { state ->  // Collect эмитит значения из Flow
                    if (state is RegistrationState.Success) {
                        val token = state.accessToken
                        if (token.isNotEmpty()) {
                            // Сохранение зашифрованного токена
                            secureTokenStorage.saveAuthToken(token)
                            secureTokenStorage.saveVkId(vkid)
                            firstUpload()
                            _isLoggedIn.value = true  // Обновляем состояние логина после успеха
                            refresh()  // Перезагружаем данные после входа}
                        }
                    }
                    // Обновляем UI-состояние на основе эмитов (Loading, Success, Error)
                    _registeredUiState.value = state
                }
        }
    }

    /**
     * Отвязка VK от аккаунта: снимаем привязку на сервере, чистим локальный
     * признак и завершаем сессию VK SDK, чтобы OneTap в следующий раз спросил
     * аккаунт заново.
     *
     * Ошибку кладём в [ProfileUiState.vkLinkMessage] — молчаливый провал здесь
     * особенно вреден: человек считает, что VK отвязан, хотя он на месте.
     */
    fun removeUsersVKID() {
        viewModelScope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            val fullToken = "Bearer $token"
            authManager.removeVKID(fullToken).collect { state ->
                if (state is GetUserProfileState.Error) {
                    _uiState.update { it.copy(vkLinkMessage = state.message) }
                }
                if (state is GetUserProfileState.Success) {
                    secureTokenStorage.saveVkId("")  // Очистка VK ID
                    VKID.instance.logout(
                        callback = object : VKIDLogoutCallback {
                            override fun onSuccess() {
                                // Пользователю отправляется уведомление, что произошел выход из аккаунта.
                            }

                            override fun onFail(fail: VKIDLogoutFail) {
                                when (fail) {
                                    is VKIDLogoutFail.FailedApiCall -> fail.description // Использование текста ошибки.
                                    is VKIDLogoutFail.NotAuthenticated -> fail.description // Использование текста ошибки.
                                    is VKIDLogoutFail.AccessTokenTokenExpired -> fail // Ошибка истечения срока жизни AT. Это уведомление о том, что токен уже просрочен и разлогиниваться не нужно.
                                }
                            }
                        }
                    )
                    refresh()  // Шапка профиля должна вернуться к «Войдите через VK ID»
                }
            }
        }
    }


    fun getUserInfo() {
        getUserInfoJob?.cancel()
        getUserInfoJob = viewModelScope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (!token.isNullOrBlank()) {
                // Токен есть — сразу показываем профиль (не ждём сетевого ответа).
                // На 401 откатываемся обратно.
                _isLoggedIn.value = true
                val fullToken = "Bearer $token"
                authManager.getUserProfile(fullToken).collect { state ->
                    if (state is GetUserProfileState.Loading) {
                        _uiState.update {
                            it.copy(userDetailsState = ResultState.Loading("Получаем данные пользователя..."))
                        }
                    }
                    if (state is GetUserProfileState.Success) {
                        currentEmail = state.user.email
                        secureTokenStorage.saveUserId(state.user.id)
                        _uiState.update {
                            it.copy(
                                userDetailsState = ResultState.Success(state.user),
                                isProfileNetworkError = false
                            )
                        }
                        subscriptionHelper.restorePurchases(null, token)

                        // Если сервер знает о привязанном VK-аккаунте — сохраняем vkId локально.
                        // Наблюдатель getVkIdFlow() в init {} автоматически вызовет
                        // vkIdRefreshToken() → getVkUserInfo() и покажет имя/фото пользователя.
                        val serverVkId = state.user.vkId
                        if (serverVkId.isNotEmpty()) {
                            val localVkId = secureTokenStorage.getVkIdFlow().first()
                            if (localVkId.isNullOrEmpty()) {
                                secureTokenStorage.saveVkId(serverVkId)
                            }
                        }
                    }
                    if (state is GetUserProfileState.Error) {
                        if (state.code == 401) {
                            // Просроченный/невалидный bearer-токен — сессия истекла.
                            // Разлогиниваем и показываем форму входа, а не «ошибку загрузки».
                            handleSessionExpired()
                        } else {
                            val networkErr = state.code == 0 || isNetworkErrorMessage(state.message ?: "")
                            _uiState.update {
                                it.copy(
                                    userDetailsState = ResultState.Error(ErrorEntity(message = state.message)),
                                    isProfileNetworkError = networkErr
                                )
                            }
                        }
                    }
                }
            } else {
                _isLoggedIn.value = false
                _uiState.update {
                    it.copy(userDetailsState = ResultState.Success(null))
                }
            }
        }
    }

    fun forgotRequest(email: String) {
        // Локальный кулдаун ещё активен — не шлём повторный запрос.
        if (_forgotCooldownSeconds.value > 0) return

        forgotJob?.cancel()
        forgotJob = viewModelScope.launch {
            authManager.forgotPassword(email.filterNot { it.isWhitespace() }).collect { state ->
                _responseState.value = state
                // 200 и 429 — оба завершают попытку, запускаем кулдаун.
                if (state is ForgotPasswordState.Success || state is ForgotPasswordState.RateLimited) {
                    startForgotCooldown()
                }
            }
        }
    }

    private fun startForgotCooldown() {
        forgotCooldownJob?.cancel()
        forgotCooldownJob = viewModelScope.launch {
            var left = FORGOT_COOLDOWN_SECONDS
            while (left > 0) {
                _forgotCooldownSeconds.value = left
                delay(1000L)
                left--
            }
            _forgotCooldownSeconds.value = 0
        }
    }

    // Вызывается из OneTap когда VK уже привязан на сервере, но SDK-сессии не было.
    // После успешной авторизации VK SDK сохраняем vkId локально и сразу берём данные.
    fun onVkAuthForLinkedAccount(vkid: String) {
        viewModelScope.launch {
            secureTokenStorage.saveVkId(vkid)
            getVkUserInfo()
        }
    }

    /**
     * Привязка VK к текущему аккаунту (OneTap в профиле, когда VK не привязан).
     *
     * @param vkAccessToken токен из VKID SDK — по нему сервер сам определяет
     *   vk_id. Не сохраняем: локально по-прежнему храним только vk id.
     *
     * Ошибку (в т.ч. 409 «VK уже привязан к другому аккаунту») кладём в
     * [ProfileUiState.vkLinkMessage], чтобы UI показал текст, а не молчал.
     */
    fun attachVKID(vkid: String, vkAccessToken: String) {
        viewModelScope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            val fullToken = "Bearer $token"
            authManager.attachVKID(
                fullToken,
                vkid,
                vkAccessToken
            ).collect { state ->
                    if (state is GetUserProfileState.Success) {
                        secureTokenStorage.saveVkId(vkid)
                        refresh()  // Обновляем, чтобы Flow VK ID эмитнул и загрузил данные
                    } else if (state is GetUserProfileState.Error) {
                        Log.e("ProfileViewModel", "Ошибка привязки VK: ${state.message}")
                        _uiState.update { it.copy(vkLinkMessage = state.message) }
                    }
                }
        }
    }

    /** Сброс сообщения о привязке VK после показа в snackbar. */
    fun clearVkLinkMessage() {
        _uiState.update { it.copy(vkLinkMessage = null) }
    }

    fun vkIdRefreshToken() {
        // VK ID SDK может выбросить исключение при отмене капчи или разрыве OAuth-потока
        // (внутренняя корутина SDK бросает RuntimeException вместо вызова onFail).
        val handler = CoroutineExceptionHandler { _, throwable ->
            Sentry.captureMessage("vkIdRefreshToken uncaught: ${throwable.message}")
        }
        viewModelScope.launch(handler) {
            try {
                VKID.instance.refreshToken(
                    callback = object : VKIDRefreshTokenCallback {
                        override fun onSuccess(token: AccessToken) {
                            viewModelScope.launch {
                                getVkUserInfo()
                            }
                        }

                        override fun onFail(fail: VKIDRefreshTokenFail) {
                            when (fail) {
                                is VKIDRefreshTokenFail.NotAuthenticated -> {
                                    // Нет VK-сессии на этом устройстве (пользователь входил
                                    // через email, а VK привязан через другое устройство).
                                    // НЕ очищаем vkId из storage — он нужен чтобы знать,
                                    // что VK привязан на сервере. Просто показываем кнопку
                                    // OneTap для однократной авторизации VK SDK на устройстве.
                                    _uiState.update {
                                        it.copy(vkUserState = ResultState.Success(null))
                                    }
                                }
                                is VKIDRefreshTokenFail.RefreshTokenExpired -> {
                                    // Токен явно отозван VK — сессия больше не действительна,
                                    // сбрасываем vkId чтобы не пытаться обновить снова.
                                    viewModelScope.launch {
                                        secureTokenStorage.saveVkId("")
                                        _uiState.update {
                                            it.copy(vkUserState = ResultState.Success(null))
                                        }
                                    }
                                }
                                is VKIDRefreshTokenFail.FailedApiCall -> {
                                    if (fail.description.contains("invalid_grant")) {
                                        // invalid_grant = истёкший/отозванный refresh token,
                                        // сбрасываем сессию VK.
                                        viewModelScope.launch {
                                            secureTokenStorage.saveVkId("")
                                            _uiState.update {
                                                it.copy(vkUserState = ResultState.Success(null))
                                            }
                                        }
                                    } else {
                                        Sentry.captureMessage("vkIdRefreshToken FailedApiCall: ${fail.description}")
                                    }
                                }
                                // Неожиданная ошибка OAuth state — логируем.
                                is VKIDRefreshTokenFail.FailedOAuthState ->
                                    Sentry.captureMessage("vkIdRefreshToken FailedOAuthState: ${fail.description}")
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                // Капча отменена или иная внутренняя ошибка VK SDK
                Sentry.captureMessage("vkIdRefreshToken exception: ${e.message}")
            }
        }
    }

    fun forgotResetState() {
        _responseState.value = ForgotPasswordState.Initial
    }

    private fun saveRouteInLocal(routes: List<Route>) {
        viewModelScope.launch {
            val totalRoutes = routes.size

            _uiState.update { it.copy(downloadRouteProgress = 0 to totalRoutes) }  // Устанавливаем начальный прогресс

            var savedCount = 0
            var hasError = false

            for (route in routes) {
                Log.d("zzz", "Saving route: $route")
                try {
                    // Ждем завершения сохранения: collect until Success or Error
                    val saveResult = routeUseCase.saveRouteAfterLoading(route).first { state ->
                        state is ResultState.Success || state is ResultState.Error
                    }

                    when (saveResult) {
                        is ResultState.Success -> {
                            savedCount++
                            _uiState.update { it.copy(downloadRouteProgress = savedCount to totalRoutes) }  // Обновляем прогресс в UI
                            Log.d("zzz", "Route saved successfully: ${route.basicData.id}")
                        }

                        is ResultState.Error -> {
                            hasError = true
                            Log.e(
                                "zzz",
                                "Error saving route ${route.basicData.id}: ${saveResult.entity.message}"
                            )
                            // Продолжаем с остальными, но отметим ошибку
                        }

                        else -> {}  // Не должно быть, но на всякий
                    }
                } catch (e: Exception) {
                    e.sendToSentry("ProfileViewModel", "saveRouteInLocal")
                    hasError = true
                    Log.e("zzz", "Exception saving route: ${e.message}")
                }
            }

            if (!hasError) {
                _uiState.update {
                    it.copy(
                        downloadState = ResultState.Success(savedCount),
                        downloadRouteProgress = null  // Скрываем прогресс после завершения
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        downloadState = ResultState.Error(ErrorEntity(message = "Ошибки при сохранении некоторых маршрутов")),
                        downloadRouteProgress = null
                    )
                }
            }
        }
    }

    // --------->>>>>> MIGRATION <<<<<<<<---------- //
    // Изменено: Симуляция миграции с прогрессом (0–100% для каждого этапа)
    // Для чего: Чтобы пользователь видел реальный прогресс вместо простого delay
    fun startMigration() {
        viewModelScope.launch {
            try {
            _migrationUiState.update {
                it.copy(
                    isMigrating = true,
                    migrationStep = "Загрузка маршрутов на сервер",
                    routesProgress = 0 to 0
                )
            }
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            val fullToken = "Bearer $token"

            val userId = secureTokenStorage.getUserIdFlow().first()
            // Получаем все локальные маршруты из Room
            routeUseCase.getListRoutesAsStateFlow()
                .first { it !is ResultState.Loading }
                .let { result ->
                    when (result) {
                        is ResultState.Success -> {
                            val routes = result.data
                            val totalRoutes = routes.size
                            _migrationUiState.update { it.copy(routesProgress = 0 to totalRoutes) }

                            var savedCount = 0
                            var hasRoutesError = false
                            for (route in routes) {
                                delay(400L)
                                routesManager.saveRouteInRemote(
                                    route = route,
                                    bearerToken = fullToken
                                ).collect { saveResult ->
                                    when (saveResult) {
                                        is ResultState.Success -> {
                                            savedCount++
                                            _migrationUiState.update { it.copy(routesProgress = savedCount to totalRoutes) }
                                        }

                                        is ResultState.Error -> {
                                            hasRoutesError = true
                                            Log.e(
                                                "Migration",
                                                "Ошибка сохранения маршрута ${route.basicData.id}: ${saveResult.entity.message}"
                                            )
                                            Log.e("Migration","migration \nsave route \nuser bearer token \n$token \nuserId $userId error \n${saveResult.entity} \n${route.basicData.id}\n $route")
                                            // Продолжаем с остальными, но отметим ошибку
                                        }

                                        else -> {}  // Loading игнорируем
                                    }
                                }
                            }

                            // После маршрутов переходим к настройкам
                            _migrationUiState.update {
                                it.copy(
                                    migrationStep = "Загрузка настроек на сервер",
                                    settingsProgress = 0f
                                )
                            }

                            var hasSettingsError = false

                            // 1. Сохранение UserSettings (аналогично SyncManager)
                            val localUserSettings = settingsUseCase.getUserSettingFlow().first()
                            val endTimeSubscription = sharedPrefs.getSubscriptionExpiration()
                            val l = localUserSettings.copy(
                                subscriptionPeriod = endTimeSubscription
                            )

                            val saveSubscribeTimeInLocal = settingsUseCase.saveSetting(l)
                                .first { it is ResultState.Success || it is ResultState.Error }

                            if (saveSubscribeTimeInLocal is ResultState.Error) {
                                Log.e("Migration","migration \n save salarySetting \nuser bearer token \n userId $userId \n$token \n при миграции не перенесены данные подписки в UserSetting")
                            }

                            settingManager.saveUserSettingInRemote(l, fullToken)
                                .collect { saveState ->
                                    when (saveState) {
                                        is ResultState.Success -> {
                                            _migrationUiState.update { it.copy(settingsProgress = 33.0f) } // Прогресс: 50% после UserSettings
                                        }

                                        is ResultState.Error -> {
                                            hasSettingsError = true
                                            Log.e("Migration","migration  \nsave userSetting \n userId $userId \nuser bearer token \n$token\n ${saveState.entity} \n $localUserSettings")
                                            // Продолжаем к следующей настройке
                                        }

                                        else -> {} // Loading игнорируем
                                    }
                                }

                            // 2. Сохранение SalarySetting (аналогично SyncManager)
                            val localSalarySetting =
                                salarySettingUseCase.salarySettingFlow().first()
                            settingManager.saveSalarySettingInRemote(localSalarySetting, fullToken)
                                .collect { saveState ->
                                    when (saveState) {
                                        is ResultState.Success -> {
                                            _migrationUiState.update { it.copy(settingsProgress = 66.0f) } // Прогресс: 100% после SalarySetting
                                        }

                                        is ResultState.Error -> {
                                            hasSettingsError = true
                                            Log.e("Migration","migration  \nsave salarySetting \n userId $userId \nuser bearer token \n$token \n${saveState.entity} \n $localSalarySetting")

                                        }

                                        else -> {} // Loading игнорируем
                                    }
                                }

                            // 3. Сохранение дней отвлечений (ReleaseDay)
                            val localReleaseDays = releaseDayUseCase.getAll()
                            settingManager.saveReleaseDaysInRemote(localReleaseDays, fullToken)
                                .collect { saveState ->
                                    when (saveState) {
                                        is ResultState.Success -> {
                                            _migrationUiState.update { it.copy(settingsProgress = 1.0f) }
                                        }

                                        is ResultState.Error -> {
                                            hasSettingsError = true
                                            Log.e(
                                                "Migration",
                                                "Ошибка сохранения дней отвлечений: ${saveState.entity.message}"
                                            )
                                        }

                                        else -> {} // Loading игнорируем
                                    }
                                }

                            if (!hasRoutesError && !hasSettingsError) {
                                _migrationUiState.update {
                                    it.copy(
                                        isMigrating = false,
                                        migrationResult = ResultState.Success(Unit)
                                    )
                                }
                                sharedPrefs.setIsMigrated(true)
                                _isMigrated.value = true
                                completeMigration()
                            } else {
                                _migrationUiState.update {
                                    it.copy(
                                        isMigrating = false,
                                        migrationResult = ResultState.Error(
                                            ErrorEntity(message = "Ошибки при сохранении некоторых данных (маршруты: $hasRoutesError, настройки: $hasSettingsError)")
                                        )
                                    )
                                }
                                completeMigration()
                            }
                        }

                        is ResultState.Error -> {
                            Log.e(
                                "Migration",
                                "Ошибка загрузки локальных маршрутов: ${result.entity.message}"
                            )
                            _migrationUiState.update {
                                it.copy(
                                    isMigrating = false,
                                    migrationResult = ResultState.Error(result.entity)
                                )
                            }
                            completeMigration()
                        }

                        else -> {}
                    }
                }
            } catch (e: Exception) {
                e.sendToSentry("ProfileViewModel", "startMigration")
                Log.e("Migration", "Критическая ошибка миграции", e)
                _migrationUiState.update {
                    it.copy(
                        isMigrating = false,
                        migrationResult = ResultState.Error(
                            ErrorEntity(message = "Перенос данных произошел с ошибкой, попробуйте позже.")
                        )
                    )
                }
                completeMigration()
            }
        }
    }

    // Добавлено: Сброс результата миграции
    // Для чего: Чтобы очистить состояние после показа snackbar
    fun resetMigrationResult() {
        _migrationUiState.update { it.copy(migrationResult = null) }
    }

    fun completeMigration() {
        sharedPrefs.setIsMigrated(true)
        _isMigrated.value = true
        _migrationUiState.update { it.copy(migrationResult = null) }
    }

    fun registeredUserByEmailForMigration(email: String, password: String) {
        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            authManager.registerByEmail(email = email, password = password)
                .collect { state ->  // Collect эмитит значения из Flow
                    if (state is RegistrationState.Success) {
                        val token = state.accessToken
                        if (token.isNotEmpty()) {
                            // Сохранение зашифрованного токена
                            secureTokenStorage.saveAuthToken(token)
                            startMigration()
                            _isLoggedIn.value = true  // Обновляем состояние логина после успеха
                            refresh()  // Перезагружаем данные после входа}
                        }
                    }
                    // Обновляем UI-состояние на основе эмитов (Loading, Success, Error)
                    _registeredUiState.value = state
                }
        }
    }

    fun registeredUserByVKIDForMigration(vkid: String, vkAccessToken: String, email: String) {
        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            // Пояснение: Запускаем корутину для collect Flow (Flow холодный, стартует здесь).
            authManager.registerByVKID(vkid, vkAccessToken, email)
                .collect { state ->  // Collect эмитит значения из Flow
                    if (state is RegistrationState.Success) {
                        val token = state.accessToken
                        if (token.isNotEmpty()) {
                            // Сохранение зашифрованного токена
                            secureTokenStorage.saveAuthToken(token)
                            secureTokenStorage.saveVkId(vkid)
                            startMigration()
                            _isLoggedIn.value = true  // Обновляем состояние логина после успеха
                            refresh()  // Перезагружаем данные после входа}
                        }
                    }
                    // Обновляем UI-состояние на основе эмитов (Loading, Success, Error)
                    _registeredUiState.value = state
                }
        }
    }

    companion object {
        private const val FORGOT_COOLDOWN_SECONDS = 60
    }
}
