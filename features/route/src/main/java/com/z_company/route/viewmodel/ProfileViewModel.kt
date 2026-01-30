package com.z_company.route.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
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
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.Back4AppManager
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.repository.SecureDataStore
import com.z_company.repository.remote_rest.AuthManager
import com.z_company.repository.remote_rest.AuthState
import com.z_company.repository.remote_rest.ForgotEmailState
import com.z_company.repository.remote_rest.GetUserProfileState
import com.z_company.repository.remote_rest.RegistrationState
import com.z_company.repository.remote_rest.RoutesManager
import com.z_company.repository.remote_rest.SettingManager
import com.z_company.repository.remote_rest.UserRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Calendar.MONTH
import java.util.Calendar.YEAR
import kotlin.collections.first

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
    val downloadRouteProgress: Pair<Int, Int>? = null
)

data class MigrationState(
    val isMigrating: Boolean = false,
    val migrationStep: String = "",
    val migrationResult: ResultState<Unit>? = null,
    val routesProgress: Pair<Int, Int> = 0 to 0,
    val settingsProgress: Float = 0f
)

class ProfileViewModel(application: Application) : AndroidViewModel(application), KoinComponent {
    private val back4AppManager: Back4AppManager by inject()
    private val sharedPrefs: SharedPreferencesRepositories by inject()
    private val settingsUseCase: SettingsUseCase by inject()

    private val salarySettingUseCase: SalarySettingUseCase by inject()
    private val routeUseCase: RouteUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val routesManager = RoutesManager

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

    private val _forgotEmailState = MutableStateFlow<ForgotEmailState>(ForgotEmailState.Initial)
    val forgotEmailState = _forgotEmailState.asStateFlow()


    var currentEmail by mutableStateOf("")
        private set

    private var loadSettingsJob: Job? = null

    init {
        getUserWithRoutes()
        loadSettingsForSyncInfo()
        loadPurchasesInfo()

        // Чтобы сразу определить, залогинен ли пользователь (токен существует и не пустой). Это обновит _isLoggedIn, которое используется в ProfileScreen для условного рендеринга.
        viewModelScope.launch(Dispatchers.IO) {
            val token = SecureDataStore.getAuthTokenFlow(application).first()
            _isLoggedIn.value = !token.isNullOrEmpty()
        }
        viewModelScope.launch {
            _isFirstAppEntry.value = sharedPrefs.tokenIsFirstAppEntry()
            _isMigrated.value = sharedPrefs.isMigrated()
        }
        viewModelScope.launch(Dispatchers.IO) {
            SecureDataStore.getVkIdFlow(application).onEach { vkId ->
                if (vkId != null && vkId.isNotEmpty()) {
                    getVkUserInfo()
                } else {
                    _uiState.update { it.copy(vkUserState = ResultState.Success(null)) }
                }
            }.launchIn(viewModelScope)
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
        loadSettingsJob = settingsUseCase.getFlowCurrentSettingsState().onEach { result ->
            if (result is ResultState.Success) {
                result.data?.let { settings ->
                    _userSetting.value = settings
                    _uiState.update {
                        it.copy(
                            updateAt = settings.updateAt,
                            dateAndTimeConverter = DateAndTimeConverter(settings)
                        )
                    }
                    // Обновляем подписку, т.к. нужен converter
                    loadPurchasesInfo()
                }
            }
        }.launchIn(viewModelScope)
    }

    fun loadPurchasesInfo() {
        viewModelScope.launch {
            val expiration = sharedPrefs.getSubscriptionExpiration()
            val text = if (expiration == 0L) {
                ""
            } else {
                _uiState.value.dateAndTimeConverter?.getDateMiniAndTime(expiration) ?: ""
            }
            _uiState.update { it.copy(purchasesEndTime = ResultState.Success(text)) }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch(Dispatchers.IO) {
            getUserWithRoutes()
            loadSettingsForSyncInfo()
            loadPurchasesInfo()
            // Перезагружаем пользователя (flow уже обновится)
            delay(500)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun setEmail(value: String) {
        currentEmail = value
    }

    fun onUploadToRemote() {
        viewModelScope.launch {
            back4AppManager.synchronizedStorage().collect { result ->
                _uiState.update { it.copy(uploadState = result) }
            }
        }
    }

    fun onDownloadFromRemote() {
        viewModelScope.launch {
            back4AppManager.loadRouteListFromRemote().collect { result ->
                _uiState.update { it.copy(downloadState = result) }
            }
        }
    }

    fun resetUploadState() = _uiState.update { it.copy(uploadState = null) }
    fun resetDownloadState() = _uiState.update { it.copy(downloadState = null) }

    fun logOut() {
        viewModelScope.launch(Dispatchers.IO) {
            SecureDataStore.saveAuthToken(application, "")
            SecureDataStore.saveVkId(application, "")
            _isLoggedIn.value = false
        }
    }

    // Чтобы выполнять вход с email и password, обновлять состояние авторизации, сохранять токен при успехе и обновлять _isLoggedIn. Это вызывается из кнопки "Войти" в ProfileScreen.
    fun authWithEmail(email: String, password: String) {
        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            AuthManager.authWithEmail(email = email, password = password).collect { state ->
                Log.d("zzz", "auth $state")
                if (state is AuthState.Success) {
                    val token = state.accessToken
                    if (token.isNotEmpty()) {
                        SecureDataStore.saveAuthToken(
                            application,
                            token
                        )  // Сохранение зашифрованного токена
                        _isLoggedIn.value = true  // Обновляем состояние логина после успеха
                        refresh()  // Перезагружаем данные после входа
                    }
                }
                _authUiState.value = state  // Обновляем UI-состояние
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
            AuthManager.registerByEmail(email = email, password = password)
                .collect { state ->  // Collect эмитит значения из Flow
                    if (state is RegistrationState.Success) {
                        val token = state.accessToken
                        if (token.isNotEmpty()) {
                            // Сохранение зашифрованного токена
                            SecureDataStore.saveAuthToken(
                                application,
                                token
                            )
                            _isLoggedIn.value = true  // Обновляем состояние логина после успеха
                            refresh()  // Перезагружаем данные после входа}
                        }
                    }
                    // Обновляем UI-состояние на основе эмитов (Loading, Success, Error)
                    _registeredUiState.value = state
                }
        }
    }

    fun registeredUserByVKID(vkid: String) {
        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            // Пояснение: Запускаем корутину для collect Flow (Flow холодный, стартует здесь).
            AuthManager.registerByVKID(vkid)
                .collect { state ->  // Collect эмитит значения из Flow
                    if (state is RegistrationState.Success) {
                        val token = state.accessToken
                        if (token.isNotEmpty()) {
                            // Сохранение зашифрованного токена
                            SecureDataStore.saveAuthToken(application, token)
                            SecureDataStore.saveVkId(application, vkid)
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
            val token = SecureDataStore.getAuthTokenFlow(application).first()
            val fullToken = "Bearer $token"
            AuthManager.removeVKID(fullToken).collect { state ->
                if (state is GetUserProfileState.Success) {  // Предполагаем, что removeVKID возвращает аналогичный state
                    SecureDataStore.saveVkId(application, "")  // Очистка VK ID
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

    fun getUserWithRoutes() {
        viewModelScope.launch {
            val token = SecureDataStore.getAuthTokenFlow(application).first()
            val fullToken = "Bearer $token"
            AuthManager.getUserProfile(fullToken).collect { state ->
                Log.d("zzz", "getUserProfile $state")
                if (state is GetUserProfileState.Success) {
                    currentEmail = state.user.email
                    _uiState.update {
                        it.copy(userDetailsState = ResultState.Success(state.user))
                    }
                }
                if (state is GetUserProfileState.Error) {
                    _uiState.update {
                        it.copy(userDetailsState = ResultState.Error(ErrorEntity(message = state.errorMessage)))
                    }
                }
            }
        }
    }

    fun forgotRequest(email: String) {
        forgotJob?.cancel()
        forgotJob = viewModelScope.launch {
            AuthManager.forgotPassword(email).collect { state ->
                _forgotEmailState.value = state
            }
        }
    }

    // Для чего: Вызывается из OneTap в профиле, когда VK не привязан. Предполагаем, что AuthManager имеет метод attachVKID (аналогичный registerByVKID, но для привязки). После успеха сохраняем VK ID и обновляем данные.
    fun attachVKID(vkid: String) {
        viewModelScope.launch {
            val token = SecureDataStore.getAuthTokenFlow(application).first()
            val fullToken = "Bearer $token"
            AuthManager.attachVKID(
                fullToken,
                vkid
            )  // Предполагаем, что этот метод добавлен в AuthManager и возвращает Flow<RegistrationState>
                .collect { state ->
                    if (state is GetUserProfileState.Success) {
                        SecureDataStore.saveVkId(application, vkid)
                        refresh()  // Обновляем, чтобы Flow VK ID эмитнул и загрузил данные
                    } else if (state is GetUserProfileState.Error) {
                        // Можно добавить обработку ошибки, например, в uiState
                        Log.e("ProfileViewModel", "Ошибка привязки VK: ${state.errorMessage}")
                    }
                }
        }
    }

    fun vkIdRefreshToken() {
        viewModelScope.launch {
            VKID.instance.refreshToken(
                callback = object : VKIDRefreshTokenCallback {
                    override fun onSuccess(token: AccessToken) {
                        Log.d("zzz", "access_token ${token.userData.email}")
                        Log.d("zzz", "access_token ${token.userData.lastName}")
                        Log.d("zzz", "access_token ${token.userData.firstName}")
                    }

                    override fun onFail(fail: VKIDRefreshTokenFail) {
                        when (fail) {
                            is VKIDRefreshTokenFail.FailedApiCall -> fail.description // Использование текста ошибки.
                            is VKIDRefreshTokenFail.FailedOAuthState -> fail.description // Использование текста ошибки.
                            is VKIDRefreshTokenFail.RefreshTokenExpired -> fail // Ошибка истечения срока жизни RT. Это уведомление о том, что пользователю нужно перелогиниться.
                            is VKIDRefreshTokenFail.NotAuthenticated -> fail // Ошибка отсутствия авторизации у пользователя. Это уведомление о том, что пользователю нужно авторизоваться.
                        }
                    }
                }
            )
        }
    }

    fun forgotResetState() {
        _forgotEmailState.value = ForgotEmailState.Initial
    }

    fun saveUserSettingInRemote() {
        viewModelScope.launch {
            val token = SecureDataStore.getAuthTokenFlow(application).first()
            val fullToken = "Bearer $token"

            SettingManager.saveUserSettingInRemote(userSetting.value, fullToken)
                .collect { resultState ->
                    Log.d("zzz", "save user setting result $resultState")
                }
        }
    }

    fun getUserSettingFromRemote() {
        viewModelScope.launch {
            val token = SecureDataStore.getAuthTokenFlow(application).first()
            val fullToken = "Bearer $token"

            SettingManager.getUserSettingFromRemote(fullToken)
                .collect { resultState ->
                    Log.d("zzz", "get user setting $resultState")
                    if (resultState is ResultState.Success) {
                        Log.d("zzz", "success ${resultState.data}")
                        val listMonthOfYear = calendarUseCase.loadFlowMonthOfYearListState().first()
                        val currentCalendar = Calendar.getInstance()
                        val currentMonthOfYear = listMonthOfYear.find {
                            it.month == currentCalendar.get(MONTH) && it.year == currentCalendar.get(
                                YEAR
                            )
                        }

                        val userSettings = resultState.data.copy(
                            selectMonthOfYear = currentMonthOfYear ?: listMonthOfYear.first()
                        )

                        settingsUseCase.saveSetting(userSettings).collect { saveState ->
                            Log.d("zzz", "save in local ${saveState}")
                        }
                    }
                }
        }
    }

    fun saveSalarySettingInRemote() {
        viewModelScope.launch {
            val token = SecureDataStore.getAuthTokenFlow(application).first()
            val fullToken = "Bearer $token"

            SettingManager.saveSalarySettingInRemote(salarySetting.value, fullToken)
                .collect { resultState ->
                    Log.d("zzz", "save salary setting result $resultState")
                }
        }
    }

    fun getSalarySettingFromRemote() {
        viewModelScope.launch {
            val token = SecureDataStore.getAuthTokenFlow(application).first()
            val fullToken = "Bearer $token"

            SettingManager.getSalarySettingFromRemote(fullToken)
                .collect { resultState ->
                    Log.d("zzz", "get salary setting $resultState")
                    if (resultState is ResultState.Success) {
                        val ss = resultState.data
                        salarySettingUseCase.saveSalarySetting(ss).collect {
                            Log.d("zzz", "save salary setting in local $resultState")
                        }
                    }
                }
        }
    }


    fun saveMonthOfYearList() {
        viewModelScope.launch {
            val token = SecureDataStore.getAuthTokenFlow(application).first()
            val fullToken = "Bearer $token"
            val listMonthOfYear = calendarUseCase.loadFlowMonthOfYearListState().first()

            SettingManager.saveMonthOfYearListInRemote(listMonthOfYear, fullToken)
                .collect { resultState ->
                    Log.d("zzz", "save calendar $resultState")
                }
        }
    }

    fun getMonthOfYearList() {
        viewModelScope.launch {
            val token = SecureDataStore.getAuthTokenFlow(application).first()
            val fullToken = "Bearer $token"

            SettingManager.getMonthOfYearListFromRemote(fullToken).collect { resultState ->
                Log.d("zzz", "get calendar $resultState")
                if (resultState is ResultState.Success) {
                    val calendar = resultState.data
                    calendarUseCase.saveCalendar(calendar).collect { saveResult ->
                        Log.d("zzz", "save calendar in local $saveResult")
                    }
                }
            }
        }
    }

    fun getRoutesFromRemote() {
        viewModelScope.launch {
            val token = SecureDataStore.getAuthTokenFlow(application).first()
            val fullToken = "Bearer $token"

            RoutesManager.getRoutesFromRemote(fullToken).collect { resultState ->
                Log.d("zzz", "save setting result $resultState")
                when (resultState) {
                    is ResultState.Loading -> {}
                    is ResultState.Success -> {
                        val routes = resultState.data
                        routes.forEach { route ->
                            Log.d("zzz", "$route")
                        }
                        saveRouteInLocal(routes)
                    }

                    is ResultState.Error -> {
                        Log.e("zzz", "Error loading routes: ${resultState.entity.message}")
                        _uiState.update {
                            it.copy(
                                downloadState = ResultState.Error(resultState.entity),
                                downloadRouteProgress = null
                            )
                        }
                    }
                }
            }
        }
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
            _migrationUiState.update {
                it.copy(
                    isMigrating = true,
                    migrationStep = "Загрузка маршрутов на сервер",
                    routesProgress = 0 to 0
                )
            }
            val token = SecureDataStore.getAuthTokenFlow(application).first()
            val fullToken = "Bearer $token"
            // Получаем все локальные маршруты из Room
            routeUseCase.getListRoutesAsFlow()
                .collect { result ->  // Предполагаем, что добавлен метод getAllRoutes(): Flow<ResultState<List<Route>>> в RouteUseCase; если нет, можно собрать по месяцам
                    when (result) {
                        is ResultState.Success -> {
                            val routes = result.data.take(3)
                            val totalRoutes = routes.size
                            _migrationUiState.update { it.copy(routesProgress = 0 to totalRoutes) }

                            var savedCount = 0
                            var hasError = false
                            for (route in routes) {
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
                                            hasError = true
                                            Log.e(
                                                "Migration",
                                                "Ошибка сохранения маршрута ${route.basicData.id}: ${saveResult.entity.message}"
                                            )
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

                            // Симуляция загрузки настроек (заменить на реальный метод, если появится)
                            for (i in 1..10) {  // Имитация 10 шагов для 100%
                                delay(200)  // 2 секунды всего
                                _migrationUiState.update { it.copy(settingsProgress = i / 10f) }
                            }

                            // Завершение миграции
                            if (!hasError) {
                                _migrationUiState.update {
                                    it.copy(
                                        isMigrating = false,
                                        migrationResult = ResultState.Success(Unit)
                                    )
                                }
                                sharedPrefs.setIsMigrated(true)
                                _isMigrated.value = true
                            } else {
                                _migrationUiState.update {
                                    it.copy(
                                        isMigrating = false, migrationResult = ResultState.Error(
                                            ErrorEntity(message = "Ошибки при сохранении некоторых маршрутов")
                                        )
                                    )
                                }
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
                        }

                        else -> {}  // Loading
                    }
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
            AuthManager.registerByEmail(email = email, password = password)
                .collect { state ->  // Collect эмитит значения из Flow
                    if (state is RegistrationState.Success) {
                        val token = state.accessToken
                        if (token.isNotEmpty()) {
                            // Сохранение зашифрованного токена
                            SecureDataStore.saveAuthToken(application, token)
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

    fun registeredUserByVKIDForMigration(vkid: String) {
        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            // Пояснение: Запускаем корутину для collect Flow (Flow холодный, стартует здесь).
            AuthManager.registerByVKID(vkid)
                .collect { state ->  // Collect эмитит значения из Flow
                    if (state is RegistrationState.Success) {
                        val token = state.accessToken
                        if (token.isNotEmpty()) {
                            // Сохранение зашифрованного токена
                            SecureDataStore.saveAuthToken(application, token)
                            SecureDataStore.saveVkId(application, vkid)
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