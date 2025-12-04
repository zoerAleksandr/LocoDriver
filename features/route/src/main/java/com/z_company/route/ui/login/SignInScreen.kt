package com.z_company.route.ui.login

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.z_company.route.R
import com.z_company.core.ResultState
import com.z_company.core.ui.component.GenericLoading
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.User
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.viewmodel.login.MIN_LENGTH_PASSWORD
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    userState: ResultState<User?>,
    email: String,
    password: String,
    setEmail: (String) -> Unit,
    setPassword: (String) -> Unit,
    onSignInSuccess: () -> Unit,
    onRegisteredClick: () -> Unit,
    logInUser: (String, String) -> Unit,
    onPasswordRecovery: () -> Unit,
    showErrorConfirmed: () -> Unit,
    isEnableButtonSignIn: Boolean,
    cancelSignIn: () -> Unit
) {
    val view = LocalView.current
    val backgroundColor = MaterialTheme.colorScheme.background

    // для изменения color status bar после изменения в PresentationBlock
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = backgroundColor.toArgb()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val paddingBetweenView = 12.dp

    val dataStyle = MaterialTheme.typography.bodyLarge
    val hintStyle = MaterialTheme.typography.bodyMedium
    val buttonTextStyle = MaterialTheme.typography.bodySmall

    if (userState is ResultState.Loading) {
        GenericLoading(
            message = "Выполняется вход...",
            onCloseClick = cancelSignIn
        )
    }
    if (userState is ResultState.Error) {
        LaunchedEffect(Unit) {
            scope.launch {
                snackbarHostState.showSnackbar("${userState.entity.throwable?.message}")
                showErrorConfirmed()
            }
        }
    }
    if (userState is ResultState.Success) {
        if (userState.data != null) {
            onSignInSuccess()
        }
    }
    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState){ snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = "Вход",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextFieldApp(
                    value = email,
                    onValueChange = { setEmail(it) },
                    placeholder = { Text(text = "email", style = dataStyle) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingBetweenView * 3),
                    singleLine = true,
                    textStyle = dataStyle,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                OutlinedTextFieldApp(
                    value = password,
                    onValueChange = { setPassword(it) },
                    placeholder = { Text(text = "пароль", style = dataStyle) },
                    modifier = Modifier
                        .padding(top = paddingBetweenView)
                        .fillMaxWidth(),
                    singleLine = true,
                    textStyle = dataStyle,
                    supportingText = {
                        if (password.isNotEmpty() && password.length < MIN_LENGTH_PASSWORD) {
                            Text(text = "Минимум $MIN_LENGTH_PASSWORD символа", style = hintStyle)
                        }
                    },
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
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingBetweenView * 3),
                    onClick = { logInUser(email, password) },
                    shape = Shapes.medium,
                    enabled = isEnableButtonSignIn,
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = 2.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                    )
                ) {
                    Text(text = "Войти", style = buttonTextStyle, color = MaterialTheme.colorScheme.secondary)
                }

                TextButton(
                    modifier = Modifier
                        .padding(top = paddingBetweenView),
                    onClick = { onPasswordRecovery() }) {
                    Text(text = "Забыли пароль?", style = buttonTextStyle, color = MaterialTheme.colorScheme.tertiary)
                }

            }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = paddingBetweenView)
                    .align(Alignment.BottomCenter),
                onClick = { onRegisteredClick() },
                shape = Shapes.medium,
                elevation = ButtonDefaults.elevatedButtonElevation(
                    defaultElevation = 2.dp
                ),
                colors = ButtonDefaults.buttonColors().copy(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(text = "Регистрация", style = buttonTextStyle, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}