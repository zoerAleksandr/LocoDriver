package com.z_company.login.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.GenericLoading
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordRecoveryScreen(
    resultState: ResultState<Unit>,
    onBack: () -> Unit,
    requestPasswordReset: (email: String) -> Unit,
    isEnableButton: Boolean,
    requestHasBeenSend: Boolean,
    isEmailValid: (email: String) -> Unit,
    cancelRequest: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var email by remember {
        mutableStateOf("")
    }
    val paddingBetweenView = 12.dp
    val scope = rememberCoroutineScope()


    LaunchedEffect(requestHasBeenSend) {
        if (requestHasBeenSend) {
            snackbarHostState.showSnackbar("Письмо отправлено на почту $email")
            onBack()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
                    .copy(containerColor = Color.Transparent)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
            }
        }
    ) { paddingValues ->
        if (resultState is ResultState.Error) {
            LaunchedEffect(Unit) {
                scope.launch {
                    snackbarHostState.showSnackbar("${resultState.entity.throwable?.message}")
                }
            }
        }
        if (resultState is ResultState.Loading) {
            GenericLoading(
                message = "Отправляем запрос...",
                onCloseClick = cancelRequest
            )
        }
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Восстановление пароля",
                style = AppTypography.getType().headlineMedium.copy(fontWeight = FontWeight.Light)
            )

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = paddingBetweenView * 7),
                value = email,
                onValueChange = {
                    email = it
                    isEmailValid(it)
                },
                label = {
                    Text(
                        text = "email",
                        style = AppTypography.getType().bodyMedium
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
            Button(
                modifier = Modifier
                    .padding(top = paddingBetweenView * 3)
                    .fillMaxWidth(),
                onClick = { requestPasswordReset(email) },
                enabled = isEnableButton,
                shape = Shapes.medium
            ) {
                Text(text = "Отправить код", style = AppTypography.getType().bodyLarge)
            }
        }
    }
}