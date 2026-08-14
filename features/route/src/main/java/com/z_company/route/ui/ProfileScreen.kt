package com.z_company.route.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.vk.id.onetap.compose.onetap.OneTap
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.CustomDivider
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.component.GenericLoading
import com.z_company.core.ui.theme.Shapes
import com.z_company.repository.remote_rest.RegistrationState
import com.z_company.route.viewmodel.ProfileViewModel
import com.z_company.core.util.isEmailValid
import com.z_company.core.util.isVpnActive
import com.z_company.core.util.vpnAwareErrorMessage
import com.z_company.core.util.VPN_ERROR_HINT
import com.z_company.repository.remote_rest.AuthState
import com.z_company.route.R
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.route.component.AppInputBottomSheet
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.component.SwitchApp
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.input.ImeAction
import com.z_company.repository.remote_rest.ForgotPasswordState
import com.z_company.route.component.AnimationDialog
import com.z_company.route.viewmodel.SyncType
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.z_company.route.viewmodel.VkUserInfo

const val MIN_LENGTH_PASSWORD = 4

// ── Хелперы редизайна профиля (по референсу android-profile.jsx) ─────
@Composable
private fun ProfileGroupLabel(text: String) {
    Text(
        modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 8.dp),
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ProfileAvatarPlaceholder() {
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceBright),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.person_24px),
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Мини-бейдж VK перед «Вход через VK ID».
@Composable
private fun VkBadge() {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF0077FF)),
        contentAlignment = Alignment.Center,
    ) {
        Text("VK", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

// Tonal-кнопка единой двусторонней синхронизации с облаком и лоадером.
@Composable
private fun SyncCloudButton(
    iconRes: Int,
    text: String,
    loading: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(Shapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), Shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
        } else {
            Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBillingClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Для чего: Чтобы условно рендерить экран профиля или форму входа на основе наличия токена
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    // Для чего: Чтобы обрабатывать loading, ошибки и успех авторизации, аналогично обработке в SignInScreen
    val authUiState by viewModel.authUiState.collectAsState()

    // Затем условно рендерить раздел синхронизации (предполагаю, что это часть LazyColumn или Column с sync элементами, например, кнопка "Синхронизировать" или блок "Синхронизация").
    // Изменено: Добавлена переменная hasSubscription и AnimatedVisibility для раздела синхронизации.
    // Для чего: Чтобы скрыть раздел "Синхронизация" если subscriptionPeriod меньше текущего времени (не оплачено). Это предотвращает ручной запуск sync и запись 0 на сервер неоплаченными пользователями.
    val hasSubscription by viewModel.hasSubscription.collectAsState()

    val registeredUiState by viewModel.registeredUiState.collectAsState()
    var isLicenseAgreementAccepted by rememberSaveable {
        mutableStateOf(false)
    }
    var isAcceptedPersonalDataProcessingPolicy by rememberSaveable {
        mutableStateOf(false)
    }
    val uiState by viewModel.uiState.collectAsState()
    val isFirstEntry = viewModel.isFirstAppEntry.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val ctx = LocalContext.current

    val styleData = MaterialTheme.typography.bodyLarge
    val styleHint = MaterialTheme.typography.bodyMedium
    val styleTitle = MaterialTheme.typography.titleSmall
    val primaryColor = MaterialTheme.colorScheme.primary

    val paddingBetweenView = 12.dp
    val buttonTextStyle = MaterialTheme.typography.bodySmall

    // Для чего: Чтобы показать экран миграции вместо обычного контента для старых пользователей
    val isMigrated by viewModel.isMigrated.collectAsState()
    val isMigrationNeeded = !isFirstEntry.value && !isMigrated
//
    // Для чего: Чтобы показывать loading с шагами миграции и обрабатывать результат
    val migrationUiState by viewModel.migrationUiState.collectAsState()

    val forgotEmailState by viewModel.forgotEmailState.collectAsState()
    val forgotCooldownSeconds by viewModel.forgotCooldownSeconds.collectAsState()

    LaunchedEffect(forgotEmailState) {
        when (forgotEmailState) {
            is ForgotPasswordState.Initial -> {

            }

            is ForgotPasswordState.Loading -> {
                snackbarHostState.showSnackbar("Отправляем запрос...")
            }

            // 200 — нейтральный ответ. Не раскрываем, зарегистрирован ли email.
            is ForgotPasswordState.Success -> {
                snackbarHostState.showSnackbar(
                    "Если такой email зарегистрирован, мы отправили на него ссылку. " +
                        "Проверьте входящие и папку «Спам».",
                    duration = SnackbarDuration.Long
                )
                viewModel.forgotResetState()
            }

            // 429 — превышен лимит запросов.
            is ForgotPasswordState.RateLimited -> {
                snackbarHostState.showSnackbar("Слишком много попыток, попробуйте позже")
                viewModel.forgotResetState()
            }

            is ForgotPasswordState.Error -> {
                snackbarHostState.showSnackbar(
                    vpnAwareErrorMessage(ctx, (forgotEmailState as ForgotPasswordState.Error).errorMessage)
                )
                viewModel.forgotResetState()
            }
        }
    }

    // Добавлено: Обработка результата миграции
    // Для чего: Чтобы показать сообщение об успешном переносе или ошибке
    LaunchedEffect(migrationUiState.migrationResult) {
        migrationUiState.migrationResult?.let { result ->
            when (result) {
                is ResultState.Success -> {
                    snackbarHostState.showSnackbar(
                        message = "Данные успешно перенесены на новый сервер",
                        duration = SnackbarDuration.Long
                    )
                    viewModel.resetMigrationResult()
                }

                is ResultState.Error -> {
                    snackbarHostState.showSnackbar(
                        message = "Ошибка переноса данных: ${result.entity.message}",
                        duration = SnackbarDuration.Long
                    )
                    viewModel.resetMigrationResult()
                }

                else -> Unit
            }
        }
    }

    // Для чего: Чтобы информировать пользователя об ошибке входа
    LaunchedEffect(authUiState) {
        if (authUiState is AuthState.Error) {
            snackbarHostState.showSnackbar(
                message = vpnAwareErrorMessage(ctx, (authUiState as AuthState.Error).errorMessage),
                duration = SnackbarDuration.Long
            )
            viewModel.resetAuthState() // Сброс состояния после показа ошибки
        }
    }

    // Для чего: Чтобы информировать пользователя об ошибке регистрации
    LaunchedEffect(registeredUiState) {
        if (registeredUiState is RegistrationState.Error) {
            val errorText = if ((registeredUiState as RegistrationState.Error).code == 409) {
                "Пользователь уже зарегистрирован."
            } else {
                vpnAwareErrorMessage(ctx, (registeredUiState as RegistrationState.Error).message)
            }
            snackbarHostState.showSnackbar(
                message = errorText,
                duration = SnackbarDuration.Long
            )
            viewModel.resetRegisteredState() // Сброс состояния после показа ошибки
        }
    }

    // Snackbar на ошибки/успех загрузки-выгрузки
    LaunchedEffect(uiState.uploadState) {
        uiState.uploadState?.let { state ->
            when (state) {
                is ResultState.Error -> {
                    snackbarHostState.showSnackbar("Ошибка выгрузки данных.\n${state.entity.message}")
                    viewModel.resetUploadState()
                }

                is ResultState.Success -> {
                    val message = if (state.data == 0) {
                        "Все маршруты синхронизированы"
                    } else {
                        "Маршруты успешно сохранены на сервере. (${state.data})"
                    }
                    snackbarHostState.showSnackbar(message)
                    viewModel.resetUploadState()
                }

                else -> Unit
            }
        }
    }

    LaunchedEffect(uiState.downloadState) {
        uiState.downloadState?.let { state ->
            when (state) {
                is ResultState.Error -> {
                    snackbarHostState.showSnackbar("Ошибка загрузки данных.\n${state.entity.message}")
                    viewModel.resetDownloadState()
                }

                is ResultState.Success -> {
                    snackbarHostState.showSnackbar("Маршруты загружены. (${state.data})")
                    viewModel.resetDownloadState()
                }

                else -> Unit
            }
        }
    }
    // Для чего: Чтобы вынести показ snackbar за пределы диалога — так snackbar отобразится на основном экране ПОСЛЕ закрытия диалога, и не исчезнет сразу.
    var showSuccessSnackbar by remember { mutableStateOf(false) }

// Изменено: Добавил LaunchedEffect вне диалога для показа snackbar при успехе.
// Для чего: Реагирует на флаг showSuccessSnackbar — показывает snackbar после закрытия диалога, затем сбрасывает флаг. Это решает проблему исчезновения toast сразу после закрытия.
    LaunchedEffect(showSuccessSnackbar) {
        if (showSuccessSnackbar) {
            snackbarHostState.showSnackbar("Email успешно изменён")
            showSuccessSnackbar = false
        }
    }

    var showEditEmailDialog by remember { mutableStateOf(false) }

    if (showEditEmailDialog) {
        // Состояние из ViewModel
        val updateEmailState by viewModel.uiState
            .map { it.updateEmailState }
            .collectAsState(initial = null)

        // Локальная ошибка — показываем под полем
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // Сброс состояния при открытии шторки
        LaunchedEffect(Unit) {
            viewModel.resetUpdateEmailState()
            errorMessage = null
        }

        // Реакция на результат запроса
        LaunchedEffect(updateEmailState) {
            updateEmailState?.let { state ->
                when (state) {
                    is ResultState.Success -> {
                        errorMessage = null
                        viewModel.resetUpdateEmailState()
                        showEditEmailDialog = false           // Закрываем диалог
                        showSuccessSnackbar = true
                    }

                    is ResultState.Error -> {
                        errorMessage = state.entity.message   // Показываем ошибку под полем
                        viewModel.resetUpdateEmailState()
                    }

                    else -> {} // Loading — ничего не делаем
                }
            }
        }

        AppInputBottomSheet(
            onDismissRequest = {
                showEditEmailDialog = false
                viewModel.resetUpdateEmailState()
            },
            title = "Новый email",
            initialValue = viewModel.currentEmail,
            onConfirm = { viewModel.updateEmail(it) },
            keyboardType = KeyboardType.Email,
            isValid = { it.isEmailValid() },
            errorText = errorMessage,
            isLoading = updateEmailState is ResultState.Loading,
            onValueChange = { errorMessage = null },
        )
    }

    SyncProgressDialog(
        showDialog = uiState.showSyncDialog,
        isSyncSuccess = uiState.isSyncSuccess,
        isSyncComplete = uiState.isSyncComplete,
        syncType = uiState.syncType,
        progressMap = when (uiState.syncType) {
            SyncType.Upload -> uiState.syncUploadProgress
            SyncType.Download -> uiState.syncDownloadProgress
            SyncType.Sync -> uiState.syncProgress
            null -> emptyMap()
        },
        syncRouteErrors = uiState.syncRouteErrors,
        syncRoutesTotalAttempted = uiState.syncRoutesTotalAttempted,
        syncRoutesSavedCount = uiState.syncRoutesSavedCount,
        userId = uiState.syncReportUserId,
        isNetworkError = uiState.isNetworkError,
        onDismiss = viewModel::resetSyncState
    )

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                CustomSnackBar(snackBarData = data)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (uiState.userDetailsState is ResultState.Loading){
                GenericLoading((uiState.userDetailsState as ResultState.Loading).message)
            } else {
                // Миграция
                if (isMigrationNeeded) {
                    val isMigrating = migrationUiState.isMigrating
                    val migrationResult = migrationUiState.migrationResult

                    when {
                        isMigrating -> {
                            // Показываем прогресс-бары вместо элементов формы
                            // Изменено: Показываем два прогресс-бара с процентами для маршрутов и настроек
                            // Для чего: Чтобы пользователь видел процесс загрузки с визуализацией прогресса
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Переносим данные на отечественный сервер",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 24.dp)
                                )

                                Text(
                                    text = migrationUiState.migrationStep,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                // Прогресс маршрутов
                                val (saved, total) = migrationUiState.routesProgress
                                val progress =
                                    if (total > 0) saved.toFloat() / total.toFloat() else 0f
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .padding(vertical = 8.dp),
                                    trackColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                                Text(
                                    text = "Загружено $saved из $total",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                // Прогресс настроек
                                LinearProgressIndicator(
                                    progress = { migrationUiState.settingsProgress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .padding(vertical = 8.dp),
                                    trackColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                                Text(
                                    text = "Настройки: ${migrationUiState.settingsProgress}%",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        migrationResult is ResultState.Success -> {
                            // Успешный перенос — показываем сообщение и кнопку "Продолжить работу"
                            // Изменено: Добавлен экран успеха с кнопкой
                            // Для чего: Чтобы пользователь мог вручную подтвердить завершение и скрыть миграцию навсегда
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.check_circle_24px),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Данные успешно перенесены!",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { viewModel.completeMigration() },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(text = "Продолжить работу", style = styleData)
                                }
                            }

                        }

                        else -> {
                            // Миграционный UI: Форма регистрации по умолчанию (login = false), плюс процесс миграции
                            var email by rememberSaveable { mutableStateOf("") }
                            var password by rememberSaveable { mutableStateOf("") }
                            var passwordVisible by rememberSaveable { mutableStateOf(false) }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Логотип «М»
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "М",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Вход в Машинист",
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                Text(
                                    text = "Пожалуйста, введите ваш email или войдите с VK ID.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 24.dp)
                                )

                                OneTap(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    onAuth = { _, accessToken ->
                                        val vkId = accessToken.userID.toString()
                                        val email = accessToken.userData.email ?: ""
                                        viewModel.registeredUserByVKIDForMigration(
                                            vkid = vkId,
                                            email = email
                                        )
                                    },
                                    onFail = { oneTapAuth, vkIdAuthFail ->
                                        Log.d("zzz", "onFail migration $oneTapAuth ${vkIdAuthFail.description}")
                                    },
                                    signInAnotherAccountButtonEnabled = true,
                                )

                                Spacer(
                                    modifier = Modifier
                                        .height(12.dp)
                                )
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    text = "или",
                                    style = styleHint
                                )


                                Spacer(
                                    modifier = Modifier
                                        .height(12.dp)
                                )

                                OutlinedTextFieldApp(
                                    value = email,
                                    onValueChange = { email = it },
                                    placeholder = { Text(text = "email", style = styleData) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = paddingBetweenView),
                                    singleLine = true,
                                    textStyle = styleData,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                                )


                                AnimatedVisibility(
                                    visible = email.isEmailValid(),
                                    enter = slideInVertically(animationSpec = tween(durationMillis = 300))
                                            + fadeIn(animationSpec = tween(durationMillis = 300)) +
                                            expandVertically(animationSpec = tween(durationMillis = 300)) +
                                            expandVertically(animationSpec = tween(durationMillis = 300)),
                                    exit = slideOutVertically(animationSpec = tween(durationMillis = 300))
                                            + fadeOut(animationSpec = tween(durationMillis = 300)) +
                                            shrinkVertically(animationSpec = tween(durationMillis = 300))
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextFieldApp(
                                            value = password,
                                            onValueChange = { password = it },
                                            placeholder = {
                                                Text(
                                                    text = "пароль",
                                                    style = styleData
                                                )
                                            },
                                            modifier = Modifier
                                                .padding(top = paddingBetweenView)
                                                .fillMaxWidth(),
                                            singleLine = true,
                                            textStyle = styleData,
                                            supportingText = {
                                                if (password.isNotEmpty() && password.length < MIN_LENGTH_PASSWORD) {
                                                    Text(
                                                        text = "Минимум $MIN_LENGTH_PASSWORD символа",
                                                        style = styleHint
                                                    )
                                                }
                                            },
                                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                            trailingIcon = {
                                                val image = if (passwordVisible)
                                                    R.drawable.outline_visibility_24
                                                else R.drawable.outline_visibility_off_24

                                                val description =
                                                    if (passwordVisible) "Скрыть пароль" else "Показать пароль"

                                                IconButton(onClick = {
                                                    passwordVisible = !passwordVisible
                                                }) {
                                                    Icon(
                                                        painter = painterResource(id = image),
                                                        description
                                                    )
                                                }
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                                        )


                                        Text(
                                            text = "Если не помните ваш пороль, можете указать новый или использовать VK ID",
                                            style = styleHint,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                    }
                                }

                                // Кнопка "Далее"
                                val text = "Далее"
                                AnimatedVisibility(
                                    visible = password.length >= MIN_LENGTH_PASSWORD && email.isEmailValid(),
                                    enter = slideInVertically(animationSpec = tween(durationMillis = 300))
                                            + fadeIn(animationSpec = tween(durationMillis = 300)),
                                    exit = slideOutVertically(animationSpec = tween(durationMillis = 300))
                                            + fadeOut(animationSpec = tween(durationMillis = 300)) +
                                            shrinkVertically(animationSpec = tween(durationMillis = 300))
                                ) {
                                    Button(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = paddingBetweenView),
                                        onClick = {
                                            viewModel.registeredUserByEmailForMigration(
                                                email,
                                                password
                                            )
                                        },
                                        shape = Shapes.medium,
                                        elevation = ButtonDefaults.elevatedButtonElevation(
                                            defaultElevation = 1.dp
                                        ),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                                        )
                                    ) {
                                        Text(
                                            text = text,
                                            style = buttonTextStyle,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                // Профиль
                else if (isLoggedIn) {
                    if (uiState.isProfileNetworkError) {
                        val vpnActive = remember(uiState.isProfileNetworkError) { isVpnActive(ctx) }
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.signal_disconnected_24px),
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (vpnActive) "Возможно, мешает VPN" else "Нет интернета",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                if (vpnActive) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = VPN_ERROR_HINT,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            Button(
                                onClick = viewModel::refresh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 24.dp)
                                    .align(Alignment.BottomCenter),
                                shape = Shapes.medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            ) {
                                Text(
                                    text = "Повторить",
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    } else SwipeRefresh(
                        state = rememberSwipeRefreshState(isRefreshing = uiState.isRefreshing),
                        onRefresh = viewModel::refresh,
                    )
                    {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .testTag("profile_lazy_column")
                        ) {
                            // Заголовок «Профиль»
                            item {
                                Text(
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        top = 8.dp,
                                        bottom = 16.dp,
                                    ),
                                    text = "Профиль",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            // ===================== ШАПКА ПРОФИЛЯ =====================
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(1.dp, Shapes.medium)
                                        .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
                                        .padding(vertical = 20.dp, horizontal = 18.dp),
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        AsyncData(
                                            resultState = uiState.vkUserState,
                                            loadingContent = { CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp) },
                                            errorContent = {
                                                ProfileAvatarPlaceholder()
                                                Text(
                                                    "Войдите через VK ID — подтянем фото и имя автоматически",
                                                    style = styleHint,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center,
                                                )
                                                OneTap(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    onAuth = { _, accessToken -> viewModel.attachVKID(accessToken.userID.toString()) },
                                                    onFail = { _, f -> scope.launch { snackbarHostState.showSnackbar("Ошибка привязки VK: ${f.description}") } },
                                                    signInAnotherAccountButtonEnabled = true,
                                                )
                                            }
                                        ) { vkUser ->
                                            val serverVkId = (uiState.userDetailsState as? ResultState.Success)?.data?.vkId
                                            val isVkLinkedOnServer = !serverVkId.isNullOrEmpty()
                                            if (vkUser != null) {
                                                if (vkUser.photoUrl != null) {
                                                    AsyncImage(
                                                        model = vkUser.photoUrl,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(84.dp)
                                                            .clip(CircleShape),
                                                    )
                                                } else {
                                                    ProfileAvatarPlaceholder()
                                                }
                                                Text(
                                                    text = vkUser.name,
                                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                ) {
                                                    VkBadge()
                                                    Text("Вход через VK ID", style = styleHint, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            } else {
                                                ProfileAvatarPlaceholder()
                                                Text(
                                                    "Войдите через VK ID — подтянем фото и имя автоматически",
                                                    style = styleHint,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center,
                                                )
                                                OneTap(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    onAuth = { _, accessToken ->
                                                        val vkId = accessToken.userID.toString()
                                                        if (isVkLinkedOnServer) viewModel.onVkAuthForLinkedAccount(vkId) else viewModel.attachVKID(vkId)
                                                    },
                                                    onFail = { _, f -> scope.launch { snackbarHostState.showSnackbar("Ошибка привязки VK: ${f.description}") } },
                                                    signInAnotherAccountButtonEnabled = true,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // ===================== EMAIL =====================
                            item {
                                ProfileGroupLabel("EMAIL")
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(1.dp, Shapes.medium)
                                        .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
                                        .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                ) {
                                    AsyncData(
                                        resultState = uiState.userDetailsState,
                                        loadingContent = { CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp) },
                                        errorContent = { Text("Ошибка загрузки", style = styleData, color = primaryColor) }
                                    ) { user ->
                                        user?.let {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceBright),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(painterResource(R.drawable.ic_mail_24), null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.tertiary)
                                                }
                                                Text(
                                                    text = it.email,
                                                    style = styleData,
                                                    color = primaryColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                IconButton(onClick = { showEditEmailDialog = true }) {
                                                    Icon(painterResource(com.z_company.core.R.drawable.ic_edit), contentDescription = "Изменить почту", tint = MaterialTheme.colorScheme.tertiary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // ===================== ПОДПИСКА =====================
                            item {
                                ProfileGroupLabel("ПОДПИСКА")
                                val onCard = if (hasSubscription) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(1.dp, Shapes.medium)
                                        .background(
                                            if (hasSubscription) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                            Shapes.medium,
                                        )
                                        .clickable { onBillingClick() }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (hasSubscription) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
                                                else MaterialTheme.colorScheme.surfaceBright
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painterResource(com.z_company.core.R.drawable.ic_star),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = if (hasSubscription) onCard else MaterialTheme.colorScheme.tertiary,
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                "Машинист Pro",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = onCard,
                                            )
                                            // Бейдж «АКТИВНА» — только при обычном шрифте: при крупном он
                                            // сжимал и обрезал название; статус активности и так виден по
                                            // сроку действия ниже.
                                            if (hasSubscription && LocalDensity.current.fontScale <= 1.15f) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.16f))
                                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                                ) {
                                                    Text("АКТИВНА", fontFamily = MonoFont, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = MaterialTheme.colorScheme.surfaceTint, maxLines = 1, softWrap = false)
                                                }
                                            }
                                        }
                                        AsyncData(
                                            resultState = uiState.purchasesEndTime,
                                            loadingContent = { },
                                            errorContent = { },
                                        ) { purchaseInfo ->
                                            Text(
                                                text = if (hasSubscription) "$purchaseInfo" else "Оформить подписку — откроется больше возможностей",
                                                style = styleHint,
                                                color = if (hasSubscription) onCard.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 2.dp),
                                            )
                                        }
                                    }
                                    Icon(
                                        painterResource(com.z_company.core.R.drawable.keyboard_arrow_right_24px),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = onCard.copy(alpha = 0.5f),
                                    )
                                }
                            }

                            // ===================== СИНХРОНИЗАЦИЯ =====================
                            item {
                                ProfileGroupLabel("СИНХРОНИЗАЦИЯ")
                                if (hasSubscription) {
                                    SyncCloudButton(
                                        iconRes = R.drawable.sync_24px,
                                        text = "Синхронизация",
                                        loading = uiState.showSyncDialog && !uiState.isSyncComplete,
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = viewModel::startSync,
                                    )
                                    uiState.updateAt?.let { timeInMillis ->
                                        val textSyncDate = uiState.dateAndTimeConverter?.getDateAndTime(timeInMillis) ?: ""
                                        Text(
                                            text = "Последняя синхронизация: $textSyncDate",
                                            style = styleHint,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 4.dp, top = 10.dp),
                                        )
                                    }
                                    AnimatedVisibility(
                                        visible = uiState.downloadRouteProgress != null,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically(),
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 12.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            val (saved, total) = uiState.downloadRouteProgress ?: (0 to 0)
                                            Text(
                                                text = "Загрузка маршрутов: $saved из $total",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LinearProgressIndicator(
                                                progress = if (total > 0) saved.toFloat() / total.toFloat() else 0f,
                                                modifier = Modifier.fillMaxWidth(),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surface,
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                                        text = "Раздел синхронизации доступен после оплаты подписки.",
                                        style = styleHint,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            // ===================== ВЫХОД =====================
                            item {
                                OutlinedButton(
                                    onClick = { viewModel.logOut() },
                                    modifier = Modifier
                                        .padding(top = 22.dp)
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = Shapes.medium,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                ) {
                                    Text(
                                        "Выйти из аккаунта",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    )
                                }
                            }
                            item { Spacer(modifier = Modifier.height(24.dp)) }
                        }
                    }
                }
                // Вход
                else {
                    // Изменено: Добавлен раздел для входа, если пользователь не залогинен
                    // Для чего: Чтобы показать форму входа с OneTap, полями email/password, кнопкой "Войти" и "Не помню пароль", аналогично SignInScreen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        var passwordVisible by remember { mutableStateOf(false) }
                        var login by remember { mutableStateOf(!viewModel.isFirstAppEntry.value) }

                        val paddingBetweenView = 12.dp
                        val dataStyle = MaterialTheme.typography.bodyLarge
                        val hintStyle = MaterialTheme.typography.bodyMedium
                        val buttonTextStyle = MaterialTheme.typography.bodySmall

                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .animateContentSize(  // Добавлено: Анимирует изменения размера всей формы
                                    animationSpec = tween(durationMillis = 300)
                                )
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),

                            ) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = paddingBetweenView * 2),
                                text = "Добро пожаловать!",
                                color = primaryColor,
                                style = MaterialTheme.typography.titleMedium
                            )


                            val colorTextLogin =
                                if (login) primaryColor else primaryColor.copy(alpha = 0.5f)
                            val colorTextSignin =
                                if (!login) primaryColor else primaryColor.copy(alpha = 0.5f)
                            SwitchApp(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = paddingBetweenView * 5),
                                checked = login,
                                onCheckedChange = { login = !login },
                                positiveContent = {
                                    Text(
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 8.dp
                                        ),
                                        text = "Вход",
                                        style = styleData,
                                        color = colorTextLogin
                                    )
                                },
                                negativeContent = {
                                    Text(
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 8.dp
                                        ),
                                        text = "Регистрация",
                                        style = styleData,
                                        color = colorTextSignin
                                    )
                                },
                            )

                            OneTap(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                onAuth = { _, accessToken ->
                                    if (login) {
                                        viewModel.authWithVKID(accessToken.userID.toString())
                                    } else {
                                        viewModel.registeredUserByVKID(
                                            vkid = accessToken.userID.toString(),
                                            email = accessToken.userData.email ?: ""
                                        )
                                    }
                                },
                                onFail = { oneTapAuth, vkIdAuthFail ->
                                    Log.d("zzz", "onFail login $oneTapAuth ${vkIdAuthFail.description}")
                                },
                                signInAnotherAccountButtonEnabled = true,
                            )


                            Spacer(
                                modifier = Modifier
                                    .height(12.dp)
                            )
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                text = "или",
                                style = styleHint
                            )


                            Spacer(
                                modifier = Modifier
                                    .height(12.dp)
                            )


                            OutlinedTextFieldApp(
                                value = email,
                                onValueChange = { email = it },
                                placeholder = { Text(text = "email", style = dataStyle) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = paddingBetweenView),
                                singleLine = true,
                                textStyle = dataStyle,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )


                            AnimatedVisibility(
                                visible = email.isEmailValid(),
                                enter = slideInVertically(animationSpec = tween(durationMillis = 300))
                                        + fadeIn(animationSpec = tween(durationMillis = 300)) +
                                        expandVertically(animationSpec = tween(durationMillis = 300)) +
                                        expandVertically(animationSpec = tween(durationMillis = 300)),
                                exit = slideOutVertically(animationSpec = tween(durationMillis = 300))
                                        + fadeOut(animationSpec = tween(durationMillis = 300)) +
                                        shrinkVertically(animationSpec = tween(durationMillis = 300))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextFieldApp(
                                        value = password,
                                        onValueChange = { password = it },
                                        placeholder = { Text(text = "пароль", style = dataStyle) },
                                        modifier = Modifier
                                            .padding(top = paddingBetweenView)
                                            .fillMaxWidth(),
                                        singleLine = true,
                                        textStyle = dataStyle,
                                        supportingText = {
                                            if (password.isNotEmpty() && password.length < MIN_LENGTH_PASSWORD) {
                                                Text(
                                                    text = "Минимум $MIN_LENGTH_PASSWORD символа",
                                                    style = hintStyle
                                                )
                                            }
                                        },
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            val image = if (passwordVisible)
                                                R.drawable.outline_visibility_24
                                            else R.drawable.outline_visibility_off_24

                                            val description =
                                                if (passwordVisible) "Скрыть пароль" else "Показать пароль"

                                            IconButton(onClick = {
                                                passwordVisible = !passwordVisible
                                            }) {
                                                Icon(
                                                    painter = painterResource(id = image),
                                                    description
                                                )
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                                    )

                                    if (login) {
                                        val onCooldown = forgotCooldownSeconds > 0
                                        TextButton(
                                            onClick = { viewModel.forgotRequest(email) },
                                            enabled = !onCooldown
                                        ) {
                                            Text(
                                                text = if (onCooldown) "Повторить через $forgotCooldownSeconds с"
                                                else "Создать новый пароль",
                                                style = buttonTextStyle,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                }
                            }


                            // Кнопка "Войти"
                            val text = if (login) "Войти" else "Зарегистрировать"
                            AnimatedVisibility(
                                visible = password.length >= MIN_LENGTH_PASSWORD && email.isEmailValid(),
                                enter = slideInVertically(animationSpec = tween(durationMillis = 300))
                                        + fadeIn(animationSpec = tween(durationMillis = 300)),
                                exit = slideOutVertically(animationSpec = tween(durationMillis = 300))
                                        + fadeOut(animationSpec = tween(durationMillis = 300)) +
                                        shrinkVertically(animationSpec = tween(durationMillis = 300))
                            ) {
                                Button(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = paddingBetweenView),
                                    onClick = {
                                        if (login) {
                                            viewModel.authWithEmail(email, password)
                                        } else {
                                            viewModel.registeredUserByEmail(email, password)
                                        }
                                    },
                                    enabled = if (!login) {
                                        isAcceptedPersonalDataProcessingPolicy && isLicenseAgreementAccepted
                                    } else true,
                                    shape = Shapes.medium,
                                    elevation = ButtonDefaults.elevatedButtonElevation(
                                        defaultElevation = 1.dp
                                    ),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                                    )
                                ) {
                                    Text(
                                        text = text,
                                        style = buttonTextStyle,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }


                            AnimatedVisibility(
                                visible = !login,
                                enter = slideInVertically(animationSpec = tween(durationMillis = 300))
                                        + fadeIn(animationSpec = tween(durationMillis = 300)),
                                exit = slideOutVertically(animationSpec = tween(durationMillis = 300))
                                        + fadeOut(animationSpec = tween(durationMillis = 300))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = paddingBetweenView * 3),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Checkbox(
                                            checked = isLicenseAgreementAccepted,
                                            onCheckedChange = {
                                                isLicenseAgreementAccepted =
                                                    !isLicenseAgreementAccepted
                                            })
                                        val urlIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            stringResource(id = R.string.url_to_license_agreement).toUri()
                                        )

                                        val text = "Я принимаю условия Лицензионного соглашения"
                                        val startIndex = 19
                                        val endIndex = text.length
                                        val annotationString = buildAnnotatedString {
                                            append(text)
                                            addStyle(
                                                style = SpanStyle(
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    textDecoration = TextDecoration.Underline
                                                ),
                                                start = startIndex, end = endIndex
                                            )
                                        }
                                        ClickableText(
                                            text = annotationString,
                                            style = hintStyle.copy(color = MaterialTheme.colorScheme.primary)
                                        ) {
                                            ctx.startActivity(urlIntent)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = paddingBetweenView),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Checkbox(
                                            checked = isAcceptedPersonalDataProcessingPolicy,
                                            onCheckedChange = {
                                                isAcceptedPersonalDataProcessingPolicy =
                                                    !isAcceptedPersonalDataProcessingPolicy
                                            })
                                        val urlIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            stringResource(id = R.string.url_to_personal_data_processing_policy).toUri()
                                        )

                                        val text =
                                            "Я ознакомлен с политикой обработки персональных данных"
                                        val startIndex = 15
                                        val endIndex = text.length
                                        val annotationString = buildAnnotatedString {
                                            append(text)
                                            addStyle(
                                                style = SpanStyle(
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    textDecoration = TextDecoration.Underline
                                                ),
                                                start = startIndex, end = endIndex
                                            )
                                        }
                                        ClickableText(
                                            text = annotationString,
                                            style = hintStyle.copy(color = MaterialTheme.colorScheme.primary)
                                        ) {
                                            ctx.startActivity(urlIntent)
                                        }
                                    }

                                }
                            }
                        }
                    }
                }

                if (authUiState is AuthState.Loading) {
                    GenericLoading(
                        message = "Выполняется вход...",
                        onCloseClick = {
                            viewModel.loginJob?.cancel()
                            viewModel.resetAuthState()
                        }
                    )
                }
                if (registeredUiState is RegistrationState.Loading) {
                    GenericLoading(
                        message = "Регистрируем нового полььзователя...",
                        onCloseClick = {
                            viewModel.loginJob?.cancel()
                            viewModel.resetRegisteredState()
                        }
                    )
                }
            }
        }
    }
}
