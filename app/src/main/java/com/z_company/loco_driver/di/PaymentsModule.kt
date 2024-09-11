package com.z_company.loco_driver.di

import com.z_company.loco_driver.R
import com.z_company.loco_driver.payment.PaymentLogger
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.rustore.sdk.billingclient.RuStoreBillingClient
import ru.rustore.sdk.billingclient.RuStoreBillingClientFactory


val paymentsModule = module {
    single<RuStoreBillingClient> {
        RuStoreBillingClientFactory.create(
            context = androidApplication(),
            consoleApplicationId = "2063566563",
            deeplinkScheme = androidContext().resources.getString(R.string.scheme),
            externalPaymentLoggerFactory = { PaymentLogger(tag = "ExamplePaymentApp") },
            debugLogs = true
        )
    }
}