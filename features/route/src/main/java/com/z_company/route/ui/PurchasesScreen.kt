package com.z_company.route.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.flowWithLifecycle
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.component.GenericLoading
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.route.viewmodel.BillingEvent
import com.z_company.route.viewmodel.BillingState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import ru.rustore.sdk.billingclient.model.product.Product
import ru.rustore.sdk.billingclient.utils.resolveForBilling
import ru.rustore.sdk.core.exception.RuStoreException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    billingState: BillingState,
    onProductClick: (Product) -> Unit,
    onBack: () -> Unit,
    eventSharedFlow: SharedFlow<BillingEvent>
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
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

    val titleStyle = AppTypography.getType().headlineMedium.copy(fontWeight = FontWeight.Light)

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
    LaunchedEffect(Unit) {
        scope.launch {
            eventSharedFlow.flowWithLifecycle(lifecycle).collect { event ->
                when (event) {
                    is BillingEvent.ShowDialog -> {
                        alertDialogShow = true
                        titleAlertDialog = event.dialogInfo.titleRes
                        textAlertDialog = event.dialogInfo.message
                    }

                    is BillingEvent.ShowError -> {
                        if (event.error is RuStoreException) {
                            event.error.resolveForBilling(context)
                        }
                        event.error.message?.let {
                            if (it.contains("Range timestamp not valid")) {
                                snackbarHostState.showSnackbar(message = "Невозможно получить данные о подписках. На телефоне установлено неверное время. Установите автоматическое определение времени в настройках телефона.")
                            } else {
                                snackbarHostState.showSnackbar(message = "Ошибка: ${event.error.message.orEmpty()}")
                            }
                        }
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
                        text = "Подписки",
                        style = titleStyle
                    )
                }, navigationIcon = {
                    IconButton(onClick = {
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
    ) {
        if (billingState.isLoading) {
            GenericLoading()
        }
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            billingState.subscriptions.forEach { subscription ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = Shapes.medium
                        )
                        .border(
                            width = 1.dp,
                            shape = Shapes.medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = subscription.title.toString(), style = subTitleTextStyle
                    )
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = subscription.description.toString(),
                        style = hintStyle
                    )
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        text = "Активна до ${
                            ConverterLongToTime.getDateAndTimeStringFormat(
                                subscription.expiryTime.toLongOrNull()
                            )
                        }",
                        textAlign = TextAlign.End,
                        style = subTitleTextStyle,
                    )
                }
            }

            billingState.products.forEach { product ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = Shapes.medium
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = product.title.toString(), style = subTitleTextStyle
                    )
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = product.description.toString(),
                        style = hintStyle
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = product.priceLabel.toString(),
                            style = subTitleTextStyle
                        )
                        TextButton(
                            onClick = { onProductClick(product) }
                        ) {
                            Text(
                                text = "Оформить подписку",
                                color = MaterialTheme.colorScheme.tertiary,
                                style = subTitleTextStyle
                            )
                        }
                    }
//                    Text(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(top = 12.dp),
//                        text = "${product.priceLabel} RUB",
//                        textAlign = TextAlign.End,
//                        style = subTitleTextStyle,
//                    )
                }
            }
//            billingState.products
//                .filter { product ->
//                    product.productStatus == ProductStatus.ACTIVE
//                }
//                .forEach { activeProduct ->
//                    Column(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .background(
//                                color = MaterialTheme.colorScheme.surface,
//                                shape = Shapes.medium
//                            )
//                            .padding(horizontal = 16.dp, vertical = 12.dp),
//                        horizontalAlignment = Alignment.End
//                    ) {
//                        Text(
//                            modifier = Modifier
//                                .fillMaxWidth(),
//                            text = activeProduct.title.toString(), style = subTitleTextStyle
//                        )
//                        Text(
//                            modifier = Modifier
//                                .fillMaxWidth(),
//                            text = activeProduct.description.toString(),
//                            style = hintStyle
//                        )
//                        if (billingState.boughtProductsId.contains(activeProduct.productId)) {
//                            val purchases =
//                                billingState.subscriptions.find { purchase -> purchase.productId == activeProduct.productId }
//                            purchases?.let { currentPurchases ->
//                                val purchasesTime = currentPurchases.purchaseTime?.time
//                                purchasesTime?.let {
//                                    val subscriptionInDays =
//                                        activeProduct.subscription?.subscriptionPeriod?.days
//                                    subscriptionInDays?.let {
////                                        val subscriptionInLong = 86_400_000L * subscriptionInDays
////                                        val endPeriodInLong = purchasesTime + subscriptionInLong
//                                        Text(
//                                            modifier = Modifier
//                                                .fillMaxWidth()
//                                                .padding(top = 12.dp),
//                                            text = "Активная",
//                                            textAlign = TextAlign.End,
//                                            style = subTitleTextStyle,
//                                            color = Color.Green
//                                        )
//                                    }
//                                }
//                            }
//                        } else {
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(top = 12.dp),
//                                horizontalArrangement = Arrangement.SpaceBetween,
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                Text(
//                                    text = activeProduct.priceLabel.toString(),
//                                    style = subTitleTextStyle
//                                )
//                                TextButton(
//                                    onClick = { onProductClick(activeProduct) }
//                                ) {
//                                    Text(
//                                        text = "Оформить подписку",
//                                        color = MaterialTheme.colorScheme.tertiary,
//                                        style = subTitleTextStyle
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }

            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                style = subTitleTextStyle,
                text = "Управление вашими подписками доступно в личном кабинете RuStore",
            )
        }
    }
}