package com.z_company.shared.ui.screen

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.shared.platform.AuthUiState
import com.z_company.shared.platform.SyncUiState
import com.z_company.shared.theme.Shapes
import com.z_company.shared.viewmodel.ProfileSharedViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val MIN_LENGTH_PASSWORD = 4

/**
 * Shared Profile screen.
 *
 * When user is logged in — shows profile with account, subscription, sync sections.
 * When not logged in — shows login/registration form.
 *
 * @param vkAuthContent Composable slot for VK ID OneTap button (Android only, empty on iOS).
 * @param vkProfileContent Composable slot for VK profile display in account section (Android only).
 * @param policyContent Composable slot for license/privacy checkboxes (Android uses ClickableText+Intent).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit = {},
    onPurchasesClick: () -> Unit,
    vkAuthContent: @Composable (isLogin: Boolean) -> Unit = {},
    vkProfileContent: @Composable () -> Unit = {},
    policyContent: @Composable (
        isLicenseAccepted: Boolean,
        onLicenseChanged: (Boolean) -> Unit,
        isPersonalDataAccepted: Boolean,
        onPersonalDataChanged: (Boolean) -> Unit,
    ) -> Unit = { _, _, _, _ -> },
) {
    val viewModel: ProfileSharedViewModel = koinInject()
    val scope = rememberCoroutineScope()

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val hasSubscription by viewModel.hasSubscription.collectAsState()
    val subscriptionEndTime by viewModel.subscriptionEndTime.collectAsState()
    val vkUserName by viewModel.vkUserName.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val styleData = MaterialTheme.typography.bodyLarge
    val styleHint = MaterialTheme.typography.bodyMedium
    val styleTitle = MaterialTheme.typography.titleSmall
    val primaryColor = MaterialTheme.colorScheme.primary
    val paddingBetweenView = 12.dp

    // Handle auth state events
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Long
                )
            }
            is AuthUiState.Success -> {
                state.message?.let {
                    snackbarHostState.showSnackbar(message = it)
                }
            }
            else -> {}
        }
    }

    // Handle sync state events
    LaunchedEffect(syncState) {
        when (val state = syncState) {
            is SyncUiState.Complete -> {
                if (state.message.isNotEmpty()) {
                    snackbarHostState.showSnackbar(message = state.message)
                }
            }
            is SyncUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Long
                )
            }
            else -> {}
        }
    }

    // Edit email dialog state
    var showEditEmailDialog by remember { mutableStateOf(false) }
    var editEmailValue by remember { mutableStateOf("") }
    var editEmailError by remember { mutableStateOf<String?>(null) }

    if (showEditEmailDialog) {
        AlertDialog(
            onDismissRequest = {
                showEditEmailDialog = false
                editEmailError = null
            },
            title = { Text("Новый email", style = styleTitle, color = primaryColor) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editEmailValue,
                        onValueChange = {
                            editEmailValue = it
                            editEmailError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = styleData,
                        isError = editEmailError != null,
                        supportingText = editEmailError?.let { err ->
                            { Text(err, color = MaterialTheme.colorScheme.error, style = styleHint) }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Button(
                    shape = Shapes.medium,
                    enabled = editEmailValue.isValidEmail(),
                    onClick = {
                        viewModel.updateEmail(editEmailValue)
                        showEditEmailDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Сохранить", style = styleData, color = MaterialTheme.colorScheme.secondary)
                }
            },
            dismissButton = {
                Button(
                    shape = Shapes.medium,
                    onClick = {
                        showEditEmailDialog = false
                        editEmailError = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Отмена", style = styleData, color = MaterialTheme.colorScheme.secondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Профиль",
                        style = styleTitle,
                        color = primaryColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {

            // Loading overlay
            if (authState is AuthUiState.Loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Выполняется вход...", style = styleData, color = primaryColor)
                    }
                }
            }
            // Profile (logged in)
            else if (isLoggedIn) {
                ProfileLoggedInContent(
                    viewModel = viewModel,
                    userEmail = userEmail,
                    hasSubscription = hasSubscription,
                    subscriptionEndTime = subscriptionEndTime,
                    vkUserName = vkUserName,
                    syncState = syncState,
                    onPurchasesClick = onPurchasesClick,
                    onEditEmailClick = {
                        editEmailValue = userEmail ?: ""
                        showEditEmailDialog = true
                    },
                    vkProfileContent = vkProfileContent,
                    styleData = styleData,
                    styleHint = styleHint,
                    styleTitle = styleTitle,
                    primaryColor = primaryColor,
                )
            }
            // Login form (not logged in)
            else {
                LoginFormContent(
                    viewModel = viewModel,
                    vkAuthContent = vkAuthContent,
                    policyContent = policyContent,
                    styleData = styleData,
                    styleHint = styleHint,
                    primaryColor = primaryColor,
                    paddingBetweenView = paddingBetweenView,
                )
            }
        }
    }
}

// ===================== PROFILE (LOGGED IN) =====================

@Composable
private fun ProfileLoggedInContent(
    viewModel: ProfileSharedViewModel,
    userEmail: String?,
    hasSubscription: Boolean,
    subscriptionEndTime: Long,
    vkUserName: String?,
    syncState: SyncUiState,
    onPurchasesClick: () -> Unit,
    onEditEmailClick: () -> Unit,
    vkProfileContent: @Composable () -> Unit,
    styleData: androidx.compose.ui.text.TextStyle,
    styleHint: androidx.compose.ui.text.TextStyle,
    styleTitle: androidx.compose.ui.text.TextStyle,
    primaryColor: Color,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        // ===================== АККАУНТ =====================
        item {
            Text(
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp, top = 16.dp),
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
                    ) {
                        Text(text = "E-mail", style = styleHint, color = primaryColor)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = userEmail ?: "—",
                                style = styleData,
                                color = primaryColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Icon(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable(onClick = onEditEmailClick),
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Редактировать email",
                                tint = primaryColor
                            )
                        }
                    }

                    HorizontalDivider()

                    // VK profile slot
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "VK", style = styleHint, color = primaryColor)
                        if (vkUserName != null) {
                            Text(text = vkUserName, style = styleData, color = primaryColor)
                        } else {
                            vkProfileContent()
                        }
                    }
                }
            }
        }

        // ===================== ПОКУПКИ =====================
        item {
            Text(
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp, top = 16.dp),
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
                    .clickable { onPurchasesClick() }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
            ) {
                val text = if (hasSubscription && subscriptionEndTime > 0L) {
                    "Подписка активна"
                } else {
                    "Подписка не оплачена"
                }
                Text(text = text, style = styleData, color = primaryColor)
            }
        }

        // ===================== СИНХРОНИЗАЦИЯ =====================
        item {
            Text(
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp, top = 16.dp),
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Sync in progress indicator
                        if (syncState is SyncUiState.InProgress) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = (syncState as SyncUiState.InProgress).message.ifEmpty { "Синхронизация..." },
                                    style = styleData,
                                    color = primaryColor,
                                    modifier = Modifier.weight(1f)
                                )
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            HorizontalDivider()
                        }

                        // Upload
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.startSyncUpload() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Отправить в облако",
                                style = styleData,
                                color = primaryColor,
                                maxLines = 2,
                                overflow = TextOverflow.Visible,
                                modifier = Modifier.weight(1f).padding(end = 12.dp)
                            )
                            Icon(
                                tint = MaterialTheme.colorScheme.tertiary,
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Отправить"
                            )
                        }

                        HorizontalDivider()

                        // Download
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.startSyncDownload() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Загрузить из облака",
                                style = styleData,
                                color = primaryColor,
                                maxLines = 2,
                                overflow = TextOverflow.Visible,
                                modifier = Modifier.weight(1f).padding(end = 12.dp)
                            )
                            Icon(
                                tint = MaterialTheme.colorScheme.tertiary,
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Загрузить"
                            )
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
                    text = "Выгрузка на сервер маршрутных листов, отвлечений и других настроек выполняется автоматически раз в 36 часов.",
                    style = styleHint,
                    color = primaryColor,
                )
            }
        }

        // ===================== ВЫЙТИ =====================
        item {
            Button(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                shape = Shapes.medium,
                elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = { viewModel.logout() }
            ) {
                Text(
                    "Выйти",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// ===================== LOGIN FORM =====================

@Composable
private fun LoginFormContent(
    viewModel: ProfileSharedViewModel,
    vkAuthContent: @Composable (isLogin: Boolean) -> Unit,
    policyContent: @Composable (
        isLicenseAccepted: Boolean,
        onLicenseChanged: (Boolean) -> Unit,
        isPersonalDataAccepted: Boolean,
        onPersonalDataChanged: (Boolean) -> Unit,
    ) -> Unit,
    styleData: androidx.compose.ui.text.TextStyle,
    styleHint: androidx.compose.ui.text.TextStyle,
    primaryColor: Color,
    paddingBetweenView: androidx.compose.ui.unit.Dp,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var login by rememberSaveable { mutableStateOf(true) }
    var isLicenseAccepted by rememberSaveable { mutableStateOf(false) }
    var isPersonalDataAccepted by rememberSaveable { mutableStateOf(false) }

    val buttonTextStyle = MaterialTheme.typography.bodySmall

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .animateContentSize(animationSpec = tween(durationMillis = 300))
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

            // Login / Register toggle
            val colorTextLogin = if (login) primaryColor else primaryColor.copy(alpha = 0.5f)
            val colorTextSignin = if (!login) primaryColor else primaryColor.copy(alpha = 0.5f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = paddingBetweenView * 3),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = { login = true }) {
                    Text("Вход", style = styleData, color = colorTextLogin)
                }
                Spacer(modifier = Modifier.size(16.dp))
                TextButton(onClick = { login = false }) {
                    Text("Регистрация", style = styleData, color = colorTextSignin)
                }
            }

            // VK Auth slot
            vkAuthContent(login)

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = "или",
                style = styleHint
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Email field
            OutlinedTextField(
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

            // Password (appears when email is valid)
            AnimatedVisibility(
                visible = email.isValidEmail(),
                enter = slideInVertically(animationSpec = tween(300))
                        + fadeIn(animationSpec = tween(300))
                        + expandVertically(animationSpec = tween(300)),
                exit = slideOutVertically(animationSpec = tween(300))
                        + fadeOut(animationSpec = tween(300))
                        + shrinkVertically(animationSpec = tween(300))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text(text = "пароль", style = styleData) },
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
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff
                            val description = if (passwordVisible) "Скрыть пароль" else "Показать пароль"
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = description)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    if (login) {
                        TextButton(onClick = { viewModel.forgotPassword(email) }) {
                            Text(
                                text = "Создать новый пароль",
                                style = buttonTextStyle,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            // Submit button
            AnimatedVisibility(
                visible = password.length >= MIN_LENGTH_PASSWORD && email.isValidEmail(),
                enter = slideInVertically(animationSpec = tween(300))
                        + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(animationSpec = tween(300))
                        + fadeOut(animationSpec = tween(300))
                        + shrinkVertically(animationSpec = tween(300))
            ) {
                val text = if (login) "Войти" else "Зарегистрировать"
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingBetweenView),
                    onClick = {
                        if (login) {
                            viewModel.authWithEmail(email, password)
                        } else {
                            viewModel.registerWithEmail(email, password)
                        }
                    },
                    enabled = if (!login) {
                        isLicenseAccepted && isPersonalDataAccepted
                    } else true,
                    shape = Shapes.medium,
                    elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
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

            // Policy checkboxes (registration only) — platform slot
            AnimatedVisibility(
                visible = !login,
                enter = slideInVertically(animationSpec = tween(300))
                        + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(animationSpec = tween(300))
                        + fadeOut(animationSpec = tween(300))
            ) {
                policyContent(
                    isLicenseAccepted,
                    { isLicenseAccepted = it },
                    isPersonalDataAccepted,
                    { isPersonalDataAccepted = it },
                )
            }
        }
    }
}

/**
 * Simple email validation (KMP compatible, no android.util.Patterns).
 */
private fun String.isValidEmail(): Boolean {
    if (isBlank()) return false
    val atIndex = indexOf('@')
    if (atIndex < 1) return false
    val dotIndex = lastIndexOf('.')
    return dotIndex > atIndex + 1 && dotIndex < length - 1
}
