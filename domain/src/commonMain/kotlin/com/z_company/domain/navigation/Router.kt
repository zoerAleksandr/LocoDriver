package com.z_company.domain.navigation

import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.OtherWork
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Train

interface Router {
    fun showSignIn()
    fun showLogIn()
    fun showStartScreen()
    fun showHome(startingRoute: String)
    fun showRouteForm(basicId: String? = null, isMakeCopy: Boolean = false)
    fun showRouteDetails(basicData: BasicData)
    fun showSettings()
    fun showSettingsRoute() { showSettings() }
    fun showSettingsRouteForm() { showSettings() }
    fun showSettingsLoco(seriesName: String? = null) { showSettings() }
    fun showSettingsRest() { showSettings() }
    fun showSettingsSeriesList() { showSettings() }
    fun showSettingsSeriesEditor(seriesId: String) { showSettingsSeriesList() }
    fun showCreateSettingsSeriesEditor(name: String) { showSettingsSeriesList() }
    fun showSettingsStationList() { showSettings() }
    fun showSettingsStationEditor(stationId: String) { showSettingsStationList() }
    fun showPartnersManage() { showSettings() }
    fun showPartnerPicker(basicId: String) {}
    fun showNewPartnerEditor() {}
    fun showEditPartnerEditor(partnerId: String) {}
    fun showSearch()
    fun back()
    fun navigationUp(): Boolean
    fun showChangedLocoForm(locomotive: Locomotive)
    fun showEmptyLocoForm(basicId: String)
    fun showChangeTrainForm(train: Train)
    fun showEmptyTrainForm(basicId: String)
    fun showChangePassengerForm(passenger: Passenger)
    fun showEmptyPassengerForm(basicId: String)
    fun showChangeOtherWorkForm(otherWork: OtherWork)
    fun showEmptyOtherWorkForm(basicId: String)
    fun showSelectReleaseDayScreen()
    fun showPurchasesScreen()

    /**
     * Переход на экран «Профиль» (вкладка нижнего меню).
     * Нужен, когда действие требует авторизации — например попытка оформить
     * подписку без входа в аккаунт. Реализация по умолчанию — no-op для
     * платформ, где отдельного Профиля в навигации ещё нет.
     */
    fun showProfile() {}
    fun showSalaryCalculation()
    fun showSettingSalary()
    fun showAllRoute()
    fun showCalendar()
    fun showScheduleWizard()
    fun showAbsence()
    fun showStatistics()
}
