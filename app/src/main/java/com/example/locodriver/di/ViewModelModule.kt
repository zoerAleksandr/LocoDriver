package com.example.locodriver.di

import com.example.login.viewmodel.LoginViewModel
import com.example.route.viewmodel.DetailsViewModel
import com.example.route.viewmodel.FormViewModel
import com.example.route.viewmodel.HomeViewModel
import com.example.settings.viewmodel.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { FormViewModel(get()) }
    viewModel { DetailsViewModel(get()) }
    viewModel { HomeViewModel() }
    viewModel { LoginViewModel() }
    viewModel { SettingsViewModel() }
}