package com.z_company.shared.di

import com.z_company.shared.platform.PlatformActions
import com.z_company.shared.platform.createPlatformActions
import com.z_company.shared.search.SharedSearchRouteUseCase
import com.z_company.shared.viewmodel.AllRouteSharedViewModel
import com.z_company.shared.viewmodel.FormLocoSharedViewModel
import com.z_company.shared.viewmodel.FormPassengerSharedViewModel
import com.z_company.shared.viewmodel.FormRouteSharedViewModel
import com.z_company.shared.viewmodel.FormTrainSharedViewModel
import com.z_company.shared.viewmodel.HomeSharedViewModel
import com.z_company.shared.viewmodel.MoreInfoSharedViewModel
import com.z_company.shared.viewmodel.ProfileSharedViewModel
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

    // Search use case (KMP version)
    factory { SharedSearchRouteUseCase(get()) }

    // Shared ViewModels — constructor injection with full Android parity
    factory { SearchSharedViewModel(get(), get(), get()) }
    factory { WorkScheduleSharedViewModel(get(), get(), get()) }
    factory { SalaryCalculationSharedViewModel(get(), get(), get()) }
    factory { SettingsSharedViewModel(get(), get()) }
    factory { SettingSalarySharedViewModel(get()) }
    factory { FormLocoSharedViewModel(get(), get()) }
    factory { FormTrainSharedViewModel(get(), get()) }
    factory { FormPassengerSharedViewModel(get(), get()) }
    factory { MoreInfoSharedViewModel(get(), get()) }

    // Phase 3 — complex screens
    factory { HomeSharedViewModel(get(), get(), get(), get()) }
    factory { FormRouteSharedViewModel(get(), get(), get(), get()) }
    factory { ProfileSharedViewModel(get(), get()) }
    factory { AllRouteSharedViewModel(get()) }
}
