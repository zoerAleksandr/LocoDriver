package com.z_company.route.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.CustomDivider
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.theme.Shapes
import com.z_company.route.component.ConfirmEmailDialog
import com.z_company.route.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogOut: () -> Unit,
    onBillingClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val styleData = MaterialTheme.typography.bodyLarge
    val styleHint = MaterialTheme.typography.bodyMedium
    val styleTitle = MaterialTheme.typography.titleSmall
    val primaryColor = MaterialTheme.colorScheme.primary

    // Snackbar на ошибки/успех загрузки-выгрузки
    LaunchedEffect(uiState.uploadState) {
        uiState.uploadState?.let { state ->
            when (state) {
                is ResultState.Error -> {
                    snackbarHostState.showSnackbar("Ошибка выгрузки данных.\n${state.entity.message}")
                    viewModel.resetUploadState()
                }

                is ResultState.Success -> {
                    val message = if (state.data == 0) {
                        "Все маршруты синхронизированы"
                    } else {
                        "Маршруты успешно сохранены на сервере. (${state.data})"
                    }
                    snackbarHostState.showSnackbar(message)
                    viewModel.resetUploadState()
                }

                else -> Unit
            }
        }
    }

    LaunchedEffect(uiState.downloadState) {
        uiState.downloadState?.let { state ->
            when (state) {
                is ResultState.Error -> {
                    snackbarHostState.showSnackbar("Ошибка загрузки данных.\n${state.entity.message}")
                    viewModel.resetDownloadState()
                }

                is ResultState.Success -> {
                    snackbarHostState.showSnackbar("Маршруты загружены. (${state.data})")
                    viewModel.resetDownloadState()
                }

                else -> Unit
            }
        }
    }

    // Выход из аккаунта
    LaunchedEffect(uiState.logOutState) {
        if (uiState.logOutState is ResultState.Success) {
            onLogOut()
        }
    }

    var showConfirmEmailDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                CustomSnackBar(snackBarData = data)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (showConfirmEmailDialog) {
                ConfirmEmailDialog(
                    onDismissRequest = { showConfirmEmailDialog = false },
                    onConfirmButton = {
                        viewModel.emailConfirmation()
                        showConfirmEmailDialog = false
                    },
                    emailForConfirm = viewModel.currentEmail,
                    onChangeEmail = viewModel::setEmail,
                    enableButtonConfirmVerification = uiState.resentVerificationEmailButton
                )
            }

            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing = uiState.isRefreshing),
                onRefresh = viewModel::refresh,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    // ===================== АККАУНТ =====================
                    item {
                        Text(
                            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp, top = 16.dp),
                            text = "Аккаунт",
                            style = styleTitle
                        )
                        Box(
                            modifier = Modifier
                                .shadow(elevation = 2.dp, shape = Shapes.medium)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = Shapes.medium
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // E-mail
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "E-mail",
                                        style = styleHint,
                                        color = primaryColor
                                    )
                                    AsyncData(
                                        resultState = uiState.userDetailsState,
                                        loadingContent = {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(
                                                    24.dp
                                                ), strokeWidth = 2.dp
                                            )
                                        },
                                        errorContent = {
                                            Text(
                                                "Ошибка загрузки",
                                                style = styleData,
                                                color = primaryColor
                                            )
                                        }
                                    ) { user ->
                                        user?.let {
                                            Text(it.email, style = styleData, color = primaryColor)
                                        }
                                    }
                                }
                                CustomDivider(orientation = Orientation.Horizontal)

                                // Статус подтверждения
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .padding(end = 12.dp),
                                        text = "Статус",
                                        style = styleHint,
                                        color = primaryColor
                                    )
                                    AsyncData(
                                        resultState = uiState.userDetailsState,
                                        loadingContent = {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(
                                                    24.dp
                                                ), strokeWidth = 2.dp
                                            )
                                        },
                                        errorContent = {
                                            Text(
                                                "Ошибка загрузки",
                                                style = styleData,
                                                color = primaryColor
                                            )
                                        }
                                    ) { user ->
                                        user?.let {
                                            val statusText =
                                                if (it.isVerification) "Подтвержден" else "Не подтвержден"
                                            Text(
                                                modifier = Modifier.clickable(enabled = !it.isVerification) {
                                                    showConfirmEmailDialog = true
                                                },
                                                text = statusText,
                                                style = styleData,
                                                color = if (!it.isVerification) MaterialTheme.colorScheme.tertiary else primaryColor
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                            text = "Подтверждение e-mail нужно для синхронизации с облачным хранилищем.",
                            style = styleHint,
                            color = primaryColor
                        )
                    }

                    // ===================== ПОДПИСКИ =====================
                    item {
                        Text(
                            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp, top = 16.dp),
                            text = "Подписки",
                            style = styleTitle
                        )
                        Box(
                            modifier = Modifier
                                .shadow(elevation = 2.dp, shape = Shapes.medium)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = Shapes.medium
                                )
                                .clickable { onBillingClick() }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .fillMaxWidth(),
                        ) {
                            AsyncData(
                                resultState = uiState.purchasesEndTime,
                                loadingContent = {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(
                                            24.dp
                                        ), strokeWidth = 2.dp
                                    )
                                },
                                errorContent = {
                                    Text(
                                        "Ошибка загрузки",
                                        style = styleData,
                                        color = primaryColor
                                    )
                                }
                            ) { purchaseInfo ->
                                val text =
                                    if (purchaseInfo.isNullOrEmpty()) "Оформить за 69₽ в месяц" else "Активна до $purchaseInfo"
                                Text(text = text, style = styleData, color = primaryColor)
                            }
                        }
                    }

                    // ===================== СИНХРОНИЗАЦИЯ =====================
                    item {
                        Text(
                            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp, top = 16.dp),
                            text = "Синхронизация",
                            style = styleTitle
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(elevation = 2.dp, shape = Shapes.medium)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = Shapes.medium
                                )
                                .padding(16.dp)
                        ) {
                            AsyncData(
                                resultState = uiState.userDetailsState,
                                loadingContent = {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Загрузка...", style = styleData, color = primaryColor)
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                },
                                errorContent = {
                                    Text(
                                        "Ошибка загрузки",
                                        style = styleData,
                                        color = primaryColor
                                    )
                                }
                            ) { user ->
                                user?.let { u ->
                                    if (u.isVerification) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            uiState.updateAt?.let { timeInMillis ->
                                                val textSyncDate =
                                                    uiState.dateAndTimeConverter?.getDateAndTime(timeInMillis) ?: ""
                                                Text(
                                                    text = "Последнее обновление данных",
                                                    style = styleHint,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Visible,
                                                    color = primaryColor
                                                )
                                                Text(
                                                    text = textSyncDate,
                                                    style = styleData,
                                                    overflow = TextOverflow.Visible,
                                                    color = primaryColor
                                                )

                                            }
                                            CustomDivider(orientation = Orientation.Horizontal)

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.onUploadToRemote() },
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    "Отправить в облако",
                                                    style = styleData,
                                                    color = primaryColor,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Visible,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(end = 12.dp)
                                                )
                                                AsyncData(
                                                    resultState = uiState.uploadState,
                                                    loadingContent = {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(
                                                                24.dp
                                                            ),
                                                            strokeWidth = 2.dp
                                                        )
                                                    }) {
                                                    Icon(
                                                        tint = MaterialTheme.colorScheme.tertiary,
                                                        painter = painterResource(id = com.z_company.core.R.drawable.rounded_cloud_upload_24),
                                                        contentDescription = null
                                                    )
                                                }
                                            }
                                            CustomDivider(orientation = Orientation.Horizontal)

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.onDownloadFromRemote() },
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    "Загрузить из облака",
                                                    style = styleData,
                                                    color = primaryColor,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Visible,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(end = 12.dp)
                                                )
                                                AsyncData(
                                                    resultState = uiState.downloadState,
                                                    loadingContent = {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(
                                                                24.dp
                                                            ),
                                                            strokeWidth = 2.dp
                                                        )
                                                    }) {
                                                    Icon(
                                                        tint = MaterialTheme.colorScheme.tertiary,
                                                        painter = painterResource(id = com.z_company.core.R.drawable.rounded_cloud_download_24),
                                                        contentDescription = null
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                            text = "Выгрузка на сервер маршрутных листов выполняется автоматически при наличии подписки и с подтвержденным e-mail",
                            style = styleHint,
                            color = primaryColor,
                        )
                    }

                    // ===================== КНОПКИ =====================
                    item {
                        Button(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth(),
                            shape = Shapes.medium,
                            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            onClick = { viewModel.logOut() }
                        ) {
                            Text(
                                "Выйти",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
//
//                    item {
//                        Button(
//                            modifier = Modifier
//                                .padding(top = 16.dp)
//                                .fillMaxWidth(),
//                            shape = Shapes.medium,
//                            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
//                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
//                            onClick = { viewModel.cleanRepo() }
//                        ) {
//                            Text(
//                                "Очистка",
//                                color = MaterialTheme.colorScheme.secondary,
//                                style = MaterialTheme.typography.bodySmall
//                            )
//                        }
//                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}