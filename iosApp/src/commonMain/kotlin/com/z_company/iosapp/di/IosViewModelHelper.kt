package com.z_company.iosapp.di

import com.z_company.iosapp.viewmodel.FormIosViewModel
import com.z_company.iosapp.viewmodel.HomeIosViewModel
import com.z_company.iosapp.viewmodel.LocoFormIosViewModel
import com.z_company.iosapp.viewmodel.SalaryCalculationIosViewModel
import com.z_company.iosapp.viewmodel.SettingsIosViewModel
import com.z_company.iosapp.viewmodel.TrainFormIosViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Вспомогательный объект для получения KMP ViewModels из Koin-контейнера в Swift.
 *
 * Использование в Swift:
 * ```swift
 * let vm = IosViewModelHelper().getHomeViewModel()
 * ```
 *
 * Каждый метод возвращает существующий single-экземпляр из Koin.
 */
object IosViewModelHelper : KoinComponent {
    private val homeViewModel: HomeIosViewModel by inject()
    private val formViewModel: FormIosViewModel by inject()
    private val settingsViewModel: SettingsIosViewModel by inject()
    private val salaryCalculationViewModel: SalaryCalculationIosViewModel by inject()
    private val locoFormViewModel: LocoFormIosViewModel by inject()
    private val trainFormViewModel: TrainFormIosViewModel by inject()

    fun getHomeViewModel(): HomeIosViewModel = homeViewModel
    fun getFormViewModel(): FormIosViewModel = formViewModel
    fun getSettingsViewModel(): SettingsIosViewModel = settingsViewModel
    fun getSalaryCalculationViewModel(): SalaryCalculationIosViewModel = salaryCalculationViewModel
    fun getLocoFormViewModel(): LocoFormIosViewModel = locoFormViewModel
    fun getTrainFormViewModel(): TrainFormIosViewModel = trainFormViewModel
}
