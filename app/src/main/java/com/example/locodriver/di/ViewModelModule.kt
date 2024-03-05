package com.example.locodriver.di

import com.example.locodriver.viewmodel.MainViewModel
import com.example.login.viewmodel.LoginViewModel
import com.example.route.viewmodel.CreatePhotoViewModel
import com.example.route.viewmodel.DetailsViewModel
import com.example.route.viewmodel.FormViewModel
import com.example.route.viewmodel.HomeViewModel
import com.example.route.viewmodel.LocoFormViewModel
import com.example.route.viewmodel.NotesFormViewModel
import com.example.route.viewmodel.PassengerFormViewModel
import com.example.route.viewmodel.TrainFormViewModel
import com.example.settings.viewmodel.SettingsViewModel
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
    viewModel { (basicId: String) ->
        NotesFormViewModel(basicId = basicId)
    }
    viewModel { (notesId: String) ->
        CreatePhotoViewModel(basicId = notesId)
    }
}