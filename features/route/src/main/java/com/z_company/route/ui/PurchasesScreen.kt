package com.z_company.route.ui

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.flowWithLifecycle
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.component.GenericLoading
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.route.viewmodel.BillingEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import ru.rustore.sdk.core.exception.RuStoreException
import ru.rustore.sdk.pay.model.Product
import androidx.compose.ui.draw.shadow
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.route.viewmodel.BillingState
import com.z_company.route.viewmodel.PurchasesViewModel
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

    LaunchedEffect(Unit) {
        scope.launch {
            eventSharedFlow.flowWithLifecycle(lifecycle).collect { event ->
                when (event) {
                    is BillingEvent.ShowError -> {
                        if (event.error is RuStoreException) {
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
                        style = titleStyle,
                        color = MaterialTheme.colorScheme.primary
                    )
                }, navigationIcon = {
                    IconButton(onClick = {
                        onBack()
                    }) {
                        Icon(
                            tint = MaterialTheme.colorScheme.primary,
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
    ) { padding ->
        if (billingState.isLoading) {
            GenericLoading()
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                items = billingState.products,
            ) { index, value ->
                val isActive =
                    billingState.activeExpirations.containsKey<String>(value.productId.value)

                val expiryText =
                    billingState.activeExpirations[value.productId.value]?.let { expiryMillis ->
                        billingState.dateAndTimeConverter?.getDateAndTime(expiryMillis)
                    }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 2.dp, shape = Shapes.medium)
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
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = Shapes.medium
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Активна до $expiryText",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Text(
                        text = value.title.value,
                        style = dataStyle,
                        color = MaterialTheme.colorScheme.primary
                    )

                    value.description?.value?.let { desc ->
                        Text(
                            text = desc,
                            style = hintStyle,
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isActive) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = value.amountLabel.value,
                                style = dataStyle,
                                color = MaterialTheme.colorScheme.primary,
                            )

                            TextButton(
                                onClick = { onProductClick(value) }
                            ) {
                                Text(
                                    text = "Оформить",
                                    color = MaterialTheme.colorScheme.tertiary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            item {
                TextButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {
                        viewModel.restoreSubscribe()
                    }
                ) {

                    Text(
                        text = "Восстановить покупки",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            item {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    style = dataStyle,
                    text = "Управление вашими подписками доступно в личном кабинете RuStore",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}