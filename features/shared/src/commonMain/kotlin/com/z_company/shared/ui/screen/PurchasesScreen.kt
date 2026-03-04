package com.z_company.shared.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.z_company.shared.theme.Shapes
import com.z_company.shared.viewmodel.PurchasesSharedViewModel
import org.koin.compose.koinInject

/**
 * Shared Purchases screen — shows product cards, subscription info, restore button.
 *
 * Product click handling is delegated to the ViewModel/BillingProvider.
 * On Android, PurchasesDestination observes paymentEvent and launches RobokassaPayLauncher.
 * On iOS, BillingProvider opens a payment URL.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    onBackClick: () -> Unit,
) {
    val viewModel: PurchasesSharedViewModel = koinInject()
    val products by viewModel.products.collectAsState()
    val purchasesEndTimeInLong by viewModel.purchasesEndTime.collectAsState()
    val dateAndTimeConverter by viewModel.dateAndTimeConverter.collectAsState()

    val dataStyle = MaterialTheme.typography.bodyLarge
    val hintStyle = MaterialTheme.typography.bodyMedium
    val titleStyle = MaterialTheme.typography.titleSmall
    val primaryColor = MaterialTheme.colorScheme.primary

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Покупки",
                        style = titleStyle,
                        color = primaryColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            val purchasesEndTime = dateAndTimeConverter?.getDateAndTime(purchasesEndTimeInLong)

            Spacer(modifier = Modifier.height(16.dp))

            if (!purchasesEndTime.isNullOrBlank() && purchasesEndTimeInLong != 0L) {
                Text(
                    text = "Оплачено до $purchasesEndTime",
                    color = primaryColor,
                    style = dataStyle
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            products.forEach { product ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 2.dp, shape = Shapes.medium)
                        .clickable { viewModel.onProductClick(product) }
                        .background(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = Shapes.medium
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (product.desc.isNotEmpty()) {
                        Text(
                            text = product.desc,
                            style = dataStyle.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(top = 4.dp),
                            color = primaryColor
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = product.name, style = dataStyle, color = primaryColor)
                        Text(text = " за ", style = dataStyle, color = primaryColor)
                        Text(
                            text = "${product.sum.toInt()} ₽",
                            style = dataStyle,
                            color = primaryColor,
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

            TextButton(onClick = viewModel::restorePurchases) {
                Text(
                    text = "Восстановить покупки",
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Обращаем Ваше внимание, покупка автоматически не продлевается. По окончанию срока ее действия необходимо снова совершить покупку.",
                color = primaryColor,
                style = hintStyle
            )
        }
    }
}
