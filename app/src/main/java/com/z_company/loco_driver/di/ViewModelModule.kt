package com.z_company.loco_driver.di

import com.z_company.loco_driver.viewmodel.MainViewModel
import com.z_company.login.viewmodel.LoginViewModel
import com.z_company.route.viewmodel.CreatePhotoViewModel
import com.z_company.route.viewmodel.DetailsViewModel
import com.z_company.route.viewmodel.FormViewModel
import com.z_company.route.viewmodel.HomeViewModel
import com.z_company.route.viewmodel.LocoFormViewModel
import com.z_company.route.viewmodel.PassengerFormViewModel
import com.z_company.route.viewmodel.PreviewPhotoViewModel
import com.z_company.route.viewmodel.TrainFormViewModel
import com.z_company.route.viewmodel.ViewingImageViewModel
import com.z_company.settings.viewmodel.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { FormViewModel(get()) }
    viewModel { DetailsViewModel(get()) }
    viewModel { HomeViewModel() }
    viewModel { LoginViewModel() }
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
    viewModel { (notesId: String) ->
        CreatePhotoViewModel(basicId = notesId)
    }
    viewModel {(basicId: String) ->
        PreviewPhotoViewModel(basicId = basicId)
    }
    viewModel { (imageId: String) ->
        ViewingImageViewModel(imageId = imageId)
    }
}