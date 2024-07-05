package com.z_company.login.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.GenericLoading
import com.z_company.domain.entities.User
import kotlinx.coroutines.launch

@Composable
fun LogInScreen(
    userState: ResultState<User?>,
    isEnableButton: Boolean,
    onLogInSuccess: () -> Unit,
    onRegisteredClick: (name: String, password: String, email: String) -> Unit,
    onSignInClick: () -> Unit,
    email: String,
    setEmail: (String) -> Unit,
    password: String,
    setPassword: (String) -> Unit,
    confirm: String,
    setConfirm: (String) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val scope = rememberCoroutineScope()

    if (userState is ResultState.Loading) {
        GenericLoading()
    }
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = {
                    setEmail(it)
                },
                label = { Text("email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            OutlinedTextField(
                value = password,
                onValueChange = {
                    setPassword(it)
                },
                label = { Text("пароль") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = confirm,
                onValueChange = {
                    setConfirm(it)
                },
                label = { Text("подтвердите пароль") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Button(
                enabled = isEnableButton,
                onClick = { onRegisteredClick(email, password, email) }
            ) {
                Text("Зарегистрировать")
            }

            Text(text = "Если у Вас есть аккаунт выполните вход")

            TextButton(onClick = onSignInClick) {
                Text(text = "Войти")
            }
        }
    }
}