package com.z_company.loco_driver.di

import org.koin.dsl.module
import ru.rustore.sdk.pay.RuStorePayClient

val paymentsModule = module {
    single<RuStorePayClient> { RuStorePayClient.instance }
}