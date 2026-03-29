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
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.AuthManager
import com.z_company.repository.remote_rest.AuthState
import com.z_company.repository.remote_rest.ResponseState
import com.z_company.repository.remote_rest.GetUserProfileState
import com.z_company.repository.remote_rest.RegistrationState
import com.z_company.repository.remote_rest.RoutesManager
import com.z_company.repository.remote_rest.SettingManager
import com.z_company.repository.remote_rest.SyncManager
import com.z_company.repository.remote_rest.UserRemote
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
    val vkUserState: ResultState<String?> = ResultState.Loading(),
    val downloadRouteProgress: Pair<Int, Int>? = null,
    val updateEmailState: ResultState<Unit>? = null,
    val syncUploadProgress: Map<String, SyncStepState> = emptyMap(),  // Прогресс для upload (ключ - этап, значение - состояние)
    val syncDownloadProgress: Map<String, SyncStepState> = emptyMap(),  // Прогресс для download
    val showSyncDialog: Boolean = false,  // Флаг показа диалога синхронизации
    val isSyncComplete: Boolean = false,  // Флаг завершения синхронизации (для показа кнопки)
    val isSyncSuccess: Boolean = false,  // Флаг полного успеха (показывает AlertDialog вместо диалога прогресса)
    val syncType: SyncType? = null,  // Тип синхронизации (Upload или Download), чтобы знать, какой progress отображать
    val syncRouteErrors: List<String> = emptyList(),
    val syncRoutesTotalAttempted: Int = 0,
    val syncRoutesSavedCount: Int = 0,
    val syncReportUserId: String? = null,
    val isNetworkError: Boolean = false,
    val isProfileNetworkError: Boolean = false
)

// Описание: Определяет тип синхронизации (загрузка на сервер или с сервера) для выбора правильного progress map в UI.
enum class SyncType {
    Upload,
    Download
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
    private val authManager: AuthManager by inject()
    private val routesManager: RoutesManager by inject()
    private val syncManager: SyncManager by inject()
    private val settingManager: SettingManager by inject()

    private val subscriptionHelper: SubscriptionHelper by inject()
    private val secureTokenStorage: SecureTokenStorage by inject()

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

    private val _responseState = MutableStateFlow<ResponseState>(ResponseState.Initial)
    val forgotEmailState = _responseState.asStateFlow()


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

    // Описание: Запускает синхронизацию на сервер (upload), показывает диалог, обновляет прогресс поэтапно через collect Flow.
    // При получении промежуточных Success - обновляет progress map. При final Success/Error - устанавливает isSyncComplete = true.
    fun startSyncUpload() {
        viewModelScope.launch(Dispatchers.IO) {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first() ?: return@launch
            val userId = secureTokenStorage.getUserIdFlow().first()
            _uiState.update {
                it.copy(
                    showSyncDialog = true,
                    syncType = SyncType.Upload,
                    syncUploadProgress = mapOf(  // Инициализируем шаги с Loading
                        "UserSettings" to SyncStepState.Loading,
                        "SalarySettings" to SyncStepState.Loading,
                        "Months" to SyncStepState.Loading,
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
                            if (result.monthsSaved) newProgress["Months"] = SyncStepState.Success("загружены")
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
                                newProgress[stepKey] = SyncStepState.Error(message = cleanMsg)
                                _uiState.update { it.copy(syncUploadProgress = newProgress) }
                            } else {
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

    private fun isNetworkErrorMessage(msg: String): Boolean =
        msg.contains("Нет соединения", ignoreCase = true) ||
        msg.contains("Unable to resolve", ignoreCase = true) ||
        msg.contains("Connection refused", ignoreCase = true) ||
        msg.contains("timeout", ignoreCase = true) ||
        msg.contains("Failed to connect", ignoreCase = true) ||
        msg.contains("Network is unreachable", ignoreCase = true) ||
        msg.contains("ECONNREFUSED", ignoreCase = true)

    private fun parseSyncUploadStep(message: String): String? = when {
        message.contains("UserSettings") -> "UserSettings"
        message.contains("SalarySetting") -> "SalarySettings"
        message.contains("MonthOfYearList") -> "Months"
        else -> null
    }

    private fun cleanSyncErrorMessage(message: String): String {
        val prefixes = listOf(
            "Ошибка сохранения UserSettings: ",
            "Ошибка сохранения SalarySetting: ",
            "Ошибка сохранения MonthOfYearList: ",
            "Ошибка загрузки UserSettings: ",
            "Ошибка загрузки SalarySetting: ",
            "Ошибка загрузки MonthOfYearList: "
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
                        "Months" to SyncStepState.Loading,
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

                            if (result.monthsLoaded) newProgress["Months"] = SyncStepState.Success("загружены")
                            if (result.salarySettingsLoaded) newProgress["SalarySettings"] = SyncStepState.Success("загружены")
                            if (result.userSettingsLoaded) newProgress["UserSettings"] = SyncStepState.Success("загружены")
                            if (result.routesLoadedCount >= 0) newProgress["Routes"] = SyncStepState.Success("загружены ${result.routesLoadedCount}(шт)")

                            val isFullSuccess = result.userSettingsLoaded && result.salarySettingsLoaded &&
                                result.monthsLoaded && result.routesLoadedCount >= 0
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
        message.contains("MonthOfYearList") -> "Months"
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
                _uiState.update { it.copy(vkUserState = ResultState.Success(fullName)) }
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

    fun resetUploadState() = _uiState.update { it.copy(uploadState = null) }
    fun resetDownloadState() = _uiState.update { it.copy(downloadState = null) }

    fun logOut() {
        viewModelScope.launch(Dispatchers.IO) {
            secureTokenStorage.saveAuthToken("")
            secureTokenStorage.saveVkId("")
            _isLoggedIn.value = false
        }
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
                        refresh()  // Перезагружаем данные после входа
                        syncManager.syncFromRemote("Bearer $token").collect {}
                    }
                }
                _authUiState.value = state  // Обновляем UI-состояние
            }
        }
    }

    fun authWithVKID(vkid: String) {
        loginJob?.cancel()
        val handler = CoroutineExceptionHandler { _, throwable ->
            // VK SDK может бросить при отмене капчи во время OAuth
            Sentry.captureMessage("authWithVKID uncaught: ${throwable.message}")
        }
        loginJob = viewModelScope.launch(handler) {
            try {
                authManager.authWithVKID(vkid).collect { state ->
                    if (state is AuthState.Success) {
                        val token = state.accessToken
                        if (token.isNotEmpty()) {
                            secureTokenStorage.saveAuthToken(token)
                            secureTokenStorage.saveVkId(vkid)
                            _isLoggedIn.value = true
                            refresh()
                            syncManager.syncFromRemote("Bearer $token").collect {}
                        }
                    }
                    _authUiState.value = state
                }
            } catch (e: Exception) {
                Sentry.captureMessage("authWithVKID exception: ${e.message}")
            }
        }
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

    fun registeredUserByVKID(vkid: String, email: String) {
        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            // Пояснение: Запускаем корутину для collect Flow (Flow холодный, стартует здесь).
            authManager.registerByVKID(vkid, email)
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

    fun removeUsersVKID() {
        viewModelScope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            val fullToken = "Bearer $token"
            authManager.removeVKID(fullToken).collect { state ->
                if (state is GetUserProfileState.Success) {  // Предполагаем, что removeVKID возвращает аналогичный state
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
                    }
                    if (state is GetUserProfileState.Error) {
                        if (state.code == 401) {
                            _isLoggedIn.value = false
                        }
                        val networkErr = state.code == 0 || isNetworkErrorMessage(state.message ?: "")
                        _uiState.update {
                            it.copy(
                                userDetailsState = ResultState.Error(ErrorEntity(message = state.message)),
                                isProfileNetworkError = networkErr
                            )
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
        forgotJob?.cancel()
        forgotJob = viewModelScope.launch {
            authManager.forgotPassword(email).collect { state ->
                Log.d("zzz", "forgotPassword $state")
                _responseState.value = state
            }
        }
    }

    // Для чего: Вызывается из OneTap в профиле, когда VK не привязан. Предполагаем, что AuthManager имеет метод attachVKID (аналогичный registerByVKID, но для привязки). После успеха сохраняем VK ID и обновляем данные.
    fun attachVKID(vkid: String) {
        viewModelScope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            val fullToken = "Bearer $token"
            authManager.attachVKID(
                fullToken,
                vkid
            )  // Предполагаем, что этот метод добавлен в AuthManager и возвращает Flow<RegistrationState>
                .collect { state ->
                    if (state is GetUserProfileState.Success) {
                        secureTokenStorage.saveVkId(vkid)
                        refresh()  // Обновляем, чтобы Flow VK ID эмитнул и загрузил данные
                    } else if (state is GetUserProfileState.Error) {
                        // Можно добавить обработку ошибки, например, в uiState
                        Log.e("ProfileViewModel", "Ошибка привязки VK: ${state.message}")
                    }
                }
        }
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
                            val desc = when (fail) {
                                is VKIDRefreshTokenFail.FailedApiCall -> fail.description
                                is VKIDRefreshTokenFail.FailedOAuthState -> fail.description
                                is VKIDRefreshTokenFail.RefreshTokenExpired -> "RefreshTokenExpired"
                                is VKIDRefreshTokenFail.NotAuthenticated -> "NotAuthenticated"
                            }
                            Sentry.captureMessage("vkIdRefreshToken onFail: $desc")
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
        _responseState.value = ResponseState.Initial
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

                            // 3. Сохранение MonthOfYearList
                            val localMonths = calendarUseCase.loadFlowMonthOfYearListState().first()
                            settingManager.saveMonthOfYearListInRemote(localMonths, fullToken)
                                .collect { saveState ->
                                    when (saveState) {
                                        is ResultState.Success -> {
                                            _migrationUiState.update { it.copy(settingsProgress = 1.0f) } // Прогресс: 100% после SalarySetting
                                        }

                                        is ResultState.Error -> {
                                            hasSettingsError = true
                                            Log.e(
                                                "Migration",
                                                "Ошибка сохранения MonthOfYearList: ${saveState.entity.message}"
                                            )
                                            Log.e("Migration","migration  \nsave MonthOfYearList \n userId $userId \nuser bearer token \n$token ${saveState.entity} \n $localMonths")

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

    fun registeredUserByVKIDForMigration(vkid: String, email: String) {
        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            // Пояснение: Запускаем корутину для collect Flow (Flow холодный, стартует здесь).
            authManager.registerByVKID(vkid, email)
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
}