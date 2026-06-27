package com.z_company.route.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.flowWithLifecycle
import com.robokassa.library.pay.RobokassaPayLauncher
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.Product
import com.z_company.route.R
import com.z_company.route.viewmodel.BillingEvent
import com.z_company.route.viewmodel.BillingState
import com.z_company.route.viewmodel.PurchasesViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    viewModel: PurchasesViewModel,
    billingState: BillingState,
    onProductClick: (Product) -> Unit,
    onBack: () -> Unit,
    eventSharedFlow: SharedFlow<BillingEvent>,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()

    val dataStyle = MaterialTheme.typography.bodyLarge
    val hintStyle = MaterialTheme.typography.bodyMedium
    val titleStyle = MaterialTheme.typography.titleSmall

    val snackbarHostState = remember { SnackbarHostState() }
    var alertDialogShow by remember {
        mutableStateOf(false)
    }
    var titleAlertDialog by remember {
        mutableIntStateOf(0)
    }

    var textAlertDialog by remember {
        mutableStateOf("")
    }
    if (alertDialogShow) {
        AlertDialog(
            title = { Text(text = (stringResource(id = titleAlertDialog))) },
            text = { Text(text = textAlertDialog) },
            onDismissRequest = { alertDialogShow = !alertDialogShow },
            confirmButton = {
                Button(
                    modifier = Modifier.background(
                        shape = Shapes.medium,
                        color = MaterialTheme.colorScheme.onPrimary
                    ),
                    onClick = { alertDialogShow = !alertDialogShow }) {
                }
            }
        )
    }

    val showPaymentLoadingDialog by viewModel.showPaymentLoadingDialog.collectAsState()
    val showPaymentFailedDialog by viewModel.showPaymentFailedDialog.collectAsState()
    val context = LocalContext.current

    if (showPaymentLoadingDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.secondary,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.primary,
            onDismissRequest = { },
            title = {
                Text(text = "Получаем данные...")
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                }
            },
            confirmButton = { }
        )
    }

    if (showPaymentFailedDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.secondary,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.primary,
            onDismissRequest = { viewModel.dismissPaymentFailedDialog() },
            title = {
                Text(text = "Оплата не завершена")
            },
            text = {
                Text(modifier = Modifier.padding(top = 24.dp), text = "Данные об оплате не получены. Если у вас есть вопросы, напишите в поддержку.")
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.dismissPaymentFailedDialog()
                    val email = "locodriver.app@yandex.ru"
                    val subject = "Вопрос по оплате"
                    val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:$email?subject=$subject".toUri()
                    }
                    try {
                        context.startActivity(mailIntent)
                    } catch (_: Exception) {
                        context.startActivity(
                            Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:".toUri()
                            }
                        )
                    }
                }) {
                    Text(
                        text = "Написать в поддержку",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPaymentFailedDialog() }) {
                    Text(
                        text = "Закрыть",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }

    val showPaymentSuccessDialog by viewModel.showPaymentSuccessDialog.collectAsState()

    if (showPaymentSuccessDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.secondary,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.primary,
            iconContentColor = MaterialTheme.colorScheme.surfaceTint,
            onDismissRequest = { viewModel.dismissPaymentSuccessDialog() },
            title = {
                Text(modifier = Modifier.padding(top = 24.dp), text = "Платеж принят!")
            },
            icon = {
                Icon(
                    modifier = Modifier.size(86.dp),
                    painter = painterResource(R.drawable.check_circle_24px),
                    contentDescription = null
                )
            },
            text = {
                Text(text = "Спасибо за поддержку приложения!")
            },
            confirmButton = {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    onClick = { viewModel.dismissPaymentSuccessDialog() }
                ) {
                    Text(
                        text = "Отлично!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        )
    }

    val showPaymentProcessingDialog by viewModel.showPaymentProcessingDialog.collectAsState()

    if (showPaymentProcessingDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.secondary,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.primary,
            onDismissRequest = { viewModel.dismissPaymentProcessingDialog() },
            title = {
                Text(text = "Платёж обрабатывается")
            },
            text = {
                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = "Robokassa подтвердила платёж, но сервер ещё не обновил подписку. Нажмите «Восстановить покупки» через несколько минут."
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissPaymentProcessingDialog() }) {
                    Text(
                        text = "Понятно",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        )
    }

    val snackbarManager: ISnackbarManager = koinInject()

    LaunchedEffect(Unit) {
        snackbarManager.events
            .flowWithLifecycle(lifecycle)
            .collect { event ->
                val result = snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = event.actionLabel,
                    duration = event.duration
                )
                if (result == SnackbarResult.ActionPerformed) {
                    event.onAction?.let { onAction ->
                        // запускаем suspend-колбек в scope
                        launch {
                            try {
                                onAction()
                            } catch (_: Exception) { /* optional logging */
                            }
                        }
                    }
                }
            }
    }

    val payLauncher = rememberLauncherForActivityResult(RobokassaPayLauncher.Contract) { result ->
        when (result) {
            is RobokassaPayLauncher.Success -> {
                Log.d("zzz", "RobokassaPayLauncher.Success")
                viewModel.checkPaymentOnServer(sdkConfirmed = true)
            }

            is RobokassaPayLauncher.Error -> {
                Log.d("zzz", "RobokassaPayLauncher.Error: ${result.desc}")
                // SDK вернула Error — платёж мог пройти (пользователь оплатил в банке)
                viewModel.checkPaymentOnServer(sdkConfirmed = false)
            }

            is RobokassaPayLauncher.Canceled -> {
                Log.d("zzz", "RobokassaPayLauncher.Canceled")
                // SDK вернула Canceled — платёж мог пройти (пользователь оплатил в банке)
                viewModel.checkPaymentOnServer(sdkConfirmed = false)
            }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            eventSharedFlow.flowWithLifecycle(lifecycle).collect { event ->
                when (event) {
                    is BillingEvent.ShowError -> {
                        snackbarHostState.showSnackbar(message = "Ошибка: ${event.error.message.orEmpty()}")
                    }

                    is BillingEvent.StartPayment -> {
                        payLauncher.launch(
                            RobokassaPayLauncher.StartPay(
                                paymentParams = event.params,
                                onlyCheck = event.onlyChek,
                                testMode = false
                            )
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Машинист Pro",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }, navigationIcon = {
                    IconButton(onClick = {
                        onBack()
                    }) {
                        Icon(
                            tint = MaterialTheme.colorScheme.primary,
                            painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_left_24px),
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            val purchasesEndTimeInLong = viewModel.purchasesEndTime.collectAsState()
            val currentState by viewModel.state.collectAsState()  // Reactive для всего state, чтобы converter был актуальным
            val purchasesEndTime =
                currentState.dateAndTimeConverter?.getDateAndTime(purchasesEndTimeInLong.value)
            Spacer(modifier = Modifier.height(16.dp))
            if (!purchasesEndTime.isNullOrBlank() && purchasesEndTimeInLong.value != 0L) {
                Text(
                    text = "Оплачено до $purchasesEndTime",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            billingState.products.forEach { product ->
                val isActive =
                    billingState.activeExpirations.containsKey<String>(product.name)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = Shapes.medium)
                        .clickable {
                            onProductClick(product)
                        }
                        .background(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = Shapes.medium
                        )
                        .then(
                            if (isActive) Modifier.border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = Shapes.medium
                            ) else Modifier
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (product.desc.isNotEmpty()) {
                        Text(
                            text = product.desc,
                            style = dataStyle.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = product.name,
                            style = dataStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = " за ",
                            style = dataStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${product.sum.toInt()} ₽",
                            style = dataStyle,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        text = "Купить",
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodySmall
                    )


                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = viewModel::restoreSubscribe) {
                Text(
                    text = "Восстановить покупки",
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Обращаем Ваше внимание, покупка автоматически не продлевается. По окончанию срока ее действия необходимо снова совершить покупку.",
                color = MaterialTheme.colorScheme.primary,
                style = hintStyle
            )
        }
    }
}