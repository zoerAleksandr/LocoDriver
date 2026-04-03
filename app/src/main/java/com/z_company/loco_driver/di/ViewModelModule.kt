package com.z_company.loco_driver.di

import com.z_company.loco_driver.viewmodel.MainViewModel
import com.z_company.route.viewmodel.all_route_view_model.AllRouteViewModel
import com.z_company.route.viewmodel.FormViewModel
import com.z_company.route.viewmodel.home_view_model.HomeViewModel
import com.z_company.route.viewmodel.LocoFormViewModel
import com.z_company.route.viewmodel.PassengerFormViewModel
import com.z_company.route.viewmodel.PdfViewModel
import com.z_company.route.viewmodel.ProfileViewModel
import com.z_company.route.viewmodel.PurchasesViewModel
import com.z_company.route.viewmodel.SalaryCalculationViewModel
import com.z_company.route.viewmodel.SearchViewModel
import com.z_company.route.viewmodel.SettingSalaryViewModel
import com.z_company.route.viewmodel.SettingsViewModel
import com.z_company.route.viewmodel.TrainFormViewModel
import com.z_company.route.viewmodel.WorkScheduleViewModel
import com.z_company.route.viewmodel.login.PasswordRecoveryViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel {(routeId: String, isCopy: Boolean) ->
        FormViewModel(application = androidApplication(), routeId = routeId,  isCopy = isCopy)
    }
    viewModel { HomeViewModel() }
    viewModel { PasswordRecoveryViewModel() }
    viewModel { SettingsViewModel() }
    viewModel { MainViewModel() }
    viewModel { (locoId: String?, basicId: String) ->
        LocoFormViewModel(locoId = locoId, basicId = basicId)
    }
    viewModel { (trainId: String?, basicId: String) ->
        TrainFormViewModel(trainId = trainId, basicId = basicId)
    }
    viewModel { (passengerId: String?, basicId: String) ->
        PassengerFormViewModel(passengerId = passengerId, basicId = basicId)
    }
    viewModel { SearchViewModel() }
    viewModel { PurchasesViewModel() }
    viewModel { SalaryCalculationViewModel() }
    viewModel { SettingSalaryViewModel() }
    viewModel { AllRouteViewModel(androidApplication()) }
    viewModel { WorkScheduleViewModel() }
    viewModel { ProfileViewModel() }
    viewModel { PdfViewModel(androidApplication()) }
}
