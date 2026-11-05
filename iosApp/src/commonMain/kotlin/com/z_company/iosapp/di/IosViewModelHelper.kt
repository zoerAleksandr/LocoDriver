package com.z_company.iosapp.di

import com.z_company.iosapp.viewmodel.AppInitIosViewModel
import com.z_company.iosapp.viewmodel.FormIosViewModel
import com.z_company.iosapp.viewmodel.HomeIosViewModel
import com.z_company.iosapp.viewmodel.HomeScreenIosViewModel
import com.z_company.iosapp.viewmodel.NewRouteIosViewModel
import com.z_company.iosapp.viewmodel.AbsenceIosViewModel
import com.z_company.iosapp.viewmodel.CalendarIosViewModel
import com.z_company.iosapp.viewmodel.LocoFormIosViewModel
import com.z_company.iosapp.viewmodel.LocoScreenIosViewModel
import com.z_company.iosapp.viewmodel.LocoTimeSheetIosViewModel
import com.z_company.iosapp.viewmodel.NormMonthIosViewModel
import com.z_company.iosapp.viewmodel.ProfileIosViewModel
import com.z_company.iosapp.viewmodel.ScheduleWizardIosViewModel
import com.z_company.iosapp.viewmodel.SalaryCalculationIosViewModel
import com.z_company.iosapp.viewmodel.PayrollCodeSearchIosViewModel
import com.z_company.iosapp.viewmodel.SettingSalaryIosViewModel
import com.z_company.iosapp.viewmodel.CalcSheetIosViewModel
import com.z_company.iosapp.viewmodel.SettingsIosViewModel
import com.z_company.iosapp.viewmodel.SettingsScreenIosViewModel
import com.z_company.iosapp.viewmodel.PassengerFormIosViewModel
import com.z_company.iosapp.viewmodel.TrainFormIosViewModel
import com.z_company.iosapp.viewmodel.TrainScreenIosViewModel
import com.z_company.iosapp.viewmodel.SeriesEditorIosViewModel
import com.z_company.iosapp.viewmodel.SeriesListIosViewModel
import com.z_company.iosapp.viewmodel.StationEditorIosViewModel
import com.z_company.iosapp.viewmodel.StationListIosViewModel
import com.z_company.iosapp.viewmodel.RouteFormIosViewModel
import com.z_company.iosapp.viewmodel.AllRoutesScreenIosViewModel
import com.z_company.iosapp.viewmodel.RouteQuickViewIosViewModel
import com.z_company.iosapp.viewmodel.SearchIosViewModel
import com.z_company.iosapp.viewmodel.TrashIosViewModel
import com.z_company.iosapp.viewmodel.StatisticsIosViewModel
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

    /** Форма маршрута §5: новый экземпляр на каждую открытую форму; Swift вызывает `dispose()`. */
    fun createRouteFormViewModel(): RouteFormIosViewModel = get()

    /** С1: инициализация при старте (календарь/миграции); один экземпляр за процесс. */
    fun createAppInitViewModel(): AppInitIosViewModel = get()

    /** Экран «Локомотив» §1: новый экземпляр на экран; Swift вызывает `dispose()`. */
    fun createLocoScreenViewModel(): LocoScreenIosViewModel = get()
    /** Экран «Поезд» §6: новый экземпляр на экран; Swift вызывает `dispose()`. */
    fun createTrainScreenViewModel(): TrainScreenIosViewModel = get()
    fun createSettingsScreenViewModel(): SettingsScreenIosViewModel = get()

    /** «Пассажиром» §7, «Прочая работа» §8, напарники §8.5: новый экземпляр на экран; Swift вызывает `dispose()`. */
    fun createPassengerScreenViewModel(): com.z_company.iosapp.viewmodel.PassengerScreenIosViewModel = get()
    fun createOtherWorkScreenViewModel(): com.z_company.iosapp.viewmodel.OtherWorkScreenIosViewModel = get()
    fun createPartnersScreenViewModel(): com.z_company.iosapp.viewmodel.PartnersScreenIosViewModel = get()
    fun createPartnerEditorViewModel(partnerId: String?): com.z_company.iosapp.viewmodel.PartnerEditorIosViewModel =
        com.z_company.iosapp.viewmodel.PartnerEditorIosViewModel(get(), get(), partnerId)

    /** Шторка времени §2: новый экземпляр на открытие; Swift вызывает `dispose()`. */
    fun createLocoTimeSheetViewModel(): LocoTimeSheetIosViewModel = get()

    /** Справочники норм §17.4: по экземпляру на экран/шторку; Swift вызывает `dispose()`. */
    fun createSeriesListViewModel(): SeriesListIosViewModel = get()
    fun createStationListViewModel(): StationListIosViewModel = get()

    /** Экран «Статистика» §13: новый экземпляр на экран; Swift вызывает `dispose()`. */
    fun createStatisticsViewModel(): StatisticsIosViewModel = get()
    fun createSeriesEditorViewModel(id: String?, initialName: String?): SeriesEditorIosViewModel =
        SeriesEditorIosViewModel(get(), get(), id, initialName)
    fun createStationEditorViewModel(id: String?, initialName: String?): StationEditorIosViewModel =
        StationEditorIosViewModel(get(), get(), get(), id, initialName)

    /** Экран «Все маршруты» §9: новый экземпляр на экран; Swift вызывает `dispose()`. */
    fun createAllRoutesViewModel(): AllRoutesScreenIosViewModel = get()

    /** Быстрый просмотр маршрута §9.1: новый экземпляр на шторку. */
    fun createRouteQuickViewModel(): RouteQuickViewIosViewModel = get()

    /** Корзина маршрутов §9.4: новый экземпляр на экран. */
    fun createTrashViewModel(): TrashIosViewModel = get()

    /** Поиск §10: новый экземпляр на экран. */
    fun createSearchViewModel(): SearchIosViewModel = get()

    // ── Зарплата (§11, §12) ─────────────────────────────────────────────────

    /** Справочник кодов §11.8: по экземпляру на открытие экрана. */
    fun createPayrollCodeSearchViewModel(): PayrollCodeSearchIosViewModel = get()

    /**
     * Настройки зарплаты §12: по экземпляру на экран. Последний созданный
     * экземпляр остаётся доступным для шторки «Изменилась тарифная ставка» (§12.3).
     */
    var lastSettingSalaryViewModel: SettingSalaryIosViewModel? = null
        private set

    fun createSettingSalaryViewModel(): SettingSalaryIosViewModel =
        get<SettingSalaryIosViewModel>().also { lastSettingSalaryViewModel = it }

    /** «Расчёт за смену» §11.9: экземпляр на открытую шторку конкретного маршрута. */
    fun createCalcSheetViewModel(basicId: String): CalcSheetIosViewModel =
        CalcSheetIosViewModel(get(), get(), get(), basicId)

    /** Календарь §14: single — месяц глобален (settings.selectMonthOfYear). */
    private val calendarViewModel: CalendarIosViewModel by inject()
    fun getCalendarViewModel(): CalendarIosViewModel = calendarViewModel

    /** Мастер «Заполнить месяц» §15: по экземпляру на открытие; Swift вызывает `dispose()`. */
    fun createScheduleWizardViewModel(): ScheduleWizardIosViewModel = get()

    /** «Новое отвлечение» §16.1/16.4: по экземпляру на экран; Swift вызывает `dispose()`. */
    fun createAbsenceViewModel(): AbsenceIosViewModel = get()

    /** «Норма по месяцам» §16.3/16.3.1: по экземпляру на экран; Swift вызывает `dispose()`. */
    fun createNormMonthViewModel(): NormMonthIosViewModel = get()
}
