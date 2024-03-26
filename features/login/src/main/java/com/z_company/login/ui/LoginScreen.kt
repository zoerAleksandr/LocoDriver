package com.z_company.login.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.z_company.login.viewmodel.LoginUiState

@Composable
fun LoginScreen(
    loginState: LoginUiState,
    onLoginSuccess: () -> Unit,
    requestingSMS: (number: String) -> Unit,
    loginWithPhone: (SMS: String) -> Unit,
) {
    if (loginState.session != null) {
        LaunchedEffect(Unit) {
            onLoginSuccess()
        }
    } else {
        LoginScreenContent(
            loginState = loginState,
            requestingSMS = requestingSMS,
            loginWithPhone = loginWithPhone,
        )
    }
}

@Composable
private fun LoginScreenContent(
    loginState: LoginUiState,
    requestingSMS: (number: String) -> Unit,
    loginWithPhone: (SMS: String) -> Unit,
) {
    var number by remember { mutableStateOf("") }
    var SMSCode by remember { mutableStateOf("") }

    Scaffold { padding ->
        if (loginState.loginState is ResultState.Loading) {
            GenericLoading(
                message = "Пожалуйста подождите..."
            )
        }
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            loginState.errorMessage?.let { message ->
                Text(text = message)
            }

            TextField(
                value = number,
                onValueChange = { number = it },
                label = { Text("number") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            TextField(
                value = SMSCode,
                onValueChange = { SMSCode = it },
                label = { Text("SMS") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Button(onClick = { requestingSMS(number) }) {
                Text("Получить СМС код")
            }
            Button(onClick = { loginWithPhone(SMSCode) }) {
                Text("Подтвердить SMS")
            }
        }
    }
}

