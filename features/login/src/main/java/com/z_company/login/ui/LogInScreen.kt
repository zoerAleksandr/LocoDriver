package com.z_company.login.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.GenericLoading
import com.z_company.core.ui.component.TopSnackbar
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.domain.entities.User
import com.z_company.login.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogInScreen(
    userState: ResultState<User?>,
    isEnableButton: Boolean,
    onLogInSuccess: () -> Unit,
    onRegisteredClick: (name: String, password: String, email: String) -> Unit,
    onBack: () -> Unit,
    email: String,
    setEmail: (String) -> Unit,
    password: String,
    setPassword: (String) -> Unit,
    confirm: String,
    setConfirm: (String) -> Unit,
    cancelRegistered: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    if (userState is ResultState.Loading) {
        GenericLoading(onCloseClick = cancelRegistered)
    }

    val paddingBetweenView = 12.dp

    if (userState is ResultState.Error) {
        LaunchedEffect(Unit) {
            scope.launch {
                snackbarHostState.showSnackbar("${userState.entity.throwable?.message}")
            }
        }
    }
    if (userState is ResultState.Success) {
        if (userState.data != null) {
            onLogInSuccess()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
                    .copy(containerColor = Color.Transparent)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackBarData ->
                TopSnackbar(snackBarData = snackBarData)
            }
        }
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = "Регистрация",
                style = AppTypography.getType().headlineMedium.copy(fontWeight = FontWeight.Light)
            )
            OutlinedTextField(
                value = email,
                onValueChange = {
                    setEmail(it)
                },
                label = { Text(text = "email", style = AppTypography.getType().bodyMedium) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = paddingBetweenView * 5),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            OutlinedTextField(
                value = password,
                onValueChange = {
                    setPassword(it)
                },
                label = { Text(text = "пароль", style = AppTypography.getType().bodyMedium) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = paddingBetweenView),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible)
                        R.drawable.outline_visibility_24
                    else R.drawable.outline_visibility_off_24

                    val description =
                        if (passwordVisible) "Скрыть пароль" else "Показать пароль"

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(painter = painterResource(id = image), description)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            OutlinedTextField(
                value = confirm,
                onValueChange = {
                    setConfirm(it)
                },
                label = {
                    Text(
                        text = "подтвердите пароль",
                        style = AppTypography.getType().bodyMedium
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = paddingBetweenView),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (confirmPasswordVisible)
                        R.drawable.outline_visibility_24
                    else R.drawable.outline_visibility_off_24

                    val description =
                        if (confirmPasswordVisible) "Скрыть пароль" else "Показать пароль"

                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(painter = painterResource(id = image), description)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = paddingBetweenView * 3),
                enabled = isEnableButton,
                shape = Shapes.medium,
                onClick = { onRegisteredClick(email, password, email) }
            ) {
                Text(text = "Зарегистрировать", style = AppTypography.getType().bodyLarge)
            }
//
//            Text(text = "Если у Вас есть аккаунт выполните вход")
//
//            TextButton(onClick = onSignInClick) {
//                Text(text = "Войти")
//            }
        }
    }
}