package com.z_company.iosapp.di

import com.z_company.iosapp.viewmodel.AnnouncementIosViewModel
import com.z_company.iosapp.viewmodel.FormIosViewModel
import com.z_company.iosapp.viewmodel.HomeIosViewModel
import com.z_company.iosapp.viewmodel.HomeScreenIosViewModel
import com.z_company.iosapp.viewmodel.NewRouteIosViewModel
import com.z_company.iosapp.viewmodel.LocoFormIosViewModel
import com.z_company.iosapp.viewmodel.LocoScreenIosViewModel
import com.z_company.iosapp.viewmodel.LocoTimeSheetIosViewModel
import com.z_company.iosapp.viewmodel.ProfileIosViewModel
import com.z_company.iosapp.viewmodel.SalaryCalculationIosViewModel
import com.z_company.iosapp.viewmodel.SettingsIosViewModel
import com.z_company.iosapp.viewmodel.SettingsScreenIosViewModel
import com.z_company.iosapp.viewmodel.PassengerFormIosViewModel
import com.z_company.iosapp.viewmodel.TrainFormIosViewModel
import com.z_company.iosapp.viewmodel.WhatsNewIosViewModel
import com.z_company.iosapp.viewmodel.SeriesEditorIosViewModel
import com.z_company.iosapp.viewmodel.SeriesListIosViewModel
import com.z_company.iosapp.viewmodel.StationEditorIosViewModel
import com.z_company.iosapp.viewmodel.StationListIosViewModel
import com.z_company.iosapp.viewmodel.RouteFormIosViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
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
    private val passengerFormViewModel: PassengerFormIosViewModel by inject()
    private val profileViewModel: ProfileIosViewModel by inject()
    private val homeScreenViewModel: HomeScreenIosViewModel by inject()
    private val newRouteViewModel: NewRouteIosViewModel by inject()
    private val announcementViewModel: AnnouncementIosViewModel by inject()
    private val whatsNewViewModel: WhatsNewIosViewModel by inject()

    fun getHomeViewModel(): HomeIosViewModel = homeViewModel
    fun getFormViewModel(): FormIosViewModel = formViewModel
    fun getSettingsViewModel(): SettingsIosViewModel = settingsViewModel
    fun getSalaryCalculationViewModel(): SalaryCalculationIosViewModel = salaryCalculationViewModel
    fun getLocoFormViewModel(): LocoFormIosViewModel = locoFormViewModel
    fun getTrainFormViewModel(): TrainFormIosViewModel = trainFormViewModel
    fun getPassengerFormViewModel(): PassengerFormIosViewModel = passengerFormViewModel
    fun getProfileViewModel(): ProfileIosViewModel = profileViewModel
    fun getHomeScreenViewModel(): HomeScreenIosViewModel = homeScreenViewModel
    fun getNewRouteViewModel(): NewRouteIosViewModel = newRouteViewModel

    /** Новость при запуске §23: single на сессию. */
    fun getAnnouncementViewModel(): AnnouncementIosViewModel = announcementViewModel

    /** «Что нового» §22: single на сессию. */
    fun getWhatsNewViewModel(): WhatsNewIosViewModel = whatsNewViewModel

    /** Форма маршрута §5: новый экземпляр на каждую открытую форму; Swift вызывает `dispose()`. */
    fun createRouteFormViewModel(): RouteFormIosViewModel = get()

    /** Экран «Локомотив» §1: новый экземпляр на экран; Swift вызывает `dispose()`. */
    fun createLocoScreenViewModel(): LocoScreenIosViewModel = get()
    fun createSettingsScreenViewModel(): SettingsScreenIosViewModel = get()

    /** Шторка времени §2: новый экземпляр на открытие; Swift вызывает `dispose()`. */
    fun createLocoTimeSheetViewModel(): LocoTimeSheetIosViewModel = get()

    /** Справочники норм §17.4: по экземпляру на экран/шторку; Swift вызывает `dispose()`. */
    fun createSeriesListViewModel(): SeriesListIosViewModel = get()
    fun createStationListViewModel(): StationListIosViewModel = get()
    fun createSeriesEditorViewModel(id: String?, initialName: String?): SeriesEditorIosViewModel =
        SeriesEditorIosViewModel(get(), get(), id, initialName)
    fun createStationEditorViewModel(id: String?, initialName: String?): StationEditorIosViewModel =
        StationEditorIosViewModel(get(), get(), get(), id, initialName)
}
