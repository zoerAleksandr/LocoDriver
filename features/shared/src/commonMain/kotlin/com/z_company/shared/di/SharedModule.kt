package com.z_company.shared.di

import com.z_company.shared.platform.PlatformActions
import com.z_company.shared.platform.createPlatformActions
import com.z_company.shared.viewmodel.MoreInfoSharedViewModel
import com.z_company.shared.viewmodel.SalaryCalculationSharedViewModel
import com.z_company.shared.viewmodel.SearchSharedViewModel
import com.z_company.shared.viewmodel.SettingSalarySharedViewModel
import com.z_company.shared.viewmodel.SettingsSharedViewModel
import com.z_company.shared.viewmodel.WorkScheduleSharedViewModel
import org.koin.dsl.module

/**
 * Shared Koin module providing ViewModels and platform services.
 * Both Android and iOS apps should include this module.
 */
val sharedModule = module {
    // Platform services
    single<PlatformActions> { createPlatformActions() }

    // Shared ViewModels
    factory { SettingSalarySharedViewModel(get()) }
    factory { SearchSharedViewModel(get()) }
    factory { WorkScheduleSharedViewModel(get(), get()) }
    factory { SalaryCalculationSharedViewModel(get(), get()) }
    factory { SettingsSharedViewModel(get()) }
    factory { MoreInfoSharedViewModel(get(), get()) }
}
