package com.z_company.login.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.GenericLoading

@Composable
fun LoginScreen(
    loginState: ResultState<Boolean>,
    onLoginSuccess: () -> Unit,
    registeredUser: (String, String, String) -> Unit,
    errorMessage: String?,
    logInUser: (String, String) -> Unit,
    onPasswordRecovery: () -> Unit,
    resetErrorState: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    if (loginState == ResultState.Success(true)) {
        LaunchedEffect(Unit) {
            onLoginSuccess()
        }
    }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar("$errorMessage")
            resetErrorState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LoginScreenContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            loginState = loginState,
            registeredUser = registeredUser,
            logInUser = logInUser,
            onPasswordRecovery = onPasswordRecovery
        )
    }
}

@Composable
private fun LoginScreenContent(
    modifier: Modifier,
    loginState: ResultState<Boolean>,
    registeredUser: (String, String, String) -> Unit,
    logInUser: (String, String) -> Unit,
    onPasswordRecovery: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Box(modifier = modifier) {
        if (loginState is ResultState.Loading) {
            GenericLoading(
                message = "Пожалуйста подождите..."
            )
        }
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
        ) {
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            TextButton(onClick = { onPasswordRecovery() }) {
                Text("Забыли пароль?")
            }
            Button(onClick = { logInUser(email, password) }) {
                Text("Вход")
            }

            Button(onClick = { registeredUser(email, password, email) }) {
                Text("Регистрация")
            }
        }
    }
}

