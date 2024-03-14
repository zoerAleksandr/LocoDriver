package com.z_company.login.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.GenericError
import com.z_company.core.ui.theme.LocoAppTheme
import com.z_company.login.R


@Composable
fun LoginScreen(
    loginState: ResultState<Unit>?,
    onLoginSuccess: () -> Unit,
    resetLoginState: () -> Unit,
    onLoginClick: () -> Unit,
) {
    AsyncData(
        resultState = loginState,
        errorContent = {
            GenericError(
                error = ErrorEntity(message = stringResource(id = R.string.msg_login_error)),
                onDismissAction = resetLoginState,
            )
        }
    ) { state ->
        if (state != null) {
            LaunchedEffect(Unit) {
                onLoginSuccess()
            }
        } else {
            LoginScreenContent(
                onLoginClick = onLoginClick
            )
        }
    }
}

@Composable
private fun LoginScreenContent(
    onLoginClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Button(onClick = onLoginClick) {
            Text(text = stringResource(id = R.string.button_google_sign_in))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewLoginScreen() {
    LocoAppTheme {
        LoginScreenContent(
            onLoginClick = {}
        )
    }
}

