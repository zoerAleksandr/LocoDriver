package com.z_company.shared.di

import com.z_company.shared.platform.PlatformActions
import com.z_company.shared.platform.createPlatformActions
import org.koin.dsl.module

/**
 * Shared Koin module providing ViewModels and platform services.
 * Both Android and iOS apps should include this module.
 */
val sharedModule = module {
    // Platform services
    single<PlatformActions> { createPlatformActions() }

    // ViewModels will be added here as screens are migrated
}
