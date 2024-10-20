package com.z_company.login.ui

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.GenericLoading
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.domain.entities.User
import com.z_company.login.R
import com.z_company.login.viewmodel.MIN_LENGTH_PASSWORD
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
    val snackbarHostState = remember { SnackbarHostState() }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val paddingBetweenView = 12.dp

    val dataTextStyle = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
    val subTitleTextStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
    val styleHint = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )
    if (userState is ResultState.Loading) {
        GenericLoading(onCloseClick = cancelSignIn)
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
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = "Вход",
                    style = AppTypography.getType().headlineMedium.copy(fontWeight = FontWeight.Light)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { setEmail(it) },
                    placeholder = { Text(text = "email", style = dataTextStyle) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingBetweenView * 5),
                    singleLine = true,
                    textStyle = dataTextStyle,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { setPassword(it) },
                    label = { Text(text = "пароль", style = dataTextStyle) },
                    modifier = Modifier
                        .padding(top = paddingBetweenView)
                        .fillMaxWidth(),
                    singleLine = true,
                    textStyle = dataTextStyle,
                    supportingText = {
                        if (password.isNotEmpty() && password.length < MIN_LENGTH_PASSWORD) {
                            Text(text = "Минимум $MIN_LENGTH_PASSWORD символа")
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
                    enabled = isEnableButtonSignIn
                ) {
                    Text(text = "Войти", style = subTitleTextStyle)
                }
                TextButton(
                    modifier = Modifier
                        .padding(top = paddingBetweenView),
                    onClick = { onPasswordRecovery() }) {
                    Text(text = "Забыли пароль?", style = styleHint)
                }

            }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = paddingBetweenView)
                    .align(Alignment.BottomCenter),
                onClick = { onRegisteredClick() },
                shape = Shapes.medium,
                colors = ButtonDefaults.buttonColors().copy(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(text = "Регистрация", style = subTitleTextStyle)
            }
        }
    }
}