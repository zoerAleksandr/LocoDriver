package com.z_company.route.navigation

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.robokassa.library.models.Culture
import com.robokassa.library.models.PaymentMethod
import com.robokassa.library.models.Receipt
import com.robokassa.library.models.ReceiptItem
import com.robokassa.library.models.Tax
import com.robokassa.library.params.PaymentParams
import com.robokassa.library.pay.RobokassaPayLauncher
import com.z_company.domain.navigation.Router
import com.z_company.shared.platform.AndroidBillingProvider
import com.z_company.shared.platform.BillingProvider
import com.z_company.shared.ui.screen.PurchasesScreen
import org.koin.compose.koinInject

@Composable
fun PurchasesDestination(
    router: Router,
) {
    val billing = koinInject<BillingProvider>()
    val androidBilling = billing as? AndroidBillingProvider

    val payLauncher = rememberLauncherForActivityResult(RobokassaPayLauncher.Contract) { result ->
        when (result) {
            is RobokassaPayLauncher.Success -> {
                Log.d("PurchasesDestination", "RobokassaPayLauncher.Success")
                androidBilling?.handlePaymentResult(true)
            }
            is RobokassaPayLauncher.Error -> {
                Log.d("PurchasesDestination", "RobokassaPayLauncher.Error: ${result.desc}")
                androidBilling?.handlePaymentResult(false)
            }
            is RobokassaPayLauncher.Canceled -> {
                Log.d("PurchasesDestination", "RobokassaPayLauncher.Canceled")
                androidBilling?.handlePaymentResult(false)
            }
        }
    }

    // Observe payment events from AndroidBillingProvider
    LaunchedEffect(androidBilling) {
        androidBilling?.paymentEvent?.collect { request ->
            val paymentParams = PaymentParams().setParams {
                orderParams {
                    invoiceId = System.currentTimeMillis().toInt()
                    orderSum = request.product.sum
                    description = request.product.desc
                    receipt = Receipt(
                        items = listOf(
                            ReceiptItem(
                                name = request.product.name,
                                sum = request.product.sum,
                                quantity = 1,
                                paymentMethod = PaymentMethod.FULL_PAYMENT,
                                tax = Tax.NONE
                            )
                        )
                    )
                }
                customerParams {
                    culture = Culture.RU
                    email = request.email
                }
                viewParams {
                    toolbarText = "Оплата ${request.product.name}"
                }
                shp = mapOf("user_id" to request.userId)
            }.also {
                it.setCredentials(
                    request.merchantLogin,
                    request.password1,
                    request.password2,
                    request.resultUrl,
                )
            }

            payLauncher.launch(
                RobokassaPayLauncher.StartPay(
                    paymentParams = paymentParams,
                    onlyCheck = false,
                    testMode = false
                )
            )
        }
    }

    PurchasesScreen(onBackClick = router::back)
}
