package com.z_company.login.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.GenericLoading
import com.z_company.core.ui.component.CustomSnackBar
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
    var isLicenseAgreementAccepted by remember {
        mutableStateOf(false)
    }
    val scope = rememberCoroutineScope()
    val dataTextStyle = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
    val subTitleTextStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )

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
                CustomSnackBar(snackBarData = snackBarData)
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
                label = { Text(text = "email", style = dataTextStyle) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = paddingBetweenView * 5),
                textStyle = dataTextStyle,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            OutlinedTextField(
                value = password,
                onValueChange = {
                    setPassword(it)
                },
                placeholder = { Text(text = "пароль", style = dataTextStyle) },
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
                textStyle = dataTextStyle,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            OutlinedTextField(
                value = confirm,
                onValueChange = {
                    setConfirm(it)
                },
                placeholder = {
                    Text(
                        text = "подтвердите пароль",
                        style = dataTextStyle
                    )
                },
                textStyle = dataTextStyle,
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = paddingBetweenView * 3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Checkbox(checked = isLicenseAgreementAccepted, onCheckedChange = {
                    isLicenseAgreementAccepted = !isLicenseAgreementAccepted
                })
                val ctx = LocalContext.current
                val urlIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(stringResource(id = R.string.url_to_license_agreement))
                )

                val text = "Я принимаю условия Лицензионного соглашения"
                val startIndex = 19
                val endIndex = text.length
                val annotationString = buildAnnotatedString {
                    append(text)
                    addStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.tertiary,
                            textDecoration = TextDecoration.Underline
                        ),
                        start = startIndex, end = endIndex
                    )
                }
                ClickableText(text = annotationString, style = hintStyle.copy(color = MaterialTheme.colorScheme.primary)) {
                    ctx.startActivity(urlIntent)
                }
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = paddingBetweenView),
                enabled = isEnableButton && isLicenseAgreementAccepted,
                shape = Shapes.medium,
                onClick = { onRegisteredClick(email, password, email) }
            ) {
                Text(text = "Зарегистрировать", style = subTitleTextStyle)
            }
        }
    }
}