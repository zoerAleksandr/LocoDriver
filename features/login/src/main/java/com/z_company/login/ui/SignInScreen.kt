package com.z_company.login.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.GenericLoading
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.domain.entities.User
import com.z_company.login.R
import com.z_company.core.R as CoreR
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    userState: ResultState<User?>,
    onSignInSuccess: () -> Unit,
    onRegisteredClick: () -> Unit,
    logInUser: (String, String) -> Unit,
    onPasswordRecovery: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val paddingBetweenView = 12.dp

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
            onSignInSuccess()
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            Image(
                modifier = Modifier.padding(top = 24.dp),
                painter = painterResource(id = CoreR.drawable.logo_v2_1),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(text = "email", style = AppTypography.getType().bodyMedium) },
                    modifier = Modifier
                        .fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(text = "пароль", style = AppTypography.getType().bodyMedium) },
                    modifier = Modifier
                        .padding(top = paddingBetweenView)
                        .fillMaxWidth(),
                    singleLine = true,
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
                        .padding(top = paddingBetweenView * 2),
                    onClick = { logInUser(email, password) },
                    shape = Shapes.medium
                ) {
                    Text(text = "Вход", style = AppTypography.getType().bodyLarge)
                }
                TextButton(
                    modifier = Modifier
                        .padding(top = paddingBetweenView),
                    onClick = { onPasswordRecovery() }) {
                    Text(text = "Забыли пароль?", style = AppTypography.getType().bodyMedium)
                }


            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = paddingBetweenView),
                onClick = { onRegisteredClick() },
                shape = Shapes.medium
            ) {
                Text(text = "Регистрация", style = AppTypography.getType().bodyLarge)
            }
        }
    }
}