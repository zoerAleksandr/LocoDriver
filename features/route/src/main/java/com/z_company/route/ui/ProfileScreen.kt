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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.z_company.repository.remote_rest.AuthState
import com.z_company.route.R
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.component.SwitchApp
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.input.ImeAction
import com.z_company.repository.remote_rest.ResponseState
import com.z_company.route.component.AnimationDialog
import com.z_company.route.viewmodel.SyncType
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

const val MIN_LENGTH_PASSWORD = 4

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

    LaunchedEffect(forgotEmailState) {
        when (forgotEmailState) {
            is ResponseState.Initial -> {

            }

            is ResponseState.Loading -> {
                snackbarHostState.showSnackbar("Отправляем запрос...")
            }

            is ResponseState.Success -> {
                snackbarHostState.showSnackbar("Писмо отравлено на почту $email")
                viewModel.forgotResetState()
            }

            is ResponseState.Error -> {
                snackbarHostState.showSnackbar((forgotEmailState as ResponseState.Error).errorMessage)
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
                message = (authUiState as AuthState.Error).errorMessage,
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
                (registeredUiState as RegistrationState.Error).message
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

    AnimationDialog(
        showDialog = showEditEmailDialog,
        onDismissRequest = {
            showEditEmailDialog = false
            viewModel.resetUpdateEmailState()  // Сбрасываем состояние при закрытии
        }
    )
    {
        // 1. Состояние из ViewModel
        val updateEmailState by viewModel.uiState
            .map { it.updateEmailState }
            .collectAsState(initial = null)

        // 2. Локальная ошибка — показываем под TextField
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // 3. Сбрасываем состояние ViewModel каждый раз, когда диалог открывается
        LaunchedEffect(showEditEmailDialog) {
            if (showEditEmailDialog) {
                viewModel.resetUpdateEmailState()
                errorMessage = null                    // Очищаем ошибку при открытии
            }
        }

        // 4. Реакция на результат запроса
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
                        // Диалог НЕ закрываем!
                        viewModel.resetUpdateEmailState()
                    }

                    else -> {} // Loading — ничего не делаем
                }
            }
        }

        var emailValue by remember {
            mutableStateOf(viewModel.currentEmail)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface, Shapes.medium)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "Новый email",
                style = MaterialTheme.typography.titleSmall,
                color = primaryColor,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextFieldApp(
                modifier = Modifier.fillMaxWidth(),
                value = emailValue,
                onValueChange = {
                    emailValue = it
                    errorMessage = null                    // Очищаем ошибку при вводе
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                isError = errorMessage != null,            // Подсвечиваем поле красным
                supportingText = {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    modifier = Modifier.padding(end = 12.dp),
                    shape = Shapes.medium,
                    onClick = {
                        showEditEmailDialog = false
                        errorMessage = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Отмена", style = styleData, color = MaterialTheme.colorScheme.secondary)
                }

                Button(
                    shape = Shapes.medium,
                    enabled = emailValue.isEmailValid() &&
                            updateEmailState !is ResultState.Loading,
                    onClick = { viewModel.updateEmail(emailValue) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (updateEmailState is ResultState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Сохранить",
                            style = styleData,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }

    SyncProgressDialog(
        showDialog = uiState.showSyncDialog,
        isSyncSuccess = uiState.isSyncSuccess,
        isSyncComplete = uiState.isSyncComplete,
        syncType = uiState.syncType,
        progressMap = if (uiState.syncType == SyncType.Upload) uiState.syncUploadProgress else uiState.syncDownloadProgress,
        syncRouteErrors = uiState.syncRouteErrors,
        syncRoutesTotalAttempted = uiState.syncRoutesTotalAttempted,
        syncRoutesSavedCount = uiState.syncRoutesSavedCount,
        userId = uiState.syncReportUserId,
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
                                Text(
                                    text = "Переход на новый сервер",
                                    style = MaterialTheme.typography.titleMedium,
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
                                        Log.d("zzz", "onAuth")
                                        Log.d("zzz", "userID ${accessToken.userID}")
                                        val vkId = accessToken.userID.toString()
                                        val email = accessToken.userData.email ?: ""
                                        viewModel.registeredUserByVKIDForMigration(
                                            vkid = vkId,
                                            email = email
                                        )
                                    },
                                    onAuthCode = { authCodeData, bool ->
                                        Log.d("zzz", "onAuthCode")
                                        Log.d("zzz", "authCodeData $authCodeData")
                                        Log.d("zzz", "bool $bool")

                                    },
                                    onFail = { oneTapAuth, vkIdAuthFail ->
                                        Log.d("zzz", "onFail")
                                        Log.d("zzz", "oneTapAuth $oneTapAuth")
                                        Log.d("zzz", "vkIdAuthFail ${vkIdAuthFail.description}")
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
                                            color = primaryColor
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
                                            defaultElevation = 2.dp
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
                    SwipeRefresh(
                        state = rememberSwipeRefreshState(isRefreshing = uiState.isRefreshing),
                        onRefresh = viewModel::refresh,
                    )
                    {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        ) {
                            // ===================== АККАУНТ =====================
                            item {
                                Text(
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        bottom = 6.dp,
                                        top = 16.dp
                                    ),
                                    text = "Аккаунт",
                                    style = styleTitle
                                )
                                Box(
                                    modifier = Modifier
                                        .shadow(elevation = 2.dp, shape = Shapes.medium)
                                        .background(
                                            color = MaterialTheme.colorScheme.secondary,
                                            shape = Shapes.medium
                                        )
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // E-mail
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        )
                                        {
                                            Text(
                                                text = "E-mail",
                                                style = styleHint,
                                                color = primaryColor
                                            )
                                            AsyncData(
                                                resultState = uiState.userDetailsState,
                                                loadingContent = {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(
                                                            24.dp
                                                        ), strokeWidth = 2.dp
                                                    )
                                                },
                                                errorContent = {
                                                    Text(
                                                        "Ошибка загрузки",
                                                        style = styleData,
                                                        color = primaryColor
                                                    )
                                                }
                                            ) { user ->
                                                user?.let {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = it.email,
                                                            style = styleData,
                                                            color = primaryColor,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .padding(end = 8.dp)
                                                        )

                                                        Icon(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clickable(
                                                                    onClick = {
                                                                        showEditEmailDialog = true
                                                                    }
                                                                ),
                                                            painter = painterResource(com.z_company.core.R.drawable.ic_edit),
                                                            contentDescription = null,
                                                            tint = primaryColor
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        CustomDivider(orientation = Orientation.Horizontal)
                                        // VK
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "VK",
                                                style = styleHint,
                                                color = primaryColor
                                            )
                                            AsyncData(
                                                resultState = uiState.vkUserState,
                                                loadingContent = {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(24.dp),
                                                        strokeWidth = 2.dp
                                                    )
                                                },
                                                errorContent = {
                                                    OneTap(
                                                        modifier = Modifier
                                                            .fillMaxWidth(),
                                                        onAuth = { _, accessToken ->
                                                            viewModel.attachVKID(accessToken.userID.toString())
                                                        },
                                                        onAuthCode = { _, _ -> },
                                                        onFail = { oneTapAuth, vkIdAuthFail ->
                                                            Log.d(
                                                                "zzz",
                                                                "onFail $oneTapAuth ${vkIdAuthFail.description}"
                                                            )
                                                            scope.launch {
                                                                snackbarHostState.showSnackbar("Ошибка привязки VK: ${vkIdAuthFail.description}")
                                                            }
                                                        },
                                                        signInAnotherAccountButtonEnabled = true,
                                                    )
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("${it.entity.message}")
                                                    }
                                                }
                                            ) { name ->
                                                if (name != null) {
                                                    Text(
                                                        text = name,
                                                        style = styleData,
                                                        color = primaryColor
                                                    )
                                                } else {
                                                    OneTap(
                                                        modifier = Modifier
                                                            .fillMaxWidth(),
                                                        onAuth = { _, accessToken ->
                                                            viewModel.attachVKID(accessToken.userID.toString())
                                                        },
                                                        onAuthCode = { _, _ -> },
                                                        onFail = { oneTapAuth, vkIdAuthFail ->
                                                            Log.d(
                                                                "zzz",
                                                                "onFail $oneTapAuth ${vkIdAuthFail.description}"
                                                            )
                                                            scope.launch {
                                                                snackbarHostState.showSnackbar("Ошибка привязки VK: ${vkIdAuthFail.description}")
                                                            }
                                                        },
                                                        signInAnotherAccountButtonEnabled = true,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // ===================== ПОДПИСКИ =====================
                            item {
                                Text(
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        bottom = 6.dp,
                                        top = 16.dp
                                    ),
                                    text = "Покупки",
                                    style = styleTitle
                                )
                                Box(
                                    modifier = Modifier
                                        .shadow(elevation = 2.dp, shape = Shapes.medium)
                                        .background(
                                            color = MaterialTheme.colorScheme.secondary,
                                            shape = Shapes.medium
                                        )
                                        .clickable { onBillingClick() }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .fillMaxWidth(),
                                ) {
                                    AsyncData(
                                        resultState = uiState.purchasesEndTime,
                                        loadingContent = {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(
                                                    24.dp
                                                ), strokeWidth = 2.dp
                                            )
                                        },
                                        errorContent = {
                                            Text(
                                                "Ошибка загрузки",
                                                style = styleData,
                                                color = primaryColor
                                            )
                                        }
                                    ) { purchaseInfo ->
                                        val text = "$purchaseInfo"
                                        Text(text = text, style = styleData, color = primaryColor)
                                    }
                                }
                            }

                            // ===================== СИНХРОНИЗАЦИЯ =====================
                            item {
                                Text(
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        bottom = 6.dp,
                                        top = 16.dp
                                    ),
                                    text = "Синхронизация",
                                    style = styleTitle
                                )
                                AnimatedVisibility(visible = hasSubscription) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(elevation = 2.dp, shape = Shapes.medium)
                                            .background(
                                                color = MaterialTheme.colorScheme.secondary,
                                                shape = Shapes.medium
                                            )
                                            .padding(16.dp)
                                    ) {
                                        AsyncData(
                                            resultState = uiState.userDetailsState,
                                            loadingContent = {
                                                Row(
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        "Загрузка...",
                                                        style = styleData,
                                                        color = primaryColor
                                                    )
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(24.dp),
                                                        strokeWidth = 2.dp
                                                    )
                                                }
                                            },
                                            errorContent = {
                                                Text(
                                                    "Ошибка загрузки",
                                                    style = styleData,
                                                    color = primaryColor
                                                )
                                            }
                                        ) { user ->
                                            user?.let { u ->
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    uiState.updateAt?.let { timeInMillis ->
                                                        val textSyncDate =
                                                            uiState.dateAndTimeConverter?.getDateAndTime(
                                                                timeInMillis
                                                            ) ?: ""
                                                        Text(
                                                            text = "Последнее обновление данных",
                                                            style = styleHint,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Visible,
                                                            color = primaryColor
                                                        )
                                                        Text(
                                                            text = textSyncDate,
                                                            style = styleData,
                                                            overflow = TextOverflow.Visible,
                                                            color = primaryColor
                                                        )

                                                    }
                                                    CustomDivider(orientation = Orientation.Horizontal)

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable { viewModel.startSyncUpload() },
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            "Отправить в облако",
                                                            style = styleData,
                                                            color = primaryColor,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Visible,
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .padding(end = 12.dp)
                                                        )
                                                        AsyncData(
                                                            resultState = uiState.uploadState,
                                                            loadingContent = {
                                                                CircularProgressIndicator(
                                                                    modifier = Modifier.size(
                                                                        24.dp
                                                                    ),
                                                                    strokeWidth = 2.dp
                                                                )
                                                            }) {
                                                            Icon(
                                                                tint = MaterialTheme.colorScheme.tertiary,
                                                                painter = painterResource(id = com.z_company.core.R.drawable.rounded_cloud_upload_24),
                                                                contentDescription = null
                                                            )
                                                        }
                                                    }
                                                    CustomDivider(orientation = Orientation.Horizontal)

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable { viewModel.startSyncDownload() },
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            "Загрузить из облака",
                                                            style = styleData,
                                                            color = primaryColor,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Visible,
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .padding(end = 12.dp)
                                                        )
                                                        AsyncData(
                                                            resultState = uiState.downloadState,
                                                            loadingContent = {
                                                                CircularProgressIndicator(
                                                                    modifier = Modifier.size(
                                                                        24.dp
                                                                    ),
                                                                    strokeWidth = 2.dp
                                                                )
                                                            }) {
                                                            Icon(
                                                                tint = MaterialTheme.colorScheme.tertiary,
                                                                painter = painterResource(id = com.z_company.core.R.drawable.rounded_cloud_download_24),
                                                                contentDescription = null
                                                            )
                                                        }
                                                    }
                                                    AnimatedVisibility(
                                                        visible = uiState.downloadRouteProgress != null,
                                                        enter = fadeIn() + expandVertically(),
                                                        exit = fadeOut() + shrinkVertically()
                                                    ) {
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(16.dp)
                                                                .background(
                                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                                    RoundedCornerShape(8.dp)
                                                                )
                                                                .padding(16.dp),
                                                            horizontalAlignment = Alignment.CenterHorizontally
                                                        ) {
                                                            val (saved, total) = uiState.downloadRouteProgress
                                                                ?: (0 to 0)
                                                            Text(
                                                                text = "Загрузка маршрутов: $saved из $total",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            LinearProgressIndicator(
                                                                progress = if (total > 0) saved.toFloat() / total.toFloat() else 0f,
                                                                modifier = Modifier.fillMaxWidth(),
                                                                color = MaterialTheme.colorScheme.primary,
                                                                trackColor = MaterialTheme.colorScheme.surface
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (!hasSubscription) {
                                    Text(
                                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                                        text = "Раздел синхронизации доступен после оплаты подписки.",
                                        style = styleHint,
                                        color = primaryColor
                                    )
                                } else {
                                    Text(
                                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                                        text = "Выгрузка на сервер маршрутных листов, отвлечений и других настрое выполняется автоматически раз в 36 часов.",
                                        style = styleHint,
                                        color = primaryColor,
                                    )
                                }
                            }


                            // ===================== КНОПКИ =====================
                            item {
                                Button(
                                    modifier = Modifier
                                        .padding(top = 16.dp)
                                        .fillMaxWidth(),
                                    shape = Shapes.medium,
                                    elevation = ButtonDefaults.elevatedButtonElevation(
                                        defaultElevation = 2.dp
                                    ),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    onClick = { viewModel.logOut() }
                                ) {
                                    Text(
                                        "Выйти",
                                        color = MaterialTheme.colorScheme.secondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
//
//                        item {
//                            Button(
//                                modifier = Modifier
//                                    .padding(top = 16.dp)
//                                    .fillMaxWidth(),
//                                shape = Shapes.medium,
//                                elevation = ButtonDefaults.elevatedButtonElevation(
//                                    defaultElevation = 2.dp
//                                ),
//                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
//                                onClick = { viewModel.test() }
//                            ) {
//                                Text(
//                                    "ТЕСТ ПОЛУЧЕНИЕ ПОЛЬЗОВАТЕЛЯ",
//                                    color = MaterialTheme.colorScheme.secondary,
//                                    style = MaterialTheme.typography.bodySmall
//                                )
//                            }
//                        }
//
//                        item {
//                            Button(
//                                modifier = Modifier
//                                    .padding(top = 16.dp)
//                                    .fillMaxWidth(),
//                                shape = Shapes.medium,
//                                elevation = ButtonDefaults.elevatedButtonElevation(
//                                    defaultElevation = 2.dp
//                                ),
//                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
//                                onClick = { viewModel.removeUsersVKID() }
//                            ) {
//                                Text(
//                                    "ТЕСТ УДАЛЕНИЕ VKID",
//                                    color = MaterialTheme.colorScheme.secondary,
//                                    style = MaterialTheme.typography.bodySmall
//                                )
//                            }
//                        }
//
//                        item {
//                            Button(
//                                modifier = Modifier
//                                    .padding(top = 16.dp)
//                                    .fillMaxWidth(),
//                                shape = Shapes.medium,
//                                elevation = ButtonDefaults.elevatedButtonElevation(
//                                    defaultElevation = 2.dp
//                                ),
//                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
//                                onClick = { viewModel.saveUserSettingInRemote() }
//                            ) {
//                                Text(
//                                    "Test SAVE UserSetting",
//                                    color = MaterialTheme.colorScheme.secondary,
//                                    style = MaterialTheme.typography.bodySmall
//                                )
//                            }
//                        }
//
//                        item {
//                            Button(
//                                modifier = Modifier
//                                    .padding(top = 16.dp)
//                                    .fillMaxWidth(),
//                                shape = Shapes.medium,
//                                elevation = ButtonDefaults.elevatedButtonElevation(
//                                    defaultElevation = 2.dp
//                                ),
//                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
//                                onClick = { viewModel.getUserSettingFromRemote() }
//                            ) {
//                                Text(
//                                    "Test GET UserSetting",
//                                    color = MaterialTheme.colorScheme.secondary,
//                                    style = MaterialTheme.typography.bodySmall
//                                )
//                            }
//                        }
//
//                        item {
//                            Button(
//                                modifier = Modifier
//                                    .padding(top = 16.dp)
//                                    .fillMaxWidth(),
//                                shape = Shapes.medium,
//                                elevation = ButtonDefaults.elevatedButtonElevation(
//                                    defaultElevation = 2.dp
//                                ),
//                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
//                                onClick = { viewModel.saveSalarySettingInRemote() }
//                            ) {
//                                Text(
//                                    "Test SAVE SalarySetting",
//                                    color = MaterialTheme.colorScheme.secondary,
//                                    style = MaterialTheme.typography.bodySmall
//                                )
//                            }
//                        }
//
//                        item {
//                            Button(
//                                modifier = Modifier
//                                    .padding(top = 16.dp)
//                                    .fillMaxWidth(),
//                                shape = Shapes.medium,
//                                elevation = ButtonDefaults.elevatedButtonElevation(
//                                    defaultElevation = 2.dp
//                                ),
//                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
//                                onClick = { viewModel.getSalarySettingFromRemote() }
//                            ) {
//                                Text(
//                                    "Test GET SalarySetting",
//                                    color = MaterialTheme.colorScheme.secondary,
//                                    style = MaterialTheme.typography.bodySmall
//                                )
//                            }
//                        }
//                        item {
//                            Button(
//                                modifier = Modifier
//                                    .padding(top = 16.dp)
//                                    .fillMaxWidth(),
//                                shape = Shapes.medium,
//                                elevation = ButtonDefaults.elevatedButtonElevation(
//                                    defaultElevation = 2.dp
//                                ),
//                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
//                                onClick = { viewModel.saveMonthOfYearList() }
//                            ) {
//                                Text(
//                                    "Test SAVE Calendar",
//                                    color = MaterialTheme.colorScheme.secondary,
//                                    style = MaterialTheme.typography.bodySmall
//                                )
//                            }
//                        }
//
//                        item {
//                            Button(
//                                modifier = Modifier
//                                    .padding(top = 16.dp)
//                                    .fillMaxWidth(),
//                                shape = Shapes.medium,
//                                elevation = ButtonDefaults.elevatedButtonElevation(
//                                    defaultElevation = 2.dp
//                                ),
//                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
//                                onClick = { viewModel.getMonthOfYearList() }
//                            ) {
//                                Text(
//                                    "Test GET Calendar",
//                                    color = MaterialTheme.colorScheme.secondary,
//                                    style = MaterialTheme.typography.bodySmall
//                                )
//                            }
//                        }
//
//                        item {
//                            Button(
//                                modifier = Modifier
//                                    .padding(top = 16.dp)
//                                    .fillMaxWidth(),
//                                shape = Shapes.medium,
//                                elevation = ButtonDefaults.elevatedButtonElevation(
//                                    defaultElevation = 2.dp
//                                ),
//                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
//                                onClick = { viewModel.getRoutesFromRemote() }
//                            ) {
//                                Text(
//                                    "Test GET Routes",
//                                    color = MaterialTheme.colorScheme.secondary,
//                                    style = MaterialTheme.typography.bodySmall
//                                )
//                            }
//                        }

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
                                onAuthCode = { authCodeData, bool ->
                                    Log.d("zzz", "onAuthCode")
                                    Log.d("zzz", "authCodeData $authCodeData")
                                    Log.d("zzz", "bool $bool")

                                },
                                onFail = { oneTapAuth, vkIdAuthFail ->
                                    Log.d("zzz", "onFail")
                                    Log.d("zzz", "oneTapAuth $oneTapAuth")
                                    Log.d("zzz", "vkIdAuthFail ${vkIdAuthFail.description}")
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
                                        TextButton(
                                            onClick = { viewModel.forgotRequest(email) }
                                        ) {
                                            Text(
                                                text = "Создать новый пароль",
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
                                        defaultElevation = 2.dp
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
